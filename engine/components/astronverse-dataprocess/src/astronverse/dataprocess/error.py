"""
오류코드및예외지정.
"""

from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

VALUE_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("입력데이터 유형있음오류, 불가로변수") + ": {}")
INVALID_REGEX_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("입력의정상이면테이블방식있음오류") + ": {}")
INVALID_NUMBER_RANGE_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("입력의숫자있음오류") + ": {}")
INVALID_NUMBER_FORMAT_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("입력의숫자형식있음오류") + ": {}")
INVALID_LIST_FORMAT_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("입력의목록형식있음오류") + ": {}")
INVALID_DICT_FORMAT_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("입력의딕셔너리형식있음오류") + ": {}")
INVALID_INDEX_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("입력의검색값있음오류") + ": {}")
INVALID_MATH_EXPRESSION_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("입력의데이터테이블방식있음오류") + ": {}")