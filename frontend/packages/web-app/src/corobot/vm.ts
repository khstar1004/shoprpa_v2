import type { Element, ElementGroup, GlobalVariable, Process, ProcessNode, Robot as Project, PythonPackage } from './type'

/**
 * NOTE: VM 테이블 ViewModel, 예서비스의데이터.
 * 필요예로완료분, 중지서비스및, 예에서서비스직선연결수정의데이터.
 * 현재예단일로완료 readonly, 완료수정보관.
 * 후가능으로근거서비스필요, 행, 또는변환.
 * !!! 로완료메모리, 현재있음, 으로서비스필요격식제어데이터의수정, 할 수 없음경과.
 */

export type ElementVM = Readonly<Element>
export type ElementGroupVM = Readonly<ElementGroup>
export type GlobalVariableVM = Readonly<GlobalVariable>
export type ProcessVM = Readonly<Process>
export type ProcessNodeVM = Readonly<ProcessNode>
export type PythonPackageVM = Readonly<PythonPackage>
export type ProjectVM = Readonly<Project>
