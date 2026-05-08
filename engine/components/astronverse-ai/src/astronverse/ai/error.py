"""AI module errors and error-code definitions."""

from astronverse.baseline.error.error import (
    BaseException as BaselineBaseException,
)
from astronverse.baseline.error.error import (
    BizCode,
    ErrorCode,
)
from astronverse.baseline.i18n.i18n import _


class AIBaseError(BaselineBaseException):
    """AI module exception."""


LLM_NO_RESPONSE_ERROR: ErrorCode = ErrorCode(
    BizCode.LocalErr, _("LLM이 응답을 반환하지 않았습니다. 다시 시도하세요") + ": {}"
)

# Export baseline exception type for external module compatibility.
BaseException = BaselineBaseException  # type: ignore
