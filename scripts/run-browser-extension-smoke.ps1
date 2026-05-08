param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path (Split-Path -Parent $PSCommandPath) "..")).Path
$scriptPath = Join-Path $repoRoot "scripts\run-browser-extension-smoke.cjs"
$extensionPath = Join-Path $repoRoot "frontend\packages\browser-plugin\dist"
$evidenceDir = Join-Path $repoRoot "build\browser-extension-smoke"
$reportPath = Join-Path $evidenceDir "browser-extension-smoke-report.json"
$pageHtmlPath = Join-Path $evidenceDir "browser-extension-smoke.html"
$userDataDir = Join-Path $repoRoot "build\browser-extension-smoke\chromium-profile"

function Resolve-BrowserExecutables {
  $candidates = @(
    $env:SHOPRPA_PLAYWRIGHT_EXECUTABLE,
    "C:\Program Files\Microsoft\Edge\Application\msedge.exe",
    "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    "C:\Program Files\Google\Chrome\Application\chrome.exe",
    "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"
  ) | Where-Object { $_ }

  $playwrightRoot = Join-Path $env:LOCALAPPDATA "ms-playwright"
  if (Test-Path -LiteralPath $playwrightRoot -PathType Container) {
    $playwrightCandidates = Get-ChildItem -LiteralPath $playwrightRoot -Directory -Filter "chromium-*" |
      Sort-Object Name -Descending |
      ForEach-Object {
        Join-Path $_.FullName "chrome-win64\chrome.exe"
        Join-Path $_.FullName "chrome-win\chrome.exe"
      }
    $candidates += @($playwrightCandidates)
  }

  foreach ($candidate in $candidates) {
    if (Test-Path -LiteralPath $candidate -PathType Leaf) {
      $candidate
    }
  }
}

function Get-FreeTcpPort {
  $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
  $listener.Start()
  try {
    return ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port
  }
  finally {
    $listener.Stop()
  }
}

function Wait-CdpEndpoint {
  param(
    [int]$Port,
    [System.Diagnostics.Process]$Process
  )

  $url = "http://127.0.0.1:$Port/json/version"
  for ($i = 0; $i -lt 30; $i++) {
    if ($Process) {
      $Process.Refresh()
      if ($Process.HasExited) {
        throw "Browser exited before opening CDP endpoint. ExitCode=$($Process.ExitCode), url=$url"
      }
    }
    try {
      $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 1
      if ($response.StatusCode -eq 200) {
        return
      }
    }
    catch {
      Start-Sleep -Milliseconds 250
    }
  }

  $detail = ""
  if ($Process) {
    try {
      $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $($Process.Id)" -ErrorAction SilentlyContinue
      if ($processInfo) {
        $detail = " CommandLine=$($processInfo.CommandLine)"
      }
    }
    catch {
      $detail = ""
    }
  }
  throw "Timed out waiting for browser CDP endpoint: $url.$detail"
}

function Stop-SmokeBrowserProcesses {
  param([string]$ProfilePath)

  $escapedProfilePath = [System.Management.Automation.WildcardPattern]::Escape($ProfilePath)
  $matchingProcesses = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object {
      $_.CommandLine -and $_.CommandLine -like "*$escapedProfilePath*"
    })

  foreach ($processInfo in $matchingProcesses) {
    Stop-Process -Id $processInfo.ProcessId -Force -ErrorAction SilentlyContinue
  }

  foreach ($processInfo in $matchingProcesses) {
    Wait-Process -Id $processInfo.ProcessId -Timeout 5 -ErrorAction SilentlyContinue
  }
}

function Remove-DirectoryWithRetry {
  param([string]$Path)

  for ($attempt = 1; $attempt -le 10; $attempt++) {
    if (-not (Test-Path -LiteralPath $Path -PathType Container)) {
      return
    }

    try {
      Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction Stop
      return
    }
    catch {
      if ($attempt -eq 10) {
        throw
      }
      Start-Sleep -Milliseconds 300
    }
  }
}

