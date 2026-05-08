// 변수유형현재에서 atom-meta.json
export type VariableType = RPA.VariableType

export type ArgumentValueType = 'other' | 'python' | 'var' // atom-meta 있음 str, 완료사용.필요프론트엔드있음있음사용까지.
export interface ArgumentValue {
  type: string
  value: string
  data?: string // 요소정보
  varId?: string
  varName?: string
  varValue?: any
}

export interface NodeArgument {
  key: string
  show?: boolean
  value: string | number | boolean | ArgumentValue[]
}

export interface ProcessNode extends Record<string, any> {
  key: string // 기존가능의key
  version: string // 사용기존가능의버전
  id: string // 일ID.아니오예의?
  inputList: NodeArgument[] // 값가능아니오, 아래
  advanced: NodeArgument[]
  exception: NodeArgument[]
  outputList: NodeArgument[]
  alias: string // 이름
  disabled?: boolean // 여부사용 안 함
  breakpoint?: boolean // 여부
  // collapsed: boolean
}

/**
 * 프로세스매개변수, 사용완료, 시사용까지
 */
export interface ProcessVariable {
  id: string
  name: string
  type: VariableType
  value: string
  desc: string
  direction: 'input' | 'output'
}

export interface Process {
  id: string
  name: string
  nodes: ProcessNode[]
  // variables: ProcessVariable[] // 시아니오사용
}

export interface GlobalVariable {
  id: string
  name: string
  type: VariableType
  value: string
  desc: string
}

/**
 * 변수, 사용본저장비밀번호대기정보, 서버아니오저장 value
 */
export interface EnvVariable {
  id: string
  name: string
  type: VariableType
  value: null | string
  desc: string
}

export interface PythonPackage {
  name: string
  version: string
  mirror: string
}

export type ElementType = 'uia' | 'web' | 'cv' | 'jab'
export type PickerType = 'ELEMENT' | 'WINDOW' | 'POINT' | 'SIMILAR' | 'OTHERS'

export interface ElementData {
  version: string
  type: ElementType
  app: string
  path: any // uia 및 web , cv 로빈
  img: {
    self: string // 이미지ID
    parent: string // 이미지ID
  }
  pos?: Record<string, number> // 위치
  sr?: Record<string, number> // 분
  picker_type: PickerType
}

export interface Element {
  id: string
  name: string
  version: string // elementData 버전
  type: ElementType
  app: string // 사용이름, type로CV시, 해당필드없음.
  imageUrl: string // 현재요소의이미지
  parentImageUrl: string // 요소의이미지, 사용위치 지정.type로CV시, 해당필드로.

  elementData: string // 기존의요소데이터, ElementData 의 JSON 문자열
}

export interface ElementGroup {
  id: string
  name: string
  elements: Element[]
}

export interface Robot {
  id: string
  name: string

  processes: RPA.Flow.ProcessModule[]
  packages: PythonPackage[]

  global: GlobalVariable[]
  env: EnvVariable[]

  elements: ElementGroup[]
  images: ElementGroup[]
}
