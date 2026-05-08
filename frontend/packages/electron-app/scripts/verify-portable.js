const { spawn, spawnSync } = require('node:child_process')
const crypto = require('node:crypto')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')

const asar = require('@electron/asar')

const appRoot = path.resolve(__dirname, '..')
const repoRoot = path.resolve(appRoot, '../../..')
const portableRoot = path.join(appRoot, 'dist/win-portable')
const smokeResultPath = path.join(portableRoot, 'smoke-verification.json')
const smokeUserDataPath = path.join(os.tmpdir(), `shoprpa-portable-smoke-${process.pid}`)
const rendererSmokeRoot = path.join(repoRoot, 'build/portable-renderer-smoke')
const rendererSmokeResultPath = path.join(rendererSmokeRoot, 'renderer-smoke-verification.json')
const rendererSmokeUserDataPath = path.join(rendererSmokeRoot, 'user-data')
const rendererSmokeScreenshotPath = path.join(rendererSmokeRoot, 'renderer-smoke.png')
const rendererSmokeOutputPath = path.join(rendererSmokeRoot, 'renderer-smoke-child-output.log')
const workflowEditorSmokeRoot = path.join(repoRoot, 'build/workflow-editor-smoke')
const workflowEditorSmokeResultPath = path.join(workflowEditorSmokeRoot, 'workflow-editor-smoke-verification.json')
const workflowEditorSmokeUserDataPath = path.join(workflowEditorSmokeRoot, 'user-data')
const workflowEditorSmokeScreenshotPath = path.join(workflowEditorSmokeRoot, 'workflow-editor-smoke.png')
const workflowEditorSmokeOutputPath = path.join(workflowEditorSmokeRoot, 'workflow-editor-smoke-child-output.log')
const backendOfflineSmokeRoot = path.join(repoRoot, 'build/backend-offline-smoke')
const backendOfflineSmokeResultPath = path.join(backendOfflineSmokeRoot, 'backend-offline-smoke-verification.json')
const backendOfflineSmokeUserDataPath = path.join(backendOfflineSmokeRoot, 'user-data')
const backendOfflineSmokeScreenshotPath = path.join(backendOfflineSmokeRoot, 'backend-offline-smoke.png')
const backendOfflineSmokeOutputPath = path.join(backendOfflineSmokeRoot, 'backend-offline-smoke-child-output.log')

const requiredFiles = [
  'ShopRPA.cmd',
  'README-portable.txt',
  'resources/app.asar',
  'resources/conf.yaml',
  'resources/renderer/boot.html',
  'resources/renderer/index.html',
  'resources/7zr.exe',
  'resources/python_core.7z',
  'resources/python_core.7z.sha256.txt',
  'runtime/node_modules/electron/dist/electron.exe',
]

const requiredAsarFiles = [
  'out/main/index.js',
  'out/preload/index.js',
  'out/renderer/index.html',
  'out/renderer/boot.html',
  'package.json',
]

function assertFile(relativePath) {
  const fullPath = path.join(portableRoot, relativePath)
  if (!fs.existsSync(fullPath)) {
    throw new Error(`Missing portable artifact: ${relativePath}`)
  }
  const stat = fs.statSync(fullPath)
  if (!stat.isFile()) {
    throw new Error(`Portable artifact is not a file: ${relativePath}`)
  }
  if (stat.size <= 0) {
    throw new Error(`Portable artifact is empty: ${relativePath}`)
  }
}

function assertAsarContents() {
  const appAsar = path.join(portableRoot, 'resources/app.asar')
  const files = new Set(asar.listPackage(appAsar).map(file => file.replace(/\\/g, '/').replace(/^\/+/, '')))
  for (const file of requiredAsarFiles) {
    if (!files.has(file)) {
      throw new Error(`Missing file inside app.asar: ${file}`)
    }
  }
}

function assertPythonArchiveHash() {
  const archivePath = path.join(portableRoot, 'resources/python_core.7z')
  const hashPath = path.join(portableRoot, 'resources/python_core.7z.sha256.txt')
  const expected = fs.readFileSync(hashPath, 'utf8').trim().toUpperCase()
  const actual = crypto.createHash('sha256').update(fs.readFileSync(archivePath)).digest('hex').toUpperCase()
  if (!expected) {
    throw new Error('Python archive hash file is empty')
  }
  if (actual !== expected) {
    throw new Error(`Python archive hash mismatch: expected ${expected}, got ${actual}`)
  }
}

