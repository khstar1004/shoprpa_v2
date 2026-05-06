from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("오류") + ": {}")
CODE_INNER: ErrorCode = ErrorCode(BizCode.LocalErr, _("내부 오류"))
PARAM_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("{} 매개변수예외"))
TIMEOUT: ErrorCode = ErrorCode(BizCode.LocalErr, "선택시간 초과")
TIMEOUT_LAG: ErrorCode = ErrorCode(BizCode.LocalErr, "선택초과경과15s, 요청 출력기기후다시 이동")
NO_WEB_INFO: ErrorCode = ErrorCode(BizCode.LocalErr, "요소의web정보")

BROWSER_EXTENSION_INSTALL_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("브라우저 확장통신출력오류, 다시 시도하세요"))

BROWSER_EXTENSION_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("브라우저 확장오류") + ": {}")

WEB_GET_ElE_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("웹 페이지원조회실패") + " {}")

WEB_EXEC_ElE_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("본실행오류") + ": {}")

BROWSER_EXTENSION_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("브라우저 확장오류") + ": {}")