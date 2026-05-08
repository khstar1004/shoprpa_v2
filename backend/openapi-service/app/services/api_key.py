from datetime import datetime
from typing import Optional

import pytz
from redis.asyncio import Redis
from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.logger import get_logger
from app.models.api_key import ShoprpaAgentDB, OpenAPIDB
from app.schemas.api_key import ApiKeyCreate
from app.utils.api_key import APIKeyUtils

logger = get_logger(__name__)


class ApiKeyService:
    def __init__(self, db: AsyncSession, redis: Redis = None):
        self.db = db
        self.redis = redis

    async def _invalidate_api_keys_cache(self, user_id: str) -> None:
        """Invalidate cached API key lists for a user."""
        if self.redis:
            keys = await self.redis.keys(f"api_keys:{user_id}:*")
            if keys:
                await self.redis.delete(*keys)

    async def create_api_key(self, api_key_data: ApiKeyCreate, user_id: str) -> str:
        """Create an API key and return the plaintext value once."""
        api_key = APIKeyUtils.generate_api_key()
        hashed_key = APIKeyUtils.hash_api_key(api_key)
        prefix = api_key[:8]
        name = api_key_data.name
        logger.info("Generated API key for user %s with prefix %s", user_id, prefix)

        new_api_key = OpenAPIDB(
            user_id=user_id,
            api_key=hashed_key,
            prefix=prefix,
            created_at=datetime.now(pytz.timezone("Asia/Seoul")).replace(tzinfo=None),
            name=name,
            is_active=1,
            updated_at=datetime.now(pytz.timezone("Asia/Seoul")).replace(tzinfo=None),
        )

        self.db.add(new_api_key)
        await self.db.flush()
        await self.db.refresh(new_api_key)

        # 지우기저장
        await self._invalidate_api_keys_cache(user_id)

        return api_key

    async def get_api_key(self, api_key_id: str, user_id: str | None = None) -> Optional[OpenAPIDB]:
        """Get an active API key row by id."""
        query = select(OpenAPIDB).where(OpenAPIDB.id == api_key_id)
        if user_id is not None:
            query = query.where(OpenAPIDB.user_id == user_id)
        query = query.where(OpenAPIDB.is_active == 1)

        result = await self.db.execute(query)
        return result.scalars().first()

    async def get_api_keys(self, user_id: str, page_no: int = 0, page_size: int = 10) -> list[OpenAPIDB]:
        """List active API keys for a user."""
        skip = (page_no - 1) * page_size
        cache_key = f"api_keys:{user_id}:{skip}:{page_size}"

        if self.redis:
            cached_data = await self.redis.get(cache_key)
            if cached_data:
                pass

        query = (
            select(OpenAPIDB)
            .where(OpenAPIDB.user_id == user_id)
            .where(OpenAPIDB.is_active == 1)
            .where(~OpenAPIDB.name.startswith("default_key_"))
            .order_by(OpenAPIDB.created_at.desc())
            .offset(skip)
            .limit(page_size)
        )
        query_result = await self.db.execute(query)
        api_keys = query_result.scalars().all()
        result = []
        for key in api_keys:
            result.append(
                {
                    "id": key.id,
                    "api_key": key.prefix + "******************",
                    "name": key.name,
                    "createTime": key.created_at,
                    "recentTime": key.updated_at,
                }
            )

        if self.redis:
            pass

        return result

    async def delete_api_key(self, api_key_id: str, user_id: str) -> bool:
        """Soft-delete an API key."""
        api_key = await self.get_api_key(api_key_id, user_id)
        if not api_key:
            return False

        stmt = update(OpenAPIDB).where(OpenAPIDB.id == api_key_id, OpenAPIDB.user_id == user_id).values(is_active=0)

        await self.db.execute(stmt)

        await self._invalidate_api_keys_cache(user_id)

        return True

    async def validate_api_key(self, key: str) -> Optional[str]:
        """Validate a plaintext API key and return its user id."""
        if not key:
            return None

        prefix = key[:8]
        query = select(OpenAPIDB).where(OpenAPIDB.prefix == prefix)
        query = query.where(OpenAPIDB.is_active == 1)
        result = await self.db.execute(query)
        api_keys = result.scalars().all()

        for api_key in api_keys:
            if APIKeyUtils.verify_api_key(key, api_key.api_key):
                return str(api_key.user_id)
        return None


