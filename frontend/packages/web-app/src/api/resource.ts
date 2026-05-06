import type { FlowItem } from '@/views/Arrange/types/flow'

import type { RequestConfig } from './http'
import http from './http'
import { getBaseURL } from './http/env'
import { sseRequest } from './sse'

// 프로세스실행
export function flowRun(data) {
  return http.post('/api/project/flow/run', data)
}

export interface StartExecutorParams {
  project_id: string | number
  exec_position?: string
  process_id?: string | number
  jwt?: string
  debug?: string
  recording_config?: string
  hide_log_window?: boolean
  project_name?: string
  open_virtual_desk?: string
  line?: string | number
  end_line?: string | number
  is_custom_component?: boolean
}
export async function startExecutor(data: StartExecutorParams) {
  const res = await http.post<{ addr: string }>('/scheduler/executor/run', data, { timeout: 0 })
  return res.data.addr
}

export function stopExecutor(data: { project_id: string | number }) {
  return http.post('/scheduler/executor/stop', data)
}

// 프로세스저장
export function flowSave(data: { robotId: string, processId: string, processJson: string }) {
  return http.post('/api/robot/process/save', data, { timeout: 10 * 1000 })
}

// 가져오기현재프로세스데이터
export function getProcess(data: { robotId: string, processId: string }) {
  return http.post('/api/robot/process/process-json', data)
}

export async function getProcessAndCodeList(data: { robotId: string }): Promise<RPA.Flow.ProcessModule[]> {
  const res = await http.post<RPA.Flow.ProcessModule[]>('/api/robot/module/processModuleList', data)
  return res.data
}

/**
 * 가져오기 python 모듈코드내용
 */
export async function getProcessPyCode(data: { robotId: string, mode?: string, moduleId: string }): Promise<string> {
  // mode 로빈시, 값로 EDIT_PAGE
  // EDIT_PAGE - 
  // PROJECT_LIST - 계획기기목록
  // EXECUTOR - 실행기기사용목록
  // CRONTAB - 트리거기기
  if (!data.mode) {
    data.mode = 'EDIT_PAGE'
  }

  const res = await http.post<{ moduleContent: string }>('/api/robot/module/open', data)
  return res.data.moduleContent
}

/**
 * 삭제 python 모듈코드내용
 */
export async function deleteProcessPyCode(moduleId: string) {
  const res = await http.get<boolean>('/api/robot/module/delete', { moduleId })
  return res.data
}

/**
 * 저장 python 모듈코드내용
 */
export async function saveProcessPyCode(data: { robotId: string, moduleId: string, moduleContent: string }): Promise<boolean> {
  const res = await http.post<boolean>('/api/robot/module/save', data)
  return res.data
}

/**
 * 가져오기추가 Python 모듈의이름
 */
export async function genProcessPyCodeName(params: { robotId: string }) {
  const res = await http.get<string>('/api/robot/module/newModuleName', params)
  return res.data
}

/**
 * 추가 Python 모듈
 */
export async function addProcessPyCode(data: { robotId: string, moduleName: string }): Promise<string> {
  const res = await http.post<{ moduleId: string }>('/api/robot/module/create', data)
  return res.data.moduleId
}

/**
 * 복사 Python 모듈
 */
export async function copyProcessPyCode(data: { robotId: string, moduleId: string }): Promise<unknown> {
  const res = await http.post<{ moduleId: string }>('/api/robot/process/copy', null, { params: { robotId: data.robotId, processId: data.moduleId, type: 'module' } })
  return res.data
}

/**
 * 이름 변경 Python 모듈
 */
export async function renameProcessPyCode(data: { robotId: string, moduleId: string, moduleName: string }) {
  const res = await http.post('/api/robot/module/rename', data)
  return res.data
}

/**
 * 가져오기 코드모듈목록
 */
export async function getProcessPyCodeList(data: { robotId: string }) {
  const res = await http.post<{ moduleId: string, name: string }[]>('/api/robot/module/moduleList', data)
  return res.data
}

