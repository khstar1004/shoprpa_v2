/**
 * 주쪽의매칭정보
 */

const ROBOT_SOURCE_LOCAL = '본'
const ROBOT_SOURCE_OFFICIAL = '방법마켓'
const ROBOT_SOURCE_TEAM = '기업업무마켓'
const ROBOT_SOURCE_COMMANDER = '부서'
const ROBOT_SOURCE_TEXT = {
 [ROBOT_SOURCE_LOCAL]: '본',
 [ROBOT_SOURCE_OFFICIAL]: '방법마켓',
 [ROBOT_SOURCE_TEAM]: '기업업무마켓',
 [ROBOT_SOURCE_COMMANDER]: '스케줄링중',
}

const ROBOT_TYPE_OPTIONS = [
 {
 value: 'finance',
 label: '재무서비스',
 },
 {
 value: 'education',
 label: '',
 },
 {
 value: 'medical',
 label: '',
 },
 {
 value: 'game',
 label: '',
 },
 {
 value: 'E-commerce',
 label: '상업',
 },
 {
 value: 'stream',
 label: '직선',
 },
 {
 value: 'telecom',
 label: '정보',
 },
 {
 value: 'government',
 label: '/업무단일위치',
 },
 {
 value: 'manufacturing',
 label: '제품제어',
 },
 {
 value: 'construction',
 label: '생성/제품',
 },
 {
 value: 'tobacco',
 label: '',
 },
 {
 value: 'operation',
 label: '실행운영',
 },
 {
 value: 'personnel',
 label: '사람',
 },
 {
 value: 'city_service',
 label: '시장서비스',
 },
 {
 value: 'car',
 label: '차업무',
 },
 {
 value: 'new_energy',
 label: '새가능',
 },
 {
 value: 'other',
 label: '그',
 },
]

export {
 ROBOT_SOURCE_LOCAL,
 ROBOT_SOURCE_OFFICIAL,
 ROBOT_SOURCE_TEAM,
 ROBOT_SOURCE_TEXT,
 ROBOT_TYPE_OPTIONS,
}
