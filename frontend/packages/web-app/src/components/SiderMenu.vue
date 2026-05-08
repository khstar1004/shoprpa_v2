<script setup lang="ts">
import { useTheme } from '@rpa/components'
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import { COMMON_SIDER_WIDTH } from '@/constants'
import { useRoutePush } from '@/hooks/useCommonRoute'
import { useTopMenu } from '@/hooks/useTopMenu'

import MenuItem from './MenuItem.vue'

defineEmits<{
  (e: 'getCurrentMenuKey', key: string): void
}>()

const { colorTheme } = useTheme()
const siderMenu = useTopMenu() // 부서메뉴
const route = useRoute()

const menuData = computed(() => {
  const menus = siderMenu.find(item => item.name === route.meta?.group)?.children || []
  return menus.filter(mu => !mu.meta.notMenu) // 필터링출력메뉴
})

const currentMenuKey = computed(() => ([route.name as string]))

// 가져오기메뉴key
function menuClick(key: string) {
  useRoutePush({ name: key })
}
</script>

<template>
  <a-menu
    mode="inline"
    trigger-sub-menu-action="click"
    :selected-keys="currentMenuKey"
    :open-keys="menuData.map(item => item.name as string)"
    class="home-menu" :class="[colorTheme]"
    :style="{ width: `${COMMON_SIDER_WIDTH}px` }"
  >
    <MenuItem v-for="menu in menuData" :key="menu.name" :route="menu" @get-current-menu-key="menuClick" />
  </a-menu>
</template>

<style lang="scss" scoped>
.home-menu {
  width: 100%;
  height: 100%;
  background: transparent;
  padding: 22px 18px;

  :deep(.ant-menu-submenu) {
    // 메뉴
    .ant-menu-submenu-title {
      color: rgba(0, 0, 0, 0.8);
      height: auto;
      pointer-events: none;
      padding-left: 0 !important;
      margin-inline: 0;
      margin-block: 0;
      padding-inline: 0;
    }

    .ant-menu-inline {
      background: transparent;
    }
  }

  :deep(.ant-menu-item-selected) {
    color: #0f766e;
    font-weight: 650;
    background: rgba(14, 165, 168, 0.12);
  }

  :deep(.ant-menu-item) {
    border-radius: 10px;
    color: rgba(15, 23, 42, 0.72);
  }

  :deep(.ant-menu-item:not(.ant-menu-item-selected):hover) {
    color: #0f766e;
    background: rgba(15, 23, 42, 0.05);
  }
}

.dark.home-menu {
  :deep(.ant-menu-submenu) {
    // 메뉴
    .ant-menu-submenu-title {
      color: rgba(255, 255, 255, 0.65);
    }
  }
}
</style>
