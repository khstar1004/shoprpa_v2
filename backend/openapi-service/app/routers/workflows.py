import json

import httpx
from fastapi import APIRouter, Depends, HTTPException, Path, Query, status

from app.config import get_settings
from app.dependencies import (
    get_astron_api_key_service,
    get_execution_service,
    get_user_id_from_api_key,
    get_user_id_from_header,
    get_user_id_with_fallback,
    get_user_service,
    get_workflow_service,
)
from app.logger import get_logger
from app.schemas import ResCode, StandardResponse
from app.schemas.workflow import (
    ExecutionCreate,
    ExecutionStatus,
    WorkflowBase,
    WorkflowCopyRequest,
)
from app.services.api_key import ShoprpaApiKeyService
from app.services.execution import ExecutionService
from app.services.user import UserService
from app.services.workflow import WorkflowService

logger = get_logger(__name__)

router = APIRouter(prefix="/workflows", tags=["workflow"])


def get_internal_headers() -> dict[str, str]:
    return {"X-API-Key": get_settings().INTERNAL_ADMIN_API_KEY}


def get_robot_service_url(path: str) -> str:
    return f"{get_settings().ROBOT_SERVICE_BASE_URL.rstrip('/')}/{path.lstrip('/')}"


@router.post(
    "/upsert",
    response_model=StandardResponse,
    status_code=status.HTTP_200_OK,
    summary="Create or update workflow",
    description="Create a workflow when project_id is new, otherwise update the existing workflow.",
)
async def create_or_update_workflow(
    workflow_data: WorkflowBase,
    user_id: str = Depends(get_user_id_from_header),
    service: WorkflowService = Depends(get_workflow_service),
):
    """Create or update a workflow."""
    try:
        existing_workflow = await service.get_workflow(str(workflow_data.project_id))

        if existing_workflow:
            if existing_workflow.user_id != user_id:
                return StandardResponse(
                    code=ResCode.ERR,
                    msg=f"Project ID '{workflow_data.project_id}' already exists and belongs to another user",
                    data=None,
                )

            workflow = await service.update_workflow(workflow_data, user_id)
            action = "updated"
        else:
            workflow = await service.create_workflow(workflow_data, user_id)
            action = "created"

        workflow_dict = workflow.to_dict()

        return StandardResponse(
            code=ResCode.SUCCESS,
            msg=f"Workflow {action} successfully",
            data={"workflow": workflow_dict, "action": action},
        )
    except Exception as e:
        logger.error("Error creating/updating workflow: %s", str(e))
        return StandardResponse(code=ResCode.ERR, msg="Failed to create or update workflow", data=None)


@router.get(
    "/get",
    response_model=StandardResponse,
    summary="List workflows",
    description="List workflows owned by the current user.",
)
async def get_workflows(
    pageNo: int = Query(1, ge=1, description="Page number"),
    pageSize: int = Query(100, ge=1, le=100, description="Page size"),
    user_id: str = Depends(get_user_id_with_fallback),
    service: WorkflowService = Depends(get_workflow_service),
):
    """List workflows."""
    try:
        skip = (pageNo - 1) * pageSize
        workflows = await service.get_workflows(user_id, skip, pageSize)
        workflow_dicts = []
        personal_total = 0
        public_total = 0
        for workflow in workflows:
            workflow_dicts.append(workflow.to_dict())
            if not workflow.example_project_id:
                personal_total += 1
            else:
                public_total += 1

        return StandardResponse(
            code=ResCode.SUCCESS,
            msg="",
            data={
                "total": len(workflow_dicts),
                "personal_total": personal_total,
                "public_total": public_total,
                "records": workflow_dicts,
            },
        )
    except Exception as e:
        logger.error("Error getting workflows: %s", str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to get workflows",
        )


@router.get(
    "/get/{project_id}",
    response_model=StandardResponse,
    summary="Get workflow",
    description="Get workflow details by project_id.",
)
async def get_workflow(
    project_id: str = Path(..., description="Project ID"),
    service: WorkflowService = Depends(get_workflow_service),
):
    """Get a workflow."""
    try:
        workflow = await service.get_workflow(project_id, None)
        if not workflow:
            return StandardResponse(
                code=ResCode.SUCCESS,
                msg=f"Workflow with project_id {project_id} not found",
                data=None,
            )
        workflow_dict = workflow.to_dict()
        return StandardResponse(code=ResCode.SUCCESS, msg="", data={"workflow": workflow_dict})
    except Exception as e:
        logger.error("Error getting workflow %s: %s", project_id, str(e))
        return StandardResponse(code=ResCode.ERR, msg="Failed to get workflow", data=None)


