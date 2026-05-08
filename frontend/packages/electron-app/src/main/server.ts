import { exec } from 'node:child_process'
import fs from 'node:fs/promises'
import { join } from 'node:path'

import { to } from 'await-to-js'

import { toUnicode } from '../common'

import { envJson } from './env'
import { mainToRender } from './event'
import { extract7z } from './file'
import logger from './log'
import { appWorkPath, confPath, pythonExe, resourcePath } from './path'
import { getMainWindow } from './window'

process.on('uncaughtException', (err) => {
  logger.error(`uncaughtException: ${err.message}`)
})

function sendToRender(message: string, percent: number) {
  const unicodeMessage = `{"type":"sync","msg":{"msg":"${toUnicode(message)}","step":${percent}}}`
  mainToRender('scheduler-event', unicodeMessage, undefined, true)
}

async function getConfiguredRoutePort() {
  try {
    const conf = await fs.readFile(confPath, 'utf-8')
    const remoteAddr = conf.match(/remote_addr:\s*(.+)/)?.[1]?.trim()
    if (remoteAddr) {
      const url = new URL(remoteAddr)
      return Number(url.port) || 32742
    }
  }
  catch (error) {
    logger.warn('Failed to read configured remote_addr, using Docker gateway port 32742', error)
  }
  return 32742
}

async function sendBackendReady() {
  const routePort = await getConfiguredRoutePort()
  const message = `{"type":"sync_cancel","msg":{"route_port":${routePort}}}`
  logger.info(`Using external backend gateway on port ${routePort}`)
  setTimeout(() => mainToRender('scheduler-event', message, undefined, true), 1100)
}

/**
 * Python scheduler process is running.
 */
export function checkPythonRpaProcess() {
  return new Promise((resolve) => {
    // On Linux/macOS, check whether the scheduler module is in the Python command line.
    if (process.platform !== 'win32') {
      exec(`ps aux | grep "${envJson.SCHEDULER_NAME}"`, (error, stdout) => {
        if (error) {
          return resolve(false)
        }
        const isRunning = stdout.trim() !== ''
        resolve(isRunning)
      })
    }
    else {
      exec('tasklist /v /fi "imagename eq python.exe"', (error, stdout) => {
        if (error) {
          logger.error(`tasklist error: ${error}`)
          return resolve(false)
        }
        const isRunning = stdout.includes(envJson.SCHEDULER_NAME)
        resolve(isRunning)
      })
    }
  })
}

/**
 * Start the local scheduler service.
 */
export async function startServer() {
  // Skip startup if the scheduler is already running.
  const isRunning = await checkPythonRpaProcess()
  if (isRunning) {
    logger.info(`${envJson.SCHEDULER_NAME} is running`)
    return
  }

  logger.info('로컬 서비스 시작')
  sendToRender('로컬 서비스를 시작하는 중...', 90)

  const rpaSetup = exec(
    `"${pythonExe}" -m ${envJson.SCHEDULER_NAME} --conf="${confPath}"`,
    { cwd: appWorkPath },
    (error) => {
      if (error) {
        logger.error(`${envJson.SCHEDULER_NAME} error: ${error}`)
      }
    },
  )

  rpaSetup.stdout?.on('data', data => msgFilter(data.toString()))

  rpaSetup.stderr?.on('data', (data) => {
    logger.info(`${envJson.SCHEDULER_NAME} stderr: ${data.toString()}`)
  })

  rpaSetup.on('close', (code) => {
    if (code === 0) {
      logger.info(`${envJson.SCHEDULER_NAME} exited successfully.`)
    }
    else {
      logger.error(`${envJson.SCHEDULER_NAME} exited with error code: ${code}`)
    }
  })

  rpaSetup.on('error', (error) => {
    logger.error(`Failed to start ${envJson.SCHEDULER_NAME}: ${error.message}`)
  })
}

/**
 * Stop all scheduler subprocesses.
 */
export function closeSubProcess() {
  return new Promise<void>((resolve) => {
    exec(
      `"${pythonExe}" -m ${envJson.SCHEDULER_NAME} --stop="True"`,
      { cwd: appWorkPath },
      (error) => {
        if (error) {
          logger.error(`${envJson.SCHEDULER_NAME} closeSubProcess error: ${error}`)
        }
        else {
          logger.info(`${envJson.SCHEDULER_NAME} closeSubProcess success`)
        }
        resolve()
      },
    )
  })
}

