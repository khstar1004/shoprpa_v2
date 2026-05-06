<script setup lang="ts">
import { useTheme } from '@rpa/components'
import { useElementVisibility, useEventBus, useScroll } from '@vueuse/core'
import { message } from 'ant-design-vue'
import { computed, onBeforeUnmount, useTemplateRef, watch } from 'vue'

import { atomScrollIntoViewKey } from '@/constants/eventBusKey'
import { SMARTCOMPONENT } from '@/constants/menu'
import { useRoutePush } from '@/hooks/useCommonRoute'
import { useFlowStore } from '@/stores/useFlowStore'
import { useProcessStore } from '@/stores/useProcessStore'
import { clearDraggable } from '@/views/Arrange/utils/drag'

import ContextMenu from './ContextMunus.vue'
import DraggableVirtualScroller from './DraggableVirtualScroller.vue'
import { addAtomData, draggableAddStyle, MAX_ATOM_NUM, moveAtomData } from './hooks/useFlow'
import { useRenderList, useRenderListProvide } from './hooks/useRenderList'
import { useRunDebug } from './hooks/useRunDebug'
import Item from './Item.vue'

const { colorTheme } = useTheme()
const flowContainer = useTemplateRef<HTMLElement>('flowContainer')
const draggableRef = useTemplateRef('draggableRef')
const flowStore = useFlowStore()
const processStore = useProcessStore()
const rawList = computed(() => flowStore.simpleFlowUIData)

useRenderListProvide(rawList)
const { renderList, adjustIndex, resetRenderList } = useRenderList()

// TODO: 개있음일지정의영역제한, 만약후원자 기능목록수정성공스크롤동작, 필요교체종방식
// 변환버튼의가능
const jumpIconIsVisible = useElementVisibility(() => document.getElementById(`jump-back__${flowStore.jumpFlowId}`))
// 원자 기능목록의스크롤동작파일, 에서스크롤동작결과시만약저장된 변환버튼의원자 기능할 수 없음, 이면숨김보관변환버튼
useScroll(flowContainer, {
 onStop: () => {
 if (flowStore.jumpFlowId && !jumpIconIsVisible.value) {
 flowStore.setJumpFlowId('')
 }
 },
})

const bus = useEventBus(atomScrollIntoViewKey)
bus.on((idOrIndex) => {
 draggableRef.value.scrollTo(idOrIndex)
})

useRunDebug()

function handleDragChange(e: any) {
 if (e.added) {
 const { element, newIndex } = e.added
 flowStore.setSimpleFlowUIDataByType(element, adjustIndex(newIndex, true), false)
 addAtomData(element.data.key, adjustIndex(newIndex, true), true)
 }
 else if (e.moved) {
 const { element, oldIndex, newIndex } = e.moved
 const data = flowStore.setSimpleFlowUIDataByType([], adjustIndex(oldIndex), true)
 flowStore.setSimpleFlowUIDataByType(data, adjustIndex(newIndex), false)

 moveAtomData(adjustIndex(oldIndex), adjustIndex(newIndex), element.id)
 resetRenderList() // 재삽입위치
 }
}

function handleBeforeAdd(e: { element: any, newIndex: number }) {
 if (e.element?.key === 'smart-component') {
 useRoutePush({
 name: SMARTCOMPONENT,
 query: {
 projectId: processStore.project.id,
 projectName: processStore.project.name,
 newIndex: Math.min(e.newIndex, flowStore.simpleFlowUIData.length),
 },
 })
 return false
 }
 else {
 return true
 }
}

async function triggerAdd(key: string, preIndex?: number) {
 addAtomData(key, preIndex)
}

watch(() => flowStore.simpleFlowUIData.length, (newLength) => {
 if (newLength > MAX_ATOM_NUM) {
 message.warning(`프로세스 단계 수가 표시 제한(${MAX_ATOM_NUM}개)을 초과했습니다. 일부 단계는 저장되지 않습니다.`)
 }
})

onBeforeUnmount(() => {
 clearDraggable(draggableRef)
 flowStore.reset()
})
</script>

<template>
 <div
 id="listwrapper"
 ref="flowContainer"
 class="select-none before:bg-[#000000]/[.08] before:dark:bg-[#FFFFFF]/[.08] right-tab-close-area"
 >
 <DraggableVirtualScroller
 ref="draggableRef"
 v-slot="{ item, index }"
 :items="renderList"
 :min-item-size="40"
 :class="[colorTheme]"
 :before-add="handleBeforeAdd"
 item-key="id"
 filter=".forbid"
 group="postTree"
 @start="draggableAddStyle"
 @move="draggableAddStyle"
 @change="handleDragChange"
 >
 <Item :key="item.id" :item="item" :index="index" @select="triggerAdd" />
 </DraggableVirtualScroller>
 <ContextMenu />
 </div>
</template>

<style lang="scss">
@import url('@/assets/css/list.scss');
</style>
