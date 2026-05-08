import fs from 'node:fs'
import path from 'node:path'

import { defineConfig } from 'vitest/config'

function resolveInstalledPlaywrightChromium() {
  const roots = [
    process.env.PLAYWRIGHT_BROWSERS_PATH,
    process.env.LOCALAPPDATA ? path.join(process.env.LOCALAPPDATA, 'ms-playwright') : '',
  ].filter(Boolean)

  for (const root of roots) {
    if (!fs.existsSync(root))
      continue

    const candidates = fs.readdirSync(root, { withFileTypes: true })
      .filter(entry => entry.isDirectory() && entry.name.startsWith('chromium-'))
      .map(entry => entry.name)
      .sort()
      .reverse()

    for (const dirName of candidates) {
      for (const relativeExe of ['chrome-win64/chrome.exe', 'chrome-win/chrome.exe']) {
        const executablePath = path.join(root, dirName, relativeExe)
        if (fs.existsSync(executablePath))
          return executablePath
      }
    }
  }

  return ''
}

function firstExistingFile(paths) {
  return paths.find(candidate => candidate && fs.existsSync(candidate)) || ''
}

function resolveChromiumExecutable() {
  return firstExistingFile([
    process.env.SHOPRPA_PLAYWRIGHT_EXECUTABLE,
    resolveInstalledPlaywrightChromium(),
    'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
    'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
    'C:/Program Files/Google/Chrome/Application/chrome.exe',
    'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
  ])
}

const chromiumExecutablePath = resolveChromiumExecutable()
const chromiumLaunchOptions = chromiumExecutablePath ? { executablePath: chromiumExecutablePath } : {}

export default defineConfig({
  test: {
    include: ['src/test/content.*.{test,spec}.js'],
    exclude: ['src/test/background.*.{test,spec}.js'],
    browser: {
      provider: 'playwright', // or 'webdriverio'
      enabled: true,
      // at least one instance is required
      headless: true,
      instances: [{
        browser: 'chromium',
        launch: chromiumLaunchOptions,
      }],
    },
  },
})
