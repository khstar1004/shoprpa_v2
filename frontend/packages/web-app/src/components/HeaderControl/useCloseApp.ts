import { ExclamationCircleOutlined } from '@ant-design/icons-vue'
import { App } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { createVNode } from 'vue'
import { useRoute } from 'vue-router'

import { useRouteBack } from '@/hooks/useCommonRoute'
import { windowManager } from '@/platform'
import { useProcessStore } from '@/stores/useProcessStore'
import useProjectDocStore from '@/stores/useProjectDocStore'
import useUserSettingStore from '@/stores/useUserSetting.ts'

import CloseTip from './CloseTip.vue'

export function useCloseApp() {
  const { message, modal } = App.useApp()
  const processStore = useProcessStore()
  const { t } = useTranslation()
  const route = useRoute()
  let modalInstance = null
  let timeoutModal = null

  const closeWindow = () => {
    windowManager.closeWindow()
  }

  const minimizeWindowTray = () => {
    windowManager.hideWindow()
  }

  const modalCloseApp = () => {
    if (modalInstance)
      return

    modalInstance = modal.confirm({
      title: t('closeConfirmExit'),
      icon: createVNode(ExclamationCircleOutlined),
      content: createVNode(CloseTip),
      okText: t('confirm'),
      cancelText: t('cancel'),
      onOk() {
        useUserSettingStore().userSetting.commonSetting.closeMainPage ? minimizeWindowTray() : closeWindow()
        modalInstance = null
      },
      onCancel() {
        modalInstance = null
      },
      centered: true,
      keyboard: false,
    })
  }
  // 네트워크시간 초과, 안내
  const modalSaveTimeout = () => {
    if (timeoutModal)
      return
    timeoutModal = modal.confirm({
      title: t('networkError'),
      content: t('saveErrorAndQuit'),
      okText: t('confirm'),
      cancelText: t('cancel'),
      onOk() {
        useRouteBack()
        timeoutModal = null
      },
      onCancel() {
        timeoutModal = null
      },
      centered: true,
      keyboard: false,
    })
  }

  // 정렬페이지저장, 출력
  const autoSave = async () => {
    // 해제저장연결길이시간아니오반환, 변환까지목록페이지
    const timer = setTimeout(() => modalSaveTimeout(), 5000)

    try {
      await processStore.saveProject()
      // 저장 후 목록 화면으로 돌아간다.
      await message.success('저장되었습니다.', 0.5)
      useRouteBack()
      message.destroy()
      useProjectDocStore().clearAllData()
    }
    catch {
      modalSaveTimeout()
    }

    clearTimeout(timer)
  }

  const closeApp = () => {
    if (route.meta.closeConfirm === false) {
      autoSave()
    }
    else {
      modalCloseApp()
    }
  }

  return { closeApp }
}
