import type { ITableResponse } from '@/types/normalTable'

import http from './http'

/**
 * @description: 가져오기 사용실행기록목록 데이터
 */
export async function getExecuteLst(data) {
  const res = await http.post<ITableResponse>('/api/robot/robot-record/list', data)
  return res.data || { records: [], total: 0 }
}

export function delExecute(data: { recordIds: string[] }) {
  return http.post('/api/robot/robot-record/delete-robot-execute-records', data)
}

export function delTaskExecute(data: { taskExecuteIdList: string[] }) {
  return http.post('/api/robot/task-execute/batch-delete', data)
}

/**
 * @description: 가져오기 지정버전사용실행기록목록 데이터
 */
export function getlogs(data) {
  return http.post('/api/robot/robot-record/log', data)
}
