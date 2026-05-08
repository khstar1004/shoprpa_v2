from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

MSG_EMPTY_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("메시지가 비어 있습니다") + ": {}")
HTTP_DOWNLOAD_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("HTTP 다운로드 실패, 오류: {}"))

FTP_CONNECTION_FORMAT: ErrorCode = ErrorCode(
    BizCode.LocalErr, _("FTP 서버 연결에 실패했습니다. 서버 주소와 포트를 확인하세요: {}:{}")
)
FTP_LOGIN_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("FTP 로그인에 실패했습니다. 사용자명과 비밀번호를 확인하세요: {}, {}"))
FTP_CLOSE_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("FTP 연결 종료에 실패했습니다. 정보: {}"))
FTP_STATUS_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("FTP 연결 오류입니다. 연결 상태를 확인하세요: {}"))
FTP_DELETE_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("FTP 파일 삭제 실패: {}"))
FTP_RENAME_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("FTP 파일/폴더 이름 변경 실패: {}"))
FTP_CREATE_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("FTP 디렉터리 생성 실패: {}. FTP 연결을 확인하세요"))
FTP_UPLOAD_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("{} 업로드 실패. FTP 연결을 확인하세요"))
FTP_DOWNLOAD_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("{} 다운로드 실패. FTP 연결을 확인하세요"))

FILE_EXIST_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("파일을 찾을 수 없거나 형식이 올바르지 않습니다: {}"))
FOLDER_EXIST_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("폴더를 찾을 수 없습니다: {}"))

FILE_NAME_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("파일 이름이 올바르지 않습니다: {}"))
FOLDER_NAME_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("폴더 이름이 올바르지 않습니다: {}"))
