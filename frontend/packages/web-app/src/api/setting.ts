import { blob2Text } from '@/utils/common'

import { fileRead, fileWrite } from '@/api/resource'
import type { ITableResponse } from '@/types/normalTable'

import http from './http'
import { hasLocalRuntimeRoute } from './runtime'

const useSettingPath = './.setting.json'

export async function getUserSetting() {
  if (!await hasLocalRuntimeRoute())
    return {}

  try {
    const { data } = await fileRead({ path: useSettingPath })
    const result = await blob2Text<string>(data)
    return JSON.parse(result || '{}')
  }
  catch {
    return {}
  }
}

export async function setUserSetting(params: RPA.UserSetting) {
  return fileWrite({ path: useSettingPath, mode: 'w', content: JSON.stringify(params) })
}

/**
 * @returns 가져오기 시작상태
 */
export async function autoStartStatus() {
  if (!await hasLocalRuntimeRoute())
    return false

  const res = await http.post<{ autostart: boolean }>('/scheduler/window/auto_start/check', null)

  return res.data.autostart
}
/**
 * @returns 시작
 */
export function autoStartEnable() {
  return http.post('/scheduler/window/auto_start/enable', null)
}
/**
 * @returns 닫기 시작
 */
export function autoStartDisable() {
  return http.post('/scheduler/window/auto_start/disable', null)
}
/**
 * @returns 조회파일여부저장에서
 */
export function checkVideoPaths(data) {
  return http.post('/scheduler/video/play', data, { toast: false })
}

/**
 * @description: 메일함짧음정보
 */
export function toolsInterfacePost(data) {
  return http.post('/scheduler/alert/test', data)
}

/**
 * @description: 가져오기Api Key목록 데이터
 */
export async function getApis(params) {
  const res = await http.get<ITableResponse>('/api/rpa-openapi/api-keys/get', params)
  return res.data || { records: [], total: 0 }
}

/**
 * @description: 삭제API Key
 */
export function deleteAPI(params) {
  return http.post('/api/rpa-openapi/api-keys/remove', params)
}

/**
 * @description: 추가API Key
 */
export async function createAPI(params) {
  const res = await http.post('/api/rpa-openapi/api-keys/create', params)
  return res.data
}

/**
 * @description: 가져오기Agent Api Key목록 데이터
 */
export async function getAgentApis(params) {
  const res = await http.get('/api/rpa-openapi/api-keys/get-astron', params)
  return res.data
}

/**
 * @description: 삭제Agent API Key
 */
export function deleteAgentAPI(id: number) {
  return http.post('/api/rpa-openapi/api-keys/remove-astron', { id })
}

/**
 * @description: 추가Agent API Key
 */
export async function createAgentAPI<T>(params: T) {
  const res = await http.post<{ id: number }>('/api/rpa-openapi/api-keys/create-astron', params)
  return res.data
}

/**
 * @description: 업데이트Agent API Key
 * @param params
 * @returns
 */
export async function updateAgentApi<T>(params: T) {
  const res = await http.post<{ id: number }>('/api/rpa-openapi/api-keys/update-astron', params)
  return res.data
}
