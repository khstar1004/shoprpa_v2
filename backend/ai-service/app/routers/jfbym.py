import httpx
from fastapi import APIRouter, Depends

from app.config import get_settings
from app.dependencies.points import PointChecker, PointsContext
from app.logger import get_logger
from app.schemas.jfbym import JFBYMGeneralRequestBody, JFBYMGeneralResponseBody
from app.services.point import PointTransactionType
from app.utils.jfbym import CaptchaVerificationError, verify_captcha

logger = get_logger(__name__)

router = APIRouter(
    prefix="/jfbym",
    tags=["captcha"],
)


@router.post("/customApi", response_model=JFBYMGeneralResponseBody)
async def general(
    params: JFBYMGeneralRequestBody,
    points_context: PointsContext = Depends(
        PointChecker(get_settings().JFBYM_POINTS_COST, PointTransactionType.JFBYM_COST),
    ),
):
    try:
        payload = params.model_dump(exclude_none=True)
        logger.info("JFBYM processing request. type=%s, direction=%s", params.type, params.direction)
        result = await verify_captcha(**payload)
        if result.code == 10000 and result.data and result.data.code == 0:
            await points_context.deduct_points()
            logger.info("JFBYM processing successful, points deducted for user")
        else:
            error_message = result.msg or "Unknown API error"
            logger.warning("JFBYM API returned error: %s", error_message)
        return result

    except CaptchaVerificationError as e:
        logger.error("JFBYM business logic error: %s", e.message)
        return JFBYMGeneralResponseBody(code=400, msg=f"CAPTCHA verification failed: {e.message}", data=None)

    except httpx.HTTPError as e:
        logger.error("JFBYM service network error: %s", e)
        return JFBYMGeneralResponseBody(code=503, msg="CAPTCHA service is temporarily unavailable.", data=None)

    except Exception as e:
        logger.error("Unexpected error in JFBYM processing: %s", e)
        return JFBYMGeneralResponseBody(code=500, msg="Unexpected error during CAPTCHA verification.", data=None)
