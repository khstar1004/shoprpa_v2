import type { IPluginManagerConfig } from './manager'
import { PluginManager } from './manager'

/**
 * 확장관리관리기기
 */
let pluginManager: PluginManager | null = null

/**
 * 확장관리관리기기
 */
export async function initializePluginManager(config: Partial<IPluginManagerConfig> = {}) {
  const managerConfig: IPluginManagerConfig = {
    apiBaseUrl: 'http://localhost:5002',
    cacheTime: 5 * 60 * 1000,
    maxConcurrentLoads: 3,
    timeout: 30000,
    enableCache: true,
    ...config,
  }

  pluginManager = new PluginManager(managerConfig)
  setupPluginManagerEvents()
  console.log('[pluginManager] Plugin manager initialized')
  return pluginManager
}

/**
 * 확장관리관리기기파일
 */
function setupPluginManagerEvents() {
  if (!pluginManager)
    return

  pluginManager.on('plugin:install', (instance) => {
    console.log(`[pluginManager] Plugin installed: ${instance.config.name}`)
  })

  pluginManager.on('plugin:uninstall', (instance) => {
    console.log(`[pluginManager] Plugin uninstalled: ${instance.config.name}`)
  })

  pluginManager.on('plugin:activate', (instance) => {
    console.log(`[pluginManager] Plugin activated: ${instance.config.name}`)
  })

  pluginManager.on('plugin:deactivate', (instance) => {
    console.log(`[pluginManager] Plugin deactivated: ${instance.config.name}`)
  })

  pluginManager.on('plugin:load', (instance) => {
    console.log(`[pluginManager] Plugin loaded: ${instance.config.name}`)
  })

  pluginManager.on('plugin:error', (instance, error) => {
    console.error(`[pluginManager] Plugin error: ${instance.config.name}`, error)
  })
}

/**
 * 가져오기확장관리관리기기
 */
export function getPluginManager(): PluginManager {
  if (!pluginManager) {
    throw new Error('PluginManager not initialized. Call initializePluginManager() first.')
  }
  return pluginManager
}

/**
 * 로드확장
 */
export async function loadPlugins() {
  try {
    const manager = getPluginManager()

    // 가져오기확장목록
    const pluginConfigs = await manager.getPluginList()
    console.log(`[pluginManager] Found ${pluginConfigs.length} plugins`)

    // 설치확장
    const installedPlugins = await manager.installPlugins(pluginConfigs)
    console.log(`[pluginManager] Installed ${installedPlugins.length} plugins`)

    // 로드확장
    const instanceIds = installedPlugins.map(plugin => plugin.instanceId)
    const loadedPlugins = await manager.loadPlugins(instanceIds)
    console.log(`[pluginManager] Loaded ${loadedPlugins.length} plugins`)

    // 확장
    for (const plugin of loadedPlugins) {
      try {
        await manager.activatePlugin(plugin.instanceId)
      }
      catch (error) {
        console.error(`[pluginManager] Failed to activate plugin ${plugin.config.name}:`, error)
      }
    }

    return loadedPlugins
  }
  catch (error) {
    console.error('[pluginManager] Failed to load plugins:', error)
    throw error
  }
}

/**
 * 확장
 */
export async function activatePlugin(instanceId: string) {
  try {
    const manager = getPluginManager()
    await manager.activatePlugin(instanceId)
    console.log(`[pluginManager] Plugin activated: ${instanceId}`)
  }
  catch (error) {
    console.error(`[pluginManager] Failed to activate plugin ${instanceId}:`, error)
    throw error
  }
}

/**
 * 중지사용확장
 */
export async function deactivatePlugin(instanceId: string) {
  try {
    const manager = getPluginManager()
    await manager.deactivatePlugin(instanceId)
    console.log(`[pluginManager] Plugin deactivated: ${instanceId}`)
  }
  catch (error) {
    console.error(`[pluginManager] Failed to deactivate plugin ${instanceId}:`, error)
    throw error
  }
}

/**
 * 가져오기모든확장
 */
export function getAllPlugins() {
  const manager = getPluginManager()
  return manager.getAllPlugins()
}

/**
 * 가져오기완료로드의확장
 */
export function getLoadedPlugins() {
  const manager = getPluginManager()
  return manager.getLoadedPlugins()
}

/**
 * 조회확장여부완료설치
 */
export function isPluginInstalled(instanceId: string) {
  const manager = getPluginManager()
  return manager.isPluginInstalled(instanceId)
}

/**
 * 조회확장여부완료로드
 */
export function isPluginLoaded(instanceId: string) {
  const manager = getPluginManager()
  return manager.isPluginLoaded(instanceId)
}

/**
 * 가져오기 
 */
export function getExtensions() {
  const manager = getPluginManager()
  return manager.getExtensions()
}

/**
 * 판매확장관리관리기기
 */
export async function destroyPluginManager() {
  if (pluginManager) {
    await pluginManager.destroy()
    pluginManager = null
  }
  console.log('[pluginManager] Plugin manager destroyed')
}