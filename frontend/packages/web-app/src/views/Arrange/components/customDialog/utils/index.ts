import { cloneDeep } from 'lodash-es'

import {
 defaultFilePathConfig,
 defaultMultiSelectConfig,
 defaultSingleSelectConfig,
 defaultValueConfig,
 directionConfig,
 fileFilterConfig,
 fileTypeConfig,
 fontFamilyConfig,
 fontSizeConfig,
 fontStyleConfig,
 optionsConfig,
 pwdDefaultValueConfig,
 requiredConfig,
 textContentConfig,
 timeDefaultValueConfig,
 timeFormatConfig,
} from '../config/index.ts'
import type { FormItemConfig } from '../types'

function getLabelConfig(dialogFormName) {
 return {
 formType: {
 type: 'INPUT',
 },
 title: `${dialogFormName} 제목`,
 default: 'dialogFormName',
 value: [
 {
 type: 'other',
 value: dialogFormName,
 },
 ],
 rpa: 'special',
 required: true,
 }
}
function getPlaceholderConfig(defaultPlaceholder) { // 입력란, 비밀번호, 파일선택, 단일선택다중선택드롭다운
 return {
 formType: {
 type: 'INPUT',
 },
 key: 'placeholder',
 title: 'placeholder',
 placeholder: 'placeholder를 입력하세요.',
 default: defaultPlaceholder,
 value: [
 {
 type: 'other',
 value: defaultPlaceholder,
 },
 ],
 rpa: 'special',
 }
}
function getBindConfig(bindKey) {
 return {
 formType: {
 type: 'INPUT',
 params: {
 values: [
 'string',
 ],
 },
 },
 title: '출력 변수 이름',
 key: bindKey,
 value: [
 {
 type: 'str',
 value: `${bindKey}_1`,
 },
 ],
 rpa: 'special',
 }
}

// 대화상자지원의테이블단일매칭
export const dialogFormConfig: Array<FormItemConfig> = [
 {
 id: '1',
 dialogFormType: 'INPUT',
 dialogFormName: '입력란',
 // configKeys: ['label', 'placeholder', 'defaultValue', 'bind', 'required', 'mockField'],
 configKeys: ['label', 'placeholder', 'defaultValue', 'bind', 'required'],
 label: getLabelConfig('입력란'),
 placeholder: getPlaceholderConfig('텍스트 내용을 입력하세요.'),
 defaultValue: defaultValueConfig,
 bind: getBindConfig('input_box'),
 required: requiredConfig,
 // mockField: {
 // types: 'Str',
 // formType: {
 // type: 'SCRIPTPARAMS',
 // },
 // key: 'params',
 // name: 'params',
 // title: '매개변수관리관리',
 // tip: '입력본닫기의매개변수관리관리',
 // value: '[]',
 // required: true,
 // },
 // mockField: {
 // types: 'Any',
 // formType: {
 // type: 'CONTENTPASTE',
 // },
 // key: 'email_body',
 // title: '정상문서',
 // name: 'email_body',
 // tip: '정상문서닫기tip',
 // required: true,
 // value: [
 // {
 // type: 'other',
 // value: '',
 // },
 // ],
 // },
 // mockField: {
 // types: 'Any',
 // formType: {
 // type: 'MODALBUTTON',
 // },
 // key: 'replace_text',
 // title: '교체문서문자',
 // name: 'replace_text',
 // tip: '',
 // required: false,
 // value: '',
 // },
 // mockField: {
 // types: 'Any',
 // formType: {
 // type: 'MOUSEPOSITION',
 // },
 // key: 'mouse_position',
 // title: '가져오기좌표위치',
 // name: 'mouse_position',
 // tip: '',
 // required: false,
 // value: '',
 // },
 },
 {
 id: '2',
 dialogFormType: 'PASSWORD',
 dialogFormName: '비밀번호',
 configKeys: ['label', 'placeholder', 'defaultValue', 'bind', 'required'],
 label: getLabelConfig('비밀번호'),
 placeholder: getPlaceholderConfig('비밀번호를 입력하세요.'),
 defaultValue: pwdDefaultValueConfig,
 bind: getBindConfig('password_box'),
 required: requiredConfig,
 },
 {
 id: '3',
 dialogFormType: 'DATEPICKER',
 dialogFormName: '날짜시간',
 configKeys: ['label', 'format', 'defaultValue', 'bind', 'required'],
 label: getLabelConfig('날짜시간'),
 format: timeFormatConfig,
 defaultValue: timeDefaultValueConfig,
 bind: getBindConfig('datepicker_box'),
 required: requiredConfig,
 conditionalFnKey: 'DATEPICKER_LINK',
 },
 {
 id: '4',
 dialogFormType: 'PATH_INPUT',
 dialogFormName: '파일선택',
 configKeys: ['label', 'selectType', 'filter', 'placeholder', 'defaultPath', 'bind', 'required'],
 label: getLabelConfig('파일선택'),
 selectType: fileTypeConfig,
 filter: fileFilterConfig,
 placeholder: getPlaceholderConfig('파일을 선택하세요.'),
 defaultPath: defaultFilePathConfig,
 bind: getBindConfig('path_input_box'),
 required: requiredConfig,
 conditionalFnKey: 'PATH_INPUT_LINK',
 },
 {
 id: '5',
 dialogFormType: 'RADIO_GROUP',
 dialogFormName: '라디오 버튼',
 configKeys: ['label', 'options', 'defaultValue', 'direction', 'bind', 'required'],
 label: getLabelConfig('라디오 버튼'),
 options: optionsConfig,
 defaultValue: defaultSingleSelectConfig,
 direction: directionConfig,
 bind: getBindConfig('radio_box'),
 required: requiredConfig,
 conditionalFnKey: 'OPTIONS_SINGLE_LINK',
 },
 {
 id: '6',
 dialogFormType: 'CHECKBOX_GROUP',
 dialogFormName: '체크박스',
 configKeys: ['label', 'options', 'defaultValue', 'direction', 'bind', 'required'],
 label: getLabelConfig('체크박스'),
 options: optionsConfig,
 defaultValue: defaultMultiSelectConfig,
 direction: directionConfig,
 bind: getBindConfig('check_box'),
 required: requiredConfig,
 conditionalFnKey: 'OPTIONS_MULTI_LINK',
 },
 {
 id: '7',
 dialogFormType: 'SINGLE_SELECT',
 dialogFormName: '단일 선택 드롭다운',
 configKeys: ['label', 'options', 'placeholder', 'defaultValue', 'bind', 'required'],
 label: getLabelConfig('단일 선택 드롭다운'),
 options: optionsConfig,
 placeholder: getPlaceholderConfig('을 선택하세요.'),
 defaultValue: defaultSingleSelectConfig,
 bind: getBindConfig('single_select_box'),
 required: requiredConfig,
 conditionalFnKey: 'OPTIONS_SINGLE_LINK',
 },
 {
 id: '8',
 dialogFormType: 'MULTI_SELECT',
 dialogFormName: '다중 선택 드롭다운',
 configKeys: ['label', 'options', 'placeholder', 'defaultValue', 'bind', 'required'],
 label: getLabelConfig('다중 선택 드롭다운'),
 options: optionsConfig,
 placeholder: getPlaceholderConfig('하나 이상의 을 선택하세요.'),
 defaultValue: defaultMultiSelectConfig,
 bind: getBindConfig('multi_select_box'),
 required: requiredConfig,
 conditionalFnKey: 'OPTIONS_MULTI_LINK',
 },
 {
 id: '9',
 dialogFormType: 'TEXT_DESC',
 dialogFormName: '텍스트',
 configKeys: ['fontFamily', 'fontSize', 'fontStyle', 'textContent'],
 fontFamily: fontFamilyConfig,
 fontSize: fontSizeConfig,
 fontStyle: fontStyleConfig,
 textContent: textContentConfig,
 },
]

