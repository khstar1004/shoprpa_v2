import json

from fastapi import APIRouter

from app.models.smart_component import SmartChatRequest, SmartChatResponse, SmartElementInfo
from app.schemas.chat import ChatCompletionParam
from app.services.chat import chat_completions
from app.utils.prompt import format_prompt

router = APIRouter(
    prefix="/smart",
    tags=["가능컴포넌트서비스"],
)


def build_messages(request: SmartChatRequest) -> list[dict]:
    def format_elements(elements: list[SmartElementInfo]) -> str:
        if not elements:
            return ""
        template = """\n
`{name}` 의 요소ID 로 `{elementId}`
`{name}` 의 XPath 로 `{XPath}`
`{name}` 의 스크린샷 로 `{imageUrl}`
`{name}` 의 outerHTML 로\n```\n{outerHTML}\n```"""
        formatted = []
        for element in elements:
            formatted.append(
                template.format(
                    name=element.name,
                    elementId=element.elementId,
                    XPath=element.xpath,
                    imageUrl=element.imageUrl,
                    outerHTML=element.outerHtml,
                )
            )
        return "".join(formatted)

    prompt = format_prompt(request.sceneCode, {})

    messages = [
        {
            "role": "system",
            "content": prompt,
        }
    ]

    # 관리새의형식
    for history in request.chatHistory or []:
        # 생성메시지내용
        content_text = ""
        if history.role == "user":
            content_text += history.content.user
        elif history.role == "assistant":
            if history.content.smartCode is not None and history.content.smartCode != "":
                content_text += f"""```smart_code\n{history.content.smartCode}\n```\n{history.content.text}
                """
            else:
                content_text += history.content.text

        # 결과가있음원정보, 추가까지내용중
        if history.content.elements:
            content_text += format_elements(history.content.elements)

        messages.append({"role": history.role, "content": content_text})

    # 관리현재사용자메시지으로예외복사메시지
    if request.needFix:
        current_content = f"요청 근거코드실행의오류 현재코드행복사\n{request.fixInfo.traceback if request.fixInfo is not None else ''}\n### 현재코드\n{request.currentCode}"
    else:
        current_content = request.user or ""
    if request.elements:
        current_content += format_elements(request.elements)

    messages.append({"role": "user", "content": current_content})
    return messages


@router.post("/chat/stream")
async def smart_chat_stream(request: SmartChatRequest):
    llm_params = ChatCompletionParam(
        # model='claude-4.5-sonnet',
        model="maas/deepseek-v3.2",
        stream=True,
        temperature=0.15,
        max_tokens=8192,
        messages=build_messages(request),
    )

    return await chat_completions(llm_params)


@router.post("/chat", response_model=SmartChatResponse)
async def smart_chat(request: SmartChatRequest):
    llm_params = ChatCompletionParam(
        # model='claude-4.5-sonnet',
        model="maas/deepseek-v3.2",
        stream=False,
        temperature=0.15,
        max_tokens=8192,
        messages=build_messages(request),
    )

    chat_result = await chat_completions(llm_params)

    return SmartChatResponse(data=json.loads(chat_result.body), code=200, success=True)