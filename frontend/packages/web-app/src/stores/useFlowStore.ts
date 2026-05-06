import { cloneDeep } from 'lodash-es'
// 프로세스정보
import { defineStore } from 'pinia'
import { ref, shallowRef } from 'vue'

import { diffArrays } from '@/utils/common'

import type { GLOBAL_VAR_IN_TYPE, PARAMETER_VAR_IN_TYPE, VAR_IN_TYPE } from '@/constants/atom'
import type { ArgumentValue } from '@/corobot/type'
import { useProcessStore } from '@/stores/useProcessStore'
import useProjectDocStore from '@/stores/useProjectDocStore'
import { changeChecked } from '@/views/Arrange/components/flow/hooks/useChangeStatus'
import { requiredItem } from '@/views/Arrange/components/flow/hooks/useValidate'
import { Catch, CvImageExist, CvImageExistEnd, FileExist, FolderExist, ForBrowserSimilar, ForDataTableLoop, ForDict, ForEnd, ForExcelContent, ForList, ForStep, Group, GroupEnd, If, IfEnd, Try, TryEnd, While, WindowExist } from '@/views/Arrange/config/atomKeyMap'
import { atomScrollIntoView } from '@/views/Arrange/utils'
import { getMultiSelectIds } from '@/views/Arrange/utils/flowUtils'
import { changeSelectAtoms } from '@/views/Arrange/utils/selectItemByClick'

