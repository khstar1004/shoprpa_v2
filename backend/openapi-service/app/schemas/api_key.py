from typing import Optional, Union

from pydantic import BaseModel, Field


class ApiKeyCreate(BaseModel):
    """생성API Key요청 유형"""

    name: str = Field(..., description="API Key이름", min_length=1, max_length=100)


class ApiKeyDelete(BaseModel):
    """삭제API Key요청 유형"""

    id: Union[int, str] = Field(..., description="API Key ID")


# class ApiKeyResponse(BaseModel):
#     """API Key유형"""
#     id: int = Field(..., description="API Key ID")
#     api_key: str = Field(..., description="API Key 값(코드)")
#     name: str = Field(..., description="API Key이름")
#     createTime: datetime = Field(..., description="생성 시간")
#     recentTime: datetime = Field(..., description="수정 시간")

#     model_config = {"from_attributes": True}


class ShoprpaAgentCreate(BaseModel):
    """생성ShoprpaAgent요청 유형"""

    api_key: str = Field(..., description="API Key", min_length=1, max_length=100)
    api_secret: str = Field(..., description="API Secret", min_length=1, max_length=100)
    app_id: Optional[str] = Field(None, description="사용ID", min_length=1, max_length=100)
    name: str = Field(..., description="사용자명명칭", min_length=1, max_length=100)


class ShoprpaAgentDelete(BaseModel):
    """삭제ShoprpaAgent요청 유형"""

    id: Union[int, str] = Field(..., description="ShoprpaAgent ID")


class ShoprpaAgentUpdate(BaseModel):
    """업데이트ShoprpaAgent요청 유형"""

    id: Union[int, str] = Field(..., description="ShoprpaAgent ID")
    name: Optional[str] = Field(None, description="사용자명명칭", min_length=1, max_length=100)
    app_id: Optional[str] = Field(None, description="사용ID", min_length=1, max_length=100)
    api_key: Optional[str] = Field(None, description="API Key", min_length=1, max_length=100)
    api_secret: Optional[str] = Field(None, description="API Secret", min_length=1, max_length=100)