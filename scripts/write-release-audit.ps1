param(
  [string]$OutputPath = "",
  [switch]$Strict
)

$ErrorActionPreference = "Stop"

function Resolve-RepoRoot {
  $scriptDir = Split-Path -Parent $PSCommandPath
  return (Resolve-Path (Join-Path $scriptDir "..")).Path
}

function Invoke-Captured {
  param(
    [string]$FilePath,
    [string[]]$Arguments,
    [string]$WorkingDirectory
  )

  $psi = [System.Diagnostics.ProcessStartInfo]::new()
  $resolvedCommand = Get-Command $FilePath -ErrorAction SilentlyContinue
  if ($resolvedCommand) {
    $psi.FileName = $resolvedCommand.Source
  }
  else {
    $psi.FileName = $FilePath
  }
  $psi.Arguments = (($Arguments | ForEach-Object {
        if ($_ -match '[\s"]') {
          '"' + ($_ -replace '"', '\"') + '"'
        }
        else {
          $_
        }
      }) -join " ")
  $psi.WorkingDirectory = $WorkingDirectory
  $psi.RedirectStandardOutput = $true
  $psi.RedirectStandardError = $true
  $psi.UseShellExecute = $false
  $psi.CreateNoWindow = $true

  $process = [System.Diagnostics.Process]::new()
  $process.StartInfo = $psi
  [void]$process.Start()
  $stdout = $process.StandardOutput.ReadToEnd()
  $stderr = $process.StandardError.ReadToEnd()
  $process.WaitForExit()

  [PSCustomObject]@{
    ExitCode = $process.ExitCode
    Output = (($stdout + "`n" + $stderr).Trim())
  }
}

function Add-Line {
  param(
    [System.Collections.Generic.List[string]]$Lines,
    [string]$Text = ""
  )
  $Lines.Add($Text) | Out-Null
}

function Escape-MarkdownTableCell {
  param([object]$Value)

  if ($null -eq $Value) {
    return ""
  }

  return (Normalize-ReportText ([string]$Value)).
    Replace("|", "\|").
    Replace("`r`n", "<br>").
    Replace("`n", "<br>").
    Replace("`r", "<br>")
}

function Normalize-ReportText {
  param([string]$Text)

  if (-not $Text) {
    return ""
  }

  return ($Text -replace "\x1B\[[0-9;?]*[ -/]*[@-~]", "").Trim()
}

function Get-ConciseReportLine {
  param(
    [string]$Text,
    [int]$MaxLength = 300
  )

  $normalized = Normalize-ReportText $Text
  foreach ($line in ($normalized -split "`r?`n")) {
    $trimmed = $line.Trim()
    if (-not $trimmed) {
      continue
    }
    if ($trimmed.StartsWith("[")) {
      continue
    }
    if ($trimmed.Length -gt $MaxLength) {
      return ($trimmed.Substring(0, $MaxLength - 3) + "...")
    }
    return $trimmed
  }
  return ""
}

function Get-GuiE2eProblemSummary {
  param(
    [string]$Text,
    [int]$MaxLength = 700
  )

  $normalized = Normalize-ReportText $Text
  $problems = @(
    $normalized -split "`r?`n" |
      ForEach-Object { $_.Trim() } |
      Where-Object { $_.StartsWith("- ") } |
      ForEach-Object { $_.Substring(2).Trim() } |
      Where-Object { $_ }
  )
  if ($problems.Count -eq 0) {
    return Get-ConciseReportLine -Text $normalized -MaxLength $MaxLength
  }

  $parts = New-Object System.Collections.Generic.List[string]
  if (@($problems | Where-Object { $_ -like "report template is incomplete*" }).Count -gt 0) {
    $parts.Add("template incomplete") | Out-Null
  }

  $reportFieldProblems = @(
    $problems |
      Where-Object { $_ -in @("ok must be true.", "tester is required.", "testedAt is required.") } |
      ForEach-Object { $_.TrimEnd(".") }
  )
  if ($reportFieldProblems.Count -gt 0) {
    $parts.Add("report fields: $($reportFieldProblems -join ', ')") | Out-Null
  }

  $environmentProblemCount = @($problems | Where-Object { $_.StartsWith("environment.") }).Count
  if ($environmentProblemCount -gt 0) {
    $parts.Add("environment issues=$environmentProblemCount") | Out-Null
  }

  $scenarioIds = New-Object System.Collections.Generic.HashSet[string]
  $evidenceScenarioIds = New-Object System.Collections.Generic.HashSet[string]
  $checkIds = New-Object System.Collections.Generic.HashSet[string]
  foreach ($problem in $problems) {
    if ($problem -match "^([A-Za-z0-9-]+) status must be PASS\.$") {
      [void]$scenarioIds.Add($Matches[1])
    }
    elseif ($problem -match "^([A-Za-z0-9-]+) (evidence|must include)") {
      [void]$evidenceScenarioIds.Add($Matches[1])
    }
    elseif ($problem -match "^([A-Za-z0-9-]+) check ([A-Za-z0-9-]+) ") {
      [void]$checkIds.Add("$($Matches[1])/$($Matches[2])")
    }
  }
  if ($scenarioIds.Count -gt 0) {
    $parts.Add("scenarios not PASS=$($scenarioIds.Count)") | Out-Null
  }
  if ($evidenceScenarioIds.Count -gt 0) {
    $parts.Add("scenarios missing evidence=$($evidenceScenarioIds.Count)") | Out-Null
  }
  if ($checkIds.Count -gt 0) {
    $parts.Add("checks not accepted=$($checkIds.Count)") | Out-Null
  }

  $examples = @($problems | Where-Object { $_ -match " check " } | Select-Object -First 3)
  if ($examples.Count -eq 0) {
    $examples = @($problems | Select-Object -First 3)
  }
  if ($examples.Count -gt 0) {
    $parts.Add("examples: $($examples -join '; ')") | Out-Null
  }

  $summary = $parts -join "; "
  if (-not $summary) {
    $summary = @($problems | Select-Object -First 3) -join "; "
  }
  if ($summary.Length -gt $MaxLength) {
    return ($summary.Substring(0, $MaxLength - 3) + "...")
  }
  return $summary
}

function Get-BrowserExtensionSmokeSummary {
  param(
    [object]$Report,
    [int]$MaxLength = 700
  )

  $parts = New-Object System.Collections.Generic.List[string]
  $firstFailure = [string](@($Report.failures | Where-Object { $_ }) | Select-Object -First 1)
  if ($firstFailure) {
    $failureLine = Get-ConciseReportLine -Text $firstFailure -MaxLength 240
    if ($failureLine) {
      $parts.Add("run failed: $failureLine") | Out-Null
    }
  }
  if ($Report.hostPolicyBlocked -eq $true) {
    $failureClass = [string]$Report.failureClass
    if ([string]::IsNullOrWhiteSpace($failureClass)) {
      $failureClass = "browser-host-policy-blocked"
    }
    $parts.Add("failureClass=$failureClass") | Out-Null
  }

  $missingFields = @()
  $requiredFieldNames = @("manifestVersion", "browserExecutable", "testPageUrl")
  if ($Report.ok -eq $true) {
    $requiredFieldNames += "extensionId"
  }
  foreach ($fieldName in $requiredFieldNames) {
    if ([string]::IsNullOrWhiteSpace([string]$Report.$fieldName)) {
      $missingFields += $fieldName
    }
  }
  if ($missingFields.Count -gt 0) {
    $parts.Add("missing fields=$($missingFields.Count) ($($missingFields -join ', '))") | Out-Null
  }

  $requiredScenarios = @("extension-load", "active-tab", "content-script-message", "element-input", "element-click", "table-extract")
  $scenarioMap = @{}
  foreach ($scenario in @($Report.scenarios)) {
    if ($scenario.id) {
      $scenarioMap[[string]$scenario.id] = $scenario
    }
  }
  $missingScenarios = @()
  $failedScenarios = @()
  if ($Report.ok -eq $true) {
    foreach ($scenarioId in $requiredScenarios) {
      if (-not $scenarioMap.ContainsKey($scenarioId)) {
        $missingScenarios += $scenarioId
        continue
      }
      if ([string]$scenarioMap[$scenarioId].status -ne "PASS") {
        $failedScenarios += $scenarioId
      }
    }
  }
  if ($missingScenarios.Count -gt 0) {
    $parts.Add("missing scenarios=$($missingScenarios.Count) ($($missingScenarios -join ', '))") | Out-Null
  }
  if ($failedScenarios.Count -gt 0) {
    $parts.Add("scenarios not PASS=$($failedScenarios.Count) ($($failedScenarios -join ', '))") | Out-Null
  }

  $evidenceIssues = @()
  $requiredEvidenceNames = if ($Report.ok -eq $true) { @("screenshot", "html") } else { @("html") }
  foreach ($evidenceName in $requiredEvidenceNames) {
    if ([string]::IsNullOrWhiteSpace([string]$Report.evidence.$evidenceName)) {
      $evidenceIssues += $evidenceName
    }
  }
  if ($evidenceIssues.Count -gt 0) {
    $parts.Add("missing evidence=$($evidenceIssues.Count) ($($evidenceIssues -join ', '))") | Out-Null
  }

  $summary = $parts -join "; "
  if (-not $summary) {
    $summary = "report has ok=false"
  }
  if ($summary.Length -gt $MaxLength) {
    return ($summary.Substring(0, $MaxLength - 3) + "...")
  }
  return $summary
}

function Get-ReportFreshnessProblem {
  param(
    [object]$Report,
    [string]$PropertyName = "generatedAt",
    [int]$MaxAgeDays = 14
  )

  $timestampValue = [string]$Report.$PropertyName
  if ([string]::IsNullOrWhiteSpace($timestampValue)) {
    return "$PropertyName is missing"
  }

  try {
    $timestamp = [DateTimeOffset]::Parse($timestampValue)
  }
  catch {
    return "$PropertyName is not a valid timestamp: $timestampValue"
  }

  if ($timestamp -gt [DateTimeOffset]::Now.AddMinutes(10)) {
    return "$PropertyName is in the future: $timestampValue"
  }

  if ($MaxAgeDays -gt 0 -and $timestamp -lt [DateTimeOffset]::Now.AddDays(-$MaxAgeDays)) {
    return "$PropertyName is older than $MaxAgeDays days: $timestampValue"
  }

  return ""
}

function Add-ArtifactRow {
  param(
    [System.Collections.Generic.List[string]]$Lines,
    [string]$Artifact,
    [string]$Path,
    [string]$State
  )

  Add-Line $Lines "| $(Escape-MarkdownTableCell $Artifact) | ``$(Escape-MarkdownTableCell $Path)`` | $(Escape-MarkdownTableCell $State) |"
}

function Add-GateRow {
  param(
    [System.Collections.Generic.List[string]]$Lines,
    [string]$Gate,
    [string]$Evidence,
    [string]$Status
  )

  Add-Line $Lines "| ``$(Escape-MarkdownTableCell $Gate)`` | $(Escape-MarkdownTableCell $Evidence) | $(Escape-MarkdownTableCell $Status) |"
}

function Get-WarningDetails {
  param([string]$DoctorOutput)

  $warnings = New-Object System.Collections.Generic.List[string]
  $inWarningDetails = $false
  foreach ($line in ($DoctorOutput -split "`r?`n")) {
    $trimmed = $line.Trim()
    if ($trimmed -eq "Warning details:") {
      $inWarningDetails = $true
      continue
    }
    if (-not $inWarningDetails) {
      continue
    }
    if ($trimmed -match "^Warnings remain:") {
      break
    }
    if ($trimmed.StartsWith("- ")) {
      $warnings.Add($trimmed.Substring(2)) | Out-Null
    }
  }
  return $warnings
}

