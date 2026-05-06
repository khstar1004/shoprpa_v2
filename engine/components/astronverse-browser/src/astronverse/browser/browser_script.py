from astronverse.actionlib import AtomicFormType, AtomicFormTypeMeta, DynamicsItem
from astronverse.actionlib.atomic import atomicMg
from astronverse.actionlib.types import PATH, WebPick
from astronverse.browser import InputType
from astronverse.browser.browser import Browser
from astronverse.browser.error import *


def eval_js_code(is_await: bool):
    # 아래개공백보관
    if is_await:
        _js = """
        // 비고보관공백
        return await main()
        """
    else:
        _js = """
       // 비고보관공백
       return main()
       """
    return _js


class BrowserScript:
    """브라우저본실행유형."""

    @staticmethod
    @atomicMg.atomic(
        "BrowserScript",
        inputList=[
            atomicMg.param(
                "content",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_PYTHON_TEXTAREAMODAL_VARIABLE.value,
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.content.show",
                        expression="return $this.input_type.value == '{}'".format(InputType.Content.value),
                    )
                ],
            ),
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"file_type": "file", "filters": [".js"]},
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.file_path.show",
                        expression="return $this.input_type.value == '{}'".format(InputType.File.value),
                    )
                ],
            ),
            atomicMg.param(
                "params",
                formType=AtomicFormTypeMeta(type=AtomicFormType.SCRIPTPARAMS.value),
                need_parse="json_str",
                required=False,
            ),
            atomicMg.param("element_data", required=False),
            atomicMg.param("iframe_url", required=False),
        ],
        outputList=[atomicMg.param("program_script", types="Any")],
    )
    def js_run(
        browser_obj: Browser,
        input_type: InputType = InputType.Content,
        content: str = "",
        file_path: PATH = "",
        params: list = None,
        element_data: WebPick = None,
        iframe_url: str = "",
    ):
        """실행JavaScript본."""
        if input_type == InputType.File:
            with open(file_path, encoding="utf8") as f:
                content = f.read()
        if not content:
            raise BaseException(CODE_EMPTY, f"본데이터비어 있습니다 {input_type} {content} {file_path}")
        if "function main" not in content:
            raise BaseException(CODE_NO_MAIN_FUNC, "코드중패키지main데이터")
        is_await = False
        if "await function main" in content:
            is_await = True
        if params:
            for p in params:
                extend_js_str = "var "
                extend_js_str = extend_js_str + p.get("varName") + "=" + f'"{p.get("varValue")}"' + ";"
                content = extend_js_str + content
        js_op = content + eval_js_code(is_await)
        data_param = {
            "code": js_op,
        }
        if element_data:
            data_param = {
                "code": js_op,
                **element_data["elementData"]["path"],
            }
        data = browser_obj.send_browser_extension(
            browser_type=browser_obj.browser_type.value,
            key="runJS",
            data=data_param,
        )
        return data