class ShoprpaApiKeyService:
    def __init__(self, db: AsyncSession, redis: Redis = None):
        self.db = db
        self.redis = redis

    async def create_astron_agent(self, astron_agent_data, user_id: str):
        """Create a ShopRPA Agent credential."""

        new_astron_agent = ShoprpaAgentDB(
            user_id=user_id,
            name=astron_agent_data.name,
            app_id=astron_agent_data.app_id,
            api_key=astron_agent_data.api_key,
            api_secret=astron_agent_data.api_secret,
            created_at=datetime.now(pytz.timezone("Asia/Seoul")).replace(tzinfo=None),
            updated_at=datetime.now(pytz.timezone("Asia/Seoul")).replace(tzinfo=None),
            is_active=1,
        )

        self.db.add(new_astron_agent)
        await self.db.flush()
        await self.db.refresh(new_astron_agent)

        logger.info("Created ShopRPA Agent credential for user %s: %s", user_id, new_astron_agent.id)

        return new_astron_agent

    async def delete_astron_agent(self, astron_agent_id: str, user_id: str) -> bool:
        """Soft-delete a ShopRPA Agent credential."""
        try:
            stmt = (
                update(ShoprpaAgentDB)
                .where(ShoprpaAgentDB.id == astron_agent_id, ShoprpaAgentDB.user_id == user_id)
                .values(is_active=0, updated_at=datetime.now(pytz.timezone("Asia/Seoul")).replace(tzinfo=None))
            )

            result = await self.db.execute(stmt)
            await self.db.commit()

            # 조회여부업데이트완료기록
            if result.rowcount > 0:
                logger.info("Soft deleted ShopRPA Agent %s for user %s", astron_agent_id, user_id)
                return True
            else:
                logger.warning("ShopRPA Agent %s for user %s not found", astron_agent_id, user_id)
                return False

        except Exception as e:
            logger.error("Error soft deleting ShopRPA Agent %s for user %s: %s", astron_agent_id, user_id, str(e))
            await self.db.rollback()
            raise

    async def get_astron_agents(self, user_id: str, pageNo: int = 1, pageSize: int = 10) -> list[dict]:
        """List ShopRPA Agent credentials."""
        try:
            skip = (pageNo - 1) * pageSize

            # 생성조회, 가져오기 의기록
            query = (
                select(ShoprpaAgentDB)
                .where(ShoprpaAgentDB.user_id == user_id)
                .where(ShoprpaAgentDB.is_active == 1)
                .order_by(ShoprpaAgentDB.created_at.desc())
                .offset(skip)
                .limit(pageSize)
            )

            result = await self.db.execute(query)
            astron_agents = result.scalars().all()

            # 형식반환데이터
            formatted_agents = []
            for agent in astron_agents:
                formatted_agents.append(
                    {
                        "id": agent.id,
                        "name": agent.name,
                        "app_id": agent.app_id,
                        "api_key": agent.api_key,
                        "api_secret": agent.api_secret,
                        "createTime": agent.created_at,
                        "recentTime": agent.updated_at,
                    }
                )

            return formatted_agents

        except Exception as e:
            logger.error("Error getting ShopRPA Agents for user %s: %s", user_id, str(e))
            raise

    async def get_all_astron_agents(self, user_id: str) -> list[dict]:
        """List all active ShopRPA Agent credentials for a user."""
        try:
            # 생성조회, 가져오기 의기록
            query = (
                select(ShoprpaAgentDB)
                .where(ShoprpaAgentDB.user_id == user_id)
                .where(ShoprpaAgentDB.is_active == 1)
                .order_by(ShoprpaAgentDB.created_at.desc())
            )

            result = await self.db.execute(query)
            astron_agents = result.scalars().all()

            # 형식반환데이터
            formatted_agents = []
            for agent in astron_agents:
                formatted_agents.append(
                    {
                        "id": agent.id,
                        "name": agent.name,
                        "app_id": agent.app_id,
                        "api_key": agent.api_key,
                        "api_secret": agent.api_secret,
                        "createTime": agent.created_at,
                        "recentTime": agent.updated_at,
                    }
                )

            return formatted_agents

        except Exception as e:
            logger.error("Error getting all ShopRPA Agents for user %s: %s", user_id, str(e))
            raise

    async def update_astron_agent(self, astron_agent_id: str, user_id: str, update_data) -> bool:
        """Update a ShopRPA Agent credential."""
        try:
            # Update the ShopRPA Agent credential owned by this user.
            update_values = {"updated_at": datetime.now(pytz.timezone("Asia/Seoul")).replace(tzinfo=None)}

            # 업데이트의필드
            if hasattr(update_data, "name") and update_data.name is not None:
                update_values["name"] = update_data.name
            if hasattr(update_data, "app_id") and update_data.app_id is not None:
                update_values["app_id"] = update_data.app_id
            if hasattr(update_data, "api_key") and update_data.api_key is not None:
                update_values["api_key"] = update_data.api_key
            if hasattr(update_data, "api_secret") and update_data.api_secret is not None:
                update_values["api_secret"] = update_data.api_secret

            stmt = (
                update(ShoprpaAgentDB)
                .where(
                    ShoprpaAgentDB.id == astron_agent_id,
                    ShoprpaAgentDB.user_id == user_id,
                    ShoprpaAgentDB.is_active == 1,  # 업데이트의기록
                )
                .values(update_values)
            )

            result = await self.db.execute(stmt)
            await self.db.commit()

            # 조회여부업데이트완료기록
            if result.rowcount > 0:
                logger.info("Updated ShopRPA Agent %s for user %s", astron_agent_id, user_id)
                return True
            else:
                logger.warning("ShopRPA Agent %s for user %s not found or not active", astron_agent_id, user_id)
                return False

        except Exception as e:
            logger.error("Error updating ShopRPA Agent %s for user %s: %s", astron_agent_id, user_id, str(e))
            await self.db.rollback()
            raise

    async def get_astron_agent_by_id(self, astron_agent_id: int, user_id: str) -> Optional[dict]:
        """Get one ShopRPA Agent credential by id."""
        try:
            # 생성조회, 가져오기 의기록
            query = (
                select(ShoprpaAgentDB)
                .where(ShoprpaAgentDB.id == astron_agent_id)
                .where(ShoprpaAgentDB.user_id == user_id)
                .where(ShoprpaAgentDB.is_active == 1)
            )

            result = await self.db.execute(query)
            agent = result.scalars().first()

            if agent:
                # 형식반환데이터
                return {
                    "id": agent.id,
                    "name": agent.name,
                    "app_id": agent.app_id,
                    "api_key": agent.api_key,
                    "api_secret": agent.api_secret,
                    "createTime": agent.created_at,
                    "recentTime": agent.updated_at,
                }

            return None

        except Exception as e:
            logger.error("Error getting ShopRPA Agent %s for user %s: %s", astron_agent_id, user_id, str(e))
            raise
