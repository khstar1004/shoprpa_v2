from dataclasses import dataclass
from enum import Enum
from typing import Any, List, Union


class AtomicLevel(Enum):
    """기존가능-대기단계"""

    # 통신
    NORMAL = "normal"
    # 높이단계
    ADVANCED = "advanced"


class AtomicFormType(Enum):
    """기존가능-FormType"""

    # 
    SWITCH = "SWITCH"
    # 안내
    TIP = "TIP"
    # 선택
    SELECT = "SELECT"
    # html5 RADIO
    RADIO = "RADIO"
    # html5 CHECKBOX
    CHECKBOX = "CHECKBOX"
    # 반환값
    RESULT = "RESULT"
    # 입력란
    INPUT = "INPUT"
    # 입력란+변수
    INPUT_VARIABLE = "INPUT_VARIABLE"
    # 입력란+변수+python
    INPUT_VARIABLE_PYTHON = "INPUT_VARIABLE_PYTHON"
    # 입력란+변수+python+시간
    INPUT_VARIABLE_PYTHON_DATETIME = "INPUT_VARIABLE_PYTHON_DATETIME"
    # 입력란+변수+python+파일
    INPUT_VARIABLE_PYTHON_FILE = "INPUT_VARIABLE_PYTHON_FILE"
    # 입력란+변수+색상
    INPUT_VARIABLE_COLOR = "INPUT_VARIABLE_COLOR"
    # 입력란+변수+python+Excel도구
    INPUT_VARIABLE_PYTHON_EXCEL = "INPUT_VARIABLE_PYTHON_EXCEL"
    # 입력란+대입력란
    INPUT_PYTHON_TEXTAREAMODAL_VARIABLE = "INPUT_PYTHON_TEXTAREAMODAL_VARIABLE"
    # 선택도구
    PICK = "PICK"

    # 가져오기 위치도구(사용키보드입력)
    KEYBOARD = "KEYBOARD"
    # 마우스클릭가져오기위치(마우스)
    MOUSEPOSITION = "MOUSEPOSITION"
    # CV구격식선택기기
    GRID = "GRID"
    # 도구 [CV정도매칭]
    SLIDER = "SLIDER"
    # 가져오기  (목록전사용메일)
    CONTENTPASTE = "CONTENTPASTE"
    # 대화상자  지정버튼
    MODALBUTTON = "MODALBUTTON"
    # 날짜시간선택
    DEFAULTDATEPICKER = "DEFAULTDATEPICKER"
    # 날짜시간선택
    RANGEDATEPICKER = "RANGEDATEPICKER"
    # 선택대화상자선택컴포넌트
    OPTIONSLIST = "OPTIONSLIST"
    # 하위 프로세스매개변수
    PROCESS_PARAM = "PROCESS_PARAM"
    # 비밀번호코드파일
    DEFAULTPASSWORD = "DEFAULTPASSWORD"
    # 원선택 + 지정원파일(목록전사용합치기기존가능) params:{"code":1} 1테이블위모듈 2테이블아래모듈 3테이블전체
    FACTOR_ELEMENT = "FACTORELEMENT"
    # js의매개변수
    SCRIPTPARAMS = "SCRIPTPARAMS"
    # 매개변수
    REMOTEPARAMS = "REMOTEPARAMS"
    # 중공유폴더
    REMOTEFOLDERS = "REMOTEFOLDERS"
    # 프로세스매개변수
    PROCESSPARAM = "PROCESSPARAM"
    # ShoprpaAgent파일
    AIWORKFLOW = "AIWORKFLOW"


class AtomicFormTypeParam(Enum):
    PICK = {
        "use": "ELEMENT",  # ["ELEMENT", "WINDOW", "POINT", "CV"] 중의일개
    }
    INPUT_VARIABLE_PYTHON_FILE = {
        "file_type": "file",  # ["file", "files", "folder"] 중의일개
        "filters": [".txt"],  # 에서file_type로file있음 식별자필요.txt의후파일
        "defaultPath": "test.xls",  # 이름 사용 file
    }
    SELECT = {
        "filters": "Process"  # ["Process", "PyModule"] 중의일개
    }
    PROCESS_PARAM = {
        "linkage": "process"  # xxx닫기 매개변수
    }
    PROCESSPARAM = {"linkage": "process"}


