import { useAsyncState, useDebounceFn } from '@vueuse/core'
import { message } from 'ant-design-vue'
import { isEmpty } from 'lodash-es'
import { defineStore } from 'pinia'
import { computed, ref, watchEffect, watchPostEffect } from 'vue'
import { useRoute } from 'vue-router'

import { flatAtomicTree } from '@/utils/common'
import { trackComponentUsageChange } from '@/utils/customComponent'
import { LRUCache } from '@/utils/lruCache'

import {
  createConfigParam,
  deleteConfigParam,
  getAtomsMeta,
  getComponentList,
  getConfigParams,
  getFavoriteList,
  getModuleMeta,
  updateConfigParam,
} from '@/api/atom'
import { getProcessPyCode, saveProcessPyCode } from '@/api/resource'
import { PROCESS_OPEN_KEYS } from '@/constants'
import { useFlowStore } from '@/stores/useFlowStore'
import useProjectDocStore from '@/stores/useProjectDocStore'
import useUserSettingStore from '@/stores/useUserSetting.ts'
import {
  delectSubProcessQuote,
  querySubProcessQuote,
} from '@/views/Arrange/utils'

interface ProjectData {
  id: string
  name: string
  version: number
}

// 여부예코드모듈
export function isPyModel(category: RPA.Flow.ProcessModuleType) {
  return category === 'module'
}

