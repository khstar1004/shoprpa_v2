import type { ITableResponse } from '@/types/normalTable'

import http from './http'

// 근거id및version가져오기기존가능의정보
export async function getAbilityInfo(atomList: { key: string, version: string }[]): Promise<string[]> {
  const res = await http.post<any[]>('/api/robot/atom-new/list', { keys: atomList.map(i => i.key) })
  const data = res.data || []
  return data.map((atom: any) => atom.atomContent)
}

// 가져오기기존가능왼쪽메뉴데이터
export async function getAtomsMeta(): Promise<RPA.AtomMetaData> {
  const res = await http.post('/api/robot/atom-new/tree')
  return JSON.parse(res.data)
}

// 가져오기 컴포넌트왼쪽메뉴데이터
export async function getModuleMeta(): Promise<RPA.AtomTreeNode[]> {
  const res = await http.post('/api/robot/atom-new/tree')
  const data = JSON.parse(res.data)
  return data.atomicTreeExtend ?? []
}

export function getTreeByParentKey(parentKey: string) {
  return http.post('/api/robot/atom/getListByParentKey', null, { params: { parentKey } })
}

export async function getNewAtomDesc(key: string): Promise<{ data: string }> {
  const res = await http.post<any[]>('/api/robot/atom-new/list', { keys: [key] })
  const atom = res.data && res.data.length > 0 ? res.data[0] : {}
  const { atomContent = '{}' } = atom as any
  return { data: atomContent }
}

/**
 * 추가즐겨찾기
 */
export function addFavorite(data: { atomKey: string }) {
  return http.get('/api/robot/atomLike/create', data)
}
/**
 * 가져오기 즐겨찾기
 */
export function removeFavorite(data: { likeId: string }) {
  return http.get('/api/robot/atomLike/cancel', data)
}
/**
 * 가져오기즐겨찾기목록
 */
export async function getFavoriteList() {
  const res = await http.get<RPA.AtomTreeNode[]>('/api/robot/atomLike/list')
  return res.data ?? []
}

/**
 * 가져오기컴포넌트목록
 */
export async function getComponentList(data: {
  robotId: string
  version?: number
}) {
  const res = await http.post<RPA.ComponentManageItem[]>('/api/robot/component/editing/list', { ...data, mode: 'EDIT_PAGE' })
  return res.data ?? []
}

/**
 * 가져오기기존가능의구성 매개변수
 */
export async function getConfigParams(params: {
  robotId: string
  robotVersion?: string | number
  processId?: string
  moduleId?: string
  mode?: string
}) {
  const res = await http.post<RPA.ConfigParamData[]>('/api/robot/param/all', params)
  return res.data
}

/**
 * 추가기존가능의구성 매개변수
 */
export async function createConfigParam(data: RPA.CreateConfigParamData) {
  const res = await http.post<string>('/api/robot/param/add', data)
  return res.data
}

/**
 * 삭제기존가능의구성 매개변수
 * @param id 매개변수id
 */
export function deleteConfigParam(id: string) {
  return http.post(`/api/robot/param/delete?id=${id}`)
}

/**
 * 업데이트기존가능의구성 매개변수
 * @param data RPA.ConfigParamData
 */
export function updateConfigParam(data: RPA.ConfigParamData) {
  return http.post('/api/robot/param/update', data)
}

/**
 * 가져오기 공유 변수
 */
export async function getRemoteParams<T>() {
  const res = await http.get<T[]>('/api/robot/robot-shared-var/get-shared-var')
  return res.data || []
}

/**
 * 가져오기 중파일관리관리공유파일목록
 */
export async function getRemoteFiles(data?: { pageSize?: number, fileName?: string }) {
  const res = await http.post<ITableResponse<RPA.SharedFileType>>('/api/robot/robot-shared-file/page', data)
  return res.data || { records: [], total: 0 }
}