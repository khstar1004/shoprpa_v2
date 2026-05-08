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
    summary="Register user",
    description="Register a user by phone number and return the default API key.",
)
async def register_user(
    request: UserRegisterRequest,
    service: UserService = Depends(get_user_service),
    token: str = Depends(verify_register_bearer_token),
):
    """Register a user and return the default API key."""
    try:
        phone = request.phone
        logger.info("User registration requested")

        result = await service.register_user(phone)

        if not result:
            logger.error("User registration failed")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="User registration failed",
            )

        response_data = UserAPIKeyResponse(user_id=result.get("user_id") or "", api_key=result.get("api_key") or "")

        logger.info("User registration succeeded for user_id %s", result.get("user_id"))

        return StandardResponse(
            code=ResCode.SUCCESS,
            msg="Registered",
            data=response_data.model_dump(),
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.error("Error during user registration: %s", str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Internal service error",
        )


@router.post(
    "/get-key",
    response_model=StandardResponse,
    status_code=status.HTTP_200_OK,
    summary="Get user API key",
    description="Get the default API key for a user by phone number.",
)
async def get_user_api_key(
    request: UserRegisterRequest,
    service: UserService = Depends(get_user_service),
    token: str = Depends(verify_getkey_bearer_token),
):
    """Get a user's default API key."""
    try:
        phone = request.phone
        logger.info("User API key requested")

        result = await service.get_user_info(phone)

        if not result:
            logger.error("User API key lookup failed")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="User is not registered",
            )

        response_data = UserAPIKeyResponse(user_id=result.get("user_id") or "", api_key=result.get("api_key") or "")

        logger.info("User API key lookup succeeded for user_id %s", result.get("user_id"))

        return StandardResponse(
            code=ResCode.SUCCESS,
            msg="Loaded",
            data=response_data.model_dump(),
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.error("Error during user API key lookup: %s", str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Internal service error",
        )
