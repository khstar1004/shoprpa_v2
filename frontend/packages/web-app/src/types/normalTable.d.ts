// packages/web-app/src/types/normalTable.d.ts

// 지정테이블단일컴포넌트유형
export type FormComponentType
  = | 'input'
    | 'select'
    | 'datePicker'
    | 'rangePicker'
    | 'cascader'
    | 'treeSelect'
    | 'checkbox'
    | 'radio'
    | 'switch'
    | 'back'
    | 'filter'

// 지정버튼유형
export type ButtonType = 'default' | 'primary' | 'dashed' | 'link' | 'text'

// 지정테이블
export type TableSize = 'small' | 'middle' | 'large'

// 지정분
export type PaginationSize = 'small' | 'default'

// 테이블단일구성 연결
export interface FormItemConfig {
  componentType: FormComponentType
  bind: string
  label?: string
  placeholder?: string
  prefix?: any
  suffix?: any
  options?: Array<{
    label: string
    value: any
    disabled?: boolean
  }>
  span?: number
  hidden?: boolean
  disabled?: boolean
  required?: boolean
  rules?: any[]
  defaultValue?: any
  isTrim?: boolean
  [key: string]: any
}

// 버튼구성 연결
export interface ButtonItemConfig {
  label: string
  action?: string
  clickFn?: (...args: any[]) => void
  type?: ButtonType
  hidden?: boolean
  disabled?: boolean
  danger?: boolean
  btnType?: 'button' | 'dropdown'
  options?: Array<{
    key: string
    label: string
    clickFn?: (...args: any[]) => void
  }>
  [key: string]: any
}

// 테이블열매칭연결
export interface TableColumnConfig {
  key: string
  title: string
  dataIndex?: string
  width?: number | string
  ellipsis?: boolean
  sorter?: boolean | ((a: any, b: any) => number)
  resizable?: boolean
  customRender?: (params: { record: any, text: any, index: number }) => any
}

// 테이블속성매칭연결
export interface TablePropsConfig {
  columns: TableColumnConfig[] | Ref<TableColumnConfig[]>
  rowKey?: string
  size?: TableSize
  customRow?: (record: any) => any
  [key: string]: any
}

// 분매개변수매칭연결
export interface PageParamsConfig {
  pageNoName?: string
  pageSizeName?: string
}

// 정렬매개변수매칭연결
export interface OrderParamsConfig {
  orderName?: string
  orderStatus?: string
}

// 분매칭연결
export interface PageConfig {
  total?: number
  current?: number
  pageSize?: number
  pageSizeOptions?: string[]
  size?: PaginationSize
  showSizeChanger?: boolean
  showQuickJumper?: boolean
  [key: string]: any
}

export interface ITableResponse<T = any> {
  records: T[]
  total: number
  [key: string]: any
}

// 데이터가져오기 데이터유형
export type GetDataFunction = (params: Record<string, any>) => Promise<ITableResponse>

// 필요의 tableOption 연결
export interface TableOption {
  // 매칭
  refresh?: boolean
  immediate?: boolean
  page?: boolean
  size?: TableSize
  emptyText?: string

  // class
  headerClass?: string

  // 데이터닫기
  getData: GetDataFunction
  params?: Record<string, any>

  // 테이블단일매칭
  formList?: FormItemConfig[]
  formListAlign?: 'left' | 'right'

  // 버튼매칭
  buttonList?: ButtonItemConfig[]
  buttonListAlign?: 'left' | 'right'

  // 테이블매칭
  tableProps?: TablePropsConfig
  tableCellHeight?: number

  // 분매칭
  pageParams?: PageParamsConfig
  pageConfig?: PageConfig

  // 정렬매칭
  orderParams?: OrderParamsConfig

  [key: string]: any
}
