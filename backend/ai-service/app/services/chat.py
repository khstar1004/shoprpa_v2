from urllib.parse import urljoin

import httpx
from fastapi import HTTPException
from fastapi.responses import Response, StreamingResponse

from app.config import get_settings
from app.logger import get_logger
from app.schemas.chat import ChatCompletionParam

API_KEY = get_settings().AICHAT_API_KEY
API_ENDPOINT = urljoin(get_settings().AICHAT_BASE_URL, "chat/completions")

logger = get_logger(__name__)

long_timeout = httpx.Timeout(
    connect=10.0,
    read=360.0,
    write=10.0,
    pool=320.0,
)


async def chat_completions(params: ChatCompletionParam, key: str = API_KEY, endpoint: str = API_ENDPOINT):
    logger.info("Processing chat completion request. stream=%s, model=%s", params.stream, params.model)
    headers = {
        "Authorization": f"Bearer {key}",
        "Content-Type": "application/json",
    }
    data = params.model_dump(exclude_none=True)

    try:
        for message in data["messages"]:
            if not message.get("content"):
                message["content"] = "You are a helpful assistant."
    except KeyError:
        raise HTTPException(status_code=400, detail="Invalid request body")

    try:
        if params.stream:
            response = await handle_stream_request(headers, data, endpoint)
        else:
            response = await handle_non_stream_request(headers, data, endpoint)

        return response
    except HTTPException as e:
        logger.warning("HTTP error: %s", e.detail)
        raise e
    except Exception:
        logger.error("Internal server error", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal Server Error")


async def handle_stream_request(headers, data, endpoint):
    """Proxy a streaming chat completion request."""
    response_meta = {"media_type": "text/event-stream"}

    async def stream_response():
        async with httpx.AsyncClient(timeout=60.0) as client:
            try:
                async with client.stream(
                    "POST",
                    endpoint,
                    headers=headers,
                    json=data,
                ) as upstream_response:
                    upstream_response.raise_for_status()
                    response_meta["media_type"] = upstream_response.headers.get("content-type", "text/event-stream")
                    async for chunk in upstream_response.aiter_raw():
                        yield chunk
            except httpx.HTTPStatusError as e:
                logger.warning("Upstream API error: %s", e.response.status_code)
                raise HTTPException(
                    status_code=e.response.status_code,
                    detail=f"Upstream API error: {e.response.status_code}",
                )
            except httpx.TimeoutException as e:
                logger.error("Request timeout: %s", str(e))
                raise HTTPException(
                    status_code=504,
                    detail="Chat completion timed out. Please try again.",
                )
            except Exception as e:
                logger.error("Request error: %s", str(e))
                raise e

    return StreamingResponse(
        content=stream_response(),
        media_type=response_meta["media_type"],
    )


async def handle_non_stream_request(headers, data, endpoint):
    """Proxy a non-streaming chat completion request."""
    async with httpx.AsyncClient(timeout=long_timeout) as client:
        try:
            upstream_response = await client.post(
                endpoint,
                headers=headers,
                json=data,
            )
            upstream_response.raise_for_status()

            return Response(
                content=upstream_response.content,
                media_type=upstream_response.headers.get("content-type"),
                status_code=upstream_response.status_code,
            )
        except httpx.HTTPStatusError as e:
            url = e.request.url
            method = e.request.method
            error_content = e.response.text

            logger.error(
                "Upstream API error: %s | Request: %s %s | Response: %s",
                e.response.status_code,
                method,
                url,
                error_content[:500],
            )

            raise HTTPException(
                status_code=e.response.status_code,
                detail=f"Upstream API error: {e.response.status_code}",
            )
        except httpx.TimeoutException as e:
            logger.error("Request timeout: %s", str(e))
            raise HTTPException(
                status_code=504,
                detail="Chat completion timed out. Please try again.",
            )
        except Exception as e:
            logger.error("Request error: %s", str(e))
            raise e
