"""
인쇄핵심 서비스모듈

다중파일유형(Word, Excel, PDF, 이미지)의인쇄공가능, 지원지정인쇄매개변수.
패키지프린터관리관리, 인쇄작업큐, 인쇄상태조회대기방법법.

필요유형: 
    PrinterCore: 인쇄서비스유형, 패키지유형인쇄방법법및도구방법법.
"""

import os
import queue
import subprocess
import sys
import time

import win32com
import win32com.client as wc
import win32print
import win32ui
from astronverse.baseline.logger.logger import logger
from astronverse.system import BatchType, DocAppType, FileType, XlsAppType
from PIL import Image, ImageWin


class PrinterCore:
    """
    인쇄핵심 서비스유형.

     Word, Excel, PDF, 이미지대기파일의인쇄공가능, 지원지정매개변수.
    패키지프린터관리관리, 인쇄작업큐, 인쇄상태조회대기방법법.

    """

    IMAGE_TYPES = {
        ".bmp",
        ".bufr",
        ".cur",
        ".dcx",
        ".eps",
        ".fits",
        ".fli",
        ".flc",
        ".fpx",
        ".gbr",
        ".gd",
        ".gif",
        ".grib",
        ".ico",
        ".im",
        ".imt",
        ".jpeg",
        ".mcidas",
        ".mic",
        ".mpeg",
        ".msp",
        ".palm",
        ".pcd",
        ".pcx",
        ".pixar",
        ".png",
        ".ppm",
        ".psd",
        ".sgi",
        ".spider",
        ".tga",
        ".tiff",
        ".wal",
        ".wmf",
        ".xbm",
        ".xpm",
        ".jpg",
    }
    GHOSTSCRIPT_PATH = "gswin32c"
    _GHOSTSCRIPT_PATH = os.path.join(os.path.dirname(__file__), "lib", GHOSTSCRIPT_PATH)

    def __init__(self):
        self.word_obj = None
        self.excel_obj = None

    @staticmethod
    def _create_app(params: str):
        try:
            app_obj = win32com.client.gencache.EnsureDispatch(params)  # type: ignore
            return app_obj
        except Exception:
            try:
                app_obj = win32com.client.Dispatch(params)
                return app_obj
            except Exception:
                logger.info(f"생성app객체실패: {params}")

    def init_word_app(self, default_application: DocAppType = DocAppType.WORD):
        """ word app"""
        keys = [
            "Word.Application",
            "Kwps.Application",
            "wps.Application",
        ]
        if default_application == DocAppType.WORD.value:
            keys = ["Word.Application"]
        elif default_application == DocAppType.WPS.value:
            keys = ["Kwps.Application", "wps.Application"]

        for key in keys:
            self.word_obj = self._create_app(key)
            if self.word_obj:
                return self.word_obj

        # 시도재생성저장
        try:
            wc.gencache.Rebuild()
            wc.gencache.EnsureModule("{00020813-0000-0000-C000-000000000046}", 0, 8, 7)
            for key in keys:
                self.word_obj = self._create_app(key)
                if self.word_obj:
                    return self.word_obj
        except:
            raise Exception(r"실패, 요청 시도삭제 %LOCALAPPDATA%\Temp\gen_py 디렉터리실행!")

        raise Exception("감지하지 못한 wps및office회원가입테이블정보!")

    def init_excel_app(self, default_application: XlsAppType = XlsAppType.EXCEL):
        """ excel app"""
        keys = [
            "Excel.Application",
            "Ket.Application",
            "et.Application",
            "Kwps.Application",
            "wps.Application",
        ]
        if default_application == XlsAppType.EXCEL.value:
            keys = ["Excel.Application"]
        elif default_application == XlsAppType.WPS.value:
            keys = ["Ket.Application", "et.Application", "Kwps.Application", "wps.Application"]

        for key in keys:
            self.excel_obj = self._create_app(key)
            if self.excel_obj:
                return self.excel_obj

        # 시도재생성저장
        try:
            wc.gencache.Rebuild()
            wc.gencache.EnsureModule("{00020813-0000-0000-C000-000000000046}", 0, 8, 7)
            for key in keys:
                self.excel_obj = self._create_app(key)
                if self.excel_obj:
                    return self.excel_obj
        except:
            raise Exception(r"실패, 요청 시도삭제 %LOCALAPPDATA%\Temp\gen_py 디렉터리실행!")

        raise Exception("감지하지 못한 wps및office회원가입테이블정보!")

    def run(self, printer_name="", print_file=None, printer_type="", file_type="", batch_print="", **kwargs):
        """
        실행인쇄작업.
        근거프린터이름및파일유형, 분까지의인쇄방법법.
        지요소량인쇄및지정매개변수.

        Args:
            printer_name (str): 프린터이름.
            print_file (str|list): 대기인쇄파일 경로또는파일목록.
            printer_type (str): 인쇄유형(default/custom).
            **kwargs: 지정매개변수.

        Returns:
            list: 매개인쇄작업의실행 결과.
        """
        logger.info(f"선택의프린터: {printer_name}")
        # 량인쇄시필터링파일유형
        if batch_print == BatchType.BATCH.value and print_file:
            print_file = self.file_type_batch(file_type=file_type, print_file=print_file)

        logger.info(f"선택의파일유형: {file_type}, 인쇄의파일: {print_file}")

        if printer_name in ["프린터", ""]:
            _default_printer_name = win32print.GetDefaultPrinter()
            logger.info(f"가져오기까지의프린터이름로: {_default_printer_name}")
        else:
            all_printers = PrinterCore.view_printer()
            logger.info(f"가져오기까지의프린터목록로: {all_printers}")
            if all_printers and printer_name not in all_printers:
                raise ValueError("전송되지 않았습니다 {} 프린터, 확인하세요프린터이름.".format(printer_name))
            _default_printer_name = printer_name

        if not print_file:
            raise ValueError("대기인쇄파일비어 있습니다, 확인하세요파일 경로정보")

        print_queue = PrinterCore.generate_printer_task(
            _default_printer_name, print_file, printer_type, file_type, **kwargs
        )

        if print_queue is None:
            raise ValueError("{} 프린터지원하지 않음인쇄, 확인하세요프린터정보".format(printer_name))

        flags = []
        while not print_queue.empty():
            task = print_queue.get()
            flag = task["print_function"](
                self,
                printer_name=task["printer_name"],
                file_path=task["file_path"],
                printer_type=task["printer_type"],
                **task["attributes"],
            )
            time.sleep(2)
            flags.append(flag)
        return flags

    @staticmethod
    def _add_task(printer_name: str = "", file_path: str = "", print_function=None, printer_type=None, **kwargs):
        """
        인쇄작업딕셔너리.

        Args:
            printer_name (str): 프린터이름.
            file_path (str): 파일 경로.
            print_function (callable): 인쇄방법법.
            printer_type (str): 인쇄유형.
            **kwargs: 매개변수.

        Returns:
            dict: 인쇄작업정보.
        """
        task = {
            "printer_name": printer_name,
            "file_path": file_path,
            "print_function": print_function,
            "printer_type": printer_type,
            "attributes": kwargs,
        }
        return task

    @staticmethod
    def _process_file(printer_name: str, file_path: str, printer_type: str, file_type: str, **kwargs):
        """
        근거파일유형분까지의인쇄방법법.

        Args:
            printer_name (str): 프린터이름.
            file_path (str): 파일 경로.
            printer_type (str): 인쇄유형.
            **kwargs: 매개변수.

        Returns:
            dict: 인쇄작업정보.
        """
        lower_suffix = file_path.lower()
        logger.info(f"관리인쇄파일유형: {file_type}, 후이름: {lower_suffix}")
        if lower_suffix.endswith((".doc", ".docx", ".wps")) and file_type == FileType.WORD.value:
            task = PrinterCore._add_task(printer_name, file_path, PrinterCore.print_word, printer_type, **kwargs)
        elif lower_suffix.endswith((".xls", ".xlsx", ".et")) and file_type == FileType.EXCEL.value:
            task = PrinterCore._add_task(printer_name, file_path, PrinterCore.print_excel, printer_type, **kwargs)
        elif lower_suffix.endswith(".pdf") and file_type == FileType.PDF.value:
            task = PrinterCore._add_task(printer_name, file_path, PrinterCore.print_pdf, printer_type, **kwargs)
        elif any(file_path.endswith(img) for img in PrinterCore.IMAGE_TYPES) and file_type == FileType.PICTURE.value:
            task = PrinterCore._add_task(printer_name, file_path, PrinterCore.print_img, printer_type, **kwargs)
        else:
            raise ValueError("지원하지 않음인쇄의파일유형, 확인하세요파일정보")
        return task

    def print_word(self, printer_name: str, file_path: str, printer_type: str, **kwargs):
        """
        인쇄 Word 문서.
        지요소및지정인쇄매개변수.

        Args:
            printer_name (str): 프린터이름.
            file_path (str): Word 파일 경로.
            printer_type (str): 인쇄유형.
            **kwargs: 매개변수.

        Returns:
            bool: 인쇄여부성공.
        """
        defaults = {
            "paper_size": "A4",
            "print_num": 1,
            "scale": None,
            "margin_type": None,
            "margin": [-1, -1, -1, -1],
            "orientation_type": None,
            "page_weight": "",
            "page_height": "",
            "pages": None,
        }

        params = {**defaults, **kwargs}
        printer_app = params.get("printer_app", DocAppType.WORD.value)
        logger.info(
            f"printer_app: {printer_app}, file_path: {file_path}, printer_type: {printer_type}, params: {params}"
        )
        if self.word_obj is None:
            self.init_word_app(printer_app)

        doc_app = self.word_obj
        if doc_app is None:
            raise ValueError("감지하지 못한 선택프로그램의회원가입테이블정보, 확인하세요여부설치!")
        doc_app.Visible = 0
        doc_app.DisplayAlerts = 0
        doc_ = doc_app.Documents.Open(file_path)
        if printer_type == "default":
            try:
                doc_.PrintOut()
                doc_.Close(SaveChanges=0)
                # if hasattr(doc_app, "Quit"):
                #     doc_app.Quit()
            except AttributeError as e:
                raise e

        elif printer_type == "custom":
            try:
                paper_size_params = {
                    "A3": 6,
                    "A4": 7,
                    "소A4": 8,
                    "A5": 9,
                    "B4": 10,
                    "B5": 11,
                    "C_Sheet": 12,
                    "D_Sheet": 13,
                    "지정": 41,
                }

                if params["orientation_type"] == "horizontal":
                    doc_.PageSetup.Orientation = 1
                elif params["orientation_type"] == "vertical":
                    doc_.PageSetup.Orientation = 0

                if params["paper_size"] == "custom":
                    doc_.PageSetup.PageWidth = float(params["page_width"]) * 2.83
                    doc_.PageSetup.PageHeight = float(params["page_height"]) * 2.83
                else:
                    doc_.PageSetup.PaperSize = paper_size_params.get(params["paper_size"], 7)

                if str(params["margin_type"]) == "custom":
                    doc_.PageSetup.LeftMargin = float(params["margin"][0]) * 2.83
                    doc_.PageSetup.RightMargin = float(params["margin"][2]) * 2.83
                    doc_.PageSetup.TopMargin = float(params["margin"][1]) * 2.83
                    doc_.PageSetup.BottomMargin = float(params["margin"][3]) * 2.83

                print_params = {}
                if 10 <= params["scale"] <= 200 and params["scale"] != 100:
                    scale = params["scale"] / 100
                    print_params["PrintZoomPaperWidth"] = round(doc_.PageSetup.PageWidth * scale * 20, 2)
                    print_params["PrintZoomPaperHeight"] = round(doc_.PageSetup.PageHeight * scale * 20, 2)

                if params["pages"]:
                    print_params["Range"] = 4
                    print_params["Pages"] = params["pages"]

                if params["print_num"]:
                    print_params["Copies"] = int(params["print_num"])

                doc_.PrintOut(**print_params)
                doc_.Close(SaveChanges=0)
                doc_app.Quit()

            except AttributeError as e:
                raise e
        return True

    def print_excel(self, printer_name: str, file_path: str, printer_type: str, **kwargs):
        """
        인쇄 Excel 파일.
        지요소및지정인쇄매개변수.

        Args:
            printer_name (str): 프린터이름.
            file_path (str): Excel 파일 경로.
            printer_type (str): 인쇄유형.
            **kwargs: 매개변수.

        Returns:
            bool: 인쇄여부성공.
        """
        defaults = {
            "paper_size": "A4",
            "print_num": 1,
            "scale": None,
            "margin_type": None,
            "margin": [-1, -1, -1, -1],
            "orientation_type": None,
            "page_weight": "",
            "page_height": "",
            "pages": None,
        }

        params = {**defaults, **kwargs}
        printer_app = params.get("printer_app", XlsAppType.EXCEL.value)
        logger.info(
            f"printer_app: {printer_app}, file_path: {file_path}, printer_type: {printer_type}, params: {params}"
        )

        if self.excel_obj is None:
            self.init_excel_app(printer_app)

        try:
            xl_app = self.excel_obj
            if xl_app is None:
                raise ValueError("감지하지 못한 선택프로그램의회원가입테이블정보, 확인하세요여부설치!")
            xl_app.Visible = 0  # 아니오EXEL의, True시로. 아니오에서후실행
            xl_app.DisplayAlerts = 0  # 아니오팝업
            if hasattr(xl_app, "EnableEvents") and xl_app:
                xl_app.EnableEvents = False

            xl_workbook = xl_app.Workbooks.Open(file_path)
            if xl_workbook is None:
                raise ValueError("불가열기해당프로그램")
            if hasattr(xl_workbook, "Checkcompatibility"):
                xl_workbook.Checkcompatibility = False  # 팝업
            if hasattr(xl_workbook, "RunAutoMacros"):
                xl_workbook.RunAutoMacros(2)  # 1:열기 , 2:사용 안 함
            if printer_type == "default":
                sheets = xl_workbook.Sheets.Count
                for sheet in range(sheets):
                    wsheet = xl_workbook.Worksheets(sheet + 1)
                    value = str(wsheet.Cells(4, 6))
                    if value != "None":
                        wsheet.PageSetup.Zoom = 50

                    wsheet.PageSetup.PaperSize = 9  # 크기, A3=8, A4=9(및Word아니오)
                    wsheet.PageSetup.Orientation = 2  # 페이지방법, 세로=1, 가로=2(및Word아니오)
                    wsheet.PrintOut()  # 인쇄# xl_workbook.Close(SaveChanges=0)  # 닫기파일, 아니오저장
                xl_workbook.Close(SaveChanges=0)
                xl_app.Quit()
            elif printer_type == "custom":
                sheets = xl_workbook.Sheets.Count
                sheets_pages = PrinterCore.parse_pages(self, page_string=params["pages"])
                if max(sheets_pages) > sheets:
                    raise ValueError("확인하세요코드!")
                for sheet in sheets_pages:
                    wsheet = xl_workbook.Worksheets(sheet)
                    if 10 <= params["scale"] <= 200 and params["scale"] != 100:
                        wsheet.PageSetup.Zoom = params["scale"]

                    paper_size_parms = {
                        "A3": 8,
                        "A4": 9,
                        "소A4": 10,
                        "A5": 11,
                        "B4": 12,
                        "B5": 13,
                        "C_Sheet": 24,
                        "D_Sheet": 25,
                        "지정": 256,
                    }
                    wsheet.PageSetup.PaperSize = 9  # 크기, A3=8, A4=9(및Word아니오)
                    if params["orientation_type"] == "horizontal":
                        wsheet.PageSetup.Orientation = 2  # 페이지방법, 세로=1, 가로=2(및Word아니오)
                    elif params["orientation_type"] == "vertical":
                        wsheet.PageSetup.Orientation = 1  # 페이지방법, 세로=1, 가로=2(및Word아니오)
                    if params["paper_size"] == "지정":
                        wsheet.PageSetup.PageWidth = float(params["page_width"]) * 2.83
                        wsheet.PageSetup.PageHeight = float(params["page_width"]) * 2.83
                    else:
                        paper_size = paper_size_parms.get(params["paper_size"])
                        wsheet.PageSetup.PaperSize = paper_size

                    # wsheet.PageSetup.AlignMarginsHeaderFooter =True #가장자리거리및
                    if str(params["margin_type"]) == "custom":
                        wsheet.PageSetup.LeftMargin = xl_app.CentimetersToPoints(float(params["margin"][0]) / 10)
                        wsheet.PageSetup.RightMargin = xl_app.CentimetersToPoints(float(params["margin"][1]) / 10)
                        wsheet.PageSetup.TopMargin = xl_app.CentimetersToPoints(float(params["margin"][2]) / 10)
                        wsheet.PageSetup.BottomMargin = xl_app.CentimetersToPoints(float(params["margin"][3]) / 10)

                    params_printer = {}
                    if params["print_num"]:
                        params_printer["Copies"] = int(params["print_num"])
                    if printer_name:
                        params_printer["ActivePrinter"] = printer_name
                    try:
                        wsheet.PrintOut(**params_printer)
                    except AttributeError as e:
                        xl_workbook.Close(SaveChanges=False)
                        xl_app.Quit()
                        raise e
        except Exception as e:
            raise e
        return True

    def print_pdf(self, printer_name: str, file_path: str, printer_type: str, **kwargs):
        """
        인쇄 PDF 파일.
        지요소및지정인쇄매개변수.

        Args:
            printer_name (str): 프린터이름.
            file_path (str): PDF 파일 경로.
            printer_type (str): 인쇄유형.
            **kwargs: 매개변수.

        Returns:
            bool: 인쇄여부성공.
        """
        defaults = {
            "paper_size": "A4",
            "print_num": 1,
            "scale": None,
            "margin_type": None,
            "margin": [-1, -1, -1, -1],
            "orientation_type": None,
            "page_weight": "",
            "page_height": "",
            "pages": None,
        }

        params = {**defaults, **kwargs}

        _A4_width = 2479
        _A4_height = 3508
        paper_size_parms = {
            "A3": [3508, 4961],
            "A4": [2479, 3508],
            "소A4": [2480, 3508],
            "A5": [1748, 2480],
            "B4": [2953, 4170],
            "B5": [2079, 2953],
        }

        if params["paper_size"] is not None:
            size_list = paper_size_parms.get(params["paper_size"])
            if size_list:
                _A4_width = size_list[0]
                _A4_height = size_list[1]
            if params["paper_size"] == "custom":
                _A4_width = float(params["page_width"]) * 300 / 25.4
                _A4_height = float(params["page_height"]) * 300 / 25.4

        if printer_type == "default":
            try:
                if sys.platform == "win32":
                    outputFile = f'-sOutputFile="%printer%{printer_name}"'
                    file_path = file_path.replace("\\", "\\\\")
                    gs_path = PrinterCore._GHOSTSCRIPT_PATH.replace("\\", "\\\\")
                    cmd = [
                        gs_path,
                        "-dPrinted",
                        "-dBATCH",
                        "-dNOPAUSE",
                        "-dNOSAFER",
                        "-sDEVICE=mswinpr2",
                        outputFile,
                        file_path,
                    ]

                    subprocess.call(cmd, shell=True)
            except Exception as e:
                raise e
        elif printer_type == "custom":
            try:
                if sys.platform == "win32":
                    # 생성 Ghostscript 명령매개변수
                    args = "-sDEVICE=mswinpr2 -dBATCH -dNOPAUSE -dFitPage -dQUIET -r300 "
                    # 가로인쇄시너비높이
                    if params["orientation_type"] == "horizontal":
                        _A4_width, _A4_height = _A4_height, _A4_width

                    # 
                    scale = params["scale"]
                    if 10 < scale <= 200 and scale != 100:
                        _A4_width = _A4_width * scale / 100
                        _A4_height = _A4_height * scale / 100

                    orientation = f"-g{int(_A4_width)}x{int(_A4_height)} "
                    output_file = f'-sOutputFile="%printer%{printer_name}" '
                    copies = f"-dNumCopies={int(params['print_num'])} " if params["print_num"] else ""
                    abs_file_path = os.path.join(os.getcwd(), file_path).replace("\\", "\\\\")
                    file_arg = f'-f "{abs_file_path}"'

                    gs_path_replaced = PrinterCore._GHOSTSCRIPT_PATH.replace("\\", "\\\\")
                    ghostscript_cmd = f'"{gs_path_replaced}" {args}{orientation}{output_file}{copies}{file_arg}'
                    subprocess.call(ghostscript_cmd, shell=True)

            except Exception as e:
                raise e
        return True

    def print_img(self, printer_name: str, file_path: str, printer_type: str, **kwargs):
        """
        인쇄이미지파일.
        지요소및지정인쇄매개변수.

        Args:
            printer_name (str): 프린터이름.
            file_path (str): 이미지파일 경로.
            printer_type (str): 인쇄유형.
            **kwargs: 매개변수.

        Returns:
            bool: 인쇄여부성공.
        """
        defaults = {
            "paper_size": "A4",
            "print_num": 1,
            "scale": None,
            "margin_type": None,
            "margin": [-1, -1, -1, -1],
            "orientation_type": None,
            "page_weight": "",
            "page_height": "",
            "pages": None,
        }

        params = {**defaults, **kwargs}
        HORZRES = 8
        VERTRES = 10
        PHYSICALWIDTH = 110
        PHYSICALHEIGHT = 111

        try:
            hDC = win32ui.CreateDC()
            if hDC is None:
                raise ValueError("불가생성인쇄준비위아래문서")
            hDC.CreatePrinterDC(printer_name)
            printable_area = hDC.GetDeviceCaps(HORZRES), hDC.GetDeviceCaps(VERTRES)
            printer_size = hDC.GetDeviceCaps(PHYSICALWIDTH), hDC.GetDeviceCaps(PHYSICALHEIGHT)
            image = Image.open(file_path)

            if printer_type == "default":
                if image.size[0] > image.size[1]:
                    image = image.rotate(90, expand=True)
                ratios = [1.0 * printable_area[0] / image.size[1], 1.0 * printable_area[0] / image.size[0]]
                scale = min(ratios)

                hDC.StartDoc(file_path)
                hDC.StartPage()

                dib = ImageWin.Dib(image)
                scaled_width, scaled_height = [int(scale * i) for i in image.size]
                x1 = int((printer_size[0] - scaled_width) / 2)
                y1 = int((printer_size[1] - scaled_height) / 2)
                x2 = x1 + scaled_width
                y2 = y1 + scaled_height
                dib.draw(hDC.GetHandleOutput(), (x1, y1, x2, y2))

                hDC.EndPage()
                hDC.EndDoc()

            elif printer_type == "custom":
                if params["orientation_type"] == "horizontal":
                    if image.size[1] > image.size[0]:
                        image = image.rotate(90, expand=True)
                elif params["orientation_type"] == "vertical":
                    if image.size[0] > image.size[1]:
                        image = image.rotate(90, expand=True)

                ratios = [1.0 * printable_area[0] / image.size[1], 1.0 * printable_area[0] / image.size[0]]
                printable_scale = min(ratios)

                hDC.StartDoc(file_path)
                hDC.StartPage()
                dib = ImageWin.Dib(image)

                scale = params["scale"]

                if not scale:
                    scale = 100

                scaled_width, scaled_height = [int((printable_scale * i) * (scale / 100)) for i in image.size]
                x1 = int((printer_size[0] * (scale / 100) - scaled_width) / 2)
                y1 = int((printer_size[1] * (scale / 100) - scaled_height) / 2)

                x2 = x1 + scaled_width
                y2 = y1 + scaled_height
                dib.draw(hDC.GetHandleOutput(), (x1, y1, x2, y2))

                hDC.EndPage()
                hDC.EndDoc()

            hDC.DeleteDC()
        except Exception as e:
            raise e
        return True

    @staticmethod
    def view_printer():
        """
        가져오기시스템모든본및네트워크연결의프린터이름목록.

        Returns:
            list: 프린터이름목록.
        """
        printers = win32print.EnumPrinters(win32print.PRINTER_ENUM_LOCAL | win32print.PRINTER_ENUM_CONNECTIONS)
        printer_names = [printers[2] for printers in printers if printers[2]]
        return printer_names

    @staticmethod
    def get_printer_status():
        """
        가져오기 프린터의상태코드.

        Returns:
            int: 프린터상태코드.
        """
        printer_name = win32print.GetDefaultPrinter()
        printer_handle = win32print.OpenPrinter(printer_name)
        status = win32print.GetPrinter(printer_handle, 2)
        print_status = status["Status"]
        return print_status

    @staticmethod
    def jobs_printer():
        """
        조회프린터여부있음인쇄작업.

        Returns:
            bool|None: True-없음작업, False-있음작업, None-예외.
        """
        try:
            # 가져오기 프린터의이름
            printer_name = win32print.GetDefaultPrinter()
            # 열기프린터
            printer_handle = win32print.OpenPrinter(printer_name)
            # 인쇄작업
            jobs = win32print.EnumJobs(printer_handle, 0, -1, 1)
            # 닫기프린터
            win32print.ClosePrinter(printer_handle)
            # 조회여부있음인쇄작업
            if not jobs:
                return True  # 있음작업
            else:
                return False  # 있음작업

        except Exception as e:
            # 관리예외인쇄오류정보
            logger.error("프린터 작업 확인 중 오류: %s", e)
            return None

    @staticmethod
    def generate_printer_task(printer_name="", print_file=None, printer_type="", file_type="", **kwargs):
        """
        완료인쇄작업큐.
        지원단일파일또는다중파일량인쇄.

        Args:
            printer_name (str): 프린터이름.
            print_file (str|list): 파일 경로또는파일목록.
            printer_type (str): 인쇄유형.
            **kwargs: 매개변수.

        Returns:
            queue.Queue: 인쇄작업큐.
        """
        task_queue = queue.Queue()
        if isinstance(print_file, str):
            task = PrinterCore._process_file(printer_name, print_file, printer_type, file_type, **kwargs)
            task_queue.put(task)
        elif isinstance(print_file, list):
            for file in print_file:
                task = PrinterCore._process_file(printer_name, file, printer_type, file_type, **kwargs)
                task_queue.put(task)
        return task_queue

    def parse_pages(self, page_string):
        """
        2, 6-10"테이블인쇄 2 으로 6  10 
        """
        pages = []
        page_ranges = page_string.split(",")  # 분문자열, 까지코드
        for page_range in page_ranges:
            if "-" in page_range:
                start, end = map(int, page_range.split("-"))  # 문자분코드의및결과
                pages.extend(range(start, end + 1))  # 를까지결과의코드추가까지목록중
            else:
                pages.append(int(page_range))  # 결과가있음문자, 이면를단일개코드추가까지목록중
        return pages

    def file_type_batch(self, file_type: str, print_file: list):
        """
        근거파일유형필터링량인쇄의파일목록.

        Args:
            file_type (str): 파일유형.
            print_file (list): 파일 경로목록.
        Returns:
            list: 필터링후의파일 경로목록.
        """
        filtered_files = []
        for file in print_file:
            lower_suffix = file.lower()
            if (
                (file_type == FileType.WORD.value and lower_suffix.endswith((".doc", ".docx", ".wps")))
                or (file_type == FileType.EXCEL.value and lower_suffix.endswith((".xls", ".xlsx", ".et")))
                or (file_type == FileType.PDF.value and lower_suffix.endswith(".pdf"))
                or (
                    file_type == FileType.PICTURE.value
                    and any(lower_suffix.endswith(img) for img in PrinterCore.IMAGE_TYPES)
                )
            ):
                filtered_files.append(file)
        return filtered_files
