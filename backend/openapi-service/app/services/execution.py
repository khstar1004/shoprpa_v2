import asyncio
import json
from datetime import datetime
from typing import Any, Optional
from uuid import uuid4

from redis.asyncio import Redis
from rpawebsocket.ws import BaseMsg
from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import AsyncSessionLocal
from app.logger import get_logger
from app.models.workflow import Execution
from app.schemas.workflow import ExecutionCreate, ExecutionStatus

logger = get_logger(__name__)


class ExecutionService:
    def __init__(self, db: AsyncSession, redis: Redis = None):
        self.db = db
        self.redis = redis

    async def create_execution(self, execution_data: ExecutionCreate, user_id: str) -> Execution:
        """생성실행기록"""
        execution_id = str(uuid4())
        parameters = execution_data.params or {}

        # 사용json.dumps확인매개변수으로있음의JSON형식저장
        parameters_json = json.dumps(parameters, ensure_ascii=False) if parameters else "{}"

        execution = Execution(
            id=execution_id,
            project_id=execution_data.project_id,
            parameters=parameters_json,
            user_id=user_id,
            exec_position=execution_data.exec_position,  # 저장실행위치
            version=execution_data.version,  # 저장버전
            recording_config=execution_data.recording_config,  # 저장기록제어매칭
            status=ExecutionStatus.PENDING.value,
        )

        self.db.add(execution)
        await self.db.flush()
        await self.db.refresh(execution)

        return execution

    async def get_execution(self, execution_id: str, user_id: str | None = None) -> Optional[Execution]:
        """가져오기실행기록"""
        query = select(Execution).where(Execution.id == execution_id)

        # 아니오검증user_id
        # if user_id is not None:
        #     query = query.where(Execution.user_id == user_id)

        result = await self.db.execute(query)
        return result.scalars().first()

    async def get_executions(
        self,
        project_id: str | None = None,
        user_id: str | None = None,
        skip: int = 0,
        limit: int = 100,
    ) -> list[Execution]:
        """가져오기실행기록목록"""
        query = select(Execution).order_by(Execution.start_time.desc()).offset(skip).limit(limit)

        if project_id is not None:
            query = query.where(Execution.project_id == project_id)
        if user_id is not None:
            query = query.where(Execution.user_id == user_id)

        result = await self.db.execute(query)
        return result.scalars().all()

    async def get_executions_by_user(
        self,
        user_id: str,
        pageNo: int = 1,
        pageSize: int = 10,
    ) -> tuple[list[Execution], int]:
        """분가져오기사용자의실행기록"""
        skip = (pageNo - 1) * pageSize

        # 조회데이터
        count_query = select(Execution).where(Execution.user_id == user_id)
        count_result = await self.db.execute(count_query)
        total = len(count_result.scalars().all())

        # 조회분데이터
        query = (
            select(Execution)
            .where(Execution.user_id == user_id)
            .order_by(Execution.start_time.desc())
            .offset(skip)
            .limit(pageSize)
        )
        result = await self.db.execute(query)
        executions = result.scalars().all()

        return executions, total

    async def update_execution_status(
        self,
        execution_id: str,
        status: str,
        result: dict[str, Any] | None = None,
        error: str | None = None,
    ) -> Optional[Execution]:
        """업데이트실행기록상태"""
        try:
            # 직선연결사용SQL업데이트, 상태제목
            update_stmt = update(Execution).where(Execution.id == execution_id)

            update_data = {Execution.status: status}
            if result is not None:
                if isinstance(result, dict):
                    update_data[Execution.result] = json.dumps(result, ensure_ascii=False)
                else:
                    # 결과가아니오예딕셔너리, 시도변환로문자열
                    update_data[Execution.result] = str(result)
            if error is not None:
                update_data[Execution.error] = error
            if status in [
                ExecutionStatus.COMPLETED.value,
                ExecutionStatus.FAILED.value,
                ExecutionStatus.CANCELLED.value,
            ]:
                update_data[Execution.end_time] = datetime.now()

            update_stmt = update_stmt.values(update_data)
            await self.db.execute(update_stmt)
            await self.db.commit()

            # 반환업데이트후의실행기록
            return await self.get_execution(execution_id)
        except Exception as e:
            # 결과가업데이트실패, 돌아가기서비스기록오류
            try:
                await self.db.rollback()
            except:
                pass  # 결과가돌아가기실패, 오류
            logger.exception("Failed to update execution %s", execution_id)
            return None

    async def execute_workflow(
        self,
        execution_data: ExecutionCreate,
        user_id: str,
        wait: bool = True,
        workflow_timeout: int = 36000,
    ) -> Optional[Execution]:
        """실행워크플로"""
        # 생성실행기록
        execution = await self.create_execution(execution_data, user_id)

        # 확인실행기록완료제출까지데이터베이스
        await self.db.commit()
        logger.info("Created execution %s and committed to database", execution.id)
        logger.info("[execute_workflow] user_id: %s ", user_id)

        # 저장 execution_id, 후필요사용
        execution_id = execution.id

        # 실행워크플로(예외/)
        # 추가짧음지연, 확인데이터베이스서비스전체제출
        await asyncio.sleep(0.1)

        if wait:
            # 실행방식 - 사용새의데이터베이스, 길이시간사용연결
            await self._run_workflow_with_new_session_sync(execution_id, workflow_timeout, user_id)

            # 사용기존다시 가져오기 새상태
            await self.db.refresh(execution)
        else:
            # 예외실행방식, 후실행(아니오대기결과)
            asyncio.create_task(self._run_workflow_with_new_session(execution_id, workflow_timeout, user_id))

        return execution

    async def _run_workflow(self, execution_id: str, workflow_timeout: int = 36000, user_id: str = "") -> None:
        """실행워크플로실행"""
        try:
            # 가져오기실행기록
            execution = await self.get_execution(execution_id)
            if not execution:
                logger.info("Execution not found for execution_id: %s", execution_id)
                raise Exception(f"Execution not found for execution_id: {execution_id}")
            logger.info("[_run_workflow] user_id: %s ", user_id)
            # 시간 초과
            await asyncio.wait_for(
                self._execute_workflow_logic(execution, user_id),
                timeout=workflow_timeout,
            )
        except TimeoutError:
            # 시간 초과관리 - 사용update_execution_status방법법제목
            await self.update_execution_status(execution_id, ExecutionStatus.RUNNING.value)
            raise
        except Exception as e:
            await self.update_execution_status(execution_id, ExecutionStatus.FAILED.value, error=str(e))

    async def _run_workflow_with_new_session_sync(self, execution_id: str, workflow_timeout: int, user_id: str) -> None:
        """사용새의데이터베이스실행워크플로(버전, 출력예외)"""
        async with AsyncSessionLocal() as db:
            execution_service = ExecutionService(db, self.redis)
            logger.info("Running workflow execution %s", execution_id)
            await execution_service._run_workflow(execution_id, workflow_timeout, user_id)

    async def _run_workflow_with_new_session(self, execution_id: str, workflow_timeout: int, user_id: str) -> None:
        """사용새의데이터베이스실행워크플로(예외버전, 아니오출력예외)"""
        async with AsyncSessionLocal() as db:
            try:
                execution_service = ExecutionService(db, self.redis)
                logger.info("Running background workflow execution %s", execution_id)
                await execution_service._run_workflow(execution_id, workflow_timeout, user_id)
            except Exception as e:
                # 기록오류로그
                logger.exception("Error in background workflow execution %s", execution_id)

                # 업데이트실행상태로실패, 확인사용자가능까지오류
                try:
                    # 사용새의업데이트상태, 제목
                    async with AsyncSessionLocal() as update_db:
                        update_service = ExecutionService(update_db, self.redis)
                        await update_service.update_execution_status(
                            execution_id, ExecutionStatus.FAILED.value, error=str(e)
                        )
                except Exception as update_error:
                    logger.exception("Failed to update execution status for %s", execution_id)

    async def _execute_workflow_logic(self, execution: Execution, user_id: str) -> None:
        """
        워크플로실행의
        예일개예시, 목록중필요근거아니오워크플로아니오의
        """
        import json

        logger.info("Starting workflow execution logic for execution %s", execution.id)

        try:
            # 예외워크플로실행
            # 목록중가능호출외부모듈시스템, 관리데이터대기
            # 돌아가기조정파일
            from app.dependencies import get_ws_service

            websocket_service = await get_ws_service()
            logger.info("Got websocket service for execution %s", execution.id)
            logger.info("execution.user_id: %s", execution.user_id)
            logger.info("input user_id: %s", user_id)

            wait = asyncio.Event()
            res = {}
            res_e = None

            def callback(watch_msg: BaseMsg | None = None, e: Exception | None = None):
                nonlocal wait, res, res_e
                if watch_msg:
                    res = watch_msg.data
                    logger.info("Received response for execution %s: %s", execution.id, res)
                    # Received response for execution 71e3147f-55cd-43f5-b7a8-b734d1075618:
                    # {'code': '5001', 'msg': '', 'data': None}
                if e:
                    res_e = e
                    logger.error("Received error for execution %s: %s", execution.id, e)
                wait.set()

            # 파싱매개변수, 확인예딕셔너리형식
            if execution.parameters is None:
                parameters_dict = {}
            elif isinstance(execution.parameters, str):
                try:
                    parameters_dict = json.loads(execution.parameters)
                except json.JSONDecodeError as e:
                    logger.exception("Failed to parse parameters JSON for execution %s", execution.id)
                    parameters_dict = {}
            else:
                parameters_dict = execution.parameters

            run_param = []
            for key, value in parameters_dict.items():
                logger.debug("매개변수: %s=%s", key, value)
                run_param.append({"varName": key, "varValue": value})
            run_param = json.dumps(run_param, ensure_ascii=False)

            executor_data = {
                "project_id": execution.project_id,
                "exec_position": execution.exec_position,
                "jwt": "",
                "run_param": run_param,
            }
            if execution.recording_config:
                executor_data["recording_config"] = execution.recording_config
            if execution.version:
                executor_data["version"] = execution.version

            base_msg = BaseMsg(
                channel="remote",
                key="run",
                uuid="$root$",
                send_uuid=f"{user_id}",
                need_reply=True,
                data=executor_data,
            ).init()

            logger.info("Sending WebSocket message for execution %s: %s", execution.id, base_msg.data)
            await websocket_service.ws_manager.send_reply(base_msg, 10 * 3600, callback)

            # 대기
            logger.info("Waiting for response for execution %s", execution.id)
            await wait.wait()
            logger.info("Received response for execution %s", execution.id)

            # 워크플로실행성공
            if res.get("code") == "0000":
                await self.update_execution_status(
                    execution.id,
                    ExecutionStatus.COMPLETED.value,
                    result=res,
                    error=str(res_e) if res_e else None,
                )
                logger.info("Updated execution %s status to COMPLETED", execution.id)
            elif res.get("code") == "5001":
                await self.update_execution_status(
                    execution.id,
                    ExecutionStatus.FAILED.value,
                    result=res,
                    error=str(res_e) if res_e else None,
                )
                logger.info("Updated execution %s status to FAILED", execution.id)

        except Exception as e:
            logger.exception("Error in workflow execution logic for %s", execution.id)
            raise

    async def cancel_execution(self, execution_id: str, user_id: str) -> bool:
        """가져오기 실행"""
        try:
            execution = await self.get_execution(execution_id, user_id)
            if not execution or execution.status not in [
                ExecutionStatus.PENDING.value,
                ExecutionStatus.RUNNING.value,
            ]:
                return False

            # 사용update_execution_status방법법
            updated_execution = await self.update_execution_status(execution_id, ExecutionStatus.CANCELLED.value)

            return updated_execution is not None
        except Exception as e:
            logger.exception("Failed to cancel execution %s", execution_id)
            return False