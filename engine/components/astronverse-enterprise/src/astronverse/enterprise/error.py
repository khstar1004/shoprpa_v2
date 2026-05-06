"Enterprise error definitions"

from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException


SERVER_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("서버 오류") + ": {}")
PATH_INVALID_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("업로드경로있음오류") + ": {}")
FILE_UPLOAD_FAILED_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("업로드실패") + ": {}")
FILE_DOWNLOAD_FAILED_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("다운로드실패") + ": {}")