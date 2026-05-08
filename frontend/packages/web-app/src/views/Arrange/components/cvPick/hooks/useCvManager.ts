import { NiceModal } from '@rpa/components'

import GlobalModal from '@/components/GlobalModal/index.ts'
import { useCvStore } from '@/stores/useCvStore.ts'
import type { Element } from '@/types/resource.d'

import { CvPickModal } from '../modals'

export function useCvManager() {
  // cv선택데이터
  const editCvItem = (itemData: Element, groupId: string) => {
    useCvStore().getCvItemDetail(itemData.id).then((res: any) => {
      useCvStore().setCurrentCvItem({ ...res })
      NiceModal.show(CvPickModal, { groupId, entry: 'edit' })
    })
  }
  // 삭제cv선택데이터
  const delCvItem = (itemData: Element) => {
    const modal = GlobalModal.confirm({
      title: '이미지 삭제',
      content: '현재 이미지를 삭제하시겠습니까? 삭제 후에는 이 이미지를 참조하는 단계에서 다시 선택해야 합니다.',
      onOk() {
        useCvStore().deleteCvItem(itemData)
        modal.destroy()
      },
      closable: true,
      centered: true,
      keyboard: false,
    })
  }

  // 조회프로세스중요소사용
  const setQuotedItem = (itemData: Element) => {
    useCvStore().setQuotedItem(itemData)
  }

  return {
    editCvItem,
    delCvItem,
    setQuotedItem,
  }
}
