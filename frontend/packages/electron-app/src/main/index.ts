import type { Buffer } from 'node:buffer'
import fs from 'node:fs'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

import type { BrowserWindow } from 'electron'
import { app, ipcMain, protocol, session } from 'electron'

import type { W2WType } from '../types'

import { envJson } from './env'
import { listenRender } from './event'
import { getExtensionResourcePath } from './extension'
import logger from './log'
import { appWorkPath, extensionHost, isPackagedRuntime, rendererPath, windowBaseUrl } from './path'
import { checkPythonRpaProcess, closeSubProcess, startBackend } from './server'
import { changeTray, createTray } from './tray'
import { createSubWindow, createMainWindow as createWindow, electronInfo, getMainWindow, getWindowFromLabel, WindowStack } from './window'

const startTime = Date.now()
const isSmokeTest = process.argv.includes('--smoke-test') || process.env.SHOPRPA_SMOKE_TEST === '1'
globalThis.MainWindowLoaded = false

if (process.env.SHOPRPA_ALLOW_INSECURE_ELECTRON === '1') {
  logger.warn('Insecure Electron web security overrides are enabled.')
  app.commandLine.appendSwitch('ignore-certificate-errors')
  app.commandLine.appendSwitch('disable-web-security')
}
if (isSmokeTest) {
  app.commandLine.appendSwitch('no-sandbox')
  app.commandLine.appendSwitch('disable-gpu')
  app.commandLine.appendSwitch('disable-gpu-sandbox')
  app.commandLine.appendSwitch('disable-features', 'NetworkServiceSandbox,RendererAppContainer')
}
app.disableHardwareAcceleration()
app.setAppUserModelId('com.shoprpa.app')
/**
 * Register rpa:// as a secure, standard origin so renderer assets can use
 * browser APIs such as localStorage, fetch, and service workers.
 */
protocol.registerSchemesAsPrivileged([
  {
    scheme: 'rpa',
    privileges: {
      secure: true,
      standard: true,
      supportFetchAPI: true,
      corsEnabled: true,
      allowServiceWorkers: true,
    },
  },
])

function createMainWindow() {
  const mainWindow = createWindow()
  const url = getRendererBootUrl()
  const fallbackUrl = isPackagedRuntime ? getRendererBootFileUrl() : ''
  logger.info(`app load url: ${url}`)

  let loadCompleted = false
  let fallbackTimer: ReturnType<typeof setTimeout> | undefined
  const markLoaded = () => {
    loadCompleted = true
    if (fallbackTimer)
      clearTimeout(fallbackTimer)
  }
  const loadUrl = (targetUrl: string, label: string) => {
    mainWindow.loadURL(targetUrl).then(() => {
      markLoaded()
      electronInfo(mainWindow)
    }).catch((err) => {
      logger.error(`Failed to load ${label} URL`, err instanceof Error ? err.message : String(err))
      if (fallbackUrl && targetUrl !== fallbackUrl) {
        logger.warn(`Retrying packaged renderer with file URL: ${fallbackUrl}`)
        loadUrl(fallbackUrl, 'fallback')
        return
      }
      logger.info('Retry loading URL after 10 seconds...')
      setTimeout(() => {
        loadUrl(targetUrl, label)
      }, 10 * 1000)
    })
  }
  if (fallbackUrl) {
    fallbackTimer = setTimeout(() => {
      if (loadCompleted)
        return
      logger.warn(`Packaged renderer URL did not finish loading; falling back to ${fallbackUrl}`)
      try {
        mainWindow.webContents.stop()
      }
      catch (err) {
        logger.warn('Failed to stop stalled renderer navigation', err instanceof Error ? err.message : String(err))
      }
      loadUrl(fallbackUrl, 'fallback')
    }, 8 * 1000)
  }
  loadUrl(url, 'primary')
  mainWindow.once('ready-to-show', () => {
    WindowStack.set('main', mainWindow)
    mainWindow.show()
    logger.info(`app show: ${`${Date.now() - startTime}ms`}`)
  })
  createTray(mainWindow)
}

function sessionHanlder() {
  let setCookieKey = ''
  let jsessionIdValue = ''
  const pattern = /jwt=(.*?);/i
  session.defaultSession.webRequest.onHeadersReceived(
    {
      urls: envJson.REQUEST_WHITE_URL,
    },
    (details, callback) => {
      if (details.responseHeaders && details.responseHeaders['Set-Cookie']) {
        setCookieKey = 'Set-Cookie'
      }
      else {
        setCookieKey = 'set-cookie'
      }
      if (details.responseHeaders && details.responseHeaders[setCookieKey] && details.responseHeaders[setCookieKey].length) {
        for (let i = 0; i < details.responseHeaders[setCookieKey].length; i++) {
          details.responseHeaders[setCookieKey][i] += '; SameSite=None; Secure'
          const match = details.responseHeaders[setCookieKey][i].match(pattern)
          const val = match && match[1]
          jsessionIdValue = val || ''
        }
      }
      callback({ responseHeaders: details.responseHeaders })
    },
  )
  session.defaultSession.webRequest.onBeforeSendHeaders(
    {
      urls: envJson.REQUEST_WHITE_URL,
    },
    (details, callback) => {
      const headers = details.requestHeaders
      headers.Cookie = `jwt=${jsessionIdValue};`
      callback({ cancel: false, requestHeaders: headers })
    },
  )
}

