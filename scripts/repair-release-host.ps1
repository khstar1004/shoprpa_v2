param(
  [switch]$Apply,
  [switch]$Strict,
  [switch]$SkipPython,
  [switch]$SkipDocker,
  [switch]$SkipMaven,
  [switch]$SkipDoctor,
  [string]$Wheelhouse = $env:SHOPRPA_PYTHON_WHEELHOUSE,
  [switch]$Offline,
  [switch]$PythonCheckOnly,
  [string]$ReportPath = "",
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
    "-Apply" {
      $Apply = $true
    }
    "-Strict" {
      $Strict = $true
    }
    "-SkipPython" {
      $SkipPython = $true
    }
    "-SkipDocker" {
      $SkipDocker = $true
    }
    "-SkipMaven" {
      $SkipMaven = $true
    }
    "-SkipDoctor" {
      $SkipDoctor = $true
    }
    "-Offline" {
      $Offline = $true
    }
    "-PythonCheckOnly" {
      $PythonCheckOnly = $true
    }
    "-Wheelhouse" {
      $remainingIndex += 1
      if ($remainingIndex -ge $remainingArgsList.Count) {
        throw "-Wheelhouse requires a path value."
      }
      $Wheelhouse = $remainingArgsList[$remainingIndex]
    }
    "-ReportPath" {
      $remainingIndex += 1
      if ($remainingIndex -ge $remainingArgsList.Count) {
        throw "-ReportPath requires a path value."
      }
      $ReportPath = $remainingArgsList[$remainingIndex]
    }
    default {
      throw "Unknown repair-release-host argument after --: $arg"
    }
  }
  $remainingIndex += 1
}

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

  [PSCustomObject]@{
    ExitCode = $process.ExitCode
    Output = (($stdout + "`n" + $stderr).Trim())
  }
}

function Test-CommandAvailable {
  param([string]$CommandName)
  return $null -ne (Get-Command $CommandName -ErrorAction SilentlyContinue)
}

function Resolve-JavaCommand {
  param(
    [int]$RequiredMajor = 0,
    [string]$WorkingDirectory = (Get-Location).Path
  )

  $candidates = New-Object System.Collections.Generic.List[string]
  $seen = @{}

  function Add-JavaCandidate {
    param([string]$Candidate)
    if ([string]::IsNullOrWhiteSpace($Candidate)) {
      return
    }
    $key = $Candidate.ToLowerInvariant()
    if (-not $seen.ContainsKey($key)) {
      $seen[$key] = $true
      $candidates.Add($Candidate) | Out-Null
    }
  }

  $pathJava = Get-Command "java" -ErrorAction SilentlyContinue
  if ($pathJava) {
    Add-JavaCandidate $(if ($pathJava.Source) { $pathJava.Source } else { "java" })
  }

  if ($env:JAVA_HOME) {
    Add-JavaCandidate (Join-Path $env:JAVA_HOME "bin\java.exe")
  }

  $candidatePatterns = @(
    "C:\Program Files\Eclipse Adoptium\*\bin\java.exe",
    "C:\Program Files\Microsoft\jdk-*\bin\java.exe",
    "C:\Program Files\Java\*\bin\java.exe",
    "C:\Program Files\Neo4j Desktop 2\resources\offline\runtime\zulu21*\bin\java.exe",
    "C:\Program Files\Neo4j Desktop 2\resources\offline\runtime\zulu17*\bin\java.exe",
    "C:\Program Files (x86)\Android\openjdk\*\bin\java.exe",
    "C:\Program Files\Android\jdk\*\*\bin\java.exe"
  )

  foreach ($candidatePattern in $candidatePatterns) {
    Get-ChildItem -Path $candidatePattern -File -ErrorAction SilentlyContinue |
      Sort-Object -Property FullName -Descending |
      ForEach-Object { Add-JavaCandidate $_.FullName }
  }

  if ($candidates.Count -eq 0) {
    return $null
  }

  if ($RequiredMajor -gt 0) {
    foreach ($candidate in $candidates) {
      try {
        $versionResult = Invoke-Captured -FilePath $candidate -Arguments @("-version") -WorkingDirectory $WorkingDirectory
        $major = Get-JavaMajorFromVersionOutput -Text $versionResult.Output
        if ($major -ge $RequiredMajor) {
          return $candidate
        }
      }
      catch {
        continue
      }
    }
  }

  return $candidates[0]
}

function Convert-JavaVersionToMajor {
  param([string]$Version)

  if ([string]::IsNullOrWhiteSpace($Version)) {
    return 0
  }
  $trimmed = $Version.Trim()
  if ($trimmed -match "^1\.(\d+)") {
    return [int]$Matches[1]
  }
  if ($trimmed -match "^(\d+)") {
    return [int]$Matches[1]
  }
  return 0
}

