import { isEmpty } from 'lodash-es'

import i18next from '@/plugins/i18next'

import { Group, GroupEnd } from '@/views/Arrange/config/atomKeyMap'
import { DEFAULT_DESC_TEXT, SPECIALKEY } from '@/views/Arrange/config/flow'
import { backContainNodeIdx, getIdx } from '@/views/Arrange/utils/flowUtils'

import { replaceStrFunc } from './atomDescUtils'
import { caculateConditional } from './selfExecuting'

// 원자 기능의비고
export function renderAtomRemark(item: RPA.Atom) {
  const { key, isOpen, id, inputList, outputList, advancedItems } = item
  if (!id)
    return
  if ([Group, GroupEnd].includes(key)) {
    let desc = ''
    if (key === Group && !isOpen) {
      const startIdx = getIdx(id)
      const endIdx = backContainNodeIdx(id)
      desc = `공유${endIdx - startIdx - 1}건`
    }
    return desc
  }
  const desc = i18next.translate(item.comment)
  const title = i18next.translate(item.alias)

  if (!desc)
    return title // 매칭완료삭제

  const replaceStrings = desc.match(/(?<=\{)(.+?)(?=\})/g)
  if (!replaceStrings)
    return desc

  const replaceArr = replaceStrings.map((str) => {
    const arr = str.split(':')
    return {
      key: arr[0],
      keyArr: arr[0].split('||'),
      placeholderText: arr[1] || '',
      input: false,
    }
  })

  const userFormItems = inputList.concat(outputList || []).concat(advancedItems || [])
  const userDataObj = {}
  userFormItems.forEach((i) => {
    userDataObj[i.key] = i
  })
  const formItemValues = {}
  const formItemsObj = {}
  userFormItems.forEach((i) => {
    const findItem = i
    if (!findItem)
      return
    const val = findItem.value === '""' ? '' : findItem.value
    // 근거테이블단일유형표시아니오의값 매칭시필요비고이, 근거아니오의수정이
    if (i.options && findItem.value) {
      const withOption = i.options.find(opt => opt.value === findItem.value)
      formItemValues[i.key] = withOption ? i18next.translate(withOption.label) : val
    }
    else {
      // formItemValues[i.key] = val ? varHtmlToStr(val) : val;
      // val가능예배열, 있음가능예문자열, 숫자, undefined대기
      if (Array.isArray(val)) {
        formItemValues[i.key] = (val || []).map(i => i.value).join('')
      }
      else {
        formItemValues[i.key] = val
      }
    }
    formItemsObj[i.key] = i
  })
  // 를설명정보중의변수교체로테이블단일값
  let str = desc
  replaceArr.forEach(({ key, keyArr, placeholderText }) => {
    const specialKey = `${SPECIALKEY + key + SPECIALKEY}`
    const replaceStr = `@{${key}${placeholderText ? `:${placeholderText}` : ''}}`
    const placeholder = placeholderText === 'null' ? '' : placeholderText || DEFAULT_DESC_TEXT
    if (keyArr.length > 1) {
      // 설명이의표시필드로다중개필드중의일개, 근거conditional대상의필드행표시
      const showKeys = keyArr.filter((k) => {
        if (!k)
          return false
        const dynamics = formItemsObj[k].dynamics
        if (isEmpty(dynamics))
          return true
        return caculateConditional(dynamics, userDataObj, formItemsObj[k])
      })
      if (showKeys.length > 0) {
        const showKey = showKeys[0]
        str = replaceStrFunc(str, replaceStr, showKey, formItemValues[showKey], placeholder, inputList)
      }
      else {
        str = str.replaceAll(!placeholder ? `(${replaceStr})` : replaceStr, `***#####${specialKey}${placeholder}***`)
      }
    }
    else {
      str = replaceStrFunc(str, replaceStr, key, formItemValues[key], placeholder, inputList)
    }
  })

  return str.split('***').map((i) => {
    if (i.includes('#####')) {
      const v = i.split('#####')
      let result = null
      if (v.length === 2) {
        const s = v[1]
        if (s.includes(SPECIALKEY)) {
          const sr = s.split(SPECIALKEY)
          const currentItem = formItemsObj[sr[1].split('||')[0]]
          result = currentItem && sr.length === 3 ? { variable: true, sr, currentItem: { ...currentItem, ...userDataObj[currentItem.key] } } : ''
          // result = sr.length === 3 ? { variable: true, sr } : ''
        }
      }
      return result || ''
    }
    return i
  })
}
