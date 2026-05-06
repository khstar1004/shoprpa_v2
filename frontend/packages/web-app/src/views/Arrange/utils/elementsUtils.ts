/** @format */

import { generateUUID } from '@/utils/common'

import type { CustomValueType, DirectoryAttrItem, DirectoryItem, EleVariableType, VarDataType, WebElementType } from '@/types/resource'
import { PATTERN_RULES, PATTERN_RULES_TYPE, PATTERN_RULES_UIA } from '@/views/Arrange/config/pick'

export type ElementT = 'uia' | 'web' | 'cv' | 'jab' | 'sap'

/**
 * 요소정보형식변환
 * @param version - version of the format
 * @param type - type of the format
 * @param data - data to format
 * @returns - formatted data
 */
export function elementDirectoryFormat(version: string = '1', type: ElementT, data: any) {
 const vFns = {
 1: elementDirectoryFormatV1,
 }
 return vFns[version](type, data)
}
/**
 * 요소정보형식복사
 * @param version 버전
 * @param type 유형
 * @param data 데이터
 * @returns 기존데이터형식
 */
export function elementDirectoryFormatRecover(version: string = '1', type: ElementT, data: any) {
 const vFns = {
 1: elementDirectoryFormatV1Recover,
 }
 return vFns[version](type, data)
}

// 가능데이터 v1
function elementDirectoryFormatV1(type: ElementT, data: any) {
 const tFns = {
 uia: elementDirectoryFormatV1Uia,
 web: elementDirectoryFormatV1Web,
 jab: elementDirectoryFormatV1Jab,
 }
 if (tFns[type] === undefined) { // 만약있음대상의관리함수데이터, 반환통신사용관리
 return elementDirectoryFormatV1Common(data, 'v1', type)
 }
 return tFns[type] && tFns[type](data)
}
// 가능데이터 v1 복사
function elementDirectoryFormatV1Recover(type: ElementT, data: any) {
 const tFns = {
 uia: elementDirectoryFormatV1UiaRecover,
 web: elementDirectoryFormatV1WebRecover,
 jab: elementDirectoryFormatV1JabRecover,
 }
 if (tFns[type] === undefined) { // 만약있음대상의관리함수데이터, 반환통신사용관리
 return elementDirectoryFormatV1RecoverCommon(data)
 }
 return tFns[type] && tFns[type](data)
}
// 공개공유형식함수데이터
function elementDirectoryFormatV1Common(
 data: any,
 version: string,
 typesPatternType: string,
) {
 if (!data)
 return []
 const ignoreKeys = ['checked', 'tag_name', 'disable_keys']
 return data.map((item: DirectoryItem) => {
 const attrKeys = Object.keys(item).filter(key => !ignoreKeys.includes(key))
 return {
 _version: version,
 _checkDisabled: false,
 _addDisabled: false,
 _deleteDisabled: false,
 tag: item.tag_name,
 checked: item.checked !== false,
 value: item.tag_name,
 attrs: attrKeys.map((key, index) => {
 const attr = {
 _checkDisabled: false,
 _addDisabled: false,
 _deleteDisabled: false,
 _typeDisabled: false,
 _nameDisabled: false,
 _typesPattern: typesPattern(typesPatternType, key),
 value: item[key],
 type: 0,
 name: key,
 checked: item.disable_keys ? !item.disable_keys.includes(key) : true,
 variableValue: null,
 }
 attr.variableValue = variableFormatValue(attr, index)
 return attr
 }),
 }
 })
}

// 공개공유복사함수데이터
function elementDirectoryFormatV1RecoverCommon(data: any) {
 if (!data)
 return []
 return data.map((item: any) => {
 const attrs = item.attrs
 const attrObj: any = {
 tag_name: item.value,
 checked: item.checked,
 disable_keys: [],
 }
 attrs.forEach((attr: any) => {
 const variableValue = attr.variableValue
 ? varibaleValueFormatSave(attr.variableValue.value)
 : attr.value
 attrObj[attr.name] = variableValue
 if (!attr.checked) {
 attrObj.disable_keys.push(attr.name)
 }
 })
 return attrObj
 })
}

