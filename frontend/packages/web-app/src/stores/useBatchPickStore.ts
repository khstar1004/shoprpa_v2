import { message } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { defineStore } from 'pinia'
import { ref } from 'vue'

import { RpaPicker } from '@/api/pick'
import { windowManager } from '@/platform'
import type { PickParams } from '@/types/resource'

import { useVariableStore } from './useVariableStore'

export const useBatchPickStore = defineStore('batchPick', () => {
  const isPicking = ref(false)
  const pickerType = ref('')
  const variableStore = useVariableStore()
  let isHighlight = false // 여부높음요소

  const { t } = useTranslation()

  const pickTypeMap = {
    BATCH: 'BATCH', // 량가져오기
    HIGHLIGHT: 'HIGHLIGHT', // 높음
    GRAB: 'GRAB', // 가져오기
    ELEMENT: 'ELEMENT',
  }

  // 선택결과
  function finishPick() {
    isPicking.value = false
    RpaPicker.destroy()
    windowManager.restoreWindow()
  }
  function finishCheck() {
    isPicking.value = false
    RpaPicker.destroy()
  }
  /**
   * 시작데이터가져오기
   */
  const startBatchPick = (type: string, batchParams: object, callback: (params: { success: boolean, data: any }) => void) => {
    type = type.toUpperCase()
    isPicking.value = true

    // 시작선택
    RpaPicker.create(() => {
      const _pickType = pickTypeMap[type] || 'BATCH'
      pickerType.value = _pickType
      const data = batchParams ? JSON.stringify(batchParams) : ''
      const ext_data = { global: variableStore.globalVariableList }
      setTimeout(() => {
        const sendParams: PickParams = {
          pick_sign: 'START',
          pick_type: _pickType,
          data,
          ext_data,
        }
        RpaPicker.send(sendParams)
      }, 500)
    })
    // 지정메시지
    RpaPicker.bindMessage((res) => {
      const { key, data, err_msg } = res || {} // key: 'success' | 'error' | 'ping' | 'interaction'
      if (key === 'success' && data) {
        const dataObj = JSON.parse(data)
        if (dataObj.app) {
          callback && callback({
            success: true,
            data: dataObj,
          })
        }
      }
      if (key === 'error') {
        message.error(err_msg || t('rpaPickerUnavailable'))
        callback && callback({
          success: false,
          data: null,
        })
        finishPick()
      }
      if (key === 'cancel') {
        callback && callback({
          success: false,
          data: null,
        })
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
      isPicking.value = false
    })
  }

  /**
   * 전송메시지
   */
  const sendMessage = (params: object) => {
    RpaPicker.send(params)
  }
  /**
   * 가져오기요소데이터, 유형선택검증, 및실행기존가능
   */
  const getBatchData = (type: string, data: string, callback: (params: { success: boolean, data: any }) => void) => {
    type = type.toUpperCase()
    isPicking.value = true
    const ext_data = { global: variableStore.globalVariableList }
    // 시작검증
    RpaPicker.create(() => {
      setTimeout(() => {
        const _pickType = pickTypeMap[type]
        RpaPicker.send({ pick_sign: 'GAIN', pick_type: _pickType, data, ext_data })
        isPicking.value = false
      }, 500)
    })
    // 지정메시지
    RpaPicker.bindMessage((res) => {
      if (res.key === 'success' && res.data) {
        const dataObj = JSON.parse(res.data)
        callback && callback({
          success: true,
          data: dataObj,
        })
      }
    })
    // 지정닫기
    RpaPicker.bindClose(() => {
      callback
      && callback({
        success: false,
        data: null,
      })
      finishCheck()
    })
    // 지정오류
    RpaPicker.bindError(() => {
      message.error(t('rpaPickerUnavailable'))
    })
  }
  /**
   * 높음요소
   */
  const highLight = (data: string, callback?: (params: { success: boolean, data: any }) => void) => {
    if (isHighlight)
      return
    const ext_data = { global: variableStore.globalVariableList }
    RpaPicker.create(() => {
      isHighlight = true
      setTimeout(() => {
        RpaPicker.send({
          pick_sign: 'HIGHLIGHT',
          pick_type: pickTypeMap.ELEMENT,
          data,
          ext_data,
        })
      }, 500)
    })
    RpaPicker.bindClose(() => {
      RpaPicker.destroy()
      isHighlight = false
    })
    RpaPicker.bindError(() => {
      message.error(t('rpaPickerUnavailable'))
      isHighlight = false
    })
    RpaPicker.bindMessage((res) => {
      callback && callback({
        success: true,
        data: res,
      })
    })
  }

  return {
    isPicking,
    startBatchPick,
    sendMessage,
    getBatchData,
    highLight,
    finishCheck,
  }
})
