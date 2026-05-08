from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

DATAFRAME_EXPECTION = BaseException

PARAMS_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("매개변수가 올바르지 않습니다") + ": {}")
DATAFRAME_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("데이터 테이블 형식이 올바르지 않습니다") + ": {}")
ROW_FORMAT_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("행 번호는 1 이상의 정수여야 합니다") + ": {}")
COL_FORMAT_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("열 이름은 A, AB와 같은 형식이어야 합니다") + ": {}")
AREA_FORMAT_ERROR: ErrorCode = ErrorCode(
    BizCode.LocalErr, _("데이터 영역 형식이 올바르지 않습니다. 문자열 또는 문자열 배열을 입력하세요") + ": {}"
)

CELL_READ_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("셀을 읽지 못했습니다") + ": {}")
DATAFRAME_FILE_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("데이터 파일을 읽지 못했습니다") + ": {}")

DATA_NONE_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("데이터는 비워 둘 수 없습니다") + ": {}")
WRITE_DATA_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("데이터를 입력하지 못했습니다") + ": {}")
WRITE_PERMISSION_DENIED_ERROR_FORMAT: ErrorCode = ErrorCode(
    BizCode.LocalErr, _("데이터 입력 권한이 없습니다. 열려 있는 파일을 닫고 다시 시도하세요") + ": {}"
)

FORMULA_FORMAT_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("수식은 =으로 시작하는 문자열이어야 합니다") + ": {}")
IMPORT_FILE_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("파일을 가져오지 못했습니다") + ": {}")