function Get-JavaMajorFromVersionOutput {
  param([string]$Text)

  if ([string]::IsNullOrWhiteSpace($Text)) {
    return 0
  }
  if ($Text -match 'version\s+"([^"]+)"') {
    return Convert-JavaVersionToMajor -Version $Matches[1]
  }
  if ($Text -match "\b(openjdk|java)\s+([0-9][^\s]*)") {
    return Convert-JavaVersionToMajor -Version $Matches[2]
  }
  return 0
}

function Get-RequiredJavaRuntime {
  param([string]$RepoRoot)

  $pomPaths = @(
    "backend\resource-service\pom.xml",
    "backend\robot-service\pom.xml",
    "backend\rpa-auth\pom.xml"
  )
  $maxMajor = 0
  $details = New-Object System.Collections.Generic.List[string]
  foreach ($pom in $pomPaths) {
    $pomPath = Join-Path $RepoRoot $pom
    if (-not (Test-Path -LiteralPath $pomPath -PathType Leaf)) {
      continue
    }
    $content = Get-Content -LiteralPath $pomPath -Raw
    if ($content -match "<java\.version>\s*([^<]+)\s*</java\.version>") {
      $version = $Matches[1].Trim()
      $major = Convert-JavaVersionToMajor -Version $version
      if ($major -gt 0) {
        $maxMajor = [Math]::Max($maxMajor, $major)
        $details.Add("$pom=$version") | Out-Null
      }
    }
  }

  [PSCustomObject]@{
    Major = $maxMajor
    Details = $details.ToArray()
  }
}

function Resolve-MavenInvocation {
  param([string]$RepoRoot)

  function New-MavenInvocation {
    param(
      [string]$FilePath,
      [string]$Display
    )

    $extension = [System.IO.Path]::GetExtension($FilePath)
    if ($extension -in @(".cmd", ".bat")) {
      return [PSCustomObject]@{
        FilePath = $env:ComSpec
        ArgumentsPrefix = @("/d", "/c", $FilePath)
        Display = $Display
      }
    }

    return [PSCustomObject]@{
      FilePath = $FilePath
      ArgumentsPrefix = @()
      Display = $Display
    }
  }

  $mavenCommand = Get-Command "mvn" -ErrorAction SilentlyContinue
  if ($mavenCommand) {
    return New-MavenInvocation -FilePath $mavenCommand.Source -Display "mvn"
  }

  $candidatePaths = New-Object System.Collections.Generic.List[string]
  foreach ($mavenHome in @($env:MAVEN_HOME, $env:M2_HOME)) {
    if ($mavenHome) {
      $candidatePaths.Add((Join-Path $mavenHome "bin\mvn.cmd")) | Out-Null
      $candidatePaths.Add((Join-Path $mavenHome "bin\mvn")) | Out-Null
    }
  }
  $candidatePatterns = @(
    "C:\Program Files\Apache\maven*\bin\mvn.cmd",
    "C:\Program Files\apache-maven*\bin\mvn.cmd",
    "C:\Program Files\Maven\*\bin\mvn.cmd",
    "C:\ProgramData\chocolatey\lib\maven\apache-maven*\bin\mvn.cmd",
    "C:\Users\$env:USERNAME\scoop\apps\maven\current\bin\mvn.cmd"
  )
  foreach ($candidatePattern in $candidatePatterns) {
    Get-ChildItem -Path $candidatePattern -File -ErrorAction SilentlyContinue |
      Sort-Object -Property FullName -Descending |
      ForEach-Object { $candidatePaths.Add($_.FullName) | Out-Null }
  }
  foreach ($candidatePath in $candidatePaths) {
    if (Test-Path -LiteralPath $candidatePath -PathType Leaf) {
      return New-MavenInvocation -FilePath $candidatePath -Display $candidatePath
    }
  }

  $repoWrapper = Join-Path $RepoRoot "mvnw.cmd"
  if (Test-Path -LiteralPath $repoWrapper -PathType Leaf) {
    return New-MavenInvocation -FilePath $repoWrapper -Display "mvnw.cmd"
  }

  $embeddedMaven = Resolve-EmbeddedM2eMavenInvocation -RepoRoot $RepoRoot
  if ($embeddedMaven) {
    return $embeddedMaven
  }

  return $null
}

