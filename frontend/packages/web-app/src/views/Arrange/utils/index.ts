import { useEventBus } from '@vueuse/core'
import { difference, includes } from 'lodash-es'
import { SnowflakeIdv1 } from 'simple-flakeid'

import { atomScrollIntoViewKey } from '@/constants/eventBusKey'
import { useFlowStore } from '@/stores/useFlowStore'
import { useProcessStore } from '@/stores/useProcessStore'
import useProjectDocStore from '@/stores/useProjectDocStore'
import { Catch, CONVERT_MAP, CvImageExist, Else, ElseIf, FileExist, Finally, FolderExist, ForDict, ForEnd, ForList, ForStep, Group, GroupEnd, If, IfEnd, Module, Process, ProcessOld, Try, TryEnd, While, WindowExist } from '@/views/Arrange/config/atomKeyMap'

import { defaultValueText, elementTag } from '../config/flow'

/**
 * 텍스트변환백엔드필요데이터
 * @param {Any} params 텍스트문자열
 * @param {boolean} isView 여부예이미지표시
 * @returns 파싱후의백엔드필요데이터형식
 */
export function varHtmlToStr(params, isView = true) {
  const str = replaceInconformity(params)
  // 중지사용자의문서중있음data-value으로및data-type 이동변수의관리
  const isVariable = str.includes('<ifly')
  const arr = str.split('<ifly')
  const result = []
  arr.forEach((item) => {
    const ends = item.split('</ifly>')
    result.push(...ends.filter(end => end))
  })
  return result.map((item) => {
    let res
    if (item.includes('data-value') && isVariable) {
      const valueType = item.match(/data-type="(\S*)"/)[1]
      const val = item.match(/data-value="(\S*)"/)[1]
      // 요소필요의형식 e["val"] 변수형식 val or g.val
      res = {
        value: valueType === elementTag && !isView ? `e["${val}"]` : `${val}`,
        type: valueType === elementTag ? 'ele' : 'other',
      }
    }
    else {
      const v = item.replace(/<p\b[^>]*>(.*?)<\/p>/gi, '$1')
      res = { value: v, type: 'string' }
    }
    return res
  })
}

/**
 * 텍스트변환백엔드필요데이터, 교체아니오기호합치기이면의입력형식
 * @param {Any} params 텍스트문자열
 */
export function replaceInconformity(params) {
  let str = String(params)
    .replaceAll(defaultValueText, '')
    .replaceAll('<br>', '')
    .replaceAll(/&nbsp;/gi, ' ') // 필터링<p><br></p>
  str = str.replaceAll('<p></p>', '')
  return str // 필터링<p><br></p>
}

/**
 * 파싱문자기호
 */
export function decodeHtml(text) {
  let resultText = text
  if (typeof text === 'string') {
    resultText = text
      .replaceAll('&amp;', '&')
      .replaceAll('&lt;', '<')
      .replaceAll('&gt;', '>')
      .replaceAll('&quot;', '"')
      .replaceAll('&#x27;', '\'')
      .replaceAll('\\r\\n', '\r\n')
  }
  return resultText
}

/**
 * 완료일id
 */
const genId = new SnowflakeIdv1({ workerId: 1 })
export function genNonDuplicateID(head: string = ''): string {
  const headStr = head || 'bh'
  return `${headStr}${genId.NextId()}`
}

/**
 * 자체동작완료이름, 숫자후자체증가
 * @param array 배열
 * @param prefix 전문자기호
 * @param splitStr 분문자기호
 * @returns 숫자후자체증가의이름
 */
export function generateName(array, prefix, splitStr = '_') {
  let selfNum = 1
  const existCheckNum = []
  const defaultCheckNum = []
  let diffCheckNum = []
  array.forEach((v) => {
    if (v) {
      const lastIndex = v.lastIndexOf(prefix + splitStr)
      const checkName = v.substring(0, lastIndex + (prefix + splitStr).length) // 필요조회의문자열
      const checkNum = v.substring(checkName.length, v.length) // 자체증가숫자
      if (prefix + splitStr === checkName) {
        existCheckNum.push(checkNum)
        defaultCheckNum.push(`${selfNum++}`)
      }
    }
  })

  diffCheckNum = difference(defaultCheckNum, existCheckNum)
  diffCheckNum && diffCheckNum.length && (selfNum = diffCheckNum[0])
  const randomName = prefix + splitStr + selfNum // 완료이름
  return randomName
}

const levelPos = {}

export function setLevelPos(key: string, level: number, pos: number) {
  levelPos[pos] = `${level}-${key}`
}

