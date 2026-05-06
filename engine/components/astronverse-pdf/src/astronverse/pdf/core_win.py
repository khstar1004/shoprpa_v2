import os

import pandas as pd
import pdfplumber
import pypdfium2
from astronverse.actionlib.utils import FileExistenceType, handle_existence
from astronverse.pdf import ImageLayoutType, MergeType, PictureType, SelectRangeType, _handle_files_input
from astronverse.pdf.core import IPDFCore
from astronverse.pdf.error import *
from pdfminer.pdfdocument import PDFPasswordIncorrect
from PIL import Image
from pypdf import PdfReader, PdfWriter


class PDFCore(IPDFCore):
    @staticmethod
    def open_pdf(file_path: str, pwd: str = "") -> PdfReader:
        # 열기PDF파일
        reader = PdfReader(file_path)

        # 조회여부필요해제
        if reader.is_encrypted:
            # 결과가있음비밀번호, 시도해제
            password_type = reader.decrypt(pwd)
            if password_type == 0:
                raise BaseException(PDF_PASSWORD_ERROR_FORMAT.format(pwd), "비밀번호오류")

        return reader

    @staticmethod
    def get_pages_num(file_path: str, pwd: str = "") -> int:
        """
        가져오기PDF데이터, 출력까지숫자유형변수;지원입력PDF문서암호화비밀번호
        """
        reader = PDFCore.open_pdf(file_path, pwd)
        # 가져오기페이지수
        num_pages = len(reader.pages)
        return num_pages

    @staticmethod
    def get_page_text(
        file_path: str,
        pwd: str = "",
        page_range: str = "",
        select_range: SelectRangeType = SelectRangeType.ALL,
    ) -> list:
        """
        가져오기PDF텍스트내용, 출력까지문자열유형변수;지원입력PDF문서암호화비밀번호
        """
        reader = PDFCore.open_pdf(file_path, pwd)
        if page_range and select_range == SelectRangeType.PART:
            page_nums = PDFCore.parse_pages(page_range, len(reader.pages))
        else:
            page_nums = [i for i in range(len(reader.pages))]
        page_text = []
        for page_num in page_nums:
            page = reader.pages[page_num]
            page_text.append(page.extract_text())
        return page_text

    @staticmethod
    def get_images_in_page(
        file_path: str,
        pwd: str = "",
        page_range: str = "",
        save_dir: str = "",
        image_type: PictureType = PictureType.PNG,
        prefix: str = "",
        exist_handle_type: FileExistenceType = FileExistenceType.RENAME,
    ) -> list:
        """
        가져오기PDF페이지이미지, 출력까지이미지파일;지원입력PDF문서암호화비밀번호
        """
        reader = PDFCore.open_pdf(file_path, pwd)
        image_paths = []
        if page_range:
            page_nums = PDFCore.parse_pages(page_range, len(reader.pages))
        else:
            page_nums = [i for i in range(len(reader.pages))]
        if not prefix:
            prefix = os.path.basename(file_path).split(".")[0]
        for page_num in page_nums:
            page = reader.pages[page_num]
            for count, image_file_object in enumerate(page.images):
                image_path = os.path.join(
                    save_dir,
                    f"{prefix}_page{page_num + 1}_{count + 1}.{image_type.value}",
                )
                image_path = handle_existence(image_path, exist_handle_type)
                if image_path:
                    image_paths.append(image_path)
                    with open(image_path, "wb") as fp:
                        fp.write(image_file_object.data)
        return image_paths

    @staticmethod
    def merge_pdf_files(
        merge_type: MergeType = MergeType.FOLDER,
        file_folder_path: str = "",
        files_path: str | list = "",
        save_dir: str = "",
        new_file_name: str = "",
        new_pwd: str = "",
        exist_handle_type: FileExistenceType = FileExistenceType.RENAME,
    ) -> str:
        """
        병합PDF파일, 지원병합폴더중의PDF파일, 또는병합지정PDF파일;지원입력PDF문서암호화비밀번호
        """
        merger = PdfWriter()

        # 관리완료의파일이름
        if not new_file_name:
            new_file_name = "병합파일.pdf"
        elif not new_file_name.endswith(".pdf"):
            new_file_name += ".pdf"
        new_file_path = os.path.join(save_dir, new_file_name)

        if merge_type == MergeType.FOLDER:
            # 병합폴더중의PDF파일
            pdf_files = []
            for file in os.listdir(file_folder_path):  # 아니오폴더, 결과가필요폴더, 이면사용os.walk()
                if file.lower().endswith(".pdf"):
                    pdf_files.append(os.path.join(file_folder_path, file))
                    merger.append(os.path.join(file_folder_path, file))
        else:
            files_path = _handle_files_input(files_path)

            # 병합지정PDF파일
            for file in files_path:
                if not os.path.exists(file):
                    raise BaseException(
                        FILE_PATH_ERROR_FORMAT.format(file),
                        "PDF파일 경로있음오류, 입력하세요정상의경로",
                    )
                merger.append(file)

        if new_pwd:
            merger.encrypt(new_pwd)  # user_password
        new_file_path = handle_existence(new_file_path, exist_handle_type) or ""
        if new_file_path:
            merger.write(new_file_path)
        merger.close()

        return new_file_path

    @staticmethod
    def extract_pdf_pages(
        file_path: str,
        pwd: str = "",
        save_dir: str = "",
        page_range: str = "",
        new_file_name: str = "",
        new_pwd: str = "",
        exist_handle_type: FileExistenceType = FileExistenceType.RENAME,
    ) -> str:
        """
        가져오기PDF페이지, 지원입력PDF문서암호화비밀번호
        :param file_path:
        :param pwd:
        :param save_dir:
        :param page_range:
        :param new_file_name:
        :param new_pwd:
        :param exist_handle_type:
        :return:
        """
        reader = PDFCore.open_pdf(file_path, pwd)
        writer = PdfWriter()

        # 관리완료의파일이름
        if not new_file_name:
            new_file_name = os.path.basename(file_path) + "_가져오기.pdf"
        elif not new_file_name.endswith(".pdf"):
            new_file_name += ".pdf"
        new_file_path = os.path.join(save_dir, new_file_name)

        if page_range:
            page_nums = PDFCore.parse_pages(page_range, len(reader.pages))
        else:
            page_nums = [i for i in range(len(reader.pages))]
        for page_num in page_nums:
            writer.add_page(reader.pages[page_num])
        if new_pwd:
            writer.encrypt(new_pwd)  # user_password
        new_file_path = handle_existence(new_file_path, exist_handle_type) or ""
        if new_file_path:
            writer.write(new_file_path)
        writer.close()
        return new_file_path

    @staticmethod
    def extract_forms_from_pdf(
        file_path: str,
        pwd: str = "",
        page_range: str = "",
        combine_flag: bool = True,
        save_dir: str = "",
        new_file_name: str = "",
        exist_handle_type: FileExistenceType = FileExistenceType.RENAME,
    ) -> str:
        """
        가져오기PDF중의테이블, 지원입력PDF문서암호화비밀번호, 변환성공Excel파일
        :param file_path:
        :param pwd:
        :param page_range:
        :param combine_flag:
        :param save_dir:
        :param new_file_name:
        :param exist_handle_type:
        :return:
        """
        # 사용pdfplumber가져오기PDF파일
        tables = []
        # 관리완료의파일이름
        if not new_file_name:
            new_file_name = os.path.basename(file_path).split(".")[0] + ".xlsx"
        elif not new_file_name.endswith(".xlsx"):
            new_file_name += ".xlsx"
        new_file_path = os.path.join(save_dir, new_file_name)
        try:
            with pdfplumber.open(file_path, password=pwd) as pdf:
                if page_range:
                    page_nums = PDFCore.parse_pages(page_range, len(pdf.pages))
                else:
                    page_nums = [i for i in range(len(pdf.pages))]
                # PDF중의테이블위치일
                for page_num in page_nums:
                    page = pdf.pages[page_num]
                    # 사용.extract_table()까지가져오기테이블데이터
                    raw_tables = page.extract_tables()  # 필요완료extract_tables
                    for table in raw_tables:
                        if table is not None:
                            tables.append(table)
        except PDFPasswordIncorrect:
            raise BaseException(
                PDF_PASSWORD_ERROR_FORMAT.format(pwd),
                "PDF파일 경로있음오류, 입력하세요정상의경로",
            )

        # 를가져오기의테이블데이터변환로pandas DataFrame
        if len(tables) == 0:
            raise Exception("선택페이지있음테이블")
        dfs = []

        for table in tables:
            df = pd.DataFrame(table[1:], columns=table[0])
            dfs.append(df)

        new_file_path = handle_existence(new_file_path, exist_handle_type)
        if not new_file_path:
            return ""

        if combine_flag:
            # 병합모든DataFrame
            try:
                result_df = pd.concat(dfs, ignore_index=True)
                result_df.to_excel(new_file_path, index=False)
            except pd.errors.InvalidIndexError:
                raise ValueError("불가연결다중테이블, 생성를[여부병합]로아니오")
        else:
            # 를dfs중의df출력까지개sheet중
            with pd.ExcelWriter(new_file_path) as writer:
                for index in range(1, len(dfs) + 1):
                    dfs[index - 1].to_excel(writer, index=False, sheet_name="Sheet{}".format(str(index)))

        return new_file_path

    @staticmethod
    def pdf_to_image(
        file_path: str,
        pwd: str = "",
        save_dir: str = "",
        page_range: str = "",
        image_type: PictureType = PictureType.PNG,
        prefix: str = "",
        exist_handle_type: FileExistenceType = FileExistenceType.RENAME,
    ):
        """
        PDF변환로이미지, 지원입력PDF문서암호화비밀번호
        :param file_path:
        :param pwd:
        :param save_dir:
        :param page_range:
        :param image_type:
        :param prefix:
        :param exist_handle_type:
        :return:
        """

        pdf_obj = pypdfium2.PdfDocument(input=file_path, password=pwd)
        if page_range:
            page_nums = PDFCore.parse_pages(page_range, len(pdf_obj))
        else:
            page_nums = [i for i in range(len(pdf_obj))]
        if not prefix:
            prefix = os.path.basename(file_path).split(".")[0]
        for page_num in page_nums:
            page = pdf_obj[page_num]
            bitmap = page.render(
                scale=int(300 / 72),  # set resolution to 300 DPI
                rotation=0,  # no additional rotation
                # ... further rendering options
            )
            pil_image = bitmap.to_pil()
            # 저장로파일
            image_path = os.path.join(save_dir, f"{prefix}_{page_num + 1}.{image_type.value}")
            image_path = handle_existence(image_path, exist_handle_type)
            if image_path:
                pil_image.save(image_path, quality=95)

    @staticmethod
    def images_to_pdf(
        image_files: list,
        save_dir: str = "",
        new_file_name: str = "",
        layout_type: ImageLayoutType = ImageLayoutType.SINGLE_PAGE,
        new_pwd: str = "",
        exist_handle_type: FileExistenceType = FileExistenceType.RENAME,
    ) -> str:
        """
        를이미지파일변환로PDF, 지원다중이미지합치기성공일개PDF또는매이미지일
        :param image_files: 이미지파일 경로목록
        :param save_dir: PDF저장경로
        :param new_file_name: PDF파일이름
        :param layout_type: 영역유형(단일또는다중)
        :param new_pwd: PDF비밀번호
        :param exist_handle_type: 파일저장된 관리유형
        :return: 완료의PDF파일 경로
        """
        if not image_files:
            raise ValueError("이미지파일목록은 비워 둘 수 없습니다")

        # 인증모든이미지파일여부존재함
        valid_image_files = []
        for img_file in image_files:
            if isinstance(img_file, str) and os.path.exists(img_file):
                valid_image_files.append(img_file)
            else:
                raise FileNotFoundError(f"이미지파일찾을 수 없습니다: {img_file}")

        if not valid_image_files:
            raise ValueError("있음까지있음의이미지파일")

        # 관리완료의파일이름
        if not new_file_name:
            new_file_name = "변환파일.pdf"
        elif not new_file_name.endswith(".pdf"):
            new_file_name += ".pdf"
        new_file_path = os.path.join(save_dir, new_file_name)

        try:
            if layout_type == ImageLayoutType.SINGLE_PAGE:
                # 모든이미지병합성공일
                images = []
                total_width = 0
                total_height = 0

                for img_file in valid_image_files:
                    img = Image.open(img_file)
                    # 변환로RGB방식(관리PNG정도대기)
                    if img.mode != "RGB":
                        img = img.convert("RGB")
                    images.append(img)
                    total_width = max(total_width, img.width)
                    total_height += img.height

                # 생성새이미지병합
                merged_image = Image.new("RGB", (total_width, total_height), (255, 255, 255))
                current_height = 0
                for img in images:
                    merged_image.paste(img, (0, current_height))
                    current_height += img.height

                # 저장로PDF
                new_file_path = handle_existence(new_file_path, exist_handle_type)
                if new_file_path:
                    if new_pwd:
                        # 생성비밀번호의PDF
                        merged_image.save(new_file_path, "PDF")
                        # 사용pypdf추가비밀번호
                        reader = PdfReader(new_file_path)
                        writer = PdfWriter()
                        for page in reader.pages:
                            writer.add_page(page)
                        writer.encrypt(new_pwd)
                        with open(new_file_path, "wb") as f:
                            writer.write(f)
                    else:
                        merged_image.save(new_file_path, "PDF")
            else:
                # 매이미지일
                writer = PdfWriter()

                for img_file in valid_image_files:
                    img = Image.open(img_file)
                    # 변환로RGB방식
                    if img.mode != "RGB":
                        img = img.convert("RGB")

                    # 사용PIL직선연결생성PDF
                    from io import BytesIO

                    temp_pdf = BytesIO()
                    img.save(temp_pdf, format="PDF")
                    temp_pdf.seek(0)
                    temp_reader = PdfReader(temp_pdf)
                    for page in temp_reader.pages:
                        writer.add_page(page)

                if new_pwd:
                    writer.encrypt(new_pwd)

                new_file_path = handle_existence(new_file_path, exist_handle_type)
                if new_file_path:
                    with open(new_file_path, "wb") as f:
                        writer.write(f)

        except Exception as e:
            raise Exception(f"이미지변환PDF실패: {str(e)}")

        return new_file_path