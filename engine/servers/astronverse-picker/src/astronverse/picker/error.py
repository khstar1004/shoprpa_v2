from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("오류") + ": {}")
CODE_INNER: ErrorCode = ErrorCode(BizCode.LocalErr, _("내부 오류"))
PARAM_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("{} 매개변수가 올바르지 않습니다"))
TIMEOUT: ErrorCode = ErrorCode(BizCode.LocalErr, "선택 시간이 초과되었습니다")
TIMEOUT_LAG: ErrorCode = ErrorCode(BizCode.LocalErr, "선택 대기가 15초를 초과했습니다. 선택 창을 다시 열고 시도하세요")
NO_WEB_INFO: ErrorCode = ErrorCode(BizCode.LocalErr, "요소의 웹 정보가 없습니다")

BROWSER_EXTENSION_INSTALL_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("브라우저 확장 통신 오류입니다. 다시 시도하세요"))

BROWSER_EXTENSION_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("브라우저 확장 오류") + ": {}")

WEB_GET_ElE_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("웹 페이지 요소 조회 실패") + ": {}")

WEB_EXEC_ElE_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("스크립트 실행 오류") + ": {}")

BROWSER_EXTENSION_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("브라우저 확장 오류") + ": {}")
