/** @format */
import type { WebElementType } from './resource.d'

export interface BatchDataTableMenu {
  key: string
  label: string
  active?: boolean
  showType?: 'similar' | 'table'
  children?: BatchDataTableSubMenu[]
}
export interface BatchDataTableSubMenu {
  key: string
  label: string
  checkable?: boolean
  checked?: boolean
  modal?: boolean
  showEdit?: boolean
}

export interface BatchElementDataInfo {
  app: string
  type: string
  version: string
  path: any
  picker_type?: string
}
export interface ElementInfo {
  elementData: string
  id: string
  name: string
}

// 가져오기요소정보
interface PathInfo extends WebElementType {
  produceType: 'similar' | 'table'
}
// 테이블요소정보
interface TableElementInfo extends PathInfo {
  produceType: 'table'
  values: TableValuesType[]
}
// 요소정보
interface SimilarElementsInfo extends WebElementType {
  produceType: 'similar'
  values: SimilarElementInfo[]
}

interface TableValuesType {
  title: string
  filterConfig?: FilterConfig[] // 선택파일
  colFilterConfig?: FilterConfig[] // 열필터링파일
  colDataProcessConfig?: DataProcessConfig[] // 열데이터 처리파일
  value: string[] // 열의데이터
}

interface SimilarElementInfo extends WebElementType {
  value: ValueType[]
  title: string
  filterConfig?: FilterConfig[] // 선택파일
  colFilterConfig?: FilterConfig[] // 열필터링파일
  colDataProcessConfig?: DataProcessConfig[] // 열데이터 처리파일
}

enum ProcessType {
  Replace = 'Replace',
  ExtractNum = 'ExtractNum',
  Trim = 'Trim',
  Prefix = 'Prefix',
  Suffix = 'Suffix',
  FormatTime = 'FormatTime',
  Regular = 'Regular',
}

// 열요소정보
interface TableColumnElementInfo extends SimilarElementsInfo {}

interface TableData2Type {
  tableColumnData: TableColumnElementInfo
  tableData: TableElementInfo
  isTable: true
}

export interface FilterConfig {
  logical: string
  parameter: string
}

export interface DataProcessConfig {
  processType: ProcessType // 관리유형
  isEnable: 0 | 1 // 여부사용, 0:아니오사용, 1:사용
  parameters: Parameter[] // 관리파일
}

interface Parameter { // 문자열의관리파일
  [key: string]: any
}

interface ValueType {
  attrs: Attrs
  text: string
}
interface Attrs {
  src: string
  href: string
  text: string
}

export interface ColumnInfo {
  title: string
  field: string
  dataIndex: string
  [key: string]: any
}
