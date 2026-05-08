import http from './http'
import { hasLocalRuntimeRoute } from './runtime'

// 가져오기확장지원의브라우저
export async function getSupportBrowser() {
  if (!await hasLocalRuntimeRoute())
    return []

  const res = await http.get<{ browsers: string[] }>('/scheduler/browser/plugins/get_support', null, {
    toast: false,
  })

  return res.data.browsers
}

// 브라우저 확장조회상태
export function checkBrowserPlugin(browsers: string[]) {
  return http.post<Record<string, { installed: boolean, installed_version: string, latest: boolean, browser_installed: boolean }>>(
    '/scheduler/browser/plugins/check_status',
    { browsers },
    { toast: false },
  )
}

// 브라우저 확장설치
export function browserPluginInstall(params) {
  return http.post(
    '/scheduler/browser/plugins/install',
    {
      op: params.action, // 추가/업데이트
      browser: params.type,
    },
    { toast: false },
  )
}

// 확장설치전브라우저여부정상에서실행의감지
export function checkBrowserRunning(params) {
  return http.post(
    '/scheduler/browser/plugins/check_running',
    {
      browser: params.type,
    },
    { toast: false },
  )
}

// 일설치모든업데이트의브라우저 확장
export function installAllUpdateBrowserPlugin() {
  return http.post(
    '/scheduler/browser/plugins/install_all_updates',
    {},
    { toast: false },
  )
}

export const checkBrowerPlugin = checkBrowserPlugin
export const browerPluginInstall = browserPluginInstall
export const checkBrowerRunning = checkBrowserRunning
export const installAllUpdateBrowerPlugin = installAllUpdateBrowserPlugin