function Get-DoctorCheckStatus {
  param(
    [string]$DoctorOutput,
    [string]$Name
  )

  $pattern = "^\s*$([regex]::Escape($Name))\s+(PASS|WARN|FAIL)\s+(.+?)\s*$"
  foreach ($line in ($DoctorOutput -split "`r?`n")) {
    $normalized = Normalize-ReportText $line
    if ($normalized -match $pattern) {
      $status = $Matches[1]
      $detail = $Matches[2].Trim()
      if ($detail) {
        return "$status; $detail"
      }
      return $status
    }
  }

  return "Missing from release doctor output"
}

function Get-FirstUsefulLine {
  param([string]$Text)

  foreach ($line in ($Text -split "`r?`n")) {
    $trimmed = $line.Trim()
    if ($trimmed) {
      return $trimmed
    }
  }
  return ""
}

function Get-LastMatchingLine {
  param(
    [string]$Text,
    [string[]]$Patterns
  )

  $matchedLines = New-Object System.Collections.Generic.List[string]
  foreach ($line in ($Text -split "`r?`n")) {
    $trimmed = (Normalize-ReportText $line)
    if (-not $trimmed) {
      continue
    }
    if ($trimmed -match "^\+" -or
      $trimmed -match "^At\s+" -or
      $trimmed -match "^\s*\+\s*CategoryInfo\s*:" -or
      $trimmed -match "^\s*\+\s*FullyQualifiedErrorId\s*:") {
      continue
    }
    foreach ($pattern in $Patterns) {
      if ($trimmed -match $pattern) {
        $matchedLines.Add($trimmed) | Out-Null
        break
      }
    }
  }

  if ($matchedLines.Count -gt 0) {
    return $matchedLines[$matchedLines.Count - 1]
  }
  return ""
}

function Get-NamedBlockedChecks {
  param([string]$Text)

  $names = New-Object System.Collections.Generic.List[string]
  $inBlockedSection = $false
  foreach ($line in ($Text -split "`r?`n")) {
    $trimmed = (Normalize-ReportText $line)
    if ($trimmed -eq "Blocked checks:") {
      $inBlockedSection = $true
      continue
    }
    if ($trimmed -eq "Warning checks:" -or $trimmed -eq "Next required host actions:") {
      $inBlockedSection = $false
      continue
    }
    if ($inBlockedSection -and $trimmed -match "^- ([^:]+):") {
      $names.Add($Matches[1].Trim()) | Out-Null
    }
  }

  return $names
}

function Get-NextRequiredHostActions {
  param([string]$Text)

  $actions = New-Object System.Collections.Generic.List[string]
  $inActionsSection = $false
  foreach ($line in ($Text -split "`r?`n")) {
    $trimmed = (Normalize-ReportText $line)
    if ($trimmed -eq "Next required host actions:") {
      $inActionsSection = $true
      continue
    }
    if (-not $inActionsSection) {
      continue
    }
    if ($trimmed -match "^- (.+)$") {
      $actions.Add($Matches[1].Trim()) | Out-Null
    }
    elseif ($trimmed) {
      $inActionsSection = $false
    }
  }

  return $actions
}

function Read-YamlScalarValue {
  param(
    [string]$YamlPath,
    [string]$Key
  )

  if (-not (Test-Path -LiteralPath $YamlPath -PathType Leaf)) {
    return ""
  }

  $pattern = "^\s*$([regex]::Escape($Key))\s*:\s*(.+?)\s*$"
  foreach ($line in Get-Content -LiteralPath $YamlPath) {
    if ($line -match $pattern) {
      return $Matches[1].Trim().Trim('"').Trim("'")
    }
  }
  return ""
}

function Test-HttpArtifact {
  param(
    [string]$BaseUrl,
    [string]$RelativePath,
    [string]$Label,
    [string]$SourcePath = ""
  )

  if (-not $BaseUrl) {
    return "$Label not checked; backend gateway config is missing"
  }

  try {
    $baseUri = [System.Uri]::new($BaseUrl)
    $targetUri = [System.Uri]::new($baseUri, $RelativePath.TrimStart("/"))
    $response = Invoke-WebRequest -Uri $targetUri.AbsoluteUri -UseBasicParsing -TimeoutSec 5
    if ($response.StatusCode -eq 200 -and [string]$response.Content) {
      return "Pass; $($targetUri.AbsoluteUri) returned HTTP 200"
    }
    return "Warn; $($targetUri.AbsoluteUri) returned HTTP $($response.StatusCode) with empty content"
  }
  catch {
    if ($SourcePath -and (Test-Path -LiteralPath $SourcePath -PathType Leaf)) {
      $sourceContent = Get-Content -LiteralPath $SourcePath -Raw
      if ($sourceContent.Trim().Length -gt 0) {
        return "Blocked; live $Label is not reachable: $($_.Exception.Message); source asset is present at $SourcePath"
      }
    }
    $sourceDetail = if ($SourcePath) { "; source asset is missing or empty: $SourcePath" } else { "" }
    return "Blocked; live $Label is not reachable: $($_.Exception.Message)$sourceDetail"
  }
}

function Test-RobotOpenApiWorkflowArtifact {
  param([string]$RepoRoot)

  $expectedUrl = "http://openapi-service:8020/workflows/upsert"
  $robotConfigPath = Join-Path $RepoRoot "backend\robot-service\src\main\resources\application-local.yml"
  $dockerEnvPath = Join-Path $RepoRoot "docker\.env"
  $dockerExampleEnvPath = Join-Path $RepoRoot "docker\.env.example"
  $composePath = Join-Path $RepoRoot "docker\docker-compose.yml"

  foreach ($file in @($robotConfigPath, $dockerEnvPath, $dockerExampleEnvPath, $composePath)) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
      return "Missing $file"
    }
  }

  $combined = (Get-Content -LiteralPath $robotConfigPath -Raw) + "`n" +
    (Get-Content -LiteralPath $dockerEnvPath -Raw) + "`n" +
    (Get-Content -LiteralPath $dockerExampleEnvPath -Raw) + "`n" +
    (Get-Content -LiteralPath $composePath -Raw)
  if ($combined -match "rpa-openapi:6699|ExampleConstants\.WORKFLOWS_UPSERT_URL|static\s+.*WORKFLOWS_UPSERT_URL") {
    return "Blocked; stale hardcoded endpoint remains"
  }
  if ($combined -notmatch [regex]::Escape($expectedUrl)) {
    return "Blocked; expected endpoint missing: $expectedUrl"
  }
  if ($combined -notmatch "(?m)^\s+openapi-service:\s*$") {
    return "Blocked; compose service openapi-service missing"
  }

  return "Pass; $expectedUrl"
}

function Test-OpenApiRobotServiceArtifact {
  param([string]$RepoRoot)

  $expectedBaseUrl = "http://robot-service:8040"
  $configPath = Join-Path $RepoRoot "backend\openapi-service\app\config.py"
  $dockerEnvPath = Join-Path $RepoRoot "docker\.env"
  $dockerExampleEnvPath = Join-Path $RepoRoot "docker\.env.example"

  foreach ($file in @($configPath, $dockerEnvPath, $dockerExampleEnvPath)) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
      return "Missing $file"
    }
  }

  $combined = (Get-Content -LiteralPath $configPath -Raw) + "`n" +
    (Get-Content -LiteralPath $dockerEnvPath -Raw) + "`n" +
    (Get-Content -LiteralPath $dockerExampleEnvPath -Raw)
  if ($combined -match "http://robot-service:8040/api/robot") {
    return "Blocked; hardcoded robot-service API URL remains"
  }
  if ($combined -notmatch "ROBOT_SERVICE_BASE_URL") {
    return "Blocked; ROBOT_SERVICE_BASE_URL setting missing"
  }
  if ($combined -notmatch [regex]::Escape($expectedBaseUrl)) {
    return "Blocked; expected base URL missing: $expectedBaseUrl"
  }

  return "Pass; $expectedBaseUrl"
}

function Test-OpenApiAdminRouteArtifact {
  param([string]$RepoRoot)

  $mainPath = Join-Path $RepoRoot "backend\openapi-service\app\main.py"
  $adminPath = Join-Path $RepoRoot "backend\openapi-service\app\internal\admin.py"
  foreach ($file in @($mainPath, $adminPath)) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
      return "Missing $file"
    }
  }

  $mainSource = Get-Content -LiteralPath $mainPath -Raw
  $adminSource = Get-Content -LiteralPath $adminPath -Raw
  if (($mainSource -notmatch "from app\.internal import admin") -or
    ($mainSource -notmatch "include_router\(admin\.router,\s*prefix=`"/admin`"")) {
    return "Blocked; admin router is not mounted at /admin"
  }
  if (($adminSource -match "schwifty|Hello Bigger Applications|TODO|FIXME") -or
    ($adminSource -notmatch "verify_admin_api_key") -or
    ($adminSource -notmatch "X-API-Key")) {
    return "Blocked; admin route is missing internal API-key protection or contains placeholder text"
  }

  return "Pass; mounted and protected by X-API-Key"
}

function Test-NginxGatewayHealthArtifact {
  param([string]$RepoRoot)

  $confPath = Join-Path $RepoRoot "docker\volumes\nginx\default.conf"
  if (-not (Test-Path -LiteralPath $confPath -PathType Leaf)) {
    return "Missing $confPath"
  }

  $confText = Get-Content -LiteralPath $confPath -Raw
  $problems = New-Object System.Collections.Generic.List[string]
  $healthRoutes = @(
    [PSCustomObject]@{
      Label    = "ai-service"
      Path     = "/api/rpa-ai-service/health"
      Upstream = "ai_service_endpoint"
    },
    [PSCustomObject]@{
      Label    = "openapi-service"
      Path     = "/api/rpa-openapi/health"
      Upstream = "openapi_service_endpoint"
    }
  )

  foreach ($route in $healthRoutes) {
    $blockPattern = "location\s+=\s+$([regex]::Escape($route.Path))\s*\{(?s:.*?)\n\s*\}"
    $blockMatch = [regex]::Match($confText, $blockPattern)
    if (-not $blockMatch.Success) {
      $problems.Add("missing exact unauthenticated location for $($route.Path)") | Out-Null
      continue
    }
    if ($blockMatch.Value -match "access_by_lua_file") {
      $problems.Add("$($route.Path) still runs Lua auth") | Out-Null
    }
    if ($blockMatch.Value -notmatch "rewrite\s+\^\s+/health\s+break;") {
      $problems.Add("$($route.Path) does not rewrite to upstream /health") | Out-Null
    }
    $expectedProxy = "proxy_pass http://`$$($route.Upstream);"
    if ($blockMatch.Value -notlike "*$expectedProxy*") {
      $problems.Add("$($route.Path) does not proxy to $($route.Upstream)") | Out-Null
    }
  }

  if ($problems.Count -gt 0) {
    return "Blocked; $($problems -join '; ')"
  }

  return "Pass; exact health routes bypass Lua auth and proxy to ai/openapi upstreams"
}

