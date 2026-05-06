from enum import Enum


class CommonForBrowserType(Enum):
    """브라우저유형"""

    BTChrome = "chrome"
    BTEdge = "edge"
    BT360SE = "360se"
    BT360X = "360ChromeX"
    BTFirefox = "firefox"
    BTChromium = "chromium"


ALL_BROWSER_TYPES = [
    CommonForBrowserType.BTChrome,
    CommonForBrowserType.BTEdge,
    CommonForBrowserType.BT360X,
    CommonForBrowserType.BT360SE,
    CommonForBrowserType.BTFirefox,
    CommonForBrowserType.BTChromium,
]

# 프로그램이름정상이면매칭
BROWSER_SOFTWARE_TAG = {
    CommonForBrowserType.BTChrome.value: "chrome",
    CommonForBrowserType.BTEdge.value: "msedge",
    CommonForBrowserType.BT360SE.value: "360se6",
    CommonForBrowserType.BT360X.value: "360ChromeX",
    CommonForBrowserType.BTFirefox.value: "firefox",
    CommonForBrowserType.BTChromium.value: "chromium",
}

# 회원가입테이블이름
BROWSER_REGISTER_NAME = {
    CommonForBrowserType.BTChrome.value: "chrome.exe",
    CommonForBrowserType.BTEdge.value: "msedge.exe",
    CommonForBrowserType.BT360SE.value: "360se6.exe",
    CommonForBrowserType.BT360X.value: "360ChromeX.exe",
    CommonForBrowserType.BTFirefox.value: "firefox.exe",
}

# 방식
BROWSER_PRIVATE_MAP = {
    CommonForBrowserType.BTChrome.value: "incognito",
    CommonForBrowserType.BTEdge.value: "inprivate",
}

# window:uia창의class_name
BROWSER_UIA_WINDOW_CLASS = {
    CommonForBrowserType.BTChrome.value: (
        "Chrome_WidgetWin_1",
        ["Chrome Legacy Window", "- Google Chrome", " - Chrome"],
        "in",
    ),
    CommonForBrowserType.BTEdge.value: ("Chrome_WidgetWin_1", ["edge"], "last_in"),
    CommonForBrowserType.BT360SE.value: ("360se6_Frame", ["- 360설치전체브라우저"], "in"),
    CommonForBrowserType.BT360X.value: ("Chrome_WidgetWin_1", ["- 360브라우저X"], "in"),
    CommonForBrowserType.BTFirefox.value: ("MozillaWindowClass", ["Firefox"], "in"),
    CommonForBrowserType.BTChromium.value: ("Chrome_WidgetWin_1", ["- Chromium"], "in"),
}

# window:uia웹 페이지의class_name
BROWSER_UIA_POINT_CLASS = {
    CommonForBrowserType.BTChrome.value: ("Chrome_RenderWidgetHostHWND", "ClassName"),
    CommonForBrowserType.BTEdge.value: ("Chrome_RenderWidgetHostHWND", "ClassName"),
    CommonForBrowserType.BT360SE.value: ("Chrome_RenderWidgetHostHWND", "ClassName"),
    CommonForBrowserType.BT360X.value: ("Chrome_RenderWidgetHostHWND", "ClassName"),
    CommonForBrowserType.BTFirefox.value: ("tabbrowser-tabpanels", "AutomationId"),
    CommonForBrowserType.BTChromium.value: ("Chrome_RenderWidgetHostHWND", "ClassName"),
}


# ------------브라우저지정결과--------------


class Element:
    def __init__(self, **kwargs):
        self.kwargs = kwargs


class CommonForTimeoutHandleType(Enum):
    """시간 초과관리유형"""

    ExecError = "execError"
    StopLoad = "stopLoad"


class WaitElementForStatusFlag(Enum):
    """대기요소상태로그"""

    ElementExists = "y"
    ElementDisappears = "n"


class ButtonForClickTypeFlag(Enum):
    """버튼클릭유형로그"""

    Left = "click"
    Double = "dbclick"
    Right = "right"


