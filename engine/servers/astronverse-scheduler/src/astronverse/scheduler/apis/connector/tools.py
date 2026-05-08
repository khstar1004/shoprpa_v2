import ast
import asyncio
import datetime
import json
import mimetypes
import os
import re
import sys
from enum import Enum

from astronverse.scheduler.apis.response import ResCode, res_msg
from astronverse.scheduler.core.schduler.venv import create_project_venv, get_project_venv
from astronverse.scheduler.core.svc import Svc, get_svc
from astronverse.scheduler.logger import logger
from astronverse.scheduler.utils.ai import InputType, get_factors
from astronverse.scheduler.utils.clipboard import Clipboard
from astronverse.scheduler.utils.pip import PipManager
from astronverse.scheduler.utils.subprocess import SubPopen
from astronverse.scheduler.utils.utils import EmitType, emit_to_front
from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, ConfigDict, Field, field_validator

router = APIRouter()


class FilePath(BaseModel):
    path: str


class WriteFile(BaseModel):
    path: str
    mode: str = "w"  # w 덮어쓰기 a 추가 입력
    content: str


class PythonCode(BaseModel):
    code: str


class VideoPaths(BaseModel):
    videoPaths: list


class BrowserPlugin(BaseModel):
    """
    지정설치확장매개변수
    """

    browser: str = "chrome"
    op: str = "install"


class BrowserType(Enum):
    CHROME = "CHROME"


class ContractFactors(BaseModel):
    contract_type: InputType = InputType.TEXT
    contract_path: str = ""
    contract_content: str = ""
    custom_factors: str = ""
    contract_validate: str = ""


class CheckBrowserPlugin(BaseModel):
    """
    지정감지설치확장매개변수
    """

    model_config = ConfigDict(validate_default=True)

    browsers: list[str] = Field(default_factory=list)

    @field_validator("browsers", mode="before")
    @classmethod
    def set_default_browsers(cls, v):
        default_browser_list = [browser.value.lower() for browser in BrowserType]
        if not v:
            return default_browser_list
        assert all(item in default_browser_list for item in v), "Invalid browser type in plugins"
        return v


class PipPackages(BaseModel):
    """
    지정설치python패키지정보
    """

    project_id: str
    package: str = ""
    version: str = ""
    mirror: str = ""


class NotifyText(BaseModel):
    alert_type: str


class ClipboardParams(BaseModel):
    is_html: bool = False


@router.on_event("startup")
async def startup_event():
    async def startup():
        emit_to_front(
            EmitType.SYNC_CANCEL,
            msg={"route_port": get_svc().rpa_route_port, "step": 100},
        )

    task = asyncio.create_task(startup())


@router.post("/file/read")
def read_file(file_path: FilePath):
    """
    프론트엔드통신사용의가져오기파일의방법법, 패키지로그파일
    """
    # 조회파일여부존재함
    if not os.path.isfile(file_path.path):
        raise HTTPException(status_code=404, detail="File not found")

    try:
        mime_type, _ = mimetypes.guess_type(file_path.path)
        if mime_type is None:
            mime_type = "application/octet-stream"  #  MIME 유형

        def file_iterator():
            with open(file_path.path, "rb") as file:
                while True:
                    chunk = file.read(1024)  # 매가져오기 1024 문자
                    if not chunk:
                        break
                    yield chunk

        return StreamingResponse(file_iterator(), media_type=mime_type)
    except Exception as e:
        logger.exception("read_file error: {}".format(e))
        return res_msg(code=ResCode.ERR, msg=str(e), data=None)


@router.post("/file/write")
def read_write(write_file: WriteFile):
    """
    프론트엔드통신사용의덮어쓰기파일의방법법
    """
    if write_file.mode not in {"w", "a"}:
        return res_msg(code=ResCode.ERR, msg="mode must be 'w' or 'a'", data=None)

    # 입력파일
    try:
        dir_name = os.path.dirname(os.path.abspath(write_file.path))
        if dir_name:
            os.makedirs(dir_name, exist_ok=True)
        with open(write_file.path, write_file.mode, encoding="utf-8") as f:
            f.write(write_file.content)
        return res_msg(code=ResCode.SUCCESS, msg="", data=None)
    except Exception as e:
        logger.exception("read_write error: {}".format(e))
        return res_msg(code=ResCode.ERR, msg=str(e), data=None)


