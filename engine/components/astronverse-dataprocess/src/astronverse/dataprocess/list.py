"""
목록 관리닫기방법법.
"""

import ast
import random
from typing import Any

from astronverse.actionlib import DynamicsItem
from astronverse.actionlib.atomic import atomicMg
from astronverse.dataprocess import DeleteMethodType, InsertMethodType, ListType, SortMethodType
from astronverse.dataprocess.error import *


def list_legal_check(list_data: list, index: str = "", allow_empty: bool = True):
    """
    사용내부모듈조회목록여부합치기법
    """
    if not allow_empty and len(list_data) == 0:
        raise ValueError("목록은 비워 둘 수 없습니다!")
    index_int = 0
    if index:
        try:
            if isinstance(index, str):
                # 를문자열분변환로정수목록
                index_list = [int(idx.strip()) for idx in index.split(",")]
                # 조회매개검색여부에서있음내부
                for idx in index_list:
                    if idx < -len(list_data) or idx >= len(list_data):
                        raise ValueError("배열검색값초과출력!")
                # 결과가있음일개검색, 반환일개값
                if len(index_list) == 1:
                    index_int = index_list[0]
                else:
                    index_int = index_list
            else:
                # 결과가아니오예문자열, 직선연결변환로정수
                index_int = int(index)
                if index < -len(list_data) or index >= len(list_data):
                    raise ValueError("배열검색값초과출력!")
        except ValueError as e:
            raise ValueError("요청 있음의정수유형검색!")
        except Exception:
            raise ValueError("요청 정수유형의검색!")
    return list_data, index_int


