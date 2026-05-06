import type { VNode } from 'vue'

// 목록유형
export interface resourceListType {
  key: string
  value: string
}

export interface ElementsPure {
  name: string
  key?: string
}

export interface ElementsT extends ElementsPure {
  type: string
  robotId: string
  groupName: string
  element: ElementsType
}

export interface ElementsType extends ElementsPure {
  id?: string // 생성시있음id
  name: string
  imageId?: string
  parentImageId?: string // 이미지id
  icon?: string
  elementData?: string
  groupName?: string // 그룹이름
  groupId?: string // 그룹id
  imageUrl?: string
  parentImageUrl?: string // 이미지주소
  elements?: ElementsType[]
  commonSubType?: string
}

export interface ElementsTree extends ElementsPure {
  name: string
  id: string
  elements: ElementsType[]
  groupName?: string
  icon?: string
  key?: string
}

// 지정데이터 유형
export interface CustomValueType {
  name: string
  value: string
}
// 가능데이터속성유형
export interface DirectoryAttrItem {
  name: string
  value: string
  type: number
  checked: boolean
}
// 가능데이터 유형
export interface DirectoryItem {
  tag_name?: string // uia 예tag_name
  tag: string
  checked: boolean
  value: string
  attrs?: DirectoryAttrItem[]
  name?: string
  disable_keys?: string[]
}

export interface VarDataType {
  type: string
  value: string
}
export interface EleVariableType {
  rpa: 'special'
  value: VarDataType[]
}

export interface WebElementType {
  xpath: string
  cssSelector: string
  pathDirs?: Array<DirectoryItem>
  parentClass: string
  domain: string
  url: string
  shadowRoot: boolean
  tabTitle?: string
  tabUrl?: string
  favIconUrl?: string
  isFrame?: boolean
  checkType?: string
  frameId?: number
  iframeXpath?: string
  iframeCssSelector?: string
  tag?: string
  text?: string
}

export interface PickElementType {
  app: string
  path: DirectoryItem[] | WebElementType
  type: ElementType
  version?: string
  img?: {
    self: string
    parent: string
  }
  picker_type?: PickerType
  relative_path?: DirectoryItem[] // uia 의요소저장에서
  similar_count?: number // 요소개데이터
}

export interface PickParams {
  pick_sign: 'START' | 'VALIDATE' | 'RECORD'
  pick_type?: string
  record_action?: RecordAction
  data: string
  ext_data?: any
  pick_mode?: string // 가능선택, 선택시지정/web대기
}

/**
 * 기록제어
 * RECORD_LISTENING: 열기시작기록제어
 * RECORD_START: 기록제어열기 
 * RECORD_PAUSE: 기록제어일시중지
 * RECORD_END: 기록제어결과
 * RECORD_AUTOMIC_HOVER_START: 프론트엔드창hover알림알림백엔드닫기선택
 * RECORD_AUTOMIC_END: 선택기존가능결과 - 프론트엔드전송정보, 백엔드알림알림프론트엔드선택정보
 * RECORD_AUTOMIC_HOVER_END: 마우스중지초요소 - 프론트엔드알림알림백엔드hover결과열기시작선택
 */
export type RecordAction = 'RECORD_LISTENING' | 'RECORD_START' | 'RECORD_PAUSE' | 'RECORD_AUTOMIC_HOVER_START' | 'RECORD_AUTOMIC_END' | 'RECORD_AUTOMIC_HOVER_END' | 'RECORD_END'

export type PickStepType = 'new' | 'repick' | 'similar' | 'anchor'

export interface PickUseItemType {
  processId: string
  processName: string
  open?: boolean
  atoms?: Array<any>
}

type ElementType = 'uia' | 'web' | 'cv' | 'jab' | 'sap'
type PickerType = 'ELEMENT' | 'WINDOW' | 'POINT' | 'SIMILAR' | 'OTHERS'

export interface ElementData {
  version: string
  type: ElementType
  app: string
  path: any // uia 및 web , cv 로빈
  img: {
    self: string // 이미지ID
    parent: string // 이미지ID
  }
  pos?: Record<string, number | string> // 위치
  sr?: Record<string, number> // 분
  picker_type: PickerType
}

export interface Element {
  id?: string
  name: string
  version?: string // elementData 버전
  type?: ElementType
  app?: string // 사용이름, type로CV시, 해당필드없음.
  imageUrl?: string // 현재요소의이미지
  parentImageUrl?: string // 요소의이미지, 사용위치 지정.type로CV시, 해당필드로.
  elementData?: string // 기존의요소데이터, ElementData 의 JSON 문자열
  imageId?: string
  parentImageId?: string
}

export interface ElementGroup {
  id: string
  name: string
  elements: Element[]
}

export type ElementActionType = 'edit' | 'searchUse' | 'move' | 'delete' | 'copy' | 'repick' | 'copy-references' | 'quoted'

export type GroupContextMenuType = 'add' | 'rename' | 'delete' | 'elementPick'

export interface MenuItem {
  key: string
  label: string
  icon?: VNode
  type?: T
  menus?: Array<MenuItem>
  children?: Array<MenuItem>
}