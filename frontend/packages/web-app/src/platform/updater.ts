import type { UpdaterManager as UpdaterManagerType } from '@rpa/shared/platform'

const checkUpdate: UpdaterManagerType['checkUpdate'] = async () => {
  return {
    couldUpdate: false,
    downloaded: false,
    manifest: null,
  }
}

const quitAndInstall: UpdaterManagerType['quitAndInstall'] = () => {
  return Promise.resolve()
}

const UpdaterManager: UpdaterManagerType = {
  checkUpdate,
  quitAndInstall,
}

export default UpdaterManager
