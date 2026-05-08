"""
오류지정모듈

지정RPA위치 지정기기닫기의오류코드및예외유형.
"""

from astronverse.baseline.error.error import BaseException as RPABaseException
from astronverse.baseline.error.error import BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

# 다시 내보내기예외유형
RPABaseException = RPABaseException
BaseException = RPABaseException

NO_FIND_ELEMENT: ErrorCode = ErrorCode(BizCode.LocalErr, _("요소를 찾을 수 없습니다"))
