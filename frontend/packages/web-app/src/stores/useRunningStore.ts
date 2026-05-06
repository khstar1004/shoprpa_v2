/**
 * 전체영역실행상태의
 */
import { message } from 'ant-design-vue'
import { set } from 'lodash-es'
import { defineStore } from 'pinia'
import { computed, ref, shallowRef } from 'vue'

import i18next from '@/plugins/i18next'

import { generateUUID, getCookie, sleep } from '@/utils/common'
import { baseUrl } from '@/utils/env'

import type { StartExecutorParams } from '@/api/resource'
import { closeDataTable, deleteDataTable, getDataTable, startDataTableListener, startExecutor, stopExecutor, updateDataTable } from '@/api/resource'
import Socket from '@/api/ws'
import { WINDOW_NAME } from '@/constants'
import { windowManager } from '@/platform'
import type { CreateWindowOptions } from '@/platform'
import { useFlowStore } from '@/stores/useFlowStore'
import { useProcessStore } from '@/stores/useProcessStore'
import { useRunlogStore } from '@/stores/useRunlogStore'
import useUserSettingStore from '@/stores/useUserSetting.ts'
import type { AnyObj, Fun } from '@/types/common'
import { changeDebugging } from '@/views/Arrange/components/flow/hooks/useChangeStatus'

export type RunState = 'run' | 'free' | 'debug' | 'silence' // 실행상태

