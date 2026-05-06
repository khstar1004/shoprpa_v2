import { message } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { defineStore } from 'pinia'
import { ref } from 'vue'

import { RpaPicker } from '@/api/pick'
import { useSmartCompPickWindow } from '@/components/SmartComponent/hooks'
import { WINDOW_NAME } from '@/constants'
import { SMART_COMP_PICK_EVENT } from '@/constants/smartCompPick'
import { utilsManager, windowManager } from '@/platform'

import { useVariableStore } from './useVariableStore'

export const useSmartCompPickStore = defineStore('smartCompPickStore', () => {
  const isPicking = ref(false) // 정상에서선택
  const pickData = ref() // 선택데이터
  let currentCallback: ((params: { success: boolean, data: any }) => void) | null = null // 현재선택의돌아가기조정데이터

  const variableStore = useVariableStore()
  const { t } = useTranslation()
  const { open: openPickMenuWindow } = useSmartCompPickWindow(resetPick)

  utilsManager.listenEvent('w2w', ({ from, target, type: eventType }: any) => {
    if (!isPicking.value || from !== WINDOW_NAME.SMART_COMP_PICK_MENU || target !== WINDOW_NAME.MAIN) {
      return
    }

    if (eventType === SMART_COMP_PICK_EVENT.ZOOM_IN) {
      // 대선택
      startPickAction('SMART_COMPONENT_PREVIOUS')
    }
    else if (eventType === SMART_COMP_PICK_EVENT.ZOOM_OUT) {
      // 소선택
      startPickAction('SMART_COMPONENT_NEXT')
    }
    else if (eventType === SMART_COMP_PICK_EVENT.CONFIRM) {
      // 
      startPickAction('SMART_COMPONENT_END', () => {
        finishPick()
        currentCallback && currentCallback({ success: true, data: pickData.value })
      })
    }
    else if (eventType === SMART_COMP_PICK_EVENT.CANCEL) {
      // 가져오기 
      startPickAction('SMART_COMPONENT_CANCEL', () => {
        finishPick()
        currentCallback && currentCallback({ success: false, data: null })
      })
    }
    else if (eventType === SMART_COMP_PICK_EVENT.ERROR_DIALOG_CONFIRM) {
      // 예결과예완료대/소단계, 통신경과 pickData 계획위치복사
      // 예결과예오류(예웹 페이지요소), 다시 열기 선택
      const menuPosition = calculateMenuPosition(pickData.value)
      if (menuPosition) {
        notifyMenuShow(menuPosition)
      }
      else {
        RpaPicker.destroy()
        startPickAction('SMART_COMPONENT_START')
      }
    }
  })

  // 알림
  const notifyMenuShow = (data: { x: number, y: number }) => {
    windowManager.emitTo({
      type: SMART_COMP_PICK_EVENT.SHOW_MENU,
      target: WINDOW_NAME.SMART_COMP_PICK_MENU,
      from: WINDOW_NAME.MAIN,
      data,
    })
  }

  // 알림오류대화상자
  const notifyErrorDialog = (errorMsg?: string) => {
    windowManager.emitTo({
      type: SMART_COMP_PICK_EVENT.SHOW_ERROR_DIALOG,
      target: WINDOW_NAME.SMART_COMP_PICK_MENU,
      from: WINDOW_NAME.MAIN,
      data: { errorMsg },
    })
  }

  // 알림
  const notifyMenuHide = () => {
    windowManager.emitTo({
      type: SMART_COMP_PICK_EVENT.HIDE_MENU,
      target: WINDOW_NAME.SMART_COMP_PICK_MENU,
      from: WINDOW_NAME.MAIN,
    })
  }

  // 근거 pickData 계획메뉴위치
  const calculateMenuPosition = (data: any): { x: number, y: number } | null => {
    if (!data)
      return null

    const rect = data.path?.rect
    const win_rect = data.path?.win_rect
    if (!rect || !win_rect)
      return null

    const devicePixelRatio = window.devicePixelRatio || 1
    const menuWidth = 160 * devicePixelRatio
    const menuHeight = 40 * devicePixelRatio

    const winWidth = win_rect.right
    const winHeight = win_rect.bottom

    const rightBottomX = rect.right
    const rightBottomY = rect.bottom

    // 조회요소오른쪽아래역할여부에서창내부
    const isRightBottomInWindow = rightBottomX >= 0
      && rightBottomX <= winWidth
      && rightBottomY >= 0
      && rightBottomY <= winHeight

    // 조회요소오른쪽아래역할위치여부가능메뉴(메뉴너비정도및높음정도)
    const canShowAtRightBottom = rightBottomX - menuWidth >= 0
      && rightBottomY + menuHeight <= winHeight

    let x = 0
    let y = 0

    if (isRightBottomInWindow && canShowAtRightBottom) {
      // 에서오른쪽아래역할
      x = rightBottomX - menuWidth
      y = rightBottomY
    }
    else {
      // 에서오른쪽위역할(rect 위방법)
      x = rightBottomX - menuWidth
      y = rect.y - menuHeight

      // 예결과오른쪽위역할초과출력창, 이면조정까지창내부
      if (x < 0) {
        x = 0
      }
      if (y < 0) {
        // 예결과위방법빈아니오, 이면에서 rect 아래방법(에서창내부)
        y = rect.bottom
      }
      // 확인메뉴아니오초과출력창오른쪽가장자리및아래가장자리
      if (x + menuWidth > winWidth) {
        x = winWidth - menuWidth
      }
      if (y + menuHeight > winHeight) {
        y = winHeight - menuHeight
      }
    }

    console.log('smartpick position', { x, y })
    return { x, y }
  }

  // 선택결과
  function finishPick() {
    isPicking.value = false
    notifyMenuHide()
    RpaPicker.destroy()
    windowManager.showWindow()
  }

  /**
   * 열기 선택
   * @param callback 성공/실패돌아가기조정
   */
  const startPick = (callback: (params: { success: boolean, data: any }) => void) => {
    isPicking.value = true
    pickData.value = null
    currentCallback = callback // 저장현재돌아가기조정

    // 열기 
    openPickMenuWindow()

    startPickAction('SMART_COMPONENT_START')
  }

  /**
   * 열기 검증
   */
  const startCheck = (data: any, callback: (params: { success: boolean, data: any }) => void) => {
    const ext_data = { global: variableStore.globalVariableList }
    // 시작검증
    RpaPicker.create(() => {
      windowManager.minimizeWindow()
      setTimeout(() => {
        RpaPicker.send({ pick_sign: 'VALIDATE', pick_type: 'ELEMENT', data, ext_data })
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
      finishPick()
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

  const startPickAction = (action: string, ofterSendCb?: () => void) => {
    // 시작선택
    RpaPicker.create(() => {
      setTimeout(() => {
        const sendParams = {
          pick_sign: 'SMART_COMPONENT',
          pick_type: 'ELEMENT',
          data: JSON.stringify(pickData.value),
          smart_component_action: action,
        }
        RpaPicker.send(sendParams)
        ofterSendCb && ofterSendCb()
        if (action === 'SMART_COMPONENT_START') {
          windowManager.hideWindow()
        }
      }, 500)
    })

    // 지정메시지
    RpaPicker.bindMessage((res) => {
      const { key, data, err_msg } = res || {} // key: 'success' | 'error' | 'ping'
      console.log('smartCompPick bindMessage: ', res)

      if (key === 'success' && data) {
        pickData.value = JSON.parse(data)
        const menuPosition = calculateMenuPosition(pickData.value)

        if (!menuPosition) {
          // 선택요소웹 페이지요소시, rect 및 win_rect 찾을 수 없습니다, 오류대화상자
          RpaPicker.destroy()
          notifyErrorDialog(t('smartCompPick.onlyWebAutomation'))
          return
        }

        notifyMenuShow(menuPosition)
        RpaPicker.destroy()
      }
      if (key === 'error') {
        const errorMsg = data || err_msg || t('rpaPickerUnavailable')
        console.error(errorMsg)
        if (action === 'SMART_COMPONENT_PREVIOUS') {
          RpaPicker.destroy()
          notifyErrorDialog(t('smartCompPick.maxLevelReached'))
        }
        else if (action === 'SMART_COMPONENT_NEXT') {
          RpaPicker.destroy()
          notifyErrorDialog(t('smartCompPick.minLevelReached'))
        }
        else {
          message.error(errorMsg)
          finishPick()
        }
      }
      if (key === 'cancel') {
        finishPick()
      }
    })

    // 지정닫기
    RpaPicker.bindClose(() => {
      RpaPicker.destroy()
    })

    // 지정오류
    RpaPicker.bindError(() => {
      message.error(t('rpaPickerUnavailable'))
    })
  }

  function resetPick() {
    if (isPicking.value) {
      startPickAction('SMART_COMPONENT_CANCEL', () => {
        finishPick()
      })
    }
  }

  return {
    isPicking,
    startPick,
    startCheck,
    resetPick,
  }
})