function Get-WheelhouseHintSummary {
  param([object]$Service)

  $hints = @($Service.availableWheelVersions)
  if ($hints.Count -eq 0) {
    return ""
  }

  $missingPackageCount = @($hints | Where-Object { [string]$_.status -eq "missing-package" }).Count
  $versionMismatchCount = @($hints | Where-Object { [string]$_.status -eq "version-mismatch" }).Count
  $incompatibleWheelCount = @($hints | Where-Object { [string]$_.status -eq "incompatible-wheel" }).Count
  $noCompatibleTagCount = @($hints | Where-Object { [string]$_.status -eq "no-compatible-tags" }).Count
  $sampleHints = @(
    $hints |
      Where-Object { [string]$_.status -in @("version-mismatch", "incompatible-wheel", "no-compatible-tags") } |
      Select-Object -First 3 |
      ForEach-Object {
        $available = @($_.availableVersions) -join "/"
        $statusSuffix = if ([string]$_.status -in @("incompatible-wheel", "no-compatible-tags")) { " but no Python 3.13-compatible wheel tag" } else { "" }
        "$($_.package) needs $($_.requiredVersion), has $available$statusSuffix"
      }
  )

  $summaryParts = @()
  if ($missingPackageCount -gt 0) {
    $summaryParts += "$missingPackageCount absent packages"
  }
  if ($versionMismatchCount -gt 0) {
    $summaryParts += "$versionMismatchCount version mismatches"
  }
  if ($incompatibleWheelCount -gt 0) {
    $summaryParts += "$incompatibleWheelCount incompatible wheel tags"
  }
  if ($noCompatibleTagCount -gt 0) {
    $summaryParts += "$noCompatibleTagCount packages with no Python 3.13-compatible tags"
  }
  if ($sampleHints.Count -gt 0) {
    $summaryParts += "examples: $($sampleHints -join '; ')"
  }

  return ($summaryParts -join "; ")
}

function Join-HttpArtifactUrl {
  param(
    [string]$BaseUrl,
    [string]$RelativePath
  )

  if (-not $BaseUrl) {
    return ""
  }

  try {
    $baseUri = [System.Uri]::new($BaseUrl)
    return ([System.Uri]::new($baseUri, $RelativePath.TrimStart("/"))).AbsoluteUri
  }
  catch {
    return $BaseUrl
  }
}

$repoRoot = Resolve-RepoRoot
if (-not $OutputPath) {
  $OutputPath = Join-Path $repoRoot "build\release-audit.md"
}
$outputDir = Split-Path -Parent $OutputPath
if (-not (Test-Path -LiteralPath $outputDir -PathType Container)) {
  New-Item -ItemType Directory -Path $outputDir | Out-Null
}

$portableRoot = Join-Path $repoRoot "frontend\packages\electron-app\dist\win-portable"
$launcherPath = Join-Path $portableRoot "ShopRPA.cmd"
$archivePath = Join-Path $portableRoot "resources\python_core.7z"
$hashPath = Join-Path $portableRoot "resources\python_core.7z.sha256.txt"
$sevenZipPath = Join-Path $portableRoot "resources\7zr.exe"
$smokeResultPath = Join-Path $portableRoot "smoke-verification.json"
$portableRendererSmokeReportPath = Join-Path $repoRoot "build\portable-renderer-smoke\renderer-smoke-verification.json"
$portableRendererSmokeScreenshotPath = Join-Path $repoRoot "build\portable-renderer-smoke\renderer-smoke.png"
$workflowEditorSmokeReportPath = Join-Path $repoRoot "build\workflow-editor-smoke\workflow-editor-smoke-verification.json"
$workflowEditorSmokeScreenshotPath = Join-Path $repoRoot "build\workflow-editor-smoke\workflow-editor-smoke.png"
$backendOfflineSmokeReportPath = Join-Path $repoRoot "build\backend-offline-smoke\backend-offline-smoke-verification.json"
$backendOfflineSmokeScreenshotPath = Join-Path $repoRoot "build\backend-offline-smoke\backend-offline-smoke.png"
$engineRuntimeReportPath = Join-Path $repoRoot "build\engine-runtime-tests.json"
$javaBackendTestsReportPath = Join-Path $repoRoot "build\java-backend-tests.json"
$aiReqPath = Join-Path $repoRoot "build\python-backend-requirements\ai-service.requirements.txt"
$openapiReqPath = Join-Path $repoRoot "build\python-backend-requirements\openapi-service.requirements.txt"
$pythonWheelhouseManifestPath = Join-Path $repoRoot "build\python-backend-requirements\python-backend-wheelhouse-manifest.md"
$pythonWheelhousePreflightPath = Join-Path $repoRoot "build\python-backend-requirements\python-backend-wheelhouse-preflight.md"
$pythonWheelhouseMissingRequirementsPath = Join-Path $repoRoot "build\python-backend-requirements\python-backend-wheelhouse-missing.requirements.txt"
$pythonWheelhouseDownloadReportPath = Join-Path $repoRoot "build\python-backend-requirements\python-backend-wheelhouse-download-report.json"
$browserExtensionSmokeReportPath = Join-Path $repoRoot "build\browser-extension-smoke\browser-extension-smoke-report.json"
$browserExtensionStaticReportPath = Join-Path $repoRoot "build\browser-extension-static\browser-extension-static-report.json"
$frontendTypecheckReportPath = Join-Path $repoRoot "build\frontend-typecheck\frontend-typecheck-report.json"
$frontendTestsReportPath = Join-Path $repoRoot "build\frontend-tests\frontend-tests-report.json"
$browserExtensionContentJsdomReportPath = Join-Path $repoRoot "build\browser-extension-content-jsdom-tests\browser-extension-content-jsdom-tests-report.json"
$guiE2eReportPath = Join-Path $repoRoot "build\gui-browser-e2e-report.json"
$releaseFullVerificationReportPath = Join-Path $repoRoot "build\release-full-verification.json"
$releaseFullVerificationSummaryPath = Join-Path $repoRoot "build\release-full-verification.md"
$releaseHostRepairReportPath = Join-Path $repoRoot "build\release-host-repair-report.json"
$releaseHostRepairSummaryPath = Join-Path $repoRoot "build\release-host-repair-report.md"
$casdoorInitDataPath = Join-Path $repoRoot "docker\volumes\casdoor\init_data_dump.json"
$dockerComposePath = Join-Path $repoRoot "docker\docker-compose.yml"
$sourceConfigPath = Join-Path $repoRoot "resources\conf.yaml"
$doctorScript = Join-Path $repoRoot "scripts\doctor-release.ps1"
$repairHostScript = Join-Path $repoRoot "scripts\repair-release-host.ps1"
$setupPythonBackendsScript = Join-Path $repoRoot "scripts\setup-python-backends.ps1"
$rootPackageJsonPath = Join-Path $repoRoot "package.json"
$electronPackageJsonPath = Join-Path $repoRoot "frontend\packages\electron-app\package.json"
$electronPackageJson = Get-Content -LiteralPath $electronPackageJsonPath -Raw | ConvertFrom-Json
$electronVersion = [string]$electronPackageJson.version
$portableInstallerPath = Join-Path $repoRoot "frontend\packages\electron-app\dist\installers\ShopRPA-$electronVersion-portable-installer.zip"
$portableInstallerHashPath = "$portableInstallerPath.sha256.txt"
$portableInstallerVerifyPath = "$portableInstallerPath.verify.json"

