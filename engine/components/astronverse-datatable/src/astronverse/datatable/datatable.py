import ast
import json
import os
import re
import sys
from datetime import datetime
from functools import wraps

from astronverse.actionlib import DynamicsItem
from astronverse.actionlib.atomic import AtomicFormType, AtomicFormTypeMeta, atomicMg
from astronverse.baseline.logger.logger import logger
from astronverse.datatable import (
    AppendShift,
    CellInsertShift,
    ColumnInsertShift,
    ConditionType,
    CopyType,
    DeleteCellMove,
    DeleteType,
    ExportFileType,
    FilterType,
    FindType,
    InsertType,
    LoopType,
    PasteType,
    ReadType,
    RowInsertShift,
    WriteMode,
    WriteType,
)
from astronverse.datatable.error import *
from astronverse.datatable.openpyxl import OpenpyxlWrapper
from astronverse.datatable.utils import (
    col_to_index,
    ensure_xlsx_file,
    filter_data,
    index_to_col,
    validate,
    validate_col,
    validate_end_col,
    validate_end_row,
    validate_formula,
    validate_row,
)

try:
    _xlsx_file_path = os.path.abspath(os.path.join(sys.exec_prefix, "../astron/data_table.xlsx"))
    _head_file_path = os.path.abspath(os.path.join(sys.exec_prefix, "../astron/data_table_head.xlsx"))
    logger.info(f"DataTable xlsx file path: {_xlsx_file_path}")
    ensure_xlsx_file(_xlsx_file_path)
    ensure_xlsx_file(_head_file_path)

    PyxlWrapper = OpenpyxlWrapper(file_path=_xlsx_file_path, sheet_name=None)
    PyxlHeadWrapper = OpenpyxlWrapper(file_path=_head_file_path, sheet_name=None)
except Exception as e:
    pass


def auto_save(func):
    """저장설치기기"""

    @wraps(func)
    def wrapper(*args, **kwargs):
        result = func(*args, **kwargs)  # type: ignore , 실행입력
        PyxlWrapper.save(path=_xlsx_file_path)
        return result

    return wrapper


def validate_cell(func):
    """인증행열설치기기"""

    @wraps(func)
    def wrapper(*args, **kwargs):
        col = kwargs.get("col")
        row = kwargs.get("row")
        start_col = kwargs.get("start_col")
        start_row = kwargs.get("start_row")

        cols_to_validate = [c for c in [col, start_col] if c]
        for c in cols_to_validate:
            validate(col=c)
        rows_to_validate = [r for r in [row, start_row] if r]
        for r in rows_to_validate:
            validate(row=r)

        return func(*args, **kwargs)  # type: ignore

    return wrapper


def sync_data_table_head():
    """에서데이터테이블삭제열데이터테이블모듈파일"""
    PyxlHeadWrapper.save(path=_head_file_path)


