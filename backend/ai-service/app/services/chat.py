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
    connect=10.0,  # 연결시간 초과: 10초
    read=360.0,  # 가져오기시간 초과: 6분
    write=10.0,  # 입력시간 초과: 10초
    pool=320.0,  # 시간 초과: 6분20초
)


async def chat_completions(params: ChatCompletionParam, key: str = API_KEY, endpoint: str = API_ENDPOINT):
    logger.info("Processing chat completion request...")
    logger.info(f"Request params: {params}")
    # 요청 매개변수
    headers = {
        "Authorization": f"Bearer {key}",
        "Content-Type": "application/json",
    }
    data = params.model_dump(exclude_none=True)

    try:
        for message in data["messages"]:
            if not message.get("content"):
                message["content"] = "You are a helpful assistant."
        logger.info(f"Request data: {data}")
    except KeyError:
        raise HTTPException(status_code=400, detail="Invalid request body")

    logger.info(f"Request headers: {headers}")
    logger.info(f"Request params.stream: {params.stream}")
    # 관리요청 
    try:
        if params.stream:
            response = await handle_stream_request(headers, data, endpoint)
        else:
            response = await handle_non_stream_request(headers, data, endpoint)
        logger.info(f"Response: {response}")

        return response
    except HTTPException as e:
        logger.warning(f"HTTP error: {e.detail}")
        raise e
    except Exception:
        logger.error("Internal server error", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal Server Error")


async def handle_stream_request(headers, data, endpoint):
    """관리방식요청 """
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
                # 위API오류
                logger.warning(f"Upstream API error: {e.response.status_code}")
                raise HTTPException(
                    status_code=e.response.status_code,
                    detail=f"Upstream API error: {e.response.status_code}",
                )
            except httpx.TimeoutException as e:
                # 시간 초과오류
                logger.error(f"Request timeout: {str(e)}")
                raise HTTPException(
                    status_code=504,
                    detail="대유형호출시간 초과, 요청 후재시도",
                )
            except Exception as e:
                # 오류
                logger.error(f"Request error: {str(e)}")
                raise e

    return StreamingResponse(
        content=stream_response(),
        media_type=response_meta["media_type"],
    )


async def handle_non_stream_request(headers, data, endpoint):
    """관리방식요청 """
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
            # 가져오기요청 의 URL 및 방법법
            url = e.request.url
            method = e.request.method

            # 가져오기위반환의내용 (통신일반패키지의오류원인, 예 "Invalid API Key")
            error_content = e.response.text

            # 기록로그
            logger.error(
                f"Upstream API error: {e.response.status_code} | Request: {method} {url} | Response: {error_content}"
            )

            # 위출력예외
            raise HTTPException(
                status_code=e.response.status_code,
                detail=f"Upstream API error: {e.response.status_code}",
            )
        except httpx.TimeoutException as e:
            # 시간 초과오류
            logger.error(f"Request timeout: {str(e)}")
            raise HTTPException(
                status_code=504,
                detail="대유형호출시간 초과, 요청 후재시도",
            )
        except Exception as e:
            # 오류
            logger.error(f"Request error: {str(e)}")
            raise e