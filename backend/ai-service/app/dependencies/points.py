from fastapi import Depends, HTTPException

from app.dependencies import get_user_id_from_header, get_user_point_service
from app.logger import get_logger
from app.services.point import (
    InsufficientPointsError,
    PointTransactionType,
    UserPointService,
)

logger = get_logger(__name__)


class PointChecker:
    def __init__(self, points_cost: int, transaction_type: PointTransactionType):
        self.points_cost = points_cost
        self.transaction_type = transaction_type

    async def __call__(
        self,
        current_user_id: str = Depends(get_user_id_from_header),
        userpoints_service: UserPointService = Depends(get_user_point_service),
    ):
        logger.info("Checking points call...")
        try:
            # 시도증가추가분, 내부모듈사용완료 Redis, 가능열기판매소
            await userpoints_service.grant_monthly_points(current_user_id)

            # 조회사용자분
            points = await userpoints_service.get_cached_points(current_user_id)
            if points < self.points_cost:
                raise HTTPException(
                    status_code=403,
                    detail="Insufficient points.",
                )

            # 반환패키지사용자 정보및제거분방법법의객체
            return PointsContext(
                user_id=current_user_id,
                service=userpoints_service,
                points_cost=self.points_cost,
                transaction_type=self.transaction_type,
            )
        except Exception as e:
            logger.error(f"Failed to check points: {str(e)}")
            raise e


class PointsContext:
    def __init__(
        self,
        user_id: str,
        service: UserPointService,
        points_cost: int,
        transaction_type: PointTransactionType,
    ):
        self.user_id = user_id
        self.service = service
        self.points_cost = points_cost
        self.transaction_type = transaction_type

    async def deduct_points(self):
        """제거분"""
        try:
            await self.service.deduct_points(
                user_id=self.user_id,
                amount=self.points_cost,
                transaction_type=self.transaction_type,
            )
            return True
        except InsufficientPointsError:
            raise HTTPException(
                status_code=403,
                detail="Insufficient points.",
            )
        except Exception as e:
            raise HTTPException(
                status_code=500,
                detail=f"Failed to deduct points: {str(e)}",
            )