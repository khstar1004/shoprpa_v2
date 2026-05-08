const { spawnSync } = require('node:child_process')
const fs = require('node:fs')
const path = require('node:path')

const appRoot = path.resolve(__dirname, '..')
const workspaceRoot = path.resolve(appRoot, '../..')
const pnpmStore = path.join(workspaceRoot, 'node_modules/.pnpm')

function fail(message) {
  console.error('')
  console.error('Windows installer preflight failed.')
  console.error(message)
  console.error('')
  console.error('Portable build is still available:')
  console.error('  corepack pnpm --dir frontend run build:desktop')
  console.error('')
  console.error('To create the NSIS installer, run this command in a normal Windows session')
  console.error('where node.exe is allowed to launch child processes such as app-builder.exe.')
  process.exit(1)
}

function run(command, args, label) {
  const result = spawnSync(command, args, {
    encoding: 'utf8',
    windowsHide: true,
  })
  if (result.error) {
    fail(`${label} could not be launched through Node child_process: ${result.error.message}`)
  }
  if (result.status !== 0) {
    const output = `${result.stdout || ''}${result.stderr || ''}`.trim()
    fail(`${label} exited with ${result.status}${output ? `:\n${output}` : ''}`)
  }
  return result.stdout.trim()
}

function findAppBuilder() {
  if (!fs.existsSync(pnpmStore)) {
    return ''
  }

  for (const entry of fs.readdirSync(pnpmStore)) {
    if (!entry.startsWith('app-builder-bin@')) {
      continue
    }
    const candidate = path.join(pnpmStore, entry, 'node_modules/app-builder-bin/win/x64/app-builder.exe')
    if (fs.existsSync(candidate)) {
      return candidate
    }
  }
  return ''
}

function main() {
  if (process.platform !== 'win32') {
    console.log('Skipping Windows installer host preflight on non-Windows host.')
    return
  }

  run(process.execPath, ['-v'], 'node.exe')

  const appBuilder = findAppBuilder()
  if (!appBuilder) {
    fail(`app-builder.exe was not found under ${pnpmStore}`)
  }

  const version = run(appBuilder, ['--version'], 'app-builder.exe')
  console.log(`Windows installer host preflight passed: app-builder ${version}`)
}

main()
