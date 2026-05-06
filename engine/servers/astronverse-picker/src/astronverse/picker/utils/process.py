import os
from typing import Optional

import psutil
from astronverse.picker.logger import logger


def get_process_name(pid: int):
    p = psutil.Process(pid)
    return p.name().split(".exe")[0] if p.name().endswith(".exe") else p.name()


def get_process_info(pid: int) -> dict:
    """가져오기 정보"""
    try:
        process = psutil.Process(pid)
        process_name = process.name()
        name = process_name.split(".exe")[0] if process_name.endswith(".exe") else process_name

        # 가져오기 정보
        parent_info = None
        try:
            parent_process = process.parent()
            if parent_process:
                parent_process_name = parent_process.name()
                if parent_process_name.endswith(".exe"):
                    parent_name = parent_process_name.split(".exe")[0]
                else:
                    parent_name = parent_process_name
                parent_info = {
                    "pid": parent_process.pid,
                    "name": parent_name,
                    "cmdline": (" ".join(parent_process.cmdline()) if parent_process.cmdline() else ""),
                }
        except:
            pass

        return {
            "pid": pid,
            "name": name,
            "cmdline": " ".join(process.cmdline()) if process.cmdline() else "",
            "parent": parent_info,
        }
    except Exception as e:
        return {"pid": pid, "name": f"지원하지 않는 (PID: {pid})", "error": str(e)}


def find_real_application_process(webview_pid: int) -> Optional[dict]:
    """
    에서WebView2까지정상의사용프로그램
    """
    try:
        webview_process = psutil.Process(webview_pid)

        # 조회여부예WebView2
        if "msedgewebview2" not in webview_process.name().lower():
            return get_process_info(webview_pid)

        # 결과가예WebView2, 조회
        parent_process = webview_process.parent()
        if parent_process:
            # 조회여부예시스템, 결과가예, 계속위조회
            parent_name = parent_process.name().lower()

            # 건너뛰기일시스템
            system_processes = ["svchost", "explorer", "winlogon", "csrss", "lsass"]

            if any(sys_proc in parent_name for sys_proc in system_processes):
                # 시도통신경과명령행매개변수 까지정상의사용
                cmdline = webview_process.cmdline()
                for cmd_part in cmdline:
                    if "--app-id=" in cmd_part or ".exe" in cmd_part:
                        # 가능패키지사용정보
                        break

                return get_process_info(parent_process.pid)
            else:
                return get_process_info(parent_process.pid)

        # 결과가있음까지합치기의, 반환WebView2의정보
        return get_process_info(webview_pid)

    except Exception as e:
        logger.info(f"에서WebView2까지정상의사용프로그램출력완료예외{e}")
        return get_process_info(webview_pid)


def get_java_process() -> tuple[list[int], list[str]]:
    username = os.getenv("USERNAME")
    if not username:
        logger.error("불가가져오기현재사용자명")
        return [], []

    temp_dir = os.path.join("C:\\Users", username, "AppData", "Local", "Temp")
    hsperf_dir = os.path.join(temp_dir, f"hsperfdata_{username}")
    if not os.path.exists(hsperf_dir):
        logger.error(f"hsperf찾을 수 없습니다: {hsperf_dir}")
        return [], []

    # 모든숫자파일이름(에서PID)
    pids = []
    for filename in os.listdir(hsperf_dir):
        if filename.isdigit():
            try:
                pid = int(filename)
                pids.append(pid)
            except ValueError:
                continue

    # 인증여부존재함
    java_pids = []
    for pid in pids:
        if psutil.pid_exists(pid):
            java_pids.append(pid)

    # 생성데이터
    prod_name_list = []
    prod_pid_list = []
    if java_pids:
        for pid in java_pids:
            process_info = psutil.Process(pid)
            prod_name_list.append(process_info.name())
            prod_pid_list.append(pid)
    return prod_pid_list, prod_name_list