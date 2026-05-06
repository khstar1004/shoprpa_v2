import { useProcessStore } from '@/stores/useProcessStore.ts'

interface OptionType<T = string> {
 label: string
 value: T
}

export const usageOptions: OptionType<RPA.ConfigParamData['varDirection']>[] = [
 { label: '입력', value: 0 },
 { label: '출력', value: 1 },
]

export function getMainProcessParameterOption(): OptionType[] {
 const processStore = useProcessStore()

 return Object.values(processStore.globalVarTypeList)
 .filter(item => item.channel.split(',').includes('main'))
 .map(item => ({
 label: item.desc,
 value: item.key,
 }))
}

export function getChildProcessParameterOption() {
 const processStore = useProcessStore()

 return processStore.globalVarTypeOption.map(item => ({
 label: item.desc,
 value: item.key,
 }))
}
