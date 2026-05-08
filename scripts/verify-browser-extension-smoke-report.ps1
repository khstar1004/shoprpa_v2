param(
  [string]$ReportPath = "",
  [int]$MaxAgeDays = 14
)

$ErrorActionPreference = "Stop"

function Resolve-RepoRoot {
  $scriptDir = Split-Path -Parent $PSCommandPath
  return (Resolve-Path (Join-Path $scriptDir "..")).Path
}

function Add-Problem {
  param([string]$Message)
  $problems.Add($Message) | Out-Null
}

$repoRoot = Resolve-RepoRoot
if (-not $ReportPath) {
  $ReportPath = Join-Path $repoRoot "build\browser-extension-smoke\browser-extension-smoke-report.json"
}
if (-not [System.IO.Path]::IsPathRooted($ReportPath)) {
  $ReportPath = Join-Path $repoRoot $ReportPath
}

if (-not (Test-Path -LiteralPath $ReportPath -PathType Leaf)) {
  Write-Host "Browser extension smoke report is missing: $ReportPath." -ForegroundColor Yellow
  Write-Host "Run corepack pnpm run smoke:browser-extension first." -ForegroundColor Yellow
  exit 1
}

$report = Get-Content -LiteralPath $ReportPath -Raw | ConvertFrom-Json
$problems = New-Object System.Collections.Generic.List[string]

if ($report.schemaVersion -ne 1) {
  Add-Problem "schemaVersion must be 1."
}
$firstFailure = [string](@($report.failures | Where-Object { $_ }) | Select-Object -First 1)
if (($report.ok -ne $true) -and $firstFailure) {
  $firstFailureLine = @($firstFailure -split "`r?`n" | Where-Object { $_.Trim() } | Select-Object -First 1)
  if ($firstFailureLine.Count -gt 0) {
    Add-Problem "smoke run failed: $($firstFailureLine[0].Trim())"
  }
}
if ($report.ok -ne $true) {
  Add-Problem "ok must be true."
}
if ([string]::IsNullOrWhiteSpace([string]$report.generatedAt)) {
  Add-Problem "generatedAt is required."
}
else {
  try {
    $generatedAt = [DateTimeOffset]::Parse([string]$report.generatedAt)
    if ($generatedAt -gt [DateTimeOffset]::Now.AddMinutes(10)) {
      Add-Problem "generatedAt is in the future: $($report.generatedAt)"
    }
    if ($MaxAgeDays -gt 0 -and $generatedAt -lt [DateTimeOffset]::Now.AddDays(-$MaxAgeDays)) {
      Add-Problem "generatedAt is older than $MaxAgeDays days: $($report.generatedAt)"
    }
  }
  catch {
    Add-Problem "generatedAt is not a valid date/time: $($report.generatedAt)"
  }
}
if (-not $report.extensionPath -or -not (Test-Path -LiteralPath ([string]$report.extensionPath) -PathType Container)) {
  Add-Problem "extensionPath is missing or invalid."
}
if ([string]::IsNullOrWhiteSpace([string]$report.manifestVersion)) {
  Add-Problem "manifestVersion is required."
}
if ([string]::IsNullOrWhiteSpace([string]$report.browserExecutable)) {
  Add-Problem "browserExecutable is required."
}
if ([string]::IsNullOrWhiteSpace([string]$report.testPageUrl)) {
  Add-Problem "testPageUrl is required."
}
else {
  try {
    $testPageUri = [System.Uri]::new([string]$report.testPageUrl)
    if ($testPageUri.Scheme -notin @("http", "https")) {
      Add-Problem "testPageUrl must be http or https."
    }
  }
  catch {
    Add-Problem "testPageUrl is not a valid URL: $($report.testPageUrl)"
  }
}

$requiredScenarios = @(
  "extension-load",
  "active-tab",
  "content-script-message",
  "element-input",
  "element-click",
  "table-extract"
)
$scenarioMap = @{}
foreach ($scenario in @($report.scenarios)) {
  if ($scenario.id) {
    $scenarioMap[[string]$scenario.id] = $scenario
  }
}

if ($report.ok -eq $true) {
  if ([string]::IsNullOrWhiteSpace([string]$report.extensionId)) {
    Add-Problem "extensionId is required."
  }
  foreach ($scenarioId in $requiredScenarios) {
    if (-not $scenarioMap.ContainsKey($scenarioId)) {
      Add-Problem "Missing required scenario: $scenarioId"
      continue
    }
    if ([string]$scenarioMap[$scenarioId].status -ne "PASS") {
      Add-Problem "$scenarioId status must be PASS."
    }
    if ([string]::IsNullOrWhiteSpace([string]$scenarioMap[$scenarioId].detail)) {
      Add-Problem "$scenarioId detail is required."
    }
  }
}

$requiredEvidencePaths = if ($report.ok -eq $true) {
  @($report.evidence.screenshot, $report.evidence.html)
}
else {
  @($report.evidence.html)
}
foreach ($evidencePath in $requiredEvidencePaths) {
  if ([string]::IsNullOrWhiteSpace([string]$evidencePath)) {
    Add-Problem "Evidence path is empty."
    continue
  }
  $resolvedEvidencePath = if ([System.IO.Path]::IsPathRooted([string]$evidencePath)) {
    [string]$evidencePath
  }
  else {
    Join-Path $repoRoot ([string]$evidencePath)
  }
  if (-not (Test-Path -LiteralPath $resolvedEvidencePath -PathType Leaf)) {
    Add-Problem "Evidence file is missing: $evidencePath"
    continue
  }
  if ((Get-Item -LiteralPath $resolvedEvidencePath).Length -le 0) {
    Add-Problem "Evidence file is empty: $evidencePath"
  }
}

if ($report.failures -and @($report.failures).Count -gt 0) {
  Add-Problem "failures must be empty."
}
if ($report.hostPolicyBlocked -eq $true) {
  $failureClass = [string]$report.failureClass
  if ([string]::IsNullOrWhiteSpace($failureClass)) {
    $failureClass = "browser-host-policy-blocked"
  }
  Add-Problem "failureClass=$failureClass"
}

if ($problems.Count -gt 0) {
  Write-Host "Browser extension smoke report verification failed." -ForegroundColor Yellow
  foreach ($problem in $problems) {
    Write-Host "- $problem" -ForegroundColor Yellow
  }
  exit 1
}

Write-Host "Browser extension smoke report verified: $ReportPath"
