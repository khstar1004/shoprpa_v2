from datetime import UTC, datetime, timedelta

from redis.asyncio import Redis
from sqlalchemy import select
from sqlalchemy.ext.asyncio import (
    AsyncSession,
)

from app.config import get_settings
from app.logger import get_logger
from app.models.point import (
    PointAllocation,
    PointConsumption,
    PointExpirationPolicy,
    PointTransaction,
    PointTransactionType,
    calculate_expiration_date,
)

logger = get_logger(__name__)


class UserPointService:
    def __init__(self, db: AsyncSession, redis: Redis):
        self.db = db
        self.redis = redis

    async def get_cached_points(self, user_id: str) -> int:
        try:
            cached_points = await self.redis.get(f"user_points:{user_id}")
            if cached_points is not None:
                return int(cached_points)
        except Exception as e:
            logger.error("Error fetching cached points: %s", e)

        user_point = await self._calculate_user_points(user_id)
        await self._set_cached_points(user_id, user_point)

        return user_point

    async def _set_cached_points(self, user_id: str, points: int):
        try:
            await self.redis.set(f"user_points:{user_id}", points, ex=3600)
        except Exception as e:
            logger.error("Error setting cached points: %s", e)

    async def _clear_cached_points(self, user_id: str):
        try:
            await self.redis.delete(f"user_points:{user_id}")
        except Exception as e:
            logger.error("Error clearing cached points: %s", e)

    async def _get_available_allocations(self, user_id: str):
        current_time = datetime.now(UTC)
        query = (
            select(PointAllocation)
            .where(
                PointAllocation.user_id == user_id,
                PointAllocation.remaining_amount > 0,
                PointAllocation.expires_at > current_time,
            )
            .order_by(PointAllocation.priority.desc(), PointAllocation.expires_at.asc())
        )
        result = await self.db.execute(query)
        allocations = result.scalars().all()
        return allocations

    async def _calculate_user_points(self, user_id: str):
        allocations = await self._get_available_allocations(user_id)
        total_points = sum(allocation.remaining_amount for allocation in allocations)
        return total_points

    def _get_priority_for_type(self, allocation_type: PointTransactionType) -> int:
        """Return consumption priority for an allocation type."""
        if allocation_type == PointTransactionType.MONTHLY_GRANT:
            return 50
        elif allocation_type == PointTransactionType.MANUAL_ADD:
            return 10
        else:
            return 20

    async def create_point_allocation(
        self,
        user_id: str,
        amount: int,
        allocation_type: PointTransactionType,
        description: str = None,
        expiration_policy: PointExpirationPolicy = None,
        fixed_expiry_date: datetime = None,
    ):
        """Create a new point allocation with expiration based on policy"""

        # Calculate expiration date
        if fixed_expiry_date and expiration_policy == PointExpirationPolicy.FIXED_DATE:
            expires_at = fixed_expiry_date
        else:
            expires_at = calculate_expiration_date(allocation_type, expiration_policy)
        try:
            # Create the allocation
            allocation = PointAllocation(
                user_id=user_id,
                initial_amount=amount,
                remaining_amount=amount,
                allocation_type=allocation_type.value,
                expires_at=expires_at,
                description=description,
                priority=self._get_priority_for_type(allocation_type),
            )
            self.db.add(allocation)
            await self.db.flush()

            # Create the transaction
            transaction = PointTransaction(
                user_id=user_id,
                amount=amount,
                transaction_type=allocation_type.value,
                related_entity_type="PointAllocation",
                related_entity_id=allocation.id,
                description=description,
            )
            self.db.add(transaction)

            # Clear the user's cached balance
            await self._clear_cached_points(user_id)

            await self.db.flush()
            await self.db.commit()
            return allocation

        except Exception as e:
            await self.db.rollback()
            raise

    async def grant_monthly_points(self, user_id: str):
        current_time = datetime.now(UTC)
        redis_key = f"points_monthly_grant:{user_id}:{current_time.year}-{current_time.month}"

        logger.info("Checking monthly point grant")

        try:
            already_granted = await self.redis.get(redis_key)
            if already_granted:
                logger.info("Monthly point grant already marked in Redis")
                return None
        except Exception as e:
            logger.error("Error checking Redis for monthly grant: %s", e)

        month_start = current_time.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        month_end = (month_start + timedelta(days=32)).replace(day=1) - timedelta(seconds=1)

        logger.info("Querying database for existing grants...")

        existing_grant = await self.db.execute(
            select(PointAllocation).where(
                PointAllocation.user_id == user_id,
                PointAllocation.allocation_type == PointTransactionType.MONTHLY_GRANT.value,
                PointAllocation.created_at.between(month_start, month_end),
            )
        )
        existing_grant = existing_grant.scalars().first()

        if existing_grant:
            logger.info("Existing monthly grant found")
            try:
                await self.redis.set(redis_key, "1", ex=2678400)
            except Exception as e:
                logger.error("Error setting Redis key after finding existing grant: %s", e)
            return None

        settings = get_settings()
        monthly_points = getattr(settings, "MONTHLY_POINTS", 100000)

        logger.info("Granting monthly points: %s", monthly_points)

        allocation = await self.create_point_allocation(
            user_id=user_id,
            amount=monthly_points,
            allocation_type=PointTransactionType.MONTHLY_GRANT,
            description=f"Monthly grant for {current_time.year}-{current_time.month}",
            expiration_policy=PointExpirationPolicy.END_OF_THIS_MONTH,
        )

        try:
            await self.redis.set(redis_key, "1", ex=2678400)
            logger.info("Monthly point grant marked in Redis")
        except Exception as e:
            logger.error("Error setting Redis key: %s", e)

        return allocation

    async def manual_add_points(self, user_id: str, amount: int):
        return await self.create_point_allocation(
            user_id=user_id,
            amount=amount,
            allocation_type=PointTransactionType.MANUAL_ADD,
        )

    async def deduct_points(
        self,
        user_id: str,
        amount: int,
        transaction_type: PointTransactionType,
        description: str = None,
        related_entity_type: str = None,
        related_entity_id: int = None,
    ) -> PointTransaction:
        if amount <= 0:
            raise ValueError("Amount must be greater than zero.")

        allocations = await self._get_available_allocations(user_id)

        available_points = sum(allocation.remaining_amount for allocation in allocations)

        if available_points < amount:
            raise InsufficientPointsError(
                f"User {user_id} has only {available_points} points, but {amount} are required."
            )

        transaction = PointTransaction(
            user_id=user_id,
            amount=-amount,
            transaction_type=transaction_type.value,
            related_entity_type=related_entity_type,
            related_entity_id=related_entity_id,
            description=description,
        )
        self.db.add(transaction)
        await self.db.flush()

        remaining_to_deduct = amount
        consumptions = []

        for allocation in allocations:
            if remaining_to_deduct <= 0:
                break

            consumption_amount = min(allocation.remaining_amount, remaining_to_deduct)

            consumption = PointConsumption(
                transaction_id=transaction.id,
                allocation_id=allocation.id,
                amount=consumption_amount,
            )
            self.db.add(consumption)
            consumptions.append(consumption)

            allocation.remaining_amount -= consumption_amount

            remaining_to_deduct -= consumption_amount
        await self.db.flush()

        user_point = await self.get_cached_points(user_id)
        user_point -= amount
        await self._set_cached_points(user_id, user_point)

        await self.db.flush()

        return transaction


class InsufficientPointsError(Exception):
    """Raised when the user does not have enough points."""

    pass
