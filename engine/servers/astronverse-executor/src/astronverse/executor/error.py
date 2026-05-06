import re
from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _
from astronverse.executor.logger import logger

BaseException = BaseException

# 공통 오류
SUCCESS: ErrorCode = ErrorCode(BizCode.LocalOK, "ok")
GENERAL_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("오류: {}"))
INTERNAL_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("내부 오류: {}"))
SERVER_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("서버 오류: {}"))
SYNTAX_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("문법 오류: {}"))

# 파싱 오류
LOOP_CONTROL_STATEMENT_ERROR = _("break와 continue는 반복문 안에서만 사용할 수 있습니다")
ATOMIC_CAPABILITY_PARSE_ERROR_FORMAT = _("기능  {} 파싱에 실패했습니다")
MISSING_REQUIRED_KEY_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("필수 key 필드가 없습니다: {}"))
ONLY_ONE_CATCH_CAN_BE_RETAINED = _("catch 은 하나만 유지할 수 있습니다")

# 외부 데이터 조회
ELEMENT_ACCESS_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("요소 조회 오류: {}"))
PROCESS_ACCESS_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("프로세스 데이터 조회 오류: {}"))

# 알림 및 상태 메시지
MSG_FLOW_INIT_START = _("워크플로우를 초기화하는 중...")
MSG_FLOW_INIT_SUCCESS = _("워크플로우 초기화 완료")
MSG_TASK_EXECUTION_START = _("작업 실행 시작")
MSG_TASK_EXECUTION_END = _("작업 실행 완료")
MSG_TASK_USER_CANCELLED = _("작업 실행이 사용자에 의해 취소되었습니다")
MSG_TASK_EXECUTION_ERROR = _("작업 실행 오류")
MSG_INSTRUCTION_EXECUTION_FORMAT = _("{} {}번째 단계 실행 [{}]")
MSG_DEBUG_INSTRUCTION_START_FORMAT = _("{} {}번째 단계 디버그 시작 [{}]")
MSG_ERROR_SKIP = _("오류를 건너뛰고 계속 실행")
MSG_EXECUTION_ERROR = _("실행 오류")
MSG_VIDEO_PROCESSING_WAIT = _("녹화 데이터를 처리하는 중입니다. 시간이 걸릴 수 있으니 잠시 기다려 주세요")
MSG_DOWNLOAD_FORMAT = _("{} 다운로드 중...")
MSG_DOWNLOAD_SUCCESS_FORMAT = _("{} 다운로드 완료")
MSG_NO_FFMPEG = _("저장 디렉터리에 ffmpeg가 없어 녹화 기능을 사용할 수 없습니다")
MSG_SUB_WINDOW = _("하위 창 시작")
MSG_GLOBAL_USE_ERROR_TIP_FORMAT = _('전역 변수는 gv["{}"] 형식으로 사용하는 것을 권장합니다')


