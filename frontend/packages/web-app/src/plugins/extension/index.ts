import { getExtensions, initializePluginManager, loadPlugins } from './utils'

type Extensions = ReturnType<typeof getExtensions>

class ExtensionManager {
  public extensions: Extensions | null = null
  private initPromise: Promise<Extensions | null> | null = null

  async init(): Promise<Extensions | null> {
    if (this.extensions) {
      return this.extensions
    }

    this.initPromise ??= this.load()
    return this.initPromise
  }

  private async load(): Promise<Extensions | null> {
    try {
      await initializePluginManager()

      try {
        await loadPlugins()
      }
      catch (error) {
        console.warn('[pluginManager] Optional plugin load failed:', error)
      }

      this.extensions = getExtensions()
      return this.extensions
    }
    catch (error) {
      console.error('[pluginManager] Plugin manager initialization failed:', error)
      this.initPromise = null
      return null
    }
  }
}

export default new ExtensionManager()
