export { isEmpty } from 'lodash-es'

/**
 * 정상이면단일매칭
 * @param {string} input
 * @param {string} target
 */
export function simpleFuzzyMatch(input: string, target: string) {
  // 매칭모든정상이면문자기호, 에서전추가반대변환
  const escapeRegExp = (str: string) => {
    return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  }

  const pattern = input
    .split('')
    .map(escapeRegExp)
    .join('.*') // 매개문자기호허용작업내용

  return new RegExp(pattern, 'i').test(target) // 대소
}

/**
 * 완료 uuid
 * @returns string
 */
export function generateUUID() {
  let d = Date.now()
  const uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (d + Math.random() * 16) % 16 | Math.trunc(d / 16)
    d = Math.floor(d / 16)
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16)
  })
  return uuid
}

/**
 *  base64 변환 file
 */
export function base64ToFile(base64: string, fileName: string) {
  const arr = base64.split(',')
  const mime = arr[0].match(/:(.*?);/)[1]
  const bstr = atob(arr[1])
  let n = bstr.length
  const u8arr = new Uint8Array(n)
  while (n--) {
    u8arr[n] = bstr.charCodeAt(n)
  }
  return new File([u8arr], fileName, { type: mime })
}

/**
 * 이미지주소변환 base64
 * @param imgUrl 이미지주소
 */
export function imgUrltoBase64(imgUrl: string) {
  return new Promise((resolve, reject) => {
    fetch(imgUrl).then(
      (res) => {
        res.blob().then((blob) => {
          const reader = new FileReader()
          reader.readAsDataURL(blob)
          reader.onloadend = () => {
            resolve(reader.result)
          }
        })
      },
      (err) => {
        reject(err)
      },
    )
  })
}

/**
 * 제거base64이미지, base64이미지이면반환기존문자열
 * @param base64
 */
export function trimBase64Header(base64: string) {
  const base64Header = 'data:image/png;base64,'
  if (!base64.includes(base64Header))
    return base64
  return base64.replace(base64Header, '')
}

/**
 * 추가base64이미지
 */
export function addBase64Header(base64: string) {
  const base64Header = 'data:image/png;base64,'
  return base64Header + base64
}

/**
 * 여부예 base64 이미지
 */
export function isBase64Image(base64: string) {
  const base64Header = 'data:image/png;base64,'
  return base64 && base64.includes(base64Header)
}

/**
 * base64 문자열변환정상일반문자열
 */
export function base64ToString(base64: string) {
  return atob(base64)
}

/**
 * 확장업데이트안내, 버전a.b.c중 a,b 변수필요강함제어업데이트확장,  c 변수이면필요안내업데이트
 * @param {string} version1
 * @param {string} version2
 * @returns {enum} 1: 필요업데이트, 2: 강함제어업데이트(시아니오사용), 0/-1: 아니오필요업데이트
 */
export function compareVersion(version1, version2) {
  // version1>version2 이면필요업데이트,
  let res = 0
  const v1 = version1.split('.')
  const v2 = version2.split('.')
  const len = Math.max(v1.length, v2.length)
  while (v1.length < len) {
    v1.push('0')
  }
  while (v2.length < len) {
    v2.push('0')
  }
  for (let i = 0; i < len; i++) {
    const num1 = Number.parseInt(v1[i])
    const num2 = Number.parseInt(v2[i])
    // if (i < 2 && num1 > num2) {
    //   // 버전a.b.c중 a,b 변수필요강함제어업데이트확장
    //   res = 2;
    //   break;
    // } else
    if (num1 > num2) {
      res = 1
      break
    }
    else if (num1 < num2) {
      res = -1
      break
    }
  }
  return res
}

// 가져오기파일이름, 아니오후
export function getFileName(fileName: string) {
  return fileName.substring(0, fileName.lastIndexOf('.'))
}
/**
 * blob변환text
 */
export function blob2Text<T>(blob: Blob) {
  return new Promise<T>((resolve, reject) => {
    const reader = new FileReader()
    reader.onloadend = () => {
      resolve(reader.result as T)
    }
    reader.onerror = reject
    reader.readAsText(blob)
  })
}

/**
 * blob변환file
 * @param blob blob객체
 * @param fileName 파일이름
 * @returns 변환된 File 객체
 */
export function blob2File(blob: Blob, fileName: string) {
  return new File([blob], fileName, { type: blob.type })
}