function Resolve-EmbeddedM2eMavenInvocation {
  param([string]$RepoRoot)

  $javaCommand = Resolve-JavaCommand -RequiredMajor 8 -WorkingDirectory $RepoRoot
  if (-not $javaCommand) {
    return $null
  }

  $extensionsRoot = Join-Path $env:USERPROFILE ".vscode\extensions"
  if (-not (Test-Path -LiteralPath $extensionsRoot -PathType Container)) {
    return $null
  }

  $runtimeJar = @(
    Get-ChildItem -Path (Join-Path $extensionsRoot "redhat.java-*\server\plugins\org.eclipse.m2e.maven.runtime_*.jar") -File -ErrorAction SilentlyContinue |
      Sort-Object -Property FullName -Descending
  ) | Select-Object -First 1
  if (-not $runtimeJar) {
    return $null
  }

  $runtimeDir = Join-Path $RepoRoot "build\tmp\m2e-maven-runtime"
  if (-not (Test-Path -LiteralPath $runtimeDir -PathType Container)) {
    New-Item -ItemType Directory -Path $runtimeDir | Out-Null
  }
  $localRepository = Join-Path $RepoRoot "build\tmp\m2-repository"
  if (-not (Test-Path -LiteralPath $localRepository -PathType Container)) {
    New-Item -ItemType Directory -Path $localRepository | Out-Null
  }

  try {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($runtimeJar.FullName)
    try {
      foreach ($entry in $zip.Entries) {
        if ($entry.FullName -like "jars/*.jar") {
          $target = Join-Path $runtimeDir ([System.IO.Path]::GetFileName($entry.FullName))
          if ((-not (Test-Path -LiteralPath $target -PathType Leaf)) -or ((Get-Item -LiteralPath $target).Length -ne $entry.Length)) {
            [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $target, $true)
          }
        }
      }
    }
    finally {
      $zip.Dispose()
    }
  }
  catch {
    return $null
  }

  $pluginDir = Split-Path -Parent $runtimeJar.FullName
  $classpathItems = New-Object System.Collections.Generic.List[string]
  $classpathItems.Add((Join-Path $runtimeDir "*")) | Out-Null
  foreach ($pattern in @(
      "org.apache.commons.cli_*.jar",
      "org.apache.commons.commons-codec_*.jar",
      "slf4j.api_*.jar",
      "com.google.guava_*.jar",
      "com.google.guava.failureaccess_*.jar",
      "jakarta.inject.jakarta.inject-api_*.jar"
    )) {
    Get-ChildItem -Path (Join-Path $pluginDir $pattern) -File -ErrorAction SilentlyContinue |
      Sort-Object -Property FullName -Descending |
      ForEach-Object { $classpathItems.Add($_.FullName) | Out-Null }
  }
  Get-ChildItem -Path (Join-Path $extensionsRoot "vscjava.vscode-gradle-*\lib\slf4j-simple-*.jar") -File -ErrorAction SilentlyContinue |
    Sort-Object -Property FullName -Descending |
    ForEach-Object { $classpathItems.Add($_.FullName) | Out-Null }

  if ($classpathItems.Count -le 1) {
    return $null
  }

  [PSCustomObject]@{
    FilePath = $javaCommand
    ArgumentsPrefix = @(
      "-Dmaven.multiModuleProjectDirectory=$RepoRoot",
      "-Dmaven.repo.local=$localRepository",
      "-cp",
      ($classpathItems.ToArray() -join [System.IO.Path]::PathSeparator),
      "org.apache.maven.cli.MavenCli"
    )
    Display = "embedded m2e Maven ($($runtimeJar.FullName))"
  }
}

function Get-FirstUsefulLine {
  param([string]$Text)
  foreach ($line in @($Text -split "`r?`n")) {
    $trimmed = $line.Trim()
    if ($trimmed) {
      return $trimmed
    }
  }
  return ""
}

function Get-PythonWheelhousePreflightDetail {
  $preflightPath = Join-Path $repoRoot "build\python-backend-requirements\python-backend-wheelhouse-preflight.json"
  $missingRequirementsPath = Join-Path $repoRoot "build\python-backend-requirements\python-backend-wheelhouse-missing.requirements.txt"
  if (-not (Test-Path -LiteralPath $preflightPath -PathType Leaf)) {
    return ""
  }

  try {
    $preflight = Get-Content -LiteralPath $preflightPath -Raw | ConvertFrom-Json
  }
  catch {
    return ""
  }

  if ($preflight.ok -eq $true) {
    return "wheelhouse preflight passed"
  }

  $blockedServices = @($preflight.services | Where-Object { $_.ok -ne $true })
  if ($blockedServices.Count -eq 0) {
    return ""
  }

  $serviceParts = @(
    $blockedServices |
      ForEach-Object {
        $serviceName = Split-Path -Leaf ([string]$_.service)
        if (-not $serviceName) {
          $serviceName = [string]$_.service
        }
        "$serviceName=$(@($_.missingLockedWheels).Count)"
      }
  )

  $combinedMissingCount = 0
  if (Test-Path -LiteralPath $missingRequirementsPath -PathType Leaf) {
    $combinedMissingCount = @(
      Get-Content -LiteralPath $missingRequirementsPath |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -and -not $_.StartsWith("#") }
    ).Count
  }

  $detail = "missing compatible wheelhouse pins: $($serviceParts -join ', ')"
  if ($combinedMissingCount -gt 0) {
    $detail = "$detail; combined=$combinedMissingCount; missing requirements=build\python-backend-requirements\python-backend-wheelhouse-missing.requirements.txt"
  }
  return $detail
}

