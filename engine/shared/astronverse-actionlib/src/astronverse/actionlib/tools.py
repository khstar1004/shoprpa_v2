import copy
import json
import time
import uuid as uid
from dataclasses import dataclass
from threading import Event
from typing import Any

from astronverse.actionlib.logger import logger


# ------------------rpawebsocket패키지내용------------------
def gen_event_id():
    """
    완료uuid
    """

    return "{}".format(str(uid.uuid4()))


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


# ------------------rpawebsocket패키지내용------------------


def tools(ws, tool, tool_value: Any, time_out_ws=5):
    """
    일report의프론트엔드통신의기기제어
    report의
    1. 아니요예통신경과관리, 있음메시지지연.
    2. 메시지필요대기프론트엔드돌아가기복사까지.

    목록전: 있음서비스사용
    """

    if not ws:
        return None, None

    wait = Event()
    res = {}
    error = None
    base_msg = BaseMsg(
        channel="flow", key="tools", uuid="$root$", send_uuid="$executor$", data={tool: tool_value}
    ).init()

    def callback(watch_msg: BaseMsg = None, e: Exception = None):
        nonlocal wait, res, error
        if watch_msg:
            logger.debug("info {}".format(watch_msg))
            res = watch_msg.data
        if e:
            # 시간 초과계속실행
            logger.error("error: res {}".format(e))
            error = e
        wait.set()

    ws.send_reply(base_msg, time_out_ws, callback)
    wait.wait()
    return res, error