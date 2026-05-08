const fs = require('node:fs')
const path = require('node:path')

const asar = require('@electron/asar')

const appRoot = path.resolve(__dirname, '..')
const workspaceRoot = path.resolve(appRoot, '../..')
const repoRoot = path.resolve(workspaceRoot, '..')
const manualElectronDist = path.resolve(workspaceRoot, 'node_modules/.manual/electron/dist')
const electronDist = fs.existsSync(manualElectronDist)
  ? manualElectronDist
  : path.resolve(workspaceRoot, 'node_modules/electron/dist')
const externalResources = path.resolve(repoRoot, 'resources')
const outputRoot = path.resolve(appRoot, 'dist/win-portable')
const outputResources = path.join(outputRoot, 'resources')
const outputApp = path.join(outputResources, 'app')
const outputElectronRuntime = path.join(outputRoot, 'runtime/node_modules/electron/dist')

const copyOptions = {
  dereference: true,
  force: true,
  recursive: true,
}

function copyRequired(source, target, label) {
  if (!fs.existsSync(source)) {
    throw new Error(`${label} not found: ${source}`)
  }
  fs.cpSync(source, target, copyOptions)
}

function copyIfExists(source, target) {
  if (fs.existsSync(source)) {
    fs.cpSync(source, target, copyOptions)
  }
}

function copyRuntimeDependencies(packageJson) {
  const dependencies = Object.keys(packageJson.dependencies || {})
  const targetNodeModules = path.join(outputApp, 'node_modules')
  fs.mkdirSync(targetNodeModules, { recursive: true })

  for (const dependency of dependencies) {
    copyNodePackage(dependency, [appRoot], targetNodeModules)
  }
}

function getPackageTarget(targetNodeModules, packageName) {
  return path.join(targetNodeModules, ...packageName.split('/'))
}

function resolvePackageRoot(packageName, resolvePaths) {
  const packageJsonPath = require.resolve(`${packageName}/package.json`, {
    paths: resolvePaths,
  })
  return path.dirname(packageJsonPath)
}

function readPackageJson(packageRoot) {
  return JSON.parse(fs.readFileSync(path.join(packageRoot, 'package.json'), 'utf8'))
}

function copyNodePackage(packageName, resolvePaths, targetNodeModules, copied = new Set()) {
  const packageRoot = resolvePackageRoot(packageName, resolvePaths)
  const packageJson = readPackageJson(packageRoot)
  const targetRoot = getPackageTarget(targetNodeModules, packageName)
  const copyKey = `${targetRoot}|${packageRoot}`

  if (!copied.has(copyKey)) {
    copyRequired(packageRoot, targetRoot, `runtime dependency ${packageName}`)
    copied.add(copyKey)
  }

  const nestedNodeModules = path.join(targetRoot, 'node_modules')
  const requiredDependencies = Object.keys(packageJson.dependencies || {})
  const optionalDependencies = Object.keys(packageJson.optionalDependencies || {})

  for (const dependency of requiredDependencies) {
    copyNodePackage(dependency, [packageRoot, appRoot], nestedNodeModules, copied)
  }

  for (const dependency of optionalDependencies) {
    try {
      copyNodePackage(dependency, [packageRoot, appRoot], nestedNodeModules, copied)
    }
    catch (error) {
      if (error.code !== 'MODULE_NOT_FOUND') {
        throw error
      }
    }
  }
}

function copyPackageJson(packageJson) {
  const runtimePackageJson = {
    author: packageJson.author,
    dependencies: packageJson.dependencies,
    description: packageJson.description,
    homepage: packageJson.homepage,
    main: packageJson.main,
    name: packageJson.name,
    productName: 'ShopRPA',
    version: packageJson.version,
  }

  fs.writeFileSync(
    path.join(outputApp, 'package.json'),
    `${JSON.stringify(runtimePackageJson, null, 2)}\n`,
  )
}

async function packAppAsar() {
  const appAsar = path.join(outputResources, 'app.asar')
  await asar.createPackage(outputApp, appAsar)
  fs.rmSync(outputApp, { force: true, recursive: true })
}

