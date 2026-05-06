import os

import psutil
from astronverse.baseline.logger.logger import logger


def get_process_name(pid: int):
    p = psutil.Process(pid)
    return p.name().split(".exe")[0] if p.name().endswith(".exe") else p.name()


def get_java_process() -> tuple[list[int], list[str]]:
    username = os.getenv("USERNAME")
    if not username:
        logger.error("불가가져오기현재사용자명")
        return [], []

    hsperf_dir = os.path.join("C:\\Users", username, "AppData", "Local", "Temp", f"hsperfdata_{username}")
    if not os.path.exists(hsperf_dir):
        logger.error(f"hsperf아니요저장에서: {hsperf_dir}")
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

    # 검증인증여부저장에서
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