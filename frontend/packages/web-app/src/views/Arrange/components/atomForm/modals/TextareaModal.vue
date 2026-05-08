<script setup lang='ts'>
import { NiceModal } from '@rpa/components'
import { ref } from 'vue'

import AtomFormItem from '../AtomFormItem.vue'

const props = defineProps<{ itemDataVal: Array<RPA.AtomFormItemResult> }>()
const emit = defineEmits(['ok'])

const modal = NiceModal.useModal()
const modalData = ref({
  types: 'Str',
  formType: {
    type: 'INPUT_VARIABLE',
  },
  title: '',
  required: false,
  key: 'textarea_modal',
  name: 'textarea_modal',
  default: '',
  value: props.itemDataVal,
})

function handleOkConfirm() {
  emit('ok', modalData.value.value)
  modal.hide()
}
</script>

<template>
  <a-modal
    v-bind="NiceModal.antdModal(modal)"
    :centered="true"
    :width="800"
    :title="$t('atomForm.textContent')"
    @ok="handleOkConfirm"
  >
    <AtomFormItem :atom-form-item="modalData" />
  </a-modal>
</template>

<style lang='scss' scoped>
:deep(.editor) {
  height: 300px;
  white-space: pre-wrap !important; // 보관교체행기호및빈격식, 자체동작교체행
}
</style>
