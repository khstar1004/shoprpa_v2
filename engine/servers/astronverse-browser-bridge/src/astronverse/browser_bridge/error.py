from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

CODE_OK: ErrorCode = ErrorCode(BizCode.LocalOK, "ok", 200)
CODE_INNER: ErrorCode = ErrorCode(BizCode.LocalErr, _("내부 오류"), 200)
ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("오류") + ": {}", 200)
PARAMETER_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("매개변수오류") + ": {}", 200)