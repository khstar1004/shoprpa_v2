import os
from datetime import datetime
from typing import Dict, List, Optional

from fastapi import Depends, Header, HTTPException, Security, status  # Added status
from fastapi.security import APIKeyHeader
from redis.asyncio import Redis
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.database import get_db
from app.models.api_key import OpenAPIDB
from app.redis import get_redis
from app.services.api_key import ApiKeyService, ShoprpaApiKeyService
from app.services.execution import ExecutionService
from app.services.user import UserService
from app.services.websocket import WsManagerService, WsService
from app.services.workflow import WorkflowService
from app.utils.api_key import APIKeyUtils

# 전체영역 WsManagerService 단일
_ws_manager_service: WsManagerService | None = None

# API Key 인증사용의 APIKeyHeader
API_KEY_HEADER = APIKeyHeader(name="Authorization", auto_error=False)


def get_user_id_from_header(
    x_user_id: str | None = Header(default=None, alias="X-User-Id"),
    user_id: str | None = Header(default=None, alias="user_id"),
) -> str:
    """
    에서요청 중가져오기사용자ID, 파싱 X-User-Id, 결과가존재하지 않음이면파싱 user_id
    """
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
    """
    인증회원가입연결의 Bearer Token (사용 Security + APIKeyHeader 방식)
    사용astron-agent빠름회원가입

    사용예시:
    @router.post("/register")
    async def register_user(
        request: UserRegisterRequest,
        token: str = Depends(verify_register_bearer_token),
    ):
        ...
    """
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing Authorization header",
        )

    # 인증 Bearer 형식
    parts = token.split()
    if len(parts) != 2 or parts[0].lower() != "bearer":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid Authorization header format",
        )

    bearer_token = parts[1]

    # 인증 Token 여부정상
    if bearer_token != "opensource-register-token":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token",
        )

    return bearer_token


async def verify_getkey_bearer_token(
    token: str = Security(API_KEY_HEADER),
) -> str:
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing Authorization header",
        )

    # 인증 Bearer 형식
    parts = token.split()
    if len(parts) != 2 or parts[0].lower() != "bearer":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid Authorization header format",
        )

    bearer_token = parts[1]

    # 인증 Token 여부정상
    if bearer_token != "opensource-register-token":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token",
        )

    return bearer_token


def extract_api_key_from_request(ctx) -> Optional[str]:
    """
    에서요청 위아래문서중가져오기API_KEY
    MCP사용
    """

    # 시도다중방식가져오기조회매개변수
    query_params = ctx.request.query_params

    if query_params:
        # 결과가예딕셔너리유형
        if isinstance(query_params, dict):
            return query_params.get("key")

        # 결과가예QueryParams객체(Starlette)
        if hasattr(query_params, "get"):
            return query_params.get("key")

        # 결과가예문자열유형의조회문자열
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
    """
    에서 Authorization 요청 중가져오기 API Key, 조회데이터베이스까지 user_id
    """
    if api_key_header is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Not authenticated",
            headers={"WWW-Authenticate": "Bearer"},
        )

    parts = api_key_header.split()
    if len(parts) != 2 or parts[0].lower() != "bearer":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )

    api_key = parts[1]

    # 사용전매칭및인증
    keys = await db.execute(select(OpenAPIDB).where(OpenAPIDB.prefix == api_key[:8], OpenAPIDB.is_active == 1))
    api_keys = keys.scalars().all()

    for key in api_keys:
        hashed_key = key.api_key
        if APIKeyUtils.verify_api_key(api_key, hashed_key):
            return str(key.user_id)

    # 결과가있음까지매칭의API key
    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid or inactive API key",
        headers={"WWW-Authenticate": "Bearer"},
    )


