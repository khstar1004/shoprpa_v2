from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

WINDOW_NO_FIND_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("아니오까지창, 확인하세요창여부완료닫기!") + ": {}")
WINDOW_SCROLL_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("창실패, 확인하세요창여부완료닫기!") + ": {}")
ELEMENT_NO_FOUND: ErrorCode = ErrorCode(BizCode.LocalErr, _("찾을 수 없는 요소"))
PATH_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("선택또는저장경로있음오류"))
UNPICKABLE: ErrorCode = ErrorCode(BizCode.LocalErr, _("선택요소지원하지 않음해당선택유형"))