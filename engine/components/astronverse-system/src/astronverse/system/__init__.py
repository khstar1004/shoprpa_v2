from enum import Enum


class ReportLevelType(Enum):
    INFO = "info"
    WARNING = "warning"
    ERROR = "error"


class ExistType(Enum):
    EXIST = "exist"  # 존재함
    NOT_EXIST = "not_exist"  # 찾을 수 없습니다


class OptionType(Enum):
    OVERWRITE = "overwrite"  # 덮어쓰기
    SKIP = "skip"  # 건너뛰기
    GENERATE = "generate"  # 완료본


class DeleteType(Enum):
    DELETE = "delete"  # 삭제
    TRASH = "trash"  # 입력돌아가기


class WriteType(Enum):
    OVERWRITE = "overwrite"  # 덮어쓰기입력
    APPEND = "append"  # 추가 입력입력


class EncodeType(Enum):
    DEFAULT = "default"
    ANSI = "ansi"
    UTF8 = "utf-8"
    UTF16 = "utf-16"
    UTF_16_BE = "utf-16 be"
    GBK = "gbk"
    GB2312 = "gb2312"
    GB18030 = "gb18030"


class StateType(Enum):
    CREATE = "create"  # 새생성
    ERROR = "error"  # 안내오류


class ReadType(Enum):
    ALL = "all"  # 가져오기전체내용
    List = "list"  # 가져오기까지목록중
    BYTE = "byte"  # 이제어방식


class SearchType(Enum):
    EXACT = "exact"  # 매칭
    FUZZY = "fuzzy"  # 매칭
    REGEX = "regex"  # 정상이면테이블방식매칭


class TraverseType(Enum):
    YES = "yes"  # 폴더
    NO = "no"  # 아니오폴더


class StatusType(Enum):
    CREATED = "created"  # 생성
    DELETED = "deleted"  # 삭제


class InfoType(Enum):
    ALL = "all"
    ABS_PATH = "abs_path"
    ROOT = "root"
    DIRECTORY = "directory"
    SIZE = "size"
    NAME_EXT = "name_ext"
    NAME = "name"
    EXTENSION = "extension"
    C_TIME = "c_time"
    M_TIME = "m_time"


class OutputType(Enum):
    LIST = "list"  # 목록출력
    EXCEL = "excel"  # 테이블문서


class SortMethod(Enum):
    NONE = "none"  # 아니오정렬
    CTIME = "ctime"  # 생성 시간정렬
    MTIME = "mtime"  # 수정시간정렬


class SortType(Enum):
    ASCENDING = "ascending"  # 상승순서
    DESCENDING = "descending"  # 순서


class ContentType(Enum):
    MSG = "msg"  # 텍스트정보
    HTML = "html"  # HTML형식텍스트
    FILE = "file"  # 파일
    FOLDER = "folder"  # 폴더


class ScreenType(Enum):
    FULL = "full"  # 전체
    REGION = "region"  # 


class RunType(Enum):
    CONTINUE = "continue"
    COMPLETE = "complete"


class CmdType(Enum):
    NORMAL = "normal"  # 보통실행
    ADMIN = "admin"  # 으로관리관리원실행


class PidType(Enum):
    ALL = "all"  # 가져오기모든기호합치기의PID
    ONE = "one"  # 가져오기매칭의일개사용프로그램의PID


class TerminationType(Enum):
    PID = "pid"
    NAME = "name"


class DirType(Enum):
    SOURCE = "source"  # 경로
    NEW = "new"  # 새경로


class SaveType(Enum):
    DELETE = "delete"
    SAVE = "save"


class PwdType(Enum):
    RSA = "rsa"  # 키
    PASSWORD = "password"  # 비밀번호


class BatchType(Enum):
    BATCH = "batch"
    SINGLE = "single"


class PrinterType(Enum):
    CUSTOM = "custom"
    DEFAULT = "default"


class OrientationType(Enum):
    HORIZONTAL = "horizontal"
    VERTICAL = "vertical"


class FileType(Enum):
    PDF = "pdf"
    WORD = "word"
    EXCEL = "excel"
    PICTURE = "picture"


class MarginType(Enum):
    CUSTOM = "custom"
    DEFAULT = "default"


class PaperType(Enum):
    A3 = "A3"
    A4 = "A4"
    LA4 = "LA4"
    A5 = "A5"
    B4 = "B4"
    B5 = "B5"
    C_SHEET = "C_SHEET"
    D_SHEET = "D_SHEET"
    CUSTOM = "CUSTOM"


class FileFolderType(Enum):
    FILE = "file"
    FOLDER = "folder"
    BOTH = "both"  # 파일및폴더일압축


class DocAppType(Enum):
    """doc 문서 처리프로그램"""

    WORD = "Word"
    WPS = "WPS"
    DEFAULT = "Default"


class XlsAppType(Enum):
    """xls 문서 처리프로그램"""

    EXCEL = "Excel"
    WPS = "WPS"
    DEFAULT = "Default"