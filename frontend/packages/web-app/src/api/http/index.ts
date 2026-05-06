import { message } from 'ant-design-vue'
import type {
  AxiosInstance,
  AxiosRequestConfig,
  AxiosRequestHeaders,
  AxiosResponse,
} from 'axios'
import axios, { AxiosHeaders } from 'axios'
import i18next from 'i18next'
import { isNil } from 'lodash-es'

import { promiseWithResolvers } from '@/utils/common'

import { ERROR_CODES, SUCCESS_CODES, UN_AUTHORIZED_CODES } from '@/constants'

import { getBaseURL, unauthorize } from './env'

export type { AxiosProgressEvent } from 'axios'

export interface RequestConfig<T = any, P = any> extends AxiosRequestConfig<P> {
  toast?: boolean
  loading?: boolean
  mock?: (params: P) => Promise<ResponseData<T>> | ResponseData<T>
}

interface InternalRequestConfig<T = any, P = any> extends RequestConfig<T, P> {
  headers: AxiosRequestHeaders
}

interface ResponseData<T = any> {
  code?: string
  data: T
  message?: string
  msg?: string
  imageType?: string
  success?: boolean
  ret?: number // 추가 ret 속성, UAP
  redirectUrl?: string // 추가 redirectUrl 속성, UAP
}

interface Response<T = any, D = any> extends AxiosResponse<ResponseData<T>, D> {
  config: InternalRequestConfig<T, D>
}

function normalizeErrorPayload<T = any>(payload?: ResponseData<T> | any) {
  if (!payload || typeof payload !== 'object')
    return payload

  const rawMsg = payload.message || payload.msg || payload.data
  if (String(rawMsg || '').toLowerCase() === 'unauthorized') {
    payload.message = i18next.t('noPermission')
    payload.msg = payload.message
    if (typeof payload.data === 'string')
      payload.data = payload.message
  }

  return payload
}

function normalizeTransportError(error: any) {
  const rawMessage = String(error?.message || '')
  const rawCode = String(error?.code || '')

  if (
    rawMessage.toLowerCase() === 'network error'
    || rawCode === 'ERR_NETWORK'
    || rawMessage.includes('ERR_CONNECTION_REFUSED')
  ) {
    return i18next.t('requestFailedRetry', { defaultValue: '요청에 실패했습니다. 잠시 후 다시 시도하세요.' })
  }

  return rawMessage || i18next.t('requestFailedRetry', { defaultValue: '요청에 실패했습니다. 잠시 후 다시 시도하세요.' })
}

class HttpClient {
  private HTTP_READY_KEY = 'httpReady'
  private instance: AxiosInstance

  // 모든의 http 요청 , 있음에서 readyPromise  resolve 시전송요청 
  private readyPromise = promiseWithResolvers<void>()

  // 허용외부부서호출의방법법, 사용에서로드완료후, 를 readyPromise 로완료완료
  public resolveReadyPromise() {
    // 를완료완료의식별자저장까지 session storage 중
    sessionStorage.setItem(this.HTTP_READY_KEY, 'true')
    this.readyPromise.resolve()
  }

  constructor() {
    // 조회 session storage 중여부완료저장에서 httpReady 식별자
    const isReady = sessionStorage.getItem(this.HTTP_READY_KEY) === 'true'
    if (isReady) {
      // 예결과완료저장에서 httpReady 식별자, 이면를 readyPromise 로완료완료
      this.readyPromise.resolve()
    }

    this.init()
  }

  public init() {
    this.instance = axios.create({
      baseURL: getBaseURL(),
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-Requested-With': 'XMLHttpRequest',
      },
      timeout: 20000,
      withCredentials: true,
      responseType: 'json',
    })

    this.instance.interceptors.request.use((config: InternalRequestConfig) => {
      if (config.mock) {
        //  adapter, 직선연결반환지정
        config.adapter = async () => {
          return Promise.resolve({
            data: await Promise.resolve(config.mock(config)),
            status: 200,
            statusText: 'OK',
            headers: new AxiosHeaders({ 'content-type': 'application/json' }),
            config, // 패키지기존의요청 매칭
          })
        }
      }

      // 에서가능으로추가요청 기기의, 예추가요청 , 관리요청 매개변수대기
      if (config.loading) {
        // TODO: 추가전체영역 loading
      }

      config.headers['Accept-Language'] = i18next.language

      return config
    })

