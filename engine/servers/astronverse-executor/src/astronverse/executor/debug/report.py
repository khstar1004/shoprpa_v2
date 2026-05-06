import copy
import json
import os
import time
from dataclasses import asdict
from enum import Enum
from queue import Queue

from astronverse.actionlib import (
    ReportCode,
    ReportCodeStatus,
    ReportFlow,
    ReportScript,
    ReportTip,
    ReportType,
    ReportUser,
)
from astronverse.actionlib.report import IReport


def normalize_korean_message(value):
    if not isinstance(value, str):
        return value

    replacements = {
        "열열기기\uD56D\uD56D\uBAA9\uBAA9\uD56D\uD56D\uBAA9\uBAA9...": "워크플로우를 초기화하는 중...",
        "열열기기\uD56D\uBAA9\uD56D\uBAA9...": "워크플로우를 초기화하는 중...",
        "열기\uD56D\uBAA9\uD56D\uBAA9...": "워크플로우를 초기화하는 중...",
        "열기 ...": "워크플로우를 초기화하는 중...",
        "\uD56D\uD56D\uBAA9\uBAA9\uD56D\uD56D\uBAA9\uBAA9성성공공": "워크플로우 초기화 완료",
        "\uD56D\uBAA9\uD56D\uBAA9성성공공": "워크플로우 초기화 완료",
        "\uD56D\uBAA9\uD56D\uBAA9성공": "워크플로우 초기화 완료",
        "열열기기\uD56D\uD56D\uBAA9\uBAA9실실행행": "작업 실행 시작",
        "열열기기\uD56D\uBAA9실실행행": "작업 실행 시작",
        "열기\uD56D\uBAA9실행": "작업 실행 시작",
        "열기 실행": "작업 실행 시작",
        "실실행행결결과과\uD56D\uD56D\uBAA9\uBAA9": "작업 실행 완료",
        "실실행행결결과과\uD56D\uBAA9": "작업 실행 완료",
        "실행결과\uD56D\uBAA9, 사용자\uD56D\uBAA9닫기": "작업 실행이 사용자에 의해 취소되었습니다",
        "실행 결과, 사용자닫기": "작업 실행이 사용자에 의해 취소되었습니다",
        "실행결과\uD56D\uBAA9": "작업 실행 완료",
        "실행 결과": "작업 실행 완료",
        "서버 오류": "서버 오류",
        "내부 오류": "내부 오류",
        "문법 오류": "문법 오류",
        "요청 열기 ": "요청 시작",
        "요청 결과": "요청 결과",
        "경로 로드완료": "경로 로드 완료",
        "서비스완료시작": "서비스 시작 완료",
        "브라우저 확장이 연결되지 않았습니다": "브라우저 확장이 연결되지 않았습니다",
        "실행할 로봇 정보를 찾을 수 없습니다": "실행할 로봇 정보를 찾을 수 없습니다",
        "로봇 마켓 정보가 올바르지 않습니다": "로봇 마켓 정보가 올바르지 않습니다",
        "비워 둘 수 없습니다": "비워 둘 수 없습니다",
    }
    for source, target in replacements.items():
        value = value.replace(source, target)
    return value