function Get-PythonSetupDetail {
  param([string]$Text)

  $lines = @($Text -split "`r?`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ })
  $summary = @(
    $lines |
      Where-Object {
        $_ -match "^uv dry run still needs downloads" -or
        $_ -match "^Wheelhouse is missing (compatible )?locked wheels" -or
        $_ -match "^Python backend wheelhouse is incomplete" -or
        $_ -match "^uv sync could not download packages" -or
        $_ -match "package index access is blocked" -or
        $_ -match "^uv sync failed"
      } |
      Select-Object -Last 1
  )
  if ($summary.Count -gt 0) {
    $detail = $summary[0]
    $preflightDetail = Get-PythonWheelhousePreflightDetail
    if ($preflightDetail) {
      return "$detail; $preflightDetail"
    }
    return $detail
  }

  $downloadLine = @($lines | Where-Object { $_ -match "^Would download\s+\d+\s+packages" } | Select-Object -First 1)
  if ($downloadLine.Count -gt 0) {
    return "offline dry run needs downloads: $($downloadLine[0])"
  }

  return (Get-FirstUsefulLine -Text $Text)
}

function Get-ReleaseDoctorResult {
  param([object]$DoctorResult)

  $outputLines = @(
    $DoctorResult.Output -split "`r?`n" |
      ForEach-Object { $_.Trim() } |
      Where-Object { $_ }
  )
  $firstLine = Get-FirstUsefulLine -Text $DoctorResult.Output
  if (-not $firstLine) {
    $firstLine = "doctor-release produced no output"
  }

  if ($DoctorResult.ExitCode -ne 0) {
    $blockingLine = @(
      $outputLines |
        Where-Object {
          $_ -match "^Blocking failures:\s+\d+" -or
          $_ -match "^Strict mode failed because warnings remain:\s+\d+" -or
          $_ -match "^Warnings remain:\s+\d+"
        } |
        Select-Object -Last 1
    )
    return [PSCustomObject]@{
      Status = "BLOCKED"
      Detail = $(if ($blockingLine.Count -gt 0) { $blockingLine[0] } else { $firstLine })
    }
  }

  $warningsLine = @(
    $outputLines |
      Where-Object { $_ -match "^Warnings remain:\s+\d+" } |
      Select-Object -Last 1
  )
  if ($warningsLine.Count -gt 0) {
    return [PSCustomObject]@{
      Status = "WARN"
      Detail = $warningsLine[0]
    }
  }

  return [PSCustomObject]@{
    Status = "PASS"
    Detail = $firstLine
  }
}

function Add-Result {
  param(
    [System.Collections.Generic.List[object]]$Results,
    [string]$Name,
    [string]$Status,
    [string]$Detail
  )

  $Results.Add([PSCustomObject]@{
      Name = $Name
      Status = $Status
      Detail = $Detail
    }) | Out-Null
}

function Add-UniqueLine {
  param(
    [System.Collections.Generic.List[string]]$Lines,
    [string]$Line
  )

  if ($Line -and (-not $Lines.Contains($Line))) {
    $Lines.Add($Line) | Out-Null
  }
}

function Escape-MarkdownTableCell {
  param([object]$Value)

  if ($null -eq $Value) {
    return ""
  }

  return ([string]$Value).
    Replace("|", "\|").
    Replace("`r`n", "<br>").
    Replace("`n", "<br>").
    Replace("`r", "<br>")
}

function Write-RepairSummary {
  param(
    [string]$Path,
    [object]$Report
  )

  $summaryPath = [System.IO.Path]::ChangeExtension($Path, ".md")
  $lines = New-Object System.Collections.Generic.List[string]
  $lines.Add("# ShopRPA Release Host Repair Summary") | Out-Null
  $lines.Add("") | Out-Null
  $lines.Add("Generated: $($Report.generatedAt)") | Out-Null
  $lines.Add("Repository: $($Report.repository)") | Out-Null
  $lines.Add("Mode: $($Report.mode)") | Out-Null
  $lines.Add("Strict: $($Report.strict)") | Out-Null
  $lines.Add("Offline: $($Report.offline)") | Out-Null
  $lines.Add("Status: $($Report.status)") | Out-Null
  if ([string]$Report.wheelhouse) {
    $lines.Add("Wheelhouse: $($Report.wheelhouse)") | Out-Null
  }
  $lines.Add("") | Out-Null
  $lines.Add("| Check | Status | Detail |") | Out-Null
  $lines.Add("| --- | --- | --- |") | Out-Null
  foreach ($result in @($Report.results)) {
    $lines.Add("| $(Escape-MarkdownTableCell $result.name) | $(Escape-MarkdownTableCell $result.status) | $(Escape-MarkdownTableCell $result.detail) |") | Out-Null
  }

  if (@($Report.blocked).Count -gt 0) {
    $lines.Add("") | Out-Null
    $lines.Add("Blocked checks:") | Out-Null
    foreach ($item in @($Report.blocked)) {
      $lines.Add("- $($item.name): $($item.detail)") | Out-Null
    }
  }

  if (@($Report.warnings).Count -gt 0) {
    $lines.Add("") | Out-Null
    $lines.Add("Warning checks:") | Out-Null
    foreach ($item in @($Report.warnings)) {
      $lines.Add("- $($item.name): $($item.detail)") | Out-Null
    }
  }

  if (@($Report.nextRequiredHostActions).Count -gt 0) {
    $lines.Add("") | Out-Null
    $lines.Add("Next required host actions:") | Out-Null
    foreach ($action in @($Report.nextRequiredHostActions)) {
      $lines.Add("- $action") | Out-Null
    }
  }

  Set-Content -LiteralPath $summaryPath -Value $lines -Encoding UTF8
  return $summaryPath
}

function Get-RemediationLines {
  param([object[]]$Results)

  $lines = New-Object System.Collections.Generic.List[string]
  foreach ($item in $Results) {
    if ($item.Status -notin @("BLOCKED", "WARN")) {
      continue
    }

    switch ($item.Name) {
      "docker engine" {
        Add-UniqueLine $lines "Start Docker Desktop or run Start-Service com.docker.service from an elevated PowerShell."
        Add-UniqueLine $lines "If this user is not in docker-users, run: net localgroup docker-users $env:USERNAME /add, then sign out and back in."
        Add-UniqueLine $lines "Verify Docker with: docker version"
      }
      "java" {
        Add-UniqueLine $lines "Install or select a JDK that satisfies the Java backend POM requirements, then verify with: java -version"
      }
      "maven" {
        Add-UniqueLine $lines "Install Apache Maven 3.9+, set MAVEN_HOME/M2_HOME, add Maven bin to PATH, or add a repository mvnw.cmd wrapper, then verify with: mvn -version or .\mvnw.cmd -version"
      }
      "java backend Maven tests" {
        Add-UniqueLine $lines "Allow Maven HTTPS access to repo.maven.apache.org, or set SHOPRPA_MAVEN_LOCAL_REPOSITORY/SHOPRPA_MAVEN_SETTINGS to a populated Maven repository or mirror, then rerun: corepack pnpm run test:java-backends"
      }
      "python backend setup" {
        Add-UniqueLine $lines "For online setup, run: corepack pnpm run setup:python-backends"
        Add-UniqueLine $lines "If a wheelhouse preflight wrote missing compatible pins, fetch those pins with: corepack pnpm run download:python-backend-wheelhouse-missing -- -Wheelhouse <path> -ContinueOnError"
        Add-UniqueLine $lines "For closed-network setup, populate a wheelhouse with all compatible locked wheels, then run: corepack pnpm run setup:python-backends -- -Wheelhouse <path> -Offline"
      }
      "docker compose repair" {
        Add-UniqueLine $lines "After Docker is available, rebuild the gateway and Python services with: corepack pnpm run repair:release-host:apply"
      }
      "release doctor" {
        Add-UniqueLine $lines "If a wheelhouse preflight wrote missing compatible pins, fetch those pins with: corepack pnpm run download:python-backend-wheelhouse-missing -- -Wheelhouse <path> -ContinueOnError"
        Add-UniqueLine $lines "If Python backend runtime or wheelhouse warnings remain, populate a compatible wheelhouse and run: corepack pnpm run setup:python-backends -- -Wheelhouse <path> -Offline"
        Add-UniqueLine $lines "If browser or installer checks fail with EPERM or CDP timeouts, allow Node, Electron, Chromium child processes, and local 127.0.0.1 CDP/WebSocket automation in the endpoint policy."
        Add-UniqueLine $lines "If backend service route smoke returns 401 after source fixes, rebuild the gateway with: corepack pnpm run repair:release-host:apply"
        Add-UniqueLine $lines "After applying host fixes, rerun: corepack pnpm run doctor:release"
        Add-UniqueLine $lines "Use strict verification before release handoff: corepack pnpm run audit:release:strict"
      }
    }
  }

  return $lines.ToArray()
}

function Get-JavaBackendTestsHostDetail {
  param([string]$ReportPath)

  if (-not (Test-Path -LiteralPath $ReportPath -PathType Leaf)) {
    return [PSCustomObject]@{
      Status = "BLOCKED"
      Detail = "missing report; run corepack pnpm run test:java-backends"
    }
  }

  try {
    $report = Get-Content -LiteralPath $ReportPath -Raw | ConvertFrom-Json
    if ($report.ok -eq $true) {
      $passedModules = @($report.modules | Where-Object { $_.ok -eq $true }).Count
      return [PSCustomObject]@{
        Status = "PASS"
        Detail = "$passedModules Java backend modules passed"
      }
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
      return [PSCustomObject]@{
        Status = "BLOCKED"
        Detail = "Maven repository access is blocked (repo.maven.apache.org: Permission denied: getsockopt)$moduleSummary"
      }
    }

    $failures = @($report.failures | Where-Object { $_ } | Select-Object -First 3)
    return [PSCustomObject]@{
      Status = "BLOCKED"
      Detail = if ($failures.Count -gt 0) { $failures -join "; " } else { "report has ok=false" }
    }
  }
  catch {
    return [PSCustomObject]@{
      Status = "BLOCKED"
      Detail = "report is unreadable; rerun corepack pnpm run test:java-backends"
    }
  }
}

function Test-CurrentUserInDockerUsers {
  try {
    $groupResult = Invoke-Captured -FilePath "whoami" -Arguments @("/groups") -WorkingDirectory $repoRoot
    return $groupResult.Output -match "(?i)(^|\s|\\)docker-users(\s|$)"
  }
  catch {
    return $false
  }
}

function Get-DockerStatusDetail {
  $details = New-Object System.Collections.Generic.List[string]
  if (-not (Test-CommandAvailable "docker")) {
    $details.Add("docker command is not on PATH") | Out-Null
    return ($details -join "; ")
  }

  $dockerResult = Invoke-Captured -FilePath "docker" -Arguments @("version", "--format", "{{.Server.Version}}") -WorkingDirectory $repoRoot
  if ($dockerResult.ExitCode -eq 0) {
    $details.Add("docker server $((Get-FirstUsefulLine -Text $dockerResult.Output))") | Out-Null
  }
  else {
    $details.Add((Get-FirstUsefulLine -Text $dockerResult.Output)) | Out-Null
  }

  $service = Get-Service -Name "com.docker.service" -ErrorAction SilentlyContinue
  if ($service) {
    $details.Add("com.docker.service=$($service.Status)") | Out-Null
  }
  else {
    $details.Add("com.docker.service not found") | Out-Null
  }

  if (-not (Test-CurrentUserInDockerUsers)) {
    $details.Add("current user is not in docker-users") | Out-Null
  }

  return ($details -join "; ")
}

$repoRoot = Resolve-RepoRoot
$dockerRoot = Join-Path $repoRoot "docker"
$composePath = Join-Path $dockerRoot "docker-compose.yml"
$dockerEnvPath = Join-Path $dockerRoot ".env"
$powershellExe = Join-Path $env:SystemRoot "System32\WindowsPowerShell\v1.0\powershell.exe"
$reportRoot = Join-Path $repoRoot "build"
if (-not $ReportPath) {
  $ReportPath = Join-Path $reportRoot "release-host-repair-report.json"
}
elseif (-not [System.IO.Path]::IsPathRooted($ReportPath)) {
  $ReportPath = Join-Path $repoRoot $ReportPath
}
$results = New-Object System.Collections.Generic.List[object]

Write-Host "ShopRPA release host repair"
Write-Host "Repository: $repoRoot"
Write-Host "Mode: $(if ($Apply) { "apply" } else { "diagnostic" })"
Write-Host ""

$dockerReady = $false
if (-not $SkipDocker) {
  $dockerDetail = Get-DockerStatusDetail
  $dockerReady = (Test-CommandAvailable "docker") -and ((Invoke-Captured -FilePath "docker" -Arguments @("version", "--format", "{{.Server.Version}}") -WorkingDirectory $repoRoot).ExitCode -eq 0)
  Add-Result -Results $results -Name "docker engine" -Status ($(if ($dockerReady) { "PASS" } else { "BLOCKED" })) -Detail $dockerDetail
}
else {
  Add-Result -Results $results -Name "docker engine" -Status "SKIPPED" -Detail "skipped by parameter"
}

$javaRequirement = Get-RequiredJavaRuntime -RepoRoot $repoRoot
$javaCommand = Resolve-JavaCommand -RequiredMajor $javaRequirement.Major -WorkingDirectory $repoRoot
if ($javaCommand) {
  $javaResult = Invoke-Captured -FilePath $javaCommand -Arguments @("-version") -WorkingDirectory $repoRoot
  $javaDetail = Get-FirstUsefulLine -Text $javaResult.Output
  if ($javaCommand -ne "java") {
    $javaDetail = "$javaDetail ($javaCommand)"
  }
  $installedJavaMajor = Get-JavaMajorFromVersionOutput -Text $javaResult.Output
  if ($javaResult.ExitCode -ne 0) {
    Add-Result -Results $results -Name "java" -Status "BLOCKED" -Detail $javaDetail
  }
  elseif (($javaRequirement.Major -gt 0) -and ($installedJavaMajor -gt 0) -and ($installedJavaMajor -lt $javaRequirement.Major)) {
    Add-Result -Results $results -Name "java" -Status "BLOCKED" -Detail "$javaDetail; Java $($javaRequirement.Major)+ required by Java backend modules ($($javaRequirement.Details -join ', '))"
  }
  elseif (($javaRequirement.Major -gt 0) -and ($installedJavaMajor -eq 0)) {
    Add-Result -Results $results -Name "java" -Status "BLOCKED" -Detail "$javaDetail; could not parse Java major version; Java $($javaRequirement.Major)+ is required by Java backend modules ($($javaRequirement.Details -join ', '))"
  }
  else {
    $requirementDetail = if ($javaRequirement.Major -gt 0) { "; satisfies Java $($javaRequirement.Major)+ requirement ($($javaRequirement.Details -join ', '))" } else { "" }
    Add-Result -Results $results -Name "java" -Status "PASS" -Detail "$javaDetail$requirementDetail"
  }
}
else {
  $requirementDetail = if ($javaRequirement.Major -gt 0) { " Java $($javaRequirement.Major)+ is required by Java backend modules ($($javaRequirement.Details -join ', '))." } else { "" }
  Add-Result -Results $results -Name "java" -Status "BLOCKED" -Detail "java was not found on PATH or common local JDK locations.$requirementDetail"
}

if (-not $SkipMaven) {
  $mavenInvocation = Resolve-MavenInvocation -RepoRoot $repoRoot
  if ($mavenInvocation) {
    $mavenResult = Invoke-Captured -FilePath $mavenInvocation.FilePath -Arguments ($mavenInvocation.ArgumentsPrefix + @("-version")) -WorkingDirectory $repoRoot
    $mavenDetail = Get-FirstUsefulLine -Text $mavenResult.Output
    if ($mavenInvocation.Display -ne "mvn") {
      $mavenDetail = "$mavenDetail ($($mavenInvocation.Display))"
    }
    Add-Result -Results $results -Name "maven" -Status ($(if ($mavenResult.ExitCode -eq 0) { "PASS" } else { "BLOCKED" })) -Detail $mavenDetail
  }
  else {
    Add-Result -Results $results -Name "maven" -Status "BLOCKED" -Detail "Maven was not found on PATH, MAVEN_HOME, M2_HOME, common local install paths, or repo mvnw.cmd; install Maven or add a repository Maven wrapper before Java backend verification."
  }
}
else {
  Add-Result -Results $results -Name "maven" -Status "SKIPPED" -Detail "skipped by parameter"
}

$javaBackendTestsReportPath = Join-Path $repoRoot "build\java-backend-tests.json"
$javaBackendTestsHost = Get-JavaBackendTestsHostDetail -ReportPath $javaBackendTestsReportPath
Add-Result -Results $results -Name "java backend Maven tests" -Status $javaBackendTestsHost.Status -Detail $javaBackendTestsHost.Detail

if (-not $SkipPython) {
  $setupPython = Join-Path $repoRoot "scripts\setup-python-backends.ps1"
  $pythonArgs = @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $setupPython)
  if ($Wheelhouse) {
    $pythonArgs += @("-Wheelhouse", $Wheelhouse)
  }
  if ($Offline -or (-not $Apply)) {
    $pythonArgs += "-Offline"
  }
  if ($PythonCheckOnly -or (-not $Apply)) {
    $pythonArgs += "-CheckOnly"
  }

  $pythonResult = Invoke-Captured -FilePath $powershellExe -Arguments $pythonArgs -WorkingDirectory $repoRoot
  Add-Result -Results $results -Name "python backend setup" -Status ($(if ($pythonResult.ExitCode -eq 0) { "PASS" } else { "BLOCKED" })) -Detail (Get-PythonSetupDetail -Text $pythonResult.Output)
}
else {
  Add-Result -Results $results -Name "python backend setup" -Status "SKIPPED" -Detail "skipped by parameter"
}

