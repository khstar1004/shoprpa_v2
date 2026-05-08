import json

import httpx

from app.config import get_settings
from app.logger import get_logger
from app.schemas.jfbym import JFBYMGeneralResponseBody

API_ENDPOINT = get_settings().JFBYM_ENDPOINT
API_TOKEN = get_settings().JFBYM_API_TOKEN

logger = get_logger(__name__)


class CaptchaVerificationError(Exception):
    """Exception raised for errors in the CAPTCHA verification process."""

    def __init__(self, message: str):
        super().__init__(message)
        self.message = message


async def verify_captcha(type: str, image: str, direction: str = "") -> JFBYMGeneralResponseBody:
    """Verify a CAPTCHA image using the JFBYM service.

    Args:
        type: Type ID of the CAPTCHA verification
        image: Base64-encoded string of the CAPTCHA image

    Returns:
        Dictionary containing the verification result
    """
    payload = {
        "token": API_TOKEN,
        "type": type,
        "image": image,
    }

    if direction:
        payload["direction"] = direction

    async with httpx.AsyncClient() as client:
        try:
            response = await client.post(API_ENDPOINT, json=payload, timeout=30.0)
            response.raise_for_status()
            response_data = response.json()
            logger.info("JFBYM response received. code=%s", response_data.get("code"))
            model = JFBYMGeneralResponseBody.model_validate(response_data)
            return model

        except httpx.HTTPError as e:
            logger.error("HTTP error during CAPTCHA request: %s", e)
            raise
        except json.JSONDecodeError as e:
            logger.error("Failed to decode CAPTCHA response: %s", e)
            raise CaptchaVerificationError("Invalid response format") from e
        except Exception as e:
            logger.error("Unexpected error during CAPTCHA verification: %s", e)
            raise CaptchaVerificationError(f"Unexpected error: {str(e)}")
