import { message } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { defineStore } from 'pinia'
import { ref } from 'vue'

import { RpaCvPicker } from '@/api/cvpick'
import { windowManager } from '@/platform'

export const useCvPickStore = defineStore('cvPickStore', () => {
  const isPicking = ref(false) // 정상에서선택
  const isChecking = ref(false) // 정상에서검증

  const pickTypeMap = { // Cv선택선택유형ELEMENT
    '': 'ELEMENT',
    'cv': 'ELEMENT',
    'CV': 'ELEMENT',
  }
  const pickerType = ref('')
  // 선택결과
  function finishPick() {
    isPicking.value = false
    RpaCvPicker.destroy()
    windowManager.maximizeWindow(true)
    console.log('finishPick')
  }
  // 검증결과
  function finishCheck() {
    isChecking.value = false
    RpaCvPicker.destroy()
    windowManager.maximizeWindow(true)
  }
  // 열기 선택
  /**
   *
   * @param type  유형 '' 통신선택,  ''similar' 정도선택, 'cv' cv선택
   * @param element  요소데이터,  정도선택시, element비워 둘 수 없습니다
   * @param pickStep  pickStep 선택 'new' | 'repick' | 'anchor'
   * @param callback 성공/실패돌아가기조정
   */
  const startCvPick = (type: string, element: any, pickStep = 'new', callback: (params: { success: boolean, data: any }) => void) => {
    isPicking.value = true
    // 시작선택
    RpaCvPicker.create(() => {
      windowManager.minimizeWindow()
      const _pickType = pickTypeMap[type] || 'ELEMENT'
      pickerType.value = _pickType
      const pickParams = element ? JSON.stringify(element) : ''
      const sign = pickStep === 'anchor' ? 'DESIGNATE' : 'START'
      setTimeout(() => {
        RpaCvPicker.send({ pick_sign: sign, pick_type: _pickType, data: pickParams })
      }, 500)
    })
    // 지정메시지
    RpaCvPicker.bindMessage((res) => {
      const { key, data, err_msg } = res || {} // key: 'success' | 'error' | 'ping'
      console.log('bindMessage: ', res)
      if (key === 'success' && data) {
        const dataObj = JSON.parse(data)
        callback && callback({
          success: true,
          data: dataObj,
        })
        finishPick()
      }
      if (key === 'cancel') {
        message.error(err_msg || '선택이 취소되었습니다.')
        finishPick()
      }
      if (key === 'error') {
        message.error(err_msg || useTranslation().t('rpaPickerUnavailable'))
        finishPick()
      }
    })
    // 지정닫기
    RpaCvPicker.bindClose(() => {
      callback
      && callback({
        success: false,
        data: null,
      })
      finishPick()
    })
    // 지정오류
    RpaCvPicker.bindError(() => {
      message.error(useTranslation().t('rpaPickerUnavailable'))
    })
  }
  // 열기 검증
  const startCvCheck = (type: string, data: any, callback: (params: { success: boolean, data: any }) => void) => {
    // console.log('startCheck: ', data)
    isChecking.value = true
    // 시작검증
    RpaCvPicker.create(() => {
      windowManager.minimizeWindow()
      setTimeout(() => {
        const _pickType = pickTypeMap[type] || 'ELEMENT'
        RpaCvPicker.send({ pick_sign: 'VALIDATE', pick_type: _pickType, data })
        isChecking.value = false // 검증시, 아니오loading
      }, 500)
    })
    // 지정메시지
    RpaCvPicker.bindMessage((res) => {
      const { key, data, err_msg } = res || {} // key: 'success' | 'error' | 'ping'
      if (key === 'success' && data) {
        callback
        && callback({
          success: true,
          data: res,
        })
        finishPick()
      }
      if (key === 'cancel') {
        message.error(err_msg || '선택이 취소되었습니다.')
        finishPick()
      }
      if (key === 'error') {
        message.error(err_msg || useTranslation().t('rpaPickerUnavailable'))
        finishPick()
      }

      finishCheck()
    })
    // 지정닫기
    RpaCvPicker.bindClose(() => {
      callback
      && callback({
        success: false,
        data: null,
      })
      finishCheck()
    })
    // 지정오류
    RpaCvPicker.bindError(() => {
      message.error(useTranslation().t('rpaPickerUnavailable'))
    })
  }

  return {
    startCvPick,
    isPicking,
    isChecking,
    startCvCheck,
  }
})
