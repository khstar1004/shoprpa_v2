import ast
import inspect
import os
from enum import Enum
from typing import Optional
import keyring
from astronverse.actionlib.error import *
from astronverse.actionlib.logger import logger


# keyring service names. Keep the legacy spelling readable for existing installs.
SERVICE_NAME = "ShopRPA"
LEGACY_SERVICE_NAMES = ("Shoprpa",)

# 빈비밀번호값, 사용분"빈비밀번호"및"아니요저장에서"
EMPTY_PASSWORD_SENTINEL = "__RPA__Credential__EMPTY__PASSWORD__"


class Credential:
    """인증가져오기서비스"""

    @staticmethod
    def _decode_password(stored: Optional[str]) -> Optional[str]:
        """에서 keyring 가져오기출력후기존비밀번호"""
        if stored is None:
            return None
        if stored == EMPTY_PASSWORD_SENTINEL:
            return ""
        return stored

    @staticmethod
    def get_credential(name: str) -> str | None:
        """
        가져오기 인증비밀번호(내부모듈사용)

        Args:
            name: 인증이름

        Returns:
            인증비밀번호(아니요저장에서반환 None, 저장에서가능반환빈문자열)
        """
        try:
            for service_name in (SERVICE_NAME, *LEGACY_SERVICE_NAMES):
                stored = keyring.get_password(service_name, name)
                decoded = Credential._decode_password(stored)
                if decoded is not None:
                    return decoded
            return None
        except Exception as e:
            logger.exception(f"가져오기 인증실패: {e}")
            return None


class InspectType(Enum):
    EMPTY = "empty"
    PYTHONBASE = "python_base"
    QUOTE = "quote"
    TYPING = "typing"
    ENUM = "enum"
    RPABASE = "rpa_base"
    OTHER = "other"


class FileExistenceType(Enum):
    OVERWRITE = "overwrite"
    RENAME = "rename"
    CANCEL = "cancel"


def gen_type(__annotation__):
    if __annotation__ == inspect.Parameter.empty:
        # 유형빈
        types = "Any"
        kind = InspectType.EMPTY
    elif __annotation__ in [str, list, tuple, int, float, dict, bool]:
        # 변수
        types = (
            __annotation__.__name__.capitalize() if getattr(__annotation__, "__name__", "_empty") != "_empty" else None
        )
        kind = InspectType.PYTHONBASE
    elif isinstance(__annotation__, str):
        # 유형
        types = __annotation__
        kind = InspectType.QUOTE
    elif str(__annotation__).startswith("typing."):
        # typing.Optional|Union|Any
        types = "Any"
        kind = InspectType.TYPING
        logger.warning("type not support: {}".format(__annotation__))
    elif issubclass(__annotation__, Enum):
        # Enum유형
        types = __annotation__.__name__ if getattr(__annotation__, "__name__", "_empty") != "_empty" else None
        kind = InspectType.ENUM
    elif hasattr(__annotation__, "__validate__"):
        # pydantic유형
        types = __annotation__.__name__ if getattr(__annotation__, "__name__", "_empty") != "_empty" else None
        kind = InspectType.RPABASE
    else:
        # 유형
        types = "Any"
        kind = InspectType.OTHER
        logger.warning("type not support: {}".format(__annotation__))
    return types, kind


def handle_existence(file_path, exist_type):
    # 파일저장에서시의관리방식
    if exist_type.value == FileExistenceType.OVERWRITE.value:
        # 덮어쓰기완료저장에서파일, 직선연결반환파일 경로
        return file_path
    elif exist_type.value == FileExistenceType.RENAME.value:
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
    elif exist_type.value == FileExistenceType.CANCEL.value:
        if os.path.exists(file_path):
            return ""
        else:
            return file_path
    logger.info("유형매칭실패, 기존경로반환파일 경로: {}".format(file_path))
    return file_path


class ParamModel:
    def __init__(self, inputList: list, key: str = ""):
        self.inputList = inputList
        self.key = key

    @staticmethod
    def parse_conditional(conditional, kwargs) -> bool:
        """파싱conditional"""
        res = []
        for i in conditional.operands:
            left = None
            if i.left in kwargs:
                left = kwargs[i.left]

            # > < == != >= <= in
            if i.operator == ">":
                res.append(bool(float(left) > float(i.right)))
            elif i.operator == "<":
                res.append(bool(float(left) < float(i.right)))
            elif i.operator == "==":
                res.append(bool(left == i.right))
            elif i.operator == "!=":
                res.append(bool(left != i.right))
            elif i.operator == ">=":
                res.append(bool(float(left) >= float(i.right)))
            elif i.operator == "<=":
                res.append(bool(float(left) <= float(i.right)))
            elif i.operator == "in":
                res.append(bool(left in i.right))
            else:
                raise BaseException(TYPE_KIND_ERROR_FORMAT.format(i.operator), "유형오류{}".format(i.operator))

        # and or
        if conditional.operators == "and":
            return all(res)
        elif conditional.operators == "or":
            return any(res)
        else:
            raise BaseException(
                TYPE_KIND_ERROR_FORMAT.format(conditional.operators), "유형오류{}".format(conditional.operators)
            )

    def __call__(self, **kwargs) -> dict:
        res_list = {}
        for i in self.inputList:
            if i.name in kwargs:
                value = kwargs[i.name]
            else:
                continue

            # 유형관리
            if i.__annotation__ == inspect.Parameter.empty:
                # 
                pass
            elif i.__annotation__ in [str, list, tuple, int, float, dict, bool]:
                try:
                    if i.__annotation__ == bool and isinstance(value, str):
                        if value.lower() in ["false", "none", "undefined", ""]:
                            value = False
                        else:
                            value = i.__annotation__(value)
                    elif i.__annotation__ in [int, float] and isinstance(value, str):
                        if value.lower() == "":
                            value = 0
                        else:
                            value = i.__annotation__(value)
                    elif (
                        i.__annotation__ == list
                        and isinstance(value, str)
                        and value.startswith("[")
                        and value.endswith("]")
                    ) or (
                        i.__annotation__ == dict
                        and isinstance(value, str)
                        and value.startswith("{")
                        and value.endswith("}")
                    ):
                        value = ast.literal_eval(value)
                    else:
                        value = i.__annotation__(value)
                except Exception as e:
                    raise ParamException(
                        PARAM_CONVERT_ERROR_FORMAT.format(i.name, i.types, value),
                        "{}의값변환성공{}실패{}, error:{}".format(i.name, i.types, value, e),
                    ) from e
            elif isinstance(i.__annotation__, str) or str(i.__annotation__).startswith("typing."):
                # 
                pass
            elif issubclass(i.__annotation__, Enum):
                # 변환
                for a in i.__annotation__:
                    if a.value == value:
                        value = a
            elif hasattr(i.__annotation__, "__validate__"):
                # 변환
                try:
                    value = i.__annotation__.__validate__(i.name, value)
                except Exception as e:
                    raise ParamException(
                        PARAM_CONVERT_ERROR_FORMAT.format(i.name, i.types, value),
                        "{}의값설치완료{}실패{}, error:{}".format(i.name, i.types, value, e),
                    ) from e
            else:
                # 
                pass
            res_list[i.name] = value
        return res_list
