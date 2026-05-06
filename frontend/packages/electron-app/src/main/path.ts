import path from 'node:path'

import { app } from 'electron'

export const appPath = app.getAppPath()
export const userDataPath = app.getPath('userData')
export const appDataPath = app.getPath('appData')

// 열기패키지후, 파일저장에서 appPath 아래의 resources 디렉터리, 아니오이면저장에서디렉터리아래의 resources 디렉터리
export const resourcePath = app.isPackaged ? path.join(appPath, '../') : path.join(appPath, '../../../resources')
// 열기패키지후, 데이터저장에서 userDataPath , 아니오이면저장에서 appPath 아래의 data 디렉터리
export const appWorkPath = app.isPackaged ? userDataPath : path.join(appPath, 'data')
export const pythonCore = path.join(appWorkPath, 'python_core')
export const pythonExe = path.join(pythonCore, 'python.exe')
export const confPath = path.join(resourcePath, 'conf.yaml')
export const d7zrPath = path.join(resourcePath, '7zr.exe')
// 확장디렉터리
export const extensionPath = [
  path.join(appPath, 'extensions'), // 시스템확장디렉터리
  path.join(appWorkPath, 'extensions'), // 사용자확장디렉터리
]
export const extensionHost = 'extensions'
export const extensionBaseUrl =  `rpa://${extensionHost}/`

export const rendererPath = path.join(__dirname, '../renderer')
export const windowBaseUrl  = app.isPackaged ? 'rpa://localhost/' : 'http://localhost:1420/'

export function openPath(targetPath: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const { exec } = require('node:child_process')
    const path = require('node:path')

    // 필요열기의파일또는폴더 경로
    targetPath = path.resolve(targetPath)

    // 근거운영체제선택명령
    const openCommand = process.platform === 'win32' ? `start "" "${targetPath}"` : `xdg-open "${targetPath}"`

    exec(openCommand, (error) => {
      error ? reject(error) : resolve()
    })
  })
}