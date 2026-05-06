from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

BaseException = BaseException

MSG_EMPTY_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("메시지비어 있습니다") + ": {}")
CV_MATCH_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("일치하지 않는까지목록 요소, 확인하세요현재또는낮음매칭 정확도후재시도"))
SPECIFIC_POSITION_ERROR = ErrorCode(BizCode.LocalErr, _("지정되지 않았습니다클릭위치, 확인하세요매개변수 "))
MOUSE_CLICK_ERROR = ErrorCode(BizCode.LocalErr, _("마우스클릭실패"))
MOUSE_HOVER_ERROR = ErrorCode(BizCode.LocalErr, _("마우스중지실패"))
CV_INPUT_ERROR = ErrorCode(BizCode.LocalErr, _("입력텍스트실패"))
TARGET_EXISTS_ERROR = ErrorCode(BizCode.LocalErr, _("현재목록 요소를 찾을 수 없습니다"))