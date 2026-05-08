import { ProfileOutlined } from '@ant-design/icons-vue'
import { Button, Table, Tooltip } from 'ant-design-vue'

import { getDurationText } from '@/utils/dayjsUtils'

import { getTaskExecuteLst } from '@/api/task'
import StatusCircle from '@/views/Home/components/StatusCircle.vue'
import { useCommonOperate } from '@/views/Home/pages/hooks/useCommonOperate.tsx'

export default function useTaskRecordOperation() {
  const { handleCheck } = useCommonOperate()

  function handleExpandedRowRender({ record }) {
    const innerColumns = [
      {
        title: '앱 이름',
        dataIndex: 'robotName',
        key: 'robotName',
        ellipsis: true,
      },
      {
        title: '앱 버전',
        key: 'robotVersion',
        dataIndex: 'robotVersion',
        ellipsis: true,
        customRender: ({ record }) => `버전 ${record.robotVersion}`,
      },
      {
        title: '시작 시간',
        key: 'startTime',
        dataIndex: 'startTime',
        width: 170,
      },
      {
        title: '종료 시간',
        key: 'endTime',
        dataIndex: 'endTime',
        width: 170,
      },
      {
        title: '실행 시간',
        key: 'executeTime',
        dataIndex: 'executeTime',
        customRender: ({ record }) => getDurationText(record.executeTime),
      },
      {
        title: '실행 결과',
        key: 'result',
        dataIndex: 'result',
        customRender: ({ record }) => {
          return <StatusCircle type={`${record.result}`} />
        },
      },
      {
        title: '작업',
        dataIndex: 'oper',
        key: 'oper',
        customRender: ({ record }) => {
          return (
            <div class="operation">
              <Tooltip title="로그 보기" placement="bottom">
                <Button
                  type="link"
                  style="margin-right: 10px;"
                  size="small"
                  onClick={() => handleCheck({ record })}
                >
                  <ProfileOutlined />
                </Button>
              </Tooltip>
            </div>
          )
        },
      },
    ]
    return (
      <Table
        rowKey="recordId"
        columns={innerColumns}
        dataSource={record.robotExecuteRecordList}
        pagination={false}
      />
    )
  }

  return {
    getTableData: getTaskExecuteLst,
    handleExpandedRowRender,
  }
}
