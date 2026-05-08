from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

DOCUMENT_PATH_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("Word 파일 경로가 올바르지 않습니다") + ": {}")
DOCUMENT_READ_ERROR_FORMAT: ErrorCode = ErrorCode(
    BizCode.LocalErr, _("Word 파일을 읽지 못했습니다. 파일 상태를 확인하세요") + ": {}"
)
DOCUMENT_NOT_EXIST_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("Word 파일이 열려 있지 않습니다. Word 파일을 먼저 여세요") + ": {}")
WORD_INITIALIZATION_ERROR_FORMAT: ErrorCode = ErrorCode(
    BizCode.LocalErr, _("Word를 초기화하지 못했습니다. Word가 정상 설치되어 있는지 확인하세요") + ": {}"
)
CONTENT_FORMAT_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("입력값 형식이 올바르지 않습니다") + ": {}")
CLIPBOARD_PASTE_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("클립보드에 붙여넣을 수 있는 내용이 없습니다") + ": {}")
TABLE_NOT_EXIST_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("테이블을 찾을 수 없습니다") + ": {}")
FILENAME_ALREADY_EXISTS_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("파일 이름이 이미 존재합니다") + ": {}")
