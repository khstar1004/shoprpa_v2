param(
  [string]$ReportPath = "",
  [switch]$SkipTests,
  [string]$MavenLocalRepository = $env:SHOPRPA_MAVEN_LOCAL_REPOSITORY,
  [string]$MavenSettings = $env:SHOPRPA_MAVEN_SETTINGS
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

function Resolve-InputPath {
  param(
    [string]$BasePath,
    [string]$InputPath
  )

  if ([string]::IsNullOrWhiteSpace($InputPath)) {
    return ""
  }
  if ([System.IO.Path]::IsPathRooted($InputPath)) {
    return [System.IO.Path]::GetFullPath($InputPath)
  }
  return [System.IO.Path]::GetFullPath((Join-Path $BasePath $InputPath))
}

function Test-CommandAvailable {
  param([string]$CommandName)
  return $null -ne (Get-Command $CommandName -ErrorAction SilentlyContinue)
}

function Invoke-Captured {
  param(
    [string]$FilePath,
    [string[]]$Arguments,
    [string]$WorkingDirectory,
    [int]$TimeoutSeconds = 0
  )

  $argumentString = (($Arguments | ForEach-Object {
        if ($_ -match '[\s"]') {
          '"' + ($_ -replace '"', '\"') + '"'
        }
        else {
          $_
        }
      }) -join " ")
  $stdoutPath = [System.IO.Path]::GetTempFileName()
  $stderrPath = [System.IO.Path]::GetTempFileName()
  $startedAt = [DateTimeOffset]::Now
  $timedOut = $false
  $terminationFailed = $false
  $process = $null
  try {
    $process = Start-Process -FilePath $FilePath -ArgumentList $argumentString -WorkingDirectory $WorkingDirectory -RedirectStandardOutput $stdoutPath -RedirectStandardError $stderrPath -WindowStyle Hidden -PassThru
    if ($TimeoutSeconds -gt 0) {
      $completed = $process.WaitForExit($TimeoutSeconds * 1000)
      if (-not $completed) {
        $timedOut = $true
        try {
          & taskkill.exe /PID $process.Id /T /F | Out-Null
        }
        catch {
          try {
            $process.Kill()
          }
        catch {
          }
        }
        Start-Sleep -Milliseconds 250
        if (-not $process.WaitForExit(5000)) {
          $terminationFailed = $true
        }
      }
    }
    if (-not $timedOut) {
      $process.WaitForExit()
    }
    $process.Refresh()
  }
  finally {
    $finishedAt = [DateTimeOffset]::Now
  }

  $stdout = if (Test-Path -LiteralPath $stdoutPath -PathType Leaf) { Get-Content -LiteralPath $stdoutPath -Raw -ErrorAction SilentlyContinue } else { "" }
  $stderr = if (Test-Path -LiteralPath $stderrPath -PathType Leaf) { Get-Content -LiteralPath $stderrPath -Raw -ErrorAction SilentlyContinue } else { "" }
  foreach ($capturePath in @($stdoutPath, $stderrPath)) {
    if (Test-Path -LiteralPath $capturePath -PathType Leaf) {
      try {
        Remove-Item -LiteralPath $capturePath -Force
      }
      catch {
      }
    }
  }
  $combinedOutput = (($stdout + "`n" + $stderr).Trim())
  if ($terminationFailed) {
    $combinedOutput = (($combinedOutput, "Timed out after ${TimeoutSeconds}s and the process tree could not be terminated by this host policy.") | Where-Object { $_ }) -join "`n"
  }
  $exitCode = if ($timedOut) {
    124
  }
  elseif ($process -and ($null -ne $process.ExitCode)) {
    [int]$process.ExitCode
  }
  elseif ($combinedOutput -match "(?m)\b(ERROR|FATAL)\b|Could not create|Could not transfer|Non-resolvable") {
    1
  }
  else {
    0
  }

  [PSCustomObject]@{
    exitCode = $exitCode
    startedAt = $startedAt.ToString("o")
    finishedAt = $finishedAt.ToString("o")
    durationSeconds = [Math]::Round(($finishedAt - $startedAt).TotalSeconds, 3)
    timedOut = $timedOut
    terminationFailed = $terminationFailed
    output = $combinedOutput
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
  if ($Text.Length -le $MaxChars) {
    return $Text
  }
  return $Text.Substring($Text.Length - $MaxChars)
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
    "C:\Program Files\Neo4j Desktop 2\resources\offline\runtime\zulu17*\bin\java.exe"
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
        $major = Get-JavaMajorFromVersionOutput -Text $versionResult.output
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

function Get-JavaHomeFromCommand {
  param([string]$JavaCommand)

  if ([string]::IsNullOrWhiteSpace($JavaCommand)) {
    return ""
  }
  if ($JavaCommand -eq "java") {
    return ""
  }
  $javaPath = Resolve-Path -LiteralPath $JavaCommand -ErrorAction SilentlyContinue
  if (-not $javaPath) {
    return ""
  }
  $binDir = Split-Path -Parent $javaPath.Path
  if ((Split-Path -Leaf $binDir) -ne "bin") {
    return ""
  }
  return Split-Path -Parent $binDir
}

function Resolve-MavenInvocation {
  param(
    [string]$RepoRoot,
    [string]$LocalRepository = ""
  )

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

  $embeddedMaven = Resolve-EmbeddedM2eMavenInvocation -RepoRoot $RepoRoot -LocalRepository $LocalRepository
  if ($embeddedMaven) {
    return $embeddedMaven
  }

  return $null
}

function Resolve-EmbeddedM2eMavenInvocation {
  param(
    [string]$RepoRoot,
    [string]$LocalRepository = ""
  )

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
  $localRepository = if ($LocalRepository) { Resolve-InputPath -BasePath $RepoRoot -InputPath $LocalRepository } else { Join-Path $RepoRoot "build\tmp\m2-repository" }
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

function Get-PomJavaMajor {
  param([string]$PomPath)

  $content = Get-Content -LiteralPath $PomPath -Raw
  if ($content -match "<java\.version>\s*([^<]+)\s*</java\.version>") {
    return Convert-JavaVersionToMajor -Version $Matches[1].Trim()
  }
  return 0
}

function Write-ReportAndExit {
  param(
    [object]$Report,
    [int]$ExitCode
  )

  $reportDir = Split-Path -Parent $ReportPath
  if (-not (Test-Path -LiteralPath $reportDir -PathType Container)) {
    New-Item -ItemType Directory -Path $reportDir | Out-Null
  }
  $Report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $ReportPath -Encoding UTF8
  if ($ExitCode -eq 0) {
    Write-Host "Java backend Maven tests passed: $ReportPath"
  }
  else {
    foreach ($failure in @($Report.failures | Where-Object { $_ })) {
      Write-Host $failure -ForegroundColor Yellow
    }
    Write-Host "Java backend Maven tests blocked or failed: $ReportPath" -ForegroundColor Yellow
  }
  exit $ExitCode
}

$repoRoot = Resolve-RepoRoot
if (-not $ReportPath) {
  $ReportPath = Join-Path $repoRoot "build\java-backend-tests.json"
}
if (-not [System.IO.Path]::IsPathRooted($ReportPath)) {
  $ReportPath = Join-Path $repoRoot $ReportPath
}
$resolvedMavenLocalRepository = Resolve-InputPath -BasePath $repoRoot -InputPath $MavenLocalRepository
$resolvedMavenSettings = Resolve-InputPath -BasePath $repoRoot -InputPath $MavenSettings
if ($resolvedMavenSettings -and -not (Test-Path -LiteralPath $resolvedMavenSettings -PathType Leaf)) {
  throw "Maven settings file was not found: $resolvedMavenSettings"
}
if ($resolvedMavenLocalRepository -and -not (Test-Path -LiteralPath $resolvedMavenLocalRepository -PathType Container)) {
  New-Item -ItemType Directory -Path $resolvedMavenLocalRepository | Out-Null
}

$modules = @(
  [PSCustomObject]@{ name = "resource-service"; path = "backend\resource-service\pom.xml" },
  [PSCustomObject]@{ name = "robot-service"; path = "backend\robot-service\pom.xml" },
  [PSCustomObject]@{ name = "rpa-auth"; path = "backend\rpa-auth\pom.xml" }
)
$failures = New-Object System.Collections.Generic.List[string]
$moduleResults = New-Object System.Collections.Generic.List[object]

$maxRequiredJavaMajor = 0
foreach ($module in $modules) {
  $pomPath = Join-Path $repoRoot $module.path
  if (-not (Test-Path -LiteralPath $pomPath -PathType Leaf)) {
    $failures.Add("Missing pom.xml: $($module.path)") | Out-Null
    continue
  }
  $requiredMajor = Get-PomJavaMajor -PomPath $pomPath
  $maxRequiredJavaMajor = [Math]::Max($maxRequiredJavaMajor, $requiredMajor)
}

$javaCommand = Resolve-JavaCommand -RequiredMajor $maxRequiredJavaMajor -WorkingDirectory $repoRoot
$javaOutput = ""
$installedJavaMajor = 0
$javaHome = ""
if ($javaCommand) {
  $javaVersion = Invoke-Captured -FilePath $javaCommand -Arguments @("-version") -WorkingDirectory $repoRoot
  $javaOutput = $javaVersion.output
  $installedJavaMajor = Get-JavaMajorFromVersionOutput -Text $javaOutput
  $javaHome = Get-JavaHomeFromCommand -JavaCommand $javaCommand
}
else {
  $failures.Add("java executable was not found.") | Out-Null
}
if (($maxRequiredJavaMajor -gt 0) -and ($installedJavaMajor -gt 0) -and ($installedJavaMajor -lt $maxRequiredJavaMajor)) {
  $failures.Add("Installed Java $installedJavaMajor does not satisfy required Java $maxRequiredJavaMajor+.") | Out-Null
}
elseif (($maxRequiredJavaMajor -gt 0) -and ($installedJavaMajor -eq 0)) {
  $failures.Add("Could not parse installed Java version; required Java $maxRequiredJavaMajor+.") | Out-Null
}

$mavenInvocation = Resolve-MavenInvocation -RepoRoot $repoRoot -LocalRepository $resolvedMavenLocalRepository
if (-not $mavenInvocation) {
  $failures.Add("Maven was not found on PATH, MAVEN_HOME, M2_HOME, common local install paths, or repo mvnw.cmd.") | Out-Null
}

if ($failures.Count -gt 0) {
  Write-ReportAndExit -Report ([PSCustomObject]@{
      schemaVersion = 1
      ok = $false
      generatedAt = (Get-Date).ToString("o")
      command = "mvn test for Java backend modules"
      javaExecutable = $javaCommand
      javaHome = $javaHome
      javaVersionOutput = $javaOutput
      installedJavaMajor = $installedJavaMajor
      requiredJavaMajor = $maxRequiredJavaMajor
      mavenLocalRepository = $resolvedMavenLocalRepository
      mavenSettings = $resolvedMavenSettings
      maven = $null
      modules = $moduleResults.ToArray()
      failures = $failures.ToArray()
    }) -ExitCode 1
}

if ($javaHome) {
  $env:JAVA_HOME = $javaHome
  $env:Path = (Join-Path $javaHome "bin") + [System.IO.Path]::PathSeparator + $env:Path
}

$mavenVersion = Invoke-Captured -FilePath $mavenInvocation.FilePath -Arguments ($mavenInvocation.ArgumentsPrefix + @("-version")) -WorkingDirectory $repoRoot
if ($mavenVersion.exitCode -ne 0) {
  $failures.Add("Maven version check failed.") | Out-Null
}

$mavenCommonArgs = @(
  "--batch-mode",
  "--no-transfer-progress",
  "-Daether.connector.connectTimeout=15000",
  "-Daether.connector.requestTimeout=30000",
  "-Dmaven.wagon.rto=30000"
)
if ($resolvedMavenLocalRepository) {
  $mavenCommonArgs += "-Dmaven.repo.local=$resolvedMavenLocalRepository"
}
if ($resolvedMavenSettings) {
  $mavenCommonArgs += @("-s", $resolvedMavenSettings)
}
$moduleTimeoutSeconds = 120
foreach ($module in $modules) {
  $pomPath = Join-Path $repoRoot $module.path
  $arguments = $mavenInvocation.ArgumentsPrefix + $mavenCommonArgs + @("-f", $pomPath)
  if ($SkipTests) {
    $arguments += @("-DskipTests=true", "package")
  }
  else {
    $arguments += @("-DskipTests=false", "test")
  }
  $result = Invoke-Captured -FilePath $mavenInvocation.FilePath -Arguments $arguments -WorkingDirectory $repoRoot -TimeoutSeconds $moduleTimeoutSeconds
  $ok = $result.exitCode -eq 0
  if (-not $ok) {
    if ($result.timedOut) {
      $failures.Add("$($module.name) Maven test timed out after $moduleTimeoutSeconds seconds.") | Out-Null
    }
    elseif ($result.output -match "repo\.maven\.apache\.org|Could not transfer artifact|Permission denied: getsockopt|Non-resolvable (parent|import) POM") {
      $failures.Add("$($module.name) Maven dependency resolution is blocked by Maven repository access (repo.maven.apache.org: Permission denied: getsockopt).") | Out-Null
    }
    else {
      $failures.Add("$($module.name) Maven test failed with exit code $($result.exitCode).") | Out-Null
    }
  }
  $moduleResults.Add([PSCustomObject]@{
      name = $module.name
      pom = $module.path
      ok = $ok
      exitCode = $result.exitCode
      timedOut = $result.timedOut
      terminationFailed = $result.terminationFailed
      timeoutSeconds = $moduleTimeoutSeconds
      durationSeconds = $result.durationSeconds
      outputTail = Get-OutputTail -Text $result.output
    }) | Out-Null
}

Write-ReportAndExit -Report ([PSCustomObject]@{
    schemaVersion = 1
    ok = $failures.Count -eq 0
    generatedAt = (Get-Date).ToString("o")
    command = if ($SkipTests) { "mvn package -DskipTests=true for Java backend modules" } else { "mvn test -DskipTests=false for Java backend modules" }
    javaExecutable = $javaCommand
    javaHome = $javaHome
    javaVersionOutput = $javaOutput
    installedJavaMajor = $installedJavaMajor
    requiredJavaMajor = $maxRequiredJavaMajor
    mavenLocalRepository = $resolvedMavenLocalRepository
    mavenSettings = $resolvedMavenSettings
    maven = [PSCustomObject]@{
      display = $mavenInvocation.Display
      output = $mavenVersion.output
    }
    modules = $moduleResults.ToArray()
    failures = $failures.ToArray()
  }) -ExitCode $(if ($failures.Count -eq 0) { 0 } else { 1 })
