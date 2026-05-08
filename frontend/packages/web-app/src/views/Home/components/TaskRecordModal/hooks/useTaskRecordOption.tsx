import { reactive } from 'vue'

import StatusCircle from '@/views/Home/components/StatusCircle.vue'

import useTaskRecordOperation from './useTaskRecordOperation'

export default function useTaskRecordOption(taskId) {
  const { getTableData, handleExpandedRowRender } = useTaskRecordOperation()

  const tableOption = reactive({
    refresh: false, // 제어테이블데이터새로고침
    getData: getTableData,
    tableProps: { // 테이블매칭, antd중의Table컴포넌트의속성
      columns: [
        {
          title: '순서',
          dataIndex: 'id',
          key: 'id',
          align: 'center',
        },
        {
          title: '실행 회차',
          key: 'count',
          dataIndex: 'count',
          ellipsis: true,
          customRender: ({ record }) => `${record.count}회`,
        },
        {
          title: '시작 시간',
          key: 'startTime',
          dataIndex: 'startTime',
        },
        {
          title: '종료 시간',
          key: 'endTime',
          dataIndex: 'endTime',
        },
        {
          title: '실행 결과',
          key: 'result',
          dataIndex: 'result',
          customRender: ({ record }) => {
            return <StatusCircle type={`${record.result}`} />
          },
        },
      ],
      rowKey: 'id',
      expandedRowRender: handleExpandedRowRender,
    },
    params: { // 지정의테이블단일매칭의데이터
      taskId,
    },
  })
  return tableOption
}
