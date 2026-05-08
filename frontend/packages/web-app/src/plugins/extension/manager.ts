import type { ModuleFederation } from '@module-federation/runtime'
import type {
  IPluginConfig,
  IPluginContext,
  IPluginContributes,
  IPluginModule,
} from '@rpa/shared'
import to from 'await-to-js'
import { EventEmitter } from 'eventemitter3'
import * as Pinia from 'pinia'
import * as Vue from 'vue'

import { utilsManager } from '@/platform'

import { createPluginContext, getPluginContextManager, PluginExtension } from './context'

function logPlugin(...args: unknown[]) {
  if (import.meta.env.DEV) {
    console.debug(...args)
  }
}

/**
 * 확장연결
 */
export interface IPluginInstance {
  /** 확장매칭 */
  config: IPluginConfig
  /** 확장모듈 */
  module?: IPluginModule
  /** 확장상태 */
  status: 'unloaded' | 'loading' | 'loaded' | 'failed'
  /** 확장오류정보 */
  error?: string
  /** 확장ID */
  instanceId: string
  /** 확장여부완료 */
  isActivated: boolean
}

/**
 * 확장관리관리기기매칭
 */
export interface IPluginManagerConfig {
  /** 확장APIURL */
  apiBaseUrl: string
  /** 확장저장시간(초) */
  cacheTime?: number
  /** 대발송로드데이터 */
  maxConcurrentLoads?: number
  /** 여부사용확장저장 */
  enableCache?: boolean
  /** 확장시간 초과시간(초) */
  timeout?: number
}

/**
 * 확장관리관리기기파일
 */
export interface IPluginManagerEvents {
  /** 확장설치파일 */
  'plugin:install': (instance: IPluginInstance) => void
  /** 확장파일 */
  'plugin:uninstall': (instance: IPluginInstance) => void
  /** 확장파일 */
  'plugin:activate': (instance: IPluginInstance) => void
  /** 확장중지사용파일 */
  'plugin:deactivate': (instance: IPluginInstance) => void
  /** 확장로드파일 */
  'plugin:load': (instance: IPluginInstance) => void
  /** 확장오류파일 */
  'plugin:error': (instance: IPluginInstance, error: Error) => void
}

/**
 * 확장관리관리기기
 */
export class PluginManager extends EventEmitter<IPluginManagerEvents> {
  private plugins = new Map<string, IPluginInstance>()
  private config: Required<IPluginManagerConfig>
  private loadingQueue: Set<string> = new Set()
  private cache = new Map<string, { data: IPluginConfig[], timestamp: number }>()
  private mf: ModuleFederation | null = null

  constructor(config: IPluginManagerConfig) {
    super()

    this.config = {
      apiBaseUrl: config.apiBaseUrl,
      cacheTime: config.cacheTime ?? 5 * 60 * 1000, // 5분
      maxConcurrentLoads: config.maxConcurrentLoads ?? 3,
      timeout: config.timeout ?? 30000, // 30초
      enableCache: config.enableCache ?? true,
    }
  }

  private async getFederationRuntime(): Promise<ModuleFederation> {
    if (this.mf) {
      return this.mf
    }

    const { createInstance } = await import('@module-federation/runtime')
    this.mf = createInstance({
      name: 'host',
      remotes: [],
      shared: {
        vue: {
          version: __VUE_VERSION__,
          scope: 'default',
          lib: () => Vue,
          shareConfig: {
            singleton: true,
            requiredVersion: '^3.5.0',
          },
        },
        pinia: {
          version: __PINIA_VERSION__,
          scope: 'default',
          lib: () => Pinia,
          shareConfig: {
            singleton: true,
            requiredVersion: '^3.0.0',
          },
        },
      },
    })
    return this.mf
  }

  /**
   * 가져오기확장목록
   */
  async getPluginList(): Promise<IPluginConfig[]> {
    const cacheKey = 'plugin-list'

    // 조회저장
    if (this.config.enableCache) {
      const cached = this.cache.get(cacheKey)
      if (cached && Date.now() - cached.timestamp < this.config.cacheTime) {
        return cached.data
      }
    }

    try {
      const [error, plugins] = await to(utilsManager.getPluginList())

      if (error) {
        throw new Error(`Failed to fetch plugins: ${error.message}`)
      }

      // 저장결과
      if (this.config.enableCache) {
        this.cache.set(cacheKey, {
          data: plugins,
          timestamp: Date.now(),
        })
      }

      return plugins
    }
    catch (error) {
      console.error('Failed to fetch plugin list:', error)
      throw error
    }
  }

