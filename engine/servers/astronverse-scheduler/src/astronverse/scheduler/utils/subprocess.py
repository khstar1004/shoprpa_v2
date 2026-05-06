import os
import shlex
import subprocess
import sys
import threading
import time

import psutil
from astronverse.scheduler.logger import logger
from astronverse.scheduler.utils.utils import kill_proc_tree


def disable_cmd_quick_edit():
    # window있음의
    if sys.platform == "win32":
        import ctypes

        # 지정일반량
        ENABLE_QUICK_EDIT_MODE = 0x0040
        ENABLE_EXTENDED_FLAGS = 0x0080

        # 가져오기 입력의
        kernel32 = ctypes.windll.kernel32
        hStdin = kernel32.GetStdHandle(-10)

        # 가져오기현재입력방식
        mode = ctypes.c_uint32()
        kernel32.GetConsoleMode(hStdin, ctypes.byref(mode))

        # 사용 안 함빠름방식
        mode.value &= ~ENABLE_QUICK_EDIT_MODE
        mode.value |= ENABLE_EXTENDED_FLAGS

        # 새의입력방식
        kernel32.SetConsoleMode(hStdin, mode)


def default_output_callback(msg):
    logger.info("[RES]{}".format(msg))


def async_default_output_callback(msg, error):
    if error:
        logger.info("[ERR]{}".format(error))
    else:
        logger.info("[RES]{}".format(msg))


class SubPopen:
    def __init__(self, name: str = None, cmd: list = None, params: dict = None):
        if not params:
            params = {}
        if not cmd:
            cmd = []
        self.name = name
        self.cmd = cmd
        self.params = params
        self.proc = None
        self.start_time = 0
        self.__log__ = None

    def logger_handler(self, output_callback=default_output_callback, timeout=None) -> (str, str):
        if self.__log__:

            def read_stdout(pipe, callback):
                """가져오기 출력의데이터"""
                try:
                    for text in iter(pipe.readline, ""):
                        if callback:
                            callback(text.strip())
                except Exception:
                    pass

            stdout_thread = threading.Thread(
                target=read_stdout,
                args=(self.proc.stdout, output_callback),
                daemon=True,
            )
            stdout_thread.start()

            self.proc.wait(timeout=timeout)
            stdout_thread.join()

            stderr_data = self.proc.stderr.read()
            return "", stderr_data
        else:
            return self.proc.communicate(timeout=timeout)

    def async_logger_handler(self, output_callback=async_default_output_callback):
        if not self.__log__:
            # 지원하지 않음있음로그의
            return

        def read_stdout(proc, callback):
            """가져오기 출력의데이터"""
            try:
                for text in iter(proc.stdout.readline, ""):
                    if callback:
                        callback(text.strip())
            except Exception as e:
                pass
            finally:
                proc.wait()
                stderr = proc.stderr.read()
                if proc.returncode != 0 and callback:
                    callback("", stderr)
                else:
                    pass

        stdout_thread = threading.Thread(target=read_stdout, args=(self.proc, output_callback), daemon=True)
        stdout_thread.start()
        return

    def run(self, shell: bool = None, log: bool = False, encoding="utf-8", env=None) -> "SubPopen":
        disable_cmd_quick_edit()

        # shell 값
        if shell is None:
            if sys.platform == "win32":
                shell = True
            else:
                shell = False

        # 매개변수
        param_list = [f"--{key}={shlex.quote(str(value))}" for key, value in self.params.items()]
        cmd = self.cmd + param_list
        logger.info(
            "cmd: {} env".format(
                cmd,
            )
        )

        # 시작여부패키지log
        self.start_time = time.time()

        # 비고모듈분명령실행할 수 없음추가env, 선택컴포넌트, 추가위env가져오기 선택시작예외
        if env is None:
            current_env = os.environ.copy()
            current_env["no_proxy"] = "True"
            env = current_env if "pip" in cmd else None

        self.__log__ = log

        self.proc = subprocess.Popen(
            cmd,
            shell=shell,
            stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE if log else subprocess.DEVNULL,
            stderr=subprocess.PIPE if log else subprocess.DEVNULL,
            text=True,
            env=env,
            encoding=encoding,
            errors="replace",
        )
        return self

    def set_param(self, key, val):
        """
        매개변수
        """
        self.params[key] = val

    def get_param(self, key):
        """
        가져오기매개변수
        """
        return self.params[key]

    def is_alive(self):
        """
        여부저장
        """
        return self.proc is not None and self.proc.poll() is None

    def kill(self):
        if self.proc:
            try:
                # 결과가완료닫기가능오류
                kill_proc_tree(psutil.Process(self.proc.pid), including_parent=True)
                self.proc.wait()
            except Exception as e:
                pass