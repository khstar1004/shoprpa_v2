/**
 * 지정일반량
 */
import { Break, Catch, CatchEnd, Continue, Else, ElseEnd, ElseIfEnd, Finally, FinallyEnd, ForDictEnd, ForEnd, ForListEnd, ForStepEnd, Group, GroupEnd, IfEnd, Try, TryEnd, WhileEnd } from '@/views/Arrange/config/atomKeyMap'

export const DISABLED_BREAKPOINT_TYPE = [
  Group,
  GroupEnd,
  IfEnd,
  ElseIfEnd,
  ElseEnd,
  // 'endloop',
  WhileEnd,
  ForStepEnd,
  ForListEnd,
  ForDictEnd,
  ForEnd,
  TryEnd,
  CatchEnd,
  FinallyEnd,
  Else,
  Try,
  Catch,
  Finally,
  Break,
  Continue,
]
export const PAGE_INIT_INDENT = 82
export const PAGE_LEVEL_INDENT = 25
export const RECORDER_INIT_INDENT = 72
export const RECORDER_LEVEL_INDENT = 25

export const FLOW_DISABLE = 'disable'
export const FLOW_ACTIVE = 'active'
export const FLOW_DEBUGGING = 'debugging'
export const FLOW_FORBID = 'forbid'
export const DEFAULT_DESC_TEXT = '--'

export const SPECIALKEY = ''

export const defaultValueText = '<p><br></p>' // 텍스트태그
export const elementTag = 'e.' // 요소태그

export const CONDITION_OPTIONS_EXCEL_TYPE = [
  {
    label: '같음',
    operator: '==',
    value: '==',
  },
  {
    label: '같지 않음',
    operator: '!=',
    value: '!=',
  },
  {
    label: '시작함',
    operator: 'startswith',
    value: 'startswith',
    // value: 'SpiffWorkflow.operators.Equal'
  },
  {
    label: '시작하지 않음',
    operator: 'not_startswith',
    value: 'not_startswith',
    // value: 'SpiffWorkflow.operators.NotEqual'
  },
  {
    label: '끝남',
    operator: 'endswith',
    value: 'endswith',
    // value: 'SpiffWorkflow.operators.Equal'
  },
  {
    label: '끝나지 않음',
    operator: 'not_endswith',
    value: 'not_endswith',
    // value: 'SpiffWorkflow.operators.NotEqual'
  },
  {
    label: '포함',
    operator: 'contains',
    value: 'contains',
    // value: 'SpiffWorkflow.operators.Match'
  },
  {
    label: '포함하지 않음',
    operator: 'not_contains',
    value: 'not_contains',
    // value: 'SpiffWorkflow.operators.NotMatch'
  },
  {
    label: '큼',
    operator: '>',
    value: '>',
    // value: 'SpiffWorkflow.operators.GreaterThan',
  },
  {
    label: '크거나 같음',
    operator: '>=',
    value: '>=',
    // value: 'SpiffWorkflow.operators.GreaterThan',
  },
  {
    label: '작음',
    operator: '<',
    value: '<',
    // value: 'SpiffWorkflow.operators.LessThan'
  },
  {
    label: '작거나 같음',
    operator: '<=',
    value: '<=',
    // value: 'SpiffWorkflow.operators.LessThan'
  },
  {
    label: '비어 있음',
    operator: 'isnull',
    value: 'isnull',
  },
  {
    label: '비어 있지 않음',
    operator: 'notnull',
    value: 'notnull',
  },
]

export const CONDITION_OPTIONS_DATAFRAME_TYPE = [
  {
    label: '같음',
    operator: '==',
    value: '==',
  },
  {
    label: '같지 않음',
    operator: '!=',
    value: '!=',
  },
  {
    label: '시작함',
    operator: 'startswith',
    value: 'startswith',
  },
  {
    label: '시작하지 않음',
    operator: 'not_startswith',
    value: 'not_startswith',
  },
  {
    label: '끝남',
    operator: 'endswith',
    value: 'endswith',
  },
  {
    label: '끝나지 않음',
    operator: 'not_endswith',
    value: 'not_endswith',
  },
  {
    label: '포함',
    operator: 'contains',
    value: 'contains',
  },
  {
    label: '포함하지 않음',
    operator: 'not_contains',
    value: 'not_contains',
  },
  {
    label: '큼',
    operator: '>',
    value: '>',
  },
  {
    label: '크거나 같음',
    operator: '>=',
    value: '>=',
  },
  {
    label: '작음',
    operator: '<',
    value: '<',
  },
  {
    label: '작거나 같음',
    operator: '<=',
    value: '<=',
  },
  {
    label: '목록에 포함',
    operator: 'enumerate',
    value: 'enumerate',
  },
  {
    label: '비어 있음',
    operator: 'isnull',
    value: 'isnull',
  },
  {
    label: '비어 있지 않음',
    operator: 'notnull',
    value: 'notnull',
  },
]