// 가져오기추가하위 프로세스의이름
export async function genProcessName(data: { robotId: string }) {
  const res = await http.post<string>('/api/robot/process/name', data)
  return res.data
}

// 추가하위 프로세스
export async function addProcess(data: { robotId: string, processName: string }): Promise<string> {
  const res = await http.post<{ processId: string }>('/api/robot/process/create', data)
  return res.data.processId
}

// 하위 프로세스이름 변경
export function renameProcess(data: { robotId: string, processId: string, processName: string }) {
  return http.post('/api/robot/process/rename', data)
}

// 삭제하위 프로세스
export async function delProcess(data: FlowItem) {
  const res = await http.post<boolean>('/api/robot/process/delete', data)
  return res.data
}

// 복사하위 프로세스
export async function copyProcess(data: { robotId: string, processId: string }): Promise<unknown> {
  const res = await http.post<{ processId: string }>('/api/robot/process/copy', null, { params: { ...data, type: 'process' } })
  return res.data
}

type ElementType = 'common' | 'cv'
//  새생성요소/이미지분그룹
export function addElementGroup(params: { robotId: string, elementType?: ElementType, groupName: string }) {
  return http.post('/api/robot/group/create', null, { params })
}

//  이름 변경요소/이미지분그룹
export function renameElementGroup(data: { robotId: string, groupId: string, elementType?: ElementType, groupName: string }) {
  return http.post('/api/robot/group/rename', data)
}

//  삭제요소/이미지분그룹
export function delElementGroup(params: { robotId: string, groupId: string }) {
  return http.post('/api/robot/group/delete', null, { params })
}

//  가져오기모든요소/이미지
export function getElementsAll(params: { robotId: string, elementType?: ElementType }) {
  return http.post('/api/robot/element/all', null, { params })
}

// 조회요소/이미지정보
export function getElementDetail(params: { robotId: string, elementId: string }) {
  return http.post('/api/robot/element/detail', null, { params })
}

// 저장요소정보----
export function postSaveElement(data: any) {
  return http.post('/api/robot/element/save', data)
}

// 생성요소/이미지정보
export function addElement(data: any) {
  return http.post<{ elementId: string, groupId: string }>('/api/robot/element/create', data)
}

// 업데이트요소/이미지정보
export function updateElement(data: any) {
  return http.post('/api/robot/element/update', data)
}

// 요소/이미지까지분그룹
export function moveElement(params: { robotId: string, groupId: string, elementId: string }) {
  return http.post('/api/robot/element/move', null, { params })
}

// 삭제요소/이미지
export function postDeleteElement(params: { robotId: string, elementId: string }) {
  return http.post('/api/robot/element/delete', null, { params })
}

// 완료cv선택이미지이름
export function generateCvElementName(params: { robotId: string }) {
  return http.post('/api/robot/element/image/create-name', null, { params })
}

// 생성요소본
export function createElementCopy(params: { robotId: string, elementId: string }) {
  return http.post('/api/robot/element/copy', null, { params })
}

// 추가전역 변수
export function addGlobalVariable(data: RPA.GlobalVariable) {
  return http.post('/api/robot/global/create', data)
}

// 저장전역 변수
export function saveGlobalVariable(data: RPA.GlobalVariable) {
  return http.post('/api/robot/global/save', data)
}

// 조회전역 변수
export function getGlobalVariable(params: { robotId: string }) {
  return http.post<RPA.GlobalVariable[]>('/api/robot/global/all', null, { params })
}

// 조회전역 변수이름목록
export function getGlobalVariableNameList(params: { robotId: string }) {
  return http.post('/api/robot/global/name-list', null, { params })
}

// 삭제전역 변수
export function deleteGlobalVariable(data: { robotId: string, globalId: string }) {
  return http.post('/api/robot/global/delete', data)
}

// 업로드파일
export async function uploadFile(data: { file: File }, config: RequestConfig = {}) {
  const res = await http.postFormData<string>('/api/resource/file/upload', data, { timeout: 5000000, ...config })
  return res.data
}

