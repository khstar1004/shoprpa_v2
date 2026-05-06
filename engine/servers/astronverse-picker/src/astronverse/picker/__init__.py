import json
from abc import ABC, abstractmethod
from dataclasses import dataclass
from enum import Enum
from typing import Any, Optional

from pydantic import BaseModel


class Point:
    """"""

    def __init__(self, x, y):
        self.x = x
        self.y = y


ZERO_POINT = Point(0, 0)


class Rect:
    """위치정보"""

    def __init__(self, left: int = 0, top: int = 0, right: int = 0, bottom: int = 0):
        self.left = int(left)
        self.top = int(top)
        self.right = int(right)
        self.bottom = int(bottom)
        self.__area = 0

    def width(self) -> int:
        return self.right - self.left

    def height(self) -> int:
        return self.bottom - self.top

    def area(self) -> int:
        if not self.__area:
            self.__area = self.width() * self.height()
        self.__area = max(self.__area, 0)
        return self.__area

    @staticmethod
    def calculate_area(left: int = 0, top: int = 0, right: int = 0, bottom: int = 0):
        area = (right - left) * (bottom - top)
        if area < 0:
            return 0
        return area

    def contains(self, point: Point) -> bool:
        """
        여부패키지point
        """
        return self.left <= point.x < self.right and self.top <= point.y < self.bottom

    @staticmethod
    def check_point_containment(left, top, right, bottom, point: Point):
        return left <= point.x < right and top <= point.y < bottom

    def contains_rect(self, rect: Any) -> bool:
        """
        여부패키지rect
        """
        return self.left < rect.left and self.top < rect.top and self.right >= rect.right and self.bottom >= rect.bottom

    def to_json(self):
        return json.dumps(
            {
                "left": self.left,
                "top": self.top,
                "right": self.right,
                "bottom": self.bottom,
            }
        )

    def __eq__(self, rect):
        return (
            self.left == rect.left and self.top == rect.top and self.right == rect.right and self.bottom == rect.bottom
        )


@dataclass
class DrawResult:
    """시스템일의선택제어결과"""

    success: bool
    rect: Optional[Rect] = None
    app: Optional[str] = None
    error_message: Optional[str] = None
    domain: Optional[str] = None

    def to_dict(self):
        """변환로딕셔너리형식"""
        result = {"success": self.success}
        if self.rect:
            result["rect"] = self.rect
        if self.app:
            result["app"] = self.app
        if self.error_message:
            result["error_message"] = self.error_message
        if self.domain:
            result["domain"] = self.domain
        return result


class OperationResultStatus(Enum):
    """서비스결과상태"""

    SUCCESS = "success"
    ERROR = "error"
    CANCEL = "cancel"


class OperationResult(BaseModel):
    """시스템일의결과유형"""

    status: OperationResultStatus
    data: Optional[Any] = None
    message: Optional[str] = None

    @classmethod
    def success(cls, data: Any = None, message: str = None):
        """생성성공"""
        return cls(status=OperationResultStatus.SUCCESS, data=data, message=message)

    @classmethod
    def error(cls, message: str, data: Any = None):
        """생성오류 """
        return cls(status=OperationResultStatus.ERROR, message=message, data=data)

    @classmethod
    def cancel(cls, message: str = "완료가져오기 "):
        """생성가져오기 """
        return cls(status=OperationResultStatus.CANCEL, message=message)

    def to_dict(self):
        """변환로딕셔너리형식, 내용 있음의_send_response데이터"""
        result = {}

        if self.status == OperationResultStatus.SUCCESS:
            result["success"] = True
            if self.data is not None:
                result["data"] = self.data
        elif self.status == OperationResultStatus.CANCEL:
            result["success"] = False
            result["cancel"] = True
        else:  # ERROR
            result["success"] = False
            result["error"] = self.message or "지원하지 않는오류"

        return result


class PickerType(Enum):
    """선택유형"""

    ELEMENT = "ELEMENT"  # 요소 선택
    WINDOW = "WINDOW"  # 창선택
    POINT = "POINT"  # 마우스위치선택
    SIMILAR = "SIMILAR"  # 요소
    BATCH = "BATCH"  # 가져오기


PICKER_TYPE_DICT = {p.value: True for p in PickerType}


class SVCSign(Enum):
    """지정시작방법"""

    PICKER = "PICKER"
    SMARTCOMPONENT = "SMARTCOMPONENT"


class MKSign(Enum):
    """지정시작방법"""

    PICKER = "PICKER"
    RECORD = "RECORD"  # 기록제어기기사용


