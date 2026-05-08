from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

KEYBOARD_MSG_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("키보드 입력 내용이 비어 있습니다."))
DRIVE_ERROR: ErrorCode = ErrorCode(
    BizCode.LocalErr,
    _(
        "드라이버 {} 설치가 완료되지 않았습니다. 드라이버 설치 후 다시 시작하세요.\n드라이버가 이미 설치되어 있다면 확장 프로그램을 설치하고 다시 시작하세요."
    ),
)
DRIVE_INPUT_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("드라이버 입력에 실패했습니다. 관리자 권한을 확인하세요."))
KEY_INPUT_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("키보드 입력에 실패했습니다. 입력 내용을 확인하세요."))
SCROLL_FAILURE: ErrorCode = ErrorCode(BizCode.LocalErr, _("작업 중 실패했습니다. 오류 여부를 확인하세요"))
CLIP_PASTE_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("클립보드에서 입력 정보를 가져올 수 없습니다. 다시 입력하세요."))
GHOST_DRIVE_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("드라이버를 찾을 수 없거나 연결되지 않았습니다. 드라이버 연결을 확인하세요"))
REGION_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("좌표가 화면 범위를 벗어났습니다. 입력 좌표를 확인하세요."))
