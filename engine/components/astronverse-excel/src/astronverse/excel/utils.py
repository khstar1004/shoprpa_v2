import os
import re
from typing import Any

import psutil
from astronverse.excel import FileExistenceType, ReadRangeType


def get_excel_processes():
    """
    조회현재실행의Excel및WPS

    Returns:
        tuple: (excel_flag, wps_flag, excel_pid, wps_pid)
            - excel_flag: Excel여부존재함
            - wps_flag: WPS여부존재함
            - excel_pid: ExcelID, 결과가찾을 수 없습니다이면로None
            - wps_pid: WPSID, 결과가찾을 수 없습니다이면로None
    """
    excel_flag, wps_flag = False, False
    excel_pid, wps_pid = None, None
    pid_list = psutil.pids()
    for pid in pid_list:
        try:
            p = psutil.Process(pid)
            p_name = p.name().lower()
            if p_name == "excel.exe":
                excel_flag = True
                excel_pid = pid
            elif p_name == "et.exe":
                wps_flag = True
                wps_pid = pid
        except Exception as e:
            pass
    return excel_flag, excel_pid, wps_flag, wps_pid


def resolve_file_path(file_path, exist_type):
    """
    근거파일존재함유형파싱파일 경로

    Args:
        file_path: 목록파일 경로
        exist_type: 파일존재함시의관리방식(FileExistenceType)
            - OVERWRITE: 덮어쓰기완료존재함파일, 직선연결반환기존경로
            - RENAME: 결과가파일존재함, 이름 변경로파일이름_1, 파일이름_2대기
            - CANCEL: 결과가파일존재함, 반환빈문자열;아니오이면반환기존경로

    Returns:
        str: 관리후의파일 경로, 결과가CANCEL파일존재함이면반환빈문자열
    """
    if exist_type == FileExistenceType.OVERWRITE:
        return file_path
    elif exist_type == FileExistenceType.RENAME:
        if os.path.exists(file_path):
            full_file_name = os.path.basename(file_path)
            file_name, file_ext = os.path.splitext(full_file_name)
            count = 1
            while True:
                new_full_file_name = f"{file_name}_{count}{file_ext}"
                new_file_path = os.path.join(os.path.dirname(file_path), new_full_file_name)
                if os.path.exists(new_file_path):
                    count += 1
                else:
                    return new_file_path
        return file_path
    elif exist_type == FileExistenceType.CANCEL:
        if os.path.exists(file_path):
            return ""
        else:
            return file_path


def column_letter_to_number(column_letter: str) -> int:
    """
    를열문자변환로열(1-based)

    지원Excel열문자형식(예"A", "B", "Z", "AA", "AB"대기)및숫자형식.

    Args:
        column_letter: 열문자문자열(예"A")또는숫자문자열(예"1"), 빈문자열반환1

    Returns:
        int: 열(1-based), 예"A"반환1, "B"반환2, "AA"반환27
    """
    if not column_letter:
        return 1
    if column_letter.isdigit():
        return int(column_letter)
    column_letter = column_letter.upper()
    column_number = 0
    for i in range(len(column_letter)):
        column_number += (ord(column_letter[i]) - ord("A") + 1) * (26 ** (len(column_letter) - i - 1))
    return column_number


def column_number_to_letter(column: int) -> str:
    """
    를열변환로열문자

    를숫자열변환로Excel열문자형식(예1->"A", 2->"B", 27->"AA"대기).

    Args:
        column: 열(1-based), 결과가소대기0이면반환"A"

    Returns:
        str: 열문자문자열, 예1반환"A", 2반환"B", 27반환"AA"
    """
    if column <= 0:
        return "A"
    result_str = ""
    while column > 0:
        column -= 1
        result_str = chr(ord("A") + column % 26) + result_str
        column //= 26
    return result_str


def handle_row_input(row: Any, used_row: int) -> int:
    """
    관리행입력, 지원데이터(에서열기 계획)

    지원정상데이터, 데이터및빈문자열입력.데이터테이블에서열기 계획, 
    예: used_row=10, row=-1 -> 10+1+(-1)=10 (후일행)

    Args:
        row: 행문자열, 가능으로예정상데이터, 데이터또는빈문자열
        used_row: 완료사용의행데이터

    Returns:
        int: 관리후의행(1-based), 빈문자열반환1

    Raises:
        ValueError: 입력값을 숫자로 변환할 수 없는 경우
    """
    if not row:
        return 1
    try:
        row_num = int(row)
        if row_num < 0:
            # 데이터테이블에서열기 계획: used_row + 1 + row_num
            return used_row + 1 + row_num
        else:
            if row_num == 0:
                return 1
            return row_num
    except (ValueError, TypeError):
        raise ValueError(f"행입력예외, 입력하세요숫자또는데이터!입력값: {row}")


def handle_column_input(col: str, used_col: int) -> int:
    """
    관리열입력, 지원데이터(에서열기 계획)

    지원문자(예"A"), 숫자(예"1"), 데이터(예"-1")또는빈문자열입력.
    데이터테이블에서열기 계획, 예: used_col=5, col=-1 -> 5+1+(-1)=5 (후일열)

    Args:
        col: 열문자열, 가능으로예문자(예"A"), 숫자(예"1"), 데이터(예"-1")또는빈문자열
        used_col: 완료사용의열데이터

    Returns:
        int: 관리후의열(1-based), 빈문자열반환1

    Raises:
        ValueError: 입력값을 숫자 또는 문자로 변환할 수 없는 경우
    """
    if not col:
        return 1

    try:
        col_num = int(col)
        if col_num < 0:
            # 데이터테이블에서열기 계획: used_col + 1 + col_num
            return used_col + 1 + col_num
        else:
            if col_num == 0:
                return 1
            return col_num
    except (ValueError, TypeError):
        # 결과가아니오예숫자, 시도로문자관리
        return column_letter_to_number(col)


