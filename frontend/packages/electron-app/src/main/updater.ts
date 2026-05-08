import { withTimeout } from '@rpa/shared'
import type { UpdateInfo, UpdateManifest } from '@rpa/shared/platform'
import { to } from 'await-to-js'
import { app } from 'electron'
import { autoUpdater } from 'electron-updater'
import type { UpdateInfo as ElectronUpdateInfo } from 'electron-updater'

import { config } from './config'
import { mainToRender } from './event'
import logger from './log'
import urlJoin from './utils'

autoUpdater.logger = logger
autoUpdater.forceDevUpdateConfig = false
// Do not install updates silently when the app exits.
autoUpdater.autoInstallOnAppQuit = false

const url = urlJoin(
  config.remote_addr,
  '/api/robot/client-version-update/update-check',
  `${process.platform}/${process.arch}/${app.getVersion()}`,
)
autoUpdater.setFeedURL(url)

// autoUpdater.on("error", (err) => {
//   logger.error("출력오류:", err);
// });

// autoUpdater.on("update-available", () => {
//   logger.info("found new version");
// });

// autoUpdater.on("download-progress", (info) => {
//   logger.info(`Download speed: ${info.bytesPerSecond}`);
//   logger.info(`Downloaded ${info.percent}%`);
//   logger.info(`Transferred ${info.transferred}/${info.total}`);
// });

function convertUpdateInfo(info: ElectronUpdateInfo): UpdateManifest {
  return {
    version: info.version,
    date: info.releaseDate,
    body: info.releaseNotes?.toString() ?? '',
  }
}

autoUpdater.on('update-downloaded', (event) => {
  const manifest = convertUpdateInfo(event)
  mainToRender('update-downloaded', JSON.stringify(manifest))
})

export async function checkForUpdates(): Promise<UpdateInfo> {
  const [error, result] = await to(autoUpdater.checkForUpdates())
  if (error) {
    return { couldUpdate: false }
  }

  const couldUpdate = result?.isUpdateAvailable ?? false
  let downloaded = false
  const manifest: UpdateManifest | null = result?.updateInfo ? convertUpdateInfo(result.updateInfo) : null

  if (couldUpdate && result?.downloadPromise) {
    // Report whether the updater has already downloaded enough to install.
    try {
      await withTimeout(result?.downloadPromise, 500)
      downloaded = true
    }
    catch {
      downloaded = false
    }
  }

  return { couldUpdate, downloaded, manifest }
}

export function quitAndInstallUpdates() {
  autoUpdater.quitAndInstall()
}
