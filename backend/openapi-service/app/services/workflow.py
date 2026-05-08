import json
from typing import Optional

import httpx
from redis.asyncio import Redis
from sqlalchemy import delete, select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.logger import get_logger
from app.models.workflow import Workflow
from app.schemas.workflow import WorkflowBase

logger = get_logger(__name__)

ASTRON_AGENT_WORKFLOWS_URL = "https://xingchen-api.xf-yun.com/manage/workflow/get_info"


class WorkflowService:
    def __init__(self, db: AsyncSession, redis: Redis = None):
        self.db = db
        self.redis = redis

    async def _invalidate_workflows_cache(self, user_id: str) -> None:
        """Invalidate cached workflow lists for a user."""
        if self.redis:
            keys = await self.redis.keys(f"workflows:{user_id}:*")
            if keys:
                await self.redis.delete(*keys)

    async def _compare_and_merge_parameters(
        self, robot_params: Optional[str], existing_params: Optional[str]
    ) -> Optional[str]:
        """Merge incoming robot parameter metadata with stored parameter values."""
        logger.info("Merging workflow parameters")

        try:
            existing_list = json.loads(existing_params)
            robot_params = json.loads(robot_params)
        except (json.JSONDecodeError, TypeError):
            logger.exception("Failed to parse workflow parameters")
            return None

        logger.info("Existing parameter count: %s", len(existing_list))
        logger.info("Incoming parameter count: %s", len(robot_params))

        existing_params_map = {param.get("id"): param for param in existing_list}

        updated_list = []
        updated_count = 0
        added_count = 0

        for robot_param in robot_params:
            robot_param_id = robot_param.get("id")
            existing_param = existing_params_map.get(robot_param_id)

            if existing_param:
                if (
                    existing_param.get("varDirection") != robot_param.get("varDirection")
                    or existing_param.get("varName") != robot_param.get("varName")
                    or existing_param.get("varType") != robot_param.get("varType")
                    or existing_param.get("varValue") != robot_param.get("varValue")
                    or existing_param.get("processId") != robot_param.get("processId")
                    or existing_param.get("varDescribe") != robot_param.get("varDescribe")
                ):
                    logger.info("Detected parameter metadata change for %s", robot_param_id)

                    varDescribe = robot_param.get("varDescribe")
                    if not existing_param.get("varDescribe"):
                        varDescribe = robot_param.get("varDescribe")
                    if not robot_param.get("varDescribe"):
                        varDescribe = existing_param.get("varDescribe")

                    updated_param = existing_param.copy()
                    updated_param.update(
                        {
                            "varDirection": robot_param.get("varDirection"),
                            "varName": robot_param.get("varName"),
                            "varType": robot_param.get("varType"),
                            "varValue": robot_param.get("varValue"),
                            "processId": robot_param.get("processId"),
                            "varDescribe": varDescribe,
                        }
                    )
                    updated_list.append(updated_param)
                    updated_count += 1
                else:
                    updated_list.append(existing_param)
            else:
                logger.info("Detected new parameter %s", robot_param_id)
                updated_list.append(robot_param)
                added_count += 1

        deleted_count = len(existing_list) - len(
            [p for p in existing_list if p.get("id") in {rp.get("id") for rp in robot_params}]
        )
        if deleted_count > 0:
            logger.info("Detected %s deleted parameters", deleted_count)

        logger.info(
            "Parameter merge complete - updated: %s, added: %s, deleted: %s, final_count: %s",
            updated_count,
            added_count,
            deleted_count,
            len(updated_list),
        )

        return json.dumps(updated_list, ensure_ascii=False)

    async def create_workflow(self, workflow_data: WorkflowBase, user_id: str) -> Workflow:
        """Create a workflow."""
        existing_workflow = await self.db.execute(
            select(Workflow).where(Workflow.project_id == workflow_data.project_id)
        )
        if existing_workflow.scalars().first():
            raise ValueError(f"Project ID '{workflow_data.project_id}' already exists")

        workflow_dict = workflow_data.model_dump()

        workflow = Workflow(**workflow_dict, user_id=user_id)

        self.db.add(workflow)
        await self.db.flush()
        await self.db.refresh(workflow)

        await self._invalidate_workflows_cache(user_id)

        return workflow

    async def get_workflow(self, project_id: str, user_id: str | None = None) -> Optional[Workflow]:
        """Get a workflow by project id."""
        query = select(Workflow).where(Workflow.project_id == project_id)
        if user_id is not None:
            query = query.where(Workflow.user_id == user_id)

        result = await self.db.execute(query)
        workflow = result.scalars().first()

        if not workflow and user_id is not None:
            example_query = select(Workflow).where(
                Workflow.user_id == user_id,
                Workflow.example_project_id == project_id,
                Workflow.example_project_id.isnot(None),
            )
            result = await self.db.execute(example_query)
            workflow = result.scalars().first()

        return workflow

    async def get_workflows(
        self, user_id: str | None = None, skip: int = 0, limit: int | None = None
    ) -> list[Workflow]:
        """List active workflows."""
        base_query = select(Workflow).where(Workflow.status == 1)

        if limit is None:
            query = base_query.order_by(Workflow.created_at.desc()).offset(skip)
        else:
            query = base_query.order_by(Workflow.created_at.desc()).offset(skip).limit(limit)
        if user_id is not None:
            query = query.where(Workflow.user_id == user_id)
        result = await self.db.execute(query)
        workflows = result.scalars().all()
        return workflows

    async def update_workflow(self, workflow_data: WorkflowBase, user_id: str) -> Optional[Workflow]:
        """Update a workflow owned by a user."""
        project_id = str(workflow_data.project_id)
        workflow = await self.get_workflow(project_id, user_id)
        if not workflow:
            return None

        workflow_dict = workflow_data.model_dump(exclude_unset=True, exclude={"project_id"})
        if not workflow_dict:
            return workflow

        if "parameters" in workflow_dict:
            logger.info("Merging parameters for workflow %s", project_id)
            merged_params = await self._compare_and_merge_parameters(
                workflow_dict["parameters"],
                workflow.parameters,
            )
            if merged_params is not None:
                workflow_dict["parameters"] = merged_params

        stmt = (
            update(Workflow)
            .where(Workflow.project_id == project_id, Workflow.user_id == user_id)
            .values(**workflow_dict)
        )

        await self.db.execute(stmt)

        await self._invalidate_workflows_cache(user_id)

        await self.db.refresh(workflow)
        return workflow

    async def delete_workflow(self, project_id: str, user_id: str) -> bool:
        """Delete a workflow owned by a user."""
        workflow = await self.get_workflow(project_id, user_id)
        if not workflow:
            return False

        stmt = delete(Workflow).where(Workflow.project_id == project_id, Workflow.user_id == user_id)

        await self.db.execute(stmt)

        await self._invalidate_workflows_cache(user_id)

        return True

    async def get_workflow_stats(self, user_id: str | None = None) -> dict:
        """Return workflow counts."""
        query = select(Workflow)
        if user_id is not None:
            query = query.where(Workflow.user_id == user_id)

        result = await self.db.execute(query)
        workflows = result.scalars().all()

        total = len(workflows)
        active = sum(1 for w in workflows if w.status == 1)
        inactive = sum(1 for w in workflows if w.status == 0)

        return {"total": total, "active": active, "inactive": inactive}

    async def get_astron_workflows(self, auth_id: int, app_id: str, api_key: str, api_secret: str):
        """List workflows available from a saved ShopRPA Agent credential."""
        try:
            headers = {
                "X-Consumer-Username": app_id,
                "Authorization": f"Bearer {api_key}:{api_secret}",
                "Content-Type": "application/json",
            }

            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.get(ASTRON_AGENT_WORKFLOWS_URL, headers=headers)
                response.raise_for_status()

                result = response.json()

                if result.get("code") != 0:
                    logger.error("ShopRPA API error: %s", result.get("message", "Unknown error"))
                    return []

                workflows_data = result.get("data", {})
                workflows = workflows_data.get("pageData", [])

                formatted_workflows = []
                for workflow in workflows:
                    formatted_workflows.append(
                        {
                            "authId": auth_id,
                            "flowId": workflow.get("flowId"),
                            "name": workflow.get("name"),
                            "description": workflow.get("description", ""),
                            "inputs": workflow.get("ioParams", {}).get("inputs", []),
                            "outputs": workflow.get("ioParams", {}).get("outputs", []),
                            "createTime": workflow.get("createTime"),
                            "updateTime": workflow.get("updateTime"),
                        }
                    )

                return formatted_workflows

        except httpx.TimeoutException:
            logger.error("Timeout when calling ShopRPA API for app_id: %s", app_id)
            return []
        except httpx.HTTPStatusError as e:
            logger.error("HTTP error when calling ShopRPA API for app_id %s: %s", app_id, str(e))
            return []
        except Exception as e:
            logger.error("Error calling ShopRPA API for app_id %s: %s", app_id, str(e))
            return []