const registeredRpaProtocols = new WeakSet<object>()
let rpaProtocolSmokeLogCount = 0
const rpaProtocolSmokeEvents: string[] = []

const protocolMimeTypes: Record<string, string> = {
  '.css': 'text/css',
  '.html': 'text/html',
  '.ico': 'image/x-icon',
  '.js': 'text/javascript',
  '.json': 'application/json',
  '.map': 'application/json',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.ttf': 'font/ttf',
  '.webp': 'image/webp',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
}

function createProtocolFileResponse(filePath: string) {
  const contentType = protocolMimeTypes[path.extname(filePath).toLowerCase()] || 'application/octet-stream'
  return {
    data: fs.readFileSync(filePath),
    mimeType: contentType,
  }
}

function resolveFileInRoot(rootPath: string, parts: string[]) {
  if (!rootPath)
    return ''

  const root = path.resolve(rootPath)
  const resolved = path.resolve(
    root,
    ...parts
      .filter(Boolean)
      .map(part => decodeURIComponent(part)),
  )
  const isInsideRoot = resolved === root || resolved.startsWith(`${root}${path.sep}`)
  return isInsideRoot ? resolved : ''
}

function resolveRpaProtocolFile(requestUrl: string) {
  const u = new URL(requestUrl)
  if (u.hostname === extensionHost) {
    const paths = u.pathname.split('/')
    const extensionName = paths[1]
    const resourcePath = getExtensionResourcePath(extensionName)
    return resolveFileInRoot(resourcePath, paths.slice(2))
  }

  return resolveFileInRoot(rendererPath, u.pathname.split('/'))
}

function logRpaProtocolFile(requestUrl: string, filePath: string) {
  if (isSmokeTest && rpaProtocolSmokeLogCount < 20) {
    rpaProtocolSmokeLogCount += 1
    rpaProtocolSmokeEvents.push(`file:${requestUrl}->${filePath}`)
    logger.info(`rpa protocol file: ${requestUrl} -> ${filePath}`)
  }
}

function logRpaProtocolResponse(requestUrl: string, status: number, mimeType: string) {
  if (isSmokeTest && rpaProtocolSmokeLogCount < 20) {
    rpaProtocolSmokeEvents.push(`response:${requestUrl}->${status}:${mimeType}`)
    logger.info(`rpa protocol response: ${requestUrl} -> ${status} ${mimeType}`)
  }
}

function getRendererBootUrl() {
  return `${windowBaseUrl}boot.html`
}

function getRendererBootFileUrl() {
  return pathToFileURL(path.join(rendererPath, 'boot.html')).toString()
}

function getRendererPageUrl(pageName: string, search = '', hash = '') {
  return `${windowBaseUrl}${pageName}${search}${hash}`
}

function getRendererPageFileUrl(pageName: string, search = '', hash = '') {
  return `${pathToFileURL(path.join(rendererPath, pageName)).toString()}${search}${hash}`
}

function getWorkflowEditorSmokeSearch() {
  return '?__shoprpa_workflow_smoke=1'
}

function getWorkflowEditorSmokeHash() {
  const params = new URLSearchParams({
    projectId: 'shoprpa-workflow-smoke-project',
    projectName: 'ShopRPA 스모크 워크플로',
    projectVersion: '1',
  })
  return `#/arrange/editorPage?${params.toString()}`
}

function getRendererWorkflowEditorSmokeUrl() {
  return getRendererPageUrl('index.html', getWorkflowEditorSmokeSearch(), getWorkflowEditorSmokeHash())
}

function getRendererWorkflowEditorSmokeFileUrl() {
  return getRendererPageFileUrl('index.html', getWorkflowEditorSmokeSearch(), getWorkflowEditorSmokeHash())
}

function registerRpaProtocol(protocolApi: typeof protocol = protocol) {
  if (registeredRpaProtocols.has(protocolApi))
    return
  registeredRpaProtocols.add(protocolApi)

  const registeredFileProtocol = protocolApi.registerFileProtocol('rpa', (request, callback) => {
    try {
      const filePath = resolveRpaProtocolFile(request.url)
      if (filePath && fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
        logRpaProtocolFile(request.url, filePath)
        const mimeType = protocolMimeTypes[path.extname(filePath).toLowerCase()] || 'application/octet-stream'
        logRpaProtocolResponse(request.url, 200, mimeType)
        callback({ path: filePath })
        return
      }
    }
    catch (err) {
      logger.error('rpa protocol file resolve error:', err)
    }
    callback({ error: -6 })
  })
  if (registeredFileProtocol)
    return

  if (protocolApi.handle) {
    try {
      protocolApi.handle('rpa', async (request) => {
        try {
          const filePath = resolveRpaProtocolFile(request.url)
          if (filePath && fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
            logRpaProtocolFile(request.url, filePath)
            const { data, mimeType } = createProtocolFileResponse(filePath)
            const body = data.buffer.slice(data.byteOffset, data.byteOffset + data.byteLength)
            const response = new Response(body, {
              status: 200,
              headers: {
                'content-length': String(data.byteLength),
                'content-type': mimeType,
              },
            })
            logRpaProtocolResponse(request.url, response.status, response.headers.get('content-type') || '')
            return response
          }
        }
        catch (err) {
          logger.error('rpa protocol file resolve error:', err)
          return new Response('rpa protocol file resolve error', { status: 500 })
        }
        return new Response('not found', { status: 404 })
      })
      return
    }
    catch (err) {
      registeredRpaProtocols.delete(protocolApi)
      throw err
    }
  }

  // Resolve rpa://localhost/boot.html and related assets from rendererPath.
  const registered = protocolApi.registerBufferProtocol('rpa', (request, callback) => {
    try {
      const filePath = resolveRpaProtocolFile(request.url)
      if (filePath && fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
        logRpaProtocolFile(request.url, filePath)
        const response = createProtocolFileResponse(filePath)
        logRpaProtocolResponse(request.url, 200, response.mimeType)
        callback(response)
        return
      }
    }
    catch (err) {
      logger.error('rpa protocol file resolve error:', err)
    }
    callback({ error: -6 })
  })
  if (!registered) {
    registeredRpaProtocols.delete(protocolApi)
    throw new Error('Failed to register rpa protocol handler')
  }
}

