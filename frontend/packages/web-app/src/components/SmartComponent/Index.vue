<script lang="ts" setup>
import { useToggle } from '@vueuse/core'
import { computed, watch } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'

import { useSmartCompPickStore } from '@/stores/useSmartCompPickStore'
import { useElementManager } from '@/views/Arrange/components/bottomTools/components/ElementManager/useElementManager'

import { provideChatContext, providePackageCheckContext, provideSmartCompContext, useChatContext, usePackageCheckContext, useSmartCompContext } from './hooks'
import NewChat from './views/Chat.vue'
import Header from './views/Header.vue'
import LogView from './views/LogView.vue'
import Preview from './views/Preview.vue'
import VersionManage from './views/VersionManage.vue'

const [versionManageVisible, toggleVersionManageVisible] = useToggle()

// 생성가능컴포넌트위아래문서
const smartCompContext = useSmartCompContext()

const chatContext = useChatContext(smartCompContext)

// 생성조회위아래문서
const packageCheckContext = usePackageCheckContext()

provideSmartCompContext(smartCompContext)
provideChatContext(chatContext)
providePackageCheckContext(packageCheckContext)

const hasEditingComp = computed(() => !!smartCompContext.editingSmartComp.value)

// 현재컴포넌트버전패키지변수
watch(() => smartCompContext.editingSmartComp.value?.packages || [], async (newVal) => {
  if (newVal) {
    await packageCheckContext.setPackages(newVal)
  }
}, { immediate: true, deep: true })

const smartCompPickStore = useSmartCompPickStore()

// 경로열기, 예결과정상에서선택이면결과선택
onBeforeRouteLeave(() => {
  if (smartCompPickStore.isPicking) {
    smartCompPickStore.resetPick()
  }
})

useElementManager()
</script>

<template>
  <div class="h-full flex flex-col gap-[2px] overflow-y-hidden">
    <Header @open-version-manage="toggleVersionManageVisible" />

    <section class="flex-1 flex gap-[2px] overflow-y-hidden">
      <NewChat :style="{ width: hasEditingComp ? '480px' : '100%' }" />
      <div class="flex-1 flex flex-col gap-[2px] overflow-x-hidden">
        <Preview v-if="hasEditingComp" />
        <LogView />
      </div>
      <VersionManage v-if="versionManageVisible" @close-version-manage="toggleVersionManageVisible" />
    </section>
  </div>
</template>