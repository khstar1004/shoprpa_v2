import i18next from '@/plugins/i18next'

import type { BatchDataTableMenu } from '@/types/databatch.d'

export const Menus: BatchDataTableMenu[] = [
  {
    key: 'editColumnName',
    label: i18next.t('dataBatch.editColumnName'),
    // showType: 'similar',
  },
  {
    key: 'copyColumn',
    label: i18next.t('dataBatch.copyColumn'),
    showType: 'similar',
  },
  {
    key: 'insertColumnLeft',
    label: i18next.t('dataBatch.insertColumnLeft'),
    showType: 'similar',
  },
  {
    key: 'insertColumnRight',
    label: i18next.t('dataBatch.insertColumnRight'),
    showType: 'similar',
  },
  {
    key: 'similarAdd',
    label: i18next.t('dataBatch.similarAdd'),
    showType: 'similar',
  },
  {
    key: 'editColumnElement',
    label: i18next.t('dataBatch.editColumnElement'),
    showType: 'similar',
  },
  {
    key: 'toggleColumnData',
    label: i18next.t('dataBatch.toggleColumnData'),
    showType: 'similar',
    children: [
      {
        key: 'text',
        label: i18next.t('dataBatch.text'),
      },
      {
        key: 'href',
        label: i18next.t('dataBatch.href'),
      },
      {
        key: 'src',
        label: i18next.t('dataBatch.src'),
      },
    ],
  },
  {
    key: 'colDataProcessConfig',
    label: i18next.t('dataBatch.colDataProcessConfig'),
    children: [
      {
        key: 'ExtractNum',
        label: i18next.t('dataBatch.ExtractNum'),
        checkable: true,
        checked: false,
        modal: false,
        showEdit: false,
      },
      {
        key: 'Trim',
        label: i18next.t('dataBatch.Trim'),
        checkable: true,
        checked: false,
        modal: false,
        showEdit: false,
      },
      {
        key: 'Replace',
        label: i18next.t('dataBatch.Replace'),
        checkable: true,
        checked: false,
        modal: true,
        showEdit: false,
      },
      {
        key: 'Prefix',
        label: i18next.t('dataBatch.Prefix'),
        checkable: true,
        checked: false,
        modal: true,
        showEdit: false,
      },
      {
        key: 'Suffix',
        label: i18next.t('dataBatch.Suffix'),
        checkable: true,
        checked: false,
        modal: true,
        showEdit: false,
      },
      {
        key: 'FormatTime',
        label: i18next.t('dataBatch.FormatTime'),
        checkable: true,
        checked: false,
        modal: true,
        showEdit: false,
      },
      {
        key: 'Regular',
        label: i18next.t('dataBatch.Regular'),
        checkable: true,
        checked: false,
        modal: true,
        showEdit: false,
      },
      {
        key: 'clear',
        label: i18next.t('dataBatch.clear'),
      },
    ],
    active: false,
  },
  {
    key: 'colFilterConfig',
    label: i18next.t('dataBatch.colFilterConfig'),
    active: false,
  },
  {
    key: 'filterConfig',
    label: i18next.t('dataBatch.filterConfig'),
    active: false,
  },
  {
    key: 'deleteColumn',
    label: i18next.t('dataBatch.deleteColumn'),
    showType: 'similar',
  },
]

export const webData = {
  version: '1',
  type: 'web',
  app: 'chrome',
  path: {
    xpath: {
      rpa: 'special',
      value: [
        {
          type: 'other',
          value: '//a[contains(@href,"iana.org")]',
        },
      ],
    },
    cssSelector: {
      rpa: 'special',
      value: [
        {
          type: 'other',
          value: 'a',
        },
      ],
    },
    pathDirs: [
      {
        tag: 'a',
        checked: true,
        value: 'a',
        attrs: [
          {
            name: 'index',
            value: {
              rpa: 'special',
              value: [
                {
                  type: 'other',
                  value: '',
                },
              ],
            },
            checked: false,
            type: 0,
          },
          {
            name: 'href',
            value: {
              rpa: 'special',
              value: [
                {
                  type: 'other',
                  value: 'https://www.iana.org/help/example-domains',
                },
              ],
            },
            checked: true,
            type: 1,
          },
        ],
      },
    ],
    parentClass: '',
    tag: '링크',
    text: 'More information...',
    url: {
      rpa: 'special',
      value: [
        {
          type: 'other',
          value: 'https://www.iana.org/help/example-domains',
        },
      ],
    },
    shadowRoot: false,
    tabTitle: 'IANA Example Domains',
    tabUrl: 'https://www.iana.org/help/example-domains',
    isFrame: false,
    frameId: 0,
    checkType: 'visualization',
    matchTypes: [],
  },
  picker_type: 'ELEMENT',
}

export const ColumnsKeys = [
  // 'field',
  // 'title',
  // 'dataIndex',
  'slots',
  // 'isTable',
  // 'app',
  // 'type',
  // 'version',
  'showOverflow',
  // 'values',
  'columnIndex',
  'width',
]

