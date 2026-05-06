from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

FILE_PATH_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("PDF파일 경로있음오류, 입력하세요정상의경로!") + ": {}")
PDF_READ_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("PDF파일가져오기실패, 확인하세요파일여부!") + ": {}")
PDF_PASSWORD_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("PDF파일가져오기실패, 확인하세요비밀번호여부정상!") + ": {}")