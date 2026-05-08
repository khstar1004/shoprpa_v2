const fs = require('node:fs')
const http = require('node:http')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..')
const frontendRoot = path.join(repoRoot, 'frontend')
const playwrightPath = path.join(frontendRoot, 'node_modules', 'playwright')
const { chromium } = require(playwrightPath)

const extensionPath = path.join(repoRoot, 'frontend', 'packages', 'browser-plugin', 'dist')
const manifestPath = path.join(extensionPath, 'manifest.json')
const evidenceRoot = path.join(repoRoot, 'build', 'browser-extension-smoke')
const reportPath = path.join(evidenceRoot, 'browser-extension-smoke-report.json')
const userDataDir = path.join(evidenceRoot, 'chromium-profile')
const screenshotPath = path.join(evidenceRoot, 'browser-extension-smoke.png')
const pageHtmlPath = path.join(evidenceRoot, 'browser-extension-smoke.html')

function resolveInstalledPlaywrightChromiumExecutables() {
  const roots = [
    process.env.LOCALAPPDATA ? path.join(process.env.LOCALAPPDATA, 'ms-playwright') : '',
    process.env.PLAYWRIGHT_BROWSERS_PATH || '',
  ].filter(Boolean)

  const executables = []
  for (const root of roots) {
    if (!fs.existsSync(root)) {
      continue
    }
    const chromiumDirs = fs.readdirSync(root, { withFileTypes: true })
      .filter(entry => entry.isDirectory() && entry.name.startsWith('chromium-'))
      .map(entry => entry.name)
      .sort()
      .reverse()

    for (const dirName of chromiumDirs) {
      for (const relativeExe of ['chrome-win64\\chrome.exe', 'chrome-win\\chrome.exe']) {
        executables.push(path.join(root, dirName, relativeExe))
      }
    }
  }

  return executables
}

function resolveBrowserExecutables() {
  const candidates = [
    process.env.SHOPRPA_PLAYWRIGHT_EXECUTABLE,
    'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
    chromium.executablePath(),
    ...resolveInstalledPlaywrightChromiumExecutables(),
  ].filter(Boolean)

  const executables = candidates.filter(candidate => fs.existsSync(candidate))

  if (executables.length > 0) {
    return [...new Set(executables)]
  }

  throw new Error(`No Chromium-compatible browser executable found. Checked: ${candidates.join(', ')}`)
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'))
}

function removeDirIfExists(dirPath) {
  if (fs.existsSync(dirPath)) {
    fs.rmSync(dirPath, { recursive: true, force: true })
  }
}

function createTestHtml() {
  return `<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <title>ShopRPA Browser Extension Smoke</title>
  <style>
    body { font-family: Arial, sans-serif; margin: 32px; color: #202124; }
    main { max-width: 720px; }
    label, input, button { font-size: 16px; }
    input { display: block; margin: 8px 0 16px; padding: 8px 10px; width: 280px; }
    button { padding: 8px 14px; }
    table { border-collapse: collapse; margin-top: 20px; width: 360px; }
    th, td { border: 1px solid #d0d7de; padding: 8px 10px; text-align: left; }
    #status { margin-top: 16px; font-weight: 700; color: #116329; }
  </style>
</head>
<body>
  <main>
    <h1>ShopRPA Browser Extension Smoke</h1>
    <label for="customerName">고객명</label>
    <input id="customerName" name="customerName" placeholder="고객명 입력">
    <button id="saveButton" type="button">저장</button>
    <p id="status">대기</p>
    <table id="orders">
      <thead>
        <tr><th>상품</th><th>수량</th></tr>
      </thead>
      <tbody>
        <tr><td>Invoice</td><td>3</td></tr>
        <tr><td>Report</td><td>2</td></tr>
      </tbody>
    </table>
  </main>
  <script>
    document.getElementById('saveButton').addEventListener('click', () => {
      const value = document.getElementById('customerName').value || 'EMPTY'
      document.getElementById('status').textContent = '저장됨: ' + value
    })
  </script>
</body>
</html>`
}