$powershellExe = "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe"
$doctor = Invoke-Captured -FilePath $powershellExe -Arguments @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $doctorScript) -WorkingDirectory $repoRoot
$engineSourceOverlayStatus = Get-DoctorCheckStatus -DoctorOutput $doctor.Output -Name "engine source overlay sync"
$portablePythonArchiveSourceStatus = Get-DoctorCheckStatus -DoctorOutput $doctor.Output -Name "portable python archive source"
$generatedSourceArtifactsStatus = Get-DoctorCheckStatus -DoctorOutput $doctor.Output -Name "generated source artifacts"
$pythonBackendSetupScriptsStatus = Get-DoctorCheckStatus -DoctorOutput $doctor.Output -Name "python backend setup scripts"
$browserExtensionStaticStatus = Get-DoctorCheckStatus -DoctorOutput $doctor.Output -Name "browser extension static package"
$frontendTypecheckStatus = Get-DoctorCheckStatus -DoctorOutput $doctor.Output -Name "frontend typecheck"
$frontendTestsStatus = Get-DoctorCheckStatus -DoctorOutput $doctor.Output -Name "frontend unit tests"
$browserExtensionContentTestsStatus = Get-DoctorCheckStatus -DoctorOutput $doctor.Output -Name "browser extension content tests"
$browserExtensionContentJsdomStatus = Get-DoctorCheckStatus -DoctorOutput $doctor.Output -Name "browser extension content jsdom tests"
$aiServiceDockerfileLockInstallStatus = Get-DoctorCheckStatus -DoctorOutput $doctor.Output -Name "ai-service Dockerfile lock install"
$openapiServiceDockerfileLockInstallStatus = Get-DoctorCheckStatus -DoctorOutput $doctor.Output -Name "openapi-service Dockerfile lock install"
$repairHostStrictStatus = "Missing; release host repair script is unavailable"
$repairHostActions = @()
if (Test-Path -LiteralPath $repairHostScript -PathType Leaf) {
  $repairHostStrict = Invoke-Captured -FilePath $powershellExe -Arguments @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $repairHostScript, "-Strict") -WorkingDirectory $repoRoot
  $repairHostActions = @(Get-NextRequiredHostActions -Text $repairHostStrict.Output)
  if ($repairHostStrict.ExitCode -eq 0) {
    $repairHostStrictStatus = "Pass"
  }
  else {
    $blockedCheckNames = @(Get-NamedBlockedChecks -Text $repairHostStrict.Output)
    if ($blockedCheckNames.Count -gt 0) {
      $repairHostStrictStatus = "Blocked; $($blockedCheckNames -join ', ')"
    }
    else {
      $strictSummary = Get-LastMatchingLine -Text $repairHostStrict.Output -Patterns @(
        "^Strict mode failed because warnings remain:\s+\d+",
        "^Warnings remain:\s+\d+",
        "^Blocking failures:\s+\d+"
      )
      if (-not $strictSummary) {
        $strictSummary = Get-FirstUsefulLine -Text $repairHostStrict.Output
      }
      $repairHostStrictStatus = "Blocked; $strictSummary"
    }
  }
}
$pythonOfflineCacheStatus = "Missing; setup-python-backends script is unavailable"
if (Test-Path -LiteralPath $setupPythonBackendsScript -PathType Leaf) {
  $pythonOfflineCheck = Invoke-Captured -FilePath $powershellExe -Arguments @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $setupPythonBackendsScript, "-Offline", "-CheckOnly") -WorkingDirectory $repoRoot
  if ($pythonOfflineCheck.ExitCode -eq 0) {
    $pythonOfflineCacheStatus = "Pass; offline cache dry run completed"
  }
  else {
    $pythonOfflineSummary = Get-LastMatchingLine -Text $pythonOfflineCheck.Output -Patterns @(
      "uv dry run still needs downloads",
      "uv sync could not download packages",
      "package index access is blocked",
      "Would download\s+[1-9]\d*\s+packages?",
      "Python backend wheelhouse is incomplete",
      "uv sync failed"
    )
    if (-not $pythonOfflineSummary) {
      $pythonOfflineSummary = Get-FirstUsefulLine -Text $pythonOfflineCheck.Output
    }
    $pythonOfflineCacheStatus = "Blocked; $pythonOfflineSummary"
  }
}
$warnings = Get-WarningDetails -DoctorOutput $doctor.Output
$sourceBackendGateway = Read-YamlScalarValue -YamlPath $sourceConfigPath -Key "remote_addr"
$backendHealthStatus = Test-HttpArtifact -BaseUrl $sourceBackendGateway -RelativePath "/health" -Label "backend gateway health"
$authWordmarkStatus = Test-HttpArtifact -BaseUrl $sourceBackendGateway -RelativePath "/shoprpa-static/shoprpa-wordmark.svg" -Label "auth wordmark asset" -SourcePath (Join-Path $repoRoot "docker\volumes\nginx\static\shoprpa-wordmark.svg")
$authIconStatus = Test-HttpArtifact -BaseUrl $sourceBackendGateway -RelativePath "/shoprpa-static/shoprpa-icon.svg" -Label "auth icon asset" -SourcePath (Join-Path $repoRoot "docker\volumes\nginx\static\shoprpa-icon.svg")
$robotOpenApiWorkflowStatus = Test-RobotOpenApiWorkflowArtifact -RepoRoot $repoRoot
$openApiRobotServiceStatus = Test-OpenApiRobotServiceArtifact -RepoRoot $repoRoot
$openApiAdminRouteStatus = Test-OpenApiAdminRouteArtifact -RepoRoot $repoRoot
$nginxGatewayHealthSourceStatus = Test-NginxGatewayHealthArtifact -RepoRoot $repoRoot
$backendHealthUrl = Join-HttpArtifactUrl -BaseUrl $sourceBackendGateway -RelativePath "/health"
$backendRouteSmokeStatus = "Pass; required gateway service routes responded as expected"
$backendRouteWarning = @($warnings | Where-Object { $_ -like "backend service route smoke:*" } | Select-Object -First 1)
if ($backendRouteWarning) {
  $backendRouteDetail = $backendRouteWarning.Substring("backend service route smoke:".Length).Trim()
  $backendRoutePrefix = if ($backendRouteDetail -match "not reachable|원격 서버에 연결|connection refused|failed to connect") { "Blocked" } else { "Warn" }
  $backendRouteSmokeStatus = "$backendRoutePrefix; $backendRouteDetail"
}
$authWordmarkUrl = Join-HttpArtifactUrl -BaseUrl $sourceBackendGateway -RelativePath "/shoprpa-static/shoprpa-wordmark.svg"
$authIconUrl = Join-HttpArtifactUrl -BaseUrl $sourceBackendGateway -RelativePath "/shoprpa-static/shoprpa-icon.svg"
$archiveHash = ""
if (Test-Path -LiteralPath $archivePath -PathType Leaf) {
  $archiveHash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToUpperInvariant()
}
$expectedHash = ""
if (Test-Path -LiteralPath $hashPath -PathType Leaf) {
  $expectedHash = (Get-Content -LiteralPath $hashPath -Raw).Trim().ToUpperInvariant()
}
$requiredArchiveEntries = @(
  "Lib\site-packages\astronverse\browser_bridge\inject\backgroundInject.js",
  "Lib\site-packages\astronverse\browser_bridge\inject\contentInject.js"
)
$archiveInjectStatus = "Missing or not inspected; run corepack pnpm run verify:portable:host"
$archiveHasBrowserInject = $false
if ((Test-Path -LiteralPath $sevenZipPath -PathType Leaf) -and (Test-Path -LiteralPath $archivePath -PathType Leaf)) {
  $listing = Invoke-Captured -FilePath $sevenZipPath -Arguments @("l", "-ba", $archivePath) -WorkingDirectory $portableRoot
  if ($listing.ExitCode -eq 0) {
    $missingArchiveEntries = @($requiredArchiveEntries | Where-Object { $listing.Output -notmatch [regex]::Escape($_) })
    if ($missingArchiveEntries.Count -eq 0) {
      $archiveHasBrowserInject = $true
      $archiveInjectStatus = "Present"
    }
    else {
      $archiveInjectStatus = "Missing: $($missingArchiveEntries -join ', ')"
    }
  }
  else {
    $archiveInjectStatus = "Could not list archive; run corepack pnpm run verify:portable:host"
  }
}
$smokeStatus = "Missing; run corepack pnpm run verify:portable:host"
$smokeOk = $false
if (Test-Path -LiteralPath $smokeResultPath -PathType Leaf) {
  try {
    $smokeResult = Get-Content -LiteralPath $smokeResultPath -Raw | ConvertFrom-Json
    $smokeFreshnessProblem = Get-ReportFreshnessProblem -Report $smokeResult -PropertyName "generatedAt"
    $smokeRendererPath = [string]$smokeResult.rendererPath
    $smokeOk = (-not $smokeFreshnessProblem) -and
      ($smokeResult.ok -eq $true) -and
      ($smokeResult.packagedRuntime -eq $true) -and
      ([string]$smokeResult.appPath).EndsWith("resources\app.asar") -and
      ($smokeRendererPath.Contains("resources\renderer") -or $smokeRendererPath.Contains("resources\app.asar\out\renderer"))
    if ($smokeOk) {
      $smokeStatus = "Pass; app=$($smokeResult.appPath)"
    }
    elseif ($smokeFreshnessProblem) {
      $smokeStatus = "$smokeFreshnessProblem; rerun corepack pnpm run verify:portable:host"
    }
    else {
      $smokeStatus = "Invalid smoke result; rerun corepack pnpm run verify:portable:host"
    }
  }
  catch {
    $smokeStatus = "Unreadable smoke result; rerun corepack pnpm run verify:portable:host"
  }
}

$portableReady = (Test-Path -LiteralPath $launcherPath -PathType Leaf) -and
  (Test-Path -LiteralPath $archivePath -PathType Leaf) -and
  ($archiveHash -and ($archiveHash -eq $expectedHash)) -and
  $archiveHasBrowserInject -and
  $smokeOk