export const expSelectList = [
  {
    label: '숫자',
    key: 'math',
    options: [
      { label: '같음', value: '==', key: 'equal' },
      { label: '같지 않음', value: '!=', key: 'notEqual' },
      { label: '작거나 같음', value: '<=', key: 'lessEqual' },
      { label: '크거나 같음', value: '>=', key: 'greaterEqual' },
      { label: '작음', value: '<', key: 'less' },
      { label: '큼', value: '>', key: 'greater' },
    ],
  },
  {
    label: '문자열',
    key: 'string',
    options: [
      { label: '비어 있음', value: 'isnull', key: 'isNull' },
      { label: '비어 있지 않음', value: 'notnull', key: 'notNull' },
      { label: '포함', value: 'contains', key: 'contains' },
      { label: '포함하지 않음', value: 'not_contains', key: 'notContains' },
      { label: '목록에 포함', value: 'enumerate', key: 'enumerate' },
      { label: '시작함', value: 'startswith', key: 'startsWith' },
      { label: '시작하지 않음', value: 'not_startswith', key: 'notStartsWith' },
      { label: '끝남', value: 'endswith', key: 'endsWith' },
      { label: '끝나지 않음', value: 'not_endswith', key: 'notEndsWith' },
    ],
  },
  {
    label: '날짜/시간',
    key: 'datetime',
    options: [
      { label: '기준 시간 이전', value: 'time_befor', key: 'timeBefore' },
      { label: '기준 시간 이후', value: 'time_after', key: 'timeAfter' },
      { label: '기간 내', value: 'time_between', key: 'timeBetween' },
    ],
  },
]

export const dateSelectList = [
  { label: '기존 형식', value: '', key: 'original' },
  { label: '%Y-%m-%d %H:%M:%S', value: '%Y-%m-%d %H:%M:%S', key: 'ymd_hms' },
  { label: '%Y/%m/%d %H:%M:%S', value: '%Y/%m/%d %H:%M:%S', key: 'y_m_d_hms' },
  { label: '%Y.%m.%d %H:%M:%S', value: '%Y.%m.%d %H:%M:%S', key: 'y.m.d_hms' },
  { label: '%Y년%m월%d일 %H:%M:%S', value: '%Y년%m월%d일 %H:%M:%S', key: 'ycn_mn_dn_hms' },
  { label: '%Y-%m-%dT%H:%M:%S', value: '%Y-%m-%dT%H:%M:%S', key: 'ymdThms' },
  { label: '%Y년%m월%d일 %H시%M분%S초', value: '%Y년%m월%d일 %H시%M분%S초', key: 'ycn_mn_dn_hms_cn' },
  { label: '%Y-%m-%d %H:%M', value: '%Y-%m-%d %H:%M', key: 'ymd_hm' },
  { label: '%Y/%m/%d %H:%M', value: '%Y/%m/%d %H:%M', key: 'y_m_d_hm' },
  { label: '%Y.%m.%d %H:%M', value: '%Y.%m.%d %H:%M', key: 'y.m.d_hm' },
  { label: '%Y년%m월%d일 %H:%M', value: '%Y년%m월%d일 %H:%M', key: 'ycn_mn_dn_hm' },
  { label: '%Y년%m월%d일 %H시%M분', value: '%Y년%m월%d일 %H시%M분', key: 'ycn_mn_dn_hm_cn' },
  { label: '%m-%d %H:%M:%S', value: '%m-%d %H:%M:%S', key: 'md_hms' },
  { label: '%m/%d %H:%M:%S', value: '%m/%d %H:%M:%S', key: 'm_d_hms' },
  { label: '%m.%d %H:%M:%S', value: '%m.%d %H:%M:%S', key: 'm.d_hms' },
  { label: '%m월%d일 %H:%M:%S', value: '%m월%d일 %H:%M:%S', key: 'mn_dn_hms' },
  { label: '%m월%d일 %H시%M분%S초', value: '%m월%d일 %H시%M분%S초', key: 'mn_dn_hms_cn' },
  { label: '%Y-%m-%d', value: '%Y-%m-%d', key: 'ymd' },
  { label: '%Y-%#m-%#d', value: '%Y-%#m-%#d', key: 'y_md' },
  { label: '%Y/%m/%d', value: '%Y/%m/%d', key: 'y_m_d' },
  { label: '%Y.%m.%d', value: '%Y.%m.%d', key: 'y.m.d' },
  { label: '%Y년%m월%d일', value: '%Y년%m월%d일', key: 'ycn_mn_dn' },
  { label: '%Y-%m', value: '%Y-%m', key: 'ym' },
  { label: '%Y/%m', value: '%Y/%m', key: 'y_m' },
  { label: '%Y.%m', value: '%Y.%m', key: 'y.m' },
  { label: '%Y년%m월', value: '%Y년%m월', key: 'ycn_mn' },
  { label: '%m-%d', value: '%m-%d', key: 'md' },
  { label: '%m/%d', value: '%m/%d', key: 'm_d' },
  { label: '%m.%d', value: '%m.%d', key: 'm.d' },
  { label: '%m월%d일', value: '%m월%d일', key: 'mn_dn' },
  { label: '%H:%M:%S', value: '%H:%M:%S', key: 'hms' },
  { label: '%H시%M분%S초', value: '%H시%M분%S초', key: 'hms_cn' },
  { label: '%H:%M', value: '%H:%M', key: 'hm' },
  { label: '%H시%M분', value: '%H시%M분', key: 'hm_cn' },
  { label: '%w (요일 번호, 일요일=0)', value: '%w', key: 'week_num' },
  { label: '%j (연중 일수)', value: '%j', key: 'day_of_year' },
  { label: '%W (연중 주차)', value: '%W', key: 'week_of_year' },
]
