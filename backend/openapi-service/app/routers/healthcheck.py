from fastapi import APIRouter, Depends, status

from app.dependencies import check_user_id_equality, get_user_id_from_api_key
from app.logger import get_logger
from app.schemas import ResCode, StandardResponse

logger = get_logger(__name__)

router = APIRouter(
    prefix="/health",
    tags=["health"],
)


@router.get("", status_code=status.HTTP_200_OK, summary="Service health")
async def health():
    return {"service": "ShopRPA OpenAPI Service", "status": "ok"}


@router.get(
    "/local-check",
    response_model=StandardResponse,
    status_code=status.HTTP_200_OK,
    summary="본조회",
    description="호출본경로 , 조회본경로 의user_idAPI_KEY여부매칭",
)
async def local_check(
    equality: bool = Depends(check_user_id_equality),
):
    """조회연결"""
    if equality:
        logger.info("본RPA보통시작")

        return StandardResponse(
            code=ResCode.SUCCESS,
            msg="본RPA보통시작",
            data={"equality": equality},
        )
    else:
        return StandardResponse(
            code=ResCode.ERR,
            msg="본RPA시작계정및Agent계정아니오매칭",
            data={"equality": equality},
        )


@router.get(
    "/remote-check",
    response_model=StandardResponse,
    status_code=status.HTTP_200_OK,
    summary="단말조회",
    description="조회API_KEY의user여부클라이언트에서",
)
async def remote_check(
    user_id: str = Depends(get_user_id_from_api_key),
):
    """조회연결"""

    from app.dependencies import get_ws_service

    websocket_service = await get_ws_service()
    if user_id not in websocket_service.ws_manager.conns:
        return StandardResponse(
            code=ResCode.ERR,
            msg="해당사용자클라이언트미완료시작!",
            data=False,
        )
    else:
        return StandardResponse(
            code=ResCode.SUCCESS,
            msg="해당사용자클라이언트보통시작!",
            data=True,
        )
