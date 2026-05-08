import json
from typing import Optional

from mcp import types

from app.logger import get_logger

logger = get_logger(__name__)


class ToolsConfig:
    """Manage MCP tool mappings for the authenticated user."""

    def __init__(self):
        self.redis = None

    async def _ensure_redis_connection(self):
        """Initialize Redis once when the service path needs it."""
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
        """Create a workflow service and return it with its DB session."""
        await self._ensure_redis_connection()
        from app.database import AsyncSessionLocal
        from app.dependencies import get_workflow_service

        db = AsyncSessionLocal()
        try:
            workflow_service = await get_workflow_service(db, self.redis)
            return workflow_service, db
        except Exception:
            await db.close()
            raise

    async def cleanup_connections(self):
        """Release cached connection references."""
        self.redis = None

    async def get_uid_from_raw_key(self, api_key: str) -> Optional[str]:
        """Validate a raw MCP API key and return the owning user id."""
        if not api_key:
            return None

        from app.database import AsyncSessionLocal
        from app.services.api_key import ApiKeyService

        db = None
        try:
            db = AsyncSessionLocal()
            return await ApiKeyService(db, self.redis).validate_api_key(api_key)
        except Exception as e:
            logger.exception("Error getting user ID from API key")
            if db:
                await db.rollback()
            return None
        finally:
            if db:
                await db.close()

    async def get_user_workflows(self, user_id: str) -> list[dict]:
        """Return workflows the user can expose as tools."""
        db = None
        try:
            workflow_service, db = await self._get_workflow_service()

            user_workflows = await workflow_service.get_workflows(user_id)
            workflows = []
            for workflow in user_workflows:
                workflows.append(workflow.to_dict())
            return workflows
        except Exception as e:
            logger.exception("Error getting user workflows")
            return []
        finally:
            if db:
                await db.close()

    async def get_project_id_by_name(self, name: str, user_id: str) -> Optional[tuple[str, int]]:
        """Find a workflow project id and version by exposed tool name."""
        db = None
        try:
            workflow_service, db = await self._get_workflow_service()

            user_workflows = await workflow_service.get_workflows(user_id)

            for workflow in user_workflows:
                workflow_name = workflow.english_name or workflow.name
                if workflow_name == name:
                    return workflow.project_id, workflow.version

            return None
        except Exception as e:
            logger.exception("Error getting project_id for name '%s' and user_id '%s'", name, user_id)
            return None
        finally:
            if db:
                await db.close()

    async def execute_workflow_by_name(self, name: str, user_id: str, arguments: dict) -> dict:
        """Execute a workflow exposed through an MCP tool name."""
        await self._ensure_redis_connection()

        try:
            project_ref = await self.get_project_id_by_name(name, user_id)
            if not project_ref:
                return {
                    "success": False,
                    "error": f"No workflow found for tool '{name}' or permission denied",
                }
            project_id, version = project_ref

            from app.schemas.workflow import ExecutionCreate

            execution_data = ExecutionCreate(
                project_id=project_id, params=arguments, exec_position="EXECUTOR", version=version
            )

            from app.database import AsyncSessionLocal
            from app.services.execution import ExecutionService

            async with AsyncSessionLocal() as db_session:
                execution_service = ExecutionService(db_session)
                logger.info("[execute_workflow_by_name] user_id '%s'", user_id)
                execution = await execution_service.execute_workflow(
                    execution_data=execution_data,
                    user_id=user_id,
                    wait=True,
                    workflow_timeout=600,
                )
                try:
                    message = json.loads(execution.result) if isinstance(execution.result, str) else execution.result
                except (TypeError, json.JSONDecodeError):
                    message = execution.result

                return {
                    "success": True,
                    "execution_id": execution.id,
                    "project_id": project_id,
                    "data": execution.to_dict(),
                    "message": message,
                }

        except Exception as e:
            logger.exception("Error executing workflow for tool '%s'", name)
            return {"success": False, "error": f"Failed to execute workflow: {str(e)}"}

    @staticmethod
    def workflow_to_tool(workflow: dict):
        """Convert a workflow record into an MCP tool definition."""
        tool_name = workflow.get("english_name") or workflow.get("name")

        parameters = workflow.get("parameters")
        if parameters and isinstance(parameters, list):
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
        """Convert workflow parameter metadata into JSON schema."""
        schema = {"type": "object", "properties": {}, "required": []}

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
            if param.get("varDirection") == 0:
                var_name = param.get("varName")
                var_type = param.get("varType", "Str")
                var_describe = param.get("varDescribe", "")
                var_value = param.get("varValue", "")

                if var_name:
                    json_type = type_mapping.get(var_type, "string")

                    property_def = {"type": json_type, "description": var_describe}

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

                    if not var_value or var_value == "":
                        schema["required"].append(var_name)

        return schema

    async def get_tools_for_user(self, user_id: str) -> list[types.Tool]:
        """Return MCP tool definitions for workflows owned by a user."""
        user_workflows = await self.get_user_workflows(user_id)
        user_tools = []
        for workflow in user_workflows:
            user_tools.append(self.workflow_to_tool(workflow))

        return user_tools
