from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

MSG_EMPTY_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("내용비어 있습니다"))
FILE_PATH_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("파일 경로가 올바르지 않습니다: {}"))
SAVE_TYPE_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("저장형식:{}있음오류, 파일이름필요로{}!"))
FILE_READ_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("파일가져오기실패, 확인하세요파일여부!") + ": {}")
FILE_WRITE_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("파일입력실패, 확인하세요파일여부!") + ": {}")
FILE_TYPE_ERROR_FORMAT: ErrorCode = ErrorCode(
    BizCode.LocalErr, _("파일이름실패, 확인하세요파일이름입력여부정상!") + ": {}"
)
FILE_DELETE_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("파일삭제실패") + ": {}")
PermissionError_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("파일: {}사용, 요청닫기파일후재시도"))
CMD_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("CMD명령:{}실행실패:{}"))

READ_TYPE_ERROR_FORMAT: ErrorCode = ErrorCode(
    BizCode.LocalErr, _("현재파일형식: {}지원하지 않음가져오기, 현재지원{}형식, 확인하세요파일형식")
)
RENAME_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("새이름: {}및기존이름일, 확인하세요이름 변경내용"))
ENCODE_TYPE_ERROR_FORMAT: ErrorCode = ErrorCode(
    BizCode.LocalErr,
    _("현재파일코드형식({})및지정의해제코드유형({})발송, 요청다시 선택코드유형또는으로이제어방식가져오기!"),
)

FOLDER_PATH_ERROR_FORMAT: ErrorCode = ErrorCode(
    BizCode.LocalErr, _("폴더찾을 수 없습니다, 확인하세요폴더 경로여부정상!") + ": {}"
)
CONTENT_TYPE_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("가져오기잘라내기내용유형오류"))
FOLDER_DELETE_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("폴더삭제실패") + ": {}")
SCREENSHOT_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("스크린샷저장실패") + ": {}")
SCREENLOCK_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("화면지정실패") + ": {}")
