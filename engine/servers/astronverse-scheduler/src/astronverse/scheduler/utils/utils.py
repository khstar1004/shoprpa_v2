import base64
import json
import locale
import os
import socket
import subprocess
import sys
import time
from enum import Enum

import psutil
from astronverse.scheduler.logger import logger

system_encoding = locale.getpreferredencoding()


class EmitType(Enum):
    """
    지정프론트엔드메시지유형
    """

    SYNC = "sync"  # 메시지, 사용컴포넌트업데이트
    SYNC_CANCEL = "sync_cancel"  # 가져오기 , 사용컴포넌트업데이트
    TIP = "tip"  # 후안내정보, 
    ALERT = "alert"  # 후경고정보, 
    LOG_REPORT = "log_report"  # 로그, 
    EDIT_SHOW_HIDE = "edit_show_hide"  # 제목
    EXECUTOR_END = "executor_end"  # 실행기기결과, +실행상태, 
    TERMINAL_STATUS = "terminal_status"  # 단말상태, "
    SUB_WINDOW = "sub_window"  # 창


def string_to_base64(input_string):
    """
    문자열변환base64
    """
    string_bytes = input_string.encode("utf-8")
    encoded_bytes = base64.b64encode(string_bytes)
    encoded_string = encoded_bytes.decode("utf-8")
    return encoded_string


def emit_to_front(emit_type: EmitType, msg=None):
    """
    제어출력정보, 까지 tauri main.rs출력중, 트리거프론트엔드
    사용print시행아니오통신, 원인지원하지 않는
    """
    data = {"type": emit_type.value, "msg": msg}
    logger.info("emit msg to front: {}".format(data))
    data = json.dumps(data)
    if sys.platform == "win32":
        encoded_data = string_to_base64(data)
        # 수정: 사용 print 강함제어 flush
        # 가능으로확인내용 까지출력,  Tauri 가져오기
        print(f"||emit|| {encoded_data}", flush=True)
    else:
        subprocess.run(
            ["echo", "||emit|| {}".format(string_to_base64(data))],
            shell=False,
            check=True,
            encoding="utf-8",
            errors="replace",
        )


def check_port(port, host="127.0.0.1"):
    """
    감지단말여부가능사용
    """
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            # 시간 초과시간
            sock.settimeout(0.2)
            result = sock.connect_ex((host, port))
            if result == 0:
                return False
    except Exception as e:
        pass
    return True


def kill_proc_tree(proc: psutil.Process = None, including_parent: bool = True, exclude_pids: list = None):
    """
    지정PID의모든.
    """
    try:
        children = proc.children(recursive=True)
        for child in children:
            # 호출으로의
            kill_proc_tree(child, including_parent=True)
    except Exception as e:
        pass

    if including_parent:
        try:
            if exclude_pids:
                if proc.pid in exclude_pids:
                    return

            # 시작실행디렉터리아래의
            proc_cwd = proc.exe()
            if "shoprpa" not in proc_cwd:
                return

            # 시도
            proc.kill()
            proc.wait(5)  # 대기결과, 중지
        except psutil.NoSuchProcess:
            pass


def read_last_n_lines(file_path, n):
    """
    반환파일후n행정보 
    """
    if not os.path.exists(file_path):
        return []
    with open(file_path, "rb") as f:
        # 까지파일
        f.seek(0, 2)
        pointer_location = f.tell()
        buffer = bytearray()
        lines_found = 0

        for i in range(pointer_location - 1, -1, -1):
            f.seek(i)
            byte = f.read(1)
            buffer.append(byte[0])

            if byte == b"\n":
                lines_found += 1
                if lines_found == n:
                    break

        # 반대변환해제코드문자배열로문자열
        line_strs = buffer[::-1].decode("utf-8").strip()
        return line_strs.splitlines(True)


def get_settings(file_path=".setting.json", times: int = 5):
    setting = {}
    for i in range(times):
        try:
            if os.path.exists(file_path):
                with open(file_path, encoding="utf-8") as file:
                    setting = json.load(file)
                    break
        except Exception as e:
            time.sleep(0.1)
    return setting