function assertPythonArchiveMatchesSource() {
  const sourceArchivePath = path.resolve(appRoot, '../../..', 'resources/python_core.7z')
  const sourceHashPath = path.resolve(appRoot, '../../..', 'resources/python_core.7z.sha256.txt')
  const archivePath = path.join(portableRoot, 'resources/python_core.7z')
  if (!fs.existsSync(sourceArchivePath) || !fs.existsSync(sourceHashPath)) {
    return
  }

  const sourceExpected = fs.readFileSync(sourceHashPath, 'utf8').trim().toUpperCase()
  const sourceActual = crypto.createHash('sha256').update(fs.readFileSync(sourceArchivePath)).digest('hex').toUpperCase()
  const portableActual = crypto.createHash('sha256').update(fs.readFileSync(archivePath)).digest('hex').toUpperCase()
  if (!sourceExpected) {
    throw new Error('Source Python archive hash file is empty')
  }
  if (sourceActual !== sourceExpected) {
    throw new Error(`Source Python archive hash mismatch: expected ${sourceExpected}, got ${sourceActual}`)
  }
  if (portableActual !== sourceActual) {
    throw new Error(`Portable Python archive differs from source resources archive: source ${sourceActual}, portable ${portableActual}`)
  }
}

function runSevenZip(args, label) {
  const sevenZipPath = path.join(portableRoot, 'resources/7zr.exe')
  const result = spawnSync(sevenZipPath, args, {
    cwd: portableRoot,
    encoding: 'utf8',
    maxBuffer: 64 * 1024 * 1024,
    windowsHide: true,
  })
  if (result.error) {
    if (result.error.code === 'EPERM') {
      console.warn(`${label} skipped because this environment blocked running 7zr.exe: ${result.error.message}`)
      return null
    }
    throw result.error
  }
  if (result.status !== 0) {
    const output = `${result.stdout || ''}${result.stderr || ''}`.trim()
    throw new Error(`${label} failed${output ? `: ${output}` : ''}`)
  }
  return `${result.stdout || ''}${result.stderr || ''}`
}

function assertPythonArchiveReadable() {
  const archivePath = path.join(portableRoot, 'resources/python_core.7z')
  const testResult = runSevenZip(['t', archivePath], 'Python archive integrity test')
  if (testResult === null)
    return
  const listing = runSevenZip(['l', '-ba', archivePath], 'Python archive listing')
  if (listing === null)
    return
  if (!/(^|\s)python\.exe(\r?\n|$)/m.test(listing)) {
    throw new Error('Python archive does not contain python.exe')
  }
  const requiredArchiveEntries = [
    'Lib\\site-packages\\astronverse\\browser_bridge\\inject\\backgroundInject.js',
    'Lib\\site-packages\\astronverse\\browser_bridge\\inject\\contentInject.js',
  ]
  for (const entry of requiredArchiveEntries) {
    if (!listing.includes(entry)) {
      throw new Error(`Python archive does not contain required browser bridge inject file: ${entry}`)
    }
  }
}

function assertBackendConfig() {
  const configPath = path.join(portableRoot, 'resources/conf.yaml')
  const config = fs.readFileSync(configPath, 'utf8')
  const remoteAddr = config.match(/remote_addr:\s*(.+)/)?.[1]?.trim()
  if (!remoteAddr) {
    throw new Error('conf.yaml is missing remote_addr')
  }
  try {
    new URL(remoteAddr)
  }
  catch {
    throw new Error(`conf.yaml remote_addr is not a valid URL: ${remoteAddr}`)
  }
}

function assertSmokeTimestamp(result) {
  if (!result.generatedAt) {
    throw new Error('Portable smoke test result is missing generatedAt')
  }
  const generatedAt = new Date(result.generatedAt)
  if (Number.isNaN(generatedAt.getTime())) {
    throw new Error(`Portable smoke test result has invalid generatedAt: ${result.generatedAt}`)
  }
  if (generatedAt.getTime() > Date.now() + 10 * 60 * 1000) {
    throw new Error(`Portable smoke test result generatedAt is in the future: ${result.generatedAt}`)
  }
}