// 동작관리함수데이터
export const conditionalFn = {
 DATEPICKER_LINK(selectedItem) {
 const { format } = selectedItem
 selectedItem.defaultValue.formType.params.format = format.value
 return selectedItem
 },
 PATH_INPUT_LINK(selectedItem) {
 const { selectType } = selectedItem
 if (selectType.value === 'file') {
 selectedItem.configKeys = ['label', 'selectType', 'filter', 'placeholder', 'defaultPath', 'bind', 'required']
 selectedItem.filter = fileFilterConfig
 selectedItem.label = getLabelConfig('파일선택')
 selectedItem.placeholder = getPlaceholderConfig('파일을 선택하세요.')
 }
 else {
 selectedItem.configKeys = ['label', 'selectType', 'placeholder', 'defaultPath', 'bind', 'required']
 delete selectedItem.filter
 selectedItem.label = getLabelConfig('폴더 선택')
 selectedItem.placeholder = getPlaceholderConfig('폴더를 선택하세요.')
 }
 return selectedItem
 },
 OPTIONS_SINGLE_LINK(selectedItem) {
 selectedItem.defaultValue.options = cloneDeep(selectedItem.options.value)
 const index = selectedItem.defaultValue.options.findIndex(i => i.rId === selectedItem.defaultValue.value)
 if (index === -1) {
 selectedItem.defaultValue.allowReverse = true
 selectedItem.defaultValue.value = ''
 }
 return selectedItem
 },
 OPTIONS_MULTI_LINK(selectedItem) {
 selectedItem.defaultValue.options = cloneDeep(selectedItem.options.value)
 selectedItem.defaultValue.allowReverse = true
 selectedItem.defaultValue.value = selectedItem.defaultValue.value.filter(i => selectedItem.defaultValue.options.some(j => j.rId === i))
 return selectedItem
 },
}

// 변환성공테이블단일팝업필요의데이터
export function transDataForPreview(dialogData) { // 사용자 지정대화상자보기필요의데이터변환
 const formModel = { result_button: 'confirm' }
 const itemList = dialogData?.formList.map((item) => {
 const { configKeys } = item
 const res = { dialogFormType: item.dialogFormType } as FormItemConfig
 configKeys.forEach((key) => {
 if (key === 'options') { // 선택컴포넌트관리
 res[key] = item.options.value
 }
 else if (key === 'defaultPath') { // 폴더기본값값지원변수입력으로지원하지 않음보기
 res[key] = ''
 }
 else {
 res[key] = Array.isArray(item[key]?.value) && !['fontStyle', 'defaultValue'].includes(key) ? item[key]?.value[0]?.value : item[key]?.value
 }
 })
 if (item.dialogFormType !== 'TEXT_DESC') {
 formModel[res.bind] = res?.defaultValue
 }
 return JSON.parse(JSON.stringify(res)) // 값로undefined의필드
 })
 return {
 itemList,
 formModel,
 }
}

// 계획까지정상의bindKey의Index
export function getRightIndex(dialogDataArr: FormItemConfig[], bindKey: string) {
 const filterArr = dialogDataArr?.filter(i => i.bind && i.bind.value[0].value.includes(bindKey))
 if (!filterArr.length)
 return 1
 return Number(filterArr
 .map(item => item.bind.value[0].value.replace(`${bindKey}_`, ''))
 .filter(i => /^\d*$/.test(i))
 .sort((a, b) => b - a)[0]) + 1
}
