import asyncio
from typing import Any

from astronverse.scheduler.core.svc import Svc, get_svc
from astronverse.scheduler.logger import logger
from astronverse.websocket_server.ws import Conn, IWebSocket
from astronverse.websocket_server.ws_service import WsManager
from fastapi import APIRouter, Depends
from fastapi.websockets import WebSocket

router = APIRouter()


def ws_log(msg):
    """로그인쇄"""
    logger.info(msg)


wsmg = WsManager(log=ws_log, ping_close_time=300)


class WsSocket(IWebSocket):
    """websocket연결유형, 완료IWebSocket연결, 의목록의필요예로완료내용fastapi연결및websocket의WebSocketServerProtocol연결"""

    def __init__(self, ws: WebSocket):
        self.ws = ws

    async def receive_text(self) -> str:
        res = await self.ws.receive()
        return str(res)

    async def send(self, message: Any) -> None:
        return await self.ws.send(message)

    async def close(self) -> None:
        return await self.ws.close()


@router.websocket("/ws")
async def websocket(ws: WebSocket, svc: Svc = Depends(get_svc)):
    try:
        uuid = "$executor$"
        await asyncio.gather(
            wsmg.listen(uuid, Conn(ws=WsSocket(ws)), svc),
            wsmg.start_ping(),
            wsmg.clear_watch(),
        )
    except Exception as e:
        logger.error("websocket error {}".format(e))


# 아래예파일회원가입