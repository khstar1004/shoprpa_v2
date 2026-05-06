"""서비스위아래문서관리관리모듈"""

import asyncio
import platform
import sys
import threading
import time
from typing import Any

from astronverse.picker import IEventCore, IPickerCore, PickerSign, SVCSign
from astronverse.picker.error import *
from astronverse.picker.error import BaseException as RpaBaseException
from astronverse.picker.logger import logger


class SyncMap:
    """설치전체의유형"""

    def __init__(self) -> None:
        self.lock = threading.Lock()
        self.map = {}

    def __setitem__(self, key, value) -> None:
        with self.lock:
            self.map[key] = value

    def __getitem__(self, key) -> Any:
        with self.lock:
            return self.map.get(key)

    def __delitem__(self, key) -> None:
        with self.lock:
            if key in self.map:
                del self.map[key]

    def __contains__(self, key) -> bool:
        with self.lock:
            return key in self.map


class ServiceContext:
    """프로그램위아래문서관리관리유형"""

    def __init__(self, port: int, highlight_socket_port: int, route_port: int) -> None:
        # 실행매칭
        self.port: str = port
        self.highlight_socket_port: str = highlight_socket_port
        self.route_port: str = route_port

        # 전체영역실행파일
        self.__sign__: SyncMap = SyncMap()  # PickerSign

        # 
        self.strategy = None

        # 컴포넌트
        self.highlight_client = None
        self.locator = None
        self.event_core = None
        self.picker_core = None

        self.pick_server = None

        # sap사용
        self.sapguiauto = None
        self.application = None

        # highlight닫기사용
        self.event_tag = None

    def load_modules(self):
        """로드시스템모듈컴포넌트"""

        # 높이
        from astronverse.picker.core.highlight_client import highlight_client

        self.highlight_client = highlight_client

        # 
        from astronverse.picker.strategy.manager import Strategy

        self.strategy: Strategy = Strategy(self)

        # 선택 - 평면닫기가져오기
        if sys.platform == "win32":
            from astronverse.picker.core.event_core_win import EventCore
            from astronverse.picker.core.picker_core_win import PickerCore

            self.event_core: IEventCore = EventCore()
            self.picker_core: IPickerCore = PickerCore()
        elif platform.system() == "Linux":
            pass

        # 지정 - 선택 가능모듈
        try:
            from astronverse.locator.locator import LocatorManager

            self.locator = LocatorManager
        except ImportError:
            logger.info("불가가져오기rpa_locator모듈, 예필요요청 ")
            self.locator = None

    def tag(self, tag=SVCSign.PICKER):
        self.event_tag = tag

    def sign(self) -> SyncMap:
        """가져오기정보 객체"""
        return self.__sign__

    async def send_sign(self, sign: PickerSign, data: Any, interval: int = 180) -> Any:
        """전송정보 대기"""
        # 빈반환값전송sign
        result_sign = f"{sign.value}_RES"
        if result_sign in self.__sign__:
            del self.__sign__[result_sign]
        self.__sign__[sign.value] = data

        # 시간 초과대기sign의반환결과
        start_time = interval
        self.pick_server.start_time = time.time()
        while result_sign not in self.__sign__ and start_time > 0:
            await asyncio.sleep(0.1)
            start_time -= 0.1
            if self.pick_server and self.pick_server.start_time and time.time() - self.pick_server.start_time > 15:
                self.event_core.close()  # 닫기 
                raise RpaBaseException(TIMEOUT_LAG, "선택초과경과15s, 요청 출력기기후다시 이동")
        if start_time <= 0:
            raise RpaBaseException(TIMEOUT, "선택시간 초과")

        # 반환값
        return self.__sign__[result_sign]