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
        """Create an execution record."""
        execution_id = str(uuid4())
        parameters = execution_data.params or {}

        parameters_json = json.dumps(parameters, ensure_ascii=False) if parameters else "{}"

        execution = Execution(
            id=execution_id,
            project_id=execution_data.project_id,
            parameters=parameters_json,
            user_id=user_id,
            exec_position=execution_data.exec_position,
            version=execution_data.version,
            recording_config=execution_data.recording_config,
            status=ExecutionStatus.PENDING.value,
        )

        self.db.add(execution)
        await self.db.flush()
        await self.db.refresh(execution)

        return execution

    async def get_execution(self, execution_id: str, user_id: str | None = None) -> Optional[Execution]:
        """Get an execution record, scoped to a user when supplied."""
        query = select(Execution).where(Execution.id == execution_id)

        if user_id is not None:
            query = query.where(Execution.user_id == user_id)

        result = await self.db.execute(query)
        return result.scalars().first()

    async def get_executions(
        self,
        project_id: str | None = None,
        user_id: str | None = None,
        skip: int = 0,
        limit: int = 100,
    ) -> list[Execution]:
        """List execution records."""
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
        """List paged execution records for a user."""
        skip = (pageNo - 1) * pageSize

        count_query = select(Execution).where(Execution.user_id == user_id)
        count_result = await self.db.execute(count_query)
        total = len(count_result.scalars().all())

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
        """Update an execution status."""
        try:
            update_stmt = update(Execution).where(Execution.id == execution_id)

            update_data = {Execution.status: status}
            if result is not None:
                if isinstance(result, dict):
                    update_data[Execution.result] = json.dumps(result, ensure_ascii=False)
                else:
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

            return await self.get_execution(execution_id)
        except Exception as e:
            try:
                await self.db.rollback()
            except Exception:
                logger.exception("Rollback failed after execution update error")
            logger.exception("Failed to update execution %s", execution_id)
            return None

    async def execute_workflow(
        self,
        execution_data: ExecutionCreate,
        user_id: str,
        wait: bool = True,
        workflow_timeout: int = 36000,
    ) -> Optional[Execution]:
        """Execute a workflow through the connected desktop worker."""
        execution = await self.create_execution(execution_data, user_id)

        await self.db.commit()
        logger.info("Created execution %s and committed to database", execution.id)
        logger.info("[execute_workflow] user_id: %s ", user_id)

        execution_id = execution.id

        await asyncio.sleep(0.1)

        if wait:
            await self._run_workflow_with_new_session_sync(execution_id, workflow_timeout, user_id)

            await self.db.refresh(execution)
        else:
            asyncio.create_task(self._run_workflow_with_new_session(execution_id, workflow_timeout, user_id))

        return execution

    async def _run_workflow(self, execution_id: str, workflow_timeout: int = 36000, user_id: str = "") -> None:
        """Run a workflow and update its execution record."""
        try:
            execution = await self.get_execution(execution_id)
            if not execution:
                logger.info("Execution not found for execution_id: %s", execution_id)
                raise Exception(f"Execution not found for execution_id: {execution_id}")
            logger.info("[_run_workflow] user_id: %s ", user_id)
            await asyncio.wait_for(
                self._execute_workflow_logic(execution, user_id),
                timeout=workflow_timeout,
            )
        except TimeoutError:
            await self.update_execution_status(execution_id, ExecutionStatus.RUNNING.value)
            raise
        except Exception as e:
            await self.update_execution_status(execution_id, ExecutionStatus.FAILED.value, error=str(e))

    async def _run_workflow_with_new_session_sync(self, execution_id: str, workflow_timeout: int, user_id: str) -> None:
        """Run a workflow in a new DB session and propagate errors."""
        async with AsyncSessionLocal() as db:
            execution_service = ExecutionService(db, self.redis)
            logger.info("Running workflow execution %s", execution_id)
            await execution_service._run_workflow(execution_id, workflow_timeout, user_id)

    async def _run_workflow_with_new_session(self, execution_id: str, workflow_timeout: int, user_id: str) -> None:
        """Run a workflow in a background DB session."""
        async with AsyncSessionLocal() as db:
            try:
                execution_service = ExecutionService(db, self.redis)
                logger.info("Running background workflow execution %s", execution_id)
                await execution_service._run_workflow(execution_id, workflow_timeout, user_id)
            except Exception as e:
                logger.exception("Error in background workflow execution %s", execution_id)

                try:
                    async with AsyncSessionLocal() as update_db:
                        update_service = ExecutionService(update_db, self.redis)
                        await update_service.update_execution_status(
                            execution_id, ExecutionStatus.FAILED.value, error=str(e)
                        )
                except Exception as update_error:
                    logger.exception("Failed to update execution status for %s", execution_id)

    async def _execute_workflow_logic(self, execution: Execution, user_id: str) -> None:
        """
        Send the workflow run request to the connected desktop worker.
        """
        import json

        logger.info("Starting workflow execution logic for execution %s", execution.id)

        try:
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
                    if isinstance(watch_msg.data, dict):
                        res = watch_msg.data
                    else:
                        res = {"code": None, "data": watch_msg.data, "msg": "Invalid worker response payload"}
                    logger.info("Received worker response for execution %s with code %s", execution.id, res.get("code"))
                if e:
                    res_e = e
                    logger.error("Received error for execution %s: %s", execution.id, e)
                wait.set()

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
                logger.debug("Preparing execution parameter %s", key)
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

            logger.info("Sending WebSocket run message for execution %s", execution.id)
            await websocket_service.ws_manager.send_reply(base_msg, 10 * 3600, callback)

            logger.info("Waiting for response for execution %s", execution.id)
            await wait.wait()
            logger.info("Received response for execution %s", execution.id)

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
            else:
                await self.update_execution_status(
                    execution.id,
                    ExecutionStatus.FAILED.value,
                    result=res,
                    error=f"Unexpected worker response code: {res.get('code')}",
                )
                logger.warning("Updated execution %s status to FAILED due to unexpected worker response", execution.id)

        except Exception as e:
            logger.exception("Error in workflow execution logic for %s", execution.id)
            raise

    async def cancel_execution(self, execution_id: str, user_id: str) -> bool:
        """Cancel a pending or running execution."""
        try:
            execution = await self.get_execution(execution_id, user_id)
            if not execution or execution.status not in [
                ExecutionStatus.PENDING.value,
                ExecutionStatus.RUNNING.value,
            ]:
                return False

            updated_execution = await self.update_execution_status(execution_id, ExecutionStatus.CANCELLED.value)

            return updated_execution is not None
        except Exception as e:
            logger.exception("Failed to cancel execution %s", execution_id)
            return False
