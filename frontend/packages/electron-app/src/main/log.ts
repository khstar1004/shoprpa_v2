import { join } from 'node:path'

import log from 'electron-log'

import { appWorkPath } from './path'

// Store Electron logs under the active app work directory.
log.transports.file.resolvePathFn = () => {
  return join(appWorkPath, 'logs', 'main.log')
}

const logger = log

export default logger