class DataTable:
    """데이터테이블"""

    @staticmethod
    @validate_cell
    @atomicMg.atomic(
        "DataTable",
        inputList=[
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression=f"return ['{ReadType.ROW.value}', '{ReadType.CELL.value}'].includes($this.read_type.value)",
                    )
                ],
            ),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression=f"return ['{ReadType.COLUMN.value}', '{ReadType.CELL.value}'].includes($this.read_type.value)",
                    )
                ],
            ),
            atomicMg.param(
                "start_row",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_row.show",
                        expression=f"return $this.read_type.value == '{ReadType.AREA.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "start_col",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_col.show",
                        expression=f"return $this.read_type.value == '{ReadType.AREA.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "end_row",
                dynamics=[
                    DynamicsItem(
                        key="$this.end_row.show",
                        expression=f"return $this.read_type.value == '{ReadType.AREA.value}'",
                    )
                ],
                required=False,
            ),
            atomicMg.param(
                "end_col",
                dynamics=[
                    DynamicsItem(
                        key="$this.end_col.show",
                        expression=f"return $this.read_type.value == '{ReadType.AREA.value}'",
                    )
                ],
                required=False,
            ),
        ],
        outputList=[
            atomicMg.param(
                "cell_info",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.cell_info.show",
                        expression=f"return $this.read_type.value == '{ReadType.CELL.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "row_info",
                types="List",
                dynamics=[
                    DynamicsItem(
                        key="$this.row_info.show",
                        expression=f"return $this.read_type.value == '{ReadType.ROW.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "column_info",
                types="List",
                dynamics=[
                    DynamicsItem(
                        key="$this.column_info.show",
                        expression=f"return $this.read_type.value == '{ReadType.COLUMN.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "area_info",
                types="List",
                dynamics=[
                    DynamicsItem(
                        key="$this.area_info.show",
                        expression=f"return $this.read_type.value == '{ReadType.AREA.value}'",
                    )
                ],
            ),
        ],
    )
    def read_data(
        read_type: ReadType = ReadType.CELL,
        row: int = 1,
        col: str = "A",
        start_row: int = 1,
        start_col: str = "A",
        end_row: int = 0,
        end_col: str = "",
        is_trim_spaces: bool = False,
        is_replace_none: bool = False,
    ):
        """
        데이터 테이블 읽기내용
        """
        if read_type == ReadType.CELL:
            if not row or not col:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("가져오기셀필요지정행열"), "가져오기셀필요지정행열")
            col_index = col_to_index(col)
            value = PyxlWrapper.read_cell(row=row, col=col_index)
            if is_trim_spaces and isinstance(value, str):
                value = value.strip()
            if is_replace_none and value is None:
                value = ""
            return value

        if read_type == ReadType.ROW:
            if not row:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("가져오기행필요지정행"), "가져오기행필요지정행")
            row_value = PyxlWrapper.read_row(row_index=row)
            if is_trim_spaces:
                row_value = [cell.strip() if isinstance(cell, str) else cell for cell in row_value]
            if is_replace_none:
                row_value = [cell if cell is not None else "" for cell in row_value]
            return row_value

        if read_type == ReadType.COLUMN:
            if not col:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("가져오기열필요지정열"), "가져오기열필요지정열")
            col_index = col_to_index(col)
            col_value = PyxlWrapper.read_column(col_index=col_index)
            if is_trim_spaces:
                col_value = [cell.strip() if isinstance(cell, str) else cell for cell in col_value]
            if is_replace_none:
                col_value = [cell if cell is not None else "" for cell in col_value]
            return col_value

        if read_type == ReadType.AREA:
            if not start_row or not start_col:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("가져오기 필요지정시작 행열"), "가져오기 필요지정시작 행열")
            if end_col is None or end_col in {"", "0"}:
                end_col = index_to_col(PyxlWrapper.get_max_column() - 1)
            if end_row is None or end_row in {"", "0", 0}:
                end_row = PyxlWrapper.get_max_row()
            validate_col(col=end_col)
            validate_row(row=end_row)
            validate_end_col(start_col=start_col, end_col=end_col)
            validate_end_row(start_row=start_row, end_row=end_row)
            col_range = f"{start_col}{start_row}:{end_col}{end_row}"
            range_value = PyxlWrapper.read_range(range_str=col_range)
            if is_trim_spaces:
                range_value = [
                    [cell.strip() if isinstance(cell, str) else cell for cell in row_data] for row_data in range_value
                ]
            if is_replace_none:
                range_value = [[cell if cell is not None else "" for cell in row_data] for row_data in range_value]
            return range_value

    @staticmethod
    @validate_cell
    @auto_save
    @atomicMg.atomic(
        "DataTable",
        inputList=[
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression=f"return ['{WriteType.ROW.value}', '{WriteType.CELL.value}'].includes($this.write_type.value)",
                    )
                ],
            ),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression=f"return ['{WriteType.COLUMN.value}', '{WriteType.CELL.value}'].includes($this.write_type.value)",
                    )
                ],
            ),
            atomicMg.param(
                "start_row",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_row.show",
                        expression=f"return ['{WriteType.AREA.value}', '{WriteType.COLUMN.value}'].includes($this.write_type.value) && $this.write_mode.value != '{WriteMode.APPEND.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "start_col",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_col.show",
                        expression=f"return ['{WriteType.AREA.value}', '{WriteType.ROW.value}'].includes($this.write_type.value) && $this.write_mode.value != '{WriteMode.APPEND.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "data",
                required=True,
            ),
            atomicMg.param(
                "write_mode",
                dynamics=[
                    DynamicsItem(
                        key="$this.write_mode.show",
                        expression=f"return ['{WriteType.CELL.value}', '{WriteType.ROW.value}', '{WriteType.COLUMN.value}'].includes($this.write_type.value)",
                    )
                ],
            ),
            atomicMg.param(
                "cell_insert_shift",
                dynamics=[
                    DynamicsItem(
                        key="$this.cell_insert_shift.show",
                        expression=f"return $this.write_mode.value == '{WriteMode.INSERT.value}' && $this.write_type.value == '{WriteType.CELL.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "row_insert_shift",
                dynamics=[
                    DynamicsItem(
                        key="$this.row_insert_shift.show",
                        expression=f"return $this.write_mode.value == '{WriteMode.INSERT.value}' && $this.write_type.value == '{WriteType.ROW.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "column_insert_shift",
                dynamics=[
                    DynamicsItem(
                        key="$this.column_insert_shift.show",
                        expression=f"return $this.write_mode.value == '{WriteMode.INSERT.value}' && $this.write_type.value == '{WriteType.COLUMN.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "append_position",
                dynamics=[
                    DynamicsItem(
                        key="$this.append_position.show",
                        expression=f"return $this.write_mode.value == '{WriteMode.APPEND.value}' && $this.write_type.value == '{WriteType.CELL.value}'",
                    )
                ],
            ),
        ],
        outputList=[],
    )
    def write_data(
        write_type: WriteType = WriteType.CELL,
        row: int = 1,
        col: str = "A",
        data=None,
        start_row: int = 1,
        start_col: str = "A",
        write_mode: WriteMode = WriteMode.OVERWRITE,
        cell_insert_shift: CellInsertShift = CellInsertShift.DOWN,
        row_insert_shift: RowInsertShift = RowInsertShift.DOWN,
        column_insert_shift: ColumnInsertShift = ColumnInsertShift.RIGHT,
        append_position: AppendShift = AppendShift.ROW,
    ):
        """
        테이블입력지정데이터
        """
        if data is None:
            raise DATAFRAME_EXPECTION(DATAFRAME_ERROR.format("데이터비워 둘 수 없습니다"), "입력데이터비워 둘 수 없습니다")

        if isinstance(data, str):
            try:
                # 시도를문자열파싱로 Python 문자량 (예목록)
                evaluated_data = ast.literal_eval(data)
                data = evaluated_data
            except (ValueError, SyntaxError):
                pass

        if write_type == WriteType.CELL:
            if not row or not col:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("입력셀필요지정행열"), "입력셀필요지정행열")
            col_index = col_to_index(col)
            if not isinstance(data, str):
                data = str(data)
            if write_mode == WriteMode.OVERWRITE:
                PyxlWrapper.write_cell(row=row, col=col_index, value=data)
            elif write_mode == WriteMode.APPEND:
                if append_position == AppendShift.ROW:
                    row_list = PyxlWrapper.read_row(row_index=row)
                    row_list.append(data)
                    PyxlWrapper.write_row(row_index=row, data=row_list)
                if append_position == AppendShift.COLUMN:
                    col_list = PyxlWrapper.read_column(col_index=col_index)
                    col_list.append(data)
                    PyxlWrapper.write_column(col_index=col_index, data=col_list)
            elif write_mode == WriteMode.INSERT:
                if cell_insert_shift == CellInsertShift.DOWN:
                    col_back = PyxlWrapper.read_column(col_index=col_index)[row - 1 :]
                    col_front = PyxlWrapper.read_column(col_index=col_index)[: row - 1]
                    new_col = col_front + [data] + col_back
                    PyxlWrapper.write_column(col_index=col_index, data=new_col)
                if cell_insert_shift == CellInsertShift.RIGHT:
                    row_back = PyxlWrapper.read_row(row_index=row)[col_index - 1 :]
                    row_front = PyxlWrapper.read_row(row_index=row)[: col_index - 1]
                    new_row = row_front + [data] + row_back
                    PyxlWrapper.write_row(row_index=row, data=new_row)
            return
        if write_type == WriteType.ROW:
            if not row:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("행비워 둘 수 없습니다"), "행비워 둘 수 없습니다")
            col_index = col_to_index(start_col)
            if not isinstance(data, list):
                data = [data]
            if write_mode == WriteMode.OVERWRITE:
                PyxlWrapper.write_row(row_index=row, data=data, start_col=col_index)
            elif write_mode == WriteMode.APPEND:
                # 행추가 입력행추가 입력
                existing_row = PyxlWrapper.read_row(row_index=row)
                new_row = existing_row + data
                PyxlWrapper.write_row(row_index=row, data=new_row)
            elif write_mode == WriteMode.INSERT:
                if row_insert_shift == RowInsertShift.DOWN:
                    row_index = row + 1
                    PyxlWrapper.insert_rows(idx=row_index, amount=1)
                    PyxlWrapper.write_row(row_index=row_index, data=data, start_col=col_index)
                if row_insert_shift == RowInsertShift.UP:
                    if row == 1:
                        PyxlWrapper.insert_rows(idx=1, amount=1)
                        PyxlWrapper.write_row(row_index=1, data=data, start_col=col_index)
                    else:
                        row_index = row - 1
                        PyxlWrapper.insert_rows(idx=row_index, amount=1)
                        PyxlWrapper.write_row(row_index=row_index, data=data, start_col=col_index)

        if write_type == WriteType.COLUMN:
            if not col:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("열비워 둘 수 없습니다"), "열비워 둘 수 없습니다")
            col_index = col_to_index(col)
            if not isinstance(data, list):
                data = [data]
            if write_mode == WriteMode.OVERWRITE:
                PyxlWrapper.write_column(col_index=col_index, data=data, start_row=start_row)
            elif write_mode == WriteMode.APPEND:
                # 열추가 입력열추가 입력
                existing_col = PyxlWrapper.read_column(col_index=col_index)
                new_col = existing_col + data
                PyxlWrapper.write_column(col_index=col_index, data=new_col)
            elif write_mode == WriteMode.INSERT:
                if column_insert_shift == ColumnInsertShift.LEFT:
                    col_index_new = col_index
                    PyxlWrapper.insert_cols(idx=col_index_new, amount=1)
                    PyxlWrapper.write_column(col_index=col_index_new, data=data, start_row=start_row)
                    PyxlHeadWrapper.insert_cols(idx=col_index_new, amount=1)
                    sync_data_table_head()
                if column_insert_shift == ColumnInsertShift.RIGHT:
                    col_index_new = col_index + 1
                    PyxlWrapper.insert_cols(idx=col_index_new, amount=1)
                    PyxlWrapper.write_column(col_index=col_index_new, data=data, start_row=start_row)
                    PyxlHeadWrapper.insert_cols(idx=col_index_new, amount=1)
                    sync_data_table_head()

        if write_type == WriteType.AREA:
            if not start_row or not start_col:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("입력필요지정시작 행열"), "입력필요지정시작 행열")
            if not isinstance(data, list):
                try:
                    # 시도를문자열파싱로목록
                    data = ast.literal_eval(data)
                except Exception:
                    data = [[data]]
            else:
                data = [row_data if isinstance(row_data, list) else [row_data] for row_data in data]

            start_col_index = col_to_index(start_col)
            for i, row_data in enumerate(data):
                PyxlWrapper.write_row(row_index=start_row + i, data=row_data, start_col=start_col_index)

    @staticmethod
    @atomicMg.atomic(
        "DataTable",
        inputList=[],
        outputList=[
            atomicMg.param("max_row", types="Int"),
        ],
    )
    def get_max_row() -> int:
        """
        데이터 테이블 읽기대행
        """
        return PyxlWrapper.get_max_row()

    @staticmethod
    @atomicMg.atomic(
        "DataTable",
        inputList=[],
        outputList=[
            atomicMg.param("max_column", types="Int"),
        ],
    )
    def get_max_column() -> int:
        """
        데이터 테이블 읽기대열
        """
        return PyxlWrapper.get_max_column()

    @staticmethod
    @validate_cell
    @atomicMg.atomic(
        "DataTable",
        inputList=[
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression=f"return ['{CopyType.ROW.value}', '{CopyType.CELL.value}'].includes($this.copy_type.value)",
                    )
                ],
            ),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression=f"return ['{CopyType.COLUMN.value}', '{CopyType.CELL.value}'].includes($this.copy_type.value)",
                    )
                ],
            ),
            atomicMg.param(
                "start_row",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_row.show",
                        expression=f"return $this.copy_type.value == '{CopyType.AREA.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "start_col",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_col.show",
                        expression=f"return $this.copy_type.value == '{CopyType.AREA.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "end_row",
                dynamics=[
                    DynamicsItem(
                        key="$this.end_row.show",
                        expression=f"return $this.copy_type.value == '{CopyType.AREA.value}'",
                    )
                ],
                required=False,
            ),
            atomicMg.param(
                "end_col",
                dynamics=[
                    DynamicsItem(
                        key="$this.end_col.show",
                        expression=f"return $this.copy_type.value == '{CopyType.AREA.value}'",
                    )
                ],
                required=False,
            ),
        ],
        outputList=[
            atomicMg.param(
                "copied_cell",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.copied_cell.show",
                        expression=f"return $this.copy_type.value == '{CopyType.CELL.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "copied_row",
                types="List",
                dynamics=[
                    DynamicsItem(
                        key="$this.copied_row.show",
                        expression=f"return $this.copy_type.value == '{CopyType.ROW.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "copied_column",
                types="List",
                dynamics=[
                    DynamicsItem(
                        key="$this.copied_column.show",
                        expression=f"return $this.copy_type.value == '{CopyType.COLUMN.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "copied_area",
                types="List",
                dynamics=[
                    DynamicsItem(
                        key="$this.copied_area.show",
                        expression=f"return $this.copy_type.value == '{CopyType.AREA.value}'",
                    )
                ],
            ),
        ],
    )
    def copy_data(
        copy_type: CopyType = CopyType.CELL,
        row: int = 1,
        col: str = "A",
        start_row: int = 1,
        start_col: str = "A",
        end_row: int = 0,
        end_col: str = "",
    ):
        """
        복사데이터, 복사지정셀, 행, 열, 의내용
        """

        if copy_type == CopyType.CELL and (not row or not col):
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("복사셀필요지정행열"), "복사셀필요지정행열")
        if copy_type == CopyType.ROW and not row:
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("복사행필요지정행"), "복사행필요지정행")
        if copy_type == CopyType.COLUMN and not col:
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("복사열필요지정열"), "복사열필요지정열")
        if copy_type == CopyType.AREA:
            if not start_row or not start_col:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("복사필요지정시작 행열"), "복사필요지정시작 행열")

        # 입력까지시스템잘라내기
        import pyperclip

        _clipboard = DataTable.read_data(
            read_type=ReadType(copy_type.value),
            row=row,
            col=col,
            start_row=start_row,
            start_col=start_col,
            end_row=end_row,
            end_col=end_col,
        )

        pyperclip.copy(str(_clipboard))
        return _clipboard

    @staticmethod
    @validate_cell
    @auto_save
    @atomicMg.atomic(
        "DataTable",
        inputList=[
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression=f"return ['{PasteType.CELL.value}', '{PasteType.ROW.value}'].includes($this.paste_type.value)",
                    )
                ],
            ),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression=f"return ['{PasteType.CELL.value}', '{PasteType.COLUMN.value}'].includes($this.paste_type.value)",
                    )
                ],
            ),
            atomicMg.param(
                "start_row",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_row.show",
                        expression=f"return ['{PasteType.AREA.value}', '{PasteType.COLUMN.value}'].includes($this.paste_type.value)",
                    )
                ],
            ),
            atomicMg.param(
                "start_col",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_col.show",
                        expression=f"return ['{PasteType.AREA.value}', '{PasteType.ROW.value}'].includes($this.paste_type.value)",
                    )
                ],
            ),
        ],
        outputList=[],
    )
    def paste_data(
        paste_type: PasteType = PasteType.CELL,
        row: int = 1,
        col: str = "A",
        start_row: int = 1,
        start_col: str = "A",
    ):
        """
        붙여넣기데이터, 를복사의데이터붙여넣기까지지정셀, 행, 열, 
        """
        import pyperclip

        _clipboard = pyperclip.paste()

        if paste_type == PasteType.CELL and (not row or not col):
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("붙여넣기셀필요지정행열"), "붙여넣기셀필요지정행열")
        if paste_type == PasteType.ROW and not row:
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("붙여넣기행필요지정행"), "붙여넣기행필요지정행")
        if paste_type == PasteType.COLUMN and not col:
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("붙여넣기열필요지정열"), "붙여넣기열필요지정열")
        if paste_type == PasteType.AREA and (not start_row or not start_col):
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("붙여넣기필요지정시작 행열"), "붙여넣기필요지정시작 행열")

        if paste_type != PasteType.CELL:
            try:
                # 사용 ast.literal_eval  eval
                _clipboard = ast.literal_eval(_clipboard)
            except (ValueError, SyntaxError):
                pass

        DataTable.write_data(
            write_type=WriteType(paste_type.value),
            row=row,
            col=col,
            data=_clipboard,
            start_row=start_row,
            start_col=start_col,
            write_mode=WriteMode.OVERWRITE,  # 붙여넣기시로덮어쓰기입력
        )

    @staticmethod
    @validate_cell
    @auto_save
    @atomicMg.atomic(
        "DataTable",
        inputList=[
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression=f"return ['{DeleteType.CELL.value}', '{DeleteType.ROW.value}'].includes($this.delete_type.value)",
                    )
                ],
            ),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression=f"return ['{DeleteType.CELL.value}', '{DeleteType.COLUMN.value}'].includes($this.delete_type.value)",
                    )
                ],
            ),
            atomicMg.param(
                "start_row",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_row.show",
                        expression=f"return $this.delete_type.value == '{DeleteType.AREA.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "start_col",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_col.show",
                        expression=f"return $this.delete_type.value == '{DeleteType.AREA.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "end_row",
                types="int",
                dynamics=[
                    DynamicsItem(
                        key="$this.end_row.show",
                        expression=f"return $this.delete_type.value == '{DeleteType.AREA.value}'",
                    )
                ],
                required=False,
            ),
            atomicMg.param(
                "end_col",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.end_col.show",
                        expression=f"return $this.delete_type.value == '{DeleteType.AREA.value}'",
                    )
                ],
                required=False,
            ),
            atomicMg.param(
                "delete_cell_move",
                dynamics=[
                    DynamicsItem(
                        key="$this.delete_cell_move.show",
                        expression=f"return $this.delete_type.value == '{DeleteType.CELL.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "delete_col_move",
                dynamics=[
                    DynamicsItem(
                        key="$this.delete_col_move.show",
                        expression=f"return $this.delete_type.value == '{DeleteType.COLUMN.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "delete_row_move",
                dynamics=[
                    DynamicsItem(
                        key="$this.delete_row_move.show",
                        expression=f"return $this.delete_type.value == '{DeleteType.ROW.value}'",
                    )
                ],
            ),
        ],
        outputList=[],
    )
    def delete_data(
        delete_type: DeleteType = DeleteType.CELL,
        row: int = 1,
        col: str = "A",
        start_row: int = 1,
        start_col: str = "A",
        end_row: int = 0,
        end_col: str = "",
        delete_cell_move: DeleteCellMove = DeleteCellMove.UP,
        delete_col_move: bool = True,
        delete_row_move: bool = True,
    ):
        """
        데이터 테이블 삭제내용
        """
        if delete_type == DeleteType.CELL:
            if not row or not col:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("삭제셀필요지정행열"), "삭제셀필요지정행열")
            col_index = col_to_index(col)
            PyxlWrapper.delete_cell(
                row=row,
                col=col_index,
                move_direction=delete_cell_move.value,
            )
        if delete_type == DeleteType.ROW:
            if not row:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("삭제행필요지정행"), "삭제행필요지정행")
            if delete_row_move:
                PyxlWrapper.delete_rows(idx=row, amount=1)
            else:
                PyxlWrapper.empty_row(
                    row_index=row,
                )
        if delete_type == DeleteType.COLUMN:
            if not col:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("삭제열필요지정열"), "삭제열필요지정열")
            col_index = col_to_index(col)
            if delete_col_move:
                PyxlWrapper.delete_cols(idx=col_index, amount=1)
                PyxlHeadWrapper.delete_cols(idx=col_index, amount=1)
                sync_data_table_head()
            else:
                PyxlWrapper.empty_column(
                    col_index=col_index,
                )
        if delete_type == DeleteType.AREA:
            if not start_row or not start_col:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("삭제필요지정시작 행열"), "삭제필요지정시작 행열")
            if end_col is None or end_col in {"", "0"}:
                end_col = index_to_col(PyxlWrapper.get_max_column() - 1)
            if end_row is None or end_row in {"", "0", 0}:
                end_row = PyxlWrapper.get_max_row()
            validate_col(col=end_col)
            validate_row(row=end_row)
            validate_end_col(start_col=start_col, end_col=end_col)
            validate_end_row(start_row=start_row, end_row=end_row)
            col_range = f"{start_col}{start_row}:{end_col}{end_row}"
            PyxlWrapper.clear_range(range_str=col_range)

    @staticmethod
    @validate_cell
    @atomicMg.atomic(
        "DataTable",
        noAdvanced=True,
        inputList=[
            atomicMg.param(
                "loop_type",
                formType=AtomicFormTypeMeta(AtomicFormType.SELECT.value),
            ),
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression=f"return $this.loop_type.value == '{LoopType.ROW.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression=f"return $this.loop_type.value == '{LoopType.COLUMN.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "start_row",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_row.show",
                        expression=f"return $this.loop_type.value == '{LoopType.AREA.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "start_col",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_col.show",
                        expression=f"return $this.loop_type.value == '{LoopType.AREA.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "end_row",
                dynamics=[
                    DynamicsItem(
                        key="$this.end_row.show",
                        expression=f"return $this.loop_type.value == '{LoopType.AREA.value}'",
                    )
                ],
                required=False,
            ),
            atomicMg.param(
                "end_col",
                dynamics=[
                    DynamicsItem(
                        key="$this.end_col.show",
                        expression=f"return $this.loop_type.value == '{LoopType.AREA.value}'",
                    )
                ],
                required=False,
            ),
        ],
        outputList=[
            atomicMg.param("index", types="Int"),
            atomicMg.param("value", types="Any"),
        ],
    )
    def loop_data_table(
        loop_type: LoopType = LoopType.ROW,
        row: int = 1,
        col: str = "A",
        start_row: int = 1,
        start_col: str = "A",
        end_row: int = 0,
        end_col: str = "",
    ):
        """
        데이터테이블내용
        """
        if loop_type == LoopType.ROW and not row:
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("행필요지정행"), "행필요지정행")
        if loop_type == LoopType.COLUMN and not col:
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("열필요지정열"), "열필요지정열")
        if loop_type == LoopType.AREA:
            if not start_row or not start_col:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("필요지정시작 행열"), "필요지정시작 행열")

        list_data = DataTable.read_data(
            read_type=ReadType(loop_type.value),
            row=row,
            col=col,
            start_row=start_row,
            start_col=start_col,
            end_row=end_row,
            end_col=end_col,
        )

        if not list_data:
            list_data = []
        if not isinstance(list_data, list):
            list_data = [list_data]

        def table_generator():
            list_length = len(list_data)
            for i in range(list_length):
                yield i, list_data[i]

        return table_generator()

    @staticmethod
    @validate_cell
    @auto_save
    @atomicMg.atomic(
        "DataTable",
        inputList=[
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression=f"return $this.insert_type.value == '{InsertType.ROW.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression=f"return $this.insert_type.value == '{InsertType.COLUMN.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "row_insert_shift",
                dynamics=[
                    DynamicsItem(
                        key="$this.row_insert_shift.show",
                        expression=f"return $this.insert_type.value == '{InsertType.ROW.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "column_insert_shift",
                dynamics=[
                    DynamicsItem(
                        key="$this.column_insert_shift.show",
                        expression=f"return $this.insert_type.value == '{InsertType.COLUMN.value}'",
                    )
                ],
            ),
        ],
        outputList=[],
    )
    def insert_row_column(
        insert_type: InsertType = InsertType.ROW,
        row: int = 1,
        col: str = "A",
        amount: int = 1,
        row_insert_shift: RowInsertShift = RowInsertShift.DOWN,
        column_insert_shift: ColumnInsertShift = ColumnInsertShift.RIGHT,
    ):
        """
        삽입행또는열
        """
        if not amount:
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("삽입수비워 둘 수 없습니다"), "삽입수비워 둘 수 없습니다")
        if amount < 0:
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("삽입수대0"), "삽입수대0")
        if amount == 0:
            return
        if insert_type == InsertType.ROW:
            if not row:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("행비워 둘 수 없습니다"), "행비워 둘 수 없습니다")
            if row_insert_shift == RowInsertShift.UP:
                if row == 1:
                    PyxlWrapper.insert_rows(idx=1, amount=amount)
                else:
                    PyxlWrapper.insert_rows(idx=row - 1, amount=amount)
            if row_insert_shift == RowInsertShift.DOWN:
                PyxlWrapper.insert_rows(idx=row + 1, amount=amount)
        if insert_type == InsertType.COLUMN:
            if not col:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("열비워 둘 수 없습니다"), "열비워 둘 수 없습니다")
            col_index = col_to_index(col)
            if column_insert_shift == ColumnInsertShift.LEFT:
                pass
            if column_insert_shift == ColumnInsertShift.RIGHT:
                col_index += 1
            PyxlWrapper.insert_cols(idx=col_index, amount=amount)
            PyxlHeadWrapper.insert_cols(idx=col_index, amount=amount)
            sync_data_table_head()

    @staticmethod
    @validate_cell
    @auto_save
    @atomicMg.atomic(
        "DataTable",
        inputList=[],
        outputList=[],
    )
    def insert_formula(
        row: int = 1,
        col: str = "A",
        formula: str = "",
    ):
        """
        삽입방식까지지정셀
        """
        if not row:
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("행비워 둘 수 없습니다"), "행비워 둘 수 없습니다")
        if not col:
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("열비워 둘 수 없습니다"), "열비워 둘 수 없습니다")
        if not formula:
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("방식비워 둘 수 없습니다"), "방식비워 둘 수 없습니다")
        validate_formula(formula)
        col_index = col_to_index(col)
        PyxlWrapper.write_cell(row=row, col=col_index, value=formula)

    @staticmethod
    @validate_cell
    @atomicMg.atomic(
        "DataTable",
        inputList=[],
        outputList=[],
    )
    def set_column_title(
        col: str = "A",
        title: str = "",
    ):
        """
        열정보
        """
        if not col:
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("열비워 둘 수 없습니다"), "열비워 둘 수 없습니다")
        if not title:
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("열정보비워 둘 수 없습니다"), "열정보비워 둘 수 없습니다")
        col_index = col_to_index(col)
        PyxlHeadWrapper.write_cell(row=1, col=col_index, value=title)
        sync_data_table_head()

    @staticmethod
    @validate_cell
    @atomicMg.atomic(
        "DataTable",
        inputList=[],
        outputList=[
            atomicMg.param(
                "column_title",
                types="Str",
            ),
        ],
    )
    def get_column_title(
        col: str = "A",
    ) -> str:
        """
        가져오기열정보
        """
        if not col:
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("열비워 둘 수 없습니다"), "열비워 둘 수 없습니다")
        col_index = col_to_index(col)
        return str(PyxlHeadWrapper.read_cell(row=1, col=col_index))

    @staticmethod
    @auto_save
    @validate_cell
    @atomicMg.atomic(
        "DataTable",
        inputList=[
            atomicMg.param(
                "find_type",
                formType=AtomicFormTypeMeta(AtomicFormType.SELECT.value),
            ),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression=f"return $this.find_type.value == '{FindType.COLUMN.value}'",
                    )
                ],
            ),
            atomicMg.param("find_value", required=True),
            atomicMg.param(
                "replace_value",
                dynamics=[
                    DynamicsItem(
                        key="$this.replace_value.show",
                        expression="return $this.is_replace.value == true",
                    )
                ],
                required=False,
            ),
        ],
        outputList=[
            atomicMg.param(
                "find_data_positions",
                types="List",
            ),
        ],
    )
    def find_and_replace(
        find_type: FindType = FindType.TABLE,
        col: str = "A",
        find_value: str = "",
        is_case_sensitive: bool = True,
        is_replace: bool = True,
        replace_value: str = "",
    ) -> list:
        """
        조회데이터테이블중의지정내용, 반환조회까지의데이터위치목록[(row, col), ...]
        """
        if not find_value:
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("조회내용비워 둘 수 없습니다"), "조회내용비워 둘 수 없습니다")

        find_data_positions = []
        if find_type == FindType.COLUMN:
            if not col:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("열비워 둘 수 없습니다"), "열비워 둘 수 없습니다")
            col_index = col_to_index(col)
            column_data = PyxlWrapper.read_column(col_index=col_index)
            for r, cell_value in enumerate(column_data, start=1):
                if cell_value is not None:
                    cell_str = str(cell_value)
                    if is_case_sensitive:
                        if find_value in cell_str:
                            find_data_positions.append((r, col))
                            if is_replace:
                                new_value = cell_str.replace(find_value, replace_value)
                                PyxlWrapper.write_cell(row=r, col=col_index, value=new_value)
                    else:
                        if find_value.lower() in cell_str.lower():
                            find_data_positions.append((r, col))
                            if is_replace:
                                new_value = re.sub(re.escape(find_value), replace_value, cell_str, flags=re.IGNORECASE)
                                PyxlWrapper.write_cell(row=r, col=col_index, value=new_value)
        else:
            max_row = PyxlWrapper.get_max_row()
            max_col = PyxlWrapper.get_max_column()
            for r in range(1, max_row + 1):
                for c in range(1, max_col + 1):
                    cell_value = PyxlWrapper.read_cell(row=r, col=c)
                    if cell_value is not None:
                        cell_str = str(cell_value)
                        found = False
                        if is_case_sensitive:
                            if find_value in cell_str:
                                found = True
                        else:
                            if find_value.lower() in cell_str.lower():
                                found = True

                        if found:
                            find_data_positions.append((r, index_to_col(c - 1)))
                            if is_replace:
                                if is_case_sensitive:
                                    new_value = cell_str.replace(find_value, replace_value)
                                else:
                                    new_value = re.sub(
                                        re.escape(find_value), replace_value, cell_str, flags=re.IGNORECASE
                                    )
                                PyxlWrapper.write_cell(row=r, col=c, value=new_value)
        return find_data_positions

    @staticmethod
    @validate_cell
    @atomicMg.atomic(
        "DataTable",
        inputList=[
            atomicMg.param("filter_type", formType=AtomicFormTypeMeta(AtomicFormType.SELECT.value)),
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression=f"return $this.filter_type.value == '{FilterType.ROW.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression=f"return $this.filter_type.value == '{FilterType.COLUMN.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "condition_value",
                dynamics=[
                    DynamicsItem(
                        key="$this.condition_value.show",
                        expression=f"return !['{ConditionType.DATE_AFTER.value}', '{ConditionType.DATE_BEFORE.value}', '{ConditionType.DATE_BETWEEN.value}'].includes($this.condition_type.value)",
                    )
                ],
            ),
            atomicMg.param(
                "date_value",
                dynamics=[
                    DynamicsItem(
                        key="$this.date_value.show",
                        expression=f"return ['{ConditionType.DATE_AFTER.value}', '{ConditionType.DATE_BEFORE.value}'].includes($this.condition_type.value)",
                    )
                ],
            ),
            atomicMg.param(
                "date_range",
                dynamics=[
                    DynamicsItem(
                        key="$this.date_range.show",
                        expression=f"return $this.condition_type.value == '{ConditionType.DATE_BETWEEN.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "is_case_sensitive",
                dynamics=[
                    DynamicsItem(
                        key="$this.is_case_sensitive.show",
                        expression=f"return ['{ConditionType.EQUALS.value}', '{ConditionType.NOT_EQUALS.value}', '{ConditionType.CONTAINS.value}', '{ConditionType.NOT_CONTAINS.value}', '{ConditionType.STARTS_WITH.value}', '{ConditionType.ENDS_WITH.value}'].includes($this.condition_type.value)",
                    )
                ],
            ),
        ],
        outputList=[
            atomicMg.param(
                "data_filtered",
                types="List",
            ),
        ],
    )
    def filter_data_table(
        filter_type: FilterType = FilterType.COLUMN,
        row: int = 1,
        col: str = "A",
        condition_type: ConditionType = ConditionType.EQUALS,
        condition_value: str = "",
        date_value: str = "",
        date_range: str = "",
        is_case_sensitive: bool = True,
        is_save_filtered: bool = False,
    ) -> list:
        """
        필터링데이터테이블내용
        """
        if condition_type == ConditionType.DATE_BETWEEN:
            if not date_range:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("날짜비워 둘 수 없습니다"), "날짜비워 둘 수 없습니다")
            elif len(date_range.split(",")) != 2:
                raise DATAFRAME_EXPECTION(
                    PARAMS_ERROR.format("날짜형식오류, 정상형식예: 2023-01-01,2023-12-31"),
                    "날짜형식오류, 정상형식예: 2023-01-01,2023-12-31",
                )
        col_index = col_to_index(col)
        data = []
        if filter_type == FilterType.COLUMN:
            if not col:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("열비워 둘 수 없습니다"), "열비워 둘 수 없습니다")
            data = PyxlWrapper.read_column(col_index=col_index)
        elif filter_type == FilterType.ROW:
            if not row:
                raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("행비워 둘 수 없습니다"), "행비워 둘 수 없습니다")
            data = PyxlWrapper.read_row(row_index=row)
        else:
            data = PyxlWrapper.read_effective_area()

        data_filtered = filter_data(
            data=data,
            filter_type=filter_type,
            condition_type=condition_type,
            condition_value=condition_value,
            date_value=date_value,
            date_range=date_range,
            is_case_sensitive=is_case_sensitive,
        )

        if is_save_filtered:
            if filter_type == FilterType.COLUMN:
                PyxlWrapper.empty_column(col_index=col_index)
                DataTable.write_data(
                    write_type=WriteType.COLUMN,
                    col=col,
                    start_row=1,
                    data=data_filtered,
                    write_mode=WriteMode.OVERWRITE,
                )
            elif filter_type == FilterType.ROW:
                PyxlWrapper.empty_row(row_index=row)
                DataTable.write_data(
                    write_type=WriteType.ROW,
                    row=row,
                    data=data_filtered,
                    write_mode=WriteMode.OVERWRITE,
                )
            else:
                PyxlWrapper.clear_range(
                    range_str=f"A1:{index_to_col(PyxlWrapper.get_max_column() - 1)}{PyxlWrapper.get_max_row()}",
                )
                DataTable.write_data(
                    write_type=WriteType.AREA,
                    start_row=1,
                    start_col="A",
                    data=data_filtered,
                    write_mode=WriteMode.OVERWRITE,
                )

        return data_filtered

    @staticmethod
    @auto_save
    @atomicMg.atomic(
        "DataTable",
        inputList=[
            atomicMg.param(
                "import_file_path",
                types="File",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={
                        "file_type": "file",
                        "filters": [".xlsx", ".xls", ".csv"],
                    },
                ),
            ),
            atomicMg.param(
                "sheet_name",
                types="Str",
                required=False,
            ),
        ],
        outputList=[],
    )
    def import_data_table_from_file(
        import_file_path: str,
        sheet_name: str = None,  # type: ignore
    ):
        """
        에서지정파일데이터 테이블 읽기
        """
        if not import_file_path:
            raise DATAFRAME_EXPECTION(IMPORT_FILE_ERROR_FORMAT.format("가져오기파일 경로비워 둘 수 없습니다"), "가져오기파일 경로비워 둘 수 없습니다")
        if os.path.splitext(import_file_path)[1].lower() not in [".xlsx", ".xls", ".csv"]:
            raise DATAFRAME_EXPECTION(
                IMPORT_FILE_ERROR_FORMAT.format(""),
                "지원가져오기Excel(.xlsx, .xls)및CSV(.csv)파일",
            )
        if not os.path.exists(import_file_path):
            raise DATAFRAME_EXPECTION(
                IMPORT_FILE_ERROR_FORMAT.format(""),
                f"파일찾을 수 없습니다: {import_file_path}",
            )

        PyxlWrapper.fill_data_table_by_import_file(
            import_file_path=import_file_path,
            sheet_name=sheet_name,
        )

    @staticmethod
    @atomicMg.atomic(
        "DataTable",
        inputList=[
            atomicMg.param(
                "export_dest_path",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"file_type": "folder"},
                ),
            ),
            atomicMg.param(
                "export_file_name",
                required=False,
            ),
        ],
        outputList=[
            atomicMg.param(
                "export_file_path",
                types="Str",
            ),
        ],
    )
    def export_data_table_to_file(
        export_dest_path: str,
        export_file_name: str = "data_table",
        export_file_type: ExportFileType = ExportFileType.XLSX,
        is_overwrite: bool = True,
    ) -> str:
        """
        내보내기데이터테이블까지지정파일
        """
        if not export_dest_path:
            raise DATAFRAME_EXPECTION(PARAMS_ERROR.format("내보내기폴더 경로비워 둘 수 없습니다"), "내보내기폴더 경로비워 둘 수 없습니다")
        if not export_file_name:
            export_file_name = "data_table"
        if not os.path.exists(export_dest_path):
            raise DATAFRAME_EXPECTION(
                PARAMS_ERROR.format(f"내보내기폴더 경로찾을 수 없습니다: {export_dest_path}"),
                "내보내기폴더 경로찾을 수 없습니다",
            )
        if not is_overwrite:
            export_file_name = export_file_name + "_" + datetime.now().strftime("%Y%m%d%H%M%S")

        file_path = os.path.join(export_dest_path, f"{export_file_name}.{export_file_type.value}")
        if export_file_type == ExportFileType.JSON:
            data = DataTable.read_data(
                read_type=ReadType.AREA,
                start_row=1,
                start_col="A",
                end_row=PyxlWrapper.get_max_row(),
                end_col=index_to_col(PyxlWrapper.get_max_column() - 1),
            )
            with open(file_path, "w", encoding="utf-8") as jsonfile:
                json.dump(data, jsonfile, indent=4)
        else:
            PyxlWrapper.export_to_file(file_path=file_path)
        return file_path