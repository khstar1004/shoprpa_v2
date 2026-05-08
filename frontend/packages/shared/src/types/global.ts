// 전체영역유형파일
// 개파일확인전체영역유형에서모든사용 @rpa/shared 의 packages 중

import type {
  ClipboardManager,
  ShortCutManager,
  UpdaterManager,
  UtilsManager,
  WindowManager,
} from './platform'

declare global {
  interface Window {
    WindowManager?: WindowManager
    ClipboardManager?: ClipboardManager
    UtilsManager?: UtilsManager
    ShortCutManager?: ShortCutManager
    UpdaterManager?: UpdaterManager
  }
}

// 확인개파일모듈관리
export {}
