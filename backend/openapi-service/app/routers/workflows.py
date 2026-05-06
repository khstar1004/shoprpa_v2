import json

import httpx
from fastapi import APIRouter, Depends, HTTPException, Path, Query, status

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


@router.post(
    "/upsert",
    response_model=StandardResponse,
    status_code=status.HTTP_200_OK,
    summary="생성또는수정워크플로",
    description="결과가 project_id 존재하지 않음이면생성새워크플로, 결과가존재함이면업데이트있음워크플로",
)
async def create_or_update_workflow(
    workflow_data: WorkflowBase,
    user_id: str = Depends(get_user_id_from_header),
    service: WorkflowService = Depends(get_workflow_service),
):
    """생성또는수정워크플로"""
    try:
        # 조회여부완료저장된  project_id 의워크플로
        existing_workflow = await service.get_workflow(str(workflow_data.project_id))

        if existing_workflow:
            # 결과가존재함, 조회여부현재사용자
            if existing_workflow.user_id != user_id:
                return StandardResponse(
                    code=ResCode.ERR,
                    msg=f"Project ID '{workflow_data.project_id}' already exists and belongs to another user",
                    data=None,
                )

            workflow = await service.update_workflow(workflow_data, user_id)
            action = "updated"
        else:
            # 생성새워크플로
            workflow = await service.create_workflow(workflow_data, user_id)
            action = "created"

        # 변환로가능순서열의딕셔너리
        workflow_dict = workflow.to_dict()

        return StandardResponse(
            code=ResCode.SUCCESS,
            msg=f"Workflow {action} successfully",
            data={"workflow": workflow_dict, "action": action},
        )
    except Exception as e:
        logger.error(f"Error creating/updating workflow: {str(e)}")
        return StandardResponse(code=ResCode.ERR, msg="Failed to create or update workflow", data=None)


@router.get(
    "/get",
    response_model=StandardResponse,
    summary="가져오기모든워크플로",
    description="가져오기현재사용자의모든워크플로목록",
)
async def get_workflows(
    pageNo: int = Query(1, ge=1, description="가져오기 일"),
    pageSize: int = Query(100, ge=1, le=100, description="일있음다중적음기록"),
    user_id: str = Depends(get_user_id_with_fallback),
    service: WorkflowService = Depends(get_workflow_service),
):
    """가져오기워크플로목록"""
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
        logger.error(f"Error getting workflows: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to get workflows",
        )


@router.get(
    "/get/{project_id}",
    response_model=StandardResponse,
    summary="가져오기 지정워크플로",
    description="가져오기 지정project_id의워크플로정보",
)
async def get_workflow(
    project_id: str = Path(..., description="목록ID"),
    service: WorkflowService = Depends(get_workflow_service),
):
    """가져오기워크플로"""
    try:
        workflow = await service.get_workflow(project_id, None)
        if not workflow:
            # 수정성공성공반환code, 프론트엔드관리
            return StandardResponse(
                code=ResCode.SUCCESS,
                msg=f"Workflow with project_id {project_id} not found",
                data=None,
            )
        workflow_dict = workflow.to_dict()
        return StandardResponse(code=ResCode.SUCCESS, msg="", data={"workflow": workflow_dict})
    except Exception as e:
        logger.error(f"Error getting workflow {project_id}: {str(e)}")
        return StandardResponse(code=ResCode.SUCCESS, msg="Failed to get workflow", data=None)


