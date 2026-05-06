from typing import Any, Optional

from astronverse.excel import (
    ClearType,
    FontNameType,
    FontType,
    HorizontalAlign,
    NumberFormatType,
    ReadRangeType,
    SetType,
    VerticalAlign,
)


class Range:
    @staticmethod
    def get_range_data(range_obj, use_text: bool = False) -> Any:
        """
        가져오기 데이터

        Args:
            range_obj: Range 객체
            use_text: 여부반환텍스트, 로 False
        """
        try:
            return range_obj.Text if use_text else range_obj.Value
        except Exception as e:
            raise ValueError(f"가져오기 데이터실패: {e}")

    @staticmethod
    def get_range_color(range_obj) -> tuple[int, int, int]:
        """
        가져오기셀의색상(RGB형식)

        Args:
            range_obj: Range 객체

        Returns:
            Tuple[int, int, int]: RGB색상원그룹 (r, g, b), 매개값 0-255
        """
        try:
            # Excel 의 Interior.Color 반환 BGR 형식의색상값
            color_num = range_obj.Interior.Color
            # 를 BGR 변환로 RGB
            # Excel 색상형식: B + (G * 256) + (R * 65536)
            r = int(color_num) // (256**2) % 256
            g = (int(color_num) // 256) % 256
            b = int(color_num) % 256
            return r, g, b
        except Exception as e:
            raise ValueError(f"가져오기셀색상실패: {e}")

    @staticmethod
    def get_range_size(range_obj) -> tuple[int, int, int, int]:
        """
        가져오기 의위치
        """
        return range_obj.Left, range_obj.Top, range_obj.Width, range_obj.Height

    @staticmethod
    def set_range_data(range_obj, value: Any):
        """
        데이터

        Args:
            range_obj: Range 객체
            value: 필요의값
        """
        try:
            range_obj.Value = value
        except Exception as e:
            raise ValueError(f"데이터실패: {e}")

    @staticmethod
    def set_range_type(
        range_obj,
        col_width: Optional[str] = None,
        bg_color: Optional[tuple[int, int, int]] = None,
        font_color: Optional[tuple[int, int, int]] = None,
        font_type: FontType = FontType.NO_CHANGE,
        font_name: FontNameType = FontNameType.NO_CHANGE,
        font_size: Optional[int] = None,
        number_format: NumberFormatType = NumberFormatType.NO_CHANGE,
        number_format_other: str = "",
        horizontal_align: HorizontalAlign = HorizontalAlign.NO_CHANGE,
        vertical_align: VerticalAlign = VerticalAlign.NO_CHANGE,
        wrap_text: bool = True,
        design_type: ReadRangeType = ReadRangeType.CELL,
        auto_row_height: bool = False,
        auto_column_width: bool = False,
    ):
        """
        형식

        Args:
            range_obj: Range 객체
            col_width: 열너비
            bg_color: 색상 RGB 원그룹 (r, g, b)
            font_color: 문자색상 RGB 원그룹 (r, g, b)
            font_type: 문자유형
            font_name: 문자이름
            font_size: 문자크기
            number_format: 숫자형식유형
            number_format_other: 지정숫자형식
            horizontal_align: 수평평면방식
            vertical_align: 수직직선방식
            wrap_text: 여부행
            design_type: 계획유형(사용조정행높이/열너비)
            auto_row_height: 여부조정행높이
            auto_column_width: 여부조정열너비
        """
        # Excel 일반량
        XlHAlign_map = {
            HorizontalAlign.DEFAULT.value: 1,  # xlHAlignGeneral
            HorizontalAlign.LEFT.value: -4131,  # xlHAlignLeft
            HorizontalAlign.RIGHT.value: -4152,  # xlHAlignRight
            HorizontalAlign.CENTER.value: -4108,  # xlHAlignCenter
            HorizontalAlign.PADDING.value: 5,  # xlHAlignFill
            HorizontalAlign.BOTH.value: -4130,  # xlHAlignJustify
            HorizontalAlign.CROSS.value: 7,  # xlHAlignCenterAcrossSelection
            HorizontalAlign.DISTRIBUTED.value: -4117,  # xlHAlignDistributed
        }

        XlVAlign_map = {
            VerticalAlign.UP.value: -4160,  # xlVAlignTop
            VerticalAlign.MIDDLE.value: -4108,  # xlVAlignCenter
            VerticalAlign.DOWN.value: -4107,  # xlVAlignBottom
            VerticalAlign.BOTH.value: -4130,  # xlVAlignJustify
            VerticalAlign.DISTRIBUTED.value: -4117,  # xlVAlignDistributed
        }

        # RGB 데이터(Excel COM 객체사용 BGR 순서)
        def RGB(r, g, b):
            return b + (g * 256) + (r * 65536)

        # 열너비
        if col_width:
            range_obj.ColumnWidth = col_width

        # 문자색상
        if font_color:
            range_obj.Font.Color = RGB(
                font_color[0],
                font_color[1],
                font_color[2],
            )
        else:
            range_obj.Font.Color = RGB(0, 0, 0)

        # 색상
        if bg_color:
            bg_color = tuple(bg_color)
            range_obj.Interior.Color = RGB(
                bg_color[0],
                bg_color[1],
                bg_color[2],
            )
        else:
            range_obj.Interior.ColorIndex = 0

        # 문자이름
        if font_name != FontNameType.NO_CHANGE:
            range_obj.Font.Name = font_name.value

        # 문자크기
        if font_size:
            range_obj.Font.Size = font_size

        # 문자유형
        if font_type == FontType.BOLD:
            range_obj.Font.Bold = True
        elif font_type == FontType.ITALIC:
            range_obj.Font.Italic = True
        elif font_type == FontType.BOLD_ITALIC:
            range_obj.Font.Bold = True
            range_obj.Font.Italic = True
        elif font_type == FontType.NORMAL:
            range_obj.Font.Bold = False
            range_obj.Font.Italic = False

        # 행
        range_obj.WrapText = True if wrap_text is True else False

        # 수평평면
        if horizontal_align != HorizontalAlign.NO_CHANGE:
            range_obj.HorizontalAlignment = XlHAlign_map.get(horizontal_align.value)

        # 수직직선
        if vertical_align != VerticalAlign.NO_CHANGE:
            range_obj.VerticalAlignment = XlVAlign_map.get(vertical_align.value)

        # 열너비/행높이
        if design_type == ReadRangeType.ROW and auto_row_height:
            range_obj.Rows.AutoFit()
        if design_type == ReadRangeType.COLUMN and auto_column_width:
            range_obj.Columns.AutoFit()

        # 숫자형식
        if number_format != NumberFormatType.NO_CHANGE:
            if number_format == NumberFormatType.CUSTOM:
                format_str = number_format_other
            else:
                format_str = number_format.value
            range_obj.NumberFormat = format_str

    @staticmethod
    def delete_range(range_obj, direction: str = "") -> None:
        """
        삭제지정, 선택 가능왼쪽또는위

        Args:
            range_obj: Range 객체
            direction: 방법, RIGHT_MOVE_LEFT(오른쪽셀왼쪽), LOWER_MOVE_UP(아래방법셀위)
        """
        try:
            XlDeleteShiftDirection_map = {
                "right_move_left": -4159,  # xlToLeft
                "lower_move_up": -4162,  # xlUp
            }
            shift = XlDeleteShiftDirection_map.get(direction)
            if shift:
                range_obj.Delete(Shift=shift)
            else:
                range_obj.Delete()
        except Exception as e:
            raise ValueError(f"삭제실패: {e}")

    @staticmethod
    def clear_range(range_obj, clear_type: str = ""):
        """
        관리셀내용, 형식또는전체

        Args:
            range_obj: Range 객체
            clear_type: 관리유형
        """
        try:
            if clear_type == ClearType.CONTENT.value:
                range_obj.ClearContents()
            elif clear_type == ClearType.STYLE.value:
                range_obj.ClearFormats()
            elif clear_type == ClearType.ALL.value:
                range_obj.ClearFormats()
                range_obj.Clear()
            else:
                raise ValueError(f"지원하지 않음의관리유형: {clear_type}")
        except Exception as e:
            raise ValueError(f"관리실패: {e}")

    @staticmethod
    def copy_range(
        range_obj,
    ):
        """
        셀
        Args:
            range_obj: Range 객체
        """
        range_obj.Copy()

    @staticmethod
    def paste_range(
        range_obj,
        paste_type: str = "",
        skip_blanks=False,
        transpose=False,
    ):
        """
        붙여넣기의내용, 지원다중붙여넣기방식

        Args:
            range_obj: Range 객체, 붙여넣기
            paste_type: 붙여넣기유형
            skip_blanks: 건너뛰기빈셀
            transpose: 여부변환붙여넣기
        """
        paste_type_conf = {
            "all": -4104,  # 전체
            "value_and_format": 12,  # 값및숫자형식
            "format": -4122,  # 형식
            "exclude_frame": 7,  # 가장자리제거외부
            "col_width_only": 8,  # 열너비
            "formula_only": -4123,  # 방식
            "formula_and_format": 11,  # 방식및숫자형식
            "paste_value": -4163,  # 붙여넣기값
        }
        paste_type_value = paste_type_conf.get(paste_type)
        if paste_type_value is None:
            raise ValueError(f"지원하지 않음의붙여넣기유형: {paste_type_value}")

        try:
            range_obj.PasteSpecial(Paste=paste_type_value, SkipBlanks=bool(skip_blanks), Transpose=bool(transpose))
        except Exception as e:
            raise ValueError(f"붙여넣기실패: {e}")

    @staticmethod
    def insert_range(range_obj, axis: str = "row"):
        """
        삽입행또는열

        Args:
            range_obj: Range 객체
            axis: "row" 테이블삽입행, "column" 테이블삽입열
        """
        if axis == "row":
            # Excel 일반량 -4162 테이블 xlShiftDown
            range_obj.EntireRow.Insert(Shift=-4162)
        elif axis == "column":
            # Excel 일반량 -4159 테이블 xlShiftToRight
            range_obj.EntireColumn.Insert(Shift=-4159)
        else:
            raise ValueError(f"지원하지 않음의axis매개변수: {axis}")

    @staticmethod
    def merge_range(
        range_obj,
        job_type: str,
    ):
        """
        병합또는분할셀

        Args:
            range_obj: Range 객체
            job_type: 유형, MERGE 테이블병합, SPLIT 테이블분할
        """
        try:
            if job_type == "merge":
                range_obj.Merge()
            else:
                range_obj.UnMerge()
        except Exception as e:
            raise ValueError(f"병합/분할셀실패: {e}")

    @staticmethod
    def autofill_range(range_obj, target_range):
        """
        

        Args:
            range_obj:  Range 객체(필요의셀/)
            target_range: 목록 (Range 객체)
        """
        try:
            range_obj.AutoFill(target_range, 0)
        except Exception as e:
            raise ValueError(f"실패: {e}")

    @staticmethod
    def set_row_height(range_obj, set_type: SetType, height_float: float):
        """
        행높이

        Args:
            range_obj: Range 객체(통신일반예 worksheet.Rows(row_num) 반환의 Range)
            set_type: 유형(VALUE 또는 AUTO)
            height_float: 행높이값(에서 VALUE 방식아래사용)
        """
        if set_type == SetType.VALUE:
            # 지정행높이
            range_obj.RowHeight = height_float
        elif set_type == SetType.AUTO:
            # 조정행높이
            range_obj.AutoFit()

    @staticmethod
    def set_column_width(range_obj, set_type: SetType, width_float: float):
        """
        열너비

        Args:
            range_obj: Range 객체(통신일반예 worksheet.Columns(col_num) 반환의 Range)
            set_type: 유형(VALUE 또는 AUTO)
            width_float: 열너비값(에서 VALUE 방식아래사용)
        """
        if set_type == SetType.VALUE:
            # 지정열너비
            range_obj.ColumnWidth = width_float
        elif set_type == SetType.AUTO:
            # 조정열너비
            range_obj.AutoFit()

    @staticmethod
    def convert_text_to_number(range_obj, temp_range):
        """
        를내부의텍스트형식변환로데이터값형식

        Args:
            range_obj: Range 객체(필요변환의)
            temp_range: 시셀 Range 객체(사용저장 VALUE 데이터결과)
        """
        # 내부의매개셀
        for cell in range_obj.Cells:
            cell_address = cell.Address.replace("$", "")
            cell_value = cell.Value

            # 건너뛰기빈셀
            if cell_value in ["", None]:
                continue

            # 숫자형식로통신사용형식
            cell.NumberFormat = "G/통신사용형식"

            # 사용 VALUE 데이터변환
            temp_range.Value = "=VALUE({})".format(cell_address)
            temp_value = temp_range.Value

            # 결과가변환성공(아니오예오류값), 업데이트셀값
            # Excel 오류값통신일반예 -2146826273.0 (xlErrValue)
            if temp_value != -2146826273.0:
                cell.Value = temp_value

    @staticmethod
    def convert_number_to_text(range_obj):
        """
        를내부의데이터값형식변환로텍스트형식

        Args:
            range_obj: Range 객체(필요변환의)
        """
        # 내부의매개셀
        for cell in range_obj.Cells:
            cell_value = cell.Value

            # 결과가값아니오예문자열유형, 변환로텍스트형식
            if not isinstance(cell_value, str):
                # 숫자형식로텍스트형식("@" 테이블텍스트)
                cell.NumberFormat = "@"
                # 사용 Text 속성가져오기 값로텍스트
                cell.Value = cell.Text

    @staticmethod
    def add_comment(range_obj, comment_text: str):
        """
        로추가비고

        Args:
            range_obj: Range 객체(필요추가비고의셀)
            comment_text: 비고텍스트내용
        """
        if not range_obj.Comment:
            range_obj.AddComment()
        range_obj.Comment.Text(comment_text)

    @staticmethod
    def delete_comment(range_obj):
        """
        삭제의비고

        Args:
            range_obj: Range 객체(삭제할비고의셀)

        Raises:
            ValueError: 셀아니오저장된 비고시출력예외
        """
        if range_obj.Comment:
            range_obj.ClearComments()
        else:
            raise ValueError("아니오저장된 비고")

    @staticmethod
    def search_and_replace(
        range_obj,
        find_str: str,
        replace_str: str = "",
        exact_match: bool = False,
        case_flag: bool = False,
        match_all: bool = True,
    ) -> list:
        """
        에서내부검색선택 가능텍스트

        Args:
            range_obj: Range 객체(필요검색의)
            find_str: 필요조회의문자열
            replace_str: 필요의문자열(결과가비어 있습니다이면아니오)
            exact_match: 여부매칭(True: 전체매칭, False: 모듈분매칭)
            case_flag: 여부분크기
            match_all: 여부조회모든매칭(True: 모든, False: 일개)

        Returns:
            list: 매칭의셀정보목록, 매개요소패키지 {"row": 행, "col": 열문자}
        """
        # Excel 일반량
        xlWhole = 1  # 전체매칭
        xlPart = 2  # 모듈분매칭
        xlValues = -4163  # 에서값중조회

        look_at = xlWhole if exact_match else xlPart
        match_case = 1 if case_flag else 0

        positions = set()
        found_cell = range_obj.Find(
            find_str,
            LookAt=look_at,
            LookIn=xlValues,
            MatchCase=match_case,
        )

        if found_cell is not None:
            first_address = found_cell.Address
            positions.add(first_address)

            # 결과가필요
            if replace_str:
                cell_value = str(found_cell.Value) if found_cell.Value is not None else ""
                found_cell.Value = cell_value.replace(find_str, replace_str)

            # 결과가필요조회모든매칭
            if match_all:
                while True:
                    found_cell = range_obj.FindNext(found_cell)
                    if found_cell is None or found_cell.Address == first_address:
                        break
                    positions.add(found_cell.Address)

                    # 결과가필요
                    if replace_str:
                        cell_value = str(found_cell.Value) if found_cell.Value is not None else ""
                        found_cell.Value = cell_value.replace(find_str, replace_str)

        # 형식결과
        res = []
        import re

        from astronverse.excel.utils import column_letter_to_number

        for address in positions:
            # 주소형식통신일반예 "$A$1" 또는 "A1", 가져올열및행
            #  $ 기호후, 사용정상이면테이블방식가져오기열문자및행
            address_clean = address.replace("$", "")
            match = re.match(r"([A-Z]+)(\d+)", address_clean)
            if match:
                col = match.group(1)
                row = match.group(2)
                res.append({"row": int(row), "col": col, "col_num": column_letter_to_number(col)})

        # 행, 열의순서정렬
        res.sort(key=lambda x: (x["row"], x["col_num"]))

        # 를행변환돌아가기문자열
        for item in res:
            item["row"] = str(item["row"])
            del item["col_num"]

        return res