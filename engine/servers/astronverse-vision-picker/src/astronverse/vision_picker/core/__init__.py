from enum import Enum


class PickerType(Enum):
    ELEMENT = "ELEMENT"  # 요소 선택
    WINDOW = "WINDOW"  # 창선택
    POINT = "POINT"  # 마우스위치선택
    SIMILAR = "SIMILAR"
    CV = "CV"
    OTHERS = "OTHERS"


class Status(Enum):
    INIT = "INIT"
    START = "START"
    WAIT_SIGNAL = "WAIT_SIGNAL"
    CV_CTRL = "CV_CTRL"
    CV_ALT = "CV_ALT"
    CV_SHIFT = "CV_SHIFT"
    CV_WAIT_TARGET = "CV_Wait_Target"
    CONFIRM = "CONFIRM"
    SEND_WINDOW = "Send_Window"
    SEND_TARGET = "Send_Target"
    CANCEL = "CANCEL"
    OVER = "OVER"
    TIMEOUT = "TIMEOUT"


class PickType(Enum):
    TARGET = "TARGET"
    ANCHOR = "ANCHOR"