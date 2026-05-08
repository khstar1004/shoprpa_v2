// packages/web-app/src/components/GlobalModal/index.ts
import { Modal as AntModal } from 'ant-design-vue'

import GlobalModalComponent from './index.vue'

// 매칭
const defaultConfig = {
  centered: true,
  keyboard: false,
}

// 생성매칭의방법법
const staticMethods = {
  confirm: (config: any) => AntModal.confirm({ ...defaultConfig, ...config }),
  info: (config: any) => AntModal.info({ ...defaultConfig, ...config }),
  success: (config: any) => AntModal.success({ ...defaultConfig, ...config }),
  error: (config: any) => AntModal.error({ ...defaultConfig, ...config }),
  warning: (config: any) => AntModal.warning({ ...defaultConfig, ...config }),
  warn: (config: any) => AntModal.warning({ ...defaultConfig, ...config }),
  destroyAll: AntModal.destroyAll,
  config: AntModal.config,
}

// 병합컴포넌트및방법법
const GlobalModal = Object.assign(GlobalModalComponent, staticMethods)

export default GlobalModal
