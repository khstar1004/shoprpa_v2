import os
import logging
import subprocess
import tempfile
import time
from io import BytesIO

import PIL
from astronverse.actionlib.utils import handle_existence
from astronverse.word import (
    ApplicationType,
    CloseRangeType,
    CommentType,
    ConvertPageType,
    CursorPositionType,
    DeleteMode,
    FileExistenceType,
    InsertImgType,
    InsertionType,
    MoveDirectionType,
    MoveLeftRightType,
    MoveUpDownType,
    ReplaceMethodType,
    ReplaceType,
    RowAlignment,
    SaveFileType,
    SaveType,
    SearchTableType,
    SelectRangeType,
    SelectTextType,
    TableBehavior,
    UnderLineStyle,
    VerticalAlignment,
)
from astronverse.word.core import IDocumentCore
from astronverse.word.error import *
from docx import Document
from docx.shared import RGBColor
from pywpsrpc import rpcwpsapi
from pywpsrpc.rpcwpsapi import *

logger = logging.getLogger(__name__)

APP = None


class WordDocumentCore(IDocumentCore):
    wps_obj = None
    init_key = 0

    @classmethod
    def find_wps_excel(cls):
        """조회excel여부존재함"""
        cmd = "ps -aux | grep 'wps -automation'"
        output = subprocess.check_output(cmd, shell=True, encoding="utf-8", errors="replace")
        lines = output.decode().split("\n")
        pids = []
        for line in lines:
            if "wps -automation" in line and "grep" not in line:
                pid = int(line.split()[1])
                pids.append(pid)
        return pids

    @classmethod
    def create(cls, file_path: str = "") -> object:
        """Word - 문서 - 생성"""
        if file_path and not os.path.exists(os.path.dirname(file_path)):
            raise Exception("경로찾을 수 없는 ")

        global APP
        # 가져오기현재pywpsrpc열기의excel목록
        ps = cls.find_wps_excel()
        if not ps:
            APP = None  # 삭제완료오류, 까지돌아가기실패
            time.sleep(0.2)
            hr, rpc = rpcwpsapi.createWpsRpcInstance()
            hr, app = rpc.getWpsApplication()
            APP = app
        else:
            app = APP

        if not app:
            raise Exception(
                """
                요청 시도WPS다중컴포넌트방식, 방식예아래: 
                열기wps → 오른쪽위역할버튼 → Settings → Others 
                → Change window manage mode… 
                → 선택[Multi-Module Mode]
                """
            )

        hr, cls.doc = app.Documents.Add()
        if file_path:
            cls.doc.SaveAs(FileName=file_path)
        return cls.doc

    @classmethod
    def open(
        cls,
        file_path: str = "",
        default_application: ApplicationType = ApplicationType.DEFAULT,
        visible_flag: bool = True,
        encoding: str = "gbk",
        open_pwd: str = "",
        write_pwd: str = "",
    ) -> object:
        """Word - 문서 - 열기"""
        global APP
        # 가져오기현재pywpsrpc열기의excel목록
        ps = cls.find_wps_excel()
        if not ps:
            APP = None  # 삭제완료오류, 까지돌아가기실패
            time.sleep(0.2)
            _, rpc = createWpsRpcInstance()
            hr, app = rpc.getWpsApplication()
            APP = app
        else:
            app = APP

        if not app:
            raise Exception(
                """
                요청 시도WPS다중컴포넌트방식, 방식예아래: 
                열기wps → 오른쪽위역할버튼 → Settings → Others → Change window manage mode… → 선택[Multi-Module Mode]
                """
            )

        hr, cls.doc = app.Documents.Open(
            file_path,
            PasswordDocument=open_pwd,
            WritePasswordDocument=write_pwd,
            Encoding="gbk",
        )
        if cls.doc:
            return cls.doc

    @classmethod
    def read(cls, doc: object, select_range=SelectRangeType.ALL):
        """
        가져오기Word문서내용
        :param doc: word객체
        :param select_range: 선택
        :return: 문서내용
        """
        global APP

        if select_range == SelectRangeType.SELECTED:
            selection = APP.Selection
            text = selection.Text
            return text
        else:
            doc.Activate()
            text = doc.Content.get_Text()
            text = list(text)
            text[1] = text[1].replace("\r", " ")

            return text[1]

    @staticmethod
    def _doc_save(doc, file_path, file_name, save_type, exist_handle_type):
        if save_type == SaveType.SAVE_AS and file_path:
            file_suffix = "." + doc.Name.split(".")[-1]  # 가져오기파일후, 중지파일이름중있음
            if not file_name:
                file_name = doc.Name.split(file_suffix)[0]
            dst_file = os.path.join(file_path, file_name + file_suffix)
            new_file_path = handle_existence(dst_file, exist_handle_type)
            doc.SaveAs(FileName=new_file_path)
        elif save_type == SaveType.SAVE:
            doc.Save()

    @classmethod
    def save(
        cls,
        doc: object,
        file_path: str = "",
        file_name: str = "",
        save_type=SaveType.SAVE,
        exist_handle_type: FileExistenceType = FileExistenceType.RENAME,
        close_flag: bool = False,
    ) -> object:
        """Word - 문서 - 저장"""
        cls._doc_save(doc, file_path, file_name, save_type, exist_handle_type)
        if close_flag:
            doc.Close(SaveChanges=0)
        return doc

    @classmethod
    def close(
        cls,
        doc: object,
        file_path: str = "",
        file_name: str = "",
        save_type=SaveType.SAVE,
        exist_handle_type: FileExistenceType = FileExistenceType.RENAME,
        close_range_flag: CloseRangeType = CloseRangeType.ONE,
        pkill_flag: bool = False,
    ):
        """
        Word - 문서 - 닫기 저장.결과가 save_flag 예 True, 필요지원 file_path
        """
        if close_range_flag == CloseRangeType.ALL:
            if pkill_flag:
                try:
                    os.system("taskkill /f /im wps.exe")
                    os.system("taskkill /f /im WINWORD.EXE")
                except Exception:
                    pass
            else:
                cls.word_obj.Quit()
            cls.word_obj = None
        else:
            cls._doc_save(doc, file_path, file_name, save_type, exist_handle_type)
            try:
                doc.Close(SaveChanges=0)
            except Exception as e:
                raise e

    @classmethod
    def insert(
        cls,
        doc: object,
        text: str = "",
        enter_flag: bool = False,
        text_format: dict = None,
    ):
        doc.Activate()
        selection = doc.Application.Selection
        if enter_flag:
            selection.TypeParagraph()
        selection.TypeText(text)
        selection.Start = selection.Start - len(text)  # 를돌아가기까지삽입의문서문자열기 위치 32
        selection.End = selection.End  # 를까지삽입의문서문자결과위치
        # 문서문자형식(예추가, 대기)
        selection.Font.Bold = text_format["bold"]
        selection.Font.Italic = text_format["italic"]
        selection.Font.Underline = text_format["underline"].value
        # 문서문자문자
        selection.Font.Name = text_format["font_name"]
        # 문서문자크기
        selection.Font.Size = text_format["font_size"]
        text_format["font_color"].split(",")

    @classmethod
    def replace(
        cls,
        doc: object,
        replace_type: ReplaceType = ReplaceType.STR,
        origin_word: str = "",
        new_word: str = "",
        img_path: str = "",
        replace_method: ReplaceMethodType = ReplaceMethodType.ALL,
        ignore_case: bool = True,
    ):
        """
        Word - 문서내용 - 
        params:replace_type:유형, str/img
                origin_word:기존문서문자/이미지경로
                new_word:새문서문자/이미지경로
                img_path:이미지경로
                replace_method:방법법, first/all
        params:ignore_case:크기,  True/False

        https://learn.microsoft.com/zh-hk/office/vba/api/word.find.execute
        wdReplaceAll	2	가져오기 모든목록.
        wdReplaceNone	0	아니오가져오기 작업목록.
        wdReplaceOne	1	가져오기 일출력의목록.
        """
        doc.Activate()
        selection = doc.Application.Selection
        replace_count = 0
        wdReplaceIndex = 2 if replace_method == ReplaceMethodType.ALL else 1
        if replace_method == ReplaceMethodType.FIRST:
            found = selection.Find
            found.ClearFormatting()
            found.Text = origin_word
            match_case = False if ignore_case else True
            found.MatchCase = match_case
            found.Execute()
            if replace_type == ReplaceType.STR:
                doc_range = doc.Content
                doc_range.Find.Execute(
                    origin_word,
                    match_case,
                    False,
                    False,
                    False,
                    False,
                    True,
                    1,
                    True,
                    new_word,
                    wdReplaceIndex,
                )
            else:
                selection.InlineShapes.AddPicture(img_path)
                selection.TypeBackspace()
            replace_count = 1
        elif replace_method == ReplaceMethodType.ALL:
            selection.HomeKey(Unit=6)
            found = selection.Find
            found.ClearFormatting()
            found.Text = origin_word
            match_case = False if ignore_case else True
            found.MatchCase = match_case
            if replace_type == ReplaceType.STR:
                doc_range = doc.Content
                doc_range.Find.Execute(
                    origin_word,
                    match_case,
                    False,
                    False,
                    False,
                    False,
                    True,
                    1,
                    True,
                    new_word,
                    wdReplaceIndex,
                )
                while found.Found:
                    replace_count += 1
                    found.Execute(Replace=wdReplaceIndex)

            else:
                while found.Execute():
                    range_obj = found.Parent
                    range_obj.Text = ""
                    range_obj.InlineShapes.AddPicture(img_path)
                    replace_count += 1
                    found.Execute(Replace=wdReplaceIndex)

        return replace_count

    @classmethod
    def select(
        cls,
        doc: object,
        select_type: SelectTextType = SelectTextType.ALL,
        p_start: int = 1,
        p_end: int = 1,
        r_start: int = 1,
        r_end: int = 1,
    ):
        doc.Activate()
        s = doc.Application.Selection
        if select_type == SelectTextType.ALL:
            s.WholeStory()
        elif select_type == SelectTextType.ROW:
            s.GoTo(3, 1, r_start)
            drift_num = r_end - r_start + 1
            s.MoveDown(5, drift_num, 1)
            s.EndOf(5, 1)
        elif select_type == SelectTextType.PARAGRAPH:
            s.SetRange(doc.Paragraphs[p_start].Range.Start, doc.Paragraphs[p_end].Range.End)
            s.Select()

    @classmethod
    def cursor_position(
        cls,
        doc,
        by: SelectTextType = SelectTextType.ALL,
        pos: CursorPositionType = CursorPositionType.HEAD,
        content: str = "",
        c_idx: int = 1,
        p_idx: int = 1,
        r_idx: int = 1,
    ):
        doc.Activate()
        s = doc.Application.Selection
        if by == SelectTextType.CONTENT:  # 텍스트위치 지정
            if not content:
                raise BaseException(
                    CONTENT_FORMAT_ERROR_FORMAT,
                    "요청 필요위치 지정의텍스트내용,목록전지원하지 않음빈내용의위치 지정!!!",
                )
            try:
                s.GoTo(3, 1, 1)  # 까지문서일행열기 의방법
                for _ in range(
                    c_idx
                ):  # 개,필요를개데이터;예조회문서중3개"문서문서"개닫기 ,필요3
                    s.Find.Execute(FindText=content, Forward=True, MatchCase=True)
                if pos == CursorPositionType.HEAD:  # 를위치 지정까지닫기 열기 
                    s.SetRange(Start=s.Start, End=s.Start)
                else:
                    s.SetRange(Start=s.End, End=s.End)
            except Exception as e:
                raise BaseException(CONTENT_FORMAT_ERROR_FORMAT, "내용을 찾을 수 없습니다") from e
        elif by == SelectTextType.ALL:  # 문서위치 지정
            try:
                p_num = doc.Paragraphs.Count  # 가져오기전체
                if pos == CursorPositionType.TAIL:  # 개문서
                    s.Move(4, p_num)
                else:  # 까지개문서열기 
                    s.Move(4, -p_num)
            except Exception as e:
                raise BaseException(CONTENT_FORMAT_ERROR_FORMAT, "문서가 비어 있습니다") from e
        elif by == SelectTextType.PARAGRAPH:  # 위치 지정
            if pos == CursorPositionType.TAIL:  # 위치 지정까지개
                s.SetRange(
                    doc.Paragraphs[p_idx].Range.End - 1,
                    doc.Paragraphs[p_idx].Range.End - 1,
                )
            elif pos == CursorPositionType.HEAD:  # 위치 지정까지개열기 
                s.SetRange(doc.Paragraphs[p_idx].Range.Start, doc.Paragraphs[p_idx].Range.Start)
            else:
                raise BaseException(
                    CONTENT_FORMAT_ERROR_FORMAT,
                    "지원하지 않는 위치 매개변수입니다. p_pos 값을 확인하세요",
                )
        elif by == SelectTextType.ROW:
            try:
                if pos == CursorPositionType.HEAD:  # 위치 지정까지행
                    s.GoTo(3, 1, r_idx)
                elif pos == CursorPositionType.TAIL:  # 위치 지정까지행
                    s.GoTo(3, 1, r_idx)
                    s.EndKey(5)
            except Exception as e:
                raise BaseException(CONTENT_FORMAT_ERROR_FORMAT, "내용이 비어 있습니다") from e

    @classmethod
    def move_cursor(
        cls,
        doc: object = None,
        direction: MoveDirectionType = MoveDirectionType.UP,
        unitupdown: MoveUpDownType = MoveUpDownType.ROW,
        unitleftright: MoveLeftRightType = MoveLeftRightType.CHARACTER,
        distance: int = 0,
        with_shift: bool = False,
    ):
        doc.Activate()
        s = doc.Application.Selection
        if direction == MoveDirectionType.UP:
            unit = 5 if unitupdown == MoveUpDownType.ROW else 4
            if with_shift:
                s.MoveUp(unit, distance, 1)
            else:
                s.MoveUp(unit, distance)
        elif direction == MoveDirectionType.DOWN:
            unit = 5 if unitupdown == MoveUpDownType.ROW else 4
            if with_shift:
                s.MoveDown(unit, distance, 1)
            else:
                s.MoveDown(unit, distance)
        elif direction == MoveDirectionType.LEFT:
            unit = 1 if unitleftright == MoveLeftRightType.CHARACTER else 2
            if with_shift:
                s.MoveLeft(unit, distance, 1)
            else:
                s.MoveLeft(unit, distance)
        elif direction == MoveDirectionType.RIGHT:
            unit = 1 if unitleftright == MoveLeftRightType.CHARACTER else 2
            if with_shift:
                s.MoveRight(unit, distance, 1)
            else:
                s.MoveRight(unit, distance)
        else:
            raise BaseException(
                CONTENT_FORMAT_ERROR_FORMAT,
                "지원하지 않는 방향 매개변수입니다. direction 값을 확인하세요",
            )

    @classmethod
    def insert_sep(cls, doc: object = None, sep_type: InsertionType = InsertionType.PARAGRAPH):
        doc.Activate()
        s = doc.Application.Selection
        if sep_type == InsertionType.PAGE:
            s.InsertNewPage()
        elif sep_type == InsertionType.PARAGRAPH:
            s.InsertParagraph()
        else:
            raise BaseException(
                CONTENT_FORMAT_ERROR_FORMAT,
                "지원하지 않는 구분 기호 유형입니다. sep_type 값을 확인하세요",
            )

    @classmethod
    def insert_hyperlink(cls, doc: object = None, url: str = "", display: str = ""):
        doc.Activate()
        s = doc.Application.Selection
        start = s.Start
        s.TypeText(url)
        end = s.End
        s.SetRange(start, end)
        if display:
            doc.Hyperlinks.Add(s.Range, Address=url, TextToDisplay=display)
        else:
            doc.Hyperlinks.Add(s.Range, Address=url, TextToDisplay=url)

    @classmethod
    def insert_img(
        cls,
        doc,
        img_from: InsertImgType = InsertImgType.FILE,
        img_path: str = "",
        scale: int = 100,
        newline: bool = False,
    ):
        doc.Activate()
        s = doc.Application.Selection
        if newline:
            s.TypeParagraph()
        if img_from == InsertImgType.CLIPBOARD:
            # 사용xclip가져오기 중의데이터
            try:
                # 시도가져오기파일 경로
                process = subprocess.Popen(
                    ["xclip", "-selection", "clipboard", "-o"],
                    stdin=subprocess.DEVNULL,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.DEVNULL,
                    encoding="utf-8",
                    errors="replace",
                )
                clipboard_data = process.communicate()[0].strip()
            except subprocess.CalledProcessError as e:
                raise BaseException("클립보드 데이터를 가져오지 못했습니다") from e

            # 조회데이터여부로파일 경로, 파일후로 .png, .jpg, .jpeg
            if os.path.isfile(clipboard_data):
                file_ext = os.path.splitext(clipboard_data)[1].lower()
                if file_ext in [".png", ".jpg", ".jpeg"]:
                    # 직선연결삽입이미지
                    _, img_shape = s.InlineShapes.AddPicture(clipboard_data)
                    img_shape.ScaleWidth = scale
                    img_shape.ScaleHeight = scale
                    return

            # 결과가데이터아니오예파일 경로또는아니오예지원의이미지형식, 이면시도가져오기이미지데이터
            try:
                process = subprocess.Popen(
                    ["xclip", "-selection", "clipboard", "-t", "image/png", "-o"],
                    stdin=subprocess.DEVNULL,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.DEVNULL,
                )
                image_data, _ = process.communicate()
                image = PIL.Image.open(BytesIO(image_data))
            except subprocess.CalledProcessError as e:
                raise BaseException("클립보드에 이미지 데이터가 없습니다") from e
            if image.mode != "RGB":
                image = image.convert("RGB")

            # 임시 파일로 저장한 뒤 Word에 삽입한다.
            with tempfile.NamedTemporaryFile(delete=False, suffix=".png") as temp_file:
                temp_file_path = temp_file.name
                image.save(temp_file, format="PNG")

            try:
                _, img_shape = s.InlineShapes.AddPicture(temp_file_path)
                img_shape.ScaleWidth = scale
                img_shape.ScaleHeight = scale
            finally:
                os.remove(temp_file_path)

        else:
            _, img_shape = s.InlineShapes.AddPicture(img_path)
            logger.debug("Inserted image shape: %s", img_shape)
            if not os.path.isfile(img_path):
                raise BaseException(DOCUMENT_PATH_ERROR_FORMAT.format(img_path), "이미지 경로 오류")
        img_shape.ScaleWidth = scale
        img_shape.ScaleHeight = scale

    @classmethod
    def read_table(
        cls,
        doc: object,
        search_type: SearchTableType = SearchTableType.IDX,
        idx: int = 1,
        text: str = "",
    ):
        doc.Activate()
        table_content = []

        if search_type == SearchTableType.IDX:
            try:
                # 가져오기  idx 개테이블
                table = doc.Tables[idx]
                table_content = cls._extract_table_content(table)
            except Exception as e:
                raise BaseException(TABLE_NOT_EXIST_ERROR.format("순서" + str(idx))) from e
        elif search_type == SearchTableType.TEXT:
            # 모든테이블, 조회패키지지정텍스트의테이블
            count = 0
            t_cnt = 1
            while t_cnt <= doc.Tables.Count:
                table = doc.Tables[t_cnt]
                row_cnt = 1
                found = False
                while row_cnt <= table.Rows.Count:
                    row = table.Rows[row_cnt]
                    cell_cnt = 1
                    while cell_cnt <= row.Cells.Count:
                        cell = row.Cells[cell_cnt]
                        if text in cell.Range.Text:
                            count += 1
                            if count == idx:
                                table_content = cls._extract_table_content(table)
                                found = True
                                break
                        cell_cnt += 1
                    if found:
                        break
                    row_cnt += 1
                if found:
                    break
                t_cnt += 1
            if not table_content:
                raise BaseException(TABLE_NOT_EXIST_ERROR.format("내용" + str(text)))

        return table_content

    @classmethod
    def insert_table(
        cls,
        doc: object,
        table_content: list = "",
        table_behavior: TableBehavior = TableBehavior.DEFAULT,
        alignment: RowAlignment = RowAlignment.LEFT,
        v_alignment: VerticalAlignment = VerticalAlignment.TOP,
        border: bool = True,
        if_change_font: bool = False,
        font_size=None,
        font_color=None,
        font_set=None,
        font_bold: bool = False,
        font_italic: bool = False,
        underline: UnderLineStyle = UnderLineStyle.DEFAULT,
        newline: bool = True,
    ):
        rows = len(table_content)
        cols = len(table_content[0]) if rows > 0 else 0
        doc.Activate()
        range = doc.Application.Selection.Range
        # 삽입테이블
        _, table = doc.Tables.Add(range, rows, cols)
        table.AutoFitBehavior(table_behavior == TableBehavior.AUTO)

        for row_idx, row_data in enumerate(table_content):
            hr, row = table.Rows.Item(row_idx + 1)
            for col_idx, cell_data in enumerate(row_data):
                hr, cell = row.Cells.Item(col_idx + 1)
                cell.Range.Text = cell_data
                cell.Range.Paragraphs.Alignment = alignment.value
                cell.VerticalAlignment = v_alignment.value
                if if_change_font:
                    # 문자속성
                    run = cell.Range.Font
                    run.Name = font_set or ""
                    run.Size = font_size or 12
                    run.Bold = font_bold
                    run.Italic = font_italic
                    run.Underline = underline.value if underline else 0
                    if font_color:
                        run.Color = RGBColor(*font_color).rgb

                if newline:
                    cell.Range.Text += "\n"

            # Set table border style
        if border:
            tblBorders = table.Borders
            tblBorders.InsideLineStyle = 1  # 
            tblBorders.OutsideLineStyle = 1  # 
            tblBorders.InsideLineWidth = 4
            tblBorders.OutsideLineWidth = 4
            tblBorders.InsideColor = 0x000000  # 색상
            tblBorders.OutsideColor = 0x000000  # 색상

    @classmethod
    def delete(
        cls,
        doc: object,
        delete_mode: DeleteMode = DeleteMode.ALL,
        delete_str: str = "",
        delete_idx: int = 0,
        str_delete_all: bool = False,
        p_start: int = 0,
        c_start: int = 0,
        p_end: int = 0,
        c_end: int = 0,
    ):
        doc.Activate()
        if delete_mode == DeleteMode.ALL:
            # 삭제모든내용
            doc.Content.Delete()
        elif delete_mode == DeleteMode.CONTENT:
            # 삭제{delete_idx}개내용로{delete_str}의요소
            if str_delete_all:
                # 삭제모든내용로{delete_str}의요소
                p_cnt = 1
                while p_cnt <= doc.Paragraphs.Count:
                    paragraph = doc.Paragraphs[p_cnt]
                    if delete_str in paragraph.Range.Text:
                        paragraph.Range.Text = paragraph.Range.Text.replace(delete_str, "")
                    p_cnt += 1
            else:
                count = 0
                p_cnt = 1
                while p_cnt <= doc.Paragraphs.Count:
                    paragraph = doc.Paragraphs[p_cnt]
                    if delete_str in paragraph.Range.Text:
                        text = paragraph.Range.Text
                        start_pos = text.find(delete_str)
                        while start_pos != -1:
                            count += 1
                            if count == delete_idx:
                                end_pos = start_pos + len(delete_str)
                                paragraph.Range.Text = text[:start_pos] + text[end_pos:]
                                break
                            start_pos = text.find(delete_str, start_pos + len(delete_str))
                    p_cnt += 1
        elif delete_mode == DeleteMode.RANGE:
            # 삭제에서{p_start}{c_start}개문자까지{p_end}{c_end}개문자의내용
            start_paragraph = doc.Paragraphs[p_start]
            end_paragraph = doc.Paragraphs[p_end]

            # 가져오기열기 및결과위치의문자
            start_range = start_paragraph.Range.Characters[c_start + 1]
            end_range = end_paragraph.Range.Characters[c_end]

            # 선택에서열기 위치까지결과위치의내용
            doc.Application.Selection.SetRange(start_range.Start, end_range.End)

            # 삭제선택중의내용
            doc.Application.Selection.Delete()

    @classmethod
    def create_comment(
        cls,
        doc: object = None,
        paragraph_idx: int = 1,
        start: int = 1,
        end: int = 1,
        comment: str = "",
        comment_type: CommentType = CommentType.POSITION,
        target_str: str = "",
        comment_all: bool = True,
        comment_index: int = 1,
    ):
        doc.Activate()
        selection = doc.Application.Selection
        if comment_type == CommentType.POSITION:
            selection.SetRange(
                doc.Paragraphs[paragraph_idx].Range.Start,
                doc.Paragraphs[paragraph_idx].Range.End,
            )
            label_text = doc.Paragraphs[paragraph_idx].Range.Text[start:end]
            selection.Find.Execute(label_text)
            doc.Comments.Add(selection.Range, comment)
        elif comment_type == CommentType.CONTENT:
            if not target_str:
                return
            if comment_all:
                selection.HomeKey(Unit=6, Extend=0)
                previous_range = None
                while True:
                    found = selection.Find.Execute(FindText=target_str, Forward=True, MatchCase=True)
                    if not found:
                        break

                        # 조회현재선택여부및위일개
                    current_range = (selection.Range.Start, selection.Range.End)
                    if current_range == previous_range:
                        break

                    # 추가비고
                    doc.Comments.Add(selection.Range, comment)

                    # 업데이트위일개
                    previous_range = current_range

            else:
                find_times = 0
                selection.HomeKey(Unit=6, Extend=0)  # 를삽입까지까지문서위치
                while selection.Find.Execute(FindText=target_str, Forward=True, MatchCase=True):
                    find_times += 1
                    if find_times == comment_index:
                        doc.Comments.Add(selection.Range, comment)
                        break

    @classmethod
    def delete_comment(cls, doc: object = None, comment_index: int = 1, delete_all: bool = False):
        doc.Activate()
        if delete_all is True:
            count = doc.Comments.Count
            for i in range(0, count):
                doc.Comments[count - i].DeleteRecursively()
        else:
            doc.Comments[comment_index].DeleteRecursively()

    @classmethod
    def convert_to_txt(
        cls,
        doc: object = None,
        output_path: str = "",
        output_name: str = "",
        save_type: SaveFileType = SaveFileType.WARN,
    ):
        filename = f"{output_name}.txt"
        if WordDocumentCore.check_file_in_path(output_path, filename):
            if save_type == SaveFileType.WARN:
                raise BaseException(FILENAME_ALREADY_EXISTS_ERROR.format(filename), "")
            if save_type == SaveFileType.GENERATE:
                # 완료재복사파일이름
                counter = 1
                oldfilename, _ = os.path.splitext(filename)
                while WordDocumentCore.check_file_in_path(output_path, filename):
                    filename = f"{oldfilename}_{counter}.txt"
                    counter += 1

            elif save_type == SaveFileType.OVERWRITE:
                fullpath = os.path.join(output_path, filename)
                os.remove(fullpath)
        oldfilepath = doc.FullName
        pydoc = Document(oldfilepath)
        new_path = os.path.join(output_path, filename)
        with open(new_path, "w", encoding="utf-8") as txt_file:
            txt_file.writelines(para.text + "\n" for para in pydoc.paragraphs)

    @classmethod
    def convert_to_pdf(
        cls,
        doc: object = None,
        output_path: str = "",
        output_name: str = "새생성PDF",
        page_type: ConvertPageType = ConvertPageType.ALL,
        page_start: int = 1,
        page_end: int = 1,
        save_type: SaveFileType = SaveFileType.WARN,
    ):
        filename = f"{output_name}.pdf"
        if WordDocumentCore.check_file_in_path(output_path, filename):
            if save_type == SaveFileType.WARN:
                raise BaseException(FILENAME_ALREADY_EXISTS_ERROR.format(filename), "")
            if save_type == SaveFileType.GENERATE:
                # 완료재복사파일이름
                counter = 1
                oldfilename, _ = os.path.splitext(filename)
                while WordDocumentCore.check_file_in_path(output_path, filename):
                    filename = f"{oldfilename}_{counter}.pdf"
                    counter += 1

        new_path = os.path.join(output_path, filename)
        doc.ExportAsFixedFormat(
            new_path,
            17,
            OpenAfterExport=True,
            Range=page_type.value,
            From=page_start,
            To=page_end,
        )