class ButtonForAssistiveKeyFlag(Enum):
    """로그"""

    Nothing = "None"
    Alt = "Alt"
    Ctrl = "Ctrl"
    Shift = "Shift"
    Win = "Win"


class FillInputForFillTypeFlag(Enum):
    """입력유형로그"""

    Text = "text"
    Clipboard = "clipboard"
    Credential = "credential"


class ElementAttributeOpTypeFlag(Enum):
    """요소속성유형로그"""

    Get = "get"
    Set = "set"
    Del = "del"


class ElementDragDirectionTypeFlag(Enum):
    """요소방법유형로그"""

    Left = "left"
    Right = "right"
    Up = "up"
    Down = "down"


class ElementDragTypeFlag(Enum):
    """요소유형로그"""

    Start = "start"
    Current = "current"


class BrowserBizCode(Enum):
    """브라우저서비스코드"""

    OK = "0000"
    ServerErr = "5001"
    ElemErr = "5002"
    ExecErr = "5003"


class ElementGetAttributeTypeFlag(Enum):
    """요소가져오기속성유형로그"""

    GetText = "getText"
    GetHtml = "getHtml"
    GetValue = "getValue"
    GetLink = "getLink"
    GetAttribute = "getAttribute"
    GetPosition = "getPosition"
    GetSelection = "getSelection"
    GetStyle = "getStyle"


class ElementGetAttributeHasSelfTypeFlag(Enum):
    """요소가져오기속성패키지유형로그"""

    GetElement = "getElement"
    GetText = "getText"
    GetHtml = "getHtml"
    GetValue = "getValue"
    GetLink = "getLink"
    GetAttribute = "getAttribute"
    GetPosition = "getPosition"
    GetSelection = "getSelection"
    GetStyle = "getStyle"


class ElementCheckedTypeFlag(Enum):
    """원선택중유형로그"""

    Checked = "checked"
    UnChecked = "unchecked"
    Reversed = "reversed"


class SelectionPartner(Enum):
    """선택"""

    Contains = "contains"
    Equal = "equal"
    Index = "index"


class RelativePosition(Enum):
    """위치"""

    ScreenLeft = "screenLeft"
    WebPageLeft = "webPageLeft"


class FillInputForInputTypeFlag(Enum):
    """입력유형로그"""

    Append = "append"
    Overwrite = "overwrite"


class ScrollbarForXScrollTypeFlag(Enum):
    """X유형로그"""

    Left = "left"
    Right = "right"
    Defined = "defined"


class ScrollbarForYScrollTypeFlag(Enum):
    """Y유형로그"""

    Top = "top"
    Bottom = "bottom"
    Defined = "defined"


class ScreenShotForShotRangeFlag(Enum):
    """스크린샷로그"""

    Visual = "visual"
    All = "all"


class DownloadModeForFlag(Enum):
    """다운로드방식로그"""

    Click = "click"
    Link = "link"


class ScrollbarType(Enum):
    """유형"""

    Window = "window"
    CustomEle = "customEle"


class ScrollDirection(Enum):
    """방법"""

    Horizontal = "horizontal"
    Vertical = "vertical"


class WebSwitchType(Enum):
    """웹 페이지유형"""

    URL = "url"
    TITLE = "title"
    TabId = "tabId"


class InputType(Enum):
    """입력유형"""

    Content = "content"
    File = "file"


class TablePickType(Enum):
    """테이블선택유형"""

    Row = "row"
    Column = "column"


class LocateType(Enum):
    """위치 지정유형"""

    Xpath = "xpath"
    CssSelector = "cssSelector"
    Text = "text"


class ElementCreateReturnType(Enum):
    """생성요소객체반환유형"""

    SINGLE = "single"  # 단일개요소객체
    LIST = "list"  # 요소객체목록


class RelativeType(Enum):
    """유형"""

    Child = "child"
    Parent = "parent"
    Sibling = "sibling"


class ChildElementType(Enum):
    """원유형"""

    All = "all"
    Index = "index"
    Xpath = "xpath"
    Last = "last"


class SiblingElementType(Enum):
    """원유형"""

    All = "all"
    Next = "next"
    Prev = "prev"