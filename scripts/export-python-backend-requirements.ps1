param(
  [string]$PythonExe = "",
  [string]$OutputDir = "",
  [string]$Wheelhouse = "",
  [string]$IndexUrl = $env:SHOPRPA_PIP_INDEX_URL,
  [switch]$NoHashes,
  [switch]$DownloadWheelhouse
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

function Get-RequirementEntries {
  param([string]$RequirementsPath)

  $entries = New-Object System.Collections.Generic.List[object]
  if (-not (Test-Path -LiteralPath $RequirementsPath -PathType Leaf)) {
    return $entries
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

    $requirementText = ($trimmed -replace "\s*\\\s*$", "").Trim()
    if ($requirementText.StartsWith(".\") -or $requirementText.StartsWith("./") -or $requirementText -match "^[A-Za-z]:[\\/]") {
      $entries.Add([PSCustomObject]@{
          Kind = "local"
          Name = [System.IO.Path]::GetFileName(($requirementText -split "\s+")[0])
          Version = ""
          NormalizedName = ""
          Requirement = $requirementText
        }) | Out-Null
      continue
    }
    if ($requirementText -match "^\w+://") {
      continue
    }

    if ($requirementText -match "^([A-Za-z0-9_.-]+)==([^\s;\\]+)") {
      $entries.Add([PSCustomObject]@{
          Kind = "package"
          Name = $Matches[1]
          Version = $Matches[2]
          NormalizedName = Normalize-PackageName $Matches[1]
          Requirement = $requirementText
        }) | Out-Null
    }
    elseif ($requirementText -match "^([A-Za-z0-9_.-]+)\s*(\[|~=|!=|<=|>=|<|>|;|$)") {
      $entries.Add([PSCustomObject]@{
          Kind = "package"
          Name = $Matches[1]
          Version = ""
          NormalizedName = Normalize-PackageName $Matches[1]
          Requirement = $requirementText
        }) | Out-Null
    }
  }

  return $entries
}

function Write-WheelhouseManifest {
  param(
    [object[]]$Services,
    [string]$OutputDir,
    [string]$WheelhousePath,
    [string]$PackageIndexUrl
  )

  $serviceManifests = New-Object System.Collections.Generic.List[object]
  $uniquePackages = @{}
  foreach ($service in $Services) {
    $entries = Get-RequirementEntries -RequirementsPath $service.Requirements
    $packages = @($entries | Where-Object { $_.Kind -eq "package" } | Sort-Object NormalizedName, Version)
    $localArtifacts = @($entries | Where-Object { $_.Kind -eq "local" } | Sort-Object Name)

    foreach ($package in $packages) {
      $key = "$($package.NormalizedName)==$($package.Version)"
      if (-not $uniquePackages.ContainsKey($key)) {
        $uniquePackages[$key] = $package
      }
    }

    $serviceManifests.Add([PSCustomObject]@{
        name = $service.Name
        root = $service.Root
        requirements = $service.Requirements
        packageCount = $packages.Count
        localArtifactCount = $localArtifacts.Count
        packages = $packages
        localArtifacts = $localArtifacts
      }) | Out-Null
  }

  $packageList = @($uniquePackages.Values | Sort-Object NormalizedName, Version)
  $manifest = [PSCustomObject]@{
    generatedAt = (Get-Date).ToString("o")
    python = $python
    outputDir = $OutputDir
    wheelhouse = $WheelhousePath
    packageIndexUrl = $PackageIndexUrl
    serviceCount = $serviceManifests.Count
    uniquePackageCount = $packageList.Count
    services = $serviceManifests
    uniquePackages = $packageList
  }

  $jsonPath = Join-Path $OutputDir "python-backend-wheelhouse-manifest.json"
  $manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

  $mdPath = Join-Path $OutputDir "python-backend-wheelhouse-manifest.md"
  $lines = New-Object System.Collections.Generic.List[string]
  $lines.Add("# Python Backend Wheelhouse Manifest") | Out-Null
  $lines.Add("") | Out-Null
  $lines.Add("Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')") | Out-Null
  $lines.Add("Python: ``$python``") | Out-Null
  if ($PackageIndexUrl) {
    $lines.Add("Package index: ``$PackageIndexUrl``") | Out-Null
  }
  else {
    $lines.Add("Package index: pip default") | Out-Null
  }
  $lines.Add("Unique package pins: $($packageList.Count)") | Out-Null
  $lines.Add("") | Out-Null
  $lines.Add("## Services") | Out-Null
  $lines.Add("") | Out-Null
  $lines.Add("| Service | Packages | Local artifacts | Requirements |") | Out-Null
  $lines.Add("| --- | ---: | ---: | --- |") | Out-Null
  foreach ($serviceManifest in $serviceManifests) {
    $relativeRequirements = Get-RelativePath -BasePath $repoRoot -TargetPath $serviceManifest.requirements
    $lines.Add("| $($serviceManifest.name) | $($serviceManifest.packageCount) | $($serviceManifest.localArtifactCount) | ``$relativeRequirements`` |") | Out-Null
  }
  $lines.Add("") | Out-Null
  $lines.Add("## Online Wheelhouse Command") | Out-Null
  $lines.Add("") | Out-Null
  $lines.Add("Set ``SHOPRPA_PIP_INDEX_URL`` first when downloading from a private PyPI mirror.") | Out-Null
  $lines.Add("") | Out-Null
  $lines.Add('```powershell') | Out-Null
  $lines.Add('corepack pnpm run export:python-backend-reqs -- -Wheelhouse D:\wheelhouse\shoprpa-python-backends -DownloadWheelhouse') | Out-Null
  $lines.Add('```') | Out-Null
  $lines.Add("") | Out-Null
  $lines.Add("## Offline Install Check") | Out-Null
  $lines.Add("") | Out-Null
  $lines.Add('```powershell') | Out-Null
  $lines.Add('corepack pnpm run setup:python-backends -- -Wheelhouse D:\wheelhouse\shoprpa-python-backends -Offline -CheckOnly') | Out-Null
  $lines.Add('```') | Out-Null
  $lines.Add("") | Out-Null
  $lines.Add("## Unique Package Pins") | Out-Null
  $lines.Add("") | Out-Null
  foreach ($package in $packageList) {
    $versionSuffix = if ($package.Version) { "==$($package.Version)" } else { "" }
    $lines.Add("- $($package.Name)$versionSuffix") | Out-Null
  }
  $lines.Add("") | Out-Null
  $lines.Add("## Local Artifacts") | Out-Null
  $lines.Add("") | Out-Null
  foreach ($serviceManifest in $serviceManifests) {
    foreach ($artifact in $serviceManifest.localArtifacts) {
      $lines.Add("- $($serviceManifest.name): ``$($artifact.Requirement)``") | Out-Null
    }
  }
  Set-Content -LiteralPath $mdPath -Value $lines -Encoding UTF8

  Write-Host "Wheelhouse manifest: $(Get-RelativePath -BasePath $repoRoot -TargetPath $jsonPath)"
  Write-Host "Wheelhouse manifest summary: $(Get-RelativePath -BasePath $repoRoot -TargetPath $mdPath)"
}

$repoRoot = Resolve-RepoRoot
$python = Resolve-PythonExe -RequestedPythonExe $PythonExe
if (-not $OutputDir) {
  $OutputDir = Join-Path $repoRoot "build\python-backend-requirements"
}
if (-not (Test-Path -LiteralPath $OutputDir -PathType Container)) {
  New-Item -ItemType Directory -Path $OutputDir | Out-Null
}
$resolvedOutputDir = (Resolve-Path -LiteralPath $OutputDir).Path

$resolvedWheelhouse = ""
if ($Wheelhouse) {
  if (-not (Test-Path -LiteralPath $Wheelhouse -PathType Container)) {
    New-Item -ItemType Directory -Path $Wheelhouse | Out-Null
  }
  $resolvedWheelhouse = (Resolve-Path -LiteralPath $Wheelhouse).Path
}
elseif ($DownloadWheelhouse) {
  Write-Host "-DownloadWheelhouse requires -Wheelhouse <path>." -ForegroundColor Yellow
  exit 1
}

$services = @(
  @{
    Name = "ai-service"
    Root = Join-Path $repoRoot "backend\ai-service"
    Requirements = Join-Path $resolvedOutputDir "ai-service.requirements.txt"
  },
  @{
    Name = "openapi-service"
    Root = Join-Path $repoRoot "backend\openapi-service"
    Requirements = Join-Path $resolvedOutputDir "openapi-service.requirements.txt"
  }
)

Write-Host "Using Python: $python"
Write-Host "Requirements output: $resolvedOutputDir"
if ($IndexUrl) {
  Write-Host "Using package index: $IndexUrl"
}

foreach ($service in $services) {
  if (-not (Test-Path -LiteralPath (Join-Path $service.Root "uv.lock") -PathType Leaf)) {
    throw "Missing uv.lock for $($service.Name): $($service.Root)"
  }

  $exportArgs = @("-m", "uv", "export", "--locked", "--no-dev", "--format", "requirements.txt", "--no-emit-project", "--output-file", $service.Requirements)
  if ($NoHashes) {
    $exportArgs += "--no-hashes"
  }

  $exportResult = Invoke-Captured -FilePath $python -Arguments $exportArgs -WorkingDirectory $service.Root
  if (($exportResult.ExitCode -ne 0) -and $exportResult.Output) {
    Write-Host $exportResult.Output
  }
  if ($exportResult.ExitCode -ne 0) {
    throw "uv export failed for $($service.Name) with exit code $($exportResult.ExitCode)"
  }

  $relativeRequirements = Get-RelativePath -BasePath $repoRoot -TargetPath $service.Requirements
  Write-Host "Exported $($service.Name): $relativeRequirements"

  if ($DownloadWheelhouse) {
    $downloadArgs = @("-m", "pip", "download", "--only-binary=:all:", "--dest", $resolvedWheelhouse, "--requirement", $service.Requirements)
    if ($IndexUrl) {
      $downloadArgs += @("--index-url", $IndexUrl)
    }
    $downloadResult = Invoke-Captured -FilePath $python -Arguments $downloadArgs -WorkingDirectory $service.Root
    if ($downloadResult.Output) {
      Write-Host $downloadResult.Output
    }
    if ($downloadResult.ExitCode -ne 0) {
      throw "pip download failed for $($service.Name) with exit code $($downloadResult.ExitCode)"
    }
  }
}

if ($DownloadWheelhouse) {
  Write-Host "Wheelhouse populated: $resolvedWheelhouse"
}
else {
  Write-Host ""
  Write-Host "To populate a wheelhouse on an online Windows/Python 3.13 machine:"
  Write-Host "  If using a private PyPI mirror, set SHOPRPA_PIP_INDEX_URL before running these commands."
  foreach ($service in $services) {
    $relativeServiceRoot = Get-RelativePath -BasePath $repoRoot -TargetPath $service.Root
    $indexArgPreview = if ($IndexUrl) { " --index-url `"$IndexUrl`"" } else { "" }
    Write-Host "  Push-Location $relativeServiceRoot; & `"$python`" -m pip download --only-binary=:all: --dest <wheelhouse> --requirement `"$($service.Requirements)`"$indexArgPreview; Pop-Location"
  }
}

Write-WheelhouseManifest -Services $services -OutputDir $resolvedOutputDir -WheelhousePath $resolvedWheelhouse -PackageIndexUrl $IndexUrl

if ($DownloadWheelhouse) {
  $setupScript = Join-Path $repoRoot "scripts\setup-python-backends.ps1"
  Write-Host ""
  Write-Host "Validating populated wheelhouse with offline dry run..."
  & "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File $setupScript -Wheelhouse $resolvedWheelhouse -Offline -CheckOnly
  if ($LASTEXITCODE -ne 0) {
    throw "Wheelhouse validation failed with exit code $LASTEXITCODE"
  }
}
