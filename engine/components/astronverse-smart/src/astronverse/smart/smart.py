import importlib
import importlib.util
import inspect
from typing import Any
from astronverse.actionlib.atomic import atomicMg
from astronverse.browser.browser import Browser
from astronverse.browser.browser_element import get_browser_instance
from astronverse.smart.browser_ai.web_browser import WebBrowser
from astronverse.smart.error import *


class Smart:
    @staticmethod
    def _smart_call(path: str, package: str, **kwargs):
        try:
            process_module = importlib.import_module(path, package=package)
        except SyntaxError as e:
            raise e
        except Exception as e:
            raise BaseException(MODULE_IMPORT_ERROR.format(path), f"불가가져오기모듈 {path}: {str(e)}")

        main_func = next((obj for _, obj in inspect.getmembers(process_module, inspect.isfunction)), None)
        if not main_func or not callable(main_func):
            raise BaseException(MODULE_MAIN_FUNCTION_NOT_FOUND.format(path), f"모듈 {path} 지정되지 않았습니다가능호출의 main 데이터")

        res = main_func(**kwargs)

        return res, kwargs

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
            package = ""

            # 건너뛰기현재(_get_auto_context 본)
            frame = frame.f_back
            if frame is None:
                return {}, ""

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
            return all_vars, package
        except Exception:
            return {}, ""

    @staticmethod
    @atomicMg.atomic(
        "Smart",
        inputList=[atomicMg.param("smart_component")],
        outputList=[atomicMg.param("smart_result", types="Any")],
    )
    def run_code(smart_component: dict, **code_params) -> Any:
        """
        실행 AI 완료의코드, 지원웹 페이지및데이터 처리유형.
        """
        code_params = {k: v for k, v in code_params.items() if v is not None and not k.startswith("__")}

        file_name = smart_component.get("file_path", "")
        smart_type = smart_component.get("smart_type", "")

        if smart_type != "web_auto":
            return Smart.run_core(file_name, **code_params)
        else:
            web_browser = None
            for key, value in code_params.items():
                if isinstance(value, Browser):
                    web_browser = WebBrowser(value)
                    code_params[key] = web_browser
                    break

            if web_browser is None:
                web_browser = WebBrowser(get_browser_instance())
                code_params["browser"] = web_browser

            # WebPick유형변환로WebElement유형
            for key, value in code_params.items():
                if isinstance(value, dict) and value.get("elementData"):
                    code_params[key] = web_browser.get_element_by_web_pick(value)

            return Smart.run_core(file_name, **code_params)

    @staticmethod
    def run_core(file_name, **kwargs) -> Any:
        _, package = Smart._get_auto_context()
        res, _ = Smart._smart_call(".{}".format(file_name), package=package, **kwargs)
        return res