import type { Ref } from 'vue'

import BUS from '@/utils/eventBus'

import { useElementsStore } from '@/stores/useElementsStore'
import { usePickStore } from '@/stores/usePickStore'
import { useCreateWindow } from '@/views/Arrange/hook/useCreateWindow'

function useFormPick(type: string = '', status?: Ref<boolean>, elementPickModal?: () => void, itemData?: any) {
  const { openDataPickWindow } = useCreateWindow()
  const usePick = usePickStore()
  const useElements = useElementsStore()

  if (type === 'BATCH') { // 데이터가져오기열기시작외부의창
    openDataPickWindow()
    return
  }

  if (type === 'POINT') {
    usePick.startMousePick((res) => {
      BUS.$emit('pick-done', { data: res.data?.point, value: res.data, picker_type: res.data?.picker_type })
    })
    return
  }

  status.value = true
  usePick.startPick(type, '', (res) => {
    status.value = false
    if (!res.success)
      return

    useElements.setTempElement(res.data)
    elementPickModal?.()
  }, itemData.types)
}

export default useFormPick
