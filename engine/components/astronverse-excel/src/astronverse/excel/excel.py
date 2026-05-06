import ast
import time
from itertools import zip_longest

import win32clipboard as cv
from astronverse.actionlib import AtomicFormType, AtomicFormTypeMeta, AtomicLevel, DynamicsItem
from astronverse.actionlib.atomic import atomicMg
from astronverse.actionlib.types import PATH
from astronverse.excel import *
from astronverse.excel.core_win.application import Application
from astronverse.excel.core_win.range import Range
from astronverse.excel.core_win.worksheet import Worksheet
from astronverse.excel.excel_obj import ExcelObj
from astronverse.excel.utils import *


class Excel:
    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [".xlsx", ".xls"], "file_type": "file"},
                ),
            ),
            atomicMg.param("password", level=AtomicLevel.ADVANCED, required=False),
        ],
        outputList=[
            atomicMg.param("open_excel_obj", types="ExcelObj"),
        ],
    )
    def open_excel(
        file_path: PATH = "",
        default_application: ApplicationType = ApplicationType.DEFAULT,
        visible_flag: bool = True,
        password: str = "",
        update_links: bool = True,
    ) -> ExcelObj:
        if not os.path.exists(file_path):
            raise Exception("의파일 경로있음오류, 입력하세요정상의경로!")
        else:
            file_path = os.path.abspath(file_path)
        application = Application.init_app(
            default_application=default_application,
            visible_flag=visible_flag,
            retry=2,
            retry_delay=1,
            prefer_existing=False,
        )
        excel = Application.open_workbook(
            application=application, file_path=file_path, password=password, update_links=update_links
        )
        return excel

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[],
        outputList=[
            atomicMg.param("get_excel_obj", types="ExcelObj"),
        ],
    )
    def get_excel(file_name) -> ExcelObj:
        excel_flag, excel_pid, wps_flag, wps_pid = get_excel_processes()
        if not excel_flag and not wps_flag:
            raise Exception("감지하지 못한 wps또는office열기!")

        keys = []
        if wps_flag:
            keys.append(ApplicationType.WPS)
        if excel_flag:
            keys.append(ApplicationType.EXCEL)

        res = []
        for key in keys:
            try:
                application = Application.init_app(
                    default_application=key, visible_flag=None, retry=0, retry_delay=0, prefer_existing=True
                )
                excel_obj = Application.get_existing_workbook(application, match_name=file_name)
                if excel_obj:
                    res.append(excel_obj)
            except Exception as e:
                pass

        if len(res) == 1:
            return res[0]
        elif len(res) == 2:
            raise Exception("감지까지객체: {}에서WPS/Office중열기,필요닫기 중일개".format(file_name))
        else:
            raise Exception("찾을 수 없습니다완료열기의Excel파일:{0}".format(file_name))

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value, params={"filters": [], "file_type": "folder"}
                ),
            ),
            atomicMg.param("password", required=False, level=AtomicLevel.ADVANCED),
        ],
        outputList=[
            atomicMg.param("create_excel_obj", types="ExcelObj"),
            atomicMg.param("excel_path", types="Str"),
        ],
    )
    def create_excel(
        file_path: str = "",
        file_name: str = "",
        default_application: ApplicationType = ApplicationType.EXCEL,
        visible_flag: bool = True,
        exist_handle_type: FileExistenceType = FileExistenceType.RENAME,
        password: str = "",
    ) -> tuple[ExcelObj, str]:
        if not os.path.exists(file_path):
            raise Exception("의파일 경로있음오류, 입력하세요정상의경로!")
        else:
            file_path = os.path.abspath(file_path)
        if file_name:
            file_name += ".xlsx"
        else:
            file_name = "새생성Excel문서.xlsx"

        new_file_path = os.path.join(file_path, file_name)
        new_file_path = resolve_file_path(new_file_path, exist_handle_type)

        application = Application.init_app(
            default_application=default_application,
            visible_flag=visible_flag,
            retry=2,
            retry_delay=1,
            prefer_existing=True,
        )
        excel = Application.create_workbook(application=application, file_path=new_file_path, password=password)
        return excel, new_file_path

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param(
                "file_path",
                dynamics=[
                    DynamicsItem(
                        key="$this.file_path.show",
                        expression="return $this.save_type.value == '{}'".format(SaveType.SAVE_AS.value),
                    )
                ],
                required=False,
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value, params={"filters": [], "file_type": "folder"}
                ),
            ),
            atomicMg.param(
                "file_name",
                dynamics=[
                    DynamicsItem(
                        key="$this.file_name.show",
                        expression="return $this.save_type.value == '{}'".format(SaveType.SAVE_AS.value),
                    )
                ],
                required=False,
            ),
            atomicMg.param(
                "exist_handle_type",
                dynamics=[
                    DynamicsItem(
                        key="$this.exist_handle_type.show",
                        expression="return $this.save_type.value == '{}'".format(SaveType.SAVE_AS.value),
                    )
                ],
            ),
        ],
        outputList=[],
    )
    def save_excel(
        excel: ExcelObj,
        save_type: SaveType = SaveType.SAVE,
        file_path: str = "",
        file_name: str = "",
        exist_handle_type: FileExistenceType = FileExistenceType.RENAME,
        close_flag: bool = False,
    ):
        if save_type == SaveType.SAVE_AS and file_path:
            file_suffix = "." + excel.get_name().split(".")[-1]
            if not file_name:
                file_name = excel.get_name().split(".")[0]
            dst_file = os.path.join(file_path, file_name + file_suffix)
            new_file_path = resolve_file_path(dst_file, exist_handle_type)
            Application.save_workbook(excel_obj=excel, file_path=new_file_path)
        elif save_type == SaveType.SAVE:
            Application.save_workbook(excel_obj=excel)
        else:
            # 아니오작업관리 SaveType.SAVE_AS 있음 file_path 또는예 SaveType.ABORT
            pass
        if close_flag:
            Application.close_workbook(excel_obj=excel, save_changes=False)

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param(
                "excel",
                dynamics=[
                    DynamicsItem(
                        key="$this.excel.show",
                        expression="return $this.close_range_flag.value == '{}'".format(CloseRangeType.ONE.value),
                    )
                ],
            ),
            atomicMg.param(
                "pkill_flag",
                dynamics=[
                    DynamicsItem(
                        key="$this.pkill_flag.show",
                        expression="return $this.close_range_flag.value == '{}'".format(CloseRangeType.ALL.value),
                    )
                ],
            ),
            atomicMg.param(
                "save_type_one",
                dynamics=[
                    DynamicsItem(
                        key="$this.save_type_one.show",
                        expression="return $this.close_range_flag.value == '{}'".format(CloseRangeType.ONE.value),
                    )
                ],
            ),
            atomicMg.param(
                "save_type_all",
                dynamics=[
                    DynamicsItem(
                        key="$this.save_type_all.show",
                        expression="return $this.close_range_flag.value == '{}'".format(CloseRangeType.ALL.value),
                    )
                ],
            ),
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value, params={"filters": [], "file_type": "folder"}
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.file_path.show",
                        expression="return $this.save_type_one.value == '{}' && $this.close_range_flag.value == '{}'".format(
                            SaveType.SAVE_AS.value, CloseRangeType.ONE.value
                        ),
                    )
                ],
            ),
            atomicMg.param(
                "file_name",
                dynamics=[
                    DynamicsItem(
                        key="$this.file_name.show",
                        expression="return $this.save_type_one.value == '{}'".format(SaveType.SAVE_AS.value),
                    )
                ],
            ),
            atomicMg.param(
                "exist_handle_type",
                dynamics=[
                    DynamicsItem(
                        key="$this.exist_handle_type.show",
                        expression="return $this.close_range_flag.value == '{}'".format(CloseRangeType.ONE.value),
                    )
                ],
            ),
        ],
        outputList=[],
    )
    def close_excel(
        close_range_flag: CloseRangeType = CloseRangeType.ONE,
        excel: ExcelObj = None,
        save_type_one: SaveType = SaveType.SAVE,
        save_type_all: SaveType_ALL = SaveType_ALL.SAVE,
        file_path: str = "",
        file_name: str = "",
        exist_handle_type: FileExistenceType = FileExistenceType.RENAME,
        pkill_flag: bool = False,
    ):
        if close_range_flag == CloseRangeType.ALL:
            save_changes = False
            if save_type_all == SaveType_ALL.SAVE:
                save_changes = True

            excel_flag, excel_pid, wps_flag, wps_pid = get_excel_processes()
            if wps_flag:
                Application.quit_app(default_application=ApplicationType.WPS, save_changes=save_changes)
            if excel_flag:
                Application.quit_app(default_application=ApplicationType.EXCEL, save_changes=save_changes)
            if pkill_flag:
                if excel_pid:
                    psutil.Process(excel_pid).kill()
                if wps_pid:
                    psutil.Process(wps_pid).kill()
        else:
            if not excel:
                raise Exception("문서찾을 수 없습니다, 요청 열기문서!")
            Excel.save_excel(
                excel=excel,
                save_type=save_type_one,
                file_path=file_path,
                file_name=file_name,
                exist_handle_type=exist_handle_type,
                close_flag=True,
            )

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("start_row", required=False),
            atomicMg.param("start_col", required=False),
            atomicMg.param("sheet_name", required=False),
            atomicMg.param(
                "edit_type",
                dynamics=[
                    DynamicsItem(
                        key="$this.edit_type.show",
                        expression="return $this.edit_range.value == '{}' || $this.edit_range.value == '{}'".format(
                            ReadRangeType.ROW.value, ReadRangeType.COLUMN.value
                        ),
                    )
                ],
                required=False,
            ),
        ],
        outputList=[],
    )
    def edit_excel(
        excel: ExcelObj,
        edit_range: EditRangeType = EditRangeType.ROW,
        sheet_name: str = "",
        start_col: str = "A",
        start_row: str = "1",
        value: str = "",
        edit_type: EditType = EditType.OVERWRITE,
    ):
        if isinstance(value, str) and value.startswith("[") and value.endswith("]"):
            try:
                value = ast.literal_eval(value)
            except Exception as e:
                raise Exception("의목록형식있음오류")

        worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)
        used_range = Worksheet.get_worksheet_used_range(worksheet)
        _, _, end_row, end_col, _ = used_range

        start_col_num = handle_column_input(start_col, end_col)
        start_row_num = handle_row_input(start_row, end_row)

        if edit_range == EditRangeType.ROW:
            if not isinstance(value, list):
                raise Exception("내용의목록형식있음오류")
            first_col = end_col + 1 if edit_type == EditType.APPEND else start_col_num
            for offset, val in enumerate(value):
                r_obj = Worksheet.get_cell(worksheet, start_row_num, first_col + offset)
                Range.set_range_data(r_obj, val)
        elif edit_range == EditRangeType.COLUMN:
            if not isinstance(value, list):
                raise Exception("내용의목록형식있음오류")
            first_row = end_row + 1 if edit_type == EditType.APPEND else start_row_num
            for offset, val in enumerate(value):
                r_obj = Worksheet.get_cell(worksheet, first_row + offset, start_col_num)
                Range.set_range_data(r_obj, val)
        elif edit_range == EditRangeType.AREA:
            if not isinstance(value, list):
                raise Exception("내용의목록형식있음오류")
            if not isinstance(value[0], list):
                raise Exception("내용의목록형식있음오류")
            first_col = end_col + 1 if edit_type == EditType.APPEND else start_col_num
            first_row = end_row + 1 if edit_type == EditType.APPEND else start_row_num
            for row in value:
                for offset, val in enumerate(row):
                    r_obj = Worksheet.get_cell(worksheet, first_row, first_col + offset)
                    Range.set_range_data(r_obj, val)
                first_row += 1  # 일행, 아래
        elif edit_range == EditRangeType.CELL:
            r_obj = Worksheet.get_cell(worksheet, start_row_num, start_col_num)
            Range.set_range_data(r_obj, value)

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param(
                "cell",
                dynamics=[
                    DynamicsItem(
                        key="$this.cell.show",
                        expression="return $this.read_range.value == '{}'".format(ReadRangeType.CELL.value),
                    )
                ],
            ),
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression="return $this.read_range.value == '{}'".format(ReadRangeType.ROW.value),
                    )
                ],
            ),
            atomicMg.param(
                "column",
                dynamics=[
                    DynamicsItem(
                        key="$this.column.show",
                        expression="return $this.read_range.value == '{}'".format(ReadRangeType.COLUMN.value),
                    )
                ],
            ),
            atomicMg.param(
                "start_row",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_row.show",
                        expression="return $this.read_range.value == '{}'".format(ReadRangeType.AREA.value),
                    )
                ],
            ),
            atomicMg.param(
                "end_row",
                dynamics=[
                    DynamicsItem(
                        key="$this.end_row.show",
                        expression="return $this.read_range.value == '{}'".format(ReadRangeType.AREA.value),
                    )
                ],
            ),
            atomicMg.param(
                "start_col",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_col.show",
                        expression="return $this.read_range.value == '{}'".format(ReadRangeType.AREA.value),
                    )
                ],
            ),
            atomicMg.param(
                "end_col",
                dynamics=[
                    DynamicsItem(
                        key="$this.end_col.show",
                        expression="return $this.read_range.value == '{}'".format(ReadRangeType.AREA.value),
                    )
                ],
            ),
            atomicMg.param("sheet_name", required=False),
        ],
        outputList=[
            atomicMg.param("read_excel_contents", types="Any"),
        ],
    )
    def read_excel(
        excel: ExcelObj,
        sheet_name: str = "",
        read_range: ReadRangeType = ReadRangeType.CELL,
        start_col: str = "",
        end_col: str = "",
        cell: str = "",
        row: str = 1,
        column: str = "",
        start_row: str = 1,
        end_row: str = 1,
        read_display: bool = True,
        trim_spaces: bool = False,
        replace_none: bool = True,
    ):
        worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)
        used_range = Worksheet.get_worksheet_used_range(worksheet)
        _, _, r_end_row, r_end_col, _ = used_range

        if read_range == ReadRangeType.CELL:
            r_obj = Worksheet.get_range(worksheet, cell)
            content = Range.get_range_data(r_obj, use_text=True if read_display else False)
        elif read_range == ReadRangeType.ROW:
            start_row_num = handle_row_input(row, r_end_row)
            content = []
            for col in range(1, r_end_col + 1):
                r_obj = Worksheet.get_cell(worksheet, start_row_num, col)
                cell_text = Range.get_range_data(r_obj, use_text=True if read_display else False)
                content.append(cell_text)
        elif read_range == ReadRangeType.COLUMN:
            start_col_num = handle_column_input(column, r_end_col)
            content = []
            for row in range(1, r_end_row + 1):
                r_obj = Worksheet.get_cell(worksheet, row, start_col_num)
                cell_text = Range.get_range_data(r_obj, use_text=True if read_display else False)
                content.append(cell_text)
        elif read_range == ReadRangeType.AREA:
            start_col_num = handle_column_input(start_col, r_end_col)
            end_col_num = handle_column_input(end_col, r_end_col)
            start_row_num = handle_row_input(start_row, r_end_row)
            end_row_num = handle_row_input(end_row, r_end_row)
            content = []
            for row in range(start_row_num, end_row_num + 1):
                row_data = []
                for col in range(start_col_num, end_col_num + 1):
                    r_obj = Worksheet.get_cell(worksheet, row, col)
                    cell_text = Range.get_range_data(r_obj, use_text=True if read_display else False)
                    row_data.append(cell_text)
                content.append(row_data)
        elif read_range == ReadRangeType.ALL:
            content = []
            for row in range(1, r_end_row + 1):
                row_data = []
                for col in range(1, r_end_col + 1):
                    r_obj = Worksheet.get_cell(worksheet, row, col)
                    cell_text = Range.get_range_data(r_obj, use_text=True if read_display else False)
                    row_data.append(cell_text)
                content.append(row_data)
        else:
            raise NotImplementedError()

        if trim_spaces:
            content = util_trim(content)
        if replace_none:
            content = util_replace_node(content)
        return content

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param(
                "cell_position",
                dynamics=[
                    DynamicsItem(
                        key="$this.cell_position.show",
                        expression="return $this.design_type.value == '{}'".format(ReadRangeType.CELL.value),
                    )
                ],
            ),
            atomicMg.param(
                "range_position",
                dynamics=[
                    DynamicsItem(
                        key="$this.range_position.show",
                        expression="return $this.design_type.value == '{}'".format(ReadRangeType.AREA.value),
                    )
                ],
            ),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression="return $this.design_type.value == '{}'".format(ReadRangeType.COLUMN.value),
                    )
                ],
            ),
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression="return $this.design_type.value == '{}'".format(ReadRangeType.ROW.value),
                    )
                ],
            ),
            atomicMg.param("col_width", required=False),
            atomicMg.param("sheet_name", required=False),
            atomicMg.param(
                "bg_color", required=False, formType=AtomicFormTypeMeta(type=AtomicFormType.INPUT_VARIABLE_COLOR.value)
            ),
            atomicMg.param(
                "font_color",
                required=False,
                formType=AtomicFormTypeMeta(type=AtomicFormType.INPUT_VARIABLE_COLOR.value),
            ),
            atomicMg.param("font_size", required=False),
            atomicMg.param(
                "numberformat_other",
                dynamics=[
                    DynamicsItem(
                        key="$this.numberformat_other.show",
                        expression="return $this.numberformat.value == '{}'".format(NumberFormatType.CUSTOM.value),
                    )
                ],
                required=False,
            ),
            atomicMg.param(
                "auto_row_height",
                dynamics=[
                    DynamicsItem(
                        key="$this.auto_row_height.show",
                        expression="return $this.design_type.value == '{}'".format(ReadRangeType.ROW.value),
                    )
                ],
            ),
            atomicMg.param(
                "auto_column_width",
                dynamics=[
                    DynamicsItem(
                        key="$this.auto_column_width.show",
                        expression="return $this.design_type.value == '{}'".format(ReadRangeType.COLUMN.value),
                    )
                ],
            ),
        ],
        outputList=[],
    )
    def design_cell_type(
        excel: ExcelObj,
        sheet_name: str = "",
        design_type: ReadRangeType = ReadRangeType.CELL,
        cell_position: str = "",
        range_position: str = "",
        col: str = "",
        row: str = "",
        col_width: str = "",
        bg_color: str = "",
        font_color: str = "",
        font_type: FontType = FontType.NO_CHANGE,
        font_name: FontNameType = FontNameType.NO_CHANGE,
        font_size: int = 11,
        numberformat: NumberFormatType = NumberFormatType.NO_CHANGE,
        numberformat_other: str = "",
        horizontal_align: HorizontalAlign = HorizontalAlign.NO_CHANGE,
        vertical_align: VerticalAlign = VerticalAlign.NO_CHANGE,
        wrap_text: bool = True,
        auto_row_height: bool = False,
        auto_column_width: bool = False,
    ):
        if bg_color:
            bg_color = check_color(bg_color)
        if font_color:
            font_color = check_color(font_color)

        worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)
        used_range = Worksheet.get_worksheet_used_range(worksheet)
        _, _, r_end_row, r_end_col, r_address = used_range

        cell_positions = calculate_cell_positions(
            design_type=design_type.value,
            cell_position=cell_position,
            range_position=range_position,
            col=col,
            row=row,
            r_end_row=r_end_row,
            r_end_col=r_end_col,
            r_address=r_address,
        )

        for cell_position in cell_positions:
            r_obj = Worksheet.get_range(worksheet, cell_position)
            Range.set_range_type(
                range_obj=r_obj,
                col_width=col_width,
                bg_color=bg_color,
                font_color=font_color,
                font_type=font_type,
                font_name=font_name,
                font_size=font_size,
                number_format=numberformat,
                number_format_other=numberformat_other,
                horizontal_align=horizontal_align,
                vertical_align=vertical_align,
                wrap_text=wrap_text,
                design_type=design_type,
                auto_row_height=auto_row_height,
                auto_column_width=auto_column_width,
            )

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("sheet_name", required=False),
            atomicMg.param(
                "cell_position",
                dynamics=[
                    DynamicsItem(
                        key="$this.cell_position.show",
                        expression="return $this.copy_range_type.value == '{}'".format(ReadRangeType.CELL.value),
                    )
                ],
            ),
            atomicMg.param(
                "range_position",
                dynamics=[
                    DynamicsItem(
                        key="$this.range_position.show",
                        expression="return $this.copy_range_type.value == '{}'".format(ReadRangeType.AREA.value),
                    )
                ],
            ),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression="return $this.copy_range_type.value == '{}'".format(ReadRangeType.COLUMN.value),
                    )
                ],
            ),
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression="return $this.copy_range_type.value == '{}'".format(ReadRangeType.ROW.value),
                    )
                ],
            ),
        ],
        outputList=[
            atomicMg.param("copy_excel_contents", types="Str"),
        ],
    )
    def copy_excel(
        excel: ExcelObj,
        sheet_name: str = "",
        copy_range_type: ReadRangeType = ReadRangeType.CELL,
        cell_position: str = "A1",
        row: str = "",
        col: str = "",
        range_position: str = "A1:B5",
    ):
        worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)
        used_range = Worksheet.get_worksheet_used_range(worksheet)
        _, _, r_end_row, r_end_col, r_address = used_range

        cell_positions = calculate_cell_positions(
            design_type=copy_range_type.value,
            cell_position=cell_position,
            range_position=range_position,
            col=col,
            row=row,
            r_end_row=r_end_row,
            r_end_col=r_end_col,
            r_address=r_address,
        )
        if len(cell_positions) > 0:
            cell = cell_positions[0]
        else:
            return ""

        r_obj = Worksheet.get_range(worksheet, cell)
        Range.copy_range(r_obj)
        try:
            cv.OpenClipboard()
            return cv.GetClipboardData(cv.CF_UNICODETEXT)
        finally:
            try:
                cv.CloseClipboard()
            except Exception as e:
                pass

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("sheet_name", required=False),
        ],
        outputList=[],
    )
    def paste_excel(
        excel: ExcelObj,
        sheet_name: str = "",
        paste_type: PasteType = PasteType.ALL,
        start_location: str = "A1",
        skip_blanks: bool = False,
        transpose: bool = False,
    ):
        worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)
        r_obj = Worksheet.get_range(worksheet, start_location)
        Range.paste_range(r_obj, paste_type=paste_type.value, skip_blanks=skip_blanks, transpose=transpose)

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("sheet_name", required=False),
            atomicMg.param(
                "coordinate",
                dynamics=[
                    DynamicsItem(
                        key="$this.coordinate.show",
                        expression="return $this.delete_range_excel.value == '{}'".format(ReadRangeType.CELL.value),
                    )
                ],
            ),
            atomicMg.param(
                "data_region",
                dynamics=[
                    DynamicsItem(
                        key="$this.data_region.show",
                        expression="return $this.delete_range_excel.value == '{}'".format(ReadRangeType.AREA.value),
                    )
                ],
            ),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression="return $this.delete_range_excel.value == '{}'".format(ReadRangeType.COLUMN.value),
                    )
                ],
            ),
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression="return $this.delete_range_excel.value == '{}'".format(ReadRangeType.ROW.value),
                    )
                ],
            ),
            atomicMg.param(
                "direction",
                dynamics=[
                    DynamicsItem(
                        key="$this.direction.show",
                        expression="return ['{}', '{}'].includes($this.delete_range_excel.value)".format(
                            ReadRangeType.CELL.value, ReadRangeType.AREA.value
                        ),
                    )
                ],
            ),
        ],
        outputList=[],
    )
    def delete_excel_cell(
        excel: ExcelObj,
        sheet_name: str = "",
        delete_range_excel: ReadRangeType = ReadRangeType.CELL,
        coordinate: str = "",
        row: str = "",
        col: str = "",
        data_region: str = "",
        direction: DeleteCellDirection = DeleteCellDirection.LOWER_MOVE_UP,
    ):
        worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)
        used_range = Worksheet.get_worksheet_used_range(worksheet)
        _, _, r_end_row, r_end_col, r_address = used_range
        cell_positions = calculate_cell_positions(
            design_type=delete_range_excel.value,
            cell_position=coordinate,
            range_position=data_region,
            col=col,
            row=row,
            r_end_row=r_end_row,
            r_end_col=r_end_col,
            r_address=r_address,
        )
        for cell_position in cell_positions:
            r_obj = Worksheet.get_range(worksheet, cell_position)
            if delete_range_excel in [ReadRangeType.CELL, ReadRangeType.AREA]:
                Range.delete_range(r_obj, direction=direction.value)
            else:
                Range.delete_range(r_obj)

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("sheet_name", required=False),
            atomicMg.param(
                "cell_location",
                dynamics=[
                    DynamicsItem(
                        key="$this.cell_location.show",
                        expression="return $this.select_type.value == '{}'".format(ReadRangeType.CELL.value),
                    )
                ],
            ),
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression="return $this.select_type.value == '{}'".format(ReadRangeType.ROW.value),
                    )
                ],
            ),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression="return $this.select_type.value == '{}'".format(ReadRangeType.COLUMN.value),
                    )
                ],
            ),
            atomicMg.param(
                "data_range",
                dynamics=[
                    DynamicsItem(
                        key="$this.data_range.show",
                        expression="return $this.select_type.value == '{}'".format(ReadRangeType.AREA.value),
                    )
                ],
            ),
        ],
        outputList=[],
    )
    def clear_excel_content(
        excel: ExcelObj,
        sheet_name: str,
        select_type: ReadRangeType = ReadRangeType.CELL,
        cell_location: str = "",
        row: str = "",
        col: str = "",
        data_range: str = "A1:B5",
        clear_type: ClearType = ClearType.CONTENT,
    ):
        worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)
        used_range = Worksheet.get_worksheet_used_range(worksheet)
        _, _, r_end_row, r_end_col, r_address = used_range
        cell_positions = calculate_cell_positions(
            design_type=select_type.value,
            cell_position=cell_location,
            range_position=data_range,
            col=col,
            row=row,
            r_end_row=r_end_row,
            r_end_col=r_end_col,
            r_address=r_address,
        )
        for cell_position in cell_positions:
            r_obj = Worksheet.get_range(worksheet, cell_position)
            Range.clear_range(r_obj, clear_type.value)

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("sheet_name", required=False),
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression="return $this.insert_type.value == '{}'".format(EnhancedInsertType.ROW.value),
                    )
                ],
            ),
            atomicMg.param(
                "row_direction",
                dynamics=[
                    DynamicsItem(
                        key="$this.row_direction.show",
                        expression="return $this.insert_type.value == '{}'".format(EnhancedInsertType.ROW.value),
                    )
                ],
            ),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression="return $this.insert_type.value == '{}'".format(EnhancedInsertType.COLUMN.value),
                    )
                ],
            ),
            atomicMg.param(
                "col_direction",
                dynamics=[
                    DynamicsItem(
                        key="$this.col_direction.show",
                        expression="return $this.insert_type.value == '{}'".format(EnhancedInsertType.COLUMN.value),
                    )
                ],
            ),
            atomicMg.param(
                "insert_num",
                dynamics=[
                    DynamicsItem(key="$this.insert_num.show", expression="return $this.blank_rows.value == true")
                ],
            ),
            atomicMg.param(
                "insert_content",
                dynamics=[
                    DynamicsItem(key="$this.insert_content.show", expression="return $this.blank_rows.value == false")
                ],
            ),
        ],
        outputList=[],
    )
    def insert_excel_row_or_column(
        excel: ExcelObj,
        sheet_name: str = "",
        insert_type: EnhancedInsertType = EnhancedInsertType.ROW,
        row: str = 1,
        row_direction: RowDirectionType = RowDirectionType.LOWER,
        col: str = 1,
        col_direction: ColumnDirectionType = ColumnDirectionType.RIGHT,
        blank_rows: bool = False,
        insert_num: int = 1,
        insert_content: str = "",
    ):
        worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)
        used_range = Worksheet.get_worksheet_used_range(worksheet)
        _, _, r_end_row, r_end_col, r_address = used_range

        r_end_col_letter = column_number_to_letter(r_end_col)

        value = insert_content
        if isinstance(value, str) and value.startswith("[") and value.endswith("]"):
            try:
                value = ast.literal_eval(value)
            except Exception as e:
                raise Exception("의목록형식있음오류")

        if not blank_rows:
            if not isinstance(value, list):
                raise Exception("삽입내용 의목록형식있음오류")
            if not isinstance(value[0], list):
                raise Exception("내용의목록형식있음오류")
            insert_num = len(value)

        if insert_type == EnhancedInsertType.ADD_ROWS:
            row_direction = RowDirectionType.LOWER
            row = r_end_row
        elif insert_type == EnhancedInsertType.ADD_COLUMNS:
            col_direction = ColumnDirectionType.RIGHT
            col = r_end_col

        if insert_type in [EnhancedInsertType.ROW, EnhancedInsertType.ADD_ROWS]:
            start_row_num = handle_row_input(row, r_end_row)
            if row_direction == RowDirectionType.UPPER:  # 위
                for _ in range(insert_num):
                    r_obj = Worksheet.get_range(
                        worksheet, "A{}:{}{}".format(start_row_num, r_end_col_letter, start_row_num)
                    )
                    Range.insert_range(r_obj, "row")
            elif row_direction == RowDirectionType.LOWER:  # 아래
                for _ in range(insert_num):
                    r_obj = Worksheet.get_range(
                        worksheet, "A{}:{}{}".format(start_row_num + 1, r_end_col_letter, start_row_num + 1)
                    )
                    Range.insert_range(r_obj, "row")
                start_row_num = start_row_num + 1

            if not blank_rows:
                # 삽입내용
                first_col = 1
                first_row = start_row_num
                for row in value:
                    for offset, val in enumerate(row):
                        r_obj = Worksheet.get_cell(worksheet, first_row, first_col + offset)
                        Range.set_range_data(r_obj, val)
                    first_row += 1  # 일행, 아래
        elif insert_type in [EnhancedInsertType.COLUMN, EnhancedInsertType.ADD_COLUMNS]:
            start_col_num = handle_column_input(col, r_end_col)
            if col_direction == ColumnDirectionType.LEFT:  # 위
                for _ in range(insert_num):
                    r_obj = Worksheet.get_range(
                        worksheet,
                        "{}1:{}{}".format(
                            column_number_to_letter(start_col_num), column_number_to_letter(start_col_num), r_end_row
                        ),
                    )
                    Range.insert_range(r_obj, "column")
            elif col_direction == ColumnDirectionType.RIGHT:  # 아래
                for _ in range(insert_num):
                    r_obj = Worksheet.get_range(
                        worksheet,
                        "{}1:{}{}".format(
                            column_number_to_letter(start_col_num + 1),
                            column_number_to_letter(start_col_num + 1),
                            r_end_row,
                        ),
                    )
                    Range.insert_range(r_obj, "column")
                start_col_num = start_col_num + 1
            if not blank_rows:
                filled_T = list(zip_longest(*value, fillvalue=""))
                value = [list(t_row) for t_row in filled_T]
                # 삽입내용
                first_col = start_col_num
                first_row = 1
                for row in value:
                    for offset, val in enumerate(row):
                        r_obj = Worksheet.get_cell(worksheet, first_row, first_col + offset)
                        Range.set_range_data(r_obj, val)
                    first_row += 1  # 일행, 아래

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("sheet_name", required=False),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression="return $this.get_col_type.value == '{}'".format(ColumnType.ONE_COLUMN.value),
                    )
                ],
            ),
        ],
        outputList=[
            atomicMg.param("excel_row_num", types="Int"),
        ],
    )
    def get_excel_row_num(
        excel: ExcelObj, sheet_name: str = "", get_col_type: ColumnType = ColumnType.ALL, col: str = ""
    ):
        worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)
        used_range = Worksheet.get_worksheet_used_range(worksheet)
        _, _, r_end_row, r_end_col, r_address = used_range

        if get_col_type == ColumnType.ALL:
            return r_end_row
        elif get_col_type == ColumnType.ONE_COLUMN:
            start_col_num = handle_column_input(col, r_end_col)
            for row in range(r_end_row, 0, -1):  # 순서
                val = Range.get_range_data(Worksheet.get_cell(worksheet, row, start_col_num), False)
                if val not in [None, ""]:
                    return row  # Excel 행 = row
            return 0  # 전체빈

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("sheet_name", required=False),
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression="return $this.get_row_type.value == '{}'".format(RowType.ONE_ROW.value),
                    )
                ],
            ),
        ],
        outputList=[
            atomicMg.param("excel_col_num", types="Int"),
        ],
    )
    def get_excel_col_num(
        excel: ExcelObj,
        sheet_name: str = "",
        get_row_type: RowType = RowType.ALL,
        row: str = "",
        output_type: ColumnOutputType = ColumnOutputType.NUMBER,
    ):
        worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)
        used_range = Worksheet.get_worksheet_used_range(worksheet)
        r_start_row, r_start_col, r_end_row, r_end_col, r_address = used_range

        if get_row_type == RowType.ALL:
            return column_number_to_letter(r_end_col) if output_type == ColumnOutputType.LETTER else r_end_col
        elif get_row_type == RowType.ONE_ROW:
            start_row_num = handle_row_input(row, r_end_row)

            for col in range(r_end_col, 0, -1):  # 에서후일열전조회
                val = Range.get_range_data(Worksheet.get_cell(worksheet, start_row_num, col), False)
                if val not in [None, ""]:
                    return column_number_to_letter(col) if output_type == ColumnOutputType.LETTER else col
            return "" if output_type == ColumnOutputType.LETTER else 0

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("sheet_name", required=False),
        ],
        outputList=[
            atomicMg.param("get_first_available_row", types="Int"),
        ],
    )
    def get_excel_first_available_row(excel: ExcelObj, sheet_name: str = ""):
        """
        가져오기 일개빈행의행

        Args:
            excel: ExcelObj 객체
            sheet_name: 테이블이름, 비어 있습니다(일개테이블)

        Returns:
            일개빈행의행(1-based), 결과가아니오비어 있습니다, 반환후일행+1
        """
        worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)
        used_range = Worksheet.get_worksheet_used_range(worksheet)
        r_start_row, r_start_col, r_end_row, r_end_col, r_address = used_range

        for row in range(r_start_row, r_end_row + 1):
            for col in range(r_start_col, r_end_col + 1):
                val = Range.get_range_data(Worksheet.get_cell(worksheet, row, col), False)
                if val not in (None, ""):
                    break
            else:
                return row
        return r_end_row + 1

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("sheet_name", required=False),
        ],
        outputList=[
            atomicMg.param("get_first_available_col", types="Any"),
        ],
    )
    def get_excel_first_available_col(
        excel: ExcelObj, sheet_name: str = "", output_type: ColumnOutputType = ColumnOutputType.LETTER
    ):
        """
        가져오기 일개빈열의열또는문자

        Args:
            excel: ExcelObj 객체
            sheet_name: 테이블이름, 비어 있습니다(일개테이블)
            output_type: 반환유형, LETTER(문자)또는 NUMBER(숫자)

        Returns:
            일개빈열의열또는문자(1-based).결과가아니오비어 있습니다, 반환후일열+1
        """
        worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)
        used_range = Worksheet.get_worksheet_used_range(worksheet)
        r_start_row, r_start_col, r_end_row, r_end_col, r_address = used_range

        for col in range(r_start_col, r_end_col + 1):
            for row in range(r_start_row, r_end_row + 1):
                val = Range.get_range_data(Worksheet.get_cell(worksheet, row, col), False)
                if val not in (None, ""):
                    break
            else:
                return column_number_to_letter(col) if output_type == ColumnOutputType.LETTER else col
        return column_number_to_letter(r_end_col + 1) if output_type == ColumnOutputType.LETTER else r_end_col + 1

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param(
                "start_row",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_row.show",
                        expression="return ['{}', '{}'].includes($this.select_type.value)".format(
                            SearchRangeType.ROW.value, SearchRangeType.AREA.value
                        ),
                    )
                ],
            ),
            atomicMg.param(
                "end_row",
                dynamics=[
                    DynamicsItem(
                        key="$this.end_row.show",
                        expression="return ['{}', '{}'].includes($this.select_type.value)".format(
                            SearchRangeType.ROW.value, SearchRangeType.AREA.value
                        ),
                    )
                ],
            ),
            atomicMg.param(
                "start_col",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_col.show",
                        expression="return ['{}', '{}'].includes($this.select_type.value)".format(
                            SearchRangeType.COLUMN.value, SearchRangeType.AREA.value
                        ),
                    )
                ],
            ),
            atomicMg.param(
                "end_col",
                dynamics=[
                    DynamicsItem(
                        key="$this.end_col.show",
                        expression="return ['{}', '{}'].includes($this.select_type.value)".format(
                            SearchRangeType.COLUMN.value, SearchRangeType.AREA.value
                        ),
                    )
                ],
            ),
            atomicMg.param("sheet_name", required=False),
        ],
        outputList=[
            atomicMg.param("key", types="Str"),
            atomicMg.param("value", types="Any"),
        ],
    )
    def loop_excel_content(
        excel: ExcelObj,
        sheet_name: str = "",
        select_type: SearchRangeType = SearchRangeType.ROW,
        start_row: str = "1",
        end_row: str = "-1",
        start_col: str = "A",
        end_col: str = "-1",
        real_text: bool = False,
        cell_strip: bool = False,
    ):
        worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)
        used_range = Worksheet.get_worksheet_used_range(worksheet)
        _, _, r_end_row, r_end_col, r_address = used_range

        r_end_col_letter = column_number_to_letter(r_end_col)

        start_col_num = handle_column_input(start_col, r_end_col)
        end_col_num = handle_column_input(end_col, r_end_col)
        start_row_num = handle_row_input(start_row, r_end_row)
        end_row_num = handle_row_input(end_row, r_end_row)

        if select_type == SearchRangeType.ROW:
            data_region = "{}{}:{}{}".format("A", start_row_num, r_end_col_letter, end_row_num)
        elif select_type == SearchRangeType.COLUMN:
            data_region = "{}{}:{}{}".format(
                column_number_to_letter(start_col_num), "1", column_number_to_letter(end_col_num), end_row_num
            )
        elif select_type == SearchRangeType.AREA:
            data_region = "{}{}:{}{}".format(
                column_number_to_letter(start_col_num), start_row_num, column_number_to_letter(end_col_num), end_row_num
            )
        elif select_type == SearchRangeType.ALL:
            data_region = "{}{}:{}{}".format("A", "1", r_end_col_letter, r_end_row)
        else:
            raise ValueError("지원하지 않음의유형: {}".format(select_type))

        content = Range.get_range_data(Worksheet.get_range(worksheet, data_region), True if real_text else False)
        if content:
            if cell_strip:
                content = util_trim(content)

            if content:
                if not isinstance(content, (list, tuple)):
                    content = ((content,),)
                if select_type == SearchRangeType.COLUMN:
                    keys = (column_number_to_letter(n) for n in range(start_col_num, end_col_num + 1))
                    vals = (list(col) for col in zip(*content))
                else:
                    keys = range(start_row_num, start_row_num + len(content))
                    vals = (list(row) for row in content)
                content = dict(zip(keys, vals))
        else:
            content = {}

        def table_generator():
            for i, v in content.items():
                yield i, v

        return table_generator()

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("sheet_name", required=False),
        ],
        outputList=[
            atomicMg.param("get_cell_color", types="Str"),
        ],
    )
    def excel_get_cell_color(excel: ExcelObj, coordinate: str, sheet_name: str = ""):
        worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)
        range_obj = Worksheet.get_range(worksheet, coordinate)
        r, g, b = Range.get_range_color(range_obj)
        color_str = "{},{},{}".format(r, g, b)
        return color_str

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("sheet_name", required=False),
            atomicMg.param(
                "merge_cell_range",
                dynamics=[
                    DynamicsItem(
                        key="$this.merge_cell_range.show",
                        expression="return $this.job_type.value == '{}'".format(MergeOrSplitType.MERGE.value),
                    )
                ],
            ),
            atomicMg.param(
                "split_cell_range",
                dynamics=[
                    DynamicsItem(
                        key="$this.split_cell_range.show",
                        expression="return $this.job_type.value == '{}'".format(MergeOrSplitType.SPLIT.value),
                    )
                ],
            ),
        ],
        outputList=[],
    )
    def merge_split_excel_cell(
        excel: ExcelObj,
        sheet_name: str,
        job_type: MergeOrSplitType = MergeOrSplitType.MERGE,
        merge_cell_range: str = "A1:B2",
        split_cell_range: str = "A1:B2",
    ):
        worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)
        if job_type == MergeOrSplitType.MERGE:
            range_obj = Worksheet.get_range(worksheet, merge_cell_range)
        else:
            range_obj = Worksheet.get_range(worksheet, split_cell_range)
        Range.merge_range(range_obj, job_type.value)

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("sheet_name", required=False),
            atomicMg.param(
                "relative_sheet_name",
                dynamics=[
                    DynamicsItem(
                        key="$this.relative_sheet_name.show",
                        expression="return $this.insert_type.value == '{}' || $this.insert_type.value == '{}'".format(
                            SheetInsertType.BEFORE.value, SheetInsertType.AFTER.value
                        ),
                    )
                ],
            ),
        ],
        outputList=[],
    )
    def add_excel_worksheet(
        excel: ExcelObj,
        sheet_name: str,
        insert_type: SheetInsertType = SheetInsertType.FIRST,
        relative_sheet_name: str = "",
    ):
        """
        생성테이블

        Args:
            excel: ExcelObj 객체
            sheet_name: 테이블이름
            insert_type: 삽입위치유형
            relative_sheet_name: 매개테이블이름( insert_type 로 BEFORE 또는 AFTER 시사용)
        """
        sheet_names = Worksheet.get_all_worksheet_names(excel)
        if sheet_name in sheet_names:
            raise ValueError("새sheet이름완료존재함")
        if len(sheet_name) >= 31:
            raise ValueError("sheet이름경과길이,필요소31개문자")

        if insert_type == SheetInsertType.FIRST:
            Worksheet.add_worksheet(excel, sheet_name, before=1)
        elif insert_type == SheetInsertType.LAST:
            Worksheet.add_worksheet(excel, sheet_name)
        else:
            if (not relative_sheet_name) or (relative_sheet_name not in sheet_names):
                raise ValueError("매개sheet이름찾을 수 없습니다")
            if insert_type == SheetInsertType.BEFORE:
                Worksheet.add_worksheet(excel, sheet_name, before=relative_sheet_name)
            elif insert_type == SheetInsertType.AFTER:
                Worksheet.add_worksheet(excel, sheet_name, after=relative_sheet_name)

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param(
                "move_to_sheet",
                dynamics=[
                    DynamicsItem(
                        key="$this.move_to_sheet.show",
                        expression="return ['{}', '{}'].includes($this.move_type.value)".format(
                            MoveSheetType.MOVE_AFTER.value, MoveSheetType.MOVE_BEFORE.value
                        ),
                    )
                ],
            )
        ],
        outputList=[],
    )
    def move_excel_worksheet(
        excel: ExcelObj,
        move_type: MoveSheetType = MoveSheetType.MOVE_AFTER,
        move_sheet: str = "",
        move_to_sheet: str = "",
    ):
        """
        테이블

        Args:
            excel: ExcelObj 객체
            move_type: 유형
            move_sheet: 필요의테이블이름
            move_to_sheet: 목록 테이블이름( move_type 로 MOVE_AFTER 또는 MOVE_BEFORE 시사용)
        """
        sheet_names = Worksheet.get_all_worksheet_names(excel)
        move_worksheet = Worksheet.get_worksheet(excel, move_sheet, default=0)

        if move_type == MoveSheetType.MOVE_TO_FIRST:
            Worksheet.move_worksheet(move_worksheet, before=1)
        elif move_type == MoveSheetType.MOVE_TO_LAST:
            Worksheet.move_worksheet(move_worksheet)
        else:
            if (not move_to_sheet) or (move_to_sheet not in sheet_names):
                raise ValueError("매개sheet이름찾을 수 없습니다")
            if move_type == MoveSheetType.MOVE_BEFORE:
                Worksheet.move_worksheet(move_worksheet, before=move_to_sheet)
            elif move_type == MoveSheetType.MOVE_AFTER:
                Worksheet.move_worksheet(move_worksheet, after=move_to_sheet)

    @staticmethod
    @atomicMg.atomic("Excel", inputList=[], outputList=[])
    def delete_excel_worksheet(excel: ExcelObj, del_sheet_name: str):
        """
        삭제테이블

        Args:
            excel: ExcelObj 객체
            del_sheet_name: 삭제할의테이블이름
        """
        worksheet = Worksheet.get_worksheet(excel, del_sheet_name)
        Worksheet.delete_worksheet(worksheet)

    @staticmethod
    @atomicMg.atomic("Excel", inputList=[], outputList=[])
    def rename_excel_worksheet(excel: ExcelObj, source_sheet_name: str, new_sheet_name: str):
        """
        이름 변경테이블

        Args:
            excel: ExcelObj 객체
            source_sheet_name: 테이블이름
            new_sheet_name: 새테이블이름
        """
        try:
            sheet_names = Worksheet.get_all_worksheet_names(excel)
            if new_sheet_name in sheet_names:
                raise ValueError("새sheet이름완료존재함")
            if len(new_sheet_name) >= 31:
                raise ValueError("sheet이름경과길이,필요소31개문자")
            worksheet = Worksheet.get_worksheet(excel, source_sheet_name)
            Worksheet.rename_worksheet(worksheet, new_sheet_name)
        except Exception as err:
            raise err

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param(
                "other_excel_obj",
                dynamics=[
                    DynamicsItem(
                        key="$this.other_excel_obj.show",
                        expression="return $this.copy_type.value == '{}'".format(CopySheetType.OTHER_WORKBOOK.value),
                    )
                ],
            ),
        ],
        outputList=[],
    )
    def copy_excel_worksheet(
        excel: ExcelObj,
        source_sheet_name: str,
        new_sheet_name: str,
        location: CopySheetLocationType = CopySheetLocationType.LAST,
        copy_type: CopySheetType = CopySheetType.CURRENT_WORKBOOK,
        other_excel_obj: ExcelObj = "",
        is_cover: bool = False,
    ):
        """
        복사테이블

        Args:
            excel: ExcelObj 객체
            source_sheet_name: 복사할sheet이름
            new_sheet_name: 새복사의sheet이름
            location: 위치, BEFORE, AFTER, FIRST, LAST
            copy_type: 복사유형, CURRENT_WORKBOOK 현재, OTHER_WORKBOOK 
            other_excel_obj: 객체(copy_type로OTHER_WORKBOOK시사용)
            is_cover: 여부덮어쓰기이름테이블
        """
        sheet_names = Worksheet.get_all_worksheet_names(excel)
        other_sheet_names = []
        if other_excel_obj:
            other_sheet_names = Worksheet.get_all_worksheet_names(other_excel_obj)

        if copy_type == CopySheetType.CURRENT_WORKBOOK:
            trigger_excel = excel
            if new_sheet_name in sheet_names and not is_cover:
                raise ValueError("복사sheet이름완료존재함")
        else:
            trigger_excel = other_excel_obj
            if new_sheet_name in other_sheet_names and not is_cover:
                raise ValueError("복사sheet이름완료존재함")

        if len(new_sheet_name) >= 31:
            raise ValueError("sheet이름경과길이,필요소31개문자")

        source_worksheet = Worksheet.get_worksheet(excel, source_sheet_name)

        # 삭제
        if is_cover and new_sheet_name in sheet_names:
            Worksheet.delete_worksheet(Worksheet.get_worksheet(trigger_excel, new_sheet_name))

        # 복사, active
        time.sleep(0.5)
        Worksheet.copy_worksheet(
            source_worksheet,
            trigger_excel,
            location,
            is_same_workbook=True if CopySheetType.CURRENT_WORKBOOK else False,
        )

        # 이름 변경
        Worksheet.rename_worksheet(Worksheet.get_active_worksheet(excel), new_sheet_name)

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[],
        outputList=[
            atomicMg.param("sheet_names", types="Str"),
        ],
    )
    def get_excel_worksheet_names(excel: ExcelObj, sheet_range: SheetRangeType = SheetRangeType.ACTIVATED):
        """
        가져오기 테이블이름

        Args:
            excel: ExcelObj 객체
            sheet_range: 유형, ACTIVATED 반환현재테이블이름, ALL 반환모든테이블이름목록

        Returns:
            테이블이름또는이름목록
        """
        if sheet_range == SheetRangeType.ALL:
            return Worksheet.get_all_worksheet_names(excel)
        else:
            return Worksheet.get_worksheet_name(Worksheet.get_active_worksheet(excel))

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param(
                "sheet_name",
                required=False,
                dynamics=[
                    DynamicsItem(
                        key="$this.sheet_name.show",
                        expression="return $this.lookup_range_excel.value == '{}'".format(SearchSheetType.ONE.value),
                    )
                ],
            ),
            atomicMg.param(
                "replace_str",
                dynamics=[
                    DynamicsItem(key="$this.replace_str.show", expression="return $this.replace_flag.value == true")
                ],
            ),
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression="return $this.search_range.value == '{}'".format(SearchRangeType.ROW.value),
                    )
                ],
            ),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression="return $this.search_range.value == '{}'".format(SearchRangeType.COLUMN.value),
                    )
                ],
            ),
            atomicMg.param(
                "start_row",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_row.show",
                        expression="return $this.search_range.value == '{}'".format(SearchRangeType.AREA.value),
                    )
                ],
            ),
            atomicMg.param(
                "end_row",
                dynamics=[
                    DynamicsItem(
                        key="$this.end_row.show",
                        expression="return $this.search_range.value == '{}'".format(SearchRangeType.AREA.value),
                    )
                ],
            ),
            atomicMg.param(
                "start_col",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_col.show",
                        expression="return $this.search_range.value == '{}'".format(SearchRangeType.AREA.value),
                    )
                ],
            ),
            atomicMg.param(
                "end_col",
                dynamics=[
                    DynamicsItem(
                        key="$this.end_col.show",
                        expression="return $this.search_range.value == '{}'".format(SearchRangeType.AREA.value),
                    )
                ],
            ),
        ],
        outputList=[atomicMg.param("search_excel_result", types="Dict")],
    )
    def search_and_replace_excel_content(
        excel: ExcelObj,
        find_str: str,
        replace_flag: bool = False,
        replace_str: str = "",
        lookup_range_excel: SearchSheetType = SearchSheetType.ALL,
        sheet_name: str = "",
        search_range: SearchRangeType = SearchRangeType.ALL,
        row: str = "",
        col: str = "",
        start_row: str = "",
        end_row: str = "",
        start_col: str = "",
        end_col: str = "",
        exact_match: bool = False,
        case_flag: bool = False,
        match_range: MatchCountType = MatchCountType.ALL,
        output_type: SearchResultType = SearchResultType.CELL,
    ):
        """
        조회및 Excel 중의내용

        Args:
            excel: Excel객체
            find_str: 필요조회의문자열
            replace_flag: 여부
            replace_str: 필요의문자열
            lookup_range_excel: 조회(ALL: 모든테이블, ONE: 지정테이블)
            sheet_name: 테이블이름
            search_range: 검색(ALL/ROW/COLUMN/AREA)
            row: 행(사용ROW유형)
            col: 열(사용COLUMN유형)
            start_row: 행(사용AREA유형)
            end_row: 끝 행(사용AREA유형)
            start_col: 열(사용AREA유형)
            end_col: 끝 열(사용AREA유형)
            exact_match: 여부매칭
            case_flag: 여부분크기
            match_range: 매칭(ALL: 모든, FIRST: 일개)
            output_type: 출력유형(CELL: 셀주소, COL_AND_ROW: 열및행)

        Returns:
            dict 또는 list: 검색결과, 형식근거 output_type 및 lookup_range_excel 지정
        """
        # 관리 find_str 로복사데이터유형의
        if isinstance(find_str, complex):
            find_str = str(find_str)

        # 지정할검색의테이블목록
        if lookup_range_excel == SearchSheetType.ALL:
            sheet_list = Worksheet.get_all_worksheets(excel)
            sheet_name = ""
        else:
            worksheet = Worksheet.get_worksheet(excel, sheet_name, 1)
            sheet_list = [worksheet]
            sheet_name = Worksheet.get_worksheet_name(worksheet)

        contents = {}

        def _format_search_results(results: list, output_type: SearchResultType) -> list:
            """
            형식검색결과

            Args:
                results: 검색결과목록, 매개요소패키지 {"row": 행, "col": 열문자}
                output_type: 출력유형

            Returns:
                list: 형식후의결과목록
            """
            formatted = []
            for result in results:
                if output_type == SearchResultType.CELL:
                    # 반환셀주소, 예 "A1"
                    formatted.append(result["col"] + result["row"])
                elif output_type == SearchResultType.COL_AND_ROW:
                    # 반환 [열문자, 행]
                    formatted.append([result["col"], result["row"]])
            return formatted

        # 매개테이블
        for worksheet in sheet_list:
            name = Worksheet.get_worksheet_name(worksheet)
            used_range = Worksheet.get_worksheet_used_range(worksheet)
            _, _, r_end_row, r_end_col, r_address = used_range

            #  AREA 유형, 필요관리
            if search_range == SearchRangeType.AREA:
                # 관리
                start_row_num = handle_row_input(start_row, r_end_row)
                end_row_num = handle_row_input(end_row, r_end_row)
                start_col_num = handle_column_input(start_col, r_end_col)
                end_col_num = handle_column_input(end_col, r_end_col)
                start_col_letter = column_number_to_letter(start_col_num)
                end_col_letter = column_number_to_letter(end_col_num)
                cell_positions = [f"{start_col_letter}{start_row_num}:{end_col_letter}{end_row_num}"]
            else:
                # 근거 search_range 계획셀위치
                cell_positions = calculate_cell_positions(
                    design_type=search_range.value,
                    cell_position="",
                    range_position="",
                    col=col,
                    row=row,
                    r_end_row=r_end_row,
                    r_end_col=r_end_col,
                    r_address=r_address,
                    support_comma=True,
                    support_colon=True,
                )

            # 에서매개내부검색
            all_results = []
            for cell_pos in cell_positions:
                range_obj = Worksheet.get_range(worksheet, cell_pos)
                results = Range.search_and_replace(
                    range_obj,
                    find_str,
                    replace_str if replace_flag else "",
                    exact_match,
                    case_flag,
                    match_range == MatchCountType.ALL,
                )
                all_results.extend(results)

            # 형식결과
            formatted_results = _format_search_results(all_results, output_type)
            contents[name] = formatted_results

        # 근거 lookup_range_excel 반환결과
        if lookup_range_excel == SearchSheetType.ONE:
            return contents.get(sheet_name, [])
        else:
            return contents

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("sheet_name", required=False),
            atomicMg.param(
                "pic_height",
                dynamics=[
                    DynamicsItem(
                        key="$this.pic_height.show",
                        expression="return $this.pic_size_type.value == '{}'".format(ImageSizeType.NUMBER.value),
                    )
                ],
            ),
            atomicMg.param(
                "pic_width",
                dynamics=[
                    DynamicsItem(
                        key="$this.pic_width.show",
                        expression="return $this.pic_size_type.value == '{}'".format(ImageSizeType.NUMBER.value),
                    )
                ],
            ),
            atomicMg.param(
                "pic_scale",
                dynamics=[
                    DynamicsItem(
                        key="$this.pic_scale.show",
                        expression="return $this.pic_size_type.value == '{}'".format(ImageSizeType.SCALE.value),
                    )
                ],
            ),
            atomicMg.param(
                "pic_path",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value, params={"filters": [], "file_type": "file"}
                ),
            ),
        ],
        outputList=[],
    )
    def insert_pic(
        excel: ExcelObj,
        sheet_name: str,
        insert_pos: str,
        pic_path: str,
        pic_size_type: ImageSizeType = ImageSizeType.AUTO,
        pic_height: int = 300,
        pic_width: int = 400,
        pic_scale: float = 1.0,
    ):
        worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)
        r_obj = Worksheet.get_range(worksheet, insert_pos)

        left, top, width, height = Range.get_range_size(r_obj)
        scale = 1.0
        if pic_size_type == ImageSizeType.SCALE:
            scale = pic_scale
        elif pic_size_type == ImageSizeType.NUMBER:
            width = pic_width
            height = pic_height
        elif pic_size_type == ImageSizeType.AUTO:
            width = width
            height = height
        Worksheet.insert_picture(worksheet, pic_path, left, top, height, width, scale)

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("sheet_name", required=False),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression="return $this.insert_direction.value == '{}'".format(
                            InsertFormulaDirectionType.DOWN.value
                        ),
                    )
                ],
            ),
            atomicMg.param(
                "start_row",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_row.show",
                        expression="return $this.insert_direction.value == '{}'".format(
                            InsertFormulaDirectionType.DOWN.value
                        ),
                    )
                ],
            ),
            atomicMg.param(
                "end_row",
                dynamics=[
                    DynamicsItem(
                        key="$this.end_row.show",
                        expression="return $this.insert_direction.value == '{}'".format(
                            InsertFormulaDirectionType.DOWN.value
                        ),
                    )
                ],
            ),
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression="return $this.insert_direction.value == '{}'".format(
                            InsertFormulaDirectionType.RIGHT.value
                        ),
                    )
                ],
            ),
            atomicMg.param(
                "start_col",
                dynamics=[
                    DynamicsItem(
                        key="$this.start_col.show",
                        expression="return $this.insert_direction.value == '{}'".format(
                            InsertFormulaDirectionType.RIGHT.value
                        ),
                    )
                ],
            ),
            atomicMg.param(
                "end_col",
                dynamics=[
                    DynamicsItem(
                        key="$this.end_col.show",
                        expression="return $this.insert_direction.value == '{}'".format(
                            InsertFormulaDirectionType.RIGHT.value
                        ),
                    )
                ],
            ),
        ],
        outputList=[],
    )
    def insert_formula(
        excel: ExcelObj,
        sheet_name: str = "",
        insert_direction: InsertFormulaDirectionType = InsertFormulaDirectionType.DOWN,
        col: str = "",
        start_row: str = "1",
        end_row: str = "-1",
        row: str = "",
        start_col: str = "A",
        end_col: str = "-1",
        formula: str = "",
    ):
        worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)
        used_range = Worksheet.get_worksheet_used_range(worksheet)
        _, _, r_end_row, r_end_col, _ = used_range

        if insert_direction == InsertFormulaDirectionType.DOWN:
            col_num = handle_column_input(col, r_end_col)
            col_letter = column_number_to_letter(col_num)
            start_row_num = handle_row_input(start_row, r_end_row)
            end_row_num = handle_row_input(end_row, r_end_row)

            starter = Worksheet.get_range(worksheet, "{}{}".format(col_letter, str(start_row_num)))
            Range.set_range_data(starter, formula)
            target_range = Worksheet.get_range(
                worksheet, "{}{}:{}{}".format(col_letter, str(start_row_num), col_letter, str(end_row_num))
            )
            Range.autofill_range(starter, target_range)

        if insert_direction == InsertFormulaDirectionType.RIGHT:
            start_col_num = handle_column_input(start_col, r_end_col)
            end_col_num = handle_column_input(end_col, r_end_col)
            start_col_letter = column_number_to_letter(start_col_num)
            end_col_letter = column_number_to_letter(end_col_num)
            row_num = handle_row_input(row, r_end_row)

            starter = Worksheet.get_range(worksheet, "{}{}".format(start_col_letter, str(row_num)))
            Range.set_range_data(starter, formula)
            target_range = Worksheet.get_range(
                worksheet, "{}{}:{}{}".format(start_col_letter, str(row_num), end_col_letter, str(row_num))
            )
            Range.autofill_range(starter, target_range)

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param(
                "cell_position",
                dynamics=[
                    DynamicsItem(
                        key="$this.cell_position.show",
                        expression="return $this.comment_type.value == '{}'".format(CreateCommentType.POSITION.value),
                    )
                ],
            ),
            atomicMg.param(
                "find_str",
                dynamics=[
                    DynamicsItem(
                        key="$this.find_str.show",
                        expression="return $this.comment_type.value == '{}'".format(CreateCommentType.CONTENT.value),
                    )
                ],
            ),
            atomicMg.param(
                "sheet_name",
                required=False,
                dynamics=[
                    DynamicsItem(
                        key="$this.sheet_name.show",
                        expression="return $this.comment_range.value == '{}' || $this.comment_type.value == '{}'".format(
                            SearchSheetType.ONE.value, CreateCommentType.POSITION.value
                        ),
                    )
                ],
            ),
            atomicMg.param(
                "comment_range",
                dynamics=[
                    DynamicsItem(
                        key="$this.comment_range.show",
                        expression="return $this.comment_type.value == '{}'".format(CreateCommentType.CONTENT.value),
                    )
                ],
            ),
            atomicMg.param(
                "comment_all",
                dynamics=[
                    DynamicsItem(
                        key="$this.comment_all.show",
                        expression="return $this.comment_type.value == '{}'".format(CreateCommentType.CONTENT.value),
                    )
                ],
            ),
        ],
        outputList=[],
    )
    def create_excel_comment(
        excel: ExcelObj,
        comment_type: CreateCommentType = CreateCommentType.POSITION,
        comment: str = "",
        sheet_name: str = "",
        cell_position: str = "",
        comment_range: SearchSheetType = SearchSheetType.ONE,
        find_str: str = "",
        comment_all: bool = False,
    ):
        """
        생성 Excel 비고

        Args:
            excel: Excel객체
            comment_type: 비고유형(POSITION: 위치 지정, CONTENT: 지정내용)
            comment: 비고내용
            sheet_name: 테이블이름
            cell_position: 셀위치(사용 POSITION 유형)
            comment_range: 비고(ONE: 지정테이블, ALL: 모든테이블)
            find_str: 대기조회의내용(사용 CONTENT 유형)
            comment_all: 여부비고모든매칭의내용(사용 CONTENT 유형)
        """
        if comment_type == CreateCommentType.POSITION:
            # 위치 지정비고
            worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)
            range_obj = Worksheet.get_range(worksheet, cell_position)
            Range.add_comment(range_obj, comment)
        elif comment_type == CreateCommentType.CONTENT:
            # 지정내용 비고
            count = 0

            # 지정할검색의테이블목록
            if comment_range == SearchSheetType.ALL:
                sheet_list = Worksheet.get_all_worksheets(excel)
            else:
                sheet_list = [Worksheet.get_worksheet(excel, sheet_name, 1)]

            # 테이블검색추가비고
            for worksheet in sheet_list:
                used_range = Worksheet.get_worksheet_used_range(worksheet)
                _, _, r_end_row, r_end_col, _ = used_range

                # 모든셀조회매칭의내용
                for row in range(1, r_end_row + 1):
                    for col in range(1, r_end_col + 1):
                        cell = Worksheet.get_cell(worksheet, row, col)
                        cell_value = Range.get_range_data(cell, use_text=True)

                        # 조회여부매칭조회내용
                        if cell_value and find_str in str(cell_value):
                            Range.add_comment(cell, comment)
                            count += 1

                            # 결과가비고일개매칭, 이면출력모든
                            if count == 1 and not comment_all:
                                return

                    # 결과가비고일개매칭완료까지, 이면출력행
                    if count == 1 and not comment_all:
                        break

                # 결과가비고일개매칭완료까지, 이면출력테이블
                if count == 1 and not comment_all:
                    break

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param(
                "cell_position",
                dynamics=[
                    DynamicsItem(key="$this.cell_position.show", expression="return $this.delete_all.value == false")
                ],
            ),
            atomicMg.param("sheet_name", required=False),
        ],
        outputList=[],
    )
    def delete_excel_comment(
        excel: ExcelObj,
        delete_all: bool = False,
        sheet_name: str = "",
        cell_position: str = "",
    ):
        """
        삭제 Excel 비고

        Args:
            excel: Excel객체
            delete_all: 삭제 여부모든비고
            sheet_name: 테이블이름
            cell_position: 셀위치( delete_all=False 시사용)

        Raises:
            ValueError: 아니오저장된 비고시출력예외
        """
        worksheet = Worksheet.get_worksheet(excel, sheet_name, default=1)

        if delete_all:
            # 삭제테이블중의모든비고
            Worksheet.delete_all_comments(worksheet)
        else:
            # 삭제지정셀의비고
            range_obj = Worksheet.get_range(worksheet, cell_position)
            Range.delete_comment(range_obj)

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("sheet_name", required=False),
            atomicMg.param(
                "cell_position",
                dynamics=[
                    DynamicsItem(
                        key="$this.cell_position.show",
                        expression="return $this.select_type.value == '{}'".format(ReadRangeType.CELL.value),
                    )
                ],
            ),
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression="return $this.select_type.value == '{}'".format(ReadRangeType.ROW.value),
                    )
                ],
            ),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression="return $this.select_type.value == '{}'".format(ReadRangeType.COLUMN.value),
                    )
                ],
            ),
            atomicMg.param(
                "range_location",
                dynamics=[
                    DynamicsItem(
                        key="$this.range_location.show",
                        expression="return $this.select_type.value == '{}'".format(ReadRangeType.AREA.value),
                    )
                ],
            ),
        ],
        outputList=[],
    )
    def excel_text_to_number(
        excel_obj: ExcelObj,
        sheet_name: str = "",
        select_type: ReadRangeType = ReadRangeType.CELL,
        cell_position: str = "",
        row: str = "",
        col: str = "",
        range_location: str = "",
    ):
        """
        Excel 텍스트변환데이터값형식

        Args:
            excel_obj: Excel객체
            sheet_name: 테이블이름
            select_type: 선택유형(CELL/ROW/COLUMN/AREA/ALL)
            cell_position: 셀위치(사용CELL유형)
            row: 행(사용ROW유형)
            col: 열(사용COLUMN유형)
            range_location: 위치(사용AREA유형)
        """
        worksheet = Worksheet.get_worksheet(excel_obj, sheet_name, default=1)
        used_range = Worksheet.get_worksheet_used_range(worksheet)
        _, _, r_end_row, r_end_col, r_address = used_range

        # 계획셀위치
        cell_positions = calculate_cell_positions(
            design_type=select_type.value,
            cell_position=cell_position,
            range_position=range_location,
            col=col,
            row=row,
            r_end_row=r_end_row,
            r_end_col=r_end_col,
            r_address=r_address,
        )

        # 사용일개시셀저장변환결과
        temp_cell = "{}{}".format(column_number_to_letter(r_end_col), r_end_row + 1)
        temp_range = Worksheet.get_range(worksheet, temp_cell)

        # 모든셀위치
        for cell_pos in cell_positions:
            range_obj = Worksheet.get_range(worksheet, cell_pos)
            Range.convert_text_to_number(range_obj, temp_range)

        # 관리시셀
        temp_range.Value = None

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("sheet_name", required=False),
            atomicMg.param(
                "cell_position",
                dynamics=[
                    DynamicsItem(
                        key="$this.cell_position.show",
                        expression="return $this.select_type.value == '{}'".format(ReadRangeType.CELL.value),
                    )
                ],
            ),
            atomicMg.param(
                "row",
                dynamics=[
                    DynamicsItem(
                        key="$this.row.show",
                        expression="return $this.select_type.value == '{}'".format(ReadRangeType.ROW.value),
                    )
                ],
            ),
            atomicMg.param(
                "col",
                dynamics=[
                    DynamicsItem(
                        key="$this.col.show",
                        expression="return $this.select_type.value == '{}'".format(ReadRangeType.COLUMN.value),
                    )
                ],
            ),
            atomicMg.param(
                "range_location",
                dynamics=[
                    DynamicsItem(
                        key="$this.range_location.show",
                        expression="return $this.select_type.value == '{}'".format(ReadRangeType.AREA.value),
                    )
                ],
            ),
        ],
        outputList=[],
    )
    def excel_number_to_text(
        excel_obj: ExcelObj,
        sheet_name: str = "",
        select_type: ReadRangeType = ReadRangeType.CELL,
        cell_position: str = "",
        row: str = "",
        col: str = "",
        range_location: str = "",
    ):
        """
        Excel 데이터값변환텍스트형식

        Args:
            excel_obj: Excel객체
            sheet_name: 테이블이름
            select_type: 선택유형(CELL/ROW/COLUMN/AREA/ALL)
            cell_position: 셀위치(사용CELL유형)
            row: 행(사용ROW유형)
            col: 열(사용COLUMN유형)
            range_location: 위치(사용AREA유형)
        """
        worksheet = Worksheet.get_worksheet(excel_obj, sheet_name, default=1)
        used_range = Worksheet.get_worksheet_used_range(worksheet)
        _, _, r_end_row, r_end_col, r_address = used_range

        # 계획셀위치
        cell_positions = calculate_cell_positions(
            design_type=select_type.value,
            cell_position=cell_position,
            range_position=range_location,
            col=col,
            row=row,
            r_end_row=r_end_row,
            r_end_col=r_end_col,
            r_address=r_address,
        )

        # 모든셀위치
        for cell_pos in cell_positions:
            range_obj = Worksheet.get_range(worksheet, cell_pos)
            Range.convert_number_to_text(range_obj)

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("sheet_name", required=False),
            atomicMg.param(
                "width",
                dynamics=[
                    DynamicsItem(
                        key="$this.width.show",
                        expression="return $this.set_type.value == '{}'".format(SetType.VALUE.value),
                    )
                ],
            ),
        ],
        outputList=[],
    )
    def excel_set_col_width(
        excel_obj: ExcelObj, sheet_name: str, set_type: SetType = SetType.AUTO, col: str = "", width: str = ""
    ):
        """
        지정열너비.
        :param excel_obj: Excel객체
        :param sheet_name: 테이블이름
        :param set_type: 방식 지정열너비,조정
        :param col: 지정열(지원다중열, 예"A:B,C"또는"1:3,5")
        :param width: 지정열너비(0-255)
        """
        worksheet = Worksheet.get_worksheet(excel_obj, sheet_name, default=1)
        used_range = Worksheet.get_worksheet_used_range(worksheet)
        _, _, r_end_row, r_end_col, _ = used_range

        # 인증열너비입력
        if set_type == SetType.VALUE:
            if width == "":
                raise ValueError("지정열너비방식아래, width매개변수비워 둘 수 없습니다!")
            try:
                width_float = float(width)
                assert width_float > 0
                assert width_float <= 255
            except Exception as e:
                raise ValueError("입력열너비있음오류, 확인하세요!열너비: 0-255")
        else:
            width_float = 0
        if col:
            col_list = handle_multiple_inputs(str(col), r_end_row, r_end_col, is_row=False)
        else:
            col_list = list(range(1, r_end_col + 1))
        for col_num in col_list:
            Range.set_column_width(Worksheet.get_columns(worksheet, col_num), set_type, width_float)

    @staticmethod
    @atomicMg.atomic(
        "Excel",
        inputList=[
            atomicMg.param("sheet_name", required=False),
            atomicMg.param(
                "height",
                dynamics=[
                    DynamicsItem(
                        key="$this.height.show",
                        expression="return $this.set_type.value == '{}'".format(SetType.VALUE.value),
                    )
                ],
            ),
        ],
        outputList=[],
    )
    def excel_set_row_height(
        excel_obj: ExcelObj, sheet_name: str, set_type: SetType = SetType.AUTO, row: str = "", height: str = ""
    ):
        """
        지정행높이.
        :param excel_obj: Excel객체
        :param sheet_name: 테이블이름
        :param set_type: 방식 지정행높이,조정
        :param row: 지정행(지원다중행, 예"1:3,5")
        :param height: 지정행높이(0-409.5)
        """
        worksheet = Worksheet.get_worksheet(excel_obj, sheet_name, default=1)
        used_range = Worksheet.get_worksheet_used_range(worksheet)
        _, _, r_end_row, r_end_col, _ = used_range

        # 인증행높이입력
        if set_type == SetType.VALUE:
            if height == "":
                raise ValueError("지정행높이방식아래, height매개변수비워 둘 수 없습니다!")
            try:
                height_float = float(height)
                assert height_float > 0
                assert height_float <= 409.5
            except Exception as e:
                raise ValueError("입력행높이있음오류, 확인하세요!행높이: 0-409.5")
        else:
            height_float = 0  # AUTO방식아래아니오필요height

        if row:
            row_list = handle_multiple_inputs(str(row), r_end_row, r_end_col, is_row=True)
        else:
            row_list = list(range(1, r_end_row + 1))
        for row_num in row_list:
            Range.set_row_height(Worksheet.get_rows(worksheet, row_num), set_type, height_float)