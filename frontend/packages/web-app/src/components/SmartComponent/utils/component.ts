import { VAR_IN_TYPE } from '@/constants/atom'
import { generateAdvancedItems, generateExceptionItems, generateOutItems } from '@/views/Arrange/utils/generateData'

import { SMART_COMPONENT_KEY_PREFIX } from '../config/constants'
import type { SmartComp } from '../types'

export function isSmartComponentKey(key: string) {
  return key?.startsWith(SMART_COMPONENT_KEY_PREFIX)
}

export function getSmartComponentId(key: string) {
  return key?.split(`${SMART_COMPONENT_KEY_PREFIX}.`)?.[1] || ''
}

export function generateComponentForm(comp: SmartComp, oldOutputList?: any[]) {
  // 가져오기 출력변수목록, 사용정렬제거현재컴포넌트의출력변수
  const excludeVariables: string[] = []
  // 생성출력의, 사용보관 key 의출력변수이름
  const oldOutputMap = new Map<string, any>()

  if (oldOutputList) {
    oldOutputList.forEach((item) => {
      if (item.value && Array.isArray(item.value)) {
        item.value.forEach((v: any) => {
          if (v.type === VAR_IN_TYPE && v.value) {
            excludeVariables.push(v.value)
          }
        })
      }
      // 생성 key 까지출력의
      if (item.key) {
        oldOutputMap.set(item.key, item)
      }
    })
  }

  // 예결과출력의 key 매칭, 보관기존있음의값;아니오이면완료새의변수이름
  const targetArr: any[] = []
  if (comp.outputList && oldOutputMap.size > 0) {
    comp.outputList.forEach((item) => {
      const oldItem = oldOutputMap.get(item.key)
      if (oldItem) {
        targetArr.push(oldItem)
      }
    })
  }

  return {
    ...comp,
    outputList: generateOutItems(comp.outputList, targetArr, excludeVariables),
    advanced: generateAdvancedItems(comp.outputList),
    exception: generateExceptionItems(comp.outputList),
  }
}