// 가능데이터 v1 uia
function elementDirectoryFormatV1Uia(data: any) {
 return elementDirectoryFormatV1Common(data, 'uia_1', 'uia_1')
}
// 가능데이터 v1 uia 복사
function elementDirectoryFormatV1UiaRecover(data: DirectoryItem[]) {
 return elementDirectoryFormatV1RecoverCommon(data)
}
// 가능데이터 v1 jab
function elementDirectoryFormatV1Jab(data: any) {
 return elementDirectoryFormatV1Common(data, 'jab_1', 'jab_1')
}
// 가능데이터 v1 jab 복사
function elementDirectoryFormatV1JabRecover(data: any) {
 return elementDirectoryFormatV1RecoverCommon(data)
}
// 가능데이터 v1 web
function elementDirectoryFormatV1Web(data: { pathDirs: DirectoryItem[] }) {
 if (!data || !data.pathDirs)
 return []
 return data.pathDirs.map((item: DirectoryItem) => {
 return {
 _version: 'web_1', // 버전
 _checkDisabled: item.tag === '$shadow$', // 여부사용 안 함선택
 _addDisabled: false, // 여부사용 안 함추가
 _deleteDisabled: item.tag === '$shadow$', // false, // 여부사용 안 함삭제
 tag: item.value || item.tag,
 checked: item.checked === true,
 value: item.value || item.tag,
 attrs: item.attrs.map((attr: DirectoryAttrItem, index: number) => {
 return {
 variableValue: variableFormatValue(attr, index),
 // _checkDisabled: true, // 여부사용 안 함선택
 // _addDisabled: true, // 여부사용 안 함추가
 _deleteDisabled: true, // 여부사용 안 함삭제
 // _typeDisabled: true, // 여부사용 안 함유형수정
 _nameDisabled: true, // 여부사용 안 함이름수정
 _typesPattern: typesPattern('web_1', attr.name),
 value: attr.value,
 type: attr.type || 0,
 name: attr.name,
 checked: attr.checked === true,
 }
 }),
 }
 })
}
// 가능데이터 v1 web 복사
function elementDirectoryFormatV1WebRecover(data: DirectoryItem[]) {
 if (!data)
 return []
 return data.map((item: DirectoryItem) => {
 return {
 tag: item.value || item.tag,
 checked: item.checked,
 value: item.value,
 attrs: item.attrs.map((attr: DirectoryAttrItem & { variableValue: EleVariableType }) => {
 const variableValue = attr.variableValue ? varibaleValueFormatSave(attr.variableValue.value) : attr.value
 return {
 name: attr.name,
 value: variableValue,
 checked: attr.checked,
 type: attr.type,
 }
 }),
 }
 })
}
// 매칭이면, uia 있음대기, web후계속필요지원다중종
export function typesPattern(type: string, name: string) {
 const tpMap = {
 uia_1: PATTERN_RULES_UIA,
 web_1: PATTERN_RULES,
 jab_1: PATTERN_RULES_UIA,
 }
 const rules = tpMap[type] || []
 if (name === 'index') {
 // index 유형로숫자 있음대기건파일
 return rules.filter(item => item.value === 0)
 }
 return rules.length > 0 ? rules : PATTERN_RULES_UIA // 만약있음대상의이면, 이면사용기본값이면
}

/**
 * 반환attrs 선택중의속성
 */
export function checkedValue(tag: string, attrs: any[]) {
 //
 let str = ''
 str += `<${tag}`
 attrs.forEach((item) => {
 if (item.checked) {
 str += ` ${item.name}${PATTERN_RULES_TYPE[item.type]}"${item.value}"`
 }
 })
 str += ` />`
 return str
}
/**
 * custom 사용자 지정의데이터형식
 * @param version 버전
 * @param type 유형
 * @param data 기존데이터
 * @returns 새데이터
 */
export function elementCustomFormat(version: string = '1', type: ElementT, data: any) {
 const vFns = {
 1: elementCustomFormatV1,
 }
 return vFns[version](type, data)
}
// 사용자 지정데이터 복사
export function elementCustomFormatRecover(version: string = '1', type: ElementT, data: any) {
 if (!data)
 return {}
 const vFns = {
 1: elementCustomFormatV1Recover,
 }
 return vFns[version](type, data)
}

/**
 * 사용자 지정데이터형식, v1 버전
 */