async function ready() {
  logger.info('app ready')
  await checkProcess()
  sessionHanlder()
  registerRpaProtocol()
  listenRender()
  createMainWindow()
}

async function checkProcess() {
  const isRunning = await checkPythonRpaProcess()
  if (isRunning) {
    logger.warn(`Another python setup is already running.`)
    app.quit()
  }
  else {
    logger.info(`No other python setup found.`)
  }
}

let smokeFinished = false
function finishSmokeTest(exitCode: number, error?: unknown, extra: Record<string, unknown> = {}) {
  if (smokeFinished)
    return
  smokeFinished = true

  const resultPath = process.env.SHOPRPA_SMOKE_RESULT
  if (resultPath) {
    fs.mkdirSync(path.dirname(resultPath), { recursive: true })
    fs.writeFileSync(resultPath, JSON.stringify({
      appPath: app.getAppPath(),
      appWorkPath,
      error: error instanceof Error ? error.message : error ? String(error) : undefined,
      generatedAt: new Date().toISOString(),
      ok: exitCode === 0,
      packaged: app.isPackaged,
      packagedRuntime: isPackagedRuntime,
      mainLogPath: path.join(appWorkPath, 'logs', 'main.log'),
      rendererPath,
      ...(isSmokeTest ? { rpaProtocolSmokeEvents: rpaProtocolSmokeEvents.slice(0, 50) } : {}),
      ...extra,
    }, null, 2))
  }

  const exitFallback = setTimeout(() => {
    process.exit(exitCode)
  }, isSmokeTest ? 500 : 2000)
  exitFallback.unref?.()
  app.exit(exitCode)
}

function withRendererSmokeDiagnostics(error: unknown, diagnostics: Record<string, unknown>) {
  if (error && typeof error === 'object') {
    Object.assign(error, { rendererSmokeDiagnostics: diagnostics })
    return error
  }

  const wrapped = new Error(String(error))
  Object.assign(wrapped, { rendererSmokeDiagnostics: diagnostics })
  return wrapped
}

function getRendererSmokeDiagnostics(error: unknown) {
  if (error && typeof error === 'object' && 'rendererSmokeDiagnostics' in error) {
    const diagnostics = (error as { rendererSmokeDiagnostics?: unknown }).rendererSmokeDiagnostics
    if (diagnostics && typeof diagnostics === 'object')
      return diagnostics as Record<string, unknown>
  }

  return {}
}

function withSmokeTimeout<T>(label: string, promise: Promise<T>, timeoutMs: number): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const timeout = setTimeout(() => {
      reject(new Error(`${label} timed out after ${timeoutMs}ms`))
    }, timeoutMs)
    promise.then((value) => {
      clearTimeout(timeout)
      resolve(value)
    }).catch((error) => {
      clearTimeout(timeout)
      reject(error)
    })
  })
}

interface RendererSmokeState {
  appMounted: boolean
  bodyText: string
  title: string
  url: string
}

interface WorkflowEditorSmokePhaseResult {
  ok: boolean
  phase: string
  savedNodeCount: number
  savedNodes: Array<{
    alias: string
    id: string
    key: string
    title: string
    version: string
  }>
}

interface BackendOfflineSmokeResult {
  ok: boolean
  bodyText: string
  errorText: string
  inlineErrorVisible: boolean
  loginButtonText: string
  submitted: boolean
  toastVisible: boolean
  url: string
}

interface RendererSmokeImage {
  toPNG: () => Buffer
}

function waitForRendererNavigation(mainWindow: BrowserWindow, url: string, navigationEvents: string[], timeoutMs = 20000): Promise<string> {
  return new Promise((resolve, reject) => {
    let settled = false
    const timeout = setTimeout(() => {
      settle(() => reject(new Error(`Renderer smoke initial navigation timed out after ${timeoutMs}ms`)))
    }, timeoutMs)
    const settle = (callback: () => void) => {
      if (settled)
        return
      settled = true
      clearTimeout(timeout)
      mainWindow.webContents.removeListener('dom-ready', onDomReady)
      mainWindow.webContents.removeListener('did-finish-load', onDidFinishLoad)
      mainWindow.webContents.removeListener('did-fail-load', onDidFailLoad)
      mainWindow.webContents.removeListener('render-process-gone', onRenderProcessGone)
      callback()
    }
    const onDomReady = () => settle(() => resolve('dom-ready'))
    const onDidFinishLoad = () => settle(() => resolve('did-finish-load'))
    const onDidFailLoad = (
      _event: unknown,
      errorCode: number,
      errorDescription: string,
      validatedURL: string,
      isMainFrame: boolean,
    ) => {
      if (!isMainFrame)
        return
      navigationEvents.push(`did-fail-load:${errorCode}:${errorDescription}:${validatedURL}`)
      settle(() => reject(new Error(`Renderer smoke load failed: ${errorCode} ${errorDescription} ${validatedURL}`)))
    }
    const onRenderProcessGone = (_event: unknown, details: { reason?: string }) => {
      navigationEvents.push(`render-process-gone:${details.reason || 'unknown'}`)
      settle(() => reject(new Error(`Renderer process exited during smoke navigation: ${details.reason || 'unknown'}`)))
    }

    mainWindow.webContents.once('dom-ready', onDomReady)
    mainWindow.webContents.once('did-finish-load', onDidFinishLoad)
    mainWindow.webContents.on('did-fail-load', onDidFailLoad)
    mainWindow.webContents.once('render-process-gone', onRenderProcessGone)
    mainWindow.loadURL(url).then(() => {
      settle(() => resolve('loadURL'))
    }).catch((error) => {
      settle(() => reject(error))
    })
  })
}

