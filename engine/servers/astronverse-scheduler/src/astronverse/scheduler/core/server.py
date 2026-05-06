import threading
import time
from abc import ABC, abstractmethod
from concurrent.futures import ThreadPoolExecutor

from astronverse.scheduler import ServerLevel
from astronverse.scheduler.logger import logger


class IServer(ABC):
    def __init__(
        self,
        svc=None,
        name: str = "",
        level: ServerLevel = ServerLevel.NORMAL,
        run_is_async: bool = False,
    ):
        # 이름
        self.name = name
        # 대기단계
        self.level = level
        # 서비스위아래문서
        self.svc = svc
        # run여부필요예외실행
        self.run_is_async = run_is_async
        # recover
        self.recover_ing = False

    @abstractmethod
    def run(self):
        """시작서비스, 할 수 없음패키지"""
        pass

    def health(self) -> bool:
        """ 10s 트리거일 반환True"""
        return True

    def recover(self):
        """조회복사"""
        pass


class ServerManager:
    """서비스관리관리"""

    def __init__(self, svc):
        self.server_list = []
        self.svc = svc

    def register(self, server: IServer):
        """회원가입"""
        self.server_list.append(server)
        return self

    def check(self):
        """감지"""
        while True:
            time.sleep(10)
            try:
                for server in self.server_list:
                    if not server.health() and server.recover_ing is False:
                        logger.info(f"{server.name} is not health, start recover")
                        server.recover_ing = True
                        server.recover()
                        server.recover_ing = False
            except Exception as e:
                pass

    def run(self):
        # 1. 시작code의예외
        for c_server in self.server_list:
            if not c_server.run_is_async and c_server.level == ServerLevel.CORE:
                c_server.run()

        def async_run():
            while not self.svc.route_server_is_start:
                time.sleep(1)

            # 2. 시작통신의의예외
            for n_server in self.server_list:
                if not n_server.run_is_async and n_server.level == ServerLevel.NORMAL:
                    n_server.run()

            # 3. 시작예외
            with ThreadPoolExecutor() as pool:
                for a_server in self.server_list:
                    if a_server.run_is_async:
                        pool.submit(a_server.run)

        threading.Thread(target=async_run, daemon=True).start()
        threading.Thread(target=self.check, daemon=True).start()
        return self