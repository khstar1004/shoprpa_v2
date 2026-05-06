import sys
import threading
import time

import requests
from astronverse.scheduler import ComponentType
from astronverse.scheduler.config import Config
from astronverse.scheduler.core.executor.executor import ExecutorManager
from astronverse.scheduler.core.picker.picker import Picker
from astronverse.scheduler.core.servers.normal_server import TriggerServer, VNCServer
from astronverse.scheduler.logger import logger
from astronverse.scheduler.utils.utils import check_port


class Svc:
    def __init__(self):
        """프로그램위아래문서관리관리유형"""
        # 0. 서비스단말
        self.port_lock = threading.Lock()
        self.port_dict: dict = {}

        # 1. 매칭

        # 실행매칭 
        self.config: Config = None

        # 단말:
        self.__local_port__: int = 13158
        # 경로 단말[기기분매칭]
        self.rpa_route_port: int = self.get_validate_port(ComponentType.ROUTE)
        # 스케줄링기기단말[기기분매칭]
        self.scheduler_port: int = self.get_validate_port(ComponentType.SCHEDULER)
        # trigger단말[기기분매칭]
        self.trigger_port: int = self.get_validate_port(ComponentType.TRIGGER)
        # 브라우저통신단말[지정분매칭]
        self.connector_port: int = 9082
        self.port_dict[ComponentType.BROWSER_CONNECTOR.name.lower()] = self.connector_port
        # 선택[지정분매칭]
        self.rpa_hl_port: int = 11001
        # 
        self.win_virtual_port: int = self.get_validate_port()

        # 2. 핵심 서비스

        # 본경로 여부시작식별자
        self.route_server_is_start = False
        # 실행기기
        self.executor_mg = ExecutorManager(self)
        # pick
        self.picker = Picker(self)
        # 트리거기기
        self.trigger_server = TriggerServer(self)
        # nav서비스
        if sys.platform == "win32":
            self.vnc_server = VNCServer(self)
        else:
            self.vnc_server = None

        # 3. 방식

        # 여부예단말방식
        self.terminal_mod = False
        self.start_watch = False
        self.terminal_task_stop = False

        # 여부예에서중실행[중실행, 실행기기아니오생성]
        self.is_venv = False

        # 4. 전체영역상태
        self.pip_download_ing = False

    def set_config(self, config):
        self.config = config
        self.is_venv = True if "venv" in self.config.python_base else False
        self.picker.init()

    def get_validate_port(self, component_type: ComponentType = None) -> int:
        """
        가져오기일개본가능사용의단말
        """
        with self.port_lock:
            while True:
                self.__local_port__ += 1
                # 길이시간실행의단말제목
                if self.__local_port__ >= 65500:
                    self.__local_port__ = 13158
                if check_port(self.__local_port__):
                    if component_type is not None:
                        self.port_dict[component_type.name.lower()] = self.__local_port__
                    return self.__local_port__

    def register_server(self):
        def register_component(component, port: int):
            try:
                url = "http://127.0.0.1:{}/rpa-local-route/registry".format(self.rpa_route_port)
                data = {"module_name": component, "port": str(port)}
                response = requests.post(
                    url=url,
                    json=data,
                )
                logger.info("register_component: {} => {}".format(data, response.text))
                if "OK" in response.text:
                    pass
                else:
                    raise Exception("route register error : {} {} => {}".format(component, port, response.text))
            except Exception as e:
                logger.exception("register_component error: {}".format(e))

        if len(self.port_dict) == 0:
            return

        if not self.route_server_is_start:
            return

        # 대기본경로 로드완료
        while check_port(port=self.rpa_route_port):
            time.sleep(0.1)
        for k, v in self.port_dict.items():
            register_component(k, v)


_svc = Svc()


def get_svc() -> Svc:
    """
    가져오기전체영역svc
    """
    return _svc