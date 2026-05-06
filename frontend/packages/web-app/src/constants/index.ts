import type { TablePaginationConfig } from 'ant-design-vue'

// 창이름
export enum WINDOW_NAME {
  MAIN = 'main',
  BATCH = 'batch',
  RECORD = 'record',
  RECORD_MENU = 'record-menu',
  SMART_COMP_PICK_MENU = 'smart-comp-pick-menu',
  LOGWIN = 'logwin',
  MULTICHAT = 'multichat',
  USERFORM = 'userform',
}

// 부서높음정도
export const BOTTOM_BOOTLS_HEIGHT_DEFAULT = 48
// 부서열기후소높음정도
export const BOTTOM_BOOTLS_HEIGHT_SIZE_MIN = 251
// 통신가장자리너비정도
export const COMMON_SIDER_WIDTH = 280

// localStorage key
export const ELEMENTS_TREE_EXPANDE_KEYS = 'elements_tree_expanded_keys'
export const IMAGES_TREE_EXPANDE_KEYS = 'images_tree_expanded_keys'
// 저장기기중열기의프로세스 key
export const PROCESS_OPEN_KEYS = 'process_open_keys'
// 선택닫기업데이트안내팝업의버전
export const CLOSE_UPDATE_MODAL_VERSION = 'close_update_modal_version'

// 왼쪽너비정도
export const LEFT_BOOTLS_WIDTH_DEFAULT = 40
// 왼쪽소너비정도
export const LEFT_BOOTLS_WIDTH_SIZE_MIN = 200
// 왼쪽대너비정도
export const LEFT_BOOTLS_WIDTH_SIZE_MAX = 240

export const SUCCES_MSG = 'Success'
export const ERROR_MSG = 'Failed'

export const SUCCESS_CODES = ['200', '000000', 200, '0000']
export const ERROR_CODES = ['500', '5001', '1001']
export const UN_AUTHORIZED_CODES = ['302', '4001', '401', '403', '900005', '900001'] // 900005빈경과 900001계정

export const VUE_APP_HELP = 'https://www.shoprpa.com/docs/'

export const paginationConfig: TablePaginationConfig = {
  hideOnSinglePage: true,
  defaultPageSize: 10,
  pageSizeOptions: ['10'],
}