export const useRunningStore = defineStore('running', () => {
  const processStore = useProcessStore()
  const flowStore = useFlowStore()
  const userSettingStore = useUserSettingStore()
  const runLogStore = useRunlogStore()

  // 실행경과중생성의창 label (후사용닫기창)
  let createdWindowLabels: string[] = []
  // 전체영역실행상태 run-실행 debug-디버그 free-중지 silence-(에서실행기기목록실행) 예free상태
  const running = ref<RunState>('free')
  const debugData = ref<any>({}) // 디버그정보, 패키지현재디버그의행, 대기정보
  const debugDataVar = ref<any>({}) // 디버그정보
  // 상태: starting 시작중,  startSuccess 시작성공,  startFailed 시작 실패, running-실행중, runSuccess-실행성공, runFailed실행실패 stopping 중지중,  stopSuccess 중지성공,   stopFailed 중지실패
  const status = ref('')
  // 데이터테이블내용
  const dataTable = shallowRef<RPA.IDataTableSheet>(null)
  let dataTableListenController: AbortController | null = null

  let debugReplyEventId = ''
  let runProjectId = null
  let RpaExecutorUrl = null
  let RpaExecutor: Socket | null = null

  const reset = () => {
    setRunning('free')
    debugData.value = {}
    RpaExecutor?.destroy()
    dataTableListenController?.abort()
    closeDataTableListener()
  }

  const setRunning = (value: RunState) => {
    running.value = value
  }

  const setDebugData = (debugMsg, replyEventId: string) => {
    if (debugMsg.process_id && debugMsg.process_id !== processStore.activeProcessId) {
      processStore.checkActiveProcess(debugMsg.process_id)
    }
    if (debugMsg.debug_data?.data) {
      debugDataVar.value = debugMsg.debug_data.data
    }
    if (debugMsg.debug_data?.data) {
      debugDataVar.value = debugMsg.debug_data.data
    }
    if (debugMsg.debug_data?.is_break) {
      debugData.value = {
        ...debugMsg.debug_data,
        line: debugMsg.line,
        atomId: debugMsg.line_id,
        processId: debugMsg.process_id,
      }
    }
    // 디버그다음 단계및계속시, 실행기기실행중, 빈현재
    if (replyEventId === debugReplyEventId) {
      debugData.value = {}
    }
  }

  // 디버그의기존가능
  const breakpointAtom = computed(() => {
    changeDebugging(debugData.value.atomId)
    if (debugData.value.atomId) {
      const findIdx = flowStore.simpleFlowUIData.findIndex(i => i.id === debugData.value.atomId)
      return flowStore.simpleFlowUIData[findIdx]
    }
    return null
  })

  const setStatus = (value: string) => {
    status.value = value
  }

  const setRunProjectId = (id: string | number) => {
    runProjectId = id
  }
  const getRunProjectId = () => {
    return runProjectId
  }

  // 생성ws연결
  const createSocket = (callback?: Fun) => {
    RpaExecutor = new Socket('', {
      url: RpaExecutorUrl,
      noInitCreat: true,
      isReconnect: true,
      reconnectCount: 5,
      isHeart: true,
      heartTime: 30 * 1000,
    })
    RpaExecutor.bindMessage(async (res: string) => { // 관리ws메시지
      const result = JSON.parse(res)
      const { data: msg, event_time, channel, reply_event_id } = result

      if (running.value !== 'silence' && !['debug_start', 'end'].includes(msg.status) && !reply_event_id) {
        runLogStore.addLog({ ...msg, event_time }) // 추가로그
      }

      // 열기 지정테이블단일창
      if (result.key === 'sub_window' && msg.name === 'userform') {
        const windowLabel = `${WINDOW_NAME.USERFORM}-${generateUUID()}`
        createdWindowLabels.push(windowLabel)

        // 열기 지정테이블단일창후, 클릭테이블단일제출, 있음일개메시지돌아가기복사
        const replyEventData = {
          key: result.key,
          channel: result.channel,
          reply_event_id: result.event_id,
          // 돌아가기복사메시지, 방법정상반대
          uuid: result.send_uuid,
          send_uuid: result.uuid,
        }

        // 생성 URL, 예결과있음 params 이면추가조회매개변수
        const options: CreateWindowOptions = {
          url: `${baseUrl}/${WINDOW_NAME.USERFORM}.html?option=${JSON.stringify(msg.option)}&reply=${JSON.stringify(replyEventData)}`,
          title: 'shoprpa-window',
          label: windowLabel,
          alwaysOnTop: true,
          position: 'center',
          width: 500,
          height: 400,
          resizable: false,
          skipTaskbar: true,
          transparent: false,
          show: false,
        }

        windowManager.createWindow(options)
      }

      // 열기다중창
      if (result.key === 'sub_window' && msg.name === 'multichat') {
        const windowLabel = `${WINDOW_NAME.MULTICHAT}-${generateUUID()}`
        createdWindowLabels.push(windowLabel)

        // 열기AI창후, 클릭저장, 있음일개메시지돌아가기복사
        const replyEventData = {
          key: result.key,
          channel: result.channel,
          reply_event_id: result.event_id,
          // 돌아가기복사메시지, 방법정상반대
          uuid: result.send_uuid,
          send_uuid: result.uuid,
        }

        const queryString = new URLSearchParams({ ...msg.params, reply: JSON.stringify(replyEventData) }).toString()
        const options: CreateWindowOptions = {
          url: `${baseUrl}/${WINDOW_NAME.MULTICHAT}.html?${queryString}`,
          label: windowLabel,
          alwaysOnTop: true,
          position: 'center',
          width: 800,
          height: 600,
          skipTaskbar: true,
          transparent: true,
        }
        windowManager.createWindow(options)
      }

      if (running.value === 'debug') {
        setDebugData(msg, reply_event_id) // 관리디버그데이터
      }

      // 실행 결과, 실행출력오류, 실행기기오류대기예외출력시, 닫기socket재상태
      if (['task_end', 'task_error'].includes(msg.status) || channel === 'exit') {
        await sleep(1000)
        setStatus(msg.status === 'task_end' ? 'runSuccess' : 'runFailed')
        reset()
      }
    })
    RpaExecutor.create(() => callback?.())
    RpaExecutor.bindOpen(() => setStatus('startSuccess'))
    RpaExecutor.bindClose(() => {
      if (RpaExecutor.OPTIONS.reconnectCount === 0) {
        setStatus('startFailed')
        reset()
      }
    })
  }

  // 닫기실행경과중생성의창
  const closeCreatedWindows = () => {
    // 닫기로그팝업
    windowManager.closeWindow(WINDOW_NAME.LOGWIN)
    createdWindowLabels.forEach(label => windowManager.closeWindow(label))
    createdWindowLabels = []
  }

  // 전송ws메시지
  const send = (sendMsg) => {
    if (RpaExecutor.isConnect()) {
      RpaExecutor.send(sendMsg)
    }
    else {
      createSocket(() => RpaExecutor.send(sendMsg))
    }
  }

  // 전송돌아가기복사데이터
  const sendReplyMessage = (data: AnyObj) => {
    send({ ...data, event_id: generateUUID(), event_time: Date.now() })
  }

  // 시작실행기기생성ws연결
  const start = async (params: StartExecutorParams) => {
    console.log('start: ', params)
    // http시작실행기기, 가져오기실행기기반환의ws url
    setStatus('starting')
    setRunProjectId(params.project_id)
    try {
      RpaExecutorUrl = await startExecutor({
        ...params,
        jwt: getCookie('jwt'),
        hide_log_window: !userSettingStore.openLogModalAfterRun,
        project_name: params.project_name || processStore.project.name,
      })
      // 연결 ws
      createSocket()

      if (running.value !== 'silence') {
        runLogStore.clearLogs()
        _startDataTableListener()
      }
    }
    catch {
      running.value = 'free'
      setStatus('startFailed')
      windowManager.maximizeWindow(true)
      dataTableListenController?.abort()
    }
  }

  const stop = (projectId: string | number) => {
    setStatus('stopping')
    stopExecutor({ project_id: projectId })
      .then(() => setStatus('stopSuccess'))
      .finally(() => reset())
  }

  const startRun = (projectId: string | number, processId?: string | number, line?: string | number, end_line?: string | number) => {
    const runParams: StartExecutorParams = { project_id: projectId, process_id: processId }

    line && (runParams.line = line)
    end_line && (runParams.end_line = end_line)
    processStore.isComponent && (runParams.is_custom_component = processStore.isComponent)

    running.value = 'run'
    start(runParams)
    windowManager.minimizeWindow()
  }

  const startDebug = (projectId: string | number, processId: string | number) => {
    const debugParams: StartExecutorParams = { project_id: projectId, process_id: processId, debug: 'y' }

    processStore.isComponent && (debugParams.is_custom_component = processStore.isComponent)

    running.value = 'debug'
    start(debugParams)
  }

  const startSlice = async (editObj: AnyObj) => {
    running.value = 'silence'
    await start({
      project_id: editObj.robotId,
      exec_position: editObj.exec_position || 'PROJECT_LIST',
      recording_config: JSON.stringify(userSettingStore.userSetting.videoForm),
      project_name: editObj.robotName,
      open_virtual_desk: editObj.open_virtual_desk || false,
    })
    windowManager.minimizeWindow()
  }

  const nextStepDebug = () => {
    if (running.value !== 'debug')
      return message.warning(i18next.t('common.startDebugFirst'))
    const msg = {
      event_id: generateUUID(),
      event_time: Date.now(),
      channel: 'flow',
      key: 'next',
      data: {},
    }
    debugReplyEventId = msg.event_id
    send(msg)
  }

  const continueDebug = () => {
    if (running.value !== 'debug')
      return message.warning(i18next.t('common.startDebugFirst'))
    const msg = {
      event_id: generateUUID(),
      event_time: Date.now(),
      channel: 'flow',
      key: 'continue',
      data: {},
    }
    debugReplyEventId = msg.event_id
    send(msg)
  }

  const breakPointDebug = (isAdd: boolean, list?: Array<{ process_id: string | number, line: number }>) => {
    if (running.value !== 'debug')
      return
    const msg = {
      event_id: generateUUID(),
      event_time: Date.now(),
      channel: 'flow',
      key: isAdd ? 'add_break' : 'clear_break',
      data: {
        break_list: list,
      },
    }
    send(msg)
  }

  /**
   * 가져오기데이터테이블내용
   */
  const fetchDataTable = async () => {
    const data = await getDataTable(processStore.project.id)
    dataTable.value = data.sheets.find(it => it.name === data.active_sheet)
  }

  /**
   * 닫기데이터테이블
   * @returns
   */
  const closeDataTableListener = () => closeDataTable(processStore.project.id)

  /**
   * 업데이트셀데이터
   * @param cellData
   */
  const updateDataTableCell = async (cellData: Omit<RPA.IUpdateDataTableCell, 'sheet'>[]) => {
    const sheetName = dataTable.value?.name
    await updateDataTable(processStore.project.id, cellData.map(it => ({ sheet: sheetName, ...it })))
    // 까지본
    cellData.forEach(it => set(dataTable.value.data, [it.row, it.col], it.value))
    dataTable.value.max_row = dataTable.value.data.length
    dataTable.value.max_column = dataTable.value.data.length > 0 ? Math.max(...dataTable.value.data.map(it => it.length)) : 0
  }

  /**
   * 빈셀데이터
   */
  const clearDataTable = async () => {
    dataTable.value = null
    await deleteDataTable(processStore.project.id)
  }

  /**
   * 열기시작데이터테이블 sse 방식
   */
  const _startDataTableListener = () => {
    dataTableListenController = startDataTableListener(processStore.project.id, (res) => {
      if (res.event === 'file_deleted') {
        dataTable.value = null
      }
      else if (res.event === 'file_changed') {
        fetchDataTable()
      }
    })
  }

  return {
    dataTable,
    running,
    debugData,
    breakpointAtom,
    status,
    debugDataVar,
    reset,
    setRunning,
    startRun,
    startDebug,
    startSlice,
    nextStepDebug,
    continueDebug,
    breakPointDebug,
    stop,
    getRunProjectId,
    fetchDataTable,
    updateDataTableCell,
    closeDataTableListener,
    clearDataTable,
    sendReplyMessage,
    closeCreatedWindows,
  }
})