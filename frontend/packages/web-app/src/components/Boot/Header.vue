<script setup lang="ts">
import { isBrowser, windowManager } from '@/platform'

// 제어창소, 대, 닫기
function handleMinMaxClose(type: string) {
  if (isBrowser)
    return

  switch (type) {
    case 'minimize':
      windowManager.minimizeWindow()
      break
    case 'close':
      windowManager.closeWindow()
      break
    default:
      break
  }
}
</script>

<template>
  <div data-tauri-drag-region class="app_control w-full drag fixed top-0 left-0">
    <div
      data-tauri-drag-region
      class="app_control_text flex items-center gap-2 drag whitespace-nowrap"
    >
      <img data-tauri-drag-region class="w-6 rounded-md" src="/icons/icon.png">
      <span class="text-base text-[#ffffff] leading-5 font-semibold tracking-[0]">
        {{ $t('app') }}
      </span>
    </div>
    <div
      data-tauri-drag-region
      class="flex items-center no-drag whitespace-nowrap h-full"
    >
      <!-- 사용props제어 -->
      <span
        class="app_control__item"
        @click="handleMinMaxClose('minimize')"
      >
        <rpa-icon name="remove" color="#ffffff" />
      </span>
      <span
        class="app_control__item"
        @click="handleMinMaxClose('close')"
      >
        <rpa-icon name="close" color="#ffffff" />
      </span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.app_control {
  height: var(--headerHeight);
  z-index: var(--headerZindex);
  display: flex;
  align-items: center;
  justify-content: space-between;
  user-select: none;
  transition: all ease 0.2s;
  &_text {
    padding-left: 16px;
    user-select: none;
    min-width: 160px;
  }
}

.app_control__item {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  width: 40px;
  &:hover {
    background-color: #ffffff1f;
  }
  &:last-child:hover {
    background-color: #dc2626;
  }
}
</style>
