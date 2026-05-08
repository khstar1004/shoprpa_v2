import { storage } from '@/utils/storage'

const CONNECTOR_STATUS = 1 // 연결상태
const ignorePath = ['event_tracking'] // 의경로
const ignoreMsg = ['ping', 'pong', 'error', 'ask'] // 의메시지

export interface SocketParamsType {
  url?: string
  port?: number
  noCreatRouters?: Array<string>
  noInitCreat?: boolean
  reconnectMaxTime?: number
  reconnectDelay?: number
  isReconnect?: boolean
  reconnectCount?: number
  isHeart?: boolean
  heartTime?: number
  timeout?: number
}

class Socket {
  ws: any

  RECONNEC_TTIMER = 0

  HEART_TIMER = 0

  CALLBACK: any // bindMessage돌아가기조정데이터

  OPENCALLBACK: any // open돌아가기조정데이터

  CLOSECALLBACK: any // close돌아가기조정데이터

  ERRORCALLBACK: any // error돌아가기조정데이터

  timer: any // 시간 초과예약기기

  OPTIONS = {
    url: 'ws://127.0.0.1',
    isHeart: false,
    heartTime: 60 * 1000, // 시간 60s
    port: 13159, // 기본 포트
    heartMsg: 'ping', // 정보,로'ping'
    isReconnect: true, // 여부재
    isDestory: false, // 여부판매
    reconnectDelay: 10 * 1000, // 재시간 10s
    reconnectCount: -1, // -1 없음제한재 0
    reconnectMaxTime: 20,
    noCreatRouters: [],
    noInitCreat: false, // 아니오에서생성시
    timeout: 10 * 1000, // 시간 초과시간
  }

  eventId = '' // 메시지id
  msgStack: any = [] // 메시지

  constructor(router: string, params: SocketParamsType) {
    const port = storage.get('route_port') || params.port || this.OPTIONS.port
    const route = router || ''
    this.OPTIONS.url = `${this.OPTIONS.url}:${port}/${route}`
    if (params.url) {
      this.OPTIONS.url = params.url
    }
    if (params.isHeart)
      this.OPTIONS.isHeart = params.isHeart
    Object.assign(this.OPTIONS, params)
    if (!params.noInitCreat) {
      this.create()
    }
  }

  /**
   * 생성연결
   */
  create(callback?: any) {
    if (!this.OPTIONS.url) {
      throw new Error('연결 주소가 없어 통신 채널을 생성할 수 없습니다.')
    }
    this.OPTIONS.isDestory = false
    delete this.ws
    this.OPTIONS.url = `${this.OPTIONS.url}`
    this.ws = new WebSocket(this.OPTIONS.url)
    this.timeout()
    this.onopen(callback)
    this.onclose()
    this.onmessage()
    this.onerror()
  }

  /**
   *  60s 일
   */
  heart() {
    this.send({ channel: this.OPTIONS.heartMsg })
    this.HEART_TIMER = window.setTimeout(() => {
      clearTimeout(this.HEART_TIMER)
      this.heart()
    }, this.OPTIONS.heartTime)
  }

  /**
   * timeout
   */
  timeout() {
    this.timer = setTimeout(() => {
      this.ws.close()
    }, this.OPTIONS.timeout)
  }

  /**
   * 지정연결성공파일
   * 예결과callback저장에서, 호출callback, 찾을 수 없습니다호출OPTIONS중의돌아가기조정
   * @param {Function} callback 돌아가기조정데이터
   */
  onopen(callback?: any) {
    this.ws.onopen = (event: any) => {
      clearTimeout(this.timer) // 지우기시간 초과예약기기
      if (this.OPTIONS.isHeart)
        this.heart()
      this.msgStack = []
      clearTimeout(this.RECONNEC_TTIMER) // 지우기재예약기기
      this.OPENCALLBACK
        ? this.OPENCALLBACK(event)
        : typeof callback === 'function' && callback(event)
    }
  }

  /**
   * 지정닫기 파일
   * 예결과callback저장에서, 호출callback, 찾을 수 없습니다호출OPTIONS중의돌아가기조정
   * @param {Function} callback 돌아가기조정데이터
   */
  onclose(callback?: any) {
    if (this.ws) {
      this.ws.onclose = (event: any) => {
        clearTimeout(this.timer) // 지우기시간 초과예약기기
        this.msgStack = []
        if (this.OPTIONS.isHeart)
          clearTimeout(this.HEART_TIMER)
        if (!this.OPTIONS.isDestory && this.OPTIONS.isReconnect) {
          // 판매, 필요재
          this.reconnect()
        }
        this.CLOSECALLBACK
          ? this.CLOSECALLBACK(event)
          : typeof callback === 'function' && callback(event)
      }
    }
  }

