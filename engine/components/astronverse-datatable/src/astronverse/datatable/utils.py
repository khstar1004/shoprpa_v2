import os
from datetime import datetime

import openpyxl
from astronverse.datatable import ConditionType, FilterType
from astronverse.datatable.error import (
    COL_FORMAT_ERROR,
    DATAFRAME_EXPECTION,
    FORMULA_FORMAT_ERROR,
    ROW_FORMAT_ERROR,
)


def validate(row=1, col="A"):
    """
    인증행열형식
    :param row: 행
    :param col: 열
    """
    print(f"Validating row: {row}, col: {col}")
    try:
        row = int(row)
    except ValueError:
        pass
    if isinstance(col, str):
        if not (col.isalpha() and col.upper() >= "A"):
            raise DATAFRAME_EXPECTION(COL_FORMAT_ERROR.format(col), "열형식오류")
    if not isinstance(row, int) or row < 1:
        raise DATAFRAME_EXPECTION(ROW_FORMAT_ERROR.format(row), "행형식오류")


def validate_row(row):
    try:
        row = int(row)
    except (ValueError, TypeError):
        pass
    if isinstance(row, int):
        if row < 1:
            raise DATAFRAME_EXPECTION(ROW_FORMAT_ERROR.format(row), "행형식오류")
    else:
        raise DATAFRAME_EXPECTION(ROW_FORMAT_ERROR.format(row), "행형식오류")


def validate_col(col):
    if not isinstance(col, str):
        raise DATAFRAME_EXPECTION(COL_FORMAT_ERROR.format(col), "열형식오류")
    col = col.upper()
    if not (col.isalpha() and col >= "A"):
        raise DATAFRAME_EXPECTION(COL_FORMAT_ERROR.format(col), "열형식오류")


def validate_end_col(start_col, end_col):
    if end_col is None or end_col == "" or start_col is None or start_col == "":
        return
    start_index = col_to_index(start_col)
    end_index = col_to_index(end_col)
    if end_index < start_index:
        raise ValueError("끝 열할 수 없음소시작 열")


def validate_end_row(start_row, end_row):
    try:
        start_row = int(start_row)
        end_row = int(end_row)
    except ValueError:
        raise DATAFRAME_EXPECTION(ROW_FORMAT_ERROR.format(end_row), "행형식오류")
    if end_row < start_row:
        raise ValueError("끝 행할 수 없음소시작 행")


def col_to_index(col="A") -> int:
    """를열변환로검색"""
    try:
        col = int(col)
    except ValueError:
        pass
    if isinstance(col, int):
        if col < 1:
            raise DATAFRAME_EXPECTION(COL_FORMAT_ERROR.format(col), "열형식오류")
        return col
    else:
        col = col.upper()
        index = 0
        for i, char in enumerate(reversed(col)):
            index += (ord(char) - ord("A") + 1) * (26**i)
        return index


def index_to_col(index=1) -> str:
    """를검색변환로열"""
    col = ""
    index += 1  # 변환로1-based검색
    while index > 0:
        index, remainder = divmod(index - 1, 26)
        col = chr(65 + remainder) + col
    return col


def validate_formula(formula: str):
    """
    인증방식형식
    :param formula: 방식문자열
    """
    if not isinstance(formula, str) or not formula.startswith("="):
        raise DATAFRAME_EXPECTION(FORMULA_FORMAT_ERROR.format(formula), "방식형식오류")


def filter_data(
    data: list,
    filter_type: FilterType,
    condition_type: ConditionType,
    condition_value: str,
    date_value: str,
    date_range: str,
    is_case_sensitive: bool,
) -> list:
    filtered_data = []
    if not data:
        return []
    if filter_type == FilterType.TABLE:  # 필터링테이블데이터
        for row in data:
            filtered_row = []
            for item in row:
                if value_check(
                    value=item,
                    condition_type=condition_type,
                    condition_value=condition_value,
                    date_value=date_value,
                    date_range=date_range,
                    is_case_sensitive=is_case_sensitive,
                ):
                    filtered_row.append(item)
            if filtered_row:
                filtered_data.append(filtered_row)
    else:
        for item in data:
            if value_check(
                value=item,
                condition_type=condition_type,
                condition_value=condition_value,
                date_value=date_value,
                date_range=date_range,
                is_case_sensitive=is_case_sensitive,
            ):
                if isinstance(item, datetime):
                    item = item.strftime("%Y-%m-%d %H:%M:%S")
                filtered_data.append(item)
    return filtered_data


