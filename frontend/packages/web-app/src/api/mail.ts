import http from './http'

// 메일함목록
export function apiGetMailList(params: { pageNo: number, pageSize: number }) {
  return http.get('/api/robot/taskMail/page/list', params)
}

// 메일함
export function apiSaveMail(params: {
  emailService: string
  emailProtocol: string
  emailServiceAddress: string
  port: string
  enableSSL: boolean
  emailAccount: string
  authorizationCode: string
}) {
  return http.post('/api/robot/taskMail/save', params)
}

// 삭제메일함
export function apiDeleteMail(params: { resourceId: string }) {
  return http.post('/api/robot/taskMail/delete', params)
}

// 메일함감지
export function apiCheckEmail(data) {
  return http.post('/api/robot/taskMail/connect', data)
}
