/** @format */

import { useFlowStore } from '@/stores/useFlowStore'
import { Group, GroupEnd } from '@/views/Arrange/config/atomKeyMap'
import { generateGroupName, generateId, generateVarName, getAllGlobalVariable, getFlowVariable } from '@/views/Arrange/utils/generateData'

// Regenerate IDs and group names for pasted atoms.
export function changePasteAtoms(arr) {
  const idMap = {} // 저장이전id및새의id의map
  const groupName = []
  arr.forEach((atom) => {
    const newId = generateId(atom.key)
    idMap[atom.id] = newId
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

// Reserved for removing pasted global variables when needed.
export function delPasteGlobalVar(_arr) {
}

// Regenerate pasted atom output variable names and remap dependent inputs.
export function changePasteAtomFlowVar(arr) {
  const varMap = {} // 저장이전변수명및새의변수명의map
  const allFlowVariable = getFlowVariable()
  const allGlobalVariable = getAllGlobalVariable()
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
  changePasteAtoms(clipBoardAtoms)
  // delPasteGlobalVar(clipBoardAtoms)
  changePasteAtomFlowVar(clipBoardAtoms)
  return clipBoardAtoms
}
