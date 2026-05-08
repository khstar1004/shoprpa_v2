import { message } from 'ant-design-vue'

import { CONTINUE_DEBUG } from '@/constants/shortcuts'
import { useRunningStore } from '@/stores/useRunningStore'
import type { ArrangeTools } from '@/views/Arrange/types/arrangeTools'

export function useToolsDebugContinue() {
  const debugNextBreakpoint = () => {
    useRunningStore().continueDebug()
  }

  const item: ArrangeTools = {
    key: 'debugContinue',
    title: 'debuggingContinues',
    name: 'debuggingContinues',
    fontSize: '',
    icon: 'tools-debug-continue',
    action: '',
    loading: false,
    hotkey: CONTINUE_DEBUG,
    show: ({ status }) => ['debug'].includes(status),
    disable: ({ status, isBreak }) => ['free', 'run'].includes(status) || !isBreak,
    clickFn: debugNextBreakpoint,
    validateFn: ({ disable, show }) => {
      if (!show) {
        message.warning('먼저 디버그 모드를 시작하세요.')
        return false
      }
      if (disable) {
        message.warning('디버그 실행 중입니다. 잠시 후 다시 시도하세요.')
        return false
      }

      return true
    },
  }
  return item
}
