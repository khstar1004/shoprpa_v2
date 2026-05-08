import asyncio
import copy
import threading
import time

from astronverse.trigger import (
    ConfigInput,
    MailDetectInput,
    NotifyReq,
    QueueConfigInput,
    RemoveQueueTaskInput,
    TaskFutureExecInput,
    TaskFutureExecWithoutIdInput,
    TaskIdInput,
)
from astronverse.trigger.core.app_context import app_context
from astronverse.trigger.core.config import config
from astronverse.trigger.core.logger import logger
from astronverse.trigger.server.gateway_client import (
    execute_single_project,
    get_executor_status,
    send_stop_current,
)
from astronverse.trigger.tasks.base_task import AsyncImmediateTask, AsyncSchedulerTask
from astronverse.trigger.tasks.mail_task import MailTask
from astronverse.trigger.tasks.scheduled_task import ScheduledTask
from astronverse.websocket_client.ws import BaseMsg
from astronverse.websocket_client.ws_client import WsApp
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware


class WebSocketManager:
    """WebSocket 연결관리관리기기"""

    def __init__(self):
        self.ws_app = None
        self._thread = None
        self._stop_event = threading.Event()

    @staticmethod
    def default_log(msg, *args, **kwargs):
        """로그기록데이터"""
        logger.info("ws: {} {} {}".format(msg, args, kwargs))

    @staticmethod
    def remote_run(msg: BaseMsg):
        """관리실행메시지"""
        logger.info(msg)
        data = msg.data
        for i in range(3):
            if not get_executor_status():
                return execute_single_project(data)
            else:
                time.sleep(2)
        return {"code": "5001", "msg": "있음작업에서실행중", "data": None}

    @staticmethod
    def remote_stop_current(msg: BaseMsg):
        """관리중지메시지"""
        logger.info(msg)
        if get_executor_status():
            return send_stop_current()
        return {"code": "5001", "msg": "있음작업에서실행중", "data": None}

    def start(self):
        """시작 WebSocket 연결"""
        try:
            if self._thread and self._thread.is_alive():
                logger.info("WebSocket 관리관리기기완료에서실행")
                return
            self._stop_event.clear()
            self.ws_app = WsApp(
                url=f"ws://127.0.0.1:{config.GATEWAY_PORT}/api/rpa-openapi/ws",
                log=self.default_log,
                reconnect_max_time=-1,
            )
            self.ws_app.event("remote", "run", self.remote_run)
            self.ws_app.event("remote", "stop_current", self.remote_stop_current)

            self._thread = threading.Thread(target=self._ws_worker, daemon=True)
            self._thread.start()

            logger.info("WebSocket 관리관리기기시작성공")
        except Exception as e:
            logger.error(f"WebSocket 관리관리기기시작 실패: {e}")

    def _ws_worker(self):
        """WebSocket """
        try:
            self.ws_app.start()
        except Exception as e:
            logger.error(f"WebSocket 예외: {e}")

    def stop(self):
        """중지 WebSocket 연결"""
        self._stop_event.set()
        if self.ws_app:
            self.ws_app.close()
        if self._thread and self._thread.is_alive():
            self._thread.join(timeout=5)
        self._thread = None
        self.ws_app = None
        logger.info("WebSocket 관리관리기기완료중지")

    def close_connection(self):
        """닫기현재연결"""
        self.stop()

    def start_connection(self):
        """다시 생성연결(사용열기후재)"""
        self.start()


# 생성 WebSocket 관리관리기기
ws_manager = WebSocketManager()

app = FastAPI()
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])


@app.get("/task/health")
async def health():
    return "ok"


@app.on_event("startup")
async def init_trigger():
    await app_context.initialize()

    # 시작 WebSocket 관리관리기기
    ws_manager.start()

    async def periodic_to_native():
        """매300초호출일trigger.to_native()"""
        while True:
            logger.info("실행지정작업")
            if not app_context.trigger:
                await asyncio.sleep(1)
                continue
            await asyncio.sleep(10)  # 10s후, 프론트엔드전송
            app_context.trigger.to_native()
            await asyncio.sleep(5 * 60)  # 매5분일서버데이터, 필요예로완료

    sync_task = asyncio.create_task(periodic_to_native())


