import { uniqBy } from 'lodash-es'
import { ref } from 'vue'

import { getRecentAtomUsage } from '@/utils/atomHistory'
import { simpleFuzzyMatch } from '@/utils/common'

import { useFlowStore } from '@/stores/useFlowStore'
import { useProcessStore } from '@/stores/useProcessStore'

let triggerPreId = ''
const inputPos = ref('')
export function setTriggerPreId(id: string) {
 if (id === triggerPreId)
 return
 const findIndex = useFlowStore().simpleFlowUIData.findIndex(i => i.id === triggerPreId)
 if (findIndex > -1) {
 const curItem = useFlowStore().simpleFlowUIData[findIndex]
 useFlowStore().setSimpleFlowUIDataByType({ ...curItem, showInput: false }, findIndex, true)
 }
 triggerPreId = id
 const curIndex = useFlowStore().simpleFlowUIData.findIndex(i => i.id === id)
 if (curIndex > -1) {
 const curItem = useFlowStore().simpleFlowUIData[curIndex]
 useFlowStore().setSimpleFlowUIDataByType({ ...curItem, showInput: true }, findIndex, true)
 }
}
export function getTriggerPreId() {
 return triggerPreId
}

export function setInputPos(pos: string) {
 inputPos.value = pos
}

export function getInputPos() {
 return inputPos
}

// 트리거방식삽입, 원자 기능검색및권장
export function useSearch(emits) {
 const processStore = useProcessStore()
 // const allAtomList = ref([]) // 모든원자 기능목록 데이터
 const searchResult = ref<RPA.AtomTreeNode[]>([]) // 아래메뉴데이터
 const defaultRecommend = ref<RPA.AtomTreeNode[]>([]) // 원자 기능권장목록
 const searchValue = ref('') // 검색의값

 // 완료원자 기능권장목록(5건사용의원자 기능)
 const generateRecommend = () => {
 const recentUsage = getRecentAtomUsage(5)
 if (recentUsage.length > 0) {
 defaultRecommend.value = recentUsage.map(record => ({
 key: record.key,
 title: record.title,
 icon: record.icon,
 parentKey: record.parentKey,
 }))
 }
 else {
 defaultRecommend.value = []
 }
 }

 // 가져오기검색결과의텍스트내용 배열방식
 const getTitleTexts = (title: string) => {
 return title?.split('').map((text, index) => {
 return {
 text,
 active: searchValue.value.includes(text) ? index === title.indexOf(text) : false,
 }
 })
 }

 // 검색
 const search = (value: string) => {
 searchValue.value = value

 if (value) {
 searchResult.value = uniqBy(processStore.atomicTreeDataFlat.filter(i => simpleFuzzyMatch(searchValue.value, i.title)), 'key')
 return
 }
 // 있음검색값, 표시권장의원자 기능목록
 searchResult.value = defaultRecommend.value
 }

 // 초점시표시표시사용의원자 기능
 const onFocus = () => {
 generateRecommend()
 searchResult.value = defaultRecommend.value
 }

 // 선택중
 const select = (value: string) => emits('select', value)

 return {
 searchValue,
 search,
 searchResult,
 select,
 getTitleTexts,
 onFocus,
 }
}

// 트리거 입력 보조 흐름
export function useRecord() {
 const inputRef = ref(null) // 입력란대상
 const recoder = ref(null) // 기록대상
 const originText = ref('') // 입력란기존텍스트
 const aiQuestion = ref('') // 후의텍스트, 기존텍스트 + 텍스트연결
 const status = ref('') // 상태 '' 기본값 answering 정상에서돌아가기 recoding 정상에서기록

 // 기록 대상 생성
 const creatRecordr = () => {

 }

 // const recodeFn = (text: string) => {
 // aiQuestion.value = (originText.value || '') + text;
 // aiQuestionChange();
 // }

 // 열기 기록
 const startRecoding = (e: Event) => {
 if (status.value === 'answering')
 return false
 e.preventDefault()
 status.value === 'recoding'
 recoder.value.recStart()
 // 기록전, 저장현재입력란의텍스트
 const tempText = aiQuestion.value
 const originVal = tempText.trim()
 if (originVal) {
 const endsStr = originVal[originVal.length - 1]
 if (![',', '.', '. ', ', '].includes(endsStr)) {
 aiQuestion.value += ', '
 }
 }
 originText.value = aiQuestion.value
 }

 // 중지기록
 const stopRecoding = (e) => {
 e.preventDefault()
 status.value === ''
 recoder.value.recStop()
 originText.value = aiQuestion.value
 }

 // 텍스트내용변수
 const aiQuestionChange = () => {
 if (inputRef.value) {
 inputRef.value.scrollLeft = inputRef.value.scrollWidth
 }
 }

 // 입력한 내용을 어시스턴트로 전달
 const sendQuestion = (e, isEnter = false) => {
 if (isEnter) {
 if (e.keyCode === 13 && !e.shiftKey) {
 e.stopPropagation()
 e.preventDefault()
 sendToAssistant()
 }
 return
 }
 sendToAssistant()
 }

 const sendToAssistant = () => {
 console.log('sendQuestion', aiQuestion.value)
 }

 return {
 inputRef,
 status,
 aiQuestion,
 aiQuestionChange,
 creatRecordr,
 startRecoding,
 stopRecoding,
 sendQuestion,
 }
}