async function waitForRendererNavigationWithFallback(
  mainWindow: BrowserWindow,
  url: string,
  navigationEvents: string[],
  fallbackUrl = isPackagedRuntime ? getRendererBootFileUrl() : '',
) {
  try {
    return await waitForRendererNavigation(mainWindow, url, navigationEvents, 12000)
  }
  catch (error) {
    if (!fallbackUrl)
      throw error
    const reason = error instanceof Error ? error.message : String(error)
    navigationEvents.push(`fallback-navigation:${fallbackUrl}`)
    try {
      mainWindow.webContents.stop()
    }
    catch (stopError) {
      navigationEvents.push(`fallback-stop-error:${stopError instanceof Error ? stopError.message : String(stopError)}`)
    }
    try {
      const fallbackStatus = await waitForRendererNavigation(mainWindow, fallbackUrl, navigationEvents, 20000)
      return `fallback:${fallbackStatus}`
    }
    catch (fallbackError) {
      const fallbackReason = fallbackError instanceof Error ? fallbackError.message : String(fallbackError)
      throw new Error(`${reason}; fallback failed: ${fallbackReason}`)
    }
  }
}

async function runRendererSmokeTest() {
  const rendererErrors: string[] = []
  const navigationEvents: string[] = []
  let mainWindow: BrowserWindow | undefined

  const diagnostics = () => ({
    rendererConsoleErrorCount: rendererErrors.length,
    rendererConsoleErrors: rendererErrors.slice(0, 10),
    rendererCurrentUrl: mainWindow?.webContents.getURL() || '',
    rendererIsLoading: mainWindow?.webContents.isLoading() || false,
    rendererNavigationEvents: navigationEvents.slice(0, 50),
  })

  try {
    if (!isPackagedRuntime) {
      throw new Error('Not running from a packaged runtime layout')
    }
    if (!fs.existsSync(rendererPath)) {
      throw new Error(`Renderer path does not exist: ${rendererPath}`)
    }

    await app.whenReady()
    registerRpaProtocol()
    listenRender()
    ipcMain.handle('main_window_onload', () => {
      globalThis.MainWindowLoaded = true
      return true
    })

    mainWindow = createWindow()
    const url = getRendererBootUrl()
    const screenshotPath = process.env.SHOPRPA_SMOKE_SCREENSHOT
    mainWindow.webContents.on('console-message', (_event, level, message) => {
      if (level >= 2)
        rendererErrors.push(message)
    })
    mainWindow.webContents.on('did-start-loading', () => navigationEvents.push('did-start-loading'))
    mainWindow.webContents.on('did-stop-loading', () => navigationEvents.push('did-stop-loading'))
    mainWindow.webContents.on('did-start-navigation', (_event, targetUrl, isInPlace, isMainFrame) => {
      if (isMainFrame)
        navigationEvents.push(`did-start-navigation:${targetUrl}:${isInPlace}`)
    })
    mainWindow.webContents.on('will-navigate', (_event, targetUrl) => navigationEvents.push(`will-navigate:${targetUrl}`))
    mainWindow.webContents.on('did-commit-navigation', (_event, targetUrl, isInPlace, isMainFrame) => {
      if (isMainFrame)
        navigationEvents.push(`did-commit-navigation:${targetUrl}:${isInPlace}`)
    })
    mainWindow.webContents.on('did-navigate', (_event, targetUrl) => navigationEvents.push(`did-navigate:${targetUrl}`))
    mainWindow.webContents.on('did-fail-provisional-load', (_event, errorCode, errorDescription, validatedURL, isMainFrame) => {
      if (isMainFrame)
        navigationEvents.push(`did-fail-provisional-load:${errorCode}:${errorDescription}:${validatedURL}`)
    })
    app.on('child-process-gone', (_event, details) => {
      navigationEvents.push(`child-process-gone:${details.type || 'unknown'}:${details.reason || 'unknown'}:${details.exitCode ?? ''}`)
    })
    session.defaultSession.webRequest.onCompleted({ urls: ['rpa://*/*'] }, (details) => {
      navigationEvents.push(`webRequest-completed:${details.statusCode}:${details.url}`)
    })
    session.defaultSession.webRequest.onErrorOccurred({ urls: ['rpa://*/*'] }, (details) => {
      navigationEvents.push(`webRequest-error:${details.error}:${details.url}`)
    })
    mainWindow.showInactive()
    let navigationStatus = ''
    try {
      navigationStatus = await waitForRendererNavigationWithFallback(mainWindow, url, navigationEvents)
    }
    catch (error) {
      const status = `url=${mainWindow.webContents.getURL() || '(empty)'}, loading=${mainWindow.webContents.isLoading()}, events=${navigationEvents.slice(0, 20).join('|') || '(none)'}`
      const reason = error instanceof Error ? error.message : String(error)
      throw new Error(`${reason}; ${status}`)
    }
    if (!mainWindow.isVisible())
      mainWindow.showInactive()
    electronInfo(mainWindow)
    await new Promise(resolve => setTimeout(resolve, 1800))

    const smokeState = await withSmokeTimeout<RendererSmokeState>('Renderer smoke state probe', mainWindow.webContents.executeJavaScript(`
    (() => {
      const bodyText = document.body ? document.body.innerText : '';
      const app = document.querySelector('#app');
      return {
        title: document.title,
        bodyText,
        appMounted: Boolean(app && app.children.length > 0),
        url: window.location.href,
      };
    })()
  `) as Promise<RendererSmokeState>, 10000)
    if (!smokeState?.appMounted) {
      throw new Error('Renderer app did not mount.')
    }
    if (!String(smokeState.bodyText || '').includes('ShopRPA') && !String(smokeState.title || '').includes('ShopRPA')) {
      throw new Error('Renderer smoke did not find ShopRPA text in the loaded page.')
    }

    let screenshotBytes = 0
    if (screenshotPath) {
      fs.mkdirSync(path.dirname(screenshotPath), { recursive: true })
      const image = await withSmokeTimeout<RendererSmokeImage>('Renderer smoke screenshot capture', mainWindow.webContents.capturePage() as Promise<RendererSmokeImage>, 10000)
      const png = image.toPNG()
      screenshotBytes = png.length
      if (screenshotBytes <= 0) {
        throw new Error('Renderer screenshot is empty.')
      }
      fs.writeFileSync(screenshotPath, png)
    }

    logger.info('packaged renderer smoke test passed')
    finishSmokeTest(0, undefined, {
      rendererSmoke: true,
      rendererNavigationStatus: navigationStatus,
      rendererSmokeState: smokeState,
      rendererConsoleErrorCount: rendererErrors.length,
      rendererConsoleErrors: rendererErrors.slice(0, 10),
      rendererNavigationEvents: navigationEvents.slice(0, 50),
      screenshotPath,
      screenshotBytes,
    })
  }
  catch (error) {
    throw withRendererSmokeDiagnostics(error, diagnostics())
  }
}