@router.post(
    "/execute",
    response_model=StandardResponse,
    summary="실행워크플로(대기결과)",
    description="실행지정의워크플로, 대기실행 결과.결과가실행시간경과길이, 반환202상태코드, 생성사용예외연결",
)
async def execute_workflow(
    execution_data: ExecutionCreate,
    user_id: str = Depends(get_user_id_from_api_key),
    workflow_service: WorkflowService = Depends(get_workflow_service),
    execution_service: ExecutionService = Depends(get_execution_service),
):
    """실행워크플로"""
    try:
        # 조회워크플로여부존재함
        workflow = await workflow_service.get_workflow(execution_data.project_id, user_id)
        if not workflow:
            return StandardResponse(
                code=ResCode.ERR,
                msg=f"Workflow with project_id {execution_data.project_id} not found",
                data=None,
            )
        execution_data.project_id = workflow.project_id
        logger.info(f"[execute_workflow] project_id: {execution_data.project_id}")
        # 사용workflowversion
        if not execution_data.version:
            execution_data.version = workflow.version

        # 실행워크플로, 시간 초과매개변수
        execution = await execution_service.execute_workflow(
            execution_data=execution_data,
            user_id=user_id,
            wait=True,
            workflow_timeout=600,  # 워크플로실행시간 초과10분(실행)
        )

        # 결과가실행중있음완료(상태로RUNNING), 반환202
        if execution.status == ExecutionStatus.RUNNING.value:
            return StandardResponse(
                code=ResCode.SUCCESS,
                msg="Execution is still in progress, please check status using execution ID",
                data={"execution": execution.to_dict()},
            )

        return StandardResponse(code=ResCode.SUCCESS, msg="", data={"execution": execution.to_dict()})
    except Exception as e:
        logger.error(f"Error executing workflow {execution_data.project_id}: {str(e)}")
        return StandardResponse(code=ResCode.ERR, msg="Failed to execute workflow", data=None)


@router.post(
    "/execute-async",
    response_model=StandardResponse,
    status_code=status.HTTP_202_ACCEPTED,
    summary="예외실행워크플로(반환작업ID)",
    description="예외실행지정의워크플로, 반환실행ID, 가능통신경과실행ID조회실행상태",
)
async def execute_workflow_async(
    execution_data: ExecutionCreate,
    user_id: str = Depends(get_user_id_from_api_key),
    user_service: UserService = Depends(get_user_service),
    workflow_service: WorkflowService = Depends(get_workflow_service),
    execution_service: ExecutionService = Depends(get_execution_service),
):
    """예외실행워크플로"""
    try:
        if execution_data.phone_number:
            # Agent복사
            # 호출외부모듈서비스가져오기user_id
            user_info = await user_service.get_user_info(execution_data.phone_number)
            if not user_info:
                logger.error(f"사용자가져오기API_KEY실패, phone: {execution_data.phone_number}")
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="사용자가져오기API_KEY실패",
                )

            # 복사까지 sub_user_id
            sub_user_id = user_info.get("user_id")

            # 호출외부모듈서비스행복사
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.post(
                    "http://robot-service:8004/api/robot/astron-agent/copy-robot",
                    json={
                        "robotId": str(execution_data.project_id),
                        "version": execution_data.version,
                        "targetPhone": execution_data.phone_number,
                    },
                    headers={"X-API-Key": "opensource666!"},
                )
                if response.status_code == 200:
                    result = response.json().get("data")
                    logger.info(f"복사워크플로결과: {result}")
                    if not result:
                        return StandardResponse(
                            code=ResCode.ERR,
                            msg=response.json().get("message"),
                            data=None,
                        )
                else:
                    logger.error(f"Failed to copy workflow: HTTP {response.status_code}, {response.text}")
                    return StandardResponse(code=ResCode.ERR, msg="요청 백엔드워크플로연결실패", data=None)

            workflow_data = WorkflowBase(
                project_id=result.get("robotId"),
                version=result.get("version"),
                name=result.get("name", ""),
                english_name=result.get("english_name", ""),
                description=result.get("description", ""),
                status=result.get("status", 1),
                parameters=json.dumps(result.get("parameters", []), ensure_ascii=False),
            )

            # 조회여부완료저장된  project_id 의워크플로
            existing_workflow = await workflow_service.get_workflow(workflow_data.project_id)

            if existing_workflow:
                # 결과가존재함, 조회여부현재사용자
                if existing_workflow.user_id != sub_user_id:
                    return StandardResponse(
                        code=ResCode.ERR,
                        msg=f"Project ID '{workflow_data.project_id}' already exists and belongs to another user",
                        data=None,
                    )

                workflow = await workflow_service.update_workflow(workflow_data, sub_user_id)
            else:
                # 생성새워크플로
                workflow = await workflow_service.create_workflow(workflow_data, sub_user_id)

            user_id = sub_user_id
            execution_data.project_id = workflow.project_id
            # 복사의아니오지정version, 지정완료아래발송까지실행기기런타임오류
            execution_data.version = None

        else:
            # 조회워크플로여부존재함
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
            logger.info(f"[execute_workflow_async] project_id: {execution_data.project_id}")

        # 실행워크플로, 아니오대기결과
        execution = await execution_service.execute_workflow(
            execution_data=execution_data,
            user_id=user_id,
            wait=False,
            workflow_timeout=36000,  # 워크플로실행시간 초과10시간
        )

        return StandardResponse(code=ResCode.SUCCESS, msg="", data={"executionId": execution.id})
    except Exception as e:
        logger.error(f"Error executing workflow async {execution_data.project_id}: {str(e)}")
        return StandardResponse(code=ResCode.ERR, msg="Failed to execute workflow asynchronously", data=None)


