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
  TAB_GET_ERROR = '가져오기탭실패',
  ACTIVE_TAB_ERROR = '찾을 수 없는 탭, 확인하세요여부목록창',
  NUMBER_ID_ERROR = 'id 예숫자',
  FRAME_GET_ERROR = '찾을 수 없는 요소의iframe',
  CURRENT_TAB_UNSUPPORT_ERROR = '현재탭지원하지 않음web선택',
  NOT_SIMILAR_ELEMENT = '해당요소아니오예요소',
  SIMILAR_NOT_FOUND = '찾을 수 없는 요소',
  RELATIVE_ELEMENT_PARAMS_ERROR = '닫기 요소매개변수오류',
  ELEMENT_NOT_FOUND = '찾을 수 없는 요소',
  UNSUPPORT_ERROR = '미완료,요청업그레이드까지새버전',
  PARAMS_URL_NOT_FOUND = '적음url필드!',
  PARAMS_NAME_NOT_FOUND = '적음name필드!',
  PARAMS_NAME_VALUE_NOT_FOUND = '적음필드name, value!',
  CONTEXT_NOT_FOUND = '찾을 수 없는 실행위아래문서, 요청확인페이지완료로드완료',
  EXECUTE_ERROR = '실행실패, 가져올 수 없는 결과',
  DEBUGGER_TIMOUT = '감지 Debugger 상태시간 초과',
  CONTENT_MESSAGE_ERROR = '내용본메시지오류',
}

export enum SuccessMessage {
  DELETE_SUCCESS = '삭제성공',
  SET_SUCCESS = '완료',
  EMPTY_SUCCESS = '빈성공',
}
