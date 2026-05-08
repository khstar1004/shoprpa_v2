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
    summary="List execution records",
    description="List execution records for the user identified by the API key.",
)
async def get_executions(
    pageNo: int = Query(1, ge=1, description="Page number"),
    pageSize: int = Query(10, ge=1, le=100, description="Page size"),
    user_id: str = Depends(get_user_id_from_api_key),
    service: ExecutionService = Depends(get_execution_service),
):
    """List execution records."""
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
                "total_pages": (total + pageSize - 1) // pageSize,
            },
        )
    except Exception as e:
        logger.error("Error getting executions for user %s: %s", user_id, str(e))
        return StandardResponse(code=ResCode.ERR, msg="Failed to get executions", data=None)


@router.get(
    "/{execution_id}",
    response_model=StandardResponse,
    summary="Get execution record",
    description="Get the status and result for a workflow execution.",
)
async def get_execution(
    execution_id: str = Path(..., description="Execution record ID"),
    user_id: str = Depends(get_user_id_from_api_key),
    service: ExecutionService = Depends(get_execution_service),
):
    """Get an execution record."""
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
        logger.error("Error getting execution %s: %s", execution_id, str(e))
        return StandardResponse(code=ResCode.ERR, msg="Failed to get execution", data=None)
