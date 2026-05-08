param(
  [string]$PortableRoot = "",
  [string]$Output = "",
  [Parameter(ValueFromRemainingArguments = $true)]
  [string[]]$RemainingArgs
)

$ErrorActionPreference = "Stop"

function Resolve-RepoRoot {
  $scriptDir = Split-Path -Parent $PSCommandPath
  return (Resolve-Path (Join-Path $scriptDir "..")).Path
}

$repoRoot = Resolve-RepoRoot
if (-not $PortableRoot) {
  $PortableRoot = Join-Path $repoRoot "frontend\packages\electron-app\dist\win-portable"
}
if (-not [System.IO.Path]::IsPathRooted($PortableRoot)) {
  $PortableRoot = Join-Path $repoRoot $PortableRoot
}

$launcher = Join-Path $PortableRoot "ShopRPA.cmd"
if (-not (Test-Path -LiteralPath $launcher -PathType Leaf)) {
  throw "Portable launcher was not found: $launcher"
}

if ($Output) {
  if (-not [System.IO.Path]::IsPathRooted($Output)) {
    $Output = Join-Path $repoRoot $Output
  }
  $outputDir = Split-Path -Parent $Output
  if ($outputDir -and -not (Test-Path -LiteralPath $outputDir -PathType Container)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
  }
}

$previousSmoke = $env:SHOPRPA_SMOKE_TEST
$previousOutput = $env:SHOPRPA_SMOKE_OUTPUT
try {
  $env:SHOPRPA_SMOKE_TEST = "1"
  if ($Output) {
    $env:SHOPRPA_SMOKE_OUTPUT = $Output
  }
  elseif ($null -ne $env:SHOPRPA_SMOKE_OUTPUT) {
    Remove-Item Env:\SHOPRPA_SMOKE_OUTPUT -ErrorAction SilentlyContinue
  }

  & $launcher @RemainingArgs
  if ($LASTEXITCODE -ne 0) {
    throw "Portable smoke failed with exit code $LASTEXITCODE."
  }
}
finally {
  if ($null -eq $previousSmoke) {
    Remove-Item Env:\SHOPRPA_SMOKE_TEST -ErrorAction SilentlyContinue
  }
  else {
    $env:SHOPRPA_SMOKE_TEST = $previousSmoke
  }

  if ($null -eq $previousOutput) {
    Remove-Item Env:\SHOPRPA_SMOKE_OUTPUT -ErrorAction SilentlyContinue
  }
  else {
    $env:SHOPRPA_SMOKE_OUTPUT = $previousOutput
  }
}
