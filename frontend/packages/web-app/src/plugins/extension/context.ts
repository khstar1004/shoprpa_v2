import type {
  IPluginContext,
  IPluginExtension,
  IPluginSubscription,
} from '@rpa/shared'

import { PluginSettings, settingsExtension } from './extensions/settings'

/**
 * 확장
 */
export class PluginExtension implements IPluginExtension {
  constructor(
    public id: string,
    public extensionPath: string,
    public isActive: boolean,
    public packageJSON: any,
  ) {}
}

/**
 * 확장주문
 */
export class PluginSubscription implements IPluginSubscription {
  private disposed = false

  constructor(private disposeFn: () => void) {}

  dispose(): void {
    if (!this.disposed) {
      this.disposeFn()
      this.disposed = true
    }
  }
}

/**
 * 확장위아래문서관리관리기기
 */
export class PluginContextManager {
  private static instance: PluginContextManager

  private constructor() {}

  /**
   * 가져오기단일
   */
  static getInstance(): PluginContextManager {
    if (!this.instance) {
      this.instance = new PluginContextManager()
    }
    return this.instance
  }

  /**
   * 생성확장위아래문서
   */
  createContext(extension: IPluginExtension): IPluginContext {
    const subscriptions: IPluginSubscription[] = []

    return {
      extension,
      subscriptions,
      settings: new PluginSettings(),
    }
  }

  /**
   * 가져오기 (사용시스템단계)
   */
  getExtensions() {
    return {
      settings: settingsExtension,
    }
  }

  /**
   * 빈모든
   */
  clearAll(): void {
    settingsExtension.clear()
  }

  /**
   * 가져오기 시스템계획정보
   */
  getStats() {
    return {
      settingsTabs: settingsExtension.size(),
    }
  }
}

/**
 * 의데이터
 */
export function createPluginContext(extension: IPluginExtension): IPluginContext {
  return PluginContextManager.getInstance().createContext(extension)
}

/**
 * 가져오기확장위아래문서관리관리기기
 */
export function getPluginContextManager(): PluginContextManager {
  return PluginContextManager.getInstance()
}
