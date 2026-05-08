param(
  [string]$PortableRoot = ""
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

  $item = Get-Item -LiteralPath $Path
  if ($item.Length -le 0) {
    throw "$Label is empty: $Path"
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

function Invoke-SmokeTest {
  param(
    [string]$LauncherPath,
    [string]$ResultPath,
    [string]$UserDataPath,
    [string]$Label
  )

  if (Test-Path -LiteralPath $ResultPath) {
    Remove-Item -LiteralPath $ResultPath -Force
  }
  if (Test-Path -LiteralPath $UserDataPath) {
    Remove-Item -LiteralPath $UserDataPath -Recurse -Force
  }

  $oldSmokeTest = $env:SHOPRPA_SMOKE_TEST
  $oldSmokeResult = $env:SHOPRPA_SMOKE_RESULT
  $oldSmokeUserData = $env:SHOPRPA_SMOKE_USER_DATA
  try {
    $env:SHOPRPA_SMOKE_TEST = "1"
    $env:SHOPRPA_SMOKE_RESULT = $ResultPath
    $env:SHOPRPA_SMOKE_USER_DATA = $UserDataPath
    Invoke-Checked { & $LauncherPath | Out-Host } $Label
  }
  finally {
    $env:SHOPRPA_SMOKE_TEST = $oldSmokeTest
    $env:SHOPRPA_SMOKE_RESULT = $oldSmokeResult
    $env:SHOPRPA_SMOKE_USER_DATA = $oldSmokeUserData
  }
}

function Assert-SmokeResult {
  param(
    [string]$ResultPath,
    [string]$Label
  )

  Assert-File $ResultPath $Label
  $result = Get-Content -LiteralPath $ResultPath -Raw | ConvertFrom-Json
  if ($result.ok -ne $true) {
    throw "$Label reported failure: $($result.error)"
  }
  if ([string]::IsNullOrWhiteSpace([string]$result.generatedAt)) {
    throw "$Label is missing generatedAt"
  }
  try {
    $generatedAt = [DateTimeOffset]::Parse([string]$result.generatedAt)
  }
  catch {
    throw "$Label generatedAt is invalid: $($result.generatedAt)"
  }
  if ($generatedAt -gt [DateTimeOffset]::Now.AddMinutes(10)) {
    throw "$Label generatedAt is in the future: $($result.generatedAt)"
  }
  if ($result.packagedRuntime -ne $true) {
    throw "$Label did not detect packaged runtime"
  }
  if (-not ([string]$result.appPath).EndsWith("resources\app.asar")) {
    throw "$Label loaded the wrong app path: $($result.appPath)"
  }
  $rendererPath = [string]$result.rendererPath
  if (-not ($rendererPath.Contains("resources\renderer") -or $rendererPath.Contains("resources\app.asar\out\renderer"))) {
    throw "$Label loaded the wrong renderer path: $($result.rendererPath)"
  }
}

$repoRoot = Resolve-RepoRoot
if ([string]::IsNullOrWhiteSpace($PortableRoot)) {
  $PortableRoot = Join-Path $repoRoot "frontend\packages\electron-app\dist\win-portable"
}

$PortableRoot = (Resolve-Path -LiteralPath $PortableRoot).Path
$resources = Join-Path $PortableRoot "resources"
$archive = Join-Path $resources "python_core.7z"
$archiveHash = Join-Path $resources "python_core.7z.sha256.txt"
$sevenZip = Join-Path $resources "7zr.exe"
$launcher = Join-Path $PortableRoot "ShopRPA.cmd"
$appAsar = Join-Path $resources "app.asar"
$conf = Join-Path $resources "conf.yaml"
$rendererBoot = Join-Path $resources "renderer\boot.html"
$rendererIndex = Join-Path $resources "renderer\index.html"
$electronExe = Join-Path $PortableRoot "runtime\node_modules\electron\dist\electron.exe"
$smokeResult = Join-Path $PortableRoot "smoke-verification.json"
$smokeUserData = Join-Path $PortableRoot ".smoke-user-data"

Write-Host "Verifying portable package: $PortableRoot"

Assert-File $launcher "Portable launcher"
Assert-File $appAsar "Electron app archive"
Assert-File $conf "Runtime config"
Assert-File $rendererBoot "Renderer boot page"
Assert-File $rendererIndex "Renderer index page"
Assert-File $sevenZip "Bundled 7-Zip"
Assert-File $archive "Python runtime archive"
Assert-File $archiveHash "Python runtime hash"
Assert-File $electronExe "Electron runtime"

$expectedHash = (Get-Content -LiteralPath $archiveHash -Raw).Trim().ToUpperInvariant()
$actualHash = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToUpperInvariant()
if ([string]::IsNullOrWhiteSpace($expectedHash)) {
  throw "Python runtime hash file is empty: $archiveHash"
}
if ($expectedHash -ne $actualHash) {
  throw "Python runtime hash mismatch: expected $expectedHash, got $actualHash"
}

Invoke-Checked { & $sevenZip t $archive | Out-Host } "Python archive integrity test"

$listing = & $sevenZip l -ba $archive
if ($LASTEXITCODE -ne 0) {
  throw "Python archive listing failed with exit code $LASTEXITCODE"
}
if (($listing -join "`n") -notmatch "(^|\s)python\.exe(\s|$)") {
  throw "Python archive does not contain python.exe"
}

$requiredArchiveEntries = @(
  "Lib\site-packages\astronverse\browser_bridge\inject\backgroundInject.js",
  "Lib\site-packages\astronverse\browser_bridge\inject\contentInject.js"
)
$listingText = $listing -join "`n"
foreach ($entry in $requiredArchiveEntries) {
  if ($listingText -notmatch [regex]::Escape($entry)) {
    throw "Python archive does not contain required browser bridge inject file: $entry"
  }
}

Invoke-SmokeTest -LauncherPath $launcher -ResultPath $smokeResult -UserDataPath $smokeUserData -Label "Portable smoke test"
Assert-SmokeResult -ResultPath $smokeResult -Label "Portable smoke result"

$buildRoot = Join-Path $repoRoot "build"
$isolationRoot = Join-Path $buildRoot "portable-host-isolation"
$isolatedPortableRoot = Join-Path $isolationRoot "ShopRPA"
if (-not (Test-Path -LiteralPath $buildRoot -PathType Container)) {
  New-Item -ItemType Directory -Path $buildRoot | Out-Null
}
Remove-SafeDirectory -BasePath $buildRoot -TargetPath $isolationRoot
New-Item -ItemType Directory -Path $isolatedPortableRoot | Out-Null
Get-ChildItem -LiteralPath $PortableRoot -Force | Where-Object { $_.Name -ne ".smoke-user-data" } | ForEach-Object {
  Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $isolatedPortableRoot $_.Name) -Recurse -Force
}

$isolatedSmokeResult = Join-Path $isolationRoot "smoke-verification.json"
$isolatedSmokeUserData = Join-Path $isolationRoot "smoke-user-data"
$isolatedLauncher = Join-Path $isolatedPortableRoot "ShopRPA.cmd"
Invoke-SmokeTest -LauncherPath $isolatedLauncher -ResultPath $isolatedSmokeResult -UserDataPath $isolatedSmokeUserData -Label "Isolated portable smoke test"
Assert-SmokeResult -ResultPath $isolatedSmokeResult -Label "Isolated portable smoke result"

Write-Host "Portable host verification passed"
Write-Host "archiveSha256=$actualHash"