function runSmokeTest() {
  return new Promise((resolve, reject) => {
    fs.rmSync(smokeResultPath, { force: true })
    fs.rmSync(smokeUserDataPath, { force: true, recursive: true })

    const child = spawn(
      path.join(portableRoot, 'ShopRPA.cmd'),
      [],
      {
        cwd: portableRoot,
        env: {
          ...process.env,
          SHOPRPA_SMOKE_RESULT: smokeResultPath,
          SHOPRPA_SMOKE_TEST: '1',
          SHOPRPA_SMOKE_USER_DATA: smokeUserDataPath,
        },
        shell: true,
        stdio: 'ignore',
        windowsHide: true,
      },
    )

    const timeout = setTimeout(() => {
      child.kill()
      reject(new Error('Portable smoke test timed out'))
    }, 30000)

    child.on('error', (error) => {
      clearTimeout(timeout)
      reject(error)
    })

    child.on('exit', () => {
      clearTimeout(timeout)
      if (!fs.existsSync(smokeResultPath)) {
        reject(new Error('Portable smoke test did not write a result file'))
        return
      }

      const result = JSON.parse(fs.readFileSync(smokeResultPath, 'utf8'))
      if (!result.ok) {
        reject(new Error(`Portable smoke test failed: ${result.error || 'unknown error'}`))
        return
      }
      try {
        assertSmokeTimestamp(result)
      }
      catch (error) {
        reject(error)
        return
      }
      if (!String(result.appPath || '').endsWith('resources\\app.asar')) {
        reject(new Error(`Portable smoke test loaded the wrong app path: ${result.appPath}`))
        return
      }
      const rendererPath = String(result.rendererPath || '')
      if (!rendererPath.includes('resources\\renderer') && !rendererPath.includes('resources\\app.asar\\out\\renderer')) {
        reject(new Error(`Portable smoke test loaded the wrong renderer path: ${result.rendererPath}`))
        return
      }
      if (result.packagedRuntime !== true) {
        reject(new Error('Portable smoke test did not detect the packaged runtime layout'))
        return
      }
      if (!result.appWorkPath || String(result.appWorkPath).includes('app.asar')) {
        reject(new Error(`Portable smoke test returned an invalid app work path: ${result.appWorkPath}`))
        return
      }

      resolve(result)
    })
  })
}

function extractHostPolicyEvidence(output) {
  const lines = String(output || '')
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(line => /platform_channel\.cc|network_sandbox\.cc|network_service_instance|RendererAppContainer|spawn EPERM|EPERM|Access is denied|액세스가 거부/.test(line))
  return lines.slice(0, 12)
}

function getRendererSmokeFailureDiagnostics(existingResult, errorMessage) {
  const events = Array.isArray(existingResult.rpaProtocolSmokeEvents)
    ? existingResult.rpaProtocolSmokeEvents.map(String)
    : []
  const protocolServed = events.some(event => /^response:rpa:\/\/localhost\/boot\.html->200:/i.test(event))
  const navigationTimedOut = /Renderer smoke initial navigation timed out/i.test(String(errorMessage || existingResult.error || ''))
  const navigationEvents = Array.isArray(existingResult.rendererNavigationEvents)
    ? existingResult.rendererNavigationEvents.map(String)
    : []
  const navigationCommitted = navigationEvents.some(event => event.startsWith('did-commit-navigation:'))
  const fileFallbackAttempted = navigationEvents.some(event => event.startsWith('fallback-navigation:file://'))
  const fileNavigationStarted = navigationEvents.some(event => event.startsWith('did-start-navigation:file://'))
  const fileNavigationCommitted = navigationEvents.some(event => event.startsWith('did-commit-navigation:file://'))
  const commitBlocked = (protocolServed || fileNavigationStarted) && navigationTimedOut && !navigationCommitted
  const failureClass = fileFallbackAttempted && fileNavigationStarted && !fileNavigationCommitted
    ? 'renderer-main-frame-navigation-blocked'
    : commitBlocked
      ? 'protocol-served-main-frame-not-committed'
      : 'renderer-smoke-failed'

  return {
    rendererCommitBlocked: commitBlocked,
    rendererFailureClass: failureClass,
    rendererFileFallbackAttempted: fileFallbackAttempted,
    rendererFileNavigationCommitted: fileNavigationCommitted,
    rendererNavigationCommitted: navigationCommitted,
    rendererProtocolServed: protocolServed,
  }
}

