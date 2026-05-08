<script lang="ts" setup>
import { computed, inject } from 'vue'

import AtomFormItem from '@/views/Arrange/components/atomForm/AtomFormItem.vue'

import type { FormItemConfig } from '../types/index.ts'
import { conditionalFn } from '../utils/index.ts'

const { selectedFormItem, updateSelectedFormItem } = inject('selectedFormItem') as { selectedFormItem: FormItemConfig, updateSelectedFormItem: any }

const filterSelectedFormItem = computed(() => {
  if (selectedFormItem.value?.conditionalFnKey) {
    updateSelectedFormItem(conditionalFn[selectedFormItem.value?.conditionalFnKey](selectedFormItem.value))
  }
  return selectedFormItem.value
})
</script>

<template>
  <div class="dialog-modal_attrs">
    <div class="header">
      속성
    </div>
    <div v-if="filterSelectedFormItem">
      <AtomFormItem v-for="t in filterSelectedFormItem?.configKeys" :key="t" :atom-form-item="filterSelectedFormItem[t]" />
    </div>
    <div v-else class="text-[#000000]/[.65] dark:text-[#FFFFFF]/[.65]">
      선택한 의 설정이 여기에 표시됩니다.
    </div>
  </div>
</template>

<style lang="scss">
</style>
