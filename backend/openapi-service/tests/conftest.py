from contextlib import asynccontextmanager

import pytest
import pytest_asyncio
from fastapi import FastAPI
from httpx import ASGITransport, AsyncClient
from redis.asyncio import ConnectionPool, Redis
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy.orm import sessionmaker

from app.database import Base, get_db
from app.main import app
from app.redis import get_redis

# 항목시도항목매칭항목
TEST_MYSQL_URL = "mysql+aiomysql://test_user:test_password@localhost:3307/test_db"
TEST_REDIS_URL = "redis://localhost:6380/0"


@asynccontextmanager
async def test_lifespan(app: FastAPI):
    yield


@pytest.fixture(scope="session")
def test_app():
    """생성항목시도항목사용항목"""
    app.router.lifespan_context = test_lifespan
    return app


@pytest_asyncio.fixture(scope="function")
async def test_redis_pool():
    """생성항목시도 Redis 연결항목"""
    pool = ConnectionPool.from_url(TEST_REDIS_URL)
    yield pool
    await pool.disconnect()


@pytest_asyncio.fixture(scope="function")
async def test_get_redis(test_redis_pool):
    """항목항목의 Redis 클라이언트"""
    redis = Redis(connection_pool=test_redis_pool)

    # 매개항목시도전항목관리데이터
    await redis.flushdb()

    yield redis

    # 매개항목시도후항목관리데이터
    await redis.flushdb()
    await redis.aclose()


@pytest_asyncio.fixture(scope="function")
async def test_db_engine():
    """생성항목시도데이터라이브러리항목"""
    engine = create_async_engine(
        TEST_MYSQL_URL,
        echo=False,  # 항목시도시닫기 SQL 로그
        pool_pre_ping=True,
        pool_recycle=300,
    )

    # 로드항목유형
    from app.models import load_models  # noqa: F401

    # 생성모든테이블
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
        await conn.run_sync(Base.metadata.create_all)

    yield engine

    # 항목관리
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)

    await engine.dispose()


@pytest_asyncio.fixture(scope="function")  # 수정로 function 단계항목확인항목시도항목
async def test_get_db(test_db_engine):
    """항목항목의데이터라이브러리항목"""
    TestingSessionLocal = sessionmaker(test_db_engine, class_=AsyncSession, expire_on_commit=False)

    async with TestingSessionLocal() as session:
        # 열기항목서비스
        transaction = await session.begin()

        yield session

        # 돌아가기항목서비스확인항목시도항목
        await transaction.rollback()


@pytest_asyncio.fixture(scope="function")
async def client(test_app: FastAPI, test_get_db, test_get_redis):
    """항목예외항목시도클라이언트"""

    async def override_get_db():
        """덮어쓰기 get_db 항목으로사용항목시도데이터라이브러리"""
        yield test_get_db

    async def override_get_redis():
        """덮어쓰기 get_redis 항목으로사용항목시도 Redis"""
        yield test_get_redis

    # 덮어쓰기항목
    test_app.dependency_overrides[get_db] = override_get_db
    test_app.dependency_overrides[get_redis] = override_get_redis

    # 사용 AsyncClient 항목행예외항목시도
    transport = ASGITransport(app=test_app)
    async with AsyncClient(transport=transport, base_url="http://test", follow_redirects=True) as client:
        yield client

    # 항목관리항목덮어쓰기
    test_app.dependency_overrides.clear()


# # 추가항목의항목클라이언트(예결과필요)
# @pytest.fixture(scope="function")
# def client(async_client):
#     """항목클라이언트항목매칭기기(예결과항목항목시도라이브러리필요항목연결항목)"""
#     return async_client


# 추가항목시도데이터항목항목 fixture
# @pytest_asyncio.fixture(scope="function")
# async def sample_data(test_db):
#     """항목항목시도데이터"""
#     # 에서항목추가항목시도항목필요의항목본데이터
#     # 항목예: 생성항목시도사용자, 문서항목대기
#     pass


async def create_api_key(user_id: str, test_get_db, test_get_redis=None):
    """로항목시도생성항목시의 API Key"""
    import random

    from app.schemas.api_key import ApiKeyCreate
    from app.services.api_key import ApiKeyService

    # 생성서비스항목
    service = ApiKeyService(test_get_db, test_get_redis)

    # 생성 API Key
    api_key_data = ApiKeyCreate(name=f"Test API Key {random.randint(1000, 9999)}")
    api_key = await service.create_api_key(api_key_data, user_id)

    return {"id": api_key.id, "key": api_key.key}


async def destroy_api_key(user_id: str, key_data: dict, test_get_db, test_get_redis=None):
    """삭제항목시도사용의 API Key"""
    from app.services.api_key import ApiKeyService

    # 생성서비스항목
    service = ApiKeyService(test_get_db, test_get_redis)

    # 삭제 API Key
    await service.delete_api_key(key_data["id"], user_id)


@pytest_asyncio.fixture(scope="function")
async def api_key_factory(test_get_db, test_get_redis):
    """
    API Key 항목항목데이터, 지원항목 user_id
    """
    created_keys = []

    async def _create_api_key(user_id: str = "1234"):
        key_data = await create_api_key(user_id, test_get_db, test_get_redis)
        created_keys.append((user_id, key_data))
        return key_data

    yield _create_api_key

    # 항목관리모든생성의 API Key
    for user_id, key_data in created_keys:
        await destroy_api_key(user_id, key_data, test_get_db, test_get_redis)


@pytest_asyncio.fixture(scope="function")
async def api_key(test_get_db, test_get_redis):
    """
    로매개항목시도사용항목항목일개항목시의 API Key
    반환패키지항목 id 및 key 의딕셔너리
    """
    # 항목사용항목지정의항목시도사용자ID, 및 USER_ID_HEADER 보관항목일항목
    user_id = "1234"
    key_data = await create_api_key(user_id, test_get_db, test_get_redis)
    yield key_data
    await destroy_api_key(user_id, key_data, test_get_db, test_get_redis)