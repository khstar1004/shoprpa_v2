import type { Component } from 'vue'

/**
 * 확장매칭연결
 */
export interface IPluginConfig {
  /** 확장이름 */
  name: string
  /** 확장버전 */
  version: string
  /** 확장설명 */
  description?: string
  /** 확장입력URL */
  entry: string
  /** 확장여부사용 */
  enabled?: boolean
  /** 확장 */
  contributes?: IPluginContributes
}

/**
 * 확장모듈연결
 */
export interface IPluginModule {
  /** 확장데이터 */
  activate?: (context: IPluginContext) => Promise<void> | void
  /** 확장중지사용데이터 */
  deactivate?: () => Promise<void> | void
  /** 확장 */
  contributes?: IPluginContributes
}

/**
 * 확장연결
 */
export interface IPluginContributes {
  /** 페이지 */
  settingsTabs?: ISettingsTabContribution[]
}

/**
 * 페이지Tab연결
 */
export interface ISettingsTabContribution {
  /** Tab ID */
  id: string
  /** Tab제목 */
  title: string
  /** Tab아이콘 */
  icon: string
  /** Tab순서 */
  order?: number
  /** Tab내용 */
  content: Component
}

/**
 * 확장위아래문서연결
 */
export interface IPluginContext {
  /** 확장정보 */
  extension: IPluginExtension
  /** 주문합치기 */
  subscriptions: IPluginSubscription[]
  /** 회원가입기기 */
  settings: IPluginSettings
}

/**
 * 확장연결
 */
export interface IPluginExtension {
  /** ID */
  id: string
  /** 경로 */
  extensionPath: string
  /** 여부 */
  isActive: boolean
  /** 패키지JSON */
  packageJSON: any
}

/**
 * 확장주문연결
 */
export interface IPluginSubscription {
  /** 가져오기 주문 */
  dispose: () => void
}

/**
 * 회원가입기기연결
 */
export interface IPluginSettings {
  /** 회원가입Tab */
  registerSettingsTab: (tab: ISettingsTabContribution) => IPluginSubscription
  /** 비고판매Tab */
  unregisterSettingsTab: (id: string) => void
  /** 가져오기모든Tab */
  getSettingsTabs: () => ISettingsTabContribution[]
}