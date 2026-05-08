import { useRoute } from 'vue-router'

import { saveSmartComp } from '@/api/component'
import { useFlowStore } from '@/stores/useFlowStore'
import { useProcessStore } from '@/stores/useProcessStore'
import { addAtomData } from '@/views/Arrange/components/flow/hooks/useFlow'

import { SMART_COMPONENT_KEY_PREFIX } from '../config/constants'
import type { SmartComp, SmartType } from '../types'
import { getSmartComponentId } from '../utils'

// 서비스
export function useSmartCompService() {
  const processStore = useProcessStore()
  const flowStore = useFlowStore()
  const route = useRoute()

  async function saveSmartCompWithVersionList(
    smartComp: SmartComp,
    smartType: SmartType | undefined,
    versionList: SmartComp[],
  ) {
    if (!smartComp) {
      throw new Error('smartComp is required')
    }

    const smartId = getSmartComponentId(smartComp.key)
    const isNewComponent = !smartId

    // 저장까지서비스기기
    const savedSmartId = await saveSmartComp({
      robotId: processStore.project.id,
      smartId,
      smartType,
      detail: {
        versionList,
      },
    })

    const key = `${SMART_COMPONENT_KEY_PREFIX}.${savedSmartId}`
    const updatedComp = smartComp
    updatedComp.key = key

    // 업데이트 versionList 중모든버전의 key
    if (versionList.length > 0) {
      versionList.forEach((version) => {
        version.key = key
      })
    }

    // 예결과예새컴포넌트, 필요저장으로업데이트 versionList 중의 key
    if (isNewComponent) {
      await saveSmartComp({
        robotId: processStore.project.id,
        smartId: savedSmartId,
        smartType,
        detail: {
          versionList,
        },
      })
    }

    // 예결과컴포넌트아니오저장된 프로세스중, 추가까지프로세스
    const hasExist = flowStore.simpleFlowUIData.find(item => item.key === smartComp.key)
    if (!hasExist) {
      await addAtomData(key, route.query.newIndex ? Number(route.query.newIndex) : undefined)
    }
    else {
      updateDocAndFlowNode(smartComp)
    }

    return updatedComp
  }

  function updateDocAndFlowNode(smartComp: SmartComp) {
    const index = flowStore.simpleFlowUIData.findIndex(item => item.key === smartComp.key)
    const flowNode = flowStore.simpleFlowUIData[index]
    const node = {
      ...smartComp,
      key: smartComp.key,
      version: smartComp.version,
      id: flowNode.id,
      alias: smartComp.alias || smartComp.title,
      inputList: smartComp.inputList || [],
      outputList: smartComp.outputList || [],
      advanced: smartComp.advanced || [],
      exception: smartComp.exception || [],
      disabled: flowNode.disabled,
      breakpoint: flowNode.breakpoint,
    } as unknown as RPA.Atom
    flowStore.updataOriginFlowData([{ node, index, process: processStore.activeProcessId }])
  }

  return {
    saveSmartCompWithVersionList,
  }
}
