import asyncio
from datetime import datetime
from queue import Queue
from typing import Union

from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.cron import CronTrigger
from apscheduler.triggers.interval import IntervalTrigger
from astronverse.trigger import CONVERT_COLUMN
from astronverse.trigger.server.gateway_client import get_executor_status, send_msg
from astronverse.trigger.tasks.file_task import FileTask
from astronverse.trigger.tasks.hotkey_task import HotKeyTask
from astronverse.trigger.tasks.mail_task import MailTask
from astronverse.trigger.tasks.scheduled_task import ScheduledTask
from tzlocal import get_localzone


class Task:
    def __init__(
        self,
        trigger_id: str,
        trigger_name: str,
        task_type: str,
        enable: bool,
        queue_enable: bool,
        q: Queue,
        callback_project_ids: list,
        exceptional: str,
        timeout: int,
        mode: str,
        screen_record_enable: bool = False,
        open_virtual_desk: bool = False,
        **kwargs,
    ):
        """
        작업유형

        Args:
            trigger_id: `str`, 작업id
            trigger_name: `str`, 작업이름
            task_type: `str`, 작업유형, 지원`scheduled`, `mail`, `hotkey`, `file`
            enable: `bool`, 시작상태
            queue_enable: `bool`, 여부사용큐
            queue: `asyncio.Queue`, 큐(전체영역)
            callback_project_ids: `List`, 돌아가기조정사용의id목록
            exceptional: str, 예외관리방식,  지원`skip`또는`stop`
            timeout: int, 시간 초과시간,  9999
            task_params: 생성task의닫기매개변수, 조회유형

        """
        self.trigger_id: str = trigger_id
        self.trigger_name: str = trigger_name
        self.task_type: str = task_type
        self.enable: bool = enable
        self.queue_enable: bool = queue_enable
        self.queue: Queue = q
        self.exceptional: str = exceptional
        self.timeout: int = timeout
        self.callback_project_ids: list = callback_project_ids
        self.trigger_json: dict = kwargs
        self.time = datetime.now(tz=get_localzone())
        self.screen_record_enable = screen_record_enable or False
        self.open_virtual_desk = open_virtual_desk or False

        # 추가mode필드, 사용분아래발송(DISPATCH)
        self.mode: str = mode
        self.kwargs: dict = {}

        for col_value in CONVERT_COLUMN.values():
            try:
                value = getattr(self, col_value)
            except AttributeError:
                continue
            if isinstance(value, dict):
                # 있음패키지
                self.kwargs.update(value)
            else:
                self.kwargs[col_value] = value

        self.kwargs.update(kwargs)

        self.minor_task: Union[ScheduledTask, MailTask, FileTask, HotKeyTask] = self._init_task_model(
            self.task_type, **kwargs
        )

    def _init_task_model(self, task_type: str, **kwargs) -> Union[ScheduledTask, MailTask, FileTask, HotKeyTask]:
        """
        작업유형

        Args:
            - task_type: `str`, 작업유형, 지원`scheduled`, `mail`, `hotkey`, `files`

        :param task_type:
        :param kwargs:
        :return:
        """
        if task_type == "schedule":
            m = ScheduledTask(**kwargs)
        elif task_type == "mail":
            m = MailTask(self.trigger_id, **kwargs)
        elif task_type == "file":
            m = FileTask(**kwargs)
        elif task_type == "hotKey":
            m = HotKeyTask(**kwargs)
        elif task_type == "manual":
            m = None
        else:
            raise NotImplementedError
        return m

    def __dict__(self):
        return self.kwargs


