from collections.abc import AsyncGenerator
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
    # 닫기 매칭: 해제연결시간 초과제목
    pool_pre_ping=True,  # 사용전조회연결여부있음, 중지사용완료열기의연결
    pool_recycle=3600,  # 1시간후돌아가기연결, 중지길이시간빈가져오기 서비스서버열기
    pool_size=5,  # 연결크기
    max_overflow=10,  # 대출력연결데이터
    pool_timeout=30,  # 가져오기연결의시간 초과시간
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