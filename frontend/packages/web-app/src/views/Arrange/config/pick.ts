export const WINELEVALUE = 'winElement'
export const WEBELEVALUE = 'webElement'
export const SAPELEVALUE = 'sapElement'
export const AIELEVALUE = 'aiElement'
export const VALID_POSITION = 'check_position'
export const VALID_CLICK = 'check_click'
export const VALID_INPUT = 'check_input'
export const VALID_HOVER = 'check_hover'
export const VALID_OPTIONS = [
 {
 label: 'location',
 value: VALID_POSITION,
 },
 // {
 // label: 'click-1',
 // value: VALID_CLICK,
 // },
 // {
 // label: 'input',
 // value: VALID_INPUT,
 // },
 // {
 // label: 'hover',
 // value: VALID_HOVER,
 // },
]

export const PICK_WINDOW = 'window' // 창선택
export const PICK_ITEM = 'item' // 요소 선택
export const PICK_MOUSE = 'mouse' // 마우스좌표선택
export const PICK_SAP = 'sap' // sap선택
export const PICK_CV = 'cv' // cv선택
export const PICK_SIMILAR_ITEM = 'similarItem'
export const PICK_SIMILAR = 'similar' // 요소 선택
export const PICK_EXCEL = 'excel' // excel선택
export const PICK_SIMPLE_ITEM = 'simpleItem' // 분ai요소의전송시스템선택
export const PICK_DATA = 'data' // 데이터선택
export const PICK_DATA_SIMILAR = 'dataSimilar' // 데이터요소 선택
export const PICKTYPE_NOVISUAL_LIST = [PICK_MOUSE, PICK_CV, PICK_WINDOW]

export const SMART_XPATH = 'smartXPath' // 가능경로
export const CUSTOM_XPATH = 'customXPath' // 사용자 지정경로

export const VISUALIZATION = 'visualization'
export const CUSTOMIZATION = 'customization'

export const CUSTOM_OPTIONS = [
 {
 label: 'visualEditing',
 value: 'visualization',
 },
 {
 label: 'customEditing',
 value: 'customization',
 },
]

// 매칭방식
export const MATCH_OPTIONS = [
 {
 label: 'onlyPosition',
 value: 'onlyPosition',
 tip: 'pickTips.similarPosition',
 },
 {
 label: 'scrollPosition',
 value: 'scrollPosition',
 tip: 'pickTips.scrollUntil',
 },
]

export const PATTERN_RULES = [
 {
 label: 'pickRules.equal',
 value: 0,
 },
 {
 label: 'pickRules.wildcard',
 value: 1,
 },
 {
 label: 'pickRules.regex',
 value: 2,
 },
 // {
 // label: '열기 로',
 // value: 3,
 // },
 // {
 // label: '결과로',
 // value: 4,
 // },
]

export const PATTERN_RULES_TYPE = {
 0: ' = ',
 1: ' includes ',
 2: ' regex ',
 // 3: ' startsWith ',
 // 4: ' endsWith ',
}

export const PATTERN_RULES_UIA = [
 {
 label: 'pickRules.equal',
 value: 0,
 },
]
