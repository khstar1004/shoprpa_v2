import asyncio
import json
import queue
from dataclasses import dataclass
from typing import Any

import websockets
from astronverse.actionlib.atomic import atomicMg
from astronverse.executor import ExecuteStatus
from astronverse.executor.error import *
from astronverse.executor.logger import logger
from astronverse.websocket_server.ws import BaseMsg, Conn, IWebSocket
from astronverse.websocket_server.ws_service import AsyncOnce, WsManager
from websockets import ServerConnection


@dataclass
class CustomResponse:
    """지정의반환값"""

    code: str
    msg: str
    data: Any


def ws_log(msg):
    """로그인쇄"""
    logger.info(msg)


def error_format(e=None) -> dict:
    """오류형식"""

    def error_to_base_error() -> BaseException:
        if isinstance(e, BaseException):
            return e
        return BaseException(GENERAL_ERROR_FORMAT.format(e), "내부 오류 error: {}".format(e))

    def gen_error_msg(exc: BaseException):
        return CustomResponse(exc.code.code.value, exc.code.message, {}).__dict__

    return gen_error_msg(error_to_base_error())


wsmg = WsManager(error_format=error_format, log=ws_log, ping_close_time=300)


class WsSocket(IWebSocket):
    """websocket연결유형, 완료IWebSocket연결"""

    def __init__(self, ws: ServerConnection):
        self.ws = ws

    async def receive_text(self) -> str:
        res = await self.ws.recv()
        return str(res)

    async def send(self, message: Any) -> None:
        return await self.ws.send(message)

    async def close(self) -> None:
        return await self.ws.close()


class Ws:
    loop = None

    def __init__(self, svc):
        self.svc = svc

        atomicMg.cfg()["WS"] = self

        self.is_open_web_link = False
        self.is_web_link = False
        self.is_open_top_link = False
        self.is_tip_link = False

        self.BASE_MSG = BaseMsg(channel="flow", key="report", uuid="$root$")
        self.report_once = AsyncOnce()

    def check_ws_link(self):
        if self.is_open_web_link and not self.is_web_link:
            return False
        if self.is_open_top_link and not self.is_tip_link:
            return False
        return True

    @staticmethod
    async def send_text(conn: Conn, msg: str):
        await conn.send_text(msg)

    @staticmethod
    def send_reply(msg, timeout, callback_func=None):
        """전송메시지대기돌아가기복사"""

        raw_data = msg.get("data") or {}  #  None
        raw_data["msg_str"] = MSG_SUB_WINDOW  # 작업증가삭제수정

        msg = BaseMsg(
            channel="flow",
            key="sub_window",
            uuid="$root$",
            send_uuid="$executor$",
            need_reply=True,
            data=msg.get("data"),
        ).init()

        async def raw_send_reply():
            await wsmg.send_reply(msg, timeout, callback_func)

        msg = msg.init()
        future = asyncio.run_coroutine_threadsafe(raw_send_reply(), Ws.loop)
        future.result(timeout)  # 직선까지완료또는시간 초과

    async def send_report(self, q: queue.Queue):
        async def inner_send_report():
            i = 1
            drop_max_size = int(q.maxsize / 10 * 8)
            drop_min_size = int(q.maxsize / 10 * 2)
            drop_num = 0

            while True:
                if not self.check_ws_link():
                    await asyncio.sleep(0.3)
                    continue
                try:
                    msg = q.get_nowait()
                except queue.Empty:
                    await asyncio.sleep(0.3)
                    continue

                try:
                    # 결과가예tip연결있음의빈
                    if not self.is_open_web_link:
                        # 메시지다중직선연결, 빠름
                        if q.qsize() > drop_max_size:
                            for i in range(drop_max_size - drop_min_size):
                                msg = q.get()
                                pass

                    # 필요전송
                    data = json.loads(msg)
                    tag = data.get("tag", None)
                    if tag == "tip":
                        # 필요전송tip
                        is_send_web = False
                        is_send_tip = True
                    else:
                        # 필요전송
                        is_send_web = True
                        is_send_tip = True

                    tasks_1 = []
                    tasks_2 = []
                    if is_send_web and wsmg.conns.get("$executor$"):
                        self.BASE_MSG.send_uuid = "$executor$"
                        self.BASE_MSG.init().data = data
                        tasks_1 = [
                            asyncio.create_task(self.send_text(v1, self.BASE_MSG.tojson()))
                            for v1 in wsmg.conns[self.BASE_MSG.send_uuid]
                        ]
                    if is_send_tip and wsmg.conns.get("$executor_tip$"):
                        # tip까지의아래직선연결, 계획수30개출력1개
                        if q.qsize() > drop_min_size and drop_num < 30:
                            tasks_2 = []
                            drop_num += 1
                        else:
                            drop_num = 0
                            self.BASE_MSG.send_uuid = "$executor_tip$"
                            self.BASE_MSG.init().data = data
                            tasks_2 = [
                                asyncio.create_task(self.send_text(v2, self.BASE_MSG.tojson()))
                                for v2 in wsmg.conns[self.BASE_MSG.send_uuid]
                            ]
                    tasks = tasks_1 + tasks_2
                    if tasks:
                        i += 1
                        if i % 30 == 0:
                            i = 1
                            await asyncio.sleep(0.3)  # 매전송30메시지0.3초
                        await asyncio.gather(*tasks)
                except Exception as e:
                    pass

        await self.report_once.do(inner_send_report)

    async def websocket_endpoint(self, ws: ServerConnection):
        try:
            path = ws.request.path

            uuid = "$executor$"
            if path in ["", "/"]:
                self.is_web_link = True
                uuid = "$executor$"  # 분일아래예web로그
            elif path == "/?tag=tip":
                uuid = "$executor_tip$"  # 분일아래예오른쪽아래역할로그
                self.is_tip_link = True
            else:
                # 파일아니오관리
                pass

            await asyncio.gather(
                wsmg.listen(uuid, Conn(ws=WsSocket(ws)), self.svc),
                wsmg.start_ping(),
                wsmg.clear_watch(),
                self.send_report(self.svc.report.queue),
            )
        except Exception as e:
            if isinstance(e, BaseException):
                error_str = e.code.message
            else:
                error_str = str(e)
            self.svc.end(ExecuteStatus.FAIL, reason=error_str)

    def server(self):
        from astronverse.executor.debug.apis.apis import route_init

        route_init()

        try:
            loop = asyncio.get_running_loop()
        except RuntimeError:
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)

        Ws.loop = loop

        async def _start():
            srv = await websockets.serve(self.websocket_endpoint, "127.0.0.1", self.svc.conf.port)
            logger.info("서비스완료시작 ws://127.0.0.1:%s", self.svc.conf.port)
            await asyncio.Event().wait()  # , 대기가격 run_forever
            return srv

        loop.run_until_complete(_start())  # 변환완료