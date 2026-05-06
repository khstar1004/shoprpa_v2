from fastapi import APIRouter, Depends, Path, Query

from app.dependencies import get_execution_service, get_user_id_from_api_key
from app.logger import get_logger
from app.schemas import ResCode, StandardResponse
from app.services.execution import ExecutionService

logger = get_logger(__name__)

router = APIRouter(
    prefix="/executions",
    tags=["executions"],
)


@router.get(
    "/get",
    response_model=StandardResponse,
    summary="가져오기실행기록목록(분)",
    description="근거API_KEY가져오기사용자ID, 후분가져오기해당사용자의모든실행기록",
)
async def get_executions(
    pageNo: int = Query(1, ge=1, description="가져오기 일"),
    pageSize: int = Query(10, ge=1, le=100, description="일있음다중적음기록"),
    user_id: str = Depends(get_user_id_from_api_key),
    service: ExecutionService = Depends(get_execution_service),
):
    """분가져오기실행기록목록"""
    try:
        executions, total = await service.get_executions_by_user(user_id, pageNo, pageSize)
        executions_dict = [execution.to_dict() for execution in executions]

        return StandardResponse(
            code=ResCode.SUCCESS,
            msg="",
            data={
                "executions": executions_dict,
                "total": total,
                "pageNo": pageNo,
                "pageSize": pageSize,
                "total_pages": (total + pageSize - 1) // pageSize,  # 위가져오기 계획데이터
            },
        )
    except Exception as e:
        logger.error(f"Error getting executions for user {user_id}: {str(e)}")
        return StandardResponse(code=ResCode.ERR, msg="Failed to get executions", data=None)


@router.get(
    "/{execution_id}",
    response_model=StandardResponse,
    summary="조회예외실행의정도및결과",
    description="조회워크플로실행의상태및결과",
)
async def get_execution(
    execution_id: str = Path(..., description="실행기록ID"),
    user_id: str = Depends(get_user_id_from_api_key),
    service: ExecutionService = Depends(get_execution_service),
):
    """가져오기실행기록"""
    try:
        execution = await service.get_execution(execution_id, user_id)
        if not execution:
            return StandardResponse(
                code=ResCode.ERR,
                msg=f"Execution with ID {execution_id} not found",
                data=None,
            )

        return StandardResponse(code=ResCode.SUCCESS, msg="", data={"execution": execution.to_dict()})
    except Exception as e:
        logger.error(f"Error getting execution {execution_id}: {str(e)}")
        return StandardResponse(code=ResCode.ERR, msg="Failed to get execution", data=None)