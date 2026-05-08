/** @format */

type TaskType = 'schedule' | 'mail' | 'file' | 'hotKey' | 'manual'
type FrequencyType = 'days' | 'weeks' | 'months' | 'hours' | 'minutes' | 'regular' | 'advance'
type FileEvents = 'create' | 'delete' | 'update' | 'renamed'
type MailFlag = 'qq' | '163' | '126' | 'shoprpa' | 'advance'
type FixedLengthArray = [0, 1, 2, 3, 4, 5, 6]

export interface Schedule {
  end_time?: string
  frequency_flag: FrequencyType
  minutes?: number
  hours?: number
  weeks?: number[]
  months?: number
  time_expression?: string
  cron_expression?: string
  end_time_checked?: boolean
}
export interface Mail {
  user_mail: string
  user_authorization: string
  mail_flag: MailFlag
  end_time: string
  interval_time: number
  condition: string
  sender_text: string
  receiver_text: string
  theme_text: string
  content_text: string
  attachment: boolean
  custom_mail_server: string
  custom_mail_port: string
}
export interface File {
  directory: string
  relative_sub_path: boolean
  events: FileEvents[]
  files_or_type: string
}
interface HotKey {
  key: string
  shortcuts: string[]
}
export interface Manual {}

export interface Task {
  /**
   * 여부사용 1 사용 ;0 아니오사용
   */
  enable?: number
  /**
   * 오류예관리: 건너뛰기 jump, 중중지 stop, 재시도후건너뛰기 retry_jump, 재시도후중중지 retry_stop
   */
  exceptional?: 'jump' | 'stop' | 'retry_stop' | 'retry_jump'
  /** 예외시재시도데이터 */
  retryNum?: number
  /**
   * 트리거기기예약 작업이름
   */
  name: string
  /**
   * 사용실행순서열
   */
  robotInfoList?: RobotInfo[]
  /**
   * 생성예약 작업의매개변수
   */
  taskJson?: any
  /**
   * 작업유형: schedule, mail, file, hotKey, manual
   */
  taskType: TaskType
  /**
   * 시간 초과시간
   */
  timeout?: number
  [property: string]: any
  mail?: Mail
  schedule?: Schedule
  file?: File
  hotkey?: HotKey
  manual?: Manual
  taskEnable?: boolean
}

/**
 * com.shoprpa.rpa.task.entity.dto.RobotInfo
 *
 * RobotInfo
 */
export interface RobotInfo {
  paramJson?: string
  robotId: string
  [property: string]: any
}

export interface TaskTrigger {
  task_type?: TaskType
  enable?: boolean
  callback_project_ids?: string[]
  queue_enable?: boolean

  end_time?: string
  frequency_flag?: FrequencyType
  minutes?: number
  hours?: number
  weeks?: number[]
  months?: number[]
  time_expression?: string
  cron_expression?: string
  timeout?: number

  directory?: string
  relative_sub_path?: boolean // 여부디렉터리
  events?: string[]
  files_or_type?: string[]

  shortcuts?: string[]

  interval_time?: number
  condition?: string
  sender_text?: string
  receiver_text?: string
  theme_text?: string
  content_text?: string
  attachment?: boolean
  mail_flag?: string
  custom_mail_server?: string
  custom_mail_port?: string
  user_mail?: string
  user_authorization?: string
}
