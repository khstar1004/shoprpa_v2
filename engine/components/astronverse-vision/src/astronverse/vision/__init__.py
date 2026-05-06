from enum import Enum


class ReportLevelType(Enum):
    INFO = "info"
    WARNING = "warning"
    ERROR = "error"


class BtnModel(Enum):  # 방식
    CLICK = "click"
    DOUBLE_CLICK = "double_click"


class BtnType(Enum):  # 유형
    LEFT = "left"
    MIDDLE = "middle"
    RIGHT = "right"


class PositionType(Enum):  # 위치유형
    CENTER = "center"
    RANDOM = "random"
    SPECIFIC = "specific"


class ExistType(Enum):  # 존재함유형
    EXIST = "exist"
    NOT_EXIST = "not_exist"


class WaitType(Enum):  # 대기유형
    APPEAR = "appear"
    DISAPPEAR = "disappear"


class InputType(Enum):  # 입력유형
    TEXT = "text"
    CLIP = "clip"