function startServer(html) {
  const server = http.createServer((req, res) => {
    if (req.url === '/' || req.url === '/index.html') {
      res.writeHead(200, {
        'content-type': 'text/html; charset=utf-8',
        'cache-control': 'no-store',
      })
      res.end(html)
      return
    }

    res.writeHead(404, { 'content-type': 'text/plain; charset=utf-8' })
    res.end('not found')
  })

  return new Promise((resolve, reject) => {
    server.once('error', reject)
    server.listen(0, '127.0.0.1', () => {
      const address = server.address()
      resolve({
        server,
        url: `http://127.0.0.1:${address.port}/index.html`,
      })
    })
  })
}

function isExtensionServiceWorker(worker) {
  const workerUrl = worker && typeof worker.url === 'function' ? worker.url() : ''
  return /^chrome-extension:\/\/[^/]+\/background\.js(?:$|\?)/.test(workerUrl)
}

async function getServiceWorker(context) {
  const seenUrls = new Set()
  const rememberWorkers = () => {
    for (const worker of context.serviceWorkers()) {
      seenUrls.add(worker.url())
    }
  }

  rememberWorkers()
  let serviceWorker = context.serviceWorkers().find(isExtensionServiceWorker)
  const deadline = Date.now() + 15000
  while (!serviceWorker && Date.now() < deadline) {
    const remainingMs = Math.max(1, deadline - Date.now())
    const worker = await context.waitForEvent('serviceworker', { timeout: remainingMs }).catch(() => null)
    if (worker) {
      seenUrls.add(worker.url())
      if (isExtensionServiceWorker(worker)) {
        serviceWorker = worker
        break
      }
    }
    rememberWorkers()
    serviceWorker = context.serviceWorkers().find(isExtensionServiceWorker)
  }

  assert(
    serviceWorker,
    `Could not find ShopRPA extension service worker. Saw workers: ${Array.from(seenUrls).join(', ') || '(none)'}`,
  )
  return serviceWorker
}

async function gotoWithRetry(page, url) {
  let lastError = null
  for (let attempt = 1; attempt <= 3; attempt += 1) {
    try {
      await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 15000 })
      await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => undefined)
      return
    }
    catch (error) {
      lastError = error
      const message = error && error.message ? error.message : String(error)
      if (!message.includes('ERR_ABORTED') || attempt === 3) {
        throw error
      }
      await page.waitForTimeout(1000)
    }
  }
  throw lastError
}

async function createContentScriptRuntime(context, page, extensionId) {
  const cdpSession = await context.newCDPSession(page)
  const contexts = []
  cdpSession.on('Runtime.executionContextCreated', event => contexts.push(event.context))
  await cdpSession.send('Runtime.enable')

  const expectedOrigin = `chrome-extension://${extensionId}`
  const deadline = Date.now() + 10000
  while (Date.now() < deadline) {
    const contentContext = contexts.find(contextInfo =>
      contextInfo.origin === expectedOrigin && contextInfo.auxData && contextInfo.auxData.type === 'isolated',
    )
    if (contentContext) {
      return {
        cdpSession,
        contextId: contentContext.id,
      }
    }
    await page.waitForTimeout(250)
  }

  const seenContexts = contexts
    .map(contextInfo => `${contextInfo.id}:${contextInfo.name || '(default)'}:${contextInfo.origin || '(none)'}`)
    .join(', ')
  throw new Error(`Could not find ShopRPA content script execution context for ${expectedOrigin}. Saw contexts: ${seenContexts || '(none)'}`)
}

async function evaluateContentHandler(contentRuntime, message) {
  const expression = `Promise.resolve(window.handle(${JSON.stringify(message)}))`
  const result = await contentRuntime.cdpSession.send('Runtime.evaluate', {
    contextId: contentRuntime.contextId,
    expression,
    awaitPromise: true,
    returnByValue: true,
  })
  if (result.exceptionDetails) {
    const exceptionText = result.exceptionDetails.exception && result.exceptionDetails.exception.description
      ? result.exceptionDetails.exception.description
      : JSON.stringify(result.exceptionDetails)
    throw new Error(exceptionText)
  }
  return result.result ? result.result.value : undefined
}

