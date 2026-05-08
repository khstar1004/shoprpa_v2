from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

INVALID_APP_PATH_ERROR_CODE: ErrorCode = ErrorCode(BizCode.LocalErr, _("프로그램 경로가 올바르지 않습니다") + ": {}")