// 프로세스의store
export const useFlowStore = defineStore('flow', () => {
  const docStore = useProjectDocStore()
  const processStore = useProcessStore()

  const nodeContactMap = ref({})
  const simpleFlowUIData = shallowRef<RPA.Atom[]>([])
  const activeAtom = ref(null) // 현재의기존
  const selectedAtomIds = shallowRef([]) // 현재선택중의모든의기존Id
  const multiSelect = ref(false) // 여부열기시작다중선택
  const jumpFlowId = ref<string>()

  const setJumpFlowId = (flowId?: string) => {
    jumpFlowId.value = flowId
  }

  // 변환돌아가기기존의 atom
  const jumpBack = () => {
    atomScrollIntoView(activeAtom.value.id)
    jumpFlowId.value = undefined
  }

  const setSelectedAtomIds = (atomIds: string[]) => {
    const { deleteIds, addIds } = diffArrays(selectedAtomIds.value, atomIds)
    changeChecked(deleteIds, addIds)
    selectedAtomIds.value = atomIds
  }

  const toggleMultiSelect = (value?: boolean) => {
    multiSelect.value = value ?? !multiSelect.value
    // 닫기다중선택시, 지우기선택중
    if (!multiSelect.value && activeAtom.value) {
      const conditionId = getMultiSelectIds(activeAtom.value.id)
      changeSelectAtoms(activeAtom.value.id, conditionId, true)
    }
  }

  const setActiveAtom = (atom: any, reset = true) => {
    activeAtom.value = atom
    reset && !multiSelect.value && setSelectedAtomIds([])
  }

  const setSimpleFlowUIDataByType = (data: RPA.Atom | RPA.Atom[], index: number, flag: boolean) => {
    const newArray = [...simpleFlowUIData.value]
    const result = newArray.splice(index, flag ? 1 : 0, ...(Array.isArray(data) ? data.map(i => i) : [{ ...data }]))
    simpleFlowUIData.value = newArray
    return result
  }

  const setSimpleFlowUIDataProps = (startIdx: number, endIdx: number, callback: (args) => void) => {
    for (let index = startIdx; index < endIdx; index++) {
      const element = simpleFlowUIData.value[index]
      callback && callback(element)
      setSimpleFlowUIDataByType({ ...element }, index, true)
    }
  }

  const setSimpleFlowUIData = (data: RPA.Atom[], index: number, append?: boolean) => {
    if (append) {
      setSimpleFlowUIDataByType(data, index, false)
    }
    else {
      simpleFlowUIData.value = data
      console.log('테이블', simpleFlowUIData.value)
      setActiveAtom(data[index])
    }
  }

  const gainLastError = (id = activeAtom.value.id) => {
    const activeAtomIdx = simpleFlowUIData.value.findIndex(item => item.id === id)
    if (activeAtomIdx < 0)
      return
    const obj = simpleFlowUIData.value[activeAtomIdx]

    obj.nodeError = requiredItem(obj ?? activeAtom.value)
    setSimpleFlowUIDataByType(obj, activeAtomIdx, true)
  }

  const setFormItemValue = (key: string, value: any, index: number | string, flush = true) => {
    const idx = typeof index === 'string' ? simpleFlowUIData.value.findIndex(item => item.id === index) : index
    const curAtom = simpleFlowUIData.value[idx] ?? activeAtom.value
    // 불가복사오류, 값확인아니오, 후제목복사정렬조회원인
    if (!simpleFlowUIData.value[idx]) {
      console.error('simpleFlowUIData.value[idx] is undefined!', idx)
    }

    if (!curAtom) {
      return
    }

    if (key === 'anotherName') {
      curAtom.alias = value
    }
    else if (key === 'conditionalShow') {
      const { advanced, exception, inputList, outputList } = value
      curAtom.inputList = inputList
      curAtom.outputList = outputList
      curAtom.exception = exception
      curAtom.advanced = advanced
    }
    else {
      const { advanced, exception, inputList, outputList } = curAtom
      const arr = [...advanced, ...exception, ...inputList, ...outputList]
      const findItem = arr.find(item => item.key === key)
      findItem && (findItem.value = value)
    }

    if (flush && curAtom.id === activeAtom.value.id)
      setActiveAtom({ ...curAtom }, false)
    gainLastError(curAtom.id)
    docStore.updateProcessNode([idx], [curAtom])
  }

  const updataOriginFlowData = (params: { node: RPA.Atom, index: number, process: string }[]) => {
    params.forEach((item: { node: RPA.Atom, index: number, process: string }) => {
      if (activeAtom.value.id === item.node.id)
        setActiveAtom(item.node, false)
      docStore.updateProcessNode([item.index], [item.node], item.process || processStore.activeProcessId)
      if (item.process === processStore.activeProcessId) {
        simpleFlowUIData[item.index] = item.node
        gainLastError(item.node.id)
      }
    })
  }

  const setFlowNodeExpand = (curNode: RPA.Atom) => {
    const { level, isOpen } = curNode
    const findIdx = simpleFlowUIData.value.findIndex(i => i.id === curNode.id)
    setSimpleFlowUIDataByType(curNode, findIdx, true)
    for (let i = findIdx; i < simpleFlowUIData.value.length; i++) {
      const nextNode = simpleFlowUIData.value[i + 1]
      if (nextNode.level === level)
        break
      if (isOpen) {
        nextNode.isOpen = isOpen
        delete nextNode.isHideNode
      }
      else {
        nextNode.isHideNode = true
      }
      setSimpleFlowUIDataByType(nextNode, i + 1, true)
    }
  }

  const generateContactMap = (list: RPA.Atom[]) => {
    const groupList = []
    const forList = []
    const ifList = []
    const tryList = []
    list.forEach((i) => {
      let res
      let tryRes
      if (i.key === Group)
        groupList.push(i)
      else if ([ForStep, ForDict, ForList, ForExcelContent, ForBrowserSimilar, ForDataTableLoop, While].includes(i.key))
        forList.push(i)
      else if ([If, CvImageExist, FileExist, FolderExist, WindowExist].includes(i.key))
        ifList.push(i)
      else if ([Try, Catch].includes(i.key))
        tryList.push(i)
      else if (i.key === GroupEnd)
        res = groupList.pop()
      else if (i.key === ForEnd)
        res = forList.pop()
      else if ([CvImageExistEnd, IfEnd].includes(i.key))
        res = ifList.pop()
      else if ([TryEnd].includes(i.key))
        tryRes = [tryList.pop(), tryList.pop()]
      if (res)
        nodeContactMap.value[res.id] = i.id
      if (tryRes) {
        nodeContactMap.value[tryRes[0].id] = i.id
        nodeContactMap.value[tryRes[1].id] = i.id
      }
    })
  }
  // 수정또는삭제기존가능테이블단일중의변수
  const flowVariableUpdate = (params: {
    varName: string // 기존본의변수이름
    newVarName?: string // 새의변수이름
    varType: typeof VAR_IN_TYPE | typeof GLOBAL_VAR_IN_TYPE | typeof PARAMETER_VAR_IN_TYPE // 변수유형
    type: 'rename' | 'delete'
    processId?: string // 프로세스id
    startIndex?: number // 열기 검색 (에서 processId 저장에서시있음)
  }) => {
    if (params.varName === params.newVarName)
      return

    const startIdx = params.processId ? params.startIndex : 0
    const processIds = params.processId ? [params.processId] : processStore.processList.filter(it => it.resourceCategory === 'process').map(it => it.resourceId)

    // 생성정상이면테이블방식매칭변수이름
    const escapeRegex = (str: string) => str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    const varNamePattern = `(?<![a-zA-Z0-9_])${escapeRegex(params.varName)}(?![a-zA-Z0-9_])`
    const varNameRegex = new RegExp(varNamePattern, 'g')

    for (const processId of processIds) {
      const processNodeList = docStore.userFlowNode(processId).slice(startIdx)
      const changedNodeList = []

      processNodeList.forEach((node, index) => {
        let isMached = false // 식별자여부매칭까지완료의변수
        const atom = cloneDeep(node)
        const formItemList = [atom.inputList, atom.advanced, atom.outputList].flat(1)

        for (let idx = 0; idx < formItemList.length; idx++) {
          const item = formItemList[idx]
          if (!Array.isArray(item.value))
            continue

          const findIsMached = (i: ArgumentValue) => {
            if (i.type !== params.varType)
              return false

            // 전체매칭
            if (i.value === params.varName) {
              isMached = true
              return true
            }

            // 테이블방식매칭: 조회문자열중여부패키지변수이름
            if (typeof i.value === 'string' && new RegExp(varNamePattern).test(i.value)) {
              isMached = true
              return true
            }

            return false
          }

          if (params.type === 'rename') {
            item.value = item.value.map((it) => {
              if (!findIsMached(it))
                return it

              // 전체매칭직선연결, 테이블방식매칭사용정상이면
              const newValue = it.value === params.varName
                ? params.newVarName
                : typeof it.value === 'string'
                  ? it.value.replace(varNameRegex, params.newVarName)
                  : it.value

              return Object.assign(it, { value: newValue })
            })
          }
          else if (params.type === 'delete') {
            item.value = item.value.filter(it => !findIsMached(it))
          }
          if (Object.is(processId, processStore.activeProcessId)) {
            const { inputList, advanced, outputList } = simpleFlowUIData.value[index]
            const arr = [inputList, advanced, outputList].flat(1)
            arr[idx].value = item.value
          }
        }

        if (isMached) {
          changedNodeList.push({ node: atom, index, process: processId })
        }
      })
      updataOriginFlowData(changedNodeList)
    }
  }

  const reset = () => {
    simpleFlowUIData.value = []
    activeAtom.value = null
    selectedAtomIds.value = []
    nodeContactMap.value = {}
  }

  return {
    jumpFlowId,
    activeAtom,
    simpleFlowUIData,
    nodeContactMap,
    multiSelect,
    selectedAtomIds,
    setFormItemValue,
    setSimpleFlowUIDataByType,
    setSimpleFlowUIDataProps,
    toggleMultiSelect,
    setJumpFlowId,
    jumpBack,
    setActiveAtom,
    setSelectedAtomIds,
    reset,
    gainLastError,
    generateContactMap,
    setSimpleFlowUIData,
    updataOriginFlowData,
    setFlowNodeExpand,
    flowVariableUpdate,
  }
})