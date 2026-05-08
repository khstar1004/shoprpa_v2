import type { AppEnv, UtilsManager as UtilsManagerType } from '@rpa/shared/platform'

import type { DialogObj } from '../types'

import ClipboardManager from './clipboardManager'

const { ipcRenderer } = window.electron

function getAppEnv(): AppEnv {
  return 'electron'
}

function openInBrowser(url: string) {
  ipcRenderer.send('open-in-browser', url)
}

function listenEvent(eventName: string, callback: (data: any) => void) {
  ipcRenderer.on(eventName, (_event, data) => callback(data))
}

function getFromElectronInfo<T>(key: string, defaultValue: T): Promise<T> {
  return new Promise((resolve) => {
    const electronInfo = localStorage.getItem('electron')
    if (electronInfo) {
      try {
        const info = JSON.parse(electronInfo)
        resolve(info[key] ?? defaultValue)
      }
      catch {
        resolve(defaultValue)
      }
    }
    else {
      resolve(defaultValue)
    }
  })
}

function getAppVersion() {
  return getFromElectronInfo<string>('appVersion', 'latest')
}

const getAppConfig: UtilsManagerType['getAppConfig'] = () => {
  return ipcRenderer.invoke('get-app-config')
}

function getAppPath() {
  return getFromElectronInfo<string>('appPath', '')
}

function getUserPath() {
  return getFromElectronInfo<string>('userDataPath', '')
}

function getResourcePath() {
  return getFromElectronInfo<string>('resourcePath', '')
}

function getAppWorkPath() {
  return getFromElectronInfo<string>('appWorkPath', '')
}

async function getBuildInfo() {
  const electronVersion = await getFromElectronInfo<string>('electronVersion', '')
  return `ShopRPA Desktop (Electron ${electronVersion})`
}

function getSystemEnv() {
  return new Promise<string>((resolve) => {
    const electronInfo = localStorage.getItem('electron')
    if (electronInfo) {
      const { arch, platform, release } = JSON.parse(electronInfo)
      const sysInfo = `${release} ${platform} ${arch} `
      resolve(sysInfo)
    }
    else {
      resolve('')
    }
  })
}

function invoke(channel: string, ...args: any[]) {
  return ipcRenderer.invoke(channel, ...args)
}

const readFile: UtilsManagerType['readFile'] = (filePath, encoding) => {
  return ipcRenderer.invoke('read-file', filePath, encoding)
}

const saveFile: UtilsManagerType['saveFile'] = (fileName, buffer) => {
  return ipcRenderer.invoke('save-file', fileName, buffer)
}

function playVideo(videoPath: string) {
  void ipcRenderer.invoke('open-path', videoPath).catch(() => undefined)
}

function pathJoin(dirArr: Array<string>) {
  return ipcRenderer.invoke('path-join', ...dirArr)
}

function shellopen(path: string) {
  return new Promise<void>((resolve, reject) => {
    const fullPath = path.replace(/\\/g, '/')
    ClipboardManager.writeClipboardText(fullPath)
    ipcRenderer
      .invoke('open-path', fullPath)
      .then((res) => {
        if (res) {
          resolve()
        }
        else {
          reject(new Error('경로를 열 수 없습니다.'))
        }
      })
      .catch((err) => {
        reject(err)
      })
  })
}

async function openPlugins() {
  const appWorkPath = await getAppWorkPath()
  const fallbackPath = await getUserPath()
  const pluginPath = await pathJoin([
    appWorkPath || fallbackPath,
    'python_core',
    'Lib',
    'site-packages',
    'astronverse',
    'browser_plugin',
    'plugins',
  ])
  await shellopen(pluginPath)
}

const showDialog: UtilsManagerType['showDialog'] = async (dialogProps) => {
  const { file_type, filters: dialogFilters, multiple, defaultPath = '' } = dialogProps
  const isDirectory = file_type === 'folder'
  const isMultiple = file_type === 'files' ? true : (file_type === 'file' ? multiple : false)
  const filterExtensions = dialogFilters?.map((item: string) => item.replace('.', ''))
  const filters = filterExtensions ? [{ name: '', extensions: filterExtensions }] : []

  const properties = [
    isDirectory ? 'openDirectory' : 'openFile',
    isMultiple ? 'multiSelections' : undefined,
  ].filter(Boolean) as string[]

  const dialogObj: DialogObj = { title: '파일 또는 폴더 선택', defaultPath, properties, filters }
  return await ipcRenderer.invoke('open-dialog', dialogObj)
}

const getPluginList: UtilsManagerType['getPluginList'] = async () => {
  return ipcRenderer.invoke('get-plugin-list')
}

const UtilsManager: UtilsManagerType = {
  getAppEnv,
  getAppPath,
  getAppVersion,
  getAppConfig,
  getBuildInfo,
  getSystemEnv,
  getUserPath,
  listenEvent,
  readFile,
  saveFile,
  invoke,
  openInBrowser,
  openPlugins,
  pathJoin,
  playVideo,
  shellopen,
  showDialog,
  getPluginList,
  getResourcePath,
}

export default UtilsManager
