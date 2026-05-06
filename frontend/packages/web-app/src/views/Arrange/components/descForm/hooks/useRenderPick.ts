import { useTranslation } from 'i18next-vue'

import { ATOM_FORM_TYPE } from '@/constants/atom'
import { useCvStore } from '@/stores/useCvStore.ts'
import { useElementsStore } from '@/stores/useElementsStore'

// 사용자 지정테이블단일정렬
export function useRenderPick() {
 const { t } = useTranslation()
 const PickTypeText = {
 [ATOM_FORM_TYPE.CVPICK]: t('common.image'),
 [ATOM_FORM_TYPE.PICK]: t('common.element'),
 }

 // 선택아래목록
 const getOperators = (notEmpty: boolean, itemType: any) => {
 const text = PickTypeText[itemType]
 if (notEmpty) {
 return [
 { label: t('common.editSomething', { name: text }), key: 'editPick' },
 { label: t('common.pickSomething', { name: text }), key: 'pick' },
 { label: t('common.selectSomething', { name: text }), key: 'selectPick' },
 ]
 }
 return [
 { label: t('common.pickSomething', { name: text }), key: 'pick' },
 { label: t('common.selectSomething', { name: text }), key: 'selectPick' },
 ]
 }

 // 가져오기기본값텍스트
 const getDefaultText = (itemType: any) => {
 return t('common.pickSomething', { name: PickTypeText[itemType] })
 }

 // 빈중단
 const notEmpty = (itemData: RPA.AtomDisplayItem) => {
 return itemData.value
 && Array.isArray(itemData.value)
 && itemData.value.filter(i => i.value).length > 0
 }

 // 가져오기선택이미지url
 const getPickImg = (itemData: RPA.AtomDisplayItem, itemType: string) => {
 const treeData = itemType === ATOM_FORM_TYPE.CVPICK ? useCvStore().cvTreeData : useElementsStore().elements
 const treeItem = itemData.value
 ? treeData.find(item =>
 item.elements.some(i => i.id === itemData.value[0]?.data),
 )
 : null
 const imageUrl = treeItem
 ? treeItem.elements.find(
 i => i.id === itemData.value[0].data,
 )?.imageUrl
 : ''
 return imageUrl
 }

 return {
 getOperators,
 getDefaultText,
 notEmpty,
 getPickImg,
 PickTypeText,
 }
}
