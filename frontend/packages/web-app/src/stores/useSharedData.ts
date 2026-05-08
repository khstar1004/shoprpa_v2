import { useAsyncState } from '@vueuse/core'
import { defineStore } from 'pinia'

import { getRemoteFiles, getRemoteParams } from '@/api/atom'

interface ISharedVariableType {
  id: number
  sharedVarName: string
  sharedVarType: string
  sharedVarValue: string
  subVarList: {
    varName: string
    varType: string
    varValue: string
  }[]
}

function fetchSharedVariables(): Promise<RPA.SharedVariableType[]> {
  return getRemoteParams<ISharedVariableType>().then(data =>
    data.map(item => ({
      value: item.id,
      label: item.sharedVarName,
      subVarList: item.subVarList || [],
    })),
  )
}

function fetchSharedFiles(): Promise<RPA.SharedFileType[]> {
  return getRemoteFiles({ pageSize: 1000 }).then(data => data.records)
}

export const useSharedData = defineStore('sharedData', () => {
  // 공유데이터목록
  const { state: sharedVariables, execute: getSharedVariables } = useAsyncState(
    fetchSharedVariables,
    [],
    { resetOnExecute: false },
  )

  // 공유파일목록
  const { state: sharedFiles, execute: getSharedFiles } = useAsyncState(
    fetchSharedFiles,
    [],
    { resetOnExecute: false },
  )

  return { sharedVariables, getSharedVariables, sharedFiles, getSharedFiles }
})
