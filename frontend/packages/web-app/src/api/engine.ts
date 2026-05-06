import http from './http'

// 요소 선택 서비스 시작
export async function startPickServices(data) {
  try {
    return await http.post('/scheduler/picker/start', data, { toast: false })
  }
  catch {
    return { data: false }
  }
}

// 서비스 중지
export function stopPickServices(data) {
  return http.post('/scheduler/picker/stop', data)
}

// 로스케줄링방식
export function startSchedulingMode(data) {
  return http.post('/scheduler/terminal/start', data)
}

// 출력스케줄링방식
export function endSchedulingMode() {
  return http.post('/scheduler/terminal/end', {})
}

// 스케줄링방식-중중지현재작업
export function stopSchedulingTask() {
  return http.post('/scheduler/executor/stop_list', {})
}

// 클라이언트 상태 조회 busy/free
export async function getTermianlStatus() {
  try {
    return await http.post('/scheduler/executor/status', {}, { toast: false })
  }
  catch {
    return { data: { running: false } }
  }
}

/**
 * @description: 가져오기 인증목록
 * @returns 인증목록
 */
export async function getCredentialList() {
  const res = await http.get<{ name: string }[]>('/scheduler/credential/list')
  return res.data || []
}

/**
 * @description: 생성인증
 * @param data 인증정보
 * @returns
 */
export async function createCredential(data: { name: string, password: string }) {
  return http.post('/scheduler/credential/create', data)
}

/**
 * @description: 삭제인증
 * @param data 인증정보
 * @returns
 */
export async function deleteCredential(name: string) {
  return http.post('/scheduler/credential/delete', { name })
}

/**
 * @description: 조회인증여부저장에서
 * @param name 인증이름
 * @returns 여부저장에서
 */
export async function checkCredentialExists(name: string) {
  const res = await http.get<{ exists: boolean }>('/scheduler/credential/exists', { name })
  return !!res?.data?.exists
}
