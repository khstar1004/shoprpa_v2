from astronverse.excel import CopySheetLocationType
from astronverse.excel.excel_obj import ExcelObj


class Worksheet:
    @staticmethod
    def get_worksheet(excel_obj: ExcelObj, sheet_name: str = "", default: int = 0) -> object:
        workbook = excel_obj.obj
        if not sheet_name:
            if default == 0:
                return Worksheet.get_active_worksheet(excel_obj)
            else:
                sheet_name = default

        # 시도이름가져오기
        try:
            return workbook.Worksheets(sheet_name)
        except Exception:
            # 시도검색가져오기
            try:
                sheet_index = int(sheet_name)
                if 1 <= sheet_index <= workbook.Sheets.Count:
                    return workbook.Worksheets(sheet_index)
            except ValueError:
                pass
            raise ValueError(f"테이블'{sheet_name}'찾을 수 없습니다")

    @staticmethod
    def get_all_worksheets(excel_obj: ExcelObj) -> list[object]:
        """
        가져오기모든테이블객체목록

        Args:
            excel_obj: ExcelObj 

        Returns:
            모든테이블객체의목록
        """
        workbook = excel_obj.obj
        return [ws for ws in workbook.Sheets]

    @staticmethod
    def get_all_worksheet_names(excel_obj: ExcelObj) -> list[str]:
        """
        가져오기모든테이블이름

        Args:
            excel_obj: ExcelObj 

        Returns:
            모든테이블이름의목록
        """
        workbook = excel_obj.obj
        return [ws.Name for ws in workbook.Sheets]

    @staticmethod
    def get_active_worksheet(excel_obj: ExcelObj) -> object:
        """
        가져오기현재의테이블객체

        Args:
            excel_obj: ExcelObj 

        Returns:
            현재의테이블객체
        """
        workbook = excel_obj.obj
        return workbook.ActiveSheet

    @staticmethod
    def add_worksheet(excel_obj: ExcelObj, sheet_name: str, before=None, after=None):
        """
        추가테이블

        Args:
            excel_obj: ExcelObj 
            sheet_name: 새테이블이름
            before: 에서지정테이블전삽입
            after: 에서지정테이블후삽입
        """
        workbook = excel_obj.obj
        if before:
            new_sheet = workbook.Sheets.Add(Before=workbook.Sheets(before))
        elif after:
            new_sheet = workbook.Sheets.Add(After=workbook.Sheets(after))
        else:
            new_sheet = workbook.Sheets.Add(After=workbook.Sheets(workbook.Sheets.Count))
        new_sheet.Name = sheet_name
        return new_sheet

    @staticmethod
    def move_worksheet(worksheet, before=None, after=None):
        """
        지정테이블

        Args:
            worksheet: 필요의테이블
            before: 까지테이블전
            after: 까지테이블후
        """
        workbook = worksheet.Parent
        ws = worksheet
        if before:
            ws.Move(Before=workbook.Sheets(before))
        elif after:
            ws.Move(After=workbook.Sheets(after))
        else:
            ws.Move(After=workbook.Sheets(workbook.Sheets.Count))

    @staticmethod
    def get_worksheet_name(worksheet) -> str:
        """
        가져오기 테이블이름
        """
        return worksheet.Name

    @staticmethod
    def rename_worksheet(worksheet, new_name: str):
        """
        이름 변경테이블
        """
        worksheet.Name = new_name

    @staticmethod
    def delete_worksheet(worksheet):
        """
        삭제지정테이블
        """
        workbook = worksheet.Parent
        worksheet.Delete()

    @staticmethod
    def copy_worksheet(
        worksheet, excel, location: CopySheetLocationType = CopySheetLocationType.LAST, is_same_workbook=False
    ):
        """
        복사테이블까지지정

        Args:
            worksheet: 복사할의테이블객체
            excel: 목록 객체
            location: 복사위치, BEFORE, AFTER, FIRST, LAST
            is_same_workbook
        """
        # 근거위치실행복사
        target_workbook = excel.obj
        if is_same_workbook:
            # 현재내부복사, BEFORE/AFTER 테이블
            if location == CopySheetLocationType.BEFORE:
                worksheet.Copy(Before=worksheet)
            elif location == CopySheetLocationType.AFTER:
                worksheet.Copy(After=worksheet)
            elif location == CopySheetLocationType.FIRST:
                worksheet.Copy(Before=target_workbook.Worksheets(1))
            elif location == CopySheetLocationType.LAST:
                worksheet.Copy(After=target_workbook.Worksheets(target_workbook.Sheets.Count))
        else:
            # 복사, BEFORE/AFTER 목록 의테이블
            if location == CopySheetLocationType.BEFORE:
                worksheet.Copy(Before=target_workbook.Worksheets(target_workbook.ActiveSheet.Name))
            elif location == CopySheetLocationType.AFTER:
                worksheet.Copy(After=target_workbook.Worksheets(target_workbook.ActiveSheet.Name))
            elif location == CopySheetLocationType.FIRST:
                worksheet.Copy(Before=target_workbook.Worksheets(1))
            elif location == CopySheetLocationType.LAST:
                worksheet.Copy(After=target_workbook.Worksheets(target_workbook.Sheets.Count))

    @staticmethod
    def get_worksheet_used_range(worksheet):
        """설치전체가져오기완료사용"""
        used_range = worksheet.UsedRange
        if used_range is None:
            return 1, 1, 0, 0
        return (
            used_range.Row,
            used_range.Column,
            used_range.Row + used_range.Rows.Count - 1,
            used_range.Column + used_range.Columns.Count - 1,
            used_range.Address,
        )

    @staticmethod
    def get_cell(worksheet, row: int, col: int) -> object:
        """
        가져오기셀 Range 객체

        Args:
            worksheet: 테이블객체
            row: 행(1-based)
            col: 열(1-based)

        Returns:
            셀 Range 객체(worksheet.Cells 반환의예 Range 객체)
        """
        try:
            return worksheet.Cells(row, col)
        except Exception as e:
            raise ValueError(f"가져오기셀({row}, {col})실패: {e}")

    @staticmethod
    def get_range(worksheet, cell: str) -> object:
        """
        가져오기  Range 객체

        Args:
            worksheet: 테이블객체
            cell: 문자열, 예 "A1", "A1:B10"

        Returns:
             Range 객체
        """
        try:
            return worksheet.Range(cell)
        except Exception as e:
            raise ValueError(f"가져오기  '{cell}' 실패: {e}")

    @staticmethod
    def get_rows(worksheet, rows) -> object:
        try:
            return worksheet.Rows(rows)
        except Exception as e:
            raise ValueError(f"가져오기  '{rows}' 실패: {e}")

    @staticmethod
    def get_columns(worksheet, columns) -> object:
        try:
            return worksheet.Columns(columns)
        except Exception as e:
            raise ValueError(f"가져오기  '{columns}' 실패: {e}")

    @staticmethod
    def get_range_from_cells(worksheet, start_cell, end_cell) -> object:
        """
        통신경과개 Range 객체(Cells 객체)가져오기  Range 객체

        Args:
            worksheet: 테이블객체
            start_cell: 셀 Range 객체(예 worksheet.Cells(row, col))
            end_cell: 결과셀 Range 객체(예 worksheet.Cells(row, col))

        Returns:
             Range 객체

        Example:
            start = worksheet.Cells(1, 1)
            end = worksheet.Cells(10, 5)
            range_obj = Worksheet.get_range_from_cells(worksheet, start, end)
        """
        try:
            return worksheet.Range(start_cell, end_cell)
        except Exception as e:
            raise ValueError(f"통신경과 Range 객체가져오기 실패: {e}")

    @staticmethod
    def insert_picture(worksheet, image_path, pic_left=0, pic_top=0, pic_height=300, pic_width=400, pic_scale=1.0):
        """
        삽입이미지, 이미지크기
        """
        picture = worksheet.Pictures().Insert(image_path)

        try:
            shape = picture.ShapeRange.Item(1)
            shape.LockAspectRatio = False
        except Exception:
            # 가져오기ShapeRange가능실패, 내용모듈분WPS대기
            pass

        # 사용
        if pic_scale != 1.0:
            from PIL import Image

            image = Image.open(image_path)
            width, height = image.size
            picture.Width = width * pic_scale
            picture.Height = height * pic_scale
        else:
            picture.Width = pic_width
            picture.Height = pic_height
        picture.Left = pic_left
        picture.Top = pic_top
        return picture

    @staticmethod
    def delete_all_comments(worksheet):
        """
        삭제테이블중의모든비고

        Args:
            worksheet: 테이블객체

        Raises:
            ValueError: 테이블의 저장되지 않은 비고 처리 중 오류
        """
        if worksheet.Comments.Count > 0:
            # 비고: Comments.Item 예의, 삭제후검색변수, 으로매삭제일개
            while worksheet.Comments.Count > 0:
                worksheet.Comments.Item(1).Delete()
        else:
            raise ValueError("아니오저장된 비고")
