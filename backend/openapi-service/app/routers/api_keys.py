from fastapi import APIRouter, Depends, HTTPException, Query, status

from app.dependencies import get_api_key_service, get_astron_api_key_service, get_user_id_from_header
from app.logger import get_logger
from app.schemas import ResCode, StandardResponse
from app.schemas.api_key import ApiKeyCreate, ApiKeyDelete, ShoprpaAgentCreate, ShoprpaAgentDelete, ShoprpaAgentUpdate
from app.services.api_key import ApiKeyService, ShoprpaApiKeyService

logger = get_logger(__name__)

router = APIRouter(
    prefix="/api-keys",
    tags=["api-key"],
    dependencies=[Depends(get_user_id_from_header)],
)


@router.get(
    "/get",
    response_model=StandardResponse,
    summary="List API keys",
    description="List API keys owned by the current user.",
)
async def get_api_keys(
    pageNo: int = Query(1, ge=1, description="Page number"),
    pageSize: int = Query(50, ge=1, le=50, description="Page size"),
    user_id: str = Depends(get_user_id_from_header),
    service: ApiKeyService = Depends(get_api_key_service),
):
    """List API keys."""
    try:
        api_keys = await service.get_api_keys(user_id, pageNo, pageSize)
        return StandardResponse(code=ResCode.SUCCESS, msg="", data={"total": len(api_keys), "records": api_keys})
    except Exception as e:
        logger.error("Error getting API keys: %s", str(e))
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Failed to get API keys")


@router.post(
    "/create",
    response_model=StandardResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create API key",
    description="Create a new API key for the current user.",
)
async def create_api_key(
    api_key_data: ApiKeyCreate,
    user_id: str = Depends(get_user_id_from_header),
    service: ApiKeyService = Depends(get_api_key_service),
):
    """Create an API key."""
    try:
        api_key = await service.create_api_key(api_key_data, user_id)
        return StandardResponse(code=ResCode.SUCCESS, msg="", data={"api_key": api_key})
    except Exception as e:
        logger.error("Error creating API key: %s", str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to create API key",
        )


@router.post(
    "/remove",
    response_model=StandardResponse,
    status_code=status.HTTP_200_OK,
    summary="Delete API key",
    description="Delete an API key owned by the current user.",
)
async def delete_api_key(
    request: ApiKeyDelete,
    user_id: str = Depends(get_user_id_from_header),
    service: ApiKeyService = Depends(get_api_key_service),
):
    """Delete an API key."""
    api_key_id = str(request.id)
    try:
        success = await service.delete_api_key(str(api_key_id), user_id)
        if not success:
            return StandardResponse(
                code=ResCode.ERR,
                msg=f"API key with ID {api_key_id} not found",
                data=None,
            )

        return StandardResponse(code=ResCode.SUCCESS, msg="", data=None)
    except Exception as e:
        logger.error("Error deleting API key %s: %s", api_key_id, str(e))
        return StandardResponse(code=ResCode.ERR, msg="Failed to delete API key", data=None)


