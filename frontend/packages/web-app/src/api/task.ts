import type { ITableResponse } from '@/types/normalTable'
import type { Task } from '@/types/schedule'

import http from './http'
import { hasLocalRuntimeRoute } from './runtime'

/**
 * @description: 가져오기예약 작업목록 데이터
 */
export async function getScheduleLst(data) {
  const res = await http.post<ITableResponse>('/api/robot/triggerTask/page/list', data)
  return res.data || { records: [], total: 0 }
}

/**
 * @description: cron테이블방식검증
 */
export function checkCronExpression(data) {
  return http.post('/api/robot/task/corn/check', data, { toast: false })
}

/**
 * @description: 가져오기예약 작업실행기록목록 데이터
 */
export async function getTaskExecuteLst(data) {
  const res = await http.post<ITableResponse>('/api/robot/task-execute/list', data)
  return res.data || { records: [], total: 0 }
}

/**
 * @description: 계획시의예약 작업가져오기
 */
export function taskCancel(data) {
  return http.post('/scheduler/crontab/cancel', data)
}

// 트리거
export function manualTrigger(data: { task_id: string }) {
  return http.post('/trigger/task/run', data)
}

// taskNotify 알림트리거기기업데이트
export async function taskNotify(params = { event: 'normal' }) {
  if (!await hasLocalRuntimeRoute())
    return { code: '000000', data: null, message: '' }

  return http.post('/trigger/task/notify', params, { toast: false })
}

// 가져오기예약 작업미완료실행시간
export function taskFutureTime(data: { task_id: string, times: number }) {
  return http.post('/trigger/task/future', data)
}

// /task/future_with_no_create
export function taskFutureTimeNoCreate(data: { frequency_flag: string, times: number }) {
  return http.post('/trigger/task/future_with_no_create', data, { toast: false })
}

// 이름 변경검증
export async function isNameCopy(data: { name: string }) {
  const res = await http.get('/api/robot/triggerTask/isNameCopy', data)
  return res.data
}

// 사용목록
export function getRobotList(data: { name: string }) {
  return http.get('/api/robot/triggerTask/robotExe/list', data)
}

// 추가예약 작업
export async function insertTask(data: Task) {
  const res = await http.post('/api/robot/triggerTask/insert', data)
  taskNotify()
  return res
}

// 가져오기단일개예약 작업연결
export function getTaskInfo(data: { taskId: string }) {
  return http.get('/api/robot/triggerTask/get', data)
}

// 삭제단일개예약 작업연결
export async function deleteTask(data: { taskId: string }) {
  const res = await http.get('/api/robot/triggerTask/delete', data)
  taskNotify()
  return res
}
// 업데이트예약 작업연결
export async function updateTask(data: Task) {
  const res = await http.post('/api/robot/triggerTask/update', data)
  taskNotify()
  return res
}
// 사용/사용 안 함 예약 작업
export async function enableTask(data: { taskId: string, enable: number }) {
  const res = await http.get('/api/robot/triggerTask/enable', data)
  taskNotify()
  return res
}

// 가져오기예약 작업큐상태
export function getTaskQueueList(data) {
  return http.get('/trigger/task/queue/status', data)
}

// 제거예약 작업큐
export function removeTaskQueue(data: { unique_id: string[] }) {
  return http.post('/trigger/task/queue/remove', data)
}

// 업데이트예약 작업큐매칭
export function updateTaskQueueConfig(data: { max_length: number, max_wait_minutes: number, deduplicate: boolean }) {
  return http.post('/trigger/task/queue/config', data)
}

// 가져오기예약 작업큐매칭
export function getTaskQueueConfig() {
  return http.get('/trigger/task/queue/config', {})
}