  /**
   * 설치확장
   */
  async installPlugin(config: IPluginConfig): Promise<IPluginInstance> {
    const instanceId = `${config.name}@${config.version}`

    // 조회여부완료설치
    if (this.plugins.has(instanceId)) {
      return this.plugins.get(instanceId)!
    }

    const instance: IPluginInstance = {
      config,
      status: 'unloaded',
      instanceId,
      isActivated: false,
    }

    this.plugins.set(instanceId, instance)
    this.emit('plugin:install', instance)

    return instance
  }

  /**
   * 확장
   */
  async uninstallPlugin(instanceId: string): Promise<void> {
    const instance = this.plugins.get(instanceId)
    if (!instance) {
      throw new Error(`Plugin ${instanceId} not found`)
    }

    // 예결과확장완료, 중지사용
    if (instance.isActivated) {
      await this.deactivatePlugin(instanceId)
    }

    this.plugins.delete(instanceId)
    this.emit('plugin:uninstall', instance)
  }

  /**
   * 로드확장
   */
  async loadPlugin(instanceId: string): Promise<IPluginInstance> {
    const instance = this.plugins.get(instanceId)
    if (!instance) {
      throw new Error(`Plugin ${instanceId} not found`)
    }

    if (instance.status === 'loaded') {
      return instance
    }

    if (instance.status === 'loading') {
      // 대기로드완료
      return new Promise((resolve, reject) => {
        const checkStatus = () => {
          const currentInstance = this.plugins.get(instanceId)
          if (!currentInstance) {
            reject(new Error(`Plugin ${instanceId} not found`))
            return
          }

          if (currentInstance.status === 'loaded') {
            resolve(currentInstance)
          }
          else if (currentInstance.status === 'failed') {
            reject(new Error(currentInstance.error || 'Plugin loading failed'))
          }
          else {
            setTimeout(checkStatus, 100)
          }
        }
        checkStatus()
      })
    }

    // 조회발송로드제한제어
    if (this.loadingQueue.size >= this.config.maxConcurrentLoads) {
      throw new Error('Maximum concurrent plugin loads reached')
    }

    this.loadingQueue.add(instanceId)
    instance.status = 'loading'

    try {
      // 매칭
      const mf = await this.getFederationRuntime()
      mf.registerRemotes([{
        name: instance.config.name,
        entry: instance.config.entry,
      }])

      // 가져오기 모듈
      const remoteModule = await Promise.race([
        mf.loadRemote(`${instance.config.name}/index`).then((res: any) => res.default),
        new Promise((_, reject) =>
          setTimeout(() => reject(new Error('Plugin loading timeout')), this.config.timeout),
        ),
      ]) as any

      instance.module = remoteModule
      instance.status = 'loaded'
      instance.error = undefined

      this.emit('plugin:load', instance)

      return instance
    }
    catch (error) {
      instance.status = 'failed'
      instance.error = error instanceof Error ? error.message : String(error)
      this.emit('plugin:error', instance, error instanceof Error ? error : new Error(String(error)))
      throw error
    }
    finally {
      this.loadingQueue.delete(instanceId)
    }
  }

  /**
   * 확장
   */
  async activatePlugin(instanceId: string): Promise<void> {
    const instance = this.plugins.get(instanceId)
    if (!instance) {
      throw new Error(`Plugin ${instanceId} not found`)
    }

    if (instance.isActivated) {
      logPlugin(`[PluginManager] Plugin ${instance.config.name} is already activated`)
      return
    }

    if (!instance.module) {
      throw new Error(`Plugin ${instance.config.name} module not loaded`)
    }

    try {
      logPlugin(`[PluginManager] Activating plugin ${instance.config.name}`)

      // 생성확장위아래문서
      const extension = new PluginExtension(
        instance.config.name,
        instance.config.entry,
        false,
        instance.config,
      )

      const context = createPluginContext(extension)

      // 실행확장데이터
      if (instance.module.activate) {
        await instance.module.activate(context)
      }

      // 회원가입확장
      if (instance.module.contributes) {
        this.registerContributions(instance.config.name, instance.module.contributes, context)
      }

      // 로완료
      instance.isActivated = true
      this.emit('plugin:activate', instance)

      logPlugin(`[PluginManager] Plugin ${instance.config.name} activated successfully`)
    }
    catch (error) {
      console.error(`[PluginManager] Failed to activate plugin ${instance.config.name}:`, error)
      throw error
    }
  }

