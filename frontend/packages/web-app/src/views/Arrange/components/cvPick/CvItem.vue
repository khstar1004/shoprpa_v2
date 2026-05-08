<script lang="ts" setup>
import { CopyOutlined, DeleteOutlined, RedoOutlined } from '@ant-design/icons-vue'
import { HintIcon } from '@rpa/components'
import { Image, message } from 'ant-design-vue'
import { computed, h } from 'vue'

import { getImageURL } from '@/api/http/env'
import ElementMenu from '@/components/ElementItemAction/elementMenu.vue'
import ElementItemAction from '@/components/ElementItemAction/Index.vue'
import { clipboardManager } from '@/platform'
import { useCvStore } from '@/stores/useCvStore.ts'
import type { Element } from '@/types/resource.d'
import type { ElementActionType } from '@/types/resource.d.ts'
import { useGroupManager } from '@/views/Arrange/components/bottomTools/components/hooks/useGroup.ts'
import { useCvManager } from '@/views/Arrange/components/cvPick/hooks/useCvManager.ts'

import { filterActionData } from '../../utils/elementsUtils'

const { itemData, groupId, elementActions, itemChosed } = defineProps({
  itemData: {
    type: Object as () => Element,
    default: () => {},
  },
  groupId: {
    type: String,
  },
  elementActions: {
    type: Array<ElementActionType>,
    default: () => ['edit', 'delete', 'move', 'copy-references', 'quoted'], // 목록표시변경다중 all delete
  },
  itemChosed: {
    type: String,
    default: '',
  },
})

const emits = defineEmits(['click', 'actionClick'])

const useGroup = useGroupManager()

const initConfigData = computed(() => {
  return [
    {
      key: 'edit',
      label: '편집',
      icon: h(HintIcon, { name: 'edit-outline', size: '12' }),
      type: 'tooltip',
    },
    {
      key: 'delete',
      label: '삭제',
      icon: h(DeleteOutlined, { style: 'color: #4e68f6;margin-left: 5px;' }),
      type: 'tooltip',
    },
    {
      key: 'more',
      label: '',
      type: 'dropdownMenus',
      icon: h(HintIcon, { name: 'ellipsis', size: '12' }),
      menus: [
        {
          key: 'quoted',
          label: '사용 위치 보기',
          icon: h(HintIcon, { name: 'bottom-pick-menu-search', size: '12' }),
        },
        {
          key: 'move',
          label: '그룹 이동',
          icon: h(HintIcon, { name: 'bottom-pick-menu-move', size: '12' }),
          children: useCvStore().cvTreeData.map((i) => {
            return { label: i.name, key: i.id }
          }),
        },
        {
          key: 'copy',
          label: '사본 만들기',
          icon: h(CopyOutlined, { style: 'color: #4e68f6;' }),
        },
        {
          key: 'repick',
          label: '다시 선택',
          icon: h(RedoOutlined),
        },
        {
          key: 'copy-references',
          label: '참조 코드 복사',
          icon: h(HintIcon, { name: 'bottom-pick-menu-copy', size: '12' }),
        },
        {
          key: 'delete',
          label: '이미지 삭제',
          icon: h(HintIcon, { name: 'bottom-pick-menu-del', size: '12' }),
        },
      ],
    },
  ]
})

function itemClick() {
  emits('click', itemData)
}

function actionClick(actions: Array<string>) {
  const key = actions[0]
  switch (key) {
    case 'edit':
      emits('actionClick')
      useCvManager().editCvItem(itemData, groupId)
      break
    case 'delete':
      emits('actionClick')
      useCvManager().delCvItem(itemData)
      break
    case 'move':
      useGroup.move2Group(itemData.id, actions[1], 'cv')
      break
    case 'quoted':
      useCvManager().setQuotedItem(itemData)
      break
    case 'copy-references':
      clipboardManager.writeClipboardText(`${itemData.name} = WinPick(h.element("${itemData.id}"))`)
      message.success('복사했습니다.')
      break
    default:
      break
  }
}
const configData = computed(() => filterActionData(initConfigData.value, elementActions))
</script>

<template>
  <a-dropdown :trigger="['contextmenu']">
    <template #overlay>
      <ElementMenu :selectd-id="groupId" :menus="configData[1].menus" @key-path="actionClick" />
    </template>
    <div class="cv-item relative" :class="{ 'cv-item-active': itemChosed === itemData.id }" :name="itemChosed ? itemData.id : ''" @click.stop="itemClick">
      <span class="cv-item-img inline-block"><Image wrapper-class-name="cv-img-mask" :title="$t('fullSizeImage')" :src="getImageURL(itemData.imageUrl)" @click.stop /></span>
      <div class="flex w-[64px]">
        <a-tooltip :title="itemData.name" class="flex-1">
          <div class="cv-item-title text-nowrap text-ellipsis overflow-hidden text-center">
            {{ itemData.name }}
          </div>
        </a-tooltip>
        <ElementItemAction :config-data="configData" :active-dropdown-menu-item-id="groupId" @click-menu="actionClick" />
      </div>
    </div>
  </a-dropdown>
</template>

<style lang="scss" scoped>
.cv-item {
  display: flex;
  flex-direction: column;
  justify-content: space-evenly;
  align-items: center;
  height: 100px;
  width: 80px;
  border-radius: 5px;
  &-active {
    background: #f1f3f8;
    border-color: #f1f3f8 !important;
  }
  &-title {
    margin-right: 2px;
    font-size: 12px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
  &-img {
    width: 64px;
    height: 56px;
    background: #fff;
    border: 1px solid #eee;
    border-radius: 3px;
    :deep(.ant-image) {
      display: inline-flex;
      height: 100%;
      align-items: center;
      justify-content: center;
      width: 100%;
    }
    :deep(.ant-image-mask) {
      border-radius: 3px;
    }
    :deep(.ant-image .ant-image-img) {
      width: auto;
      max-width: 100%;
      max-height: 100%;
    }
  }
  :deep(.pick-item-action) {
    display: none;
  }
  &:hover {
    opacity: 0.9;
    background: rgba(93, 89, 255, 0.35);
    :deep(.pick-item-action) {
      display: flex;
      gap: 4px;
    }
  }
}
:global(.ant-image-preview-root .ant-image-preview-mask) {
  z-index: 1060 !important;
}
</style>
