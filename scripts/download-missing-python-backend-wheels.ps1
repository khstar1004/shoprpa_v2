param(
  [string]$PythonExe = "",
  [string]$Wheelhouse = $env:SHOPRPA_PYTHON_WHEELHOUSE,
  [string]$RequirementsPath = "",
  [string]$ReportPath = "",
  [string]$IndexUrl = $env:SHOPRPA_PIP_INDEX_URL,
  [switch]$NoValidate,
  [switch]$ListOnly,
  [switch]$ContinueOnError,
  [int]$PipRetries = 5,
  [int]$PipTimeoutSeconds = 15
)

$ErrorActionPreference = "Stop"

trap {
  $message = $_.Exception.Message
  if (-not $message) {
    $message = [string]$_
  }
  Write-Host $message -ForegroundColor Yellow
  exit 1
}

function Resolve-RepoRoot {
  $scriptDir = Split-Path -Parent $PSCommandPath
  return (Resolve-Path (Join-Path $scriptDir "..")).Path
}

function Resolve-PythonExe {
  param([string]$RequestedPythonExe)

  if ($RequestedPythonExe) {
    if (-not (Test-Path -LiteralPath $RequestedPythonExe -PathType Leaf)) {
      throw "Python executable was not found: $RequestedPythonExe"
    }
    return (Resolve-Path -LiteralPath $RequestedPythonExe).Path
  }

  $pythonCommand = Get-Command "python" -ErrorAction SilentlyContinue
  if ($pythonCommand) {
    return $pythonCommand.Source
  }

  $knownPython313 = Join-Path $env:LOCALAPPDATA "Programs\Python\Python313\python.exe"
  if (Test-Path -LiteralPath $knownPython313 -PathType Leaf) {
    return $knownPython313
  }

  throw "python.exe is not on PATH and Python 3.13 was not found under LOCALAPPDATA."
}

function Get-PythonPrepHostInfo {
  param([string]$PythonExe)

  $script = @"
import json
import platform
import sys

print(json.dumps({
    "version": f"{sys.version_info.major}.{sys.version_info.minor}",
    "system": platform.system(),
    "machine": platform.machine(),
}))
"@

  $psi = [System.Diagnostics.ProcessStartInfo]::new()
  $psi.FileName = $PythonExe
  $psi.Arguments = "-c " + '"' + ($script -replace '"', '\"') + '"'
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

  if ($process.ExitCode -ne 0) {
    throw "Could not inspect Python prep host with ${PythonExe}: $stderr"
  }

  return ($stdout.Trim() | ConvertFrom-Json)
}

function Assert-WindowsPython313PrepHost {
  param([string]$PythonExe)

  $info = Get-PythonPrepHostInfo -PythonExe $PythonExe
  $machine = [string]$info.machine
  $isX64 = $machine -match "^(AMD64|x86_64)$"
  if ([string]$info.version -ne "3.13" -or [string]$info.system -ne "Windows" -or -not $isX64) {
    throw "Python wheelhouse download must run on a Windows/Python 3.13 x64 prep host. Current host: python=$($info.version), system=$($info.system), machine=$machine."
  }
  return $info
}

function Get-RequirementLines {
  param([string]$Path)

  return @(
    Get-Content -LiteralPath $Path |
      ForEach-Object { $_.Trim() } |
      Where-Object { $_ -and -not $_.StartsWith("#") }
  )
}

function Test-PolicyBlockedOutput {
  param([string]$Text)

  if (-not $Text) {
    return $false
  }
  return (
    $Text -match "WinError\s+10013" -or
    $Text -match "액세스 권한에 의해 숨겨진 소켓" -or
    $Text -match "socket access" -or
    $Text -match "Temporary failure in name resolution" -or
    $Text -match "Could not find a version that satisfies the requirement .+ \(from versions: none\)"
  )
}

function Get-OutputTail {
  param(
    [string]$Output,
    [int]$MaxLength = 12000
  )

  if (-not $Output) {
    return ""
  }

  $normalized = $Output.Trim()
  if ($normalized.Length -gt $MaxLength) {
    return $normalized.Substring($normalized.Length - $MaxLength)
  }
  return $normalized
}

