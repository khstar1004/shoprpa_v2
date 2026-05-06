from fastapi import APIRouter, Depends, HTTPException, status

from app.dependencies import get_user_service, verify_getkey_bearer_token, verify_register_bearer_token
from app.logger import get_logger
from app.schemas import ResCode, StandardResponse
from app.schemas.user import UserAPIKeyResponse, UserRegisterRequest
from app.services.user import UserService

logger = get_logger(__name__)


router = APIRouter(
    prefix="/users",
    tags=["user"],
)


@router.post(
    "/register",
    response_model=StandardResponse,
    status_code=status.HTTP_200_OK,
    summary="사용자회원가입",
    description="근거휴대폰 번호행사용자회원가입, 반환api_key, 비밀번호및다운로드연결",
)
async def register_user(
    request: UserRegisterRequest,
    service: UserService = Depends(get_user_service),
    token: str = Depends(verify_register_bearer_token),
):
    """가져오기API_KEY연결"""
    try:
        phone = request.phone
        logger.info(f"사용자가져오기API_KEY연결요청 , phone: {phone}")

        # 호출서비스행가져오기
        result = await service.get_user_info(phone)

        if not result:
            logger.error(f"사용자가져오기API_KEY실패, phone: {phone}")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="사용자미완료회원가입",
            )

        # 생성반환데이터
        response_data = UserAPIKeyResponse(user_id=result.get("user_id") or "", api_key=result.get("api_key") or "")

        logger.info(f"사용자회원가입성공, phone: {phone}, user_id: {result.get('user_id')}")

        return StandardResponse(
            code=ResCode.SUCCESS,
            msg="가져오기API_KEY성공",
            data=response_data.model_dump(),
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"사용자가져오기API_KEY경과중출력오류: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="서비스서버내부 오류",
        )


@router.post(
    "/get-key",
    response_model=StandardResponse,
    status_code=status.HTTP_200_OK,
    summary="사용자가져오기API_KEY",
    description="근거휴대폰 번호가져오기API_KEY",
)
async def get_user_api_key(
    request: UserRegisterRequest,
    service: UserService = Depends(get_user_service),
    token: str = Depends(verify_getkey_bearer_token),
):
    """가져오기API_KEY연결"""
    try:
        phone = request.phone
        logger.info(f"사용자가져오기API_KEY연결요청 , phone: {phone}")

        # 호출서비스행가져오기
        result = await service.get_user_info(phone)

        if not result:
            logger.error(f"사용자가져오기API_KEY실패, phone: {phone}")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="사용자미완료회원가입",
            )

        # 생성반환데이터
        response_data = UserAPIKeyResponse(user_id=result.get("user_id") or "", api_key=result.get("api_key") or "")

        logger.info(f"사용자가져오기API_KEY성공, phone: {phone}, user_id: {result.get('user_id')}")

        return StandardResponse(
            code=ResCode.SUCCESS,
            msg="가져오기API_KEY성공",
            data=response_data.model_dump(),
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"사용자가져오기API_KEY경과중출력오류: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="서비스서버내부 오류",
        )