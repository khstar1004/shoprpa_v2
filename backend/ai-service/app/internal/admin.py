from fastapi import APIRouter, Depends, Header, HTTPException, status

from app.config import get_settings
from app.dependencies import get_user_point_service
from app.services.point import PointTransactionType, UserPointService

router = APIRouter()


def verify_admin_api_key(x_api_key: str | None = Header(default=None, alias="X-API-Key")) -> None:
    if not x_api_key or x_api_key != get_settings().INTERNAL_ADMIN_API_KEY:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid admin API key")


@router.post("")
async def update_admin(_: None = Depends(verify_admin_api_key)):
    return {"message": "Admin endpoint is available"}


@router.get("/user/points")
async def get_user_points(
    user_id: str,
    _: None = Depends(verify_admin_api_key),
    user_point_service: UserPointService = Depends(get_user_point_service),
):
    user_point = await user_point_service.get_cached_points(user_id)
    return {"user_point": user_point}


@router.post("/user/points")
async def add_user_points(
    user_id: str,
    amount: int,
    _: None = Depends(verify_admin_api_key),
    user_point_service: UserPointService = Depends(get_user_point_service),
):
    """Add points to a user."""
    user_point = await user_point_service.manual_add_points(
        user_id=user_id,
        amount=amount,
    )
    return {
        "message": "Points added",
        "allocation_id": user_point.id,
        "amount": amount,
    }


@router.post("/user/points/deduct")
async def deduct_user_points(
    user_id: str,
    amount: int,
    _: None = Depends(verify_admin_api_key),
    user_point_service: UserPointService = Depends(get_user_point_service),
):
    """Deduct points from a user."""
    user_point = await user_point_service.deduct_points(
        user_id=user_id,
        amount=amount,
        transaction_type=PointTransactionType.MANUAL_DEDUCT,
    )
    return {
        "message": "Points deducted",
        "transaction_id": user_point.id,
        "amount": amount,
    }
