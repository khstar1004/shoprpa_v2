import locale
import os
import subprocess
from abc import ABC, abstractmethod

import psutil

system_encoding = locale.getpreferredencoding()


class IProcessCore(ABC):
    @staticmethod
    @abstractmethod
    def run_cmd_admin(cmd: str, cwd: str = ""):
        pass

    @staticmethod
    @abstractmethod
    def run_cmd(cmd=None, cwd=None):
        pass

    @staticmethod
    @abstractmethod
    def get_pid_list():
        """
        가져오기현재모든pid
        """
        pass

    @staticmethod
    @abstractmethod
    def terminate_pid(pid: int, wait_time: int = 1):
        pass


class ProcessCoreWin(IProcessCore):
    @staticmethod
    def run_cmd_admin(cmd: str, cwd: str = ""):
        """
        으로관리자 권한실행명령
        """
        if not cmd:
            raise ValueError("명령은 비워 둘 수 없습니다")
        if not cwd:
            cwd = ""

        try:
            # 사용runas명령으로관리자 권한실행
            if cwd and os.path.exists(cwd):
                # 결과가있음디렉터리, 까지해당디렉터리
                full_cmd = f'cd /d "{cwd}" && {cmd}'
            else:
                full_cmd = cmd

            # 사용runas명령요청 관리자 권한
            process = subprocess.Popen(
                ["runas", "/user:Administrator", f'cmd /c "{full_cmd}"'],
                stdin=subprocess.DEVNULL,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
            )
            return process
        except Exception as e:
            raise RuntimeError(f"실행관리관리원명령실패: {e}")

    @staticmethod
    def run_cmd(cmd=None, cwd=None):
        """
        으로통신권한실행명령
        """
        if not cmd:
            raise ValueError("명령은 비워 둘 수 없습니다")
        if not cwd:
            cwd = None

        try:
            # 확인cmd예문자열형식
            if isinstance(cmd, list):
                cmd = " ".join(cmd)

            # 에서Windows위사용변경지정의매칭
            process = subprocess.Popen(
                cmd, cwd=cwd, shell=True, stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
            )
            return process
        except Exception as e:
            raise RuntimeError(f"실행명령실패: {e}")

    @staticmethod
    def get_pid_list():
        """
        가져오기현재모든pid
        """
        return psutil.process_iter(["name"])

    @staticmethod
    def terminate_pid(pid: int, wait_time: int = 1):
        try:
            process = psutil.Process(pid)
            process.terminate()
            try:
                process.wait(timeout=wait_time)
            except psutil.NoSuchProcess:
                pass
        except psutil.AccessDenied:
            raise ValueError(f"불가종료 {pid}: 방문.")
        except psutil.TimeoutExpired:
            raise ValueError(f" {pid} 미완료에서시간 초과시간내부종료.")
        except Exception as e:
            raise ValueError(f"{pid} 종료 중 오류: {e}")


class ProcessCoreLinux(IProcessCore):
    @staticmethod
    def run_cmd_admin(cmd: str, cwd: str = ""):
        """
        으로관리자 권한실행명령
        """
        if not cmd:
            raise ValueError("명령은 비워 둘 수 없습니다")

        try:
            full_cmd = f"sudo {cmd}"
            process = subprocess.Popen(
                full_cmd,
                cwd=cwd,
                shell=True,
                start_new_session=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                encoding=system_encoding,
                errors="replace",
            )
            return process
        except Exception as e:
            raise RuntimeError(f"실행관리관리원명령실패: {e}")

    @staticmethod
    def run_cmd(cmd=None, cwd=None):
        """
        으로통신권한실행명령
        """
        if not cmd:
            raise ValueError("명령은 비워 둘 수 없습니다")

        try:
            process = subprocess.Popen(
                cmd,
                cwd=cwd,
                start_new_session=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                encoding=system_encoding,
                errors="replace",
            )
            return process
        except Exception as e:
            raise RuntimeError(f"실행명령실패: {e}")

    @staticmethod
    def get_pid_list():
        """
        가져오기현재모든pid
        """
        return psutil.process_iter(["name"])

    @staticmethod
    def terminate_pid(pid: int, wait_time: int = 1):
        try:
            process = psutil.Process(pid)
            process.terminate()
            try:
                process.wait(timeout=wait_time)
            except psutil.NoSuchProcess:
                pass
        except psutil.AccessDenied:
            raise ValueError(f"불가종료 {pid}: 방문.")
        except psutil.TimeoutExpired:
            raise ValueError(f" {pid} 미완료에서시간 초과시간내부종료.")
        except Exception as e:
            raise ValueError(f"{pid} 종료 중 오류: {e}")
