import { NiceModal } from '@rpa/components'
import type { IAppConfig, UpdateInfo } from '@rpa/shared/platform'
import { useAsyncState, useLocalStorage } from '@vueuse/core'
import { defineStore } from 'pinia'
import { computed, reactive } from 'vue'

import { checkBrowserPlugin, getSupportBrowser } from '@/api/plugin'
import { UpdaterModal } from '@/components/Updater'
import { CLOSE_UPDATE_MODAL_VERSION } from '@/constants'
import type { PLUGIN_ITEM } from '@/constants/plugin'
import { BROWSER_PLUGIN_LIST } from '@/constants/plugin'
import { updaterManager, utilsManager } from '@/platform'

const ENV = import.meta.env

interface UpdaterState extends UpdateInfo {
  checkLoading: boolean // 조회업데이트loading
}

// app config 정보
export const useAppConfigStore = defineStore('appConfig', () => {
  // 닫기업데이트안내팝업의버전
  const closeUpdateModalVersion = useLocalStorage<string[]>(CLOSE_UPDATE_MODAL_VERSION, [])

  const updaterState = reactive<UpdaterState>({
    couldUpdate: false, // 여부필요업데이트
    downloaded: false, // 여부다운로드완료
    manifest: null, // 새버전
    checkLoading: false, // 조회업데이트loading
  })

  // 현재버전
  const { state: appVersion } = useAsyncState<string>(utilsManager.getAppVersion, '')
  // 설치 경로
  const { state: appPath } = useAsyncState<string>(utilsManager.getAppPath, '')
  // 생성버전
  const { state: buildInfo } = useAsyncState<string>(utilsManager.getBuildInfo, '')
  // 시스템
  const { state: systemInfo } = useAsyncState<string>(utilsManager.getSystemEnv, '')
  // 사용자디렉터리
  const { state: userPath } = useAsyncState<string>(utilsManager.getUserPath, '')
  // 사용매칭
  const { state: appConfig } = useAsyncState<IAppConfig>(utilsManager.getAppConfig, {
    remote_addr: '',
    app_auth_type: ENV.VITE_AUTH_TYPE || 'casdoor',
    app_edition: ENV.VITE_EDITION || 'saas',
  })

  const updateBrowserPluginStatus = async (plugins: PLUGIN_ITEM[]) => {
    if (plugins.length === 0)
      return plugins

    // 조회브라우저 확장수정pluginList중브라우저 확장의상태--isInstall및IsNewest
    const { data } = await checkBrowserPlugin(plugins.map(it => it.type))
    return plugins.map((it) => {
      const target = data[it.type]

      if (!target)
        return it

      return {
        ...it,
        isInstall: target.installed,
        isNewest: target.latest,
        installVersion: target.installed_version,
        browserInstalled: target.browser_installed,
      }
    })
  }

  // 브라우저 확장목록
  const { state: browserPlugins } = useAsyncState<PLUGIN_ITEM[]>(async () => {
    const browser = await getSupportBrowser()
    const plugins = BROWSER_PLUGIN_LIST.filter(it => browser.includes(it.type))
    return updateBrowserPluginStatus(plugins)
  }, [])

  // 새로고침브라우저 확장상태
  const refreshBrowserPluginStatus = async () => {
    if (browserPlugins.value.length > 0) {
      browserPlugins.value = await updateBrowserPluginStatus(browserPlugins.value)
    }
  }

  const appInfo = computed(() => ({
    appEdition: appConfig.value.app_edition,
    appAuthType: appConfig.value.app_auth_type,
    appVersion: appVersion.value,
    appPath: appPath.value,
    buildInfo: buildInfo.value,
    systemInfo: systemInfo.value,
    userPath: userPath.value,
    remotePath: appConfig.value.remote_addr,
  }))

  /**
   * 조회업데이트
   * @param manualCheck 여부조회업데이트
   * @returns 업데이트 검사 결과
   */
  const checkUpdate = async (manualCheck = false) => {
    if (updaterState.checkLoading)
      return

    updaterState.checkLoading = true
    const { couldUpdate, downloaded, manifest = null } = await updaterManager.checkUpdate()

    updaterState.checkLoading = false
    updaterState.couldUpdate = couldUpdate
    updaterState.downloaded = downloaded
    updaterState.manifest = manifest

    manualCheck && showUpdaterModal()
  }

  const quitAndInstall = async () => {
    updaterManager.quitAndInstall()
  }

  const showUpdaterModal = () => {
    const needUpdate = updaterState.couldUpdate && updaterState.downloaded
    const latestVersion = needUpdate ? updaterState.manifest?.version : appInfo.value.appVersion

    NiceModal.show(UpdaterModal, {
      needUpdate,
      latestVersion: latestVersion || appInfo.value.appVersion,
      updateNote: updaterState.manifest?.body,
    })
  }

  const onUpdaterDownloaded = () => {
    updaterState.downloaded = true

    // 다운로드완료후, 예결과새의버전완료업데이트, 이면아니오안내
    if (closeUpdateModalVersion.value.includes(updaterState.manifest?.version)) {
      return
    }

    showUpdaterModal()
  }

  // 업데이트
  const rejectUpdate = (version: string) => {
    if (!closeUpdateModalVersion.value.includes(version)) {
      closeUpdateModalVersion.value.push(version)
    }
  }

  return {
    browserPlugins,
    appInfo,
    updaterState,
    checkUpdate,
    quitAndInstall,
    showUpdaterModal,
    rejectUpdate,
    refreshBrowserPluginStatus,
    onUpdaterDownloaded,
  }
})
