import { message, Table } from 'ant-design-vue'
import dayjs from 'dayjs'
import { useTranslation } from 'i18next-vue'
import { computed, ref } from 'vue'

import { delTaskExecute } from '@/api/record'
import { checkVideoPaths } from '@/api/setting'
import { getTaskExecuteLst } from '@/api/task'
import useRecordTableColumns from '@/views/Home/components/RecordTable/hooks/useRecordTableColumns.tsx'

export default function useTastRecordOperation(refreshWithDelete?: (count: number) => void) {
 const { t } = useTranslation()
 const selectedRowKeys = ref<string[]>([])
 const rowSelection = computed(() => {
 return {
 onChange: (keys: string[], _selectedRows: any[]) => {
 console.log('선택중의행', keys)
 selectedRowKeys.value = keys
 },
 selectedRowKeys: selectedRowKeys.value,
 }
 })

 function batchDelete(selected: string[]) {
 const selectedKeys = Array.isArray(selected) ? selected : selectedRowKeys.value
 if (selectedKeys.length === 0) {
 message.warning(t('selectOne'))
 }
 delTaskExecute({ taskExecuteIdList: selectedKeys }).then((res) => {
 message.success(res.data || t('common.deleteSuccess'))
 selectedRowKeys.value = []
 refreshWithDelete?.(selectedKeys.length)
 })
 }

 function getTableData(params) {
 return new Promise((resolve) => {
 const paramsObj = {
 ...params,
 startDate: params.timeRange ? `${dayjs(params.timeRange[0]).format('YYYY-MM-DD')} 00:00:00` : '',
 endDate: params.timeRange ? `${dayjs(params.timeRange[1]).format('YYYY-MM-DD')} 23:59:59` : '',
 }
 delete paramsObj.timeRange
 getTaskExecuteLst(paramsObj).then((data) => {
 const { total, records } = data
 /**
 * 조회여부본저장에서
 */
 const videoPaths = Array.from(new Set(records.flatMap(i => (i.robotExecuteRecordList || []).filter(item => item.videoLocalPath).map(item => item.videoLocalPath))))
 if (videoPaths.length <= 0) {
 resolve({ records, total })
 return
 }
 checkVideoPaths({ videoPaths })
 .then((result) => {
 const exist = result?.data?.exist
 if (exist && exist?.length > 0) {
 const newRecords = records.map((record) => {
 record.robotExecuteRecordList = record.robotExecuteRecordList.map((item) => {
 item.videoExist = exist.includes(item.videoLocalPath) ? '0' : '1' // 0: exists, 1: missing
 return item
 })
 return record
 })
 resolve({ records: newRecords, total })
 }
 else {
 resolve({ records, total })
 }
 })
 .catch(() => {
 resolve({ records, total })
 })
 })
 })
 }

 function handleExpandedRowRender({ record }) {
 const { columns } = useRecordTableColumns({ taskId: record.taskId })

 const innerColumns = [
 { width: '32px' }, // 사용위치위체크박스위치, 보관테이블및테이블열대상
 { width: '50px', title: '', key: 'expand', dataIndex: 'expand' }, // 사용위치위열기버튼위치,보관테이블및테이블열대상
 ...columns,
 ]
 return (
 <Table
 size="small"
 rowKey="recordId"
 columns={innerColumns}
 dataSource={record.robotExecuteRecordList}
 pagination={false}
 />
 )
 }

 return {
 getTableData,
 rowSelection,
 batchDelete,
 handleExpandedRowRender,
 }
}