/**
 * Forward scheduler messages emitted by Python to the renderer.
 * @param msg - Raw message from Python stdout.
 */
function msgFilter(msg: string) {
  const win = getMainWindow()
  // Forward messages that use the ||emit|| protocol marker.
  const match = msg.match(/\|\|emit\|\|(.*)/)
  if (match && win) {
    const message = match[1].trim().replaceAll('"', '')
    logger.info(`${envJson.SCHEDULER_NAME} message: `, message)
    win.webContents.send('scheduler-event', message)
  }
}

/**
 * Find Python runtime archives that may need extraction.
 * @returns Python archive file names.
 */
async function checkNeedExtractPythonPackage() {
  if (process.platform === 'win32') {
    const fileNames = await fs.readdir(resourcePath)
    return fileNames.filter(fileName => fileName.endsWith('.7z'))
  }
  logger.info('No python package in resources for non-windows platform')
  return []
}

/**
 * Read a hash file.
 * @param hashFilePath - Hash file path.
 * @returns Hash value.
 */
async function readHashFile(hashFilePath: string): Promise<string> {
  try {
    const hashContent = await fs.readFile(hashFilePath, 'utf-8')
    return hashContent.trim()
  }
  catch (error) {
    logger.error(`해시 파일을 읽지 못했습니다: ${hashFilePath}`, error)
    throw error
  }
}

/**
 * Check whether one archive needs to be extracted again.
 * @param packageFile - Archive file name.
 * @returns True when re-extraction is required.
 */
async function checkSingleFile(packageFile: string): Promise<boolean> {
  const archiveName = packageFile.replace('.7z', '')
  const archivePath = join(appWorkPath, archiveName)
  const hashFileName = `${packageFile}.sha256.txt`
  const resourceHashPath = join(resourcePath, hashFileName)
  const appWorkHashPath = join(appWorkPath, hashFileName)

  logger.info(`패키지 파일 확인: ${packageFile}`)

  try {
    // 1. Make sure the resource hash file exists.
    await fs.access(resourceHashPath)

    // 2. Check extracted runtime and copied hash file.
    const [archiveExists, hashFileExists] = await Promise.all([
      fs.access(archivePath).then(() => true).catch(() => false),
      fs.access(appWorkHashPath).then(() => true).catch(() => false),
    ])

    // 3. Re-extract when the runtime folder does not exist.
    if (!archiveExists) {
      logger.warn(`압축 해제된 런타임이 없습니다: ${packageFile}`)
      return true
    }

    // 4. Re-extract when the copied hash file is missing.
    if (!hashFileExists) {
      logger.warn(`사용자 데이터 디렉터리에 해시 파일이 없습니다: ${packageFile}`)
      return true
    }

    // 5. Read both hash files.
    const [resourceHash, appWorkHash] = await Promise.all([
      readHashFile(resourceHashPath),
      readHashFile(appWorkHashPath),
    ])

    // 6. Re-extract when either hash file is empty.
    if (!resourceHash || !appWorkHash) {
      logger.warn(`해시 파일 내용이 비어 있습니다: ${packageFile}`)
      return true
    }

    // 7. Re-extract when hash values do not match.
    if (resourceHash !== appWorkHash) {
      logger.warn(`해시가 일치하지 않습니다: ${packageFile}`)
      return true
    }
    else {
      logger.info(`해시 검증 완료: ${packageFile}`)
    }

    return false
  }
  catch (error) {
    logger.error(`압축 해제 상태 확인 실패: ${packageFile}`, error)
    return true
  }
}

/**
 * Determine which archives need extraction based on extracted files and hashes.
 * @param packageFiles - Archive file names to check.
 * @returns Archive file names that need extraction.
 */
async function checkAndCleanExtractedFiles(packageFiles: string[]): Promise<string[]> {
  const needExtractFiles: string[] = []

  for (const packageFile of packageFiles) {
    const needExtract = await checkSingleFile(packageFile)
    if (needExtract) {
      needExtractFiles.push(packageFile)
    }
  }

  logger.info(`다시 압축 해제가 필요한 파일 수: ${needExtractFiles.length}`)
  return needExtractFiles
}