  /**
   * 지정오류파일
   * 예결과callback저장에서, 호출callback, 찾을 수 없습니다호출OPTIONS중의돌아가기조정
   * @param {Function} callback 돌아가기조정데이터
   */
  onerror(callback?: any) {
    this.ws.onerror = (event: any) => {
      clearTimeout(this.timer)
      this.msgStack = []
      this.ERRORCALLBACK ? this.ERRORCALLBACK(event) : typeof callback === 'function' && callback(event)
    }
  }

  /**
   * 지정메시지파일
   * 예결과callback저장에서, 호출callback, 찾을 수 없습니다호출OPTIONS중의돌아가기조정
   */
  onmessage() {
    this.ws.onmessage = (event: any) => {
      let dataAll: any
      try {
        dataAll = JSON.parse(event.data)
      }
      catch (error) {
        console.error('WebSocket 메시지 파싱 실패:', error, event.data)
        return
      }

      if (!dataAll || typeof dataAll !== 'object')
        return

      if (ignoreMsg.includes(dataAll.channel))
        return
      this.eventId = dataAll.event_id
      if (typeof dataAll.data === 'object') {
        this.CALLBACK && this.CALLBACK(JSON.stringify(dataAll))
      }
      else {
        this.CALLBACK && this.CALLBACK(dataAll)
      }
    }
  }

  bindMessage(callback?: any) {
    if (callback) {
      this.CALLBACK = callback
    }
  }

  bindClose(callback?: any) {
    if (callback) {
      this.CLOSECALLBACK = callback
    }
  }

  bindError(callback?: any) {
    if (callback) {
      this.ERRORCALLBACK = callback
    }
  }

  bindOpen(callback?: any) {
    if (callback) {
      this.OPENCALLBACK = callback
    }
  }

  send(data: object) {
    if (!this.ws || this.ws.readyState !== this.ws.OPEN) {
      return new Error('서비스와 연결되지 않아 메시지를 전송할 수 없습니다.')
    }
    this.sendText(JSON.stringify(data))
  }

  sendText(data: string) {
    if (typeof data !== 'string')
      throw new Error('전송할 메시지는 문자열이어야 합니다.')
    this.msgStack.pop()
    this.ws.send(data)
  }

  /**
   * 여부완료연결
   */
  isConnect() {
    return this.ws && this.ws.readyState === CONNECTOR_STATUS
  }

  /**
   * 연결파일
   */
  reconnect() {
    if (this.OPTIONS.reconnectCount === 0 || !this.OPTIONS.isReconnect) {
      // 아니오재
      clearTimeout(this.RECONNEC_TTIMER)
      this.OPTIONS.isDestory = true
      return
    }
    if (this.OPTIONS.isReconnect && this.OPTIONS.reconnectCount === -1) {
      // 아니오제한재 아니오기록재로그
      this.RECONNEC_TTIMER = window.setTimeout(() => {
        this.create()
      }, this.OPTIONS.reconnectDelay)
      return
    }

    this.RECONNEC_TTIMER = window.setTimeout(() => {
      // 열기 재
      this.create()
    }, this.OPTIONS.reconnectDelay)

    this.OPTIONS.reconnectCount--
  }

  /**
   * 판매
   */
  destroy() {
    clearTimeout(this.HEART_TIMER) // 지우기예약기기
    clearTimeout(this.RECONNEC_TTIMER) // 지우기재예약기기
    clearTimeout(this.timer)
    this.OPTIONS.isDestory = true
    this.ws && this.ws.close()
    this.CALLBACK = null
    this.OPENCALLBACK = null
    this.CLOSECALLBACK = null
    this.ERRORCALLBACK = null
    this.ws = null
    this.msgStack = []
  }

  /**
   * , 로그기록연결정보
   */
  ignoreLog(msg: string) {
    const fitPath = ignorePath.find(i => this.OPTIONS.url.includes(i))
    const fitMsg = ignoreMsg.find(j => msg.includes(j))
    return fitPath || fitMsg
  }
}

export default Socket