export function text2LogArray(text: string) {
  try {
    // 행완료배열
    let arr = text.split('\n')
    // 빈행
    for (let i = 0; i < arr.length; i++) {
      // 매일행의후의빈격식행제거
      arr[i] = arr[i].replace(/\s+$/g, '')
    }
    arr = arr.filter(item => item)

    return arr.map((item) => {
      const itemObj = JSON.parse(item)
      return {
        event_time: itemObj.event_time,
        ...itemObj.data,
      }
    })
  }
  catch (error) {
    console.error('Error text2LogArray:', error)
    return []
  }
}
// 가져오기파일후
export function getFileExtension(filename: string) {
  const lastDotIndex = filename.lastIndexOf('.')
  if (lastDotIndex === -1) {
    return '' // 있음후
  }
  return filename.substring(lastDotIndex)
}

/**
 * 재시도데이터
 * @param operation
 * @param maxRetries 재시도데이터
 * @param interval 재시도시간
 */
export function retry<T>(operation: () => Promise<T>, maxRetries: number, interval: number): Promise<T> {
  return new Promise((resolve, reject) => {
    let retries = 0

    function attempt() {
      operation()
        .then(resolve)
        .catch((error) => {
          retries++
          if (retries >= maxRetries) {
            reject(error)
          }
          else {
            setTimeout(attempt, interval)
          }
        })
    }

    attempt()
  })
}

/**
 * 중문자열로..., 문자열길이정도로N
 * @returns string
 */
export function replaceMiddle(str: string, length: number = 16) {
  if (str.length <= length) {
    return str
  }
  const newStr = `${str.slice(0, length / 2)}...${str.slice(-length / 2)}`
  return newStr
}

/**
 * 완료열이름 A-Z, AA-ZZ, AAA-ZZZ
 * @param num 열데이터 1+ 숫자대대기1
 * @returns A-Z, AA-ZZ, AAA-ZZZ ...
 */
export function generateColumnNames(num: number) {
  let result = ''
  while (num > 0) {
    const remainder = (num - 1) % 26
    const char = String.fromCharCode(65 + remainder)
    result = char + result
    num = Math.floor((num - 1) / 26)
  }
  return result
}
/**
 * 완료데이터테이블이름
 * @param allNames 데이터테이블이름목록
 * @returns 데이터테이블num
 */
export function generateSheetName(allNames: string[], locale = 'zh-CN') {
  const name = locale === 'zh-CN' ? '데이터 테이블 ' : 'DataSheet'
  let index = 1
  let tempName = name + index
  while (allNames.includes(tempName)) {
    index++
    tempName = name + index
  }
  return tempName
}
/**
 * 가져오기url중의매개변수
 * @param field string
 * @param url string
 * @returns string
 */
export function getUrlQueryField(field: string, url?: string) {
  url = url || window.location.href
  const reg = new RegExp(`[?&]${field}=([^&#]*)`, 'i')
  const match = url.match(reg)
  return match ? decodeURIComponent(match[1]) : ''
}

/**
 *  URL 중개조회매개변수의값
 * @param field 조회매개변수의이름
 * @param val 조회매개변수의새값
 * @param url 기존 URL, 로현재창의 URL
 * @returns 업데이트후의 URL
 */
export function setUrlQueryField(field: string, val: string, url?: string): string {
  url = url || window.location.href
  const encodedVal = encodeURIComponent(val)
  // 정상이면테이블방식: 매칭지정의조회매개변수
  const reg = new RegExp(`([?&])${field}=.*?(?=&|$)`, 'i')
  // 조회 URL 중여부완료저장에서해당조회매개변수
  if (url.match(reg)) {
    // 있음의조회매개변수값
    return url.replace(reg, `$1${field}=${encodedVal}`)
  }
  else {
    // 예결과찾을 수 없습니다, 추가새의조회매개변수
    const separator = url.includes('?') ? '&' : '?'
    return `${url}${separator}${field}=${encodedVal}`
  }
}

export function replaceUrlDomain(url: string, newDomain: string) {
  let newUrl = ''
  try {
    const path = new URL(decodeURIComponent(url)).pathname
    newUrl = newDomain + path
  }
  catch (error) {
    console.error('Error URL:', error)
    newUrl = url
  }
  return encodeURIComponent(newUrl)
}

export function getUrlPath(url: string) {
  let origin = ''
  try {
    const uri = new URL(decodeURIComponent(url))
    origin = uri.origin + uri.pathname
  }
  catch (error) {
    console.error('Error URL:', error)
    origin = ''
  }
  return origin
}

export function getUrlDomain(url: string) {
  let origin = ''
  try {
    origin = new URL(decodeURIComponent(url)).origin
  }
  catch (error) {
    console.error('Error URL:', error)
    origin = ''
  }
  return encodeURIComponent(origin)
}

