from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

KEYBOARD_MSG_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("키보드입력비어 있습니다, 확인하세요입력내용"))
DRIVE_ERROR: ErrorCode = ErrorCode(
    BizCode.LocalErr,
    _(
        "드라이버:{}미완료설치성공, 설치경과드라이버, 요청재시작드라이버;\n미완료설치드라이버, 요청전확장설치설치드라이버, 재시작, 드라이버!"
    ),
)
DRIVE_INPUT_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("드라이버입력있음관리자 권한, 입력실패"))
KEY_INPUT_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("키보드입력실패, 확인하세요입력내용"))
SCROLL_FAILURE: ErrorCode = ErrorCode(BizCode.LocalErr, _("경과중실패, 확인하세요여부출력예외"))
CLIP_PASTE_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("잘라내기중가져올 수 없는 있음입력정보, 요청다시 입력!"))
GHOST_DRIVE_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("드라이버를 찾을 수 없거나 연결되지 않았습니다. 드라이버 연결을 확인하세요"))
REGION_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("매개변수아니오합치기법, 확인하세요지정매개변수 여부소0또는초과출력화면가장자리"))