async function runBackendOfflineSmokeTest() {
  const rendererErrors: string[] = []
  const navigationEvents: string[] = []
  let mainWindow: BrowserWindow | undefined

  const diagnostics = () => ({
    rendererConsoleErrorCount: rendererErrors.length,
    rendererConsoleErrors: rendererErrors.slice(0, 10),
    rendererCurrentUrl: mainWindow?.webContents.getURL() || '',
    rendererIsLoading: mainWindow?.webContents.isLoading() || false,
    rendererNavigationEvents: navigationEvents.slice(0, 80),
  })

  try {
    if (!isPackagedRuntime) {
      throw new Error('Not running from a packaged runtime layout')
    }
    if (!fs.existsSync(rendererPath)) {
      throw new Error(`Renderer path does not exist: ${rendererPath}`)
    }

    await app.whenReady()
    registerRpaProtocol()
    listenRender()
    ipcMain.handle('main_window_onload', () => {
      globalThis.MainWindowLoaded = true
      return true
    })

    mainWindow = createWindow()
    const url = getRendererBootUrl()
    const fallbackUrl = isPackagedRuntime ? getRendererBootFileUrl() : ''
    const screenshotPath = process.env.SHOPRPA_SMOKE_SCREENSHOT

    mainWindow.webContents.on('console-message', (_event, level, message) => {
      if (level >= 2)
        rendererErrors.push(message)
    })
    mainWindow.webContents.on('did-start-loading', () => navigationEvents.push('did-start-loading'))
    mainWindow.webContents.on('did-stop-loading', () => navigationEvents.push('did-stop-loading'))
    mainWindow.webContents.on('did-start-navigation', (_event, targetUrl, isInPlace, isMainFrame) => {
      if (isMainFrame)
        navigationEvents.push(`did-start-navigation:${targetUrl}:${isInPlace}`)
    })
    mainWindow.webContents.on('did-commit-navigation', (_event, targetUrl, isInPlace, isMainFrame) => {
      if (isMainFrame)
        navigationEvents.push(`did-commit-navigation:${targetUrl}:${isInPlace}`)
    })
    mainWindow.webContents.on('did-navigate', (_event, targetUrl) => navigationEvents.push(`did-navigate:${targetUrl}`))
    mainWindow.webContents.on('did-fail-provisional-load', (_event, errorCode, errorDescription, validatedURL, isMainFrame) => {
      if (isMainFrame)
        navigationEvents.push(`did-fail-provisional-load:${errorCode}:${errorDescription}:${validatedURL}`)
    })

    mainWindow.showInactive()
    let navigationStatus = ''
    try {
      navigationStatus = await waitForRendererNavigationWithFallback(mainWindow, url, navigationEvents, fallbackUrl)
    }
    catch (error) {
      const status = `url=${mainWindow.webContents.getURL() || '(empty)'}, loading=${mainWindow.webContents.isLoading()}, events=${navigationEvents.slice(0, 20).join('|') || '(none)'}`
      const reason = error instanceof Error ? error.message : String(error)
      throw new Error(`${reason}; ${status}`)
    }

    if (!mainWindow.isVisible())
      mainWindow.showInactive()
    electronInfo(mainWindow)

    const offlineResult = await withSmokeTimeout<BackendOfflineSmokeResult>(
      'Backend offline UI state probe',
      mainWindow.webContents.executeJavaScript(`
        new Promise((resolve, reject) => {
          const deadline = Date.now() + 25000;
          let submitted = false;

          const isVisible = (el) => {
            if (!el) return false;
            const rect = el.getBoundingClientRect();
            const style = window.getComputedStyle(el);
            return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none';
          };

          const setValue = (el, value) => {
            const prototype = Object.getPrototypeOf(el);
            const descriptor = Object.getOwnPropertyDescriptor(prototype, 'value');
            if (descriptor && descriptor.set) descriptor.set.call(el, value);
            else el.value = value;
            el.dispatchEvent(new Event('input', { bubbles: true }));
            el.dispatchEvent(new Event('change', { bubbles: true }));
            el.dispatchEvent(new Event('blur', { bubbles: true }));
          };

          const getErrorState = () => {
            const inline = document.querySelector('[data-shoprpa-auth-error-state]');
            const toast = Array.from(document.querySelectorAll('.ant-message-notice-content'))
              .find((el) => /서버|백엔드|연결|실패/.test(el.textContent || ''));
            const text = [inline && inline.textContent, toast && toast.textContent]
              .filter(Boolean)
              .join(' ')
              .replace(/\\s+/g, ' ')
              .trim();
            return {
              inlineErrorVisible: Boolean(inline && isVisible(inline)),
              toastVisible: Boolean(toast && isVisible(toast)),
              text,
            };
          };

          const submitLogin = () => {
            const loginRoot = document.querySelector('.auth-login');
            if (!loginRoot || !isVisible(loginRoot)) return { ready: false };

            const textInputs = Array.from(loginRoot.querySelectorAll('input'))
              .filter((el) => isVisible(el) && !el.disabled && el.type !== 'checkbox');
            if (textInputs.length < 2) return { ready: false };

            setValue(textInputs[0], textInputs[0].maxlength === 11 ? '13800138000' : 'shoprpa-smoke-user');
            setValue(textInputs[1], 'ShopRPA123!');

            const checkbox = loginRoot.querySelector('input[type="checkbox"]');
            if (checkbox && !checkbox.checked) checkbox.click();

            const buttons = Array.from(loginRoot.querySelectorAll('button')).filter(isVisible);
            const loginButton = buttons.find((button) => /로그인/.test(button.textContent || '')) || buttons[0];
            if (!loginButton) return { ready: false };
            loginButton.click();
            return { ready: true, loginButtonText: (loginButton.textContent || '').replace(/\\s+/g, ' ').trim() };
          };

          const poll = () => {
            const errorState = getErrorState();
            if (submitted && errorState.text.includes('서버') && errorState.text.includes('확인')) {
              resolve({
                ok: true,
                bodyText: document.body ? document.body.innerText : '',
                errorText: errorState.text,
                inlineErrorVisible: errorState.inlineErrorVisible,
                loginButtonText: submitted.loginButtonText || '',
                submitted: true,
                toastVisible: errorState.toastVisible,
                url: window.location.href,
              });
              return;
            }

            if (!submitted) {
              const submitResult = submitLogin();
              if (submitResult.ready) submitted = submitResult;
            }

            if (Date.now() > deadline) {
              reject(new Error('Backend offline UI error state did not appear after login attempt.'));
              return;
            }
            setTimeout(poll, 200);
          };

          poll();
        })
      `) as Promise<BackendOfflineSmokeResult>,
      30000,
    )

    if (!offlineResult?.ok || !offlineResult.inlineErrorVisible) {
      throw new Error('Backend offline smoke did not find a visible inline auth error state.')
    }
    if (!offlineResult.errorText.includes('서버') || !offlineResult.errorText.includes('확인')) {
      throw new Error(`Backend offline smoke found a weak error message: ${offlineResult.errorText}`)
    }

    let screenshotBytes = 0
    if (screenshotPath) {
      fs.mkdirSync(path.dirname(screenshotPath), { recursive: true })
      const image = await withSmokeTimeout<RendererSmokeImage>('Backend offline smoke screenshot capture', mainWindow.webContents.capturePage() as Promise<RendererSmokeImage>, 10000)
      const png = image.toPNG()
      screenshotBytes = png.length
      if (screenshotBytes <= 0) {
        throw new Error('Backend offline screenshot is empty.')
      }
      fs.writeFileSync(screenshotPath, png)
    }

    logger.info('backend offline UI smoke test passed')
    finishSmokeTest(0, undefined, {
      backendOfflineSmoke: true,
      backendOfflineSmokeResult: offlineResult,
      backendOfflineSmokeNavigationStatus: navigationStatus,
      rendererConsoleErrorCount: rendererErrors.length,
      rendererConsoleErrors: rendererErrors.slice(0, 10),
      rendererNavigationEvents: navigationEvents.slice(0, 80),
      screenshotPath,
      screenshotBytes,
    })
  }
  catch (error) {
    throw withRendererSmokeDiagnostics(error, diagnostics())
  }
}

