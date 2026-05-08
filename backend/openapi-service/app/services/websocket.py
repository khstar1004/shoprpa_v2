from typing import Any

from fastapi import WebSocket
from rpawebsocket.ws import IWebSocket
from rpawebsocket.ws_service import WsManager

from app.logger import get_logger

logger = get_logger(__name__)


class WsService(IWebSocket):
    """개예로완료내용 websocket및fastapi의예외의설치유형"""

    def __init__(self, ws: WebSocket):
        self.ws = ws

    async def receive_text(self) -> str:
        return await self.ws.receive_text()

    async def send(self, message: Any) -> None:
        return await self.ws.send({"type": "websocket.send", "text": message})

    async def close(self) -> None:
        return await self.ws.close()


def ws_log(msg):
    logger.info(msg)


class WsManagerService:
    def __init__(self):
        self.ws_manager = WsManager(log=ws_log)