"""오류코드지정"""

from astronverse.baseline.error.error import *
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

EXECUTABLE_PATH_NOT_FOUND_ERROR: ErrorCode = ErrorCode(
    BizCode.LocalErr, _("지정창실행경로찾을 수 없습니다,확인하세요경로정보") + ": {}"
)