import json
from datetime import datetime
from typing import Optional

import httpx
import pytz
from redis.asyncio import Redis
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import get_settings
from app.logger import get_logger
from app.models.user import User
from app.schemas.api_key import ApiKeyCreate

logger = get_logger(__name__)


class UserService:
    def __init__(self, db: AsyncSession, redis: Redis = None, api_key_service=None):
        self.db = db
        self.redis = redis
        self.api_key_service = api_key_service
        settings = get_settings()
        robot_service_base_url = settings.ROBOT_SERVICE_BASE_URL.rstrip("/")
        self.register_api_url = f"{robot_service_base_url}/api/robot/register"
        self.user_info_api_url = f"{robot_service_base_url}/api/robot/astron-agent/get-user-id"
        self.internal_headers = {"X-API-Key": settings.INTERNAL_ADMIN_API_KEY}

    async def _call_register_api(self, phone: str) -> Optional[dict]:
        """Register a user through the internal robot service."""
        try:
            async with httpx.AsyncClient() as client:
                logger.info("Calling robot service user registration")

                response = await client.post(self.register_api_url, params={"phone": phone}, timeout=10.0)

                if response.status_code != 200:
                    logger.error(
                        "Robot registration returned HTTP %s: %s",
                        response.status_code,
                        response.text[:500],
                    )
                    return None

                response_data = response.json()
                logger.info("Robot registration response received with code %s", response_data.get("code"))

                user_data = response_data.get("data", {})
                user_id = user_data.get("userId")
                if not user_id:
                    logger.error("Robot registration response did not include userId")
                    return None

                logger.info("Robot registration succeeded for user_id %s", user_id)
                return user_data

        except httpx.TimeoutException as e:
            logger.exception("Robot registration timed out")
            return None
        except httpx.RequestError as e:
            logger.exception("Robot registration request failed")
            return None
        except json.JSONDecodeError as e:
            logger.exception("Failed to parse robot registration response")
            return None
        except Exception as e:
            logger.exception("Unexpected robot registration error")
            return None

    async def _call_user_info_api(self, phone: str) -> Optional[dict]:
        """Look up a user through the internal robot service."""
        try:
            async with httpx.AsyncClient() as client:
                url = self.user_info_api_url
                logger.info("Calling robot service user lookup")

                response = await client.post(url, json={"phone": phone}, headers=self.internal_headers, timeout=10.0)

                if response.status_code != 200:
                    logger.error(
                        "Robot user lookup returned HTTP %s: %s",
                        response.status_code,
                        response.text[:500],
                    )
                    return None

                response_data = response.json()
                logger.info("Robot user lookup response received with code %s", response_data.get("code"))
                if response_data.get("code") == "500000":
                    logger.error("Robot user lookup returned application error: %s", response_data.get("message"))
                    return None

                user_data = response_data.get("data", {})
                user_id = user_data.get("userId", None)
                if not user_id:
                    logger.error("Robot user lookup response did not include userId")
                    return None

                logger.info("Robot user lookup succeeded for user_id %s", user_id)
                return user_data

        except httpx.TimeoutException as e:
            logger.exception("Robot user lookup timed out")
            return None
        except httpx.RequestError as e:
            logger.exception("Robot user lookup request failed")
            return None
        except json.JSONDecodeError as e:
            logger.exception("Failed to parse robot user lookup response")
            return None
        except Exception as e:
            logger.exception("Unexpected robot user lookup error")
            return None

    async def register_user(self, phone: str) -> Optional[dict]:
        """Register a user and create the default API key."""
        query = select(User).where(User.phone == phone)
        result = await self.db.execute(query)
        existing_user = result.scalars().first()

        if existing_user:
            logger.info("Existing user found for user_id %s", existing_user.user_id)
            return {
                "user_id": existing_user.user_id,
                "api_key": existing_user.default_api_key,
            }

        logger.info("Starting new user registration")
        user_data = await self._call_register_api(phone)

        if not user_data:
            logger.error("Robot registration call failed")
            return None

        user_id = user_data.get("userId")

        default_api_key = None
        if self.api_key_service:
            try:
                api_key_data = ApiKeyCreate(name=f"default_key_{phone}")
                default_api_key = await self.api_key_service.create_api_key(api_key_data, user_id)
                logger.info("Created default API key for user_id %s", user_id)
            except Exception as e:
                logger.exception("Failed to create default API key for user_id %s", user_id)

        new_user = User(
            user_id=user_id,
            phone=phone,
            default_api_key=default_api_key,
            created_at=datetime.now(pytz.timezone("Asia/Seoul")).replace(tzinfo=None),
            updated_at=datetime.now(pytz.timezone("Asia/Seoul")).replace(tzinfo=None),
        )

        self.db.add(new_user)
        await self.db.flush()
        await self.db.refresh(new_user)

        logger.info("User registration succeeded for user_id %s", user_id)

        return {
            "user_id": user_id,
            "api_key": default_api_key,
            "password": user_data.get("password"),
            "url": user_data.get("url"),
            "account": user_data.get("account"),
        }

    async def get_user_info(self, phone: str) -> Optional[dict]:
        """Get a user and default API key by phone."""
        query = select(User).where(User.phone == phone)
        result = await self.db.execute(query)
        existing_user = result.scalars().first()

        if existing_user:
            logger.info("Existing user found for user_id %s", existing_user.user_id)
            return {
                "user_id": existing_user.user_id,
                "api_key": existing_user.default_api_key,
            }

        logger.info("Starting robot user lookup")
        user_data = await self._call_user_info_api(phone)

        if not user_data:
            logger.error("Robot user lookup call failed")
            return None

        user_id = user_data.get("userId")

        default_api_key = None
        if self.api_key_service:
            try:
                api_key_data = ApiKeyCreate(name=f"default_key_{phone}")
                default_api_key = await self.api_key_service.create_api_key(api_key_data, user_id)
                logger.info("Created default API key for user_id %s", user_id)
            except Exception as e:
                logger.exception("Failed to create default API key for user_id %s", user_id)

        new_user = User(
            user_id=user_id,
            phone=phone,
            default_api_key=default_api_key,
            created_at=datetime.now(pytz.timezone("Asia/Seoul")).replace(tzinfo=None),
            updated_at=datetime.now(pytz.timezone("Asia/Seoul")).replace(tzinfo=None),
        )

        self.db.add(new_user)
        await self.db.flush()
        await self.db.refresh(new_user)

        logger.info("User API key lookup succeeded for user_id %s", user_id)

        return {"user_id": user_id, "api_key": default_api_key}

    async def get_user_by_phone(self, phone: str) -> Optional[User]:
        """Get a user by phone."""
        query = select(User).where(User.phone == phone)
        result = await self.db.execute(query)
        return result.scalars().first()

    async def get_user_by_user_id(self, user_id: str) -> Optional[User]:
        """Get a user by user_id."""
        query = select(User).where(User.user_id == user_id)
        result = await self.db.execute(query)
        return result.scalars().first()
