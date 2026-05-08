"""데이터 처리닫기유형지정모듈"""

import ast
import json
from typing import Any

from astronverse.actionlib import DynamicsItem
from astronverse.actionlib.atomic import atomicMg
from astronverse.actionlib.types import Ciphertext
from astronverse.dataprocess import VariableType
from astronverse.dataprocess.error import *


class DataProcess:
    """데이터 처리컴포넌트"""

    @staticmethod
    @atomicMg.atomic(
        "DataProcess",
        inputList=[atomicMg.param("value", types="Any")],
        outputList=[
            atomicMg.param(
                "variable_var",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.variable_var.types",
                        expression="return ['int','str', 'float', 'bool', 'list', 'dict'].includes($this.variable_type.value) ? $this.variable_type.value[0].toUpperCase() + $this.variable_type.value.slice(1) : 'Any'",  # noqa: E501
                    )
                ],
            ),
        ],
    )
    def set_variable_value(value: Any, variable_type: VariableType = VariableType.INT):
        """변수 값"""
        if variable_type != variable_type.OTHER:
            value = str(value)

        try:
            if variable_type == VariableType.INT:
                result = int(float(value))
            elif variable_type == VariableType.FLOAT:
                result = float(value)
            elif variable_type == VariableType.STR:
                result = str(value)
            elif variable_type == VariableType.BOOL:
                if value in ["True", "true", "1", True, 1]:
                    result = True
                elif value in ["False", "false", "0", False, 0]:
                    result = False
                else:
                    result = bool(value)
            elif variable_type in [
                VariableType.LIST,
                VariableType.DICT,
                VariableType.TUPLE,
            ]:
                result = ast.literal_eval(value)
            elif variable_type == VariableType.JSON:
                result = json.loads(value)
            else:
                result = value
        except Exception as e:
            raise BaseException(VALUE_ERROR_FORMAT.format(e), "입력데이터 유형있음오류, 불가로변수")
        return result

    @staticmethod
    def get_shared_variable(shared_variable: dict):
        sub_var_list = shared_variable.get("subVarList", []) if shared_variable else []
        if not sub_var_list:
            return None

        result = {}
        for sub_var in sub_var_list:
            var_name = sub_var.get("varName")
            if not var_name:
                continue
            if sub_var.get("encrypt"):
                cipher = Ciphertext(sub_var.get("varValue"))
                cipher.set_key(sub_var.get("key"))
                result[var_name] = cipher
            else:
                result[var_name] = sub_var.get("varValue")
        return result or None
