/** @format */

import { useFlowStore } from '@/stores/useFlowStore'
import { Group, GroupEnd } from '@/views/Arrange/config/atomKeyMap'
import { generateGroupName, generateId, generateVarName, getAllGlobalVariable, getFlowVariable } from '@/views/Arrange/utils/generateData'

// 수정붙여넣기의원자 기능의필드, 패키지id, 분그룹이름, 세트유형닫기 Id
export function changePasteAtoms(arr) {
 const idMap = {} // 저장이전id및새의id의map
 const groupName = []
 arr.forEach((atom) => {
 // 수정id
 const newId = generateId(atom.key)
 idMap[atom.id] = newId
 // 수정분그룹이름
 if (atom.key === Group) {
 const gname = generateGroupName()
 atom.alias = gname
 groupName.push(gname)
 }
 if (atom.key === GroupEnd) {
 atom.alias = groupName.pop()
 }
 atom.id = newId
 })
 useFlowStore().generateContactMap(arr)
 // arr.forEach((atom) => {
 // // 수정분그룹이름
 // if (atom.key === GroupEnd)
 // changeAnotherName(atom, groupNameMap[atom.relationStartId])
 // // 수정세트유형원자 기능의닫기 점
 // if (atom.relationStartId)
 // atom.relationStartId = idMap[atom.relationStartId]
 // if (atom.relationEndId)
 // atom.relationEndId = idMap[atom.relationEndId]
 // })
}

// 붙여넣기, 필요삭제전역 변수
export function delPasteGlobalVar(arr) {
 console.log('arr', arr)
}

// 수정붙여넣기의원자 기능의변수
export function changePasteAtomFlowVar(arr) {
 const varMap = {} // 저장이전변수명및새의변수명의map
 const allFlowVariable = getFlowVariable()
 const allGlobalVariable = getAllGlobalVariable()
 // 완료새의출력변수
 arr.forEach((atom) => {
 atom.outputList.forEach((outItem) => {
 if (outItem.value.length === 1) {
 if (allGlobalVariable.includes(outItem.value[0].value))
 return
 const newVarName = generateVarName(outItem.key, allFlowVariable)
 varMap[outItem.value[0].value] = newVarName
 outItem.value[0].value = newVarName
 }
 })
 })

 // 수정원자 기능입력변수로새완료의출력변수
 arr.forEach((atom) => {
 atom.inputList.forEach((inputItem: any) => {
 if (Array.isArray(inputItem.value)) {
 inputItem.value.forEach((v) => {
 if (varMap[v.value]) {
 v.value = varMap[v.value]
 }
 })
 }
 })
 })
}

export function generatePasteAtoms(clipBoardAtoms) {
 // 수정붙여넣기의원자 기능의필드, 패키지id, 분그룹이름, 세트유형닫기 Id
 changePasteAtoms(clipBoardAtoms)
 // TODO: 복사붙여넣기, 삭제전역 변수
 // delPasteGlobalVar(clipBoardAtoms)
 // 수정붙여넣기의원자 기능변수
 changePasteAtomFlowVar(clipBoardAtoms)
 return clipBoardAtoms
}
