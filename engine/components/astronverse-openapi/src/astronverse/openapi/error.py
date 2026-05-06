from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

MSG_EMPTY_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("메시지비어 있습니다") + ": {}")
AI_SERVER_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("ai서비스서버없음또는오류"))
AI_REQ_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("ai서비스요청 예외 {}"))
IMAGE_EMPTY: ErrorCode = ErrorCode(BizCode.LocalErr, _("이미지경로찾을 수 없습니다또는형식오류"))