function Write-DownloadReport {
  param(
    [string]$Path,
    [string]$Status,
    [bool]$Ok,
    [string]$PythonExe,
    [string]$WheelhousePath,
    [string]$RequirementsFile,
    [int]$RequirementsCount,
    [string]$Index,
    [string]$Step,
    [int]$ExitCode,
    [bool]$BlockedByPolicy,
    [string]$Message,
    [string]$Output,
    [object[]]$PackageResults = @(),
    [string[]]$DownloadedRequirements = @(),
    [string[]]$FailedRequirements = @(),
    [string[]]$BlockedRequirements = @()
  )

  if (-not $Path) {
    return
  }
  if (-not [System.IO.Path]::IsPathRooted($Path)) {
    $Path = Join-Path $repoRoot $Path
  }

  $reportDir = Split-Path -Parent $Path
  if ($reportDir -and -not (Test-Path -LiteralPath $reportDir -PathType Container)) {
    New-Item -ItemType Directory -Path $reportDir | Out-Null
  }

  $outputTail = Get-OutputTail -Output $Output

  $report = [PSCustomObject]@{
    schemaVersion = 1
    generatedAt = (Get-Date).ToString("o")
    ok = $Ok
    status = $Status
    step = $Step
    exitCode = $ExitCode
    blockedByPolicy = $BlockedByPolicy
    python = $PythonExe
    wheelhouse = $WheelhousePath
    requirementsPath = $RequirementsFile
    requirementsCount = $RequirementsCount
    downloadedCount = @($DownloadedRequirements).Count
    failedCount = @($FailedRequirements).Count
    blockedCount = @($BlockedRequirements).Count
    pipRetries = $PipRetries
    pipTimeoutSeconds = $PipTimeoutSeconds
    downloadedRequirements = @($DownloadedRequirements)
    failedRequirements = @($FailedRequirements)
    blockedRequirements = @($BlockedRequirements)
    packageResults = @($PackageResults)
    indexUrlConfigured = -not [string]::IsNullOrWhiteSpace($Index)
    message = $Message
    outputTail = $outputTail
  }
  $report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $Path -Encoding UTF8

  $summaryPath = [System.IO.Path]::ChangeExtension($Path, ".md")
  $lines = New-Object System.Collections.Generic.List[string]
  $lines.Add("# Python Backend Wheelhouse Download Report") | Out-Null
  $lines.Add("") | Out-Null
  $lines.Add("Generated: $($report.generatedAt)") | Out-Null
  $lines.Add("Status: $Status") | Out-Null
  $lines.Add("Step: $Step") | Out-Null
  $lines.Add("Exit code: $ExitCode") | Out-Null
  $lines.Add("Requirements: $RequirementsCount") | Out-Null
  $lines.Add("Downloaded: $($report.downloadedCount)") | Out-Null
  $lines.Add("Failed: $($report.failedCount)") | Out-Null
  $lines.Add("Policy blocked: $($report.blockedCount)") | Out-Null
  $lines.Add("pip retries: $PipRetries") | Out-Null
  $lines.Add("pip timeout: ${PipTimeoutSeconds}s") | Out-Null
  $lines.Add("Wheelhouse: $WheelhousePath") | Out-Null
  $lines.Add("Requirements file: $RequirementsFile") | Out-Null
  if ($report.failedCount -gt 0) {
    $lines.Add("") | Out-Null
    $lines.Add("Failed requirements:") | Out-Null
    foreach ($requirement in $FailedRequirements) {
      $lines.Add("- $requirement") | Out-Null
    }
  }
  if ($BlockedByPolicy) {
    $lines.Add("") | Out-Null
    $lines.Add("Blocked by host network or endpoint security policy.") | Out-Null
    $lines.Add("") | Out-Null
    $lines.Add("Next actions:") | Out-Null
    $lines.Add("- Run the same command on an online Windows/Python 3.13 prep host where Python can access the package index.") | Out-Null
    $lines.Add("- Or set SHOPRPA_PIP_INDEX_URL to an allowed private PyPI mirror.") | Out-Null
    $lines.Add("- Add -ContinueOnError to collect every still-missing package instead of stopping on the first blocked package.") | Out-Null
    $lines.Add("- Copy the populated Python 3.13-compatible wheelhouse back and rerun: corepack pnpm run setup:python-backends -- -Wheelhouse <path> -Offline -CheckOnly") | Out-Null
  }
  elseif ($Message) {
    $lines.Add("") | Out-Null
    $lines.Add($Message) | Out-Null
  }
  Set-Content -LiteralPath $summaryPath -Value $lines -Encoding UTF8
}

function Invoke-Checked {
  param(
    [string]$FilePath,
    [string[]]$Arguments,
    [string]$WorkingDirectory,
    [string]$Label
  )

  $psi = [System.Diagnostics.ProcessStartInfo]::new()
  $psi.FileName = $FilePath
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

  $output = (($stdout + "`n" + $stderr).Trim())
  if ($output) {
    Write-Host $output
  }

  if ($process.ExitCode -ne 0) {
    if (Test-PolicyBlockedOutput -Text $output) {
      Write-Host ""
      Write-Host "Package download is blocked by the host network, endpoint security policy, or configured package index availability." -ForegroundColor Yellow
      Write-Host "Run this command on an online Windows/Python 3.13 prep host where Python can access the package index, or set SHOPRPA_PIP_INDEX_URL to an allowed private PyPI mirror." -ForegroundColor Yellow
      Write-Host "Then copy the populated wheelhouse back and rerun: corepack pnpm run setup:python-backends -- -Wheelhouse <path> -Offline -CheckOnly" -ForegroundColor Yellow
    }
  }

  return [PSCustomObject]@{
    ok = $process.ExitCode -eq 0
    exitCode = $process.ExitCode
    output = $output
  }
}

