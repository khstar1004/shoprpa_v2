import { useTranslation } from 'i18next-vue'

import { baseUrl } from '@/utils/env'

import { WINDOW_NAME } from '@/constants'
import type { CreateWindowOptions } from '@/platform'
import { windowManager } from '@/platform'

export function useSmartCompPickWindow(closeCallback?: () => void) {
  const { t } = useTranslation()

  // 가능컴포넌트선택
  const openSmartCompPickMenuWindow = async () => {
    // 창할 수 없음후, 아니오이면불가통신경과작업닫기모든창, 가져오기 사용불가닫기
    // windowManager.createWindow 의위치아니오, 생성창지연높음, 가능생성창통신경과 windowManager.setWindowPosition 조정창위치
    const options: CreateWindowOptions = {
      url: `${baseUrl}/smartcompickmenu.html`,
      title: t('smartComponentPick'),
      label: WINDOW_NAME.SMART_COMP_PICK_MENU,
      alwaysOnTop: true,
      width: 160,
      height: 40,
      x: -999,
      y: -999,
      resizable: false,
      decorations: false,
      fileDropEnabled: false,
      maximizable: false,
      transparent: true,
      show: true,
      skipTaskbar: false,
    }

    await windowManager.createWindow(options, closeCallback)
  }

  const open = () => {
    openSmartCompPickMenuWindow()
  }

  return { open }
}