if (-not $SkipDocker) {
  if (-not (Test-Path -LiteralPath $composePath -PathType Leaf) -or -not (Test-Path -LiteralPath $dockerEnvPath -PathType Leaf)) {
    Add-Result -Results $results -Name "docker compose repair" -Status "BLOCKED" -Detail "docker-compose.yml or docker/.env is missing."
  }
  elseif (-not $dockerReady) {
    Add-Result -Results $results -Name "docker compose repair" -Status "BLOCKED" -Detail "Docker engine is not available to this Windows user."
  }
  elseif (-not $Apply) {
    Add-Result -Results $results -Name "docker compose repair" -Status "SKIPPED" -Detail "diagnostic mode; rerun with -Apply to rebuild ai-service, openapi-service, and openresty-nginx."
  }
  else {
    $composeResult = Invoke-Captured -FilePath "docker" -Arguments @(
      "compose",
      "--env-file",
      $dockerEnvPath,
      "-f",
      $composePath,
      "up",
      "-d",
      "--build",
      "ai-service",
      "openapi-service",
      "openresty-nginx"
    ) -WorkingDirectory $dockerRoot
    Add-Result -Results $results -Name "docker compose repair" -Status ($(if ($composeResult.ExitCode -eq 0) { "PASS" } else { "BLOCKED" })) -Detail (Get-FirstUsefulLine -Text $composeResult.Output)
  }
}
else {
  Add-Result -Results $results -Name "docker compose repair" -Status "SKIPPED" -Detail "skipped by parameter"
}

