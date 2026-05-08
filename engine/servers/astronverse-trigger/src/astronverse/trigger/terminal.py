import asyncio
import threading
import time

from apscheduler.schedulers.asyncio import AsyncIOScheduler
from astronverse.trigger.core.logger import logger
from astronverse.trigger.server.gateway_client import (
    send_stop_list,
    terminal_list_task,
    terminal_poll_update,
)
from astronverse.trigger.tasks.base_task import AsyncImmediateTask, AsyncSchedulerTask


class Terminal:
    def __init__(self, global_deque, global_queue, global_scheduler):
        self.task_queue_monitor = global_deque
        self.queue = global_queue
        self.scheduler: AsyncIOScheduler = global_scheduler
        self.tasks = {}

        # terminal문의제어
        self.poll_thread = None
        self.poll_stop_event = threading.Event()

    def terminal_poll_worker(self):
        """
        terminal문의
        """
        logger.info("[terminal_poll_worker]terminal문의시작")
        self.update_task_list()
        count = 1
        while not self.poll_stop_event.is_set():
            try:
                if count % 5 == 0:
                    flag = terminal_poll_update()
                    if flag:
                        logger.info("[terminal_poll_worker]terminal문의결과: {}", flag)
                    if flag or count % 100 == 0:
                        # 관리terminal업데이트
                        self.update_task_list()
            except Exception as e:
                logger.error(f"[terminal_poll_worker]terminal문의예외: {e}")
            finally:
                count += 1
                if count > 100:
                    count = 1
                time.sleep(1)

        logger.info("[terminal_poll_worker]terminal문의중지")

    def start_poll(self):
        """
        시작terminal문의
        """
        if self.poll_thread and self.poll_thread.is_alive():
            logger.warning("[start_poll]terminal문의완료에서실행")
            return

        self.poll_stop_event.clear()
        self.poll_thread = threading.Thread(target=self.terminal_poll_worker, daemon=True)
        self.poll_thread.start()
        logger.info("[start_poll]terminal문의시작성공")

    def stop_poll(self):
        """
        중지terminal문의
        """
        if not self.poll_thread or not self.poll_thread.is_alive():
            logger.warning("[stop_poll]terminal문의미완료실행")
            return

        logger.info("[stop_poll]정상에서중지terminal문의")
        self.poll_stop_event.set()
        self.poll_thread.join(timeout=15)  # 대기다중5초
        if self.poll_thread.is_alive():
            logger.warning("[stop_poll]terminal문의미완료가능에서15초내부중지")
        else:
            logger.info("[stop_poll]terminal문의완료중지")

        self.delete_all_tasks()

    def update_task_list(self):
        # 요청 전체량작업목록
        new_task_list, retry_task_list, stop_task_list = terminal_list_task()
        new_task_ids = [task["trigger_id"] for task in new_task_list if task["task_type"] != "manual"]
        # 및본작업의, 본작업, 단말작업

        manual_new_task_list = [task for task in new_task_list if task["task_type"] == "manual"]

        non_manual_new_task_list = [task for task in new_task_list if task["task_type"] != "manual"]
        logger.info(f"[update_task_list]new_task_list전체량작업목록: {non_manual_new_task_list} ")

        intersection = [task for task in non_manual_new_task_list if task["trigger_id"] in self.tasks.keys()]
        local_tasks_unique = [task_id for task_id, task in self.tasks.items() if task_id not in new_task_ids]
        cloud_tasks_unique = [task for task in non_manual_new_task_list if task["trigger_id"] not in self.tasks.keys()]

        logger.info(f"[update_task_list]intersection본, 단말모듈분예: {intersection} ")
        logger.info(f"[update_task_list]local_tasks_unique본있음작업: {local_tasks_unique} ")
        logger.info(f"[update_task_list]cloud_tasks_unique단말있음작업: {cloud_tasks_unique} ")

        # 본작업값, 직선연결삭제
        for task_id in local_tasks_unique:
            logger.info(f"[update_task_list]본삭제작업: {task_id}")
            self.delete_task(task_id)

        # 단말작업값, 직선연결추가
        for task in cloud_tasks_unique:
            logger.info(f"[update_task_list]단말추가작업: {task}")
            self.add_task(**task)

        # 작업직선연결추가
        for task in manual_new_task_list:
            logger.info(f"[update_task_list]추가작업: {task}")
            self.add_task(**task)

        # 본예약 작업, 행업데이트관리
        # intersection의예새의
        for new_task in intersection:
            task = self.tasks[new_task["trigger_id"]]

            if task.kwargs == new_task:
                logger.info("[update_task_list]단말, 본작업ID, 매개변수 , 아니오행업데이트. ")
                continue
            else:
                logger.info(f"[update_task_list]단말, 본작업ID, 매개변수아니오, 행업데이트: {new_task} ")
                self.update_task(**new_task)

        # 관리retry및stop작업
        for task in retry_task_list:
            task["task_type"] = "manual"
            logger.info(f"[retry_task] 재시도작업: {task}")
            self.add_task(**task)

        if stop_task_list:
            for task in stop_task_list:
                logger.info(f"[stop_task] 중지작업: {task}")
                send_stop_list(task.get("trigger_id", None))

    def add_task(
        self,
        trigger_id: str,
        trigger_name: str,
        task_type: str,
        callback_project_ids: list,
        exceptional: str,
        timeout: int,
        queue_enable: bool,
        screen_record_enable: bool,
        open_virtual_desk: bool,
        **kwargs,
    ):
        """
        추가작업

        Args:
            trigger_id: 작업id
            trigger_name: 작업이름
            task_type: 작업유형
            callback_project_ids: 스케줄링봇정보
            exceptional: 예외관리방식
            timeout: 시간 초과시간
            queue_enable: 여부사용큐
            screen_record_enable: 여부사용기록
            open_virtual_desk: 여부사용
            kwargs: 생성task의닫기매개변수
        """

        task = self.get_task(trigger_id)
        if task and task_type != "manual":
            return False

        if task_type == "schedule":
            task = AsyncSchedulerTask(
                scheduler=self.scheduler,
                trigger_id=trigger_id,
                trigger_name=trigger_name,
                task_type=task_type,
                queue_enable=queue_enable,
                q=self.queue,
                callback_project_ids=callback_project_ids,
                exceptional=exceptional,
                timeout=timeout,
                screen_record_enable=screen_record_enable,
                open_virtual_desk=open_virtual_desk,
                **kwargs,
            )
            task.create()
            self.tasks[trigger_id] = task

        elif task_type == "manual":
            task = AsyncImmediateTask(
                trigger_id=trigger_id,
                trigger_name=trigger_name,
                task_type=task_type,
                queue_enable=queue_enable,
                q=self.queue,
                callback_project_ids=callback_project_ids,
                exceptional=exceptional,
                timeout=timeout,
                screen_record_enable=screen_record_enable,
                open_virtual_desk=open_virtual_desk,
                **kwargs,
            )
            flag = asyncio.run(task.callback())
            if flag:
                logger.info("작업 스케줄링 성공")
            else:
                logger.info("작업 스케줄링 실패. 저장된 작업이 정상 실행 가능한지 확인하세요.")

            del task
        else:
            raise Exception("지원하지 않는 작업 유형입니다: {}".format(task_type))
        return True

    def get_task(self, task_id: str):
        """
        가져오기작업

        Args:
            task_id: 작업ID

        Returns:
            작업객체또는None
        """
        return self.tasks.get(task_id)

    def delete_task(self, task_id: str):
        """
        삭제작업

        Args:
            task_id: 작업ID

        Returns:
            삭제 여부성공
        """
        task = self.get_task(task_id)
        if not task:
            logger.warning(f"[delete_task]작업찾을 수 없습니다: {task_id}")
            return False

        try:
            if hasattr(task, "delete"):
                task.delete()
            del self.tasks[task_id]
            logger.info(f"[delete_task]작업삭제성공: {task_id}")
            return True
        except Exception as e:
            logger.error(f"[delete_task]삭제작업예외: {task_id}, 오류: {e}")
            return False

    def update_task(self, **kwargs):
        """
        업데이트작업

        Args:
            **kwargs: 작업매개변수

        Returns:
            여부업데이트성공
        """
        task_id = kwargs.get("trigger_id")
        if not task_id:
            logger.error("[update_task]적음taskId매개변수")
            return False

        try:
            # 삭제작업
            self.delete_task(task_id)
            # 추가새작업
            return self.add_task(**kwargs)
        except Exception as e:
            logger.error(f"[update_task]업데이트작업예외: {task_id}, 오류: {e}")
            return False

    def delete_all_tasks(self):
        """
        삭제모든작업
        """
        logger.info("[delete_all_tasks]: {}".format(self.tasks.keys()))
        task_keys = list(self.tasks.keys())
        for task_key in task_keys:
            logger.info("[delete_all_tasks]삭제작업: {}".format(task_key))
            try:
                self.delete_task(task_key)
            except Exception as e:
                logger.error("[delete_all_tasks]삭제작업실패: {}".format(e))