$browserExtensionStaticReady = $browserExtensionStaticStatus -like "PASS;*"
$browserExtensionContentJsdomReady = $browserExtensionContentJsdomStatus -like "PASS;*"
$browserExtensionSmokeReady = $false
$browserExtensionSmokeStatus = "Missing; run corepack pnpm run smoke:browser-extension"
if (Test-Path -LiteralPath $browserExtensionSmokeReportPath -PathType Leaf) {
  try {
    $browserExtensionSmokeReport = Get-Content -LiteralPath $browserExtensionSmokeReportPath -Raw | ConvertFrom-Json
    if ($browserExtensionSmokeReport.ok -eq $true) {
      $browserExtensionSmokeReady = $true
      $scenarioCount = @($browserExtensionSmokeReport.scenarios | Where-Object { $_.status -eq "PASS" }).Count
      $browserExtensionSmokeStatus = "Pass; $scenarioCount scenarios"
    }
    else {
      $browserExtensionSmokeStatus = "Blocked; $(Get-BrowserExtensionSmokeSummary -Report $browserExtensionSmokeReport)"
    }
  }
  catch {
    $browserExtensionSmokeStatus = "Unreadable; run corepack pnpm run smoke:browser-extension"
  }
}
$guiE2eReady = $false
$guiE2eStatus = "Missing; real interactive GUI automation and browser extension flows still require a capable desktop validation session"
if (Test-Path -LiteralPath $guiE2eReportPath -PathType Leaf) {
  $guiE2eVerifierPath = Join-Path $repoRoot "scripts\verify-gui-browser-e2e-report.ps1"
  $guiE2eVerification = Invoke-Captured -FilePath $powershellExe -Arguments @(
    "-NoProfile",
    "-ExecutionPolicy",
    "Bypass",
    "-File",
    $guiE2eVerifierPath,
    "-ReportPath",
    $guiE2eReportPath
  ) -WorkingDirectory $repoRoot
  if ($guiE2eVerification.ExitCode -eq 0) {
    $guiE2eReady = $true
    $guiE2eStatus = "Pass"
  }
  else {
    $guiE2eStatus = "Blocked; $(Get-GuiE2eProblemSummary -Text $guiE2eVerification.Output)"
  if (-not $guiE2eStatus) {
      $guiE2eStatus = "Report exists but verifier failed with exit code $($guiE2eVerification.ExitCode)"
    }
  }
}
$javaBackendTestsReady = $false
$javaBackendTestsStatus = "Missing; run corepack pnpm run test:java-backends"
$javaBackendMavenRepoBlocked = $false
if (Test-Path -LiteralPath $javaBackendTestsReportPath -PathType Leaf) {
  try {
    $javaBackendTestsReport = Get-Content -LiteralPath $javaBackendTestsReportPath -Raw | ConvertFrom-Json
    if ($javaBackendTestsReport.ok -eq $true) {
      $javaBackendTestsReady = $true
      $passedModules = @($javaBackendTestsReport.modules | Where-Object { $_.ok -eq $true }).Count
      $javaBackendTestsStatus = "Pass; $passedModules Java backend modules"
    }
    else {
      $moduleOutput = @(
        $javaBackendTestsReport.modules |
          ForEach-Object { [string]$_.outputTail } |
          Where-Object { $_ }
      ) -join "`n"
      if ($moduleOutput -match "repo\.maven\.apache\.org|Could not transfer artifact|Permission denied: getsockopt|Non-resolvable (parent|import) POM") {
        $javaBackendMavenRepoBlocked = $true
        $failedModuleNames = @(
          $javaBackendTestsReport.modules |
            Where-Object { $_.ok -ne $true } |
            ForEach-Object { [string]$_.name } |
            Where-Object { $_ }
        )
        $moduleSummary = if ($failedModuleNames.Count -gt 0) { "; modules=$($failedModuleNames -join ', ')" } else { "" }
        $javaBackendTestsStatus = "Blocked; Maven repository access is blocked (repo.maven.apache.org: Permission denied: getsockopt)$moduleSummary"
      }
      else {
        $failure = @($javaBackendTestsReport.failures | Where-Object { $_ } | Select-Object -First 3)
        if ($failure.Count -gt 0) {
          $javaBackendTestsStatus = "Blocked; $($failure -join '; ')"
        }
        else {
          $javaBackendTestsStatus = "Blocked; report has ok=false"
        }
      }
    }
  }
  catch {
    $javaBackendTestsStatus = "Unreadable; run corepack pnpm run test:java-backends"
  }
}
$doctorReady = ($doctor.ExitCode -eq 0) -and ($warnings.Count -eq 0)
$fullStackReady = $doctorReady -and $guiE2eReady -and $browserExtensionSmokeReady -and $javaBackendTestsReady -and ($repairHostStrictStatus -eq "Pass")
$portableStatus = if ($portableReady) { "Pass" } else { "Fail" }
$bugFixStatus = if ($fullStackReady) { "Pass" } else { "Partial" }
$handoffStatus = if ($fullStackReady) { "Pass" } elseif ($portableReady) { "Partial" } else { "Blocked" }
$allFeaturesStatus = if ($fullStackReady) { "Pass" } else { "Blocked" }
$launcherStatus = if (Test-Path -LiteralPath $launcherPath -PathType Leaf) { "Present" } else { "Missing" }
$archiveStatus = if (Test-Path -LiteralPath $archivePath -PathType Leaf) { "Present" } else { "Missing" }
$archiveHashStatus = if ($archiveHash -and ($archiveHash -eq $expectedHash)) { "Matches $archiveHash" } else { "Missing or mismatch" }
$aiReqStatus = if (Test-Path -LiteralPath $aiReqPath -PathType Leaf) { "Present" } else { "Missing; run corepack pnpm run export:python-backend-reqs" }
$openapiReqStatus = if (Test-Path -LiteralPath $openapiReqPath -PathType Leaf) { "Present" } else { "Missing; run corepack pnpm run export:python-backend-reqs" }
$pythonWheelhouseManifestStatus = if (Test-Path -LiteralPath $pythonWheelhouseManifestPath -PathType Leaf) { "Present" } else { "Missing; run corepack pnpm run export:python-backend-reqs" }
$repairHostStatus = "Missing"
if ((Test-Path -LiteralPath $repairHostScript -PathType Leaf) -and (Test-Path -LiteralPath $rootPackageJsonPath -PathType Leaf)) {
  $rootPackageJsonText = Get-Content -LiteralPath $rootPackageJsonPath -Raw
  if (($rootPackageJsonText -match '"repair:release-host"') -and
    ($rootPackageJsonText -match '"repair:release-host:strict"') -and
    ($rootPackageJsonText -match '"repair:release-host:apply"')) {
    $repairHostStatus = "Present; corepack pnpm run repair:release-host; strict=corepack pnpm run repair:release-host:strict; apply=corepack pnpm run repair:release-host:apply"
  }
  else {
    $repairHostStatus = "Script present, but package.json command is missing"
  }
}
$releaseHostRepairReportStatus = "Missing; run corepack pnpm run repair:release-host:strict"
$releaseHostRepairReport = $null
if (Test-Path -LiteralPath $releaseHostRepairReportPath -PathType Leaf) {
  try {
    $releaseHostRepairReport = Get-Content -LiteralPath $releaseHostRepairReportPath -Raw | ConvertFrom-Json
    $repairReportFreshnessProblem = Get-ReportFreshnessProblem -Report $releaseHostRepairReport -PropertyName "generatedAt"
    $repairReportStatus = [string]$releaseHostRepairReport.status
    if ($repairReportFreshnessProblem) {
      $releaseHostRepairReportStatus = "Blocked; $repairReportFreshnessProblem"
    }
    elseif ($repairReportStatus -notin @("PASS", "WARN", "BLOCKED")) {
      $releaseHostRepairReportStatus = "Blocked; unexpected status=$repairReportStatus"
    }
    else {
      $releaseHostRepairReportStatus = "Present; status=$repairReportStatus; generated=$($releaseHostRepairReport.generatedAt)"
    }
  }
  catch {
    $releaseHostRepairReportStatus = "Unreadable; run corepack pnpm run repair:release-host:strict"
  }
}
$releaseHostRepairSummaryStatus = "Missing; run corepack pnpm run repair:release-host:strict"
if (Test-Path -LiteralPath $releaseHostRepairSummaryPath -PathType Leaf) {
  try {
    $repairSummaryText = Get-Content -LiteralPath $releaseHostRepairSummaryPath -Raw
    $repairSummaryStatusMatch = [regex]::Match($repairSummaryText, "(?m)^Status:\s*(PASS|WARN|BLOCKED)\s*$")
    $repairSummaryGeneratedMatch = [regex]::Match($repairSummaryText, "(?m)^Generated:\s*(.+?)\s*$")
    if (-not $repairSummaryStatusMatch.Success) {
      $releaseHostRepairSummaryStatus = "Blocked; summary status line is missing"
    }
    elseif (-not $repairSummaryGeneratedMatch.Success) {
      $releaseHostRepairSummaryStatus = "Blocked; summary generated line is missing"
    }
    else {
      $repairSummaryStatus = $repairSummaryStatusMatch.Groups[1].Value
      $repairSummaryGenerated = $repairSummaryGeneratedMatch.Groups[1].Value.Trim()
      $repairSummaryFreshnessProblem = Get-ReportFreshnessProblem -Report ([PSCustomObject]@{ generatedAt = $repairSummaryGenerated }) -PropertyName "generatedAt"
      if ($repairSummaryFreshnessProblem) {
        $releaseHostRepairSummaryStatus = "Blocked; $repairSummaryFreshnessProblem"
      }
      elseif ($releaseHostRepairReport -and ($repairSummaryStatus -ne [string]$releaseHostRepairReport.status)) {
        $releaseHostRepairSummaryStatus = "Blocked; status=$repairSummaryStatus but report expects $($releaseHostRepairReport.status)"
      }
      else {
        $releaseHostRepairSummaryStatus = "Present; status=$repairSummaryStatus; generated=$repairSummaryGenerated"
      }
    }
  }
  catch {
    $releaseHostRepairSummaryStatus = "Unreadable; run corepack pnpm run repair:release-host:strict"
  }
}
$pythonComposeHealthcheckStatus = "Missing; docker compose file was not found"
$javaComposeHealthcheckStatus = "Missing; docker compose file was not found"
if (Test-Path -LiteralPath $dockerComposePath -PathType Leaf) {
  $composeText = Get-Content -LiteralPath $dockerComposePath -Raw
  if (($composeText -match "127\.0\.0\.1:8010/health") -and
    ($composeText -match "127\.0\.0\.1:8020/health") -and
    ($composeText -match "(?s)openresty-nginx:\s.*?ai-service:\s*\r?\n\s*condition:\s*service_healthy") -and
    ($composeText -match "(?s)openresty-nginx:\s.*?openapi-service:\s*\r?\n\s*condition:\s*service_healthy")) {
    $pythonComposeHealthcheckStatus = "Pass; ai/openapi healthchecks are defined and nginx waits for them"
  }
  else {
    $pythonComposeHealthcheckStatus = "Warn; Python service healthcheck gating is incomplete"
  }

  if (($composeText -match "com\.iflytek\.rpa\.resource\.common\.health\.HealthcheckProbe") -and
    ($composeText -match "com\.iflytek\.rpa\.common\.health\.HealthcheckProbe") -and
    ($composeText -match "com\.iflytek\.rpa\.auth\.health\.HealthcheckProbe") -and
    ($composeText -match "(?s)openresty-nginx:\s.*?resource-service:\s*\r?\n\s*condition:\s*service_healthy") -and
    ($composeText -match "(?s)openresty-nginx:\s.*?robot-service:\s*\r?\n\s*condition:\s*service_healthy") -and
    ($composeText -match "(?s)openresty-nginx:\s.*?rpa-auth:\s*\r?\n\s*condition:\s*service_healthy")) {
    $javaComposeHealthcheckStatus = "Pass; Java service healthchecks are defined and nginx waits for them"
  }
  else {
    $javaComposeHealthcheckStatus = "Warn; Java service healthcheck gating is incomplete"
  }
}
$pythonWheelhousePreflightStatus = "Missing; run corepack pnpm run setup:python-backends -- -Wheelhouse <path> -Offline -CheckOnly"
if (Test-Path -LiteralPath $pythonWheelhousePreflightPath -PathType Leaf) {
  try {
    $pythonWheelhousePreflightJsonPath = [System.IO.Path]::ChangeExtension($pythonWheelhousePreflightPath, ".json")
    $pythonWheelhousePreflight = Get-Content -LiteralPath $pythonWheelhousePreflightJsonPath -Raw | ConvertFrom-Json
    if ($pythonWheelhousePreflight.ok -eq $true) {
      $pythonWheelhousePreflightStatus = "Pass"
    }
    else {
      $blockedServices = @($pythonWheelhousePreflight.services | Where-Object { $_.ok -ne $true })
      $serviceSummaries = @(
        $blockedServices | ForEach-Object {
          $hintSummary = Get-WheelhouseHintSummary -Service $_
          if ($hintSummary) {
            $summary = "$($_.service): $($_.missingLockedWheels.Count) missing compatible locked wheels ($hintSummary)"
          }
          else {
            $summary = "$($_.service): $($_.missingLockedWheels.Count) missing compatible locked wheels"
          }
          if ([string]$_.missingRequirementsFile) {
            $summary = "$summary; missing requirements=$($_.missingRequirementsFile)"
          }
          $summary
        }
      )
      $pythonWheelhousePreflightStatus = "Blocked; $($serviceSummaries -join '; ')"
    }
  }
  catch {
    $pythonWheelhousePreflightStatus = "Present but unreadable; rerun corepack pnpm run setup:python-backends -- -Wheelhouse <path> -Offline -CheckOnly"
  }
}
$pythonWheelhouseMissingRequirementsStatus = "Missing; run corepack pnpm run setup:python-backends -- -Wheelhouse <path> -Offline -CheckOnly"
if (Test-Path -LiteralPath $pythonWheelhouseMissingRequirementsPath -PathType Leaf) {
  $missingRequirementPins = @(
    Get-Content -LiteralPath $pythonWheelhouseMissingRequirementsPath |
      ForEach-Object { $_.Trim() } |
      Where-Object { $_ -and -not $_.StartsWith("#") }
  )
  if ($missingRequirementPins.Count -gt 0) {
    $pythonWheelhouseMissingRequirementsStatus = "Present; $($missingRequirementPins.Count) exact pins still need wheels"
  }
  else {
    $pythonWheelhouseMissingRequirementsStatus = "Present; no missing pins listed"
  }
}
$pythonWheelhouseDownloadReportStatus = "Missing; run corepack pnpm run download:python-backend-wheelhouse-missing -- -Wheelhouse <path> -ContinueOnError on an online prep host"
if (Test-Path -LiteralPath $pythonWheelhouseDownloadReportPath -PathType Leaf) {
  try {
    $pythonWheelhouseDownloadReport = Get-Content -LiteralPath $pythonWheelhouseDownloadReportPath -Raw | ConvertFrom-Json
    $downloadGeneratedAt = [string]$pythonWheelhouseDownloadReport.generatedAt
    $downloadRequirementCount = [int]$pythonWheelhouseDownloadReport.requirementsCount
    $downloadStats = ""
    if ($pythonWheelhouseDownloadReport.PSObject.Properties.Name -contains "failedCount") {
      $downloadStats = "; downloaded=$($pythonWheelhouseDownloadReport.downloadedCount); failed=$($pythonWheelhouseDownloadReport.failedCount); blocked=$($pythonWheelhouseDownloadReport.blockedCount)"
    }
    if ($pythonWheelhouseDownloadReport.ok -eq $true) {
      $pythonWheelhouseDownloadReportStatus = "Pass; requirements=$downloadRequirementCount$downloadStats; generated=$downloadGeneratedAt"
    }
    elseif ($pythonWheelhouseDownloadReport.blockedByPolicy -eq $true) {
      $pythonWheelhouseDownloadReportStatus = "Blocked; host network or endpoint policy blocked package index access; requirements=$downloadRequirementCount$downloadStats; generated=$downloadGeneratedAt"
    }
    else {
      $downloadStatus = [string]$pythonWheelhouseDownloadReport.status
      $downloadStep = [string]$pythonWheelhouseDownloadReport.step
      $pythonWheelhouseDownloadReportStatus = "Failed; status=$downloadStatus; step=$downloadStep; requirements=$downloadRequirementCount$downloadStats; generated=$downloadGeneratedAt"
    }
  }
  catch {
    $pythonWheelhouseDownloadReportStatus = "Present but unreadable; rerun corepack pnpm run download:python-backend-wheelhouse-missing -- -Wheelhouse <path> -ContinueOnError"
  }
}
$casdoorSeedStatus = "Missing"
if (Test-Path -LiteralPath $casdoorInitDataPath -PathType Leaf) {
  try {
    $casdoorRaw = Get-Content -LiteralPath $casdoorInitDataPath -Raw
    $casdoorJson = $casdoorRaw | ConvertFrom-Json
    $forbiddenSeedText = @("example-org", "example-app", "example-user", "example-role", "example.com", "admin@example.com", "Example Inc.", "dc=example", "예시사용자", "예시역할", "예시인증서", "New User", "New Role", "18888888888")
    $forbiddenHits = @($forbiddenSeedText | Where-Object { $casdoorRaw.Contains($_) })
    $seedRecords = @($casdoorJson.records).Count
    $seedTokens = @($casdoorJson.tokens).Count
    $seedSessions = @($casdoorJson.sessions).Count
    if (($forbiddenHits.Count -eq 0) -and (($seedRecords + $seedTokens + $seedSessions) -eq 0)) {
      $casdoorSeedStatus = "Clean; users=$(@($casdoorJson.users).Count), roles=$(@($casdoorJson.roles).Count), records/tokens/sessions=0"
    }
    else {
      $casdoorSeedStatus = "Needs cleanup; forbidden=$($forbiddenHits -join ', '), records=$seedRecords, tokens=$seedTokens, sessions=$seedSessions"
    }
  }
  catch {
    $casdoorSeedStatus = "Unreadable"
  }
}
$engineRuntimeStatus = "Missing; run corepack pnpm run test:engine-runtime"
if (Test-Path -LiteralPath $engineRuntimeReportPath -PathType Leaf) {
  try {
    $engineRuntimeReport = Get-Content -LiteralPath $engineRuntimeReportPath -Raw | ConvertFrom-Json
    if ($engineRuntimeReport.ok -eq $true) {
      $engineRuntimeSuites = @($engineRuntimeReport.suites | ForEach-Object { [string]$_.name })
      $timedOutEngineRuntimeSuites = @($engineRuntimeReport.suites | Where-Object { $_.timedOut -eq $true } | ForEach-Object { [string]$_.name })
      $badExitEngineRuntimeSuites = @($engineRuntimeReport.suites | Where-Object { [int]$_.exitCode -ne 0 } | ForEach-Object { [string]$_.name })
      if ($timedOutEngineRuntimeSuites.Count -gt 0) {
        $engineRuntimeStatus = "Timed out; suites=$($timedOutEngineRuntimeSuites -join ', ')"
      }
      elseif ($badExitEngineRuntimeSuites.Count -gt 0) {
        $engineRuntimeStatus = "Failed; nonzero exit suites=$($badExitEngineRuntimeSuites -join ', ')"
      }
      else {
        $engineRuntimeStatus = "Pass; $($engineRuntimeReport.totalTests) tests, $($engineRuntimeReport.totalSkipped) skipped; suites=$($engineRuntimeSuites -join ', ')"
      }
    }
    else {
      $engineRuntimeStatus = "Report has ok=false"
    }
  }
  catch {
    $engineRuntimeStatus = "Unreadable; rerun corepack pnpm run test:engine-runtime"
  }
}
$releaseFullVerificationStatus = "Missing; run corepack pnpm run verify:release:full"
$releaseFullVerificationReport = $null
if (Test-Path -LiteralPath $releaseFullVerificationReportPath -PathType Leaf) {
  try {
    $releaseFullVerificationReport = Get-Content -LiteralPath $releaseFullVerificationReportPath -Raw | ConvertFrom-Json
    $failedFullSteps = @($releaseFullVerificationReport.steps | Where-Object { ($_.ok -ne $true) -or ($_.timedOut -eq $true) })
    $auditRefreshOk = $releaseFullVerificationReport.auditRefresh -and ($releaseFullVerificationReport.auditRefresh.ok -eq $true) -and ($releaseFullVerificationReport.auditRefresh.timedOut -ne $true)
    $fullVerificationFreshnessProblem = Get-ReportFreshnessProblem -Report $releaseFullVerificationReport -PropertyName "generatedAt"
    $auditRefreshFreshnessProblem = if ($releaseFullVerificationReport.auditRefresh) {
      Get-ReportFreshnessProblem -Report $releaseFullVerificationReport.auditRefresh -PropertyName "finishedAt"
    }
    else {
      ""
    }
    $requiredFullSteps = @(
      [PSCustomObject]@{ name = "portable release gate"; command = "corepack pnpm run verify:release" },
      [PSCustomObject]@{ name = "browser extension content tests"; command = "corepack pnpm run test:browser-extension-content" },
      [PSCustomObject]@{ name = "browser extension content jsdom tests"; command = "corepack pnpm run test:browser-extension-content:jsdom" },
      [PSCustomObject]@{ name = "browser extension runtime smoke"; command = "corepack pnpm run smoke:browser-extension" },
      [PSCustomObject]@{ name = "browser extension smoke report"; command = "corepack pnpm run verify:browser-extension-smoke" },
      [PSCustomObject]@{ name = "interactive GUI E2E report"; command = "corepack pnpm run verify:gui-e2e" },
      [PSCustomObject]@{ name = "java backend Maven tests"; command = "corepack pnpm run test:java-backends" },
      [PSCustomObject]@{ name = "host strict repair preflight"; command = "corepack pnpm run repair:release-host:strict" }
    )
    $actualFullStepNames = @($releaseFullVerificationReport.steps | ForEach-Object { [string]$_.name })
    $missingFullStepNames = @($requiredFullSteps | Where-Object { $_.name -notin $actualFullStepNames } | ForEach-Object { $_.name })
    $mismatchedFullStepCommands = @(
      foreach ($requiredStep in $requiredFullSteps) {
        $actualStep = @($releaseFullVerificationReport.steps | Where-Object { [string]$_.name -eq $requiredStep.name } | Select-Object -First 1)
        if ($actualStep.Count -gt 0 -and ([string]$actualStep[0].command -ne $requiredStep.command)) {
          "$($requiredStep.name): expected '$($requiredStep.command)'"
        }
      }
    )
    if ($releaseFullVerificationReport.schemaVersion -ne 2) {
      $releaseFullVerificationStatus = "Blocked; schemaVersion must be 2"
    }
    elseif ([string]$releaseFullVerificationReport.acceptanceContractVersion -ne "shoprpa-full-release-2026-05-v3") {
      $releaseFullVerificationStatus = "Blocked; acceptanceContractVersion must be shoprpa-full-release-2026-05-v3"
    }
    elseif ($fullVerificationFreshnessProblem) {
      $releaseFullVerificationStatus = "Blocked; $fullVerificationFreshnessProblem"
    }
    elseif ($auditRefreshFreshnessProblem) {
      $releaseFullVerificationStatus = "Blocked; audit refresh $auditRefreshFreshnessProblem"
    }
    elseif ($missingFullStepNames.Count -gt 0) {
      $releaseFullVerificationStatus = "Blocked; missing steps=$($missingFullStepNames -join ', ')"
    }
    elseif ($mismatchedFullStepCommands.Count -gt 0) {
      $releaseFullVerificationStatus = "Blocked; mismatched step commands=$($mismatchedFullStepCommands -join '; ')"
    }
    elseif ($releaseFullVerificationReport.ok -eq $true) {
      $releaseFullVerificationStatus = "Pass; $(@($releaseFullVerificationReport.steps).Count) steps"
    }
    elseif ($failedFullSteps.Count -gt 0) {
      $failedFullStepNames = @($failedFullSteps | ForEach-Object { [string]$_.name })
      $releaseFullVerificationStatus = "Blocked; failed steps=$($failedFullStepNames -join ', ')"
    }
    elseif (-not $auditRefreshOk) {
      $releaseFullVerificationStatus = "Blocked; audit refresh is missing or failed"
    }
    else {
      $releaseFullVerificationStatus = "Blocked; report has ok=false"
    }
  }
  catch {
    $releaseFullVerificationStatus = "Unreadable; rerun corepack pnpm run verify:release:full"
  }
}
$releaseFullVerificationSummaryStatus = "Missing; run corepack pnpm run verify:release:full"
if (Test-Path -LiteralPath $releaseFullVerificationSummaryPath -PathType Leaf) {
  try {
    $summaryText = Get-Content -LiteralPath $releaseFullVerificationSummaryPath -Raw
    $statusMatch = [regex]::Match($summaryText, "(?m)^Status:\s*(PASS|BLOCKED)\s*$")
    $refreshedMatch = [regex]::Match($summaryText, "(?m)^Refreshed:\s*(.+?)\s*$")
    if (-not $statusMatch.Success) {
      $releaseFullVerificationSummaryStatus = "Blocked; summary status line is missing"
    }
    elseif (-not $refreshedMatch.Success) {
      $releaseFullVerificationSummaryStatus = "Blocked; summary refreshed line is missing"
    }
    else {
      $summaryStatus = $statusMatch.Groups[1].Value
      $summaryRefreshed = $refreshedMatch.Groups[1].Value.Trim()
      $summaryFreshnessProblem = Get-ReportFreshnessProblem -Report ([PSCustomObject]@{ refreshedAt = $summaryRefreshed }) -PropertyName "refreshedAt"
      $expectedSummaryStatus = if ($releaseFullVerificationReport -and ($releaseFullVerificationReport.ok -eq $true)) { "PASS" } else { "BLOCKED" }
      if ($summaryFreshnessProblem) {
        $releaseFullVerificationSummaryStatus = "Blocked; $summaryFreshnessProblem"
      }
      elseif ($releaseFullVerificationReport -and ($summaryStatus -ne $expectedSummaryStatus)) {
        $releaseFullVerificationSummaryStatus = "Blocked; status=$summaryStatus but report expects $expectedSummaryStatus"
      }
      else {
        $releaseFullVerificationSummaryStatus = "Present; status=$summaryStatus; refreshed=$summaryRefreshed"
      }
    }
  }
  catch {
    $releaseFullVerificationSummaryStatus = "Unreadable; rerun corepack pnpm run verify:release:full"
  }
}
$portableInstallerStatus = "Missing; run corepack pnpm run build:portable-installer"
$portableInstallerReady = $false
if (Test-Path -LiteralPath $portableInstallerPath -PathType Leaf) {
  $portableInstallerStatus = "Present"
  if (Test-Path -LiteralPath $portableInstallerHashPath -PathType Leaf) {
    $installerHash = (Get-FileHash -LiteralPath $portableInstallerPath -Algorithm SHA256).Hash.ToUpperInvariant()
    $expectedInstallerHash = (Get-Content -LiteralPath $portableInstallerHashPath -Raw).Trim().ToUpperInvariant()
    if ($installerHash -eq $expectedInstallerHash) {
      $portableInstallerStatus = "Hash matches $installerHash"
    }
    else {
      $portableInstallerStatus = "Hash mismatch"
    }
  }
  if (Test-Path -LiteralPath $portableInstallerVerifyPath -PathType Leaf) {
    try {
      $installerVerification = Get-Content -LiteralPath $portableInstallerVerifyPath -Raw | ConvertFrom-Json
      $installerVerificationFreshnessProblem = Get-ReportFreshnessProblem -Report $installerVerification -PropertyName "verifiedAt"
      if ($installerVerificationFreshnessProblem) {
        $portableInstallerStatus = "$portableInstallerStatus; $installerVerificationFreshnessProblem; rerun verify:portable-installer"
      }
      elseif ($installerVerification.ok -eq $true) {
        $portableInstallerStatus = "$portableInstallerStatus; verified"
        $installerVerificationChecks = @()
        $missingInstallerVerificationChecks = @()
        foreach ($checkName in @("installerManifest", "installerReadme", "installedManifest", "smoke", "uninstall")) {
          if ([string]$installerVerification.$checkName) {
            $installerVerificationChecks += "$checkName=$($installerVerification.$checkName)"
          }
          if ([string]$installerVerification.$checkName -ne "pass") {
            $missingInstallerVerificationChecks += $checkName
          }
        }
        if ($installerVerificationChecks.Count -gt 0) {
          $portableInstallerStatus = "$portableInstallerStatus; $($installerVerificationChecks -join ', ')"
        }
        if ($missingInstallerVerificationChecks.Count -eq 0) {
          $portableInstallerReady = $true
        }
        else {
          $portableInstallerStatus = "$portableInstallerStatus; missing required installer checks: $($missingInstallerVerificationChecks -join ', ')"
        }
      }
      else {
        $portableInstallerStatus = "$portableInstallerStatus; verification reports failure"
      }
    }
    catch {
      $portableInstallerStatus = "$portableInstallerStatus; verification unreadable"
    }
  }
  else {
    $portableInstallerStatus = "$portableInstallerStatus; verification missing"
  }
}
$portableRendererSmokeStatus = "Missing; run corepack pnpm run smoke:portable-renderer"
$portableRendererSmokeReady = $false
if (Test-Path -LiteralPath $portableRendererSmokeReportPath -PathType Leaf) {
  try {
    $portableRendererSmokeReport = Get-Content -LiteralPath $portableRendererSmokeReportPath -Raw | ConvertFrom-Json
    $portableRendererSmokeFreshnessProblem = Get-ReportFreshnessProblem -Report $portableRendererSmokeReport -PropertyName "generatedAt"
    if ($portableRendererSmokeFreshnessProblem) {
      $portableRendererSmokeStatus = "Blocked; $portableRendererSmokeFreshnessProblem"
    }
    elseif ($portableRendererSmokeReport.ok -eq $true -and $portableRendererSmokeReport.rendererSmoke -eq $true) {
      if (Test-Path -LiteralPath $portableRendererSmokeScreenshotPath -PathType Leaf) {
        $screenshotSize = (Get-Item -LiteralPath $portableRendererSmokeScreenshotPath).Length
        if ($screenshotSize -gt 0) {
          $portableRendererSmokeReady = $true
          $portableRendererSmokeStatus = "Pass; screenshot=$portableRendererSmokeScreenshotPath ($screenshotSize bytes)"
        }
        else {
          $portableRendererSmokeStatus = "Blocked; screenshot is empty"
        }
      }
      else {
        $portableRendererSmokeStatus = "Blocked; screenshot is missing"
      }
    }
    else {
      $rendererSmokeError = [string]$portableRendererSmokeReport.error
      $rendererFailureClass = [string]$portableRendererSmokeReport.rendererFailureClass
      if ([string]::IsNullOrWhiteSpace($rendererFailureClass)) {
        $rendererFailureClass = "renderer-smoke-failed"
      }
      $rendererEvidence = ""
      if ($portableRendererSmokeReport.hostPolicyEvidence) {
        $rendererEvidence = Normalize-ReportText ([string](@($portableRendererSmokeReport.hostPolicyEvidence) | Select-Object -First 1))
        if ($rendererEvidence.Length -gt 220) {
          $rendererEvidence = $rendererEvidence.Substring(0, 217) + "..."
        }
      }
      if ($portableRendererSmokeReport.hostPolicyBlocked -eq $true) {
        $hostSummary = "Electron/Chromium renderer navigation is blocked by host sandbox/profile policy; failureClass=$rendererFailureClass"
        if ($rendererEvidence) {
          $hostSummary = "$hostSummary; evidence=$rendererEvidence"
        }
        if ($rendererSmokeError) {
          $portableRendererSmokeStatus = "Blocked; $hostSummary; $rendererSmokeError"
        }
        else {
          $portableRendererSmokeStatus = "Blocked; $hostSummary"
        }
      }
      elseif ($portableRendererSmokeReport.rendererCommitBlocked -eq $true) {
        $protocolSummary = "rpa://localhost/boot.html served HTTP 200, but Chromium did not commit the main-frame navigation"
        if ($portableRendererSmokeReport.rendererFileFallbackAttempted -eq $true) {
          $protocolSummary = "$protocolSummary; file:// fallback also did not commit"
        }
        $protocolSummary = "$protocolSummary; failureClass=$rendererFailureClass"
        if ($rendererSmokeError) {
          $portableRendererSmokeStatus = "Blocked; $protocolSummary; $rendererSmokeError"
        }
        else {
          $portableRendererSmokeStatus = "Blocked; $protocolSummary"
        }
      }
      elseif ($rendererSmokeError) {
        $portableRendererSmokeStatus = "Blocked; $rendererSmokeError"
      }
      else {
        $portableRendererSmokeStatus = "Blocked; report has ok=false"
      }
    }
  }
  catch {
    $portableRendererSmokeStatus = "Unreadable; rerun portable verification"
  }
}

