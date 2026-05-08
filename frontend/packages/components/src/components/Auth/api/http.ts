import { message } from 'ant-design-vue'
import axios from 'axios'
import type { AxiosRequestConfig } from 'axios'
import i18next from 'i18next'

let BASE_URL = localStorage.getItem('authBaseUrl') || import.meta.env.VITE_API_BASE_URL || '/api'
export function setBaseUrl(url?: string) {
  BASE_URL = url || import.meta.env.VITE_API_BASE_URL || '/api'
  localStorage.setItem('authBaseUrl', BASE_URL || '')
}

const SUCCESS_CODE = '000000'
export interface ResponseData<T = any> {
  code?: string
  data: T
  message?: string
  msg?: string
}

function normalizeMessage(value?: string) {
  return String(value || '').toLowerCase() === 'unauthorized'
    ? i18next.t('noPermission', { defaultValue: '권한이 없습니다. 관리자에게 문의하세요.' })
    : value
}

function normalizeTransportError(err: any) {
  const rawMessage = String(err?.message || '')
  const rawCode = String(err?.code || '')
  const lowerMessage = rawMessage.toLowerCase()

  if (err?.response?.status === 403) {
    return i18next.t('noPermission', { defaultValue: '권한이 없습니다. 관리자에게 문의하세요.' })
  }

  if (
    rawCode === 'ERR_NETWORK'
    || rawCode === 'ECONNABORTED'
    || lowerMessage === 'network error'
    || lowerMessage.includes('timeout')
    || lowerMessage.includes('err_connection_refused')
    || lowerMessage.includes('connection refused')
  ) {
    return '서버에 연결할 수 없습니다. 백엔드 주소와 서비스 실행 상태를 확인한 뒤 다시 시도하세요.'
  }

  return err?.response
    ? i18next.t('components.auth.serviceError')
    : rawMessage || i18next.t('components.auth.serviceError')
}

export async function request<T = any, P = any>(
  config: AxiosRequestConfig<P> & { url: string },
): Promise<ResponseData<T>> {
  try {
    const { data: res } = await axios<ResponseData<T>>({
      baseURL: BASE_URL,
      timeout: 20000,
      withCredentials: true,
      headers: { 'Content-Type': 'application/json;charset=UTF-8' },
      ...config,
      data: config.data && JSON.parse(JSON.stringify(config.data)),
    })

    if (res.code === SUCCESS_CODE)
      return res

    if (res.code !== SUCCESS_CODE) {
      res.message = normalizeMessage(res.message || res.msg)
      res.msg = res.message
      message.error(res.message || i18next.t('components.auth.serviceError'))
    }
    return Promise.reject(res)
  }
  catch (err: any) {
    const msg = normalizeTransportError(err)
    err.message = msg
    err.userMessage = msg
    message.error(msg)
    return Promise.reject(err)
  }
}

export const http = {
  get: <T = any>(url: string, params?: any, config?: AxiosRequestConfig) =>
    request<T>({ method: 'GET', url, params, ...config }),

  post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) =>
    request<T>({ method: 'POST', url, data, ...config }),

  postparams: <T = any>(url: string, params?: any, config?: AxiosRequestConfig) =>
    request<T>({ method: 'POST', url, params, ...config }),

  put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) =>
    request<T>({ method: 'PUT', url, data, ...config }),

  del: <T = any>(url: string, params?: any, config?: AxiosRequestConfig) =>
    request<T>({ method: 'DELETE', url, params, ...config }),
}

export default request
