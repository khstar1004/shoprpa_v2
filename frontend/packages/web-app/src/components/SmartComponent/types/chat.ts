export type SmartType = 'web_auto' | 'data_process'

export type SceneCode = `smart_${SmartType | 'optimize_input'}`

export type Role = 'user' | 'assistant'

export type MessageStatus = 'generating' | 'completed' | 'failed'

export interface MessageOutput {
  text: string
  smartCode?: string
  status?: MessageStatus // 메시지상태
  version?: number // 컴포넌트버전
  error?: string
  tip?: string
  duration?: number // 컴포넌트완료시(초)
  packages?: string[] // 패키지목록
}

export interface Message {
  role: Role
  content: MessageInput | MessageOutput
}

export interface MessageInput {
  sceneCode?: SceneCode
  user: string

  needFix?: boolean
  fixInfo?: {
    consoleLog: string
    traceback: string
  }
  currentCode?: string

  elements?: any[]
  chatHistory?: Message[]
}