class Report(IReport):
    """실행로그관리프로그램"""

    def __init__(self, svc):
        self.svc = svc
        self.queue = Queue(maxsize=1000)
        local_file_path = os.path.join(self.svc.conf.log_path, "report", self.svc.conf.project_id)
        if not os.path.exists(local_file_path):
            os.makedirs(local_file_path)
        self.log_local_file = open(
            os.path.join(str(local_file_path), "{}.txt".format(self.svc.conf.exec_id)), "w", encoding="utf-8"
        )

        self.process = {}
        for i, v in self.svc.ast_globals.process_info.items():
            process_meta = {}
            if v.process_meta:
                for m in v.process_meta:
                    process_meta[m[0]] = m
            new_v = copy.copy(v)
            new_v.process_meta = process_meta
            self.process[v.process_id] = new_v

        self.last_process_id = ""
        self.last_line = 0

    def close(self):
        self.log_local_file.close()

    @staticmethod
    def __json__(obj):
        if isinstance(obj, Enum):
            return obj.value
        else:
            return obj.__dict__

    def __send__(self, filtered_dict):
        if "msg_str" in filtered_dict:
            filtered_dict["msg_str"] = normalize_korean_message(filtered_dict["msg_str"])
        if "msg" in filtered_dict:
            filtered_dict["msg"] = normalize_korean_message(filtered_dict["msg"])

        if self.queue and self.svc.conf.open_log_ws:
            ms = json.dumps(filtered_dict, ensure_ascii=False, default=self.__json__)
            self.queue.put(ms, block=True, timeout=None)

        if (
            self.log_local_file
            and (not self.log_local_file.closed)
            and filtered_dict["log_type"] != ReportType.Tip
            and filtered_dict.get("tag", None) != "tip"
        ):
            # Tip데이터아니오입력까지로그, tag대기Tag아니오입력까지로그
            message = json.dumps(
                {"event_time": int(time.time()), "data": filtered_dict}, ensure_ascii=False, default=self.__json__
            )
            self.log_local_file.write(f"{message}\n")
            self.log_local_file.flush()

    def __pre__(self, message):
        if (
            isinstance(message, ReportFlow)
            or isinstance(message, ReportCode)
            or isinstance(message, ReportUser)
            or isinstance(message, ReportTip)
        ):
            pass
        else:
            process_id, line = self.svc.debug.find_log_position()
            if process_id in self.process:
                message = ReportScript(msg_str=str(message), process_id=process_id, line=line)
            else:
                message = ReportScript(msg_str=str(message))

        if isinstance(message, ReportCode) or isinstance(message, ReportUser) or isinstance(message, ReportScript):
            if message.process_id in self.process:
                # 본
                process_id = message.process_id
                line = message.line
                self.last_line = line
                self.last_process_id = process_id
            else:
                # 본, 컴포넌트
                process_id = self.last_process_id
                line = self.last_line
                message.process_id = self.last_process_id
                message.line = self.last_line

            # 통신데이터데이터
            process = self.process[process_id]
            process_name = process.process_name
            if not message.process:
                message.process = process_name
                message.msg_str = message.msg_str.replace("{process}", process_name)
            if line in process.process_meta:
                meta = process.process_meta[line]
                atomic = meta[2]
                key = meta[3]
                line_id = meta[1]
                if hasattr(message, "atomic") and not message.atomic:
                    message.atomic = atomic
                    message.msg_str = message.msg_str.replace("{atomic}", atomic)
                if hasattr(message, "key") and not message.key:
                    message.key = key
                    message.msg_str = message.msg_str.replace("{key}", key)
                if hasattr(message, "line_id") and not message.line_id:
                    message.line_id = line_id
                    message.msg_str = message.msg_str.replace("{line_id}", line_id)
        return message

    def info(self, message):
        message = self.__pre__(message)

        filtered_dict = {k: v for k, v in asdict(message).items() if v is not None}

        if isinstance(message, ReportCode):
            if message.status == ReportCodeStatus.START:
                filtered_dict["tag"] = "tip"  # 관리, 전송오른쪽아래역할tip
        filtered_dict["log_level"] = "info"
        return self.__send__(filtered_dict)

    def warning(self, message):
        message = self.__pre__(message)

        filtered_dict = {k: v for k, v in asdict(message).items() if v is not None}
        filtered_dict["log_level"] = "warning"
        return self.__send__(filtered_dict)

    def error(self, message):
        message = self.__pre__(message)

        filtered_dict = {k: v for k, v in asdict(message).items() if v is not None}
        filtered_dict["log_level"] = "error"
        return self.__send__(filtered_dict)
