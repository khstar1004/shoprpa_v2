import importlib
import importlib.util
import inspect
from typing import Any

from astronverse.actionlib import AtomicFormType, AtomicFormTypeMeta, ReportTip
from astronverse.actionlib.atomic import atomicMg
from astronverse.actionlib.report import report
from astronverse.script.error import *


class Script:
    @staticmethod
    def _call(path: str, package: str, **kwargs):
        try:
            process_module = importlib.import_module(path, package=package)
        except SyntaxError as e:
            raise e
        except Exception as e:
            raise BaseException(MODULE_IMPORT_ERROR.format(path), f"불가가져오기모듈 {path}: {str(e)}")

        main_func = getattr(process_module, "main", None)
        if not main_func or not callable(main_func):
            raise BaseException(MODULE_MAIN_FUNCTION_NOT_FOUND.format(path), f"모듈 {path} 지정되지 않았습니다가능호출의 main 데이터")

        res = main_func(kwargs)

        return res, kwargs

    @staticmethod
    def _module_call(path: str, package: str, out_kwargs, out_param_meta, inn_kwargs):
        try:
            process_module = importlib.import_module(path, package=package)
        except SyntaxError as e:
            raise e
        except Exception as e:
            raise BaseException(MODULE_IMPORT_ERROR.format(path), f"불가가져오기모듈 {path}: {str(e)}")

        def is_v2() -> bool:
            """
            반환 1 테이블 main(*args, **kwargs)
            반환 2 테이블 main(args)
            """
            sig = inspect.signature(process_module.main)

            # 필요출력 VAR_POSITIONAL 또는 VAR_KEYWORD 예일버전
            # main(args) 예이버전
            # main(*args, **kwargs) 예일버전
            params = list(sig.parameters.values())
            return len(params) == 1 and params[0].name == "args"

        if is_v2():
            out_params_res = {}
            for meta in out_param_meta:
                k = meta.get("varName")
                v = meta.get("varValue")
                if v:
                    out_params_res[k] = eval(v, process_module.__dict__)
                else:
                    out_params_res[k] = ""

            out_kwargs = {**out_params_res, **out_kwargs}

            main_func = getattr(process_module, "main", None)
            if not main_func or not callable(main_func):
                raise BaseException(
                    MODULE_MAIN_FUNCTION_NOT_FOUND.format(path), f"모듈 {path} 지정되지 않았습니다가능호출의 main 데이터"
                )

            res = main_func(out_kwargs)
            return out_kwargs
        else:
            report.warning(ReportTip(msg_str=MSG_MODULE_VERSION_WARRING))

            main_func = getattr(process_module, "main", None)
            if not main_func or not callable(main_func):
                raise BaseException(
                    MODULE_MAIN_FUNCTION_NOT_FOUND.format(path), f"모듈 {path} 지정되지 않았습니다가능호출의 main 데이터"
                )

            res = main_func(**inn_kwargs)
            return res

    @staticmethod
    def _get_auto_context() -> (dict, str):
        """
        가져오기호출의위아래문서변수, 모든호출중의변수
        """
        try:
            frame = inspect.currentframe()
            if frame is None:
                return {}, ""

            # 모든호출중의변수
            all_vars = {}
            global_vars = {}
            package = ""

            # 건너뛰기현재(_get_auto_context 본)
            frame = frame.f_back
            if frame is None:
                return {}, "", ""

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
                local_vars = cframe.f_locals
                # 병합변수, 영역모듈변수(덮어쓰기전역 변수)
                all_vars.update(local_vars)
                package = cframe.f_globals.get("__package__")
                global_vars = cframe.f_globals.get("gv")
            return all_vars, package, global_vars
        except Exception:
            return {}, ""

    @staticmethod
    @atomicMg.atomic(
        "Script",
        inputList=[
            atomicMg.param(
                "process",
                types="Any",
                formType=AtomicFormTypeMeta(type=AtomicFormType.SELECT.value, params={"filters": ["Process"]}),
            ),
            atomicMg.param(
                "process_param",
                types="List",
                need_parse="str",
                formType=AtomicFormTypeMeta(type=AtomicFormType.PROCESSPARAM.value, params={"linkage": "process"}),
                required=False,
            ),
        ],
        outputList=[atomicMg.param("process_res", types="Any")],
    )
    def process(process: Any, process_param: list = None):
        """호출프로세스"""
        if isinstance(process, tuple):
            process, param_meta = process
        else:
            process, param_meta = process, None  # 로완료내용, 가능으로삭제
        kwargs = {}
        if process_param:
            for p in process_param:
                kwargs[p.get("varName")] = p.get("varValue")

        _, package, _ = Script._get_auto_context()
        _, kwargs = Script._call(".{}".format(process), package=package, **kwargs)
        return kwargs

    @staticmethod
    @atomicMg.atomic(
        "Script",
        inputList=[
            atomicMg.param(
                "content",
                types="Any",
                formType=AtomicFormTypeMeta(type=AtomicFormType.SELECT.value, params={"filters": "PyModule"}),
            ),
            atomicMg.param(
                "module_param",
                types="List",
                need_parse="str",
                formType=AtomicFormTypeMeta(type=AtomicFormType.PROCESSPARAM.value, params={"linkage": "content"}),
                required=False,
            ),
        ],
        outputList=[atomicMg.param("program_script", types="Any")],
    )
    def module(content: Any, module_param: list = None):
        """호출모듈"""
        if isinstance(content, tuple):
            content, param_meta = content
        else:
            content, param_meta = content, None  # 로완료내용, 가능으로삭제
        out_kwargs = {}
        if module_param:
            for p in module_param:
                out_kwargs[p.get("varName")] = p.get("varValue")

        inn_kwargs, package, global_vars = Script._get_auto_context()
        inn_kwargs = {**global_vars, **inn_kwargs}

        # 로완료내용 버전출력
        res = Script._module_call(
            ".{}".format(content),
            package=package,
            out_kwargs=out_kwargs,
            out_param_meta=param_meta,
            inn_kwargs=inn_kwargs,
        )
        return res

    @staticmethod
    @atomicMg.atomic("Script", inputList=[], outputList=[])
    def component(component: Any, **kwargs):
        # 모든__열기 의kwargs값
        kwargs = {k: v for k, v in kwargs.items() if not k.startswith("__")}

        if isinstance(component, tuple):
            component, param_meta = component
        else:
            component, param_meta = component, None  # 로완료내용, 가능으로삭제

        # 파싱컴포넌트경로: c1990298105483890688.main -> 컴포넌트디렉터리이름및모듈이름
        package = component.split(".")[0] if "." in component else component
        module_name = component.split(".")[-1] if "." in component else component
        _, kwargs = Script._call(".{}".format(module_name), package=package, **kwargs)

        if not param_meta:
            #  로완료내용, 가능으로삭제
            return None

        output_values = [kwargs.get(p["varName"]) for p in param_meta if p.get("varDirection") == 1]
        if len(output_values) == 1:
            return output_values[0]
        elif len(output_values) > 1:
            return tuple(output_values)
        else:
            return None