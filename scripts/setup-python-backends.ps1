param(
  [string]$PythonExe = "",
  [string]$Wheelhouse = $env:SHOPRPA_PYTHON_WHEELHOUSE,
  [string]$IndexUrl = $env:SHOPRPA_PIP_INDEX_URL,
  [string]$PreflightReportPath = "",
  [switch]$Offline,
  [switch]$NoIndex,
  [switch]$CheckOnly
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

function Resolve-UvInvocation {
  param([string]$PythonExe)

  $candidates = New-Object System.Collections.Generic.List[string]
  $uvCommand = Get-Command "uv" -ErrorAction SilentlyContinue
  if ($uvCommand -and $uvCommand.Source) {
    $candidates.Add($uvCommand.Source) | Out-Null
  }

  if ($PythonExe -and (Test-Path -LiteralPath $PythonExe -PathType Leaf)) {
    $pythonDir = Split-Path -Parent $PythonExe
    $candidates.Add((Join-Path $pythonDir "Scripts\uv.exe")) | Out-Null
  }

  $candidates.Add((Join-Path $env:LOCALAPPDATA "Programs\Python\Python313\Scripts\uv.exe")) | Out-Null
  $candidates.Add((Join-Path $env:LOCALAPPDATA "Programs\Python\Python312\Scripts\uv.exe")) | Out-Null

  $seen = New-Object System.Collections.Generic.HashSet[string]
  foreach ($candidate in $candidates) {
    if (-not $candidate) {
      continue
    }
    $normalizedCandidate = [System.IO.Path]::GetFullPath($candidate)
    if (-not $seen.Add($normalizedCandidate)) {
      continue
    }
    if (Test-Path -LiteralPath $normalizedCandidate -PathType Leaf) {
      $resolved = (Resolve-Path -LiteralPath $normalizedCandidate).Path
      return [PSCustomObject]@{
        FilePath = $resolved
        Arguments = @()
        Display = $resolved
      }
    }
  }

  return [PSCustomObject]@{
    FilePath = $PythonExe
    Arguments = @("-m", "uv")
    Display = "$PythonExe -m uv"
  }
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

function Normalize-PackageName {
  param([string]$Name)
  return ($Name.Trim().ToLowerInvariant() -replace '[-_.]+', '_')
}

function Get-PythonCompatibilityInfo {
  param([string]$PythonExe)

  $script = @"
import platform
import sys

print(f"{sys.version_info.major}.{sys.version_info.minor}")
print(platform.machine())
"@
  $result = Invoke-Captured -FilePath $PythonExe -Arguments @("-c", $script) -WorkingDirectory $repoRoot
  if ($result.ExitCode -ne 0) {
    throw "Could not inspect Python compatibility tags with ${PythonExe}: $($result.Output)"
  }

  $lines = @($result.Output -split "`r?`n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
  if ($lines.Count -lt 2 -or $lines[0] -notmatch "^(\d+)\.(\d+)$") {
    throw "Could not parse Python version from ${PythonExe}: $($result.Output)"
  }

  $major = [int]$Matches[1]
  $minor = [int]$Matches[2]
  $machine = [string]$lines[1]
  $windowsPlatforms = @("any")
  switch -Regex ($machine) {
    "^(AMD64|x86_64)$" {
      $windowsPlatforms += "win_amd64"
      break
    }
    "^(ARM64|aarch64)$" {
      $windowsPlatforms += "win_arm64"
      break
    }
    "^(x86|i386|i686)$" {
      $windowsPlatforms += "win32"
      break
    }
    default {
      $windowsPlatforms += "win_amd64"
    }
  }

  return [PSCustomObject]@{
    major = $major
    minor = $minor
    implementationTag = "cp$major$minor"
    platformTags = @($windowsPlatforms | Select-Object -Unique)
  }
}

function Test-WheelCompatible {
  param(
    [string]$WheelName,
    [object]$CompatibilityInfo
  )

  if ([string]::IsNullOrWhiteSpace($WheelName) -or -not $WheelName.ToLowerInvariant().EndsWith(".whl")) {
    return $false
  }

  $stem = [System.IO.Path]::GetFileNameWithoutExtension($WheelName)
  $parts = @($stem -split "-")
  if ($parts.Count -lt 5) {
    return $false
  }

  $pythonTags = @($parts[$parts.Count - 3] -split "\.")
  $abiTags = @($parts[$parts.Count - 2] -split "\.")
  $platformTags = @($parts[$parts.Count - 1] -split "\.")

  $platformOk = @($platformTags | Where-Object {
      $CompatibilityInfo.platformTags -contains $_
    }).Count -gt 0
  if (-not $platformOk) {
    return $false
  }

  $abiOk = $false
  $pythonOk = $false
  foreach ($pythonTag in $pythonTags) {
    if ($pythonTag -eq "py3" -or $pythonTag -eq "py$($CompatibilityInfo.major)") {
      $pythonOk = $true
      if ($abiTags -contains "none") {
        $abiOk = $true
      }
      continue
    }

    if ($pythonTag -eq $CompatibilityInfo.implementationTag) {
      $pythonOk = $true
      if (($abiTags -contains $CompatibilityInfo.implementationTag) -or ($abiTags -contains "abi3") -or ($abiTags -contains "none")) {
        $abiOk = $true
      }
      continue
    }

    if ($pythonTag -match "^cp$($CompatibilityInfo.major)(\d+)$") {
      $tagMinor = [int]$Matches[1]
      if (($abiTags -contains "abi3") -and ($tagMinor -le [int]$CompatibilityInfo.minor)) {
        $pythonOk = $true
        $abiOk = $true
      }
    }
  }

  return ($pythonOk -and $abiOk)
}

function Get-WheelMetadataTags {
  param([string]$WheelPath)

  $tags = New-Object System.Collections.Generic.List[string]
  try {
    Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
    $zip = [System.IO.Compression.ZipFile]::OpenRead($WheelPath)
    try {
      $wheelEntry = @($zip.Entries | Where-Object { $_.FullName -like "*.dist-info/WHEEL" } | Select-Object -First 1)
      if ($wheelEntry.Count -eq 0) {
        return $tags.ToArray()
      }
      $reader = [System.IO.StreamReader]::new($wheelEntry[0].Open())
      try {
        foreach ($line in @($reader.ReadToEnd() -split "`r?`n")) {
          if ($line -match "^Tag:\s*(.+)$") {
            $tags.Add($Matches[1].Trim()) | Out-Null
          }
        }
      }
      finally {
        $reader.Dispose()
      }
    }
    finally {
      $zip.Dispose()
    }
  }
  catch {
    return $tags.ToArray()
  }
  return $tags.ToArray()
}

function Repair-WheelhouseMetadataTagFilenames {
  param(
    [string]$WheelhousePath,
    [object]$CompatibilityInfo
  )

  $repairs = New-Object System.Collections.Generic.List[object]
  foreach ($wheel in @(Get-ChildItem -LiteralPath $WheelhousePath -File -Filter "*.whl")) {
    if (Test-WheelCompatible -WheelName $wheel.Name -CompatibilityInfo $CompatibilityInfo) {
      continue
    }

    $stem = [System.IO.Path]::GetFileNameWithoutExtension($wheel.Name)
    if ($stem -notmatch "^(.+?)-([0-9][^-]*)-") {
      continue
    }
    $distribution = $Matches[1]
    $version = $Matches[2]

    foreach ($metadataTag in @(Get-WheelMetadataTags -WheelPath $wheel.FullName)) {
      $candidateName = "$distribution-$version-$metadataTag.whl"
      if (-not (Test-WheelCompatible -WheelName $candidateName -CompatibilityInfo $CompatibilityInfo)) {
        continue
      }

      $targetPath = Join-Path $WheelhousePath $candidateName
      $copied = $false
      if (-not (Test-Path -LiteralPath $targetPath -PathType Leaf)) {
        Copy-Item -LiteralPath $wheel.FullName -Destination $targetPath
        $copied = $true
      }
      $repairs.Add([PSCustomObject]@{
          source = $wheel.Name
          target = $candidateName
          copied = $copied
        }) | Out-Null
      break
    }
  }
  return $repairs.ToArray()
}

function Get-RequirementPackages {
  param([string]$RequirementsPath)

  $packages = New-Object System.Collections.Generic.List[object]
  if (-not (Test-Path -LiteralPath $RequirementsPath -PathType Leaf)) {
    return $packages
  }

  foreach ($line in Get-Content -LiteralPath $RequirementsPath) {
    $trimmed = $line.Trim()
    if (-not $trimmed -or $trimmed.StartsWith("#") -or $trimmed.StartsWith("--")) {
      continue
    }
    if ($trimmed -match ";\s*(.+)$") {
      $marker = $Matches[1]
      if ($marker -match "sys_platform\s*!=\s*['`"]win32['`"]" -or $marker -match "platform_system\s*!=\s*['`"]Windows['`"]") {
        continue
      }
    }
    if ($trimmed.StartsWith(".\") -or $trimmed.StartsWith("./") -or $trimmed -match "^[A-Za-z]:[\\/]" -or $trimmed -match "^\w+://") {
      continue
    }
    if ($trimmed -match "^([A-Za-z0-9_.-]+)==([^\s;\\]+)") {
      $packages.Add([PSCustomObject]@{
          Name = $Matches[1]
          NormalizedName = Normalize-PackageName $Matches[1]
          Version = $Matches[2]
        }) | Out-Null
    }
    elseif ($trimmed -match "^([A-Za-z0-9_.-]+)\s*(===|~=|!=|<=|>=|<|>|;|\[)") {
      $packages.Add([PSCustomObject]@{
          Name = $Matches[1]
          NormalizedName = Normalize-PackageName $Matches[1]
          Version = ""
        }) | Out-Null
    }
  }

  return $packages
}

function Assert-WheelhouseHasRequirements {
  param(
    [string]$ServiceRoot,
    [string]$RelativeServiceRoot,
    [string]$WheelhousePath,
    [object]$CompatibilityInfo
  )

  $serviceName = Split-Path -Leaf $ServiceRoot
  $requirementsPath = Join-Path $repoRoot "build\python-backend-requirements\$serviceName.requirements.txt"
  if (-not (Test-Path -LiteralPath $requirementsPath -PathType Leaf)) {
    throw "Wheelhouse preflight cannot run for ${RelativeServiceRoot}: requirements export is missing: $requirementsPath. Run corepack pnpm run export:python-backend-reqs first."
  }

  $requiredPackages = @(Get-RequirementPackages -RequirementsPath $requirementsPath)
  if ($requiredPackages.Count -eq 0) {
    throw "Wheelhouse preflight cannot run for ${RelativeServiceRoot}: no package pins were found in $requirementsPath. Regenerate requirements with corepack pnpm run export:python-backend-reqs."
  }

  $availablePackageNames = New-Object System.Collections.Generic.HashSet[string]
  $availablePackagePins = New-Object System.Collections.Generic.HashSet[string]
  $compatiblePackagePins = New-Object System.Collections.Generic.HashSet[string]
  Get-ChildItem -LiteralPath $WheelhousePath -File | ForEach-Object {
    $fileName = $_.Name.ToLowerInvariant()
    if ($fileName -match "^(.+?)-([0-9][^-]*)-") {
      $normalizedName = Normalize-PackageName $Matches[1]
      $pin = "$normalizedName==$($Matches[2])"
      [void]$availablePackageNames.Add($normalizedName)
      [void]$availablePackagePins.Add($pin)
      if (Test-WheelCompatible -WheelName $_.Name -CompatibilityInfo $CompatibilityInfo) {
        [void]$compatiblePackagePins.Add($pin)
      }
    }
  }

  $missingPackages = @(
    $requiredPackages |
      Where-Object {
        if ($_.Version) {
          return -not $compatiblePackagePins.Contains("$($_.NormalizedName)==$($_.Version)")
        }
        return (@($compatiblePackagePins | Where-Object { $_.StartsWith("$($_.NormalizedName)==") }).Count -eq 0)
      } |
      ForEach-Object {
        if ($_.Version) {
          "$($_.Name)==$($_.Version)"
        }
        else {
          $_.Name
        }
      } |
      Sort-Object
  )
  if ($missingPackages.Count -gt 0) {
    throw "Wheelhouse is missing compatible locked wheels for ${RelativeServiceRoot}: $($missingPackages -join ', ')"
  }

  Write-Host "Wheelhouse preflight passed for $RelativeServiceRoot ($($requiredPackages.Count) packages found)."
}

function Get-WheelhouseAvailableVersionHints {
  param(
    [string]$WheelhousePath,
    [string[]]$MissingLockedWheels,
    [object]$CompatibilityInfo
  )

  $availableVersionsByName = @{}
  $compatibleVersionsByName = @{}
  $incompatibleVersionsByName = @{}
  Get-ChildItem -LiteralPath $WheelhousePath -File | ForEach-Object {
    $fileName = $_.Name.ToLowerInvariant()
    if ($fileName -match "^(.+?)-([0-9][^-]*)-") {
      $normalizedName = Normalize-PackageName $Matches[1]
      $version = $Matches[2]
      if (-not $availableVersionsByName.ContainsKey($normalizedName)) {
        $availableVersionsByName[$normalizedName] = New-Object System.Collections.Generic.HashSet[string]
      }
      if (-not $compatibleVersionsByName.ContainsKey($normalizedName)) {
        $compatibleVersionsByName[$normalizedName] = New-Object System.Collections.Generic.HashSet[string]
      }
      if (-not $incompatibleVersionsByName.ContainsKey($normalizedName)) {
        $incompatibleVersionsByName[$normalizedName] = New-Object System.Collections.Generic.HashSet[string]
      }
      [void]$availableVersionsByName[$normalizedName].Add($version)
      if (Test-WheelCompatible -WheelName $_.Name -CompatibilityInfo $CompatibilityInfo) {
        [void]$compatibleVersionsByName[$normalizedName].Add($version)
      }
      else {
        [void]$incompatibleVersionsByName[$normalizedName].Add($version)
      }
    }
  }

  $hints = New-Object System.Collections.Generic.List[object]
  foreach ($missing in $MissingLockedWheels) {
    $name = $missing
    $requiredVersion = ""
    if ($missing -match "^(.+?)==(.+)$") {
      $name = $Matches[1]
      $requiredVersion = $Matches[2]
    }
    $normalizedName = Normalize-PackageName $name
    $availableVersions = @()
    if ($availableVersionsByName.ContainsKey($normalizedName)) {
      $availableVersions = @($availableVersionsByName[$normalizedName] | Sort-Object)
    }
    $compatibleVersions = @()
    if ($compatibleVersionsByName.ContainsKey($normalizedName)) {
      $compatibleVersions = @($compatibleVersionsByName[$normalizedName] | Sort-Object)
    }
    $incompatibleVersions = @()
    if ($incompatibleVersionsByName.ContainsKey($normalizedName)) {
      $incompatibleVersions = @($incompatibleVersionsByName[$normalizedName] | Sort-Object)
    }
    $status = "missing-package"
    if ($availableVersions.Count -gt 0) {
      $status = "version-mismatch"
      if ($requiredVersion -and ($availableVersions -contains $requiredVersion)) {
        $status = "incompatible-wheel"
      }
      elseif ($compatibleVersions.Count -eq 0 -and $incompatibleVersions.Count -gt 0) {
        $status = "no-compatible-tags"
      }
    }
    $hints.Add([PSCustomObject]@{
        package = $name
        requiredVersion = $requiredVersion
        availableVersions = $availableVersions
        compatibleVersions = $compatibleVersions
        incompatibleVersions = $incompatibleVersions
        status = $status
      }) | Out-Null
  }
  return $hints.ToArray()
}

function Escape-MarkdownCell {
  param([string]$Text)
  return (($Text -replace "\|", "\|") -replace "`r?`n", " ").Trim()
}

function Write-WheelhousePreflightReport {
  param(
    [string]$ReportPath,
    [string]$WheelhousePath,
    [object[]]$Records
  )

  $reportDir = Split-Path -Parent $ReportPath
  if (-not (Test-Path -LiteralPath $reportDir -PathType Container)) {
    New-Item -ItemType Directory -Path $reportDir | Out-Null
  }

  $combinedMissing = New-Object System.Collections.Generic.List[string]
  $missingRequirementFiles = New-Object System.Collections.Generic.List[object]
  foreach ($record in $Records) {
    $serviceName = Split-Path -Leaf ([string]$record.service)
    if (-not $serviceName) {
      $serviceName = (([string]$record.service) -replace '[\\/]+', '-')
    }
    $missingRequirementsPath = Join-Path $reportDir "$serviceName.missing-wheelhouse.requirements.txt"
    $missingLockedWheels = @($record.missingLockedWheels | Sort-Object -Unique)
    $missingLines = New-Object System.Collections.Generic.List[string]
    $missingLines.Add("# Missing compatible locked wheels for $($record.service)") | Out-Null
    $missingLines.Add("# Generated by setup-python-backends.ps1 wheelhouse preflight.") | Out-Null
    $missingLines.Add("# Download these exact pins on an online Windows/Python 3.13 host, then rerun the offline preflight.") | Out-Null
    $missingLines.Add("# Use -ContinueOnError -PipRetries 0 on blocked hosts to collect every unavailable package quickly.") | Out-Null
    $missingLines.Add("") | Out-Null
    foreach ($missing in $missingLockedWheels) {
      $missingLines.Add($missing) | Out-Null
      $combinedMissing.Add($missing) | Out-Null
    }
    Set-Content -LiteralPath $missingRequirementsPath -Value $missingLines -Encoding UTF8
    $relativeMissingRequirementsPath = Get-RelativePath -BasePath $repoRoot -TargetPath $missingRequirementsPath
    $record | Add-Member -NotePropertyName missingRequirementsFile -NotePropertyValue $relativeMissingRequirementsPath -Force
    $missingRequirementFiles.Add([PSCustomObject]@{
        service = $record.service
        path = $relativeMissingRequirementsPath
        missingLockedWheelCount = $missingLockedWheels.Count
      }) | Out-Null
  }

  $combinedMissingRequirementsPath = Join-Path $reportDir "python-backend-wheelhouse-missing.requirements.txt"
  $combinedLines = New-Object System.Collections.Generic.List[string]
  $combinedLines.Add("# Missing compatible locked wheels for all Python backend services") | Out-Null
  $combinedLines.Add("# Generated by setup-python-backends.ps1 wheelhouse preflight.") | Out-Null
  $combinedLines.Add("# Download these exact pins into the wheelhouse, then rerun:") | Out-Null
  $combinedLines.Add("# corepack pnpm run download:python-backend-wheelhouse-missing -- -Wheelhouse <path> -ContinueOnError") | Out-Null
  $combinedLines.Add("# On blocked hosts, add -PipRetries 0 for a fast full missing-package inventory.") | Out-Null
  $combinedLines.Add("# corepack pnpm run setup:python-backends -- -Wheelhouse <path> -Offline -CheckOnly") | Out-Null
  $combinedLines.Add("") | Out-Null
  foreach ($missing in @($combinedMissing | Sort-Object -Unique)) {
    $combinedLines.Add($missing) | Out-Null
  }
  Set-Content -LiteralPath $combinedMissingRequirementsPath -Value $combinedLines -Encoding UTF8
  $relativeCombinedMissingRequirementsPath = Get-RelativePath -BasePath $repoRoot -TargetPath $combinedMissingRequirementsPath

  $ok = @($Records | Where-Object { $_.ok -ne $true }).Count -eq 0
  $report = [PSCustomObject]@{
    generatedAt = (Get-Date).ToString("o")
    wheelhouse = $WheelhousePath
    ok = $ok
    missingRequirementsFile = $relativeCombinedMissingRequirementsPath
    missingRequirementsFiles = $missingRequirementFiles.ToArray()
    services = $Records
  }
  $report | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $ReportPath -Encoding UTF8

  $markdownPath = [System.IO.Path]::ChangeExtension($ReportPath, ".md")
  $lines = New-Object System.Collections.Generic.List[string]
  $lines.Add("# Python Backend Wheelhouse Preflight") | Out-Null
  $lines.Add("") | Out-Null
  $lines.Add("Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')") | Out-Null
  $lines.Add("Wheelhouse: ``$WheelhousePath``") | Out-Null
  $lines.Add("Status: $(if ($ok) { "PASS" } else { "BLOCKED" })") | Out-Null
  $lines.Add("Missing requirements: ``$relativeCombinedMissingRequirementsPath``") | Out-Null
  $lines.Add("") | Out-Null
  $lines.Add("| Service | Status | Required packages | Missing requirements file | Missing compatible locked wheels | Available versions in wheelhouse |") | Out-Null
  $lines.Add("| --- | --- | ---: | --- | --- | --- |") | Out-Null
  foreach ($record in $Records) {
    $status = if ($record.ok) { "PASS" } else { "BLOCKED" }
    $missing = if ($record.missingLockedWheels.Count -gt 0) {
      Escape-MarkdownCell (($record.missingLockedWheels | Sort-Object) -join ", ")
    }
    else {
      ""
    }
    $availableHints = if ($record.availableWheelVersions) {
      @($record.availableWheelVersions | Where-Object { $_.availableVersions.Count -gt 0 } | ForEach-Object {
          $statusSuffix = if ($_.status -and $_.status -ne "version-mismatch") { " ($($_.status))" } else { "" }
          $tagSuffix = if ($_.compatibleVersions.Count -gt 0 -and $_.compatibleVersions.Count -lt $_.availableVersions.Count) {
            "; compatible=$($_.compatibleVersions -join '/')"
          }
          elseif ($_.compatibleVersions.Count -eq 0 -and $_.incompatibleVersions.Count -gt 0) {
            "; no compatible tags"
          }
          else {
            ""
          }
          "$($_.package): $($_.availableVersions -join '/')$statusSuffix$tagSuffix"
        })
    }
    else {
      @()
    }
    $available = Escape-MarkdownCell (($availableHints | Select-Object -First 12) -join ", ")
    if ($availableHints.Count -gt 12) {
      $available = "$available, ..."
    }
    $lines.Add("| $($record.service) | $status | $($record.requiredPackageCount) | ``$($record.missingRequirementsFile)`` | $missing | $available |") | Out-Null
  }
  Set-Content -LiteralPath $markdownPath -Value $lines -Encoding UTF8

  Write-Host "Wheelhouse preflight report: $(Get-RelativePath -BasePath $repoRoot -TargetPath $ReportPath)"
  Write-Host "Wheelhouse preflight summary: $(Get-RelativePath -BasePath $repoRoot -TargetPath $markdownPath)"
  Write-Host "Missing wheel requirements: $relativeCombinedMissingRequirementsPath"
}

function Test-UvNetworkBlocked {
  param([string[]]$OutputLines)

  return @(
    $OutputLines |
      Where-Object {
        $_ -match "Failed to download" -or
        $_ -match "error sending request" -or
        $_ -match "tcp connect error" -or
        $_ -match "os error 10013" -or
        $_ -match "access.*socket" -or
        $_ -match "액세스 권한"
      }
  ).Count -gt 0
}

$repoRoot = Resolve-RepoRoot
$python = Resolve-PythonExe -RequestedPythonExe $PythonExe
$pythonCompatibilityInfo = Get-PythonCompatibilityInfo -PythonExe $python
$uvInvocation = Resolve-UvInvocation -PythonExe $python
$resolvedWheelhouse = ""
if ($Wheelhouse) {
  if (-not (Test-Path -LiteralPath $Wheelhouse -PathType Container)) {
    Write-Host "Python wheelhouse directory was not found: $Wheelhouse" -ForegroundColor Yellow
    exit 1
  }
  $resolvedWheelhouse = (Resolve-Path -LiteralPath $Wheelhouse).Path
}
$serviceRoots = @(
  Join-Path $repoRoot "backend\ai-service"
  Join-Path $repoRoot "backend\openapi-service"
)

Write-Host "Using Python: $python"
Write-Host "Python wheel tag target: $($pythonCompatibilityInfo.implementationTag) ($($pythonCompatibilityInfo.platformTags -join '/'))"
Write-Host "Using uv: $($uvInvocation.Display)"
if ($resolvedWheelhouse) {
  Write-Host "Using Python wheelhouse: $resolvedWheelhouse"
  $wheelhouseFilenameRepairs = @(Repair-WheelhouseMetadataTagFilenames -WheelhousePath $resolvedWheelhouse -CompatibilityInfo $pythonCompatibilityInfo)
  foreach ($repair in $wheelhouseFilenameRepairs) {
    $action = if ($repair.copied) { "created" } else { "already present" }
    Write-Host "Wheelhouse filename normalized ($action): $($repair.source) -> $($repair.target)"
  }
}
elseif ($Offline) {
  Write-Host "Offline mode enabled; uv will use only its local cache."
}
if ($CheckOnly) {
  Write-Host "Check-only mode enabled; uv will perform a dry run."
}

$requiresWheelhousePreflight = $resolvedWheelhouse -and ($Offline -or $NoIndex -or $CheckOnly)
if ($requiresWheelhousePreflight) {
  $wheelhousePreflightErrors = New-Object System.Collections.Generic.List[string]
  $wheelhousePreflightRecords = New-Object System.Collections.Generic.List[object]
  $resolvedPreflightReportPath = $PreflightReportPath
  if (-not $resolvedPreflightReportPath) {
    $resolvedPreflightReportPath = Join-Path $repoRoot "build\python-backend-requirements\python-backend-wheelhouse-preflight.json"
  }
  foreach ($serviceRoot in $serviceRoots) {
    $relativeServiceRoot = Get-RelativePath -BasePath $repoRoot -TargetPath $serviceRoot
    $serviceName = Split-Path -Leaf $serviceRoot
    $requirementsPath = Join-Path $repoRoot "build\python-backend-requirements\$serviceName.requirements.txt"
    $requiredPackageCount = if (Test-Path -LiteralPath $requirementsPath -PathType Leaf) {
      @(Get-RequirementPackages -RequirementsPath $requirementsPath).Count
    }
    else {
      0
    }
    try {
      Assert-WheelhouseHasRequirements -ServiceRoot $serviceRoot -RelativeServiceRoot $relativeServiceRoot -WheelhousePath $resolvedWheelhouse -CompatibilityInfo $pythonCompatibilityInfo
      $wheelhousePreflightRecords.Add([PSCustomObject]@{
          service = $relativeServiceRoot
          requirements = $requirementsPath
          requiredPackageCount = $requiredPackageCount
          ok = $true
          detail = "Wheelhouse contains all locked wheels."
          missingLockedWheels = @()
          availableWheelVersions = @()
        }) | Out-Null
    }
    catch {
      $message = $_.Exception.Message
      $missingLockedWheels = @()
      if ($message -match "missing (?:compatible )?locked wheels .*:\s+(.+)$") {
        $missingLockedWheels = @($Matches[1] -split ",\s*" | Where-Object { $_ })
      }
      $availableWheelVersions = @(Get-WheelhouseAvailableVersionHints -WheelhousePath $resolvedWheelhouse -MissingLockedWheels $missingLockedWheels -CompatibilityInfo $pythonCompatibilityInfo)
      $wheelhousePreflightRecords.Add([PSCustomObject]@{
          service = $relativeServiceRoot
          requirements = $requirementsPath
          requiredPackageCount = $requiredPackageCount
          ok = $false
          detail = $message
          missingLockedWheels = $missingLockedWheels
          availableWheelVersions = $availableWheelVersions
        }) | Out-Null
      $wheelhousePreflightErrors.Add($message) | Out-Null
    }
  }

  Write-WheelhousePreflightReport -ReportPath $resolvedPreflightReportPath -WheelhousePath $resolvedWheelhouse -Records $wheelhousePreflightRecords.ToArray()

  if ($wheelhousePreflightErrors.Count -gt 0) {
    Write-Host ""
    Write-Host "Wheelhouse preflight failed." -ForegroundColor Yellow
    $wheelhousePreflightErrors | ForEach-Object { Write-Host "- $_" -ForegroundColor Yellow }
    Write-Host ""
    Write-Host "Populate the missing compatible locked wheels on an online Windows/Python 3.13 machine, then rerun:" -ForegroundColor Yellow
    Write-Host "  corepack pnpm run download:python-backend-wheelhouse-missing -- -Wheelhouse <path> -ContinueOnError" -ForegroundColor Yellow
    Write-Host "  Add -PipRetries 0 on blocked hosts to inventory every missing package quickly." -ForegroundColor Yellow
    Write-Host "  corepack pnpm run setup:python-backends -- -Wheelhouse <path> -Offline -CheckOnly" -ForegroundColor Yellow
    throw "Python backend wheelhouse is incomplete."
  }
}

foreach ($serviceRoot in $serviceRoots) {
  $pyprojectPath = Join-Path $serviceRoot "pyproject.toml"
  if (-not (Test-Path -LiteralPath $pyprojectPath -PathType Leaf)) {
    throw "Missing pyproject.toml: $pyprojectPath"
  }

  $relativeServiceRoot = Get-RelativePath -BasePath $repoRoot -TargetPath $serviceRoot
  Write-Host "Syncing $relativeServiceRoot"

  $uvArgs = @($uvInvocation.Arguments)
  $uvArgs += @("sync", "--locked", "--no-dev")
  if ($CheckOnly) {
    $uvArgs += "--dry-run"
  }
  if ($Offline) {
    $uvArgs += "--offline"
  }
  if ($resolvedWheelhouse) {
    $uvArgs += @("--find-links", $resolvedWheelhouse)
  }
  if ($NoIndex -or ($Offline -and $resolvedWheelhouse)) {
    $uvArgs += "--no-index"
  }
  elseif ($IndexUrl) {
    $uvArgs += @("--index-url", $IndexUrl)
  }

  Push-Location -LiteralPath $serviceRoot
  try {
    $captured = Invoke-Captured -FilePath $uvInvocation.FilePath -Arguments $uvArgs -WorkingDirectory $serviceRoot
    $syncExitCode = $captured.ExitCode
    $syncOutput = @($captured.Output -split "`r?`n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $syncOutput | ForEach-Object { Write-Host $_ }

    $wouldDownload = $false
    if ($CheckOnly -and ($Offline -or $NoIndex -or $resolvedWheelhouse)) {
      $wouldDownload = @($syncOutput | Where-Object { "$_" -match 'Would download\s+[1-9]\d*\s+packages?' }).Count -gt 0
    }

    if (($syncExitCode -ne 0) -or $wouldDownload) {
      Write-Host ""
      Write-Host "Python backend dependency sync did not complete." -ForegroundColor Yellow
      Write-Host "For closed-network installs, provide all locked wheels in a local folder and rerun:" -ForegroundColor Yellow
      Write-Host "  corepack pnpm run setup:python-backends -- -Wheelhouse <path> -Offline" -ForegroundColor Yellow
      if ($wouldDownload) {
        throw "uv dry run still needs downloads for $relativeServiceRoot; wheelhouse or local uv cache is incomplete"
      }
      if (Test-UvNetworkBlocked -OutputLines $syncOutput) {
        throw "uv sync could not download packages for $relativeServiceRoot; package index access is blocked by network or endpoint policy. Use -Wheelhouse <path> -Offline."
      }
      throw "uv sync failed for $relativeServiceRoot with exit code $syncExitCode"
    }
  }
  finally {
    Pop-Location
  }
}

if ($CheckOnly) {
  Write-Host "Python backend dependency dry run completed."
}
else {
  Write-Host "Python backend virtual environments are ready."
}
