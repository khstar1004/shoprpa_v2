<script lang="ts" setup>
import { onMounted } from 'vue'

import ConfigProvider from '@/components/ConfigProvider/index.vue'
import { windowManager } from '@/platform'

import LogWindow from './LogWindow.vue'

onMounted(() => {
  // 에서생성로그창시완료완료창상단, 예있음가능됨그창
  // 원인이에서로드완료후일창상단
  windowManager.setWindowAlwaysOnTop(true)
})
</script>

<template>
  <ConfigProvider>
    <LogWindow />
  </ConfigProvider>
</template>

<style lang="scss">
#app {
  width: 100vw;
  height: 100vh;
}
html,
body {
  margin: 0;
  padding: 0;
}
</style>
