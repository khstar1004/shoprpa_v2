import dayjs from 'dayjs'
import { debounce } from 'lodash-es'
// 실행로그정보
import { defineStore } from 'pinia'
import { ref, shallowRef } from 'vue'

const LOG_LEVEL_MAP: Record<RPA.LogLevel, string> = {
  error: '오류',
  info: '정보',
  warning: '경고',
  debug: '디버그',
}

function geneLogItem(it: RPA.ServerLogItem, index: number): RPA.LogItem {
  return {
    id: it.line_id || it.event_id || `row_${index}`,
    logLevel: it.log_level,
    logLevelText: LOG_LEVEL_MAP[it.log_level],
    logType: it.log_type,
    timestamp: dayjs(it.event_time * 1000).format('YYYY-MM-DD HH:mm:ss'),
    content: it.msg_str,
    lineNum: it.line,
    processName: it.process,
    processId: it.process_id,
    error_traceback: it.error_traceback,
  }
}

export const useRunlogStore = defineStore('runlog', () => {
  const logList = shallowRef<RPA.LogItem[]>([])
  const activeLogId = ref('')
  const pendingLogs = shallowRef<RPA.ServerLogItem[]>([])

  const flushLogs = debounce(() => {
    const newLogs = pendingLogs.value.map((it, index) =>
      geneLogItem(it, logList.value.length + index),
    )

    logList.value = [...logList.value, ...newLogs]
    pendingLogs.value = []

    if (logList.value.length > 10000) { // 값, 단말
      logList.value = logList.value.slice(-10000)
    }
  }, 300, { maxWait: 1000 })

  const addLog = (log: RPA.ServerLogItem) => {
    if (log.status === 'debug_start')
      return

    // 예결과 logList 로빈, 추가일로그
    if (logList.value.length === 0) {
      logList.value.push(geneLogItem(log, 0))
      return
    }

    // 아니오이면추가입력대기관리큐
    pendingLogs.value.push(log)
    flushLogs()
  }

  const clearLogs = () => {
    logList.value = []
  }

  const setLogs = (data: Array<RPA.ServerLogItem>) => {
    logList.value = data.map((it, index) => geneLogItem(it, index))
  }

  const setActiveLogId = (id: string) => {
    activeLogId.value = id
  }

  return {
    activeLogId,
    logList,
    addLog,
    setLogs,
    clearLogs,
    setActiveLogId,
  }
})
