"""
데이터 처리닫기유형지정모듈.
"""

from enum import Enum


class VariableType(Enum):
    """변수유형"""

    STR = "str"
    INT = "int"
    FLOAT = "float"
    BOOL = "bool"
    LIST = "list"
    DICT = "dict"
    JSON = "json"
    TUPLE = "tuple"
    OTHER = "other"


class ExtractType(Enum):
    """가져오기유형."""

    PHONE_NUMBER = "phone_number"
    EMAIL = "email"
    URL = "url"
    DIGIT = "digit"
    ID_NUMBER = "id_number"
    REGEX = "regex"


class ReplaceType(Enum):
    """유형."""

    STRING = "string"
    PHONE_NUMBER = "phone_number"
    EMAIL = "email"
    URL = "url"
    DIGIT = "digit"
    ID_NUMBER = "id_number"
    REGEX = "regex"


class NumberType(Enum):
    """숫자유형."""

    INTEGER = "integer"
    FLOAT = "float"


class ListType(Enum):
    """목록유형."""

    EMPTY = "empty"
    SAME_DATA = "same_data"
    USER_DEFINED = "user_defined"


class InsertMethodType(Enum):
    """삽입방법법유형."""

    APPEND = "append"
    INDEX = "index"


class DeleteMethodType(Enum):
    """삭제방법법유형."""

    INDEX = "index"
    VALUE = "value"


class SortMethodType(Enum):
    """정렬방법법유형."""

    ASC = "asc"
    DESC = "desc"


class ConcatStringType(Enum):
    """문자열연결유형."""

    NONE = "none"
    LINEBREAK = "linebreak"
    SPACE = "space"
    HYPHEN = "hyphen"
    UNDERLINE = "underline"
    OTHER = "other"


class FillStringType(Enum):
    """문자열유형."""

    RIGHT = "right"
    LEFT = "left"


class StripStringType(Enum):
    """문자열제거유형."""

    LEFT = "left"
    RIGHT = "right"
    BOTH = "both"


class CutStringType(Enum):
    """문자열유형."""

    FIRST = "first"
    INDEX = "index"
    STRING = "string"


class CaseChangeType(Enum):
    """문자열크기변수유형."""

    UPPER = "upper"
    LOWER = "lower"
    CAPS = "caps"


class NoKeyOptionType(Enum):
    """없음 key 선택유형."""

    RAISE_ERROR = "raise_error"
    RETURN_DEFAULT = "return_default"


class JSONConvertType(Enum):
    """JSON 변환유형."""

    JSON_TO_STR = "json_to_str"
    STR_TO_JSON = "str_to_json"


class StringConvertType(Enum):
    """문자열변환유형."""

    STR_TO_LIST = "str_to_list"
    STR_TO_DICT = "str_to_dict"
    STR_TO_TUPLE = "str_to_tuple"
    STR_TO_BOOL = "str_to_bool"
    STR_TO_INT = "str_to_int"
    STR_TO_FLOAT = "str_to_float"


class AddSubType(Enum):
    """추가유형."""

    ADD = "add"
    SUB = "sub"


class MathOperatorType(Enum):
    """데이터실행기호유형."""

    ADD = "+"
    SUB = "-"
    MUL = "*"
    DIV = "/"


class MathRoundType(Enum):
    """데이터가져오기 유형."""

    ROUND = "round"
    CEIL = "ceil"
    FLOOR = "floor"
    NONE = "none"


class TimeChangeType(Enum):
    """시간변수유형."""

    MAINTAIN = "maintain"
    ADD = "add"
    SUB = "sub"


class TimestampUnitType(Enum):
    """시간단일위치유형."""

    SECOND = "second"
    MILLISECOND = "millisecond"
    MICROSECOND = "microsecond"


class TimeZoneType(Enum):
    """시유형."""

    UTC = "UTC"
    LOCAL = "local"


class TimeUnitType(Enum):
    """시간단일위치유형."""

    SECOND = "second"
    MINUTE = "minute"
    HOUR = "hour"
    DAY = "day"
    MONTH = "month"
    YEAR = "year"