@router.post(
    "/stop-current",
    response_model=StandardResponse,
    status_code=status.HTTP_202_ACCEPTED,
    summary="중지현재워크플로",
    description="중지현재워크플로",
)
async def stop_current_workflow(user_id: str = Depends(get_user_id_from_api_key)):
    """중지현재워크플로"""
    import asyncio

    from rpawebsocket.ws import BaseMsg

    from app.dependencies import get_ws_service

    try:
        # 예외워크플로실행
        # 목록중가능호출외부모듈시스템, 관리데이터대기
        # 돌아가기조정파일
        websocket_service = await get_ws_service()
        logger.info("input user_id: %s", user_id)

        wait = asyncio.Event()
        res = {}
        res_e = None

        def callback(watch_msg: BaseMsg | None = None, e: Exception | None = None):
            nonlocal wait, res, res_e
            if watch_msg:
                res = watch_msg.data
                logger.info("Received response for stop_current: %s", res)
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

        # 대기
        await wait.wait()

        # 워크플로실행성공
        if res.get("code") == "0000":
            return StandardResponse(code=ResCode.SUCCESS, msg="중지성공", data={})
        elif res.get("code") == "5001":
            return StandardResponse(code=ResCode.ERR, msg="중지실패또는있음정상에서실행의봇", data=res_e)

    except Exception as e:
        logger.exception("Error in stop_current for %s", e)
        raise


@router.get(
    "/get-astron",
    response_model=StandardResponse,
    summary="가져오기ShoprpaAgent모든워크플로",
    description="가져오기 지정완료의ShoprpaAgent의모든워크플로목록",
)
async def get_astron_workflows(
    user_id: str = Depends(get_user_id_from_header),
    api_key_service: ShoprpaApiKeyService = Depends(get_astron_api_key_service),
    workflow_service: WorkflowService = Depends(get_workflow_service),
):
    """가져오기ShoprpaAgent모든워크플로"""
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
            code=ResCode.SUCCESS, msg="가져오기성공", data={"total": len(total_workflows), "records": total_workflows}
        )
    except Exception as e:
        logger.error(f"Error getting Shoprpa workflows: {str(e)}")
        return StandardResponse(code=ResCode.ERR, msg="Failed to get Shoprpa workflows", data=None)


@router.post(
    "/copy-workflow",
    response_model=StandardResponse,
    status_code=status.HTTP_200_OK,
    summary="복사워크플로",
    description="복사지정의워크플로까지목록 휴대폰 번호",
)
async def copy_workflow(
    copy_data: WorkflowCopyRequest,
    user_id: str = Depends(get_user_id_from_api_key),
):
    """복사워크플로"""
    try:
        # 호출외부모듈서비스행복사
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                "http://robot-service:8004/api/astron-agent/copy-robot",
                json={
                    "robotId": str(copy_data.project_id),
                    "version": copy_data.version,
                    "targetPhone": copy_data.phone_number,
                },
            )

            if response.status_code == 200:
                result = response.json()
                return StandardResponse(code=ResCode.SUCCESS, msg="워크플로복사성공", data=result)
            else:
                logger.error(f"Failed to copy workflow: HTTP {response.status_code}, {response.text}")
                return StandardResponse(code=ResCode.ERR, msg=f"복사실패: HTTP {response.status_code}", data=None)

    except httpx.RequestError as e:
        logger.error(f"Request error copying workflow: {str(e)}")
        return StandardResponse(code=ResCode.ERR, msg="네트워크 요청실패", data=None)
    except Exception as e:
        logger.error(f"Error copying workflow: {str(e)}")
        return StandardResponse(code=ResCode.ERR, msg="복사워크플로실패", data=None)