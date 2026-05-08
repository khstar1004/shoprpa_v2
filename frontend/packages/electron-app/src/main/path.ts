import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'

import { app, shell } from 'electron'

const isSmokeTest = process.argv.includes('--smoke-test') || process.env.SHOPRPA_SMOKE_TEST === '1'
if (isSmokeTest) {
  const smokeUserDataPath = process.env.SHOPRPA_SMOKE_USER_DATA
    || path.join(os.tmpdir(), `shoprpa-smoke-${process.pid}`)
  app.setPath('userData', smokeUserDataPath)
}

export const appPath = app.getAppPath()
export const userDataPath = app.getPath('userData')
export const appDataPath = app.getPath('appData')
const appPathName = path.basename(appPath).toLowerCase()
const appPathParentName = path.basename(path.dirname(appPath)).toLowerCase()
export const isPackagedRuntime = app.isPackaged
  || ((appPathName === 'app' || appPathName === 'app.asar') && appPathParentName === 'resources')

// Packaged builds keep runtime resources next to app.asar; dev builds use the repo resources directory.
export const resourcePath = isPackagedRuntime ? path.join(appPath, '../') : path.join(appPath, '../../../resources')
// Packaged builds store mutable data in Electron userData; dev builds keep it under the app directory.
export const appWorkPath = isPackagedRuntime ? userDataPath : path.join(appPath, 'data')
export const pythonCore = path.join(appWorkPath, 'python_core')
export const pythonExe = path.join(pythonCore, 'python.exe')
export const confPath = path.join(resourcePath, 'conf.yaml')
export const d7zrPath = path.join(resourcePath, '7zr.exe')
export const extensionPath = [
  path.join(appPath, 'extensions'),
  path.join(appWorkPath, 'extensions'),
]
export const extensionHost = 'extensions'
export const extensionBaseUrl = `rpa://${extensionHost}/`

const bundledRendererPath = path.join(resourcePath, 'renderer')
export const rendererPath = isPackagedRuntime && fs.existsSync(bundledRendererPath)
  ? bundledRendererPath
  : path.join(__dirname, '../renderer')
export const windowBaseUrl = isPackagedRuntime ? 'rpa://localhost/' : 'http://localhost:1420/'

export async function openPath(targetPath: string): Promise<void> {
  if (typeof targetPath !== 'string' || !targetPath.trim()) {
    throw new Error('Invalid path')
  }
  const resolvedPath = path.resolve(targetPath)
  const errorMessage = await shell.openPath(resolvedPath)
  if (errorMessage) {
    throw new Error(errorMessage)
  }
}
