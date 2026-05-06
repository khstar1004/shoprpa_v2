from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

PARAMETER_INVALID_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("매개변수예외") + ": {}")
WINDOW_NO_FIND: ErrorCode = ErrorCode(BizCode.LocalErr, _("찾을 수 없는 목록 창"))
WINDOW_SCROLL_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("창예외"))
WINDOW_SIZE_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("창 크기를 지정할 수 없습니다"))