def handle_used_range(address: str):
    # 관리기호의used_range 예 $A$1:$B$2
    # sheet전체사용경과의시, address예$A$1
    address_list = address.split(":")
    if len(address_list) == 1:
        starter = address_list[0]
        ender = address_list[0]
    else:
        starter = address_list[0]
        ender = address_list[1]
    if address.find("$") == -1:
        start_col = re.findall(r"[A-Z]+", starter)[0]
        start_row = re.findall(r"[0-9]+", starter)[0]
        end_col = re.findall(r"[A-Z]+", ender)[0]
        end_row = re.findall(r"[0-9]+", ender)[0]
    else:
        start_col = starter.split("$")[1]
        start_row = starter.split("$")[2]
        end_col = ender.split("$")[1]
        end_row = ender.split("$")[2]
    return [start_col, start_row, end_col, end_row]


def handle_multiple_inputs(inputs: str, used_row: int, used_col: int, is_row=True):
    """
    관리다중개열또는행입력, 예A:B,C,-1

    Args:
        inputs: 입력문자열, 지요소분및, 예 "1:3,5" 또는 "A:B,C"
        used_row: 완료사용의대행
        used_col: 완료사용의대열
        is_row: 여부로행입력, True테이블행, False테이블열

    Returns:
        List[int]: 관리후의행또는열목록
    """
    inputs = inputs.replace(", ", ",").replace(": ", ":")
    inputs = inputs.split(",")
    result = []
    for element in inputs:
        element = element.strip()
        if ":" in element:
            left_element = element.split(":")[0].strip()
            right_element = element.split(":")[1].strip()
            if is_row:
                left_num = handle_row_input(left_element, used_row)
                right_num = handle_row_input(right_element, used_row)
            else:
                left_num = handle_column_input(left_element, used_col)
                right_num = handle_column_input(right_element, used_col)
            for i in range(left_num, right_num + 1):
                result.append(i)
        else:
            if is_row:
                result.append(handle_row_input(element, used_row))
            else:
                result.append(handle_column_input(element, used_col))
    return result


def check_color(color: str):
    if not color:
        return color
    if isinstance(color, str):
        color = color.split(",")
        try:
            color = [int(c.strip()) for c in color]
        except Exception as e:
            raise ValueError("올바른 색상 형식을 입력하세요.")
    if isinstance(color, list):
        if len(color) != 3:
            raise ValueError("올바른 색상 형식을 입력하세요.")
        for rgb in color:
            if (not isinstance(rgb, int)) or rgb >= 256 or rgb < 0:
                raise ValueError("올바른 색상 형식을 입력하세요.")
    else:
        raise ValueError("올바른 색상 형식을 입력하세요.")
    return color


def util_trim(value):
    if isinstance(value, str):
        return value.strip()
    elif isinstance(value, list):
        return [util_trim(v) for v in value]
    return value


def util_replace_node(value):
    if value is None:
        return ""
    elif isinstance(value, list):
        return [util_replace_node(v) for v in value]
    return value


def calculate_cell_positions(
    design_type: str,
    cell_position: str = "",
    range_position: str = "",
    col: str = "",
    row: str = "",
    r_end_row: int = 0,
    r_end_col: int = 0,
    r_address: str = "",
    support_comma: bool = True,
    support_colon: bool = True,
) -> list[str]:
    end_col_letter = column_number_to_letter(r_end_col)
    positions: list[str] = []

    def _handler(raw: str, single_fn, range_fn) -> None:
        raw = raw.replace(", ", ",").replace(": ", ":")
        if ":" in raw:
            if not support_colon:
                raise Exception("범위 입력은 지원하지 않습니다.")
            a, b = raw.split(":", 1)
            positions.append(range_fn(a, b))
        elif "," in raw:
            if not support_comma:
                raise Exception("여러 값 입력은 지원하지 않습니다.")
            for item in raw.split(","):
                positions.append(single_fn(item))
        else:
            positions.append(single_fn(raw))

    if design_type == ReadRangeType.CELL.value:
        positions.append(cell_position)
    elif design_type == ReadRangeType.ROW.value:
        if isinstance(row, str):
            _handler(
                row,
                lambda r: f"A{handle_row_input(r, r_end_row)}:{end_col_letter}{handle_row_input(r, r_end_row)}",
                lambda r1, r2: f"A{handle_row_input(r1, r_end_row)}:{end_col_letter}{handle_row_input(r2, r_end_row)}",
            )
        else:
            r = handle_row_input(row, r_end_row)
            positions.append(f"A{r}:{end_col_letter}{r}")
    elif design_type == ReadRangeType.COLUMN.value:
        if isinstance(col, str):
            _handler(
                col,
                lambda c: f"{column_number_to_letter(handle_column_input(c, r_end_col))}1:"
                f"{column_number_to_letter(handle_column_input(c, r_end_col))}{r_end_row}",
                lambda c1, c2: f"{column_number_to_letter(handle_column_input(c1, r_end_col))}1:"
                f"{column_number_to_letter(handle_column_input(c2, r_end_col))}{r_end_row}",
            )
        else:
            c = column_number_to_letter(handle_column_input(col, r_end_col))
            positions.append(f"{c}1:{c}{r_end_row}")
    elif design_type == ReadRangeType.AREA.value:
        c1, r1, c2, r2 = handle_used_range(range_position)
        positions.append(f"{c1}{r1}:{c2}{r2}")
    elif design_type == ReadRangeType.ALL.value:
        c1, r1, c2, r2 = handle_used_range(r_address)
        positions.append(f"{c1}{r1}:{c2}{r2}")
    else:
        raise NotImplementedError()
    return positions
