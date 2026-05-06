import log from 'electron-log'
import { join } from 'node:path'

import { appWorkPath } from './path'

// 매칭 electron-log 의로그파일 경로
log.transports.file.resolvePathFn = () => {
  return join(appWorkPath, 'logs', 'main.log')
}

const logger = log

export default logger