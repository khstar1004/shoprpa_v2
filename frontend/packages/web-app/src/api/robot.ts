import { pickBy } from 'lodash-es'

import type { ITableResponse } from '@/types/normalTable'

import http from './http'

/**
 * @description: 가져오기실행기기사용목록 데이터
 */
export async function getRobotLst(data) {
  const res = await http.post<ITableResponse>('/api/robot/robot-execute/execute-list', data)
  return res.data || { records: [], total: 0 }
}

/**
 * 감지사용여부예약 작업사용반환사용개사용의예약 작업의배열
 */
export async function isRobotInTask(params) {
  const res = await http.get('/api/robot/robot-execute/delete-robot-res', params)
  return res.data
}

/**
 * @description: 삭제사용
 */
export function deleteRobot(data) {
  return http.post('/api/robot/robot-execute/delete-robot', data)
}

/**
 * @description: 게시사용
 */
export function publishRobot<T = any>(data: T) {
  return http.post('/api/robot/robot-version/publish', data)
}

/**
 * @description: 가져오기 사용위발송버전정보돌아가기
 */
export async function getRobotLastVersion(robotId: string) {
  const res = await http.post('/api/robot/robot-version/latest-info', { robotId })
  return pickBy(res.data, value => value !== null)
}

/**
 * @description: 가져오기해당사용여부허용외부부서호출
 */

export async function getRobotLastIsExternalCall(robotId: string) {
  const res = await http.get(`/api/rpa-openapi/workflows/get/${robotId}`)
  return pickBy(res?.data?.workflow, value => value !== null)
}

/**
 * @description: 저장여부허용외부부서호출의매칭
 */
export function setRobotIsExternalCall(data) {
  return http.post('/api/rpa-openapi/workflows/upsert', data)
}

/**
 * 가져오기AI워크플로목록
 */
export async function getWorkflowList() {
  const res = await http.get('/api/rpa-openapi/workflows/get-astron')
  return res.data?.records || []
}

/**
 * @description: 가져오기 사용이름으로영어
 */
export function getRobotEnglishName(name: string) {
  return http.post('/api/rpa-ai-service/v1/chat/prompt', { prompt_type: 'translate', params: { name } })
}

/**
 * @description: 문의실행기기아래사용업데이트상태
 */
export async function getRobotUpdateStatus(data) {
  const res = await http.post('/api/robot/robot-execute/execute-update-check', data)
  return res.data
}

/**
 * @description: 업데이트실행기기아래사용
 */
export function updateRobot(data) {
  return http.post('/api/robot/robot-execute/update/pull', data)
}

/**
 * @description: 사용재이름검증
 */
export function checkRobotName(data: { robotId: string, name: string }) {
  return http.post('/api/robot/robot-version/same-name', data)
}

/**
 * 생성의사용
 */
export async function getMyRobotDetail(robotId: string) {
  const res = await http.get('/api/robot/robot-design/my-robot-detail', { robotId })
  return res.data
}

/**
 * 가져오기의사용
 */
export async function getMarketRobotDetail(robotId: string) {
  const res = await http.get('/api/robot/robot-design/market-robot-detail', { robotId })
  const { myRobotDetailVo, sourceName, versionInfoList } = res.data

  return { ...myRobotDetailVo, sourceName, versionList: versionInfoList }
}

/**
 * @description: 가져오기 사용
 */
export async function getRobotRecordOverview(data: { robotId: string, version: number, deadline: string }) {
  const res = await http.post('/api/robot/robot-record/detail/overview', data)
  return res.data
}

/**
 * @description: 조회사용의모든프로세스데이터
 */
export async function getRobotProcessList(robotId: string): Promise<any[]> {
  const res = await http.post('/api/robot/process/all-data', { robotId })
  return res.data.map(it => ({
    ...it,
    processContent: JSON.parse(it.processContent),
  }))
}

/**
 * 저장사용지정구성 매개변수
 */
export async function saveRobotConfigParamValue(data: RPA.CreateConfigParamData[], mode: string, robotId: string) {
  return http.post('/api/robot/param/saveUserParam', { paramList: data, mode, robotId })
}

/**
 * 실행기기사용본정보
 */
export async function getRobotBasicInfo(robotId: string) {
  const res = await http.get('/api/robot/robot-execute/robot-detail', { robotId })
  return res.data
}

/**
 * 가져오기 내부컴포넌트관리관리목록
 */
export async function getComponentManageList(robotId: string) {
  const res = await http.post<RPA.ComponentManageItem[]>('/api/robot/component/editing/manage-list', { robotId, version: 0 })
  return res.data
}

/**
 * 설치컴포넌트
 */
export async function installComponent(data: { robotId: string, componentId: string }) {
  const res = await http.post<string>('/api/robot/component-robot-block/delete', { ...data, mode: 'EDIT_PAGE' })
  return res.data
}

/**
 * 제거컴포넌트
 */
export async function removeComponent(data: { robotId: string, componentId: string }) {
  const res = await http.post<string>('/api/robot/component-robot-block/add', { ...data, mode: 'EDIT_PAGE' })
  return res.data
}

/**
 * 추가컴포넌트사용
 */
export async function addComponentUse(data: { componentId: string, robotId: string, robotVersion?: number }) {
  const res = await http.post<boolean>('/api/robot/component-robot-use/add', { ...data, mode: 'EDIT_PAGE' })
  return res.data
}

/**
 * 삭제컴포넌트사용
 */
export async function deleteComponentUse(data: { componentId: string, robotId: string, robotVersion?: number }) {
  const res = await http.post<boolean>('/api/robot/component-robot-use/delete', { ...data, mode: 'EDIT_PAGE' })
  return res.data
}

/**
 * 업데이트컴포넌트사용
 */
export async function updateComponent(data: { robotId: string, componentId: string, componentVersion: number }) {
  const res = await http.post<string>('/api/robot/component-robot-use/update', { ...data, mode: 'EDIT_PAGE' })
  return res.data
}

/**
 * 가져오기컴포넌트
 */
export async function getComponentDetail(data: { robotId: string, componentId: string }) {
  const res = await http.post<RPA.ComponentManageItem>('/api/robot/component/editing/info', { ...data, mode: 'EDIT_PAGE' })
  return res.data
}

/**
 * 조회입력의컴포넌트
 */
export async function getEditComponentDetail(data: { robotId: string, componentId: string }) {
  const res = await http.post('/api/robot/component-robot-use/edit', { ...data, mode: 'EDIT_PAGE' })
  return res.data
}
