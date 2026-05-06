import { nextTick } from 'vue'

import { getBaseURL } from '@/api/http/env'
import { addPyPackageApi, deletePyPackageApi, packageVersion, updatePyPackageApi } from '@/api/resource'
import { sseRequest } from '@/api/sse'
import { useProcessStore } from '@/stores/useProcessStore'
import { usePythonPackageStore } from '@/stores/usePythonPackageStore'

import { pythonInstallModal } from '../modals'

export function useManagePython() {
  const pythonPackageStore = usePythonPackageStore()
  const processStore = useProcessStore()
  let controller: AbortController | null = null

  // 설치패키지
  function installPackage(pacakgeOption, upgrade = false) {
    const params = { robotId: processStore.project.id, ...pacakgeOption }
    pythonPackageStore.setPyLoadingType(upgrade ? 'upgrading' : 'installing')
    controller = sseRequest.post(
      `${getBaseURL()}/scheduler/pip/install`,
      {
        project_id: processStore.project.id,
        package: pacakgeOption.packageName,
        version: pacakgeOption.packageVersion,
        mirror: pacakgeOption.mirror,
      },
      (res) => {
        if (!res)
          return
        let newData
        try {
          newData = JSON.parse(res.data).stdout
        }
        // eslint-disable-next-line unused-imports/no-unused-vars
        catch (e) {
          newData = res.data
        }

        if (newData.includes('stderr')) {
          pythonPackageStore.setPyLoadingType(upgrade ? 'upgradeFail' : 'installFail')
          controller.abort()
          controller = null
          return
        }
        if (newData.includes('[DONE]')) {
          pythonPackageStore.setPyLoadingType(upgrade ? 'upgradeSuccess' : 'installSuccess')
          pythonInstallModal.hide()
          upgrade ? updatePackage(params) : addPackage(params)
          controller.abort()
          controller = null
          return
        }
        if (newData) {
          pacakgeOption.output += newData
          handleScrollToBottom()
        }
      },
      () => {
        pythonPackageStore.setPyLoadingType(upgrade ? 'upgradeFail' : 'installFail')
      },
    )
  }

  function handleScrollToBottom() {
    nextTick(() => {
      const dom = document.querySelector('#form_item_output')
      if (dom) {
        dom.scrollTop = dom.scrollHeight
      }
    })
  }
  // 업그레이드패키지
  function upgradePackage() {
    const packages = pythonPackageStore.getSelectedPackages()
    packages.forEach((item) => {
      installPackage({ ...item, packageVersion: '', projectId: useProcessStore().project.id }, true)
    })
  }

  // 패키지
  function uninstallPackage() {
    // 완료후, 호출http삭제패키지
    pythonPackageStore.setPyLoadingType('uninstalling')
    delPackage()
  }

  // 가져오기패키지버전, 호출http연결추가또는업데이트패키지데이터
  function getPackageVersion(packageParams, apiFn = addPyPackageApi) {
    packageVersion(packageParams).then((res) => {
      apiFn({ ...packageParams, packageVersion: res.data.version }).then(() => {
        pythonPackageStore.updatePythonList()
      })
    }).catch(() => {
    })
  }

  // 호출http추가패키지
  function addPackage(packageParams) {
    getPackageVersion(packageParams, addPyPackageApi)
  }

  // 호출http업데이트패키지
  function updatePackage(packageParams) {
    getPackageVersion(packageParams, updatePyPackageApi)
  }

  // 호출http삭제패키지
  function delPackage() {
    const packages = pythonPackageStore.getSelectedPackages()
    deletePyPackageApi({
      robotId: useProcessStore().project.id,
      idList: packages.map(i => i.id),
    }).then(() => {
      pythonPackageStore.setPyLoadingType('uninstallSuccess')
      pythonPackageStore.updatePythonList()
    }).catch(() => {
      pythonPackageStore.setPyLoadingType('uninstallFail')
    })
  }

  // 업데이트팝업안내
  const upgradeModal = (ids) => {
    ids && pythonPackageStore.setSelectedPackageIds(ids)
    pythonPackageStore.setPyLoadingType('upgradePythonTip')
  }

  // 팝업안내
  const uninstallModal = (ids) => {
    ids && pythonPackageStore.setSelectedPackageIds(ids)
    pythonPackageStore.setPyLoadingType('uninstallTip')
  }

  return {
    installPackage,
    upgradePackage,
    uninstallPackage,
    upgradeModal,
    uninstallModal,
  }
}