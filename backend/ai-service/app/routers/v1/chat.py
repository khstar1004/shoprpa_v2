import json
from urllib.parse import urljoin

from fastapi import APIRouter, Depends, HTTPException

from app.config import get_settings
from app.dependencies.points import PointChecker, PointsContext
from app.logger import get_logger
from app.schemas import ResCode, StandardResponse
from app.schemas.chat import ChatCompletionParam, ChatPromptParam
from app.services.chat import chat_completions
from app.services.point import PointTransactionType
from app.utils.prompt import format_prompt, get_available_prompts, prompt_dict

API_KEY = get_settings().AICHAT_API_KEY
API_ENDPOINT = urljoin(get_settings().AICHAT_BASE_URL, "chat/completions")

logger = get_logger(__name__)

router = APIRouter(
    prefix="/chat",
    tags=["시스템일대유형연결"],
)


@router.post("/completions")
async def chat(
    params: ChatCompletionParam,
    points_context: PointsContext = Depends(
        PointChecker(get_settings().AICHAT_POINTS_COST, PointTransactionType.AICHAT_COST),
    ),
):
    response = await chat_completions(params, API_KEY, API_ENDPOINT)

    # 관리성공, 제거분, 반환
    await points_context.deduct_points()
    return response


@router.post("/prompt")
async def chat_prompt(
    params: ChatPromptParam,
    points_context: PointsContext = Depends(
        PointChecker(get_settings().AICHAT_POINTS_COST, PointTransactionType.AICHAT_COST),
    ),
):
    """
    근거prompt호출대유형
    """
    logger.info(f"Processing chat prompt request: {params.prompt_type}")

    # 조회prompt유형여부존재함
    if params.prompt_type not in prompt_dict:
        raise HTTPException(
            status_code=400,
            detail=f"Unknown prompt type: {params.prompt_type}. Available types: {get_available_prompts()}",
        )

    # 형식prompt
    try:
        formatted_prompt = format_prompt(params.prompt_type, params.params or {})
        logger.info(f"Formatted prompt: {formatted_prompt[:100]}...")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    # 메시지
    messages = [{"role": "user", "content": formatted_prompt}]

    data = {
        "model": params.model,
        "messages": messages,
        "stream": params.stream,
    }

    logger.info(f"Request data: {data}")

    chat_model = ChatCompletionParam.model_validate(data)
    response = await chat_completions(chat_model, API_KEY, API_ENDPOINT)
    # logger.info("response: %s", response)

    await points_context.deduct_points()

    if params.stream:
        return response
    else:
        if isinstance(response.body, bytes):
            content_str = response.body.decode("utf-8")
        else:
            content_str = response.body

        # 파싱 JSON
        data = json.loads(content_str)

        # 결과가예방식, 파싱성공, data예결과
        return StandardResponse(
            code=ResCode.SUCCESS,
            msg="호출 {} prompt 성공".format(params.prompt_type),
            data=data["choices"][0]["message"]["content"],
        )