class ListProcess:
    """목록 관리프로세스유형."""

    @staticmethod
    @atomicMg.atomic(
        "ListProcess",
        inputList=[
            atomicMg.param(
                "size",
                dynamics=[
                    DynamicsItem(
                        key="$this.size.show",
                        expression="return $this.list_type.value == '{}'".format(ListType.SAME_DATA.value),
                    )
                ],
            ),
            atomicMg.param(
                "value",
                types="Any",
                dynamics=[
                    DynamicsItem(
                        key="$this.value.show",
                        expression="return ['{}', '{}'].includes($this.list_type.value)".format(
                            ListType.SAME_DATA.value, ListType.USER_DEFINED.value
                        ),
                    )
                ],
            ),
            atomicMg.param(
                "custom_list",
                types="Any",
                dynamics=[
                    DynamicsItem(
                        key="$this.custom_list.show",
                        expression="return $this.list_type.value == '{}'".format(ListType.USER_DEFINED.value),
                    )
                ],
            ),
        ],
        outputList=[atomicMg.param("created_list_data", types="List")],
    )
    def create_new_list(list_type: ListType = ListType.EMPTY, size: int = 0, value: Any = "", custom_list: Any = ""):
        """
        생성새목록
        """
        new_array = []
        if list_type == ListType.EMPTY:
            pass
        elif list_type == ListType.SAME_DATA:
            if isinstance(value, str) and value.startswith("[") and value.endswith("]"):
                try:
                    value = ast.literal_eval(value)
                except Exception as e:
                    raise BaseException(INVALID_LIST_FORMAT_ERROR_FORMAT.format(e), "올바른 목록 형식을 입력하세요")
            new_array = [value] * size
        elif list_type == ListType.USER_DEFINED:
            source_list = custom_list if custom_list not in ("", None) else value
            if isinstance(source_list, str):
                if source_list.startswith("[") and source_list.endswith("]"):
                    try:
                        new_array = ast.literal_eval(source_list)
                    except Exception as e:
                        raise BaseException(INVALID_LIST_FORMAT_ERROR_FORMAT.format(e), "올바른 목록 형식을 입력하세요")
                else:
                    new_array = [source_list]
            elif isinstance(source_list, list):
                new_array = source_list
            else:
                raise ValueError("사용자지정목록유형오류!")
        return new_array

    @staticmethod
    @atomicMg.atomic(
        "ListProcess",
        inputList=[atomicMg.param("list_data", types="List")],
        outputList=[atomicMg.param("cleared_list_data", types="List")],
    )
    def clear_list(list_data: list):
        """
        빈목록
        """
        list_data.clear()
        return list_data

    @staticmethod
    @atomicMg.atomic(
        "ListProcess",
        inputList=[
            atomicMg.param("list_data", types="Any"),
            atomicMg.param(
                "index",
                dynamics=[
                    DynamicsItem(
                        key="$this.index.show",
                        expression="return $this.insert_method.value == '{}'".format(InsertMethodType.INDEX.value),
                    )
                ],
            ),
        ],
        outputList=[atomicMg.param("inserted_list_data", types="List")],
    )
    def insert_value_to_list(
        list_data: list,
        value: Any,
        insert_method: InsertMethodType = InsertMethodType.APPEND,
        index: str = "",
    ):
        """
        목록삽입일
        """
        index_int = 0
        if insert_method == InsertMethodType.APPEND:
            index = ""
        list_data, _ = list_legal_check(list_data, "", True)
        if insert_method == InsertMethodType.APPEND:  # 삽입방식: 추가 입력
            list_data.append(value)
        elif insert_method == InsertMethodType.INDEX:  # 삽입방식: 위치 지정
            try:
                index_int = int(index)
            except:
                raise BaseException(INVALID_INDEX_ERROR_FORMAT.format(index), "필요정수유형의검색!")
            list_data.insert(index_int, value)
        return list_data

    @staticmethod
    @atomicMg.atomic(
        "ListProcess",
        inputList=[
            atomicMg.param("list_data", types="Any"),
            atomicMg.param("index", types="Any"),
        ],
        outputList=[atomicMg.param("changed_list_data", types="List")],
    )
    def change_value_in_list(list_data: list, index: str = "", new_value: Any = ""):
        """
        목록수정일
        """
        index_int = 0
        list_data, index_int = list_legal_check(list_data, index, False)

        if isinstance(index_int, list):
            raise ValueError("요청 단일개정수유형의검색!")
        else:
            list_data[index_int] = new_value
        return list_data

    @staticmethod
    @atomicMg.atomic(
        "ListProcess",
        inputList=[
            atomicMg.param("list_data", types="Any"),
            atomicMg.param("value", types="Any"),
        ],
        outputList=[atomicMg.param("get_list_position", types="Int")],
    )
    def get_list_position(list_data: list, value: Any):
        """
        목록가져오기일의위치
        """
        try:
            list_pos = list_data.index(value)
            return list_pos
        except ValueError:
            raise ValueError("목록중찾을 수 없습니다해당객체!")

    @staticmethod
    @atomicMg.atomic(
        "ListProcess",
        inputList=[
            atomicMg.param("list_data", types="Any"),
            atomicMg.param(
                "del_value",
                types="Any",
                dynamics=[
                    DynamicsItem(
                        key="$this.del_value.show",
                        expression="return $this.del_mode.value == '{}'".format(DeleteMethodType.VALUE.value),
                    )
                ],
            ),
            atomicMg.param(
                "del_pos",
                types="Any",
                dynamics=[
                    DynamicsItem(
                        key="$this.del_pos.show",
                        expression="return $this.del_mode.value == '{}'".format(DeleteMethodType.INDEX.value),
                    )
                ],
            ),
        ],
        outputList=[atomicMg.param("removed_list_data", types="List")],
    )
    def remove_value_from_list(
        list_data: list,
        del_mode: DeleteMethodType = DeleteMethodType.INDEX,
        del_value: Any = "",
        del_pos: str = "",
    ):
        """
        목록삭제일
        """
        del_pos_int = 0
        list_data, del_pos_int = list_legal_check(list_data, del_pos, False)
        if del_mode == DeleteMethodType.INDEX:
            if isinstance(del_pos_int, list):
                # 에서대까지소정렬검색, 삭제시검색변수
                sorted_indices = sorted(del_pos_int, reverse=True)
                for index in sorted_indices:
                    del list_data[index]
            else:
                del list_data[del_pos_int]
            return list_data
        elif del_mode == DeleteMethodType.VALUE:
            try:
                index = list_data.index(del_value)
            except ValueError:
                raise ValueError("목록중찾을 수 없는 해당요소!")
            del list_data[index]
            return list_data

    @staticmethod
    @atomicMg.atomic(
        "ListProcess",
        inputList=[
            atomicMg.param("list_data", types="Any"),
        ],
        outputList=[atomicMg.param("sorted_list_data", types="List")],
    )
    def sort_list(list_data: list, sort_method: SortMethodType = SortMethodType.DESC):
        """
        목록정렬
        """
        list_instance = []
        if sort_method == SortMethodType.ASC:
            try:
                list_instance = sorted(list_data)  # 상승순서
            except:
                raise ValueError("요청 원데이터 유형일의목록 행정렬!")
        elif sort_method == SortMethodType.DESC:
            try:
                list_instance = sorted(list_data, reverse=True)  # 순서
            except:
                raise ValueError("요청 원데이터 유형일의목록 행정렬!")
        return list_instance

    @staticmethod
    @atomicMg.atomic(
        "ListProcess",
        inputList=[
            atomicMg.param("list_data", types="Any"),
        ],
        outputList=[atomicMg.param("shuffled_list_data", types="List")],
    )
    def random_shuffle_list(list_data: list):
        """
        목록 기기정렬
        :param list_data:
        :return:
        """
        random.shuffle(list_data)
        return list_data

    @staticmethod
    @atomicMg.atomic(
        "ListProcess",
        inputList=[
            atomicMg.param("list_data_1", types="Any"),
            atomicMg.param("list_data_2", types="Any"),
        ],
        outputList=[atomicMg.param("filter_list_data", types="List")],
    )
    def filter_elements_from_list(list_data_1: list, list_data_2: list):
        """
        목록필터링
        """
        return [i for i in list_data_1 if i not in list_data_2]

    @staticmethod
    @atomicMg.atomic(
        "ListProcess",
        inputList=[
            atomicMg.param("list_data", types="Any"),
        ],
        outputList=[atomicMg.param("reversed_list_data", types="List")],
    )
    def reverse_list(list_data: list):
        """
        목록반대변환
        """
        list_data.reverse()
        return list_data

    @staticmethod
    @atomicMg.atomic(
        "ListProcess",
        inputList=[
            atomicMg.param("list_data_1", types="Any"),
            atomicMg.param("list_data_2", types="Any"),
        ],
        outputList=[atomicMg.param("merged_list_data", types="List")],
    )
    def merge_list(list_data_1: list, list_data_2: list):
        """
        목록병합
        """
        result_list = list_data_1.copy()
        result_list.extend(list_data_2)
        return result_list

    @staticmethod
    @atomicMg.atomic(
        "ListProcess",
        inputList=[
            atomicMg.param("list_data", types="Any"),
        ],
        outputList=[atomicMg.param("unique_list_data", types="List")],
    )
    def get_unique_list(list_data: list):
        """
        목록 재
        """
        list_data = list(set(list_data))
        return list_data

    @staticmethod
    @atomicMg.atomic(
        "ListProcess",
        inputList=[
            atomicMg.param("list_data_1", types="Any"),
            atomicMg.param("list_data_2", types="Any"),
        ],
        outputList=[atomicMg.param("common_list_data", types="List")],
    )
    def get_common_elements_from_list(list_data_1: list, list_data_2: list):
        """
        목록가져오기공유요소
        """
        list_result = list(set(list_data_1) & set(list_data_2))
        return list_result

    @staticmethod
    @atomicMg.atomic(
        "ListProcess",
        inputList=[
            atomicMg.param("list_data", types="Any"),
            atomicMg.param("index", types="Any"),
        ],
        outputList=[atomicMg.param("get_list_value", types="Any")],
    )
    def get_value_from_list(list_data: list, index: str = ""):
        """
        목록가져오기일
        """
        index_int = 0
        list_data, index_int = list_legal_check(list_data, index, False)
        if isinstance(index_int, list):
            raise ValueError("요청 단일개정수유형의검색!")
        return list_data[index_int]

    @staticmethod
    @atomicMg.atomic(
        "ListProcess",
        inputList=[
            atomicMg.param("list_data", types="Any"),
        ],
        outputList=[atomicMg.param("get_list_length", types="Int")],
    )
    def get_length_of_list(list_data: list):
        """
        목록가져오기길이정도
        """
        return len(list_data)
