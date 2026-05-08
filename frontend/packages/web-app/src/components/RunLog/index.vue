<script lang="tsx" setup>
import { useTheme } from '@rpa/components'
import { message, Tooltip } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { debounce, isNil, last, noop } from 'lodash-es'
import type { Ref } from 'vue'
import { computed, inject, onMounted, onUnmounted, ref, shallowRef, watch } from 'vue'
import type {
  VxeGridEventProps,
  VxeGridInstance,
  VxeGridProps,
} from 'vxe-table'

import VxeGrid from '@/plugins/VxeTable'

import { clipboardManager, windowManager } from '@/platform'
import { useRunlogStore } from '@/stores/useRunlogStore'
import { useRunningStore } from '@/stores/useRunningStore'

const props = defineProps<{ size?: 'default' | 'small' }>()
const emit = defineEmits(['rowClick'])
const { colorTheme } = useTheme()
const height = inject<Ref<number>>('logTableHeight', ref(320)) // 있음비고입력, 값로320
const runlogStore = useRunlogStore()
const { t, i18next } = useTranslation()
const xGrid = ref<VxeGridInstance<RPA.LogItem>>()
const isMinimized = ref(false) // 여부소
const dataList = shallowRef<RPA.LogItem[]>([])

let unWindowResizeListen = noop

onMounted(async () => {
  const resizeDebounce = debounce(async () => {
    isMinimized.value = await windowManager.isMinimized()
  }, 200)
  unWindowResizeListen = await windowManager.onWindowResize(resizeDebounce)
})

onUnmounted(() => unWindowResizeListen())

const columns = computed<VxeGridProps<RPA.LogItem>['columns']>(() => {
  const isSmall = props.size === 'small'
  const isEnglish = i18next.language === 'en-US'

  return [
    {
      title: t('errorType'),
      field: 'logLevelText',
      width: isSmall ? 50 : 80,
    },
    {
      title: t('timestamp'),
      field: 'timestamp',
      width: isSmall ? 130 : 160,
    },
    {
      title: t('processName'),
      field: 'processName',
      width: isSmall ? 60 : isEnglish ? 100 : 80,
      align: 'right',
      formatter: ({ cellValue }) => (isNil(cellValue) ? '--' : cellValue),
    },
    {
      title: t('rows'),
      field: 'lineNum',
      width: isSmall ? 50 : 80,
      align: 'right',
      formatter: ({ cellValue }) => (isNil(cellValue) ? '--' : cellValue),
    },
    {
      title: t('content'),
      field: 'content',
      showOverflow: 'ellipsis',
      slots: { default: 'content_default' },
    },
  ]
})

const onMenuClick: VxeGridEventProps<RPA.LogItem>['onMenuClick'] = ({
  menu,
  row,
  column,
}) => {
  const $grid = xGrid.value

  if ($grid && menu.code === 'copy' && row && column) {
    const text = row[column.field]
    clipboardManager.writeClipboardText(text)
    message.info(t('contentCopied'))
  }
}

async function scrollTobottom() {
  const $grid = xGrid.value
  if (!$grid)
    return

  await $grid.clearScroll()

  $grid.scrollToRow(last(dataList.value), 'id')
}

function refreshDataList() {
  //  dataList 및 runlogStore.logList 여부대기
  // 1. 길이정도여부대기
  // 2. 매일여부대기
  if (
    dataList.value.length !== runlogStore.logList.length
    || dataList.value.find((item, index) => item.id !== runlogStore.logList[index].id)
  ) {
    dataList.value = runlogStore.logList
    setTimeout(() => scrollTobottom(), 100)
  }
}

// 테이블데이터변수시까지
watch(
  () => runlogStore.logList.length,
  () => {
    // 예결과창소, 이면아니오업데이트데이터
    if (!isMinimized.value) {
      refreshDataList()
    }
  },
  { immediate: true },
)

watch(
  () => isMinimized.value,
  (val) => {
    // 예결과창복사, 이면업데이트데이터
    if (!val) {
      refreshDataList()
    }
  },
)

// 매실행전를dataList빈, 해제로그데이터아니오업데이트제목
watch(() => useRunningStore().running, (val) => {
  if (val !== 'free') {
    dataList.value = []
  }
})

const onCellClick: VxeGridEventProps['onCellClick'] = (data) => {
  const { row } = data
  emit('rowClick', row)
}

const setRowClassName: VxeGridProps['rowClassName'] = ({ row }) => {
  const levelClassMap = {
    error: 'row-error',
    warning: 'row-warning',
  }

  return levelClassMap[row.logLevel] || ''
}
</script>

<template>
  <div class="runlog-content_table" :class="[colorTheme]">
    <VxeGrid
      id="id"
      ref="xGrid"
      size="mini"
      :height="height"
      border="none"
      :show-overflow="true"
      :keep-source="false"
      :columns="columns"
      :data="dataList"
      :empty-text="$t('noData')"
      :row-class-name="setRowClassName"
      :scroll-y="{ enabled: true }"
      :row-config="{ isHover: true }"
      :menu-config="{
        body: {
          options: [[{ code: 'copy', name: $t('copy') }]],
        },
        visibleMethod: ({ options, column }) => {
          const isVisible = column?.field === 'content';

          const copyOption = options
            .flat(1)
            .find((item) => item.code === 'copy');
          if (copyOption) {
            copyOption.visible = isVisible;
          }

          return true;
        },
      }"
      :tooltip-config="{ showAll: false }"
      @menu-click="onMenuClick"
      @cell-click="onCellClick"
    >
      <template #content_default="{ row }">
        <Tooltip
          :title="row.content"
          :mouse-enter-delay="1"
          :overlay-inner-style="{
            width: row.content.length >= 100 ? '600px' : 'auto',
            maxHeight: '150px',
            overflow: 'hidden',
            overflowY: 'auto',
          }"
          placement="topLeft"
        >
          {{ row.content }}
        </Tooltip>
      </template>
    </VxeGrid>
  </div>
</template>

<style lang="scss">
.runlog-content_table {
  height: 100%;

  --vxe-ui-table-row-height-mini: 32px;
  --vxe-ui-table-column-padding-mini: 5px 0;
  --vxe-ui-font-color: rgba(0, 0, 0, 0.85);
  --vxe-ui-table-header-font-color: rgba(0, 0, 0, 0.45);
  --vxe-ui-table-header-font-weight: 400;

  &.dark {
    --vxe-ui-font-color: rgba(255, 255, 255, 0.85);
    --vxe-ui-table-header-font-color: rgba(255, 255, 255, 0.45);
  }

  .row-error {
    color: $color-error;
  }
  .row-warning {
    color: $color-warning;
  }

  .custom-content-render {
    white-space: nowrap;
    text-overflow: ellipsis;
    overflow: hidden;
    user-select: text;
  }
}
</style>
