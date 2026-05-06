"""본컴포넌트암호화닫기오류및오류코드지정.

직선연결덮어쓰기내부 `BaseException`, 사용이름가져오기.
"""

from astronverse.baseline.error.error import BaseException as CoreBaseException
from astronverse.baseline.error.error import BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = CoreBaseException

__all__ = [
    "MSG_EMPTY_FORMAT",
    "BizCode",
    "CoreBaseException",
    "ErrorCode",
]

MSG_EMPTY_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("메시지비어 있습니다") + ": {}")