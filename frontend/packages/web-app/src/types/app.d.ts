declare namespace RPA {
  type Theme = 'light' | 'dark' | 'auto'

  interface UserSetting {
    commonSetting: { // 일반
      startupSettings: boolean // 시작 - 연결가져오기저장, true 열기시작시작, false-닫기열기기기시작
      closeMainPage: boolean // true-소  false-출력사용
      hideLogWindow: boolean // 런타임의오른쪽아래역할로그창 true-열기시작  false-닫기 
      hideDetailLogWindow: boolean // 실행의로그창 true-열기시작  false-닫기 
      autoSave: boolean // 저장 true-열기시작  false-닫기
      theme?: Theme // 제목 가능선택값: light, dark, system
    }
    shortcutConfig: Record<string, any> // 빠름
    videoForm: VideoFormMap // 기록
    msgNotifyForm: MessageFormMap // 메시지알림
  }

  interface EmailFormMap {
    is_enable?: boolean // 여부사용
    receiver?: string // 파일사람
    is_default?: boolean // 아니오사용메일함
    mail_server?: string // 발송파일서비스기기
    mail_port?: string // 단말
    sender_mail?: string // 메일계정
    password?: string // 메일비밀번호(필요사용키, 저장의예키이름)
    use_ssl?: boolean // 여부SSL
    cc?: string // 
  }

  interface PhoneFormMap {
    is_enable?: boolean // 여부사용
    receiver?: string // 파일사람휴대폰 번호
    phone_msg_url?: string
  }

  interface MessageFormMap {
    email?: EmailFormMap
    phone_msg?: PhoneFormMap
  }

  interface VideoFormMap {
    cutTime: number | string
    enable: boolean
    // maxRecordingTime: number | string
    saveType: boolean
    fileClearTime: number | string
    filePath: string
    scene: string
  }
}