/**
 * 완료일개있음 resolve 및 reject 방법법의 Promise 객체
 * Promise.withResolvers polyfill
 */
export function promiseWithResolvers<T>() {
  let resolve: (value: T | PromiseLike<T>) => void
  let reject: (reason?: unknown) => void
  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve: resolve!, reject: reject! }
}

/**
 * 를결과의기존가능목록평면
 * @param treeData 결과의기존가능목록
 * @param widthParent 여부패키지
 * @returns 평면후의기존가능목록
 */
export function flatAtomicTree(treeData: RPA.AtomTreeNode[], widthParent = true) {
  const flattenedArray: Array<RPA.AtomTreeNode & { parentKey: RPA.AtomTreeNode['key'] }> = []

  function traverse(nodes: RPA.AtomTreeNode[], parentKey: RPA.AtomTreeNode['key'] = '') {
    if (!nodes || !Array.isArray(nodes)) {
      return
    }

    for (const node of nodes) {
      if (widthParent || !node.atomics) {
        flattenedArray.push({ ...node, parentKey }) // 를현재추가까지결과배열
      }

      if (node.atomics && Array.isArray(node.atomics)) {
        traverse(node.atomics, node.key) // 관리
      }
    }
  }

  traverse(treeData)

  return flattenedArray
}

/**
 * 가져오기 결과중의모든, 반환 key 그룹성공의배열
 * @param treeData 기존의배열
 * @returns  key 그룹성공의배열
 */
export function getParentNodes(treeData: RPA.AtomTreeNode[]): string[] {
  const parentNodes: string[] = []

  function traverse(nodes: RPA.AtomTreeNode[]) {
    for (const node of nodes) {
      if (node.atomics && node.atomics.length > 0) {
        parentNodes.push(node.uniqueId)
        traverse(node.atomics)
      }
    }
  }

  traverse(treeData)

  return parentNodes
}

/**
 * 개배열, 출력있음의
 * @param arrOld 기존배열
 * @param arrNew 새배열
 * @returns { deleteIds: string[], addIds: string[] } 삭제의및추가의
 */
export function diffArrays<T>(arrOld: T[], arrNew: T[]): { deleteIds: T[], addIds: T[] } {
  const deleteIds = arrOld.filter(item => !arrNew.includes(item))
  const addIds = arrNew.filter(item => !arrOld.includes(item))
  return { deleteIds, addIds }
}

/**
 * 계획문자열의추가권한길이정도(영어1, 중국어2)
 * @param {string} str
 * @returns {number} 길이정도
 */
export function getWeightedLength(str: string = ''): number {
  let len = 0
  str = str || ''
  for (let i = 0; i < str.length; i++) {
    // 조회문자기호여부로중국어(Unicode매개)
    const charCode = str.charCodeAt(i)
    if (charCode >= 0x4E00 && charCode <= 0x9FA5) {
      len += 2 // 중국어문자기호
    }
    else {
      len += 1 // 영어문자기호또는
    }
  }
  return len
}

/**
 * 근거getWeightedLength 계획출력의문자열반환32위치으로내부의텍스트
 */
export function getWeightText(len: number = 32, text: string): string {
  let newText = ''
  let currentLength = 0
  for (let i = 0; i < text.length; i++) {
    const charCode = text.charCodeAt(i)
    const charLength = (charCode >= 0x4E00 && charCode <= 0x9FA5) ? 2 : 1 // 중국어문자기호길이정도로2, 문자기호길이정도로1
    if (currentLength + charLength > len) {
      break
    }
    newText += text[i]
    currentLength += charLength
  }
  return newText
}

/**
 * 계획antdesign table컴포넌트scrollY
 */
// 시로일반량, 후가능으로사용에서 antv tokens 중가져오기
const TABLE_HEADER_HEIGHT = 47 // 테이블높음정도
const TABLE_CELL_HEIGHT = 49 // 테이블단일높음정도
export function getTableScrollY(tableMaxSize: number, rowLength: number) {
  const contentHeight = rowLength * TABLE_CELL_HEIGHT + TABLE_HEADER_HEIGHT
  //  table 내용기기여부가득
  const isFull = tableMaxSize < contentHeight
  return isFull ? tableMaxSize - TABLE_HEADER_HEIGHT : undefined
}

/**
 * 데이터
 * @param ms 시간
 * @returns Promise
 */
export function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/**
 * 가져오기cookie값
 * @param name cookie이름
 * @returns cookie값
 */
export function getCookie(name: string) {
  const arr = document.cookie.match(new RegExp(`(^| )${name}=([^;]*)(;|$)`))
  return arr != null ? unescape(arr[2]) : ''
}
