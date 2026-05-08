<script setup lang="ts">
import { Divider } from 'ant-design-vue'
import { onMounted, ref } from 'vue'

import { isBrowser, windowManager } from '@/platform'

import { useCloseApp } from './HeaderControl/useCloseApp'

// 지정props
const props = defineProps({
  minimize: {
    type: Boolean,
    default: true,
  },
  maximize: {
    type: Boolean,
    default: true,
  },
  close: {
    type: Boolean,
    default: true,
  },
  title: {
    type: String,
    default: '',
  },
  closeFn: {
    type: Function,
    default: null,
  },
  dbtogglePrevent: {
    type: Boolean,
    default: false,
  },
})

const { closeApp } = useCloseApp()
const isMaximized = ref(true)

function dbClickFn(e: MouseEvent) {
  if (props.dbtogglePrevent) {
    e.preventDefault()
    e.stopPropagation()
  }
}

// 제어창소, 대, 닫기
function handleMinMaxClose(type: string) {
  if (isBrowser)
    return

  switch (type) {
    case 'minimize':
      windowManager.minimizeWindow()
      break
    case 'maximize':
      windowManager.maximizeWindow().then((isMax) => {
        isMaximized.value = isMax
      })
      break
    case 'close':
      handleClose()
      break
    default:
      break
  }
}
/**
 * 닫기창전, 실행의
 */
function handleClose() {
  if (props.closeFn) {
    props.closeFn() // 지정닫기 데이터
  }
  else {
    closeApp()
  }
}
onMounted(() => {
  windowManager.onWindowResize(() => {
    windowManager.isMaximized().then((isMax) => {
      isMaximized.value = isMax
    })
  })
})
</script>

<template>
  <div data-tauri-drag-region class="app_control w-full drag shrink-0">
    <div
      data-tauri-drag-region
      class="app_control_text flex items-center gap-2 drag whitespace-nowrap"
      @dblclick="dbClickFn"
    >
      <img
        v-if="!title"
        data-tauri-drag-region
        class="w-6 rounded-md"
        src="/icons/icon.png"
        @dblclick="dbClickFn"
      >
      <span class="text-base leading-5 font-semibold">
        {{ title || $t("app") }}
      </span>
    </div>
    <slot name="headMenu" />
    <div
      data-tauri-drag-region
      class="drag whitespace-nowrap flex-1 header-center"
      @dblclick="dbClickFn"
    >
      <slot name="headProject" />
    </div>
    <div
      data-tauri-drag-region
      class="flex items-center no-drag whitespace-nowrap h-full"
      @dblclick="dbClickFn"
    >
      <slot name="headControl" />

      <template v-if="$slots.headControl">
        <Divider type="vertical" />
      </template>

      <!-- 사용props제어 -->
      <span
        v-if="props.minimize"
        class="app_control__item"
        @click="handleMinMaxClose('minimize')"
      >
        <rpa-icon name="remove" />
      </span>
      <span
        v-if="props.maximize"
        class="app_control__item"
        @click="handleMinMaxClose('maximize')"
      >
        <rpa-icon :name="isMaximized ? 'middle' : 'maxwin'" />
      </span>
      <span
        v-if="props.close"
        class="app_control__item"
        @click="handleMinMaxClose('close')"
      >
        <rpa-icon name="close" />
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
  user-select: none;
  transition: all ease 0.2s;
  background: rgba(255, 255, 255, 0.82);
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(18px);

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
    background-color: $color-fill-secondary;
  }
  &:last-child:hover {
    background-color: #dc2626;
    color: #fff;
  }
}
</style>
