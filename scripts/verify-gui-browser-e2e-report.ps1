param(
  [string]$ReportPath = "",
  [int]$MaxAgeDays = 14,
  [switch]$WriteTemplate,
  [switch]$Force
)

$ErrorActionPreference = "Stop"

function Resolve-RepoRoot {
  $scriptDir = Split-Path -Parent $PSCommandPath
  return (Resolve-Path (Join-Path $scriptDir "..")).Path
}

function Get-RelativePath {
  param(
    [string]$BasePath,
    [string]$TargetPath
  )

  $resolvedBase = (Resolve-Path -LiteralPath $BasePath).Path.TrimEnd("\")
  $resolvedTarget = (Resolve-Path -LiteralPath $TargetPath).Path
  if ($resolvedTarget.StartsWith($resolvedBase, [System.StringComparison]::OrdinalIgnoreCase)) {
    return $resolvedTarget.Substring($resolvedBase.Length).TrimStart("\")
  }
  return $resolvedTarget
}

function Resolve-ReportEvidencePath {
  param([string]$Path)

  if ([System.IO.Path]::IsPathRooted($Path)) {
    return $Path
  }
  return Join-Path $repoRoot $Path
}

function Add-Problem {
  param([string]$Message)
  $problems.Add($Message) | Out-Null
}

function Test-ScenarioEvidence {
  param(
    [object]$Scenario,
    [object]$RequiredScenario
  )

  $ScenarioId = [string]$RequiredScenario.id
  if (-not $Scenario.evidence) {
    Add-Problem "$ScenarioId is missing evidence."
    return
  }

  if ($Scenario.evidence -is [string]) {
    Add-Problem "$ScenarioId evidence must be an object with summary and files."
    return
  }
  else {
    $summary = [string]$Scenario.evidence.summary
    if ([string]::IsNullOrWhiteSpace($summary)) {
      Add-Problem "$ScenarioId evidence summary is required."
    }
    elseif ($summary.Trim().Length -lt 20) {
      Add-Problem "$ScenarioId evidence summary is too short to support release acceptance."
    }

    $files = @($Scenario.evidence.files | Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) })
    if ($files.Count -eq 0) {
      Add-Problem "$ScenarioId must include at least one evidence file."
    }
    if ($ScenarioId -eq "visual-polish") {
      $screenshotFiles = @($files | Where-Object { [string]$_ -match "\.(png|jpe?g|webp)$" })
      if ($screenshotFiles.Count -eq 0) {
        Add-Problem "$ScenarioId must include at least one screenshot evidence file (.png, .jpg, .jpeg, or .webp)."
      }
    }
    foreach ($file in $files) {
      $evidencePath = Resolve-ReportEvidencePath -Path ([string]$file)
      if (-not (Test-Path -LiteralPath $evidencePath -PathType Leaf)) {
        Add-Problem "$ScenarioId evidence file is missing: $file"
        continue
      }
      if ((Get-Item -LiteralPath $evidencePath).Length -le 0) {
        Add-Problem "$ScenarioId evidence file is empty: $file"
        continue
      }
    }

    $checks = @($Scenario.evidence.checks)
    if ($checks.Count -eq 0) {
      Add-Problem "$ScenarioId must include evidence.checks."
    }
    $checkMap = @{}
    foreach ($check in $checks) {
      if ($check.id) {
        $checkMap[[string]$check.id] = $check
      }
    }
    foreach ($requiredCheck in @($RequiredScenario.checks)) {
      $checkId = [string]$requiredCheck.id
      if (-not $checkMap.ContainsKey($checkId)) {
        Add-Problem "$ScenarioId is missing required check: $checkId"
        continue
      }
      $check = $checkMap[$checkId]
      if ([string]$check.status -ne "PASS") {
        Add-Problem "$ScenarioId check $checkId status must be PASS."
      }
      $checkSummary = [string]$check.summary
      if ([string]::IsNullOrWhiteSpace($checkSummary) -or $checkSummary.Trim().Length -lt 12) {
        Add-Problem "$ScenarioId check $checkId needs a concrete summary."
      }
    }
  }
}

$repoRoot = Resolve-RepoRoot
if (-not $ReportPath) {
  $ReportPath = Join-Path $repoRoot "build\gui-browser-e2e-report.json"
}
if (-not [System.IO.Path]::IsPathRooted($ReportPath)) {
  $ReportPath = Join-Path $repoRoot $ReportPath
}

$portableRoot = Join-Path $repoRoot "frontend\packages\electron-app\dist\win-portable"
$launcherPath = Join-Path $portableRoot "ShopRPA.cmd"
$archivePath = Join-Path $portableRoot "resources\python_core.7z"
$archiveHash = ""
if (Test-Path -LiteralPath $archivePath -PathType Leaf) {
  $archiveHash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToUpperInvariant()
}

