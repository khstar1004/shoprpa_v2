import i18next from '@/plugins/i18next'

import { ATOM_FORM_TYPE } from '@/constants/atom'

export type ATOMTABKEYS = 'BASE_ATOM' | 'EXT_ATOM'

export interface ATOMTABDATA {
 title: string
 value: ATOMTABKEYS
}

export const ATOMTABS: Array<ATOMTABDATA> = [
 {
 title: 'atomicPower',
 value: 'BASE_ATOM',
 },
 {
 title: 'extensionComponents',
 value: 'EXT_ATOM',
 },
]

// 프로세스변수유형
export const GLOBAL_VAR_TYPE = 'globalVariables' // 전역 변수
export const PROCESS_VAR_TYPE = 'processVariables' // 영역부서변수
export const PARAMETER_VAR_TYPE = 'configParameters' // 구성 매개변수

export const PICK_TYPE_CV = 'CV' // 선택유형CV

// 기존의input가능입력데이터값유형
export const INPUT_NUMBER_TYPE_ARR = ['Float', 'Int']

// 가능입력일개변수의유형 브라우저대상, excel대상, word대상
export const SINGLE_VAR_TYPE_ARR = ['browser_obj', 'excel', 'doc']

// 생성dom요소의 origin
export const ORIGIN_BUTTON = 'button' // 버튼
export const ORIGIN_VAR = 'var_input' // 변수입력란
export const ORIGIN_SPECIAL = 'special' // 합치기입력란

// 원자 기능정보테이블단일매칭
export const BASE_FORM = [
 {
 formType: {
 type: 'INPUT',
 params: {
 values: [],
 },
 },
 title: '작업이름',
 key: 'baseName',
 noInput: true,
 value: '',
 },
 {
 formType: {
 type: 'INPUT',
 params: {
 values: ['string'],
 },
 },
 title: '작업명',
 key: 'anotherName',
 value: '',
 },
]

// 기본값색상목록
export const DEFAULT_COLOR_LIST = [
 '#ff7e79',
 '#fefe7f',
 '#00ff81',
 '#007ffe',
 '#ff80c0',
 '#ff0104',
 '#00fcff',
 '#847cc2',
 '#fe00fe',
 '#7e0101',
 '#fc7f01',
 '#027e04',
 '#65b2f3',
 '#f9b714',
 '#068081',
 '#8305a1',
 '#b0cf29',
 '#0bfa49',
 '#9e255e',
 '#ffffff',
]

// cv선택매칭정도
export const MATCH_DEGREE = {
 0: i18next.t('matchDegree.fuzzy'),
 95: i18next.t('matchDegree.default'),
 100: i18next.t('matchDegree.exact'),
}

export const SELECT_VALUE_TYPES = [
 ATOM_FORM_TYPE.RADIO,
 ATOM_FORM_TYPE.CHECKBOX,
 ATOM_FORM_TYPE.CHECKBOXGROUP,
 ATOM_FORM_TYPE.SWITCH,
 ATOM_FORM_TYPE.SELECT,
 ATOM_FORM_TYPE.FONTSIZENUMBER,
 ATOM_FORM_TYPE.DEFAULTDATEPICKER,
 ATOM_FORM_TYPE.RANGEDATEPICKER,
]

export const CATEGORY_MAP = {
 process: '하위 프로세스',
 module: '모듈',
}