$repoRoot = Resolve-RepoRoot
if (-not $RequirementsPath) {
  $RequirementsPath = Join-Path $repoRoot "build\python-backend-requirements\python-backend-wheelhouse-missing.requirements.txt"
}
if (-not $ReportPath) {
  $ReportPath = Join-Path $repoRoot "build\python-backend-requirements\python-backend-wheelhouse-download-report.json"
}
if (-not [System.IO.Path]::IsPathRooted($RequirementsPath)) {
  $RequirementsPath = Join-Path $repoRoot $RequirementsPath
}
if (-not (Test-Path -LiteralPath $RequirementsPath -PathType Leaf)) {
  throw "Missing requirements file was not found: $RequirementsPath. Run corepack pnpm run setup:python-backends -- -Wheelhouse <path> -Offline -CheckOnly first."
}
if ($PipRetries -lt 0) {
  throw "-PipRetries must be 0 or greater."
}
if ($PipTimeoutSeconds -lt 1) {
  throw "-PipTimeoutSeconds must be 1 or greater."
}

$requirements = @(Get-RequirementLines -Path $RequirementsPath)
if ($requirements.Count -eq 0) {
  Write-Host "No missing Python backend wheels are listed in $RequirementsPath"
  exit 0
}

if ($ListOnly) {
  $requirements | ForEach-Object { Write-Host $_ }
  exit 0
}

if (-not $Wheelhouse) {
  $Wheelhouse = Join-Path $repoRoot "build\tmp\offline-wheelhouse"
}
if (-not (Test-Path -LiteralPath $Wheelhouse -PathType Container)) {
  New-Item -ItemType Directory -Path $Wheelhouse | Out-Null
}

$python = Resolve-PythonExe -RequestedPythonExe $PythonExe
$prepHostInfo = Assert-WindowsPython313PrepHost -PythonExe $python
$resolvedWheelhouse = (Resolve-Path -LiteralPath $Wheelhouse).Path
$resolvedRequirementsPath = (Resolve-Path -LiteralPath $RequirementsPath).Path

Write-Host "Using Python: $python"
Write-Host "Wheelhouse target: Windows Python $($prepHostInfo.version) $($prepHostInfo.machine)"
Write-Host "Missing requirements: $resolvedRequirementsPath"
Write-Host "Wheelhouse: $resolvedWheelhouse"
Write-Host "Missing compatible locked wheels: $($requirements.Count)"
Write-Host "pip retry policy: retries=$PipRetries, timeout=${PipTimeoutSeconds}s"
if ($IndexUrl) {
  Write-Host "Using package index: $IndexUrl"
}
if ($ContinueOnError) {
  Write-Host "Continuing after package download failures to produce a complete missing-wheel report."
}

$downloadArgsBase = @(
  "-m",
  "pip",
  "download",
  "--only-binary=:all:",
  "--no-deps",
  "--retries",
  ([string]$PipRetries),
  "--timeout",
  ([string]$PipTimeoutSeconds),
  "--dest",
  $resolvedWheelhouse
)
if ($IndexUrl) {
  $downloadArgsBase += @("--index-url", $IndexUrl)
}

$packageResults = New-Object System.Collections.Generic.List[object]
$downloadedRequirements = New-Object System.Collections.Generic.List[string]
$failedRequirements = New-Object System.Collections.Generic.List[string]
$blockedRequirements = New-Object System.Collections.Generic.List[string]

