import { NiceModal } from '@rpa/components'
import { message } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { onBeforeMount, ref } from 'vue'

import { storage } from '@/utils/storage'

import { browserPluginInstall, checkBrowserRunning, installAllUpdateBrowserPlugin } from '@/api/plugin'
import GlobalModal from '@/components/GlobalModal/index.ts'
import type { PLUGIN_ITEM } from '@/constants/plugin'
import { BROWSER_LIST } from '@/constants/plugin'
import { useAppConfigStore } from '@/stores/useAppConfig'

import _PluginTipModal from '../PluginTipModal.vue'
import _PluginUpdateModal from '../pluginUpdateModal.vue'

const PluginTipModal = NiceModal.create(_PluginTipModal)
const PluginUpdateModal = NiceModal.create(_PluginUpdateModal)

export function useBrowserPlugin() {
  const appConfigStore = useAppConfigStore()
  const pluginList = ref(appConfigStore.browserPlugins)
  const { t } = useTranslation()

  const getInstallOrUpdateText = (pluginItem: PLUGIN_ITEM) => {
    return pluginItem.isNewest ? t('install') : t('update')
  }

  const getReinstallOrUpdateText = (pluginItem: PLUGIN_ITEM) => {
    return pluginItem.isInstall ? t('update') : t('install')
  }

  onBeforeMount(() => {
    appConfigStore.refreshBrowserPluginStatus()
  })

  // 강함제어닫기브라우저, 다시 설치
  const killBrowserReinstall = (pluginItem: PLUGIN_ITEM) => {
    const type = getInstallOrUpdateText(pluginItem)
    const modelConf = {
      title: t('presentation'),
      zIndex: 100,
      content: t('plugin.runningTip', { name: t(pluginItem.title), action: type }),
      okText: t('forceClose'),
      cancelText: t('cancel'),
      onOk: () => {
        installBrowserPlugin(pluginItem, 'brower_install_killBrower')
      },
      centered: true,
      keyboard: false,
    }

    GlobalModal.confirm(modelConf)
  }

  // 설치성공안내, 열기시작확장의팝업
  const successTipOpenStep = (pluginItem: PLUGIN_ITEM) => {
    pluginItem.isInstall = true
    pluginItem.isNewest = true

    if (pluginItem.oparateStepImgs?.length > 0) {
      NiceModal.show(PluginTipModal, {
        stepImgs: pluginItem.oparateStepImgs,
        stepInfo: pluginItem.stepDescription,
      })
      return
    }

    const type = getInstallOrUpdateText(pluginItem)

    GlobalModal.confirm({
      title: t('presentation'),
      zIndex: 100,
      content: t('plugin.operateSuccess', { name: t(pluginItem.title), action: type }),
      okText: t('confirm'),
      cancelText: t('cancel'),
      centered: true,
      keyboard: false,
    })
  }

  // 설치실패안내, 다시 설치
  const failTipWithReinstall = (pluginItem: PLUGIN_ITEM) => {
    const type = getReinstallOrUpdateText(pluginItem)

    GlobalModal.confirm({
      title: t('presentation'),
      zIndex: 100,
      content: t('plugin.operateFailed', { name: t(pluginItem.title), action: type }),
      okText: t('plugin.reAction', { action: type }),
      okType: 'primary',
      onOk: () => installBrowserPlugin(pluginItem),
      centered: true,
      keyboard: false,
    })
  }

  // 설치
  const install = (pluginItem: PLUGIN_ITEM, action = 'install') => {
    pluginItem.loading = true
    browserPluginInstall({ ...pluginItem, action }).then(
      () => {
        successTipOpenStep(pluginItem)
        pluginItem.isInstall = true
        pluginItem.isNewest = true
      },
    ).catch(() => failTipWithReinstall(pluginItem)).finally(() => {
      pluginItem.loading = false
    })
  }

  // 설치확장
  const installBrowserPlugin = (pluginItem: PLUGIN_ITEM, action: 'install' | 'brower_install_killBrower' = 'install') => {
    switch (pluginItem.type) {
      case BROWSER_LIST.CHROME:
      case BROWSER_LIST.EDGE:
      case BROWSER_LIST.FIREFOX:
      case BROWSER_LIST['360SE']:
      case BROWSER_LIST['360X']:
        install(pluginItem, action)
        break
      default:
        message.info(t('comingSoon'))
    }
  }

  // 설치전감지브라우저여부실행
  const safeInstallBrowserPlugin = async (pluginItem: PLUGIN_ITEM) => {
    pluginItem.loading = true
    try {
      const { data } = await checkBrowserRunning({ type: pluginItem.type })
      if (data && data.running) {
        pluginItem.loading = false
        killBrowserReinstall(pluginItem)
      }
      else {
        installBrowserPlugin(pluginItem)
      }
    }
    catch (error) {
      console.error('checkBrowserRunning failed: ', error)
      // 출력오류이면브라우저미완료실행, 직선연결설치
      installBrowserPlugin(pluginItem)
    }
  }

  // 있음업데이트의확장일설치
  const pluginUpdateModal = () => {
    const lastUpdateTimestamp = storage.get('browserPluginUpdateTimestamp')
    // 일내부안내일
    if (lastUpdateTimestamp && Date.now() - Number(lastUpdateTimestamp) < 24 * 60 * 60 * 1000) {
      return
    }
    appConfigStore.browserPlugins.forEach((plugin) => {
      if (plugin.isInstall && !plugin.isNewest) {
        NiceModal.show(PluginUpdateModal, {}).then(async () => {
          await installAllUpdateBrowserPlugin()
          appConfigStore.refreshBrowserPluginStatus()
        })
        storage.set('browserPluginUpdateTimestamp', Date.now())
      }
    })
  }

  return {
    pluginList,
    install: installBrowserPlugin,
    installBrowerPlugin: installBrowserPlugin,
    safeInstallBrowserPlugin,
    safeInstallBrowerPlugin: safeInstallBrowserPlugin,
    pluginUpdateModal,
  }
}

export const useBrowerPlugin = useBrowserPlugin