export const useProcessStore = defineStore('process', () => {
  const openProcessLRUCache = new LRUCache<string[]>(PROCESS_OPEN_KEYS, 10, [])

  const docStore = useProjectDocStore()
  const flowStore = useFlowStore()
  const route = useRoute()
  const isComponent = computed(() => route?.query?.type === 'component')

  const atomMeta = useAsyncState<RPA.AtomMetaData>(() => getAtomsMeta(), {
    atomicTree: [],
    atomicTreeExtend: [],
    commonAdvancedParameter: [],
    types: {},
  })

  const project = ref<ProjectData>({ id: '', name: '이름', version: 0 })
  const activeProcessId = ref('')
  const processList = ref<RPA.Flow.ProcessModule[]>([])
  const searchSubProcessId = ref('')
  const searchSubProcessResult = ref([])
  const pyCodeText = ref('')
  const changeFlag = ref(false)
  // 구성 매개변수목록
  const parameters = ref<RPA.ConfigParamData[]>([])
  // 컴포넌트속성목록
  const attributes = ref<RPA.ComponentAttrData[]>([])
  // 기존가능 tree 목록
  const atomicTreeData = computed<RPA.AtomTreeNode[]>(
    () => atomMeta.state.value.atomicTree || [],
  )
  // 의즐겨찾기 tree 목록
  const favorite = useAsyncState(getFavoriteList, [], { immediate: false })
  // 컴포넌트 tree 목록
  const extendTree = useAsyncState(getModuleMeta, [], { immediate: false })
  // 지정컴포넌트 tree 목록
  const componentTree = useAsyncState(() => getComponentList({ robotId: project.value.id }), [], { immediate: false })
  // 전역 변수목록
  const globalVarTypeList = computed<Record<string, RPA.VariableValueType>>(
    () => atomMeta.state.value?.types || {},
  )
  // 높음단계매개변수및예외관리테이블단일공유매칭
  const commonAdvancedParameter = computed<RPA.AtomFormBaseForm[]>(
    () => atomMeta.state.value?.commonAdvancedParameter || [],
  )

  // 평면후의기존가능 tree 목록 (아니오패키지)
  const atomicTreeDataFlat = computed<RPA.AtomTreeNode[]>(() =>
    flatAtomicTree(atomicTreeData.value, false),
  )

  // 구성 매개변수연결매개변수
  const cofnigParamIdOption = computed(() => {
    const isPy = isPyModel(activeProcess.value?.resourceCategory)
    return isPy ? { moduleId: activeProcessId.value } : { processId: activeProcessId.value }
  })

  // 새로고침후요청
  watchEffect(async () => {
    if (project.value.id && activeProcessId.value) {
      parameters.value = await getConfigParams({
        ...cofnigParamIdOption.value,
        robotId: project.value.id,
      })
    }
  })

  watchPostEffect(() => {
    if (isEmpty(processList.value))
      return

    const openKeys = processList.value
      .filter(it => it.isOpen)
      .map(it => it.resourceId)
    openProcessLRUCache.debouncedSet(project.value.id, openKeys)
  })

  // 완료일의구성 매개변수이름
  const generateParameterName = () => {
    const baseName = 'p_variable'
    let count = 0
    let variableName = baseName

    while (
      parameters.value.some(variable => variable.varName === variableName)
    ) {
      count += 1
      variableName = `${baseName}_${count}`
    }

    return variableName
  }

  // 추가매개변수
  const createParameter = async () => {
    const data: RPA.CreateConfigParamData = {
      ...cofnigParamIdOption.value,
      varName: generateParameterName(),
      varDirection: 0,
      varType: 'Str',
      varDescribe: '',
      varValue: '',
      robotId: project.value.id,
    }
    const id = await createConfigParam(data)

    parameters.value.push({ id, ...data })
  }

  // 삭제매개변수
  const deleteParameter = async (item: RPA.ConfigParamData) => {
    await deleteConfigParam(item.id)
    parameters.value = parameters.value.filter(it => item.id !== it.id)
  }

  // 업데이트매개변수
  const updateParameter = async (data: RPA.ConfigParamData) => {
    const newParameter = { ...data, robotId: project.value.id }
    if (!data.processId && !data.moduleId) {
      Object.assign(newParameter, cofnigParamIdOption.value)
    }
    await updateConfigParam(newParameter)
    const target = parameters.value.find(item => item.id === data.id)
    target && Object.assign(target, data)
  }

  // 완료일의컴포넌트속성이름
  const generateAttributeName = () => {
    const baseName = 'p_variable'
    let count = 0
    let variableName = baseName

    while (
      attributes.value.some(variable => variable.varName === variableName)
    ) {
      count += 1
      variableName = `${baseName}_${count}`
    }

    return variableName
  }

  // 추가속성
  const createAttribute = async () => {
    const data: RPA.CreateComponentAttrData = {
      varName: generateAttributeName(),
      varDirection: 0,
      varType: 'Str',
      varDescribe: '',
      varValue: '',
      varFormType: {
        type: 'other',
        value: [],
      },
      robotId: project.value.id,
      processId: activeProcessId.value,
    }
    // const id = await createComponentAttr(data)
    const id = Date.now().toString()

    attributes.value.push({ id, ...data })
  }

  // 삭제속성
  const deleteAttribute = async (item: RPA.ComponentAttrData) => {
    // await deleteComponentAttr(item.id)
    attributes.value = attributes.value.filter(it => item.id !== it.id)
  }

  // 업데이트속성
  const updateAttribute = async (data: RPA.ComponentAttrData) => {
    // await updateComponentAttr({ ...data, robotId: project.value.id })
    attributes.value = attributes.value.map(item =>
      item.id === data.id ? { ...item, ...data } : item,
    )
  }

  const globalVarTypeOption = computed(() => {
    // 백엔드반환의전역 변수목록있음 channel 대기 global 의허용에서변수관리관리중추가
    return Object.values(globalVarTypeList.value).filter(
      item => item.channel.split(',').includes('global'),
    )
  })

  // 정보
  const setProject = (projectInfo: ProjectData) => {
    project.value = projectInfo
  }

  // id
  const setProjectId = (id: string) => {
    project.value.id = id
  }

  const reset = () => {
    processList.value = []
    activeProcessId.value = ''
  }

  const getProcessList = (list: RPA.Flow.ProcessModule[]) => {
    // 에서저장중가져오기열기의프로세스
    const openProcessKeys = openProcessLRUCache.get(project.value.id) ?? []

    processList.value = list
      .map(it => ({
        ...it,
        isLoading: true,
        isOpen: it.name === '프로세스' || openProcessKeys.includes(it.resourceId),
        isMain: it.name === '프로세스', // 일개프로세스예프로세스
      }))
      .sort((a, b) => (b.isMain ? 1 : 0) - (a.isMain ? 1 : 0)) // 근거 isMain 행정렬

    activeProcessId.value = processList.value[0]?.resourceId ?? ''
  }

  const activeProcess = computed(() => {
    return processList.value.find(
      it => it.resourceId === activeProcessId.value,
    )
  })

  // 전체영역의, 예프로세스의저장, 실행여부가능으로행
  const operationDisabled = computed(() => {
    return isEmpty(processList.value) || !!activeProcess.value?.isLoading
  })

  const checkActiveProcess = (id: string) => {
    activeProcessId.value = id
    flowStore.reset()

    // 조회열기여부예프로세스
    const activeProcess = processList.value.find(it => it.resourceId === id)
    if (activeProcess.resourceCategory === 'process') {
      docStore.checkProcess()
    }
  }

  // 열기프로세스
  const openProcess = (id: string) => {
    const process = processList.value.find(it => it.resourceId === id)
    process.isOpen = true
    checkActiveProcess(id)
  }

  // 닫기프로세스
  const closeProcess = (id: string, isDelete = false) => {
    if (isDelete) {
      processList.value = processList.value.filter(
        it => it.resourceId !== id,
      )
    }
    else {
      processList.value = processList.value.map(it => ({
        ...it,
        isOpen: it.resourceId === id ? false : it.isOpen,
      }))
    }

    if (id === activeProcessId.value) {
      activeProcessId.value = processList.value[0]?.resourceId ?? ''
    }
    checkActiveProcess(activeProcessId.value)
  }

  // 닫기모든하위 프로세스
  const closeAllChildProcess = () => {
    processList.value = processList.value.map((item, index) => ({
      ...item,
      isOpen: index === 0,
    }))
    activeProcessId.value = processList.value[0].resourceId
  }

  // 삭제프로세스
  const deleteProcess = (resourceId: string, flag: boolean) => {
    flag ? message.success('삭제되었습니다.') : message.error('삭제에 실패했습니다.')
    if (flag) {
      trackComponentUsageChange(() => {
        closeProcess(resourceId, true)
        delectSubProcessQuote(resourceId)
        changeFlag.value = !changeFlag.value
      })
    }
  }

  // 하위 프로세스사용
  const searchSubProcess = (id: string) => {
    searchSubProcessId.value = id
    searchSubProcessResult.value = querySubProcessQuote(id)
  }

  // 닫기하위 프로세스사용
  const closeSearchSubProcess = () => {
    searchSubProcessId.value = ''
    searchSubProcessResult.value = []
  }

  const getPyCodeText = async (resourceId = activeProcess.value.resourceId) => {
    activeProcess.value.isLoading = true
    pyCodeText.value = await getProcessPyCode({
      moduleId: resourceId,
      robotId: project.value.id,
    })
    activeProcess.value.isLoading = false
  }

  const setCodeText = (text: string) => {
    pyCodeText.value = text
  }

  const savePyCode = () => {
    return saveProcessPyCode({
      moduleId: activeProcess.value.resourceId,
      robotId: project.value.id,
      moduleContent: pyCodeText.value,
    })
  }

  const saveProject = () => {
    let saveFn = Promise.resolve<void | boolean>(true)

    if (isPyModel(activeProcess.value?.resourceCategory)) {
      saveFn = savePyCode()
    }
    else {
      saveFn = docStore.saveProcess()
    }

    return saveFn
  }

  // 추가프로세스또는모듈
  const processOrModule = ({ resourceId, name, type }) => {
    const process: RPA.Flow.ProcessModule = {
      resourceId,
      name,
      resourceCategory: type,
      isOpen: true,
      isLoading: false,
    }
    processList.value.push(process)
    checkActiveProcess(resourceId)
    changeFlag.value = !changeFlag.value
  }

  // 이름 변경프로세스
  const renameModule = async (name: string, id: string) => {
    const process = processList.value.find(it => it.resourceId === id)
    if (process) {
      process.name = name
      changeFlag.value = !changeFlag.value
    }
  }

  // 저장상태
  let timer = null
  const canSave = (isSaveing = true, ignoreAutoSave = false) => {
    if (ignoreAutoSave)
      return isSaveing
    // 중의저장여부사용
    return (
      isSaveing && useUserSettingStore().userSetting.commonSetting.autoSave
    )
  }
  const setSavingType = useDebounceFn(
    async (
      id: string,
      saveCallback: () => Promise<unknown>,
      isSaveing = true,
      ignoreAutoSave = true,
      delayTime = 0,
    ) => {
      const process = processList.value.find(it => it.resourceId === id)
      if (!process)
        return
      process.isSaveing = isSaveing
      if (!canSave(isSaveing, ignoreAutoSave)) {
        timer && clearTimeout(timer)
        return
      }
      timer = setTimeout(async () => {
        clearTimeout(timer)
        if (canSave(process.isSaveing, ignoreAutoSave)) {
          await saveCallback()
          process.isSaveing = false
        }
      }, delayTime)
    },
    500,
  )

  return {
    operationDisabled,
    project,
    changeFlag,
    activeProcessId,
    processList,
    activeProcess,
    pyCodeText,
    getPyCodeText,
    globalVarTypeList,
    globalVarTypeOption,
    parameters,
    attributes,
    commonAdvancedParameter,
    atomicTreeDataFlat,
    atomicTreeData,
    favorite,
    atomMeta,
    extendTree,
    componentTree,
    isComponent,
    updateParameter,
    deleteParameter,
    createParameter,
    updateAttribute,
    deleteAttribute,
    createAttribute,
    searchSubProcessId,
    searchSubProcessResult,
    reset,
    savePyCode,
    saveProject,
    setCodeText,
    setProject,
    setProjectId,
    getProcessList,
    checkActiveProcess,
    closeProcess,
    openProcess,
    deleteProcess,
    searchSubProcess,
    closeSearchSubProcess,
    processOrModule,
    renameModule,
    closeAllChildProcess,
    setSavingType,
  }
})
