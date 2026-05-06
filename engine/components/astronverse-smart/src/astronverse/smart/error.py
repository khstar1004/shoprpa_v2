from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

# 모듈가져오기 닫기오류
MODULE_IMPORT_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("불가가져오기모듈: {}"))
MODULE_MAIN_FUNCTION_NOT_FOUND: ErrorCode = ErrorCode(BizCode.LocalErr, _("모듈 {} 지정되지 않았습니다가능호출의 main 데이터"))