@router.post("/video/play")
def video_play(video_paths: VideoPaths):
    try:
        video_paths = video_paths.videoPaths
        if not video_paths:
            return res_msg(code=ResCode.ERR, msg="videoPaths is empty", data={"exist": []})
        existing_files = [path for path in video_paths if os.path.exists(path)]
        return res_msg(code=ResCode.SUCCESS, msg="", data={"exist": existing_files})
    except Exception as e:
        logger.exception("An error occurred while checking video paths.")
        return res_msg(code=ResCode.ERR, msg=str(e), data=None)


@router.post("/window/auto_start/check")
def auto_start_check():
    """
    시작
    """
    if sys.platform != "win32":
        return res_msg(msg="", data={"autostart": False})

    from astronverse.scheduler.utils.window import AutoStart

    return res_msg(msg="", data={"autostart": AutoStart.check()})


@router.post("/window/auto_start/enable")
def auto_start_enable(svc: Svc = Depends(get_svc)):
    """
    열기시작
    """
    if sys.platform != "win32":
        return res_msg(msg="", data={"tips": "예외, linux지원하지 않음시작"})

    from astronverse.scheduler.utils.window import AutoStart

    exe_path = os.path.join(os.path.dirname(os.path.dirname(svc.config.conf_file)), "shoprpa.exe").lower()
    AutoStart.enable(exe_path)
    return res_msg(msg="", data={"tips": "완료"})


@router.post("/window/auto_start/disable")
def auto_start_disable():
    """
    시작닫기
    """
    if sys.platform != "win32":
        return res_msg(msg="", data={"tips": "예외, linux지원하지 않음시작"})

    from astronverse.scheduler.utils.window import AutoStart

    AutoStart.disable()
    return res_msg(msg="", data={"tips": "완료"})


@router.get("/browser/plugins/get_support")
def browser_get_support():
    """
    가져오기확장지원의브라우저목록
    """
    try:
        from astronverse.browser_plugin.browser import ExtensionManager

        browsers = [browser.value.lower() for browser in ExtensionManager.get_support()]
        return res_msg(msg="가져오기성공", data={"browsers": browsers})
    except Exception as e:
        logger.exception(e)
    return res_msg(code=ResCode.ERR, msg="가져오기실패", data=None)


@router.post("/browser/plugins/install")
def browser_install(plugin_op: BrowserPlugin):
    """
    설치확장
    """
    try:
        from astronverse.browser_plugin import BrowserType
        from astronverse.browser_plugin.browser import ExtensionManager

        browser = BrowserType.init(plugin_op.browser)
        ex_manager = ExtensionManager(browser_type=browser)
        ex_manager.install()
        return res_msg(msg="설치성공", data=None)
    except Exception as e:
        logger.exception(e)
    return res_msg(code=ResCode.ERR, msg="설치실패", data=None)


@router.post("/browser/plugins/check_status")
def browser_check(options: CheckBrowserPlugin):
    """
    감지확장상태
    """
    try:
        from astronverse.browser_plugin import BrowserType
        from astronverse.browser_plugin.browser import ExtensionManager

        check_result = dict()
        for browser in options.browsers:
            ex_manager = ExtensionManager(browser_type=BrowserType.init(browser))
            check_result[browser.lower()] = ex_manager.check_status()
        return res_msg(msg="", data=check_result)
    except Exception as e:
        logger.exception(e)
    return res_msg(code=ResCode.ERR, msg="감지실패", data=None)


@router.post("/browser/plugins/check_running")
def browser_check_running(plugin_op: BrowserPlugin):
    """
    감지브라우저여부실행
    """
    try:
        from astronverse.browser_plugin import BrowserType
        from astronverse.browser_plugin.browser import ExtensionManager

        browser = BrowserType.init(plugin_op.browser)
        ex_manager = ExtensionManager(browser_type=browser)
        running = ex_manager.check_browser_running()
        return res_msg(msg="", data={"running": running})
    except Exception as e:
        logger.exception(e)
    return res_msg(code=ResCode.ERR, msg="감지실패", data=None)


