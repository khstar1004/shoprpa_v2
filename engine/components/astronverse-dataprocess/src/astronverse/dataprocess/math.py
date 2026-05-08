"""데이터및데이터값관리닫기공가능."""

import math
import re
from typing import Any

from astronverse.actionlib import DynamicsItem
from astronverse.actionlib.atomic import atomicMg
from astronverse.dataprocess import AddSubType, MathOperatorType, MathRoundType, NumberType
from astronverse.dataprocess.error import *


def random_number(
    start,
    end,
    number_type: NumberType = NumberType.INTEGER,
    size: int = 1,
) -> list:
    """기기데이터완료.

    반환지정및유형의기기데이터목록.
    """
    import numpy  # type: ignore

    if number_type == NumberType.INTEGER:
        return numpy.random.randint(start, end, size).tolist()
    if number_type == NumberType.FLOAT:
        return numpy.random.uniform(start, end, size).tolist()
    raise ValueError("지원하지 않는 number_type입니다")


class MathProcess:
    """데이터관리기존가능합치기."""

    @staticmethod
    @atomicMg.atomic(
        "MathProcess",
        inputList=[atomicMg.param("size", types="Int", required=False)],
        outputList=[atomicMg.param("generated_random_numbers", types="Any")],
    )
    def generate_random_number(
        number_type: NumberType = NumberType.INTEGER,
        size: int = 1,
        start: float = 0,
        end: float = 101,
    ):
        """
        완료기기데이터, 가능으로지정정수, 소데이터
        """
        if start > end:
            raise BaseException(INVALID_NUMBER_RANGE_ERROR_FORMAT, "열기 값소결과값")
        res = random_number(number_type=number_type, start=start, end=end, size=size)
        return res[0] if len(res) == 1 else res

    @staticmethod
    @atomicMg.atomic("MathProcess", outputList=[atomicMg.param("rounding_number", types="Any")])
    def get_rounding_number(number: float, precision: int = 2):
        """
        사오입력
        """
        if precision <= 0:
            return int(round(float(number), int(precision)))
        if float(number).is_integer():
            return int(round(float(number), int(precision)))
        return round(float(number), int(precision))

    @staticmethod
    @atomicMg.atomic(
        "MathProcess",
        outputList=[atomicMg.param("self_calculation_number", types="Int")],
    )
    def self_calculation_number(number: int, add_sub: AddSubType = AddSubType.ADD, add_sub_number: int = 1):
        """
        증가
        """
        if add_sub == AddSubType.ADD:
            return number + add_sub_number
        if add_sub == AddSubType.SUB:
            return number - add_sub_number
        raise ValueError("지원하지 않는 추가 유형입니다")

    @staticmethod
    @atomicMg.atomic(
        "MathProcess",
        inputList=[atomicMg.param("raw_number", types="Any")],
        outputList=[atomicMg.param("absolute_number", types="Any")],
    )
    def get_absolute_number(raw_number: Any):
        """
        가져오기 값
        """
        if isinstance(raw_number, str):
            if re.match(r"^-?\d+$", raw_number):
                raw_number = int(raw_number)
            elif re.match(r"^-?\d+\.\d+$", raw_number):  # 데이터
                raw_number = float(raw_number)
            else:
                raise BaseException(INVALID_NUMBER_FORMAT_ERROR_FORMAT, "입력하세요정수또는데이터")
        return abs(raw_number)

    @staticmethod
    @atomicMg.atomic(
        "MathProcess",
        inputList=[
            atomicMg.param(
                "precision",
                types="Int",
                dynamics=[
                    DynamicsItem(
                        key="$this.precision.show",
                        expression=f"return $this.handle_method.value == '{MathRoundType.ROUND.value}'",
                    )
                ],
            )
        ],
        outputList=[atomicMg.param("calculation_number", types="Any")],
    )
    def calculate_expression(
        left: str = "",
        operator: MathOperatorType = MathOperatorType.ADD,
        right: str = "",
        handle_method: MathRoundType = MathRoundType.NONE,
        precision: int = 0,
    ):
        """
        테이블방식계획
        """
        if not left:
            left = "0"
        if not right:
            right = "0"
        try:
            calc_res = eval(str(left) + operator.value + str(right))
        except Exception as e:
            raise BaseException(
                INVALID_MATH_EXPRESSION_ERROR_FORMAT.format(e),
                str(left) + operator.value + str(right),
            )
        if handle_method == MathRoundType.ROUND:
            if precision <= 0:
                return int(round(float(calc_res), int(precision)))
            else:
                if float(calc_res).is_integer():
                    return int(round(float(calc_res), int(precision)))
                else:
                    return round(float(calc_res), int(precision))
        elif handle_method == MathRoundType.FLOOR:
            calc_res = math.floor(calc_res)
        elif handle_method == MathRoundType.CEIL:
            calc_res = math.ceil(calc_res)
        return calc_res
