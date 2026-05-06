<script lang="ts" setup>
import { NiceModal } from '@rpa/components'
import { useTranslation } from 'i18next-vue'
import { get } from 'lodash-es'
import { provide, ref } from 'vue'

import DialogContent from './components/dialogContent.vue'
import DialogFooter from './components/dialogFooter.vue'
import type { FormItemConfig } from './types/index.ts'

interface IDialogData {
 mode: string
 title: string
 buttonType: string
 formList: Array<FormItemConfig>
 table_required: boolean
}

const props = withDefaults(defineProps<{ title?: string, option?: string }>(), {
 title: '',
})

const emit = defineEmits(['ok'])

const { t } = useTranslation()

function DEFAULT_DIALOG_DATA(): IDialogData {
 return {
 mode: 'window',
 title: t('components.customComponent'),
 buttonType: 'confirm_cancel',
 formList: [],
 table_required: false,
 }
}

function parseDialogData(dataText: string): IDialogData {
 try {
 const data = JSON.parse(dataText)
 // 필요내용의데이터결과 { value: dialogData.value, rpa: 'special' }
 return get(data, 'rpa') === 'special' ? data.value : data
 }
 catch {
 return DEFAULT_DIALOG_DATA()
 }
}

const modal = NiceModal.useModal()
// 사용자 지정대화상자결과
const dialogData = ref(props.option ? parseDialogData(props.option) : DEFAULT_DIALOG_DATA())
dialogData.value.title = props.title || t('components.customComponent')

// 현재선택중필요매칭의데이터
const selectedFormItem = ref(dialogData.value?.formList[0] || null as FormItemConfig)

provide('dialogData', {
 dialogData,
 updateDialogDataFormList: (type: 'splice' | 'push', ...params: any) => {
 // TODO: 안필요분리성공개함수데이터
 // @ts-expect-error Dynamic method call on array with string index
 dialogData.value?.formList[type](...params)
 },
})

provide('selectedFormItem', {
 selectedFormItem,
 updateSelectedFormItem: (data: FormItemConfig) => {
 selectedFormItem.value = data
 },
})

function saveData(data) {
 emit('ok', data)
 modal.hide()
}
</script>

<template>
 <a-modal
 v-bind="NiceModal.antdModal(modal)"
 centered
 :width="800"
 :title="t('components.customComponent')"
 class="dialog-modal"
 :z-index="19"
 :footer="null"
 >
 <DialogContent />
 <DialogFooter @save-data="saveData" @close="modal.hide" />
 </a-modal>
</template>

<style lang="scss">
@import './index.scss';
</style>
