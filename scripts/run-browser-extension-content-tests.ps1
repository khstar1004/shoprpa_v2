param(
  [int]$TimeoutSeconds = 60
)

$ErrorActionPreference = "Stop"

function Resolve-RepoRoot {
  $scriptDir = Split-Path -Parent $PSCommandPath
  return (Resolve-Path (Join-Path $scriptDir "..")).Path
}

function Remove-Ansi {
  param([string]$Text)
  if (-not $Text) {
    return ""
  }
  return ($Text -replace "\x1B\[[0-9;?]*[ -/]*[@-~]", "")
}

function Get-Summary {
  param(
    [string]$Output,
    [bool]$TimedOut,
    [int]$ExitCode,
    [string]$ErrorMessage
  )

  $clean = Remove-Ansi $Output
  if ($TimedOut) {
    return "Timed out after $($TimeoutSeconds * 1000) ms"
  }
  if ($ErrorMessage) {
    return $ErrorMessage
  }
  if ($clean -match "spawn EPERM") {
    return "Chromium launch is blocked by host policy: spawn EPERM"
  }
  if ($clean -match "Executable doesn't exist at\s+([^\r\n]+)") {
    return "Playwright browser executable is missing: $($Matches[1].Trim())"
  }
  if ($clean -match "(?s)Test Files\s+(\d+ passed).*Tests\s+(\d+ passed)") {
    return "$($Matches[1]), $($Matches[2])"
  }
  if ($clean -match "Errors\s+([^\r\n]+)") {
    return "Errors $($Matches[1].Trim())"
  }
  foreach ($line in ($clean -split "`r?`n")) {
    $trimmed = $line.Trim()
    if ($trimmed) {
      return $trimmed
    }
  }
  return "Exited with code $ExitCode"
}

function Test-HostPolicyBlockedOutput {
  param([string]$Text)

  if (-not $Text) {
    return $false
  }
  return (
    $Text -match "spawn EPERM" -or
    $Text -match "access is denied" -or
    $Text -match "액세스가 거부되었습니다" -or
    $Text -match "permission denied"
  )
}

function Stop-ProcessTree {
  param([int]$ProcessId)

  $taskkill = Join-Path $env:SystemRoot "System32\taskkill.exe"
  if (Test-Path -LiteralPath $taskkill -PathType Leaf) {
    try {
      & $taskkill /PID $ProcessId /T /F | Out-Null
      return
    }
    catch {}
  }

  try {
    $target = [System.Diagnostics.Process]::GetProcessById($ProcessId)
    if (-not $target.HasExited) {
      $target.Kill()
    }
  }
  catch {}
}

function Invoke-CapturedWithTimeout {
  param(
    [string]$FilePath,
    [string[]]$Arguments,
    [string]$WorkingDirectory,
    [int]$TimeoutSeconds,
    [string]$StdoutPath,
    [string]$StderrPath
  )

  Remove-Item -LiteralPath $StdoutPath, $StderrPath -Force -ErrorAction SilentlyContinue

  $psi = [System.Diagnostics.ProcessStartInfo]::new()
  $psi.FileName = $env:ComSpec
  $commandLine = "$FilePath " + (($Arguments | ForEach-Object {
        if ($_ -match '[\s"]') {
          '"' + ($_ -replace '"', '\"') + '"'
        }
        else {
          $_
        }
      }) -join " ")
  $psi.Arguments = "/d /c $commandLine 1> `"$StdoutPath`" 2> `"$StderrPath`""
  $psi.WorkingDirectory = $WorkingDirectory
  $psi.RedirectStandardOutput = $false
  $psi.RedirectStandardError = $false
  $psi.UseShellExecute = $false
  $psi.CreateNoWindow = $true

  $process = [System.Diagnostics.Process]::new()
  $process.StartInfo = $psi

  [void]$process.Start()
  $timedOut = -not $process.WaitForExit($TimeoutSeconds * 1000)
  $terminationFailed = $false
  if ($timedOut) {
    Stop-ProcessTree -ProcessId $process.Id
    if (-not $process.WaitForExit(5000)) {
      $terminationFailed = $true
    }
  }
  $stdout = if (Test-Path -LiteralPath $StdoutPath -PathType Leaf) { Get-Content -LiteralPath $StdoutPath -Raw } else { "" }
  $stderr = if (Test-Path -LiteralPath $StderrPath -PathType Leaf) { Get-Content -LiteralPath $StderrPath -Raw } else { "" }
  $output = (($stdout + "`n" + $stderr).Trim())
  if ($terminationFailed) {
    $output = (($output, "Timed out after ${TimeoutSeconds}s and the process tree could not be terminated by this host policy.") | Where-Object { $_ }) -join "`n"
  }

  [PSCustomObject]@{
    ExitCode = if ($process.HasExited) { $process.ExitCode } else { 124 }
    TimedOut = $timedOut
    TerminationFailed = $terminationFailed
    Output = $output
  }
}

