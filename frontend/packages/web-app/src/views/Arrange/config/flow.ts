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
 label: '대기',
 operator: '==',
 value: '==',
 },
 {
 label: '아니오대기',
 operator: '!=',
 value: '!=',
 },
 {
 label: '열기 예',
 operator: 'startswith',
 value: 'startswith',
 // value: 'SpiffWorkflow.operators.Equal'
 },
 {
 label: '열기 아니오예',
 operator: 'not_startswith',
 value: 'not_startswith',
 // value: 'SpiffWorkflow.operators.NotEqual'
 },
 {
 label: '결과예',
 operator: 'endswith',
 value: 'endswith',
 // value: 'SpiffWorkflow.operators.Equal'
 },
 {
 label: '결과아니오예',
 operator: 'not_endswith',
 value: 'not_endswith',
 // value: 'SpiffWorkflow.operators.NotEqual'
 },
 {
 label: '패키지',
 operator: 'contains',
 value: 'contains',
 // value: 'SpiffWorkflow.operators.Match'
 },
 {
 label: '아니오패키지',
 operator: 'not_contains',
 value: 'not_contains',
 // value: 'SpiffWorkflow.operators.NotMatch'
 },
 {
 label: '대',
 operator: '>',
 value: '>',
 // value: 'SpiffWorkflow.operators.GreaterThan',
 },
 {
 label: '대대기',
 operator: '>=',
 value: '>=',
 // value: 'SpiffWorkflow.operators.GreaterThan',
 },
 {
 label: '소',
 operator: '<',
 value: '<',
 // value: 'SpiffWorkflow.operators.LessThan'
 },
 {
 label: '소대기',
 operator: '<=',
 value: '<=',
 // value: 'SpiffWorkflow.operators.LessThan'
 },
 {
 label: '로빈',
 operator: 'isnull',
 value: 'isnull',
 },
 {
 label: '아니오로빈',
 operator: 'notnull',
 value: 'notnull',
 },
]

export const CONDITION_OPTIONS_DATAFRAME_TYPE = [
 {
 label: '대기',
 operator: '==',
 value: '==',
 },
 {
 label: '아니오대기',
 operator: '!=',
 value: '!=',
 },
 {
 label: '열기 예',
 operator: 'startswith',
 value: 'startswith',
 },
 {
 label: '열기 아니오예',
 operator: 'not_startswith',
 value: 'not_startswith',
 },
 {
 label: '결과예',
 operator: 'endswith',
 value: 'endswith',
 },
 {
 label: '결과아니오예',
 operator: 'not_endswith',
 value: 'not_endswith',
 },
 {
 label: '패키지',
 operator: 'contains',
 value: 'contains',
 },
 {
 label: '아니오패키지',
 operator: 'not_contains',
 value: 'not_contains',
 },
 {
 label: '대',
 operator: '>',
 value: '>',
 },
 {
 label: '대대기',
 operator: '>=',
 value: '>=',
 },
 {
 label: '소',
 operator: '<',
 value: '<',
 },
 {
 label: '소대기',
 operator: '<=',
 value: '<=',
 },
 {
 label: '',
 operator: 'enumerate',
 value: 'enumerate',
 },
 {
 label: '로빈',
 operator: 'isnull',
 value: 'isnull',
 },
 {
 label: '아니오로빈',
 operator: 'notnull',
 value: 'notnull',
 },
]
