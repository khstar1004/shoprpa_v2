from typing import Any, Optional

from pydantic import BaseModel


# 요소본정보
class SmartElementInfo(BaseModel):
    name: Optional[str] = None
    imageUrl: Optional[str] = None
    xpath: Optional[str] = None
    outerHtml: Optional[str] = None
    elementId: Optional[str] = None


# 내용결과
class SmartChatContent(BaseModel):
    smartCode: Optional[str] = None
    user: Optional[str] = None
    text: Optional[str] = None
    status: Optional[str] = None  # 'generating', 'completed', 'error' 대기
    elements: Optional[list[SmartElementInfo]] = None
    tip: Optional[str] = None
    # 가능으로추가변경다중필드
    metadata: Optional[dict] = None


# 기록 - 새형식
class SmartChatHistoryItem(BaseModel):
    role: Optional[str] = None
    content: Optional[SmartChatContent] = None


# 예외복사정보
class SmartFixInfo(BaseModel):
    traceback: Optional[str] = None
    consoleLog: Optional[str] = None


# 요청 매개변수
class SmartChatRequest(BaseModel):
    sceneCode: Optional[str] = None
    user: Optional[str] = None
    needFix: Optional[bool] = None
    fixInfo: Optional[SmartFixInfo] = None
    currentCode: Optional[str] = None
    elements: Optional[list[SmartElementInfo]] = None
    chatHistory: Optional[list[SmartChatHistoryItem]] = None


class SmartChatResponse(BaseModel):
    data: Optional[Any] = None
    code: Optional[int] = None
    success: Optional[bool] = None