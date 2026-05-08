import { NiceModal } from '@rpa/components'

import { ElementGroupFormModal } from '@/components/ElementGroupFormModal'
import GlobalModal from '@/components/GlobalModal'
import { useCvStore } from '@/stores/useCvStore.ts'
import { useElementsStore } from '@/stores/useElementsStore'

export function useGroupManager() {
  const useElements = useElementsStore()
  const useCv = useCvStore()
  const elementGroupFormModal = NiceModal.useModal(ElementGroupFormModal)

  // 추가분그룹
  const addGroup = (type: 'cv' | 'common') => {
    elementGroupFormModal.show({
      onConfirm: (gname) => {
        // 추가또는이름 변경분그룹
        if (type === 'cv') {
          useCv.addGroup(gname).then(() => {
            elementGroupFormModal.hide()
          })
        }
        if (type === 'common') {
          useElements.addGroup(gname).then(() => {
            elementGroupFormModal.hide()
          })
        }
      },
    })
  }

  // 이름 변경분그룹
  const renameGroup = (groupItem, type: 'cv' | 'common') => {
    elementGroupFormModal.show({
      groupItem,
      onConfirm: (gname) => {
        // 이름 변경분그룹
        if (type === 'cv') {
          useCv.renameGroup(groupItem.id, gname).then(() => {
            elementGroupFormModal.hide()
          })
        }
        if (type === 'common') {
          useElements.renameGroup(groupItem.id, gname).then(() => {
            elementGroupFormModal.hide()
          })
        }
      },
    })
  }

  // 삭제분그룹
  const delGroup = (groupItem, type: 'cv' | 'common') => {
    const typeStr = type === 'cv' ? '이미지' : '요소'
    const modal = GlobalModal.confirm({
      title: `${typeStr} 그룹 삭제`,
      content: `"${groupItem.name}" 그룹과 그룹 안의 ${typeStr}를 삭제하시겠습니까? 삭제 후에는 참조 단계를 다시 설정해야 할 수 있습니다.`,
      onOk() {
        if (type === 'cv') {
          useCv.deleteGroup(groupItem.id)
        }
        if (type === 'common') {
          useElements.deleteGroup(groupItem.id)
        }
        modal.destroy()
      },
      closable: true,
      centered: true,
      keyboard: false,
    })
  }
  // 동작까지분그룹
  const move2Group = (originId: string, targetId: string, type: 'cv' | 'common') => {
    if (type === 'cv') {
      useCv.moveCvItem(originId, targetId)
    }

    if (type === 'common') {
      useElements.moveGroup(originId, targetId)
    }
  }

  return {
    addGroup,
    renameGroup,
    delGroup,
    move2Group,
  }
}
