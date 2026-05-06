import { autoUpdater, type UpdateInfo as ElectronUpdateInfo } from "electron-updater"
import { to } from 'await-to-js'
import { app } from 'electron'
import type { UpdateInfo, UpdateManifest } from '@rpa/shared/platform'
import { withTimeout } from '@rpa/shared'

import logger from "./log"
import { mainToRender } from './event'
import { config } from './config'
import urlJoin from './utils';

autoUpdater.logger = logger
// 열기시작후, 가능으로에서열기발송디버그업데이트
autoUpdater.forceDevUpdateConfig = false
// 출력후아니오설치
autoUpdater.autoInstallOnAppQuit = false

const url = urlJoin(
  config.remote_addr,
  '/api/robot/client-version-update/update-check',
  `${process.platform}/${process.arch}/${app.getVersion()}`
)
autoUpdater.setFeedURL(url)

//'error'파일
// autoUpdater.on("error", (err) => {
//   logger.error("출력오류:", err);
// });

//'update-available'파일, 발송있음새버전시트리거
// autoUpdater.on("update-available", () => {
//   logger.info("found new version");
// });

//다운로드새버전, 예결과아니오다운로드, autoUpdater.autoDownload = false
// 'download-progress'파일, 다운로드정도업데이트시트리거
// autoUpdater.on("download-progress", (info) => {
//   logger.info(`Download speed: ${info.bytesPerSecond}`);
//   logger.info(`Downloaded ${info.percent}%`);
//   logger.info(`Transferred ${info.transferred}/${info.total}`);
// });

const convertUpdateInfo = (info: ElectronUpdateInfo): UpdateManifest => ({
  version: info.version,
  date: info.releaseDate,
  body: info.releaseNotes?.toString() ?? '',
})

// 'update-downloaded'파일, 새버전다운로드완료시트리거
autoUpdater.on("update-downloaded", (event) => {
  const manifest = convertUpdateInfo(event)
  mainToRender('update-downloaded', JSON.stringify(manifest))
})

//감지업데이트
export const checkForUpdates = async (): Promise<UpdateInfo> => {
  const [error, result] = await to(autoUpdater.checkForUpdates());
  if (error) {
    return { couldUpdate: false }
  }

  let couldUpdate = result?.isUpdateAvailable ?? false
  let downloaded = false
  const manifest: UpdateManifest | null = result?.updateInfo ? convertUpdateInfo(result.updateInfo) : null

  if (couldUpdate && result?.downloadPromise) {
    // 있음새버전시, 감지설치 패키지여부완료다운로드, 있음설치 패키지전체다운로드, 가능재시작설치시, 반환있음새버전
    //  autoUpdater 있음 api 직선연결감지설치 패키지여부완료다운로드, , 사용 downloadPromise 
    // 예결과에서 500ms 내부, downloadPromise 있음 resolve, 설명설치 패키지있음다운로드
    try {
      await withTimeout(result?.downloadPromise, 500)
      downloaded = true
    } catch {
      downloaded = false
    }
  }

  return { couldUpdate, downloaded, manifest }
}

// 출력설치업데이트
export const quitAndInstallUpdates = () => {
  autoUpdater.quitAndInstall()
}