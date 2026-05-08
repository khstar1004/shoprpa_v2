from fastapi import APIRouter, Depends, Header, HTTPException, status

from app.config import get_settings

router = APIRouter()


def verify_admin_api_key(x_api_key: str | None = Header(default=None, alias="X-API-Key")) -> None:
    if not x_api_key or x_api_key != get_settings().INTERNAL_ADMIN_API_KEY:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid admin API key")


@router.get("")
async def get_admin_status(_: None = Depends(verify_admin_api_key)):
    settings = get_settings()
    return {
        "service": settings.APP_NAME,
        "version": settings.API_VERSION,
        "status": "ok",
    }


@router.post("")
async def update_admin(_: None = Depends(verify_admin_api_key)):
    settings = get_settings()
    return {
        "service": settings.APP_NAME,
        "version": settings.API_VERSION,
        "status": "ok",
    }
