param(
  [string]$PythonExe = "",
  [string]$ReportPath = "",
  [int]$SuiteTimeoutSeconds = 300
)

$ErrorActionPreference = "Stop"

function Resolve-RepoRoot {
  $scriptDir = Split-Path -Parent $PSCommandPath
  return (Resolve-Path (Join-Path $scriptDir "..")).Path
}

function Assert-File {
  param(
    [string]$Path,
    [string]$Label
  )

  if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
    throw "$Label is missing: $Path"
  }
}

function Invoke-Captured {
  param(
    [string]$FilePath,
    [string[]]$Arguments,
    [string]$WorkingDirectory,
    [int]$TimeoutSeconds = 300
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
  $stdoutTask = $process.StandardOutput.ReadToEndAsync()
  $stderrTask = $process.StandardError.ReadToEndAsync()

  $timedOut = $false
  $terminationFailed = $false
  if (-not $process.WaitForExit([Math]::Max(1, $TimeoutSeconds) * 1000)) {
    $timedOut = $true
    try {
      $process.Kill()
    }
    catch {
      # The process may have exited between timeout detection and Kill().
    }
    if (-not $process.WaitForExit(5000)) {
      $terminationFailed = $true
    }
  }
  else {
    $process.WaitForExit()
  }

  $stdout = if ($terminationFailed) { "" } else { $stdoutTask.Result }
  $stderr = if ($terminationFailed) { "" } else { $stderrTask.Result }
  $output = (($stdout + "`n" + $stderr).Trim())
  if ($terminationFailed) {
    $output = "Timed out after ${TimeoutSeconds}s and the process could not be terminated by this host policy."
  }

  [PSCustomObject]@{
    ExitCode = if ($timedOut) { -1 } else { $process.ExitCode }
    TimedOut = $timedOut
    TerminationFailed = $terminationFailed
    Output = $output
  }
}

function Get-TestCount {
  param([string]$Output)

  $match = [regex]::Match($Output, "Ran\s+(\d+)\s+tests?")
  if ($match.Success) {
    return [int]$match.Groups[1].Value
  }
  return 0
}

function Get-SkipCount {
  param([string]$Output)

  $match = [regex]::Match($Output, "skipped=(\d+)")
  if ($match.Success) {
    return [int]$match.Groups[1].Value
  }
  return 0
}

function Write-EngineRuntimeReport {
  param(
    [string]$Path,
    [bool]$Ok,
    [string]$PythonPath,
    [int]$TotalSuites,
    [int]$TotalTests,
    [int]$TotalSkipped,
    [object[]]$SuiteResults,
    [string]$Failure = ""
  )

  $reportDir = Split-Path -Parent $Path
  if (-not (Test-Path -LiteralPath $reportDir -PathType Container)) {
    New-Item -ItemType Directory -Path $reportDir | Out-Null
  }

  $report = [PSCustomObject]@{
    ok = $Ok
    generatedAt = (Get-Date).ToString("o")
    python = $PythonPath
    totalSuites = $TotalSuites
    totalTests = $TotalTests
    totalSkipped = $TotalSkipped
    suiteTimeoutSeconds = $SuiteTimeoutSeconds
    suites = @($SuiteResults)
    failure = $Failure
  }
  $report | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $Path -Encoding UTF8
}

$repoRoot = Resolve-RepoRoot
if (-not $PythonExe) {
  $PythonExe = Join-Path $repoRoot "build\python_core\python.exe"
}
if (-not $ReportPath) {
  $ReportPath = Join-Path $repoRoot "build\engine-runtime-tests.json"
}
$PythonExe = (Resolve-Path -LiteralPath $PythonExe).Path
Assert-File $PythonExe "Bundled Python runtime"

$suites = @(
  @{
    Name = "baseline"
    Args = @("-m", "unittest", "discover", "-s", "engine\shared\astronverse-baseline\tests", "-p", "test_*.py")
  },
  @{
    Name = "atom-metadata"
    Args = @("-m", "unittest", "discover", "-s", "engine\tests", "-p", "test_*.py")
  },
  @{
    Name = "dataprocess"
    Args = @("-m", "unittest", "discover", "-s", "engine\components\astronverse-dataprocess\tests", "-p", "test_*.py")
  },
  @{
    Name = "datatable"
    Args = @("-m", "unittest", "discover", "-s", "engine\components\astronverse-datatable\tests", "-p", "test_*.py")
  },
  @{
    Name = "encrypt"
    Args = @("-m", "unittest", "discover", "-s", "engine\components\astronverse-encrypt\tests", "-p", "test_*.py")
  },
  @{
    Name = "enterprise"
    Args = @("-m", "unittest", "discover", "-s", "engine\components\astronverse-enterprise\tests", "-p", "test_*.py")
  },
  @{
    Name = "email"
    Args = @("-m", "unittest", "discover", "-s", "engine\components\astronverse-email\tests", "-p", "test_*.py")
  },
  @{
    Name = "script"
    Args = @("-m", "unittest", "discover", "-s", "engine\components\astronverse-script\tests", "-p", "test_*.py")
  },
  @{
    Name = "system-clipboard"
    Args = @("-m", "unittest", "discover", "-s", "engine\components\astronverse-system\tests", "-p", "test_clipboard.py")
  },
  @{
    Name = "system-file"
    Args = @("-m", "unittest", "discover", "-s", "engine\components\astronverse-system\tests", "-p", "test_file.py")
  },
  @{
    Name = "system-folder"
    Args = @("-m", "unittest", "discover", "-s", "engine\components\astronverse-system\tests", "-p", "test_folder.py")
  },
  @{
    Name = "system-compress"
    Args = @("-m", "unittest", "discover", "-s", "engine\components\astronverse-system\tests", "-p", "test_compress.py")
  },
  @{
    Name = "system-process"
    Args = @("-m", "unittest", "discover", "-s", "engine\components\astronverse-system\tests", "-p", "test_process.py")
  },
  @{
    Name = "system-screen"
    Args = @("-m", "unittest", "discover", "-s", "engine\components\astronverse-system\tests", "-p", "test_system.py")
  }
)

$results = New-Object System.Collections.Generic.List[object]
$totalTests = 0
$totalSkipped = 0

Write-Host "Engine runtime test Python: $PythonExe"
foreach ($suite in $suites) {
  Write-Host "Running engine runtime suite: $($suite.Name)"
  $pythonArgs = @("-W", "ignore::ResourceWarning") + $suite.Args
  $result = Invoke-Captured -FilePath $PythonExe -Arguments $pythonArgs -WorkingDirectory $repoRoot -TimeoutSeconds $SuiteTimeoutSeconds

  $testCount = Get-TestCount -Output $result.Output
  $skipCount = Get-SkipCount -Output $result.Output
  $totalTests += $testCount
  $totalSkipped += $skipCount
  $status = if (($result.ExitCode -eq 0) -and (-not $result.TimedOut)) { "PASS" } else { "FAIL" }
  $results.Add([PSCustomObject]@{
      name = $suite.Name
      status = $status
      exitCode = $result.ExitCode
      timedOut = $result.TimedOut
      terminationFailed = $result.TerminationFailed
      tests = $testCount
      skipped = $skipCount
    }) | Out-Null

  if (($result.ExitCode -ne 0) -or $result.TimedOut) {
    if ($result.Output) {
      Write-Host $result.Output
    }
    $failure = if ($result.TimedOut) {
      "Engine runtime suite timed out after $SuiteTimeoutSeconds seconds: $($suite.Name)"
    }
    else {
      "Engine runtime suite failed: $($suite.Name)"
    }
    Write-EngineRuntimeReport -Path $ReportPath -Ok $false -PythonPath $PythonExe -TotalSuites $suites.Count -TotalTests $totalTests -TotalSkipped $totalSkipped -SuiteResults $results -Failure $failure
    throw $failure
  }
  Write-Host "Suite passed: $($suite.Name) ($testCount tests, $skipCount skipped)"
}

Write-EngineRuntimeReport -Path $ReportPath -Ok $true -PythonPath $PythonExe -TotalSuites $suites.Count -TotalTests $totalTests -TotalSkipped $totalSkipped -SuiteResults $results

Write-Host "Engine runtime tests passed"
Write-Host "totalTests=$totalTests"
Write-Host "totalSkipped=$totalSkipped"
Write-Host "report=$ReportPath"