async function waitForWorkflowEditorSmokeHarness(mainWindow: BrowserWindow) {
  return withSmokeTimeout<boolean>('Workflow editor smoke harness probe', mainWindow.webContents.executeJavaScript(`
    new Promise((resolve, reject) => {
      const deadline = Date.now() + 15000;
      const poll = () => {
        if (typeof window.__SHOPRPA_RUN_WORKFLOW_SMOKE_PHASE__ === 'function') {
          resolve(true);
          return;
        }
        if (Date.now() > deadline) {
          reject(new Error('Workflow editor smoke harness was not installed.'));
          return;
        }
        setTimeout(poll, 100);
      };
      poll();
    })
  `) as Promise<boolean>, 18000)
}

async function runWorkflowEditorSmokePhase(mainWindow: BrowserWindow, phase: 'create-save' | 'reload-edit') {
  return withSmokeTimeout<WorkflowEditorSmokePhaseResult>(
    `Workflow editor smoke phase ${phase}`,
    mainWindow.webContents.executeJavaScript(`
      window.__SHOPRPA_RUN_WORKFLOW_SMOKE_PHASE__(${JSON.stringify(phase)})
    `) as Promise<WorkflowEditorSmokePhaseResult>,
    30000,
  )
}

async function runWorkflowEditorSmokeTest() {
  const rendererErrors: string[] = []
  const navigationEvents: string[] = []
  let mainWindow: BrowserWindow | undefined

  const diagnostics = () => ({
    rendererConsoleErrorCount: rendererErrors.length,
    rendererConsoleErrors: rendererErrors.slice(0, 10),
    rendererCurrentUrl: mainWindow?.webContents.getURL() || '',
    rendererIsLoading: mainWindow?.webContents.isLoading() || false,
    rendererNavigationEvents: navigationEvents.slice(0, 80),
  })

  try {
    if (!isPackagedRuntime) {
      throw new Error('Not running from a packaged runtime layout')
    }
    if (!fs.existsSync(rendererPath)) {
      throw new Error(`Renderer path does not exist: ${rendererPath}`)
    }

    await app.whenReady()
    registerRpaProtocol()
    listenRender()
    ipcMain.handle('main_window_onload', () => {
      globalThis.MainWindowLoaded = true
      return true
    })

    mainWindow = createWindow()
    const url = getRendererWorkflowEditorSmokeUrl()
    const fallbackUrl = isPackagedRuntime ? getRendererWorkflowEditorSmokeFileUrl() : ''
    const screenshotPath = process.env.SHOPRPA_SMOKE_SCREENSHOT

    mainWindow.webContents.on('console-message', (_event, level, message) => {
      if (level >= 2)
        rendererErrors.push(message)
    })
    mainWindow.webContents.on('did-start-loading', () => navigationEvents.push('did-start-loading'))
    mainWindow.webContents.on('did-stop-loading', () => navigationEvents.push('did-stop-loading'))
    mainWindow.webContents.on('did-start-navigation', (_event, targetUrl, isInPlace, isMainFrame) => {
      if (isMainFrame)
        navigationEvents.push(`did-start-navigation:${targetUrl}:${isInPlace}`)
    })
    mainWindow.webContents.on('did-commit-navigation', (_event, targetUrl, isInPlace, isMainFrame) => {
      if (isMainFrame)
        navigationEvents.push(`did-commit-navigation:${targetUrl}:${isInPlace}`)
    })
    mainWindow.webContents.on('did-navigate', (_event, targetUrl) => navigationEvents.push(`did-navigate:${targetUrl}`))
    mainWindow.webContents.on('did-fail-provisional-load', (_event, errorCode, errorDescription, validatedURL, isMainFrame) => {
      if (isMainFrame)
        navigationEvents.push(`did-fail-provisional-load:${errorCode}:${errorDescription}:${validatedURL}`)
    })

    mainWindow.showInactive()
    let navigationStatus = ''
    try {
      navigationStatus = await waitForRendererNavigationWithFallback(mainWindow, url, navigationEvents, fallbackUrl)
    }
    catch (error) {
      const status = `url=${mainWindow.webContents.getURL() || '(empty)'}, loading=${mainWindow.webContents.isLoading()}, events=${navigationEvents.slice(0, 20).join('|') || '(none)'}`
      const reason = error instanceof Error ? error.message : String(error)
      throw new Error(`${reason}; ${status}`)
    }

    if (!mainWindow.isVisible())
      mainWindow.showInactive()
    electronInfo(mainWindow)
    await waitForWorkflowEditorSmokeHarness(mainWindow)
    const createSave = await runWorkflowEditorSmokePhase(mainWindow, 'create-save')
    if (!createSave.ok || createSave.savedNodeCount < 1) {
      throw new Error('Workflow editor create/save phase did not persist a node.')
    }

    navigationEvents.push('workflow-smoke-reload')
    const reloadStatus = await waitForRendererNavigationWithFallback(mainWindow, url, navigationEvents, fallbackUrl)
    await waitForWorkflowEditorSmokeHarness(mainWindow)
    const reloadEdit = await runWorkflowEditorSmokePhase(mainWindow, 'reload-edit')
    if (!reloadEdit.ok || reloadEdit.savedNodeCount < 1) {
      throw new Error('Workflow editor reload/edit phase did not persist a node.')
    }

    const smokeState = await withSmokeTimeout<RendererSmokeState>('Workflow editor smoke state probe', mainWindow.webContents.executeJavaScript(`
    (() => {
      const bodyText = document.body ? document.body.innerText : '';
      const app = document.querySelector('#app');
      return {
        title: document.title,
        bodyText,
        appMounted: Boolean(app && app.children.length > 0),
        url: window.location.href,
      };
    })()
  `) as Promise<RendererSmokeState>, 10000)

    if (!smokeState?.appMounted) {
      throw new Error('Workflow editor renderer app did not mount.')
    }
    if (!String(smokeState.bodyText || '').includes('메시지 대화상자') && !String(smokeState.bodyText || '').includes('프로세스')) {
      throw new Error('Workflow editor smoke did not find expected editor text in the loaded page.')
    }

    let screenshotBytes = 0
    if (screenshotPath) {
      fs.mkdirSync(path.dirname(screenshotPath), { recursive: true })
      const image = await withSmokeTimeout<RendererSmokeImage>('Workflow editor smoke screenshot capture', mainWindow.webContents.capturePage() as Promise<RendererSmokeImage>, 10000)
      const png = image.toPNG()
      screenshotBytes = png.length
      if (screenshotBytes <= 0) {
        throw new Error('Workflow editor screenshot is empty.')
      }
      fs.writeFileSync(screenshotPath, png)
    }

    logger.info('workflow editor smoke test passed')
    finishSmokeTest(0, undefined, {
      workflowEditorSmoke: true,
      workflowEditorSmokeCreateSave: createSave,
      workflowEditorSmokeReloadEdit: reloadEdit,
      workflowEditorSmokeState: smokeState,
      workflowEditorSmokeNavigationStatus: navigationStatus,
      workflowEditorSmokeReloadNavigationStatus: reloadStatus,
      rendererConsoleErrorCount: rendererErrors.length,
      rendererConsoleErrors: rendererErrors.slice(0, 10),
      rendererNavigationEvents: navigationEvents.slice(0, 80),
      screenshotPath,
      screenshotBytes,
    })
  }
  catch (error) {
    throw withRendererSmokeDiagnostics(error, diagnostics())
  }
}

