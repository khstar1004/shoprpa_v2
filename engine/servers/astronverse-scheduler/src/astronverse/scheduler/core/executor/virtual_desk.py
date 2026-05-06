import os
import subprocess
import sys
import threading
import time

import requests
from astronverse.scheduler.logger import logger


class LinuxVirtualDesk:
    """사용xephyr생성"""

    def __init__(self, display=":10"):
        self.display = display
        self.xephyr = None
        self.env = os.environ.copy()
        self.env["DISPLAY"] = display
        self.resolution = "1280x720"

    def start(self, minimized=False):
        """
        시작Xephyr

        Args:
            minimized: 여부소시작, 로True
        """
        if not (self.xephyr is not None and self.xephyr.poll() is None):
            # 생성Xephyr명령
            cmd = [
                "Xephyr",
                self.display,
                "-screen",
                self.resolution,
                "-title",
                "Shoprpa",
            ]

            # 사용가져오기까지의분시작Xephyr
            self.xephyr = subprocess.Popen(
                cmd, stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
            )
            time.sleep(2)  # 적음대기

    def stop(self):
        """닫기 : 사용자닫기"""
        pass


class WindowVirtualDesk:
    def __init__(self):
        self.svc = None
        self.resolution = "1280x720"
        self.handler = None

    def start(self, svc, minimized=False):
        """
        시작

        Args:
            svc: 위아래문서객체
            minimized: 여부소시작, 로True
        """
        if not (self.handler is not None and self.handler.poll() is None):
            self.svc = svc
            vd_path = os.path.join(os.path.dirname(__file__), "virtual_desktop", "ShoprpaRDP.exe")
            ve_path = os.path.join(os.path.dirname(__file__), "virtual_desktop", "virtual-engine.exe")
            python_path = os.path.abspath(svc.config.python_base)
            port = svc.win_virtual_port

            cmd = [vd_path, ve_path, f"--python-path={python_path}", f"--port={port}"]
            self.handler = subprocess.Popen(
                cmd,
                stdin=subprocess.DEVNULL,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
            )
            time.sleep(2)  # 적음대기
        else:
            logger.info("완료열기시작")

    def stop(self):
        """닫기 : 사용자닫기"""
        pass


class WindowVirtualDeskSubprocessAdapter:
    """subprocess매칭기기"""

    def __init__(self, svc, params: dict = None, exec_python=None):
        if not params:
            params = {}
        self.params = params
        self.svc = svc
        self.json = None
        self.live_cache = None
        self.live_cache_time = None
        self.set_param("exec_python", exec_python)

    def run(self, timeout=10 * 60):
        def is_start():
            is_live = virtual_desk.handler is not None and virtual_desk.handler.poll() is None
            if not is_live:
                logger.info("조회여부열기시작 {}".format(False))
                raise Exception("조회여부열기시작")

            try:
                url = "http://127.0.0.1:{}/is_alive".format(self.svc.win_virtual_port)
                response = requests.get(url)
                if response.status_code != 200:
                    logger.info("조회여부열기시작 {}".format(False))
                    return False
                logger.info("조회여부열기시작 {}".format(True))
                return True
            except Exception as e:
                logger.warning("조회여부열기시작 {}".format(False))
                return False

        def raw_run():
            url = "http://127.0.0.1:{}/run".format(self.svc.win_virtual_port)
            response = requests.post(url, json=self.params)
            return response.json()

        i = 0
        while not is_start():
            time.sleep(2)
            i += 2
            if i > timeout:
                raise Exception("시작시간 초과")
        threading.Thread(target=raw_run, daemon=True).start()

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
        if self.live_cache is not None and self.live_cache_time > time.time():
            return self.live_cache

        logger.info("조회작업")
        self.live_cache_time = time.time() + 2
        try:
            url = "http://127.0.0.1:{}/is_alive".format(self.svc.win_virtual_port)
            response = requests.get(url)
            response_json = response.json()
            self.live_cache = response_json["data"]["current_process"]["has_running_process"]
            logger.info("조회작업 {}".format(self.live_cache))
        except Exception as e:
            logger.error("조회작업 {}".format(e))
            self.live_cache = False
        return self.live_cache

    def kill(self):
        try:
            logger.info("조회작업닫기")
            url = "http://127.0.0.1:{}/kill".format(self.svc.win_virtual_port)
            response = requests.post(url)
            result = response.json()
            logger.info("조회작업닫기 {}".format(result))
            return result
        except Exception as e:
            logger.error("조회작업닫기 {}".format(e))


if sys.platform != "win32":
    virtual_desk = LinuxVirtualDesk()
else:
    virtual_desk = WindowVirtualDesk()