function writePortableReadme(packageJson) {
  const lines = [
    `ShopRPA ${packageJson.version} portable build`,
    '',
    'How to run',
    '1. Keep this folder intact. Do not move ShopRPA.cmd away from the resources or runtime folders.',
    '2. Double-click ShopRPA.cmd, or run it from PowerShell:',
    '   .\\ShopRPA.cmd',
    '3. On first launch, ShopRPA extracts the bundled Python runtime into the app user-data folder.',
    '',
    'Backend connection',
    '- The default backend URL is configured in resources/conf.yaml.',
    '- The default local development gateway is http://127.0.0.1:32742/.',
    '- Before handing the package to another machine, update resources/conf.yaml if the server is remote.',
    '- If login or API calls fail, check that the configured server is reachable and then inspect the logs below.',
    '',
    'What is included',
    '- ShopRPA.cmd: portable desktop launcher.',
    '- resources/app.asar: desktop application payload.',
    '- resources/renderer: packaged renderer files loaded by the desktop shell.',
    '- resources/conf.yaml: runtime connection configuration.',
    '- resources/python_core.7z: bundled Python automation runtime.',
    '- resources/python_core.7z.sha256.txt: Python runtime integrity hash.',
    '- resources/7zr.exe: archive extractor used by the app.',
    '- runtime/node_modules/electron/dist: bundled Electron runtime.',
    '',
    'Diagnostics',
    '- Main log file: %APPDATA%\\ShopRPA\\logs\\main.log',
    '- Smoke-test log file after verification: .smoke-user-data\\logs\\main.log',
    '- Repo verification command: corepack pnpm run verify:portable:host',
    '',
    'Package boundary',
    '- This is a portable desktop package, not an NSIS installer.',
    '- Keep resources/conf.yaml with the package when copying it to another machine.',
    '- The package is self-contained for the desktop client and bundled Python runtime; external services configured in conf.yaml must still be reachable.',
    '',
  ]
  fs.writeFileSync(path.join(outputRoot, 'README-portable.txt'), lines.join('\n'))
}

function writePortableLauncher() {
  const lines = [
    '@echo off',
    'setlocal',
    'set "APP_DIR=%~dp0"',
    'set "ELECTRON_EXE=%APP_DIR%runtime\\node_modules\\electron\\dist\\electron.exe"',
    'set "APP_ASAR=%APP_DIR%resources\\app.asar"',
    'if not exist "%ELECTRON_EXE%" (',
    '  echo ShopRPA launch failed: Electron runtime is missing.',
    '  echo Expected: "%ELECTRON_EXE%"',
    '  exit /b 1',
    ')',
    'if not exist "%APP_ASAR%" (',
    '  echo ShopRPA launch failed: application payload is missing.',
    '  echo Expected: "%APP_ASAR%"',
    '  exit /b 1',
    ')',
    'pushd "%APP_DIR%" >nul',
    'set "SHOPRPA_ELECTRON_FLAGS="',
    'if "%SHOPRPA_SMOKE_TEST%"=="1" set "ELECTRON_DISABLE_SANDBOX=1"',
    'if "%SHOPRPA_SMOKE_TEST%"=="1" set "SHOPRPA_ELECTRON_FLAGS=--no-sandbox --disable-gpu --disable-gpu-sandbox --disable-features=NetworkServiceSandbox,RendererAppContainer"',
    'if "%SHOPRPA_SMOKE_OUTPUT%"=="" (',
    '  "%ELECTRON_EXE%" %SHOPRPA_ELECTRON_FLAGS% "%APP_ASAR%" %*',
    ') else (',
    '  "%ELECTRON_EXE%" %SHOPRPA_ELECTRON_FLAGS% "%APP_ASAR%" %* > "%SHOPRPA_SMOKE_OUTPUT%" 2>&1',
    ')',
    'set "SHOPRPA_EXIT=%ERRORLEVEL%"',
    'popd >nul',
    'if not "%SHOPRPA_EXIT%"=="0" (',
    '  echo ShopRPA exited with code %SHOPRPA_EXIT%.',
    '  echo Check %APPDATA%\\ShopRPA\\logs\\main.log for details.',
    ')',
    'exit /b %SHOPRPA_EXIT%',
    '',
  ]
  fs.writeFileSync(path.join(outputRoot, 'ShopRPA.cmd'), lines.join('\r\n'))
}

async function main() {
  const packageJson = JSON.parse(fs.readFileSync(path.join(appRoot, 'package.json'), 'utf8'))

  fs.rmSync(outputRoot, { force: true, recursive: true })
  fs.mkdirSync(outputRoot, { recursive: true })

  copyRequired(electronDist, outputElectronRuntime, 'Electron runtime')
  copyIfExists(
    path.resolve(workspaceRoot, 'node_modules/.manual/electron/package.json'),
    path.join(outputRoot, 'runtime/node_modules/electron/package.json'),
  )
  copyIfExists(
    path.resolve(workspaceRoot, 'node_modules/.manual/electron/index.d.ts'),
    path.join(outputRoot, 'runtime/node_modules/electron/index.d.ts'),
  )

  copyRequired(externalResources, outputResources, 'backend resources')
  fs.mkdirSync(outputApp, { recursive: true })
  copyRequired(path.join(appRoot, 'out'), path.join(outputApp, 'out'), 'built Electron app')
  copyRequired(path.join(appRoot, 'out/renderer'), path.join(outputResources, 'renderer'), 'built renderer files')
  copyIfExists(path.join(appRoot, 'extensions'), path.join(outputApp, 'extensions'))
  copyPackageJson(packageJson)
  copyRuntimeDependencies(packageJson)
  await packAppAsar()
  writePortableLauncher()
  writePortableReadme(packageJson)

  console.log(`Portable build ready: ${outputRoot}`)
}

main().catch((error) => {
  console.error('Portable build failed:', error)
  process.exit(1)
})
