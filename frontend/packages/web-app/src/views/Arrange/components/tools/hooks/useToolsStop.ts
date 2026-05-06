import { message } from 'ant-design-vue'
import { throttle } from 'lodash-es'

import { STOP_RUN } from '@/constants/shortcuts'
import { useProcessStore } from '@/stores/useProcessStore'
import { useRunningStore } from '@/stores/useRunningStore'
import type { ArrangeTools } from '@/views/Arrange/types/arrangeTools'

export function useToolsStop() {
 const handleConfirmStop = throttle(() => {
 useRunningStore().stop(useProcessStore().project.id)
 }, 1500, { leading: true, trailing: false })

 const item: ArrangeTools = {
 key: 'stop',
 title: 'stop',
 name: 'stop',
 fontSize: '',
 icon: 'tools-stop',
 action: '',
 loading: false,
 color: '#EC483E',
 show: ({ status }) => ['debug', 'run'].includes(status),
 disable: ({ status }) => ['free'].includes(status),
 hotkey: STOP_RUN,
 clickFn: handleConfirmStop,
 validateFn: ({ disable, show }) => {
 if (disable || !show) {
 message.warning('실행 또는 디버그 중인 프로세스가 없어 중지할 수 없습니다.')
 return false
 }
 return true
 },
 }

 return item
}
