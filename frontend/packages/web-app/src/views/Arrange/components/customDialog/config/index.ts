import { nanoid } from 'nanoid'

export const limitFormsNum = 50 // 제한제어사용자 지정대화상자사용자의대테이블단일데이터

export const defaultValueConfig = {
 formType: {
 type: 'INPUT_VARIABLE',
 },
 key: 'defaultValue',
 title: '기본값',
 placeholder: '기본값을 입력하세요.',
 default: '',
 value: [
 {
 type: 'other',
 value: '',
 },
 ],
 rpa: 'special',
}
export const pwdDefaultValueConfig = {
 formType: {
 type: 'DEFAULTPASSWORD',
 },
 key: 'defaultValue',
 title: '기본값',
 placeholder: '기본값을 입력하세요.',
 default: '',
 limitLength: [4, 16],
 value: '',
}
export const requiredConfig = {
 formType: {
 type: 'CHECKBOX',
 },
 title: '필수 입력',
 options: [
 {
 label: '예',
 value: true,
 },
 {
 label: '아니오',
 value: false,
 },
 ],
 default: false,
 required: false,
 value: false,
}
export const timeFormatConfig = {
 formType: {
 type: 'SELECT',
 },
 key: 'time_format_select',
 title: '시간형식',
 options: [
 {
 label: '년-월-일',
 value: 'YYYY-MM-DD',
 },
 {
 label: '년-월-일 시:분',
 value: 'YYYY-MM-DD HH:mm',
 },
 {
 label: '년-월-일 시:분:초',
 value: 'YYYY-MM-DD HH:mm:ss',
 },
 {
 label: '년/월/일',
 value: 'YYYY/MM/DD',
 },
 {
 label: '년/월/일 시:분',
 value: 'YYYY/MM/DD HH:mm',
 },
 {
 label: '년/월/일 시:분:초',
 value: 'YYYY/MM/DD HH:mm:ss',
 },
 ],
 default: 'YYYY-MM-DD',
 required: false,
 value: 'YYYY-MM-DD',
}
export const timeDefaultValueConfig = {
 formType: {
 type: 'DEFAULTDATEPICKER',
 params: {
 format: 'YYYY-MM-DD',
 },
 },
 key: 'default_time',
 title: '기본 시간',
 default: '',
 value: '',
}
export const fileTypeConfig = {
 formType: {
 type: 'RADIO',
 },
 key: 'file_type',
 title: '선택 유형',
 options: [
 {
 label: '파일',
 value: 'file',
 },
 {
 label: '폴더',
 value: 'folder',
 },
 ],
 default: 'file',
 value: 'file',
}
export const fileFilterConfig = {
 formType: {
 type: 'SELECT',
 },
 key: 'file_filter_select',
 title: '파일유형',
 options: [
 {
 label: '모든 파일',
 value: '*',
 },
 {
 label: 'Excel 파일',
 value: '.xls,.xlsx',
 },
 {
 label: 'Word 파일',
 value: '.doc,.docx',
 },
 {
 label: '텍스트 파일',
 value: '.txt',
 },
 {
 label: '이미지 파일',
 value: '.png,.jpg,.bmp',
 },
 {
 label: 'PPT 파일',
 value: '.ppt,.pptx',
 },
 {
 label: '압축 파일',
 value: '.zip,.rar',
 },
 ],
 default: '*',
 value: '*',
}
export const defaultFilePathConfig = {
 formType: {
 type: 'INPUT_VARIABLE_PYTHON_FILE',
 params: {
 file_type: 'folder',
 },
 },
 key: 'default_file_path',
 title: '기본 폴더',
 default: '',
 value: [
 {
 type: 'other',
 value: '',
 },
 ],
 rpa: 'special',
}
export const optionsConfig = {
 formType: {
 type: 'OPTIONSLIST',
 },
 key: 'options',
 title: '선택 ',
 default: [],
 required: true,
 value: [{
 rId: nanoid(),
 value: {
 rpa: 'special',
 value: [
 {
 type: 'other',
 value: '선택  1',
 },
 ],
 },
 }],
}
export const defaultSingleSelectConfig = {
 formType: {
 type: 'SELECT',
 },
 key: 'default_option_single_select',
 title: '기본값',
 options: [
 {
 label: '선택  1',
 value: nanoid(),
 },
 ],
 allowReverse: false,
 default: '',
 value: '',
}
export const defaultMultiSelectConfig = {
 formType: {
 type: 'SELECT',
 params: {
 multiple: true,
 },
 },
 key: 'default_option_multi_select',
 title: '기본값',
 options: [
 {
 label: '선택  1',
 value: nanoid(),
 },
 ],
 allowReverse: false,
 default: [],
 value: [],
}
export const directionConfig = {
 formType: {
 type: 'RADIO',
 },
 key: 'direction',
 title: '정렬 방향',
 options: [
 {
 label: '가로 정렬',
 value: 'horizontal',
 },
 {
 label: '세로 정렬',
 value: 'vertical',
 },
 ],
 default: 'horizontal',
 value: 'horizontal',
}
export const fontFamilyConfig = {
 formType: {
 type: 'SELECT',
 },
 key: 'fontFamily',
 title: '글꼴',
 options: [
 {
 label: 'Microsoft YaHei',
 value: 'msyh',
 },
 {
 label: 'SimSun',
 value: 'simsun',
 },
 {
 label: 'SimHei',
 value: 'simhei',
 },
 {
 label: 'FangSong',
 value: 'simfang',
 },
 {
 label: 'Times New Roman',
 value: 'times',
 },
 {
 label: 'KaiTi',
 value: 'KaiTi',
 },
 {
 label: 'LiSu',
 value: 'LiShu',
 },
 {
 label: 'NSimSun',
 value: 'NSimSun',
 },
 {
 label: 'YouYuan',
 value: 'YouYuan',
 },
 {
 label: 'Arial',
 value: 'Arial',
 },
 {
 label: 'Microsoft JhengHei',
 value: 'MicrosoftJhengHei',
 },
 // {
 // label: 'Microsoft YaHei UI',
 // value: 'Microsoft YaHei UI',
 // },
 {
 label: 'Calibri',
 value: 'Calibri',
 },
 ],
 default: 'msyh',
 required: false,
 value: 'msyh',
}
export const fontSizeConfig = {
 formType: {
 type: 'FONTSIZENUMBER',
 },
 key: 'fontSize',
 title: '글자 크기',
 min: 12,
 max: 72,
 step: 2,
 default: 12,
 required: true,
 value: 12,
}
export const fontStyleConfig = {
 formType: {
 type: 'CHECKBOXGROUP',
 },
 key: 'fontStyle',
 title: '글자 스타일',
 options: [
 {
 label: '굵게',
 value: 'bold',
 },
 {
 label: '기울임',
 value: 'italic',
 },
 {
 label: '밑줄',
 value: 'underline',
 },
 ],
 default: [],
 value: [],
}
export const textContentConfig = {
 formType: {
 type: 'INPUT',
 },
 key: 'textContent',
 title: '내용',
 placeholder: '텍스트를 입력하세요.',
 default: '텍스트',
 value: [
 {
 type: 'other',
 value: '텍스트',
 },
 ],
 rpa: 'special',
 required: true,
}
export const fontFamilyMap = {
 msyh: 'font-family: "msyh", "소프트", sans-serif', // 소프트
 simsun: 'font-family: "simsun", "", sans-serif;', // 
 simhei: 'font-family: "simhei", "", sans-serif;', // 
 simfang: 'font-family: "simfang", "", sans-serif;', // 
 times: 'font-family: "times", "Times New Roman", sans-serif;', // Times New Roman
 KaiTi: 'font-family: "KaiTi", "", sans-serif;', // 
 LiShu: 'font-family: "LiShu", "서", sans-serif;', // 서
 NSimSun: 'font-family: "NSimSun", "새", sans-serif;', // 새
 YouYuan: 'font-family: "YouYuan", "", sans-serif;', // 
 Arial: 'font-family: Arial, "Helvetica Neue", Helvetica, sans-serif;', // Arial
 MicrosoftJhengHei: 'font-family: "Microsoft JhengHei", "정상", sans-serif;', // Microsoft JhengHei
 Calibri: 'Calibri, "Helvetica Neue", Helvetica, Arial, sans-serif', // Calibri
}
export const fontStyleMap = {
 bold: 'font-weight: bold',
 italic: 'font-style: italic',
 underline: 'text-decoration: underline',
}
