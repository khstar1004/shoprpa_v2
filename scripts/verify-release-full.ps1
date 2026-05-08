param(
  [string]$ReportPath = "",
  [int]$StepTimeoutSeconds = 900,
  [int]$AuditTimeoutSeconds = 300,
  [switch]$StopOnFirstFailure,
  [switch]$RefreshAuditOnly,
  [switch]$List,
  [Parameter(ValueFromRemainingArguments = $true)]
  [string[]]$RemainingArgs
)

$ErrorActionPreference = "Stop"

$remainingArgsList = if ($null -eq $RemainingArgs) { @() } else { @($RemainingArgs) }
$remainingIndex = 0
while ($remainingIndex -lt $remainingArgsList.Count) {
  $arg = $remainingArgsList[$remainingIndex]
  switch ($arg) {
    "--" {
      $remainingIndex += 1
      continue
    }
    "-ReportPath" {
      $remainingIndex += 1
      if ($remainingIndex -ge $remainingArgsList.Count) {
        throw "-ReportPath requires a path value."
      }
      $ReportPath = $remainingArgsList[$remainingIndex]
    }
    "-StepTimeoutSeconds" {
      $remainingIndex += 1
      if ($remainingIndex -ge $remainingArgsList.Count) {
        throw "-StepTimeoutSeconds requires a value."
      }
      $StepTimeoutSeconds = [int]$remainingArgsList[$remainingIndex]
    }
    "-AuditTimeoutSeconds" {
      $remainingIndex += 1
      if ($remainingIndex -ge $remainingArgsList.Count) {
        throw "-AuditTimeoutSeconds requires a value."
      }
      $AuditTimeoutSeconds = [int]$remainingArgsList[$remainingIndex]
    }
    "-StopOnFirstFailure" {
      $StopOnFirstFailure = $true
    }
    "-RefreshAuditOnly" {
      $RefreshAuditOnly = $true
    }
    "-List" {
      $List = $true
    }
    default {
      throw "Unknown verify-release-full argument after --: $arg"
    }
  }
  $remainingIndex += 1
}

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

function Invoke-CapturedCommand {
  param(
    [string]$CommandLine,
    [string]$WorkingDirectory,
    [int]$TimeoutSeconds = 900
  )

  $psi = [System.Diagnostics.ProcessStartInfo]::new()
  $psi.FileName = "$env:SystemRoot\System32\cmd.exe"
  $psi.Arguments = "/d /c $CommandLine"
  $psi.WorkingDirectory = $WorkingDirectory
  $psi.RedirectStandardOutput = $true
  $psi.RedirectStandardError = $true
  $psi.UseShellExecute = $false
  $psi.CreateNoWindow = $true

  $process = [System.Diagnostics.Process]::new()
  $process.StartInfo = $psi
  $startedAt = [DateTimeOffset]::Now
  [void]$process.Start()
  $stdoutTask = $process.StandardOutput.ReadToEndAsync()
  $stderrTask = $process.StandardError.ReadToEndAsync()
  $timedOut = $false
  $terminationFailed = $false
  if (-not $process.WaitForExit([Math]::Max(1, $TimeoutSeconds) * 1000)) {
    $timedOut = $true
    try {
      & "$env:SystemRoot\System32\taskkill.exe" /PID $process.Id /T /F | Out-Null
    }
    catch {
      try {
        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
      }
      catch {
      }
    }
    if (-not $process.WaitForExit(5000)) {
      $terminationFailed = $true
    }
  }
  if ($terminationFailed) {
    $stdout = if ($stdoutTask.IsCompleted) { $stdoutTask.GetAwaiter().GetResult() } else { "" }
    $stderr = if ($stderrTask.IsCompleted) { $stderrTask.GetAwaiter().GetResult() } else { "" }
    $stderr = (($stderr, "Timed out after ${TimeoutSeconds}s and the process tree could not be terminated by this host policy.") | Where-Object { $_ }) -join "`n"
  }
  else {
    $stdout = $stdoutTask.GetAwaiter().GetResult()
    $stderr = $stderrTask.GetAwaiter().GetResult()
  }
  $finishedAt = [DateTimeOffset]::Now

  [PSCustomObject]@{
    exitCode = if ($timedOut) { 124 } else { $process.ExitCode }
    timedOut = $timedOut
    terminationFailed = $terminationFailed
    timeoutSeconds = $TimeoutSeconds
    startedAt = $startedAt.ToString("o")
    finishedAt = $finishedAt.ToString("o")
    durationSeconds = [Math]::Round(($finishedAt - $startedAt).TotalSeconds, 3)
    output = (($stdout + "`n" + $stderr).Trim())
  }
}

