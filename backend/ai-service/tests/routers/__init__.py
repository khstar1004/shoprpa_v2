import pytest
import pytest_asyncio
from contextlib import asynccontextmanager
from fastapi import FastAPI
from httpx import AsyncClient, ASGITransport
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sqlalchemy.orm import sessionmaker
from redis.asyncio import ConnectionPool, Redis

from app.main import app
from app.database import get_db, Base
from app.redis_op import get_redis

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
    TestingSessionLocal = sessionmaker(
        test_db_engine, class_=AsyncSession, expire_on_commit=False
    )

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
    async with AsyncClient(transport=transport, base_url="http://test") as client:
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
