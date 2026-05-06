import { createInjectionState } from '@vueuse/core'
import type { MaybeRef } from 'vue'
import { computed, ref, unref, watch } from 'vue'

type Position = 'top' | 'bottom' | ''

const [useRenderListProvide, useRenderList] = createInjectionState((rawList: MaybeRef<RPA.Atom[]>) => {
 const renderList = computed(() => [...unref(rawList).slice(0, insertItemIndex.value), unref(insertItem), ...unref(rawList).slice(insertItemIndex.value)]) // 패키지금액외부삽입의대기목록
 const insertItem = ref({ id: 'insertItem' } as RPA.Atom) // 금액외부삽입
 const insertItemIndex = ref(unref(rawList).length) // 금액외부삽입검색
 const insertItemLast = computed(() => renderList.value[insertItemIndex.value - 1]) // 삽입위일목록
 const insertItemNext = computed(() => renderList.value[insertItemIndex.value + 1]) // 삽입아래일목록

 function triggerInsert(item: RPA.Atom, position: Position) {
 const index = adjustIndex(renderList.value.findIndex(i => i.id === item.id)) // 가져오기트리거검색(검사정상후)
 const targetIndex = position === 'top' ? index : index + 1 // 만약트리거위치로bottom, 목록표시삽입검색필요추가일
 insertItemIndex.value = targetIndex
 }

 function resetRenderList() {
 insertItemIndex.value = unref(rawList).length
 }

 function adjustIndex(index: number, isInsert?: boolean) {
 if (isInsert) {
 return index > insertItemIndex.value ? Math.max(index - 1, 0) : index
 }
 else {
 return index >= insertItemIndex.value ? Math.max(index - 1, 0) : index
 }
 }

 function canInsert(item: RPA.Atom, position: Position) {
 if (item.id === insertItem.value.id) {
 return false
 }
 else if (position === 'top' && item.id === insertItemNext.value?.id) {
 return false
 }
 else if (position === 'bottom' && item.id === insertItemLast.value?.id) {
 return false
 }
 else {
 return true
 }
 }

 watch(() => unref(rawList).length, (newVal, oldVal) => {
 // 추가/삭제점시재삽입위치
 if (newVal !== oldVal) {
 resetRenderList()
 }
 })

 return {
 renderList,
 insertItem,
 insertItemIndex,
 insertItemLast,
 insertItemNext,
 triggerInsert,
 canInsert,
 adjustIndex,
 resetRenderList,
 }
})

export { useRenderList, useRenderListProvide }
