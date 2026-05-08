/**
 * 주쪽의매칭정보
 */

const ROBOT_SOURCE_LOCAL = '본'
const ROBOT_SOURCE_OFFICIAL = '방법마켓'
const ROBOT_SOURCE_TEAM = '기업업무마켓'
const ROBOT_SOURCE_COMMANDER = '부서'
const ROBOT_SOURCE_TEXT = {
  [ROBOT_SOURCE_LOCAL]: '내 로봇',
  [ROBOT_SOURCE_OFFICIAL]: '공식 마켓',
  [ROBOT_SOURCE_TEAM]: '팀 마켓',
  [ROBOT_SOURCE_COMMANDER]: '스케줄러',
}

const ROBOT_TYPE_OPTIONS = [
  {
    value: 'finance',
    label: '금융',
  },
  {
    value: 'education',
    label: '교육',
  },
  {
    value: 'medical',
    label: '의료',
  },
  {
    value: 'game',
    label: '게임',
  },
  {
    value: 'E-commerce',
    label: '이커머스',
  },
  {
    value: 'stream',
    label: '스트리밍',
  },
  {
    value: 'telecom',
    label: '통신',
  },
  {
    value: 'government',
    label: '공공기관',
  },
  {
    value: 'manufacturing',
    label: '제조',
  },
  {
    value: 'construction',
    label: '건설',
  },
  {
    value: 'tobacco',
    label: '담배',
  },
  {
    value: 'operation',
    label: '운영',
  },
  {
    value: 'personnel',
    label: '인사',
  },
  {
    value: 'city_service',
    label: '도시 서비스',
  },
  {
    value: 'car',
    label: '자동차',
  },
  {
    value: 'new_energy',
    label: '신재생 에너지',
  },
  {
    value: 'other',
    label: '기타',
  },
]

export {
  ROBOT_SOURCE_LOCAL,
  ROBOT_SOURCE_OFFICIAL,
  ROBOT_SOURCE_TEAM,
  ROBOT_SOURCE_TEXT,
  ROBOT_TYPE_OPTIONS,
}
