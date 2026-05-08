export enum AppFileStatus {
  normal, // 정상일반상태
  downloading, // 다운로드중
  success, // 다운로드성공
  exception, // 다운로드실패
  cancled, // 완료가져오기메시지
}

export interface AppFileItem {
  link: string
  appendixId: string
  filename: string
  status: AppFileStatus
  percent?: number
}

export interface cardAppItem {
  appId: string
  appName: string
  appIntro: string
  checkNum: number
  downloadNum: number
  iconUrl: string
  marketId: string
  appVersion: string | number
  securityLevel: string
}
