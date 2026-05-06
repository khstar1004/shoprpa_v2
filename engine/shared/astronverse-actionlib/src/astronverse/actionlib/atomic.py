import inspect
import json
import os
import re
import time
import traceback
from enum import Enum
from functools import wraps
from typing import Any

from astronverse.actionlib import (
    AtomicFormType,
    AtomicFormTypeMeta,
    AtomicMeta,
    AtomicOption,
    AtomicParamMeta,
    ReportCode,
    ReportCodeStatus,
    ReportType,
    utils,
)
from astronverse.actionlib.config import config
from astronverse.actionlib.error import *
from astronverse.actionlib.report import report
from astronverse.actionlib.types import Bool, Date, Pick
from astronverse.actionlib.utils import InspectType


class AtomicManager:
    """기존가능, 실행"""

    _cfg = {"GATEWAY_PORT": "", "WS": None}

    def __init__(self):
        self.atomic_dict = {}
        self.model_cache = {}
        self.model_cache_max_size = 1000

    @staticmethod
    def cfg() -> dict:
        return AtomicManager._cfg

    @staticmethod
    def cfg_from_file(key: str = "", file: str = "") -> dict:
        """
        에서지정매칭파일가져오기매칭정보

        Args:
            file: 매칭파일 경로
            key: 필요가져오기의지정매칭, 비어 있음시반환모든매칭

        Returns:
            dict: 매칭딕셔너리
        """
        if not file:
            # 에서목록의지정매칭파일가져오기경로, 기존가능의key값파일의전체경로
            search_paths = [
                ""  # 예위단계디렉터리 os.path.dirname(os.getcwd())+'.setting.json'
            ]

            for search_path in search_paths:
                if os.path.exists(search_path):
                    file = search_path
                    break

            if not file:
                return {}  # 아니요까지매칭파일

        if not os.path.exists(file):
            return {}

        try:
            with open(file, encoding="utf-8") as f:
                config = json.load(f)

            if not isinstance(config, dict):
                return {}

            # 반환지정또는전체매칭
            if key:
                return {key: config.get(key, "")}

            return {}

        except OSError:
            return {}

    def atomic(self, group_key="", **kwargs):
        def real_atomic(func):
            t = AtomicMeta(**kwargs).init()
            if not t.key:
                t.key = "{}.{}".format(group_key, func.__name__)
            self.atomic_dict[t.key] = t

            @wraps(func)
            def wrapper(*args, **war_kwargs):
                if len(args) > 1:
                    raise BaseException(PARAM_ARGS_NO_SUPPORT_FORMAT.format(args), "매개변수지원하지 않음args")
                return self.atomic_run(func, t.key, *args, **war_kwargs)

            wrapper.__tag_key__ = "atomic"  # 
            return wrapper

        return real_atomic

    @staticmethod
    def param(key: str, **kwargs):
        """
        convert 
        """
        return AtomicParamMeta(**kwargs, key=key)

    def atomic_run(self, func: Any, key: str, *args, **kwargs):
        base_kwargs = {k: v for k, v in kwargs.items() if v is not None and not k.startswith("__")}
        advance_kwargs = {k: v for k, v in kwargs.items() if v is not None and k.startswith("__")}

        info = kwargs.get("__info__", [])
        if not info:
            # 아니요예사용기존가능호출, 예직선연결호출, 아니요관리
            return func(*args, **base_kwargs, **advance_kwargs)

        # 금액외부정보
        line = int(info[0])
        process_id = info[1]

        # 높이단계매개변수
        res_print = kwargs.get("__res_print__", False)
        delay_before = float(kwargs.get("__delay_before__", 0))
        delay_after = float(kwargs.get("__delay_after__", 0))

        report.info(
            ReportCode(
                log_type=ReportType.Code,
                process_id=process_id,
                key=key,
                line=line,
                status=ReportCodeStatus.START,
                msg_str=ReportStartMsgFormat.format("{process}", line, "{atomic}"),
            )
        )

        # Meta데이터: 매개변수검증인증+변환
        if not self.atomic_dict[key].__end__:
            self._update_atomic_param(key, func)
        if key not in self.model_cache:
            if len(self.model_cache) > self.model_cache_max_size:
                # gc돌아가기
                self.model_cache = {}
            model = utils.ParamModel(self.atomic_dict[key].inputList, key)
            self.model_cache[key] = model
        else:
            model = self.model_cache[key]

        # 2. 높이단계매개변수 관리
        has_result = True
        if self.atomic_dict[key].outputList is None or len(self.atomic_dict[key].outputList) == 0:
            has_result = False

        if delay_before > 0:
            time.sleep(delay_before)

        # 검증인증검증인증 __convert__ 로true의매개변수, 아니요사용객체검증인증
        model_res = model(**base_kwargs)
        for name, value in model_res.items():
            base_kwargs[name] = value

        # 있음**kwargs의기존가능연결높이단계매개변수,필터링다중의매개변수,보관인증내용
        if not self.atomic_dict[key].__has_kwargs__:
            advance_kwargs = {}
            sig = inspect.signature(func)
            base_kwargs = {k: v for k, v in base_kwargs.items() if k in set(sig.parameters.keys())}

        res = func(*args, **base_kwargs, **advance_kwargs)
        if res_print and has_result:
            report.info(
                ReportCode(
                    log_type=ReportType.Code,
                    process_id=process_id,
                    line=line,
                    status=ReportCodeStatus.RES,
                    msg_str=str(res),
                )
            )

        if delay_after > 0:
            time.sleep(delay_after)
        return res

    @staticmethod
    def _inspect_param(inspect_item, user_item):
        options = None
        name = inspect_item.name
        default = inspect_item.default if inspect_item.default != inspect.Parameter.empty else None
        formType = AtomicFormTypeMeta(type=AtomicFormType.INPUT_VARIABLE_PYTHON.value)
        __annotation__ = inspect_item.annotation
        noInput = None

        # 반대
        types, kind = utils.gen_type(__annotation__)
        if kind == InspectType.PYTHONBASE:
            if __annotation__ is bool:
                if options is None:
                    options = []
                options.append(AtomicOption("예", True))
                options.append(AtomicOption("아니요", False))
                formType = AtomicFormTypeMeta(type=AtomicFormType.SWITCH.value, params={})
        elif kind == InspectType.ENUM:
            default = default.value
            for e in __annotation__:
                if options is None:
                    options = []
                options.append(AtomicOption(e.value, e.value))
            if len(options) <= 3:
                formType = AtomicFormTypeMeta(type=AtomicFormType.RADIO.value)
            else:
                formType = AtomicFormTypeMeta(type=AtomicFormType.SELECT.value)
        elif kind == InspectType.RPABASE:
            if issubclass(__annotation__, Bool):
                formType = AtomicFormTypeMeta(type=AtomicFormType.SWITCH.value, params={})
            if issubclass(__annotation__, Pick):
                formType = AtomicFormTypeMeta(type=AtomicFormType.PICK.value, params={"use": types})
                noInput = True
            if issubclass(__annotation__, Date):
                formType = AtomicFormTypeMeta(type=AtomicFormType.DEFAULTDATEPICKER.value, params={})

        # 업데이트값
        return {
            "name": name,
            "types": types,
            "__annotation__": __annotation__,
            "formType": formType,
            "default": default,
            "options": options,
            "noInput": noInput,
            "required": True,
        }

    def _update_atomic_param(self, key, func):
        if self.atomic_dict[key].__end__:
            return

        sig = inspect.signature(func)

        # 관리입력
        inputList = []
        __has_kwargs__ = any(p.kind == inspect.Parameter.VAR_KEYWORD for p in sig.parameters.values())
        for k, v in sig.parameters.items():
            if k in ("self", "cls", "args"):
                continue
            if v.kind == inspect.Parameter.VAR_KEYWORD:
                continue
            has = False
            if self.atomic_dict[key].inputList:
                for k1, v1 in enumerate(self.atomic_dict[key].inputList):
                    assert isinstance(v1, AtomicParamMeta)
                    if v1.key == k:
                        v1.update(**self._inspect_param(v, v1))
                        inputList.append(v1)
                        has = True
                        break
            if not has:
                v1 = AtomicParamMeta(key=k)
                v1.update(**self._inspect_param(v, v1))
                inputList.append(v1)
        self.atomic_dict[key].inputList = inputList

        # 관리출력
        outputList = []
        if self.atomic_dict[key].outputList:
            for k2, v2 in enumerate(self.atomic_dict[key].outputList):
                assert isinstance(v2, AtomicParamMeta)
                if not v2.types:
                    raise BaseException(REQUIRED_PARAM_MISSING.format("types"), "적음매개변수, 반환값의types값")
                v2.update(formType=AtomicFormTypeMeta(type=AtomicFormType.RESULT.value), required=None)
                outputList.append(v2)
        self.atomic_dict[key].outputList = outputList

        self.atomic_dict[key].__has_kwargs__ = __has_kwargs__
        self.atomic_dict[key].__end__ = True

    def register(self, cls: Any, group_key: str = "", version: str = "1"):
        # group_key
        if not group_key:
            group_key = cls.__name__

        # src
        src = "{}.{}()".format(cls.__module__, cls.__name__)

        # 반대유형
        for name, member in inspect.getmembers(cls):
            # 필터링기존가능방법법
            if not (inspect.isfunction(member) or inspect.ismethod(member)):
                continue
            try:
                if member.__tag_key__ != "atomic":
                    continue
            except Exception as e:
                continue

            key = "{}.{}".format(group_key, member.__name__)

            # 관리공유
            self.atomic_dict[key].version = version
            self.atomic_dict[key].src = "{}.{}".format(src, member.__name__)

            # 반대업데이트방법법
            self._update_atomic_param(key, member)

    def json(self):
        temp_atomic_dict = {}
        for k, v in self.atomic_dict.items():
            if not v.src:
                continue
            if not v.title:
                v.title = config.get("atomic", v.key, "title")
            if not v.comment:
                v.comment = config.get("atomic", v.key, "comment")
            if not v.icon:
                v.icon = config.get("atomic", v.key, "icon")
            if not v.helpManual:
                v.helpManual = config.get("atomic", v.key, "helpManual")

            # intputList
            inputList = config.get("atomic", v.key, "inputList")
            if not inputList:
                inputList = []
            inputMap = {}
            for i in inputList:
                inputMap[i.get("key", None)] = i

            for a in v.inputList:
                if not a.title and a.key in inputMap:
                    a.title = inputMap[a.key].get("title", None)
                if not a.tip and a.key in inputMap:
                    a.tip = inputMap[a.key].get("tip", None)
                if not a.subTitle and a.key in inputMap:
                    a.subTitle = inputMap[a.key].get("subTitle", None)

                options = config.get("options", a.types)
                if a.options and options:
                    optionsMap = {}
                    for op in options:
                        optionsMap[op.get("value", None)] = op.get("label", None)
                    for p in a.options:
                        if p.value in optionsMap:
                            p.label = optionsMap[p.value]

            # outputList
            outputList = config.get("atomic", v.key, "outputList")
            if not outputList:
                outputList = []
            outputMap = {}
            for i in outputList:
                outputMap[i.get("key", None)] = i

            if v.outputList:
                for o in v.outputList:
                    if not o.title and o.key in outputMap:
                        o.title = outputMap[o.key].get("title", None)
                    if not o.tip and o.key in outputMap:
                        o.tip = outputMap[o.key].get("tip", None)
                    if not o.subTitle and o.key in outputMap:
                        o.subTitle = outputMap[o.key].get("subTitle", None)

            # 6.1 감지comment
            if v.comment:
                matches = re.findall(r"@\{(.*?)\}", v.comment)
                result = []
                if matches:
                    for item in matches:
                        # 분 '||' 의
                        if "||" in item:
                            parts = item.split("||")
                            result.extend(parts)
                        # 분 ':' 의
                        elif ":" in item:
                            parts = item.split(":")
                            result.append(parts[0])
                        # 통신요소
                        else:
                            result.append(item)
                for res in result:
                    if (res in outputMap) or (res in inputMap):
                        continue
                    else:
                        raise BaseException(
                            REQUIRED_PARAM_MISSING.format(res), "comment저장에서미완료지정의데이터:{}".format(res)
                        )

            # 6.2 돌아가기
            temp_atomic_dict[k] = v

        def _json(obj):
            if isinstance(obj, Enum):
                return obj.value
            elif hasattr(obj, "tojson"):
                return obj.tojson(True)
            else:
                return obj.__dict__

        return json.dumps(temp_atomic_dict, ensure_ascii=False, default=_json)

    def meta(self):
        data = self.json()
        file = "meta.json"
        with open(file, "w", encoding="utf-8") as file:
            file.write(data)
        return file


atomicMg = AtomicManager()