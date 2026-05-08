import http from '../http'

export function mobileLogin(params) {
  return http.post('uac/sys-login/login-by-code', null, { params })
}

export function rpaLoginPassWord(data: { phone: string, password: string }) {
  return http.post(
    '/uac/client/login',
    data,
    { loading: false },
  )
}

export function rpaGetSMSCode(data: { phone: string }) {
  return http.post('/uac/user/sms-code', data)
}

export function rpaRegister(data: { phone: string, password: string, code: string, confirmPassword: string }) {
  return http.post('/uac/user/register', data)
}

/**
 * @description: 로그아웃
 */
export function rpaLogout(data: any) {
  return http.post('/uac/client/logout', data)
}

/**
 * @description: 가져오기테넌트빈목록
 */
export function getTenanList() {
  return http.get('/right/tenant/userinfo/getbyuser')
}

/**
 * 사용자 정보
 */
export function rpaUserInfo() {
  return http.post('/uac/user/user-info', {}, { toast: false })
}

/**
 * 돌아가기사용자선택의테넌트 정보
 */
export function sendTenantId(data: { tenantId: string | number }) {
  return http.post('/right/tenant/select/client', data)
}

/**
 * 가져오기uuid
 */
export function getUUID(data: { phone: string }) {
  return http.get('/uac/sys-login/get/uuid', data)
}

/**
 * 전송짧음정보인증 코드
 */
export function sendSMSCode(data: { phone: string }) {
  return http.post('/uac/sms/login-send', data)
}