$workflowEditorSmokeStatus = "Missing; run corepack pnpm run smoke:workflow-editor"
$workflowEditorSmokeReady = $false
if (Test-Path -LiteralPath $workflowEditorSmokeReportPath -PathType Leaf) {
  try {
    $workflowEditorSmokeReport = Get-Content -LiteralPath $workflowEditorSmokeReportPath -Raw | ConvertFrom-Json
    $workflowEditorSmokeFreshnessProblem = Get-ReportFreshnessProblem -Report $workflowEditorSmokeReport -PropertyName "generatedAt"
    if ($workflowEditorSmokeFreshnessProblem) {
      $workflowEditorSmokeStatus = "Blocked; $workflowEditorSmokeFreshnessProblem"
    }
    elseif (
      $workflowEditorSmokeReport.ok -eq $true -and
      $workflowEditorSmokeReport.workflowEditorSmoke -eq $true -and
      $workflowEditorSmokeReport.workflowEditorSmokeCreateSave.ok -eq $true -and
      $workflowEditorSmokeReport.workflowEditorSmokeReloadEdit.ok -eq $true
    ) {
      if (Test-Path -LiteralPath $workflowEditorSmokeScreenshotPath -PathType Leaf) {
        $workflowScreenshotSize = (Get-Item -LiteralPath $workflowEditorSmokeScreenshotPath).Length
        if ($workflowScreenshotSize -gt 0) {
          $workflowEditorSmokeReady = $true
          $workflowEditorSmokeStatus = "Pass; create/save and reload/edit persisted workflow nodes; screenshot=$workflowEditorSmokeScreenshotPath ($workflowScreenshotSize bytes)"
        }
        else {
          $workflowEditorSmokeStatus = "Blocked; screenshot is empty"
        }
      }
      else {
        $workflowEditorSmokeStatus = "Blocked; screenshot is missing"
      }
    }
    elseif ($workflowEditorSmokeReport.error) {
      $workflowEditorSmokeStatus = "Blocked; $($workflowEditorSmokeReport.error)"
    }
    else {
      $workflowEditorSmokeStatus = "Blocked; report has ok=false"
    }
  }
  catch {
    $workflowEditorSmokeStatus = "Unreadable; rerun corepack pnpm run smoke:workflow-editor"
  }
}

