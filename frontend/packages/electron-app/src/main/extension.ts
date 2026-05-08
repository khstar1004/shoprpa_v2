import fs from 'node:fs'
import path from 'node:path'

import type { IPluginConfig } from '@rpa/shared'
import to from 'await-to-js'

import logger from './log'
import { extensionPath } from './path'

interface MainManifest {
  id: string
  name: string
  metaData: {
    buildInfo: { buildVersion: string }
    publicPath: string
  }
}

interface IExtension {
  name: string
  resourcePath: string
  config: IPluginConfig
}

// eslint-disable-next-line import/no-mutable-exports
export let extensions: IExtension[] = []

/**
 * Discover extension packages from every configured extension directory.
 */
export async function init() {
  const validExtensions: IExtension[] = []

  const promises = extensionPath.map(async (basePath) => {
    const [errStats, stats] = await to(fs.promises.stat(basePath))
    if (errStats || !stats.isDirectory()) {
      return
    }

    const [errItems, items] = await to(fs.promises.readdir(basePath))
    if (errItems) {
      logger.error(`Error reading extension directory ${basePath}:`, errItems)
      return
    }

    const itemPromises = items.map(async (item) => {
      const itemPath = path.join(basePath, item)

      const [errItemStats, itemStats] = await to(fs.promises.stat(itemPath))
      if (errItemStats || !itemStats.isDirectory())
        return

      const manifestJsonPath = path.join(itemPath, 'mf-manifest.json')

      const [errManifestStats, manifestStats] = await to(fs.promises.stat(manifestJsonPath))
      if (errManifestStats || !manifestStats.isFile())
        return

      const [errContent, content] = await to(fs.promises.readFile(manifestJsonPath, 'utf-8'))
      if (errContent)
        return

      try {
        const manifest: MainManifest = JSON.parse(content)
        const resourcePath = itemPath
        const entryUrl = `${manifest.metaData.publicPath}mf-manifest.json`

        validExtensions.push({
          name: manifest.name,
          resourcePath,
          config: {
            name: manifest.name,
            version: manifest.metaData.buildInfo.buildVersion,
            entry: entryUrl,
            enabled: true,
          },
        })
      }
      catch (err) {
        logger.error(`Error parsing manifest at ${manifestJsonPath}:`, err)
      }
    })

    await Promise.all(itemPromises)
  })

  await Promise.all(promises)

  extensions = validExtensions
}

init()

export const loadExtensions = () => extensions.map(it => it.config)

export function getExtensionResourcePath(name: string) {
  const extension = extensions.find(it => it.name === name)
  return extension?.resourcePath || ''
}
