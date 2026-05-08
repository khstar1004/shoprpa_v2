param(
  [string]$PackagePath = "",
  [switch]$SkipSmoke
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

function Assert-File {
  param(
    [string]$Path,
    [string]$Label
  )

  if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
    throw "$Label is missing: $Path"
  }
  if ((Get-Item -LiteralPath $Path).Length -le 0) {
    throw "$Label is empty: $Path"
  }
}

function Assert-SafeChildPath {
  param(
    [string]$BasePath,
    [string]$TargetPath
  )

  $baseFull = [System.IO.Path]::GetFullPath($BasePath).TrimEnd("\") + "\"
  $targetFull = [System.IO.Path]::GetFullPath($TargetPath)
  if (-not $targetFull.StartsWith($baseFull, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to operate outside $baseFull`: $targetFull"
  }
}

function Remove-SafeDirectory {
  param(
    [string]$BasePath,
    [string]$TargetPath
  )

  Assert-SafeChildPath -BasePath $BasePath -TargetPath $TargetPath
  if (Test-Path -LiteralPath $TargetPath -PathType Container) {
    Remove-Item -LiteralPath $TargetPath -Recurse -Force
  }
}

function Invoke-Checked {
  param(
    [scriptblock]$Command,
    [string]$Label
  )

  & $Command
  if ($LASTEXITCODE -ne 0) {
    throw "$Label failed with exit code $LASTEXITCODE"
  }
}

function Wait-PathRemoved {
  param(
    [string]$Path,
    [int]$TimeoutSeconds = 20
  )

  $deadline = [DateTimeOffset]::Now.AddSeconds($TimeoutSeconds)
  while ([DateTimeOffset]::Now -lt $deadline) {
    if (-not (Test-Path -LiteralPath $Path)) {
      return
    }
    Start-Sleep -Milliseconds 250
  }
  throw "Path was not removed within ${TimeoutSeconds}s: $Path"
}

$repoRoot = Resolve-RepoRoot
$packageJsonPath = Join-Path $repoRoot "frontend\packages\electron-app\package.json"
$packageJson = Get-Content -LiteralPath $packageJsonPath -Raw | ConvertFrom-Json
$version = [string]$packageJson.version

if ([string]::IsNullOrWhiteSpace($PackagePath)) {
  $PackagePath = Join-Path $repoRoot "frontend\packages\electron-app\dist\installers\ShopRPA-$version-portable-installer.zip"
}

$PackagePath = (Resolve-Path -LiteralPath $PackagePath).Path
$packageHashPath = "$PackagePath.sha256.txt"
Assert-File $PackagePath "Portable installer package"
Assert-File $packageHashPath "Portable installer hash"

$expectedPackageHash = (Get-Content -LiteralPath $packageHashPath -Raw).Trim().ToUpperInvariant()
$actualPackageHash = (Get-FileHash -LiteralPath $PackagePath -Algorithm SHA256).Hash.ToUpperInvariant()
if ($expectedPackageHash -ne $actualPackageHash) {
  throw "Portable installer hash mismatch: expected $expectedPackageHash, got $actualPackageHash"
}

$buildRoot = Join-Path $repoRoot "build"
$verifyRoot = Join-Path $buildRoot "portable-installer-verify"
$extractRoot = Join-Path $verifyRoot "extracted"
$installDir = Join-Path $verifyRoot "installed\ShopRPA"
if (-not (Test-Path -LiteralPath $buildRoot -PathType Container)) {
  New-Item -ItemType Directory -Path $buildRoot | Out-Null
}
Remove-SafeDirectory -BasePath $buildRoot -TargetPath $verifyRoot
New-Item -ItemType Directory -Path $extractRoot | Out-Null

Expand-Archive -LiteralPath $PackagePath -DestinationPath $extractRoot -Force

$installPs1 = Join-Path $extractRoot "Install-ShopRPA.ps1"
$installCmd = Join-Path $extractRoot "Install-ShopRPA.cmd"
$uninstallTemplate = Join-Path $extractRoot "Uninstall-ShopRPA.ps1"
$manifestPath = Join-Path $extractRoot "manifest.json"
$readmePath = Join-Path $extractRoot "README-install.txt"
$payloadRoot = Join-Path $extractRoot "payload\ShopRPA"
Assert-File $installPs1 "Installer PowerShell script"
Assert-File $installCmd "Installer command wrapper"
Assert-File $uninstallTemplate "Uninstaller template"
Assert-File $manifestPath "Installer manifest"
Assert-File $readmePath "Installer README"
Assert-File (Join-Path $payloadRoot "ShopRPA.cmd") "Payload launcher"
Assert-File (Join-Path $payloadRoot "resources\app.asar") "Payload app archive"
Assert-File (Join-Path $payloadRoot "resources\conf.yaml") "Payload runtime config"
Assert-File (Join-Path $payloadRoot "resources\renderer\boot.html") "Payload renderer boot page"
Assert-File (Join-Path $payloadRoot "resources\renderer\index.html") "Payload renderer index page"
Assert-File (Join-Path $payloadRoot "resources\python_core.7z") "Payload Python archive"
Assert-File (Join-Path $payloadRoot "resources\python_core.7z.sha256.txt") "Payload Python hash"
foreach ($transientPayloadPath in @("smoke-verification.json", ".smoke-user-data")) {
  $transientPath = Join-Path $payloadRoot $transientPayloadPath
  if (Test-Path -LiteralPath $transientPath) {
    throw "Transient verification artifact must not be packaged: payload\ShopRPA\$transientPayloadPath"
  }
}

$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
if ([string]$manifest.productName -ne "ShopRPA") {
  throw "Installer manifest productName mismatch: $($manifest.productName)"
}
if ([string]$manifest.version -ne $version) {
  throw "Installer manifest version mismatch: expected $version, got $($manifest.version)"
}
if ([string]::IsNullOrWhiteSpace([string]$manifest.deterministicZipTimestamp)) {
  throw "Installer manifest is missing deterministicZipTimestamp."
}
$requiredEntrypoints = @("Install-ShopRPA.cmd", "Install-ShopRPA.ps1", "Uninstall-ShopRPA.ps1", "payload\ShopRPA\ShopRPA.cmd")
$manifestEntrypoints = @($manifest.entrypoints | ForEach-Object { [string]$_ })
foreach ($entrypoint in $requiredEntrypoints) {
  if ($manifestEntrypoints -notcontains $entrypoint) {
    throw "Installer manifest is missing entrypoint: $entrypoint"
  }
}

$readme = Get-Content -LiteralPath $readmePath -Raw
$requiredReadmePhrases = @("How to install", "Default install location", "What this installer does", "Validation")
foreach ($phrase in $requiredReadmePhrases) {
  if ($readme -notmatch [regex]::Escape($phrase)) {
    throw "Installer README is missing section: $phrase"
  }
}

$payloadArchive = Join-Path $payloadRoot "resources\python_core.7z"
$payloadHash = (Get-FileHash -LiteralPath $payloadArchive -Algorithm SHA256).Hash.ToUpperInvariant()
if ([string]$manifest.pythonArchiveSha256 -ne $payloadHash) {
  throw "Installer manifest Python archive hash mismatch: expected $($manifest.pythonArchiveSha256), got $payloadHash"
}

Invoke-Checked {
  & "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File $installPs1 -InstallDir $installDir -NoShortcuts -Force | Out-Host
} "Portable installer dry install"

$installedLauncher = Join-Path $installDir "ShopRPA.cmd"
$installedArchive = Join-Path $installDir "resources\python_core.7z"
$installedHashPath = Join-Path $installDir "resources\python_core.7z.sha256.txt"
Assert-File $installedLauncher "Installed launcher"
Assert-File (Join-Path $installDir "resources\app.asar") "Installed app archive"
Assert-File (Join-Path $installDir "resources\conf.yaml") "Installed runtime config"
Assert-File (Join-Path $installDir "resources\renderer\boot.html") "Installed renderer boot page"
Assert-File (Join-Path $installDir "resources\renderer\index.html") "Installed renderer index page"
Assert-File $installedArchive "Installed Python archive"
Assert-File $installedHashPath "Installed Python hash"
Assert-File (Join-Path $installDir "Uninstall-ShopRPA.ps1") "Installed uninstaller"
$installedManifestPath = Join-Path $installDir "installed.json"
Assert-File $installedManifestPath "Install manifest"

$installedManifest = Get-Content -LiteralPath $installedManifestPath -Raw | ConvertFrom-Json
if ([string]$installedManifest.productName -ne "ShopRPA") {
  throw "Installed manifest productName mismatch: $($installedManifest.productName)"
}
if ([string]$installedManifest.version -ne $version) {
  throw "Installed manifest version mismatch: expected $version, got $($installedManifest.version)"
}
if ([string]$installedManifest.installDir -ne $installDir) {
  throw "Installed manifest installDir mismatch: expected $installDir, got $($installedManifest.installDir)"
}

$expectedInstalledHash = (Get-Content -LiteralPath $installedHashPath -Raw).Trim().ToUpperInvariant()
$actualInstalledHash = (Get-FileHash -LiteralPath $installedArchive -Algorithm SHA256).Hash.ToUpperInvariant()
if ($expectedInstalledHash -ne $actualInstalledHash) {
  throw "Installed Python archive hash mismatch: expected $expectedInstalledHash, got $actualInstalledHash"
}

$smokeOk = $false
$smokeResultPath = Join-Path $verifyRoot "installed-smoke.json"
$smokeUserData = Join-Path $verifyRoot "smoke-user-data"
if (-not $SkipSmoke) {
  $oldSmokeTest = $env:SHOPRPA_SMOKE_TEST
  $oldSmokeResult = $env:SHOPRPA_SMOKE_RESULT
  $oldSmokeUserData = $env:SHOPRPA_SMOKE_USER_DATA
  try {
    $env:SHOPRPA_SMOKE_TEST = "1"
    $env:SHOPRPA_SMOKE_RESULT = $smokeResultPath
    $env:SHOPRPA_SMOKE_USER_DATA = $smokeUserData
    Invoke-Checked { & $installedLauncher | Out-Host } "Installed package smoke test"
  }
  finally {
    $env:SHOPRPA_SMOKE_TEST = $oldSmokeTest
    $env:SHOPRPA_SMOKE_RESULT = $oldSmokeResult
    $env:SHOPRPA_SMOKE_USER_DATA = $oldSmokeUserData
  }

  Assert-File $smokeResultPath "Installed smoke result"
  $smokeResult = Get-Content -LiteralPath $smokeResultPath -Raw | ConvertFrom-Json
  if ([string]::IsNullOrWhiteSpace([string]$smokeResult.generatedAt)) {
    throw "Installed smoke result is missing generatedAt."
  }
  try {
    $smokeGeneratedAt = [DateTimeOffset]::Parse([string]$smokeResult.generatedAt)
  }
  catch {
    throw "Installed smoke result generatedAt is invalid: $($smokeResult.generatedAt)"
  }
  if ($smokeGeneratedAt -gt [DateTimeOffset]::Now.AddMinutes(10)) {
    throw "Installed smoke result generatedAt is in the future: $($smokeResult.generatedAt)"
  }
  $smokeRendererPath = [string]$smokeResult.rendererPath
  $smokeOk = ($smokeResult.ok -eq $true) -and
    ($smokeResult.packagedRuntime -eq $true) -and
    ([string]$smokeResult.appPath).EndsWith("resources\app.asar") -and
    ($smokeRendererPath.Contains("resources\renderer") -or $smokeRendererPath.Contains("resources\app.asar\out\renderer"))
  if (-not $smokeOk) {
    throw "Installed package smoke result is invalid."
  }
}

$installedUninstaller = Join-Path $installDir "Uninstall-ShopRPA.ps1"
Invoke-Checked {
  & "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File $installedUninstaller -NoShortcuts -Force | Out-Host
} "Portable installer uninstall"
Wait-PathRemoved -Path $installDir -TimeoutSeconds 20

$verificationPath = "$PackagePath.verify.json"
$verification = [PSCustomObject]@{
  ok = $true
  verifiedAt = (Get-Date).ToString("o")
  packagePath = $PackagePath
  packageSha256 = $actualPackageHash
  installedPath = $installDir
  pythonArchiveSha256 = $actualInstalledHash
  installerManifest = "pass"
  installerReadme = "pass"
  installedManifest = "pass"
  smoke = if ($SkipSmoke) { "skipped" } else { "pass" }
  uninstall = "pass"
}
$verification | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $verificationPath -Encoding UTF8

Write-Host "Portable installer verification passed"
Write-Host "packageSha256=$actualPackageHash"
Write-Host "verification=$verificationPath"