function Write-SmokeFailureReport {
  param([string]$Failure)

  if (-not (Test-Path -LiteralPath $evidenceDir -PathType Container)) {
    New-Item -ItemType Directory -Path $evidenceDir | Out-Null
  }

  $previousReport = $null
  if (Test-Path -LiteralPath $reportPath -PathType Leaf) {
    try {
      $previousReport = Get-Content -LiteralPath $reportPath -Raw | ConvertFrom-Json
    }
    catch {
      $previousReport = $null
    }
  }

  $manifestVersion = ""
  if (Test-Path -LiteralPath (Join-Path $extensionPath "manifest.json") -PathType Leaf) {
    try {
      $manifestVersion = [string]((Get-Content -LiteralPath (Join-Path $extensionPath "manifest.json") -Raw | ConvertFrom-Json).version)
    }
    catch {
      $manifestVersion = ""
    }
  }

  $hostPolicyBlocked = $Failure -match "spawn EPERM" -or
    $Failure -match "Access is denied" -or
    $Failure -match "액세스가 거부" -or
    $Failure -match "Timed out waiting for browser CDP endpoint"
  $directLaunchBlocked = $Failure -match "direct launch failed" -and $Failure -match "spawn EPERM"
  $cdpAttachFailed = $Failure -match "CDP attach failed" -or $Failure -match "CDP endpoint"
  $failureClass = if ($hostPolicyBlocked) {
    "browser-host-policy-blocked"
  }
  else {
    "browser-extension-smoke-failed"
  }

  $scenarios = @()
  if ($previousReport -and $previousReport.scenarios) {
    foreach ($scenario in @($previousReport.scenarios)) {
      if ($scenario -and ($scenario.PSObject.Properties.Name -contains "id")) {
        $scenarios += $scenario
      }
    }
  }
  $report = [ordered]@{
    schemaVersion = 1
    ok = $false
    generatedAt = (Get-Date).ToUniversalTime().ToString("o")
    extensionPath = $extensionPath
    manifestVersion = if ([string]$previousReport.manifestVersion) { [string]$previousReport.manifestVersion } else { $manifestVersion }
    extensionId = if ([string]$previousReport.extensionId) { [string]$previousReport.extensionId } else { "" }
    browserExecutable = if ([string]$previousReport.browserExecutable) { [string]$previousReport.browserExecutable } else { (@(Resolve-BrowserExecutables) -join " | ") }
    cdpUrl = if ([string]$previousReport.cdpUrl) { [string]$previousReport.cdpUrl } else { [string]$env:SHOPRPA_BROWSER_CDP_URL }
    testPageUrl = if ([string]$previousReport.testPageUrl) { [string]$previousReport.testPageUrl } else { "" }
    failureClass = $failureClass
    hostPolicyBlocked = $hostPolicyBlocked
    cdpAttachFailed = $cdpAttachFailed
    directLaunchBlocked = $directLaunchBlocked
    scenarios = $scenarios
    evidence = [ordered]@{
      screenshot = if ([string]$previousReport.evidence.screenshot) { [string]$previousReport.evidence.screenshot } else { "" }
      html = if ([string]$previousReport.evidence.html) { [string]$previousReport.evidence.html } elseif (Test-Path -LiteralPath $pageHtmlPath -PathType Leaf) { "build\browser-extension-smoke\browser-extension-smoke.html" } else { "" }
    }
    failures = @($Failure)
  }
  $report | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $reportPath -Encoding UTF8
}

function Get-SmokeFailureSummary {
  if (-not (Test-Path -LiteralPath $reportPath -PathType Leaf)) {
    return ""
  }

  try {
    $report = Get-Content -LiteralPath $reportPath -Raw | ConvertFrom-Json
    $failureText = [string](@($report.failures | Where-Object { $_ }) | Select-Object -First 1)
    $firstLine = @($failureText -split "`r?`n" | Where-Object { $_.Trim() } | Select-Object -First 1)
    if ($firstLine.Count -gt 0) {
      return $firstLine[0].Trim()
    }
  }
  catch {}

  return ""
}

