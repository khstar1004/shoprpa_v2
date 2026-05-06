import locale
import os
import subprocess
import sys
import time

import psutil
from astronverse.scheduler.logger import logger
from astronverse.scheduler.utils.utils import kill_proc_tree

system_encoding = locale.getpreferredencoding()


class Process:
    """프로세스 관리유형"""

    @staticmethod
    def kill_all_zombie():
        """모든의"""
        zombie_processes = Process.get_python_proc_in_current_dir()
        for z_p in zombie_processes:
            kill_proc_tree(z_p)

    @staticmethod
    def get_python_proc_in_current_dir():
        """가져오기모든에서현재디렉터리Python"""

        if sys.platform != "win32":
            return []

        # 모든필요닫기 의
        all_process_ids = []
        for process_name in ["python.exe", "route.exe", "ConsoleApp1.exe", "winvnc.exe"]:
            output = subprocess.check_output(
                ["tasklist", "/FI", f"IMAGENAME eq {process_name}", "/FO", "CSV"],
                encoding=system_encoding,
                errors="replace",
            )
            for line in output.splitlines()[1:]:
                parts = line.split(",")
                if len(parts) > 1:
                    pid = parts[1].strip('"')
                    all_process_ids.append(int(pid))

        if not all_process_ids:
            return []

        # 의정보
        self_proc_id_list = []
        try:
            proc = psutil.Process(os.getpid())
            while proc:
                self_proc_id_list.append(proc.pid)
                proc = proc.parent()
        except psutil.NoSuchProcess:
            pass

        # 모든의여부합치기관리
        all_process = list()
        for pid in all_process_ids:
            try:
                # 
                if pid in self_proc_id_list:
                    continue

                # 조회cwd
                proc = psutil.Process(pid)
                proc_cwd = proc.exe()
                if "shoprpa" not in proc_cwd:
                    continue

                # 기호합치기파일
                all_process.append(proc)
            except Exception as e:
                pass
        return all_process

    @staticmethod
    def get_root_process(proc):
        """
        가져오기 (일개아니오예python의)
        """
        if "python" in proc.name():
            p_proc = proc.parent()
            return Process.get_root_process(p_proc)
        else:
            return proc

    @staticmethod
    def pid_exist_check():
        # 일열기 시작가져오기root, 아니오예rpa, 예외3초가져오기
        time.sleep(3)
        self = psutil.Process(os.getpid())
        root = Process.get_root_process(self)
        root_id = root.pid
        root_name = root.name()
        while True:
            time.sleep(1)
            try:
                if not psutil.pid_exists(root_id) or psutil.Process(root_id).name() != root_name:
                    logger.info("pid_exist_check kill process...")
                    # 일
                    kill_proc_tree(psutil.Process(os.getpid()), exclude_pids=[os.getpid()])
                    # 까지현재의시작경로의모든python일
                    Process.kill_all_zombie()
                    # 행
                    kill_proc_tree(psutil.Process(os.getpid()))
            except Exception as e:
                logger.exception(e)