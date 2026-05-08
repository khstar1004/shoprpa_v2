import httpx
from fastapi import APIRouter, Depends, HTTPException

from app.config import get_settings
from app.dependencies.points import PointChecker, PointsContext
from app.logger import get_logger
from app.schemas.ocr import OCRGeneralRequestBody, OCRGeneralResponseBody
from app.services.point import PointTransactionType
from app.utils.ocr import OCRError, recognize_text_from_image

logger = get_logger(__name__)

router = APIRouter(
    prefix="/ocr",
    tags=["ocr"],
)


@router.post("/general", response_model=OCRGeneralResponseBody)
async def general_ocr(
    params: OCRGeneralRequestBody,
    points_context: PointsContext = Depends(
        PointChecker(get_settings().OCR_GENERAL_POINTS_COST, PointTransactionType.XFYUN_COST),
    ),
):
    """
    Perform OCR on an image using Xunfei's general text recognition API.

    Returns:
        OCR result in the following format:
        {
          "header": {
            "code": 0,
            "message": "success",
            "sid": "ase000d1688@hu17b34308ea40210882"
          },
          "payload": {
            "result": {
              "compress": "raw",
              "encoding": "utf8",
              "format": "json",
              "text": "ewogImNhdGVnb3J5IjogImNoX2VuX3B1YmxpY19jbG91ZC..."
            }
          }
        }

    Raises:
        HTTPException: 400 for invalid requests, 500 for server errors, 503 for network issues
    """
    try:
        result = await recognize_text_from_image(params.image, params.encoding, params.status)

        if result.header.code == 0:
            await points_context.deduct_points()
            logger.info("OCR processing successful, points deducted for user")

        return result

    except OCRError as e:
        logger.error("OCR business logic error: %s", e.message)
        raise HTTPException(status_code=400, detail=f"OCR processing failed: {e.message}")

    except httpx.HTTPError as e:
        logger.error("OCR service network error: %s", e)
        raise HTTPException(
            status_code=503,
            detail="OCR service is temporarily unavailable. Please try again later.",
        )

    except Exception as e:
        logger.error("Unexpected error in OCR processing: %s", e)
        raise HTTPException(status_code=500, detail="An unexpected error occurred during OCR processing")
