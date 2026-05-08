param(
  [Parameter(ValueFromRemainingArguments = $true)]
  [string[]]$DoctorArgs
)

$ErrorActionPreference = "Stop"
$Strict = $false

foreach ($arg in $DoctorArgs) {
  switch ($arg) {
    "--" {
      continue
    }
    "-Strict" {
      $Strict = $true
    }
    "--Strict" {
      $Strict = $true
    }
    default {
      throw "Unknown doctor-release argument: $arg"
    }
  }
}

function Resolve-RepoRoot {
  $scriptDir = Split-Path -Parent $PSCommandPath
  return (Resolve-Path (Join-Path $scriptDir "..")).Path
}

function New-CheckResult {
  param(
    [string]$Name,
    [string]$Status,
    [string]$Detail
  )

  [PSCustomObject]@{
    Name = $Name
    Status = $Status
    Detail = $Detail
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
  param(
    [string]$RepoRoot,
    [string[]]$PomPaths
  )

  $maxMajor = 0
  $details = New-Object System.Collections.Generic.List[string]
  foreach ($pom in $PomPaths) {
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

function Test-CurrentUserInGroup {
  param([string]$GroupName)

  try {
    $groupResult = Invoke-Captured -FilePath "whoami" -Arguments @("/groups") -WorkingDirectory (Get-Location).Path
    return $groupResult.Output -match "(?i)(^|\s|\\)$([regex]::Escape($GroupName))(\s|$)"
  }
  catch {
    return $false
  }
}

function Get-DockerEngineDiagnostic {
  param([string]$BaseDetail)

  $details = New-Object System.Collections.Generic.List[string]
  if ($BaseDetail) {
    $details.Add($BaseDetail) | Out-Null
  }

  try {
    $dockerService = Get-Service -Name "com.docker.service" -ErrorAction SilentlyContinue
    if ($dockerService) {
      $details.Add("com.docker.service=$($dockerService.Status)") | Out-Null
    }
    else {
      $details.Add("com.docker.service not found") | Out-Null
    }
  }
  catch {
    $details.Add("could not inspect com.docker.service") | Out-Null
  }

  if (-not (Test-CurrentUserInGroup -GroupName "docker-users")) {
    $details.Add("current user is not in docker-users") | Out-Null
  }

  try {
    $dockerPipes = @(Get-ChildItem -LiteralPath "\\.\pipe\" -ErrorAction SilentlyContinue | Where-Object { $_.Name -match "docker" })
    if ($dockerPipes.Count -gt 0) {
      $details.Add("$($dockerPipes.Count) docker named pipes visible") | Out-Null
    }
    else {
      $details.Add("no docker named pipes visible") | Out-Null
    }
  }
  catch {
    $details.Add("could not inspect docker named pipes") | Out-Null
  }

  return ($details -join "; ")
}

function Resolve-AvailablePythonExe {
  $pythonCommand = Get-Command "python" -ErrorAction SilentlyContinue
  if ($pythonCommand) {
    return $pythonCommand.Source
  }

  $knownPython313 = Join-Path $env:LOCALAPPDATA "Programs\Python\Python313\python.exe"
  if (Test-Path -LiteralPath $knownPython313 -PathType Leaf) {
    return $knownPython313
  }

  $portablePython = Join-Path $repoRoot "build\python_core\python.exe"
  if (Test-Path -LiteralPath $portablePython -PathType Leaf) {
    return $portablePython
  }

  return ""
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

function Get-FirstLine {
  param([string]$Text)
  $line = (($Text -split "`r?`n") | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -First 1)
  if ($line) {
    return $line.Trim()
  }
  return ""
}

function Get-LastLine {
  param([string]$Text)
  $line = (($Text -split "`r?`n") | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Last 1)
  if ($line) {
    return $line.Trim()
  }
  return ""
}

function Get-JavaBackendTestsDoctorDetail {
  param([string]$ReportPath)

  if (-not (Test-Path -LiteralPath $ReportPath -PathType Leaf)) {
    return [PSCustomObject]@{
      Status = "WARN"
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
        Status = "WARN"
        Detail = "Maven repository access is blocked (repo.maven.apache.org: Permission denied: getsockopt)$moduleSummary"
      }
    }

    $failures = @($report.failures | Where-Object { $_ } | Select-Object -First 3)
    if ($failures.Count -gt 0) {
      return [PSCustomObject]@{
        Status = "WARN"
        Detail = $failures -join "; "
      }
    }

    return [PSCustomObject]@{
      Status = "WARN"
      Detail = "report has ok=false"
    }
  }
  catch {
    return [PSCustomObject]@{
      Status = "WARN"
      Detail = "report is unreadable; rerun corepack pnpm run test:java-backends"
    }
  }
}

function Get-FirstUsefulLine {
  param(
    [string]$Text,
    [string[]]$IgnoredLines = @()
  )

  foreach ($line in ($Text -split "`r?`n")) {
    $trimmed = $line.Trim()
    if (-not $trimmed) {
      continue
    }
    if ($IgnoredLines -contains $trimmed) {
      continue
    }
    return $trimmed
  }
  return ""
}

function Get-TextSnippet {
  param(
    [string]$Text,
    [int]$MaxLength = 180
  )

  $line = Get-FirstUsefulLine -Text $Text
  if (-not $line) {
    return ""
  }
  if ($line.Length -le $MaxLength) {
    return $line
  }
  return ($line.Substring(0, $MaxLength - 3) + "...")
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

function Assert-PortableFiles {
  param(
    [string]$PortableRoot,
    [string[]]$RelativePaths
  )

  $missing = @()
  foreach ($relativePath in $RelativePaths) {
    $fullPath = Join-Path $PortableRoot $relativePath
    if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) {
      $missing += $relativePath
      continue
    }

    if ((Get-Item -LiteralPath $fullPath).Length -le 0) {
      $missing += "$relativePath (empty)"
    }
  }
  return $missing
}

function Test-PortableSmokeResult {
  param([string]$SmokeResultPath)

  if (-not (Test-Path -LiteralPath $SmokeResultPath -PathType Leaf)) {
    $results.Add((New-CheckResult "portable smoke result" "WARN" "Missing smoke-verification.json; run verify:portable:host."))
    return
  }

  try {
    $smokeResult = Get-Content -LiteralPath $SmokeResultPath -Raw | ConvertFrom-Json
  }
  catch {
    $results.Add((New-CheckResult "portable smoke result" "FAIL" "smoke-verification.json is unreadable; rerun verify:portable:host."))
    return
  }

  if ($smokeResult.ok -ne $true) {
    $detail = if ($smokeResult.error) { [string]$smokeResult.error } else { "smoke result reported ok=false" }
    $results.Add((New-CheckResult "portable smoke result" "FAIL" $detail))
    return
  }

  $freshnessProblem = Get-ReportFreshnessProblem -Report $smokeResult -PropertyName "generatedAt"
  if ($freshnessProblem) {
    $results.Add((New-CheckResult "portable smoke result" "WARN" "$freshnessProblem; run corepack pnpm run verify:portable:host."))
    return
  }

  if ($smokeResult.packagedRuntime -ne $true) {
    $results.Add((New-CheckResult "portable smoke result" "FAIL" "smoke test did not detect the packaged runtime layout."))
    return
  }

  if (-not ([string]$smokeResult.appPath).EndsWith("resources\app.asar")) {
    $results.Add((New-CheckResult "portable smoke result" "FAIL" "smoke test loaded the wrong app path: $($smokeResult.appPath)"))
    return
  }

  $smokeRendererPath = [string]$smokeResult.rendererPath
  if (-not ($smokeRendererPath.Contains("resources\renderer") -or $smokeRendererPath.Contains("resources\app.asar\out\renderer"))) {
    $results.Add((New-CheckResult "portable smoke result" "FAIL" "smoke test loaded the wrong renderer path: $($smokeResult.rendererPath)"))
    return
  }

  if (-not $smokeResult.appWorkPath -or ([string]$smokeResult.appWorkPath).Contains("app.asar")) {
    $results.Add((New-CheckResult "portable smoke result" "FAIL" "smoke test returned an invalid app work path: $($smokeResult.appWorkPath)"))
    return
  }

  $results.Add((New-CheckResult "portable smoke result" "PASS" "packaged startup smoke result is valid"))
}

function Test-BrowserBridgeInjectSync {
  param(
    [string]$RepoRoot,
    [string]$PythonCoreRoot
  )

  $files = @("backgroundInject.js", "contentInject.js")
  $frontendInjectRoot = Join-Path $RepoRoot "frontend\packages\browser-plugin\dist-bridge-inject"
  $engineInjectRoot = Join-Path $RepoRoot "engine\servers\astronverse-browser-bridge\src\astronverse\browser_bridge\inject"
  $pythonCoreInjectRoot = Join-Path $PythonCoreRoot "Lib\site-packages\astronverse\browser_bridge\inject"
  $problems = New-Object System.Collections.Generic.List[string]

  foreach ($file in $files) {
    $frontendPath = Join-Path $frontendInjectRoot $file
    $enginePath = Join-Path $engineInjectRoot $file
    $pythonCorePath = Join-Path $pythonCoreInjectRoot $file

    if (-not (Test-Path -LiteralPath $frontendPath -PathType Leaf)) {
      $problems.Add("missing frontend build output: $file") | Out-Null
      continue
    }
    if (-not (Test-Path -LiteralPath $enginePath -PathType Leaf)) {
      $problems.Add("missing engine inject file: $file") | Out-Null
      continue
    }

    $frontendHash = (Get-FileHash -LiteralPath $frontendPath -Algorithm SHA256).Hash
    $engineHash = (Get-FileHash -LiteralPath $enginePath -Algorithm SHA256).Hash
    if ($frontendHash -ne $engineHash) {
      $problems.Add("engine inject mismatch: $file") | Out-Null
    }

    if (Test-Path -LiteralPath $pythonCorePath -PathType Leaf) {
      $pythonCoreHash = (Get-FileHash -LiteralPath $pythonCorePath -Algorithm SHA256).Hash
      if ($frontendHash -ne $pythonCoreHash) {
        $problems.Add("python_core inject mismatch: $file") | Out-Null
      }
    }
  }

  if ($problems.Count -gt 0) {
    $results.Add((New-CheckResult "browser bridge inject sync" "WARN" (($problems -join "; ") + "; run corepack pnpm --dir frontend --filter @rpa/extension run build:bridge-inject and rebuild the engine archive.")))
  }
  else {
    $results.Add((New-CheckResult "browser bridge inject sync" "PASS" "frontend build, engine source, and python_core inject files match"))
  }
}

function Test-EngineSourceOverlaySync {
  param(
    [string]$RepoRoot,
    [string]$PythonCoreRoot
  )

  $engineRoot = Join-Path $RepoRoot "engine"
  $targetNamespace = Join-Path $PythonCoreRoot "Lib\site-packages\astronverse"
  if (-not (Test-Path -LiteralPath $targetNamespace -PathType Container)) {
    $results.Add((New-CheckResult "engine source overlay sync" "WARN" "Skipped; bundled astronverse namespace is missing: $targetNamespace"))
    return
  }

  $sourceEntries = @("shared", "servers", "components") |
    ForEach-Object { Join-Path $engineRoot $_ } |
    Where-Object { Test-Path -LiteralPath $_ -PathType Container } |
    ForEach-Object {
      Get-ChildItem -LiteralPath $_ -Directory | ForEach-Object {
        $sourceNamespace = Join-Path $_.FullName "src\astronverse"
        if (Test-Path -LiteralPath $sourceNamespace -PathType Container) {
          Get-ChildItem -LiteralPath $sourceNamespace -Directory
        }
      }
    }

  if (@($sourceEntries).Count -eq 0) {
    $results.Add((New-CheckResult "engine source overlay sync" "WARN" "Skipped; no engine source packages were found."))
    return
  }

  $problems = New-Object System.Collections.Generic.List[string]
  $checkedFiles = 0
  foreach ($sourceEntry in $sourceEntries) {
    $targetEntry = Join-Path $targetNamespace $sourceEntry.Name
    if (-not (Test-Path -LiteralPath $targetEntry -PathType Container)) {
      $problems.Add("missing bundled package: $($sourceEntry.Name)") | Out-Null
      continue
    }

    $sourceFiles = Get-ChildItem -LiteralPath $sourceEntry.FullName -File -Recurse |
      Where-Object {
        $_.FullName -notmatch "\\__pycache__\\" -and
        $_.Extension -notin @(".pyc", ".pyo")
      }
    foreach ($sourceFile in $sourceFiles) {
      $relativePath = $sourceFile.FullName.Substring($sourceEntry.FullName.Length).TrimStart("\", "/")
      $targetFile = Join-Path $targetEntry $relativePath
      $checkedFiles += 1
      if (-not (Test-Path -LiteralPath $targetFile -PathType Leaf)) {
        $problems.Add("missing bundled file: $($sourceEntry.Name)\$relativePath") | Out-Null
        continue
      }

      $sourceHash = (Get-FileHash -LiteralPath $sourceFile.FullName -Algorithm SHA256).Hash
      $targetHash = (Get-FileHash -LiteralPath $targetFile -Algorithm SHA256).Hash
      if ($sourceHash -ne $targetHash) {
        $problems.Add("bundled file mismatch: $($sourceEntry.Name)\$relativePath") | Out-Null
      }
    }
  }

  if ($problems.Count -gt 0) {
    $preview = @($problems | Select-Object -First 5) -join "; "
    if ($problems.Count -gt 5) {
      $preview = "$preview; ..."
    }
    $results.Add((New-CheckResult "engine source overlay sync" "WARN" "$preview; run SHOPRPA_OFFLINE_ENGINE_OVERLAY=1 .\build.bat --skip-frontend or rebuild the engine archive."))
  }
  else {
    $results.Add((New-CheckResult "engine source overlay sync" "PASS" "$checkedFiles engine source files match build\python_core"))
  }
}

function Test-PortablePythonArchiveSource {
  param(
    [string]$RepoRoot,
    [string]$PortableRoot
  )

  $sourceArchive = Join-Path $RepoRoot "resources\python_core.7z"
  $sourceHashPath = Join-Path $RepoRoot "resources\python_core.7z.sha256.txt"
  $portableArchive = Join-Path $PortableRoot "resources\python_core.7z"
  $portableHashPath = Join-Path $PortableRoot "resources\python_core.7z.sha256.txt"
  $requiredFiles = @($sourceArchive, $sourceHashPath, $portableArchive, $portableHashPath)
  $missingFiles = @($requiredFiles | Where-Object { -not (Test-Path -LiteralPath $_ -PathType Leaf) })
  if ($missingFiles.Count -gt 0) {
    $relativeMissing = @($missingFiles | ForEach-Object { Get-RelativePath -BasePath $RepoRoot -TargetPath $_ })
    $results.Add((New-CheckResult "portable python archive source" "FAIL" "Missing: $($relativeMissing -join ', ')"))
    return
  }

  $sourceArchiveHash = (Get-FileHash -LiteralPath $sourceArchive -Algorithm SHA256).Hash.ToUpperInvariant()
  $portableArchiveHash = (Get-FileHash -LiteralPath $portableArchive -Algorithm SHA256).Hash.ToUpperInvariant()
  $sourceExpectedHash = (Get-Content -LiteralPath $sourceHashPath -Raw).Trim().ToUpperInvariant()
  $portableExpectedHash = (Get-Content -LiteralPath $portableHashPath -Raw).Trim().ToUpperInvariant()
  if (($sourceArchiveHash -ne $sourceExpectedHash) -or ($portableArchiveHash -ne $portableExpectedHash)) {
    $results.Add((New-CheckResult "portable python archive source" "FAIL" "Source or portable hash file does not match its archive."))
    return
  }
  if ($sourceArchiveHash -ne $portableArchiveHash) {
    $results.Add((New-CheckResult "portable python archive source" "FAIL" "Portable archive differs from resources archive: resources=$sourceArchiveHash, portable=$portableArchiveHash"))
    return
  }

  $results.Add((New-CheckResult "portable python archive source" "PASS" "portable archive matches resources archive $portableArchiveHash"))
}

function Test-PortableInstallerPackage {
  param([string]$RepoRoot)

  $packageJsonPath = Join-Path $RepoRoot "frontend\packages\electron-app\package.json"
  if (-not (Test-Path -LiteralPath $packageJsonPath -PathType Leaf)) {
    $results.Add((New-CheckResult "portable installer package" "WARN" "Skipped; Electron package.json is missing."))
    return
  }

  try {
    $packageJson = Get-Content -LiteralPath $packageJsonPath -Raw | ConvertFrom-Json
    $version = [string]$packageJson.version
  }
  catch {
    $results.Add((New-CheckResult "portable installer package" "WARN" "Could not read Electron package version."))
    return
  }

  $packagePath = Join-Path $RepoRoot "frontend\packages\electron-app\dist\installers\ShopRPA-$version-portable-installer.zip"
  $hashPath = "$packagePath.sha256.txt"
  $verificationPath = "$packagePath.verify.json"

  if (-not (Test-Path -LiteralPath $packagePath -PathType Leaf)) {
    $results.Add((New-CheckResult "portable installer package" "WARN" "Missing; run corepack pnpm run build:portable-installer."))
    return
  }
  if (-not (Test-Path -LiteralPath $hashPath -PathType Leaf)) {
    $results.Add((New-CheckResult "portable installer package" "WARN" "Hash file is missing; rerun build:portable-installer."))
    return
  }

  $expectedHash = (Get-Content -LiteralPath $hashPath -Raw).Trim().ToUpperInvariant()
  $actualHash = (Get-FileHash -LiteralPath $packagePath -Algorithm SHA256).Hash.ToUpperInvariant()
  if ($expectedHash -ne $actualHash) {
    $results.Add((New-CheckResult "portable installer package" "FAIL" "Hash mismatch: expected $expectedHash, got $actualHash"))
    return
  }

  if (-not (Test-Path -LiteralPath $verificationPath -PathType Leaf)) {
    $results.Add((New-CheckResult "portable installer package" "WARN" "Package exists but has not been verified; run corepack pnpm run verify:portable-installer."))
    return
  }

  try {
    $verification = Get-Content -LiteralPath $verificationPath -Raw | ConvertFrom-Json
    $freshnessProblem = Get-ReportFreshnessProblem -Report $verification -PropertyName "verifiedAt"
    if ($freshnessProblem) {
      $results.Add((New-CheckResult "portable installer package" "WARN" "$freshnessProblem; rerun verify:portable-installer."))
    }
    elseif (($verification.ok -eq $true) -and ([string]$verification.packageSha256).ToUpperInvariant() -eq $actualHash) {
      $requiredChecks = @("installerManifest", "installerReadme", "installedManifest", "smoke", "uninstall")
      $missingChecks = @()
      $checkDetails = @()
      foreach ($checkName in $requiredChecks) {
        $checkValue = [string]$verification.$checkName
        if ($checkValue) {
          $checkDetails += "$checkName=$checkValue"
        }
        if ($checkValue -ne "pass") {
          $missingChecks += $checkName
        }
      }
      if ($missingChecks.Count -gt 0) {
        $results.Add((New-CheckResult "portable installer package" "WARN" "Verification JSON is missing required checks: $($missingChecks -join ', '); rerun verify:portable-installer."))
      }
      else {
        $results.Add((New-CheckResult "portable installer package" "PASS" "verified zip package $actualHash; $($checkDetails -join ', ')"))
      }
    }
    else {
      $results.Add((New-CheckResult "portable installer package" "WARN" "Verification JSON is stale or invalid; rerun verify:portable-installer."))
    }
  }
  catch {
    $results.Add((New-CheckResult "portable installer package" "WARN" "Verification JSON is unreadable; rerun verify:portable-installer."))
  }
}

function Read-EnvValue {
  param(
    [string]$EnvPath,
    [string]$Key
  )

  if (-not (Test-Path -LiteralPath $EnvPath -PathType Leaf)) {
    return ""
  }

  $pattern = "^\s*$([regex]::Escape($Key))\s*=\s*(.+?)\s*$"
  foreach ($line in Get-Content -LiteralPath $EnvPath) {
    if ($line -match $pattern) {
      return $Matches[1].Trim().Trim('"').Trim("'")
    }
  }
  return ""
}

function Test-CasdoorSeedHygiene {
  param(
    [string]$EnvPath,
    [string]$InitDataPath
  )

  if (-not (Test-Path -LiteralPath $EnvPath -PathType Leaf)) {
    $results.Add((New-CheckResult "casdoor seed hygiene" "WARN" "Skipped; env file is missing: $EnvPath"))
    return
  }
  if (-not (Test-Path -LiteralPath $InitDataPath -PathType Leaf)) {
    $results.Add((New-CheckResult "casdoor seed hygiene" "FAIL" "Missing Casdoor init data: $InitDataPath"))
    return
  }

  $expectedOrg = Read-EnvValue -EnvPath $EnvPath -Key "CASDOOR_ORGANIZATION_NAME"
  $expectedApp = Read-EnvValue -EnvPath $EnvPath -Key "CASDOOR_APPLICATION_NAME"
  $problems = New-Object System.Collections.Generic.List[string]

  try {
    $rawInitData = Get-Content -LiteralPath $InitDataPath -Raw
    $initData = $rawInitData | ConvertFrom-Json
  }
  catch {
    $results.Add((New-CheckResult "casdoor seed hygiene" "FAIL" "Casdoor init_data_dump.json is not valid JSON."))
    return
  }

  $forbiddenSeedText = @("example-org", "example-app", "example-user", "example-role", "example.com", "admin@example.com", "Example Inc.", "dc=example", "예시사용자", "예시역할", "예시인증서", "New User", "New Role", "18888888888")
  foreach ($forbidden in $forbiddenSeedText) {
    if ($rawInitData.Contains($forbidden)) {
      $problems.Add("seed contains $forbidden") | Out-Null
    }
  }

  if (-not $expectedOrg -or -not $expectedApp) {
    $problems.Add("CASDOOR_ORGANIZATION_NAME or CASDOOR_APPLICATION_NAME is missing from env") | Out-Null
  }
  else {
    $org = @($initData.organizations | Where-Object { $_.name -eq $expectedOrg })
    $app = @($initData.applications | Where-Object { $_.name -eq $expectedApp })
    if ($org.Count -ne 1) {
      $problems.Add("expected organization not found exactly once: $expectedOrg") | Out-Null
    }
    if ($app.Count -ne 1) {
      $problems.Add("expected application not found exactly once: $expectedApp") | Out-Null
    }
    elseif ($app[0].organization -ne $expectedOrg) {
      $problems.Add("application $expectedApp points to organization $($app[0].organization), expected $expectedOrg") | Out-Null
    }
  }

  $seedUsers = @($initData.users)
  $seedRecords = @($initData.records)
  $seedTokens = @($initData.tokens)
  $seedSessions = @($initData.sessions)
  if ($seedUsers.Count -gt 2) {
    $problems.Add("seed contains more users than the built-in and ShopRPA admin accounts") | Out-Null
  }
  if (($seedRecords.Count + $seedTokens.Count + $seedSessions.Count) -gt 0) {
    $problems.Add("seed contains request records, access tokens, or sessions") | Out-Null
  }

  if ($problems.Count -gt 0) {
    $results.Add((New-CheckResult "casdoor seed hygiene" "FAIL" ($problems -join "; ")))
  }
  else {
    $results.Add((New-CheckResult "casdoor seed hygiene" "PASS" "seed uses ShopRPA org/app names and contains no sample users, tokens, sessions, or request logs"))
  }
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

function Get-DockerGatewayPort {
  param([string]$ComposePath)

  if (-not (Test-Path -LiteralPath $ComposePath -PathType Leaf)) {
    return $null
  }

  foreach ($line in Get-Content -LiteralPath $ComposePath) {
    if ($line -match '^\s*-\s*["'']?(\d+):80["'']?\s*$') {
      return [int]$Matches[1]
    }
  }
  return $null
}

function Test-RemoteAddrConfig {
  param(
    [string]$ConfigPath,
    [string]$Label,
    [Nullable[int]]$ExpectedPort
  )

  if (-not (Test-Path -LiteralPath $ConfigPath -PathType Leaf)) {
    $results.Add((New-CheckResult $Label "FAIL" "Missing config file: $ConfigPath"))
    return
  }

  $remoteAddr = Read-YamlScalarValue -YamlPath $ConfigPath -Key "remote_addr"
  if (-not $remoteAddr) {
    $results.Add((New-CheckResult $Label "FAIL" "remote_addr is missing."))
    return
  }

  try {
    $remoteUri = [System.Uri]::new($remoteAddr)
    if ($remoteUri.Scheme -notin @("http", "https")) {
      $results.Add((New-CheckResult $Label "FAIL" "remote_addr must use http or https: $remoteAddr"))
      return
    }

    if ($ExpectedPort -and $remoteUri.Port -ne $ExpectedPort) {
      $results.Add((New-CheckResult $Label "FAIL" "remote_addr port $($remoteUri.Port) does not match Docker gateway port $ExpectedPort."))
      return
    }

    $results.Add((New-CheckResult $Label "PASS" $remoteUri.AbsoluteUri))
  }
  catch {
    $results.Add((New-CheckResult $Label "FAIL" "remote_addr is not a valid URL: $remoteAddr"))
  }
}

function Test-DockerComposePythonHealthchecks {
  param([string]$ComposePath)

  if (-not (Test-Path -LiteralPath $ComposePath -PathType Leaf)) {
    $results.Add((New-CheckResult "python service compose healthchecks" "FAIL" "Missing compose file: $ComposePath"))
    return
  }

  $composeText = Get-Content -LiteralPath $ComposePath -Raw
  $problems = New-Object System.Collections.Generic.List[string]
  $requiredPatterns = @(
    @{
      Label = "ai-service /health"
      Pattern = "(?s)ai-service:\s.*?healthcheck:\s.*?127\.0\.0\.1:8010/health"
    },
    @{
      Label = "openapi-service /health"
      Pattern = "(?s)openapi-service:\s.*?healthcheck:\s.*?127\.0\.0\.1:8020/health"
    },
    @{
      Label = "nginx waits for ai-service health"
      Pattern = "(?s)openresty-nginx:\s.*?ai-service:\s*\r?\n\s*condition:\s*service_healthy"
    },
    @{
      Label = "nginx waits for openapi-service health"
      Pattern = "(?s)openresty-nginx:\s.*?openapi-service:\s*\r?\n\s*condition:\s*service_healthy"
    }
  )

  foreach ($requiredPattern in $requiredPatterns) {
    if ($composeText -notmatch $requiredPattern.Pattern) {
      $problems.Add($requiredPattern.Label) | Out-Null
    }
  }

  if ($problems.Count -gt 0) {
    $results.Add((New-CheckResult "python service compose healthchecks" "FAIL" ("Missing: " + ($problems -join ", "))))
  }
  else {
    $results.Add((New-CheckResult "python service compose healthchecks" "PASS" "ai-service and openapi-service expose /health checks and nginx waits for them"))
  }
}

function Test-DockerComposeJavaHealthchecks {
  param([string]$ComposePath)

  if (-not (Test-Path -LiteralPath $ComposePath -PathType Leaf)) {
    $results.Add((New-CheckResult "java service compose healthchecks" "FAIL" "Missing compose file: $ComposePath"))
    return
  }

  $composeText = Get-Content -LiteralPath $ComposePath -Raw
  $problems = New-Object System.Collections.Generic.List[string]
  $requiredPatterns = @(
    @{
      Label = "resource-service health probe"
      Pattern = "(?s)resource-service:\s.*?healthcheck:\s.*?com\.iflytek\.rpa\.resource\.common\.health\.HealthcheckProbe"
    },
    @{
      Label = "robot-service health probe"
      Pattern = "(?s)robot-service:\s.*?healthcheck:\s.*?com\.iflytek\.rpa\.common\.health\.HealthcheckProbe"
    },
    @{
      Label = "rpa-auth health probe"
      Pattern = "(?s)rpa-auth:\s.*?healthcheck:\s.*?com\.iflytek\.rpa\.auth\.health\.HealthcheckProbe"
    },
    @{
      Label = "nginx waits for resource-service health"
      Pattern = "(?s)openresty-nginx:\s.*?resource-service:\s*\r?\n\s*condition:\s*service_healthy"
    },
    @{
      Label = "nginx waits for robot-service health"
      Pattern = "(?s)openresty-nginx:\s.*?robot-service:\s*\r?\n\s*condition:\s*service_healthy"
    },
    @{
      Label = "nginx waits for rpa-auth health"
      Pattern = "(?s)openresty-nginx:\s.*?rpa-auth:\s*\r?\n\s*condition:\s*service_healthy"
    }
  )

  foreach ($requiredPattern in $requiredPatterns) {
    if ($composeText -notmatch $requiredPattern.Pattern) {
      $problems.Add($requiredPattern.Label) | Out-Null
    }
  }

  if ($problems.Count -gt 0) {
    $results.Add((New-CheckResult "java service compose healthchecks" "FAIL" ("Missing: " + ($problems -join ", "))))
  }
  else {
    $results.Add((New-CheckResult "java service compose healthchecks" "PASS" "resource, robot, and auth services expose container probes and nginx waits for them"))
  }
}

function Test-NginxGatewaySource {
  param([string]$RepoRoot)

  $confPath = Join-Path $RepoRoot "docker\volumes\nginx\default.conf"
  $authLuaPath = Join-Path $RepoRoot "docker\volumes\nginx\lua\auth_handler.lua"
  $problems = New-Object System.Collections.Generic.List[string]

  foreach ($file in @($confPath, $authLuaPath)) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
      $problems.Add("missing file: $file") | Out-Null
    }
  }

  if (Test-Path -LiteralPath $confPath -PathType Leaf) {
    $confText = Get-Content -LiteralPath $confPath -Raw
    $healthLocations = @(
      @{
        Label    = "ai-service"
        Path     = "/api/rpa-ai-service/health"
        Upstream = "ai_service_endpoint"
      },
      @{
        Label    = "openapi-service"
        Path     = "/api/rpa-openapi/health"
        Upstream = "openapi_service_endpoint"
      }
    )

    foreach ($healthLocation in $healthLocations) {
      $blockPattern = "location\s+=\s+$([regex]::Escape($healthLocation.Path))\s*\{(?s:.*?)\n\s*\}"
      $blockMatch = [regex]::Match($confText, $blockPattern)
      if (-not $blockMatch.Success) {
        $problems.Add("missing unauthenticated $($healthLocation.Label) gateway health location") | Out-Null
        continue
      }
      if ($blockMatch.Value -match "access_by_lua_file") {
        $problems.Add("$($healthLocation.Label) gateway health still runs Lua auth") | Out-Null
      }
      $expectedProxy = "proxy_pass http://`$$($healthLocation.Upstream);"
      if ($blockMatch.Value -notlike "*$expectedProxy*") {
        $problems.Add("$($healthLocation.Label) gateway health does not proxy to $($healthLocation.Upstream)") | Out-Null
      }
    }
  }

  if (Test-Path -LiteralPath $authLuaPath -PathType Leaf) {
    $authLua = Get-Content -LiteralPath $authLuaPath -Raw
    $bearerBlock = [regex]::Match($authLua, 'if\s+token_type\s+and\s+token_type:lower\(\)\s*==\s*"bearer"\s+then(?s:.*?)(?:else|end)')
    if (-not $bearerBlock.Success) {
      $problems.Add("Bearer token handling block was not found in auth_handler.lua") | Out-Null
    }
    else {
      if ($bearerBlock.Value -notmatch "session_token\s*=\s*token_value") {
        $problems.Add("Bearer token is not captured for robot-service validation") | Out-Null
      }
      if ($bearerBlock.Value -match "(?m)^\s*return\s*$") {
        $problems.Add("Bearer token branch bypasses robot-service validation") | Out-Null
      }
    }
  }

  if ($problems.Count -gt 0) {
    $results.Add((New-CheckResult "gateway auth source" "FAIL" ($problems -join "; ")))
  }
  else {
    $results.Add((New-CheckResult "gateway auth source" "PASS" "health probes bypass session auth and Bearer tokens are validated"))
  }
}

function Test-RobotOpenApiWorkflowConfig {
  param([string]$RepoRoot)

  $robotConfigPath = Join-Path $RepoRoot "backend\robot-service\src\main\resources\application-local.yml"
  $dockerEnvPath = Join-Path $RepoRoot "docker\.env"
  $dockerExampleEnvPath = Join-Path $RepoRoot "docker\.env.example"
  $composePath = Join-Path $RepoRoot "docker\docker-compose.yml"
  $expectedUrl = "http://openapi-service:8020/workflows/upsert"
  $problems = New-Object System.Collections.Generic.List[string]

  $badEndpointResult = Invoke-Captured -FilePath "rg" -Arguments @("-n", "rpa-openapi:6699|ExampleConstants\.WORKFLOWS_UPSERT_URL|static\s+.*WORKFLOWS_UPSERT_URL", "backend\robot-service") -WorkingDirectory $RepoRoot
  if ($badEndpointResult.ExitCode -eq 0) {
    $problems.Add("stale hardcoded OpenAPI endpoint found: $(Get-FirstUsefulLine -Text $badEndpointResult.Output)") | Out-Null
  }
  elseif ($badEndpointResult.ExitCode -gt 1) {
    $problems.Add("could not scan robot-service OpenAPI endpoint: $(Get-FirstUsefulLine -Text $badEndpointResult.Output)") | Out-Null
  }

  foreach ($file in @($robotConfigPath, $dockerEnvPath, $dockerExampleEnvPath, $composePath)) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
      $problems.Add("missing file: $file") | Out-Null
    }
  }

  if ((Test-Path -LiteralPath $robotConfigPath -PathType Leaf) -and ((Get-Content -LiteralPath $robotConfigPath -Raw) -notmatch [regex]::Escape($expectedUrl))) {
    $problems.Add("robot-service config does not default to $expectedUrl") | Out-Null
  }
  if ((Test-Path -LiteralPath $dockerEnvPath -PathType Leaf) -and ((Get-Content -LiteralPath $dockerEnvPath -Raw) -notmatch [regex]::Escape("OPENAPI_WORKFLOWS_UPSERT_URL=`"$expectedUrl`""))) {
    $problems.Add("docker/.env does not set OPENAPI_WORKFLOWS_UPSERT_URL to $expectedUrl") | Out-Null
  }
  if ((Test-Path -LiteralPath $dockerExampleEnvPath -PathType Leaf) -and ((Get-Content -LiteralPath $dockerExampleEnvPath -Raw) -notmatch [regex]::Escape("OPENAPI_WORKFLOWS_UPSERT_URL=`"$expectedUrl`""))) {
    $problems.Add("docker/.env.example does not set OPENAPI_WORKFLOWS_UPSERT_URL to $expectedUrl") | Out-Null
  }
  if ((Test-Path -LiteralPath $composePath -PathType Leaf) -and ((Get-Content -LiteralPath $composePath -Raw) -notmatch "(?m)^\s+openapi-service:\s*$")) {
    $problems.Add("docker compose does not define openapi-service") | Out-Null
  }

  if ($problems.Count -gt 0) {
    $results.Add((New-CheckResult "robot openapi workflow URL" "FAIL" ($problems -join "; ")))
  }
  else {
    $results.Add((New-CheckResult "robot openapi workflow URL" "PASS" $expectedUrl))
  }
}

function Test-OpenApiRobotServiceConfig {
  param([string]$RepoRoot)

  $expectedBaseUrl = "http://robot-service:8040"
  $configPath = Join-Path $RepoRoot "backend\openapi-service\app\config.py"
  $dockerEnvPath = Join-Path $RepoRoot "docker\.env"
  $dockerExampleEnvPath = Join-Path $RepoRoot "docker\.env.example"
  $problems = New-Object System.Collections.Generic.List[string]

  $hardcodedResult = Invoke-Captured -FilePath "rg" -Arguments @("-n", "http://robot-service:8040/api/robot", "backend\openapi-service") -WorkingDirectory $RepoRoot
  if ($hardcodedResult.ExitCode -eq 0) {
    $problems.Add("hardcoded robot-service API URL found: $(Get-FirstUsefulLine -Text $hardcodedResult.Output)") | Out-Null
  }
  elseif ($hardcodedResult.ExitCode -gt 1) {
    $problems.Add("could not scan openapi-service robot URL: $(Get-FirstUsefulLine -Text $hardcodedResult.Output)") | Out-Null
  }

  foreach ($file in @($configPath, $dockerEnvPath, $dockerExampleEnvPath)) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
      $problems.Add("missing file: $file") | Out-Null
    }
  }

  if ((Test-Path -LiteralPath $configPath -PathType Leaf) -and ((Get-Content -LiteralPath $configPath -Raw) -notmatch "ROBOT_SERVICE_BASE_URL")) {
    $problems.Add("openapi-service settings do not expose ROBOT_SERVICE_BASE_URL") | Out-Null
  }
  if ((Test-Path -LiteralPath $dockerEnvPath -PathType Leaf) -and ((Get-Content -LiteralPath $dockerEnvPath -Raw) -notmatch [regex]::Escape("ROBOT_SERVICE_BASE_URL=`"$expectedBaseUrl`""))) {
    $problems.Add("docker/.env does not set ROBOT_SERVICE_BASE_URL to $expectedBaseUrl") | Out-Null
  }
  if ((Test-Path -LiteralPath $dockerExampleEnvPath -PathType Leaf) -and ((Get-Content -LiteralPath $dockerExampleEnvPath -Raw) -notmatch [regex]::Escape("ROBOT_SERVICE_BASE_URL=`"$expectedBaseUrl`""))) {
    $problems.Add("docker/.env.example does not set ROBOT_SERVICE_BASE_URL to $expectedBaseUrl") | Out-Null
  }

  if ($problems.Count -gt 0) {
    $results.Add((New-CheckResult "openapi robot service URL" "FAIL" ($problems -join "; ")))
  }
  else {
    $results.Add((New-CheckResult "openapi robot service URL" "PASS" $expectedBaseUrl))
  }
}

function Test-OpenApiAdminRouteSource {
  param([string]$RepoRoot)

  $mainPath = Join-Path $RepoRoot "backend\openapi-service\app\main.py"
  $adminPath = Join-Path $RepoRoot "backend\openapi-service\app\internal\admin.py"
  $problems = New-Object System.Collections.Generic.List[string]

  foreach ($file in @($mainPath, $adminPath)) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
      $problems.Add("missing file: $file") | Out-Null
    }
  }

  if (Test-Path -LiteralPath $mainPath -PathType Leaf) {
    $mainSource = Get-Content -LiteralPath $mainPath -Raw
    if ($mainSource -notmatch "from app\.internal import admin") {
      $problems.Add("main.py does not import app.internal.admin") | Out-Null
    }
    if ($mainSource -notmatch "include_router\(admin\.router,\s*prefix=`"/admin`"") {
      $problems.Add("main.py does not mount admin router at /admin") | Out-Null
    }
  }

  if (Test-Path -LiteralPath $adminPath -PathType Leaf) {
    $adminSource = Get-Content -LiteralPath $adminPath -Raw
    if ($adminSource -match "schwifty|Hello Bigger Applications|TODO|FIXME") {
      $problems.Add("admin route contains placeholder text") | Out-Null
    }
    if ($adminSource -notmatch "verify_admin_api_key") {
      $problems.Add("admin route does not enforce internal API key") | Out-Null
    }
    if ($adminSource -notmatch "X-API-Key") {
      $problems.Add("admin route does not require X-API-Key") | Out-Null
    }
  }

  if ($problems.Count -gt 0) {
    $results.Add((New-CheckResult "openapi admin route source" "FAIL" ($problems -join "; ")))
  }
  else {
    $results.Add((New-CheckResult "openapi admin route source" "PASS" "admin router is mounted and protected by X-API-Key"))
  }
}

function Test-HttpHealthEndpoint {
  param(
    [string]$BaseUrl,
    [string]$Label
  )

  if (-not $BaseUrl) {
    $results.Add((New-CheckResult $Label "WARN" "Skipped; base URL is missing."))
    return
  }

  try {
    $baseUri = [System.Uri]::new($BaseUrl)
    $healthUri = [System.Uri]::new($baseUri, "health")
    $response = Invoke-WebRequest -Uri $healthUri.AbsoluteUri -UseBasicParsing -TimeoutSec 5
    if ($response.StatusCode -eq 200) {
      $results.Add((New-CheckResult $Label "PASS" "$($healthUri.AbsoluteUri) returned HTTP 200"))
    }
    else {
      $results.Add((New-CheckResult $Label "WARN" "$($healthUri.AbsoluteUri) returned HTTP $($response.StatusCode)"))
    }
  }
  catch {
    $results.Add((New-CheckResult $Label "WARN" "Backend gateway health endpoint is not reachable: $($_.Exception.Message)"))
  }
}

function Test-HttpStaticAsset {
  param(
    [string]$BaseUrl,
    [string]$AssetPath,
    [string]$Label,
    [string]$SourcePath = ""
  )

  if (-not $BaseUrl) {
    $results.Add((New-CheckResult $Label "WARN" "Skipped; base URL is missing."))
    return
  }

  try {
    $baseUri = [System.Uri]::new($BaseUrl)
    $assetUri = [System.Uri]::new($baseUri, $AssetPath.TrimStart("/"))
    $response = Invoke-WebRequest -Uri $assetUri.AbsoluteUri -UseBasicParsing -TimeoutSec 5
    if ($response.StatusCode -eq 200 -and $response.Content.Length -gt 0) {
      $results.Add((New-CheckResult $Label "PASS" "$($assetUri.AbsoluteUri) returned HTTP 200"))
    }
    else {
      $results.Add((New-CheckResult $Label "WARN" "$($assetUri.AbsoluteUri) returned HTTP $($response.StatusCode) with empty or invalid content"))
    }
  }
  catch {
    if ($SourcePath -and (Test-Path -LiteralPath $SourcePath -PathType Leaf)) {
      $sourceContent = Get-Content -LiteralPath $SourcePath -Raw
      if ($sourceContent.Trim().Length -gt 0) {
        $relativeSourcePath = Get-RelativePath -BasePath $repoRoot -TargetPath $SourcePath
        $results.Add((New-CheckResult $Label "WARN" "Live static asset is not reachable: $($_.Exception.Message); source asset is present at $relativeSourcePath"))
        return
      }
    }
    $sourceDetail = if ($SourcePath) { "; source asset is missing or empty: $SourcePath" } else { "" }
    $results.Add((New-CheckResult $Label "WARN" "Static asset is not reachable: $($_.Exception.Message)$sourceDetail"))
  }
}

function Invoke-GatewayRouteCheck {
  param(
    [string]$BaseUrl,
    [string]$Path,
    [string]$Name,
    [int]$ExpectedStatus = 200,
    [string]$ExpectedContent = "",
    [hashtable]$Headers = @{}
  )

  try {
    $baseUri = [System.Uri]::new($BaseUrl)
    $targetUri = [System.Uri]::new($baseUri, $Path.TrimStart("/"))
  }
  catch {
    return [PSCustomObject]@{
      Ok     = $false
      Detail = "$Name skipped; invalid base URL: $BaseUrl"
    }
  }

  $statusCode = 0
  $content = ""
  try {
    $requestArgs = @{
      Uri             = $targetUri.AbsoluteUri
      UseBasicParsing = $true
      TimeoutSec      = 5
    }
    if ($Headers -and $Headers.Count -gt 0) {
      $requestArgs.Headers = $Headers
    }

    $response = Invoke-WebRequest @requestArgs
    $statusCode = [int]$response.StatusCode
    $content = [string]$response.Content
  }
  catch {
    if ($_.Exception.Response) {
      $statusCode = [int]$_.Exception.Response.StatusCode
      try {
        $stream = $_.Exception.Response.GetResponseStream()
        if ($stream) {
          $reader = [System.IO.StreamReader]::new($stream)
          try {
            $content = $reader.ReadToEnd()
          }
          finally {
            $reader.Dispose()
          }
        }
      }
      catch {
        $content = ""
      }
    }
    else {
      return [PSCustomObject]@{
        Ok     = $false
        Detail = "$Name is not reachable at $($targetUri.AbsoluteUri): $($_.Exception.Message)"
      }
    }
  }

  if ($statusCode -ne $ExpectedStatus) {
    $snippet = Get-TextSnippet -Text $content
    $staleGatewayHint = ""
    if (($statusCode -eq 401) -and ($ExpectedStatus -eq 200) -and ($Path -in @("/api/rpa-openapi/health", "/api/rpa-ai-service/health"))) {
      $staleGatewayHint = " Current source bypasses gateway auth for this health route; rebuild openresty-nginx with corepack pnpm run repair:release-host:apply if the live gateway still returns 401."
    }
    if ($snippet) {
      return [PSCustomObject]@{
        Ok     = $false
        Detail = "$Name expected HTTP $ExpectedStatus but got HTTP $statusCode at $($targetUri.AbsoluteUri): $snippet$staleGatewayHint"
      }
    }
    return [PSCustomObject]@{
      Ok     = $false
      Detail = "$Name expected HTTP $ExpectedStatus but got HTTP $statusCode at $($targetUri.AbsoluteUri).$staleGatewayHint"
    }
  }

  if ($ExpectedContent -and (-not $content.Contains($ExpectedContent))) {
    $snippet = Get-TextSnippet -Text $content
    if (-not $snippet) {
      $snippet = "empty response"
    }
    return [PSCustomObject]@{
      Ok     = $false
      Detail = "$Name returned HTTP $ExpectedStatus but response did not contain '$ExpectedContent': $snippet"
    }
  }

  return [PSCustomObject]@{
    Ok     = $true
    Detail = "$Name HTTP $ExpectedStatus"
  }
}

function Test-BackendGatewayServiceRoutes {
  param([string]$BaseUrl)

  if (-not $BaseUrl) {
    $results.Add((New-CheckResult "backend service route smoke" "WARN" "Skipped; base URL is missing."))
    return
  }

  $routeChecks = @(
    @{
      Name            = "auth login-status"
      Path            = "/api/rpa-auth/login-status"
      ExpectedStatus  = 200
      ExpectedContent = '"code"'
      Headers         = @{}
    },
    @{
      Name            = "robot user info"
      Path            = "/api/robot/user/info"
      ExpectedStatus  = 200
      ExpectedContent = '"code"'
      Headers         = @{}
    },
    @{
      Name            = "casdoor health"
      Path            = "/api/casdoor/api/health"
      ExpectedStatus  = 200
      ExpectedContent = "ok"
      Headers         = @{}
    },
    @{
      Name            = "openapi health"
      Path            = "/api/rpa-openapi/health"
      ExpectedStatus  = 200
      ExpectedContent = '"status"'
      Headers         = @{}
    },
    @{
      Name            = "ai-service health"
      Path            = "/api/rpa-ai-service/health"
      ExpectedStatus  = 200
      ExpectedContent = '"status"'
      Headers         = @{}
    },
    @{
      Name            = "openapi auth guard"
      Path            = "/api/rpa-openapi/workflows"
      ExpectedStatus  = 401
      ExpectedContent = "Missing SESSION"
      Headers         = @{}
    },
    @{
      Name            = "ai-service auth guard"
      Path            = "/api/rpa-ai-service/v1/models"
      ExpectedStatus  = 401
      ExpectedContent = "Missing SESSION"
      Headers         = @{}
    }
  )

  $problems = New-Object System.Collections.Generic.List[string]
  foreach ($routeCheck in $routeChecks) {
    $result = Invoke-GatewayRouteCheck `
      -BaseUrl $BaseUrl `
      -Path $routeCheck.Path `
      -Name $routeCheck.Name `
      -ExpectedStatus $routeCheck.ExpectedStatus `
      -ExpectedContent $routeCheck.ExpectedContent `
      -Headers $routeCheck.Headers

    if (-not $result.Ok) {
      $problems.Add($result.Detail) | Out-Null
    }
  }

  if ($problems.Count -gt 0) {
    $results.Add((New-CheckResult "backend service route smoke" "WARN" ($problems -join "; ")))
  }
  else {
    $results.Add((New-CheckResult "backend service route smoke" "PASS" "$($routeChecks.Count) gateway-backed service routes passed"))
  }
}

function Normalize-PythonPackageName {
  param([string]$Name)
  return ($Name.Trim().ToLowerInvariant() -replace '[-_.]+', '_')
}

function Get-RequirementPackagePins {
  param([string]$ServiceRoot)

  $pins = New-Object System.Collections.Generic.List[object]
  $serviceName = Split-Path -Leaf $ServiceRoot
  $requirementsPath = Join-Path $repoRoot "build\python-backend-requirements\$serviceName.requirements.txt"
  if (-not (Test-Path -LiteralPath $requirementsPath -PathType Leaf)) {
    return $pins
  }

  foreach ($line in Get-Content -LiteralPath $requirementsPath) {
    $trimmed = $line.Trim()
    if ($trimmed -match "^([A-Za-z0-9_.-]+)==([^\s;\\]+)") {
      $pins.Add([PSCustomObject]@{
          Display = "$($Matches[1])==$($Matches[2])"
          Pin = "$(Normalize-PythonPackageName $Matches[1])==$($Matches[2])"
        }) | Out-Null
    }
  }

  return $pins
}

function Get-InstalledPythonPackagePins {
  param([string]$PythonPath)

  $pins = [System.Collections.Generic.HashSet[string]]::new()
  $scriptsDir = Split-Path -Parent $PythonPath
  $venvRoot = Split-Path -Parent $scriptsDir
  $sitePackages = Join-Path $venvRoot "Lib\site-packages"
  if (-not (Test-Path -LiteralPath $sitePackages -PathType Container)) {
    return $pins
  }

  Get-ChildItem -LiteralPath $sitePackages -Directory -Filter "*.dist-info" -ErrorAction SilentlyContinue | ForEach-Object {
    if ($_.Name -match "^(.+)-([0-9][^-]*)\.dist-info$") {
      [void]$pins.Add("$(Normalize-PythonPackageName $Matches[1])==$($Matches[2])")
    }
  }

  return $pins
}

function Test-PythonServiceImport {
  param(
    [string]$ServiceRoot,
    [string]$Label
  )

  $pythonPath = Join-Path $ServiceRoot ".venv\Scripts\python.exe"
  if (-not (Test-Path -LiteralPath $pythonPath -PathType Leaf)) {
    $results.Add((New-CheckResult $Label "WARN" "Skipped; service virtualenv is missing: $pythonPath"))
    return
  }

  $requiredPackagePins = @(Get-RequirementPackagePins -ServiceRoot $ServiceRoot)
  if ($requiredPackagePins.Count -gt 0) {
    $installedPackagePins = @(Get-InstalledPythonPackagePins -PythonPath $pythonPath)
    $missingLockedPackages = @(
      $requiredPackagePins |
        Where-Object { $installedPackagePins -notcontains $_.Pin } |
        ForEach-Object { $_.Display }
    )
    if ($missingLockedPackages.Count -gt 0) {
      $serviceRelativePath = Get-RelativePath -BasePath $repoRoot -TargetPath $ServiceRoot
      $preview = @($missingLockedPackages | Select-Object -First 8) -join ", "
      if ($missingLockedPackages.Count -gt 8) {
        $preview = "$preview, ..."
      }
      $results.Add((New-CheckResult $Label "WARN" "Missing or mismatched Python locked packages: $($missingLockedPackages.Count) ($preview); run corepack pnpm run setup:python-backends or uv sync --locked in $serviceRelativePath."))
      return
    }
  }

  $compileResult = Invoke-Captured -FilePath $pythonPath -Arguments @("-m", "compileall", "-q", "app", "tests", "run.py") -WorkingDirectory $ServiceRoot
  if ($compileResult.ExitCode -ne 0) {
    $detail = Get-FirstLine $compileResult.Output
    if (-not $detail) {
      $detail = "compileall failed with exit code $($compileResult.ExitCode)"
    }
    $results.Add((New-CheckResult "$Label compile" "FAIL" $detail))
    return
  }

  $importScript = "import os; defaults={'DATABASE_URL':'mysql+aiomysql://{username}:{password}@localhost:3306/shoprpa','DATABASE_USERNAME':'shoprpa','DATABASE_PASSWORD':'shoprpa','REDIS_URL':'redis://localhost:6379/0','AICHAT_BASE_URL':'http://localhost','AICHAT_API_KEY':'doctor','CUA_BASE_URL':'http://localhost','CUA_API_KEY':'doctor','XFYUN_APP_ID':'doctor','XFYUN_API_SECRET':'doctor','XFYUN_API_KEY':'doctor','JFBYM_API_TOKEN':'doctor','INTERNAL_ADMIN_API_KEY':'doctor','REGISTER_BEARER_TOKEN':'doctor'}; [os.environ.setdefault(k, v) for k, v in defaults.items()]; import app.main"
  $importResult = Invoke-Captured -FilePath $pythonPath -Arguments @("-c", $importScript) -WorkingDirectory $ServiceRoot
  if ($importResult.ExitCode -eq 0) {
    $results.Add((New-CheckResult $Label "PASS" "app.main imports successfully"))
  }
  else {
    $detail = Get-LastLine $importResult.Output
    if (-not $detail) {
      $detail = "app.main import failed with exit code $($importResult.ExitCode)"
    }
    elseif ($detail -match "ModuleNotFoundError:\s+No module named '([^']+)'") {
      $serviceRelativePath = Get-RelativePath -BasePath $repoRoot -TargetPath $ServiceRoot
      $detail = "Missing Python dependency '$($Matches[1])'; run corepack pnpm run setup:python-backends or uv sync --locked in $serviceRelativePath."
    }
    $results.Add((New-CheckResult $Label "WARN" $detail))
  }
}

function Test-PythonBackendSourceSyntax {
  param(
    [string]$ServiceRoot,
    [string]$Label,
    [string]$PythonExe
  )

  if (-not $PythonExe) {
    $results.Add((New-CheckResult $Label "WARN" "Skipped; no Python executable is available for source syntax checks."))
    return
  }

  $targets = @("app", "tests", "run.py") | Where-Object { Test-Path -LiteralPath (Join-Path $ServiceRoot $_) }
  if ($targets.Count -eq 0) {
    $results.Add((New-CheckResult $Label "WARN" "Skipped; no Python source targets were found."))
    return
  }

  $compileResult = Invoke-Captured -FilePath $PythonExe -Arguments (@("-m", "compileall", "-q") + $targets) -WorkingDirectory $ServiceRoot
  if ($compileResult.ExitCode -eq 0) {
    $results.Add((New-CheckResult $Label "PASS" "source files compile with $PythonExe"))
  }
  else {
    $detail = Get-FirstUsefulLine -Text $compileResult.Output
    if (-not $detail) {
      $detail = "compileall failed with exit code $($compileResult.ExitCode)"
    }
    $results.Add((New-CheckResult $Label "FAIL" $detail))
  }
}

function Test-SourceTextHygiene {
  if (-not (Test-CommandAvailable "rg")) {
    $results.Add((New-CheckResult "source text hygiene" "WARN" "Skipped; ripgrep is not available."))
    return
  }

  $commonGlobs = @(
    "--glob", "!build/**",
    "--glob", "!frontend/**/dist/**",
    "--glob", "!frontend/**/node_modules/**",
    "--glob", "!frontend/docs/**",
    "--glob", "!frontend/packages/**/scripts/**",
    "--glob", "!frontend/packages/cli/templates/**",
    "--glob", "!frontend/packages/shared/tokens/build-tokens.js",
    "--glob", "!node_modules/**",
    "--glob", "!*.lock",
    "--glob", "!**/*.md",
    "--glob", "!**/*.map"
  )
  $checkedPaths = @("frontend", "backend", "engine", "resources", "docker", "README.md", "BUILD_GUIDE.md")
  $productTodoGlobs = $commonGlobs + @(
    "--glob", "!backend/**/checkstyle.xml",
    "--glob", "!backend/robot-service/src/main/java/com/iflytek/rpa/task/service/CronExpression.java",
    "--glob", "!docker/volumes/nginx/lua/resty/http.lua"
  )
  $todoResult = Invoke-Captured -FilePath "rg" -Arguments (@("-n", "TODO|FIXME") + $checkedPaths + $productTodoGlobs) -WorkingDirectory $repoRoot
  if ($todoResult.ExitCode -eq 0) {
    $firstHit = Get-FirstUsefulLine -Text $todoResult.Output
    $results.Add((New-CheckResult "source text hygiene" "FAIL" "Product TODO/FIXME marker found: $firstHit"))
    return
  }
  if ($todoResult.ExitCode -gt 1) {
    $results.Add((New-CheckResult "source text hygiene" "WARN" (Get-FirstUsefulLine -Text $todoResult.Output)))
    return
  }

  $placeholderPattern = "Add your description here|debugger;|console\.log\(|测试|示例|中文|占位|lorem|Lorem|FireScope|AstronRPA|Astron RPA"
  $placeholderResult = Invoke-Captured -FilePath "rg" -Arguments (@("-n", $placeholderPattern) + $checkedPaths + $commonGlobs) -WorkingDirectory $repoRoot
  if ($placeholderResult.ExitCode -eq 0) {
    $firstHit = Get-FirstUsefulLine -Text $placeholderResult.Output
    $results.Add((New-CheckResult "source text hygiene" "FAIL" "Placeholder/debug/legacy text found: $firstHit"))
    return
  }
  if ($placeholderResult.ExitCode -gt 1) {
    $results.Add((New-CheckResult "source text hygiene" "WARN" (Get-FirstUsefulLine -Text $placeholderResult.Output)))
    return
  }

  $hanResult = Invoke-Captured -FilePath "rg" -Arguments (@("-n", "[\p{Han}]", "frontend", "backend", "engine", "resources", "README.md", "BUILD_GUIDE.md") + $commonGlobs) -WorkingDirectory $repoRoot
  if ($hanResult.ExitCode -eq 0) {
    $firstHit = Get-FirstUsefulLine -Text $hanResult.Output
    $results.Add((New-CheckResult "source text hygiene" "FAIL" "Unlocalized Han text found: $firstHit"))
    return
  }
  if ($hanResult.ExitCode -gt 1) {
    $results.Add((New-CheckResult "source text hygiene" "WARN" (Get-FirstUsefulLine -Text $hanResult.Output)))
    return
  }

  $results.Add((New-CheckResult "source text hygiene" "PASS" "no product TODO/FIXME, placeholder, debug, legacy brand, or Han-script text found in checked product paths"))
}

function Test-GeneratedSourceArtifacts {
  $artifactGlobs = @(
    "engine\components\*\test.xlsx",
    "engine\components\*\*.tmp",
    "engine\components\*\*.bak"
  )
  $hits = New-Object System.Collections.Generic.List[string]

  foreach ($artifactGlob in $artifactGlobs) {
    $found = @(Get-ChildItem -Path (Join-Path $repoRoot $artifactGlob) -File -ErrorAction SilentlyContinue)
    foreach ($item in $found) {
      $hits.Add((Get-RelativePath -BasePath $repoRoot -TargetPath $item.FullName)) | Out-Null
    }
  }

  if ($hits.Count -gt 0) {
    $preview = @($hits | Select-Object -First 5) -join ", "
    if ($hits.Count -gt 5) {
      $preview = "$preview, ..."
    }
    $results.Add((New-CheckResult "generated source artifacts" "FAIL" "Generated files found in source paths: $preview"))
  }
  else {
    $results.Add((New-CheckResult "generated source artifacts" "PASS" "no generated workbook/temp artifacts found in source paths"))
  }
}

function Test-PythonLockContainsDependency {
  param(
    [string]$ServiceRoot,
    [string]$Label,
    [string[]]$Dependencies
  )

  $pyprojectPath = Join-Path $ServiceRoot "pyproject.toml"
  $lockPath = Join-Path $ServiceRoot "uv.lock"
  if ((-not (Test-Path -LiteralPath $pyprojectPath -PathType Leaf)) -or (-not (Test-Path -LiteralPath $lockPath -PathType Leaf))) {
    $results.Add((New-CheckResult $Label "WARN" "Skipped; pyproject.toml or uv.lock is missing."))
    return
  }

  $pyproject = Get-Content -LiteralPath $pyprojectPath -Raw
  $lock = Get-Content -LiteralPath $lockPath -Raw
  $projectNameMatch = [regex]::Match($pyproject, '(?m)^\s*name\s*=\s*"([^"]+)"')
  if (-not $projectNameMatch.Success) {
    $results.Add((New-CheckResult $Label "WARN" "Could not read the project name from pyproject.toml."))
    return
  }

  $projectName = $projectNameMatch.Groups[1].Value
  $projectBlockMatch = [regex]::Match(
    $lock,
    "(?s)\[\[package\]\]\s*name\s*=\s*`"$([regex]::Escape($projectName))`".*?(?=\[\[package\]\]|$)"
  )
  if (-not $projectBlockMatch.Success) {
    $results.Add((New-CheckResult $Label "WARN" "uv.lock does not contain the project package: $projectName"))
    return
  }

  $projectBlock = $projectBlockMatch.Value
  $directDependencyBlock = ($projectBlock -split '\[package\.dev-dependencies\]', 2)[0]
  $missingFromLock = @()

  foreach ($dependency in $Dependencies) {
    $escapedDependency = [regex]::Escape($dependency)
    if ($pyproject -match "`"$escapedDependency(\[|[<>=,`"])" -and $directDependencyBlock -notmatch "name\s*=\s*`"$escapedDependency`"") {
      $missingFromLock += $dependency
    }
  }

  if ($missingFromLock.Count -eq 0) {
    $results.Add((New-CheckResult $Label "PASS" "uv.lock contains declared runtime dependencies"))
  }
  else {
    $results.Add((New-CheckResult $Label "WARN" ("uv.lock is stale or incomplete; missing: " + ($missingFromLock -join ", "))))
  }
}

function Test-PythonDatabaseDriverConsistency {
  param(
    [string]$ServiceRoot,
    [string]$Label
  )

  $pyprojectPath = Join-Path $ServiceRoot "pyproject.toml"
  if (-not (Test-Path -LiteralPath $pyprojectPath -PathType Leaf)) {
    $results.Add((New-CheckResult $Label "WARN" "Skipped; pyproject.toml is missing."))
    return
  }

  $pyproject = Get-Content -LiteralPath $pyprojectPath -Raw
  $declaredDependencies = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
  foreach ($match in [regex]::Matches($pyproject, '"([A-Za-z0-9_.-]+)(?:\[.*?\])?(?:[<>=~!,].*)?"')) {
    [void]$declaredDependencies.Add((Normalize-PythonPackageName $match.Groups[1].Value))
  }

  $driverDependencyByName = @{
    aiomysql = "aiomysql"
    asyncmy = "asyncmy"
    pymysql = "pymysql"
    mysqldb = "mysqlclient"
  }
  $driverHits = New-Object System.Collections.Generic.List[object]
  $sourceFiles = @(
    Get-ChildItem -LiteralPath $ServiceRoot -File -Recurse -Include "*.py", "*.toml", "*.yml", "*.yaml" -ErrorAction SilentlyContinue |
      Where-Object { $_.FullName -notmatch "\\(\.venv|__pycache__)\\|\\uv\.lock$" }
  )

  foreach ($file in $sourceFiles) {
    $relativePath = Get-RelativePath -BasePath $repoRoot -TargetPath $file.FullName
    $text = Get-Content -LiteralPath $file.FullName -Raw
    if ($null -eq $text) {
      $text = ""
    }
    foreach ($match in [regex]::Matches($text, "mysql\+([A-Za-z0-9_]+)")) {
      $driverName = $match.Groups[1].Value.ToLowerInvariant()
      $requiredDependency = [string]$driverDependencyByName[$driverName]
      if (-not $requiredDependency) {
        $requiredDependency = $driverName
      }
      $driverHits.Add([PSCustomObject]@{
          file = $relativePath
          driver = $driverName
          dependency = (Normalize-PythonPackageName $requiredDependency)
        }) | Out-Null
    }
  }

  if ($driverHits.Count -eq 0) {
    $results.Add((New-CheckResult $Label "WARN" "No mysql+driver URLs were found in service source or tests."))
    return
  }

  $missing = @(
    $driverHits |
      Where-Object { -not $declaredDependencies.Contains($_.dependency) } |
      ForEach-Object { "$($_.file) uses mysql+$($_.driver), but pyproject.toml does not declare $($_.dependency)" } |
      Sort-Object -Unique
  )
  if ($missing.Count -gt 0) {
    $results.Add((New-CheckResult $Label "FAIL" (($missing | Select-Object -First 5) -join "; ")))
    return
  }

  $drivers = @($driverHits | ForEach-Object { $_.driver } | Sort-Object -Unique)
  $results.Add((New-CheckResult $Label "PASS" "mysql drivers match pyproject dependencies: $($drivers -join ', ')"))
}

function Test-PythonServiceDockerfileLockInstall {
  param(
    [string]$DockerfilePath,
    [string]$Label
  )

  if (-not (Test-Path -LiteralPath $DockerfilePath -PathType Leaf)) {
    $results.Add((New-CheckResult $Label "FAIL" "Missing Dockerfile: $DockerfilePath"))
    return
  }

  $source = Get-Content -LiteralPath $DockerfilePath -Raw
  if ($source -match "pip\s+install\s+--no-cache-dir\s+-e\s+\.") {
    $results.Add((New-CheckResult $Label "FAIL" "Dockerfile installs the project with pip install -e ., which ignores uv.lock."))
    return
  }
  if ($source -notmatch "ARG\s+UV_VERSION\s*=\s*[0-9]+\.[0-9]+\.[0-9]+") {
    $results.Add((New-CheckResult $Label "FAIL" "Dockerfile does not pin the uv installer version with ARG UV_VERSION."))
    return
  }
  if ($source -notmatch 'pip\s+install\s+--upgrade\s+pip\s+"uv==\$\{UV_VERSION\}"') {
    $results.Add((New-CheckResult $Label "FAIL" "Dockerfile installs uv without using the pinned UV_VERSION build argument."))
    return
  }
  if ($source -notmatch "uv\s+sync\s+--locked\s+--no-dev") {
    $results.Add((New-CheckResult $Label "FAIL" "Dockerfile does not install runtime dependencies with uv sync --locked --no-dev."))
    return
  }
  if ($source -notmatch "uv\.lock") {
    $results.Add((New-CheckResult $Label "FAIL" "Dockerfile does not copy uv.lock before dependency installation."))
    return
  }
  if ($source -notmatch "PATH=.*/app/\.venv/bin") {
    $results.Add((New-CheckResult $Label "FAIL" "Dockerfile does not add /app/.venv/bin to PATH for the locked runtime environment."))
    return
  }

  $results.Add((New-CheckResult $Label "PASS" "Dockerfile installs locked runtime dependencies with uv sync."))
}

$repoRoot = Resolve-RepoRoot
$doctorPython = Resolve-AvailablePythonExe
$portableRoot = Join-Path $repoRoot "frontend\packages\electron-app\dist\win-portable"
$frontendRoot = Join-Path $repoRoot "frontend"
$dockerRoot = Join-Path $repoRoot "docker"
$javaBackendTestsReportPath = Join-Path $repoRoot "build\java-backend-tests.json"
$results = New-Object System.Collections.Generic.List[object]

$requiredPortableFiles = @(
  "ShopRPA.cmd",
  "README-portable.txt",
  "resources\app.asar",
  "resources\conf.yaml",
  "resources\renderer\boot.html",
  "resources\renderer\index.html",
  "resources\7zr.exe",
  "resources\python_core.7z",
  "resources\python_core.7z.sha256.txt",
  "runtime\node_modules\electron\dist\electron.exe"
)

if (Test-Path -LiteralPath $portableRoot -PathType Container) {
  $missing = Assert-PortableFiles -PortableRoot $portableRoot -RelativePaths $requiredPortableFiles
  if ($missing.Count -eq 0) {
    $results.Add((New-CheckResult "portable files" "PASS" $portableRoot))
  }
  else {
    $results.Add((New-CheckResult "portable files" "FAIL" ("Missing: " + ($missing -join ", "))))
  }
}
else {
  $results.Add((New-CheckResult "portable files" "FAIL" "Portable package folder is missing: $portableRoot"))
}

$hashPath = Join-Path $portableRoot "resources\python_core.7z.sha256.txt"
$archivePath = Join-Path $portableRoot "resources\python_core.7z"
if ((Test-Path -LiteralPath $hashPath -PathType Leaf) -and (Test-Path -LiteralPath $archivePath -PathType Leaf)) {
  $expectedHash = (Get-Content -LiteralPath $hashPath -Raw).Trim().ToUpperInvariant()
  $actualHash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToUpperInvariant()
  if ($expectedHash -eq $actualHash) {
    $results.Add((New-CheckResult "python archive hash" "PASS" $actualHash))
  }
  else {
    $results.Add((New-CheckResult "python archive hash" "FAIL" "Expected $expectedHash, got $actualHash"))
  }
}
else {
  $results.Add((New-CheckResult "python archive hash" "FAIL" "Archive or hash file is missing."))
}

Test-PortableSmokeResult -SmokeResultPath (Join-Path $portableRoot "smoke-verification.json")
Test-BrowserBridgeInjectSync -RepoRoot $repoRoot -PythonCoreRoot (Join-Path $repoRoot "build\python_core")
Test-EngineSourceOverlaySync -RepoRoot $repoRoot -PythonCoreRoot (Join-Path $repoRoot "build\python_core")
Test-PortablePythonArchiveSource -RepoRoot $repoRoot -PortableRoot $portableRoot
Test-PortableInstallerPackage -RepoRoot $repoRoot

$requiredEngineRuntimeSuites = @(
  "baseline",
  "atom-metadata",
  "dataprocess",
  "datatable",
  "encrypt",
  "enterprise",
  "email",
  "script",
  "system-clipboard",
  "system-file",
  "system-folder",
  "system-compress",
  "system-process",
  "system-screen"
)
$engineRuntimeReportPath = Join-Path $repoRoot "build\engine-runtime-tests.json"
if (Test-Path -LiteralPath $engineRuntimeReportPath -PathType Leaf) {
  try {
    $engineRuntimeReport = Get-Content -LiteralPath $engineRuntimeReportPath -Raw | ConvertFrom-Json
    if ($engineRuntimeReport.ok -eq $true) {
      $freshnessProblem = Get-ReportFreshnessProblem -Report $engineRuntimeReport -PropertyName "generatedAt"
      if ($freshnessProblem) {
        $results.Add((New-CheckResult "engine runtime tests" "WARN" "$freshnessProblem; rerun corepack pnpm run test:engine-runtime."))
      }
      else {
        $suiteNames = @($engineRuntimeReport.suites | ForEach-Object { [string]$_.name })
        $missingSuites = @($requiredEngineRuntimeSuites | Where-Object { $suiteNames -notcontains $_ })
        $failedSuites = @($engineRuntimeReport.suites | Where-Object { [string]$_.status -ne "PASS" } | ForEach-Object { [string]$_.name })
        $timedOutSuites = @($engineRuntimeReport.suites | Where-Object { $_.timedOut -eq $true } | ForEach-Object { [string]$_.name })
        $badExitSuites = @($engineRuntimeReport.suites | Where-Object { [int]$_.exitCode -ne 0 } | ForEach-Object { [string]$_.name })
        if ($missingSuites.Count -gt 0) {
          $results.Add((New-CheckResult "engine runtime tests" "FAIL" "required suites are missing: $($missingSuites -join ', ')"))
        }
        elseif ($failedSuites.Count -gt 0) {
          $results.Add((New-CheckResult "engine runtime tests" "FAIL" "suite failures remain: $($failedSuites -join ', ')"))
        }
        elseif ($timedOutSuites.Count -gt 0) {
          $results.Add((New-CheckResult "engine runtime tests" "FAIL" "suite timeouts remain: $($timedOutSuites -join ', ')"))
        }
        elseif ($badExitSuites.Count -gt 0) {
          $results.Add((New-CheckResult "engine runtime tests" "FAIL" "suite exit codes are not clean: $($badExitSuites -join ', ')"))
        }
        elseif ([int]$engineRuntimeReport.totalTests -lt 416) {
          $results.Add((New-CheckResult "engine runtime tests" "FAIL" "coverage regressed: $($engineRuntimeReport.totalTests) tests; expected at least 416"))
        }
        else {
          $results.Add((New-CheckResult "engine runtime tests" "PASS" "$($engineRuntimeReport.totalTests) tests, $($engineRuntimeReport.totalSkipped) skipped; suites=$($suiteNames -join ', ')"))
        }
      }
    }
    else {
      $results.Add((New-CheckResult "engine runtime tests" "FAIL" "engine runtime report has ok=false"))
    }
  }
  catch {
    $results.Add((New-CheckResult "engine runtime tests" "WARN" "Report is unreadable; run corepack pnpm run test:engine-runtime."))
  }
}
else {
  $results.Add((New-CheckResult "engine runtime tests" "WARN" "Missing; run corepack pnpm run test:engine-runtime."))
}

$browserExtensionStaticScript = Join-Path $repoRoot "scripts\verify-browser-extension-static.cjs"
if ((Test-Path -LiteralPath $browserExtensionStaticScript -PathType Leaf) -and (Test-CommandAvailable "node")) {
  $browserExtensionStaticResult = Invoke-Captured -FilePath "node" -Arguments @($browserExtensionStaticScript) -WorkingDirectory $repoRoot
  if ($browserExtensionStaticResult.ExitCode -eq 0) {
    $detail = @(
      $browserExtensionStaticResult.Output -split "`r?`n" |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -match "^checkedFiles=\d+" } |
        Select-Object -Last 1
    )
    if ($detail.Count -gt 0) {
      $detail = $detail[0]
    }
    else {
      $detail = ""
    }
    if (-not $detail) {
      $detail = Get-FirstUsefulLine -Text $browserExtensionStaticResult.Output
    }
    $results.Add((New-CheckResult "browser extension static package" "PASS" $detail))
  }
  else {
    $detail = Get-FirstUsefulLine -Text $browserExtensionStaticResult.Output
    if (-not $detail) {
      $detail = "static verifier exited with code $($browserExtensionStaticResult.ExitCode)"
    }
    $results.Add((New-CheckResult "browser extension static package" "FAIL" $detail))
  }
}
else {
  $results.Add((New-CheckResult "browser extension static package" "WARN" "Cannot run static package verifier because node or scripts/verify-browser-extension-static.cjs is unavailable."))
}

$frontendTestsReportPath = Join-Path $repoRoot "build\frontend-tests\frontend-tests-report.json"
$frontendTypecheckReportPath = Join-Path $repoRoot "build\frontend-typecheck\frontend-typecheck-report.json"
if (Test-Path -LiteralPath $frontendTypecheckReportPath -PathType Leaf) {
  try {
    $frontendTypecheckReport = Get-Content -LiteralPath $frontendTypecheckReportPath -Raw | ConvertFrom-Json
    if ($frontendTypecheckReport.ok -eq $true) {
      $freshnessProblem = Get-ReportFreshnessProblem -Report $frontendTypecheckReport -PropertyName "generatedAt"
      if ($freshnessProblem) {
        $results.Add((New-CheckResult "frontend typecheck" "WARN" "$freshnessProblem; rerun corepack pnpm run typecheck:frontend."))
      }
      else {
        $results.Add((New-CheckResult "frontend typecheck" "PASS" $frontendTypecheckReport.summary))
      }
    }
    else {
      $detail = [string]$frontendTypecheckReport.summary
      if (-not $detail) {
        $detail = "report has ok=false"
      }
      $results.Add((New-CheckResult "frontend typecheck" "WARN" $detail))
    }
  }
  catch {
    $results.Add((New-CheckResult "frontend typecheck" "WARN" "Report is unreadable; run corepack pnpm run typecheck:frontend."))
  }
}
else {
  $results.Add((New-CheckResult "frontend typecheck" "WARN" "Missing; run corepack pnpm run typecheck:frontend."))
}

if (Test-Path -LiteralPath $frontendTestsReportPath -PathType Leaf) {
  try {
    $frontendTestsReport = Get-Content -LiteralPath $frontendTestsReportPath -Raw | ConvertFrom-Json
    if ($frontendTestsReport.ok -eq $true) {
      $freshnessProblem = Get-ReportFreshnessProblem -Report $frontendTestsReport -PropertyName "generatedAt"
      if ($freshnessProblem) {
        $results.Add((New-CheckResult "frontend unit tests" "WARN" "$freshnessProblem; rerun corepack pnpm run test:frontend."))
      }
      else {
        $results.Add((New-CheckResult "frontend unit tests" "PASS" $frontendTestsReport.summary))
      }
    }
    else {
      $detail = [string]$frontendTestsReport.summary
      if (-not $detail) {
        $detail = "report has ok=false"
      }
      $results.Add((New-CheckResult "frontend unit tests" "WARN" $detail))
    }
  }
  catch {
    $results.Add((New-CheckResult "frontend unit tests" "WARN" "Report is unreadable; run corepack pnpm run test:frontend."))
  }
}
else {
  $results.Add((New-CheckResult "frontend unit tests" "WARN" "Missing; run corepack pnpm run test:frontend."))
}

$browserExtensionContentTestReportPath = Join-Path $repoRoot "build\browser-extension-content-tests\browser-extension-content-tests-report.json"
if (Test-Path -LiteralPath $browserExtensionContentTestReportPath -PathType Leaf) {
  try {
    $browserExtensionContentTestReport = Get-Content -LiteralPath $browserExtensionContentTestReportPath -Raw | ConvertFrom-Json
    if ($browserExtensionContentTestReport.ok -eq $true) {
      $freshnessProblem = Get-ReportFreshnessProblem -Report $browserExtensionContentTestReport -PropertyName "generatedAt"
      if ($freshnessProblem) {
        $results.Add((New-CheckResult "browser extension content tests" "WARN" "$freshnessProblem; rerun corepack pnpm run test:browser-extension-content."))
      }
      else {
        $results.Add((New-CheckResult "browser extension content tests" "PASS" $browserExtensionContentTestReport.summary))
      }
    }
    else {
      $detail = [string]$browserExtensionContentTestReport.summary
      if (-not $detail) {
        $detail = "report has ok=false"
      }
      if ($browserExtensionContentTestReport.failureClass) {
        $detail = "$detail; failureClass=$($browserExtensionContentTestReport.failureClass)"
      }
      $results.Add((New-CheckResult "browser extension content tests" "WARN" $detail))
    }
  }
  catch {
    $results.Add((New-CheckResult "browser extension content tests" "WARN" "Report is unreadable; run corepack pnpm run test:browser-extension-content."))
  }
}
else {
  $results.Add((New-CheckResult "browser extension content tests" "WARN" "Missing; run corepack pnpm run test:browser-extension-content."))
}

$browserExtensionContentJsdomReportPath = Join-Path $repoRoot "build\browser-extension-content-jsdom-tests\browser-extension-content-jsdom-tests-report.json"
if (Test-Path -LiteralPath $browserExtensionContentJsdomReportPath -PathType Leaf) {
  try {
    $browserExtensionContentJsdomReport = Get-Content -LiteralPath $browserExtensionContentJsdomReportPath -Raw | ConvertFrom-Json
    if ($browserExtensionContentJsdomReport.ok -eq $true) {
      $freshnessProblem = Get-ReportFreshnessProblem -Report $browserExtensionContentJsdomReport -PropertyName "generatedAt"
      if ($freshnessProblem) {
        $results.Add((New-CheckResult "browser extension content jsdom tests" "WARN" "$freshnessProblem; rerun corepack pnpm run test:browser-extension-content:jsdom."))
      }
      else {
        $results.Add((New-CheckResult "browser extension content jsdom tests" "PASS" $browserExtensionContentJsdomReport.summary))
      }
    }
    else {
      $detail = [string]$browserExtensionContentJsdomReport.summary
      if (-not $detail) {
        $detail = "report has ok=false"
      }
      $results.Add((New-CheckResult "browser extension content jsdom tests" "WARN" $detail))
    }
  }
  catch {
    $results.Add((New-CheckResult "browser extension content jsdom tests" "WARN" "Report is unreadable; run corepack pnpm run test:browser-extension-content:jsdom."))
  }
}
else {
  $results.Add((New-CheckResult "browser extension content jsdom tests" "WARN" "Missing; run corepack pnpm run test:browser-extension-content:jsdom."))
}

$browserExtensionSmokeReportPath = Join-Path $repoRoot "build\browser-extension-smoke\browser-extension-smoke-report.json"
if (Test-Path -LiteralPath $browserExtensionSmokeReportPath -PathType Leaf) {
  try {
    $browserExtensionSmokeReport = Get-Content -LiteralPath $browserExtensionSmokeReportPath -Raw | ConvertFrom-Json
    if ($browserExtensionSmokeReport.ok -eq $true) {
      $freshnessProblem = Get-ReportFreshnessProblem -Report $browserExtensionSmokeReport -PropertyName "generatedAt"
      if ($freshnessProblem) {
        $results.Add((New-CheckResult "browser extension smoke" "WARN" "$freshnessProblem; rerun corepack pnpm run smoke:browser-extension."))
      }
      else {
        $scenarioCount = @($browserExtensionSmokeReport.scenarios | Where-Object { $_.status -eq "PASS" }).Count
        $results.Add((New-CheckResult "browser extension smoke" "PASS" "$scenarioCount scenarios passed with $($browserExtensionSmokeReport.browserExecutable)"))
      }
    }
    else {
      $failure = @($browserExtensionSmokeReport.failures | Where-Object { $_ } | Select-Object -First 1)
      if (-not $failure) {
        $failure = "report has ok=false"
      }
      else {
        $failure = Get-FirstUsefulLine -Text ([string]$failure)
      }
      $results.Add((New-CheckResult "browser extension smoke" "WARN" $failure))
    }
  }
  catch {
    $results.Add((New-CheckResult "browser extension smoke" "WARN" "Report is unreadable; run corepack pnpm run smoke:browser-extension."))
  }
}
else {
  $results.Add((New-CheckResult "browser extension smoke" "WARN" "Missing; run corepack pnpm run smoke:browser-extension."))
}

$readmePath = Join-Path $portableRoot "README-portable.txt"
if (Test-Path -LiteralPath $readmePath -PathType Leaf) {
  $readme = Get-Content -LiteralPath $readmePath -Raw
  $requiredReadmePhrases = @("How to run", "What is included", "Diagnostics", "Package boundary")
  $missingPhrases = $requiredReadmePhrases | Where-Object { $readme -notmatch [regex]::Escape($_) }
  if ($missingPhrases.Count -eq 0) {
    $results.Add((New-CheckResult "portable README" "PASS" "handoff instructions present"))
  }
  else {
    $results.Add((New-CheckResult "portable README" "WARN" ("Missing sections: " + ($missingPhrases -join ", "))))
  }
}
else {
  $results.Add((New-CheckResult "portable README" "FAIL" "README-portable.txt is missing."))
}

$repairScriptPath = Join-Path $repoRoot "scripts\repair-release-host.ps1"
$packageJsonPath = Join-Path $repoRoot "package.json"
if ((Test-Path -LiteralPath $repairScriptPath -PathType Leaf) -and (Test-Path -LiteralPath $packageJsonPath -PathType Leaf)) {
  $packageJsonText = Get-Content -LiteralPath $packageJsonPath -Raw
  $requiredRepairScripts = @("repair:release-host", "repair:release-host:strict", "repair:release-host:apply")
  $missingRepairScripts = @($requiredRepairScripts | Where-Object { $packageJsonText -notmatch '"' + [regex]::Escape($_) + '"' })
  if ($missingRepairScripts.Count -eq 0) {
    $results.Add((New-CheckResult "release host repair script" "PASS" "diagnostic, strict, and apply handoff commands are available"))
  }
  else {
    $results.Add((New-CheckResult "release host repair script" "WARN" ("script exists but package.json is missing: " + ($missingRepairScripts -join ", "))))
  }
}
else {
  $results.Add((New-CheckResult "release host repair script" "WARN" "Missing scripts/repair-release-host.ps1 or package.json"))
}

$setupPythonScriptPath = Join-Path $repoRoot "scripts\setup-python-backends.ps1"
$exportPythonReqsScriptPath = Join-Path $repoRoot "scripts\export-python-backend-requirements.ps1"
$downloadMissingPythonWheelsScriptPath = Join-Path $repoRoot "scripts\download-missing-python-backend-wheels.ps1"
$downloadMissingPythonWheelsWrapperPath = Join-Path $repoRoot "scripts\download-missing-python-backend-wheels.cjs"
if ((Test-Path -LiteralPath $setupPythonScriptPath -PathType Leaf) -and
  (Test-Path -LiteralPath $exportPythonReqsScriptPath -PathType Leaf) -and
  (Test-Path -LiteralPath $downloadMissingPythonWheelsScriptPath -PathType Leaf) -and
  (Test-Path -LiteralPath $downloadMissingPythonWheelsWrapperPath -PathType Leaf) -and
  (Test-Path -LiteralPath $packageJsonPath -PathType Leaf)) {
  $packageJsonText = Get-Content -LiteralPath $packageJsonPath -Raw
  $requiredPythonScripts = @(
    "setup:python-backends",
    "setup:python-backends:check",
    "setup:python-backends:offline-check",
    "export:python-backend-reqs",
    "download:python-backend-wheelhouse-missing"
  )
  $missingPythonScripts = @($requiredPythonScripts | Where-Object { $packageJsonText -notmatch '"' + [regex]::Escape($_) + '"' })
  if ($missingPythonScripts.Count -eq 0) {
    if (Test-CommandAvailable "node") {
      $wrapperSyntax = Invoke-Captured -FilePath "node" -Arguments @("--check", $downloadMissingPythonWheelsWrapperPath) -WorkingDirectory $repoRoot
      if ($wrapperSyntax.ExitCode -eq 0) {
        $results.Add((New-CheckResult "python backend setup scripts" "PASS" "online, dry-run, offline dry-run, requirements export, and missing-wheel download commands are available"))
      }
      else {
        $results.Add((New-CheckResult "python backend setup scripts" "WARN" ("download wrapper syntax check failed: " + (Get-FirstUsefulLine -Text $wrapperSyntax.Output))))
      }
    }
    else {
      $results.Add((New-CheckResult "python backend setup scripts" "WARN" "Node is required to validate the missing-wheel download wrapper."))
    }
  }
  else {
    $results.Add((New-CheckResult "python backend setup scripts" "WARN" ("Missing package scripts: " + ($missingPythonScripts -join ", "))))
  }
}
else {
  $results.Add((New-CheckResult "python backend setup scripts" "WARN" "Missing setup/export/download script file, wrapper file, or package.json"))
}

if (Test-CommandAvailable "node") {
  $nodeResult = Invoke-Captured -FilePath "node" -Arguments @("-v") -WorkingDirectory $repoRoot
  $results.Add((New-CheckResult "node" ($(if ($nodeResult.ExitCode -eq 0) { "PASS" } else { "FAIL" })) (Get-FirstLine $nodeResult.Output)))
}
else {
  $results.Add((New-CheckResult "node" "FAIL" "node.exe is not on PATH."))
}

if (Test-CommandAvailable "corepack") {
  $corepackResult = Invoke-Captured -FilePath "corepack" -Arguments @("--version") -WorkingDirectory $repoRoot
  $results.Add((New-CheckResult "corepack" ($(if ($corepackResult.ExitCode -eq 0) { "PASS" } else { "FAIL" })) (Get-FirstLine $corepackResult.Output)))
}
else {
  $results.Add((New-CheckResult "corepack" "FAIL" "corepack is not on PATH."))
}

Test-SourceTextHygiene
Test-GeneratedSourceArtifacts

$javaBackendPoms = @(
  "backend\resource-service\pom.xml",
  "backend\robot-service\pom.xml",
  "backend\rpa-auth\pom.xml"
)
$missingPoms = @($javaBackendPoms | Where-Object { -not (Test-Path -LiteralPath (Join-Path $repoRoot $_) -PathType Leaf) })
if ($missingPoms.Count -eq 0) {
  $results.Add((New-CheckResult "java backend modules" "PASS" ($javaBackendPoms -join ", ")))
  $invalidPoms = New-Object System.Collections.Generic.List[string]
  foreach ($pom in $javaBackendPoms) {
    $pomPath = Join-Path $repoRoot $pom
    try {
      [xml](Get-Content -LiteralPath $pomPath -Raw) | Out-Null
    }
    catch {
      $invalidPoms.Add("${pom}: $($_.Exception.Message)") | Out-Null
    }
  }

  if ($invalidPoms.Count -eq 0) {
    $results.Add((New-CheckResult "java pom xml" "PASS" "all backend pom.xml files parse as XML"))
  }
  else {
    $results.Add((New-CheckResult "java pom xml" "FAIL" ($invalidPoms -join "; ")))
  }
}
else {
  $results.Add((New-CheckResult "java backend modules" "FAIL" ("Missing: " + ($missingPoms -join ", "))))
}

$pythonBackendFiles = @(
  "backend\ai-service\pyproject.toml",
  "backend\openapi-service\pyproject.toml"
)
$missingPythonBackends = @($pythonBackendFiles | Where-Object { -not (Test-Path -LiteralPath (Join-Path $repoRoot $_) -PathType Leaf) })
if ($missingPythonBackends.Count -eq 0) {
  $results.Add((New-CheckResult "python backend modules" "PASS" ($pythonBackendFiles -join ", ")))
}
else {
  $results.Add((New-CheckResult "python backend modules" "FAIL" ("Missing: " + ($missingPythonBackends -join ", "))))
}

Test-PythonBackendSourceSyntax -ServiceRoot (Join-Path $repoRoot "backend\ai-service") -Label "ai-service source syntax" -PythonExe $doctorPython
Test-PythonBackendSourceSyntax -ServiceRoot (Join-Path $repoRoot "backend\openapi-service") -Label "openapi-service source syntax" -PythonExe $doctorPython
Test-PythonServiceImport -ServiceRoot (Join-Path $repoRoot "backend\ai-service") -Label "ai-service python runtime"
Test-PythonServiceImport -ServiceRoot (Join-Path $repoRoot "backend\openapi-service") -Label "openapi-service python runtime"
Test-PythonLockContainsDependency -ServiceRoot (Join-Path $repoRoot "backend\ai-service") -Label "ai-service uv lock" -Dependencies @("httpx", "pytz", "uvicorn")
Test-PythonLockContainsDependency -ServiceRoot (Join-Path $repoRoot "backend\openapi-service") -Label "openapi-service uv lock" -Dependencies @("httpx", "uvicorn")
Test-PythonDatabaseDriverConsistency -ServiceRoot (Join-Path $repoRoot "backend\ai-service") -Label "ai-service database driver"
Test-PythonDatabaseDriverConsistency -ServiceRoot (Join-Path $repoRoot "backend\openapi-service") -Label "openapi-service database driver"
Test-PythonServiceDockerfileLockInstall -DockerfilePath (Join-Path $repoRoot "backend\ai-service\Dockerfile") -Label "ai-service Dockerfile lock install"
Test-PythonServiceDockerfileLockInstall -DockerfilePath (Join-Path $repoRoot "backend\openapi-service\Dockerfile") -Label "openapi-service Dockerfile lock install"

$pythonWheelhouseManifest = Join-Path $repoRoot "build\python-backend-requirements\python-backend-wheelhouse-manifest.md"
if (Test-Path -LiteralPath $pythonWheelhouseManifest -PathType Leaf) {
  $results.Add((New-CheckResult "python wheelhouse manifest" "PASS" "backend dependency handoff manifest is present"))
}
else {
  $results.Add((New-CheckResult "python wheelhouse manifest" "WARN" "Missing; run corepack pnpm run export:python-backend-reqs."))
}

$pythonWheelhousePath = $env:SHOPRPA_PYTHON_WHEELHOUSE
if (-not $pythonWheelhousePath) {
  $defaultWheelhousePath = Join-Path $repoRoot "build\tmp\offline-wheelhouse"
  if (Test-Path -LiteralPath $defaultWheelhousePath -PathType Container) {
    $pythonWheelhousePath = $defaultWheelhousePath
  }
}

if ($pythonWheelhousePath) {
  if (-not (Test-Path -LiteralPath $pythonWheelhousePath -PathType Container)) {
    $results.Add((New-CheckResult "python wheelhouse preflight" "WARN" "Wheelhouse path does not exist: $pythonWheelhousePath"))
  }
  else {
    $setupPythonBackends = Join-Path $repoRoot "scripts\setup-python-backends.ps1"
    $powershellExe = Join-Path $env:SystemRoot "System32\WindowsPowerShell\v1.0\powershell.exe"
    $resolvedWheelhousePath = (Resolve-Path -LiteralPath $pythonWheelhousePath).Path
    $wheelhouseResult = Invoke-Captured -FilePath $powershellExe -Arguments @(
      "-NoProfile",
      "-ExecutionPolicy",
      "Bypass",
      "-File",
      $setupPythonBackends,
      "-Wheelhouse",
      $resolvedWheelhousePath,
      "-Offline",
      "-CheckOnly"
    ) -WorkingDirectory $repoRoot

    if ($wheelhouseResult.ExitCode -eq 0) {
      $results.Add((New-CheckResult "python wheelhouse preflight" "PASS" "offline dependency dry run passed with $resolvedWheelhousePath"))
    }
    else {
      $missingWheelLines = @(($wheelhouseResult.Output -split "`r?`n") | Where-Object { $_.Trim().StartsWith("- Wheelhouse is missing packages") } | ForEach-Object { $_.Trim().TrimStart("-").Trim() })
      if ($missingWheelLines.Count -eq 0) {
        $missingWheelLines = @(($wheelhouseResult.Output -split "`r?`n") | Where-Object { $_.Trim().StartsWith("- Wheelhouse is missing locked wheels") } | ForEach-Object { $_.Trim().TrimStart("-").Trim() })
      }
      $detail = if ($missingWheelLines.Count -gt 0) {
        @($missingWheelLines | ForEach-Object {
            if ($_ -match "for\s+([^:]+):\s+(.+)$") {
              $serviceName = $Matches[1]
              $missingPackages = @($Matches[2] -split ",\s*" | Where-Object { $_ })
              $sample = @($missingPackages | Select-Object -First 8) -join ", "
              if ($missingPackages.Count -gt 8) {
                "$serviceName`: $($missingPackages.Count) missing locked wheels ($sample, ...)"
              }
              else {
                "$serviceName`: $($missingPackages.Count) missing locked wheels ($sample)"
              }
            }
            else {
              $_
            }
          }) -join "; "
      }
      else {
        Get-FirstUsefulLine -Text $wheelhouseResult.Output
      }
      if (-not $detail) {
        $detail = "offline dependency dry run failed with exit code $($wheelhouseResult.ExitCode)"
      }
      $downloadReportPath = Join-Path $repoRoot "build\python-backend-requirements\python-backend-wheelhouse-download-report.json"
      if (Test-Path -LiteralPath $downloadReportPath -PathType Leaf) {
        try {
          $downloadReport = Get-Content -LiteralPath $downloadReportPath -Raw | ConvertFrom-Json
          $downloadStats = ""
          if ($downloadReport.PSObject.Properties.Name -contains "failedCount") {
            $downloadStats = " (downloaded=$($downloadReport.downloadedCount), failed=$($downloadReport.failedCount), blocked=$($downloadReport.blockedCount))"
          }
          if ($downloadReport.blockedByPolicy -eq $true) {
            $detail = "$detail; latest missing-wheel download attempt was blocked by host network or endpoint policy$downloadStats at $($downloadReport.generatedAt)"
          }
          elseif ($downloadReport.ok -ne $true -and [string]$downloadReport.status) {
            $detail = "$detail; latest missing-wheel download attempt ended with status $($downloadReport.status)$downloadStats at $($downloadReport.generatedAt)"
          }
        }
        catch {
          $detail = "$detail; latest missing-wheel download report is unreadable"
        }
      }
      $detail = "$detail; to fetch only missing pins on an online prep host, run corepack pnpm run download:python-backend-wheelhouse-missing -- -Wheelhouse <path> -ContinueOnError"
      $results.Add((New-CheckResult "python wheelhouse preflight" "WARN" $detail))
    }
  }
}
else {
  $results.Add((New-CheckResult "python wheelhouse preflight" "WARN" "No wheelhouse configured; set SHOPRPA_PYTHON_WHEELHOUSE or prepare build\tmp\offline-wheelhouse."))
}

$composePath = Join-Path $dockerRoot "docker-compose.yml"
$dockerEnvPath = Join-Path $dockerRoot ".env"
$dockerGatewayPort = Get-DockerGatewayPort -ComposePath $composePath
if (Test-Path -LiteralPath $composePath -PathType Leaf) {
  $results.Add((New-CheckResult "docker compose file" "PASS" $composePath))
}
else {
  $results.Add((New-CheckResult "docker compose file" "FAIL" "Missing docker/docker-compose.yml"))
}

if (Test-Path -LiteralPath $dockerEnvPath -PathType Leaf) {
  $results.Add((New-CheckResult "docker env file" "PASS" $dockerEnvPath))
}
else {
  $results.Add((New-CheckResult "docker env file" "WARN" "docker/.env is missing; copy docker/.env.example and adjust secrets before deployment."))
}

if ($dockerGatewayPort) {
  $results.Add((New-CheckResult "docker gateway port" "PASS" "openresty-nginx publishes host port $dockerGatewayPort"))
}
else {
  $results.Add((New-CheckResult "docker gateway port" "FAIL" "Could not find the openresty-nginx host port mapping."))
}
Test-DockerComposePythonHealthchecks -ComposePath $composePath
Test-DockerComposeJavaHealthchecks -ComposePath $composePath
Test-NginxGatewaySource -RepoRoot $repoRoot

Test-RemoteAddrConfig -ConfigPath (Join-Path $repoRoot "resources\conf.yaml") -Label "source backend gateway config" -ExpectedPort $dockerGatewayPort
Test-RemoteAddrConfig -ConfigPath (Join-Path $portableRoot "resources\conf.yaml") -Label "portable backend gateway config" -ExpectedPort $dockerGatewayPort
Test-RobotOpenApiWorkflowConfig -RepoRoot $repoRoot
Test-OpenApiRobotServiceConfig -RepoRoot $repoRoot
Test-OpenApiAdminRouteSource -RepoRoot $repoRoot
$sourceBackendGateway = Read-YamlScalarValue -YamlPath (Join-Path $repoRoot "resources\conf.yaml") -Key "remote_addr"
Test-HttpHealthEndpoint -BaseUrl $sourceBackendGateway -Label "backend gateway health"
Test-HttpStaticAsset -BaseUrl $sourceBackendGateway -AssetPath "/shoprpa-static/shoprpa-wordmark.svg" -Label "auth wordmark asset" -SourcePath (Join-Path $repoRoot "docker\volumes\nginx\static\shoprpa-wordmark.svg")
Test-HttpStaticAsset -BaseUrl $sourceBackendGateway -AssetPath "/shoprpa-static/shoprpa-icon.svg" -Label "auth icon asset" -SourcePath (Join-Path $repoRoot "docker\volumes\nginx\static\shoprpa-icon.svg")
Test-BackendGatewayServiceRoutes -BaseUrl $sourceBackendGateway

function Test-DockerEnvHostnames {
  param(
    [string]$EnvPath,
    [string]$Label
  )

  if (-not (Test-Path -LiteralPath $EnvPath -PathType Leaf)) {
    $results.Add((New-CheckResult $Label "WARN" "Skipped because the env file is missing: $EnvPath"))
    return
  }

  $expectedDockerEnv = @{
    DATABASE_HOST = "mysql"
    CASDOOR_DATABASE_HOST = "mysql"
    REDIS_HOST = "redis"
    MINIO_URL = "http://minio:9000"
    CASDOOR_ENDPOINT = "http://casdoor:8000"
    CASDOOR_REDIRECT_URL = "http://localhost:1420/"
  }
  $mismatches = @()
  foreach ($entry in $expectedDockerEnv.GetEnumerator()) {
    $actualValue = Read-EnvValue -EnvPath $EnvPath -Key $entry.Key
    if ($actualValue -ne $entry.Value) {
      $mismatches += "$($entry.Key)=$actualValue expected $($entry.Value)"
    }
  }

  if ($mismatches.Count -eq 0) {
    $results.Add((New-CheckResult $Label "PASS" "compose service names are used"))
  }
  else {
    $results.Add((New-CheckResult $Label "FAIL" ($mismatches -join "; ")))
  }
}

Test-DockerEnvHostnames -EnvPath $dockerEnvPath -Label "docker env hostnames"
Test-DockerEnvHostnames -EnvPath (Join-Path $dockerRoot ".env.example") -Label "docker example hostnames"
Test-CasdoorSeedHygiene -EnvPath $dockerEnvPath -InitDataPath (Join-Path $dockerRoot "volumes\casdoor\init_data_dump.json")

$casdoorAuthPath = Join-Path $repoRoot "backend\rpa-auth\src\main\java\com\iflytek\rpa\auth\idp\casdoorIdentity\CasdoorAuthenticationServiceImpl.java"
if (Test-Path -LiteralPath $casdoorAuthPath -PathType Leaf) {
  $casdoorAuthSource = Get-Content -LiteralPath $casdoorAuthPath -Raw
  $placeholderPatterns = @(
    "public\s+User\s+login\s*\([^)]*\)\s*\{[^{}]*return\s+null\s*;",
    "public\s+User\s+setPasswordAndLogin\s*\([^)]*\)\s*\{[^{}]*return\s+null\s*;",
    "public\s+boolean\s+setPassword\s*\([^)]*\)\s*\{[^{}]*return\s+false\s*;",
    "public\s+AppResponse<Boolean>\s+refreshToken\s*\([^)]*\)\s*\{[^{}]*return\s+null\s*;",
    "public\s+String\s+getVerificationCode\s*\([^)]*\)\s*\{[^{}]*return\s+`"`"\s*;",
    "public\s+AppResponse<String>\s+changePassword\s*\([^)]*\)\s*\{[^{}]*return\s+null\s*;",
    "public\s+AppResponse<String>\s+addUser\s*\([^)]*\)\s*\{[^{}]*return\s+null\s*;"
  )
  $matchedPlaceholders = @($placeholderPatterns | Where-Object { $casdoorAuthSource -match $_ })
  if ($matchedPlaceholders.Count -eq 0) {
    $results.Add((New-CheckResult "casdoor auth placeholders" "PASS" "active auth endpoints return explicit results"))
  }
  else {
    $results.Add((New-CheckResult "casdoor auth placeholders" "FAIL" "Found placeholder return paths in Casdoor authentication service."))
  }
}
else {
  $results.Add((New-CheckResult "casdoor auth placeholders" "FAIL" "Missing Casdoor authentication service: $casdoorAuthPath"))
}

if ((Test-CommandAvailable "docker") -and (Test-Path -LiteralPath $composePath -PathType Leaf) -and (Test-Path -LiteralPath $dockerEnvPath -PathType Leaf)) {
  $composeResult = Invoke-Captured -FilePath "docker" -Arguments @("compose", "--env-file", $dockerEnvPath, "-f", $composePath, "config", "--quiet") -WorkingDirectory $repoRoot
  if ($composeResult.ExitCode -eq 0) {
    $results.Add((New-CheckResult "docker compose config" "PASS" "docker compose config --quiet"))
  }
  else {
    $detail = Get-FirstLine $composeResult.Output
    if (-not $detail) {
      $detail = "docker compose config failed with exit code $($composeResult.ExitCode)"
    }
    $results.Add((New-CheckResult "docker compose config" "FAIL" $detail))
  }
}
else {
  $results.Add((New-CheckResult "docker compose config" "WARN" "Skipped; docker, docker-compose.yml, or docker/.env is unavailable."))
}

$portableInstallerCheck = @($results | Where-Object { $_.Name -eq "portable installer package" } | Select-Object -First 1)
$portableInstallerVerified = $portableInstallerCheck.Count -gt 0 -and [string]$portableInstallerCheck[0].Status -eq "PASS"
$installerDoctor = Join-Path $repoRoot "frontend\packages\electron-app\scripts\check-installer-host.js"
if ((Test-CommandAvailable "node") -and (Test-Path -LiteralPath $installerDoctor -PathType Leaf)) {
  $installerResult = Invoke-Captured -FilePath "node" -Arguments @($installerDoctor) -WorkingDirectory (Join-Path $repoRoot "frontend\packages\electron-app")
  if ($installerResult.ExitCode -eq 0) {
    $results.Add((New-CheckResult "nsis installer host" "PASS" "NSIS installer preflight passed."))
  }
  else {
    $detail = Get-FirstUsefulLine -Text $installerResult.Output -IgnoredLines @("Windows installer preflight failed.")
    if (-not $detail) {
      $detail = "installer preflight exited with code $($installerResult.ExitCode)"
    }
    if ($portableInstallerVerified) {
      $results.Add((New-CheckResult "nsis installer host" "PASS" "Optional NSIS preflight unavailable on this host ($detail); verified portable installer zip is the release installer."))
    }
    else {
      $results.Add((New-CheckResult "nsis installer host" "WARN" $detail))
    }
  }
}
else {
  if ($portableInstallerVerified) {
    $results.Add((New-CheckResult "nsis installer host" "PASS" "Optional NSIS preflight skipped; verified portable installer zip is the release installer."))
  }
  else {
    $results.Add((New-CheckResult "nsis installer host" "WARN" "Cannot run installer preflight because node or check script is unavailable."))
  }
}

$javaRequirement = Get-RequiredJavaRuntime -RepoRoot $repoRoot -PomPaths $javaBackendPoms
$javaCommand = Resolve-JavaCommand -RequiredMajor $javaRequirement.Major -WorkingDirectory $repoRoot
if ($javaCommand) {
  $javaResult = Invoke-Captured -FilePath $javaCommand -Arguments @("-version") -WorkingDirectory $repoRoot
  $javaDetail = Get-FirstLine $javaResult.Output
  if ($javaCommand -ne "java") {
    $javaDetail = "$javaDetail ($javaCommand)"
  }
  $installedJavaMajor = Get-JavaMajorFromVersionOutput -Text $javaResult.Output
  if ($javaResult.ExitCode -ne 0) {
    $results.Add((New-CheckResult "java" "FAIL" $javaDetail))
  }
  elseif (($javaRequirement.Major -gt 0) -and ($installedJavaMajor -gt 0) -and ($installedJavaMajor -lt $javaRequirement.Major)) {
    $results.Add((New-CheckResult "java" "WARN" "$javaDetail; Java $($javaRequirement.Major)+ required by Java backend modules ($($javaRequirement.Details -join ', '))"))
  }
  elseif (($javaRequirement.Major -gt 0) -and ($installedJavaMajor -eq 0)) {
    $results.Add((New-CheckResult "java" "WARN" "$javaDetail; could not parse Java major version; Java $($javaRequirement.Major)+ is required by Java backend modules ($($javaRequirement.Details -join ', '))"))
  }
  else {
    $requirementDetail = if ($javaRequirement.Major -gt 0) { "; satisfies Java $($javaRequirement.Major)+ requirement ($($javaRequirement.Details -join ', '))" } else { "" }
    $results.Add((New-CheckResult "java" "PASS" "$javaDetail$requirementDetail"))
  }
}
else {
  $requirementDetail = if ($javaRequirement.Major -gt 0) { " Java $($javaRequirement.Major)+ is required by Java backend modules ($($javaRequirement.Details -join ', '))." } else { "" }
  $results.Add((New-CheckResult "java" "WARN" "java was not found on PATH or common local JDK locations; Java backend checks cannot run here.$requirementDetail"))
}

$mavenInvocation = Resolve-MavenInvocation -RepoRoot $repoRoot
if ($mavenInvocation) {
  $mavenResult = Invoke-Captured -FilePath $mavenInvocation.FilePath -Arguments ($mavenInvocation.ArgumentsPrefix + @("-version")) -WorkingDirectory $repoRoot
  $mavenDetail = Get-FirstLine $mavenResult.Output
  if ($mavenInvocation.Display -ne "mvn") {
    $mavenDetail = "$mavenDetail ($($mavenInvocation.Display))"
  }
  $results.Add((New-CheckResult "maven" ($(if ($mavenResult.ExitCode -eq 0) { "PASS" } else { "FAIL" })) $mavenDetail))
}
else {
  $results.Add((New-CheckResult "maven" "WARN" "Maven was not found on PATH, MAVEN_HOME, M2_HOME, common local install paths, or repo mvnw.cmd; Java backend checks cannot run here."))
}

$javaBackendTestsDoctor = Get-JavaBackendTestsDoctorDetail -ReportPath $javaBackendTestsReportPath
$results.Add((New-CheckResult "java backend Maven tests" $javaBackendTestsDoctor.Status $javaBackendTestsDoctor.Detail))

if (Test-CommandAvailable "docker") {
  $dockerResult = Invoke-Captured -FilePath "docker" -Arguments @("version", "--format", "{{.Server.Version}}") -WorkingDirectory $repoRoot
  if ($dockerResult.ExitCode -eq 0) {
    $results.Add((New-CheckResult "docker engine" "PASS" (Get-FirstLine $dockerResult.Output)))
  }
  else {
    $detail = Get-FirstLine $dockerResult.Output
    if (-not $detail) {
      $detail = "docker version failed with exit code $($dockerResult.ExitCode)"
    }
    $detail = Get-DockerEngineDiagnostic -BaseDetail $detail
    $results.Add((New-CheckResult "docker engine" "WARN" $detail))
  }
}
else {
  $results.Add((New-CheckResult "docker engine" "WARN" "docker is not on PATH; Docker compose checks cannot run here."))
}

Write-Host "ShopRPA release readiness doctor"
Write-Host "Repository: $repoRoot"
Write-Host ""
$results | Format-Table -AutoSize

$failures = @($results | Where-Object { $_.Status -eq "FAIL" })
$warnings = @($results | Where-Object { $_.Status -eq "WARN" })

if ($failures.Count -gt 0) {
  Write-Host ""
  Write-Host "Failure details:" -ForegroundColor Red
  foreach ($failure in $failures) {
    Write-Host "- $($failure.Name): $($failure.Detail)" -ForegroundColor Red
  }
}

if ($warnings.Count -gt 0) {
  Write-Host ""
  Write-Host "Warning details:" -ForegroundColor Yellow
  foreach ($warning in $warnings) {
    Write-Host "- $($warning.Name): $($warning.Detail)" -ForegroundColor Yellow
  }
}

if ($failures.Count -gt 0) {
  Write-Host "Blocking failures: $($failures.Count)" -ForegroundColor Red
  exit 1
}

if ($Strict -and $warnings.Count -gt 0) {
  Write-Host "Strict mode failed because warnings remain: $($warnings.Count)" -ForegroundColor Yellow
  exit 1
}

if ($warnings.Count -gt 0) {
  Write-Host "Warnings remain: $($warnings.Count). Portable desktop readiness can still be valid, but full-stack release coverage is incomplete." -ForegroundColor Yellow
}
else {
  Write-Host "All release doctor checks passed." -ForegroundColor Green
}
