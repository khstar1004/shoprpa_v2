"""
닫기의유형지정.
"""

from enum import Enum


class MessageType(Enum):
    """메시지유형."""

    MESSAGE = "message"
    WARNING = "warning"
    QUESTION = "question"
    ERROR = "error"


class ButtonType(Enum):
    """버튼유형."""

    CONFIRM = "confirm"
    CONFIRM_CANCEL = "confirm_cancel"
    YES_NO = "yes_no"
    YES_NO_CANCEL = "yes_no_cancel"


class InputType(Enum):
    """입력유형."""

    TEXT = "text"
    PASSWORD = "password"


class SelectType(Enum):
    """선택유형."""

    SINGLE = "single"
    MULTI = "multi"


class TimeType(Enum):
    """시간유형."""

    TIME = "time"
    TIME_RANGE = "time_range"


class TimeFormat(Enum):
    """시간형식."""

    YEAR_MONTH_DAY = "YYYY-MM-DD"
    YEAR_MONTH_DAY_HOUR_MINUTE = "YYYY-MM-DD HH:mm"
    YEAR_MONTH_DAY_HOUR_MINUTE_SECOND = "YYYY-MM-DD HH:mm:ss"
    YEAR_MONTH_DAY_SLASH = "YYYY/MM/DD"
    YEAR_MONTH_DAY_HOUR_MINUTE_SLASH = "YYYY/MM/DD HH:mm"
    YEAR_MONTH_DAY_HOUR_MINUTE_SECOND_SLASH = "YYYY/MM/DD HH:mm:ss"


class OpenType(Enum):
    """열기유형."""

    FILE = "file"
    FOLDER = "folder"


class FileType(Enum):
    """파일유형."""

    ALL = "*"
    EXCEL = ".xls,.xlsx"
    WORD = ".doc,.docx"
    TXT = ".txt"
    IMG = ".png,.jpg,.jpeg,.bmp,.gif"
    PPT = ".ppt,.pptx"
    RAR = ".zip,.rar"


class DefaultButtonC(Enum):
    """버튼."""

    CONFIRM = "confirm"


class DefaultButtonCN(Enum):
    """및가져오기 버튼."""

    CONFIRM = "confirm"
    CANCEL = "cancel"


class DefaultButtonY(Enum):
    """예및아니오버튼."""

    YES = "yes"
    NO = "no"


class DefaultButtonYN(Enum):
    """예, 아니오및가져오기 버튼."""

    YES = "yes"
    NO = "no"
    CANCEL = "cancel"