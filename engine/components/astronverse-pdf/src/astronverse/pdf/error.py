from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

FILE_PATH_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("PDF 파일 경로가 올바르지 않습니다") + ": {}")
PDF_READ_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("PDF파일가져오기실패, 확인하세요파일여부!") + ": {}")
PDF_PASSWORD_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("PDF파일가져오기실패, 확인하세요비밀번호여부정상!") + ": {}")
