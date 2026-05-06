"""email오류코드"""

from astronverse.baseline.error.error import *
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

MSG_EMPTY_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("메시지비어 있습니다") + ": {}")
LOGIN_FAIL_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("로그인실패") + ": {}")