function writeRendererSmokeFailure(error, extra = {}) {
  const childOutput = String(extra.childOutput || '')
  const hostPolicyEvidence = extractHostPolicyEvidence(childOutput)
  const existingResult = extra.existingResult && typeof extra.existingResult === 'object'
    ? extra.existingResult
    : {}
  const errorMessage = error instanceof Error ? error.message : String(error)
  const diagnostics = getRendererSmokeFailureDiagnostics(existingResult, errorMessage)
  fs.mkdirSync(rendererSmokeRoot, { recursive: true })
  fs.writeFileSync(rendererSmokeResultPath, JSON.stringify({
    ...existingResult,
    error: errorMessage,
    generatedAt: new Date().toISOString(),
    hostPolicyBlocked: hostPolicyEvidence.length > 0,
    hostPolicyEvidence,
    ok: false,
    rendererSmoke: true,
    childOutputPath: fs.existsSync(rendererSmokeOutputPath) ? rendererSmokeOutputPath : undefined,
    ...diagnostics,
    screenshotPath: rendererSmokeScreenshotPath,
  }, null, 2))
}

function readRendererSmokeResult() {
  if (!fs.existsSync(rendererSmokeResultPath)) {
    return null
  }
  try {
    return JSON.parse(fs.readFileSync(rendererSmokeResultPath, 'utf8'))
  }
  catch {
    return null
  }
}

function stopSmokeChild(child) {
  if (!child.pid) {
    return
  }
  try {
    child.kill()
  }
  catch {
    // Best effort cleanup; taskkill below handles the common Windows shell case.
  }
  if (process.platform === 'win32') {
    spawnSync('taskkill', ['/pid', String(child.pid), '/t', '/f'], {
      stdio: 'ignore',
      windowsHide: true,
    })
  }
}

function assertRendererSmokeResult(result) {
  if (!result.ok) {
    const hostPolicySuffix = result.hostPolicyBlocked
      ? ' (host policy blocked Chromium sandbox/profile access)'
      : ''
    const commitBlockedSuffix = result.rendererCommitBlocked
      ? ' (rpa:// boot.html was served, but Chromium did not commit the main-frame navigation)'
      : ''
    throw new Error(`Portable renderer smoke test failed: ${result.error || 'unknown error'}${hostPolicySuffix}${commitBlockedSuffix}`)
  }
  if (result.rendererSmoke !== true) {
    throw new Error('Portable renderer smoke test did not report rendererSmoke=true')
  }
  if (!result.rendererSmokeState?.appMounted) {
    throw new Error('Portable renderer smoke test did not mount the renderer app')
  }
  if (!fs.existsSync(rendererSmokeScreenshotPath)) {
    throw new Error('Portable renderer smoke test did not write a screenshot')
  }
  const screenshotStat = fs.statSync(rendererSmokeScreenshotPath)
  if (!screenshotStat.isFile() || screenshotStat.size <= 0) {
    throw new Error('Portable renderer smoke screenshot is empty')
  }
  if (Number(result.screenshotBytes || 0) !== screenshotStat.size) {
    throw new Error('Portable renderer smoke screenshot size does not match the result file')
  }
}

function writeWorkflowEditorSmokeFailure(error, extra = {}) {
  const childOutput = String(extra.childOutput || '')
  const hostPolicyEvidence = extractHostPolicyEvidence(childOutput)
  const existingResult = extra.existingResult && typeof extra.existingResult === 'object'
    ? extra.existingResult
    : {}
  fs.mkdirSync(workflowEditorSmokeRoot, { recursive: true })
  fs.writeFileSync(workflowEditorSmokeResultPath, JSON.stringify({
    ...existingResult,
    error: error instanceof Error ? error.message : String(error),
    generatedAt: new Date().toISOString(),
    hostPolicyBlocked: hostPolicyEvidence.length > 0,
    hostPolicyEvidence,
    ok: false,
    workflowEditorSmoke: true,
    childOutputPath: fs.existsSync(workflowEditorSmokeOutputPath) ? workflowEditorSmokeOutputPath : undefined,
    screenshotPath: workflowEditorSmokeScreenshotPath,
  }, null, 2))
}

function readWorkflowEditorSmokeResult() {
  if (!fs.existsSync(workflowEditorSmokeResultPath)) {
    return null
  }
  try {
    return JSON.parse(fs.readFileSync(workflowEditorSmokeResultPath, 'utf8'))
  }
  catch {
    return null
  }
}