foreach ($requirement in $requirements) {
  Write-Host "Downloading: $requirement"
  $downloadResult = Invoke-Checked -FilePath $python -Arguments ($downloadArgsBase + @($requirement)) -WorkingDirectory $repoRoot -Label "pip download $requirement"
  $downloadOutput = [string]$downloadResult.output
  $blockedByPolicy = Test-PolicyBlockedOutput -Text $downloadOutput
  $itemStatus = if ($downloadResult.ok -eq $true) {
    "PASS"
  }
  elseif ($blockedByPolicy) {
    "BLOCKED"
  }
  else {
    "FAILED"
  }

  if ($downloadResult.ok -eq $true) {
    $downloadedRequirements.Add($requirement) | Out-Null
  }
  else {
    $failedRequirements.Add($requirement) | Out-Null
    if ($blockedByPolicy) {
      $blockedRequirements.Add($requirement) | Out-Null
    }
  }

  $packageResults.Add([PSCustomObject]@{
      requirement = $requirement
      ok = $downloadResult.ok -eq $true
      status = $itemStatus
      exitCode = $downloadResult.exitCode
      blockedByPolicy = $blockedByPolicy
      outputTail = Get-OutputTail -Output $downloadOutput -MaxLength 4000
    }) | Out-Null

  if ($downloadResult.ok -ne $true -and -not $ContinueOnError) {
    $downloadStatus = if ($blockedByPolicy) { "BLOCKED" } else { "FAILED" }
    $message = if ($blockedByPolicy) {
      "Package download is blocked by the host network, endpoint security policy, or configured package index availability."
    }
    else {
      "pip download failed for $requirement with exit code $($downloadResult.exitCode)."
    }
    Write-DownloadReport -Path $ReportPath -Status $downloadStatus -Ok $false -PythonExe $python -WheelhousePath $resolvedWheelhouse -RequirementsFile $resolvedRequirementsPath -RequirementsCount $requirements.Count -Index $IndexUrl -Step "pip download $requirement" -ExitCode $downloadResult.exitCode -BlockedByPolicy $blockedByPolicy -Message $message -Output $downloadOutput -PackageResults $packageResults.ToArray() -DownloadedRequirements $downloadedRequirements.ToArray() -FailedRequirements $failedRequirements.ToArray() -BlockedRequirements $blockedRequirements.ToArray()
    throw "pip download failed for $requirement with exit code $($downloadResult.exitCode)"
  }
}

if ($failedRequirements.Count -gt 0) {
  $blockedByPolicy = $blockedRequirements.Count -gt 0
  $downloadStatus = if ($downloadedRequirements.Count -gt 0) {
    if ($blockedByPolicy) { "PARTIAL_BLOCKED" } else { "PARTIAL_FAILED" }
  }
  elseif ($blockedByPolicy) {
    "BLOCKED"
  }
  else {
    "FAILED"
  }
  $message = if ($blockedByPolicy) {
    "One or more package downloads are blocked by the host network, endpoint security policy, or configured package index availability."
  }
  else {
    "One or more package downloads failed."
  }
  Write-DownloadReport -Path $ReportPath -Status $downloadStatus -Ok $false -PythonExe $python -WheelhousePath $resolvedWheelhouse -RequirementsFile $resolvedRequirementsPath -RequirementsCount $requirements.Count -Index $IndexUrl -Step "pip download" -ExitCode 1 -BlockedByPolicy $blockedByPolicy -Message $message -Output "" -PackageResults $packageResults.ToArray() -DownloadedRequirements $downloadedRequirements.ToArray() -FailedRequirements $failedRequirements.ToArray() -BlockedRequirements $blockedRequirements.ToArray()
  throw "$($failedRequirements.Count) Python backend wheel downloads failed."
}

if (-not $NoValidate) {
  $setupScript = Join-Path $repoRoot "scripts\setup-python-backends.ps1"
  $powershellExe = "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe"
  $validationResult = Invoke-Checked -FilePath $powershellExe -Arguments @(
    "-NoProfile",
    "-ExecutionPolicy",
    "Bypass",
    "-File",
    $setupScript,
    "-Wheelhouse",
    $resolvedWheelhouse,
    "-Offline",
    "-CheckOnly"
  ) -WorkingDirectory $repoRoot -Label "offline wheelhouse validation"
  if ($validationResult.ok -ne $true) {
    $message = "offline wheelhouse validation failed with exit code $($validationResult.exitCode)."
    Write-DownloadReport -Path $ReportPath -Status "VALIDATION_FAILED" -Ok $false -PythonExe $python -WheelhousePath $resolvedWheelhouse -RequirementsFile $resolvedRequirementsPath -RequirementsCount $requirements.Count -Index $IndexUrl -Step "offline wheelhouse validation" -ExitCode $validationResult.exitCode -BlockedByPolicy $false -Message $message -Output ([string]$validationResult.output) -PackageResults $packageResults.ToArray() -DownloadedRequirements $downloadedRequirements.ToArray() -FailedRequirements $failedRequirements.ToArray() -BlockedRequirements $blockedRequirements.ToArray()
    throw $message
  }
}

Write-DownloadReport -Path $ReportPath -Status "PASS" -Ok $true -PythonExe $python -WheelhousePath $resolvedWheelhouse -RequirementsFile $resolvedRequirementsPath -RequirementsCount $requirements.Count -Index $IndexUrl -Step "download and validation" -ExitCode 0 -BlockedByPolicy $false -Message "Missing Python backend wheels downloaded and validated." -Output "" -PackageResults $packageResults.ToArray() -DownloadedRequirements $downloadedRequirements.ToArray() -FailedRequirements $failedRequirements.ToArray() -BlockedRequirements $blockedRequirements.ToArray()
Write-Host "Missing Python backend wheels downloaded and validated."
