import type { Message, SmartType } from './chat'

export interface SmartComp extends Omit<RPA.Atom, 'version'> {
  key: string
  version?: number
  smartType: SmartType
  smartCode: string
  createTime?: number // 컴포넌트완료시간(시간, 초)
  detail: {
    packages: string[]
    elements: string[]
    chatHistory: Message[]
  }
}