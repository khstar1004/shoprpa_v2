<script lang="ts" setup>
import { Empty, message } from 'ant-design-vue'
import { computed, inject, ref, watch } from 'vue'
import type { Ref } from 'vue'

import ElementUseFlowList from '@/components/ElementUseFlowList/Index.vue'
import { useCvStore } from '@/stores/useCvStore.ts'
import { useProcessStore } from '@/stores/useProcessStore'
import CvTree from '@/views/Arrange/components/cvPick/CvTree.vue'
import { PICK_TYPE_CV } from '@/views/Arrange/config/atom'
import { quoteManage } from '@/views/Arrange/hook/useQuoteManage'

defineProps({
 operates: {
 type: Object,
 },
})

const collapsed = inject<Ref<boolean>>('collapsed')
const searchText = inject<Ref<string>>('searchText')
const moduleType = inject<Ref<string>>('moduleType')
const refresh = inject<Ref<boolean>>('refresh')
const unUseNum = inject<Ref<number>>('unUseNum')
const activeTab = inject<Ref<string>>('activeTab')
const height = inject<Ref<number>>('logTableHeight', ref(180))

const cvStore = useCvStore()
const processStore = useProcessStore()

// 기본값표시의이미지데이터
const cvTreeData = computed(() => {
 if (!searchText.value)
 return cvStore.cvTreeData
 return cvStore.cvTreeData.map((i) => {
 return {
 ...i,
 elements: i.elements.filter(i => i.name.toLowerCase().includes(searchText.value.toLowerCase())),
 }
 }).filter(i => i.elements.length > 0)
})

// 사용이미지의프로세스데이터
const flowItems = ref([])
// 사용할 수 없습니다의이미지데이터
const unuseTreeData = ref([])

function refreshData(moduleType: string) {
 if (activeTab.value !== 'cvManagement')
 return
 switch (moduleType) {
 case 'unuse':
 cvStore.getUnUseTreeData(unuseTreeData, unUseNum, PICK_TYPE_CV)
 break
 case 'quoted':
 quoteManage(cvStore.quotedItem, list => flowItems.value = list, PICK_TYPE_CV)
 break
 }
}

watch(() => moduleType.value, (val) => {
 if (val !== 'quoted')
 useCvStore().setQuotedItem()
 refreshData(val)
})

watch(() => cvStore.cvTreeData, () => {
 if (moduleType.value === 'unuse')
 refreshData(moduleType.value)
}, { immediate: true })

watch(() => refresh.value, () => {
 if (activeTab.value !== 'cvManagement')
 return
 refreshData(moduleType.value)
 message.success('새로고침성공')
})

watch(() => cvStore.quotedItem?.id, (val) => {
 if (val)
 moduleType.value = 'quoted'
})
</script>

<template>
 <div class="cv-manager" :style="{ height: `${height}px` }">
 <!-- 이미지관리관리및검색 -->
 <template v-if="moduleType === 'default'">
 <CvTree v-if="cvTreeData.length > 0" :storage-id="processStore.project.id" :tree-data="cvTreeData" :default-collapse="!searchText" :collapsed="collapsed" />
 <a-empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" :description="searchText ? '검색 결과 없음' : $t('noData')" />
 </template>
 <!-- 조회사용할 수 없습니다요소 -->
 <template v-else-if="moduleType === 'unuse'">
 <CvTree v-if="unuseTreeData.length > 0" :tree-data="unuseTreeData" :default-collapse="false" :collapsed="collapsed" />
 <a-empty v-else description="사용되지 않은 이미지가 없습니다." />
 </template>
 <!-- 조회요소사용 -->
 <template v-else-if="moduleType === 'quoted'">
 <ElementUseFlowList v-if="flowItems.length > 0" :use-name="cvStore.quotedItem?.name" :use-flow-items="flowItems" :collapsed="collapsed" />
 <a-empty v-else description="참조 없음" />
 </template>
 </div>
</template>
