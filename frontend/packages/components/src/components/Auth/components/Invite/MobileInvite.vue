<script setup lang="ts">
import { Button } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'

import appIcon from '../../assets/imgs/shoprpa-app-icon.png'

const emit = defineEmits<{
  accept: []
}>()

const { t } = useTranslation()

function handleClick() {
  emit('accept')
}
</script>

<template>
  <div
    class="w-full flex flex-col justify-center items-center gap-8"
  >
    <img :src="appIcon" class="w-[180px] h-[180px] rounded-[36px] shadow-2xl" alt="shoprpa">
    <Button type="primary" size="large" class="!h-[48px] w-[calc(100%-40px)] text-[16px] text-[#000000D9] bg-white fixed bottom-[65px] left-1/2 -translate-x-1/2 z-[99]" @click="handleClick">
      {{ t('components.auth.acceptInvite') }}
    </Button>
  </div>
</template>
