import type { Rule } from 'ant-design-vue/es/form'
/**
 * 휴대폰 번호형식인증
 */
async function phoneValidate(_rule: Rule, value: string) {
  if (!value) {
    return Promise.reject(new Error('휴대폰 번호를 입력하세요.'))
  }
  if (!/^\d{11}$/.test(value)) {
    return Promise.reject(new Error('올바른 휴대폰 번호 형식을 입력하세요.'))
  }
  return Promise.resolve()
}

/**
 * 사용자빈격식검증
 */
async function validateTrim(_rule: Rule, value: string) {
  const trimReg = /\s+/g // 매칭빈격식
  if (trimReg.test(value)) {
    return Promise.reject(new Error('비밀번호에 공백을 사용할 수 없습니다.'))
  }
  else {
    return Promise.resolve()
  }
}

/**
 * 사용자검증
 */
async function validateAccount(_rule: Rule, value: string) {
  console.log('validateAccount: ', value)
  const accountReg = /\s+/g // 매칭빈격식
  if (!value) {
    return Promise.reject(new Error('계정을 입력하세요.'))
  }
  if (accountReg.test(value)) {
    return Promise.reject(new Error('계정에 공백을 사용할 수 없습니다.'))
  }
  else {
    return Promise.resolve()
  }
}

/**
 * 비밀번호검증
 */
async function validatePass(_rule: Rule, value: string) {
  const res = value.replace(/\s*/g, '')
  const passwordReg = /^[a-z0-9\x21-\x2F\x3A-\x40\x5B-\x60\x7B-\x7F]{6,20}$/i
  if (value === '') {
    return Promise.reject(new Error('비밀번호를 입력하세요.'))
  }
  // 수
  if (res.length < 6 || res.length > 20) {
    return Promise.reject(new Error('비밀번호는 6-20자로 입력하세요.'))
  }
  if (!passwordReg.test(res)) {
    return Promise.reject(new Error('영문, 숫자, 특수문자를 사용해 올바른 비밀번호 형식으로 입력하세요.'))
  }
  else {
    return Promise.resolve()
  }
}
/**
 * 새비밀번호검증
 */
async function validatePassNew(_rule: Rule, value: string) {
  const res = value.replace(/\s*/g, '')
  const numReg = /\d/
  const AZReg = /[A-Z]/
  const azReg = /[a-z]/
  const spReg = /[!@#$%^&*()?]/
  if (res.length < 6 || res.length > 20) {
    return Promise.reject(new Error('비밀번호는 6-20자로 입력하세요.'))
  }
  if (!numReg.test(res)) {
    return Promise.reject(new Error('비밀번호에는 숫자가 포함되어야 합니다.'))
  }
  if (!AZReg.test(res)) {
    return Promise.reject(new Error('비밀번호에는 대문자가 포함되어야 합니다.'))
  }
  if (!azReg.test(res)) {
    return Promise.reject(new Error('비밀번호에는 소문자가 포함되어야 합니다.'))
  }
  if (!spReg.test(res)) {
    return Promise.reject(new Error('비밀번호에는 특수문자 !@#$%^&*()? 중 하나가 포함되어야 합니다.'))
  }
  return Promise.resolve()
}
/**
 * 짧음정보인증 코드형식인증
 */
async function codeValidate(_rule: Rule, value: string) {
  if (!/^\d{6}$/.test(value)) {
    return Promise.reject(new Error('6자리 인증 코드를 입력하세요.'))
  }
  return Promise.resolve()
}

/**
 * 메일함형식인증
 */
async function emailValidate(_rule: Rule, value: string) {
  if (!/^\w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/.test(value)) {
    return Promise.reject(new Error('올바른 이메일 주소를 입력하세요.'))
  }
  return Promise.resolve()
}

/**
 * url형식인증
 */
async function urlValidate(_rule: Rule, value: string) {
  const pattern = /^[a-z]+:\/\/[a-z0-9\-.]+\.[a-z]{2,}(\/\S*)?$/i
  if (!pattern.test(value)) {
    return Promise.reject(new Error('올바른 URL을 입력하세요.'))
  }
  return Promise.resolve()
}

/**
 * 파일주소형식인증
 */
async function fileUrlValidate(_rule: Rule, value: string) {
  if (!/^[C-F|]:\\.+\\.+$/.test(value)) {
    return Promise.reject(new Error('올바른 파일 경로를 입력하세요.'))
  }
  return Promise.resolve()
}

/**
 * 메일함검증xxx@xxx.xxx
 */
async function validateEmail(_rule: Rule, value: string) {
  const emailReg = /^[\w-]+@[\w-]+(\.[\w-]+)+$/
  // 163메일함
  if (!emailReg.test(value)) {
    return Promise.reject(new Error('올바른 이메일 주소를 입력하세요.'))
  }
  else {
    return Promise.resolve()
  }
}

export {
  codeValidate,
  emailValidate,
  fileUrlValidate,
  phoneValidate,
  urlValidate,
  validateAccount,
  validateEmail,
  validatePass,
  validatePassNew,
  validateTrim,
}
