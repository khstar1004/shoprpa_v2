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
            raise BaseException(MODULE_IMPORT_ERROR.format(path), f"모듈을 가져올 수 없습니다: {path}: {str(e)}")

        main_func = next((obj for _, obj in inspect.getmembers(process_module, inspect.isfunction)), None)
        if not main_func or not callable(main_func):
            raise BaseException(MODULE_MAIN_FUNCTION_NOT_FOUND.format(path), f"모듈 {path}에서 호출 가능한 main 함수를 찾을 수 없습니다.")

        res = main_func(**kwargs)

        return res, kwargs

    @staticmethod
    def _get_auto_context() -> (dict, str):
        """
        호출 컨텍스트의 변수와 패키지 정보를 가져옵니다.
        """
        try:
            frame = inspect.currentframe()
            if frame is None:
                return {}, ""

            # 호출 체인에서 main 함수의 로컬 변수를 찾는다.
            all_vars = {}
            package = ""

            # 현재 함수 프레임은 건너뛴다.
            frame = frame.f_back
            if frame is None:
                return {}, ""

            # 모든호출, 까지외부로main의
            cframe = None
            while frame is not None:
                if frame.f_code.co_name == "main":
                    cframe = frame
                    break
                else:
                    frame = frame.f_back

            if cframe is not None:
                local_vars = cframe.f_locals
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
        스마트 컴포넌트의 Python 코드를 실행합니다.
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

            # WebPick 값을 WebElement 객체로 변환한다.
            for key, value in code_params.items():
                if isinstance(value, dict) and value.get("elementData"):
                    code_params[key] = web_browser.get_element_by_web_pick(value)

            return Smart.run_core(file_name, **code_params)

    @staticmethod
    def run_core(file_name, **kwargs) -> Any:
        _, package = Smart._get_auto_context()
        res, _ = Smart._smart_call(".{}".format(file_name), package=package, **kwargs)
        return res
