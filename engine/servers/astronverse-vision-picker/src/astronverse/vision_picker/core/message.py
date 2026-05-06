from enum import Enum

from pydantic import BaseModel


class PickerSign(Enum):
    """
    지정사용자선택입력의메시지유형
    """

    START = "START"
    # WIN_PICKER_START = "WIN_PICKER_START"   # 시작창선택
    STOP = "STOP"
    VALIDATE = "VALIDATE"
    DESIGNATE = "DESIGNATE"


class PickerType(Enum):
    ELEMENT = "ELEMENT"  # 요소 선택
    WINDOW = "WINDOW"  # 창선택
    POINT = "POINT"  # 마우스위치선택
    SIMILAR = "SIMILAR"
    OTHERS = "OTHERS"
    CV = "CV"


class PickerInputData(BaseModel):
    """
    지정사용자선택입력의매개변수결과
    """

    pick_sign: PickerSign = PickerSign.START
    pick_type: PickerType = PickerType.ELEMENT
    data: str = None
    ext_data: dict = {}


class PickerResponseItem(Enum):
    """
    지정선택반환의결과유형
    """

    PING = "ping"
    SUCCESS = "success"
    ERROR = "error"
    CANCEL = "cancel"


class PickerResponse(BaseModel):
    """
    지정선택
    """

    err_msg: str
    data: str
    key: PickerResponseItem = PickerResponseItem.SUCCESS