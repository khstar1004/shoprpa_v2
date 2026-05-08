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
                path = kwargs.get(param_name)

                if not os.path.exists(path):
                    raise ValueError(f"{param_name} 경로를 찾을 수 없습니다")

                if not (path.endswith(".docx") or path.endswith(".doc") or path.endswith(".wps")):
                    raise ValueError(f"{param_name} 경로는 .docx, .doc 또는 .wps 파일이어야 합니다")
                return func(*args, **kwargs)

            return wrapper

        return decorator

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
        if not os.path.exists(file_path):
            raise BaseException(
                DOCUMENT_PATH_ERROR_FORMAT.format(file_path),
                "파일 경로가 올바르지 않습니다",
            )

        full_file_path = os.path.join(file_path, file_name)
        if os.path.exists(full_file_path):
            return True

        return False

    @staticmethod
    def handle_existence(file_path, exist_type):
        if exist_type == FileExistenceType.OVERWRITE:
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
