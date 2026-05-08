from collections.abc import AsyncGenerator
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
            pool_recycle=3600,
            pool_size=5,
            max_overflow=10,
            pool_timeout=30,
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
