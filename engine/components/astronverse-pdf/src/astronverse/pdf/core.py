import os
from abc import ABC, abstractmethod
from functools import wraps

from astronverse.pdf import FileExistenceType, PictureType
from docx import Document


class IPDFCore(ABC):
    @staticmethod
    def validate_path(param_name):
        def decorator(func):
            @wraps(func)
            def wrapper(*args, **kwargs):
                # 통신경과매개변수 이름가져오기매개변수 값
                path = kwargs.get(param_name)

                # 결과가매개변수 값찾을 수 없습니다, 출력예외
                if path is None or not os.path.exists(path):
                    raise ValueError(f"{param_name} 경로찾을 수 없습니다")

                if not path.endswith(".pdf"):
                    raise ValueError(f"{param_name} 경로 예.docx 또는.doc 결과")
                # 결과가검증통신경과, 호출기존데이터
                return func(*args, **kwargs)

            return wrapper

        return decorator

    @staticmethod
    def parse_pages(page_str: str, total_pages: int):
        """
        PDF파일공유방법법, 파싱코드매개변수
        파싱입력매개변수성공목록, 예"1,3,5-8,12" -> [1,3,5,6,7,8,12]
        """
        page_str = page_str.replace(", ", ",")  # 관리중국어
        page_str = page_str.replace(" ", "")  # 제거공백
        pages_range = page_str.split(",")
        page_nums = []
        for p_range in pages_range:
            if "-" in p_range:
                try:
                    start_ind = int(p_range.split("-")[0])
                    end_ind = int(p_range.split("-")[-1])
                except ValueError:
                    raise ValueError(f"코드{p_range}아니오기호합치기지정!")
                if start_ind > end_ind:
                    start_ind, end_ind = end_ind, start_ind
                nums = list(range(start_ind, end_ind + 1))
                page_nums.extend(nums)
            else:
                try:
                    num = int(p_range)
                except ValueError:
                    raise ValueError(f"코드{p_range}아니오기호합치기지정!")
                page_nums.append(num)
        result_page_nums = []
        for page_num in page_nums:
            if page_num > total_pages or page_num < 1:
                raise ValueError(f"코드{page_num}아니오기호합치기지정!")
            result_page_nums.append(page_num - 1)
        return result_page_nums

    @staticmethod
    def write_text_to_word(text, word_path):
        """
        변환text목록로word문서
        """
        doc = Document()
        doc.add_paragraph(text)  # 를가져오기의텍스트추가까지Word문서중
        doc.save(word_path)

    @staticmethod
    @abstractmethod
    def get_pages_num(file_path, pwd) -> int:
        pass

    @staticmethod
    @abstractmethod
    def get_page_text(file_path, pwd, page_range, select_range) -> list:
        pass

    @staticmethod
    @abstractmethod
    def get_images_in_page(file_path, pwd, page_range, save_dir, image_type, prefix, exist_handle_type) -> list:
        pass

    @staticmethod
    @abstractmethod
    def merge_pdf_files(
        merge_type,
        file_folder_path,
        files_path,
        save_dir,
        new_file_name,
        new_pwd,
        exist_handle_type,
    ) -> str:
        pass

    @staticmethod
    @abstractmethod
    def extract_pdf_pages(
        file_path: str,
        pwd: str = "",
        save_dir: str = "",
        page_range: str = "",
        new_file_name: str = "",
        new_pwd: str = "",
        exist_handle_type: FileExistenceType = FileExistenceType.OVERWRITE,
    ) -> str:
        pass

    @staticmethod
    @abstractmethod
    def pdf_to_image(
        file_path: str,
        pwd: str = "",
        save_dir: str = "",
        page_range: str = "",
        image_type: PictureType = PictureType.PNG,
        prefix: str = "",
        exist_handle_type: FileExistenceType = FileExistenceType.OVERWRITE,
    ):
        pass

    @staticmethod
    @abstractmethod
    def extract_forms_from_pdf(
        file_path, pwd, page_range, combine_flag, save_dir, new_file_name, exist_handle_type
    ) -> str:
        pass