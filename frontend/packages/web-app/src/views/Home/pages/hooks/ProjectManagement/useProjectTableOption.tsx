import { SearchOutlined } from '@ant-design/icons-vue'
import { storeToRefs } from 'pinia'
import { reactive, ref, watch } from 'vue'

import { getDesignList } from '@/api/project'
import type { TableOption } from '@/components/NormalTable'
import type { VIEW_OTHER } from '@/constants/resource'
import { VIEW_OWN } from '@/constants/resource'
import { useAppConfigStore } from '@/stores/useAppConfig'
import { useUserStore } from '@/stores/useUserStore'

import { useProjectOperate } from './useProjectOperate'

type DataSource = typeof VIEW_OWN | typeof VIEW_OTHER

export default function useProjectTableOption(dataSource: DataSource = VIEW_OWN) {
 const homeTableRef = ref(null)
 const consultRef = ref(null)

 function refreshHomeTable() {
 homeTableRef.value?.fetchTableData()
 }

 function refreshWithDelete(count: number = 1) {
 homeTableRef.value?.refreshWithDelete(count)
 }

 const { createColumns, currHoverId, handleEdit } = useProjectOperate(homeTableRef, consultRef, refreshHomeTable, refreshWithDelete)
 const appStore = useAppConfigStore()
 const userStore = useUserStore()
 const { appInfo } = storeToRefs(appStore)

 const tableOption = reactive<TableOption>({
 refresh: false, // 제어테이블데이터새로고침
 getData: getDesignList,
 formList: [ // 테이블위방법의테이블단일매칭
 {
 componentType: 'input',
 bind: 'name',
 placeholder: 'enterName',
 prefix: <SearchOutlined />,
 },
 ],
 tableProps: { // 테이블매칭, antd중의Table컴포넌트의속성
 columns: createColumns,
 onResizeColumn: (w, col) => {
 createColumns.value.find(item => item.key === col.key).width = w
 },
 rowKey: 'robotId',
 size: 'middle',
 customRow: record => ({
 onDblclick: () => { // 더블클릭행
 handleEdit(record)
 },
 onMouseenter: () => { // 마우스동작까지행
 currHoverId.value = record.robotId // 현재선택중행식별자
 },
 onMouseleave: () => { // 마우스열기행
 currHoverId.value = ''
 },
 }),
 },
 params: { // 지정의테이블단일매칭의데이터
 name: '',
 dataSource,
 },
 })

 // 자르기교체테넌트후목록새로고침
 watch(() => userStore.currentTenant?.id, (val) => {
 if (val) {
 refreshHomeTable()
 }
 })

 return {
 homeTableRef,
 consultRef,
 tableOption,
 authType: appInfo.value.appAuthType,
 }
}