if (-not $SkipDoctor) {
  $doctorArgs = @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $repoRoot "scripts\doctor-release.ps1"))
  if ($Strict) {
    $doctorArgs += "-Strict"
  }
  $doctorResult = Invoke-Captured -FilePath $powershellExe -Arguments $doctorArgs -WorkingDirectory $repoRoot
  $doctorSummary = Get-ReleaseDoctorResult -DoctorResult $doctorResult
  Add-Result -Results $results -Name "release doctor" -Status $doctorSummary.Status -Detail $doctorSummary.Detail
}
else {
  Add-Result -Results $results -Name "release doctor" -Status "SKIPPED" -Detail "skipped by parameter"
}

Write-Host ""
$results | Format-Table -AutoSize

$blocked = @($results | Where-Object { $_.Status -eq "BLOCKED" })
if ($blocked.Count -gt 0) {
  Write-Host ""
  Write-Host "Blocked checks:" -ForegroundColor Yellow
  foreach ($item in $blocked) {
    Write-Host "- $($item.Name): $($item.Detail)" -ForegroundColor Yellow
  }
}

$warnings = @($results | Where-Object { $_.Status -eq "WARN" })
if ($warnings.Count -gt 0) {
  Write-Host ""
  Write-Host "Warning checks:" -ForegroundColor Yellow
  foreach ($item in $warnings) {
    Write-Host "- $($item.Name): $($item.Detail)" -ForegroundColor Yellow
  }
}

