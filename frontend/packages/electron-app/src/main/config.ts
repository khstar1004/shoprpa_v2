import fs from 'node:fs'

import type { IAppConfig } from '@rpa/shared/platform'
import { nativeImage } from 'electron'
import { parse as parseYAML } from 'yaml'

import appIcon from '../../../../public/icons/icon.ico?asset'

import logger from './log'
import { confPath } from './path'

export const APP_ICON_PATH = nativeImage.createFromPath(appIcon)

export const MAIN_WINDOW_LABEL = 'main'

function loadConfig(): IAppConfig {
  try {
    const yamlData = fs.readFileSync(confPath, { encoding: 'utf-8' })
    return parseYAML(yamlData) as IAppConfig
  }
  catch (error) {
    logger.error(`FATAL: Failed to load config file at ${confPath}. App cannot start.`, error)
    process.exit(1)
  }
}

export const config = loadConfig()
