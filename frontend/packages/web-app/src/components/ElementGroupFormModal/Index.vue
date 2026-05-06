<script setup lang="ts">
import { NiceModal } from '@rpa/components'
import type { FormInstance } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { reactive, ref } from 'vue'

import type { FormRules } from '@/types/common'
import type { Element } from '@/types/resource.d'

interface FormState {
  groupName: string
}

const props = defineProps<{ groupItem?: Element }>()
const emit = defineEmits(['confirm'])

const modal = NiceModal.useModal()
const { t } = useTranslation()
const formRef = ref<FormInstance>()
const formState = reactive<FormState>({
  groupName: props.groupItem?.name || '',
})

const rules: FormRules = {
  groupName: [
    { required: true, message: '그룹 이름을 입력하세요.', trigger: 'change' },
    {
      max: 20,
      message: t('donotExceedCharacters', { num: 20 }),
      trigger: 'change',
    },
  ],
}

async function handleOk() {
  formRef.value.validate().then((valid) => {
    if (valid) {
      emit('confirm', formState.groupName)
    }
  })
}
</script>

<template>
  <a-modal
    v-bind="NiceModal.antdModal(modal)"
    :title="groupItem ? '그룹 이름 변경' : '새 그룹 만들기'"
    :width="400"
    @ok="handleOk"
  >
    <a-form
      ref="formRef"
      :model="formState"
      :rules="rules"
      autocomplete="off"
      layout="vertical"
    >
      <a-form-item name="groupName" :label="t('name')">
        <a-input
          v-model:value="formState.groupName"
          placeholder="그룹 이름을 입력하세요."
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