if (isSmokeTest) {
  if (process.env.SHOPRPA_SMOKE_BACKEND_OFFLINE === '1') {
    runBackendOfflineSmokeTest().catch((err) => {
      logger.error('backend offline UI smoke test failed', err instanceof Error ? err.message : String(err))
      finishSmokeTest(1, err, { backendOfflineSmoke: true, ...getRendererSmokeDiagnostics(err) })
    })
  }
  else if (process.env.SHOPRPA_SMOKE_WORKFLOW_EDITOR === '1') {
    runWorkflowEditorSmokeTest().catch((err) => {
      logger.error('workflow editor smoke test failed', err instanceof Error ? err.message : String(err))
      finishSmokeTest(1, err, { workflowEditorSmoke: true, ...getRendererSmokeDiagnostics(err) })
    })
  }
  else if (process.env.SHOPRPA_SMOKE_RENDERER === '1') {
    runRendererSmokeTest().catch((err) => {
      logger.error('packaged renderer smoke test failed', err instanceof Error ? err.message : String(err))
      finishSmokeTest(1, err, { rendererSmoke: true, ...getRendererSmokeDiagnostics(err) })
    })
  }
  else {
    try {
      if (!isPackagedRuntime) {
        throw new Error('Not running from a packaged runtime layout')
      }
      if (!fs.existsSync(rendererPath)) {
        throw new Error(`Renderer path does not exist: ${rendererPath}`)
      }
      logger.info('packaged smoke test passed')
      finishSmokeTest(0)
    }
    catch (err) {
      logger.error('packaged smoke test failed', err instanceof Error ? err.message : String(err))
      finishSmokeTest(1, err)
    }
  }
}
else {
  const gotTheLock = app.requestSingleInstanceLock()
  if (!gotTheLock) {
    app.quit()
  }
  else {
    app.on('second-instance', () => {
      // Focus the existing main window when a second app instance is launched.
      const mainWindow = getMainWindow()
      if (mainWindow) {
        if (mainWindow.isMinimized())
          mainWindow.restore()
        mainWindow.focus()
      }
    })
    app.whenReady().then(ready).catch((err) => {
      logger.error('app ready error', err.toString())
    })
  }

  app.on('window-all-closed', () => {
    app.quit()
  })

  let isQuitting = false
  app.on('before-quit', async (e) => {
    if (isQuitting)
      return
    e.preventDefault()
    isQuitting = true
    await closeSubProcess()
    app.exit()
  })

  ipcMain.handle('ipcCreateWindow', (_event, options) => {
    const local_win = createSubWindow(options)
    const id = local_win.id
    const mainWindow = getMainWindow()
    local_win.once('close', () => {
      mainWindow?.webContents.send('window-close', id)
      options.label && WindowStack.delete(options.label)
    })
    return id
  })

  ipcMain.handle('w2w', (_event, arg: W2WType) => {
    logger.info('w2w', JSON.stringify(arg))
    const targetWin = getWindowFromLabel(arg.target) || getMainWindow()
    targetWin?.webContents.send('w2w', arg)
    return true
  })

  ipcMain.handle('main_window_onload', (_event) => {
    if (globalThis.MainWindowLoaded)
      return true
    startBackend()
    globalThis.MainWindowLoaded = true
    return true
  })

  ipcMain.handle('tray_change', (_event, { mode, status }) => {
    const mainWindow = getMainWindow()
    mainWindow && changeTray(mainWindow, mode, status)
  })
}
