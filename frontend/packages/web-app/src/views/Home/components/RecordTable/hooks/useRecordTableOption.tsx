import { DownOutlined, UpOutlined } from '@ant-design/icons-vue'
import { Tooltip } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { reactive, ref } from 'vue'

import useRecordOperation from './useRecordOperation.tsx'
import useRecordTableColumns from './useRecordTableColumns.tsx'

export default function useRecordTableOption(props?: { robotId: string }) {
 const { t } = useTranslation()
 const homeTableRef = ref(null)
 function refreshWithDelete(count: number = 1) {
 homeTableRef.value?.refreshWithDelete(count)
 }
 const { columns } = useRecordTableColumns(props, refreshWithDelete)
 const { rowSelection, getTableData, batchDelete } = useRecordOperation(refreshWithDelete)

 const tableOption = reactive({
 refresh: false, // 제어테이블데이터새로고침
 formList: props.robotId
 ? []
 : [
 {
 componentType: 'input',
 bind: 'robotName',
 placeholder: t('enterAppName'),
 },
 {
 componentType: 'datePicker',
 bind: 'timeRange',
 },
 {
 componentType: 'select',
 bind: 'result',
 placeholder: t('record.selectStatus'),
 options: [
 {
 label: t('record.allStatus'),
 value: '',
 },
 {
 label: t('common.success'),
 value: 'robotSuccess',
 },
 {
 label: t('common.fail'),
 value: 'robotFail',
 },
 {
 label: t('common.cancel'),
 value: 'robotCancel',
 },
 {
 label: t('common.robotExecute'),
 value: 'robotExecute',
 },
 ],
 isTrim: true,
 },
 ],
 buttonList: [
 {
 label: t('batchDelete'),
 action: '',
 clickFn: batchDelete,
 hidden: false,
 },
 ],
 getData: getTableData,
 tableProps: {
 // 테이블매칭, antd중의Table컴포넌트의속성
 columns,
 rowKey: 'executeId',
 size: 'middle',
 rowSelection,
 expandIcon: ({ expanded, onExpand, record }) => {
 return record.children
 ? (
 <Tooltip title={expanded ? t('common.collapse') : t('common.expand')}>
 <span
 class="mr-2"
 onClick={(e) => {
 e.stopPropagation()
 onExpand(record, e)
 }}
 >
 {expanded ? <UpOutlined /> : <DownOutlined />}
 </span>
 </Tooltip>
 )
 : (
 <span class="mr-2"></span>
 )
 },
 },
 params: {
 // 지정의테이블단일매칭의데이터
 robotName: '',
 robotId: props.robotId || '',
 },
 })
 return {
 homeTableRef,
 tableOption,
 }
}