def value_check(
    value, condition_type: ConditionType, condition_value: str, date_value, date_range, is_case_sensitive: bool
) -> bool:
    """필터링관리기기"""
    val = value
    cond_val = condition_value

    # Handle case sensitivity for string operations
    if isinstance(val, str) and isinstance(cond_val, str) and not is_case_sensitive:
        val = val.lower()
        cond_val = cond_val.lower()

    if condition_type == ConditionType.EQUALS:
        return str(val) == str(cond_val)
    elif condition_type == ConditionType.NOT_EQUALS:
        return str(val) != str(cond_val)
    elif condition_type == ConditionType.GREATER_THAN:
        try:
            val_num = float(val)
            cond_val_num = float(cond_val)
            return val_num > cond_val_num
        except (ValueError, TypeError):
            return False
    elif condition_type == ConditionType.LESS_THAN:
        try:
            val_num = float(val)
            cond_val_num = float(cond_val)
            return val_num < cond_val_num
        except (ValueError, TypeError):
            return False
    elif condition_type == ConditionType.GREATER_THAN_OR_EQUAL:
        try:
            val_num = float(val)
            cond_val_num = float(cond_val)
            return val_num >= cond_val_num
        except (ValueError, TypeError):
            return False
    elif condition_type == ConditionType.LESS_THAN_OR_EQUAL:
        try:
            val_num = float(val)
            cond_val_num = float(cond_val)
            return val_num <= cond_val_num
        except (ValueError, TypeError):
            return False
    elif condition_type == ConditionType.CONTAINS:
        return str(val).find(str(cond_val)) != -1
    elif condition_type == ConditionType.NOT_CONTAINS:
        return str(val).find(str(cond_val)) == -1
    elif condition_type == ConditionType.IS_EMPTY:
        return val is None or val == ""
    elif condition_type == ConditionType.IS_NOT_EMPTY:
        return val is not None and val != ""
    elif condition_type == ConditionType.STARTS_WITH:
        return str(val).startswith(str(cond_val))
    elif condition_type == ConditionType.ENDS_WITH:
        return str(val).endswith(str(cond_val))
    elif condition_type == ConditionType.DATE_AFTER:
        try:
            val_date = val if isinstance(val, datetime) else datetime.strptime(val, "%Y-%m-%d")
            cond_date = datetime.strptime(date_value, "%Y-%m-%d")
            return val_date > cond_date
        except (ValueError, TypeError):
            return False
    elif condition_type == ConditionType.DATE_BEFORE:
        try:
            val_date = val if isinstance(val, datetime) else datetime.strptime(val, "%Y-%m-%d")
            cond_date = datetime.strptime(date_value, "%Y-%m-%d")
            return val_date < cond_date
        except (ValueError, TypeError):
            return False
    elif condition_type == ConditionType.DATE_BETWEEN:
        try:
            val_date = val if isinstance(val, datetime) else datetime.strptime(val, "%Y-%m-%d")
            start_date_str, end_date_str = date_range.split(",")
            start_date = datetime.strptime(start_date_str.strip(), "%Y-%m-%d")
            end_date = datetime.strptime(end_date_str.strip(), "%Y-%m-%d")
            return start_date <= val_date <= end_date
        except (ValueError, TypeError):
            return False
    return False


def ensure_xlsx_file(file_path):
    # 결과가파일찾을 수 없습니다또는아니오예합치기법xlsx, 이면새생성일개
    if not os.path.exists(file_path) or not is_valid_xlsx(file_path):
        wb = openpyxl.Workbook()
        wb.save(file_path)


def is_valid_xlsx(file_path):
    try:
        openpyxl.load_workbook(file_path)
        return True
    except Exception:
        return False