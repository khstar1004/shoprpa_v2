<script setup lang="ts">
import { useMediaQuery } from '@vueuse/core'
import { onBeforeUnmount } from 'vue'

import { useTheme } from '../../../../../theme'
import LoginBgDark from '../../../assets/imgs/shoprpa-hero-bg.png'

import PageHeader from './PageHeader.vue'
import StarCanvas from './StarCanvas.vue'

const { colorTheme, setColorMode } = useTheme()
const isMobile = useMediaQuery('(max-width: 768px)')

const loginBg = LoginBgDark

const color = colorTheme.value
setColorMode('light')
onBeforeUnmount(() => {
  setColorMode(color)
})
</script>

<template>
  <div
    class="auth-container w-full h-full min-h-screen bg-[#141414]"
    :class="[colorTheme]"
    :style="{ backgroundImage: `url(${loginBg})` }"
  >
    <slot name="header">
      <PageHeader v-if="!isMobile" />
    </slot>
    <div v-if="!isMobile" class="h-full relative z-[2] flex items-center justify-center">
      <slot name="container">
        <div class="auth-card-wrap">
          <div class="auth-brand-copy">
            <div class="auth-eyebrow">{{ $t('app') }}</div>
            <h1>{{ $t('auth.letAutomate') }}</h1>
            <p>{{ $t('auth.makeDecisions') }}</p>
          </div>
          <slot />
        </div>
      </slot>
    </div>
    <div v-else class="mobile-content w-full h-full">
      <slot />
    </div>
    <StarCanvas />
  </div>
</template>

<style lang="scss" scoped>
.auth-container {
  position: relative;
  overflow: hidden;
  background-position: center;
  background-size: cover;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(90deg, rgba(6, 10, 16, 0.92), rgba(6, 10, 16, 0.42));
    z-index: 1;
  }
}

.auth-card-wrap {
  width: min(1080px, calc(100vw - 80px));
  min-height: 560px;
  display: grid;
  grid-template-columns: minmax(360px, 1fr) 400px;
  align-items: center;
  gap: 64px;
}

.auth-brand-copy {
  color: #fff;

  .auth-eyebrow {
    margin-bottom: 18px;
    color: #2dd4bf;
    font-size: 13px;
    line-height: 18px;
    font-weight: 700;
    letter-spacing: 0;
  }

  h1 {
    margin: 0;
    color: #fff;
    font-size: 52px;
    line-height: 1.08;
    font-weight: 760;
    letter-spacing: 0;
  }

  p {
    max-width: 520px;
    margin-top: 18px;
    color: rgba(255, 255, 255, 0.72);
    font-size: 16px;
    line-height: 1.7;
  }
}
</style>
