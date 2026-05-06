import http from './http'

// AI완료내용
export function aiFeedback<T>(data: T) {
  return http.post('/api/robot/feedback/submit', data)
}