import { NiceModal } from '@rpa/components'
import { ref } from 'vue'

import { useCvPickStore } from '@/stores/useCvPickStore'
import { useCvStore } from '@/stores/useCvStore'
import type { Element, PickStepType } from '@/types/resource.d'

import { CvPickModal } from '../modals'

export function useCvPick() {
  const pickerType = ref('cv') // 선택유형-cv
  const cvPickStore = useCvPickStore()

  function openCvPickModal({ entry, groupId }) {
    NiceModal.show(CvPickModal, { entry, groupId })
  }

  /**
   * @description cv선택
   * @param congfig { showCvModal?: boolean, entry?: string, isRePick?: boolean, cvItem?: Element } showCvModal:여부표시표시선택결과팝업, isContinue: 여부계속선택, isRePick: 여부다시 선택, cvItem: 다시 선택의cvItem
   * @returns {Promise<Element | boolean>}
   */
  interface pickConfig { showCvModal?: boolean, groupId?: string, entry?: string, pickStep?: PickStepType, cvItem?: Element }
  const defaultConfig: pickConfig = { showCvModal: true, groupId: '', entry: undefined, pickStep: 'new', cvItem: null }
  const pick = (congfig: pickConfig = defaultConfig) => {
    const conf = { ...defaultConfig, ...congfig }
    const { showCvModal, pickStep, entry, groupId, cvItem } = conf
    return new Promise((resolve) => {
      const type = pickerType.value || ''
      cvPickStore.startCvPick(type, cvItem?.elementData || '', pickStep, (res) => {
        if (res.success) {
          const pickData = res.data
          // 다시 선택 업데이트데이터, 이미지이름할 수 없음수정
          useCvStore().setTempCvItem(pickData, pickStep)
          // 선택결과팝업
          if (showCvModal) {
            openCvPickModal({ entry, groupId })
          }
          resolve(pickData)
        }
        else {
          resolve(false)
        }
      })
    })
  }

  // 선택점
  const pickAnchor = (cvItem: Element) => {
    return pick({ showCvModal: false, pickStep: 'anchor', cvItem })
  }

  // cv다시 선택
  const rePick = (cvItem: Element, showCvModal = false) => {
    return pick({ showCvModal, pickStep: 'repick', cvItem, entry: 'edit' })
  }

  // cv검증요소
  const check = (elementData: Element) => {
    const element = JSON.stringify(elementData)
    cvPickStore.startCvCheck(pickerType.value, element, () => {
    })
  }

  return {
    pick,
    pickAnchor,
    rePick,
    check,
  }
}