$repoRoot = Resolve-RepoRoot
$reportRoot = Join-Path $repoRoot "build\browser-extension-content-tests"
$reportPath = Join-Path $reportRoot "browser-extension-content-tests-report.json"
if (-not (Test-Path -LiteralPath $reportRoot -PathType Container)) {
  New-Item -ItemType Directory -Path $reportRoot | Out-Null
}

$commandText = "corepack pnpm --dir frontend --filter @rpa/extension run test:browser"
$stdoutPath = Join-Path $reportRoot "browser-extension-content-tests.stdout.log"
$stderrPath = Join-Path $reportRoot "browser-extension-content-tests.stderr.log"
try {
  $result = Invoke-CapturedWithTimeout -FilePath "corepack" -Arguments @("pnpm", "--dir", "frontend", "--filter", "@rpa/extension", "run", "test:browser") -WorkingDirectory $repoRoot -TimeoutSeconds $TimeoutSeconds -StdoutPath $stdoutPath -StderrPath $stderrPath
  $cleanOutput = Remove-Ansi $result.Output
  $summary = Get-Summary -Output $cleanOutput -TimedOut $result.TimedOut -ExitCode $result.ExitCode -ErrorMessage ""
  $hostPolicyBlocked = Test-HostPolicyBlockedOutput -Text $cleanOutput
  $failureClass = if ($hostPolicyBlocked) {
    "browser-host-policy-blocked"
  }
  elseif ($result.TimedOut) {
    "browser-content-tests-timeout"
  }
  elseif ($result.ExitCode -ne 0) {
    "browser-content-tests-failed"
  }
  else {
    ""
  }
  $report = [ordered]@{
    ok = (-not $result.TimedOut -and $result.ExitCode -eq 0)
    generatedAt = (Get-Date).ToUniversalTime().ToString("o")
    command = $commandText
    exitCode = $result.ExitCode
    timedOut = $result.TimedOut
    terminationFailed = $result.TerminationFailed
    hostPolicyBlocked = $hostPolicyBlocked
    failureClass = $failureClass
    summary = $summary
    output = $(if ($cleanOutput.Length -gt 30000) { $cleanOutput.Substring($cleanOutput.Length - 30000) } else { $cleanOutput })
  }
}
catch {
  $summary = $_.Exception.Message
  $hostPolicyBlocked = Test-HostPolicyBlockedOutput -Text $summary
  $report = [ordered]@{
    ok = $false
    generatedAt = (Get-Date).ToUniversalTime().ToString("o")
    command = $commandText
    exitCode = 1
    timedOut = $false
    terminationFailed = $false
    hostPolicyBlocked = $hostPolicyBlocked
    failureClass = $(if ($hostPolicyBlocked) { "browser-host-policy-blocked" } else { "browser-content-tests-failed" })
    summary = $summary
    output = $_.ScriptStackTrace
  }
}

$report | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $reportPath -Encoding UTF8

if ($report.ok) {
  Write-Host "Browser extension content tests passed: $reportPath"
  Write-Host $report.summary
}
else {
  [Console]::Error.WriteLine("Browser extension content tests failed: $reportPath")
  [Console]::Error.WriteLine($report.summary)
  if ($report.failureClass) {
    [Console]::Error.WriteLine("failureClass=$($report.failureClass)")
  }
  exit 1
}
