const { spawnSync } = require('node:child_process')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..')
const powershell = path.join(
  process.env.SystemRoot || 'C:\\Windows',
  'System32',
  'WindowsPowerShell',
  'v1.0',
  'powershell.exe',
)
const scriptPath = path.join(__dirname, 'download-missing-python-backend-wheels.ps1')

const forwardedArgs = process.argv.slice(2).filter((arg) => arg !== '--')

const result = spawnSync(
  powershell,
  [
    '-NoProfile',
    '-ExecutionPolicy',
    'Bypass',
    '-File',
    scriptPath,
    ...forwardedArgs,
  ],
  {
    cwd: repoRoot,
    stdio: 'inherit',
    windowsHide: true,
  },
)

if (result.error) {
  console.error(result.error.message)
  process.exit(1)
}

process.exit(result.status ?? 1)
