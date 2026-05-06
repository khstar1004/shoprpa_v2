from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

MSG_EMPTY_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("메시지비어 있습니다") + ": {}")
HTTP_DOWNLOAD_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("HTTP다운로드실패, 오류: {}"))

FTP_CONNECTION_FORMAT: ErrorCode = ErrorCode(
    BizCode.LocalErr, _("연결까지FTP서비스서버실패, 확인하세요서비스서버주소: {}단말: {}")
)
FTP_LOGIN_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("까지FTP실패, 확인하세요사용자명: {}및비밀번호: {}"))
FTP_CLOSE_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("FTP연결닫기실패, 정보: {}"))
FTP_STATUS_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("연결출력오류, 확인하세요FTP여부완료열기연결: {}"))
FTP_DELETE_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("FTP파일삭제실패: {}"))
FTP_RENAME_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("FTP파일/폴더이름 변경실패: {}"))
FTP_CREATE_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("FTP디렉터리생성실패: {},확인하세요FTP연결"))
FTP_UPLOAD_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("{}업로드실패, 확인하세요FTP연결"))
FTP_DOWNLOAD_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("{}다운로드실패, 확인하세요FTP연결"))

FILE_EXIST_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("파일: {}찾을 수 없습니다또는형식오류, 확인하세요파일 경로정보"))
FOLDER_EXIST_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("폴더: {}찾을 수 없습니다, 확인하세요폴더 경로정보"))

FILE_NAME_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("입력파일이름: {}이름실패, 확인하세요입력내용"))
FOLDER_NAME_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("입력폴더이름: {}있음오류, 확인하세요입력내용"))