/** @format */

import { Icon } from '@rpa/components'
import { Button, Tooltip } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { reactive, ref } from 'vue'

import StatusCircle from '@/views/Home/components/StatusCircle.vue'

import { TASK_TYPE_OPTION, TASK_TYPE_TEXT } from '../../../config/task.ts'

import useTaskRecordOperation from './useTaskRecordOperation.tsx'

export default function useRecordTableOption(props?: { robotId: string }) {
  const homeTableRef = ref(null)
  function refreshWithDelete(count: number = 1) {
    homeTableRef.value?.refreshWithDelete(count)
  }
  const translate = useTranslation()
  const { getTableData, handleExpandedRowRender, batchDelete, rowSelection } = useTaskRecordOperation(refreshWithDelete)
  const formList = [
    {
      componentType: 'input',
      bind: 'taskName',
      placeholder: translate.t('taskNamePlaceholder'),
    },
    {
      componentType: 'datePicker',
      bind: 'timeRange',
    },
    {
      componentType: 'select',
      bind: 'status',
      placeholder: translate.t('record.selectStatus'),
      options: [
        {
          label: translate.t('record.allStatus'),
          value: '',
        },
        {
          label: translate.t('common.executing'),
          value: 'executing',
        },
        {
          label: translate.t('common.planCompleted'),
          value: 'success',
        },
        {
          label: translate.t('common.startFailed'),
          value: 'start_error',
        },
        {
          label: translate.t('common.executionFailed'),
          value: 'exe_error',
        },
        {
          label: translate.t('common.planAborted'),
          value: 'cancel',
        },
      ],
      isTrim: true,
    },
    {
      componentType: 'select',
      bind: 'taskType',
      placeholder: translate.t('record.selectTriggerType'),
      options: [
        {
          label: translate.t('record.allTriggerType'),
          value: '',
        },
        ...TASK_TYPE_OPTION,
      ],
      isTrim: true,
    },
  ]

  const columns = [
    { width: '50px', title: '', key: 'expand', dataIndex: 'expand' }, // 사용위치열기버튼위치, 를열기버튼열위치 지정까지이열, 결과합치기css체크박스에서첫 번째열의결과
    {
      title: translate.t('taskName'),
      dataIndex: 'taskName',
      key: 'taskName',
      ellipsis: true,
    },
    {
      title: translate.t('record.executionCount'),
      key: 'count',
      dataIndex: 'count',
      ellipsis: true,
      customRender: ({ record }) => record.count && translate.t('record.nthExecution', { count: record.count }),
    },
    {
      title: translate.t('excuteCondition'),
      key: 'taskType',
      dataIndex: 'taskType',
      ellipsis: true,
      customRender: ({ record }) => {
        return TASK_TYPE_TEXT[record.taskType]
      },
    },
    {
      title: translate.t('startTime'),
      dataIndex: 'taskStartTime',
      key: 'taskStartTime',
      ellipsis: true,
      sorter: true,
    },
    {
      title: translate.t('endTime'),
      dataIndex: 'taskEndTime',
      key: 'taskEndTime',
      ellipsis: true,
      sorter: true,
    },
    {
      title: translate.t('result'),
      dataIndex: 'taskExecuteStatus',
      key: 'taskExecuteStatus',
      ellipsis: true,
      width: 100,
      customRender: ({ record }) => {
        return <StatusCircle type={String(record.taskExecuteStatus)} />
      },
    },
    {
      title: translate.t('operate'),
      dataIndex: 'oper',
      key: 'oper',
      width: 120,
      customRender: ({ record }) => {
        return (
          <div class="operation">
            <Tooltip title="삭제">
              <Button
                type="link"
                size="small"
                class="cursor-pointer outline-none p-[5px] text-[initial] hover:!text-primary"
                onClick={() => batchDelete([record.taskExecuteId])}
              >
                <Icon name="market-del" size="16px" />
              </Button>
            </Tooltip>
          </div>
        )
      },
    },
  ]

  const tableOption = reactive({
    refresh: false, // 제어테이블데이터새로고침
    getData: getTableData,
    formList,
    buttonList: [
      {
        label: '일괄 삭제',
        action: '',
        clickFn: batchDelete,
        hidden: false,
      },
    ],
    tableProps: {
      columns,
      rowKey: 'taskExecuteId',
      size: 'middle',
      rowSelection,
      expandIcon: ({ expanded, onExpand, record }) => {
        if (record.robotExecuteRecordList) {
          return (
            <Tooltip title={expanded ? '접기' : '열기'}>
              <span
                class={[
                  'cursor-pointer absolute left-[45px] top-1/2 z-10 -translate-y-1/2',
                  'border border-[#E6E6E6] hover:border-primary rounded-[2px] hover:!text-primary',
                  'w-4 h-4 inline-flex items-center justify-center',
                ]}
                onClick={(e) => {
                  e.stopPropagation()
                  onExpand(record, e)
                }}
              >
                <Icon name={expanded ? 'collapse' : 'uncollapse'} size="14px" />
              </span>
            </Tooltip>
          )
        }

        return <span class="mr-2" />
      },
      expandedRowRender: handleExpandedRowRender,
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