$requiredScenarios = @(
  [PSCustomObject]@{
    id = "portable-launch"
    label = "Portable app launches in an interactive desktop session"
    checks = @(
      [PSCustomObject]@{ id = "launcher-used"; label = "Started from the packaged ShopRPA.cmd launcher" },
      [PSCustomObject]@{ id = "window-visible"; label = "Main window is visible and usable" },
      [PSCustomObject]@{ id = "packaged-runtime"; label = "Packaged runtime path/hash matches the release artifact" }
    )
  },
  [PSCustomObject]@{
    id = "workflow-editor"
    label = "Workflow can be created, saved, reloaded, and edited"
    checks = @(
      [PSCustomObject]@{ id = "create-save"; label = "Created and saved a workflow" },
      [PSCustomObject]@{ id = "reload-edit"; label = "Reloaded and edited the saved workflow" },
      [PSCustomObject]@{ id = "no-data-loss"; label = "No workflow data loss or UI corruption observed" }
    )
  },
  [PSCustomObject]@{
    id = "engine-run"
    label = "A representative desktop automation workflow runs through the packaged engine"
    checks = @(
      [PSCustomObject]@{ id = "packaged-engine"; label = "Run used the packaged engine/runtime" },
      [PSCustomObject]@{ id = "task-complete"; label = "Representative workflow completed" },
      [PSCustomObject]@{ id = "output-verified"; label = "Expected output or side effect was verified" }
    )
  },
  [PSCustomObject]@{
    id = "browser-extension"
    label = "Browser extension installs, opens, and connects to the bridge"
    checks = @(
      [PSCustomObject]@{ id = "extension-loaded"; label = "Extension is loaded in Chrome or Edge" },
      [PSCustomObject]@{ id = "bridge-connected"; label = "Bridge connection is established" },
      [PSCustomObject]@{ id = "permission-state"; label = "Required extension permissions are granted" }
    )
  },
  [PSCustomObject]@{
    id = "browser-automation"
    label = "A representative browser automation workflow runs successfully"
    checks = @(
      [PSCustomObject]@{ id = "target-page"; label = "Target web page opened" },
      [PSCustomObject]@{ id = "action-executed"; label = "Browser action sequence executed" },
      [PSCustomObject]@{ id = "result-verified"; label = "Browser automation result was verified" }
    )
  },
  [PSCustomObject]@{
    id = "backend-connect"
    label = "Client connects to the configured backend and handles auth/session state"
    checks = @(
      [PSCustomObject]@{ id = "backend-health"; label = "Configured backend health is reachable" },
      [PSCustomObject]@{ id = "auth-session"; label = "Authentication/session flow works" },
      [PSCustomObject]@{ id = "error-state"; label = "Expected error states are visible and actionable" }
    )
  },
  [PSCustomObject]@{
    id = "visual-polish"
    label = "Primary UI surfaces are visually polished, readable, and free of obvious layout defects"
    checks = @(
      [PSCustomObject]@{ id = "primary-screenshots"; label = "Primary screens have screenshot evidence" },
      [PSCustomObject]@{ id = "no-overlap"; label = "No text overlap, clipping, or broken layout observed" },
      [PSCustomObject]@{ id = "readability"; label = "Text, controls, and status states are readable" }
    )
  },
  [PSCustomObject]@{
    id = "logs-no-errors"
    label = "Runtime logs and UI show no blocking errors after the run"
    checks = @(
      [PSCustomObject]@{ id = "main-log"; label = "Main process log reviewed" },
      [PSCustomObject]@{ id = "renderer-log"; label = "Renderer/UI log or console reviewed" },
      [PSCustomObject]@{ id = "backend-log"; label = "Backend log or remote health evidence reviewed" }
    )
  }
)

if ($WriteTemplate) {
  $reportDir = Split-Path -Parent $ReportPath
  if (-not (Test-Path -LiteralPath $reportDir -PathType Container)) {
    New-Item -ItemType Directory -Path $reportDir | Out-Null
  }
  if ((Test-Path -LiteralPath $ReportPath -PathType Leaf) -and -not $Force) {
    throw "Report already exists: $ReportPath. Use -Force to overwrite it."
  }

  $template = [PSCustomObject]@{
    schemaVersion = 1
    ok = $false
    testedAt = ""
    tester = ""
    portableLauncher = $launcherPath
    portableArchiveSha256 = $archiveHash
    environment = [PSCustomObject]@{
      windowsVersion = ""
      backendMode = "docker|remote"
      backendUrl = ""
      browser = "Chrome|Edge"
      notes = ""
    }
    scenarios = @($requiredScenarios | ForEach-Object {
        [PSCustomObject]@{
          id = $_.id
          label = $_.label
          status = "PENDING"
          evidence = [PSCustomObject]@{
            summary = ""
            files = @()
            checks = @($_.checks | ForEach-Object {
                [PSCustomObject]@{
                  id = $_.id
                  label = $_.label
                  status = "PENDING"
                  summary = ""
                }
              })
          }
        }
      })
    failures = @()
  }

  $template | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $ReportPath -Encoding UTF8
  Write-Host "GUI/browser E2E report template written: $(Get-RelativePath -BasePath $repoRoot -TargetPath $ReportPath)"
  exit 0
}

