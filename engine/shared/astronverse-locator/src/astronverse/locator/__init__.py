"""
RPA위치 지정기기모듈 - UI요소위치 지정공가능

본모듈패키지완료사용RPA의위치 지정기기유형및도구데이터, 
지원UIA, Web, MSAA대기다중의요소위치 지정.
"""

import json
from abc import ABC, abstractmethod
from enum import Enum
from typing import Any, Optional

class Point:
    """테이블이의유형"""

    def __init__(self, x_coordinate, y_coordinate):
        """

        Args:
            x_coordinate: X값
            y_coordinate: Y값
        """
        self.x_coordinate = x_coordinate
        self.y_coordinate = y_coordinate

    @property
    def x(self):
        """X속성"""
        return self.x_coordinate

    @property
    def y(self):
        """Y속성"""
        return self.y_coordinate


ZERO_POINT = Point(0, 0)


class Rect:
    """테이블의유형"""

    def __init__(self, left: int = 0, top: int = 0, right: int = 0, bottom: int = 0):
        self.left = int(left)
        self.top = int(top)
        self.right = int(right)
        self.bottom = int(bottom)
        self.__area = 0

    def width(self) -> int:
        """계획너비정도"""
        return self.right - self.left

    def height(self) -> int:
        """계획높이정도"""
        return self.bottom - self.top

    def area(self) -> int:
        """계획"""
        if not self.__area:
            self.__area = self.width() * self.height()
        self.__area = max(self.__area, 0)
        return self.__area

    @staticmethod
    def calculate_area(left: int = 0, top: int = 0, right: int = 0, bottom: int = 0):
        """계획지정의"""
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
    def check_point_contains(left, top, right, bottom, point: Point):
        """조회여부에서지정내부"""
        return left <= point.x < right and top <= point.y < bottom

    def contains_rect(self, rect: Any) -> bool:
        """
        여부패키지rect
        """
        return self.left < rect.left and self.top < rect.top and self.right >= rect.right and self.bottom >= rect.bottom

    def overlaps(self, other: Any) -> bool:
        """
        여부및other재
        """
        return not (
            self.right <= other.left or self.left >= other.right or self.bottom <= other.top or self.top >= other.bottom
        )

    def to_json(self):
        """를정보변환로JSON문자열"""
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


EMPTY_RECT = Rect(0, 0, 0, 0)


class PickerDomain(Enum):
    """선택기기유형"""

    UIA = "uia"
    WEB = "web"
    WEB_IE = "web_ie"  # 선택의시 web의type예web, 아니요예web_ie
    JAB = "jab"
    SAP = "SAP"
    MSAA = "msaa"
    AUTO = "auto"  # 선택의시 없음


class PickerType(Enum):
    """선택유형"""

    ELEMENT = "ELEMENT"  # 요소 선택
    WINDOW = "WINDOW"  # 창선택
    POINT = "POINT"  # 마우스위치선택
    SIMILAR = "SIMILAR"  # 요소
    BATCH = "BATCH"  # 가져오기


PICKER_TYPE_DICT = {p.value: True for p in PickerType}


class ILocator(ABC):
    """위치 지정기기연결유형"""

    @abstractmethod
    def rect(self) -> Optional[Rect]:
        """가져오기위치"""

    @abstractmethod
    def control(self) -> Any:
        """가져오기 설치유형"""

    def point(self) -> Point:
        """공유방법법"""
        rect = self.rect()
        center_x = rect.left + rect.width() // 2
        center_y = rect.top + rect.height() // 2
        return Point(center_x, center_y)

    def move(self, point: Point = None, duration=0.4):
        """공유방법법"""
        from astronverse.locator.utils.move import smooth_move

        if point is None:
            point = self.point()
        center_x = point.x
        center_y = point.y
        smooth_move(center_x, center_y, duration=duration)

    def hover(self, point: Point = None):
        """공유방법법"""
        return self.move(point)


class BrowserType(Enum):
    """브라우저유형"""

    CHROME = "chrome"
    EDGE = "edge"
    INTERNET_EXPLORER = "iexplore"
    CHROME_360_SE = "360se"
    CHROME_360_X = "360ChromeX"
    FIREFOX = "firefox"
    CHROMIUM = "chromium"


LIKE_CHROME_BROWSER_TYPES = [
    BrowserType.CHROME.value,
    BrowserType.EDGE.value,
    BrowserType.CHROME_360_SE.value,
    BrowserType.CHROME_360_X.value,
    BrowserType.FIREFOX.value,
    BrowserType.CHROMIUM.value,
]


def smooth_move(*args, **kwargs):
    from astronverse.locator.utils.move import smooth_move as _smooth_move

    return _smooth_move(*args, **kwargs)


BROWSER_UIA_WINDOW_CLASS = {
    BrowserType.CHROME.value: (
        "Chrome_WidgetWin_1",
        ["Chrome Legacy Window", "- Google Chrome", " - Chrome"],
        "in",
    ),
    BrowserType.EDGE.value: ("Chrome_WidgetWin_1", ["edge"], "last_in"),
    BrowserType.CHROME_360_SE.value: ("360se6_Frame", ["- 360설치전체브라우저"], "in"),
    BrowserType.CHROME_360_X.value: ("Chrome_WidgetWin_1", ["- 360브라우저X"], "in"),
    BrowserType.FIREFOX.value: ("MozillaWindowClass", ["Firefox"], "in"),
    BrowserType.INTERNET_EXPLORER.value: ("IEFrame", None, "ClassName"),
    BrowserType.CHROMIUM.value: ("Chrome_WidgetWin_1", ["- Chromium"], "in"),
}

BROWSER_UIA_POINT_CLASS = {
    BrowserType.CHROME.value: ("Chrome_RenderWidgetHostHWND", "ClassName"),
    BrowserType.EDGE.value: ("Chrome_RenderWidgetHostHWND", "ClassName"),
    BrowserType.CHROME_360_SE.value: ("Chrome_RenderWidgetHostHWND", "ClassName"),
    BrowserType.CHROME_360_X.value: ("Chrome_RenderWidgetHostHWND", "ClassName"),
    BrowserType.FIREFOX.value: ("tabbrowser-tabpanels", "AutomationId"),
    BrowserType.INTERNET_EXPLORER.value: ("Internet Explorer_Server", "ClassName"),
    BrowserType.CHROMIUM.value: ("Chrome_RenderWidgetHostHWND", "ClassName"),
}
