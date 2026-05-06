import os
import re
from abc import ABC
from functools import wraps

from astronverse.word import FileExistenceType
from astronverse.word.error import *


class IDocumentCore(ABC):
    @staticmethod
    def validate_path(param_name):
        def decorator(func):
            @wraps(func)
            def wrapper(*args, **kwargs):
                # 통신경과매개변수 이름가져오기매개변수 값
                path = kwargs.get(param_name)

                # 결과가매개변수 값찾을 수 없습니다, 출력예외
                if not os.path.exists(path):
                    raise ValueError(f"{param_name} 경로찾을 수 없습니다")

                if not (path.endswith(".docx") or path.endswith(".doc") or path.endswith(".wps")):
                    raise ValueError(f"{param_name} 경로 예.docx 또는.doc또는.wps 결과")
                # 결과가검증통신경과, 호출기존데이터
                return func(*args, **kwargs)

            return wrapper

        return decorator

    # 여부로정상정수, 를데이터로유형
    @staticmethod
    def are_positive_integers(*values):
        for value in values:
            if isinstance(value, float):
                value = int(value)
            if not isinstance(value, int):
                return False
            if value <= 0:
                return False
        return True

    @staticmethod
    def _extract_table_content(table):
        # 가져오기테이블내용 반환로목록, 시관리할 수 없음문자
        table_content = []
        row_count = 1
        while row_count <= table.Rows.Count:
            row = table.Rows(row_count)
            cell_count = 1
            row_content = []
            while cell_count <= row.Cells.Count:
                cell = row.Cells(cell_count)
                cell_text = re.sub(r"[\x00-\x1F\x7F]", "", cell.Range.Text).strip()
                row_content.append(cell_text)
                cell_count += 1
            table_content.append(row_content)
            row_count += 1
        return table_content

    @staticmethod
    def check_file_in_path(file_path, file_name):
        # 조회파일 경로여부존재함
        if not os.path.exists(file_path):
            raise BaseException(
                DOCUMENT_PATH_ERROR_FORMAT.format(file_path),
                "의경로있음오류, 입력하세요정상의경로!",
            )

        full_file_path = os.path.join(file_path, file_name)
        # 조회지정파일이름여부에서파일 경로중
        if os.path.exists(full_file_path):
            return True

        return False

    @staticmethod
    def handle_existence(file_path, exist_type):
        # 파일존재함시의관리방식
        if exist_type == FileExistenceType.OVERWRITE:
            # 덮어쓰기완료존재함파일, 직선연결반환파일 경로
            # os.remove(file_path)
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