from queue import Queue
from typing import Union

from apscheduler.schedulers.asyncio import AsyncIOScheduler
from astronverse.trigger.core.config import config
from astronverse.trigger.core.logger import logger
from astronverse.trigger.server.gateway_client import list_trigger
from astronverse.trigger.tasks.base_task import (
    AsyncImmediateTask,
    AsyncOneCallTask,
    AsyncSchedulerTask,
)


class Trigger:
    def __init__(self):
        self.tasks: dict[str, Union[AsyncSchedulerTask, AsyncOneCallTask, AsyncImmediateTask]] = {}
        self.queue: Queue = Queue(maxsize=1000)
        self.scheduler = AsyncIOScheduler()
        self.scheduler.start()

    def to_native(self, clear: bool = False):
        """에서단말까지본시작"""

        """
        1. 합치기있음robot_id있음의모듈분, 에서분출력있음의모듈분(분예본목록 의, 단말아래의)
        2. 본목록 있음의모듈분, 삭제
        3. 단말아래있음의모듈분, 추가
        4. 있음의모듈분: 
          4.1 값여부대기, 대기아니오변수
          4.2 아니오대기이면재시작업데이트
        """

        try:
            if config.TERMINAL_MODE:
                # 결과가예스케줄링방식, 경과업데이트
                return True

            # 에서단말가져오기데이터정보
            if not clear:
                triggers = list_trigger()
                if triggers is None:
                    # 결과가서버비어 있습니다직선연결결과
                    return True
            else:
                triggers = {}

            trigger_ids = triggers.keys()
            # 및본작업의, 본작업, 단말작업
            intersection = [triggers.get(task_id) for task_id, task in self.tasks.items() if task_id in trigger_ids]
            local_tasks_unique = [task_id for task_id, task in self.tasks.items() if task_id not in trigger_ids]
            cloud_tasks_unique = [task for task_id, task in triggers.items() if task_id not in self.tasks.keys()]
            logger.info(f"[to_native]intersection본, 단말모듈분예: {intersection} ")
            logger.info(f"[to_native]local_tasks_unique본있음작업: {local_tasks_unique} ")
            logger.info(f"[to_native]cloud_tasks_unique단말있음작업: {cloud_tasks_unique} ")

            # 본작업값, 직선연결삭제
            for task_id in local_tasks_unique:
                logger.info("[to_native]본삭제작업: {}".format(task_id))
                self.delete_task(task_id)
            # 단말작업값, 직선연결추가
            for trigger in cloud_tasks_unique:
                logger.info("[to_native]단말추가작업: {}".format(trigger))
                self.add_task(**trigger)
            # 본예약 작업, 행업데이트관리
            for trigger in intersection:
                task = self.tasks[trigger["trigger_id"]]

                if task.kwargs == trigger:
                    logger.info("[to_native]단말, 본작업ID, 매개변수 , 아니오행업데이트. ")
                    continue
                else:
                    logger.info("[to_native]단말, 본작업ID, 매개변수아니오, 행업데이트")
                    logger.info(f"[to_native]기존있음task매개변수: {task.kwargs}, 새매개변수: {trigger}")
                    self.update_task(**trigger)
            return True
        except Exception as e:
            import traceback

            logger.error("트리거기기실패: {} {}".format(e, traceback.extract_stack()))
            return False

    def add_task(
        self,
        trigger_id: str,
        trigger_name: str,
        task_type: str,
        queue_enable: bool,
        callback_project_ids: list,
        exceptional: str,
        timeout: int,
        **kwargs,
    ):
        """
        추가작업

        Args:
            trigger_id: 작업id
            trigger_name: 트리거기기이름
            task_type: 작업유형, 지원`scheduled`, `mail`, `hotkey`, `files`
            queue_enable: bool, 여부사용큐
            callback_project_ids: 돌아가기조정사용의id순서열
            exceptional: str, 예외관리방식,  지원`skip`또는`stop`
            timeout: int, 시간 초과시간,  9999
            kwargs: 생성task의닫기매개변수
        """
        # 예약작업및메일작업사용개유형생성작업
        task = self.get_task(trigger_id)
        if task:
            return False

        # 트리거유형기존관리예: 
        # 1. schedule및mail가능으로사용예약문의의방식스케줄링
        # 2. file및hotKey필요시작일
        # 3. manual트리거

        if task_type == "schedule" or task_type == "mail":
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
                **kwargs,
            )
            task.create()

        elif task_type == "file" or task_type == "hotKey":
            task = AsyncOneCallTask(
                trigger_id=trigger_id,
                trigger_name=trigger_name,
                task_type=task_type,
                queue_enable=queue_enable,
                q=self.queue,
                callback_project_ids=callback_project_ids,
                exceptional=exceptional,
                timeout=timeout,
                **kwargs,
            )
            task.create()

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
                **kwargs,
            )

        else:
            raise Exception("지원하지 않음의작업유형")

        self.tasks[trigger_id] = task
        return True

    def update_task(
        self,
        trigger_id: str,
        task_type: str,
        queue_enable: bool,
        callback_project_ids: list,
        **kwargs,
    ):
        """
        업데이트작업

        Args:
            trigger_id: 작업id
            task_type: 작업유형, 지원`scheduled`, `mail`, `hotkey`, `files`
            queue_enable: bool, 여부사용큐
            callback_project_ids: 돌아가기조정사용의id
            kwargs: 업데이트task의닫기매개변수
        """
        task = self.get_task(trigger_id)
        if not task:
            return False

        # 제거작업스케줄링
        self.delete_task(trigger_id)

        self.add_task(
            trigger_id=trigger_id,
            task_type=task_type,
            queue_enable=queue_enable,
            callback_project_ids=callback_project_ids,
            **kwargs,
        )

        return True

    def delete_task(self, trigger_id: str) -> bool:
        """
        삭제작업

        :param trigger_id: `str`, 작업id
        :return:
        """
        logger.info(f"[delete_task]열기 삭제작업: {trigger_id}")

        task = self.get_task(trigger_id)
        if not task:
            logger.warning(f"[delete_task]작업찾을 수 없습니다, 삭제실패: {trigger_id}")
            return False

        try:
            # 기록작업정보
            task_type = getattr(task, "task_type", "unknown")
            task_name = getattr(task, "trigger_name", "unknown")
            logger.info(f"[delete_task]까지작업: ID={trigger_id}, 유형={task_type}, 이름={task_name}")

            # 작업 실행삭제
            if hasattr(task, "delete"):
                logger.info(f"[delete_task]작업 실행삭제방법법: {trigger_id}")
                task.delete()
            else:
                logger.warning(f"[delete_task]작업있음delete방법법: {trigger_id}")

            # 에서작업딕셔너리중제거
            del self.tasks[trigger_id]
            logger.info(f"[delete_task]작업삭제성공: {trigger_id}")
            return True

        except Exception as e:
            logger.error(f"[delete_task]삭제작업시발송예외: {trigger_id}, 오류: {str(e)}")
            import traceback

            logger.error(f"[delete_task]오류정보: {traceback.format_exc()}")
            return False

    def get_task(self, trigger_id: str) -> Union[AsyncSchedulerTask, AsyncOneCallTask]:
        """
        가져오기 작업

        :param trigger_id: `str`, 작업id
        :return:
        """
        return self.tasks.get(trigger_id)

    def resume_task(self, trigger_id: str) -> bool:
        """
        열기시작작업

        :param trigger_id: `str`, 작업id
        :return:
        """
        task = self.get_task(trigger_id)
        if not task:
            return False

        task.resume()
        task.enable = True
        return True

    def pause_task(self, trigger_id: str) -> bool:
        """
        중지작업

        :param trigger_id: `str`, 작업id
        :return:
        """
        task = self.get_task(trigger_id)
        if not task:
            return False

        task.pause()
        task.enable = False
        return True

    def to_dict(self) -> list:
        """
        순서열작업

        :return:
            `List`, 순서열후의목록
        """

        return [task.__dict__() for task in self.tasks.values()]

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