function Invoke-NodeSmoke {
  $nodeCommand = Get-Command "node" -ErrorAction Stop
  $psi = [System.Diagnostics.ProcessStartInfo]::new()
  $psi.FileName = $nodeCommand.Source
  $psi.Arguments = "`"$scriptPath`""
  $psi.WorkingDirectory = $repoRoot
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

$smokeExitCode = 0
Push-Location $repoRoot
$browserProcess = $null
$previousCdpUrl = $env:SHOPRPA_BROWSER_CDP_URL
$previousExecutable = $env:SHOPRPA_PLAYWRIGHT_EXECUTABLE
try {
  if (Test-Path -LiteralPath $reportPath -PathType Leaf) {
    Remove-Item -LiteralPath $reportPath -Force
  }

  if ($previousCdpUrl) {
    Write-Host "Using existing browser CDP endpoint from SHOPRPA_BROWSER_CDP_URL=$previousCdpUrl"
    $nodeResult = Invoke-NodeSmoke
    if ($nodeResult.ExitCode -eq 0) {
      if ($nodeResult.Output) {
        Write-Host $nodeResult.Output
      }
      return
    }

    $cdpFailure = Get-SmokeFailureSummary
    if (-not $cdpFailure) {
      $firstNodeOutput = @($nodeResult.Output -split "`r?`n" | Where-Object { $_.Trim() } | Select-Object -First 1)
      if ($firstNodeOutput.Count -gt 0) {
        $cdpFailure = $firstNodeOutput[0].Trim()
      }
    }
    if ($cdpFailure) {
      throw "Browser extension smoke failed with provided SHOPRPA_BROWSER_CDP_URL. $cdpFailure"
    }
    throw "Browser extension smoke failed with provided SHOPRPA_BROWSER_CDP_URL and exit code $($nodeResult.ExitCode)."
  }

  $browserCandidates = @(Resolve-BrowserExecutables)
  if ($browserCandidates.Count -eq 0) {
    throw "No Chromium-compatible browser executable found."
  }

  $browserErrors = New-Object System.Collections.Generic.List[string]
  $selectedBrowserExe = ""
  $port = 0
  foreach ($browserExe in $browserCandidates) {
    $port = Get-FreeTcpPort
    Stop-SmokeBrowserProcesses -ProfilePath $userDataDir
    Remove-DirectoryWithRetry -Path $userDataDir
    New-Item -ItemType Directory -Path $userDataDir | Out-Null

    $browserArgs = @(
      "--remote-debugging-port=$port",
      "--remote-debugging-address=127.0.0.1",
      "--user-data-dir=$userDataDir",
      "--disable-extensions-except=$extensionPath",
      "--load-extension=$extensionPath",
      "--edge-skip-compat-layer-relaunch",
      "--no-sandbox",
      "--no-first-run",
      "--no-default-browser-check",
      "about:blank"
    ) -join " "

    try {
      $browserProcess = Start-Process -FilePath $browserExe -ArgumentList $browserArgs -PassThru -WindowStyle Minimized
      Wait-CdpEndpoint -Port $port -Process $browserProcess
      $selectedBrowserExe = $browserExe
      break
    }
    catch {
      $browserErrors.Add("$browserExe -> $($_.Exception.Message)") | Out-Null
      if ($browserProcess -and -not $browserProcess.HasExited) {
        Stop-Process -Id $browserProcess.Id -Force -ErrorAction SilentlyContinue
        Wait-Process -Id $browserProcess.Id -Timeout 5 -ErrorAction SilentlyContinue
      }
      $browserProcess = $null
    }
  }

  if (-not $selectedBrowserExe) {
    $cdpFailure = "Could not open a browser CDP endpoint. $($browserErrors -join ' | ')"
    Write-Host "$cdpFailure"
    Write-Host "Falling back to Playwright direct launch."
    Remove-Item Env:\SHOPRPA_BROWSER_CDP_URL -ErrorAction SilentlyContinue
    $nodeResult = Invoke-NodeSmoke
    if ($nodeResult.ExitCode -eq 0) {
      if ($nodeResult.Output) {
        Write-Host $nodeResult.Output
      }
      return
    }
    $directFailure = Get-SmokeFailureSummary
    if ($directFailure) {
      throw "Browser extension smoke failed after CDP fallback. $directFailure"
    }
    throw $cdpFailure
  }

  $env:SHOPRPA_BROWSER_CDP_URL = "http://127.0.0.1:$port"
  $env:SHOPRPA_PLAYWRIGHT_EXECUTABLE = $selectedBrowserExe
  $nodeResult = Invoke-NodeSmoke
  if ($nodeResult.ExitCode -ne 0) {
    $cdpFailure = Get-SmokeFailureSummary
    Remove-Item Env:\SHOPRPA_BROWSER_CDP_URL -ErrorAction SilentlyContinue
    if ($browserProcess -and -not $browserProcess.HasExited) {
      Stop-Process -Id $browserProcess.Id -Force -ErrorAction SilentlyContinue
      Wait-Process -Id $browserProcess.Id -Timeout 5 -ErrorAction SilentlyContinue
      $browserProcess = $null
    }
    Remove-DirectoryWithRetry -Path $userDataDir
    if (Test-Path -LiteralPath $reportPath -PathType Leaf) {
      Remove-Item -LiteralPath $reportPath -Force
    }
    $directResult = Invoke-NodeSmoke
    if ($directResult.ExitCode -eq 0) {
      if ($directResult.Output) {
        Write-Host $directResult.Output
      }
      return
    }

    $directFailure = Get-SmokeFailureSummary
    if (-not $directFailure) {
      $firstDirectOutput = @($directResult.Output -split "`r?`n" | Where-Object { $_.Trim() } | Select-Object -First 1)
      if ($firstDirectOutput.Count -gt 0) {
        $directFailure = $firstDirectOutput[0].Trim()
      }
    }
    if ($cdpFailure -and $directFailure) {
      Write-SmokeFailureReport -Failure "CDP attach failed: $cdpFailure; direct launch failed: $directFailure"
      throw "Browser extension smoke failed. CDP attach failed: $cdpFailure; direct launch failed: $directFailure"
    }
    if ($directFailure) {
      throw "Browser extension smoke failed. $directFailure"
    }
    if ($cdpFailure) {
      throw "Browser extension smoke failed. $cdpFailure"
    }
    $firstNodeOutput = @($nodeResult.Output -split "`r?`n" | Where-Object { $_.Trim() } | Select-Object -First 1)
    if ($firstNodeOutput.Count -gt 0) {
      throw "Browser extension smoke failed. $($firstNodeOutput[0].Trim())"
    }
    throw "Browser extension smoke failed with exit code $($nodeResult.ExitCode)."
  }
  if ($nodeResult.Output) {
    Write-Host $nodeResult.Output
  }
}
catch {
  if (-not (Test-Path -LiteralPath $reportPath -PathType Leaf)) {
    Write-SmokeFailureReport -Failure ($_.Exception.Message)
  }
  $summary = Get-SmokeFailureSummary
  if (-not $summary) {
    $summary = $_.Exception.Message
  }
  [Console]::Error.WriteLine("Browser extension smoke failed: $reportPath")
  [Console]::Error.WriteLine($summary)
  $smokeExitCode = 1
}
finally {
  if ($previousCdpUrl) {
    $env:SHOPRPA_BROWSER_CDP_URL = $previousCdpUrl
  }
  else {
    Remove-Item Env:\SHOPRPA_BROWSER_CDP_URL -ErrorAction SilentlyContinue
  }
  if ($previousExecutable) {
    $env:SHOPRPA_PLAYWRIGHT_EXECUTABLE = $previousExecutable
  }
  else {
    Remove-Item Env:\SHOPRPA_PLAYWRIGHT_EXECUTABLE -ErrorAction SilentlyContinue
  }
  if ($browserProcess -and -not $browserProcess.HasExited) {
    Stop-Process -Id $browserProcess.Id -Force -ErrorAction SilentlyContinue
    Wait-Process -Id $browserProcess.Id -Timeout 5 -ErrorAction SilentlyContinue
  }
  Pop-Location
}

if ($smokeExitCode -ne 0) {
  exit $smokeExitCode
}
