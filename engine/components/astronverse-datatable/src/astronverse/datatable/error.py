from astronverse.baseline.error.error import BaseException, BizCode, ErrorCode
from astronverse.baseline.i18n.i18n import _

DATAFRAME_EXPECTION = BaseException

PARAMS_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("매개변수있음오류") + ": {}")
DATAFRAME_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("데이터테이블형식있음오류") + ": {}")
ROW_FORMAT_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("행데이터있음오류,  행가능예대0의정상정수") + ": {}")
COL_FORMAT_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("열데이터있음오류, 열이름가능예A,AB...") + ": {}")
AREA_FORMAT_ERROR: ErrorCode = ErrorCode(
    BizCode.LocalErr, _("데이터있음오류, 데이터가능예문자열, 문자열배열, 이문자열배열의일") + ": {}"
)

CELL_READ_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("셀가져오기실패") + ": {}")
DATAFRAME_FILE_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("데이터파일가져오기실패") + ": {}")

DATA_NONE_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("데이터비워 둘 수 없습니다") + ": {}")
WRITE_DATA_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("데이터입력실패") + ": {}")
WRITE_PERMISSION_DENIED_ERROR_FORMAT: ErrorCode = ErrorCode(
    BizCode.LocalErr, _("입력데이터권한, 요청닫기 닫기파일후재시도") + ": {}"
)

FORMULA_FORMAT_ERROR: ErrorCode = ErrorCode(BizCode.LocalErr, _("방식형식있음오류, 방식예으로=열기 의문자열") + ": {}")
IMPORT_FILE_ERROR_FORMAT: ErrorCode = ErrorCode(BizCode.LocalErr, _("가져오기파일실패") + ": {}")