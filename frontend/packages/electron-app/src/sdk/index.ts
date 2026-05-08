import './polyfill'

if (typeof window === 'undefined') {
  throw new TypeError('This code must be run in a browser environment.')
}

let maxRetries = 100

function init() {
  if (window.electron) {
    import('./windowManager').then((module) => {
      const ElectronWindowManager = module.default
      window.WindowManager = new ElectronWindowManager()
    })
    import('./shortcutManager').then((module) => {
      const ShortCutManager = module.default
      window.ShortCutManager = new ShortCutManager()
    })
    import('./clipboardManager').then((module) => {
      const ClipboardManager = module.default
      window.ClipboardManager = ClipboardManager
    })
    import('./utilsManager').then((module) => {
      const UtilsManager = module.default
      window.UtilsManager = UtilsManager
    })
    import('./updaterManager').then((module) => {
      const UpdaterManager = module.default
      window.UpdaterManager = UpdaterManager
    })
  }
  else {
    setTimeout(() => {
      if (maxRetries-- > 0) {
        init()
      }
    }, 0)
  }
}

init()
