import { defineStore } from 'pinia'
import { ref } from 'vue'

import { getPyPackageListApi } from '@/api/resource'
import { useProcessStore } from '@/stores/useProcessStore'

// 지정프로세스변수store
export const usePythonPackageStore = defineStore('pythonPackage', () => {
  const pythonPackageList = ref([]) // 패키지목록
  const pyLoadingType = ref('') // 저장현재설치, 업그레이드, 삭제의닫기상태
  const selectedPackageIds = ref([]) // 저장현재선택중의패키지id

  const setPythonPackageList = (list: any) => {
    pythonPackageList.value = list
  }

  const setPyLoadingType = (loadingType: string) => {
    pyLoadingType.value = loadingType
  }

  const setSelectedPackageIds = (list: any) => {
    selectedPackageIds.value = list
  }

  const getSelectedPackages = () => {
    return pythonPackageList.value.filter(i => selectedPackageIds.value.includes(i.id))
  }

  /**
   * 재
   */
  const reset = () => {
    pyLoadingType.value = ''
    pythonPackageList.value = []
    selectedPackageIds.value = []
  }

  // 가져오기완료설치의python패키지목록
  function getPythonList() {
    getPyPackageListApi({ robotId: useProcessStore().project.id }).then((res) => {
      setPythonPackageList(res.data)
    })
  }

  function updatePythonList() {
    getPythonList()
  }

  return {
    pythonPackageList,
    pyLoadingType,
    selectedPackageIds,
    setPyLoadingType,
    setSelectedPackageIds,
    getSelectedPackages,
    updatePythonList,
    reset,
  }
})