import { SearchOutlined, SyncOutlined } from '@ant-design/icons-vue'
import { Icon } from '@rpa/components'
import { Tooltip } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { h, reactive, ref, watch } from 'vue'

import type { TableOption } from '@/components/NormalTable'
import { useUserStore } from '@/stores/useUserStore'
import type { AnyObj } from '@/types/common'
import { ROBOT_SOURCE_LOCAL, ROBOT_SOURCE_TEXT } from '@/views/Home/config'
import { handleRun } from '@/views/Home/pages/hooks/useCommonOperate.tsx'

import OperMenu from '../../../components/OperMenu.vue'

import useRobotOperation from './useRobotOperation'

export default function useRobotTableOption() {
 const userStore = useUserStore()
 const { t } = useTranslation()
 const homeTableRef = ref(null)

 function refreshHomeTable() {
 homeTableRef.value?.fetchTableData()
 }

 function refreshWithDelete(count: number = 1) {
 homeTableRef.value?.refreshWithDelete(count)
 }

 const { getTableData, handleToConfig, openRobotDetailModal, openMcpConfigModal, handleDeleteRobot, handleRobotUpdate, expiredTip } = useRobotOperation(
 homeTableRef,
 refreshHomeTable,
 refreshWithDelete,
 )

 const baseOpts = [
 {
 key: 'run',
 text: 'run',
 clickFn: (record) => { !expiredTip(record) && handleRun({ ...record, exec_position: 'EXECUTOR' }) },
 icon: h(<Icon name="play-circle-stroke" size="16px" />),
 },
 {
 key: 'config',
 text: 'configParameters',
 clickFn: handleToConfig,
 icon: h(<Icon name="config-params" size="16px" />),
 },
 ]

 const localMoreOpts = [
 {
 key: 'virtualRun',
 text: 'virtualDesktopRunning',
 icon: h(<Icon name="virtual-desktop" size="16px" />),
 disableFn: (row: AnyObj) => row.usePermission === 0,
 clickFn: (record) => { !expiredTip(record) && handleRun({ ...record, exec_position: 'EXECUTOR', open_virtual_desk: true }) },
 },
 {
 key: 'check',
 text: 'appDetails',
 icon: h(<Icon name="robot" size="16px" />),
 clickFn: openRobotDetailModal,
 },
 {
 key: 'mcpconfig',
 text: 'mcpconfig',
 icon: h(<Icon name="robot" size="16px" />),
 clickFn: openMcpConfigModal,
 },
 ]
 const marketMoreOpts = [
 {
 key: 'virtualRun',
 text: 'virtualDesktopRunning',
 icon: h(<Icon name="virtual-desktop" size="16px" />),
 disableFn: (row: AnyObj) => row.usePermission === 0,
 clickFn: (record) => { !expiredTip(record) && handleRun({ ...record, exec_position: 'EXECUTOR', open_virtual_desk: true }) },
 },
 {
 key: 'check',
 text: 'appDetails',
 icon: h(<Icon name="robot" size="16px" />),
 clickFn: openRobotDetailModal,
 },
 {
 key: 'delete',
 text: 'delete',
 icon: h(<Icon name="market-del" size="16px" />),
 clickFn: handleDeleteRobot,
 },
 ]

 const columns = ref([
 {
 title: t('robotName'),
 key: 'robotName',
 dataIndex: 'robotName',
 width: 150,
 resizable: true,
 ellipsis: true,
 customRender: ({ record }) => (
 <div>
 <span class="inline-flex items-center w-full overflow-hidden">
 <Tooltip title={t('common.idWithColon', { id: record.robotId })}>
 <span class="truncate">{record.robotName}</span>
 </Tooltip>
 {record.updateStatus === 1 && <SyncOutlined onClick={() => { handleRobotUpdate(record) }} />}
 {record.expiryDateStr && (
 <span class="inline-block text-[12px] text-[#EC483E] bg-[#EC483E1A] rounded-[4px] px-[4px] py-[1px] ml-[5px] font-normal">
 {record.expiryDateStr}
 </span>
 )}
 </span>
 </div>
 ),
 },
 {
 title: t('updated'),
 key: 'updateTime',
 dataIndex: 'updateTime',
 width: 150,
 sorter: true,
 ellipsis: true,
 },
 {
 title: t('common.enabled'),
 dataIndex: 'version',
 key: 'version',
 ellipsis: true,
 customRender: ({ record }) => {
 return Number(record.version) === 0 ? '--' : `V${record.version}`
 },
 },
 {
 title: t('source'),
 dataIndex: 'sourceName',
 key: 'sourceName',
 ellipsis: true,
 customRender: ({ record }) => {
 return (<span class="h-[24px] px-[6px] py-[2px] leading-6 bg-[rgba(215,215,255,0.4)] dark:bg-[rgba(56,55,100,0.60)] text-[rgba(132,130,254,0.9)] rounded-[4px] flex-inline items-center justify-center">{ROBOT_SOURCE_TEXT[record.sourceName]}</span>)
 },
 },
 {
 title: t('operate'),
 key: 'oper',
 dataIndex: 'oper',
 width: 200,
 customRender: ({ record }) => {
 const { sourceName } = record
 const moreOpts = sourceName === ROBOT_SOURCE_LOCAL ? localMoreOpts : marketMoreOpts
 return <OperMenu moreOpts={moreOpts} baseOpts={baseOpts} row={record} />
 },
 },
 ])

 const tableOption = reactive<TableOption>({
 refresh: false, // 제어테이블데이터새로고침
 getData: getTableData,
 formList: [ // 테이블위방법의테이블단일매칭
 {
 componentType: 'input',
 bind: 'name',
 placeholder: 'enterName',
 prefix: <SearchOutlined />,
 },
 ],
 tableProps: { // 테이블매칭, antd중의Table컴포넌트의속성
 columns,
 onResizeColumn: (w, col) => {
 columns.value.find(item => item.key === col.key).width = w
 },
 size: 'middle',
 rowKey: 'resourceId',
 customRow: (record) => {
 return {
 class: record.usePermission === 0 ? 'opacity-50' : '',
 onDblclick: () => {
 expiredTip(record)
 },
 }
 },
 },
 params: { // 지정의테이블단일매칭의데이터
 name: '',
 },
 })

 // 자르기교체테넌트후목록새로고침
 watch(() => userStore.currentTenant?.id, (val) => {
 val && refreshHomeTable()
 })

 return {
 homeTableRef,
 tableOption,
 }
}
