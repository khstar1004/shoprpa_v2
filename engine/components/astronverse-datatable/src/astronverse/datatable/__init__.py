from enum import Enum


class BaseOperateType(Enum):
    CELL = "cell"  # 셀
    ROW = "row"  # 행
    COLUMN = "column"  # 열
    AREA = "area"  # 


ReadType = BaseOperateType
WriteType = BaseOperateType
CopyType = BaseOperateType
PasteType = BaseOperateType
DeleteType = BaseOperateType


class WriteMode(Enum):
    OVERWRITE = "overwrite"  # 덮어쓰기입력
    INSERT = "insert"  # 삽입입력
    APPEND = "append"  # 추가 입력입력


class CellInsertShift(Enum):
    """셀삽입, 지정삽입시셀의방법"""

    DOWN = "down"  # 아래
    RIGHT = "right"  # 오른쪽


class RowInsertShift(Enum):
    """행삽입, 지정삽입까지지정행의위방법예아래방법"""

    UP = "up"  # 위삽입
    DOWN = "down"  # 아래삽입


class ColumnInsertShift(Enum):
    """열삽입, 지정삽입까지지정열의왼쪽예오른쪽"""

    LEFT = "left"  # 왼쪽삽입
    RIGHT = "right"  # 오른쪽삽입


class AppendShift(Enum):
    ROW = "row"  # 행추가 입력
    COLUMN = "column"  # 열추가 입력


class InsertType(Enum):
    ROW = "row"  # 삽입행
    COLUMN = "column"  # 삽입열


class PasteValueType(Enum):
    VALUE = "value"  # 붙여넣기값
    FORMULA = "formula"  # 붙여넣기방식


class DeleteCellMove(Enum):
    LEFT = "left"  # 왼쪽
    UP = "up"  # 위
    NOT_MOVE = "not"  # 아니오


class SortOrder(Enum):
    ASCENDING = "ascending"  # 상승순서
    DESCENDING = "descending"  # 순서


class ExportFileType(Enum):
    XLSX = "xlsx"  # Excel 파일 .xlsx
    XLS = "xls"  # Excel 파일 .xls
    CSV = "csv"  # CSV 파일 .csv
    JSON = "json"  # JSON 파일 .json


class FilterType(Enum):
    ROW = "row"  # 행필터링
    COLUMN = "column"  # 열필터링
    TABLE = "table"  # 테이블필터링


class LoopType(Enum):
    ROW = "row"  # 행
    COLUMN = "column"  # 열
    AREA = "area"  # 


class ConditionType(Enum):
    EQUALS = "equals"  # 대기
    NOT_EQUALS = "not_equals"  # 아니오대기
    GREATER_THAN = "greater_than"  # 대
    LESS_THAN = "less_than"  # 소
    GREATER_THAN_OR_EQUAL = "greater_than_or_equal"  # 대대기
    LESS_THAN_OR_EQUAL = "less_than_or_equal"  # 소대기
    CONTAINS = "contains"  # 패키지
    NOT_CONTAINS = "not_contains"  # 아니오패키지
    IS_EMPTY = "is_empty"  # 비어 있습니다
    IS_NOT_EMPTY = "is_not_empty"  # 아니오비어 있습니다
    STARTS_WITH = "starts_with"  # 으로...열기 
    ENDS_WITH = "ends_with"  # 으로...결과
    DATE_BEFORE = "date_before"  # 날짜에서...전
    DATE_AFTER = "date_after"  # 날짜에서...후
    DATE_BETWEEN = "date_between"  # 날짜에서...


class FindType(Enum):
    COLUMN = "column"  # 열조회
    TABLE = "table"  # 테이블조회