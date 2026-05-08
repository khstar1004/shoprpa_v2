import { message } from 'ant-design-vue'
import { useTranslation } from 'i18next-vue'
import { cloneDeep } from 'lodash-es'
import { reactive, ref, watch } from 'vue'

import type { VxeGridProps } from '@/plugins/VxeTable'

import { generateColumnNames, generateSheetName, getUrlQueryField } from '@/utils/common'

import http from '@/api/http'
import { addElement, getElementDetail, getElementsAll, updateElement } from '@/api/resource'
import GlobalModal from '@/components/GlobalModal/index.ts'
import { WINDOW_NAME } from '@/constants'
import { windowManager } from '@/platform'
import { useBatchPickStore } from '@/stores/useBatchPickStore'
import { usePickStore } from '@/stores/usePickStore'
import type { FormRules } from '@/types/common'
import type { BatchElementDataInfo, ColumnInfo, ElementInfo } from '@/types/databatch.d'
import { VISUALIZATION } from '@/views/Arrange/config/pick'

import { ColumnsKeys, Menus } from './config'

export function useDataBatch() {
  const { t } = useTranslation()
  const useBatchPick = useBatchPickStore()
  const usePick = usePickStore()
  const selectedColumnIndex = ref(-1) // 현재선택중열의검색
  const menuItems = ref([]) // 현재선택중열의메뉴
  const batchFormType = ref('') // 테이블단일유형,  similar: 가져오기, table: 테이블가져오기
  const gridRef = ref(null) // 테이블ref
  const formRef = ref(null) // 테이블단일ref
  const batchModalRef = ref(null) // 량팝업ref
  const batchModalVisible = ref(false) // 량팝업여부
  const batchModalConfig = ref({}) // 량팝업매칭
  const rules = reactive<FormRules>({ name: [{ message: t('dataBatch.tableNamePlaceholder'), trigger: 'blur' }] })
  const formState = reactive({ name: '' })
  const activeColumn = ref(null) // 현재의열
  const checkData = ref(false) // 조회현재데이터
  const locale = getUrlQueryField('locale') || 'zh-CN'
  let isEdit = false // 여부예상태
  let batchObject: ElementInfo // 가져오기객체
  let batchElementData: BatchElementDataInfo // 가져오기객체요소데이터
  let tableElement = null // 테이블요소정보
  // 테이블열
  let columns: ColumnInfo[] = []
  // 테이블데이터
  let tableData: any[] = []
  // 테이블열의데이터
  let columnData: any[] = []
  let isHightLight = false // 여부높음열

  // 단일열 필터링출력 menus
  watch(
    () => activeColumn.value,
    (newVal) => {
      let menus = batchTypeMenus(batchFormType.value)
      if (newVal) {
        const { filterConfig, colFilterConfig, colDataProcessConfig, isTable } = newVal // 가져오기 관리파일
        menus = isTable ? menus.filter(menu => menu.key !== 'similarAdd') : menus
        menus.forEach((menu) => {
          if (menu.key === 'filterConfig' && filterConfig) {
            menu.active = filterConfig.length > 0
          }
          if (menu.key === 'colFilterConfig' && colFilterConfig) {
            menu.active = colFilterConfig.length > 0
          }
          if (menu.key === 'colDataProcessConfig' && colDataProcessConfig) {
            menu.children.forEach((child) => {
              const data = colDataProcessConfig.find(item => item.processType === child.key)
              if (data) {
                menu.active = data.isEnable === 1
                child.showEdit = data.parameters.length > 0
                child.checked = data.isEnable === 1
              }
              else {
                child.checked = false
              }
            })
          }
          if (menu.key === 'toggleColumnData') {
            const attrsMap = {}
            newVal.value.forEach((item) => {
              Object.keys(item.attrs).forEach((attrKey) => {
                attrsMap[attrKey] = true
              })
            })
            menu.children = menu.children.filter(item => attrsMap[item.key])
          }
        })
      }
      menuItems.value = menus
    },
  )
  // 테이블단일유형 필터링출력menus
  watch(
    () => batchFormType.value,
    (newVal) => {
      menuItems.value = batchTypeMenus(newVal)
    },
  )

  const batchTypeMenus = (type) => {
    let menus = cloneDeep(Menus)
    if (type === 'similar') {
      menus = menus.filter(item => item)
    }
    else {
      menus = menus.filter(item => !item.showType || item.showType !== 'similar')
    }
    return menus
  }

  // 테이블열메뉴
  const menuClick = (item, index) => {
    if (item.domEvent?.target.tagName === 'INPUT')
      return
    const { keyPath } = item
    let key = keyPath[keyPath.length - 1]
    key = key.includes('-else') ? key.replace('-else', '') : key
    let menuItem
    menuItems.value.some((menu) => {
      if (menu.key === key) {
        menuItem = menu
        return true // 까지후출력
      }
      if (menu.children) {
        menuItem = menu.children.find(child => child.key === key)
        if (menuItem)
          return true // 예결과에서메뉴중까지, 출력
      }
      return false
    })

    switch (key) {
      case 'editColumnElement':
        openModal({
          title: t(`dataBatch.${key}`),
          type: key,
          column: cloneDeep(columns[index]),
          width: '100%',
          warpClassName: 'full-modal',
        })
        break
      case 'editColumnName':
        openModal({
          title: t(`dataBatch.${key}`),
          type: key,
          column: { title: columns[index].title, dataIndex: columns[index].dataIndex },
          width: '400px',
        })
        break
      case 'deleteColumn':
        deleteColumn(index)
        break
      case 'copyColumn':
        copyColumn(index)
        break
      case 'insertColumnLeft':
        insertColumn(index, 'left')
        break
      case 'insertColumnRight':
        insertColumn(index, 'right')
        break
      case 'similarAdd':
        addSimilarData(index)
        break
      case 'colFilterConfig':
        openNormalModal(menuItem.label, key, columns[index], '480px')
        break
      case 'filterConfig':
        openNormalModal(menuItem.label, key, columns[index], '480px')
        break
      case 'Replace':
        openNormalModal(menuItem.label, key, columns[index])
        break
      case 'Prefix':
        openNormalModal(menuItem.label, key, columns[index])
        break
      case 'Suffix':
        openNormalModal(menuItem.label, key, columns[index])
        break
      case 'FormatTime':
        openNormalModal(menuItem.label, key, columns[index])
        break
      case 'Regular':
        openNormalModal(menuItem.label, key, columns[index])
        break
      case 'clear':
        clearExpression(columns[index])
        break
      case 'text':
        toggleColumnDataHandle('text', index)
        break
      case 'href':
        toggleColumnDataHandle('href', index)
        break
      case 'src':
        toggleColumnDataHandle('src', index)
        break
      default:
        // message.warning('없음')
        break
    }
  }
  // 열기팝업
  const openModal = (params) => {
    params.open = true
    batchModalVisible.value = true
    batchModalConfig.value = params
  }
  // 열기소팝업
  const openNormalModal = (_label, key, col, width = '400px') => {
    openModal({
      title: t(`dataBatch.${key}`),
      type: key,
      column: cloneDeep(col),
      width,
    })
  }
  // 유형
  const toggleColumnDataHandle = (value_type: string, index: number) => {
    columns[index].value_type = value_type
    const value = columns[index].value
    if (value) {
      value.forEach((item) => {
        item.text = item.attrs[value_type] || ''
      })
    }
    columns[index].value = value
    columnData[index] = value
    columns = columnItemGenerate(columns)
    tableData = tableDataHandle(columns, columnData)
    refresh()
  }
  // 빈테이블파일
  const clearExpression = (col) => {
    col.colDataProcessConfig = []
    col.colFilterConfig = []
    col.filterConfig = []
    send2FetchData(columns)
  }
  /**
   * 삭제열
   * @param index 현재열검색
   */
  const deleteColumn = (index) => {
    columns.splice(index, 1)
    columnData.splice(index, 1)
    columns = columnItemGenerate(columns)
    tableData = tableDataHandle(columns, columnData)
    refresh()
  }
  /**
   * 복사열
   * @param index 현재열검색
   */
  const copyColumn = (index) => {
    const column = cloneDeep(columns[index])
    const columnDataItem = cloneDeep(columnData[index])
    column.title = `${column.title}_copy`
    column.dataIndex = `${column.dataIndex}_copy`
    column.field = `${column.field}_copy`
    columns.splice(index + 1, 0, column)
    columnData.splice(index + 1, 0, columnDataItem)
    similarTypeHandle(columns, columnData)
    // autoAddColumn() // 추가일열
  }
  /**
   * 위치 지정삽입열
   * @param index 현재열검색
   * @param insert 삽입위치, left: 왼쪽가장자리, right: 오른쪽가장자리
   */
  const insertColumn = (index: number, insert: 'left' | 'right') => {
    newBatch(index, insert)
    refresh()
  }

  const moreMenuClick = (index: number) => {
    selectedColumnIndex.value = index
    activeColumn.value = cloneDeep(columns[index])
  }
  // checbox 클릭
  const checkboxClick = (event, index: number, current: any) => {
    current.checked = event.target.checked
    dataProcessEnable(index, current, event)
  }
  // 파일선택
  const dataProcessEnable = (index: number, current: any, _event) => {
    const column = columns[index]
    const currentProcessConfig = column.colDataProcessConfig.find(item => item.processType === current.key)
    if (current.checked) {
      // 필요팝업의메뉴에서있음해당파일또는파일로빈시필요출력팝업
      if ((!currentProcessConfig || currentProcessConfig.parameters.length === 0) && current.modal) {
        openNormalModal(current.label, current.key, column)
        current.checked = false
        return
      }
      // 필요팝업의메뉴예결과있음현재있음관리파일, 관리파일아니오로빈, 이면업데이트 isEnable
      if (currentProcessConfig && currentProcessConfig.parameters.length > 0) {
        currentProcessConfig.isEnable = 1
      }
      if (!currentProcessConfig && !current.modal) {
        column.colDataProcessConfig.push({
          processType: current.key,
          isEnable: 1,
          parameters: [],
        })
      }
    }
    else {
      if (currentProcessConfig) {
        // 예결과있음현재관리파일, 이면업데이트 isEnable
        currentProcessConfig.isEnable = 0
      }
      else {
        // 예결과있음현재관리파일, 이면아니오관리
        return
      }
    }
    // todo 전송가져오기객체가져오기새의데이터
    selectedColumnIndex.value = index
    activeColumn.value = cloneDeep(columns[index])
    send2FetchData(columns)
  }
  /**
   * 클릭열
   * @param col 현재열
   */
  const columnClick = (col) => {
    const index = col.columnIndex
    selectedColumnIndex.value = index
    activeColumn.value = cloneDeep(columns[index])
    highLightColumn(index)
  }
  const highLightColumn = (index: number) => {
    if (usePick.isChecking)
      return
    if (isHightLight)
      return
    let colElement = columns[index]
    if (batchFormType.value === 'table') {
      colElement = tableElement
    }
    const path = colElement
    path.checkType = colElement.checkType || VISUALIZATION
    path.matchTypes = []
    path.columnIndex = index + 1 // 에서1열기
    const ele = {
      app: colElement.app,
      type: colElement.type,
      version: colElement.version,
      path,
    }
    const eleStr = JSON.stringify(ele)
    isHightLight = true
    useBatchPick.highLight(eleStr, (_res) => {
      isHightLight = false
    })
  }

  const resetActiveColumn = () => {
    activeColumn.value = null
    selectedColumnIndex.value = -1
  }

  // grid 매칭
  const gridOptions = reactive<VxeGridProps & { columnLength: number, rowLength: number }>({
    stripe: true,
    resizable: true,
    round: true,
    loading: false,
    border: 'none',
    height: 230,
    columns: [],
    data: [],
    columnConfig: {
      isCurrent: true,
    },
    size: 'mini',
    showOverflow: true,
    showHeaderOverflow: 'tooltip',
    showFooterOverflow: true,
    emptyText: t('dataBatch.emptyData'),
    scrollY: {
      enabled: false,
      gt: 0,
    },
    scrollX: {
      enabled: false,
      gt: 0,
    },
    columnLength: 0,
    rowLength: 0,
    loadingConfig: {
      text: '데이터를 가져오는 중...',
    },
  })
  /**
   * 관리열, 테이블데이터, 여부열기시작
   */
  const handleColumnsAndData = (cols: any[], data: any[]) => {
    if (cols.length > 5) {
      columns = cols.map((col) => {
        return {
          ...col,
          width: 120,
        }
      })
    }
    if (columns.length > 30) {
      gridOptions.scrollX.enabled = true
    }
    if (data.length > 50) {
      gridOptions.scrollY.enabled = true
    }
  }
  /**
   * 로드테이블데이터
   */
  const loadGridData = () => {
    handleColumnsAndData(columns, tableData)
    gridRef.value
    && gridRef.value.loadColumn(columns).then(() => {
      gridRef.value.loadData(tableData)
      gridOptions.loading = false
      gridOptions.columnLength = columns.length
      gridOptions.rowLength = tableData.length
    })
  }
  /**
   * 새로고침테이블데이터
   */
  const refresh = () => {
    loadGridData()
    resetActiveColumn()
  }

  /**
   *  열매칭
   * @param cols 열매칭
   */
  const columnsConfig = (cols: any[]) => {
    return cols.map((col) => {
      const filterConfig = col.filterConfig || []
      const colFilterConfig = col.colFilterConfig || []
      const colDataProcessConfig = col.colDataProcessConfig || []
      return {
        ...col,
        filterConfig,
        colFilterConfig,
        colDataProcessConfig,
      }
    })
  }
  /**
   * 테이블데이터테이블데이터 처리
   */
  const tableHeadGenerate = (cols: any[]) => {
    return cols.map((col, index) => {
      const oldTitle = columns[index]?.title || ''
      const safeIndex = index + 1
      return {
        ...col,
        field: col.field || generateColumnNames(safeIndex),
        title: oldTitle || col.title || generateColumnNames(safeIndex), // 목록이름
        dataIndex: col.dataIndex || generateColumnNames(safeIndex),
        slots: { header: 'customHeader' },
        showOverflow: 'ellipsis',
        value: col.value, // 열값
      }
    })
  }
  // 테이블가져오기업데이트테이블
  const tableHeadHandle = (cols: string[]) => {
    if (columns.length === 0) { // 있음가져오기경과데이터, 테이블
      const thead = cols.map((col) => {
        return { title: col, value: [] }
      })
      columns = tableHeadGenerate(thead)
    }
    columns.forEach((col, index) => {
      col.title = cols[index] || ''
    })
    return columns
  }
  /**
   * 테이블데이터테이블body 데이터 처리
   */
  const tableBodyGenerate = (cols: any[], rows: any[]) => {
    return rows.map((row, index) => {
      const rowObj = {}
      row.forEach((item, index) => {
        rowObj[cols[index].field] = item
      })
      return {
        key: String(index + 1),
        ...rowObj,
      }
    })
  }
  /**
   * 관리열데이터로vxe-grid의형식
   * @param cols 열
   */
  const columnItemGenerate = (cols) => {
    return cols.map((col, index) => {
      const safeIndex = index + 1
      const colName = generateColumnNames(safeIndex) // 열이름
      return {
        ...col,
        field: colName,
        title: col.title || colName,
        dataIndex: colName,
        slots: { header: 'customHeader' },
        showOverflow: 'ellipsis',
      }
    })
  }

  /**
   * 가져오기의데이터 처리
   * @param data 가져오기의데이터 { app: string, type: string, version: string, path: { produceType: string, values: any[] } }
   * @param index 행데이터, 사용업데이트 index 열의데이터
   */
  const originDataHandle = (data: BatchElementDataInfo, index?: number, insert?: 'left' | 'right') => {
    const { path, app, type, version, picker_type } = data
    const { isTable, produceType } = path
    batchElementData = { app, path, picker_type, type, version }
    if (isTable) {
      // 테이블데이터, 안내여부가져오기단일열, 예개테이블
      GlobalModal.confirm({
        title: t('presentation'),
        content: t('dataBatch.tableGetConfirm'),
        okText: t('dataBatch.wholeTable'),
        cancelText: t('dataBatch.singleColumn'),
        onOk: () => {
          // 개테이블
          const { values } = path.tableData
          batchFormType.value = 'table'
          tableElement = { ...path.tableData, app, type, version } // 테이블요소
          if (values) {
            tableTypeHandle(values)
          }
        },
        onCancel: () => {
          // 단일열, 에서일가져오기시출력, columns 아니오로빈시, 추가으로 요소가져오기의방식관리
          const { values } = path.tableColumnData
          const col = { ...values[0], app, type, version }
          batchFormType.value = 'similar' // 단일열가져오기, 으로요소가져오기방식관리
          columns = [col]
          columnData = [values[0].value]
          similarTypeHandle(columns, columnData)
          col.values && delete col.values
          autoAddColumn() // 추가일열
        },
        centered: true,
        keyboard: false,
      })
      return
    }
    if (produceType === 'similar') {
      // 데이터
      const { values } = path
      const col = { ...values[0], app, type, version }
      const colData = values[0].value
      batchFormType.value = 'similar'
      if (columns.length > 0) {
        // 새데이터및기존데이터의tabUrl,  아니오일불가페이지가져오기
        const { tabUrl } = columns[0]
        if (tabUrl !== col.tabUrl) {
          message.warning(t('dataBatch.noSupportTwoPage'))
          return
        }
      }
      if (insert === 'left' && index !== undefined) {
        // 에서index 의왼쪽가장자리삽입
        columns.splice(index, 0, col)
        columnData.splice(index, 0, colData)
      }
      if (insert === 'right' && index !== undefined) {
        // 에서index 의오른쪽가장자리삽입
        columns.splice(index + 1, 0, col)
        columnData.splice(index + 1, 0, colData)
      }
      if (insert === undefined && index !== undefined) {
        // 업데이트 index 의데이터,요소가져오기의데이터
        columns[index] = col
        columnData[index] = colData
      }
      if (insert === undefined && index === undefined) {
        // 추가일열, 추가가져오기
        columns.push(col)
        columnData.push(colData)
      }
      similarTypeHandle(columns, columnData)
      autoAddColumn()
    }
  }
  /**
   * tableData데이터
   */
  const tableDataHandle = (cols, colsData: any[]) => {
    const tableDataTemp: any[] = []
    for (let i = 0; i < colsData.length; i++) {
      const colData = colsData[i]
      const col = cols[i]
      colData.forEach((item, index) => {
        tableDataTemp[index] = tableDataTemp[index] ? tableDataTemp[index] : { key: String(index + 1) }
        tableDataTemp[index][col.field] = item.text
      })
    }
    return tableDataTemp
  }

  /**
   * 가져오기데이터 처리
   * @param cols 열
   * @param colsData 열의데이터, 기존데이터의 values
   */
  const similarTypeHandle = (cols, colsData: any[]) => {
    columns = columnItemGenerate(cols)
    columns = columnsConfig(columns)
    tableData = tableDataHandle(columns, colsData)
    refresh()
  }

  /**
   * table 가져오기데이터 처리
   * @param values
   */
  const tableTypeHandle = (values: any[]) => {
    const tbody = []
    values.forEach((col) => {
      col.value.forEach((item, index2) => {
        tbody[index2] = tbody[index2] || []
        tbody[index2].push(item)
      })
    })
    columns = tableHeadGenerate(values)
    columns = columnsConfig(columns)
    tableData = tableBodyGenerate(columns, tbody)
    refresh()
  }

  /**
   * 추가일열
   */
  const autoAddColumn = () => {
    console.trace('autoAddColumn')
    let retryCount = 0
    const maxRetries = 10 // Maximum number of retries

    if (retryCount >= maxRetries) {
      console.warn('Max retries reached. Stopping autoAddColumn.')
      return
    }
    setTimeout(() => {
      if (gridOptions.loading) {
        retryCount++
        autoAddColumn()
      }
      else {
        retryCount = 0 // Reset retry count on success
        newBatch()
      }
    }, 200) // 지연200초, 대기데이터 처리완료
  }

  /**
   * 추가가져오기
   */
  const newBatch = (index?: number, insert?: 'left' | 'right') => {
    if (usePick.isChecking)
      return
    const batchType = batchFormType.value === 'similar' ? 'similar' : ''
    gridOptions.loading = true
    useBatchPick.startBatchPick('batch', { path: { batchType } }, (res) => {
      gridOptions.loading = false
      if (res.success) {
        const { type } = res.data
        if (type === 'web') {
          originDataHandle(res.data, index, insert)
        }
        else {
          message.warning(t('dataBatch.noSupportNotWeb'))
        }
      }
    })
  }
  /**
   * 테이블가져오기
   */
  const batchTableHead = () => {
    if (usePick.isChecking)
      return
    useBatchPick.startBatchPick('batch', { path: { batchType: 'head' } }, (res) => {
      if (res.success) {
        const { type, path } = res.data
        if (type === 'web') {
          columns = tableHeadHandle(path)
          columns = columnsConfig(columns)
          refresh()
        }
        else {
          message.warning(t('dataBatch.noSupportNotWeb'))
        }
      }
    })
  }
  /**
   * 요소
   * @param index 열검색
   */
  const addSimilarData = (index: number) => {
    if (usePick.isChecking)
      return
    const elements = columns[index]
    if (elements && 'xpath' in elements) {
      const path = JSON.parse(JSON.stringify(elements))
      path.checkType = VISUALIZATION
      path.matchTypes = []
      path.batchType = 'similarAdd'
      'value' in path && delete path.value
      const originElements = {
        app: elements.app,
        type: elements.type,
        version: elements.version,
        path,
      }
      useBatchPick.startBatchPick('batch', originElements, (res) => {
        if (res.success) {
          const { type } = res.data
          if (type === 'web') {
            originDataHandle(res.data, index)
          }
          else {
            message.warning(t('dataBatch.noSupportNotWeb'))
          }
        }
      })
    }
    else {
      message.warning(t('dataBatch.pleaseSelectColumn'))
    }
  }
  /**
   * 빈
   */
  const clear = () => {
    columnData = []
    tableData = []
    columns = []
    selectedColumnIndex.value = -1
    batchFormType.value = ''
    activeColumn.value = null
    tableElement = {}
    // batchObject = null
    batchElementData = null
    checkData.value = false
    refresh()
  }
  // 팝업
  const handleModalOk = (params) => {
    const data = params.column
    batchModalVisible.value = false
    const { dataIndex } = data
    columns.forEach((item) => {
      if (item.dataIndex === dataIndex) {
        Object.assign(item, data)
        selectedColumnIndex.value = -1
        activeColumn.value = null
      }
    })
    if (params.type === 'editColumnElement' && batchFormType.value === 'table') {
      // 테이블요소
      tableElement = { ...data }
    }
    // 제거완료열이름, 전송요청
    if (params.type !== 'editColumnName') {
      send2FetchData(columns)
    }
    else {
      loadGridData()
    }
  }
  // 테이블요소
  const editTableElement = () => {
    openModal({
      title: t(`dataBatch.editColumnElement`),
      type: 'editColumnElement',
      width: '100%',
      warpClassName: 'full-modal',
      column: tableElement,
    })
  }

  /**
   * 저장요소
   */
  const save = () => {
    formRef.value.validate().then((valid) => {
      if (valid) {
        if (!columns.length) {
          message.warning(t('dataBatch.batchFirst'))
          return
        }
        // 저장가져오기의요소까지요소라이브러리
        elementHandle()
      }
    })
  }
  const elementHandle = () => {
    const robotId = getUrlQueryField('robotId')
    const _name = formState.name
    let elementDataObj
    if (batchFormType.value === 'similar') {
      elementDataObj = {
        ...batchElementData,
        path: {
          checkType: columns[0]?.checkType || VISUALIZATION,
          matchTypes: [],
          produceType: batchFormType.value,
          values: columns.map((item) => {
            return removeColumnUselessKey(item)
          }),
        },
      }
    }
    else {
      elementDataObj = {
        ...batchElementData,
        path: {
          checkType: tableElement.checkType || VISUALIZATION,
          matchTypes: [],
          ...tableElement,
          // thead,
          produceType: batchFormType.value,
        },
      }
      elementDataObj.path = removeColumnUselessKey(elementDataObj.path) // 아니오필요의속성
      elementDataObj.path.values = columns.map((item) => {
        return removeColumnUselessKey(item)
      })
    }

    if (isEdit) {
      updateElement({
        robotId,
        element: {
          commonSubType: 'batch',
          id: batchObject.id,
          name: _name,
          elementData: JSON.stringify(elementDataObj),
        },
      }).then((res) => {
        if (res.code === '000000') {
          saveDone(_name, batchObject.id, robotId)
        }
      })
    }
    else {
      addElement({
        type: 'common',
        robotId,
        groupName: batchElementData.app,
        element: {
          commonSubType: 'batch',
          name: _name,
          icon: '',
          elementData: JSON.stringify(elementDataObj),
        },
      }).then((res) => {
        if (res.code === '000000') {
          saveDone(_name, res.data.elementId, robotId)
        }
      })
    }
  }
  const saveDone = (name: string, elementId: string, robotId: string) => {
    const noEmit = getUrlQueryField('noEmit')
    windowManager.emitTo({
      target: WINDOW_NAME.MAIN,
      type: 'save',
      from: WINDOW_NAME.BATCH,
      data: {
        name,
        elementId,
        robotId,
        noEmit,
      },
    })
    windowManager.closeWindow(WINDOW_NAME.BATCH)
  }
  // remove column useless key
  const removeColumnUselessKey = (column: any) => {
    const newItem = { ...column }
    Object.keys(newItem).forEach((key) => {
      if (ColumnsKeys.includes(key)) {
        delete newItem[key]
      }
    })
    return newItem
  }

  // 가져오기
  const cancel = () => {
    closeDataBatch(false)
  }

  // 닫기창
  function closeDataBatch(ask: boolean = true, data: any = {}) {
    if (useBatchPick.isPicking) {
      message.warning(t('dataBatch.dontCloseAtRuning'))
      return
    }
    function close() {
      windowManager.emitTo({
        target: WINDOW_NAME.MAIN,
        type: 'close',
        from: WINDOW_NAME.BATCH,
        data,
      })
      windowManager.closeWindow(WINDOW_NAME.BATCH)
    }
    if (!ask) {
      close()
    }
    else {
      GlobalModal.confirm({
        getContainer: () => document.querySelector('#dataBatch'),
        title: t('presentation'),
        content: t('dataBatch.windowCloseConfirm'),
        onOk() {
          close()
        },
        centered: true,
        keyboard: false,
      })
    }
  }
  /**
   * 열기 페이지, 가져오기데이터
   */
  function openSourcePage() {
    getElementData(batchObject, true)
  }

  //
  const hookInit = () => {
    http.resolveReadyPromise()
    isEdit = getUrlQueryField('isEdit') === 'true'
    const robotId = getUrlQueryField('robotId')
    if (isEdit) {
      // 요소
      const elementId = getUrlQueryField('elementId')
      getElementDetail({
        robotId,
        elementId,
      }).then((res) => {
        batchObject = res.data
        getElementData(res.data)
      })
    }
    else {
      // 추가요소
      newBatch()
    }
    //  가져오기모든요소목록
    getElementsAll({
      robotId,
      elementType: 'common',
    }).then((res) => {
      let allElements = []
      // 를 res.data배열내부item 의elements 배열평면
      res.data.forEach((item) => {
        allElements = allElements.concat(item.elements)
      })
      if (!isEdit) {
        const allElementsNames = allElements.map(item => item.name)
        formState.name = generateSheetName(allElementsNames, locale)
      }
    })
  }

  // 데이터
  function getElementData(data: { name: string, elementData: string }, openSourcePage: boolean = false) {
    const { name, elementData } = data
    formState.name = name
    gridOptions.loading = true
    const element = elementData ? JSON.parse(elementData) : {}
    batchElementData = element
    const { produceType } = element?.path
    batchFormType.value = produceType
    if (produceType === 'table') {
      tableElement = { ...element, ...element.path }
      tableElement.path && delete tableElement.path
    }
    if (openSourcePage) {
      element.path.openSourcePage = true
    }
    const timer = setTimeout(() => {
      gridOptions.loading = false
      checkData.value = true
      useBatchPick.finishCheck()
    }, 10 * 1000) // 10초후닫기
    useBatchPick.getBatchData('batch', JSON.stringify(element), (res) => {
      if (res.success) {
        const { data } = res
        if (data) {
          const { produceType } = data
          batchFormType.value = produceType
          if (produceType === 'table') {
            const { values } = data
            tableElement = { ...element, ...data }
            tableElement.path && delete tableElement.path
            if (values) {
              tableTypeHandle(values)
            }
          }
          else {
            const { values } = data
            columns = values.map(item => item)
            columnData = values.map(item => item.value)
            similarTypeHandle(columns, columnData)
          }
        }
        else {
          checkData.value = true
        }
        clearTimeout(timer)
      }
      else {
        checkData.value = true
        gridOptions.loading = false
        clearTimeout(timer)
      }
    })
  }

  function send2FetchData(cols) {
    gridOptions.loading = true
    if (batchFormType.value === 'similar') {
      batchElementData.path = {
        produceType: 'similar',
        checkType: cols[0].checkType || VISUALIZATION,
        matchTypes: [],
        values: cols.map((item) => {
          return removeColumnUselessKey(item)
        }),
      }
    }
    else {
      batchElementData.path = {
        ...tableElement,
        produceType: 'table',
        checkType: cols[0]?.checkType || VISUALIZATION,
        matchTypes: [],
        values: cols.map((item) => {
          return removeColumnUselessKey(item)
        }),
      }
    }
    useBatchPick.getBatchData('batch', JSON.stringify(batchElementData), (res) => {
      if (res.success && res.data) {
        const { produceType } = res.data
        if (produceType === 'table') {
          const { values } = res.data
          if (values) {
            tableTypeHandle(values)
          }
        }
        else {
          const { values } = res.data
          columns = values.map(item => item)
          columnData = values.map(item => item.value)
          similarTypeHandle(columns, columnData)
        }
      }
      loadGridData()
    })
  }

  return {
    formState,
    columns,
    tableData,
    menuItems,
    selectedColumnIndex,
    gridOptions,
    gridRef,
    formRef,
    rules,
    batchFormType,
    activeColumn,
    batchModalRef,
    checkData,
    menuClick,
    columnClick,
    loadGridData,
    save,
    cancel,
    newBatch,
    batchTableHead,
    clear,
    addSimilarData,
    handleModalOk,
    editTableElement,
    hookInit,
    closeDataBatch,
    openSourcePage,
    moreMenuClick,
    dataProcessEnable,
    checkboxClick,
    batchModalVisible,
    batchModalConfig,
  }
}
