from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

DOCUMENT_PATH_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("Word파일 경로있음오류, 입력하세요정상의경로!") + ": {}")
DOCUMENT_READ_ERROR_FORMAT: ErrorCode = ErrorCode(
    BizCode.LocalErr, _("Word파일가져오기실패, 확인하세요파일여부!") + ": {}"
)
DOCUMENT_NOT_EXIST_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("Word 파일이 열려 있지 않습니다. Word 파일을 먼저 여세요") + ": {}")
WORD_INITIALIZATION_ERROR_FORMAT: ErrorCode = ErrorCode(
    BizCode.LocalErr, _("Word실패, 확인하세요Word여부설치정상!") + ": {}"
)
CONTENT_FORMAT_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("확인하세요입력여부정상!") + ": {}")
CLIPBOARD_PASTE_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("확인하세요여부존재함내용!") + ": {}")
TABLE_NOT_EXIST_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("테이블찾을 수 없습니다") + ": {}")
FILENAME_ALREADY_EXISTS_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("파일이름완료존재함") + ": {}")