@router.post(
    "/execute",
    response_model=StandardResponse,
    summary="Execute workflow and wait",
    description="Execute a workflow and wait for the execution result.",
)
async def execute_workflow(
    execution_data: ExecutionCreate,
    user_id: str = Depends(get_user_id_from_api_key),
    workflow_service: WorkflowService = Depends(get_workflow_service),
    execution_service: ExecutionService = Depends(get_execution_service),
):
    """Execute a workflow and wait for the result."""
    try:
        workflow = await workflow_service.get_workflow(execution_data.project_id, user_id)
        if not workflow:
            return StandardResponse(
                code=ResCode.ERR,
                msg=f"Workflow with project_id {execution_data.project_id} not found",
                data=None,
            )
        execution_data.project_id = workflow.project_id
        logger.info("[execute_workflow] project_id: %s", execution_data.project_id)
        if not execution_data.version:
            execution_data.version = workflow.version

        execution = await execution_service.execute_workflow(
            execution_data=execution_data,
            user_id=user_id,
            wait=True,
            workflow_timeout=600,
        )

        if execution.status == ExecutionStatus.RUNNING.value:
            return StandardResponse(
                code=ResCode.SUCCESS,
                msg="Execution is still in progress, please check status using execution ID",
                data={"execution": execution.to_dict()},
            )

        return StandardResponse(code=ResCode.SUCCESS, msg="", data={"execution": execution.to_dict()})
    except Exception as e:
        logger.error("Error executing workflow %s: %s", execution_data.project_id, str(e))
        return StandardResponse(code=ResCode.ERR, msg="Failed to execute workflow", data=None)


@router.post(
    "/execute-async",
    response_model=StandardResponse,
    status_code=status.HTTP_202_ACCEPTED,
    summary="Execute workflow asynchronously",
    description="Execute a workflow asynchronously and return an execution ID.",
)
async def execute_workflow_async(
    execution_data: ExecutionCreate,
    user_id: str = Depends(get_user_id_from_api_key),
    user_service: UserService = Depends(get_user_service),
    workflow_service: WorkflowService = Depends(get_workflow_service),
    execution_service: ExecutionService = Depends(get_execution_service),
):
    """Execute a workflow asynchronously."""
    try:
        if execution_data.phone_number:
            user_info = await user_service.get_user_info(execution_data.phone_number)
            if not user_info:
                logger.error("Failed to load user API key for copied workflow execution")
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Failed to load user API key",
                )

            sub_user_id = user_info.get("user_id")

            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.post(
                    get_robot_service_url("/api/robot/astron-agent/copy-robot"),
                    json={
                        "robotId": str(execution_data.project_id),
                        "version": execution_data.version,
                        "targetPhone": execution_data.phone_number,
                    },
                    headers=get_internal_headers(),
                )
                if response.status_code == 200:
                    result = response.json().get("data")
                    logger.info("Workflow copy response received")
                    if not result:
                        return StandardResponse(
                            code=ResCode.ERR,
                            msg=response.json().get("message"),
                            data=None,
                        )
                else:
                    logger.error("Failed to copy workflow: HTTP %s, %s", response.status_code, response.text[:500])
                    return StandardResponse(code=ResCode.ERR, msg="Failed to request backend workflow service", data=None)

            workflow_data = WorkflowBase(
                project_id=result.get("robotId"),
                version=result.get("version"),
                name=result.get("name", ""),
                english_name=result.get("english_name", ""),
                description=result.get("description", ""),
                status=result.get("status", 1),
                parameters=json.dumps(result.get("parameters", []), ensure_ascii=False),
            )

            existing_workflow = await workflow_service.get_workflow(workflow_data.project_id)

            if existing_workflow:
                if existing_workflow.user_id != sub_user_id:
                    return StandardResponse(
                        code=ResCode.ERR,
                        msg=f"Project ID '{workflow_data.project_id}' already exists and belongs to another user",
                        data=None,
                    )

                workflow = await workflow_service.update_workflow(workflow_data, sub_user_id)
            else:
                workflow = await workflow_service.create_workflow(workflow_data, sub_user_id)

            user_id = sub_user_id
            execution_data.project_id = workflow.project_id
            execution_data.version = None

        else:
            workflow = await workflow_service.get_workflow(execution_data.project_id, user_id)
            if not workflow:
                return StandardResponse(
                    code=ResCode.ERR,
                    msg=f"Workflow with project_id {execution_data.project_id} not found",
                    data=None,
                )

            if not execution_data.version:
                execution_data.version = workflow.version

            execution_data.project_id = workflow.project_id
            logger.info("[execute_workflow_async] project_id: %s", execution_data.project_id)

        execution = await execution_service.execute_workflow(
            execution_data=execution_data,
            user_id=user_id,
            wait=False,
            workflow_timeout=36000,
        )

        return StandardResponse(code=ResCode.SUCCESS, msg="", data={"executionId": execution.id})
    except Exception as e:
        logger.error("Error executing workflow async %s: %s", execution_data.project_id, str(e))
        return StandardResponse(code=ResCode.ERR, msg="Failed to execute workflow asynchronously", data=None)


