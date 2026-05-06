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
    "/get", response_model=StandardResponse, summary="가져오기모든 API Key", description="가져오기현재사용자의모든 API Key 목록"
)
async def get_api_keys(
    pageNo: int = Query(1, ge=1, description="가져오기 일"),
    pageSize: int = Query(100, ge=1, le=50, description="일있음다중적음기록"),
    user_id: str = Depends(get_user_id_from_header),
    service: ApiKeyService = Depends(get_api_key_service),
):
    """가져오기 API Key 목록"""
    try:
        api_keys = await service.get_api_keys(user_id, pageNo, pageSize)
        return StandardResponse(code=ResCode.SUCCESS, msg="", data={"total": len(api_keys), "records": api_keys})
    except Exception as e:
        logger.error(f"Error getting API keys: {str(e)}")
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Failed to get API keys")


@router.post(
    "/create",
    response_model=StandardResponse,
    status_code=status.HTTP_201_CREATED,
    summary="생성새의 API Key",
    description="로현재사용자생성새의 API Key",
)
async def create_api_key(
    api_key_data: ApiKeyCreate,
    user_id: str = Depends(get_user_id_from_header),
    service: ApiKeyService = Depends(get_api_key_service),
):
    """생성 API Key"""
    try:
        api_key = await service.create_api_key(api_key_data, user_id)
        return StandardResponse(code=ResCode.SUCCESS, msg="", data={"api_key": api_key})
    except Exception as e:
        logger.error(f"Error creating API key: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to create API key",
        )


@router.post(
    "/remove",
    response_model=StandardResponse,
    status_code=status.HTTP_200_OK,
    summary="삭제지정 API Key",
    description="삭제지정의 API Key",
)
async def delete_api_key(
    request: ApiKeyDelete,
    user_id: str = Depends(get_user_id_from_header),
    service: ApiKeyService = Depends(get_api_key_service),
):
    """삭제 API Key"""
    try:
        api_key_id = int(request.id)  # 변환로 int 유형
        success = await service.delete_api_key(str(api_key_id), user_id)
        if not success:
            return StandardResponse(
                code=ResCode.ERR,
                msg=f"API key with ID {api_key_id} not found",
                data=None,
            )

        return StandardResponse(code=ResCode.SUCCESS, msg="", data=None)
    except Exception as e:
        logger.error(f"Error deleting API key {api_key_id}: {str(e)}")
        return StandardResponse(code=ResCode.ERR, msg="Failed to delete API key", data=None)


@router.post(
    "/create-astron",
    response_model=StandardResponse,
    status_code=status.HTTP_201_CREATED,
    summary="생성ShoprpaAgent",
    description="로현재사용자생성ShoprpaAgent",
)
async def create_astron_agent(
    astron_agent_data: ShoprpaAgentCreate,
    user_id: str = Depends(get_user_id_from_header),
    service: ShoprpaApiKeyService = Depends(get_astron_api_key_service),
):
    """생성ShoprpaAgent"""
    try:
        # 인증데이터
        if not astron_agent_data.api_key or not astron_agent_data.api_secret:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="api_key 및 api_secret 비워 둘 수 없습니다",
            )

        # GET http://dev-agent.xfyun.cn/xingchen-api/manage/workflow/get_info
        # X-Consumer-Username [appId]
        # Authorization [Bearer api_key:api_secret]

        # 호출서비스생성ShoprpaAgent
        astron_auth = await service.create_astron_agent(astron_agent_data, user_id)

        return StandardResponse(
            code=ResCode.SUCCESS,
            msg="ShoprpaAgent권한 부여생성성공",
            data={
                "id": astron_auth.id,
                "created_at": astron_auth.created_at,
                "updated_at": astron_auth.updated_at,
            },
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error creating ShoprpaAgent: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="생성ShoprpaAgent인증실패",
        )


