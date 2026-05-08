param(
  [int]$TimeoutSeconds = 120
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
  if ($ExitCode -eq 0) {
    return "vue-tsc passed"
  }
  foreach ($line in ($clean -split "`r?`n")) {
    $trimmed = $line.Trim()
    if ($trimmed -match "error TS\d+:|ERR_PNPM|Exit status|Command failed") {
      return $trimmed
    }
  }
  foreach ($line in ($clean -split "`r?`n")) {
    $trimmed = $line.Trim()
    if ($trimmed) {
      return $trimmed
    }
  }
  return "Exited with code $ExitCode"
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
$reportRoot = Join-Path $repoRoot "build\frontend-typecheck"
$reportPath = Join-Path $reportRoot "frontend-typecheck-report.json"
if (-not (Test-Path -LiteralPath $reportRoot -PathType Container)) {
  New-Item -ItemType Directory -Path $reportRoot | Out-Null
}

$commandText = "corepack pnpm --dir frontend --filter @rpa/web-app run tsc"
$stdoutPath = Join-Path $reportRoot "frontend-typecheck.stdout.log"
$stderrPath = Join-Path $reportRoot "frontend-typecheck.stderr.log"
try {
  $result = Invoke-CapturedWithTimeout -FilePath "corepack" -Arguments @("pnpm", "--dir", "frontend", "--filter", "@rpa/web-app", "run", "tsc") -WorkingDirectory $repoRoot -TimeoutSeconds $TimeoutSeconds -StdoutPath $stdoutPath -StderrPath $stderrPath
  $cleanOutput = Remove-Ansi $result.Output
  $summary = Get-Summary -Output $cleanOutput -TimedOut $result.TimedOut -ExitCode $result.ExitCode -ErrorMessage ""
  $report = [ordered]@{
    ok = (-not $result.TimedOut -and $result.ExitCode -eq 0)
    generatedAt = (Get-Date).ToUniversalTime().ToString("o")
    command = $commandText
    exitCode = $result.ExitCode
    timedOut = $result.TimedOut
    terminationFailed = $result.TerminationFailed
    summary = $summary
    output = $(if ($cleanOutput.Length -gt 30000) { $cleanOutput.Substring($cleanOutput.Length - 30000) } else { $cleanOutput })
  }
}
catch {
  $summary = $_.Exception.Message
  $report = [ordered]@{
    ok = $false
    generatedAt = (Get-Date).ToUniversalTime().ToString("o")
    command = $commandText
    exitCode = 1
    timedOut = $false
    terminationFailed = $false
    summary = $summary
    output = $_.ScriptStackTrace
  }
}

$report | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $reportPath -Encoding UTF8

if ($report.ok) {
  Write-Host "Frontend typecheck passed: $reportPath"
  Write-Host $report.summary
}
else {
  [Console]::Error.WriteLine("Frontend typecheck failed: $reportPath")
  [Console]::Error.WriteLine($report.summary)
  exit 1
}