function Get-OutputTail {
  param(
    [string]$Text,
    [int]$MaxChars = 12000
  )

  if (-not $Text) {
    return ""
  }
  $Text = Normalize-CapturedOutput -Text $Text
  if ($Text.Length -le $MaxChars) {
    return $Text
  }

  $tail = $Text.Substring($Text.Length - $MaxChars)
  $firstNewline = $tail.IndexOf("`n")
  if ($firstNewline -ge 0 -and $firstNewline -lt ($tail.Length - 1)) {
    $tail = $tail.Substring($firstNewline + 1)
  }
  return "[output truncated]`n$tail"
}

function Normalize-CapturedOutput {
  param([string]$Text)

  if (-not $Text) {
    return ""
  }

  $lines = New-Object System.Collections.Generic.List[string]
  foreach ($line in @($Text -split "`r?`n")) {
    if ($line -match "Local package\.json exists, but node_modules missing, did you mean to install\?") {
      continue
    }
    $lines.Add($line) | Out-Null
  }
  return ($lines -join "`n").Trim()
}

function Test-TransientProcessFailure {
  param([object]$Result)

  if ($Result.timedOut) {
    return $false
  }
  return [int]$Result.exitCode -lt 0
}

function Get-GuiE2eFailureSummary {
  param([string]$Text)

  $normalized = Normalize-CapturedOutput -Text $Text
  if ($normalized -notmatch "GUI/browser E2E report verification failed") {
    return ""
  }

  $problems = @(
    $normalized -split "`r?`n" |
      ForEach-Object { $_.Trim() } |
      Where-Object { $_.StartsWith("- ") } |
      ForEach-Object { $_.Substring(2).Trim() } |
      Where-Object { $_ }
  )
  if ($problems.Count -eq 0) {
    return ""
  }

  $parts = New-Object System.Collections.Generic.List[string]
  if (@($problems | Where-Object { $_ -like "report template is incomplete*" }).Count -gt 0) {
    $parts.Add("template incomplete") | Out-Null
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
  if ($examples.Count -gt 0) {
    $parts.Add("examples: $($examples -join '; ')") | Out-Null
  }
  return ($parts -join "; ")
}

function Get-BrowserExtensionSmokeFailureSummary {
  param([string]$Text)

  $normalized = Normalize-CapturedOutput -Text $Text
  if ($normalized -notmatch "Browser extension smoke report verification failed") {
    return ""
  }

  $problems = @(
    $normalized -split "`r?`n" |
      ForEach-Object { $_.Trim() } |
      Where-Object { $_.StartsWith("- ") } |
      ForEach-Object { $_.Substring(2).Trim() } |
      Where-Object { $_ }
  )
  if ($problems.Count -eq 0) {
    return ""
  }

  $parts = New-Object System.Collections.Generic.List[string]
  $runFailure = @($problems | Where-Object { $_ -like "smoke run failed:*" } | Select-Object -First 1)
  if ($runFailure.Count -gt 0) {
    $parts.Add($runFailure[0]) | Out-Null
  }
  $missingFields = @()
  foreach ($fieldName in @("manifestVersion", "extensionId", "browserExecutable", "testPageUrl")) {
    if ($problems -contains "$fieldName is required.") {
      $missingFields += $fieldName
    }
  }
  if ($missingFields.Count -gt 0) {
    $parts.Add("missing fields=$($missingFields.Count) ($($missingFields -join ', '))") | Out-Null
  }
  $missingScenarios = @(
    $problems |
      Where-Object { $_ -match "^Missing required scenario: (.+)$" } |
      ForEach-Object { ($_ -replace "^Missing required scenario:\s*", "").Trim() }
  )
  if ($missingScenarios.Count -gt 0) {
    $parts.Add("missing scenarios=$($missingScenarios.Count) ($($missingScenarios -join ', '))") | Out-Null
  }
  $evidenceIssues = @($problems | Where-Object { $_ -like "Evidence*" })
  if ($evidenceIssues.Count -gt 0) {
    $parts.Add("evidence issues=$($evidenceIssues.Count)") | Out-Null
  }
  if ($problems -contains "failures must be empty.") {
    $parts.Add("failures not empty") | Out-Null
  }
  $failureClass = @($problems | Where-Object { $_ -like "failureClass=*" } | Select-Object -First 1)
  if ($failureClass.Count -gt 0) {
    $parts.Add($failureClass[0]) | Out-Null
  }
  return ($parts -join "; ")
}

function Get-ConciseFailureLine {
  param([string]$Text)

  $normalized = Normalize-CapturedOutput -Text $Text
  if (-not $normalized) {
    return ""
  }

  $guiE2eFailure = Get-GuiE2eFailureSummary -Text $normalized
  if ($guiE2eFailure) {
    return $guiE2eFailure
  }

  $browserSmokeFailure = Get-BrowserExtensionSmokeFailureSummary -Text $normalized
  if ($browserSmokeFailure) {
    return $browserSmokeFailure
  }

  $blockedChecks = New-Object System.Collections.Generic.List[string]
  $inBlockedChecks = $false
  foreach ($line in @($normalized -split "`r?`n")) {
    $trimmed = $line.Trim()
    if ($trimmed -eq "Blocked checks:") {
      $inBlockedChecks = $true
      continue
    }
    if (-not $inBlockedChecks) {
      continue
    }
    if (-not $trimmed) {
      break
    }
    if ($trimmed.StartsWith("- ")) {
      $blockedChecks.Add($trimmed.Substring(2)) | Out-Null
    }
  }
  if ($blockedChecks.Count -gt 0) {
    return (@($blockedChecks | Select-Object -First 3) -join "; ")
  }

  $patterns = @(
    "spawn EPERM",
    "Timeout \d+ms exceeded",
    "permission denied",
    "mvn is not on PATH",
    "uv dry run still needs downloads",
    "wheelhouse or local uv cache is incomplete",
    "report template is incomplete",
    "Release audit is partial",
    "Strict mode failed",
    "Missing required scenario",
    "ok must be true"
  )

  foreach ($line in @($normalized -split "`r?`n")) {
    $trimmed = $line.Trim()
    if (-not $trimmed) {
      continue
    }
    if ($trimmed.EndsWith("...")) {
      continue
    }
    foreach ($pattern in $patterns) {
      if ($trimmed -match $pattern) {
        return ($trimmed -replace "^- ", "")
      }
    }
  }

  foreach ($line in @($normalized -split "`r?`n")) {
    $trimmed = $line.Trim()
    if (-not $trimmed) {
      continue
    }
    if ($trimmed.StartsWith(">")) {
      continue
    }
    if ($trimmed -match "^( )?ELIFECYCLE") {
      continue
    }
    if ($trimmed -match "^\s*WARN\s+") {
      continue
    }
    if ($trimmed.EndsWith("...")) {
      continue
    }
    return ($trimmed -replace "^- ", "")
  }
  return ""
}

function Get-NextHostActions {
  param([string]$RepoRoot)

  $actions = New-Object System.Collections.Generic.List[string]
  function Add-NextHostAction {
    param([string]$Action)
    if ($Action -and ($actions -notcontains $Action)) {
      $actions.Add($Action) | Out-Null
    }
  }

  $reportPath = Join-Path $RepoRoot "build\release-host-repair-report.json"
  if (Test-Path -LiteralPath $reportPath -PathType Leaf) {
    try {
      $report = Get-Content -LiteralPath $reportPath -Raw | ConvertFrom-Json
      foreach ($action in @($report.nextRequiredHostActions | Where-Object { $_ } | ForEach-Object { [string]$_ })) {
        Add-NextHostAction $action
      }
    }
    catch {
    }
  }

  $auditSummaryPath = Join-Path $RepoRoot "build\release-audit.md"
  if (Test-Path -LiteralPath $auditSummaryPath -PathType Leaf) {
    try {
      $inActions = $false
      foreach ($line in (Get-Content -LiteralPath $auditSummaryPath)) {
        $trimmed = $line.Trim()
        if ($trimmed -eq "Next required host actions:") {
          $inActions = $true
          continue
        }
        if ($inActions -and $trimmed -match "^##\s+") {
          break
        }
        if ($inActions -and $trimmed.StartsWith("- ")) {
          Add-NextHostAction ($trimmed.Substring(2).Trim())
        }
      }
    }
    catch {
    }
  }

  return $actions.ToArray()
}

function Get-JavaBackendTestsSummaryFromReport {
  param([string]$RepoRoot)

  $reportPath = Join-Path $RepoRoot "build\java-backend-tests.json"
  if (-not (Test-Path -LiteralPath $reportPath -PathType Leaf)) {
    return ""
  }

  try {
    $report = Get-Content -LiteralPath $reportPath -Raw | ConvertFrom-Json
    if ($report.ok -eq $true) {
      $passedModules = @($report.modules | Where-Object { $_.ok -eq $true }).Count
      return "$passedModules Java backend modules passed"
    }
    $moduleOutput = @(
      $report.modules |
        ForEach-Object { [string]$_.outputTail } |
        Where-Object { $_ }
    ) -join "`n"
    if ($moduleOutput -match "repo\.maven\.apache\.org|Could not transfer artifact|Permission denied: getsockopt|Non-resolvable (parent|import) POM") {
      $failedModuleNames = @(
        $report.modules |
          Where-Object { $_.ok -ne $true } |
          ForEach-Object { [string]$_.name } |
          Where-Object { $_ }
      )
      $moduleSummary = if ($failedModuleNames.Count -gt 0) { "; modules=$($failedModuleNames -join ', ')" } else { "" }
      return "Maven repository access is blocked (repo.maven.apache.org: Permission denied: getsockopt)$moduleSummary"
    }
    $failure = @($report.failures | Where-Object { $_ } | Select-Object -First 1)
    if ($failure.Count -gt 0) {
      return [string]$failure[0]
    }
  }
  catch {
  }

  return ""
}

function Write-FullVerificationSummary {
  param(
    [string]$Path,
    [object[]]$Records,
    [bool]$Failed,
    [bool]$StopOnFirstFailureEnabled,
    [string]$GeneratedAt,
    [string]$RefreshedAt,
    [bool]$AuditOnlyRefresh = $false,
    [object]$AuditRefresh = $null
  )

  $summaryPath = [System.IO.Path]::ChangeExtension($Path, ".md")
  $failedRecordsForSummary = @($Records | Where-Object { $_.ok -ne $true })
  $expectedStepNames = if ($script:steps) { @($script:steps | ForEach-Object { [string]$_.name }) } else { @($Records | ForEach-Object { [string]$_.name }) }
  $recordedStepNames = @($Records | ForEach-Object { [string]$_.name })
  $pendingStepNames = @()
  if (-not $AuditOnlyRefresh) {
    $pendingStepNames = @($expectedStepNames | Where-Object { $recordedStepNames -notcontains $_ })
  }
  $summaryStatus = if ($Failed -or $failedRecordsForSummary.Count -gt 0 -or $pendingStepNames.Count -gt 0) { "BLOCKED" } else { "PASS" }
  $lines = New-Object System.Collections.Generic.List[string]
  $lines.Add("# ShopRPA Full Release Verification Summary") | Out-Null
  $lines.Add("") | Out-Null
  $lines.Add("Generated: $GeneratedAt") | Out-Null
  $lines.Add("Refreshed: $RefreshedAt") | Out-Null
  if ($AuditOnlyRefresh) {
    $lines.Add("Step results source: last full run from $GeneratedAt; only the release audit was refreshed.") | Out-Null
  }
  $lines.Add("Repository: $repoRoot") | Out-Null
  $lines.Add("Status: $summaryStatus") | Out-Null
  $lines.Add("Stop on first failure: $StopOnFirstFailureEnabled") | Out-Null
  $lines.Add("") | Out-Null
  $lines.Add("| Step | Status | Exit | Seconds | Reason |") | Out-Null
  $lines.Add("| --- | --- | --- | --- | --- |") | Out-Null

  foreach ($record in @($Records)) {
    $status = if ($record.status) { [string]$record.status } elseif ($record.ok -eq $true) { "PASS" } elseif ($record.timedOut -eq $true) { "TIMEOUT" } else { "FAIL" }
    $reason = if ($record.summary) { [string]$record.summary } else { "" }
    if ([string]$record.name -eq "java backend Maven tests") {
      $javaSummary = Get-JavaBackendTestsSummaryFromReport -RepoRoot $repoRoot
      if ($javaSummary) {
        $reason = $javaSummary
      }
    }
    if (($record.ok -ne $true) -and (-not $reason)) {
      $reason = Get-ConciseFailureLine -Text ([string]$record.outputTail)
      if (-not $reason) {
        $reason = "See build\release-full-verification.json"
      }
    }
    $escapedReason = $reason.Replace("|", "\|")
    $lines.Add("| $($record.name) | $status | $($record.exitCode) | $($record.durationSeconds) | $escapedReason |") | Out-Null
  }
  foreach ($pendingStepName in $pendingStepNames) {
    $lines.Add("| $pendingStepName | PENDING |  |  | Step has not completed in this full verification run. |") | Out-Null
  }

  if ($AuditRefresh) {
    $auditLabel = if ([string]$AuditRefresh.command -like "*:strict*") { "Final strict audit" } else { "Audit refresh" }
    $lines.Add("") | Out-Null
    $lines.Add("${auditLabel}: $(if ($AuditRefresh.ok -eq $true) { "PASS" } else { "FAIL" }) (exit $($AuditRefresh.exitCode), $($AuditRefresh.durationSeconds)s)") | Out-Null
  }

  $failedRecords = $failedRecordsForSummary
  if ($failedRecords.Count -gt 0 -or $pendingStepNames.Count -gt 0) {
    $lines.Add("") | Out-Null
    $blockedStepNames = @(@($failedRecords | ForEach-Object { [string]$_.name }) + $pendingStepNames)
    $lines.Add("Blocked steps: $($blockedStepNames -join ', ')") | Out-Null
    $lines.Add("") | Out-Null
    $hostActions = @(Get-NextHostActions -RepoRoot $repoRoot)
    if ($hostActions.Count -gt 0) {
      $lines.Add("Next required host actions:") | Out-Null
      foreach ($action in $hostActions) {
        $lines.Add("- $action") | Out-Null
      }
      $lines.Add("") | Out-Null
    }
    $lines.Add("Next commands after host fixes:") | Out-Null
    $lines.Add("- corepack pnpm run repair:release-host:apply") | Out-Null
    $lines.Add("- corepack pnpm run verify:release:full") | Out-Null
  }

  Set-Content -LiteralPath $summaryPath -Value $lines -Encoding UTF8
}

function Write-FullVerificationReport {
  param(
    [string]$Path,
    [object[]]$Records,
    [bool]$Failed,
    [bool]$StopOnFirstFailureEnabled,
    [string]$GeneratedAt = "",
    [string]$RefreshedAt = "",
    [bool]$AuditOnlyRefresh = $false,
    [object]$AuditRefresh = $null
  )

  if (-not $GeneratedAt) {
    $GeneratedAt = (Get-Date).ToString("o")
  }
  $refreshedAt = if ($RefreshedAt) { $RefreshedAt } else { (Get-Date).ToString("o") }
  $recordsForReport = @($Records)
  $expectedStepNames = if ($script:steps) { @($script:steps | ForEach-Object { [string]$_.name }) } else { @($recordsForReport | ForEach-Object { [string]$_.name }) }
  $recordedStepNames = @($recordsForReport | ForEach-Object { [string]$_.name })
  $pendingStepNames = @()
  if (-not $AuditOnlyRefresh) {
    $pendingStepNames = @($expectedStepNames | Where-Object { $recordedStepNames -notcontains $_ })
  }
  $javaSummary = Get-JavaBackendTestsSummaryFromReport -RepoRoot $repoRoot
  if ($javaSummary) {
    foreach ($record in $recordsForReport) {
      if ([string]$record.name -eq "java backend Maven tests") {
        $record.summary = $javaSummary
      }
    }
  }
  $failedStepNames = @($recordsForReport | Where-Object { $_.ok -ne $true } | ForEach-Object { [string]$_.name })
  $blockedStepNames = @($failedStepNames + $pendingStepNames)
  $reportStatus = if ($Failed -or $blockedStepNames.Count -gt 0) { "BLOCKED" } else { "PASS" }
  $nextHostActions = @()
  if ($reportStatus -ne "PASS") {
    $nextHostActions = @(Get-NextHostActions -RepoRoot $repoRoot)
  }
  $report = [PSCustomObject]@{
    schemaVersion = 2
    acceptanceContractVersion = "shoprpa-full-release-2026-05-v3"
    generatedAt = $GeneratedAt
    refreshedAt = $refreshedAt
    repository = $repoRoot
    status = $reportStatus
    ok = $reportStatus -eq "PASS"
    failedStepCount = $blockedStepNames.Count
    blockedSteps = $blockedStepNames
    completedStepCount = $recordsForReport.Count
    expectedStepCount = $expectedStepNames.Count
    pendingSteps = $pendingStepNames
    nextHostActions = $nextHostActions
    stopOnFirstFailure = $StopOnFirstFailureEnabled
    auditOnlyRefresh = $AuditOnlyRefresh
    stepResultsRefreshed = -not $AuditOnlyRefresh
    auditRefresh = $AuditRefresh
    steps = $recordsForReport
  }
  $report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $Path -Encoding UTF8
  Write-FullVerificationSummary -Path $Path -Records $recordsForReport -Failed $Failed -StopOnFirstFailureEnabled $StopOnFirstFailureEnabled -GeneratedAt $GeneratedAt -RefreshedAt $refreshedAt -AuditOnlyRefresh $AuditOnlyRefresh -AuditRefresh $AuditRefresh
}

$repoRoot = Resolve-RepoRoot
if (-not $ReportPath) {
  $ReportPath = Join-Path $repoRoot "build\release-full-verification.json"
}
if (-not [System.IO.Path]::IsPathRooted($ReportPath)) {
  $ReportPath = Join-Path $repoRoot $ReportPath
}

$steps = @(
  [PSCustomObject]@{
    name = "portable release gate"
    command = "corepack pnpm run verify:release"
    purpose = "build and verify portable desktop package, browser static package, jsdom content tests, engine runtime tests, and portable installer"
    maxAttempts = 2
  },
  [PSCustomObject]@{
    name = "browser extension content tests"
    command = "corepack pnpm run test:browser-extension-content"
    purpose = "run browser-backed content script tests"
    timeoutSeconds = 120
  },
  [PSCustomObject]@{
    name = "browser extension content jsdom tests"
    command = "corepack pnpm run test:browser-extension-content:jsdom"
    purpose = "run auxiliary jsdom content script DOM tests without browser process launch"
    timeoutSeconds = 120
  },
  [PSCustomObject]@{
    name = "browser extension runtime smoke"
    command = "corepack pnpm run smoke:browser-extension"
    purpose = "launch or attach to a Chromium-compatible browser and exercise extension runtime flow"
    timeoutSeconds = 120
  },
  [PSCustomObject]@{
    name = "browser extension smoke report"
    command = "corepack pnpm run verify:browser-extension-smoke"
    purpose = "verify browser extension runtime smoke report"
    timeoutSeconds = 60
  },
  [PSCustomObject]@{
    name = "workflow editor packaged smoke"
    command = "corepack pnpm run smoke:workflow-editor"
    purpose = "launch the packaged Electron editor and verify create, save, reload, edit, and save persistence"
    timeoutSeconds = 120
  },
  [PSCustomObject]@{
    name = "backend offline UI smoke"
    command = "corepack pnpm run smoke:backend-offline"
    purpose = "launch the packaged Electron login screen and verify an actionable backend-unavailable error state"
    timeoutSeconds = 120
  },
  [PSCustomObject]@{
    name = "GUI/browser automated evidence refresh"
    command = "corepack pnpm run evidence:gui-e2e"
    purpose = "refresh the GUI/browser evidence report from accepted automated smoke artifacts"
    timeoutSeconds = 60
  },
  [PSCustomObject]@{
    name = "interactive GUI E2E report"
    command = "corepack pnpm run verify:gui-e2e"
    purpose = "verify human-collected interactive desktop GUI and browser automation evidence"
    timeoutSeconds = 60
  },
  [PSCustomObject]@{
    name = "java backend Maven tests"
    command = "corepack pnpm run test:java-backends"
    purpose = "compile and test Java backend modules with the required JDK and Maven"
    timeoutSeconds = 180
  },
  [PSCustomObject]@{
    name = "host strict repair preflight"
    command = "corepack pnpm run repair:release-host:strict"
    purpose = "require Docker, Maven, Python backend dependencies, compose repair path, and zero doctor warnings"
    timeoutSeconds = 180
  }
)

if ($List) {
  Write-Host "ShopRPA full release verification steps:"
  foreach ($step in $steps) {
    $listedTimeoutSeconds = if ($step.timeoutSeconds) { [int]$step.timeoutSeconds } else { $StepTimeoutSeconds }
    Write-Host "- $($step.name): $($step.command) (timeout ${listedTimeoutSeconds}s)"
  }
  Write-Host "- final strict audit: corepack pnpm run audit:release:strict (timeout ${AuditTimeoutSeconds}s)"
  exit 0
}

$reportDir = Split-Path -Parent $ReportPath
if (-not (Test-Path -LiteralPath $reportDir -PathType Container)) {
  New-Item -ItemType Directory -Path $reportDir | Out-Null
}

if ($RefreshAuditOnly) {
  if (-not (Test-Path -LiteralPath $ReportPath -PathType Leaf)) {
    throw "Full release verification report is missing: $ReportPath"
  }
  $existingReport = Get-Content -LiteralPath $ReportPath -Raw | ConvertFrom-Json
  if ($existingReport.schemaVersion -ne 2) {
    throw "Full release verification report schemaVersion must be 2 for -RefreshAuditOnly. Run corepack pnpm run verify:release:full."
  }
  if ([string]$existingReport.acceptanceContractVersion -ne "shoprpa-full-release-2026-05-v3") {
    throw "Full release verification report acceptanceContractVersion is not current. Run corepack pnpm run verify:release:full."
  }
  $existingRecords = @($existingReport.steps)
  if ($existingRecords.Count -eq 0) {
    throw "Full release verification report has no steps: $ReportPath"
  }
  foreach ($requiredStep in $steps) {
    $existingStep = @($existingRecords | Where-Object { [string]$_.name -eq $requiredStep.name } | Select-Object -First 1)
    if ($existingStep.Count -eq 0) {
      throw "Full release verification report is missing step '$($requiredStep.name)'. Run corepack pnpm run verify:release:full."
    }
    if ([string]$existingStep[0].command -ne [string]$requiredStep.command) {
      throw "Full release verification report command for '$($requiredStep.name)' is not current. Run corepack pnpm run verify:release:full."
    }
  }
  $existingFailed = ($existingReport.ok -ne $true) -or (@($existingRecords | Where-Object { $_.ok -ne $true }).Count -gt 0)
  $refreshTimestamp = (Get-Date).ToString("o")
  Write-FullVerificationReport -Path $ReportPath -Records $existingRecords -Failed $existingFailed -StopOnFirstFailureEnabled ([bool]$existingReport.stopOnFirstFailure) -GeneratedAt ([string]$existingReport.generatedAt) -RefreshedAt $refreshTimestamp -AuditOnlyRefresh $true -AuditRefresh $existingReport.auditRefresh
  $auditRefresh = Invoke-CapturedCommand -CommandLine "corepack pnpm run audit:release" -WorkingDirectory $repoRoot -TimeoutSeconds $AuditTimeoutSeconds
  if (($auditRefresh.exitCode -ne 0) -or $auditRefresh.timedOut) {
    $existingFailed = $true
  }
  Write-FullVerificationReport -Path $ReportPath -Records $existingRecords -Failed $existingFailed -StopOnFirstFailureEnabled ([bool]$existingReport.stopOnFirstFailure) -GeneratedAt ([string]$existingReport.generatedAt) -RefreshedAt $refreshTimestamp -AuditOnlyRefresh $true -AuditRefresh ([PSCustomObject]@{
      command = "corepack pnpm run audit:release"
      ok = $auditRefresh.exitCode -eq 0
      exitCode = $auditRefresh.exitCode
      timedOut = $auditRefresh.timedOut
      timeoutSeconds = $auditRefresh.timeoutSeconds
      startedAt = $auditRefresh.startedAt
      finishedAt = $auditRefresh.finishedAt
      durationSeconds = $auditRefresh.durationSeconds
      outputTail = Get-OutputTail -Text $auditRefresh.output
    })
  Write-Host "Full release verification report refreshed: $(Get-RelativePath -BasePath $repoRoot -TargetPath $ReportPath)"
  if ($existingFailed) {
    exit 1
  }
  exit 0
}

$fullRunGeneratedAt = (Get-Date).ToString("o")
$records = New-Object System.Collections.Generic.List[object]
$failed = $false
foreach ($step in $steps) {
  Write-Host ""
  Write-Host "==> $($step.name)"
  Write-Host $step.command
  $maxAttempts = if ($step.maxAttempts) { [Math]::Max(1, [int]$step.maxAttempts) } else { 1 }
  $stepTimeoutSeconds = if ($step.timeoutSeconds) { [Math]::Max(1, [int]$step.timeoutSeconds) } else { $StepTimeoutSeconds }
  $attemptRecords = New-Object System.Collections.Generic.List[object]
  $result = $null
  for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
    if ($maxAttempts -gt 1) {
      Write-Host "Attempt $attempt of $maxAttempts"
    }
    $result = Invoke-CapturedCommand -CommandLine $step.command -WorkingDirectory $repoRoot -TimeoutSeconds $stepTimeoutSeconds
    $attemptRecords.Add([PSCustomObject]@{
        attempt = $attempt
        exitCode = $result.exitCode
        timedOut = $result.timedOut
        terminationFailed = $result.terminationFailed
        timeoutSeconds = $result.timeoutSeconds
        startedAt = $result.startedAt
        finishedAt = $result.finishedAt
        durationSeconds = $result.durationSeconds
        outputTail = Get-OutputTail -Text $result.output -MaxChars 4000
      }) | Out-Null
    if (($result.exitCode -eq 0) -and (-not $result.timedOut)) {
      break
    }
    if ($attempt -lt $maxAttempts -and (Test-TransientProcessFailure -Result $result)) {
      Write-Host "Retrying $($step.name) after transient process exit $($result.exitCode)." -ForegroundColor Yellow
      continue
    }
    break
  }
  $ok = ($result.exitCode -eq 0) -and (-not $result.timedOut)
  $status = if ($ok) { "PASS" } elseif ($result.timedOut) { "TIMEOUT" } else { "FAIL" }
  $summary = if ($ok) { "completed" } else { Get-ConciseFailureLine -Text ([string]$result.output) }
  if ((-not $ok) -and (-not $summary)) {
    $summary = "See build\release-full-verification.json"
  }
  if (-not $ok) {
    $failed = $true
  }
  if ($result.output) {
    Write-Host (Get-OutputTail -Text $result.output -MaxChars 4000)
  }
  $timeoutNote = if ($result.timedOut) { ", timed out after $($result.timeoutSeconds)s" } else { "" }
  Write-Host "$status $($step.name) (exit $($result.exitCode), $($result.durationSeconds)s$timeoutNote)"

  $records.Add([PSCustomObject]@{
      name = $step.name
      command = $step.command
      purpose = $step.purpose
      status = $status
      summary = $summary
      ok = $ok
      exitCode = $result.exitCode
      timedOut = $result.timedOut
      terminationFailed = $result.terminationFailed
      timeoutSeconds = $result.timeoutSeconds
      attempts = $attemptRecords.ToArray()
      startedAt = $result.startedAt
      finishedAt = $result.finishedAt
      durationSeconds = $result.durationSeconds
      outputTail = Get-OutputTail -Text $result.output
    }) | Out-Null
  Write-FullVerificationReport -Path $ReportPath -Records $records.ToArray() -Failed $failed -StopOnFirstFailureEnabled ([bool]$StopOnFirstFailure) -GeneratedAt $fullRunGeneratedAt

  if ((-not $ok) -and $StopOnFirstFailure) {
    break
  }
}

