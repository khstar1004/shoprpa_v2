/// <reference types="@rpa/shared/platform" />

export interface W2WType {
  from: string // 창
  target: string // 목록창
  type: string // 유형
  data?: any // 데이터
}

export interface DialogObj {
  title: string
  multiple?: boolean
  directory?: boolean
  properties?: string[]
  filters?: any[]
  defaultPath?: string
}

export interface AxiosResponse<T = any> {
  code: string | number
  message: string
  data: T
}

declare global {
  interface Window {
    electron: {
      ipcRenderer: {
        invoke: (channel: string, ...args: any[]) => Promise<any>
        send: (channel: string, ...args: any[]) => void
        sendTo: (webContentsId: number, channel: string, ...args: any[]) => void
        on: (channel: string, listener: (...args: any[]) => void) => void
        off: (channel: string, listener: (...args: any[]) => void) => void
      }
      globalShortcut: {
        register: (shortcut: string, callback: () => void) => Promise<boolean>
        unregister: (shortcut: string) => Promise<boolean>
        unregisterAll: () => Promise<boolean>
      }
      clipboard: {
        readText: () => Promise<string>
        writeText: (text: string) => Promise<boolean>
      }
    }
    api: unknown
  }
}