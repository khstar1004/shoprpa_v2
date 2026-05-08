import type { IPluginConfig } from './plugin'

export interface IAppConfig {
  remote_addr: string
  skip_engine_start?: boolean
  app_auth_type: 'uap' | 'casdoor'
  app_edition: 'saas' | 'enterprise'
}

// Window management APIs exposed to the renderer runtime.
export interface WindowManager {
  platform: string
  windows: Map<string, any>
  getScaleFactor: () => Promise<number>
  createWindow: (options?: CreateWindowOptions, closeCallback?: () => void) => Promise<any>
  scaleFactor: () => Promise<number>
  showWindow: (label?: string | number) => Promise<void>
  hideWindow: () => Promise<void>
  closeWindow: (label?: string | number) => void
  maximizeWindow: (always?: boolean) => Promise<boolean>
  minimizeWindow: () => void
  restoreWindow: () => void
  setWindowSize: (params?: { width?: number, height?: number }) => Promise<void>
  setWindowAlwaysOnTop: (alwaysOnTop?: boolean) => Promise<void>
  centerWindow: (options?: any) => void
  isMaximized: () => Promise<boolean>
  isMinimized: () => Promise<boolean>
  showDecorations: () => void
  hideDecorations: () => void
  focusWindow: () => void
  /** @deprecated Use focusWindow. */
  foucsWindow: () => void
  restoreLoginWindow: () => void
  getScreenWorkArea: () => any
  setWindowPosition: (x?: number, y?: number) => Promise<void>
  minLogWindow: (bool: boolean) => void
  onWindowResize: (callback: () => void) => Promise<() => void>
  onWindowClose: (callback: () => void) => void
  emitTo: (msg: WindowMessage) => Promise<any>
}

export type WindowPosition = 'left_top' | 'right_top' | 'left_bottom' | 'right_bottom' | 'top_center' | 'center' | 'right_center'

// Window creation options.
export interface CreateWindowOptions {
  url: string
  position?: WindowPosition
  offset?: number
  x?: number
  y?: number
  width?: number
  height?: number
  minWidth?: number
  minHeight?: number
  maxWidth?: number
  maxHeight?: number
  resizable?: boolean
  title?: string
  label?: string
  fullscreen?: boolean
  focus?: boolean
  transparent?: boolean
  maximized?: boolean
  show?: boolean
  decorations?: boolean
  alwaysOnTop?: boolean
  contentProtected?: boolean
  skipTaskbar?: boolean
  fileDropEnabled?: boolean
  hiddenTitle?: boolean
  acceptFirstMouse?: boolean
  tabbingIdentifier?: string
  userAgent?: string
  maximizable?: boolean
  minimizable?: boolean
  closable?: boolean
}

// Cross-window message payload.
export interface WindowMessage {
  from?: string // window label
  target: string // window label
  type: string // message type
  data?: any // message data
}

// Clipboard APIs exposed to plugins and renderer code.
export interface ClipboardManager {
  writeClipboardText: (text: string) => Promise<void>
  readClipboardText: () => Promise<string>
}

// Runtime environment.
export type AppEnv = 'tauri' | 'electron' | 'browser'

// Utility APIs exposed to plugins and renderer code.
export interface UtilsManager {
  isBrowser?: boolean
  getAppEnv: () => AppEnv
  openInBrowser: (url: string, browser?: string) => void
  listenEvent: (eventName: string, callback: (data: any) => void) => void
  getAppVersion: () => Promise<string>
  getAppPath: () => Promise<string>
  getAppConfig: () => Promise<IAppConfig>
  getUserPath: () => Promise<string>
  getBuildInfo: () => Promise<string>
  getSystemEnv: () => Promise<string>
  getResourcePath: () => Promise<string>
  invoke: (channel: string, ...args: any[]) => Promise<any>
  readFile: (filePath: string, encoding?: string) => Promise<string | Uint8Array | ArrayBuffer>
  saveFile: (fileName: string, buffer: ArrayBuffer | string) => Promise<boolean>
  playVideo: (videoPath: string) => void
  pathJoin: (dirArr: Array<string>) => Promise<string>
  shellopen: (path: string) => Promise<void>
  openPlugins: () => Promise<void>
  showDialog: (dialogProps: any) => Promise<string[]>
  getPluginList: () => Promise<IPluginConfig[]>
}

// Shortcut APIs exposed to plugins and renderer code.
export interface ShortCutManager {
  register: (shortKey: string, handler: any) => void
  unregister: (shortKey: string) => void
  unregisterAll: () => void
  registerToolbar: () => void
  registerFlow: () => void
  /** @deprecated Use registerToolbar. */
  regeisterToolbar: () => void
  /** @deprecated Use registerFlow. */
  regeisterFlow: () => void
}

// Update manifest returned by the updater.
export interface UpdateManifest {
  version: string
  date: string
  body: string
}

// Update status returned by the updater.
export interface UpdateInfo {
  couldUpdate: boolean
  downloaded?: boolean
  manifest?: UpdateManifest | null
}

// Updater APIs exposed to the renderer runtime.
export interface UpdaterManager {
  checkUpdate: () => Promise<UpdateInfo>
  quitAndInstall: () => void
}
