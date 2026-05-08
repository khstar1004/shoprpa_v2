<script lang="ts" setup>
import { computed } from 'vue'

import CvUploadBtn from '@/views/Arrange/components/cvPick/CvUploadBtn.vue'

type PickType = 'cv' | 'element'

const { pickType, disabled, groupId } = defineProps({
  pickType: {
    type: String as () => PickType,
    required: true,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  groupId: {
    type: String,
    required: false,
  },
})

const emits = defineEmits(['contextmenu'])

// 분그룹오른쪽 버튼메뉴
const contextmenus = [
  { label: '이름 변경', key: 'rename' },
  { label: '이미지 추가', key: 'cvPick' },
  { label: '이미지 업로드', key: 'cvUpload' },
  { label: '새 요소 선택', key: 'elementPick' },
  { label: '그룹 삭제', key: 'delete' },
]

const menus = computed(() => {
  return contextmenus.filter((item) => {
    if (pickType === 'cv') {
      return !item.key.includes('element')
    }

    return !item.key.includes('cv')
  })
})

function handleMenuClick(item) {
  emits('contextmenu', item.key)
}
</script>

<template>
  <a-dropdown
    :trigger="['contextmenu']"
    :disabled="disabled"
    overlay-class-name="pick-group-contextmenu"
  >
    <div>
      <slot name="content" />
    </div>
    <template #overlay>
      <a-menu @click="handleMenuClick">
        <template v-for="item in menus" :key="item.key">
          <!-- 관리업로드버튼 -->
          <a-menu-item v-if="item.key === 'cvUpload'">
            <CvUploadBtn type="text" :group-id="groupId" class="cv-upload-btn" />
          </a-menu-item>
          <!-- 일반메뉴 -->
          <a-menu-item v-else :key="item.key">
            <span>{{ item.label }}</span>
          </a-menu-item>
        </template>
      </a-menu>
    </template>
  </a-dropdown>
</template>