function assertWorkflowEditorSmokeResult(result) {
  if (!result.ok) {
    const hostPolicySuffix = result.hostPolicyBlocked
      ? ' (host policy blocked Chromium sandbox/profile access)'
      : ''
    throw new Error(`Workflow editor smoke test failed: ${result.error || 'unknown error'}${hostPolicySuffix}`)
  }
  if (result.workflowEditorSmoke !== true) {
    throw new Error('Workflow editor smoke test did not report workflowEditorSmoke=true')
  }
  if (!result.workflowEditorSmokeCreateSave?.ok || Number(result.workflowEditorSmokeCreateSave.savedNodeCount || 0) < 1) {
    throw new Error('Workflow editor smoke did not accept create/save evidence')
  }
  if (!result.workflowEditorSmokeReloadEdit?.ok || Number(result.workflowEditorSmokeReloadEdit.savedNodeCount || 0) < 1) {
    throw new Error('Workflow editor smoke did not accept reload/edit evidence')
  }
  if (!result.workflowEditorSmokeState?.appMounted) {
    throw new Error('Workflow editor smoke test did not mount the renderer app')
  }
  if (!fs.existsSync(workflowEditorSmokeScreenshotPath)) {
    throw new Error('Workflow editor smoke test did not write a screenshot')
  }
  const screenshotStat = fs.statSync(workflowEditorSmokeScreenshotPath)
  if (!screenshotStat.isFile() || screenshotStat.size <= 0) {
    throw new Error('Workflow editor smoke screenshot is empty')
  }
  if (Number(result.screenshotBytes || 0) !== screenshotStat.size) {
    throw new Error('Workflow editor smoke screenshot size does not match the result file')
  }
}

function writeBackendOfflineSmokeFailure(error, extra = {}) {
  const childOutput = String(extra.childOutput || '')
  const hostPolicyEvidence = extractHostPolicyEvidence(childOutput)
  const existingResult = extra.existingResult && typeof extra.existingResult === 'object'
    ? extra.existingResult
    : {}
  fs.mkdirSync(backendOfflineSmokeRoot, { recursive: true })
  fs.writeFileSync(backendOfflineSmokeResultPath, JSON.stringify({
    ...existingResult,
    error: error instanceof Error ? error.message : String(error),
    generatedAt: new Date().toISOString(),
    hostPolicyBlocked: hostPolicyEvidence.length > 0,
    hostPolicyEvidence,
    ok: false,
    backendOfflineSmoke: true,
    childOutputPath: fs.existsSync(backendOfflineSmokeOutputPath) ? backendOfflineSmokeOutputPath : undefined,
    screenshotPath: backendOfflineSmokeScreenshotPath,
  }, null, 2))
}

function readBackendOfflineSmokeResult() {
  if (!fs.existsSync(backendOfflineSmokeResultPath)) {
    return null
  }
  try {
    return JSON.parse(fs.readFileSync(backendOfflineSmokeResultPath, 'utf8'))
  }
  catch {
    return null
  }
}

function assertBackendOfflineSmokeResult(result) {
  if (!result.ok) {
    const hostPolicySuffix = result.hostPolicyBlocked
      ? ' (host policy blocked Chromium sandbox/profile access)'
      : ''
    throw new Error(`Backend offline UI smoke test failed: ${result.error || 'unknown error'}${hostPolicySuffix}`)
  }
  if (result.backendOfflineSmoke !== true) {
    throw new Error('Backend offline smoke test did not report backendOfflineSmoke=true')
  }
  const smokeResult = result.backendOfflineSmokeResult || {}
  const errorText = String(smokeResult.errorText || '')
  if (smokeResult.ok !== true || smokeResult.inlineErrorVisible !== true) {
    throw new Error('Backend offline smoke did not accept visible inline error-state evidence')
  }
  if (!errorText.includes('서버') || !errorText.includes('확인')) {
    throw new Error(`Backend offline smoke error text is not actionable: ${errorText}`)
  }
  if (!fs.existsSync(backendOfflineSmokeScreenshotPath)) {
    throw new Error('Backend offline smoke test did not write a screenshot')
  }
  const screenshotStat = fs.statSync(backendOfflineSmokeScreenshotPath)
  if (!screenshotStat.isFile() || screenshotStat.size <= 0) {
    throw new Error('Backend offline smoke screenshot is empty')
  }
  if (Number(result.screenshotBytes || 0) !== screenshotStat.size) {
    throw new Error('Backend offline smoke screenshot size does not match the result file')
  }
}

