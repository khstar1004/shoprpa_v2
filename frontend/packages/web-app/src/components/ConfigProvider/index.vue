<script lang="ts" setup>
import { ConfigProvider, NiceModal, useTheme } from '@rpa/components'
import { watchImmediate } from '@vueuse/core'
import { App } from 'ant-design-vue'
import dayjs from 'dayjs'
import { useTranslation } from 'i18next-vue'
import { VxeUI } from 'vxe-table'
import 'dayjs/locale/ko'
import 'dayjs/locale/zh-cn'

import { getPublicLanguage } from '@/plugins/i18next'

const NiceModalProvider = NiceModal.Provider

const { i18next } = useTranslation()
const { colorTheme } = useTheme()

watchImmediate(colorTheme, theme => VxeUI.setTheme(theme))

watchImmediate(
  () => i18next.language,
  (lang) => {
    dayjs.locale(getPublicLanguage(lang) === 'ko-KR' ? 'ko' : 'en')
  },
)
</script>

<template>
  <ConfigProvider :locale="i18next.language">
    <App class="w-full h-full">
      <NiceModalProvider>
        <slot />
      </NiceModalProvider>
    </App>
  </ConfigProvider>
</template>
