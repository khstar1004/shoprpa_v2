<script lang="tsx">
import { useElementSize } from '@vueuse/core'
import { Empty, Pagination, Table } from 'ant-design-vue'
import { to } from 'await-to-js'
import { useTranslation } from 'i18next-vue'
import { isBoolean, isEmpty } from 'lodash-es'
import type { SlotsType } from 'vue'
import { computed, onMounted, reactive, ref, useTemplateRef } from 'vue'

import type { TableOption } from '@/types/normalTable'

import useTable from './hooks/useTable'

const PAGE_SIZE_OPTIONS = ['15', '30', '50', '100']
const DEFAULT_PAGE = 1
// 시로일반량, 후가능으로사용에서 antv tokens 중가져오기 필요아니오table의높음정도가능아니오
const TABLE_HEADER_HEIGHT = 47 // 테이블높음정도
const TABLE_CELL_HEIGHT = 49 // 테이블단일높음정도

function getItemsOnPage(total: number, pageSize: number, page: number) {
  // 확인매개변수있음
  if (total <= 0 || pageSize <= 0 || page <= 0) {
    return 0
  }

  // 계획데이터
  const totalPages = Math.ceil(total / pageSize)

  // 예결과요청 의코드초과경과데이터, 반환0
  if (page > totalPages) {
    return 0
  }

  // 예결과예후일, 계획데이터
  if (page === totalPages) {
    return total - (pageSize * (totalPages - 1))
  }

  // 예가득
  return pageSize
}

