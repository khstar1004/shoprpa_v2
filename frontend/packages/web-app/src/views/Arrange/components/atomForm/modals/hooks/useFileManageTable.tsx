import { SearchOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { useTranslation } from 'i18next-vue'
import { reactive, ref } from 'vue'

import { getRemoteFiles } from '@/api/atom'
import type { TableOption } from '@/types/normalTable'

export default function useFileManageTable() {
  const selectFileId = ref('')
  const { t } = useTranslation()

  const handleClick = (record) => {
    selectFileId.value = record.fileId
  }
  const tableOption = reactive<TableOption>({
    refresh: false, // 제어테이블데이터새로고침
    getData: getRemoteFiles,
    formList: [ // 테이블위방법의테이블단일매칭
      {
        componentType: 'input',
        bind: 'fileName',
        placeholder: t('market.enterFileName'),
        prefix: <SearchOutlined />,
      },
    ],
    tableProps: { // 테이블매칭, antd중의Table컴포넌트의속성
      columns: [
        {
          title: t('market.fileName'),
          dataIndex: 'fileName',
          key: 'fileName',
          fixed: 'left',
          ellipsis: true,
        },
        {
          title: t('common.createTime'),
          dataIndex: 'createTime',
          key: 'createTime',
          sortable: true,
          ellipsis: true,
          customRender: ({ record }) => dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss'),
        },
        {
          title: t('common.updateTime'),
          dataIndex: 'updateTime',
          key: 'updateTime',
          sortable: true,
          ellipsis: true,
          customRender: ({ record }) => dayjs(record.updateTime).format('YYYY-MM-DD HH:mm:ss'),
        },
        {
          title: t('market.owner'),
          dataIndex: 'creatorName',
          key: 'creatorName',
          ellipsis: true,
        },
        {
          title: t('common.account'),
          dataIndex: 'phone',
          key: 'phone',
          ellipsis: true,
        },
        {
          title: t('market.department'),
          dataIndex: 'deptName',
          key: 'deptName',
          ellipsis: true,
        },
        {
          title: t('market.tags'),
          dataIndex: 'tags',
          key: 'tags',
          ellipsis: true,
          customRender: ({ record }) => {
            return record.tagsNames?.length > 0 ? record.tagsNames.join(', ') : '--'
          },
        },
      ],
      rowKey: 'fileId',
      size: 'middle',
      customRow: (record) => {
        return {
          class: `cursor-pointer ${record.fileId === selectFileId.value ? 'selectRow' : ''}`,
          onClick: () => { // 더블클릭행
            handleClick(record)
          },
        }
      },
    },
    params: { // 지정의테이블단일매칭의데이터
      fileName: '',
    },
  })

  return {
    selectFileId,
    tableOption,
  }
}
