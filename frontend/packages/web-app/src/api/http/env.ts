import { message } from 'ant-design-vue'

import { isBase64Image } from '@/utils/common'
import { storage } from '@/utils/storage'

import GlobalModal from '@/components/GlobalModal/index.ts'

const DEFAULT_PORT = 13159
const DEFAULT_HOST = import.meta.env.VITE_SERVICE_HOST ?? 'localhost'

/**
 * 가져오기연결URL
 * @returns baseURL
 */
export function getBaseURL(): string {
  const port = Number(storage.get('route_port')) || DEFAULT_PORT
  return `http://${DEFAULT_HOST}:${port}`
}

export function getAPIBaseURL(): string {
  return `${getBaseURL()}/api`
}

export function getImageURL(str: string): string {
  return isBase64Image(str) ? str : `${getBaseURL()}${str}`
}

/**
 * 로그인실패
 */
export function unauthorize(response) {
  if (response.config.toast === false) {
    message.error(response.data.message || response.data.msg || '로그인이 만료되었습니다. 다시 로그인해 주세요.')
  }
  const code = response.data.code || response.data.ret
  location.href = `/boot.html?code=${code}`
}

let isUnauthorized = null
export function unauthorizeModal(code?: string | number) {
  if (isUnauthorized)
    return

  let message = '로그인이 만료되었습니다. 다시 로그인해 주세요.'
  if (code === '900001') {
    message = '다른 환경에서 로그인되어 현재 세션이 만료되었습니다. 다시 로그인해 주세요.'
  }
  else if (code === '900005') {
    message = '이용 기간이 만료되었습니다. 다시 로그인해 주세요.'
  }

  isUnauthorized = GlobalModal.error({
    title: '로그인 필요',
    content: message,
    keyboard: false,
    maskClosable: false,
    onOk: () => {
      isUnauthorized = null
    },
  })
}

let isExpired = null
export function expiredModal(tenantType?: string | number) {
  if (isExpired)
    return

  isExpired = GlobalModal.error({
    title: '이용 기간 만료',
    content: `${tenantType === 'enterprise' ? '엔터프라이즈' : '개인'} 이용 기간이 만료되었습니다. 관리자에게 문의하세요.`,
    keyboard: false,
    maskClosable: false,
    okTxt: '확인',
    onOk: () => {
      isExpired = null
    },
  })
}