  /**
   * 중지사용확장
   */
  async deactivatePlugin(instanceId: string): Promise<void> {
    const instance = this.plugins.get(instanceId)
    if (!instance) {
      throw new Error(`Plugin ${instanceId} not found`)
    }

    if (!instance.isActivated) {
      logPlugin(`[PluginManager] Plugin ${instance.config.name} is not activated`)
      return
    }

    try {
      logPlugin(`[PluginManager] Deactivating plugin ${instance.config.name}`)

      // 실행확장중지사용데이터
      if (instance.module?.deactivate) {
        await instance.module.deactivate()
      }

      // 비고판매확장
      if (instance.module?.contributes) {
        this.unregisterContributions(instance.config.name, instance.module.contributes)
      }

      // 로미완료
      instance.isActivated = false
      this.emit('plugin:deactivate', instance)

      logPlugin(`[PluginManager] Plugin ${instance.config.name} deactivated successfully`)
    }
    catch (error) {
      console.error(`[PluginManager] Failed to deactivate plugin ${instance.config.name}:`, error)
      throw error
    }
  }

  /**
   * 회원가입확장
   */
  private registerContributions(_pluginId: string, contributes: IPluginContributes, context: IPluginContext): void {
    // const extensions = PluginContextFactory.getExtensions()

    // 회원가입Tab
    if (contributes.settingsTabs) {
      contributes.settingsTabs.forEach((tab) => {
        context.settings.registerSettingsTab(tab)
      })
    }
  }

  /**
   * 비고판매확장
   */
  private unregisterContributions(pluginId: string, _contributes: any): void {
    // 가능으로비고판매
    logPlugin(`[PluginManager] Unregistering contributions for plugin: ${pluginId}`)
  }

  /**
   * 량설치확장
   */
  async installPlugins(configs: IPluginConfig[]): Promise<IPluginInstance[]> {
    const instances: IPluginInstance[] = []

    for (const config of configs) {
      try {
        const instance = await this.installPlugin(config)
        instances.push(instance)
      }
      catch (error) {
        console.error(`Failed to install plugin ${config.name}:`, error)
      }
    }

    return instances
  }

  /**
   * 량로드확장
   */
  async loadPlugins(instanceIds: string[]): Promise<IPluginInstance[]> {
    const loadPromises = instanceIds.map(id => this.loadPlugin(id))
    return Promise.allSettled(loadPromises).then(results =>
      results
        .filter((result): result is PromiseFulfilledResult<IPluginInstance> => result.status === 'fulfilled')
        .map(result => result.value),
    )
  }

  /**
   * 가져오기확장
   */
  getPlugin(instanceId: string): IPluginInstance | undefined {
    return this.plugins.get(instanceId)
  }

  /**
   * 가져오기모든확장
   */
  getAllPlugins(): IPluginInstance[] {
    return Array.from(this.plugins.values())
  }

  /**
   * 가져오기완료로드의확장
   */
  getLoadedPlugins(): IPluginInstance[] {
    return this.getAllPlugins().filter(plugin => plugin.status === 'loaded')
  }

  /**
   * 조회확장여부완료설치
   */
  isPluginInstalled(instanceId: string): boolean {
    return this.plugins.has(instanceId)
  }

  /**
   * 조회확장여부완료로드
   */
  isPluginLoaded(instanceId: string): boolean {
    const instance = this.plugins.get(instanceId)
    return instance?.status === 'loaded'
  }

  /**
   * 지우기저장
   */
  clearCache(): void {
    this.cache.clear()
  }

  /**
   * 가져오기
   */
  getExtensions() {
    return getPluginContextManager().getExtensions()
  }

  /**
   * 판매확장관리관리기기
   */
  async destroy(): Promise<void> {
    // 중지사용모든확장
    const instances = this.getAllPlugins()
    for (const instance of instances) {
      try {
        await this.deactivatePlugin(instance.instanceId)
        await this.uninstallPlugin(instance.instanceId)
      }
      catch (error) {
        console.error(`Failed to destroy plugin ${instance.instanceId}:`, error)
      }
    }

    this.plugins.clear()
    this.loadingQueue.clear()
    this.cache.clear()
    this.removeAllListeners()
  }
}