async def get_user_id_with_fallback(
    api_key_header: str = Security(API_KEY_HEADER),
    x_user_id: str | None = Header(default=None, alias="X-User-Id"),
    user_id: str | None = Header(default=None, alias="user_id"),
    db: AsyncSession = Depends(get_db),
) -> str:
    """
    단계가져오기사용자ID: 
    1. 시도에서Authorization Bearer token중가져오기API key인증
    2. 결과가있음API key, 이면에서X-User-Id또는user_id header중가져오기
    3. 결과가있음, 이면출력401오류

    사용get_workflows(있음가능본호출, 있음가능외부모듈호출)

    사용예시: 
    @router.get("/example")
    async def example_endpoint(
        user_id: str = Depends(get_user_id_with_fallback)
    ):
        return {"user_id": user_id}
    """
    # 시도에서API key가져오기사용자ID
    if api_key_header:
        try:
            parts = api_key_header.split()
            if len(parts) == 2 and parts[0].lower() == "bearer":
                api_key = parts[1]

                # 사용전매칭및인증
                keys = await db.execute(
                    select(OpenAPIDB).where(OpenAPIDB.prefix == api_key[:8], OpenAPIDB.is_active == 1)
                )
                api_keys = keys.scalars().all()

                for key in api_keys:
                    hashed_key = key.api_key
                    if APIKeyUtils.verify_api_key(api_key, hashed_key):
                        return str(key.user_id)

                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Authentication failed. Your API_KEY is not correct.",
                    headers={"WWW-Authenticate": "Bearer"},
                )
        except Exception as e:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Authentication required. Please provide either a valid API key in Authorization header.",
                headers={"WWW-Authenticate": "Bearer"},
            )

    # 결과가API key인증실패또는존재하지 않음, 시도에서header가져오기
    header_user_id = x_user_id or user_id
    if header_user_id:
        return header_user_id
    else:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication required. Please provide either a valid API key in Authorization header or user_id in X-User-Id/user_id header.",
            headers={"WWW-Authenticate": "Bearer"},
        )


async def check_user_id_equality(
    api_key_header: str = Security(API_KEY_HEADER),
    user_id: str | None = Header(default=None, alias="user_id"),
    db: AsyncSession = Depends(get_db),
) -> bool:
    """
    사용예시: 
    @router.get("/example")
    async def example_endpoint(
        user_id: str = Depends(get_user_id_with_fallback)
    ):
        return {"user_id": user_id}
    """
    # API_KEY의user_id및본경로 의user_id여부매칭
    if api_key_header and user_id:
        try:
            parts = api_key_header.split()
            if len(parts) == 2 and parts[0].lower() == "bearer":
                api_key = parts[1]

                # 사용전매칭및인증
                keys = await db.execute(
                    select(OpenAPIDB).where(OpenAPIDB.prefix == api_key[:8], OpenAPIDB.is_active == 1)
                )
                api_keys = keys.scalars().all()
                if not api_keys:
                    raise HTTPException(
                        status_code=status.HTTP_401_UNAUTHORIZED,
                        detail="Authentication required. Please provide either a valid API key in Authorization header",
                        headers={"WWW-Authenticate": "Bearer"},
                    )

                for key in api_keys:
                    hashed_key = key.api_key
                    if APIKeyUtils.verify_api_key(api_key, hashed_key):
                        if str(key.user_id) == user_id:
                            return True

                return False

        except Exception as e:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Authentication required. Please provide either a valid API key in Authorization header",
                headers={"WWW-Authenticate": "Bearer"},
            )

    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Authentication required. Either API key or user_id is not provided.",
        headers={"WWW-Authenticate": "Bearer"},
    )


# 비고: get_uid_from_raw_key 데이터완료까지 app.services.streamable_mcp.ToolsConfig 유형중
# 으로생성다중개데이터베이스연결


async def get_workflow_service(
    db: AsyncSession = Depends(get_db), redis: Redis = Depends(get_redis)
) -> WorkflowService:
    """WorkflowService의"""
    return WorkflowService(db, redis)


async def get_execution_service(
    db: AsyncSession = Depends(get_db), redis: Redis = Depends(get_redis)
) -> ExecutionService:
    """ExecutionService의"""
    return ExecutionService(db, redis)


async def get_api_key_service(db: AsyncSession = Depends(get_db), redis: Redis = Depends(get_redis)) -> ApiKeyService:
    """ApiKeyService의"""
    return ApiKeyService(db, redis)


async def get_astron_api_key_service(
    db: AsyncSession = Depends(get_db), redis: Redis = Depends(get_redis)
) -> ShoprpaApiKeyService:
    """XcApiKeyService의"""
    return ShoprpaApiKeyService(db, redis)


async def get_user_service(
    db: AsyncSession = Depends(get_db),
    redis: Redis = Depends(get_redis),
    api_key_service: ApiKeyService = Depends(get_api_key_service),
) -> UserService:
    """UserService의"""
    return UserService(db, redis, api_key_service)


async def get_ws_service() -> WsManagerService:
    """ WsManagerService 단일의"""
    global _ws_manager_service
    if _ws_manager_service is None:
        # 에서다중worker아래, 매개있음의
        # 예보통의, 원인로WebSocket연결예단계의
        worker_id = os.getpid()
        _ws_manager_service = WsManagerService()
        # 가능으로에서추가worker식별자, 디버그
        _ws_manager_service.worker_id = worker_id
    return _ws_manager_service