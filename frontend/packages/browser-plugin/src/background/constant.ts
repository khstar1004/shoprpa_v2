export const SUPPORTED_PROTOCOLS = ['http://', 'https://', 'file://', 'ftp://']

export const OLD_EXTENSION_IDS = ['dibfknoajiboamheempfppeapcedplgm', 'gfpcfabhkgenjcmjgnldmkhjieekeeea']

export const CURRENT_EXTENSION_ID = chrome.runtime.id

export const NATIVE_HOST_NAME = 'com.shoprpa.nativehost'

export const IGNORE_LOG_KEYS = ['getElement', 'contentInject', 'backgroundInject']

export enum StatusCode {
  SUCCESS = '0000',
  UNKNOWN_ERROR = '5001',
  ELEMENT_NOT_FOUND = '5002',
  EXECUTE_ERROR = '5003',
  VERSION_ERROR = '5004',
}

export enum ErrorMessage {
  TAB_GET_ERROR = '탭 정보를 가져오지 못했습니다.',
  ACTIVE_TAB_ERROR = '대상 탭을 찾을 수 없습니다. 브라우저 창을 확인하세요.',
  NUMBER_ID_ERROR = '탭 ID는 숫자여야 합니다.',
  FRAME_GET_ERROR = '대상 iframe을 찾을 수 없습니다.',
  CURRENT_TAB_UNSUPPORT_ERROR = '현재 탭에서는 웹 요소 선택을 지원하지 않습니다.',
  NOT_SIMILAR_ELEMENT = '선택한 요소가 유사 요소가 아닙니다.',
  SIMILAR_NOT_FOUND = '요소를 찾을 수 없습니다.',
  RELATIVE_ELEMENT_PARAMS_ERROR = '상대 요소 매개변수가 올바르지 않습니다.',
  // Two legacy error codes intentionally share the same user-facing message.
  // eslint-disable-next-line ts/no-duplicate-enum-values
  ELEMENT_NOT_FOUND = '요소를 찾을 수 없습니다.',
  UNSUPPORT_ERROR = '현재 버전에서 지원하지 않는 기능입니다. 최신 버전으로 업데이트하세요.',
  PARAMS_URL_NOT_FOUND = 'url 필드가 필요합니다.',
  PARAMS_NAME_NOT_FOUND = 'name 필드가 필요합니다.',
  PARAMS_NAME_VALUE_NOT_FOUND = 'name 및 value 필드가 필요합니다.',
  CONTEXT_NOT_FOUND = '실행 컨텍스트를 찾을 수 없습니다. 페이지가 완전히 로드된 뒤 다시 시도하세요.',
  EXECUTE_ERROR = '실행에 실패하여 결과를 가져올 수 없습니다.',
  DEBUGGER_TIMEOUT = '디버거 분리 상태 확인 시간이 초과되었습니다',
  CONTENT_MESSAGE_ERROR = '콘텐츠 스크립트 메시지 처리 중 오류가 발생했습니다.',
}

export enum SuccessMessage {
  DELETE_SUCCESS = '삭제되었습니다.',
  SET_SUCCESS = '완료',
  EMPTY_SUCCESS = '비웠습니다.',
}
