import Seven from 'node-7z'

import logger from './log'
import { d7zrPath } from './path'

export function extract7z(archivePath: string, outputDir: string, callback?: (percent: number) => void) {
  let prePercent: number = 0

  return new Promise<void>((resolve, reject) => {
    const myStream = Seven.extractFull(archivePath, outputDir, {
      $progress: true,
      $bin: d7zrPath,
    })

    myStream.on('progress', (progress) => {
      if (progress.percent === prePercent)
        return

      prePercent = progress.percent
      callback?.(prePercent)
    })

    myStream.on('end', () => {
      logger.info(`${archivePath} 압축 해제완료`)
      resolve()
    })

    myStream.on('error', (error) => {
      logger.error(`압축 해제오류: ${error}`)
      reject(error)
    })
  })
}
