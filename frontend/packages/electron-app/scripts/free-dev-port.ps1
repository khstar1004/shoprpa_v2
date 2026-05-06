param(
  [int]$Port = $(if ($env:SHOPRPA_DEV_PORT) { [int]$env:SHOPRPA_DEV_PORT } elseif ($env:PORT) { [int]$env:PORT } else { 1420 })
)

$ErrorActionPreference = 'Stop'

function Get-ListeningPids {
  param([int]$TargetPort)

  $lines = netstat -ano -p tcp | Select-String -Pattern "LISTENING"
  $pids = New-Object System.Collections.Generic.HashSet[int]

  foreach ($line in $lines) {
    $parts = ($line.ToString().Trim() -split '\s+')
    if ($parts.Length -lt 5) {
      continue
    }

    $localAddress = $parts[1]
    $pidText = $parts[4]

    if (-not $localAddress.EndsWith(":$TargetPort")) {
      continue
    }

    $processId = 0
    if ([int]::TryParse($pidText, [ref]$processId) -and $processId -ne $PID) {
      [void]$pids.Add($processId)
    }
  }

  return @($pids)
}

$pids = @(Get-ListeningPids -TargetPort $Port)

if ($pids.Count -eq 0) {
  Write-Host "[dev-port] port $Port is free"
  exit 0
}

Write-Host "[dev-port] releasing port $Port from PID(s): $($pids -join ', ')"

foreach ($processId in $pids) {
  Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
}

Start-Sleep -Milliseconds 500

$remainingPids = @(Get-ListeningPids -TargetPort $Port)
if ($remainingPids.Count -gt 0) {
  foreach ($processId in $remainingPids) {
    taskkill /PID $processId /T /F | Out-Null
  }
}

Start-Sleep -Milliseconds 300

$remainingPids = @(Get-ListeningPids -TargetPort $Port)
if ($remainingPids.Count -gt 0) {
  Write-Error "[dev-port] port $Port is still busy: $($remainingPids -join ', ')"
  exit 1
}

Write-Host "[dev-port] port $Port is ready"
