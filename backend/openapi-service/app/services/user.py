import json
from datetime import datetime
from typing import Optional

import httpx
import pytz
from redis.asyncio import Redis
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.logger import get_logger
from app.models.user import User
from app.schemas.api_key import ApiKeyCreate

logger = get_logger(__name__)


class UserService:
    def __init__(self, db: AsyncSession, redis: Redis = None, api_key_service=None):
        self.db = db
        self.redis = redis
        self.api_key_service = api_key_service
        self.register_api_url = "http://robot-service:8004/api/robot/register"
        self.user_info_api_url = "http://robot-service:8004/api/robot/astron-agent/get-user-id"

    async def _call_register_api(self, phone: str) -> str:
        """
        호출외부모듈연결행사용자회원가입

        입력매개: phone (휴대폰 번호)
        출력매개: user_id (사용자ID)

        외부모듈연결반환형식:
        {
          "code": "000000",
          "data": {
           "userId":"1cb222f6-6ae1-4d6e-a8d7-709374c02821",
            "account": "1234567890",
            "password": "y3#J3vm!4hJ8k2v",
            "url":"https://xxxxxxxxxxx"
          },
          "message": ""
        }
        """
        try:
            async with httpx.AsyncClient() as client:
                # 생성요청  URL
                url = f"{self.register_api_url}?phone={phone}"
                logger.info("호출외부모듈회원가입연결, phone: %s, url: %s", phone, url)

                # 전송 POST 요청 
                response = await client.post(url, timeout=10.0)

                # 조회 HTTP 상태코드
                if response.status_code != 200:
                    logger.error(
                        "외부모듈연결반환예외상태코드, phone: %s, status_code: %s, response: %s",
                        phone,
                        response.status_code,
                        response.text,
                    )
                    return None

                # 파싱반환데이터
                response_data = response.json()
                logger.info(
                    "외부모듈연결반환데이터, phone: %s, response: %s",
                    phone,
                    json.dumps(response_data, ensure_ascii=False),
                )

                # 가져오기 의사용자 정보
                user_data = response_data.get("data", {})
                user_id = user_data.get("userId")
                if not user_id:
                    logger.error("외부모듈연결반환데이터중적음 userId, phone: %s", phone)
                    return None

                logger.info("외부모듈연결반환성공, phone: %s, user_id: %s", phone, user_id)
                return user_data

        except httpx.TimeoutException as e:
            logger.exception("호출외부모듈연결시간 초과, phone: %s", phone)
            return None
        except httpx.RequestError as e:
            logger.exception("호출외부모듈연결요청 오류, phone: %s", phone)
            return None
        except json.JSONDecodeError as e:
            logger.exception("파싱외부모듈연결반환데이터실패, phone: %s", phone)
            return None
        except Exception as e:
            logger.exception("호출외부모듈연결발송예외, phone: %s", phone)
            return None

    async def _call_user_info_api(self, phone: str) -> Optional[dict]:
        """
        호출외부모듈연결행사용자회원가입

        입력매개: phone (휴대폰 번호)
        출력매개: user_id (사용자ID)

        외부모듈연결반환형식:
        {
          "code": "000000",
          "data": {
            "userId":"1cb222f6-6ae1-4d6e-a8d7-709374c02821",
          },
          "message": ""
        }
        """
        try:
            async with httpx.AsyncClient() as client:
                # 사용URL, 통신경과JSONphone매개변수
                url = self.user_info_api_url
                logger.info("호출외부모듈회원가입연결, phone: %s, url: %s", phone, url)

                # 전송 POST 요청 , phone통신경과JSON
                response = await client.post(
                    url, json={"phone": phone}, headers={"X-API-Key": "opensource666!"}, timeout=10.0
                )

                # 조회 HTTP 상태코드
                if response.status_code != 200:
                    logger.error(
                        "외부모듈연결반환예외상태코드, phone: %s, status_code: %s, response: %s",
                        phone,
                        response.status_code,
                        response.text,
                    )
                    return None

                # 파싱반환데이터
                response_data = response.json()
                logger.info(
                    "외부모듈연결반환데이터, phone: %s, response: %s",
                    phone,
                    json.dumps(response_data, ensure_ascii=False),
                )
                if response_data.get("code") == "500000":
                    logger.error("조회사용자 정보오류, %s", response_data.get("message"))
                    return None

                # 가져오기 의사용자 정보
                user_data = response_data.get("data", {})
                user_id = user_data.get("userId", None)
                if not user_id:
                    logger.error("외부모듈연결반환데이터중적음 userId, phone: %s", phone)
                    return None

                logger.info("외부모듈연결반환성공, phone: %s, user_id: %s", phone, user_id)
                return user_data

        except httpx.TimeoutException as e:
            logger.exception("호출외부모듈연결시간 초과, phone: %s", phone)
            return None
        except httpx.RequestError as e:
            logger.exception("호출외부모듈연결요청 오류, phone: %s", phone)
            return None
        except json.JSONDecodeError as e:
            logger.exception("파싱외부모듈연결반환데이터실패, phone: %s", phone)
            return None
        except Exception as e:
            logger.exception("호출외부모듈연결발송예외, phone: %s", phone)
            return None

    async def register_user(self, phone: str) -> Optional[dict]:
        """
        사용자회원가입방법법

        입력매개: phone (휴대폰 번호)
        출력매개: dict 패키지 User 객체및외부모듈연결반환의사용자데이터

        프로세스:
        1. 근거휴대폰 번호조회사용자여부존재함
        2. 결과가존재함, 반환사용자 정보
        3. 결과가존재하지 않음, 호출외부모듈연결가져오기user_id닫기정보
        4. 완료API Key
        5. 생성새사용자저장까지데이터베이스
        """
        # 조회휴대폰 번호여부완료존재함
        query = select(User).where(User.phone == phone)
        result = await self.db.execute(query)
        existing_user = result.scalars().first()

        # 결과가사용자완료존재함, 직선연결반환
        if existing_user:
            logger.info("사용자완료존재함, phone: %s, user_id: %s", phone, existing_user.user_id)
            return {
                "user_id": existing_user.user_id,
                "api_key": existing_user.default_api_key,
            }

        # 호출외부모듈연결가져오기user_id닫기정보
        logger.info("열기 회원가입새사용자, phone: %s", phone)
        user_data = await self._call_register_api(phone)

        if not user_data:
            logger.error("외부모듈연결호출실패, phone: %s", phone)
            return None

        user_id = user_data.get("userId")

        # 완료API Key
        default_api_key = None
        if self.api_key_service:
            try:
                api_key_data = ApiKeyCreate(name=f"default_key_{phone}")
                default_api_key = await self.api_key_service.create_api_key(api_key_data, user_id)
                logger.info("로사용자완료API Key, user_id: %s", user_id)
            except Exception as e:
                logger.exception("완료API Key실패, user_id: %s", user_id)
                # API Key완료실패아니오사용자생성

        # 생성새사용자 (저장 user_id, phone, default_api_key)
        new_user = User(
            user_id=user_id,
            phone=phone,
            default_api_key=default_api_key,
            created_at=datetime.now(pytz.timezone("Asia/Seoul")).replace(tzinfo=None),
            updated_at=datetime.now(pytz.timezone("Asia/Seoul")).replace(tzinfo=None),
        )

        # 저장까지데이터베이스
        self.db.add(new_user)
        await self.db.flush()
        await self.db.refresh(new_user)

        logger.info("사용자회원가입성공, phone: %s, user_id: %s", phone, user_id)

        # 반환패키지 User 객체및사용자데이터의딕셔너리
        return {
            "user_id": user_id,
            "api_key": default_api_key,
            "password": user_data.get("password"),
            "url": user_data.get("url"),
            "account": user_data.get("account"),
        }

    async def get_user_info(self, phone: str) -> Optional[dict]:
        """
        가져오기사용자 정보

        입력매개: phone (휴대폰 번호)
        출력매개: user_id

        """
        # 조회휴대폰 번호여부완료존재함
        query = select(User).where(User.phone == phone)
        result = await self.db.execute(query)
        existing_user = result.scalars().first()

        # 결과가사용자완료존재함, 직선연결반환
        if existing_user:
            logger.info("사용자완료존재함, phone: %s, user_id: %s", phone, existing_user.user_id)
            return {
                "user_id": existing_user.user_id,
                "api_key": existing_user.default_api_key,
            }

        # 호출외부모듈연결가져오기user_id닫기정보
        logger.info("열기 가져오기새사용자, phone: %s", phone)
        user_data = await self._call_user_info_api(phone)

        if not user_data:
            logger.error("외부모듈연결호출실패, phone: %s", phone)
            return None

        user_id = user_data.get("userId")

        # 완료API Key
        default_api_key = None
        if self.api_key_service:
            try:
                api_key_data = ApiKeyCreate(name=f"default_key_{phone}")
                default_api_key = await self.api_key_service.create_api_key(api_key_data, user_id)
                logger.info("로사용자완료API Key, user_id: %s", user_id)
            except Exception as e:
                logger.exception("완료API Key실패, user_id: %s", user_id)
                # API Key완료실패아니오사용자생성

        # 생성새사용자 (저장 user_id, phone, default_api_key)
        new_user = User(
            user_id=user_id,
            phone=phone,
            default_api_key=default_api_key,
            created_at=datetime.now(pytz.timezone("Asia/Seoul")).replace(tzinfo=None),
            updated_at=datetime.now(pytz.timezone("Asia/Seoul")).replace(tzinfo=None),
        )

        # 저장까지데이터베이스
        self.db.add(new_user)
        await self.db.flush()
        await self.db.refresh(new_user)

        logger.info("사용자가져오기key성공, phone: %s, user_id: %s", phone, user_id)

        # 반환패키지 User 객체및사용자데이터의딕셔너리
        return {"user_id": user_id, "api_key": default_api_key}

    async def get_user_by_phone(self, phone: str) -> Optional[User]:
        """근거휴대폰 번호가져오기사용자 정보"""
        query = select(User).where(User.phone == phone)
        result = await self.db.execute(query)
        return result.scalars().first()

    async def get_user_by_user_id(self, user_id: str) -> Optional[User]:
        """근거user_id가져오기사용자 정보"""
        query = select(User).where(User.user_id == user_id)
        result = await self.db.execute(query)
        return result.scalars().first()