@dataclass
class AtomicOption:
    """기존가능-매개변수-Option"""

    label: str
    value: Any

    def tojson(self, filtered_none=True):
        data = self.__dict__
        if filtered_none:
            data = {k: v for k, v in data.items() if v is not None}
        return data


@dataclass
class DynamicsItem:
    key: str = ""
    expression: str = ""  # 테이블방식


@dataclass
class AtomicMeta:
    """기존가능-원데이터"""

    # 저장: 완료결과의아니요관리
    __end__: bool = None
    # 여부있음kwargs
    __has_kwargs__: bool = None

    # ----

    # 일값
    key: str = None
    # 이름
    title: str = None
    # 버전 1
    version: str = None
    # 백엔드코드위치
    src: str = None
    # 설명
    comment: str = None

    # 입력목록
    inputList: list = None
    # 출력목록
    outputList: list = None

    # --프론트엔드--

    # 이름
    anotherName: str = None
    # Icon
    icon: str = None
    # 
    helpManual: str = None
    # 없음높이단계매개변수
    noAdvanced: bool = None

    # 기존가능지원의평면
    platform: str = None  # 지원windows linux windows linux,windows

    def tojson(self, filtered_none=True):
        data = self.__dict__
        if filtered_none:
            data = {k: v for k, v in data.items() if v is not None and not k.startswith("_")}
        return data

    def init(self):
        if self.title and not self.anotherName:
            self.anotherName = self.title
        return self


@dataclass
class AtomicFormTypeMeta:
    """기존가능-FormType"""

    # 유형지정
    type: str = None  # AtomicFormType조회
    # 매개변수
    params: dict = None  # AtomicFormTypeParam조회

    def tojson(self, filtered_none=True):
        data = self.__dict__
        if filtered_none:
            data = {k: v for k, v in data.items() if v is not None}
        return data


@dataclass
class AtomicParamMeta:
    """기존가능-입력출력Meta"""

    # 유형
    __annotation__: Any = None

    # ----

    # []유형
    types: str = None
    # []프론트엔드유형
    formType: AtomicFormTypeMeta = None
    # []일값
    key: str = None
    # []이름
    title: str = None
    # title
    subTitle: str = None
    # 백엔드이름
    name: str = None
    # 사용자안내
    tip: str = None
    # 가능선택값
    options: list = None
    # 값
    default: Any = None
    # 값
    value: Any = None
    # 파싱
    need_parse: str = None

    # --프론트엔드--

    # 대기단계
    level: AtomicLevel = None
    # 
    dynamics: list[DynamicsItem] = None
    # 연결방법
    direction: str = None
    # 여부(전예필요)
    required: bool = None
    # 여부
    readOnly: bool = None
    # 할 수 없음입력, 가능선택
    noInput: bool = None
    # 게시시여부필요공유해당매개변수(아니요필요)
    share: bool = None
    # 여부검증입력문자기호길이정도
    limitLength: list = None

    # 값(속성지정)
    exit: dict = None

    def tojson(self, filtered_none=True):
        data = self.__dict__
        if filtered_none:
            data = {k: v for k, v in data.items() if v is not None and not k.startswith("_")}
        return data

    def update(
        self,
        name: str = None,
        types: str = None,
        __annotation__: Any = None,
        formType: AtomicFormTypeMeta = None,
        default: Any = None,
        options: list = None,
        noInput: bool = None,
        required: Union[None, bool] = True,
    ):
        if self.name is None:
            self.name = name
        if self.types is None:
            self.types = types
        if self.__annotation__ is None:
            self.__annotation__ = __annotation__
        if self.formType is None:
            self.formType = formType
            if self.formType.type.startswith("INPUT"):
                self.value = [{"type": "str", "value": ""}]  # input열기 의경과예배열, 일개
        if self.default is None:
            self.default = default
        if self.required is None:
            self.required = required
        if self.noInput is None:
            self.noInput = noInput
        if self.options is None:
            self.options = options
        return self


