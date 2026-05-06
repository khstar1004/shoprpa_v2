<script setup lang="ts">
import type { TreeProps } from 'ant-design-vue'
import { Empty, Popover } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { filter, find } from 'lodash-es'
import { computed, inject, ref } from 'vue'

import type { VARIABLE_TYPE } from '@/constants/atom'
import { GLOBAL_VAR_IN_TYPE, LIMIT_VARIABLE_SELECT, PARAMETER_VAR_IN_TYPE, VAR_IN_TYPE } from '@/constants/atom'
import { useFlowStore } from '@/stores/useFlowStore'
import { useProcessStore } from '@/stores/useProcessStore'
import { useVariableStore } from '@/stores/useVariableStore'
import { GLOBAL_VAR_TYPE, ORIGIN_VAR, PARAMETER_VAR_TYPE, PROCESS_VAR_TYPE } from '@/views/Arrange/config/atom'
import type { VariableTypes } from '@/views/Arrange/types/atomForm'
import { atomScrollIntoView } from '@/views/Arrange/utils'

import type { VarTreeItem } from '../../types/flow'

import { createDom, generateValTree, varListUnique } from './hooks/useAtomVarPopover'
import VariableTreeTitle from './VariableTreeTitle.vue'

const props = defineProps({
  renderData: {
    type: Object as () => RPA.AtomDisplayItem,
    default: () => ({}),
  },
  varType: {
    type: String,
    default: '',
  },
})
// inject 변수유형
const variableType = inject<VariableTypes>('variableType', '') // 로빈문자열, 테이블아니오제한제어유형
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
let leafInputObj = {}

const { t } = useTranslation()
const variableStore = useVariableStore()
const flowStore = useFlowStore()
const processStore = useProcessStore()

/**
 * 변수유형선택제한제어
 * 1. 저장에서브라우저객체, word객체, excel객체로입력매개변수의기존가능, 분가능선택의객체유형
 */
const isLimitSelect = computed(() =>
  LIMIT_VARIABLE_SELECT.includes(props.varType as VARIABLE_TYPE),
)

// 완료변수
function getFlowVarTree(): VarTreeItem[] {
  const { formType: { type } } = props.renderData
  const flowVariableList = variableStore.filterCurrentVariableListByType(type)

  const filterArr = varListUnique(flowVariableList)
    .filter((item) => {
      if (!props.varType)
        return true

      if (isLimitSelect.value) {
        return item.types === props.varType
      }

      return true
    })
    .map(item => ({
      ...item,
      definition: `${item.rowNum}행[${item.anotherName}]`,
      template: processStore.globalVarTypeList[item.types]?.template,
    }))

  return generateValTree(filterArr).reverse()
}

// 완료전역 변수
function getGlobalVarTree(): VarTreeItem[] {
  const filterGlobalVarList = variableStore.globalVariableList
    .filter((item) => {
      if (!props.varType)
        return true

      const isItemLimitSelect = LIMIT_VARIABLE_SELECT.includes(item.varType as VARIABLE_TYPE)
      return isLimitSelect.value ? isItemLimitSelect : !isItemLimitSelect
    })
    .map(item => ({
      ...item,
      definition: item.varDescribe
        ? `전역 변수[${item.varDescribe}]`
        : '전역 변수',
    }))

  // TODO: generateValTree 합치기재완료, 해당출력
  return generateValTree(filterGlobalVarList as unknown as any[])
}

// 완료구성 매개변수
function getParameterVarTree(): VarTreeItem[] {
  return processStore.parameters.map((item) => {
    const { desc, funcList } = processStore.globalVarTypeList[item.varType]

    return {
      key: item.id,
      id: item.id,
      title: `${item.varName} (${desc})`,
      definition: item.varDescribe
        ? `구성 매개변수[${item.varDescribe}]`
        : '구성 매개변수',
      children: funcList.map(fn => ({
        title: `${fn.funcDesc} (${fn.resDesc})`,
        key: `${item.varName}/${fn.useSrc}`,
        isLeaf: true,
      })),
    }
  })
}

const varTypeList = [
  { label: PROCESS_VAR_TYPE, jumpable: true, type: VAR_IN_TYPE, generate: getFlowVarTree },
  { label: GLOBAL_VAR_TYPE, jumpable: false, type: GLOBAL_VAR_IN_TYPE, generate: getGlobalVarTree },
  { label: PARAMETER_VAR_TYPE, jumpable: false, type: PARAMETER_VAR_IN_TYPE, generate: getParameterVarTree },
]

const activeKey = ref<string>(
  find(varTypeList, { label: variableType })
    ? variableType
    : varTypeList[0].label,
)
const searchValue = ref<string>('')

