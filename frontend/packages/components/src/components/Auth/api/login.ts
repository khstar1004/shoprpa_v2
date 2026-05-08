import { message } from 'ant-design-vue'
import i18next from 'i18next'

import type { ConsultFormData, LoginFormData, RegisterFormData, TenantItem } from '../interface'

import { http } from './http'

interface PreAuthenticateData extends LoginFormData {
  platform: string
  scene: string
}

interface SetPasswordData extends LoginFormData {
  tempToken: string
}

// 조회로그인상태
export async function loginStatus() {
  const { data } = await http.get('/rpa-auth/login-status')
  return data
}

// 가져오기 token
export async function getToken() {
  const { data } = await http.get('/rpa-auth/token')
  return data
}

// 출력로그인
export async function logout() {
  const { data } = await http.post('/rpa-auth/logout')
  return data
}

// 조회여부예사용자
export async function isHistory(params: LoginFormData) {
  const { data } = await http.get('/rpa-auth/user/history', params)
  return data
}

//
export async function preAuthenticate(params: PreAuthenticateData) {
  const { data } = await http.post('/rpa-auth/pre-authenticate', params)
  return data
}

// 전송검증인증코드
export async function sendCaptcha(phone: string, scene: string, isRegister: boolean = true) {
  if (!isRegister) {
    const registered = await checkRegistered({ phone })
    if (!registered) {
      message.warning(i18next.t('components.auth.phoneNotRegistered'))
      return Promise.reject(new Error(i18next.t('components.auth.phoneNotRegistered')))
    }
  }
  const { data } = await http.postparams('/rpa-auth/verification-code/send', { phone, scene })
  return data
}

// 가져오기테넌트목록
export async function tenantList(tempToken?: string) {
  const { data } = await http.get<TenantItem[]>('/rpa-auth/tenant/list', { tempToken })
  return data.map((i) => {
    return {
      ...i,
      tenantType: i.tenantType?.includes('enterprise_') ? 'enterprise' : i.tenantType,
    }
  })
}

// 정상방식로그인
export async function login(params: { tempToken: string, tenantId: string }) {
  const { data } = await http.postparams('/rpa-auth/login', params)
  return data
}

// 회원가입
export async function register(params: RegisterFormData) {
  const { data } = await http.post('/rpa-auth/register', params)
  return data
}

// 조회여부완료회원가입
export async function checkRegistered({ phone }: { phone: string }) {
  const { data } = await http.get('/rpa-auth/user/exist', { phone })
  return data
}

// 비밀번호
export async function setPassword(params: SetPasswordData) {
  const { data } = await http.post('/rpa-auth/password/set', params)
  return data
}

// 테넌트
export async function switchTenant(params: { tenantId: string }) {
  const { data } = await http.postparams('/rpa-auth/tenant/switch', params)
  return data
}

// 가져오기사용자 정보
export async function userInfo() {
  const { data } = await http.get('/rpa-auth/user/info')
  return data
}

// 수정비밀번호
export async function modifyPassword(params: LoginFormData) {
  const { data } = await http.post('/rpa-auth/password/change', params)
  return data
}

// 제출문의
export async function submitConsult(params: ConsultFormData) {
  const { data } = await http.post('/robot/feedback/consult/submit', params)
  return data
}

// 제출
export async function submitRenewal(params: ConsultFormData) {
  const { data } = await http.post('/robot/feedback/renewal/submit', params)
  return data
}