@app.post("/task/run")
async def run_tasks(task_info: TaskIdInput):
    """
    트리거작업

    :return:
    """
    logger.info("[/task/run] 열기 호출")

    # 작업여부존재함
    _t_info = task_info.model_dump()
    task_id = _t_info["task_id"]
    task = app_context.trigger.tasks.get(task_id)

    # 작업여부존재함
    if not task:
        return {
            "code": 201,
            "message": "지정작업찾을 수 없습니다, 요청대기일시간후재시도",
            "data": {},
        }
    if not isinstance(task, AsyncImmediateTask):
        return {"code": 201, "message": "해당 스케줄링 방식의 지정 작업을 찾을 수 없습니다", "data": {}}

    flag = await task.callback()
    if flag:
        return {"code": 200, "message": "작업 스케줄링 성공", "data": {}}
    else:
        return {
            "code": 201,
            "message": "작업 스케줄링 실패. 저장된 작업이 정상 실행 가능한지 확인하세요.",
            "data": {},
        }


@app.post("/task/notify")
async def notify(req: NotifyReq):
    """
    알림업데이트작업

    :return:
    """
    logger.info("notify: {}".format(req))

    if req.event == "login":
        ws_manager.start()
        tag = app_context.trigger.to_native()
    elif req.event == "exit":
        ws_manager.stop()
        tag = app_context.trigger.to_native(clear=True)
    elif req.event == "switch":
        ws_manager.stop()
        app_context.trigger.to_native(clear=True)
        tag = app_context.trigger.to_native()
    else:
        tag = app_context.trigger.to_native()
    if tag:
        return {"code": 200, "message": "작업업데이트성공", "data": {}}
    else:
        return {"code": 201, "message": "작업업데이트실패"}


@app.post("/mail/detect")
def mail_detect(task_info: MailDetectInput):
    """
    메일함감지

    :param task_info:
    :return:
    """
    logger.info("[/mail/detect] 열기 호출")
    _t_info = task_info.model_dump()
    amail = MailTask(task_id="book", **_t_info)

    flag = amail.connect()
    if flag:
        return {"code": 200, "message": "메일함연결성공", "data": {}}
    else:
        return {"code": 201, "message": "메일함연결 실패", "data": {}}


@app.post("/task/future")
def future(task_info: TaskFutureExecInput):
    """
    가져오기작업미완료N실행의시간
    :param task_info:
    :return:
    """
    try:
        _t_info = task_info.model_dump()
        task_id = _t_info["task_id"]
        times = _t_info["times"]

        # 작업여부존재함
        task = app_context.trigger.tasks.get(task_id)
        if not task:
            return {"code": 201, "message": "지정작업찾을 수 없습니다", "data": {}}
        if not isinstance(task, AsyncSchedulerTask):
            return {
                "code": 201,
                "message": "지정작업찾을 수 없습니다가져오기미완료실행시간의방식",
                "data": {},
            }

        l = task.get_future_time(times=times)

        return {"code": 200, "message": "가져오기성공", "data": {"next_exec_times": l}}

    except Exception as e:
        return {"code": 201, "message": "가져오기실패", "data": {"next_exec_times": []}}


@app.post("/task/future_with_no_create")
def future_with_create(task_info: TaskFutureExecWithoutIdInput):
    """
    가져오기작업미완료N실행의시간(에서있음생성작업의아래)
    :param task_info:
    :return:
    """

    try:
        _t_info = task_info.model_dump()
        times = _t_info["times"]

        task = ScheduledTask(**_t_info)
        l = AsyncSchedulerTask.get_future_time_without_create(task.to_trigger(), times)
        return {"code": 200, "message": "가져오기성공", "data": {"next_exec_times": l}}
    except Exception:
        return {"code": 201, "message": "가져오기실패", "data": {"next_exec_times": []}}


@app.get("/task/queue/status")
async def get_queue_status(pageNo: int = 1, pageSize: int = 10, name: str = None, taskType: str = None):
    """
    가져오기현재작업큐상태

    :param pageNo: 코드, 에서1열기 
    :param pageSize: 매크기
    :param name: 작업이름, 사용검색필터링
    :param taskType: 작업유형, 사용검색필터링
    :return: 분후의작업큐상태
    """
    try:
        filtered_tasks = []
        total = 0
        start_idx = (pageNo - 1) * pageSize
        end_idx = start_idx + pageSize

        # 일완료필터링및분
        for task in app_context.task_queue_monitor:
            # 조회여부시간 초과
            if app_context.task_queue_mgr.is_task_timeout(task):
                app_context.task_queue_monitor.remove(task)
                logger.info(
                    f"작업대기시간초과경과{app_context.queue_config['max_wait_minutes']}분, 완료제거: {task.get('trigger_id')}"
                )
                continue

            # 사용검색필터링
            if name and name.lower() not in str(task.get("trigger_name", "")).lower():
                continue
            if taskType and taskType.lower() != str(task.get("task_type", "")).lower():
                continue

            # 계획데이터
            total += 1

            # 현재의데이터
            if start_idx <= total - 1 < end_idx:
                # 사용수정기존작업객체
                task_copy = copy.deepcopy(task)
                task_copy["status_index"] = total
                filtered_tasks.append(task_copy)

        return {
            "code": 200,
            "message": "가져오기성공",
            "data": {
                "max_size": app_context.queue_config["max_length"],
                "monitor_queue_size": total,
                "current_tasks": filtered_tasks,
                "pagination": {
                    "pageNo": pageNo,
                    "pageSize": pageSize,
                    "total": total,
                    "totalPages": (total + pageSize - 1) // pageSize,
                },
            },
        }
    except Exception as e:
        logger.error(f"가져오기큐상태실패: {e}")
        return {"code": 500, "message": f"가져오기큐상태실패: {str(e)}", "data": None}


