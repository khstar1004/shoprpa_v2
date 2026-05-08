param(
  [Parameter(Mandatory = $true)]
  [string]$EngineDir,

  [Parameter(Mandatory = $true)]
  [string]$PythonCoreDir
)

$ErrorActionPreference = "Stop"

function Resolve-ExistingPath([string]$PathValue, [string]$Label) {
  if (-not (Test-Path -LiteralPath $PathValue)) {
    throw "$Label not found: $PathValue"
  }
  return (Resolve-Path -LiteralPath $PathValue).Path
}

function Assert-ChildPath([string]$Child, [string]$Parent) {
  $childFull = [System.IO.Path]::GetFullPath($Child)
  $parentFull = [System.IO.Path]::GetFullPath($Parent).TrimEnd(
    [System.IO.Path]::DirectorySeparatorChar,
    [System.IO.Path]::AltDirectorySeparatorChar
  )
  if (-not ($childFull.StartsWith($parentFull + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase))) {
    throw "Refusing to write outside target directory: $childFull"
  }
}

$engineRoot = Resolve-ExistingPath $EngineDir "Engine directory"
$pythonRoot = Resolve-ExistingPath $PythonCoreDir "Python core directory"
$pythonExe = Join-Path $pythonRoot "python.exe"
$targetNamespace = Join-Path $pythonRoot "Lib\site-packages\astronverse"

if (-not (Test-Path -LiteralPath $pythonExe)) {
  throw "Python executable not found: $pythonExe"
}

New-Item -ItemType Directory -Force -Path $targetNamespace | Out-Null
$targetNamespace = (Resolve-Path -LiteralPath $targetNamespace).Path

$sourceEntries = @("shared", "servers", "components") |
  ForEach-Object { Join-Path $engineRoot $_ } |
  Where-Object { Test-Path -LiteralPath $_ } |
  ForEach-Object {
    Get-ChildItem -LiteralPath $_ -Directory | ForEach-Object {
      $sourceNamespace = Join-Path $_.FullName "src\astronverse"
      if (Test-Path -LiteralPath $sourceNamespace) {
        Get-ChildItem -LiteralPath $sourceNamespace -Force
      }
    }
  }

$copied = 0
foreach ($source in $sourceEntries) {
  if ($source.Name -in @(".", "..", "")) {
    throw "Invalid source package name: $($source.FullName)"
  }

  $target = Join-Path $targetNamespace $source.Name
  Assert-ChildPath $target $targetNamespace

  if (Test-Path -LiteralPath $target) {
    Remove-Item -LiteralPath $target -Recurse -Force
  }

  Copy-Item -LiteralPath $source.FullName -Destination $targetNamespace -Recurse -Force
  $copied += 1
}

if ($copied -eq 0) {
  throw "No engine source packages were found under $engineRoot"
}

& $pythonExe -m compileall -q $targetNamespace
if ($LASTEXITCODE -ne 0) {
  throw "Python compileall failed for overlaid engine sources"
}

Write-Host "Offline engine source overlay completed: $copied packages copied to $targetNamespace"
