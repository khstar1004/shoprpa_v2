import io
import os
import re
import tempfile

import psutil
import win32clipboard
import win32com.client as wc
from astronverse.actionlib.logger import logger
from astronverse.actionlib.types import PATH
from astronverse.actionlib.utils import FileExistenceType, handle_existence
from astronverse.word import (
    ApplicationType,
    CloseRangeType,
    CommentType,
    ConvertPageType,
    CursorPointerType,
    CursorPositionType,
    DeleteMode,
    EncodingType,
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
from win32api import RGB


class WordDocumentCore(IDocumentCore):
    word_application_instance = None

    @staticmethod
    def _is_word_application_running():
        is_word_running, is_wps_running = False, False
        process_ids = psutil.pids()
        for process_id in process_ids:
            process = psutil.Process(process_id)
            if process.name() == "WINWORD.EXE":
                is_word_running = True
            if process.name() == "wps.exe":
                is_wps_running = True
        return is_word_running, is_wps_running

    @staticmethod
    def _save_document(doc, file_path, file_name, save_type, exist_handle_type):
        if save_type == SaveType.SAVE_AS and file_path:
            document_extension = "." + doc.Name.split(".")[-1]  # 가져오기파일후, 중지파일이름중있음
            if not file_name:
                file_name = doc.Name.split(document_extension)[0]
            destination_file_path = os.path.join(file_path, file_name + document_extension)
            new_file_path = handle_existence(destination_file_path, exist_handle_type)
            doc.SaveAs(FileName=new_file_path)
            return new_file_path
        if save_type == SaveType.SAVE:
            doc.Save()
            return doc.FullName
        return None

    @staticmethod
    def _create_word_application(params: str):
        try:
            word_application = wc.gencache.EnsureDispatch(params)
            return word_application
        except Exception:
            try:
                word_application = wc.Dispatch(params)
                return word_application
            except Exception:
                logger.debug("Word 객체 생성 실패: %s", params)
                return None

    @classmethod
    def initialize_word_application(cls, default_application: ApplicationType = ApplicationType.DEFAULT):
        is_word_running, _ = cls._is_word_application_running()
        if default_application == ApplicationType.DEFAULT and is_word_running:
            keys = ["Word.Application", "Kwps.Application", "wps.Application"]
        elif default_application == ApplicationType.DEFAULT:
            keys = ["Kwps.Application", "wps.Application", "Word.Application"]
        elif default_application == ApplicationType.WORD:
            keys = ["Word.Application"]
        elif default_application == ApplicationType.WPS:
            keys = ["Kwps.Application", "wps.Application"]
        else:
            keys = []

        for application_key in keys:
            cls.word_application_instance = cls._create_word_application(application_key)
            if cls.word_application_instance:
                return cls.word_application_instance

        # 시도재생성저장
        try:
            wc.gencache.Rebuild()
            wc.gencache.EnsureModule("{00020905-0000-0000-C000-000000000046}", 0, 8, 7)
            for application_key in keys:
                cls.word_application_instance = cls._create_word_application(application_key)
                if cls.word_application_instance:
                    return cls.word_application_instance
        except Exception as e:
            raise Exception("Word COM 초기화에 실패했습니다. %LOCALAPPDATA%\\Temp\\gen_py 디렉터리를 삭제한 뒤 다시 시도하세요.")

        raise Exception("사용 가능한 WPS 또는 Microsoft Word 설치 정보를 찾지 못했습니다.")

    @classmethod
    def open(
        cls,
        document_path: PATH = "",
        preferred_application: ApplicationType = ApplicationType.WORD,
        is_visible: bool = True,
        text_encoding: EncodingType = EncodingType.UTF8,
        open_password: str = "",
        write_password: str = "",
    ) -> object:
        """
        열기Word파일
        :param document_path: word파일 경로
        :param preferred_application: 열기의사용
        :param is_visible: 여부가능
        :param text_encoding: 코드형식
        :param open_password: 열기비밀번호
        :param write_password: 입력비밀번호
        :return: word객체
        """
        if cls.word_application_instance is None:
            cls.initialize_word_application(preferred_application)
            # cls.word_application_instance.Visible = True
        if is_visible:
            cls.word_application_instance.Visible = True
        if document_path:
            cls.ScreenUpdating = True
            cls.word_application_instance.DisplayAlerts = False
            document = cls.word_application_instance.Documents.Open(
                FileName=document_path,
                PasswordDocument=open_password,
                WritePasswordDocument=write_password,
                Encoding=text_encoding,
                Visible=is_visible,
            )
            cls.word_application_instance.DisplayAlerts = True
            logger.debug("Opened Word document: %s", document.Name)
        else:
            raise LookupError("Word 파일 경로가 비어 있거나 올바르지 않습니다.")
        return document

    @classmethod
    def read(cls, document: object, content_selection_range=SelectRangeType.ALL):
        """
        가져오기Word문서내용
        :param document: word객체
        :param content_selection_range: 선택
        :return: 문서내용
        """
        if not cls.word_application_instance:
            cls.word_application_instance = document.Application
            cls.word_application_instance.Visible = True
        if content_selection_range == SelectRangeType.SELECTED:
            selection = cls.word_application_instance.Selection
            return re.sub(r"\r", "\n", selection.Text)

        document_content = ""
        for paragraph in document.Paragraphs:
            paragraph_text = re.sub(r"\r", "", paragraph.Range.Text) + "\n"
            document_content += paragraph_text
        return document_content

    @classmethod
    def create(
        cls,
        file_path: str = "",
        file_name: str = "",
        visible_flag: bool = True,
        default_application: ApplicationType = ApplicationType.WORD,
        exist_handle_type: FileExistenceType = FileExistenceType.RENAME,
    ) -> tuple[object, str]:
        """
        Word - 문서 - 생성
        """
        if cls.word_application_instance is None:
            cls.initialize_word_application(default_application)
            cls.word_application_instance.Visible = True

        doc = cls.word_application_instance.Documents.Add()  #  Visible 매개변수으로내용 WPS
        # 관리저장경로
        new_file_path = ""
        if file_path and file_name:
            tentative_path = os.path.join(file_path, file_name)
            new_file_path = IDocumentCore.handle_existence(tentative_path, exist_handle_type)
            if new_file_path:
                try:
                    doc.SaveAs(FileName=new_file_path)
                except Exception as e:
                    raise RuntimeError(f"문서저장실패: {e}")
        return doc, new_file_path

    @classmethod
    def save(
        cls,
        doc: object,
        file_path: str = "",
        file_name: str = "",
        save_type=SaveType.SAVE,
        exist_handle_type: FileExistenceType = FileExistenceType.RENAME,
        close_flag: bool = False,
    ) -> PATH:
        """
        Word - 문서 - 저장
        """
        save_file_path = cls._save_document(doc, file_path, file_name, save_type, exist_handle_type)
        if close_flag:
            doc.Close(SaveChanges=0)
        return save_file_path

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
                cls.word_application_instance.Quit()
            cls.word_application_instance = None
        else:
            cls._save_document(doc, file_path, file_name, save_type, exist_handle_type)
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
        # 서식 적용에 실패해도 텍스트 삽입 자체는 유지한다.
        try:
            selection.Start = selection.Start - len(text)  # 를돌아가기까지삽입의문서문자열기 위치
            selection.End = selection.End  # 를까지삽입의문서문자결과위치
            # 문서문자형식(예추가, 대기)
            selection.Font.Bold = text_format["bold"]
            selection.Font.Italic = text_format["italic"]
            selection.Font.Underline = 1 if text_format["underline"] else 0
            selection.Font.Name = text_format["font_name"]
            selection.Font.Size = text_format["font_size"]
            rgb_color = text_format["font_color"].split(",")
            selection.Font.Color = RGB(int(rgb_color[0]), int(rgb_color[1]), int(rgb_color[2]))
        except Exception as e:
            logger.warning("Word 삽입 후 서식 적용 실패(텍스트 삽입은 완료됨): %s", e)
        try:
            selection.Start = selection.End  # 를까지
        except Exception as e:
            logger.warning("Word 커서 위치 이동 실패: %s", e)

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
            match_case = not ignore_case
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
            match_case = not ignore_case
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
                while found.Execute():
                    replace_count += 1
            else:
                while found.Execute():
                    range_obj = found.Parent
                    range_obj.Text = ""
                    range_obj.InlineShapes.AddPicture(img_path)
                    replace_count += 1

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
            s.SetRange(
                Start=doc.Paragraphs(p_start).Range.Start,
                End=doc.Paragraphs(p_end).Range.End,
            )
            s.Select()

    @classmethod
    def cursor_position(
        cls,
        doc,
        by: CursorPointerType = CursorPointerType.ALL,
        pos: CursorPositionType = CursorPositionType.HEAD,
        content: str = "",
        c_idx: int = 1,
        p_idx: int = 1,
        r_idx: int = 1,
    ):
        doc.Activate()
        s = doc.Application.Selection
        if by == CursorPointerType.CONTENT:  # 텍스트위치 지정
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
        elif by == CursorPointerType.ALL:  # 문서위치 지정
            try:
                p_num = doc.Paragraphs.Count  # 가져오기전체
                if pos == CursorPositionType.TAIL:  # 개문서
                    s.Move(4, p_num)
                else:  # 까지개문서열기 
                    s.Move(4, -p_num)
            except Exception as e:
                raise BaseException(CONTENT_FORMAT_ERROR_FORMAT, "문서가 비어 있습니다") from e
        elif by == CursorPointerType.PARAGRAPH:  # 위치 지정
            if pos == CursorPositionType.TAIL:  # 위치 지정까지개
                s.SetRange(
                    Start=doc.Paragraphs(p_idx).Range.End - 1,
                    End=doc.Paragraphs(p_idx).Range.End - 1,
                )
            elif pos == CursorPositionType.HEAD:  # 위치 지정까지개열기 
                s.SetRange(
                    Start=doc.Paragraphs(p_idx).Range.Start,
                    End=doc.Paragraphs(p_idx).Range.Start,
                )
            else:
                raise BaseException(
                    CONTENT_FORMAT_ERROR_FORMAT,
                    "지원하지 않는 위치 매개변수입니다. p_pos 값을 확인하세요",
                )
        elif by == CursorPointerType.ROW:
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
        s.SetRange(Start=start, End=end)
        if display:
            doc.Hyperlinks.Add(Anchor=s.Range, Address=url, TextToDisplay=display)
        else:
            doc.Hyperlinks.Add(Anchor=s.Range, Address=url, TextToDisplay=url)

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
            # 조회여부있음이미지데이터
            win32clipboard.OpenClipboard()
            if win32clipboard.IsClipboardFormatAvailable(win32clipboard.CF_HDROP):
                file_paths = win32clipboard.GetClipboardData(win32clipboard.CF_HDROP)
                for file_path in file_paths:
                    if file_path.lower().endswith((".png", ".jpg", ".jpeg")):
                        img_shape = s.InlineShapes.AddPicture(file_path)
                        img_shape.ScaleWidth = scale
                        img_shape.ScaleHeight = scale
                        return
            # 조회중여부있음위치이미지데이터
            if win32clipboard.IsClipboardFormatAvailable(win32clipboard.CF_DIB):
                img = win32clipboard.GetClipboardData(win32clipboard.CF_DIB)
            elif win32clipboard.IsClipboardFormatAvailable(win32clipboard.CF_BITMAP):
                img = win32clipboard.GetClipboardData(win32clipboard.CF_BITMAP)
            elif win32clipboard.IsClipboardFormatAvailable(win32clipboard.CF_DIBV5):
                img = win32clipboard.GetClipboardData(win32clipboard.CF_DIBV5)
            else:
                win32clipboard.CloseClipboard()
                raise BaseException(CLIPBOARD_PASTE_ERROR.format("이미지 데이터"), "")
            image = Image.open(io.BytesIO(img))
            if image.mode != "RGB":
                image = image.convert("RGB")
            with tempfile.NamedTemporaryFile(delete=False, suffix=".png") as temp_file:
                temp_file_path = temp_file.name
                logger.debug("Temporary image path: %s", temp_file_path)
                image.save(temp_file_path, "PNG")
                img_shape = s.InlineShapes.AddPicture(temp_file_path)
                img_shape.ScaleWidth = scale
                img_shape.ScaleHeight = scale

                os.remove(temp_file_path)
            win32clipboard.CloseClipboard()

        else:
            img_shape = s.InlineShapes.AddPicture(img_path)
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
                table = doc.Tables(idx)
                table_content = cls._extract_table_content(table)
            except Exception as e:
                raise BaseException(TABLE_NOT_EXIST_ERROR.format("순서" + str(idx))) from e
        elif search_type == SearchTableType.TEXT:
            # 모든테이블, 조회패키지지정텍스트의테이블
            count = 0
            for table in doc.Tables:
                if any(text in cell.Range.Text for row in table.Rows for cell in row.Cells):
                    count += 1
                    if count == idx:
                        table_content = cls._extract_table_content(table)
                        return table_content
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
        selection_range = doc.Application.Selection.Range
        # 삽입테이블
        table = doc.Tables.Add(selection_range, NumRows=rows, NumColumns=cols)
        table.AutoFitBehavior(table_behavior == TableBehavior.AUTO)

        for row_idx, row_data in enumerate(table_content):
            row = table.Rows.Item(row_idx + 1)
            for col_idx, cell_data in enumerate(row_data):
                cell = row.Cells.Item(col_idx + 1)
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

                    # 가져오기또는추가 rPr 요소
                    # rPr = run._element.get_or_add_rPr()
                    # rFonts = rPr.find(qn('w:rFonts'))
                    # if rFonts is None:
                    #     rFonts = OxmlElement('w:rFonts')
                    #     rPr.append(rFonts)
                    # rFonts.set(qn('w:eastAsia'), font_set if font_set else '')
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
                for paragraph in doc.Paragraphs:
                    if delete_str in paragraph.Range.Text:
                        paragraph.Range.Text = paragraph.Range.Text.replace(delete_str, "")
            else:
                count = 0
                for paragraph in doc.Paragraphs:
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
        elif delete_mode == DeleteMode.RANGE:
            # 삭제에서{p_start}{c_start}개문자까지{p_end}{c_end}개문자의내용
            start_paragraph = doc.Paragraphs(p_start)
            end_paragraph = doc.Paragraphs(p_end)

            # 가져오기열기 및결과위치의문자
            start_range = start_paragraph.Range.Characters(c_start + 1)
            end_range = end_paragraph.Range.Characters(c_end)

            # 선택에서열기 위치까지결과위치의내용
            doc.Application.Selection.SetRange(Start=start_range.Start, End=end_range.End)

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
                Start=doc.Paragraphs(paragraph_idx).Range.Start,
                End=doc.Paragraphs(paragraph_idx).Range.End,
            )
            label_text = doc.Paragraphs(paragraph_idx).Range.Text[start:end]
            selection.Find.Execute(label_text)
            doc.Comments.Add(Range=selection.Range, Text=comment)
        elif comment_type == CommentType.CONTENT:
            if not target_str:
                return
            if comment_all:
                selection.HomeKey(Unit=6, Extend=0)  # 를삽입까지까지문서위치
                while selection.Find.Execute(FindText=target_str, Forward=True, MatchCase=True):
                    doc.Comments.Add(Range=selection.Range, Text=comment)
            else:
                find_times = 0
                selection.HomeKey(Unit=6, Extend=0)  # 를삽입까지까지문서위치
                while selection.Find.Execute(FindText=target_str, Forward=True, MatchCase=True):
                    find_times += 1
                    if find_times == comment_index:
                        doc.Comments.Add(Range=selection.Range, Text=comment)
                        break

    @classmethod
    def delete_comment(cls, doc: object = None, comment_index: int = 1, delete_all: bool = False):
        doc.Activate()
        if delete_all is True:
            count = doc.Comments.Count
            for i in range(0, count):
                doc.Comments(count - i).DeleteRecursively()
        else:
            doc.Comments(comment_index).DeleteRecursively()

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
            OutputFileName=new_path,
            ExportFormat=17,
            OpenAfterExport=True,
            Range=page_type.value,
            From=page_start,
            To=page_end,
        )
