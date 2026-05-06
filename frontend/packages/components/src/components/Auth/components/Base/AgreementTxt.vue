<script setup lang="ts">
import { Button } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { computed } from 'vue'

declare global {
  interface Window {
    UtilsManager?: {
      openInBrowser: (url: string) => void
    }
  }
}

interface Props {
  type?: 'show' | 'check'
}

const { type = 'check' } = defineProps<Props>()
const { t } = useTranslation()

const text = computed(() => {
  if (type === 'show') {
    return t('components.auth.agreeToJoin')
  }
  return t('components.auth.checkToAgree')
})

function openLink(linkType: 'service' | 'privacy') {
  const urls: Record<string, string> = {
    service: 'https://www.shoprpa.com/resource/server.html',
    privacy: 'https://www.shoprpa.com/resource/licence.html',
  }
  if (urls[linkType]) {
    if (window.UtilsManager) {
      window.UtilsManager.openInBrowser(urls[linkType])
      return
    }
    window.open(urls[linkType], '_blank')
  }
}
</script>

<template>
  <div class="agreement-text">
    <span>{{ text }}</span>
    <Button class="agreement-link" type="link" @click="openLink('service')">
      {{ t('components.auth.serviceAgreement') }}
    </Button>
    <span>{{ t('components.auth.and') }}</span>
    <Button class="agreement-link" type="link" @click="openLink('privacy')">
      {{ t('components.auth.privacyPolicy') }}
    </Button>
  </div>
</template>

<style lang="scss" scoped>
.agreement-text {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px 4px;
  text-align: left;
  color: rgba(15, 23, 42, 0.76);
  font-size: 13px;
  line-height: 20px;
}

.agreement-link {
  height: auto;
  padding: 0;
  font-size: 13px;
  line-height: 20px;
}
</style>