def python_base_error(e):
    if isinstance(e, NameError):
        error_str = str(e)
        name_error_translations = [
            (r"name '(.+)' is not defined", "정의되지 않은 이름입니다: '{}'"),
        ]
        for pattern, translation in name_error_translations:
            match = re.search(pattern, error_str)
            if match:
                error_str = translation.format(*match.groups())
        return error_str
    elif isinstance(e, TypeError):
        type_error_translations = [
            (
                r"unsupported operand type\(s\) for ([^:]+): '(.+)' and '(.+)'",
                "연산자 '{}'에 지원하지 않는 데이터 유형입니다: '{}' 및 '{}'",
            ),
            (
                r'can only concatenate ([^(]+) \(not \"([^"]+)\"\) to ([^(]+)',
                "'{}'에는 '{}'가 아닌 '{}'만 연결할 수 있습니다",
            ),
            (r"'(.+)' object is not subscriptable", "'{}' 객체는 인덱싱할 수 없습니다"),
            (r"'(.+)' object is not callable", "'{}' 객체는 호출할 수 없습니다"),
            (r"'(.+)' object is not iterable", "'{}' 객체는 반복할 수 없습니다"),
            (r"([^()]+)\(\) missing (\d+) required positional argument(s)?", "'{}' 함수에 필수 위치 인수 {}개가 없습니다"),
            (
                r"([^()]+)\(\) takes (\d+) positional argument(?:s)? but (\d+) (was|were) given",
                "'{}' 함수는 위치 인수 {}개가 필요하지만 {}개가 전달되었습니다",
            ),
            (r"([^()]+)\(\) got an unexpected keyword argument '(.+)'", "'{}' 함수에 알 수 없는 키워드 인수 '{}'가 전달되었습니다"),
            (r"unhashable type: '(.+)'", "해시할 수 없는 유형입니다: '{}'"),
        ]

        error_str = str(e)
        for pattern, translation in type_error_translations:
            match = re.search(pattern, error_str)
            if match:
                error_str = translation.format(*match.groups())
        return error_str
    elif isinstance(e, IndexError):
        index_error_translations = [
            (r"list index out of range", "목록 인덱스가 범위를 벗어났습니다"),
            (r"tuple index out of range", "튜플 인덱스가 범위를 벗어났습니다"),
            (r"string index out of range", "문자열 인덱스가 범위를 벗어났습니다"),
        ]
        error_str = str(e)
        for pattern, translation in index_error_translations:
            match = re.search(pattern, error_str)
            if match:
                error_str = translation.format(*match.groups())
        return error_str
    elif isinstance(e, KeyError):
        key_error_translations = [
            (r"'(.+)'", "딕셔너리에 '{}' 키가 없습니다"),
        ]
        error_str = str(e)
        for pattern, translation in key_error_translations:
            match = re.search(pattern, error_str)
            if match:
                error_str = translation.format(*match.groups())
        return error_str
    elif isinstance(e, ValueError):
        value_error_translations = [
            (r"invalid literal for int\(\) with base 10: '(.+)'", "'{}' 값은 정수로 변환할 수 없습니다"),
            (r"could not convert string to float: '(.+)'", "'{}' 값은 숫자로 변환할 수 없습니다"),
        ]
        error_str = str(e)
        for pattern, translation in value_error_translations:
            match = re.search(pattern, error_str)
            if match:
                error_str = translation.format(*match.groups())
        return error_str
    elif isinstance(e, AttributeError):
        attribute_error_translations = [
            (r"(.+) object has no attribute '(.+)'", "{} 객체에 '{}' 속성이 없습니다"),
        ]
        error_str = str(e)
        for pattern, translation in attribute_error_translations:
            match = re.search(pattern, error_str)
            if match:
                error_str = translation.format(*match.groups())
        return error_str
    elif isinstance(e, ZeroDivisionError):
        error_str = "0으로 나눌 수 없습니다"
        return error_str
    elif isinstance(e, ImportError):
        import_error_translations = [
            (r"cannot import name '(.+)' from '(.+)'", "'{}'에서 '{}' 이름을 가져올 수 없습니다"),
            (r"No module named '(.+)'", "'{}' 모듈을 찾을 수 없습니다"),
        ]
        error_str = str(e)
        for pattern, translation in import_error_translations:
            match = re.search(pattern, error_str)
            if match:
                error_str = translation.format(*match.groups())
        return error_str
    elif isinstance(e, SyntaxError):
        error_str = f"문법 오류입니다. 내용을 확인한 뒤 다시 시도해 주세요"
        return error_str
    elif isinstance(e, RecursionError):
        error_str = f"재귀 깊이 제한을 초과했습니다. 프로세스가 무한 반복되지 않는지 확인해 주세요"
        return error_str
    elif isinstance(e, BaseException):
        error_str = e.code.message
        logger.error("BaseException: {}".format(e.message))
        return error_str
    else:
        return str(e)
