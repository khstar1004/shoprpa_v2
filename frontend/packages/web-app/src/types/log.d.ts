declare namespace RPA {
  type LogLevel = 'error' | 'info' | 'warning' | 'debug'
  // 0 열기  1 성공 2 실패 3 실패건너뛰기
  type LogStatus = 0 | 1 | 2 | 3

  // "code(코드실행위치)" "user(통신인쇄 사용자인쇄)" "flow(프로세스역할정도 시작-닫기)"
  type LogType = 'code' | 'user' | 'flow'
  type LogFlowStatus = 'init' | 'init_success' | 'task_start' | 'task_end' | 'task_error'
  type LogCodeStatus = 'start' | 'res' | 'error' | 'skip' | 'debug_start'

  interface ServerLogItem {
    event_id: number
    log_level: LogLevel
    log_type: LogType
    line: number
    event_time: number
    msg_str: string
    process?: string
    process_id?: string
    line_id?: string
    status?: LogCodeStatus | LogFlowStatus
    atomic?: string
    error_str?: string
    error_traceback?: any
  }

  interface LogItem {
    id: string | number
    logLevel: LogLevel
    logLevelText: string
    logType: LogType
    timestamp: string
    content: string
    lineNum: number
    processName?: string
    processId?: string
    error_traceback?: string
  }
}