@dataclass
class TypesMeta:
    """유형-Meta"""

    # key
    key: str = None
    # src 코드위치
    src: str = None
    # 설명
    desc: str = None
    # version
    version: str = None
    # 
    channel: str = None
    # 설명
    template: str = None
    # 유형목록
    funcList: list = None


@dataclass
class TypeFuncMeta:
    """유형-빠름방법법"""

    # key
    key: str = None
    # 방법법설명
    funcDesc: str = None
    # 반환값유형
    resType: str = None
    # 반환값설명
    resDesc: str = None
    # src 코드예사용
    useSrc: str = None


class ReportType(Enum):
    Code = "code"  # 프로세스의매일개기존금액가능의로그인쇄
    Flow = "flow"  # 프로세스의로그인쇄
    User = "user"  # 사용자의로그인쇄
    Tip = "tip"  # 안내로그인쇄
    Script = "script"  # 본중의로그인쇄


class ReportFlowStatus(Enum):
    INIT = "init"
    INIT_SUCCESS = "init_success"
    TASK_START = "task_start"
    TASK_END = "task_end"
    TASK_ERROR = "task_error"


class ReportCodeStatus(Enum):
    DEBUG_START = "debug_start"
    START = "start"
    RES = "res"
    ERROR = "error"
    SKIP = "skip"


class TimeFormatType(Enum):
    YMD = "%Y-%m-%d"
    YMD_HMS = "%Y-%m-%d %H:%M:%S"
    YMD_SHORT = "%Y-%m-%d"
    YMD_HM = "%Y-%m-%d %H:%M"
    YMD_HMS2 = "%Y-%m-%d %H:%M:%S"
    YMD_SLASH = "%Y/%m/%d"
    YMD_SLASH_HM = "%Y/%m/%d %H:%M"
    YMD_SLASH_HMS = "%Y/%m/%d %H:%M:%S"
    YMD_COMPACT = "%Y%m%d"
    HM = "%H:%M"
    HMS = "%H:%M:%S"
    WEEKDAY = "%w"
    DOY = "%j"
    ISO_WEEK = "%W"
    YMD_CN = "%Y년%m월%d일"
    YMD_CN_HM = "%Y년%m월%d일 %H:%M"
    YMD_CN_HMS = "%Y년%m월%d일 %H:%M:%S"


@dataclass
class ReportUser:
    log_type: ReportType = ReportType.User
    process: str = None  # 가능빈
    process_id: str = None
    atomic: str = None  # 가능빈
    line_id: str = ""  # 가능빈
    line: int = 0
    msg_str: str = None


@dataclass
class ReportFlow:
    log_type: ReportType = ReportType.Flow
    max_line: int = 0
    status: ReportFlowStatus = ReportFlowStatus.INIT
    result: str = None  # 봇상태 서버위상태 result
    data: Any = None  # 프로세스의반환데이터
    error_traceback: Any = None
    msg_str: str = None


@dataclass
class ReportTip:
    log_type: ReportType = ReportType.Tip
    msg_str: str = None


@dataclass
class ReportScript:
    log_type: ReportType = ReportType.Script
    process: str = None  # 가능빈
    process_id: str = None
    line_id: str = ""  # 가능빈
    line: int = 0
    msg_str: str = None


@dataclass
class ReportCode:
    log_type: ReportType = ReportType.Code
    process: str = None  # 가능빈
    process_id: str = None
    atomic: str = None  # 가능빈
    key: str = None  # 가능빈
    line_id: str = ""  # 가능빈
    line: int = 0
    status: ReportCodeStatus = ReportCodeStatus.RES
    error_traceback: Any = None
    msg_str: str = None
    debug_data: Any = None