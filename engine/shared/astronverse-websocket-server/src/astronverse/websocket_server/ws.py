import copy
import json
import logging
import time
import uuid as uid
from abc import ABC, abstractmethod
from collections.abc import Callable
from dataclasses import dataclass
from datetime import datetime, timedelta
from typing import Any, Union


class IWebSocket(ABC):
    @abstractmethod
    async def receive_text(self) -> str:
        pass

    @abstractmethod
    async def send(self, message: Any) -> None:
        pass

    @abstractmethod
    async def close(self) -> None:
        pass


def gen_event_id():
    """
    완료uuid
    """

    return "{}".format(str(uid.uuid4()))


def default_error_format(e: Exception = None) -> Union[None, dict]:
    """
    default_error_format 오류형식
    """
    return None


def default_log(msg, *args, **kwargs):
    logging.info(msg, *args, **kwargs)


@dataclass
class Conn:
    """
    Conn 연결관리관리기기
    """

    # 
    ws: IWebSocket = None
    # msg의uuid
    uuid: str = ""
    # 후일ping의시간
    last_ping: int = 0

    async def send_text(self, data: str) -> None:
        await self.ws.send(data)


@dataclass
class BaseMsg:
    """
    BaseMsg 메시지
    """

    # 돌아가기복사파일id 완료http의방식사용 합치기일일돌아가기의메시지
    reply_event_id: str = None
    # 파일id 일값
    event_id: str = None
    # 파일발송의시간
    event_time: int = None
    # 서비스 대서비스
    channel: str = None
    # 파일이름 대서비스아래의소파일
    key: str = None
    # 사용자명, 사용자식별자, 일개사용자다중개연결, 전송시, 필요시해제돌아가기아니요시제목
    uuid: str = None
    # 메시지전송
    send_uuid: str = None
    # 여부필요ack[없음]
    need_ack: bool = None
    # 여부필요돌아가기복사
    need_reply: bool = None
    # 데이터
    data: dict = None

    def to_reply(self):
        res_msg = copy.deepcopy(self)
        res_msg.reply_event_id = self.event_id
        res_msg.uuid = self.send_uuid
        res_msg.send_uuid = self.uuid
        res_msg.data = None
        res_msg.init()
        return res_msg

    def init(self):
        self.event_id = gen_event_id()
        self.event_time = int(time.time())
        return self

    def tojson(self, filtered_none: bool = True):
        data = self.__dict__
        if filtered_none:
            data = {k: v for k, v in data.items() if v is not None}
        return json.dumps(data, ensure_ascii=False)


@dataclass
class Route:
    """
    Route 의경로 
    """

    # msg의channel
    channel: str = ""
    # msg의key
    key: str = ""
    # 돌아가기조정
    func: Callable[[BaseMsg, Any], Any] = None


@dataclass
class Watch:
    # 유형
    watch_type: str = ""
    # key
    watch_key: str = ""
    # 트리거callback
    callback: Callable[[Union[BaseMsg, None], Union[Exception, None]], Any] = None

    # 금액외부재시도 데이터
    retry_time: int = 0
    # 
    interval: int = 10

    # 경과시간
    timeout: datetime = None
    # 재시도 데이터
    time: int = 0

    def init(self, interval: int = 10):
        self.interval = interval
        self.timeout = datetime.now() + timedelta(seconds=interval)
        return self

    def retry(self):
        self.time += 1
        self.timeout += timedelta(seconds=self.interval)


class WsException(Exception):
    """
    WsException ws예외
    """

    pass


class WatchRetry(WsException):
    """
    WatchTimeout watch재시도
    """

    pass


class WatchTimeout(WsException):
    """
    WatchTimeout watch시간 초과
    """

    pass


class WsError(WsException):
    """
    WsError ws오류, 닫기연결
    """

    pass


class PingTimeoutError(WsError):
    """
    PingTimeoutException ping시간 초과
    """

    pass


class MsgUnlawfulnessError(WsError):
    """
    MsgUnlawfulnessError 메시지아니요합치기법
    """

    pass


PingMsg = BaseMsg(channel="ping")
PongMsg = BaseMsg(channel="pong")
ExitMsg = BaseMsg(channel="exit")
AckMsg = BaseMsg(channel="ack")


def gen_ack_msg(event_id: str = ""):
    """
    gen_ack_msg 빠름완료ack메시지
    """
    return BaseMsg(channel=AckMsg.channel, event_id=event_id)


def gen_exit_msg(data: dict = None):
    """
    gen_exit_msg 빠름완료exit출력메시지
    """
    return BaseMsg(channel=ExitMsg.channel, data=data)