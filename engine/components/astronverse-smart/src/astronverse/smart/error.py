from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

# 스마트 컴포넌트 모듈 로딩 오류
MODULE_IMPORT_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("모듈을 가져올 수 없습니다: {}"))
MODULE_MAIN_FUNCTION_NOT_FOUND: ErrorCode = ErrorCode(BizCode.LocalErr, _("모듈 {}에서 호출 가능한 main 함수를 찾을 수 없습니다"))
