import fss from 'node:fs'
import fs from 'node:fs/promises'
import path from 'node:path'

import axios from 'axios'
import dotenv from 'dotenv'

const { parsed: envConfig = {} } = dotenv.config()

async function downloadCDNFile(options) {
  const { fileName, cdnUrl, output } = options

  if (!fileName || !output) {
    throw new Error('fileName and output are required.')
  }

  const resolvedOutput = path.resolve('./', output)
  const resolvedOutputFile = path.join(resolvedOutput, fileName)
  const recordFileName = path.join(resolvedOutput, 'cdn-info.json') // CDN 정보기록파일

  await fs.mkdir(resolvedOutput, { recursive: true })

  if (!cdnUrl) {
    if (fss.existsSync(resolvedOutputFile)) {
      console.warn(`ICONS_CDN is not set. Using vendored ${resolvedOutputFile}.`)
      return
    }
    throw new Error(`ICONS_CDN is not set and ${resolvedOutputFile} does not exist.`)
  }

  try {
    const shouldDownload = await checkFileNeedDownload(
      cdnUrl,
      resolvedOutputFile,
      recordFileName,
    )
    if (shouldDownload) {
      await downloadFile(cdnUrl, resolvedOutputFile)
      await recordCDNInfo(cdnUrl, fileName, recordFileName) // 기록 CDN 정보
    }
    else {
      console.warn(`CDN file is up-to-date, skipping download.`)
    }
  }
  catch (error) {
    console.error(`Failed to process CDN file: ${error.message}`)
  }
}

async function downloadFile(cdnUrl, outputFile) {
  try {
    const response = await axios({
      method: 'GET',
      url: cdnUrl,
      responseType: 'stream',
      timeout: 5000,
    })

    const writer = fss.createWriteStream(outputFile)
    response.data.pipe(writer)

    return new Promise((resolve, reject) => {
      writer.on('finish', () => {
        console.warn(`Downloaded CDN file from ${cdnUrl} to ${outputFile}`)
        resolve()
      })
      writer.on('error', (error) => {
        fss.unlink(outputFile, () => {
          reject(error)
        })
      })
    })
  }
  catch (error) {
    fss.unlink(outputFile, () => {
      const message = `Failed to download ${cdnUrl}: ${error.message}`
      throw new Error(message)
    })
  }
}

async function checkFileNeedDownload(cdnUrl, outputFile, recordFileName) {
  try {
    let cdnInfo = null
    try {
      const recordFileContent = await fs.readFile(recordFileName, 'utf-8')
      cdnInfo = JSON.parse(recordFileContent)
    }
    catch {
      // 파일아니오저장에서또는파싱실패, 테이블다운로드
      return true
    }

    return (
      cdnInfo.cdnUrl !== cdnUrl
      || cdnInfo.fileName !== path.basename(outputFile)
    )
  }
  catch (error) {
    console.error(`Error checking CDN info: ${error.message}`)
    return false
  }
}

async function recordCDNInfo(cdnUrl, fileName, recordFileName) {
  const cdnInfo = {
    cdnUrl,
    fileName,
  }
  await fs.writeFile(recordFileName, JSON.stringify(cdnInfo, null, 2), 'utf-8')
}

downloadCDNFile({
  fileName: 'iconpark.js',
  cdnUrl: envConfig.SHOPRPA_ICONS_CDN || envConfig.ICONS_CDN,
  output: 'lib',
})
