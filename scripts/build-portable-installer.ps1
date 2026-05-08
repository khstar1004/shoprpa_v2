param(
  [string]$PortableRoot = "",
  [string]$OutputDir = "",
  [switch]$SkipPortableVerify
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

function Get-DeterministicPackageTimestamp {
  $minimumZipTimestamp = [DateTimeOffset]::new(1980, 1, 1, 0, 0, 0, [TimeSpan]::Zero)
  $defaultTimestamp = [DateTimeOffset]::new(2000, 1, 1, 0, 0, 0, [TimeSpan]::Zero)
  if (-not [string]::IsNullOrWhiteSpace($env:SOURCE_DATE_EPOCH)) {
    $sourceDateEpoch = [long]0
    if ([long]::TryParse($env:SOURCE_DATE_EPOCH, [ref]$sourceDateEpoch)) {
      $sourceTimestamp = [DateTimeOffset]::FromUnixTimeSeconds($sourceDateEpoch).ToUniversalTime()
      if ($sourceTimestamp -lt $minimumZipTimestamp) {
        return $minimumZipTimestamp
      }
      return $sourceTimestamp
    }
  }
  return $defaultTimestamp
}

function Get-ZipRelativePath {
  param(
    [string]$BasePath,
    [string]$TargetPath
  )

  $baseFull = [System.IO.Path]::GetFullPath($BasePath).TrimEnd("\", "/")
  $targetFull = [System.IO.Path]::GetFullPath($TargetPath)
  return $targetFull.Substring($baseFull.Length).TrimStart("\", "/").Replace("\", "/")
}

function New-DeterministicZip {
  param(
    [string]$SourceDirectory,
    [string]$ZipPath,
    [DateTimeOffset]$Timestamp
  )

  Add-Type -AssemblyName System.IO.Compression | Out-Null
  Add-Type -AssemblyName System.IO.Compression.FileSystem | Out-Null

  if (Test-Path -LiteralPath $ZipPath -PathType Leaf) {
    Remove-Item -LiteralPath $ZipPath -Force
  }

  $zipStream = [System.IO.File]::Open($ZipPath, [System.IO.FileMode]::CreateNew)
  $archive = [System.IO.Compression.ZipArchive]::new($zipStream, [System.IO.Compression.ZipArchiveMode]::Create)
  try {
    $emptyDirectories = @(
      Get-ChildItem -LiteralPath $SourceDirectory -Directory -Recurse -Force |
        Where-Object { @(Get-ChildItem -LiteralPath $_.FullName -Force).Count -eq 0 } |
        ForEach-Object {
          [PSCustomObject]@{
            ZipPath = (Get-ZipRelativePath -BasePath $SourceDirectory -TargetPath $_.FullName) + "/"
          }
        } |
        Sort-Object ZipPath
    )
    foreach ($directory in $emptyDirectories) {
      $entry = $archive.CreateEntry($directory.ZipPath)
      $entry.LastWriteTime = $Timestamp
    }

    $files = @(
      Get-ChildItem -LiteralPath $SourceDirectory -File -Recurse -Force |
        ForEach-Object {
          [PSCustomObject]@{
            File = $_
            ZipPath = Get-ZipRelativePath -BasePath $SourceDirectory -TargetPath $_.FullName
          }
        } |
        Sort-Object ZipPath
    )
    foreach ($fileEntry in $files) {
      $entry = $archive.CreateEntry($fileEntry.ZipPath, [System.IO.Compression.CompressionLevel]::Optimal)
      $entry.LastWriteTime = $Timestamp
      $entryStream = $entry.Open()
      $fileStream = [System.IO.File]::OpenRead($fileEntry.File.FullName)
      try {
        $fileStream.CopyTo($entryStream)
      }
      finally {
        $fileStream.Dispose()
        $entryStream.Dispose()
      }
    }
  }
  finally {
    $archive.Dispose()
    $zipStream.Dispose()
  }
}

function New-InstallScript {
  param([string]$Version)

  $script = @'
param(
  [string]$InstallDir = "",
  [switch]$NoShortcuts,
  [switch]$Force
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

function Get-FullPath {
  param([string]$Path)
  return [System.IO.Path]::GetFullPath($Path)
}

function Assert-SafeInstallDir {
  param([string]$Path)

  $fullPath = Get-FullPath $Path
  $trimmed = $fullPath.TrimEnd("\")
  if ($trimmed -match "^[A-Za-z]:$") {
    throw "InstallDir cannot be a drive root: $fullPath"
  }

  $leaf = Split-Path -Leaf $trimmed
  if ($leaf -ne "ShopRPA") {
    throw "InstallDir must end with ShopRPA to avoid accidental deletion: $fullPath"
  }
}

function New-ShopRpaShortcut {
  param(
    [string]$ShortcutPath,
    [string]$TargetPath,
    [string]$WorkingDirectory,
    [string]$Description,
    [string]$Arguments = ""
  )

  $parent = Split-Path -Parent $ShortcutPath
  if (-not (Test-Path -LiteralPath $parent -PathType Container)) {
    New-Item -ItemType Directory -Path $parent | Out-Null
  }

  $shell = New-Object -ComObject WScript.Shell
  $shortcut = $shell.CreateShortcut($ShortcutPath)
  $shortcut.TargetPath = $TargetPath
  $shortcut.WorkingDirectory = $WorkingDirectory
  $shortcut.Description = $Description
  $shortcut.Arguments = $Arguments
  $shortcut.IconLocation = "$TargetPath,0"
  $shortcut.Save()
}

$scriptDir = Split-Path -Parent $PSCommandPath
$payloadDir = Join-Path $scriptDir "payload\ShopRPA"
$uninstallTemplate = Join-Path $scriptDir "Uninstall-ShopRPA.ps1"
if (-not (Test-Path -LiteralPath $payloadDir -PathType Container)) {
  throw "Payload folder is missing: $payloadDir"
}
if (-not (Test-Path -LiteralPath $uninstallTemplate -PathType Leaf)) {
  throw "Uninstaller template is missing: $uninstallTemplate"
}

if ([string]::IsNullOrWhiteSpace($InstallDir)) {
  $InstallDir = Join-Path $env:LOCALAPPDATA "Programs\ShopRPA"
}

$targetDir = Get-FullPath $InstallDir
Assert-SafeInstallDir $targetDir
$parentDir = Split-Path -Parent $targetDir
if (-not (Test-Path -LiteralPath $parentDir -PathType Container)) {
  New-Item -ItemType Directory -Path $parentDir | Out-Null
}

if (Test-Path -LiteralPath $targetDir -PathType Container) {
  if (-not $Force) {
    throw "ShopRPA is already installed at $targetDir. Rerun with -Force to upgrade."
  }
  Remove-Item -LiteralPath $targetDir -Recurse -Force
}

Copy-Item -LiteralPath $payloadDir -Destination $targetDir -Recurse -Force
Copy-Item -LiteralPath $uninstallTemplate -Destination (Join-Path $targetDir "Uninstall-ShopRPA.ps1") -Force

if (-not $NoShortcuts) {
  $launcher = Join-Path $targetDir "ShopRPA.cmd"
  $desktop = [Environment]::GetFolderPath("DesktopDirectory")
  $programs = [Environment]::GetFolderPath("Programs")
  $startMenuDir = Join-Path $programs "ShopRPA"
  $powershell = Join-Path $env:SystemRoot "System32\WindowsPowerShell\v1.0\powershell.exe"
  $uninstaller = Join-Path $targetDir "Uninstall-ShopRPA.ps1"

  New-ShopRpaShortcut -ShortcutPath (Join-Path $desktop "ShopRPA.lnk") -TargetPath $launcher -WorkingDirectory $targetDir -Description "ShopRPA desktop automation client"
  New-ShopRpaShortcut -ShortcutPath (Join-Path $startMenuDir "ShopRPA.lnk") -TargetPath $launcher -WorkingDirectory $targetDir -Description "ShopRPA desktop automation client"
  New-ShopRpaShortcut -ShortcutPath (Join-Path $startMenuDir "Uninstall ShopRPA.lnk") -TargetPath $powershell -WorkingDirectory $targetDir -Description "Uninstall ShopRPA" -Arguments "-NoProfile -ExecutionPolicy Bypass -File `"$uninstaller`""
}

$manifest = [PSCustomObject]@{
  productName = "ShopRPA"
  version = "__SHOPRPA_VERSION__"
  installedAt = (Get-Date).ToString("o")
  installDir = $targetDir
}
$manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $targetDir "installed.json") -Encoding UTF8

Write-Host "ShopRPA installed: $targetDir"
'@

  return $script.Replace("__SHOPRPA_VERSION__", $Version)
}

function New-UninstallScript {
  return @'
param(
  [switch]$NoShortcuts,
  [switch]$Force
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

$installDir = Split-Path -Parent $PSCommandPath
$installDir = [System.IO.Path]::GetFullPath($installDir)

if ((Split-Path -Leaf $installDir.TrimEnd("\")) -ne "ShopRPA") {
  throw "Refusing to uninstall from an unexpected directory: $installDir"
}

if (-not $Force) {
  Write-Host "This will remove ShopRPA from: $installDir"
  $answer = Read-Host "Type YES to continue"
  if ($answer -ne "YES") {
    Write-Host "Uninstall cancelled."
    exit 0
  }
}

if (-not $NoShortcuts) {
  $desktop = [Environment]::GetFolderPath("DesktopDirectory")
  $programs = [Environment]::GetFolderPath("Programs")
  $shortcutPaths = @(
    (Join-Path $desktop "ShopRPA.lnk"),
    (Join-Path $programs "ShopRPA\ShopRPA.lnk"),
    (Join-Path $programs "ShopRPA\Uninstall ShopRPA.lnk")
  )
  foreach ($shortcutPath in $shortcutPaths) {
    if (Test-Path -LiteralPath $shortcutPath -PathType Leaf) {
      Remove-Item -LiteralPath $shortcutPath -Force
    }
  }
  $startMenuDir = Join-Path $programs "ShopRPA"
  if ((Test-Path -LiteralPath $startMenuDir -PathType Container) -and @(Get-ChildItem -LiteralPath $startMenuDir -Force).Count -eq 0) {
    Remove-Item -LiteralPath $startMenuDir -Force
  }
}

$cleanupPath = Join-Path ([System.IO.Path]::GetTempPath()) ("ShopRPA-uninstall-" + [System.Guid]::NewGuid().ToString("N") + ".ps1")
$escapedInstallDir = $installDir.Replace("'", "''")
$cleanupLines = @(
  "Start-Sleep -Seconds 1",
  "if (Test-Path -LiteralPath '$escapedInstallDir' -PathType Container) {",
  "  Remove-Item -LiteralPath '$escapedInstallDir' -Recurse -Force",
  "}",
  'Remove-Item -LiteralPath $PSCommandPath -Force'
)
Set-Content -LiteralPath $cleanupPath -Encoding UTF8 -Value $cleanupLines

Start-Process -FilePath "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $cleanupPath) -WindowStyle Hidden
Write-Host "ShopRPA uninstall started."
'@
}

function New-InstallCmd {
  return @"
@echo off
set "SCRIPT_DIR=%~dp0"
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%Install-ShopRPA.ps1" -Force %*
if errorlevel 1 (
  echo.
  echo ShopRPA install failed.
  pause
  exit /b 1
)
echo.
echo ShopRPA install completed.
"@
}

$repoRoot = Resolve-RepoRoot
if ([string]::IsNullOrWhiteSpace($PortableRoot)) {
  $PortableRoot = Join-Path $repoRoot "frontend\packages\electron-app\dist\win-portable"
}
if ([string]::IsNullOrWhiteSpace($OutputDir)) {
  $OutputDir = Join-Path $repoRoot "frontend\packages\electron-app\dist\installers"
}

$PortableRoot = (Resolve-Path -LiteralPath $PortableRoot).Path
$packageJsonPath = Join-Path $repoRoot "frontend\packages\electron-app\package.json"
$packageJson = Get-Content -LiteralPath $packageJsonPath -Raw | ConvertFrom-Json
$version = [string]$packageJson.version

$launcher = Join-Path $PortableRoot "ShopRPA.cmd"
$appAsar = Join-Path $PortableRoot "resources\app.asar"
$conf = Join-Path $PortableRoot "resources\conf.yaml"
$archive = Join-Path $PortableRoot "resources\python_core.7z"
$archiveHash = Join-Path $PortableRoot "resources\python_core.7z.sha256.txt"
Assert-File $launcher "Portable launcher"
Assert-File $appAsar "Electron app archive"
Assert-File $conf "Runtime config"
Assert-File $archive "Python runtime archive"
Assert-File $archiveHash "Python runtime hash"

if (-not $SkipPortableVerify) {
  $verifyScript = Join-Path $repoRoot "scripts\verify-portable-host.ps1"
  Invoke-Checked { & "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File $verifyScript -PortableRoot $PortableRoot | Out-Host } "Portable host verification"
}

$buildRoot = Join-Path $repoRoot "build"
$workRoot = Join-Path $buildRoot "portable-installer"
$stagingRoot = Join-Path $workRoot "ShopRPA-$version-portable-installer"
$payloadRoot = Join-Path $stagingRoot "payload\ShopRPA"
if (-not (Test-Path -LiteralPath $buildRoot -PathType Container)) {
  New-Item -ItemType Directory -Path $buildRoot | Out-Null
}
Remove-SafeDirectory -BasePath $buildRoot -TargetPath $workRoot
New-Item -ItemType Directory -Path $payloadRoot | Out-Null

$excludedPortablePayloadNames = @(".smoke-user-data", "smoke-verification.json")
Get-ChildItem -LiteralPath $PortableRoot -Force | Where-Object { $excludedPortablePayloadNames -notcontains $_.Name } | ForEach-Object {
  Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $payloadRoot $_.Name) -Recurse -Force
}

Set-Content -LiteralPath (Join-Path $stagingRoot "Install-ShopRPA.ps1") -Value (New-InstallScript -Version $version) -Encoding UTF8
Set-Content -LiteralPath (Join-Path $stagingRoot "Install-ShopRPA.cmd") -Value (New-InstallCmd) -Encoding ASCII
Set-Content -LiteralPath (Join-Path $stagingRoot "Uninstall-ShopRPA.ps1") -Value (New-UninstallScript) -Encoding UTF8

$portableHash = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToUpperInvariant()
$packageTimestamp = Get-DeterministicPackageTimestamp
$manifest = [PSCustomObject]@{
  productName = "ShopRPA"
  version = $version
  createdAt = $packageTimestamp.ToString("o")
  portableRoot = $PortableRoot
  pythonArchiveSha256 = $portableHash
  deterministicZipTimestamp = $packageTimestamp.ToString("o")
  entrypoints = @("Install-ShopRPA.cmd", "Install-ShopRPA.ps1", "Uninstall-ShopRPA.ps1", "payload\ShopRPA\ShopRPA.cmd")
}
$manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $stagingRoot "manifest.json") -Encoding UTF8

$readme = @"
ShopRPA $version install package

How to install
1. Extract this zip to a normal folder.
2. Double-click Install-ShopRPA.cmd, or run:
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Install-ShopRPA.ps1 -Force

Default install location
%LOCALAPPDATA%\Programs\ShopRPA

What this installer does
- Copies the verified portable ShopRPA payload into the install folder.
- Creates Desktop and Start Menu shortcuts unless -NoShortcuts is used.
- Writes installed.json and Uninstall-ShopRPA.ps1 into the install folder.

After installation
- Launch ShopRPA from the Desktop or Start Menu shortcut.
- The backend URL is configured in resources\conf.yaml under the install folder.
- The default local gateway is http://127.0.0.1:32742/.
- Main logs are written to %APPDATA%\ShopRPA\logs\main.log.

Validation
- The package payload was created from the verified portable build.
- Python runtime archive SHA-256: $portableHash
- Run from the repository: corepack pnpm run verify:portable-installer
"@
Set-Content -LiteralPath (Join-Path $stagingRoot "README-install.txt") -Value $readme -Encoding UTF8

if (-not (Test-Path -LiteralPath $OutputDir -PathType Container)) {
  New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

$zipPath = Join-Path $OutputDir "ShopRPA-$version-portable-installer.zip"
$hashPath = "$zipPath.sha256.txt"
if (Test-Path -LiteralPath $zipPath -PathType Leaf) {
  Remove-Item -LiteralPath $zipPath -Force
}
if (Test-Path -LiteralPath $hashPath -PathType Leaf) {
  Remove-Item -LiteralPath $hashPath -Force
}

New-DeterministicZip -SourceDirectory $stagingRoot -ZipPath $zipPath -Timestamp $packageTimestamp
$zipHash = (Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash.ToUpperInvariant()
Set-Content -LiteralPath $hashPath -Value $zipHash -Encoding ASCII

Write-Host "Portable installer package ready: $zipPath"
Write-Host "installerSha256=$zipHash"
