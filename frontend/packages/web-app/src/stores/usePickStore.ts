import { NiceModal } from '@rpa/components'
import { message } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

import BUS from '@/utils/eventBus'
import $loading from '@/utils/globalLoading'

import { RpaPicker } from '@/api/pick'
import { windowManager } from '@/platform'
import { useElementsStore } from '@/stores/useElementsStore'
import type { PickParams } from '@/types/resource'
import { ElementPickModal } from '@/views/Arrange/components/pick'

import { useVariableStore } from './useVariableStore'

export const usePickStore = defineStore('pickStore', () => {
  const isPicking = ref(false) // 정상에서선택
  const isChecking = ref(false) // 정상에서검증
  const isDataPicking = ref(false) // 정상에서데이터가져오기
  const pickerType = ref('')

  const variableStore = useVariableStore()
  const { t } = useTranslation()
  const useElements = useElementsStore()
  const elementPickModal = NiceModal.useModal(ElementPickModal)

  const pickTypeMap = {
    '': 'ELEMENT', // 통신선택
    'ELEMENT': 'ELEMENT', // 통신선택
    'WEBPICK': 'ELEMENT', // web선택
    'WINPICK': 'ELEMENT', // win선택
    'SIMILAR': 'SIMILAR', // 선택
    'CV': 'CV',
    'WINDOW': 'WINDOW', // 창선택
    'POINT': 'POINT', // 선택
    'BATCH': 'BATCH', // 량가져오기
  }
  const validTypeMap = {
    '': 'ELEMENT', // 통신선택
    'ELEMENT': 'ELEMENT', // 통신선택
    'WEBPICK': 'ELEMENT', // web선택
    'WINPICK': 'ELEMENT', // win선택
    'SIMILAR': 'ELEMENT', // 선택
    'CV': 'CV',
    'WINDOW': 'WINDOW', // 창선택
    'POINT': 'POINT', // 선택
  }
  // 선택결과
  function finishPick() {
    isPicking.value = false
    RpaPicker.destroy()
    windowManager.maximizeWindow(true)
  }
  // 검증결과
  function finishCheck(finshType = 'maximize') {
    isChecking.value = false
    RpaPicker.destroy()
    finshType === 'maximize' ? windowManager.maximizeWindow(true) : windowManager.restoreWindow()
  }
  // 열기 마우스위치선택
  const startMousePick = (callback: (params: { success: boolean, data: any }) => void) => {
    // 시작선택
    RpaPicker.create(() => {
      const _pickType = pickTypeMap.POINT
      pickerType.value = _pickType
      setTimeout(() => {
        const sendParams: PickParams = {
          pick_sign: 'START',
          pick_type: _pickType,
          data: '',
        }
        RpaPicker.send(sendParams)
        windowManager.minimizeWindow()
      }, 500)
    })
    // 지정메시지
    RpaPicker.bindMessage((res) => {
      const { key, data, err_msg } = res || {} // key: 'success' | 'error' | 'ping'
      console.log('startPick res: ', res)
      if (key === 'success' && data) {
        const dataObj = JSON.parse(data)
        callback && callback({
          success: true,
          data: dataObj,
        })
        finishPick()
      }
      if (key === 'error') {
        const errorMsg = data || err_msg || t('rpaPickerUnavailable')
        message.error(errorMsg)
        finishPick()
      }
      if (key === 'cancel') {
        finishPick()
      }
    })
    // 지정닫기
    RpaPicker.bindClose(() => {
      callback
      && callback({
        success: false,
        data: null,
      })
      finishPick()
    })
    // 지정오류
    RpaPicker.bindError(() => {
      message.error(t('rpaPickerUnavailable'))
    })
  }

  /**
   * 열기 선택
   * @param type  유형 '' 통신선택,  ''similar' 정도선택, 'cv' cv선택
   * @param element  요소데이터,  정도선택시, element비워 둘 수 없습니다
   * @param callback 성공/실패돌아가기조정
   * @param mode 가능선택, 선택가능지정/web대기, 기존가능매칭중선택
   */
  const startPick = (type: string, element: any, callback: (params: { success: boolean, data: any }) => void, mode = '') => {
    type = type.toUpperCase()
    isPicking.value = true
    // 시작선택
    RpaPicker.create(() => {
      const _pickType = pickTypeMap[type] || 'ELEMENT'
      console.log('type: ', type)
      console.log('_pickType: ', _pickType)
      console.log('element: ', element)
      pickerType.value = _pickType
      const data = element ? JSON.stringify(element) : ''
      const ext_data = { global: variableStore.globalVariableList }
      setTimeout(() => {
        const sendParams: PickParams = {
          pick_sign: 'START',
          pick_type: _pickType,
          pick_mode: mode,
          data,
        }
        console.log('startPick sendParams: ', sendParams)
        if (_pickType === 'SIMILAR') { // 선택 위ext_data
          sendParams.ext_data = ext_data
        }
        RpaPicker.send(sendParams)
        windowManager.minimizeWindow()
      }, 500)
    })
    // 지정메시지
    RpaPicker.bindMessage((res) => {
      const { key, data, err_msg } = res || {} // key: 'success' | 'error' | 'ping'
      if (key === 'success' && data) {
        finishPick()
        const dataObj = JSON.parse(data)
        if (dataObj.app) {
          callback?.({ success: true, data: dataObj })
        }
      }
      if (key === 'error') {
        const errorMsg = data || err_msg || t('rpaPickerUnavailable')
        message.error(errorMsg)
        finishPick()
      }
      if (key === 'cancel') {
        finishPick()
      }
    })
    // 지정닫기
    RpaPicker.bindClose(() => {
      callback?.({
        success: false,
        data: null,
      })
      finishPick()
    })
    // 지정오류
    RpaPicker.bindError(() => {
      message.error(t('rpaPickerUnavailable'))
    })
  }
  // 열기 검증
  const startCheck = (type: string, data: any, callback: (params: { success: boolean, data: any }) => void, finshType = 'maximize') => {
    // console.log('startCheck: ', data)
    type = type.toUpperCase()
    isChecking.value = true
    const ext_data = { global: variableStore.globalVariableList }
    // 시작검증
    RpaPicker.create(() => {
      windowManager.minimizeWindow()
      setTimeout(() => {
        const _pickType = validTypeMap[type] || 'ELEMENT'
        RpaPicker.send({ pick_sign: 'VALIDATE', pick_type: _pickType, data, ext_data })
        isChecking.value = false // 검증시, 아니오loading
      }, 500)
    })
    // 지정메시지
    RpaPicker.bindMessage((res) => {
      console.log('startCheck res: ', res)
      if (res && res.key === 'success') {
        callback && callback({
          success: true,
          data: res,
        })
      }
      else {
        const { data, err_msg } = res || {}
        const errorMsg = data || err_msg || t('rpaPickerUnavailable')
        message.error(errorMsg)
        callback?.({
          success: false,
          data: null,
        })
      }
      finishCheck(finshType)
    })
    // 지정닫기
    RpaPicker.bindClose(() => {
      callback?.({
        success: false,
        data: null,
      })
      finishCheck(finshType)
    })
    // 지정오류
    RpaPicker.bindError(() => {
      message.error(t('rpaPickerUnavailable'))
    })
  }

  // 다시 선택
  const repick = (type: string, isModal: boolean = false, group: string, callback?: () => void) => {
    startPick(type, '', (res) => {
      // console.log('repick res: ', res)
      if (res.success) {
        useElements.setTempElement(res.data, 'repick', group)
      }
      isModal && elementPickModal.show()
      callback && callback()
    })
  }
  // 선택
  const similarPick = (element: any, callback?: () => void) => {
    startPick('SIMILAR', element, (res) => {
      console.log('similarPick res: ', res)
      if (res.success) {
        useElements.setTempElement(res.data, 'similar')
      }
      callback && callback()
    })
  }
  // 새생성선택
  const newPick = (type: string, callback?: () => void) => {
    startPick(type, '', (res) => {
      if (res.success) {
        useElements.setTempElement(res.data)
        elementPickModal.show({ isContinue: true })
      }
      callback?.()
    })
  }

  // groupPick
  const groupPick = (type: string, group: string, callback?: () => void) => {
    startPick(type, '', (res) => {
      if (res.success) {
        useElements.setTempElement(res.data, 'new', group)
        elementPickModal.show()
        callback && callback()
      }
    })
  }
  // set isDataPicking
  const setDataPicking = (val: boolean) => {
    isDataPicking.value = val
    BUS.$once('batch-close', () => {
      isDataPicking.value = false
    })
  }

  watch(isDataPicking, (val) => {
    if (val) {
      $loading.open({ msg: '데이터를 가져오는 중입니다. 작업 창을 닫지 마세요.', timeout: 100 * 60 })
    }
    else {
      $loading.close()
    }
  })

  return {
    isPicking,
    isChecking,
    isDataPicking,
    startMousePick,
    startPick,
    startCheck,
    repick,
    similarPick,
    newPick,
    groupPick,
    setDataPicking,
  }
})
