import copy
import time
import uuid
from collections import deque

from astronverse.trigger.core.config import config
from astronverse.trigger.core.logger import logger
from astronverse.trigger.server.gateway_client import execute_multiple_projects


class TaskQueueManager:
    def __init__(self, task_queue_monitor: deque, trigger_queue, app_context):
        self.task_queue_monitor = task_queue_monitor
        self.trigger_queue = trigger_queue
        self.app_context = app_context  # 직선연결사용 app_context
        self.trigger = None

    def set_trigger(self, trigger):
        """trigger"""
        self.trigger = trigger

    @property
    def queue_config(self):
        """가져오기큐매칭"""
        return self.app_context.queue_config

    @staticmethod
    def is_task_timeout(task):
        """조회작업여부시간 초과"""
        try:
            current_time = time.strftime("%Y-%m-%d %H:%M:%S")
            expire_time = task.get("expire_time", "")
            # 여부시간 초과
            return time.mktime(time.strptime(current_time, "%Y-%m-%d %H:%M:%S")) > time.mktime(
                time.strptime(expire_time, "%Y-%m-%d %H:%M:%S")
            )
        except Exception as e:
            logger.error(f"조회작업여부시간 초과실패: {e}")
            return False

    def fetch_tasks(self):
        """가져오기작업추가까지큐"""
        while True:
            if not self.trigger:
                time.sleep(1)
                continue
            task_info = self.trigger.queue.get()  # 대기직선까지있음로중지
            if not task_info:
                time.sleep(1)
                continue
            logger.info(f"수신까지트리거작업, task_info: {task_info}")

            if len(self.task_queue_monitor) >= self.queue_config["max_length"]:
                logger.warning(f"작업큐완료가득, 작업완료: {task_info.get('trigger_id')}")
                continue

            # 사용수정기존작업객체
            task_copy = copy.deepcopy(task_info)

            if not config.TERMINAL_MODE:
                # 조회여부필요재
                if self.queue_config["deduplicate"]:
                    # 조회여부완료저장된 trigger_id의작업
                    for task in self.task_queue_monitor:
                        if task.get("trigger_id") == task_copy.get("trigger_id"):
                            logger.info(f"작업완료존재함, 건너뛰기: {task_copy.get('trigger_id')}")
                            continue

            # 를작업정보추가까지큐, 기록입력팀시간및경과시간
            current_time = time.strftime("%Y-%m-%d %H:%M:%S")
            task_copy["enqueue_time"] = current_time
            # 계획경과시간
            expire_time = time.strftime(
                "%Y-%m-%d %H:%M:%S",
                time.localtime(
                    time.mktime(time.strptime(current_time, "%Y-%m-%d %H:%M:%S"))
                    + self.queue_config["max_wait_minutes"] * 60
                ),
            )
            task_copy["expire_time"] = expire_time
            # 추가일ID
            task_copy["unique_id"] = str(uuid.uuid4())
            self.task_queue_monitor.append(task_copy)

    def process_tasks(self):
        """관리큐중의작업"""
        while True:
            if not self.task_queue_monitor:
                time.sleep(1)
                continue

            task_info = self.task_queue_monitor[0]  # 조회아니오제거

            # 조회작업여부예현재mode의
            if task_info.get("mode") == "DISPATCH" and not config.TERMINAL_MODE:
                self.task_queue_monitor.popleft()  # 아래발송전제거일개요소
                logger.info(f"작업방식로본예약 작업, 완료제거스케줄링작업: {task_info.get('trigger_id')}")
                continue
            if task_info.get("mode") != "DISPATCH" and config.TERMINAL_MODE:
                self.task_queue_monitor.popleft()  # 아래발송전제거일개요소
                logger.info(f"작업방식로스케줄링작업, 완료제거본예약 작업: {task_info.get('trigger_id')}")
                continue

            # 조회작업여부시간 초과
            if self.is_task_timeout(task_info):
                self.task_queue_monitor.popleft()  # 제거시간 초과작업
                logger.info(
                    f"작업대기시간초과경과{self.queue_config['max_wait_minutes']}분, 완료제거: {task_info.get('trigger_id')}"
                )
                continue

            self.task_queue_monitor.popleft()  # 아래발송전제거일개요소
            i = 0
            while True:
                success_flag = execute_multiple_projects(task_info)  # 스케줄링스케줄링기기
                if not success_flag:
                    if i < 10:
                        i += 1
                    time.sleep(6 * i)  # 대기6*i초후, 다시 아래발송[테이블아래발송실패]
                    logger.info(f"다시 아래발송, task_info: {task_info}")
                    continue
                # 작업실행완료후, 에서큐중제거
                break