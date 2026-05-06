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
    tags=["코드인증코드"],
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
        logger.info(f"JFBYM processing request: {payload}")
        result = await verify_captcha(**payload)
        if result.code == 10000 and result.data.code == 0:
            # 성공시제거분
            await points_context.deduct_points()
            logger.info("JFBYM processing successful, points deducted for user")
        else:
            # API반환오류, 아니오제거분
            error_message = result.get("message", "Unknown API error")
            logger.warning(f"JFBYM API returned error: {error_message}")
        return result

    except CaptchaVerificationError as e:
        # 서비스오류 - 반환오류정보
        logger.error(f"JFBYM business logic error: {e.message}")
        return JFBYMGeneralResponseBody(code=400, message=f"코드인증코드관리실패: {e.message}", data=None)

    except httpx.HTTPError as e:
        # 네트워크오류 - 반환오류정보
        logger.error(f"JFBYM service network error: {e}")
        return JFBYMGeneralResponseBody(code=503, message="코드인증코드서비스시할 수 없음사용, 요청 후재시도", data=None)

    except Exception as e:
        # 미완료의오류 - 반환오류정보
        logger.error(f"Unexpected error in JFBYM processing: {e}")
        return JFBYMGeneralResponseBody(code=500, message="코드인증코드관리경과중발송미완료알림오류", data=None)