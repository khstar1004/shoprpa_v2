// 예약 작업트리거기기유형
export const TASK_FILE = 'file'
export const TASK_MAIL = 'mail'
export const TASK_TIME = 'schedule'
export const TASK_MANUAL = 'manual'
export const TASK_HOTKEY = 'hotKey'

export enum TASK_TYPE {
  TASK_FILE = 'file',
  TASK_MAIL = 'mail',
  TASK_TIME = 'schedule',
  TASK_MANUAL = 'manual',
  TASK_HOTKEY = 'hotKey',
}

export const TASK_TYPE_TEXT = {
  [TASK_FILE]: '파일트리거',
  [TASK_MAIL]: '메일함트리거',
  [TASK_TIME]: '시간트리거',
  [TASK_MANUAL]: '동작트리거',
  [TASK_HOTKEY]: '핫키트리거',
}

export const TASK_TYPE_OPTION = [
  { label: '동작트리거', value: TASK_MANUAL },
  { label: '시간트리거', value: TASK_TIME },
  { label: '파일트리거', value: TASK_FILE },
  { label: '메일함트리거', value: TASK_MAIL },
  { label: '핫키트리거', value: TASK_HOTKEY },
]

export const FileEvents = [
  {
    label: '생성',
    value: 'create',
  },
  {
    label: '삭제',
    value: 'delete',
  },
  {
    label: '업데이트',
    value: 'update',
  },
  {
    label: '이름 변경',
    value: 'rename',
  },
]

export const WEEK_OPTIONS = [
  { label: '월요일', value: 0 },
  { label: '화요일', value: 1 },
  { label: '수요일', value: 2 },
  { label: '목요일', value: 3 },
  { label: '금요일', value: 4 },
  { label: '토요일', value: 5 },
  { label: '일요일', value: 6 },
]

export const WEEK_MAP = {
  0: '월요일',
  1: '화요일',
  2: '수요일',
  3: '목요일',
  4: '금요일',
  5: '토요일',
  6: '일요일',
}
export const WEEK_MAP_EN = {
  0: 'monday',
  1: 'tuesday',
  2: 'wednesday',
  3: 'thursday',
  4: 'friday',
  5: 'saturday',
  6: 'sunday',
}
export const WEEK_MAP_INDAYJS = {
  0: 1,
  1: 2,
  2: 3,
  3: 4,
  4: 5,
  5: 6,
  6: 0,
}

export const DAYJS_WEEK_MAP = {
  0: 6, // 일요일
  1: 0, // 월요일
  2: 1, // 화요일
  3: 2, // 수요일
  4: 3, // 목요일
  5: 4, // 금요일
  6: 5, // 토요일
}

/**
 * @returns F1-F12, 0-9, A-Z
 */
export function Hotkeys() {
  const keys = []
  for (let i = 1; i <= 12; i++) {
    keys.push({ label: `F${i}`, value: `F${i}` })
  }
  for (let i = 0; i <= 9; i++) {
    keys.push({ label: `${i}`, value: `${i}` })
  }
  for (let i = 65; i <= 90; i++) {
    keys.push({ label: String.fromCharCode(i), value: String.fromCharCode(i) })
  }
  return keys
}
