import asyncio
import functools
import heapq
import json
import time
from collections.abc import Callable
from datetime import datetime
from typing import Any

import anyio
from astronverse.websocket_server.ws import (
    AckMsg,
    BaseMsg,
    Conn,
    ExitMsg,
    MsgUnlawfulnessError,
    PingMsg,
    PingTimeoutError,
    PongMsg,
    Route,
    Watch,
    WatchRetry,
    WatchTimeout,
    WsError,
    WsException,
    default_error_format,
    default_log,
    gen_exit_msg,
)


class AsyncOnce:
    """
    도구유형, 확인fun실행일
    """

    def __init__(self):
        self._done = False
        self._lock = asyncio.Lock()

    async def do(self, func, *args, **kwargs):
        if not self._done:
            async with self._lock:
                if not self._done:
                    self._done = True
                    await func(*args, **kwargs)


async def call(func, *args, **kwargs):
    """
    call방법법호출, 지원예외및정상일반
    """
    if not asyncio.iscoroutinefunction(func):
        if kwargs:
            func = functools.partial(func, **kwargs)
        return await anyio.to_thread.run_sync(func, *args)
    else:
        return await func(*args, **kwargs)


class WsManager:
    def __init__(
        self,
        ping_check_time: int = 30,
        ping_close_time: int = 90,
        error_format: Callable[[Exception], dict] = default_error_format,
        log: Callable[..., Any] = default_log,
    ):
        # 시스템일관리파일
        self.error_format = error_format
        # wrap logger to include timestamp prefix
        # def _log_with_ts(msg, *args, **kwargs):
        #     ts = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        #     return log("[{}] {}".format(ts, msg), *args, **kwargs)
        # self.log = _log_with_ts
        self.log = log

        # ping_check
        self.ping_check_time = ping_check_time
        self.ping_close_time = ping_close_time

        # 메시지
        self.watch_msg: dict[str, Watch] = {}
        self.watch_interval = 1
        self.watch_msg_queue: list = []

        # 경로 관리관리
        self.routes: dict[str, Route] = {}

        # 연결관리관리
        self.conns: dict[str, list[Conn]] = {}
        self.no_login_conns: list[Conn] = []

        # 시작작업
        self.check_ping_once = AsyncOnce()
        self.clear_watch_once = AsyncOnce()
        self.clear_ack_once = AsyncOnce()

    async def _send_text(self, conn: Conn, msg: str):
        # ping/pong메시지아니요출력로그, 로그경과다중
        try:
            data = json.loads(msg)
            channel = data.get("channel")
        except Exception:
            channel = None
        if channel not in (PingMsg.channel, PongMsg.channel):
            self.log(">>>{}".format(msg))
        await conn.send_text(msg)

    async def _call_route(self, channel: str, key: str, *args, **kwargs):
        """
        _call_route 경로 호출
        """
        name = "{}$${}".format(channel, key)
        if name in self.routes:
            func = self.routes[name].func
            return await call(func, *args, **kwargs)
        else:
            raise WsException("func is not exist: {}".format((channel, key)))

    @staticmethod
    async def _call_wait(watch: Watch, *args, **kwargs):
        if watch.callback:
            await call(watch.callback, *args, **kwargs)

    def _add_route(self, channel: str, key: str, func: Callable[[BaseMsg, Any], Any]):
        """
        _add_route 경로 추가
        """
        name = "{}$${}".format(channel, key)
        self.routes[name] = Route(channel=channel, key=key, func=func)

    def event(self, channel: str, key: str) -> Callable[..., Any]:
        """
        event 경로 추가설치기기
        """

        def decorator(func: Callable[..., Any]):
            self._add_route(channel, key, func)
            return func

        return decorator

    def _add_conn(self, uuid: str, conn: Conn):
        """
        _add_conn 추가연결
        """
        self.log("_add_conn {}".format(uuid))
        conn.last_ping = int(time.time())
        conn.uuid = uuid
        if not uuid:
            self.no_login_conns.append(conn)
        else:
            if uuid not in self.conns:
                self.conns[uuid] = []
            self.conns[uuid].append(conn)

    def _del_conn(self, uuid: str, conn: Conn):
        """
        _del_conn 삭제연결
        """
        self.log("_del_conn {}".format(uuid))
        try:
            if not uuid:
                self.no_login_conns.remove(conn)
                return True
            else:
                if uuid in self.conns:
                    self.conns[uuid].remove(conn)
                    if len(self.conns[uuid]) == 0:
                        del self.conns[uuid]
                    return True
                else:
                    return False
        except Exception as e:
            pass

    async def listen(self, uuid: str, conn: Conn, svc: Any = None):
        """
        listen 시작메시지
        """

        async def _listen():
            # 메시지
            if msg.channel == PingMsg.channel:
                conn.last_ping = int(time.time())
                await self._send_text(conn, PongMsg.tojson())
                return
            elif msg.channel == AckMsg.channel:
                name = "{}$${}".format("ack", msg.event_id)
                if name in self.watch_msg:
                    watch = self.watch_msg[name]
                    await self._call_wait(watch, msg, None)
                    del self.watch_msg[name]
                return
            elif msg.channel == ExitMsg.channel:
                # ExitMsg 로인쇄사용
                self.log("error ExitMsg: {}".format(msg))
                return

            # reply메시지
            if msg.reply_event_id:
                name = "{}$${}".format("reply", msg.reply_event_id)
                if name in self.watch_msg:
                    watch = self.watch_msg[name]
                    await self._call_wait(watch, msg, None)
                    del self.watch_msg[name]
                return

            # 분발송메시지
            res_msg = msg.to_reply()
            try:
                res = await self._call_route(msg.channel, msg.key, msg, svc)
                try:
                    res = json.loads(res.body.decode("utf-8"))
                except Exception:
                    pass
                res_msg.data = res
            except Exception as e2:
                err_msg = self.error_format(e2)
                if err_msg:
                    res_msg.data = err_msg

            # 메시지반환
            try:
                if res_msg.data is not None:
                    await self.send(res_msg)
            except Exception as e:
                self.log("error: _call_route {}".format(e))

        self._add_conn(uuid, conn)
        try:
            while True:
                text = await conn.ws.receive_text()
                # 검증인증
                try:
                    data = json.loads(text)
                    msg = BaseMsg(**data)

                    if not msg.channel:
                        await self._send_exit(conn, MsgUnlawfulnessError("msg unlawfulness, {}".format(msg.tojson())))
                        continue

                    if not msg.uuid:
                        msg.uuid = conn.uuid

                    if not msg.send_uuid:
                        msg.send_uuid = "$root$"
                except Exception as e:
                    # 법메시지기록로그정렬조회
                    self.log("<<<{}".format(text))
                    await self._send_exit(conn, MsgUnlawfulnessError("msg unlawfulness, {}".format(text)))
                    continue
                # ping/pong메시지아니요출력로그, 로그경과다중
                if msg.channel not in (PingMsg.channel, PongMsg.channel):
                    self.log("<<<{}".format(text))

                # 관리
                asyncio.create_task(_listen())
        except Exception as e:
            self.log("listen error {}".format(uuid))
            await self._send_exit(conn, e)
        finally:
            # 연결열기시시스템일관리
            if self._del_conn(conn.uuid, conn):
                try:
                    await conn.ws.close()
                except Exception as e:
                    pass

    async def _send_exit(self, conn: Conn, e: Exception = None):
        """
        _send_exit 전송exit
        """
        # 서비스오류 시도전송exit, 관리에서listen의finally
        if not isinstance(e, WsError):
            return
        try:
            err_msg = self.error_format(e)
            if err_msg:
                await self._send_text(conn, gen_exit_msg(err_msg).tojson())
        except Exception as e:
            pass

    def _add_watch(self, watch: Watch):
        """
        _add_watch 추가
        """
        name = "{}$${}".format(watch.watch_type, watch.watch_key)
        self.watch_msg[name] = watch
        heapq.heappush(self.watch_msg_queue, (watch.timeout, name))

    async def clear_watch(self):
        """
        clear_watch 관리경과watch
        """

        async def inner_clear_watch():
            while True:
                if self.watch_msg_queue:
                    now = datetime.now()
                    if self.watch_msg_queue[0][0] > now:
                        await asyncio.sleep((self.watch_msg_queue[0][0] - now).total_seconds())
                    _, name = heapq.heappop(self.watch_msg_queue)

                    try:
                        if name in self.watch_msg:
                            watch = self.watch_msg[name]
                            watch.retry()
                            if watch.time > watch.retry_time:
                                await self._call_wait(watch, None, WatchTimeout("watch timeout"))
                                del self.watch_msg[name]
                            else:
                                await self._call_wait(watch, None, WatchRetry("retry"))
                                heapq.heappush(self.watch_msg_queue, (watch.timeout, name))
                    except Exception as e:
                        self.log("error clear_watch: {}".format(e))
                else:
                    await asyncio.sleep(self.watch_interval)

        await self.clear_watch_once.do(inner_clear_watch)

    async def start_ping(self):
        """
        조회 ping 닫기 연결, 예외
        """

        async def inner_check_ping():
            while True:
                try:
                    for key, l in self.conns.items():
                        for index, item in enumerate(l):
                            if item.last_ping + self.ping_close_time < int(time.time()):
                                await self._send_exit(
                                    item, PingTimeoutError("ping time expires, {}".format(item.last_ping))
                                )
                    for index, item in enumerate(self.no_login_conns):
                        if item.last_ping + self.ping_close_time < int(time.time()):
                            await self._send_exit(
                                item, PingTimeoutError("ping time expires, {}".format(item.last_ping))
                            )
                except Exception as e:
                    self.log("error check_ping: {}".format(e))
                finally:
                    await asyncio.sleep(self.ping_check_time)

        await self.check_ping_once.do(inner_check_ping)

    async def send(self, msg: BaseMsg):
        """
        send 전송메시지, 지원ack기기제어
        """
        if not msg.send_uuid:
            raise WsException("msg unlawfulness, {}".format(msg.tojson()))

        if msg.send_uuid not in self.conns:
            self.log("uuid empty {} {}".format(msg.send_uuid, self.conns))
            raise WsException("send uuid empty")

        try:
            tasks = [asyncio.create_task(self._send_text(v, msg.tojson())) for v in self.conns[msg.send_uuid]]
            await asyncio.gather(*tasks)
        except Exception as e:
            pass

    async def send_reply(self, msg: BaseMsg, timeout, callback_func=None):
        msg.need_reply = True

        async def callback(watch_msg: BaseMsg = None, e: Exception = None):
            nonlocal callback_func
            if isinstance(e, WatchTimeout):
                # 완료출력
                return await call(callback_func, None, e)
            elif isinstance(e, WatchRetry):
                # 재시도
                return
            elif watch_msg:
                # 완료트리거성공
                return await call(callback_func, watch_msg, None)

        self._add_watch(
            Watch(
                watch_type="reply",
                watch_key=msg.event_id,
                callback=callback,
            ).init(timeout)
        )

        return await self.send(msg)