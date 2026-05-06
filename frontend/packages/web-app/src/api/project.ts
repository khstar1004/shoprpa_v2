import type { ITableResponse } from '@/types/normalTable'

import http from './http'

// 새생성
export function createProject(data) {
  return http.post('/api/robot/robot-design/create', data, { toast: true })
}

// 새생성-수검증
export function checkProjectNum() {
  return http.get('/api/robot/quota/check-designer', { toast: true })
}

/**
 * 감지사용여부예약 작업사용반환사용개사용의예약 작업의배열
 */
export async function isInTask(params) {
  const res = await http.get('/api/robot/robot-design/delete-robot-res', params)
  return res.data
}

// 삭제
export function delectProject(data) {
  return http.post('/api/robot/robot-design/delete-robot', data)
}

/**
 * @description: 가져오기 계획기기목록목록 데이터
 */
export async function getDesignList(data) {
  try {
    const res = await http.post('/api/robot/robot-design/design-list', data, { toast: false })
    return res.data
  }
  catch {
    return { records: [], total: 0 }
  }
}

/**
 * 공유마켓
 */
export function shareRobotToMarket(data) {
  return http.post('/api/robot/market-resource/share', data)
}

/**
 * 가져오기버전목록
 */
export async function getVersionLst<T>(params) {
  const res = await http.get<T>('/api/robot/robot-version/list4Design', params)
  return res.data
}

/**
 * 복사버전
 */
export function versionRecover(data) {
  return http.post('/api/robot/robot-version/recover-version', data)
}

/**
 * 사용버전
 */
export function versionEnable(data) {
  return http.post('/api/robot/robot-version/enable-version', data)
}

/**
 * 이름 변경
 */
export function rename(params) {
  return http.get('/api/robot/robot-design/rename', params)
}

/**
 * 이름 변경검증
 */
export function renameCheck(params) {
  return http.get('/api/robot/robot-design/design-name-dup', params)
}

/**
 * 생성본
 */
export function createCopy(params) {
  return http.get('/api/robot/robot-design/copy-design-robot', params)
}

/**
 * 생성본
 */
export async function getDefaultName() {
  const res = await http.post<string>('/api/robot/robot-design/create-name')
  return res.data
}

/**
 * 가져오기컴포넌트목록
 */
export async function getComponentList(data: {
  name: string
  dataSource: 'create' | 'market'
  pageNum?: number
  pageSize?: number
  sortType?: string
}) {
  const res = await http.post<ITableResponse>('/api/robot/component/page-list', data)
  return res.data || { records: [], total: 0 }
}

// 새생성컴포넌트
export function createComponent(params: { componentName: string }) {
  return http.get('/api/robot/component/create', params, { toast: true })
}

/**
 * 새생성컴포넌트-가져오기 컴포넌트이름
 */
export async function getDefaultComponentName() {
  const res = await http.post<string>('/api/robot/component/create-name', null, { toast: false })
  return res.data
}

/**
 * 조회컴포넌트이름여부재복사
 */
export async function checkComponentName(params: { name: string, componentId?: string }) {
  const res = await http.post<boolean>('/api/robot/component/check-name', params, { toast: false })
  return res.data
}

/**
 * 이름 변경컴포넌트
 */
export function renameComponent(params: { newName: string, componentId: string }) {
  return http.get('/api/robot/component/rename', params)
}

/**
 * 삭제컴포넌트
 */
export async function deleteComponent(params: { componentId: string }) {
  const res = await http.get<boolean>('/api/robot/component/delete', params)
  return res.data
}

/**
 * 생성본컴포넌트이름
 */
export async function createCopyComponentName(params: { componentId: string }) {
  const res = await http.get<string>('/api/robot/component/copy/create-name', params)
  return res.data
}

/**
 * 생성본
 */
export async function createCopyComponent(params: { componentId: string, name: string }) {
  const res = await http.get<boolean>('/api/robot/component/copy', params)
  return res.data
}

/**
 * 가져오기컴포넌트
 */
export async function getComponentDetail(params: { componentId: string }) {
  const res = await http.get<RPA.ComponentDetail>('/api/robot/component/info', params)
  return res.data
}

/**
 * 가져오기컴포넌트아래일개버전
 */
export async function getComponentNextVersion(params: { componentId: string }) {
  const res = await http.get<number>('/api/robot/component-version/next-version', params)
  return res.data
}

/**
 * 컴포넌트발송버전
 */
export async function publishComponent<T>(params: T) {
  const res = await http.post<boolean>('/api/robot/component-version/create', params)
  return res.data
}
