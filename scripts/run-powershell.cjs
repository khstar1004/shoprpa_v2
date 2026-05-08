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

const scriptArg = process.argv[2]
if (!scriptArg) {
  console.error('Usage: node scripts/run-powershell.cjs <script.ps1> [args...]')
  process.exit(1)
}

const scriptPath = path.isAbsolute(scriptArg)
  ? scriptArg
  : path.join(repoRoot, scriptArg)
const forwardedArgs = process.argv.slice(3).filter((arg) => arg !== '--')

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