$remediationLines = @(Get-RemediationLines -Results $results)
if ($remediationLines.Count -gt 0) {
  Write-Host ""
  Write-Host "Next required host actions:" -ForegroundColor Yellow
  foreach ($line in $remediationLines) {
    Write-Host "- $line" -ForegroundColor Yellow
  }
}

$reportDir = Split-Path -Parent $ReportPath
if (-not (Test-Path -LiteralPath $reportDir -PathType Container)) {
  New-Item -ItemType Directory -Path $reportDir | Out-Null
}
$resultRecords = @($results | ForEach-Object {
    [PSCustomObject]@{
      name = [string]$_.Name
      status = [string]$_.Status
      detail = [string]$_.Detail
    }
  })
$blockedRecords = @($blocked | ForEach-Object {
    [PSCustomObject]@{
      name = [string]$_.Name
      status = [string]$_.Status
      detail = [string]$_.Detail
    }
  })
$warningRecords = @($warnings | ForEach-Object {
    [PSCustomObject]@{
      name = [string]$_.Name
      status = [string]$_.Status
      detail = [string]$_.Detail
    }
  })
$report = [PSCustomObject]@{
  generatedAt = (Get-Date).ToString("o")
  repository = $repoRoot
  mode = $(if ($Apply) { "apply" } else { "diagnostic" })
  strict = [bool]$Strict
  offline = [bool]$Offline
  skipPython = [bool]$SkipPython
  skipDocker = [bool]$SkipDocker
  skipMaven = [bool]$SkipMaven
  skipDoctor = [bool]$SkipDoctor
  wheelhouse = $Wheelhouse
  status = $(if ($blocked.Count -gt 0) { "BLOCKED" } elseif ($warnings.Count -gt 0) { "WARN" } else { "PASS" })
  results = $resultRecords
  blocked = $blockedRecords
  warnings = $warningRecords
  nextRequiredHostActions = @($remediationLines)
}
$report | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $ReportPath -Encoding UTF8
$summaryPath = Write-RepairSummary -Path $ReportPath -Report $report
Write-Host ""
Write-Host "Release host repair report: $ReportPath"
Write-Host "Release host repair summary: $summaryPath"

if (($Strict -or $Apply) -and (($blocked.Count -gt 0) -or ($warnings.Count -gt 0))) {
  exit 1
}
