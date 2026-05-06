import { message } from 'ant-design-vue'
import { uniq } from 'lodash-es'

import i18next from '@/plugins/i18next'

import { useFlowStore } from '@/stores/useFlowStore'
import { toggleContextmenu } from '@/views/Arrange/utils/contextMenu'
import { betweenTowItem, getIdx, getMultiSelectIds } from '@/views/Arrange/utils/flowUtils'

export function changeSelectAtoms(curId: string | null, newIds, isSetLastClickItem = true) {
 const flowStore = useFlowStore()
 if (isSetLastClickItem)
 setLastClickAtomId(curId)
 const selectedIds = newIds || (curId ? [curId] : [])
 curId !== null && curId !== flowStore.activeAtom?.id && flowStore.setActiveAtom(flowStore.simpleFlowUIData.find(item => item.id === curId))
 flowStore.setSelectedAtomIds(selectedIds)
}

// 기록마우스마지막일회클릭의item데이터정보
let lastClickAtomId = null
export function setLastClickAtomId(id: string) {
 lastClickAtomId = id
}

export function getLastClickAtomId() {
 return lastClickAtomId
}

// ctrl, shift다중선택
export function setMultiSelectByClick(item: any, index: number, ctrlKey, shiftKey) {
 // 까지완료선택중점
 let newSelectIds = useFlowStore().selectedAtomIds || []

 // 가져오기닫기 점및점
 const curIds = getMultiSelectIds(item.id)

 let noShiftOps = true
 if (shiftKey && lastClickAtomId) {
 noShiftOps = false
 // shift 선택 위일회클릭및본회클릭의전체선택중
 if (lastClickAtomId === item.id) {
 newSelectIds = curIds
 }
 else {
 const lastIdx = getIdx(lastClickAtomId)
 const curStartIdx = getIdx(curIds[0])
 const curEndIdx = getIdx(curIds[curIds.length - 1])
 const firstIdx = Math.min(lastIdx, curStartIdx, curEndIdx)
 const secondIdx = Math.max(lastIdx, curStartIdx, curEndIdx)
 newSelectIds = betweenTowItem(firstIdx, secondIdx, useFlowStore().simpleFlowUIData).map(i => i.id)
 }
 console.log('shift다중선택ids: ', newSelectIds)
 }
 else if (ctrlKey) {
 // ctrl다중선택
 if (!newSelectIds.includes(item.id)) {
 newSelectIds = newSelectIds.concat(curIds)
 }
 else {
 newSelectIds = newSelectIds.filter(sItem => !curIds.includes(sItem))
 }
 console.log('ctrl다중선택선택의', newSelectIds)
 }
 else {
 // 단일선택
 newSelectIds = curIds
 console.log('단일선택선택의', newSelectIds)
 }

 newSelectIds = uniq(newSelectIds)
 console.log('선택의ids', newSelectIds)
 changeSelectAtoms(item.id, newSelectIds, noShiftOps)
}

// 전체선택
export function setSelectAll() {
 // 까지완료선택중점
 const selectList = useFlowStore().simpleFlowUIData.map(i => i.id)
 if (selectList.length) {
 changeSelectAtoms(null, selectList, false)
 toggleContextmenu({ visible: false }) // 왼쪽 버튼클릭숨김보관오른쪽 버튼메뉴
 }
 else {
 message.error(i18next.t('arrange.noAtomsInFlow'))
 }
}

// 열기시작다중선택공가능시, 추가점
export function addMultiSelectId(id: string) {
 const selectedAtomIds = useFlowStore().selectedAtomIds
 if (!selectedAtomIds.includes(id)) {
 changeSelectAtoms(null, selectedAtomIds.concat(getMultiSelectIds(id)), false)
 }
}

// 열기시작다중선택공가능시, 삭제점
export function deleteMultiSelectId(id: string) {
 const selectedAtomIds = useFlowStore().selectedAtomIds
 if (selectedAtomIds.includes(id)) {
 const delIds = getMultiSelectIds(id)
 changeSelectAtoms(null, selectedAtomIds.filter((id: string) => !delIds.includes(id)), false)
 }
}
