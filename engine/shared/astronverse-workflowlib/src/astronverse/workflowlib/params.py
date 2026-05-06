import functools
import inspect
from enum import Enum
from typing import Any, Optional
import ast
import astor


class ParamType(Enum):
    PYTHON = "python"  # python방식
    VAR = "var"  # 변수
    P_VAR = "p_var"  # 프로세스변수
    G_VAR = "g_var"  # 전역 변수
    STR = "str"  # 예str
    OTHER = "other"  # 대기str, 단일변환[현재버전아니요변환]
    ELEMENT = "element"  # 요소

    @classmethod
    def to_dict(cls):
        return {item.value: item.value for item in cls}


param_type_dict = ParamType.to_dict()


class GlobalVarRewriter(ast.NodeTransformer):
    """
    이름단일의변수이름전체수정성공 gv["기존이름"]
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


class RpaExpression:
    """
    패키지설치후의 code object, 설치전체값연결
    """

    __slots__ = ("code", "expr_str")

    def __init__(self, expr_str: str):
        self.expr_str = expr_str
        self.code = compile(expr_str, "<rpa>", "eval")

    def eval(self, context: dict):
        if self.code:
            return eval(self.code, context)
        else:
            return ""

    def __repr__(self):
        return f"RpaExpression({self.expr_str!r})"


@functools.lru_cache(maxsize=1024)
def _compile_expression(expr_str: str) -> RpaExpression:
    return RpaExpression(expr_str)


class ComplexParamParser:
    """
    복사매개변수파싱기기
    """

    @staticmethod
    def param_to_eval(ls: list, gv: dict = None) -> (Any, bool):
        """
        를매개변수파싱성공evaL가능실행의상태,
        need_eval=False예로완료추가, 가능직선연결출력아니요경과eval관리, 직선연결출력결과
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

    @classmethod
    def _recursive_convert_params(cls, data: Any, gv=None) -> Any:
        """
        변환복사매개변수결과
        """
        if isinstance(data, dict):
            if data.get("rpa") == "special" and "value" in data:
                if isinstance(data["value"], list) and len(data["value"]) > 0:
                    expr_str, need_eval = cls.param_to_eval(data["value"], gv=gv)
                    if need_eval and expr_str:
                        return _compile_expression(expr_str)
                    else:
                        return expr_str
                else:
                    return data["value"]
            return {k: cls._recursive_convert_params(v, gv=gv) for k, v in data.items()}
        if isinstance(data, list):
            return [cls._recursive_convert_params(item, gv=gv) for item in data]
        return data

    @classmethod
    def parse_params(cls, source: Any, context_vars: Optional[dict] = None) -> Any:
        """
        파싱복사매개변수결과
        """
        # context_vars 매개변수보관사용후내용, 에서변환아니요필요사용
        # 정상의변수파싱에서 evaluate_params 행

        auto_ctx = cls._get_auto_context()
        return cls._recursive_convert_params(source, gv=auto_ctx.get("gv"))

    @classmethod
    def evaluate_params(cls, converted: Any, ctx: Optional[dict] = None) -> Any:
        """
        완료파싱의매개변수결과행값
        """
        auto_ctx = cls._get_auto_context()
        if ctx is not None:
            merged_ctx = {**auto_ctx, **ctx}
        else:
            merged_ctx = auto_ctx
        res = cls._evaluate_params_recursive(converted, merged_ctx)
        return res

    @classmethod
    def _evaluate_params_recursive(cls, converted: Any, merged_ctx: dict) -> Any:
        """
        값매개변수결과, 사용가져오기의위아래문서
        """
        if isinstance(converted, RpaExpression):
            res = converted.eval(merged_ctx)
            return res
        if isinstance(converted, dict):
            return {k: cls._evaluate_params_recursive(v, merged_ctx) for k, v in converted.items()}
        if isinstance(converted, list):
            return [cls._evaluate_params_recursive(item, merged_ctx) for item in converted]
        return converted

    @staticmethod
    def _get_auto_context() -> dict:
        """
        가져오기호출의위아래문서변수, 모든호출중의변수
        """
        try:
            frame = inspect.currentframe()
            if frame is None:
                return {}

            # 모든호출중의변수
            all_vars = {}

            # 건너뛰기현재(_get_auto_context 본)
            frame = frame.f_back
            if frame is None:
                return {}

            # 모든호출, 까지외부로main의
            cframe = None
            while frame is not None:
                # 가져오기현재의영역모듈변수
                if frame.f_code.co_name == "main":
                    # 까지 main 데이터, 사용해당
                    cframe = frame
                    break
                else:
                    frame = frame.f_back

            # 가져오기영역모듈변수및전역 변수
            if cframe is not None:
                local_vars = cframe.f_locals.copy()
                global_vars = cframe.f_globals.get("gv").copy()
                # 병합변수, 영역모듈변수(덮어쓰기전역 변수)
                all_vars.update({"gv": global_vars})
                all_vars.update(local_vars)
            return all_vars
        except Exception:
            return {}