class AsyncSchedulerTask(Task):
    def __init__(self, scheduler: AsyncIOScheduler, **kwargs):
        self.trigger: Union[CronTrigger, IntervalTrigger] = None
        self.scheduler = scheduler
        super().__init__(**kwargs)

    async def callback(self, *args) -> bool:
        """돌아가기조정, 행작업트리거"""

        # minor_task예tasks유형의
        flag = await self.minor_task.callback()
        if not flag:
            return False

        status = get_executor_status()  # 스케줄링기기의실행상태
        # 일파일예아니오추가큐의
        # 이파일예큐가능사용
        if (not self.queue_enable and not status) or self.queue_enable:
            self.queue.put(self.__dict__())
            return True
        else:
            send_msg(
                {
                    "msg": "작업사용, 현재존재함정상에서실행의작업, 해당작업미완료열기시작정렬팀",
                    "type": "tip",
                }
            )
            return False

    def create(self):
        """생성작업"""
        self.trigger = self.minor_task.to_trigger()
        self.job = self.scheduler.add_job(
            self.callback,  # 예본의callback, 사용및tasks의유형의callback행통신
            trigger=self.trigger,
            id=self.trigger_id,
        )

        if not self.enable:
            self.pause()

    def delete(self):
        """삭제작업"""
        task = self.scheduler.get_job(self.trigger_id)
        if task:
            self.scheduler.remove_job(self.trigger_id)

    def pause(self):
        """중지작업"""
        self.job.pause()

    def resume(self):
        """복사작업"""
        self.job.resume()

    def get_future_time(self, times: int = 3):
        """가져오기미완료N실행시간"""
        if not self.trigger:
            return []

        executions = []
        next_run = self.time
        # 가져오기미완료N실행시간
        for _ in range(times):
            next_run = self.trigger.get_next_fire_time(next_run, next_run)
            if next_run is None:
                break
            formatted_time = next_run.strftime("%Y-%m-%d %H:%M:%S")
            executions.append(formatted_time)

        return executions

    @classmethod
    def get_future_time_without_create(cls, trigger=None, times: int = 3):
        """가져오기미완료N실행시간(미완료생성작업아래)"""
        if not trigger:
            return []

        executions = []
        next_run = datetime.now(tz=get_localzone())
        # 가져오기미완료N실행시간
        for _ in range(times):
            next_run = trigger.get_next_fire_time(next_run, next_run)
            if next_run is None:
                break
            formatted_time = next_run.strftime("%Y-%m-%d %H:%M:%S")
            executions.append(formatted_time)

        return executions


class AsyncOneCallTask(Task):
    def __init__(self, **kwargs):
        self.task: asyncio.Task = None
        self._run_event = asyncio.Event()
        super().__init__(**kwargs)

    async def callback(self, *args):
        """돌아가기조정, 행작업트리거"""
        q = asyncio.Queue()
        # 열기 실행돌아가기조정

        self.task = asyncio.create_task(self.minor_task.callback(q, args[0]))

        while True:
            flag = await q.get()
            if not flag:
                continue

            status = get_executor_status()
            # 열기정렬팀빈 및 열기완료정렬팀
            if (not self.queue_enable and not status) or self.queue_enable:
                self.queue.put(self.__dict__())
            else:
                send_msg(
                    {
                        "msg": "작업사용, 현재존재함정상에서실행의작업, 해당작업미완료열기시작정렬팀",
                        "type": "tip",
                    }
                )
            continue

    def create(self):
        """생성작업"""
        asyncio.create_task(self.callback(self._run_event))

        if not self.enable:
            self.pause()

    def delete(self):
        """삭제작업"""
        self.task.cancel()
        if hasattr(
            self.minor_task, "force_end_callback"
        ):  # 작업불가통신경과Asyncio.task.cancel직선연결가져오기 , 으로필요추가force_end_callback에서내부모듈가져오기 
            self.minor_task.force_end_callback()

    def pause(self):
        """중지작업"""
        self._run_event.set()

    def resume(self):
        """복사작업"""
        self._run_event.clear()


class AsyncImmediateTask(Task):
    """예일개시데이터의스케줄링유형"""

    def __init__(self, **kwargs):
        super().__init__(**kwargs)

    async def callback(self, *args):
        """
        해당돌아가기조정방법법예가져오기 실행객체의실행관리데이터

        :return:
        """
        try:
            from astronverse.trigger.core.app_context import app_context

            status = get_executor_status()
            # 감지큐여부존재함대기정렬팀, 존재함이면아니오입력큐
            # 스케줄링방식, 전체정렬팀
            if (self.enable and len(app_context.task_queue_monitor) == 0 and not status) or (self.mode == "DISPATCH"):
                self.queue.put(self.__dict__())
                return True
            return False
        except Exception as e:
            return False