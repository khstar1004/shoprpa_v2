import asyncio
from collections.abc import AsyncGenerator
from functools import wraps
from urllib.parse import quote

from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker

from app.config import get_settings

engine = create_async_engine(
    get_settings()
    .DATABASE_URL.replace("{username}", get_settings().DATABASE_USERNAME)
    .replace("{password}", quote(get_settings().DATABASE_PASSWORD)),
    echo=False,
    future=True,
    pool_pre_ping=True,
    pool_recycle=1800,  # 적음까지30분, 연결경과
    pool_size=20,  # 증가추가연결크기으로지원변경높이발송
    max_overflow=30,  # 증가추가대출력연결데이터
    pool_timeout=60,  # 증가추가연결시간 초과시간까지60초
    pool_reset_on_return="commit",  # 반환연결시재
    connect_args={
        "autocommit": False,
        "charset": "utf8mb4",
        "connect_timeout": 60,  # 연결시간 초과시간
    },
)


class Base(DeclarativeBase):
    pass


AsyncSessionLocal = sessionmaker(bind=engine, class_=AsyncSession, expire_on_commit=False, autoflush=False)


async def get_db() -> AsyncGenerator[AsyncSession]:
    async with AsyncSessionLocal() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise
        finally:
            await session.close()


async def create_db_and_tables():
    """Create the database and tables."""
    from app.models import load_models  # noqa: F401

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all, checkfirst=True)


def with_db_retry(max_retries=3, delay=1):
    """데이터베이스재시도설치기기"""

    def decorator(func):
        @wraps(func)
        async def wrapper(*args, **kwargs):
            last_exception = None
            for attempt in range(max_retries):
                try:
                    return await func(*args, **kwargs)
                except Exception as e:
                    last_exception = e
                    if "Lost connection" in str(e) and attempt < max_retries - 1:
                        await asyncio.sleep(delay * (attempt + 1))
                        continue
                    raise
            raise last_exception

        return wrapper

    return decorator