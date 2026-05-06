import type { Bus as Emittery } from '@/utils/eventBus'

import type { ProcessNode } from '@/corobot/type'

import type {
  ElementGroupVM,
  ElementVM,
  GlobalVariableVM,
  ProcessNodeVM,
  ProjectVM,
  PythonPackageVM,
} from './vm'

interface BaseEvent<T, U = undefined> {
  add: [index: number[], data: T | T[], options: U]
  delete: [index: number[], data: T | T[], options: U]
  update: [index: number[], data: T[], options: U]
  [key: string]: any
}

export interface ProcessEvent extends BaseEvent<ProcessNodeVM> {
  open: [processId: string, nodes: ProcessNodeVM[], type: string] // 열기/로드프로세스
  close: [processId: string] // 닫기프로세스
}

export interface ProcessNodeEvent
  extends BaseEvent<ProcessNodeVM, { processId: string }> {}

export interface ElementGroupEvent extends BaseEvent<ElementGroupVM> {}

export interface ElementEvent
  extends BaseEvent<ElementVM, { groupId: string }> {}

export interface GlobalVariableEvent extends BaseEvent<GlobalVariableVM> {}

export interface PythonPackageEvent extends BaseEvent<PythonPackageVM> {}

interface BaseActor<T, U = undefined> {
  add: (index: number[], data: T[], options: U) => void
  delete: (data: string[], options: U) => void
  update: (index: number[], data: T[], options: U) => void
  move: (from: number, to: number, options: U) => void
}

export interface ProcessActor {
  loadProcess: (processId: string) => Promise<void>
  saveProcess: (processId: string) => Promise<void>
  gainProcess: (processId: string) => ProcessNode[] | RPA.Atom[]
  addProcessOrModule: (type: RPA.Flow.ProcessModuleType, name: string) => Promise<string>
  copyProcessOrModule: (type: RPA.Flow.ProcessModuleType, name: string) => Promise<unknown>
  genProcessOrModuleName: (type: RPA.Flow.ProcessModuleType) => Promise<string>
  updateProcessOrModule: (type: RPA.Flow.ProcessModuleType, processId: string, name: string) => Promise<unknown>
  deleteProcessOrModule: (data: RPA.Flow.ProcessModule) => Promise<boolean>
}
export interface ProcessNodeActor
  extends BaseActor<ProcessNodeVM, { processId: string, conditionId?: any }> {
  canUndo: (processId: string) => boolean
  undo: (processId: string) => void
  canRestore: (processId: string) => boolean
  restore: (processId: string) => void
  clear: (processId: string) => void
  clearAll: () => void
}
export interface ElementGroupActor extends BaseActor<ElementGroupVM> {}
export interface ElementActor
  extends BaseActor<ElementVM, { groupId: string }> {}
export interface GlobalVariableActor extends BaseActor<GlobalVariableVM> {}
export interface PythonPackageActor extends BaseActor<PythonPackageVM> {}

/**
 * 개사용완료일개문서, 모든의예개문서의
 * 문서후, 문서데이터의변수변경통신경과 emitter 돌아가기조정서비스
 * 서비스까지돌아가기조정의데이터, 업데이트 UI
 */
export interface IProjectDocument {
  readonly id: string
  // nodeAbilityMap: Record<string, Record<string, any>>
  // MAKR: 로드
  loadProject: () => Promise<ProjectVM>
  loadNodeAbilityList: (list: ProcessNodeVM[], processId: string, type: string) => void
  getProcessNodes: (processId: string) => ProcessNodeVM[]

  // MAKR: 통신경과 emitter 수신데이터의변수
  processEmitter: Emittery<ProcessEvent>
  processNodeEmitter: Emittery<ProcessNodeEvent>
  // 미완료
  elementGroupEmitter?: Emittery<ElementGroupEvent>
  elementEmitter?: Emittery<ElementEvent>
  globalVariableEmitter?: Emittery<GlobalVariableEvent>
  pythonPackageEmitter?: Emittery<PythonPackageEvent>

  // MAKR: 모든의입력, 서비스아니오직선연결데이터
  processActor: ProcessActor
  processNodeActor: ProcessNodeActor
  // 미완료
  elementGroupActor?: ElementGroupActor
  elementActor?: ElementActor
  globalVariableActor?: GlobalVariableActor
  pythonPackageActor?: PythonPackageActor
}