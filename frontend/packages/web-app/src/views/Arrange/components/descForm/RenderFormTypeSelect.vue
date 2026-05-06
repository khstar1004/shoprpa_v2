<script setup lang="ts">
import type { Ref } from 'vue'
import { computed, inject } from 'vue'

import { useFlowStore } from '@/stores/useFlowStore'
import { isConditionalKeys } from '@/views/Arrange/components/atomForm/hooks/useBaseConfig'

interface Props {
 desc?: string
 itemData: RPA.AtomDisplayItem
 id?: string
 canEdit?: boolean
}

const props = withDefaults(defineProps<Props>(), {
 desc: '',
 id: '',
 canEdit: true,
})

const isShowFormItem = inject<Ref<boolean>>('showAtomFormItem')
const flowStore = useFlowStore()

// 를메뉴로 변환계획속성, 높음가능
const menuItems = computed(() => {
 return props.itemData.options?.map(i => ({ key: i.value, label: i.label })) ?? []
})

const isEmpty = computed(() => menuItems.value.length === 0)

function handleClick(val: string) {
 // 업데이트 itemData 의값(원인로 itemData 예방식대상사용)
 props.itemData.value = val
 // 통신경과 store 업데이트테이블단일값
 flowStore.setFormItemValue(props.itemData.key, props.itemData.value, props.id)
 // 만약예건파일키, 자르기교체테이블단일표시표시상태
 if (isConditionalKeys(props.itemData.key))
 isShowFormItem.value = !isShowFormItem.value
}
</script>

<template>
 <!-- 아래선택, 단일선택, 자르기교체, 체크박스 -->
 <a-dropdown :disabled="!props.canEdit || isEmpty">
 <span>{{ isEmpty ? '--' : props.desc }}</span>
 <template #overlay>
 <a-menu
 mode="vertical"
 class="form-type-select-menu"
 :items="menuItems"
 @click="(item) => handleClick(item.key as string)"
 />
 </template>
 </a-dropdown>
</template>

<style lang="scss" scoped>
// 매개메뉴높음정도로 32px, 5 공유 160px
.form-type-select-menu {
 min-width: 130px;
 max-height: 168px;
 overflow-y: auto;
 overflow-x: hidden;
}
</style>
