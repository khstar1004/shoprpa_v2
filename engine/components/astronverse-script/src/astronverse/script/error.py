from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

CODE_EMPTY: ErrorCode = ErrorCode(BizCode.LocalErr, _("본데이터비어 있습니다"))
SERVER_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("서버 오류") + ": {}")

# 모듈가져오기 닫기오류
MODULE_IMPORT_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("불가가져오기모듈: {}"))
MODULE_MAIN_FUNCTION_NOT_FOUND: ErrorCode = ErrorCode(BizCode.LocalErr, _("모듈 {} 지정되지 않았습니다가능호출의 main 데이터"))

MSG_MODULE_VERSION_WARRING = _("현재본로버전, 생성업데이트새버전본")