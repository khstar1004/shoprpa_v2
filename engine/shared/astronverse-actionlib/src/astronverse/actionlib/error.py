from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException


IGNORE_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("{}"))
TYPE_KIND_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("유형오류: {}"))

CONFIG_LOAD_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("매칭파일로드출력오류: {}"))
CONFIG_TYPE_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("매칭파일파싱지원하지 않음해당유형: {}"))

REQUIRED_PARAM_MISSING: ErrorCode = ErrorCode(BizCode.LocalErr, _("적음매개변수: {}"))
PARAM_ARGS_NO_SUPPORT_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("매개변수지원하지 않음args: {}"))

PARAM_REQUIRED_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("매개변수 {} "))
PARAM_VALUE_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("매개변수 {} 의값오류{}"))
PARAM_TYPE_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("{} 의값유형오류{}"))
PARAM_CONVERT_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("매개변수 {} 의값변환성공 {} 실패, 기존값: {}"))
PARAM_VERIFY_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("매개변수 {} 검증인증실패: {}"))
VALUE_IS_EMPTY: ErrorCode = ErrorCode(BizCode.LocalErr, _("{} 매개변수값비워 둘 수 없습니다"))

ReportStartMsgFormat = _("{} 실행{} [{}]")
ReportCodeError = _("실행오류")
ReportCodeSkip = _("실행오류건너뛰기")
ReportCodeRetry = _("실행오류재시도")


class IgnoreException(BaseException):
    """내부모듈완료관리완료오류, 외부모듈가능오류 의오류"""

    pass


class ParamException(BaseException):
    """매개변수오류, 금액외부출력오류의매개변수이름"""

    pass