@router.post("/browser/plugins/install_all_updates")
def update_installed_plugins():
    """
    업데이트완료설치의브라우저 확장
    """
    try:
        from astronverse.browser_plugin.browser import UpdateManager

        install_results = UpdateManager().update_installed_plugins()
        return res_msg(msg="업데이트완료", data=install_results)
    except Exception as e:
        logger.exception(e)
        return res_msg(code=ResCode.ERR, msg="업데이트실패", data=None)


@router.post("/clipboard/get")
def clipboard_get(params: ClipboardParams):
    """
    가져오기 내용
    """
    if params.is_html:
        content = Clipboard.paste_html_clip()
    else:
        content = Clipboard.paste_str_clip()
    return res_msg(code=ResCode.SUCCESS, msg="", data={"content": content})


@router.post("/pip/install")
def stream_sse(pck: PipPackages, svc: Svc = Depends(get_svc)):
    def sse_async_generator(pck: PipPackages):
        """
        프론트엔드패키지설치
        """
        sub_processes = list()
        try:
            project_id = pck.project_id
            package = pck.package
            version = pck.version
            mirror = pck.mirror
            exec_python = create_project_venv(svc, project_id)
            pck_v = package
            if version:
                pck_v += "=={}".format(version)

            def log(sub_proc):
                while True:
                    output = sub_proc.proc.stdout.readline()
                    if output == "" and not sub_proc.is_alive():
                        break
                    if not output.strip():
                        continue
                    yield "data: {}\n\n".format(json.dumps({"stdout": output}))
                    if "Successfully installed {}".format(package) in output:
                        return
                err_info = sub_proc.proc.stderr.read().strip()
                if err_info:
                    raise Exception(err_info)

            # 다운로드저장
            download_proc = SubPopen(cmd=PipManager.download_pip_cmd(package, version, mirror)).run(log=True)
            sub_processes.append(download_proc)
            for log_data in log(download_proc):
                yield log_data

            # 실행설치
            install_proc = SubPopen(cmd=PipManager.install_pip_cmd(package, version, exec_python=exec_python)).run(
                log=True
            )
            sub_processes.append(install_proc)
            for log_data in log(install_proc):
                yield log_data
        except Exception as e:
            logger.exception(e)
            err = str(e)
            data = json.dumps({"stderr": err})
            yield f"data: {data}\n\n"
        finally:
            for sub_pro in sub_processes:
                sub_pro.kill()
            yield "event: done\ndata: [DONE]\n\n"

    return StreamingResponse(sse_async_generator(pck), media_type="text/event-stream")


@router.post("/package/version")
def package_version(pck: PipPackages, svc: Svc = Depends(get_svc)):
    package = pck.package
    project_id = pck.project_id
    exec_python = get_project_venv(svc, project_id)
    if os.path.exists(exec_python):
        version = PipManager.package_version(package, exec_python=exec_python)
    else:
        version = None
    return res_msg(msg="", data={"package": package, "version": version})


