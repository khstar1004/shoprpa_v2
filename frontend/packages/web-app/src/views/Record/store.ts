import { message } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { isArray, isNil, last } from 'lodash-es'
import { defineStore } from 'pinia'
import { ref } from 'vue'

import { WINDOW_NAME } from '@/constants'
import type { RecordActionType } from '@/constants/record'
import { RECORD_EVENT } from '@/constants/record'
import { useHistory } from '@/hooks/useHistory'
import { utilsManager } from '@/platform'
import type { PickElementType } from '@/types/resource'

import { emitToRecordMenu, RecordEvent } from './utils'

interface W2WType {
 from: WINDOW_NAME // 창
 target: WINDOW_NAME // 목록표시창
 type: RECORD_EVENT // 유형
 data?: any // 데이터
}

interface RecordData {
 action: RecordActionType
 pickInfo?: string // 선택정보
 name?: string // 요소이름
}

export const useRecordStore = defineStore('record', () => {
 const { t } = useTranslation()
 const isRecording = ref(false)
 const { state: list, perform, undo, redo, canUndo, canRedo } = useHistory<RecordData[]>([])

 const recordEvent = new RecordEvent(
 res => handleMessage(res),
 () => message.error(t('rpaPickerUnavailable')),
 )

 utilsManager.listenEvent('w2w', ({ from, target, type, data }: W2WType) => {
 if (from !== WINDOW_NAME.RECORD_MENU || target !== WINDOW_NAME.RECORD)
 return

 if (type === RECORD_EVENT.PAUSE_PICK) {
 recordEvent.pausePick()
 }
 else if (type === RECORD_EVENT.RESUME_PICK) {
 recordEvent.resumePick()
 }
 else if (type === RECORD_EVENT.CLICK_ACTION) {
 setPickInfo(data)
 }
 })

 // 중지기록제어
 const stopRecord = () => {
 recordEvent.stopPick()
 recordEvent.destroy()
 }

 const toggleRecord = async () => {
 isRecording.value ? recordEvent.pauseRecord() : recordEvent.startRecord()
 isRecording.value = !isRecording.value
 }

 // 관리 message
 const handleMessage = (res: { data: string, key: string }) => {
 const { data, key } = res

 console.log('handleMessage', res)

 switch (key) {
 // 마우스중지초요소 - 알림알림프론트엔드출력, 사용자마우스동작까지이가능출력원자 기능선택창
 case 'record_automic_start':
 notifyMenuShow(data)
 break

 case 'record_automic_draw_end':
 notifyMenuHide()
 break

 // 열기 기록제어
 case 'record_start':
 isRecording.value = true
 break

 // 일시중지기록제어
 case 'record_pause':
 isRecording.value = false
 notifyMenuHide()
 break

 // 가져오기선택정보성공
 case 'record_success':
 case 'success':
 data && recordEvent.pickInfoCallback(data)
 break

 default:
 break
 }
 }

 // 알림 menu 창표시표시
 const notifyMenuShow = (data: string) => {
 const { mouse_x, mouse_y, domain = 'web' } = JSON.parse(data)

 if (isNil(mouse_x) || isNil(mouse_y))
 return

 const isWeb = domain === 'web'
 emitToRecordMenu(RECORD_EVENT.SHOW_MENU, { x: mouse_x, y: mouse_y, isWeb })
 }

 // 알림 menu 창숨김보관
 const notifyMenuHide = () => {
 emitToRecordMenu(RECORD_EVENT.HIDE_MENU)
 }

 const setPickInfo = async (data: string) => {
 const itemData: RecordData = { action: data as RecordActionType }
 // 가져오기현재의선택정보
 const pickInfo = await recordEvent.getPickInfo()

 try {
 const info = JSON.parse(pickInfo) as PickElementType
 const tagName = (isArray(info.path) ? last(info.path).tag_name : info.path.tag) || '요소'
 itemData.name = tagName
 itemData.pickInfo = pickInfo
 }
 catch (error) {
 console.error(error)
 }

 perform((draft) => {
 draft.push(itemData)
 })
 }

 function clearAll() {
 perform(() => ([]))
 }

 function clear(index: number) {
 perform((draft) => {
 draft.splice(index, 1)
 })
 }

 return { isRecording, list, undo, redo, canUndo, canRedo, stopRecord, toggleRecord, clearAll, clear }
})
