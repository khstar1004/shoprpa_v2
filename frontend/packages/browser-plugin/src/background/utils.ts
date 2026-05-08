import { StatusCode } from './constant'

const SPECIAL_CHARACTER_PATTERN = /[^\p{L}\p{N}_-]/u

export const Utils = {
  getNavigatorUserAgent() {
    const isChrome = /Chrome/.test(navigator.userAgent)
    const isFirefox = /Firefox/.test(navigator.userAgent)
    const isEdge = /Edg/.test(navigator.userAgent)

    if (isFirefox)
      return '$firefox$'
    if (isEdge)
      return '$edge$'
    if (isChrome)
      return '$chrome$'
    return '$unknown$'
  },

  async wait(seconds: number) {
    return new Promise((resolve) => {
      setTimeout(resolve, seconds * 1000)
    })
  },

  removeUrlParams(url: string) {
    return url.replace(/\?.*$/, '')
  },

  isEndWithSlash(url: string) {
    if (url.endsWith) {
      return url.endsWith('/')
    }
    else {
      return url.substr(-1) === '/'
    }
  },

  stringToRegex(inputString: string) {
    let body = inputString
    let flags = ''
    const lastSlashIndex = inputString.lastIndexOf('/')

    if (inputString.startsWith('/') && lastSlashIndex > 0) {
      body = inputString.slice(1, lastSlashIndex)
      if (body.startsWith('^') && body.endsWith('$')) {
        body = body.slice(1, -1)
      }
      if (
        body.includes('\\d')
        || body.includes('\\w')
        || body.includes('\\s')
        || body.includes('\\b')
        || body.includes('\\.')
        || body.includes('\\*')
        || body.includes('\\?')
        || body.includes('\\+')
        || body.includes('\\{')
        || body.includes('\\}')
        || body.includes('\\[')
        || body.includes('\\]')
      ) {
        body = body.replace(/\\/g, '\\')
      }
      flags = inputString.slice(lastSlashIndex + 1)
    }
    try {
      const regex = new RegExp(body, flags)
      return regex
    }
    catch {
      throw new Error('정규식 패턴이 올바르지 않습니다.')
    }
  },

  isSupportProtocol(url: string) {
    if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('ftp://') || url.startsWith('file://')) {
      return true
    }
    else {
      return false
    }
  },

  isSupportProtocal(url: string) {
    return this.isSupportProtocol(url)
  },

  isNumberStartString(str: string) {
    return /^\d/.test(str)
  },

  isNumberString(str: string) {
    return /\d/.test(str)
  },

  isSpecialCharacter(str: string) {
    if (this.isNumberStartString(str))
      return true
    return SPECIAL_CHARACTER_PATTERN.test(str)
  },

  success(data, msg = 'success') {
    return {
      code: StatusCode.SUCCESS,
      data,
      msg,
    }
  },

  fail(msg = 'failed', code = StatusCode.UNKNOWN_ERROR) {
    return {
      data: null,
      code,
      msg,
    }
  },

  result(data, msg = 'success', code = StatusCode.SUCCESS) {
    return {
      code,
      data,
      msg,
    }
  },
}