    this.instance.interceptors.response.use(
      (response: Response) => {
        // 에서가능으로추가기기의, 예관리데이터, 관리오류대기
        if (response.config.loading) {
          // 닫기loading
        }

        if (response.config.responseType === 'blob') {
          response.data = { code: '0000', data: response.data }

          // TODO: 직선연결반환여부필요
          return response
        }

        // toast 열기시작, 가능으로통신경과 config.toast 제어여부
        normalizeErrorPayload(response.data)

        if (!SUCCESS_CODES.includes(response.data.code) && response.config.toast !== false) {
          const rawMsg = response.data.message || response.data.msg
          message.error(rawMsg || i18next.t('requestFailedRetry', { defaultValue: '요청에 실패했습니다. 잠시 후 다시 시도하세요.' }))
        }

        if (UN_AUTHORIZED_CODES.includes(response.data.code) || response.data.ret === 302) {
          unauthorize(response)
        }

        if (response.headers.token) {
          sessionStorage.setItem('tokenValue', response.headers.token)
        }

        if (response.headers['content-type'].search('image') > -1) {
          response.data = {
            data: response.data,
            imageType: response.headers['content-type'],
          }
        }

        if (ERROR_CODES.includes(response.data.code)) {
          return Promise.reject(response)
        }

        return response
      },
      (error) => {
        // 에서가능으로관리요청 오류, 예오류안내, 변환까지오류페이지대기
        const fallbackMsg = error.response?.status === 403
          ? i18next.t('noPermission')
          : i18next.t('requestFailedRetry', { defaultValue: '요청에 실패했습니다. 잠시 후 다시 시도하세요.' })
        if (String(error.message || '').toLowerCase() === 'unauthorized' || String(error.response?.data || '').toLowerCase() === 'unauthorized')
          error.message = i18next.t('noPermission')
        else if (error.response)
          error.message = fallbackMsg
        else
          error.message = normalizeTransportError(error)

        if (error.config.toast !== false) {
          if (error.response) {
            message.error(error.message)
          }
          else {
            message.error(error.message)
          }
        }
        return Promise.reject(error)
      },
    )
  }

  private request<T = any, P = any>(config: RequestConfig<T, P>) {
    // eslint-disable-next-line no-async-promise-executor
    return new Promise<ResponseData<T>>(async (resolve, reject) => {
      try {
        // 에서전송요청 전, 필요대기로드완료
        await this.readyPromise.promise

        const res = await this.instance.request<ResponseData<T>>(config)

        const isSuccess = SUCCESS_CODES.includes(res?.data.code)
        isSuccess ? resolve(res?.data) : reject(normalizeErrorPayload(res?.data))
      }
      catch (error) {
        return reject(error)
      }
    })
  }

  public get<T = any, P = any>(url: string, params?: P, config?: RequestConfig<T, P>) {
    return this.request<T, P>({ method: 'get', url, params, ...config })
  }

  public delete<T = any, P = any>(url: string, params?: P, config?: RequestConfig<T, P>) {
    return this.request<T, P>({ method: 'delete', url, params, ...config })
  }

  public put<T = any, P = any>(url: string, data?: P, config?: RequestConfig<T, P>) {
    return this.request<T, P>({ method: 'put', url, data, ...config })
  }

  public post<T = any, P = Record<string, any>>(
    url: string,
    data?: P,
    config?: RequestConfig<T, P>,
  ) {
    const dataConfig = !isNil(data)
      ? { data: Object.fromEntries(Object.entries(data).map(([key, value]) => [key, value ?? ''])) as P }
      : {}

    return this.request<T, P>({
      method: 'post',
      url,
      ...dataConfig,
      ...config,
    })
  }

  public getBlob(url: string, params?: any, config?: RequestConfig) {
    return this.request({
      method: 'get',
      url,
      params,
      responseType: 'blob',
      ...config,
    })
  }

  public getStream(url: string, data?: any, config?: RequestConfig) {
    return this.request({
      method: 'get',
      url,
      data,
      responseType: 'blob',
      ...config,
    })
  }

  public postStream(url: string, data?: any, config?: RequestConfig) {
    return this.request({
      method: 'post',
      url,
      data,
      responseType: 'blob',
      ...config,
    })
  }

  public postForm<T = any, P = any>(
    url: string,
    data: P,
    config?: RequestConfig<T, URLSearchParams>,
  ) {
    const formData = new URLSearchParams()
    Object.keys(data).forEach((key) => {
      formData.append(key, (data as any)[key] ?? '')
    })

    return this.request<T, URLSearchParams>({
      method: 'post',
      url,
      data: formData,
      ...config,
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        ...config?.headers,
      },
    })
  }

  public postFormData<T = any, P = Record<string, any>>(
    url: string,
    data: P,
    config?: RequestConfig<T, P>,
  ) {
    const formDataConfig: RequestConfig<T, P> = {
      method: 'post',
      url,
      data,
      ...config,
      headers: {
        ...config?.headers,
        'Content-Type': 'multipart/form-data',
      },
    }

    return this.request<T, P>(formDataConfig)
  }

  public postBlob(url: string, data?: any, config?: RequestConfig) {
    return this.request({
      method: 'post',
      url,
      data,
      responseType: 'blob',
      ...config,
    })
  }
}

export default new HttpClient()
