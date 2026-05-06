"""AI 닫기오류및오류코드지정."""

from astronverse.baseline.error.error import (
    BaseException as BaselineBaseException,
)
from astronverse.baseline.error.error import (
    BizCode,
    ErrorCode,
)
from astronverse.baseline.i18n.i18n import _


class AIBaseError(BaselineBaseException):
    """AI 모듈지정예외."""


LLM_NO_RESPONSE_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("대유형없음반환결과, 다시 시도하세요") + ": {}")

# 보관내용: 외부모듈가능사용 BaseException, 내보내기 Baseline 유형
BaseException = BaselineBaseException  # type: ignore