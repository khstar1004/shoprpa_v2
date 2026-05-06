import threading
from collections import deque
from typing import Optional

from astronverse.trigger.core.config import config
from astronverse.trigger.core.logger import logger
from astronverse.trigger.core.queue_manager import TaskQueueManager
from astronverse.trigger.terminal import Terminal
from astronverse.trigger.trigger import Trigger


class AppContext:
    """사용프로그램위아래문서관리관리기기, 사용관리관리전체영역상태및컴포넌트"""

    def __init__(self):
        # 컴포넌트
        self.trigger: Optional[Trigger] = None
        self.terminal: Optional[Terminal] = None
        self.task_queue_mgr: Optional[TaskQueueManager] = None

        # 큐매칭
        self.queue_config = {
            "max_length": 500,  # 대큐길이정도
            "max_wait_minutes": 30,  # 대대기시간(분)
            "deduplicate": False,  # 여부재
        }

        # 사용의큐, 저장현재정상에서정렬팀의작업정보
        self.task_queue_monitor = deque(maxlen=1000)  # 대길이정도로1000, 중지메모리출력

        # 관리관리
        self._threads = []

    async def initialize(self):
        """사용프로그램위아래문서"""
        logger.info("열기 사용프로그램위아래문서")

        # 트리거기기
        await self._init_trigger()

        # 작업큐관리관리기기
        self._init_task_queue_manager()

        # 단말(결과가필요)
        if config.TERMINAL_MODE:
            await self._init_terminal()

        logger.info("사용프로그램위아래문서완료")

    async def _init_trigger(self):
        """트리거기기"""
        self.trigger = Trigger()  # 일지정할에서예외시작Trigger, New일개AsyncIOScheduler()
        logger.info("trigger완료")

    def _init_task_queue_manager(self):
        """작업큐관리관리기기"""
        if not self.trigger:
            raise RuntimeError("trigger에서task_queue_manager전")

        self.task_queue_mgr = TaskQueueManager(
            self.task_queue_monitor,
            self.trigger.queue,
            self,  # 입력 self (app_context)
        )
        self.task_queue_mgr.set_trigger(self.trigger)

        # 시작작업큐관리관리
        fetch_thread = threading.Thread(target=self.task_queue_mgr.fetch_tasks, daemon=True)
        process_thread = threading.Thread(target=self.task_queue_mgr.process_tasks, daemon=True)

        fetch_thread.start()
        process_thread.start()

        self._threads.extend([fetch_thread, process_thread])
        logger.info("작업큐관리관리기기완료")

    async def _init_terminal(self):
        """단말(에서TERMINAL_MODE아래)"""
        if not self.trigger:
            raise RuntimeError("trigger에서terminal전")

        self.trigger.delete_all_tasks()

        if not self.terminal:
            self.terminal = Terminal(self.task_queue_monitor, self.trigger.queue, self.trigger.scheduler)
            logger.info("terminal완료")

        self.terminal.start_poll()  # 시작terminal문의

    def update_config(self, terminal_mode: bool):
        """업데이트매칭"""
        config.TERMINAL_MODE = terminal_mode

        self.trigger.delete_all_tasks()

        if self.terminal:
            self.terminal.stop_poll()  # 중지terminal문의

        if config.TERMINAL_MODE:
            if not self.terminal:
                self.terminal = Terminal(self.task_queue_monitor, self.trigger.queue, self.trigger.scheduler)
                logger.info("terminal다시 완료")
            self.terminal.start_poll()
        else:
            self.trigger.to_native()

    def get_active_threads(self):
        """가져오기 목록"""
        return [t for t in self._threads if t.is_alive()]


# 전체영역사용프로그램위아래문서
app_context = AppContext()