if (-not (Test-Path -LiteralPath $ReportPath -PathType Leaf)) {
  Write-Host "GUI/browser E2E report is missing: $ReportPath." -ForegroundColor Yellow
  Write-Host "Create a template with corepack pnpm run template:gui-e2e, complete it in an interactive desktop validation session, then rerun verification." -ForegroundColor Yellow
  exit 1
}

$report = Get-Content -LiteralPath $ReportPath -Raw | ConvertFrom-Json
$problems = New-Object System.Collections.Generic.List[string]

if ($report.schemaVersion -ne 1) {
  Add-Problem "schemaVersion must be 1."
}
$pendingScenarioCount = @($report.scenarios | Where-Object { [string]$_.status -eq "PENDING" }).Count
if (($report.ok -ne $true) -and ($pendingScenarioCount -gt 0)) {
  Add-Problem "report template is incomplete; complete the interactive desktop validation run and attach evidence for every required scenario."
}
if ($report.ok -ne $true) {
  Add-Problem "ok must be true."
}
if ([string]::IsNullOrWhiteSpace([string]$report.tester)) {
  Add-Problem "tester is required."
}
if ([string]::IsNullOrWhiteSpace([string]$report.testedAt)) {
  Add-Problem "testedAt is required."
}
else {
  try {
    $testedAt = [DateTimeOffset]::Parse([string]$report.testedAt)
    if ($testedAt -gt [DateTimeOffset]::Now.AddMinutes(10)) {
      Add-Problem "testedAt is in the future: $($report.testedAt)"
    }
    if ($MaxAgeDays -gt 0 -and $testedAt -lt [DateTimeOffset]::Now.AddDays(-$MaxAgeDays)) {
      Add-Problem "testedAt is older than $MaxAgeDays days: $($report.testedAt)"
    }
  }
  catch {
    Add-Problem "testedAt is not a valid date/time: $($report.testedAt)"
  }
}

if (-not $report.environment) {
  Add-Problem "environment is required."
}
else {
  if ([string]::IsNullOrWhiteSpace([string]$report.environment.windowsVersion)) {
    Add-Problem "environment.windowsVersion is required."
  }
  $backendMode = [string]$report.environment.backendMode
  if ($backendMode -notin @("docker", "remote")) {
    Add-Problem "environment.backendMode must be docker or remote."
  }
  if ([string]::IsNullOrWhiteSpace([string]$report.environment.backendUrl)) {
    Add-Problem "environment.backendUrl is required."
  }
  if ([string]::IsNullOrWhiteSpace([string]$report.environment.browser)) {
    Add-Problem "environment.browser is required."
  }
}

if (-not (Test-Path -LiteralPath $launcherPath -PathType Leaf)) {
  Add-Problem "current portable launcher is missing: $launcherPath"
}
if (-not (Test-Path -LiteralPath $archivePath -PathType Leaf)) {
  Add-Problem "current portable Python archive is missing: $archivePath"
}
if ([string]$report.portableLauncher -ne $launcherPath) {
  Add-Problem "portableLauncher does not match the current portable package path."
}
if ($archiveHash -and ([string]$report.portableArchiveSha256).ToUpperInvariant() -ne $archiveHash) {
  Add-Problem "portableArchiveSha256 does not match current package hash $archiveHash."
}

$scenarioMap = @{}
foreach ($scenario in @($report.scenarios)) {
  if ($scenario.id) {
    $scenarioMap[[string]$scenario.id] = $scenario
  }
}

foreach ($requiredScenario in $requiredScenarios) {
  if (-not $scenarioMap.ContainsKey($requiredScenario.id)) {
    Add-Problem "Missing required scenario: $($requiredScenario.id)"
    continue
  }

  $scenario = $scenarioMap[$requiredScenario.id]
  if ([string]$scenario.status -ne "PASS") {
    Add-Problem "$($requiredScenario.id) status must be PASS."
  }
  Test-ScenarioEvidence -Scenario $scenario -RequiredScenario $requiredScenario
}

if ($report.failures -and @($report.failures).Count -gt 0) {
  Add-Problem "failures must be empty for release acceptance."
}

if ($problems.Count -gt 0) {
  Write-Host "GUI/browser E2E report verification failed." -ForegroundColor Yellow
  foreach ($problem in $problems) {
    Write-Host "- $problem" -ForegroundColor Yellow
  }
  exit 1
}

Write-Host "GUI/browser E2E report verified: $(Get-RelativePath -BasePath $repoRoot -TargetPath $ReportPath)"
