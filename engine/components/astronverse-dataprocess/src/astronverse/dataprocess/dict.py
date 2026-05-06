"""딕셔너리관리닫기유형지정모듈"""

from typing import Any

from astronverse.actionlib import DynamicsItem
from astronverse.actionlib.atomic import atomicMg
from astronverse.dataprocess import NoKeyOptionType


class DictProcess:
    """딕셔너리관리컴포넌트"""

    @staticmethod
    @atomicMg.atomic(
        "DictProcess",
        inputList=[
            atomicMg.param("dict_data", types="Any", required=False),
        ],
        outputList=[atomicMg.param("created_new_dict_data", types="Dict")],
    )
    def create_new_dict(dict_data: dict):
        """
        생성일개새의딕셔너리
        """
        return dict_data

    @staticmethod
    @atomicMg.atomic(
        "DictProcess",
        inputList=[
            atomicMg.param("dict_data", types="Any"),
            atomicMg.param("dict_key", types="Any", required=False),
            atomicMg.param("value", types="Any", required=False),
        ],
        outputList=[atomicMg.param("inserted_dict_data", types="Dict")],
    )
    def set_value_to_dict(dict_data: dict, dict_key: Any, value: Any):
        """
        딕셔너리삽입일
        """
        # dict_key 로None시, 변환비어 있습니다문자열
        if dict_key is None:
            dict_key = ""
        dict_data[dict_key] = value
        return dict_data

    @staticmethod
    @atomicMg.atomic(
        "DictProcess",
        inputList=[
            atomicMg.param("dict_data", types="Any"),
        ],
        outputList=[atomicMg.param("deleted_dict_data", types="Dict")],
    )
    def delete_value_from_dict(dict_data: dict, dict_key: str):
        """
        딕셔너리삭제일
        """
        if dict_key in dict_data:
            del dict_data[dict_key]
        return dict_data

    @staticmethod
    @atomicMg.atomic(
        "DictProcess",
        inputList=[
            atomicMg.param("dict_data", types="Any"),
            atomicMg.param(
                "default_value",
                dynamics=[
                    DynamicsItem(
                        key="$this.default_value.show",
                        expression="return $this.fail_option.value == '{}'".format(
                            NoKeyOptionType.RETURN_DEFAULT.value
                        ),
                    )
                ],
            ),
        ],
        outputList=[atomicMg.param("get_dict_value", types="Any")],
    )
    def get_value_from_dict(
        dict_data: dict,
        dict_key: str,
        fail_option: NoKeyOptionType = NoKeyOptionType.RAISE_ERROR,
        default_value: Any = "",
    ):
        """
        딕셔너리가져오기일
        """
        if dict_data.get(dict_key) is not None:
            return dict_data[dict_key]
        else:
            if fail_option == NoKeyOptionType.RAISE_ERROR:
                raise ValueError("딕셔너리중찾을 수 없습니다해당!")
            elif fail_option == NoKeyOptionType.RETURN_DEFAULT:
                return default_value

    @staticmethod
    @atomicMg.atomic(
        "DictProcess",
        inputList=[
            atomicMg.param("dict_data", types="Any"),
        ],
        outputList=[atomicMg.param("get_dict_keys", types="Dict")],
    )
    def get_keys_from_dict(dict_data: dict):
        """
        딕셔너리가져오기모든
        """
        return list(dict_data.keys())

    @staticmethod
    @atomicMg.atomic(
        "DictProcess",
        inputList=[
            atomicMg.param("dict_data", types="Any"),
        ],
        outputList=[atomicMg.param("get_dict_values", types="Dict")],
    )
    def get_values_from_dict(dict_data: dict):
        """
        딕셔너리가져오기모든값
        """
        return list(dict_data.values())