export function generateAtomLevel(curAtomKey: string, curAtomIdx: number, preKey: string, preLevel: number) {
  const arr = Object.keys(levelPos)
  let res = -1
  arr.some((pos, i) => {
    if (Number(pos) > curAtomIdx) {
      res = i - 2
      return true
    }
    return false
  })
  let curLevel = 1
  const parent = res > -1 ? levelPos[arr[res]] : null
  const cur = CONVERT_MAP[curAtomKey]
  if (!cur) {
    return [If, Else, ElseIf, Try, Catch, Finally, ForStep, ForDict, ForList, While, Group, CvImageExist, FileExist, FolderExist, WindowExist].includes(preKey) ? preLevel + 1 : preLevel
  }
  if (parent) {
    const [parentLevel, parentKey] = parent.split('-')
    if ([ElseIf, Else].includes(cur.key)) {
      curLevel = [If, CvImageExist, FileExist, FolderExist, WindowExist].includes(parentKey) ? Number(parentLevel) : Number(parentLevel) + 1
    }
    else if ([Catch, Finally].includes(cur.key)) {
      curLevel = parentKey !== Try ? Number(parentLevel) + 1 : Number(parentLevel)
    }
    else if ([IfEnd, TryEnd, ForEnd, GroupEnd].includes(parentKey)) {
      curLevel = Number(parentLevel)
    }
    else {
      curLevel = Number(parentLevel) + 1
    }
  }
  return curLevel
}

let timer = null
export function setRoll(type: 'left' | 'right' | 'top', id: string, step = 150) {
  // 스크롤동작의데이터값 또는 +- = 값 예위치
  timer && clearTimeout(timer)
  timer = setTimeout(() => {
    const scrollDom = document.getElementById(id)
    switch (type) {
      case 'left':
        scrollDom.scrollLeft += step
        break
      case 'right':
        scrollDom.scrollLeft -= step
        break
      case 'top':
        scrollDom.scrollTop += step
        break
      default:
        scrollDom.scrollTop -= step
        break
    }
    clearTimeout(timer)
    timer = null
  }, 0)
}

// 를원자 기능스크롤동작까지가능내부
export function atomScrollIntoView(atomId: string) {
  const bus = useEventBus(atomScrollIntoViewKey)
  bus.emit(atomId)
}

/**
 * 선택출력현재프로세스가능선택의하위 프로세스프로세스및모듈목록
 * @param item
 */
export function pickProcessAndModuleOptions(item: RPA.AtomDisplayItem) {
  const processStore = useProcessStore()
  const formTypeParams = item?.formType?.params?.filters

  if (includes(formTypeParams, 'Process')) { // 중단여부예선택하위 프로세스
    // 선택출력모든의프로세스
    const flowList = processStore.processList.filter(item => item.resourceCategory === 'process')
    // 필터링자체및주프로세스
    const filterFlow = flowList.filter(item => item.resourceId !== processStore.activeProcessId && !item.isMain)
    // 를프로세스점로 변환테이블단일선택
    return filterFlow.map(item => ({
      label: item.name,
      value: item.resourceId,
    }))
  }

  if (includes(formTypeParams, 'PyModule')) { // 중단여부예선택py모듈
    // 선택출력모든 py 모듈
    const pyModuleList = processStore.processList.filter(item => item.resourceCategory === 'module')
    return pyModuleList.map(item => ({
      label: item.name,
      value: item.resourceId,
    }))
  }

  return item?.options
}

/**
 * 조회하위 프로세스사용
 * @param processId
 */
export function querySubProcessQuote(processId: string) {
  // console.time('useSearchSubProcess')
  const processStore = useProcessStore()
  const processList = processStore.processList.filter(item => item.resourceCategory !== 'module')
  const result = []
  processList.forEach((pItem: any) => {
    const searchProcessItem = useProjectDocStore().userFlowNode(pItem.resourceId).reduce((acc, item, index) => {
      if ([Process, ProcessOld, Module].includes(item.key) && item.inputList.find(i => i.value === processId)) {
        acc.push({
          id: item.id, // 실행하위 프로세스점id
          alias: item.alias, // 실행하위 프로세스점이름
          row: index + 1, // 실행하위 프로세스점에서의행
        })
      }
      return acc
    }, [])

    searchProcessItem.length > 0 && result.push({
      processId: pItem.resourceId, // 프로세스id
      processName: pItem.name, // 프로세스이름
      nodes: searchProcessItem, // 실행하위 프로세스점목록
    })
  })
  // console.timeEnd('useSearchSubProcess')
  return result
}

// 삭제하위 프로세스사용
export function delectSubProcessQuote(processId: string) {
  const processStore = useProcessStore()
  const flowStore = useFlowStore()
  const processList = processStore.processList.filter(item => item.resourceCategory !== 'module')
  processList.forEach((pItem: any) => {
    useProjectDocStore().userFlowNode(pItem.resourceId).forEach((item, index) => {
      const findIdx = item.inputList.findIndex(i => i.value === processId)
      if ([Process, ProcessOld, Module].includes(item.key) && findIdx > -1) {
        item.inputList[findIdx].value = ''
        if (processStore.activeProcessId === pItem.resourceId) {
          const uiData = flowStore.simpleFlowUIData[index]
          uiData.inputList[findIdx].value = ''
          flowStore.setSimpleFlowUIDataByType({ ...uiData }, index, true)
        }
      }
    })
  })
}
