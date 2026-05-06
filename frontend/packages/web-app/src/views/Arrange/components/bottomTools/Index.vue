<script setup lang="ts">
import { reactiveComputed } from '@vueuse/core'
import { sum } from 'lodash-es'
import { computed, provide, ref, shallowRef, watch } from 'vue'
import { useRoute } from 'vue-router'

import { BOTTOM_BOOTLS_HEIGHT_SIZE_MIN } from '@/constants'
import { SMARTCOMPONENT } from '@/constants/menu.ts'
import { useProcessStore } from '@/stores/useProcessStore'
import { useRunningStore } from '@/stores/useRunningStore'

import { useProvideConfigParameter } from './components/ConfigParameter/useConfigParameter.ts'
import { useCVManager } from './components/CvManager/useCVManager.ts'
import { useProvideDataSheetStore } from './components/DataSheet/useDataSheet'
import { useDebugLog } from './components/DebugLog/useDebugLog.ts'
import { useElementManager } from './components/ElementManager/useElementManager.ts'
import { useLog } from './components/Log/useLog.ts'
import { useSubProcessUse } from './components/SubProcessSearch/useSubProcessUse'
import type { TabConfig } from './types'

const props = defineProps<{ height: number }>()
const collapsed = defineModel('collapsed', { type: Boolean, default: false })

// 생성그리고 configParameter 
const { config: configParamsTabConfig } = useProvideConfigParameter()
const { dataSheetConfig } = useProvideDataSheetStore()

const route = useRoute()
const processStore = useProcessStore()

const initTabs = reactiveComputed(() => [
 useLog(),
 useElementManager(),
 useCVManager(),
 configParamsTabConfig,
 dataSheetConfig,
])

const tabs = shallowRef<TabConfig[]>(initTabs)
const activeKey = ref(tabs.value[0].key)
const searchText = ref('')
const moduleType = ref('default')

// 내용의대높음정도
const contentHeight = computed(() => {
 return Math.max(props.height, BOTTOM_BOOTLS_HEIGHT_SIZE_MIN) - 46 - 8 //  tab 높음정도및 margin-bottom
})

// provide 요소관리관리/이미지관리관리
provide('collapsed', ref(false)) // 열기 
provide('searchText', searchText) // 검색텍스트
provide('moduleType', moduleType) // 표시모듈유형 moduleType: 'default' | 'unuse'-사용할 수 없습니다 | 'quoted'-됨사용
provide('refresh', ref(true)) // 새로고침
provide('unUseNum', ref(0)) // 사용할 수 없습니다요소수
provide('activeTab', activeKey) // 사용할 수 없습니다요소수
provide('logTableHeight', contentHeight)

const activeTab = computed<TabConfig>(() => tabs.value.find(tab => tab.key === activeKey.value))
const searchSubProcessTotal = computed(() => {
 return sum(processStore.searchSubProcessResult.map(pItem => pItem.nodes?.length || 0))
})

function expand(bool: boolean) {
 collapsed.value = bool
 moduleType.value = 'default' // 자르기교체 tab 시재모듈유형
}

watch(() => useRunningStore().running, (val) => {
 if (route.name === SMARTCOMPONENT) {
 return
 }
 if (['run', 'debug'].includes(val)) {
 expand(false)
 }
 if (val === 'run') {
 activeKey.value = 'logs'
 }
 if (val === 'debug') {
 tabs.value = [...tabs.value, useDebugLog()]
 activeKey.value = 'debugLog'
 }
 else {
 tabs.value = tabs.value.filter(tab => tab.key !== 'debugLog')
 activeKey.value = tabs.value[0].key
 }
})

watch(() => processStore.searchSubProcessId, (val) => {
 if (val) {
 tabs.value = [useSubProcessUse()]
 activeKey.value = tabs.value[0].key
 expand(false)
 }
 else {
 tabs.value = initTabs
 activeKey.value = tabs.value[0].key
 expand(true)
 }
})

// 자르기교체프로세스시, 재하단부서도구의탭
watch(() => processStore.activeProcessId, () => {
 const initTabKeys = initTabs.map(item => item.key)
 const otherTabs = tabs.value.filter(tab => !initTabKeys.includes(tab.key))
 tabs.value = [...initTabs, ...otherTabs]
})
</script>

<template>
 <section class="text-xs bottom-tools bg-[#FFFFFF] dark:bg-[#FFFFFF]/[.12] rounded-lg">
 <a-tabs v-model:active-key="activeKey" class="right-tab-close-area" size="small" @tab-click="() => expand(false)">
 <template #rightExtra>
 <div class="flex items-center">
 <template v-if="!collapsed">
 <component :is="activeTab.rightExtra" />
 </template>
 <rpa-hint-icon
 v-if="!activeTab.hideCollapsed"
 name="caret-down-small"
 :title="collapsed ? $t('common.expand') : $t('common.collapse')"
 class="ml-1"
 :class="[collapsed ? '-rotate-180' : 'rotate-0']"
 enable-hover-bg
 @click="() => expand(!collapsed)"
 />
 </div>
 </template>
 <a-tab-pane v-for="item in tabs" :key="item.key" class="z-0">
 <template #tab>
 <span class="flex items-center">
 <rpa-icon :name="item.icon" width="16px" height="16px" class="mr-1" />
 {{ $t(item.text) }}
 <span v-if="activeKey === 'subProcessSearch'" class="ml-1">{{ searchSubProcessTotal }}</span>
 </span>
 </template>
 <component :is="item.component" :height="contentHeight" />
 </a-tab-pane>
 </a-tabs>
 </section>
</template>

<style lang="scss" scoped>
.search-input {
 font-size: 12px;
 width: 230px;
 height: 22px;
 overflow: hidden;
}

.icon-close {
 font-size: 12px;
 color: #666;
 cursor: pointer;
 &:hover {
 color: #000;
 }
}

:deep(.search-input .ant-input) {
 height: 21px;
 font-size: 12px;
}

:deep(.search-input .ant-btn-sm) {
 height: 22px;
 font-size: 12px;
}

:deep(.search-input .anticon) {
 vertical-align: middle;
}

:deep(.ant-tabs .ant-tabs-extra-content) {
 height: 24px;
 margin-right: 16px;
}

:deep(.ant-tabs-small > .ant-tabs-nav .ant-tabs-tab) {
 margin-left: 16px;
 font-size: 12px;
}

:deep(.ant-tabs .ant-tabs-tab.ant-tabs-tab-active .ant-tabs-tab-btn) {
 font-weight: 600;
 color: inherit;
}

:deep(.ant-tabs > .ant-tabs-nav) {
 margin-bottom: 8px;
 height: 46px;
}

:deep(.ant-tabs-extra-content) {
 display: flex;
 align-items: center;
}

:deep(.ant-tabs .ant-tabs-tabpane) {
 padding: 0 8px;
}

:deep(.cv-pick-btn) {
 margin-right: 8px;
}

:deep(.vxe-table--render-wrapper) {
 background-color: transparent !important;
}

:deep(.vxe-table--header-wrapper) {
 background-color: transparent !important;
}

:deep(.vxe-table--body-wrapper) {
 background-color: transparent !important;
}
</style>
