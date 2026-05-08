import { RpaPicker } from '@/api/pick'
import { WINDOW_NAME } from '@/constants'
import type { RECORD_EVENT } from '@/constants/record'
import { windowManager } from '@/platform'
import type { PickParams, RecordAction } from '@/types/resource'

// 통신경과 websocket 및선택통신의일시스템열파일
export class RecordEvent {
  private socket: typeof RpaPicker
  private pickInfoResolve: PromiseWithResolvers<string>

  constructor(bindMessage: (message: any) => void, bindError: (error: any) => void) {
    RpaPicker.create(() => {
      this.listening()
    })

    // 지정메시지
    RpaPicker.bindMessage(bindMessage)

    // 지정오류
    RpaPicker.bindError(bindError)

    this.socket = RpaPicker
  }

  private sendEvent(action: RecordAction, data = '') {
    const params: PickParams = {
      pick_sign: 'RECORD',
      record_action: action,
      data,
    }
    return this.socket.send(params)
  }

  destroy() {
    this.socket.destroy()
  }

  // 열기시작
  listening() {
    this.sendEvent('RECORD_LISTENING')
  }

  // 프론트엔드창hover알림알림백엔드닫기선택
  pausePick() {
    this.sendEvent('RECORD_AUTOMIC_HOVER_START')
  }

  // 프론트엔드알림알림백엔드hover결과열기시작선택
  resumePick() {
    this.sendEvent('RECORD_AUTOMIC_HOVER_END')
  }

  // 기록제어결과, 관리데이터
  stopPick() {
    this.sendEvent('RECORD_END')
  }

  // 열기 기록제어 - 열기시작선택
  startRecord() {
    this.sendEvent('RECORD_START')
  }

  // 일시중지기록제어 - 닫기선택
  pauseRecord() {
    this.sendEvent('RECORD_PAUSE')
  }

  // 선택원자 기능결과 - 프론트엔드전송정보, 백엔드알림알림프론트엔드선택정보
  getPickInfo() {
    this.pickInfoResolve = Promise.withResolvers<string>()

    this.sendEvent('RECORD_AUTOMIC_END')

    return this.pickInfoResolve.promise
  }

  pickInfoCallback(data: string) {
    this.pickInfoResolve?.resolve(data)
  }
}

export function emitToRecordMenu(type: RECORD_EVENT, data: any = '') {
  windowManager.emitTo({
    from: WINDOW_NAME.RECORD,
    target: WINDOW_NAME.RECORD_MENU,
    type,
    data,
  })
}

export function emitToMain(type: string, data: any = '') {
  windowManager.emitTo({
    from: WINDOW_NAME.RECORD,
    target: WINDOW_NAME.MAIN,
    type,
    data,
  })
}