$backendOfflineSmokeStatus = "Missing; run corepack pnpm run smoke:backend-offline"
$backendOfflineSmokeReady = $false
if (Test-Path -LiteralPath $backendOfflineSmokeReportPath -PathType Leaf) {
  try {
    $backendOfflineSmokeReport = Get-Content -LiteralPath $backendOfflineSmokeReportPath -Raw | ConvertFrom-Json
    $backendOfflineSmokeFreshnessProblem = Get-ReportFreshnessProblem -Report $backendOfflineSmokeReport -PropertyName "generatedAt"
    $backendOfflineErrorText = [string]$backendOfflineSmokeReport.backendOfflineSmokeResult.errorText
    if ($backendOfflineSmokeFreshnessProblem) {
      $backendOfflineSmokeStatus = "Blocked; $backendOfflineSmokeFreshnessProblem"
    }
    elseif (
      $backendOfflineSmokeReport.ok -eq $true -and
      $backendOfflineSmokeReport.backendOfflineSmoke -eq $true -and
      $backendOfflineSmokeReport.backendOfflineSmokeResult.ok -eq $true -and
      $backendOfflineSmokeReport.backendOfflineSmokeResult.inlineErrorVisible -eq $true -and
      $backendOfflineErrorText.Contains("서버") -and
      $backendOfflineErrorText.Contains("확인")
    ) {
      if (Test-Path -LiteralPath $backendOfflineSmokeScreenshotPath -PathType Leaf) {
        $backendOfflineScreenshotSize = (Get-Item -LiteralPath $backendOfflineSmokeScreenshotPath).Length
        if ($backendOfflineScreenshotSize -gt 0) {
          $backendOfflineSmokeReady = $true
          $backendOfflineSmokeStatus = "Pass; backend-unavailable login error state is visible and actionable; screenshot=$backendOfflineSmokeScreenshotPath ($backendOfflineScreenshotSize bytes)"
        }
        else {
          $backendOfflineSmokeStatus = "Blocked; screenshot is empty"
        }
      }
      else {
        $backendOfflineSmokeStatus = "Blocked; screenshot is missing"
      }
    }
    elseif ($backendOfflineSmokeReport.error) {
      $backendOfflineSmokeStatus = "Blocked; $($backendOfflineSmokeReport.error)"
    }
    else {
      $backendOfflineSmokeStatus = "Blocked; report has ok=false or weak error text"
    }
  }
  catch {
    $backendOfflineSmokeStatus = "Unreadable; rerun corepack pnpm run smoke:backend-offline"
  }
}

$verifyReleaseGateStatus = if ($portableReady -and $browserExtensionStaticReady -and $browserExtensionContentJsdomReady -and ($engineRuntimeStatus -like "Pass;*") -and $portableInstallerReady) { "Pass" } else { "Blocked" }
$doctorGateStatus = if ($doctorReady) { "Pass" } elseif ($doctor.ExitCode -eq 0) { "Warn" } else { "Blocked" }
$strictAuditGateStatus = if ($fullStackReady) { "Pass" } else { "Blocked" }
$fullReleaseGateStatus = if (($verifyReleaseGateStatus -eq "Pass") -and $doctorReady -and $guiE2eReady -and $browserExtensionSmokeReady -and $javaBackendTestsReady -and ($repairHostStrictStatus -eq "Pass")) { "Pass" } else { "Blocked" }