@router.get(
    "/get-astron",
    response_model=StandardResponse,
    summary="가져오기모든ShoprpaAgent",
    description="가져오기현재사용자의모든ShoprpaAgent목록",
)
async def get_astron_agents(
    pageNo: int = Query(1, ge=1, description="가져오기 일"),
    pageSize: int = Query(10, ge=1, le=50, description="일있음다중적음기록"),
    user_id: str = Depends(get_user_id_from_header),
    service: ShoprpaApiKeyService = Depends(get_astron_api_key_service),
):
    """가져오기ShoprpaAgent목록"""
    try:
        astron_agents = await service.get_astron_agents(user_id, pageNo, pageSize)
        return StandardResponse(
            code=ResCode.SUCCESS, msg="가져오기성공", data={"total": len(astron_agents), "records": astron_agents}
        )
    except Exception as e:
        logger.error(f"Error getting ShoprpaAgents: {str(e)}")
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Failed to get ShoprpaAgents")


@router.get(
    "/get-astron-by-id",
    response_model=StandardResponse,
    summary="근거ID가져오기ShoprpaAgent",
    description="근거ID가져오기 지정의ShoprpaAgent정보",
)
async def get_astron_agent_by_id(
    id: int = Query(..., description="ShoprpaAgent의ID"),
    user_id: str = Depends(get_user_id_from_header),
    service: ShoprpaApiKeyService = Depends(get_astron_api_key_service),
):
    """근거ID가져오기ShoprpaAgent"""
    try:
        astron_agent = await service.get_astron_agent_by_id(id, user_id)
        if astron_agent is None:
            return StandardResponse(
                code=ResCode.ERR,
                msg=f"ShoprpaAgent with ID {id} not found",
                data=None,
            )

        return StandardResponse(code=ResCode.SUCCESS, msg="가져오기성공", data=astron_agent)
    except Exception as e:
        logger.error(f"Error getting ShoprpaAgent by id {id}: {str(e)}")
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Failed to get ShoprpaAgent")


@router.post(
    "/remove-astron",
    response_model=StandardResponse,
    status_code=status.HTTP_200_OK,
    summary="삭제지정ShoprpaAgent",
    description="삭제지정의ShoprpaAgent",
)
async def delete_astron_agent(
    request: ShoprpaAgentDelete,
    user_id: str = Depends(get_user_id_from_header),
    service: ShoprpaApiKeyService = Depends(get_astron_api_key_service),
):
    """삭제ShoprpaAgent"""
    try:
        astron_agent_id = str(request.id)  # 변환로문자열유형
        success = await service.delete_astron_agent(astron_agent_id, user_id)
        if not success:
            return StandardResponse(
                code=ResCode.ERR,
                msg=f"ShoprpaAgent with ID {astron_agent_id} not found",
                data=None,
            )

        return StandardResponse(code=ResCode.SUCCESS, msg="삭제성공", data=None)
    except Exception as e:
        logger.error(f"Error deleting ShoprpaAgent {astron_agent_id}: {str(e)}")
        return StandardResponse(code=ResCode.ERR, msg="Failed to delete ShoprpaAgent", data=None)


@router.post(
    "/update-astron",
    response_model=StandardResponse,
    status_code=status.HTTP_200_OK,
    summary="업데이트지정ShoprpaAgent",
    description="업데이트지정의ShoprpaAgent정보",
)
async def update_astron_agent(
    request: ShoprpaAgentUpdate,
    user_id: str = Depends(get_user_id_from_header),
    service: ShoprpaApiKeyService = Depends(get_astron_api_key_service),
):
    """업데이트ShoprpaAgent"""
    astron_agent_id = str(request.id)  # 변환로문자열유형
    try:
        success = await service.update_astron_agent(astron_agent_id, user_id, request)
        if not success:
            return StandardResponse(
                code=ResCode.ERR,
                msg=f"ShoprpaAgent with ID {astron_agent_id} not found",
                data=None,
            )

        return StandardResponse(code=ResCode.SUCCESS, msg="업데이트성공", data=None)
    except Exception as e:
        logger.error(f"Error updating ShoprpaAgent {astron_agent_id}: {str(e)}")
        return StandardResponse(code=ResCode.ERR, msg="Failed to update ShoprpaAgent", data=None)