function expectSuccess(result, label) {
  assert(result, `${label}: no response`)
  if (result.code) {
    assert(result.code === '0000', `${label}: expected code 0000, got ${result.code} (${result.msg || ''})`)
  }
  return result
}

async function main() {
  assert(fs.existsSync(manifestPath), `Browser extension build is missing: ${manifestPath}. Run corepack pnpm --dir frontend --filter @rpa/extension run build:chromium.`)

  const manifest = readJson(manifestPath)
  assert(manifest.manifest_version === 3, 'Browser extension manifest must be MV3.')
  assert(manifest.background && manifest.background.service_worker, 'Browser extension service worker is missing.')
  assert(Array.isArray(manifest.content_scripts) && manifest.content_scripts.length > 0, 'Browser extension content script is missing.')

  fs.mkdirSync(evidenceRoot, { recursive: true })
  fs.writeFileSync(pageHtmlPath, createTestHtml(), 'utf8')
  if (!process.env.SHOPRPA_BROWSER_CDP_URL) {
    removeDirIfExists(userDataDir)
  }

  const { server, url } = await startServer(fs.readFileSync(pageHtmlPath, 'utf8'))
  let context
  let browser
  const consoleMessages = []
  const scenarioResults = []
  let browserExecutable = ''
  let extensionId = ''

  try {
    if (process.env.SHOPRPA_BROWSER_CDP_URL) {
      browser = await chromium.connectOverCDP(process.env.SHOPRPA_BROWSER_CDP_URL)
      context = browser.contexts()[0]
      browserExecutable = process.env.SHOPRPA_PLAYWRIGHT_EXECUTABLE || `CDP:${process.env.SHOPRPA_BROWSER_CDP_URL}`
      assert(context, `Could not find a browser context from ${process.env.SHOPRPA_BROWSER_CDP_URL}.`)
    }
    else {
      const browserExecutables = resolveBrowserExecutables()
      browserExecutable = browserExecutables.join(' | ')
      const launchErrors = []
      for (const candidate of browserExecutables) {
        try {
          removeDirIfExists(userDataDir)
          context = await chromium.launchPersistentContext(userDataDir, {
            headless: false,
            executablePath: candidate,
            args: [
              `--disable-extensions-except=${extensionPath}`,
              `--load-extension=${extensionPath}`,
              '--no-first-run',
              '--no-default-browser-check',
            ],
          })
          browserExecutable = candidate
          break
        }
        catch (error) {
          const message = error && error.message ? error.message.split(/\r?\n/)[0] : String(error)
          launchErrors.push(`${candidate} -> ${message}`)
        }
      }

      if (!context) {
        throw new Error(`Could not launch Chromium for extension smoke. ${launchErrors.join(' | ')}`)
      }
    }

    const serviceWorker = await getServiceWorker(context)
    const serviceWorkerUrl = serviceWorker.url()
    extensionId = new URL(serviceWorkerUrl).host
    assert(extensionId, 'Could not resolve loaded extension id.')
    scenarioResults.push({ id: 'extension-load', status: 'PASS', detail: `Loaded ${extensionId} from ${serviceWorkerUrl}` })

    const page = context.pages()[0] || await context.newPage()
    page.on('console', msg => consoleMessages.push(`${msg.type()}: ${msg.text()}`))
    await gotoWithRetry(page, url)
    await page.bringToFront()

    const contentRuntime = await createContentScriptRuntime(context, page, extensionId)
    scenarioResults.push({ id: 'active-tab', status: 'PASS', detail: page.url() })

    const dpr = expectSuccess(await evaluateContentHandler(contentRuntime, { key: 'getDPR', data: {} }), 'getDPR')
    assert(typeof dpr.dpr === 'number', 'getDPR did not return a numeric DPR.')
    scenarioResults.push({ id: 'content-script-message', status: 'PASS', detail: `DPR ${dpr.dpr}` })

    const inputElement = expectSuccess(await evaluateContentHandler(contentRuntime, {
      key: 'generateElement',
      data: { type: 'cssSelector', value: '#customerName', returnType: 'single' },
    }), 'generate input element').data
    assert(inputElement && inputElement.cssSelector, 'Input element metadata is missing.')

    expectSuccess(await evaluateContentHandler(contentRuntime, {
      key: 'inputElement',
      data: { ...inputElement, atomConfig: { inputText: 'ShopRPA Smoke' } },
    }), 'input element')

    const inputText = expectSuccess(await evaluateContentHandler(contentRuntime, {
      key: 'getElementText',
      data: inputElement,
    }), 'get input text').data
    assert(inputText === 'ShopRPA Smoke', `Input text mismatch: ${inputText}`)
    scenarioResults.push({ id: 'element-input', status: 'PASS', detail: inputText })

    const buttonElement = expectSuccess(await evaluateContentHandler(contentRuntime, {
      key: 'generateElement',
      data: { type: 'cssSelector', value: '#saveButton', returnType: 'single' },
    }), 'generate button element').data
    expectSuccess(await evaluateContentHandler(contentRuntime, {
      key: 'clickElement',
      data: { ...buttonElement, atomConfig: { buttonType: 'click' } },
    }), 'click button')
    await page.waitForFunction(() => document.querySelector('#status')?.textContent === '저장됨: ShopRPA Smoke', null, { timeout: 5000 })
    scenarioResults.push({ id: 'element-click', status: 'PASS', detail: await page.locator('#status').textContent() })

    const tableElement = expectSuccess(await evaluateContentHandler(contentRuntime, {
      key: 'generateElement',
      data: { type: 'cssSelector', value: '#orders', returnType: 'single' },
    }), 'generate table element').data
    const tableData = expectSuccess(await evaluateContentHandler(contentRuntime, {
      key: 'getTableData',
      data: { ...tableElement, atomConfig: {} },
    }), 'get table data').data
    assert(tableData, 'Table data response is empty.')
    scenarioResults.push({ id: 'table-extract', status: 'PASS', detail: JSON.stringify(tableData).slice(0, 240) })

    await page.screenshot({ path: screenshotPath, fullPage: true })

    const report = {
      schemaVersion: 1,
      ok: true,
      generatedAt: new Date().toISOString(),
      extensionPath,
      manifestVersion: manifest.version,
      extensionId,
      browserExecutable,
      cdpUrl: process.env.SHOPRPA_BROWSER_CDP_URL || '',
      testPageUrl: url,
      scenarios: scenarioResults,
      evidence: {
        screenshot: path.relative(repoRoot, screenshotPath),
        html: path.relative(repoRoot, pageHtmlPath),
      },
      consoleMessages,
      failures: [],
    }
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2), 'utf8')
    console.log(`Browser extension smoke passed: ${reportPath}`)
  }
  catch (error) {
    const report = {
      schemaVersion: 1,
      ok: false,
      generatedAt: new Date().toISOString(),
      extensionPath,
      manifestVersion: manifest?.version || '',
      extensionId,
      browserExecutable,
      cdpUrl: process.env.SHOPRPA_BROWSER_CDP_URL || '',
      testPageUrl: typeof url === 'string' ? url : '',
      scenarios: scenarioResults,
      evidence: {
        screenshot: fs.existsSync(screenshotPath) ? path.relative(repoRoot, screenshotPath) : '',
        html: path.relative(repoRoot, pageHtmlPath),
      },
      failures: [error && error.stack ? error.stack : String(error)],
    }
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2), 'utf8')
    throw error
  }
  finally {
    if (browser) {
      await browser.close().catch(() => undefined)
    }
    else if (context) {
      await context.close().catch(() => undefined)
    }
    await new Promise(resolve => server.close(resolve))
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exit(1)
})