$lines = New-Object System.Collections.Generic.List[string]
Add-Line $lines "# ShopRPA Release Audit"
Add-Line $lines ""
Add-Line $lines "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')"
Add-Line $lines "Repository: $repoRoot"
Add-Line $lines ""
Add-Line $lines "## Objective"
Add-Line $lines ""
Add-Line $lines "1. Fix existing bugs."
Add-Line $lines "2. Leave the product in an executable state."
Add-Line $lines "3. Raise visible quality enough for paid handoff."
Add-Line $lines "4. Ensure every feature works normally."
Add-Line $lines ""
Add-Line $lines "## Current Verdict"
Add-Line $lines ""
if ($fullStackReady) {
  Add-Line $lines "COMPLETE: full commercial acceptance gates passed."
}
elseif ($portableReady) {
  Add-Line $lines "PARTIAL: portable desktop package is ready, but full-stack commercial acceptance is not complete."
}
else {
  Add-Line $lines "BLOCKED: portable desktop package is not fully verified."
}
Add-Line $lines ""
Add-Line $lines "## Prompt-To-Artifact Checklist"
Add-Line $lines ""
Add-Line $lines "| Requirement | Evidence | Status |"
Add-Line $lines "| --- | --- | --- |"
Add-Line $lines "| Existing bugs fixed | Static parse/build gates, text hygiene, source syntax, runtime checks, doctor checks, host strict checks, browser runtime smoke, and GUI E2E evidence are the required automated evidence. | $bugFixStatus |"
Add-Line $lines "| Executable form | ``$launcherPath`` | $portableStatus |"
Add-Line $lines "| Paid-quality handoff | Requires verified portable package, verified PowerShell installer zip, zero release doctor warnings, host strict pass, Java backend Maven tests, browser runtime smoke, and GUI E2E evidence. | $handoffStatus |"
Add-Line $lines "| All features work normally | Requires Python backend runtime, Java/Maven tests, Docker engine, and GUI automation validation. NSIS remains host-limited only if that installer format is required. | $allFeaturesStatus |"
Add-Line $lines ""
Add-Line $lines "## Acceptance Gates"
Add-Line $lines ""
Add-Line $lines "| Gate | Evidence | Status |"
Add-Line $lines "| --- | --- | --- |"
Add-GateRow $lines "corepack pnpm run verify:release" "Portable smoke=$smokeStatus; frontend typecheck=$frontendTypecheckStatus; frontend tests=$frontendTestsStatus; browser static=$browserExtensionStaticStatus; browser jsdom=$browserExtensionContentJsdomStatus; engine=$engineRuntimeStatus; installer=$portableInstallerStatus" $verifyReleaseGateStatus
Add-GateRow $lines "corepack pnpm run smoke:portable-renderer" "optional host renderer screenshot smoke; $portableRendererSmokeStatus" $(if ($portableRendererSmokeReady) { "Pass" } else { "Optional" })
Add-GateRow $lines "corepack pnpm run smoke:workflow-editor" "packaged workflow editor create/save/reload/edit smoke; $workflowEditorSmokeStatus" $(if ($workflowEditorSmokeReady) { "Pass" } else { "Blocked" })
Add-GateRow $lines "corepack pnpm run smoke:backend-offline" "packaged login backend-unavailable error-state smoke; $backendOfflineSmokeStatus" $(if ($backendOfflineSmokeReady) { "Pass" } else { "Blocked" })
Add-GateRow $lines "corepack pnpm run verify:release:full" "requires portable release gate, browser runtime tests/smoke, backend-offline UI smoke, GUI E2E report, host strict repair, and strict audit" $fullReleaseGateStatus
Add-GateRow $lines "corepack pnpm run doctor:release" "exitCode=$($doctor.ExitCode); warnings=$($warnings.Count)" $doctorGateStatus
Add-GateRow $lines "corepack pnpm run test:java-backends" $javaBackendTestsStatus $(if ($javaBackendTestsReady) { "Pass" } else { "Blocked" })
Add-GateRow $lines "corepack pnpm run repair:release-host:strict" "strict dry run=$repairHostStrictStatus" $(if ($repairHostStrictStatus -eq "Pass") { "Pass" } else { "Blocked" })
Add-GateRow $lines "corepack pnpm run audit:release:strict" "requires doctor warnings=0, host strict pass, browser runtime smoke pass, and GUI E2E pass" $strictAuditGateStatus
Add-GateRow $lines "real GUI/browser extension validation" "$guiE2eStatus; browser smoke=$browserExtensionSmokeStatus" $(if ($guiE2eReady -and $browserExtensionSmokeReady) { "Pass" } else { "Blocked" })
Add-Line $lines ""
Add-Line $lines "## Artifacts"
Add-Line $lines ""
Add-Line $lines "| Artifact | Path | State |"
Add-Line $lines "| --- | --- | --- |"
Add-ArtifactRow $lines "Portable launcher" $launcherPath $launcherStatus
Add-ArtifactRow $lines "Portable smoke result" $smokeResultPath $smokeStatus
Add-ArtifactRow $lines "Portable renderer smoke screenshot" $portableRendererSmokeScreenshotPath $portableRendererSmokeStatus
Add-ArtifactRow $lines "Workflow editor packaged smoke" $workflowEditorSmokeReportPath $workflowEditorSmokeStatus
Add-ArtifactRow $lines "Backend offline UI smoke" $backendOfflineSmokeReportPath $backendOfflineSmokeStatus
Add-ArtifactRow $lines "Engine runtime tests" $engineRuntimeReportPath $engineRuntimeStatus
Add-ArtifactRow $lines "Python archive" $archivePath $archiveStatus
Add-ArtifactRow $lines "Python archive hash" $hashPath $archiveHashStatus
Add-ArtifactRow $lines "Browser bridge inject in Python archive" $archivePath $archiveInjectStatus
Add-ArtifactRow $lines "Engine source overlay sync" (Join-Path $repoRoot "build\python_core") $engineSourceOverlayStatus
Add-ArtifactRow $lines "Portable Python archive source sync" $archivePath $portablePythonArchiveSourceStatus
Add-ArtifactRow $lines "Portable installer package" $portableInstallerPath $portableInstallerStatus
Add-ArtifactRow $lines "AI service requirements export" $aiReqPath $aiReqStatus
Add-ArtifactRow $lines "OpenAPI service requirements export" $openapiReqPath $openapiReqStatus
Add-ArtifactRow $lines "Python wheelhouse manifest" $pythonWheelhouseManifestPath $pythonWheelhouseManifestStatus
Add-ArtifactRow $lines "Python wheelhouse preflight" $pythonWheelhousePreflightPath $pythonWheelhousePreflightStatus
Add-ArtifactRow $lines "Python wheelhouse missing requirements" $pythonWheelhouseMissingRequirementsPath $pythonWheelhouseMissingRequirementsStatus
Add-ArtifactRow $lines "Python wheelhouse missing download report" $pythonWheelhouseDownloadReportPath $pythonWheelhouseDownloadReportStatus
Add-ArtifactRow $lines "Python backend offline cache dry run" $setupPythonBackendsScript $pythonOfflineCacheStatus
Add-ArtifactRow $lines "Python backend setup commands" $rootPackageJsonPath $pythonBackendSetupScriptsStatus
Add-ArtifactRow $lines "AI service Dockerfile locked install" "backend\ai-service\Dockerfile" $aiServiceDockerfileLockInstallStatus
Add-ArtifactRow $lines "OpenAPI service Dockerfile locked install" "backend\openapi-service\Dockerfile" $openapiServiceDockerfileLockInstallStatus
Add-ArtifactRow $lines "Python service compose healthchecks" $dockerComposePath $pythonComposeHealthcheckStatus
Add-ArtifactRow $lines "Java service compose healthchecks" $dockerComposePath $javaComposeHealthcheckStatus
Add-ArtifactRow $lines "Nginx gateway health source routes" "docker\volumes\nginx\default.conf" $nginxGatewayHealthSourceStatus
Add-ArtifactRow $lines "Backend gateway health" $backendHealthUrl $backendHealthStatus
Add-ArtifactRow $lines "Backend service route smoke" $sourceBackendGateway $backendRouteSmokeStatus
Add-ArtifactRow $lines "Auth wordmark asset" $authWordmarkUrl $authWordmarkStatus
Add-ArtifactRow $lines "Auth icon asset" $authIconUrl $authIconStatus
Add-ArtifactRow $lines "Robot OpenAPI workflow URL" "backend\robot-service\src\main\resources\application-local.yml" $robotOpenApiWorkflowStatus
Add-ArtifactRow $lines "OpenAPI robot service URL" "backend\openapi-service\app\config.py" $openApiRobotServiceStatus
Add-ArtifactRow $lines "OpenAPI admin route source" "backend\openapi-service\app\internal\admin.py" $openApiAdminRouteStatus
Add-ArtifactRow $lines "Release host repair script" $repairHostScript $repairHostStatus
Add-ArtifactRow $lines "Release host repair strict dry run" $repairHostScript $repairHostStrictStatus
Add-ArtifactRow $lines "Release host repair report" $releaseHostRepairReportPath $releaseHostRepairReportStatus
Add-ArtifactRow $lines "Release host repair summary" $releaseHostRepairSummaryPath $releaseHostRepairSummaryStatus
Add-ArtifactRow $lines "Full release verification report" $releaseFullVerificationReportPath $releaseFullVerificationStatus
Add-ArtifactRow $lines "Full release verification summary" $releaseFullVerificationSummaryPath $releaseFullVerificationSummaryStatus
Add-ArtifactRow $lines "Browser extension static package" $browserExtensionStaticReportPath $browserExtensionStaticStatus
Add-ArtifactRow $lines "Frontend typecheck" $frontendTypecheckReportPath $frontendTypecheckStatus
Add-ArtifactRow $lines "Frontend unit tests" $frontendTestsReportPath $frontendTestsStatus
Add-ArtifactRow $lines "Browser extension content browser tests" "frontend\packages\browser-plugin\src\test" $browserExtensionContentTestsStatus
Add-ArtifactRow $lines "Browser extension content jsdom tests" $browserExtensionContentJsdomReportPath $browserExtensionContentJsdomStatus
Add-ArtifactRow $lines "Browser extension automated smoke" $browserExtensionSmokeReportPath $browserExtensionSmokeStatus
Add-ArtifactRow $lines "GUI and browser extension E2E report" $guiE2eReportPath $guiE2eStatus
Add-ArtifactRow $lines "Java backend Maven tests" $javaBackendTestsReportPath $javaBackendTestsStatus
Add-ArtifactRow $lines "Generated source artifacts" "engine\components" $generatedSourceArtifactsStatus
Add-ArtifactRow $lines "Casdoor seed data" $casdoorInitDataPath $casdoorSeedStatus
Add-Line $lines ""
Add-Line $lines "## Release Doctor"
Add-Line $lines ""
Add-Line $lines "Command: ``corepack pnpm run doctor:release``"
Add-Line $lines "Exit code: $($doctor.ExitCode)"
Add-Line $lines ""
if ($warnings.Count -gt 0) {
  Add-Line $lines "Warnings:"
  foreach ($warning in $warnings) {
    Add-Line $lines "- $warning"
  }
}
else {
  Add-Line $lines "Warnings: none"
}
Add-Line $lines ""
Add-Line $lines "## Completion Audit"
Add-Line $lines ""
if ($fullStackReady) {
  Add-Line $lines "No missing full commercial acceptance coverage remains."
}
else {
  Add-Line $lines "Missing or weakly verified requirements:"
  if ($warnings.Count -gt 0) {
    foreach ($warning in $warnings) {
      Add-Line $lines "- $warning"
    }
  }
  if ($pythonOfflineCacheStatus -like "Blocked;*") {
    Add-Line $lines "- Python backend offline cache dry run is blocked: $pythonOfflineCacheStatus"
  }
  if ($repairHostStrictStatus -like "Blocked;*") {
    Add-Line $lines "- Release host repair strict dry run is blocked: $repairHostStrictStatus"
  }
  if (-not $guiE2eReady) {
    Add-Line $lines "- Real interactive GUI automation and browser extension flows still require a capable desktop validation session."
  }
  if (-not $browserExtensionSmokeReady) {
    Add-Line $lines "- Browser extension automated smoke is not verified: $browserExtensionSmokeStatus"
  }
  if (-not $javaBackendTestsReady) {
    Add-Line $lines "- Java backend Maven tests are not verified: $javaBackendTestsStatus"
  }
}
$nextHostActions = New-Object System.Collections.Generic.List[string]
foreach ($action in $repairHostActions) {
  if ($action -and ($nextHostActions -notcontains $action)) {
    $nextHostActions.Add($action) | Out-Null
  }
}
if ($javaBackendMavenRepoBlocked) {
  $mavenRepoAction = "Allow Maven HTTPS access to repo.maven.apache.org, or set SHOPRPA_MAVEN_LOCAL_REPOSITORY/SHOPRPA_MAVEN_SETTINGS to a populated Maven repository or mirror, then rerun: corepack pnpm run test:java-backends"
  if ($nextHostActions -notcontains $mavenRepoAction) {
    $nextHostActions.Add($mavenRepoAction) | Out-Null
  }
}
if ($nextHostActions.Count -gt 0) {
  Add-Line $lines ""
  Add-Line $lines "Next required host actions:"
  foreach ($action in $nextHostActions) {
    Add-Line $lines "- $action"
  }
}
Add-Line $lines ""
Add-Line $lines "## Raw Doctor Output"
Add-Line $lines ""
Add-Line $lines '```text'
Add-Line $lines $doctor.Output
Add-Line $lines '```'

Set-Content -LiteralPath $OutputPath -Value $lines -Encoding UTF8
Write-Host "Release audit written: $OutputPath"
if (-not $fullStackReady) {
  Write-Host "Release audit is partial; full-stack blockers remain." -ForegroundColor Yellow
  if ($Strict) {
    exit 1
  }
}