@router.post("/alert/test")
def notify_text(param: NotifyText, svc: Svc = Depends(get_svc)):
    from astronverse.scheduler.utils.notify_utils import NotifyUtils

    notifier = NotifyUtils(svc)
    if param.alert_type == "mail":
        if not notifier.email_setting["receiver"]:
            return res_msg(code=ResCode.ERR, msg="메일", data=None)

        notifier.login_send()
        notifier.send_email("시도메일")
    else:
        if not notifier.text_setting["receiver"]:
            return res_msg(code=ResCode.ERR, msg="휴대폰 번호", data=None)

        notifier.send_text("test", datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
    return res_msg(code=ResCode.SUCCESS, msg="", data=None)


@router.post("/send/tip")
def send_tip(tip_data: dict, svc: Svc = Depends(get_svc)):
    emit_to_front(EmitType.TIP, msg=tip_data)
    return res_msg(code=ResCode.SUCCESS, msg="", data=None)


@router.post("/send/alert")
def send_alert(tip_data: dict, svc: Svc = Depends(get_svc)):
    emit_to_front(EmitType.ALERT, msg=tip_data)
    return res_msg(code=ResCode.SUCCESS, msg="", data=None)


@router.post("/send/sub_window")
def send_sub_window(sub_window_data: dict, svc: Svc = Depends(get_svc)):
    emit_to_front(EmitType.SUB_WINDOW, msg=sub_window_data)
    return res_msg(code=ResCode.SUCCESS, msg="", data=None)


@router.post("/validate/contract")
def validate_contract(params: ContractFactors, svc: Svc = Depends(get_svc)):
    logger.info(f"params: {params}")
    get_factors(
        params.contract_type,
        params.contract_path,
        params.contract_content,
        params.custom_factors,
        params.contract_validate,
        svc.rpa_route_port,
    )
    return res_msg(code=ResCode.SUCCESS, msg="", data=None)


@router.post("/clipboard")
def get_clipboard_html(params: ClipboardParams, svc: Svc = Depends(get_svc)):
    content = ""
    if params.is_html:
        content = Clipboard.paste_html_clip()
    else:
        content = Clipboard.paste_str_clip()
    return res_msg(code=ResCode.SUCCESS, msg="", data={"content": content})


@router.post("/smart/code-to-meta")
def code_to_meta(pycode: PythonCode):
    python_code = pycode.code

    # 0. 가져오기설치 패키지
    def extract_pip_packages(code: str) -> list:
        """
        파싱코드중통신경과비고의pip설치 패키지이름
        :param code: 패키지pip설치비고의코드문자열
        :return: 재후의삼방법패키지이름목록
        """
        # 합치기(재)
        package_set = set()
        # 행분코드
        lines = code.splitlines()

        for line in lines:
            # 제거행빈문자(공백/제어테이블기호/행기호대기)
            stripped_line = line.strip()
            # 선택으로"# pip install "열기 의비고행
            if stripped_line.startswith("# pip install "):
                # 가져오기"# pip install "후의모듈분제거빈
                pkg_str = stripped_line[len("# pip install ") :].strip()
                # 공백분완료패키지이름목록(내용다중패키지일행의)
                pkg_list = pkg_str.split()
                # 추가까지합치기중재
                package_set.update(pkg_list)

        # 합치기변환목록반환(보관인증결과있음순서, Python3.7+합치기있음순서)
        return list(package_set)

    pip_packages = extract_pip_packages(python_code)

    # 1. 가져오기 데이터지정및 docstring
    try:
        func_pattern = re.compile(
            r"def\s+\w+\([\s\S]*?\)(?:\s*->\s*[\s\S]*?)?:\s*\n"  # 매칭def데이터지정행(매개변수)
            r"(?P<indent> {4}|\t)"  # 가져오기 데이터의(4공백/제어테이블기호)
            r'(?P<docstring>["\']{3}[\s\S]*?["\']{3})',  # 매칭삼패키지의docstring
            re.MULTILINE,
        )

        # 매칭일개데이터의docstring
        match = func_pattern.search(python_code)
        docstring = "title: 웹 페이지"
        if match:
            # 2: 가져오기 관리docstring(삼, 다중/빈)
            docstring = match.group("docstring")
            # 의삼
            docstring = docstring.strip('"""').strip("'''")
            # 매행열기 의(보관기존형식의시관리다중공백)
            indent = match.group("indent")
            docstring_lines = [line.lstrip(indent).rstrip() for line in docstring.split("\n")]
            # 연결관리빈행
            docstring = "\n".join([line for line in docstring_lines if line.strip()])
    except Exception as e:
        raise ValueError(f"파싱 Python 코드실패: {e}")

    # 2. 파싱 docstring
    lines = [line.strip() for line in docstring.split("\n") if line.strip()]

    # 가져오기 title
    try:
        title_match = re.match(r"^title[:: ]\s*(.+)$", lines[0])
        title = title_match.group(1).strip()
    except Exception as e:
        raise ValueError("docstring 일행로 'title: ...'")

    # 가져오기 description(가능다중행)
    desc_lines = []
    i = 1
    while i < len(lines) and not lines[i].startswith(("inputs:", "outputs:")):
        desc_lines.append(lines[i])
        i += 1
    description = " ".join(desc_lines).strip()
    if description.startswith("description:"):
        description = description[len("description:") :].strip()

    # 입력출력목록
    input_list = []
    output_list = []

    # 데이터: 파싱매개변수행
    def parse_param_line(line: str):
        pattern = re.compile(
            r"^-\s*(?P<name>\w+)\s*"
            r"\((?P<type>[^)]+)\):\s*"
            r"「(?P<description>[^」]*)」"
            r"(?P<extra>(?:(?!id:|options:|eg:).)*)"
            r"(?:\s*[, ,]?\s*id:\s*(?P<element_id>\d*))?"
            r"(?:[, , ]\s*options:\s*(?P<options>\[[^\]]*\]))?"
            r"(?:[, , ]\s*eg:\s*(?P<example>\S.*?))?"
            r"\s*$"
        )
        match = pattern.match(line.strip())
        if match:
            result = {k: v or "" for k, v in match.groupdict().items()}
            return result
        else:
            raise ValueError(f"불가파싱매개변수행: {line}")

    def safe_str_to_type(value_str: str, target_type: str):
        """
        를문자열 value_str 변환로 target_type 지정의유형.

        지원의 target_type: 'str', 'int', 'float', 'bool', 'list', 'dict'
        """
        type_mapping = {
            "str": lambda x: x,  # 문자열본
            "int": int,
            "float": float,
            "bool": lambda x: x.lower() in ("true", "1", "yes", "on"),
            "list": lambda x: (ast.literal_eval(x) if x.strip().startswith(("[",)) else [x]),
            "dict": ast.literal_eval,
            "tuple": ast.literal_eval,
        }

        if target_type not in type_mapping or target_type == "str":
            return value_str  # 아니오변환
        try:
            if target_type in ("list", "dict", "tuple"):
                # 직선연결시도파싱,  ast.literal_eval 오류(결과가형식아니오)
                result = ast.literal_eval(value_str)
                # 선택 가능: 격식검증결과유형(중지 "(42)" 파싱로 int 아니오예 tuple)
                if target_type == "list" and not isinstance(result, list):
                    raise ValueError("Not a list")
                elif target_type == "dict" and not isinstance(result, dict):
                    raise ValueError("Not a dict")
                elif target_type == "tuple" and not isinstance(result, tuple):
                    raise ValueError("Not a tuple")
                return result
            else:
                return type_mapping[target_type](value_str)
        except Exception as e:
            return value_str

    # 파싱 inputs
    if i < len(lines) and lines[i] == "inputs:":
        i += 1
        while i < len(lines) and not lines[i].startswith("outputs:"):
            if "None" in lines[i]:
                i += 1
                continue
            result = parse_param_line(lines[i])
            name, type_hint, chinese_name, extra, element_id, options, case = (
                result["name"],
                result["type"],
                result["description"],
                result["extra"],
                result["element_id"],
                result["options"],
                result["example"],
            )
            input_list.append(
                {
                    "name": name,
                    "type_hint": type_hint,
                    "title": chinese_name,
                    "extra_info": extra,
                    "element_id": element_id,
                    "options": options,
                    "case": case,
                }
            )
            i += 1

    # 파싱 outputs
    if i < len(lines) and lines[i] == "outputs:":
        i += 1
        while i < len(lines):
            if "None" in lines[i]:
                i += 1
                continue
            result = parse_param_line(lines[i])
            name, type_hint, chinese_name, extra, options, case = (
                result["name"],
                result["type"],
                result["description"],
                result["extra"],
                result["options"],
                result["example"],
            )
            output_list.append({"name": name, "type_hint": type_hint, "title": chinese_name, "case": case})
            i += 1

    # 3. 생성 inputList 및 outputList 의종료결과
    type_map = {
        "bool": "Bool",
        "str": "Str",
        "int": "Int",
        "float": "Float",
        "list": "List",
        "tuple": "Tuple",
        "dict": "Dict",
    }

    def map_type_to_meta_type(type_str: str) -> str:
        base_type = type_str.split("[")[0].split(".")[-1]  # 지원유형예 List[str]
        return type_map.get(base_type.lower(), "Str")

    def build_input_item(param: dict) -> dict:
        name = param["name"]
        type_hint = param["type_hint"]
        title = param["title"]
        element_id = param["element_id"]
        case = param["case"].strip('"')
        options = param["options"]
        item = {"types": "", "key": name, "title": title, "name": name, "required": True}
        # 관리 formType
        if type_hint == "Browser":
            item["types"] = "Browser"
            item["formType"] = {"type": "INPUT_VARIABLE_PYTHON"}
            item["value"] = []
        elif type_hint == "WebPick":
            item["types"] = "WebPick"
            item["formType"] = {"type": "PICK", "params": {"use": "WebPick"}}
            item["noInput"] = True
            item["value"] = []
            if element_id is not None and element_id != "":
                item["default"] = element_id
                value = {"data": element_id, "type": "element", "value": case}
                item["value"].append(value)
        elif type_hint in type_map:
            item["formType"] = {"type": "INPUT_VARIABLE_PYTHON"}
            item["types"] = type_map[type_hint]
            item["value"] = []
            # 값
            if case is not None and case != "" and type_hint in ("list", "tuple", "dict", "int", "bool", "float"):
                item["value"].append({"type": "python", "value": safe_str_to_type(case, type_hint)})
            else:
                item["value"].append({"type": "other", "value": case})
        else:
            try:
                python_types, control_types = type_hint.split("-")
                item["types"] = map_type_to_meta_type(python_types)
            except Exception as e:
                raise ValueError(f"파싱필드유형실패: {e}")
            if control_types == "file":
                item["formType"] = {"type": "INPUT_VARIABLE_PYTHON_FILE", "params": {"file_type": "file"}}
            elif control_types == "folder":
                item["formType"] = {"type": "INPUT_VARIABLE_PYTHON_FOLDER", "params": {"file_type": "folder"}}
            elif control_types == "textbox":
                item["formType"] = {"type": "INPUT_VARIABLE_PYTHON"}
            elif control_types == "checkbox":
                item["formType"] = {"type": "CHECKBOX"}
                item["options"] = [{"label": "예", "value": True}, {"label": "아니오", "value": False}]
            elif control_types == "select" or control_types == "multi_select":
                item["formType"] = {"type": "SELECT"}
                item["options"] = safe_str_to_type(options, "list")

            # 값
            item["value"] = []
            if case is not None and case != "":
                if python_types in ("list", "tuple", "dict", "int", "bool", "float"):
                    item["value"].append({"type": "python", "value": safe_str_to_type(case, type_hint)})
                else:
                    item["value"].append({"type": "other", "value": case})

            # types
            item["types"] = map_type_to_meta_type(python_types)

        return item

    def build_output_item(param: dict) -> dict:
        name = param["name"]
        type_hint = param["type_hint"]
        title = param["title"]
        case = param["case"].strip('"')
        meta_type = type_hint
        if meta_type in type_map:
            meta_type = map_type_to_meta_type(type_hint)
        item = {
            "types": meta_type,
            "formType": {"type": "RESULT"},
            "key": name,
            "title": title,
            "value": [{"type": "var", "value": None}],
        }
        return item

    # 4. 생성종료 meta 결과
    meta = {
        "key": "Smart.run_code",
        "title": title,
        "version": "1.0.1",
        "src": "astronverse.smart.smart.Smart().run_code",
        "comment": description,
        "packages": [],
        "inputList": [],
        "outputList": [],
        "icon": "magic-command",
    }

    # 추가pip packages
    for package in pip_packages:
        meta["packages"].append(package)

    # 추가사용자 지정 입력매개변수
    for param in input_list:
        meta["inputList"].append(build_input_item(param))

    # 추가출력매개변수
    for param in output_list:
        meta["outputList"].append(build_output_item(param))

    return res_msg(code=ResCode.SUCCESS, msg="", data=meta)
