import asyncio
from collections.abc import AsyncGenerator
from functools import wraps
from urllib.parse import quote

from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker

from app.config import get_settings

_engine = None
_session_factory = None


class Base(DeclarativeBase):
    pass


def _database_url() -> str:
    settings = get_settings()
    return settings.DATABASE_URL.replace("{username}", settings.DATABASE_USERNAME).replace(
        "{password}",
        quote(settings.DATABASE_PASSWORD),
    )


def get_engine():
    global _engine
    if _engine is None:
        _engine = create_async_engine(
            _database_url(),
            echo=False,
            future=True,
            pool_pre_ping=True,
            pool_recycle=1800,
            pool_size=20,
            max_overflow=30,
            pool_timeout=60,
            pool_reset_on_return="commit",
            connect_args={
                "autocommit": False,
                "charset": "utf8mb4",
                "connect_timeout": 60,
            },
        )
    return _engine


def AsyncSessionLocal() -> AsyncSession:
    global _session_factory
    if _session_factory is None:
        _session_factory = sessionmaker(
            bind=get_engine(),
            class_=AsyncSession,
            expire_on_commit=False,
            autoflush=False,
        )
    return _session_factory()


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

    async with get_engine().begin() as conn:
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