@router.post(
    "/create-astron",
    response_model=StandardResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create ShopRPA Agent credential",
    description="Create a ShopRPA Agent credential for the current user.",
)
async def create_astron_agent(
    astron_agent_data: ShoprpaAgentCreate,
    user_id: str = Depends(get_user_id_from_header),
    service: ShoprpaApiKeyService = Depends(get_astron_api_key_service),
):
    """Create a ShopRPA Agent credential."""
    try:
        if not astron_agent_data.api_key or not astron_agent_data.api_secret:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="api_key and api_secret are required",
            )

        # GET http://dev-agent.xfyun.cn/xingchen-api/manage/workflow/get_info
        # X-Consumer-Username [appId]
        # Authorization [Bearer api_key:api_secret]

        astron_auth = await service.create_astron_agent(astron_agent_data, user_id)

        return StandardResponse(
            code=ResCode.SUCCESS,
            msg="ShopRPA Agent credential created",
            data={
                "id": astron_auth.id,
                "created_at": astron_auth.created_at,
                "updated_at": astron_auth.updated_at,
            },
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.error("Error creating ShopRPA Agent credential: %s", str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to create ShopRPA Agent credential",
        )


@router.get(
    "/get-astron",
    response_model=StandardResponse,
    summary="List ShopRPA Agent credentials",
    description="List ShopRPA Agent credentials owned by the current user.",
)
async def get_astron_agents(
    pageNo: int = Query(1, ge=1, description="Page number"),
    pageSize: int = Query(10, ge=1, le=50, description="Page size"),
    user_id: str = Depends(get_user_id_from_header),
    service: ShoprpaApiKeyService = Depends(get_astron_api_key_service),
):
    """List ShopRPA Agent credentials."""
    try:
        astron_agents = await service.get_astron_agents(user_id, pageNo, pageSize)
        return StandardResponse(
            code=ResCode.SUCCESS, msg="Loaded", data={"total": len(astron_agents), "records": astron_agents}
        )
    except Exception as e:
        logger.error("Error getting ShopRPA Agent credentials: %s", str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to get ShopRPA Agent credentials",
        )


@router.get(
    "/get-astron-by-id",
    response_model=StandardResponse,
    summary="Get ShopRPA Agent credential",
    description="Get a ShopRPA Agent credential by id.",
)
async def get_astron_agent_by_id(
    id: int = Query(..., description="ShopRPA Agent credential id"),
    user_id: str = Depends(get_user_id_from_header),
    service: ShoprpaApiKeyService = Depends(get_astron_api_key_service),
):
    """Get a ShopRPA Agent credential by id."""
    try:
        astron_agent = await service.get_astron_agent_by_id(id, user_id)
        if astron_agent is None:
            return StandardResponse(
                code=ResCode.ERR,
                msg=f"ShopRPA Agent credential with ID {id} not found",
                data=None,
            )

        return StandardResponse(code=ResCode.SUCCESS, msg="Loaded", data=astron_agent)
    except Exception as e:
        logger.error("Error getting ShopRPA Agent credential by id %s: %s", id, str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to get ShopRPA Agent credential",
        )


@router.post(
    "/remove-astron",
    response_model=StandardResponse,
    status_code=status.HTTP_200_OK,
    summary="Delete ShopRPA Agent credential",
    description="Delete a ShopRPA Agent credential owned by the current user.",
)
async def delete_astron_agent(
    request: ShoprpaAgentDelete,
    user_id: str = Depends(get_user_id_from_header),
    service: ShoprpaApiKeyService = Depends(get_astron_api_key_service),
):
    """Delete a ShopRPA Agent credential."""
    astron_agent_id = str(request.id)
    try:
        success = await service.delete_astron_agent(astron_agent_id, user_id)
        if not success:
            return StandardResponse(
                code=ResCode.ERR,
                msg=f"ShopRPA Agent credential with ID {astron_agent_id} not found",
                data=None,
            )

        return StandardResponse(code=ResCode.SUCCESS, msg="Deleted", data=None)
    except Exception as e:
        logger.error("Error deleting ShopRPA Agent credential %s: %s", astron_agent_id, str(e))
        return StandardResponse(code=ResCode.ERR, msg="Failed to delete ShopRPA Agent credential", data=None)


@router.post(
    "/update-astron",
    response_model=StandardResponse,
    status_code=status.HTTP_200_OK,
    summary="Update ShopRPA Agent credential",
    description="Update a ShopRPA Agent credential owned by the current user.",
)
async def update_astron_agent(
    request: ShoprpaAgentUpdate,
    user_id: str = Depends(get_user_id_from_header),
    service: ShoprpaApiKeyService = Depends(get_astron_api_key_service),
):
    """Update a ShopRPA Agent credential."""
    astron_agent_id = str(request.id)
    try:
        success = await service.update_astron_agent(astron_agent_id, user_id, request)
        if not success:
            return StandardResponse(
                code=ResCode.ERR,
                msg=f"ShopRPA Agent credential with ID {astron_agent_id} not found",
                data=None,
            )

        return StandardResponse(code=ResCode.SUCCESS, msg="Updated", data=None)
    except Exception as e:
        logger.error("Error updating ShopRPA Agent credential %s: %s", astron_agent_id, str(e))
        return StandardResponse(code=ResCode.ERR, msg="Failed to update ShopRPA Agent credential", data=None)