@app.post("/task/queue/remove")
async def remove_queue_task(task_info: RemoveQueueTaskInput):
    """
    에서큐중삭제지정작업

    :param task_info: 패키지unique_id목록의요청 
    :return:
    """
    try:
        removed_count = 0
        # 삭제할의unique_id목록
        for unique_id in task_info.unique_id:
            # 에서큐중조회의작업
            for task in list(app_context.task_queue_monitor):
                if task.get("unique_id") == unique_id:
                    app_context.task_queue_monitor.remove(task)
                    removed_count += 1
                    logger.info(f"에서큐중삭제작업: {unique_id}")
                    break

        if removed_count > 0:
            return {
                "code": 200,
                "message": f"성공삭제{removed_count}개작업",
                "data": {"removed_count": removed_count},
            }
        else:
            return {
                "code": 404,
                "message": "찾을 수 없는 지정의작업",
                "data": {"removed_count": 0},
            }
    except Exception as e:
        logger.error(f"삭제큐작업실패: {e}")
        return {"code": 500, "message": f"삭제실패: {str(e)}", "data": None}


@app.post("/task/queue/config")
async def update_queue_config(config: QueueConfigInput):
    """
    업데이트큐매칭

    :param config: 큐매칭정보
    :return:
    """
    try:
        # 결과가대대기시간발송변수, 업데이트모든작업의경과시간
        if config.max_wait_minutes != app_context.queue_config["max_wait_minutes"]:
            for task in app_context.task_queue_monitor:
                enqueue_time = task.get("enqueue_time", "")
                if enqueue_time:
                    # 다시 계획경과시간
                    expire_time = time.strftime(
                        "%Y-%m-%d %H:%M:%S",
                        time.localtime(
                            time.mktime(time.strptime(enqueue_time, "%Y-%m-%d %H:%M:%S")) + config.max_wait_minutes * 60
                        ),
                    )
                    task["expire_time"] = expire_time

        # 결과가큐대길이정도변수소완료, 삭제초과출력제한의작업
        if config.max_length < app_context.queue_config["max_length"]:
            current_size = len(app_context.task_queue_monitor)
            if current_size > config.max_length:
                # 삭제초과출력제한의작업(에서큐삭제, 보관의작업)
                tasks_to_remove = current_size - config.max_length
                for _ in range(tasks_to_remove):
                    if app_context.task_queue_monitor:
                        removed_task = app_context.task_queue_monitor.pop()
                        logger.info(f"큐길이정도초과제한, 삭제작업: {removed_task.get('unique_id')}")

        if config.deduplicate and not app_context.queue_config["deduplicate"]:
            # 열기시작재, 필요다시 조회모든작업여부재복사
            trigger_ids = set()
            for task in app_context.task_queue_monitor:
                if task.get("trigger_id") in trigger_ids:
                    app_context.task_queue_monitor.remove(task)
                    logger.info(f"작업완료존재함, 건너뛰기: {task.get('unique_id')}")
                    continue
                trigger_ids.add(task.get("trigger_id"))

        app_context.queue_config.update(config.model_dump())
        logger.info(f"업데이트큐매칭: {app_context.queue_config}")
        return {
            "code": 200,
            "message": "매칭업데이트성공",
            "data": app_context.queue_config,
        }
    except Exception as e:
        logger.error(f"업데이트큐매칭실패: {e}")
        return {"code": 500, "message": f"매칭업데이트실패: {str(e)}", "data": None}


@app.get("/task/queue/config")
async def get_queue_config():
    """
    가져오기현재큐매칭
    """
    return {"code": 200, "message": "가져오기성공", "data": app_context.queue_config}


@app.post("/config/update")
async def update_config(input_config: ConfigInput):
    """
    업데이트매칭
    """
    logger.info(input_config.terminal_mode)
    app_context.update_config(input_config.terminal_mode)
    return {"code": 200, "message": "매칭업데이트성공", "data": config}