function runRendererSmokeTest() {
  return new Promise((resolve, reject) => {
    fs.rmSync(rendererSmokeRoot, { force: true, recursive: true })
    fs.mkdirSync(rendererSmokeRoot, { recursive: true })

    let settled = false
    let timeout
    let resultPoll
    let childOutput = ''
    const readChildOutput = () => {
      if (childOutput) {
        return childOutput
      }
      if (!fs.existsSync(rendererSmokeOutputPath)) {
        return ''
      }
      try {
        return fs.readFileSync(rendererSmokeOutputPath, 'utf8').slice(-30000)
      }
      catch {
        return ''
      }
    }
    const appendChildOutput = (chunk) => {
      childOutput = `${childOutput}${chunk.toString()}`
      if (childOutput.length > 30000)
        childOutput = childOutput.slice(-30000)
    }
    const child = spawn(
      path.join(portableRoot, 'ShopRPA.cmd'),
      [],
      {
        cwd: portableRoot,
        env: {
          ...process.env,
          SHOPRPA_SMOKE_RENDERER: '1',
          SHOPRPA_SMOKE_OUTPUT: rendererSmokeOutputPath,
          SHOPRPA_SMOKE_RESULT: rendererSmokeResultPath,
          SHOPRPA_SMOKE_SCREENSHOT: rendererSmokeScreenshotPath,
          SHOPRPA_SMOKE_TEST: '1',
          SHOPRPA_SMOKE_USER_DATA: rendererSmokeUserDataPath,
        },
        shell: true,
        stdio: 'ignore',
        windowsHide: true,
      },
    )
    child.stdout?.on('data', appendChildOutput)
    child.stderr?.on('data', appendChildOutput)

    const settle = (callback) => {
      if (settled) {
        return
      }
      settled = true
      clearTimeout(timeout)
      clearInterval(resultPoll)
      callback()
    }

    const rejectWithFailure = (error, options = {}) => {
      settle(() => {
        stopSmokeChild(child)
        if (options.writeFailure !== false) {
          writeRendererSmokeFailure(error, {
            childOutput: readChildOutput(),
            existingResult: options.existingResult,
          })
        }
        reject(error)
      })
    }

    const resolveWithResult = (result, shouldStopChild) => {
      settle(() => {
        if (shouldStopChild) {
          stopSmokeChild(child)
        }
        resolve(result)
      })
    }

    timeout = setTimeout(() => {
      const result = readRendererSmokeResult()
      if (result) {
        try {
          assertRendererSmokeResult(result)
          resolveWithResult(result, true)
        }
        catch (error) {
          rejectWithFailure(error, { existingResult: result })
        }
        return
      }
      rejectWithFailure(new Error('Portable renderer smoke test timed out'))
    }, 45000)

    resultPoll = setInterval(() => {
      const result = readRendererSmokeResult()
      if (!result) {
        return
      }
      try {
        assertRendererSmokeResult(result)
        resolveWithResult(result, true)
      }
      catch (error) {
        rejectWithFailure(error, { existingResult: result })
      }
    }, 500)
    resultPoll.unref?.()

    child.on('error', (error) => {
      rejectWithFailure(error)
    })

    child.on('exit', () => {
      settle(() => {
        const result = readRendererSmokeResult()
        if (!result) {
          reject(new Error('Portable renderer smoke test did not write a result file'))
          return
        }

        try {
          assertRendererSmokeResult(result)
        }
        catch (error) {
          writeRendererSmokeFailure(error, {
            childOutput: readChildOutput(),
            existingResult: result,
          })
          reject(error)
          return
        }

        resolve(result)
      })
    })
  })
}

