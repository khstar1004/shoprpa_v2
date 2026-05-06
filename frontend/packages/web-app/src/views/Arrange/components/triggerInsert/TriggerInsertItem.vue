<!-- @format -->
<script setup lang="ts">
import { inject, ref, watch } from 'vue'

import TriggerInput from '@/views/Arrange/components/triggerInsert/TriggerInput.vue'

const { id, index, indent } = defineProps({
 id: {
 type: String,
 },
 index: {
 type: Number,
 },
 indent: {
 type: Number,
 },
})

const emits = defineEmits(['select'])

const triggerId = inject('triggerId', ref(''))
const inputPos = inject('inputPos', ref(''))
const triggerIds = inject('triggerIds', ref([]))
const reset = inject('reset', () => {})

const addPos = ref('') // 추가버튼위치 top | bottom
const addPosStyle = ref({}) // 추가버튼방식

const preId = ref('') // 전점의id
const nextId = ref('') // 후점의id
const content = ref() // 점내용

watch(() => triggerIds.value, (val) => {
 preId.value = val[index - 1] || ''
 nextId.value = val[index + 1] || ''
}, {
 immediate: true,
})

function canInsert(pos: string) {
 // 현재pos로top, 이면전일개점저장에서입력란, pos로bottom, 이면아니오허용회삽입
 if (pos === 'top' && triggerId.value && preId.value && triggerId.value === preId.value && inputPos.value === 'bottom') {
 return false
 }
 // 현재pos로bottom, 이면후일개점의저장에서입력란, pos로top, 이면아니오허용회삽입
 if (pos === 'bottom' && triggerId.value && nextId.value && triggerId.value === nextId.value && inputPos.value === 'top') {
 return false
 }
 // 현재의점표시완료입력란, 입력란의위치및계획위치, 이면입력란의위치아니오허용회삽입
 if (triggerId.value === id && pos === inputPos.value) {
 return false
 }
 return true
}

function mouseMove(e: MouseEvent) {
 if (content.value) {
 const domRect = content.value.getBoundingClientRect()
 const pos = e.clientY > domRect.top + domRect.height / 2 ? 'bottom' : 'top'
 if (canInsert(pos)) {
 addPosStyle.value = {
 top: pos === 'top' ? `${domRect.top - 13}px` : `${domRect.bottom - 13}px`,
 }
 addPos.value = pos
 }
 else {
 addPosStyle.value = {}
 addPos.value = ''
 }
 }
}

function mouseLeave() {
 addPosStyle.value = {}
 addPos.value = ''
}

// 표시입력란
function showTriggerInput() {
 triggerId.value = id
 inputPos.value = addPos.value
 addPosStyle.value = {}
 addPos.value = ''
}

function clickAtom(key: string) {
 const addToIndex = inputPos.value === 'top' ? index : index + 1
 emits('select', key, addToIndex)
 reset()
}
</script>

<template>
 <div
 ref="content" class="trigger-insert-item relative"
 :data-id="id"
 :data-indent="indent"
 @mousemove="mouseMove"
 @mouseleave="mouseLeave"
 >
 <div ref="addAtomBtn" class="addAtom" :class="addPos" :style="addPosStyle">
 <span class="addAtom-btn" @click="showTriggerInput">+</span>
 <span class="addAtom-line" />
 </div>
 <div v-if="triggerId === id && inputPos === 'top'" class="trigger-insert-item-input">
 <TriggerInput @select="clickAtom" />
 </div>
 <slot />
 <div v-if="triggerId === id && inputPos === 'bottom'" class="trigger-insert-item-input">
 <TriggerInput @select="clickAtom" />
 </div>
 </div>
</template>

<style scoped lang="scss">
.trigger-insert-item {
 &-input {
 padding-left: 82px;
 }
}
.addAtom {
 position: fixed;
 left: 325px;
 z-index: 1000;
 width: calc(100% - 70px);
 height: 20px;
 text-align: left;
 display: none;
 &.bottom,
 &.top {
 display: block;
 }

 &-btn {
 display: inline-block;
 width: 24px;
 height: 24px;
 line-height: 24px;
 border-radius: 20px;
 color: #fff;
 font-size: 14px;
 cursor: pointer;
 text-align: center;
 background: $color-primary;

 &:hover {
 & + .addAtom-line {
 display: block;
 }
 }
 }

 &-line {
 width: calc(100% - 5px);
 margin-left: 0px;
 height: 1px;
 border-bottom: 2px dashed rgba(72, 106, 255, 0.41);
 display: none;
 margin-top: -12px;
 }
}
</style>