@router.post(
    "/stop-current",
    response_model=StandardResponse,
    status_code=status.HTTP_202_ACCEPTED,
    summary="Stop current workflow",
    description="Stop the currently running workflow.",
)
async def stop_current_workflow(user_id: str = Depends(get_user_id_from_api_key)):
    """Stop the currently running workflow."""
    import asyncio

    from rpawebsocket.ws import BaseMsg

    from app.dependencies import get_ws_service

    try:
        websocket_service = await get_ws_service()
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
                logger.info("Received response for stop_current with code %s", res.get("code"))
            if e:
                res_e = e
                logger.error("Received error for stop_current: %s", e)
            wait.set()

        base_msg = BaseMsg(
            channel="remote",
            key="stop_current",
            uuid="$root$",
            send_uuid=f"{user_id}",
            need_reply=True,
            data={},
        ).init()

        await websocket_service.ws_manager.send_reply(base_msg, 10 * 3600, callback)

        await wait.wait()

        if res.get("code") == "0000":
            return StandardResponse(code=ResCode.SUCCESS, msg="Stopped", data={})
        elif res.get("code") == "5001":
            return StandardResponse(
                code=ResCode.ERR,
                msg="No active workflow could be stopped",
                data=str(res_e) if res_e else None,
            )
        return StandardResponse(code=ResCode.ERR, msg="Unexpected stop response", data=res)

    except Exception as e:
        logger.exception("Error in stop_current for %s", e)
        raise


@router.get(
    "/get-astron",
    response_model=StandardResponse,
    summary="List ShopRPA Agent workflows",
    description="List workflows available through saved ShopRPA Agent credentials.",
)
async def get_astron_workflows(
    user_id: str = Depends(get_user_id_from_header),
    api_key_service: ShoprpaApiKeyService = Depends(get_astron_api_key_service),
    workflow_service: WorkflowService = Depends(get_workflow_service),
):
    """List ShopRPA Agent workflows."""
    try:
        astron_auths = await api_key_service.get_all_astron_agents(user_id)
        total_workflows = []
        for astron_record in astron_auths:
            auth_id = astron_record.get("id")
            app_id = astron_record.get("app_id")
            api_key = astron_record.get("api_key")
            api_secret = astron_record.get("api_secret")
            workflows = await workflow_service.get_astron_workflows(auth_id, app_id, api_key, api_secret)
            total_workflows.extend(workflows)

        return StandardResponse(
            code=ResCode.SUCCESS, msg="Loaded", data={"total": len(total_workflows), "records": total_workflows}
        )
    except Exception as e:
        logger.error("Error getting ShopRPA Agent workflows: %s", str(e))
        return StandardResponse(code=ResCode.ERR, msg="Failed to get ShopRPA Agent workflows", data=None)


@router.post(
    "/copy-workflow",
    response_model=StandardResponse,
    status_code=status.HTTP_200_OK,
    summary="Copy workflow",
    description="Copy a workflow to the user identified by phone number.",
)
async def copy_workflow(
    copy_data: WorkflowCopyRequest,
    user_id: str = Depends(get_user_id_from_api_key),
):
    """Copy a workflow."""
    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                get_robot_service_url("/api/robot/astron-agent/copy-robot"),
                json={
                    "robotId": str(copy_data.project_id),
                    "version": copy_data.version,
                    "targetPhone": copy_data.phone_number,
                },
                headers=get_internal_headers(),
            )

            if response.status_code == 200:
                result = response.json()
                return StandardResponse(code=ResCode.SUCCESS, msg="Workflow copied", data=result)
            else:
                logger.error("Failed to copy workflow: HTTP %s, %s", response.status_code, response.text[:500])
                return StandardResponse(code=ResCode.ERR, msg=f"Copy failed: HTTP {response.status_code}", data=None)

    except httpx.RequestError as e:
        logger.error("Request error copying workflow: %s", str(e))
        return StandardResponse(code=ResCode.ERR, msg="Network request failed", data=None)
    except Exception as e:
        logger.error("Error copying workflow: %s", str(e))
        return StandardResponse(code=ResCode.ERR, msg="Failed to copy workflow", data=None)
