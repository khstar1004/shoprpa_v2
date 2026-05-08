import { NiceModal } from '@rpa/components'

import { useProcessStore } from '@/stores/useProcessStore'
import useProjectDocStore from '@/stores/useProjectDocStore'
import type { Fun } from '@/types/common'
import { ProcessModal } from '@/views/Arrange/components/process'
import { CATEGORY_MAP } from '@/views/Arrange/config/atom'

import type { IMenuItem } from '../DropdownMenu.vue'

export enum ProcessActionEnum {
  OPEN = 'open',
  RENAME = 'rename',
  DELETE = 'delete',
  SEARCH_CHILD_PROCESS = 'searchChildProcess',
  COPY = 'copy',
  CLOSE_ALL = 'closeAll',
}

export function useProcessMenuActions(params: { item: RPA.Flow.ProcessModule, disabled?: Fun, actions: ProcessActionEnum[] }) {
  const { item, disabled, actions } = params
  const processStore = useProcessStore()

  const renameFn = () => {
    NiceModal.show(ProcessModal, { processItem: item, type: item.resourceCategory })
  }

  const menus: IMenuItem[] = [
    { key: ProcessActionEnum.OPEN, name: '열기', fn: () => processStore.openProcess(item.resourceId) },
    { key: ProcessActionEnum.RENAME, name: '이름 변경', fn: renameFn },
    { key: ProcessActionEnum.DELETE, name: '삭제', fn: () => useProjectDocStore().removeProcessOrModule(item) },
    { key: ProcessActionEnum.COPY, name: `${CATEGORY_MAP[item.resourceCategory]} 복사`, fn: () => useProjectDocStore().copyProcessOrModule(item.resourceCategory, item.resourceId) },
    { key: ProcessActionEnum.SEARCH_CHILD_PROCESS, name: `${CATEGORY_MAP[item.resourceCategory]} 사용 위치 조회`, fn: () => processStore.searchSubProcess(item.resourceId) },
    { key: ProcessActionEnum.CLOSE_ALL, name: '모든 하위 프로세스 닫기', fn: () => processStore.closeAllChildProcess() },
  ]

  return menus.filter(item => actions.includes(item.key as ProcessActionEnum)).map(item => ({ ...item, disabled: disabled(item.key) }))
}
