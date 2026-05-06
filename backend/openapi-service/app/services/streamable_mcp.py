import json
from typing import Optional

from mcp import types

from app.logger import get_logger

logger = get_logger(__name__)


class ToolsConfig:
    """도구매칭관리관리기기"""

    def __init__(self):
        self.redis = None

    async def _ensure_redis_connection(self):
        """확인Redis연결완료"""
        if self.redis is None:
            try:
                from app.redis import get_redis

                async for redis_conn in get_redis():
                    self.redis = redis_conn
                    break
            except Exception as e:
                logger.warning("Failed to initialize Redis connection: %s", e)
                self.redis = None

    async def _get_workflow_service(self):
        """가져오기WorkflowService"""
        await self._ensure_redis_connection()
        from app.database import AsyncSessionLocal
        from app.dependencies import get_workflow_service

        # 생성데이터베이스
        db = AsyncSessionLocal()
        try:
            # 가져오기WorkflowService
            workflow_service = await get_workflow_service(db, self.redis)
            return workflow_service, db
        except Exception as e:
            # 결과가출력오류, 닫기데이터베이스
            await db.close()
            raise e

    async def cleanup_connections(self):
        """관리Redis연결"""
        # Redis연결반환까지연결, 아니오필요닫기
        self.redis = None

    async def get_uid_from_raw_key(self, api_key: str) -> Optional[str]:
        """
        직선연결API Key, 조회데이터베이스까지 user_id (사용MCP도구데이터)
        사용비고입력방식
        """
        if not api_key:
            return None

        from sqlalchemy.future import select

        from app.database import AsyncSessionLocal
        from app.models.api_key import OpenAPIDB
        from app.utils.api_key import APIKeyUtils

        db = None
        try:
            db = AsyncSessionLocal()

            # 사용전매칭및인증
            keys = await db.execute(select(OpenAPIDB).where(OpenAPIDB.prefix == api_key[:8], OpenAPIDB.is_active == 1))
            api_keys = keys.scalars().all()

            for key in api_keys:
                hashed_key = key.api_key
                if APIKeyUtils.verify_api_key(api_key, hashed_key):
                    return str(key.user_id)
            return None
        except Exception as e:
            logger.exception("Error getting user ID from API key")
            if db:
                await db.rollback()
            return None
        finally:
            # 확인데이터베이스닫기
            if db:
                await db.close()

    async def get_user_workflows(self, user_id: str) -> list[dict]:
        """가져오기사용자허용사용의도구목록"""
        db = None
        try:
            workflow_service, db = await self._get_workflow_service()

            # 가져오기사용자워크플로
            user_workflows = await workflow_service.get_workflows(user_id)
            workflows = []
            for workflow in user_workflows:
                workflows.append(workflow.to_dict())
            return workflows
        except Exception as e:
            logger.exception("Error getting user workflows")
            return []
        finally:
            # 확인데이터베이스닫기
            if db:
                await db.close()

    async def get_project_id_by_name(self, name: str, user_id: str) -> Optional[tuple[str, int]]:
        """근거도구이름및사용자ID조회의워크플로목록ID"""
        db = None
        try:
            workflow_service, db = await self._get_workflow_service()

            # 가져오기사용자워크플로
            user_workflows = await workflow_service.get_workflows(user_id)

            # 조회매칭의워크플로
            for workflow in user_workflows:
                # 사용 english_name, 결과가있음이면사용 name
                workflow_name = workflow.english_name or workflow.name
                if workflow_name == name:
                    return workflow.project_id, workflow.version

            return None
        except Exception as e:
            logger.exception("Error getting project_id for name '%s' and user_id '%s'", name, user_id)
            return None
        finally:
            # 확인데이터베이스닫기
            if db:
                await db.close()

    async def execute_workflow_by_name(self, name: str, user_id: str, arguments: dict) -> dict:
        """근거도구이름실행의워크플로"""
        await self._ensure_redis_connection()

        try:
            # 조회의워크플로목록ID
            project_id, version = await self.get_project_id_by_name(name, user_id)
            if not project_id:
                return {
                    "success": False,
                    "error": f"No workflow found for tool '{name}' or permission denied",
                }

            # 생성실행매개변수
            from app.schemas.workflow import ExecutionCreate

            execution_data = ExecutionCreate(
                project_id=project_id, params=arguments, exec_position="EXECUTOR", version=version
            )

            # 생성실행서비스실행워크플로
            from app.database import AsyncSessionLocal
            from app.services.execution import ExecutionService

            async with AsyncSessionLocal() as db_session:
                execution_service = ExecutionService(db_session)
                logger.info("[execute_workflow_by_name] user_id '%s'", user_id)
                # 예외실행워크플로
                execution = await execution_service.execute_workflow(
                    execution_data=execution_data,
                    user_id=user_id,
                    wait=True,  # 대기결과, 사용방법법
                    workflow_timeout=600,
                )

                return {
                    "success": True,
                    "execution_id": execution.id,
                    "project_id": project_id,
                    "data": execution.to_dict(),
                    "message": json.loads(execution.result),
                }

        except Exception as e:
            logger.exception("Error executing workflow for tool '%s'", name)
            return {"success": False, "error": f"Failed to execute workflow: {str(e)}"}

    @staticmethod
    def workflow_to_tool(workflow: dict):
        """를워크플로매칭변환로MCP도구매칭"""
        # 사용english_name로도구이름
        tool_name = workflow.get("english_name") or workflow.get("name")

        # 결과가있음지정parameters, 사용로입력매개변수매칭
        parameters = workflow.get("parameters")
        if parameters and isinstance(parameters, list):
            # 변환매개변수배열로JSON Schema형식
            tool_input_schema = ToolsConfig._convert_parameters_to_schema(parameters)
        else:
            tool_input_schema = parameters or {"type": "object"}

        tool_config = types.Tool(
            name=tool_name,
            description=workflow.get("description"),
            inputSchema=tool_input_schema,
        )

        return tool_config

    @staticmethod
    def _convert_parameters_to_schema(parameters: list[dict]) -> dict:
        """를워크플로매개변수배열변환로JSON Schema형식"""
        schema = {"type": "object", "properties": {}, "required": []}

        # 유형테이블
        type_mapping = {
            "Str": "string",
            "Int": "integer",
            "Float": "number",
            "PATH": "string",
            "DIRPATH": "string",
            "Date": "string",
            "Password": "string",
        }

        for param in parameters:
            # 관리입력매개변수 (varDirection = 0)
            if param.get("varDirection") == 0:
                var_name = param.get("varName")
                var_type = param.get("varType", "Str")
                var_describe = param.get("varDescribe", "")
                var_value = param.get("varValue", "")

                if var_name:
                    # 유형
                    json_type = type_mapping.get(var_type, "string")

                    # 생성속성지정
                    property_def = {"type": json_type, "description": var_describe}

                    # 결과가있음값, 추가값
                    if var_value and var_value != "":
                        if json_type == "integer":
                            try:
                                property_def["default"] = int(var_value)
                            except ValueError:
                                pass
                        elif json_type == "number":
                            try:
                                property_def["default"] = float(var_value)
                            except ValueError:
                                pass
                        else:
                            property_def["default"] = var_value

                    schema["properties"][var_name] = property_def

                    # 결과가있음값, 추가까지필요필드
                    if not var_value or var_value == "":
                        schema["required"].append(var_name)

        return schema

    async def get_tools_for_user(self, user_id: str) -> list[types.Tool]:
        """가져오기사용자가능사용의도구매칭목록"""
        user_workflows = await self.get_user_workflows(user_id)
        user_tools = []
        for workflow in user_workflows:
            user_tools.append(self.workflow_to_tool(workflow))

        return user_tools