const tabList = computed(() => {
  return find(varTypeList, { label: variableType })
    ? filter(varTypeList, { label: variableType })
    : varTypeList
})

const treeData = computed(() => {
  const generateFn = tabList.value.find(
    item => item.label === activeKey.value,
  )?.generate
  const treeDataList: VarTreeItem[] = generateFn?.() || []

  // 근거 searchValue 선택 title
  return treeDataList.filter((item) => {
    if (!searchValue.value)
      return true

    return item.title.toLowerCase().includes(searchValue.value.toLowerCase())
  })
})

const selectedHandle: TreeProps['onSelect'] = (selectedKeys, info) => {
  const { selectedNodes, node } = info
  const activeType = varTypeList.find(item => item.label === activeKey.value)?.type

  if (selectedNodes.length < 1 || !Array.isArray(props.renderData.value) || !activeType)
    return

  let title = ''
  if (node.parent) {
    // 클릭의예
    const keyArr = (selectedKeys[0] as string).split('/')
    title = keyArr[1]
      .replace('@{self:self}', keyArr[0])
      .replace(/@\([^()]*\)/g, (args) => {
        const match = args.match(/\((.*?)\)/)
        const valData = leafInputObj[`@(@{${match[1]}})`]
        if (match[1].includes('str'))
          return valData
        if (valData) {
          if (!(selectedKeys[0] as string).includes('-'))
            return Number(valData) - 1
          return valData
        }
        return keyArr[1].includes('-') ? 1 : 0
      })
  }
  else {
    const varName = selectedNodes[0].title.split(' ')[0]
    // 예결과예전역 변수, 형식로 gv['변수이름']
    if (activeType === GLOBAL_VAR_IN_TYPE) {
      title = `gv['${varName}']`
    }
    else {
      title = varName
    }
  }

  const obj = { val: title, category: activeType }

  createDom(obj, props.renderData, ORIGIN_VAR)
}

function leafInput(val: Record<string, unknown>) {
  leafInputObj = { ...leafInputObj, ...val }
}

function handleJump(id: string) {
  if (activeKey.value === PROCESS_VAR_TYPE) {
    atomScrollIntoView(id)
    flowStore.setJumpFlowId(id)
  }
}
</script>

<template>
  <a-input
    v-model:value="searchValue"
    class="atom-popover-search"
    :placeholder="t('searchVariables')"
    style="width: 230px"
  />
  <a-tabs
    v-model:active-key="activeKey"
    size="small"
    :tab-bar-gutter="18"
    class="mt-1"
  >
    <a-tab-pane
      v-for="tab in tabList"
      :key="tab.label"
      class="atom-popover-tabs"
      :tab-bar-style="{ fontSize: '12px' }"
      :tab="t(tab.label)"
    >
      <a-tree
        v-if="treeData.length > 0"
        block-node
        :tree-data="treeData"
        :height="160"
        @select="selectedHandle"
      >
        <template #title="{ title, definition, template, id }">
          <Popover placement="left" :align="{ offset: [-24, 0] }" :open="definition ? undefined : false">
            <template #content>
              <div class="flex items-center text-xs">
                지정: {{ definition }}
                <span
                  v-if="tab.jumpable"
                  class="flex items-center cursor-pointer text-primary gap-1"
                  @click="handleJump(id)"
                >
                  <rpa-icon name="jump" size="14" />
                  변환
                </span>
              </div>
              <div v-if="template" class="text-xs">
                매개출력: {{ template }}
              </div>
            </template>
            <div class="input-box">
              <VariableTreeTitle
                :key="title"
                :title="title"
                @get-input-val="leafInput"
              />
            </div>
          </Popover>
        </template>
      </a-tree>
      <Empty v-else :image="simpleImage" :description="null" />
    </a-tab-pane>
  </a-tabs>
</template>

<style lang="scss" scoped>
.atom-popover-search {
  font-size: 12px;
}

.input-box {
  display: flex;
  align-items: center;
}

:deep(.ant-tabs-nav-list > .ant-tabs-tab) {
  font-size: 12px;
}

:deep(.ant-tree-title) {
  display: inline-flex;
  line-height: 28px;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  align-items: center;
}

:global(.ui-at) {
  display: inline-flex;
  align-items: center;
  border: 0;
  cursor: pointer;
}

:global(.ui-at::before) {
  display: inline-block;
  border-radius: 5px;
  padding: 0 5px;
  border: 1px solid #ccc;
  line-height: 18px;
  content: attr(data-name);
}

:deep(.ant-tree-list::-webkit-scrollbar) {
  width: 6px;
}
</style>