function elementCustomFormatV1(type: ElementT, data: any) {
 const tFns = {
 // uia: null, // 필요 없음관리
 web: elementCustomFormatV1Web,
 }
 return tFns[type] && tFns[type](data)
}
// 사용자 지정데이터 v1 web 형식
function elementCustomFormatV1Web(data: WebElementType) {
 if (!data)
 return []
 const { xpath, cssSelector, shadowRoot, url, isFrame, iframeXpath } = data
 const resArr = [
 {
 name: 'url',
 value: url,
 },
 {
 name: 'xpath',
 value: xpath,
 },
 {
 name: 'cssSelector',
 value: cssSelector,
 },
 ]
 if (shadowRoot) {
 // shadowRoot 사용 cssSelector 매칭, 아니오사용 xpath
 // 삭제 xpath 일
 const xapthIndex = resArr.findIndex(item => item.name === 'xpath')
 resArr.splice(xapthIndex, 1)
 }
 if (isFrame) {
 const urlIndex = resArr.findIndex(item => item.name === 'url')
 resArr.splice(urlIndex, 1) // 삭제 url
 resArr.unshift({
 name: 'iframeXpath',
 value: iframeXpath,
 })
 }
 const res = customValueFormatVariable(resArr)
 return res
}
// 사용자 지정데이터 복사
function elementCustomFormatV1Recover(type: ElementT, data: any) {
 const tFns = {
 // uia: elementCustomFormatV1Uia,
 web: elementCustomFormatV1WebRecover,
 }
 return tFns[type] && tFns[type](data)
}
// 사용자 지정데이터 web 복사
function elementCustomFormatV1WebRecover(data: any[]) {
 const obj = {}
 data.forEach((item) => {
 const variableObj = varibaleValueFormatSave(item.value)
 obj[item.name] = variableObj
 })
 return obj
}

/**
 * 를배열데이터형식로 지원변수의방식데이터
 */
function customValueFormatVariable(arr: CustomValueType[]) {
 return arr.map((item, index) => {
 return variableFormatValue(item, index)
 })
}
// 를데이터형식로 지원변수의방식데이터
// "xpath": {
// "rpa": "special",
// "value": [
// {
// "type": "var",
// "value": "var1"
// },
// {
// "type": "other",
// "value": "//div[@id='test']"
// }
// ]
// }
function variableFormatValue(item: DirectoryAttrItem | CustomValueType, index: number) {
 const variableValue = makeVaribaleValue(item.value) // 변환완료지원변수의형식
 return {
 types: 'Any',
 default: '',
 rowIdx: index,
 formType: {
 type: 'INPUT_VARIABLE',
 params: {
 values: [],
 },
 },
 key: item.name,
 title: item.name,
 name: item.name,
 value: variableValue,
 show: true,
 uniqueKey: generateUUID(),
 }
}

/**
 * 제어변수
 */
function makeVaribaleValue(val: EleVariableType | any) {
 // 중단 val 여부예 {rpa: 'special'} 형식의대상
 const isVariable = val !== null
 && typeof val === 'object'
 && 'rpa' in val
 && val.rpa === 'special'
 const variableValue = isVariable
 ? val.value
 : [
 { type: 'other', value: val === null ? '' : val },
 ] // 변환완료지원변수의형식
 return variableValue
}
/**
 * 저장변수시형식
 */
function varibaleValueFormatSave(value: VarDataType[]) {
 return {
 rpa: 'special',
 value,
 }
}

export function addAttr(v: string, index: number) {
 const attr = {
 name: '',
 type: 0,
 value: '',
 checked: false,
 _typesPattern: typesPattern(v, ''),
 variableValue: null,
 }
 attr.variableValue = variableFormatValue(attr, index)
 return attr
}

export function addNode(v: string, originNode: DirectoryItem) {
 const node = {
 _version: v,
 tag: originNode.tag,
 checked: true,
 value: originNode.value,
 attrs: [],
 }
 return node
}

// elementAction 필터링, 필터링이면예만약more의menus중의매일에서외부, 이면필터링외부에서menus중의데이터, 만약more중있음일, 외부패키지, 이면필터링more
export function filterActionData(data, actions) {
 const actionData = data.filter(i => actions.includes(i.key) || i.menus.some(item => actions.includes(item.key))).map((i) => {
 if (i.menus) {
 i.menus = i.menus.filter(item => actions.includes(item.key))
 }
 return i
 })
 const moreItem = actionData.find(item => item.key === 'more')
 if (!moreItem)
 return actionData

 const moreMenusKeys = moreItem.menus.map(item => item.key)
 const externalItems = actionData.filter(item => item.key !== 'more')

 // 만약more중있음일, 외부패키지, 이면필터링more
 if (moreItem.menus.length === 1 && externalItems.some(extItem => extItem.key === moreItem.menus[0].key)) {
 return actionData.filter(item => item.key !== 'more')
 }

 // 조회more중의menus여부에서외부
 // const allInMore = moreMenusKeys.every(key => externalItems.some(extItem => extItem.key === key));

 // 만약more중의매일에서외부, 이면필터링외부에서menus중의데이터
 // if (allInMore) {
 return actionData.filter(item => item.key === 'more' || !moreMenusKeys.includes(item.key))
 // }

 // return actionData;
}
