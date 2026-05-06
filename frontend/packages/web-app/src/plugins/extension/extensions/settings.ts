import type { IPluginSettings, IPluginSubscription, ISettingsTabContribution } from '@rpa/shared'

import { SimpleRegistry } from './_registry'

/**
 * 페이지Tab - 직선연결사용유형, 필요하지 않습니다복사
 */
export const settingsExtension = new SimpleRegistry<ISettingsTabContribution, string>()

/**
 * 회원가입기기
 */
export class PluginSettings implements IPluginSettings {
  constructor() {}

  registerSettingsTab(tab: ISettingsTabContribution): IPluginSubscription {
    return settingsExtension.register(tab.id, tab)
  }

  unregisterSettingsTab(id: string): void {
    settingsExtension.unregister(id)
  }

  getSettingsTabs(): ISettingsTabContribution[] {
    return settingsExtension.getAll()
  }
}