$finalRefreshedAt = (Get-Date).ToString("o")
Write-FullVerificationReport -Path $ReportPath -Records $records.ToArray() -Failed $failed -StopOnFirstFailureEnabled ([bool]$StopOnFirstFailure) -GeneratedAt $fullRunGeneratedAt -RefreshedAt $finalRefreshedAt
$auditRefresh = Invoke-CapturedCommand -CommandLine "corepack pnpm run audit:release:strict" -WorkingDirectory $repoRoot -TimeoutSeconds $AuditTimeoutSeconds
if (($auditRefresh.exitCode -ne 0) -or $auditRefresh.timedOut) {
  $failed = $true
}
Write-FullVerificationReport -Path $ReportPath -Records $records.ToArray() -Failed $failed -StopOnFirstFailureEnabled ([bool]$StopOnFirstFailure) -GeneratedAt $fullRunGeneratedAt -RefreshedAt $finalRefreshedAt -AuditRefresh ([PSCustomObject]@{
    command = "corepack pnpm run audit:release:strict"
    ok = ($auditRefresh.exitCode -eq 0) -and (-not $auditRefresh.timedOut)
    exitCode = $auditRefresh.exitCode
    timedOut = $auditRefresh.timedOut
    timeoutSeconds = $auditRefresh.timeoutSeconds
    startedAt = $auditRefresh.startedAt
    finishedAt = $auditRefresh.finishedAt
    durationSeconds = $auditRefresh.durationSeconds
    outputTail = Get-OutputTail -Text $auditRefresh.output
  })

Write-Host ""
Write-Host "Full release verification report: $(Get-RelativePath -BasePath $repoRoot -TargetPath $ReportPath)"
if ($failed) {
  Write-Host "Full release verification failed." -ForegroundColor Yellow
  exit 1
}

Write-Host "Full release verification passed."
