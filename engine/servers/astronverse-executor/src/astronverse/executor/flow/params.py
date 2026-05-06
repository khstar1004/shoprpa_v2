import ast
import astor
import json
from enum import Enum
from typing import Any

from astronverse.executor.flow.syntax import InputParam, IParam, OutputParam, Token


class ParamType(Enum):
    PYTHON = "python"  # python방식
    VAR = "var"  # 변수
    P_VAR = "p_var"  # 프로세스변수
    G_VAR = "g_var"  # 전역 변수
    STR = "str"  # 예str
    OTHER = "other"  # 대기str, 단일변환[현재버전아니오변환]
    ELEMENT = "element"  # 요소

    @classmethod
    def to_dict(cls):
        return {item.value: item.value for item in cls}


param_type_dict = ParamType.to_dict()


class GlobalVarRewriter(ast.NodeTransformer):
    """
    이름단일의변수 이름전체수정성공 gv["기존이름"]
    """

    def __init__(self, glist):
        self.glist = set(glist)

    def visit_Name(self, node: ast.Name):
        if node.id in self.glist:
            new_node = ast.Subscript(
                value=ast.Name(id="gv", ctx=ast.Load()), slice=ast.Constant(value=node.id), ctx=node.ctx
            )
            return ast.copy_location(new_node, node)
        return node


def refactor_globals(code: str, glist) -> str:
    tree = ast.parse(code)
    tree = GlobalVarRewriter(glist).visit(tree)
    ast.fix_missing_locations(tree)
    return astor.to_source(tree).rstrip("\n")


class Param(IParam):
    def __init__(self, svc):
        self.svc = svc

    @staticmethod
    def pre_param_handler(param_value: Any):
        """
        관리매개변수
        1. 관리data
        2. 경과프론트엔드없음데이터
        3. 결과가배열있음일개type예pythonvalue로""의시, value로None
        """

        ls = []
        # 예아니오예목록, 목록의결과기호합치기필요
        if (
            isinstance(param_value, list)
            and len(param_value) > 0
            and "type" in param_value[0]
            and param_value[0]["type"] in param_type_dict
        ):
            # 관리1: 관리data
            # 관리2: 경과프론트엔드없음데이터
            # 관리3: 결과가배열있음일개type예pythonvalue로""의시, value로None
            for v in param_value:
                if "data" not in v:
                    v["data"] = v.get("value", "")
                del v["value"]
                if v["data"] != "":
                    ls.append(v)
            if len(ls) == 0:
                ls.append(param_value[0])
            if len(ls) == 1 and ls[0].get("type") == ParamType.PYTHON.value and ls[0].get("data") == "":
                ls[0]["data"] = None
        else:
            ls = [{"type": ParamType.OTHER.value, "data": param_value}]
        return ls

    def _param_to_eval(self, ls: list, gv: dict = None) -> (Any, bool):
        """
        를매개변수파싱성공evaL가능실행의상태,
        need_eval=False예로완료추가, 가능직선연결출력아니오경과eval관리, 직선연결출력결과
        """

        need_eval = False
        for v in ls:
            if v.get("type", "str") in [
                ParamType.PYTHON.value,
                ParamType.VAR.value,
                ParamType.G_VAR.value,
                ParamType.P_VAR.value,
            ]:
                need_eval = True
                break

        pieces = []
        for v in ls:
            types = v.get("type", "str")
            data = v.get("data", v.get("value", ""))
            if need_eval:
                if types in [ParamType.STR.value, ParamType.OTHER.value]:
                    pieces.append(f"{data!r}")
                else:
                    if gv:
                        # 내용gv
                        data = refactor_globals(data, gv.keys())
                    pieces.append(f"{data}")
            else:
                pieces.append(f"{data}")

        if len(pieces) == 1:
            return pieces[0], need_eval
        if need_eval:
            return "+".join(f"str({p})" for p in pieces), need_eval
        else:
            return "".join(pieces), need_eval, need_eval

    def parse_param(self, i: dict, token=None, gv: dict = None) -> InputParam:
        name = i.get("name", i.get("key"))
        data = i.get("value")
        parse = i.get("need_parse")
        key = token.value.get("key") if token else ""
        special = ""

        if parse is not None:
            if parse == "json_str":
                if data:
                    data = json.loads(data)
            if data == "":
                data = []
            return InputParam(key=name, value=data, need_eval=True, special="complex_param_parser")
        else:
            if isinstance(data, list) and len(data) == 1 and data[0].get("type", None) == ParamType.ELEMENT.value:
                # 요소
                special = "element"
            elif key == "Script.process" and name == "process" or key == "Script.module" and name == "content":
                # 모듈
                special = "module"
            elif key == "Script.component" and name == "component":
                # 모듈
                special = "component"
            elif key == "Smart.run_code" and name == "smart_component":
                # 컴포넌트의모듈
                special = "smart_component"
            value, need_eval = self._param_to_eval(self.pre_param_handler(data), gv=gv)
            return InputParam(key=name, value=value, need_eval=need_eval, special=special)

    def parse_input(self, token: Token) -> dict[str, InputParam]:
        res = {}
        params_name = {}
        input_list = token.value.get("inputList", [])
        project_id = self.svc.ast_curr_info.get("__project_id__")
        global_var = self.svc.ast_globals_dict[project_id].project_info.global_var
        for i in input_list:
            # : 필터링높이단계선택중의값, 적음매개변수 [가능으로제거코드]
            if (
                i.get("key")
                in [
                    "__delay_before__",
                    "__delay_after__",
                    "__retry_time__",
                    "__retry_interval__",
                ]
                and i.get("value") == [{"type": "other", "value": 0}]
                or i.get("key") == "__res_print__"
                and i.get("value") is False
                or i.get("key") == "__skip_err__"
                and i.get("value") == "exit"
            ):
                continue

            # 1. 닫기시스템
            if not i.get("show", True):
                continue

            if not i.get("key").startswith("__"):
                params_name[i.get("name", i.get("key"))] = i.get("title", "")

            # 2. 파싱
            res[i.get("name", i.get("key"))] = self.parse_param(i, token=token, gv=global_var)

        # 높이단계선택
        info = [
            token.value.get("__line__", 0),
            token.value.get("__process_id__", ""),
        ]
        res["info"] = InputParam(key="__info__", value=info, need_eval=True)
        self.svc.add_atomic_info(project_id, token.value.get("key"), params_name)
        return res

    def parse_output(self, token: Token) -> list[OutputParam]:
        res = []
        output_list = token.value.get("outputList", [])
        if len(output_list) > 0:
            for i in output_list:
                # 0. 닫기시스템
                if not i.get("show", True):
                    continue

                # 1. 관리
                ls = self.pre_param_handler(param_value=i.get("value", []))
                value = ls[0].get("data", "")

                project_id = self.svc.ast_curr_info.get("__project_id__")
                gv = self.svc.ast_globals_dict[project_id].project_info.global_var
                if gv:
                    # 내용gv
                    value = refactor_globals(value, gv.keys())

                # 2. 파싱
                res.append(OutputParam(value=value))
        return res