/**
 * Copy one resource file to the app work directory.
 * @param fileName - File name.
 * @returns Copy result.
 */
async function copySingleFile(fileName: string): Promise<boolean> {
  const sourcePath = join(resourcePath, fileName)
  const targetPath = join(appWorkPath, fileName)

  try {
    // Make sure the source file exists.
    await fs.access(sourcePath)
    logger.info(`복사파일: ${fileName}`)
    await fs.copyFile(sourcePath, targetPath)
    return true
  }
  catch (error) {
    logger.error(`파일 복사 실패: ${fileName}`, error)
    throw error
  }
}

/**
 * Ensure the app work directory exists.
 */
async function ensureAppWorkPathExists(): Promise<void> {
  try {
    await fs.access(appWorkPath)
  }
  catch {
    logger.info(`사용자 데이터 디렉터리 생성: ${appWorkPath}`)
    await fs.mkdir(appWorkPath, { recursive: true })
  }
}

/**
 * Start the backend runtime used by the desktop app.
 */
export async function startBackend() {
  if (globalThis.serverRunning)
    return

  sendToRender('초기화 중...', 10)

  // Check whether the scheduler is already running.
  const isRunning = await checkPythonRpaProcess()
  if (isRunning) {
    logger.info('rpa is already running')
    return
  }

  // Find Python runtime packages in the resources directory.
  const packageFiles = await checkNeedExtractPythonPackage()

  // If no archive is bundled, use the configured external backend.
  if (packageFiles.length === 0) {
    const hasPythonRuntime = await fs.access(pythonExe).then(() => true).catch(() => false)
    if (!hasPythonRuntime) {
      logger.warn(`Python runtime not found at ${pythonExe}; using configured external backend.`)
      globalThis.serverRunning = true
      await sendBackendReady()
      return
    }
    logger.info('처리할 Python 패키지가 없습니다')
    startServer()
    return
  }

  logger.info(`확인할 Python 패키지: ${packageFiles.join(', ')}`)

  // Check whether any Python package needs extraction.
  const needExtractFiles = await checkAndCleanExtractedFiles(packageFiles)

  // Start the service immediately when every package is already ready.
  if (needExtractFiles.length === 0) {
    logger.info('모든 Python 패키지가 준비되어 있어 압축 해제가 필요하지 않습니다')
    startServer()
    return
  }

  await ensureAppWorkPathExists()

  logger.info(`압축 해제가 필요한 파일: ${needExtractFiles.join(', ')}`)

  const preStep = 30
  const singlePercentStep = (90 - preStep) / needExtractFiles.length
  sendToRender('Python 패키지를 압축 해제하는 중...', preStep)

  // Extract all required archives.
  await Promise.allSettled(needExtractFiles.map(file => extractAndCleanFile(file, (percent) => {
    const newStep = preStep + (percent / 100 * singlePercentStep)
    sendToRender('압축 해제중...', newStep)
  })))

  startServer()
}

/**
 * Extract one archive and replace the old runtime directory.
 * @param fileName - Archive file name.
 */
async function extractAndCleanFile(fileName: string, percentCallback: (percent: number) => void): Promise<void> {
  const archivePath = join(resourcePath, fileName)
  const outputDir = join(appWorkPath, fileName.replace('.7z', ''))
  const tempOutputDir = `${outputDir}.temp`

  // 1. Remove old and temporary directories.
  const [error] = await to(Promise.all([
    fs.rm(tempOutputDir, { recursive: true, force: true }),
    fs.rm(outputDir, { recursive: true, force: true }),
  ]))
  if (error) {
    logger.error(`기존 압축 해제 디렉터리를 정리하지 못했습니다: ${error}`)
    return
  }
  logger.info('기존 압축 해제 디렉터리 삭제 완료')

  // 2. Extract to a temporary directory.
  logger.info(`임시 디렉터리에 압축 해제 시작: ${tempOutputDir}`)
  await extract7z(archivePath, tempOutputDir, percentCallback)

  // 3. Move the temporary directory into place.
  logger.info(`압축 해제 디렉터리 적용: ${outputDir}`)
  await fs.rename(tempOutputDir, outputDir)

  // 4. Copy the hash file.
  await copySingleFile(`${fileName}.sha256.txt`)
}