export default {
  name: 'NormalTable',
  props: {
    option: {
      type: Object as () => TableOption,
      default(): TableOption {
        return {
          getData: async () => ({ records: [], total: 0 }),
          tableProps: { columns: [] },
        }
      },
    },
  },
  slots: Object as SlotsType<{ default: any, headerPrefix: any }>,
  setup(props, { expose, slots }) {
    const { t } = useTranslation()
    /**
     * 예 table 의 scroll 높음정도?
     * 1. 사용 position: absolute  table 문서
     * 2. 사용 useElementSize 가져오기 table 에서내용기기의 height
     * 3. 예결과 table 있음가득내용기기, 이면아니오 table 의 scroll
     * 4. 아니오이면,  table 의 scroll 높음정도로내용기기높음정도 47px
     */
    const tableElement = useTemplateRef<HTMLTableElement>('table')
    const tableSize = useElementSize(tableElement)

    const { option } = reactive({ ...props })
    const { renderHeaderForm, renderHeaderButton } = useTable()
    const localOption = computed(() => option)
    const pageNoName = computed(() => option?.pageParams?.pageNoName || 'pageNo')
    const pageSizeName = computed(() => option?.pageParams?.pageSizeName || 'pageSize')
    const pageOption = ref({ // 분매칭
      total: 0,
      current: 1,
      pageSize: Number((PAGE_SIZE_OPTIONS)[0]),
      pageSizeOptions: PAGE_SIZE_OPTIONS,
      size: 'small' as const,
      showSizeChanger: true,
      showQuickJumper: true,
      ...(option?.pageConfig || {}),
    })
    const orderData = ref(option?.orderParams || { orderName: 'sortBy', orderStatus: 'sortType' }) // 정렬필드
    const immediate = isBoolean(option?.immediate) ? option.immediate : true // 여부실행
    const isPage = ref(option?.page === false ? option.page : true) // 여부열기시작분, 분
    const loading = ref(false) // 열기시작loading
    const tableData = ref([]) // 테이블데이터
    const formOption = reactive({ // 테이블단일매칭
      formList: option?.formList || [],
      params: option?.params || {},
      searchFn: () => {
        if (isPage.value) {
          pageOption.value.current = Number(DEFAULT_PAGE)
          localOption.value.params[pageNoName.value] = Number(DEFAULT_PAGE)
        }
        fetchTableData()
      },
    })

    const buttonOption = reactive({ // 버튼매칭
      buttonList: option?.buttonList || [],
    })

    const tableColumns = computed(() => {
      return option.tableProps?.columns.map(item => ({
        ...item,
        title: t(item.title),
      })) || []
    })

    async function fetchTableData() {
      if (isPage.value) {
        localOption.value.params[pageSizeName.value] = pageOption.value.pageSize
        localOption.value.params[pageNoName.value] = pageOption.value.current
      }

      loading.value = true
      const [error, data] = await to(localOption.value.getData(localOption.value.params))
      tableData.value = error ? [] : data.records
      pageOption.value.total = error ? 0 : data.total
      loading.value = false
    }

    function onPageChange(page: number) {
      pageOption.value.current = page

      fetchTableData()
    }

    function onShowSizeChange(_, size) {
      pageOption.value.pageSize = size
    }

    // 분, 정렬, 선택
    function tableChange(_pagination, _filters, sorter) {
      const { field, order } = sorter // sorter있음가능로빈
      const ORDER_OPTION = {
        ascend: 'asc',
        descend: 'desc',
      }
      localOption.value.params[orderData.value.orderName] = field || '' // checkNum
      localOption.value.params[orderData.value.orderStatus] = ORDER_OPTION[order] || '' // ascend

      fetchTableData()
    }

    function renderTable() { // 지원table
      const page = pageOption.value.current
      const pageSize = pageOption.value.pageSize
      const total = pageOption.value.total

      // 계획 table 의대높음정도
      const currentCellLength = getItemsOnPage(total, pageSize, page)
      const maxHeight = currentCellLength * (option.tableCellHeight || TABLE_CELL_HEIGHT) + TABLE_HEADER_HEIGHT
      //  table 내용기기여부가득
      const isFull = tableSize.height.value < maxHeight

      return (
        <Table
          key={isFull ? 'full' : 'notFull'}
          {...localOption.value.tableProps}
          columns={tableColumns.value}
          scroll={{ y: isFull ? tableSize.height.value - TABLE_HEADER_HEIGHT : undefined }}
          class="custom-table absolute w-full"
          loading={loading.value}
          dataSource={tableData.value}
          pagination={false}
          v-slots={{
            emptyText: () => <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={localOption.value.emptyText ?? undefined} />,
          }}
          onChange={tableChange}
          size={option.size || 'small'}
        />
      )
    }

    function renderPagination() {
      return (
        <div class="nTable-pagination flex items-center justify-between">
          <div>
            {t('common.totalData', { total: pageOption.value.total })}
          </div>
          <Pagination
            onChange={onPageChange}
            onShowSizeChange={onShowSizeChange}
            {...pageOption.value}
          />
        </div>
      )
    }

    function renderHeader() {
      const hasForm = !isEmpty(option.formList)
      const hasBtns = !isEmpty(option.buttonList)

      if (!hasBtns && !hasForm) {
        return null
      }

      const header = (className?: string) => (
        <div
          class={[
            className,
            'nTable-header',
            { 'flex-row-reverse': option.formListAlign === 'right' || option.buttonListAlign === 'left' },
            option.headerClass,
          ]}
        >
          {/* 왼쪽form */}
          {hasForm && renderHeaderForm(formOption)}
          {/* 오른쪽button */}
          {hasBtns && renderHeaderButton(buttonOption)}
        </div>
      )

      if (!slots.headerPrefix) {
        return header()
      }

      return (
        <div class="flex items-center">
          {slots.headerPrefix?.()}
          {header('flex-1')}
        </div>
      )
    }

    const refreshWithDelete = (count: number = 1) => {
      // 예결과예삭제조회, 필요를데이터 count, 정상 pageNum
      const newTotal = pageOption.value.total - count
      const oldPageNum = pageOption.value.current
      const newTotalPages = Math.ceil(newTotal / pageOption.value.pageSize)

      if (oldPageNum > newTotalPages) {
        pageOption.value.current = Math.max(1, newTotalPages)
      }

      fetchTableData()
    }

    onMounted(() => {
      if (immediate && option.params) {
        fetchTableData()
      }
    })

    expose({
      tableData,
      localOption,
      fetchTableData,
      refreshWithDelete,
    })

    return () => (
      <div class="wrapper h-full">
        <div class="nTable h-full flex flex-col gap-4">
          {/* 부서 */}
          {renderHeader()}
          {/*  */}
          <div class="flex-1 relative" ref="table">
            {slots.default?.({ loading: loading.value, tableData: tableData.value, height: tableSize.height.value }) || renderTable()}
          </div>
          {/* 부서 */}
          {isPage.value && renderPagination()}
        </div>
      </div>
    )
  },
}
</script>

<style lang="scss" scoped>
@import './index.scss';

:deep(.ant-table-row-level-1) {
  background-color: var(--color-fill-secondary);
}
</style>