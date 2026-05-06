import { get } from 'lodash-es'
import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

import { addGlobalVariable, deleteGlobalVariable, getGlobalVariable, saveGlobalVariable } from '@/api/resource'
import { ATOM_FORM_TYPE } from '@/constants/atom'
import { useFlowStore } from '@/stores/useFlowStore'
import { useProcessStore } from '@/stores/useProcessStore'
import useProjectDocStore from '@/stores/useProjectDocStore'
import { caculateConditional } from '@/views/Arrange/utils/selfExecuting'

// 지정프로세스변수store
export const useVariableStore = defineStore('variable', () => {
  const flowStore = useFlowStore()
  const processStore = useProcessStore()
  const projectDocStore = useProjectDocStore()

  const globalVariableList = ref<RPA.GlobalVariable[]>([]) // 전역 변수목록

  //   프로세스변수목록
  const getFlowVariableList = (idx: number, processId: string) => {
    const localVariableList = [] // 프로세스변수목록

    projectDocStore.userFlowNode(processId).slice(0, idx).forEach((flow: RPA.Atom, pos) => {
      const { outputList, id, alias } = flow
      const formItemList = [
        ...get(flow, 'inputList', []),
        ...get(flow, 'outputList', []),
        ...get(flow, 'advanced', []),
      ]
      const formValues = formItemList.reduce((result, item) => {
        return Object.assign(result, { [item.key]: typeof item.value === 'string' ? { value: item.value } : item.value })
      }, {})

      outputList.forEach((item, index) => {
        const { dynamics } = item
        const isShow = !dynamics || caculateConditional(dynamics, formValues, item)

        if (isShow) {
          const { value } = item
          const notNullArr = Array.isArray(value) ? value.filter((item: RPA.AtomFormItemResult) => item.value) : []
          const dialogResult = flow.key === 'Dialog.custom_box' ? flow.inputList.find(input => input.key === 'design_interface')?.value : ''

          notNullArr.length > 0 && localVariableList.push({
            id: `${id}-${index}`,
            types: item.types,
            rowNum: pos + 1,
            anotherName: alias,
            atomId: id,
            value: notNullArr,
            dialogResult,
          })
        }
      })
    })

    return localVariableList
  }

  //   가져오기현재기존가능의입력변수목록(아니오패키지현재기존가능)
  const getBeforeCurrentVariableList = (rowNum: number, id: string) => {
    if (rowNum <= 0)
      return []
    return getFlowVariableList(rowNum, id)
  }

  //   가져오기현재기존가능의출력변수목록(패키지현재기존가능)
  const getCurrentVariableList = (rowNum: number, id: string) => getFlowVariableList(rowNum + 1, id)

  //   선택기호합치기현재기존가능유형의변수
  const filterCurrentVariableListByType = (ioType: string, idx = getActiveAtomIndex(), processId = processStore.activeProcessId) => {
    if (Object.is(ioType, ATOM_FORM_TYPE.RESULT)) {
      return getCurrentVariableList(idx, processId)
    }

    return getBeforeCurrentVariableList(idx, processId)
  }

  // 가져오기전역 변수목록
  const getGlobalVariableList = async (robotId: string = processStore.project.id) => {
    const { data = [] } = await getGlobalVariable({ robotId })
    return globalVariableList.value = data.reverse()
  }

  // 삭제전역 변수
  const deleteGlobalVariableList = async (globalId: string) => {
    await deleteGlobalVariable({
      robotId: processStore.project.id,
      globalId,
    })

    globalVariableList.value = globalVariableList.value.filter(item => item.globalId !== globalId)
  }

  // 완료일변수이름
  const genUniqueVariableName = () => {
    const baseName = 'g_variable'
    let count = 0
    let variableName = baseName

    while (globalVariableList.value.some(variable => variable.varName === variableName)) {
      count += 1
      variableName = `${baseName}_${count}`
    }

    return variableName
  }

  // 추가전역 변수
  const addGlobalVariableList = async () => {
    const newVariable: RPA.GlobalVariable = {
      robotId: processStore.project.id,
      globalId: '',
      varName: genUniqueVariableName(),
      varType: 'Any',
      varValue: '',
      varDescribe: '',
    }

    await addGlobalVariable(newVariable)
    const newVariableList = await getGlobalVariableList()

    return newVariableList[0]
  }

  // 수정/저장전역 변수
  const saveGlobalVariableList = async (globalVariable: RPA.GlobalVariable) => {
    await saveGlobalVariable(globalVariable)
    await getGlobalVariableList()
  }

  const getActiveAtomIndex = () => {
    return flowStore.simpleFlowUIData.findIndex(flow => flow.id === flowStore.activeAtom.id)
  }

  watch(() => processStore.project.id, (robotId) => {
    if (robotId) {
      getGlobalVariableList(robotId)
    }
  }, { immediate: true })

  return {
    globalVariableList,
    addGlobalVariableList,
    deleteGlobalVariableList,
    saveGlobalVariableList,
    filterCurrentVariableListByType,
  }
})