class PickerSign(Enum):
    """지정사용자선택입력의메시지유형"""

    START = "START"
    STOP = "STOP"  # EXIT
    VALIDATE = "VALIDATE"
    DESIGNATE = "DESIGNATE"  # CV의사용
    GAIN = "GAIN"  # 가져오기선택결과
    HIGHLIGHT = "HIGHLIGHT"  # 높이,및검증

    RECORD = "RECORD"  # 기록제어기기사용

    SMART_COMPONENT = "SMART_COMPONENT"


class RecordAction(Enum):
    """기록제어 - 문관리기록제어닫기의"""

    LISTENING = "RECORD_LISTENING"  # 열기 
    START = "RECORD_START"  # 열기 기록제어
    PAUSE = "RECORD_PAUSE"  # 일시중지기록제어
    HOVER_START = "RECORD_AUTOMIC_HOVER_START"  # 열기 선택기존가능
    HOVER_END = "RECORD_AUTOMIC_HOVER_END"  # 기존선택 가능
    AUTOMIC_END = "RECORD_AUTOMIC_END"  # 기존선택 가능결과
    END = "RECORD_END"  # 결과기록제어


class SmartComponentAction(Enum):
    """가능컴포넌트 - 문관리가능컴포넌트선택닫기의"""

    START = "SMART_COMPONENT_START"  # 열기 선택
    PREVIOUS = "SMART_COMPONENT_PREVIOUS"  # 가져오기선택요소의유형요소
    NEXT = "SMART_COMPONENT_NEXT"  # 가져오기선택요소의유형요소
    CANCEL = "SMART_COMPONENT_CANCEL"  # 가져오기 선택
    END = "SMART_COMPONENT_END"  # 선택완료결과


class PickerDomain(Enum):
    """선택기기유형"""

    UIA = "uia"
    WEB = "web"
    WEB_IE = "web_ie"  # 선택의시 web의type예web, 아니오예web_ie
    JAB = "jab"
    SAP = "SAP"
    MSAA = "msaa"

    AUTO = "auto"  # 선택의시 없음
    AUTO_DESK = "auto_desk"  # 
    AUTO_WEB = "auto_web"  # 


class IEventCore(ABC):
    """사용자키보드마우스파일"""

    @abstractmethod
    def is_cancel(self):
        """여부예출력"""

    @abstractmethod
    def is_focus(self):
        """여부예focus"""

    @abstractmethod
    def start(self) -> bool:
        """열기시작"""

    @abstractmethod
    def close(self):
        """닫기 """

    @abstractmethod
    def is_f4_pressed(self):
        """조회F4여부아래"""

    @abstractmethod
    def reset_f4_flag(self):
        """재F4로그위치"""

    @abstractmethod
    def reset_cancel_flag(self):
        """재ESC로그위치"""


class IPickerCore(ABC):
    """선택"""

    @abstractmethod
    def draw(self, svc, highlight_client, data: dict) -> DrawResult:
        """이미지"""

    @abstractmethod
    def element(self, svc, data: dict) -> dict:
        """가져오기요소"""


class IElement(ABC):
    @abstractmethod
    def rect(self) -> Rect:
        """"""

    @abstractmethod
    def tag(self) -> str:
        """의태그"""

    @abstractmethod
    def path(self, svc=None, strategy_svc=None) -> dict:
        """변환로path"""


class APP(Enum):
    """
    일반사용의app
    """

    Chrome = "chrome"
    Edge = "edge"
    IE = "iexplore"
    Firefox = "firefox"
    Chrome360X = "360ChromeX"
    Chrome360se = "360se"
    Chrome360 = "360Chrome"
    SAP = "saplogon"
    Thunder = "Thunder"
    Unknown = "Unknown"
    Chromium = "chromium"

    @classmethod
    def init(cls, name: str):
        try:
            if name == "msedge":
                return APP.Edge
            return cls(name)
        except ValueError:
            return APP.Unknown


BROWSER_UIA_POINT_CLASS = {
    APP.Chrome.value: ("Chrome_RenderWidgetHostHWND", "ClassName"),
    APP.Edge.value: ("Chrome_RenderWidgetHostHWND", "ClassName"),
    APP.Chrome360se.value: ("Chrome_RenderWidgetHostHWND", "ClassName"),
    APP.Chrome360X.value: ("Chrome_RenderWidgetHostHWND", "ClassName"),
    APP.Firefox.value: ("tabbrowser-tabpanels", "AutomationId"),
    APP.IE.value: ("Internet Explorer_Server", "ClassName"),
    APP.Chromium.value: ("Chrome_RenderWidgetHostHWND", "ClassName"),
}


CHROME_LIKE_BROWSER_TYPES = [
    APP.Chrome.value,
    APP.Edge.value,
    APP.Chrome360se.value,
    APP.Chrome360X.value,
    APP.Firefox.value,
    APP.Chromium.value,
]

MSAA_APPLICATIONS = [APP.Thunder.value]

RECORDING_BLACKLIST = ["shoprpa"]  # 기록제어이름단일