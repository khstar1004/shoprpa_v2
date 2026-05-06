<script setup lang="ts">
import { refDebounced } from '@vueuse/core'
import hotkeys from 'hotkeys-js'
import { escapeRegExp, isArray } from 'lodash-es'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useTemplateRef, watch } from 'vue'

import { SCOPE } from '@/constants/shortcuts'
import { useFlowStore } from '@/stores/useFlowStore'
import { toggleFold } from '@/views/Arrange/components/flow/hooks/useFlow'
import SearchWidget from '@/views/Arrange/components/search/SearchWidget.vue'
import { backContainNodeIdx } from '@/views/Arrange/utils/flowUtils'
import { atomScrollIntoView, decodeHtml } from '@/views/Arrange/utils/index'
import { renderAtomRemark } from '@/views/Arrange/utils/renderAtomRemark'
import { changeSelectAtoms } from '@/views/Arrange/utils/selectItemByClick'

const SEARCH_HOTKEY = 'Ctrl+F'
const ARROW_UP_KEY = 'up'
const ARROW_DOWN_KEY = 'down'

const showSearch = ref(false)
const activeIndex = ref(0)
const searchKeyword = ref('')
const debouncedSearchKeyword = refDebounced(searchKeyword, 300)
const searchWidget = useTemplateRef('searchWidget')
const flowStore = useFlowStore()

// 검색결과표시표시
const searchResults = computed(() => {
 if (!showSearch.value || !debouncedSearchKeyword.value)
 return []

 const searchRegex = new RegExp(escapeRegExp(debouncedSearchKeyword.value), 'i')
 const dataWithComments = flowStore.simpleFlowUIData.map((item, index) => {
 const comment = renderAtomRemark(item)
 const commentText = isArray(comment)
 ? comment.map(i => (i.variable ? decodeHtml(i.sr[2]) : i)).join('')
 : comment
 return { id: item.id, title: item.alias, commentText, item, index }
 })

 return dataWithComments.filter(
 it => searchRegex.test(it.title) || searchRegex.test(it.commentText),
 )
})

// 현재의검색결과
const activeSearchAtom = computed(() => {
 return searchResults.value[activeIndex.value]
})

// 열기패키지현재검색결과의그룹
function expandContainingGroups(atomIndex: number) {
 const groupKeys = Object.keys(flowStore.nodeContactMap)
 groupKeys.forEach((groupId) => {
 const groupStartIdx = flowStore.simpleFlowUIData.findIndex(node => node.id === groupId)
 const groupEndIdx = backContainNodeIdx(groupId)

 if (groupStartIdx > -1 && groupStartIdx <= atomIndex && groupEndIdx >= atomIndex) {
 toggleFold(flowStore.simpleFlowUIData[groupStartIdx])
 }
 })
}

// 관리검색결과자르기교체
function handleSearchResultChange(atomId: string | undefined) {
 if (!atomId) {
 activeIndex.value = 0
 return
 }

 const canvas = document.querySelector<HTMLElement>('.postTask-content-canvas')
 if (canvas) {
 canvas.scrollTop = 0
 }
 changeSelectAtoms(atomId, null, false)
 expandContainingGroups(activeSearchAtom.value.index)
 nextTick(() => atomScrollIntoView(atomId))
}

// 검색파일
function showSearchWidget() {
 showSearch.value = true
 // 지정위아래키빠름키
 hotkeys(ARROW_UP_KEY, SCOPE, handleArrowUp)
 hotkeys(ARROW_DOWN_KEY, SCOPE, handleArrowDown)
 nextTick(() => searchWidget.value?.focus())
}

function closeSearchWidget() {
 showSearch.value = false
 searchKeyword.value = ''
 activeIndex.value = 0
 // 가져오기메시지지정위아래키빠름키
 hotkeys.unbind(ARROW_UP_KEY, SCOPE)
 hotkeys.unbind(ARROW_DOWN_KEY, SCOPE)
}

function next() {
 if (searchResults.value.length === 0)
 return
 const total = searchResults.value.length
 activeIndex.value = (activeIndex.value + 1) % total
}

function previous() {
 if (searchResults.value.length === 0)
 return
 const total = searchResults.value.length
 activeIndex.value = activeIndex.value === 0 ? total - 1 : activeIndex.value - 1
}

// 위아래키관리함수데이터
function handleArrowUp() {
 console.log('handleArrowUp')
 previous()
}

function handleArrowDown() {
 next()
}

// 검색결과변수
watch(
 () => activeSearchAtom.value?.id,
 handleSearchResultChange,
)

// 빠름키지정
onMounted(() => {
 hotkeys(SEARCH_HOTKEY, SCOPE, showSearchWidget)
})

onBeforeUnmount(() => {
 hotkeys.unbind(SEARCH_HOTKEY, SCOPE)
 hotkeys.unbind(ARROW_UP_KEY, SCOPE)
 hotkeys.unbind(ARROW_DOWN_KEY, SCOPE)
})
</script>

<template>
 <div class="search">
 <Transition name="search-fade">
 <SearchWidget
 v-if="showSearch"
 ref="searchWidget"
 v-model:value="searchKeyword"
 class="search-widget"
 :active="activeIndex + 1"
 :total="searchResults.length"
 @next="next"
 @previous="previous"
 @close="closeSearchWidget"
 />
 </Transition>
 </div>
</template>

<style scoped lang="scss">
.search {
 width: 100%;
 position: relative;
 z-index: 1;

 .search-widget {
 position: absolute;
 right: 10px;
 top: 20px;
 }
}

// 완화완화출력동작
.search-fade-enter-active,
.search-fade-leave-active {
 transition:
 opacity 0.3s ease-in-out,
 transform 0.3s ease-in-out;
}

.search-fade-enter-from {
 opacity: 0;
 transform: translateY(-10px);
}

.search-fade-leave-to {
 opacity: 0;
 transform: translateY(-10px);
}
</style>