// 게시업로드파일
export async function uploadVideoFile(data: { file: File }, config: RequestConfig = {}) {
  const res = await http.postFormData<string>('/api/resource/file/upload-video', data, { timeout: 5000000, ...config })
  return res.data
}

// 조회패키지버전
export function packageVersion(params: { robotId: string, packageName: string }) {
  return http.post('/scheduler/package/version', {
    project_id: params.robotId,
    package: params.packageName,
  }, { timeout: 0 })
}

// 추가패키지
export function addPyPackageApi(data: { robotId: string, packageName: string, packageVersion: string, mirror: string }) {
  return http.post('/api/robot/require/add', data)
}
// 삭제패키지
export function deletePyPackageApi(data: { robotId: string, idList: Array<string> }) {
  return http.post('/api/robot/require/delete', data)
}
// 업데이트패키지
export function updatePyPackageApi(data: { robotId: string, packageName: string, packageVersion: string, mirror: string }) {
  return http.post('/api/robot/require/update', data)
}
// 가져오기 패키지목록
export function getPyPackageListApi(data: { robotId: string }) {
  return http.post('/api/robot/require/list', data)
}
/**
 * 가져오기파일, 방식
 */
export function fileRead(data: { path: string }) {
  return http.postBlob('/scheduler/file/read', data, { toast: false, timeout: 5000000 })
}
/**
 * 파일
 * @params { path: string, mode: 'w' | 'a', content: string } w 덮어쓰기 a 추가 입력
 */
export function fileWrite(data: { path: string, mode: string, content: string }) {
  return http.post('/scheduler/file/write', data, { timeout: 5000000 })
}
/**
 * 가져오기HTML형식의붙여넣기내용
 * @params { is_html: boolean }
 */
export async function getHTMLClip(data: { is_html: boolean }) {
  const res = await http.post('/scheduler/clipboard', data)
  return res.data
}

/**
 * 가져오기데이터테이블내용
 * @param projectId
 */
export async function getDataTable(projectId: string) {
  const res = await http.post<RPA.IDataTableSheets>(
    '/scheduler/datatable/open',
    {
      project_id: projectId,
      filename: 'data_table', // 목록전단일개있음일개데이터테이블파일, 원인파일이름
    },
    { toast: false },
  )
  return res.data
}

/**
 * 업데이트데이터테이블셀
 * @param projectId
 * @param data
 * @returns
 */
export async function updateDataTable(projectId: string, data: RPA.IUpdateDataTableCell[]) {
  const res = await http.post(
    '/scheduler/datatable/update-cells',
    {
      project_id: projectId,
      filename: 'data_table',
      updates: data,
    },
    { toast: false },
  )
  return res.data
}

/**
 * 닫기데이터테이블파일
 * @param projectId
 */
export async function closeDataTable(projectId: string) {
  const res = await http.post(
    '/scheduler/datatable/close',
    {
      project_id: projectId,
      filename: 'data_table',
    },
    { toast: false },
  )
  return res.data
}

/**
 * 삭제데이터테이블
 * @param projectId
 * @returns
 */
export async function deleteDataTable(projectId: string) {
  const res = await http.post(
    '/scheduler/datatable/delete',
    {
      project_id: projectId,
      filename: 'data_table',
    },
    { toast: false },
  )
  return res.data
}

/**
 * 데이터테이블
 * @param projectId
 * @param callback
 * @returns
 */
export function startDataTableListener<T>(projectId: string, callback?: (data: { event: string, data: T }) => void) {
  return sseRequest.get(
    `${getBaseURL()}/scheduler/datatable/stream?project_id=${projectId}&filename=data_table`,
    (res) => {
      try {
        const dataJson: T = JSON.parse(res.data)
        callback?.({ event: res.event, data: dataJson })
      }
      catch (error) {
        console.error('파싱 SSE 데이터실패:', error, res.data)
      }
    },
  )
}