function runBackendOfflineSmokeTest() {
  return new Promise((resolve, reject) => {
    fs.rmSync(backendOfflineSmokeRoot, { force: true, recursive: true })
    fs.mkdirSync(backendOfflineSmokeRoot, { recursive: true })

    let settled = false
    let timeout
    let resultPoll
    let childOutput = ''
    const readChildOutput = () => {
      if (childOutput) {
        return childOutput
      }
      if (!fs.existsSync(backendOfflineSmokeOutputPath)) {
        return ''
      }
      try {
        return fs.readFileSync(backendOfflineSmokeOutputPath, 'utf8').slice(-30000)
      }
      catch {
        return ''
      }
    }
    const appendChildOutput = (chunk) => {
      childOutput = `${childOutput}${chunk.toString()}`
      if (childOutput.length > 30000)
        childOutput = childOutput.slice(-30000)
    }
    const child = spawn(
      path.join(portableRoot, 'ShopRPA.cmd'),
      [],
      {
        cwd: portableRoot,
        env: {
          ...process.env,
          SHOPRPA_SMOKE_BACKEND_OFFLINE: '1',
          SHOPRPA_SMOKE_OUTPUT: backendOfflineSmokeOutputPath,
          SHOPRPA_SMOKE_RESULT: backendOfflineSmokeResultPath,
          SHOPRPA_SMOKE_SCREENSHOT: backendOfflineSmokeScreenshotPath,
          SHOPRPA_SMOKE_TEST: '1',
          SHOPRPA_SMOKE_USER_DATA: backendOfflineSmokeUserDataPath,
        },
        shell: true,
        stdio: 'ignore',
        windowsHide: true,
      },
    )
    child.stdout?.on('data', appendChildOutput)
    child.stderr?.on('data', appendChildOutput)

    const settle = (callback) => {
      if (settled) {
        return
      }
      settled = true
      clearTimeout(timeout)
      clearInterval(resultPoll)
      callback()
    }

    const rejectWithFailure = (error, options = {}) => {
      settle(() => {
        stopSmokeChild(child)
        if (options.writeFailure !== false) {
          writeBackendOfflineSmokeFailure(error, {
            childOutput: readChildOutput(),
            existingResult: options.existingResult,
          })
        }
        reject(error)
      })
    }

    const resolveWithResult = (result, shouldStopChild) => {
      settle(() => {
        if (shouldStopChild) {
          stopSmokeChild(child)
        }
        resolve(result)
      })
    }

    timeout = setTimeout(() => {
      const result = readBackendOfflineSmokeResult()
      if (result) {
        try {
          assertBackendOfflineSmokeResult(result)
          resolveWithResult(result, true)
        }
        catch (error) {
          rejectWithFailure(error, { existingResult: result })
        }
        return
      }
      rejectWithFailure(new Error('Backend offline smoke test timed out'))
    }, 50000)

    resultPoll = setInterval(() => {
      const result = readBackendOfflineSmokeResult()
      if (!result) {
        return
      }
      try {
        assertBackendOfflineSmokeResult(result)
        resolveWithResult(result, true)
      }
      catch (error) {
        rejectWithFailure(error, { existingResult: result })
      }
    }, 500)
    resultPoll.unref?.()

    child.on('error', (error) => {
      rejectWithFailure(error)
    })

    child.on('exit', () => {
      settle(() => {
        const result = readBackendOfflineSmokeResult()
        if (!result) {
          reject(new Error('Backend offline smoke test did not write a result file'))
          return
        }

        try {
          assertBackendOfflineSmokeResult(result)
        }
        catch (error) {
          writeBackendOfflineSmokeFailure(error, {
            childOutput: readChildOutput(),
            existingResult: result,
          })
          reject(error)
          return
        }

        resolve(result)
      })
    })
  })
}

