import os
from typing import Optional

from fastapi import Depends, Header, HTTPException, Security, status
from fastapi.security import APIKeyHeader
from redis.asyncio import Redis
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import get_settings
from app.database import get_db
from app.redis import get_redis
from app.services.api_key import ApiKeyService, ShoprpaApiKeyService
from app.services.execution import ExecutionService
from app.services.user import UserService
from app.services.websocket import WsManagerService, WsService
from app.services.workflow import WorkflowService

# One process-local WebSocket manager. Each worker process keeps its own instance.
_ws_manager_service: WsManagerService | None = None

# API key authentication via Authorization: Bearer <key>.
API_KEY_HEADER = APIKeyHeader(name="Authorization", auto_error=False)


def _bearer_auth_error(detail: str = "Invalid authentication credentials") -> HTTPException:
    return HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail=detail,
        headers={"WWW-Authenticate": "Bearer"},
    )


def _extract_bearer_api_key(
    authorization: str | None,
    *,
    missing_detail: str = "Not authenticated",
    invalid_detail: str = "Invalid authentication credentials",
) -> str:
    if not authorization:
        raise _bearer_auth_error(missing_detail)

    parts = authorization.split()
    if len(parts) != 2 or parts[0].lower() != "bearer":
        raise _bearer_auth_error(invalid_detail)

    return parts[1]


async def _resolve_user_id_for_api_key(api_key: str, db: AsyncSession) -> str | None:
    return await ApiKeyService(db).validate_api_key(api_key)


def get_user_id_from_header(
    x_user_id: str | None = Header(default=None, alias="X-User-Id"),
    user_id: str | None = Header(default=None, alias="user_id"),
) -> str:
    """Read the caller user id from X-User-Id or user_id."""
    header_user_id = x_user_id or user_id

    if header_user_id is None:
        raise HTTPException(
            status_code=401,
            detail="Missing X-User-Id or user_id header.",
        )
    return header_user_id


async def verify_register_bearer_token(
    token: str = Security(API_KEY_HEADER),
) -> str:
    """Validate the shared token used by the registration bridge."""
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing Authorization header",
        )

    parts = token.split()
    if len(parts) != 2 or parts[0].lower() != "bearer":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid Authorization header format",
        )

    bearer_token = parts[1]

    if bearer_token != get_settings().REGISTER_BEARER_TOKEN:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token",
        )

    return bearer_token


async def verify_getkey_bearer_token(
    token: str = Security(API_KEY_HEADER),
) -> str:
    """Validate the shared token used by the user-key bridge."""
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing Authorization header",
        )

    parts = token.split()
    if len(parts) != 2 or parts[0].lower() != "bearer":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid Authorization header format",
        )

    bearer_token = parts[1]

    if bearer_token != get_settings().REGISTER_BEARER_TOKEN:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token",
        )

    return bearer_token


def extract_api_key_from_request(ctx) -> Optional[str]:
    """Extract a raw API key from an MCP request query string."""

    query_params = ctx.request.query_params

    if query_params:
        if isinstance(query_params, dict):
            return query_params.get("key")

        if hasattr(query_params, "get"):
            return query_params.get("key")

        if isinstance(query_params, str):
            from urllib.parse import parse_qs

            parsed = parse_qs(query_params)
            key_values = parsed.get("key", [])
            return key_values[0] if key_values else None
    return None


async def get_user_id_from_api_key(
    api_key_header: str = Security(API_KEY_HEADER),
    db: AsyncSession = Depends(get_db),
) -> str:
    """Resolve the caller user id from an Authorization bearer API key."""
    api_key = _extract_bearer_api_key(api_key_header)
    user_id = await _resolve_user_id_for_api_key(api_key, db)

    if user_id:
        return user_id

    raise _bearer_auth_error("Invalid or inactive API key")


async def get_user_id_with_fallback(
    api_key_header: str = Security(API_KEY_HEADER),
    x_user_id: str | None = Header(default=None, alias="X-User-Id"),
    user_id: str | None = Header(default=None, alias="user_id"),
    db: AsyncSession = Depends(get_db),
) -> str:
    """Resolve a user id from bearer API key, then fall back to user id headers."""
    if api_key_header:
        api_key = _extract_bearer_api_key(api_key_header)
        authenticated_user_id = await _resolve_user_id_for_api_key(api_key, db)
        if authenticated_user_id:
            return authenticated_user_id

        raise _bearer_auth_error("Authentication failed. Your API key is not correct.")

    header_user_id = x_user_id or user_id
    if header_user_id:
        return header_user_id

    raise _bearer_auth_error(
        "Authentication required. Please provide either a valid API key in Authorization header or user_id in X-User-Id/user_id header."
    )


async def check_user_id_equality(
    api_key_header: str = Security(API_KEY_HEADER),
    user_id: str | None = Header(default=None, alias="user_id"),
    db: AsyncSession = Depends(get_db),
) -> bool:
    """Check whether the bearer API key belongs to the supplied user_id header."""
    if api_key_header and user_id:
        api_key = _extract_bearer_api_key(api_key_header)
        authenticated_user_id = await _resolve_user_id_for_api_key(api_key, db)
        if authenticated_user_id is None:
            raise _bearer_auth_error("Authentication required. Please provide a valid API key in Authorization header.")

        return authenticated_user_id == user_id

    raise _bearer_auth_error("Authentication required. Either API key or user_id is not provided.")


async def get_workflow_service(
    db: AsyncSession = Depends(get_db), redis: Redis = Depends(get_redis)
) -> WorkflowService:
    """Create a WorkflowService for request scope."""
    return WorkflowService(db, redis)


async def get_execution_service(
    db: AsyncSession = Depends(get_db), redis: Redis = Depends(get_redis)
) -> ExecutionService:
    """Create an ExecutionService for request scope."""
    return ExecutionService(db, redis)


async def get_api_key_service(db: AsyncSession = Depends(get_db), redis: Redis = Depends(get_redis)) -> ApiKeyService:
    """Create an ApiKeyService for request scope."""
    return ApiKeyService(db, redis)


async def get_astron_api_key_service(
    db: AsyncSession = Depends(get_db), redis: Redis = Depends(get_redis)
) -> ShoprpaApiKeyService:
    """Create a ShoprpaApiKeyService for request scope."""
    return ShoprpaApiKeyService(db, redis)


async def get_user_service(
    db: AsyncSession = Depends(get_db),
    redis: Redis = Depends(get_redis),
    api_key_service: ApiKeyService = Depends(get_api_key_service),
) -> UserService:
    """Create a UserService for request scope."""
    return UserService(db, redis, api_key_service)


async def get_ws_service() -> WsManagerService:
    """Return the process-local WebSocket manager."""
    global _ws_manager_service
    if _ws_manager_service is None:
        worker_id = os.getpid()
        _ws_manager_service = WsManagerService()
        _ws_manager_service.worker_id = worker_id
    return _ws_manager_service
