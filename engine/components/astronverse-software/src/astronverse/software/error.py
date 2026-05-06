from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

INVALID_APP_PATH_ERROR_CODE: ErrorCode = ErrorCode(BizCode.LocalErr, _("사용프로그램경로있음오류, 입력하세요정상의경로!") + ": {}")