import type { WindowManager } from '@rpa/shared/platform'
import { to } from 'await-to-js'
import { noop } from 'lodash-es'

const { ipcRenderer } = window.electron

const loginWinState = {
  width: 1280,
  height: 750,
  maximized: false,
  center: true,
}

class ElectronWindowManager implements WindowManager {
  public platform = 'electron'
  public windows = new Map<string, any>()

  public async createWindow(options: any, closeCallback?: () => void): Promise<number | string> {
    const winId = await ipcRenderer.invoke('ipcCreateWindow', {
      ...options,
      title: options.title || options.label,
    })
    ipcRenderer.on('window-close', (_ev, id) => {
      id === winId && closeCallback?.()
    })
    return winId
  }

  async emitTo(message: {
    target: string
    type: string
    data?: any
    from?: string
  }): Promise<any> {
    return ipcRenderer.invoke('w2w', message)
  }

  async showWindow() {
    ipcRenderer.send('window-show')
  }

  async hideWindow() {
    ipcRenderer.send('window-hide')
  }

  async maximizeWindow(always: boolean = false): Promise<boolean> {
    const isMaximized = await this.isMaximized()

    if (isMaximized && !always) {
      await ipcRenderer.invoke('window-unmaximize')
      this.centerWindow()
      return false
    }
    else {
      await ipcRenderer.invoke('window-maximize')
      return true
    }
  }

  async minimizeWindow() {
    return await ipcRenderer.invoke('window-minimize')
  }

  async restoreWindow() {
    return await ipcRenderer.invoke('window-restore')
  }

  closeWindow(label?: string | number) {
    ipcRenderer.send('window-close', { label })
  }

  setWindowSize: WindowManager['setWindowSize'] = async (params) => {
    const width = params?.width ?? screen.availWidth - 40
    const height = params?.height ?? screen.availHeight - 40

    ipcRenderer.send('window-set-size', width, height)
  }

  getScaleFactor() {
    return Promise.resolve(window.devicePixelRatio || 1)
  }

  scaleFactor() {
    return Promise.resolve(window.devicePixelRatio || 1)
  }

  async setWindowAlwaysOnTop(alwaysOnTop: boolean = true) {
    ipcRenderer.send('window-set-always-on-top', alwaysOnTop)
  }

  async isMaximized(): Promise<boolean> {
    const [err, isMaximized] = await to<boolean>(ipcRenderer.invoke('window-is-maximized'))
    if (err) {
      return false
    }

    return isMaximized
  }

  async isMinimized(): Promise<boolean> {
    const [err, isMinimized] = await to<boolean>(ipcRenderer.invoke('window-is-minimized'))
    if (err) {
      return false
    }

    return isMinimized
  }

  async focusWindow() {
    const [err, focused] = await to<boolean>(ipcRenderer.invoke('window-focus'))

    if (err) {
      return
    }

    if (!focused) {
      setTimeout(() => ipcRenderer.invoke('window-focus'), 300)
    }
  }

  async foucsWindow() {
    return this.focusWindow()
  }

  restoreLoginWindow() {
    ipcRenderer.send('window-set-size', loginWinState.width, loginWinState.height)
    this.centerWindow()
  }

  centerWindow() {
    ipcRenderer.send('window-center')
  }

  getScreenWorkArea() {
    return new Promise((resolve, reject) => {
      ipcRenderer
        .invoke('get-workarea')
        .then((workArea) => {
          resolve(workArea)
        })
        .catch((err) => {
          reject(err)
        })
    })
  }

  /**
   * set physical window position
   * @param x
   * @param y
   */
  async setWindowPosition(x: number = 0, y: number = 0) {
    const dpr = devicePixelRatio
    ipcRenderer.send('window-set-position', Math.floor(x / dpr), Math.floor(y / dpr))
  }

  /**
   * set logical window position
   * @param x
   * @param y
   */
  async setLogicalWindowPosition(x: number = 0, y: number = 0) {
    ipcRenderer.send('window-set-position', x, y)
  }

  showDecorations() {
    ipcRenderer.send('window-set-menubar', true)
  }

  hideDecorations() {
    // electron doesn't support hide close buttons
    ipcRenderer.send('window-set-menubar', false)
  }

  minLogWindow(bool: boolean) {
    const initialWidth = Math.floor(window.screen.availWidth)
    const initialHeight = Math.floor(window.screen.availHeight)
    if (bool) {
      this.setWindowSize({ width: 32, height: 128 })
      this.setLogicalWindowPosition(initialWidth - 32 - 2, initialHeight - 128 - 2)
    }
    else {
      this.setWindowSize({ width: 360, height: 128 })
      this.setLogicalWindowPosition(initialWidth - 360 - 2, initialHeight - 128 - 2)
    }
  }

  async onWindowResize(callback: () => void) {
    window.addEventListener('resize', callback)
    return noop
  }

  async onWindowClose(callback: () => void) {
    ipcRenderer.on('window-close-confirm', (_ev, _arg) => callback?.())
  }
}

export default ElectronWindowManager
