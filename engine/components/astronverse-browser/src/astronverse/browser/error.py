from astronverse.baseline.error.error import BaseException
from astronverse.baseline.error.error import *
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

BROWSER_OPEN_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("브라우저열기실패"))
BROWSER_PATH_EMPTY: ErrorCode = ErrorCode(BizCode.LocalErr, _("찾을 수 없는 브라우저경로, 입력하세요브라우저경로실행"))
SELECT_MATCHING_APP_PATH: ErrorCode = ErrorCode(BizCode.LocalErr, _("선택하세요브라우저매칭의사용경로"))

PARAMETER_INVALID_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("매개변수예외") + ": {}")
WEB_LOAD_TIMEOUT: ErrorCode = ErrorCode(BizCode.LocalErr, _("웹 페이지로드시간 초과, 다시 시도하세요"))
WEB_GET_BROWSER_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("가져오기브라우저객체실패"))
WEB_GET_URL_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("가져오기웹 페이지URL실패"))
WEB_GET_TITLE_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("가져오기현재페이지제목실패"))
WEB_GET_ELE_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("웹 페이지원조회실패") + " {}")
WEB_GET_SIMILAR_ELE_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("브라우저가져오기 원목록실패"))
WEB_GET_SELECTED_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("브라우저가져오기드롭다운선택중값실패"))
WEB_GET_CHECKBOX_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("브라우저가져오기복사선택선택중값실패"))
WEB_PAGES_NUM_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("페이지수예정수"))
WEB_WAIT_TIME_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("대기시간할 수 없음소0!"))
WEB_ELE_ATTR_NAME_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("입력하세요요소속성이름"))
WEB_EXEC_ELE_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("확장실행오류") + ": {}")
DOWNLOAD_WINDOW_NO_FIND: ErrorCode = ErrorCode(
    BizCode.LocalErr, _("다운로드 창을 찾을 수 없습니다. 직접 클릭했거나 브라우저가 다운로드 창을 닫았을 수 있습니다")
)
UPLOAD_WINDOW_NO_FIND: ErrorCode = ErrorCode(
    BizCode.LocalErr, _("업로드 창을 찾을 수 없습니다. 직접 클릭했거나 브라우저가 업로드 창을 닫았을 수 있습니다")
)
SWITCH_TAB_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("웹 페이지실패, 있음까지기호합치기파일의웹 페이지"))
BROWSER_EXTENSION_INSTALL_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("브라우저 확장 통신 오류입니다. 다시 시도하세요."))
BROWSER_EXTENSION_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("브라우저 확장 오류") + ": {}")
BROWSER_OPEN_TIMEOUT: ErrorCode = ErrorCode(BizCode.LocalErr, _("브라우저 열기 시간 초과"))
BROWSER_GET_TIMEOUT: ErrorCode = ErrorCode(BizCode.LocalErr, _("브라우저가 아직 열리지 않았습니다"))
BROWSER_NO_INSTALL: ErrorCode = ErrorCode(BizCode.LocalErr, _("브라우저가 설치되어 있지 않습니다"))
CODE_EMPTY: ErrorCode = ErrorCode(BizCode.LocalErr, _("스크립트 내용이 비어 있습니다"))
CODE_NO_MAIN_FUNC: ErrorCode = ErrorCode(BizCode.LocalErr, _("JavaScript 코드에 main 함수가 없습니다"))
FOCUS_TIMEOUT_MUST_BE_POSITIVE: ErrorCode = ErrorCode(BizCode.LocalErr, _("포커스 대기시간은 0 이상이어야 합니다"))
KEY_PRESS_INTERVAL_MUST_BE_NON_NEGATIVE: ErrorCode = ErrorCode(BizCode.LocalErr, _("입력 간격은 0 이상이어야 합니다"))

LINUX_MUST_BROWSER_PATH_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("Linux에서는 브라우저 실행 파일 경로를 입력해야 합니다"))
