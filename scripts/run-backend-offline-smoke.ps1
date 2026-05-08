param()

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $PSCommandPath
$repoRoot = (Resolve-Path (Join-Path $scriptDir "..")).Path

$env:SHOPRPA_VERIFY_BACKEND_OFFLINE_SMOKE = "1"
try {
  Push-Location -LiteralPath $repoRoot
  corepack pnpm --dir frontend --filter shoprpa run verify:portable
  $exitCode = $LASTEXITCODE
}
finally {
  Pop-Location
  Remove-Item Env:\SHOPRPA_VERIFY_BACKEND_OFFLINE_SMOKE -ErrorAction SilentlyContinue
}

if ($exitCode -ne 0) {
  exit $exitCode
}
