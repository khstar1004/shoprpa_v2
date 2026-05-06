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
        """지우기사용자워크플로저장"""
        if self.redis:
            # 지우기가능의모든분저장
            keys = await self.redis.keys(f"workflows:{user_id}:*")
            if keys:
                await self.redis.delete(*keys)

    async def _compare_and_merge_parameters(
        self, robot_params: Optional[str], existing_params: Optional[str]
    ) -> Optional[str]:
        """
        새매개변수 병합

        결과가robot_params미완료, 이면및existing_params행, 
        결과가id, varDirection, varName, varType, varValue있음작업일아니오, 업데이트일기록

        Args:
            robot_params: 새의매개변수JSON문자열
            existing_params: 있음의매개변수JSON문자열

        Returns:
            병합후의매개변수JSON문자열
        """
        logger.info("열기 및병합워크플로매개변수")

        # 파싱있음매개변수
        try:
            existing_list = json.loads(existing_params)
            robot_params = json.loads(robot_params)
        except (json.JSONDecodeError, TypeError):
            logger.exception("파싱있음매개변수실패")
            return None

        logger.info("있음매개변수개수: %s", len(existing_list))
        logger.info("새매개변수개수: %s", len(robot_params))

        # 생성있음매개변수의id조회
        existing_params_map = {param.get("id"): param for param in existing_list}

        # 으로robot_params로행업데이트
        updated_list = []
        updated_count = 0
        added_count = 0

        for robot_param in robot_params:
            robot_param_id = robot_param.get("id")
            existing_param = existing_params_map.get(robot_param_id)

            if existing_param:
                # 있음매개변수중존재함, 조회닫기 필드여부있음변수
                if (
                    existing_param.get("varDirection") != robot_param.get("varDirection")
                    or existing_param.get("varName") != robot_param.get("varName")
                    or existing_param.get("varType") != robot_param.get("varType")
                    or existing_param.get("varValue") != robot_param.get("varValue")
                    or existing_param.get("processId") != robot_param.get("processId")
                    or existing_param.get("varDescribe") != robot_param.get("varDescribe")
                ):
                    # 업데이트기록(보관필드)
                    logger.info("매개변수 %s 감지까지변수, 행업데이트", robot_param_id)

                    # 설명필드, 결과가개아니오빈 값, 사용새의설명
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
                    # 보관있음매개변수아니오변수
                    updated_list.append(existing_param)
            else:
                # 있음매개변수중존재하지 않음, 예새매개변수, 직선연결추가
                logger.info("감지까지새매개변수 %s, 직선연결추가", robot_param_id)
                updated_list.append(robot_param)
                added_count += 1

        # 기록삭제의매개변수
        deleted_count = len(existing_list) - len(
            [p for p in existing_list if p.get("id") in {rp.get("id") for rp in robot_params}]
        )
        if deleted_count > 0:
            logger.info("감지까지 %s 개삭제됨의매개변수", deleted_count)

        logger.info(
            "매개변수업데이트완료 - 업데이트: %s, 추가: %s, 삭제: %s, 종료매개변수개수: %s",
            updated_count,
            added_count,
            deleted_count,
            len(updated_list),
        )

        # 반환업데이트후의매개변수JSON
        return json.dumps(updated_list, ensure_ascii=False)

    async def create_workflow(self, workflow_data: WorkflowBase, user_id: str) -> Workflow:
        """생성새워크플로"""
        # 조회 project_id 여부완료존재함
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

        # 지우기저장
        await self._invalidate_workflows_cache(user_id)

        return workflow

    async def get_workflow(self, project_id: str, user_id: str | None = None) -> Optional[Workflow]:
        """가져오기 지정워크플로"""
        query = select(Workflow).where(Workflow.project_id == project_id)
        if user_id is not None:
            query = query.where(Workflow.user_id == user_id)

        result = await self.db.execute(query)
        workflow = result.scalars().first()

        # 결과가직선연결검색검색까지project_id, 시도통신경과example_project_id조회
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
        """가져오기워크플로목록(반환상태로1의목록)"""
        base_query = select(Workflow).where(Workflow.status == 1)

        if limit is None:
            query = base_query.order_by(Workflow.created_at.desc()).offset(skip)
        else:
            query = base_query.order_by(Workflow.created_at.desc()).offset(skip).limit(limit)
        # 결과가지정완료사용자ID, 추가사용자필터링파일
        if user_id is not None:
            query = query.where(Workflow.user_id == user_id)
        result = await self.db.execute(query)
        workflows = result.scalars().all()
        return workflows

    async def update_workflow(self, workflow_data: WorkflowBase, user_id: str) -> Optional[Workflow]:
        """업데이트워크플로"""
        # 조회워크플로여부저장된 현재사용자
        project_id = str(workflow_data.project_id)
        workflow = await self.get_workflow(project_id, user_id)
        if not workflow:
            return None

        # 업데이트의필드
        workflow_dict = workflow_data.model_dump(exclude_unset=True, exclude={"project_id"})
        if not workflow_dict:  # 결과가있음작업필드, 직선연결반환
            return workflow

        if "parameters" in workflow_dict:
            logger.info(workflow_dict["parameters"])
            # 사용자parameters, 요청 연결
            merged_params = await self._compare_and_merge_parameters(
                workflow_dict["parameters"],  # 새매개변수로None테이블사용자미완료
                workflow.parameters,  # 있음매개변수
            )
            if merged_params is not None:
                workflow_dict["parameters"] = merged_params

        # 실행업데이트
        stmt = (
            update(Workflow)
            .where(Workflow.project_id == project_id, Workflow.user_id == user_id)
            .values(**workflow_dict)
        )

        await self.db.execute(stmt)

        # 지우기저장
        await self._invalidate_workflows_cache(user_id)

        # 다시 가져오기업데이트후의워크플로
        await self.db.refresh(workflow)
        return workflow

    async def delete_workflow(self, project_id: str, user_id: str) -> bool:
        """삭제워크플로"""
        # 조회워크플로여부저장된 현재사용자
        workflow = await self.get_workflow(project_id, user_id)
        if not workflow:
            return False

        # 실행삭제
        stmt = delete(Workflow).where(Workflow.project_id == project_id, Workflow.user_id == user_id)

        await self.db.execute(stmt)

        # 지우기저장
        await self._invalidate_workflows_cache(user_id)

        return True

    async def get_workflow_stats(self, user_id: str | None = None) -> dict:
        """가져오기워크플로시스템계획정보"""
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
        """가져오기ShoprpaAgent모든워크플로"""
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

                # 조회API여부성공
                if result.get("code") != 0:
                    logger.error("Shoprpa API error: %s", result.get("message", "Unknown error"))
                    return []

                # 가져오기워크플로데이터
                workflows_data = result.get("data", {})
                workflows = workflows_data.get("pageData", [])

                # 형식반환데이터, 보관닫기 정보
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
            logger.error("Timeout when calling Shoprpa API for app_id: %s", app_id)
            return []
        except httpx.HTTPStatusError as e:
            logger.error("HTTP error when calling Shoprpa API for app_id %s: %s", app_id, str(e))
            return []
        except Exception as e:
            logger.error("Error calling Shoprpa API for app_id %s: %s", app_id, str(e))
            return []