function runWorkflowEditorSmokeTest() {
  return new Promise((resolve, reject) => {
    fs.rmSync(workflowEditorSmokeRoot, { force: true, recursive: true })
    fs.mkdirSync(workflowEditorSmokeRoot, { recursive: true })

    let settled = false
    let timeout
    let resultPoll
    let childOutput = ''
    const readChildOutput = () => {
      if (childOutput) {
        return childOutput
      }
      if (!fs.existsSync(workflowEditorSmokeOutputPath)) {
        return ''
      }
      try {
        return fs.readFileSync(workflowEditorSmokeOutputPath, 'utf8').slice(-30000)
      }
      catch {
        return ''
      }
    }
    const appendChildOutput = (chunk) => {
      childOutput = `${childOutput}${chunk.toString()}`
      if (childOutput.length > 30000)
        childOutput = childOutput.slice(-30000)
    }
    const child = spawn(
      path.join(portableRoot, 'ShopRPA.cmd'),
      [],
      {
        cwd: portableRoot,
        env: {
          ...process.env,
          SHOPRPA_SMOKE_WORKFLOW_EDITOR: '1',
          SHOPRPA_SMOKE_OUTPUT: workflowEditorSmokeOutputPath,
          SHOPRPA_SMOKE_RESULT: workflowEditorSmokeResultPath,
          SHOPRPA_SMOKE_SCREENSHOT: workflowEditorSmokeScreenshotPath,
          SHOPRPA_SMOKE_TEST: '1',
          SHOPRPA_SMOKE_USER_DATA: workflowEditorSmokeUserDataPath,
        },
        shell: true,
        stdio: 'ignore',
        windowsHide: true,
      },
    )
    child.stdout?.on('data', appendChildOutput)
    child.stderr?.on('data', appendChildOutput)

    const settle = (callback) => {
      if (settled) {
        return
      }
      settled = true
      clearTimeout(timeout)
      clearInterval(resultPoll)
      callback()
    }

    const rejectWithFailure = (error, options = {}) => {
      settle(() => {
        stopSmokeChild(child)
        if (options.writeFailure !== false) {
          writeWorkflowEditorSmokeFailure(error, {
            childOutput: readChildOutput(),
            existingResult: options.existingResult,
          })
        }
        reject(error)
      })
    }

    const resolveWithResult = (result, shouldStopChild) => {
      settle(() => {
        if (shouldStopChild) {
          stopSmokeChild(child)
        }
        resolve(result)
      })
    }

    timeout = setTimeout(() => {
      const result = readWorkflowEditorSmokeResult()
      if (result) {
        try {
          assertWorkflowEditorSmokeResult(result)
          resolveWithResult(result, true)
        }
        catch (error) {
          rejectWithFailure(error, { existingResult: result })
        }
        return
      }
      rejectWithFailure(new Error('Workflow editor smoke test timed out'))
    }, 70000)

    resultPoll = setInterval(() => {
      const result = readWorkflowEditorSmokeResult()
      if (!result) {
        return
      }
      try {
        assertWorkflowEditorSmokeResult(result)
        resolveWithResult(result, true)
      }
      catch (error) {
        rejectWithFailure(error, { existingResult: result })
      }
    }, 500)
    resultPoll.unref?.()

    child.on('error', (error) => {
      rejectWithFailure(error)
    })

    child.on('exit', () => {
      settle(() => {
        const result = readWorkflowEditorSmokeResult()
        if (!result) {
          reject(new Error('Workflow editor smoke test did not write a result file'))
          return
        }

        try {
          assertWorkflowEditorSmokeResult(result)
        }
        catch (error) {
          writeWorkflowEditorSmokeFailure(error, {
            childOutput: readChildOutput(),
            existingResult: result,
          })
          reject(error)
          return
        }

        resolve(result)
      })
    })
  })
}

async function main() {
  for (const file of requiredFiles) {
    assertFile(file)
  }
  assertAsarContents()
  assertPythonArchiveHash()
  assertPythonArchiveMatchesSource()
  assertPythonArchiveReadable()
  assertBackendConfig()

  const result = await runSmokeTest()
  const rendererResult = process.env.SHOPRPA_VERIFY_RENDERER_SMOKE === '1'
    ? await runRendererSmokeTest()
    : null
  const backendOfflineResult = process.env.SHOPRPA_VERIFY_BACKEND_OFFLINE_SMOKE === '1'
    ? await runBackendOfflineSmokeTest()
    : null
  const workflowEditorResult = process.env.SHOPRPA_VERIFY_WORKFLOW_EDITOR_SMOKE === '1'
    ? await runWorkflowEditorSmokeTest()
    : null
  console.log('Portable verification passed')
  console.log(`appPath=${result.appPath}`)
  console.log(`rendererPath=${result.rendererPath}`)
  if (rendererResult)
    console.log(`rendererSmokeScreenshot=${rendererResult.screenshotPath}`)
  if (backendOfflineResult)
    console.log(`backendOfflineSmokeScreenshot=${backendOfflineResult.screenshotPath}`)
  if (workflowEditorResult)
    console.log(`workflowEditorSmokeScreenshot=${workflowEditorResult.screenshotPath}`)
}

main().catch((error) => {
  console.error(`Portable verification failed: ${error.message}`)
  process.exit(1)
})
