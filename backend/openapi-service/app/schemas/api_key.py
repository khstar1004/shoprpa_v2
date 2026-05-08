from typing import Optional, Union

from pydantic import BaseModel, Field


class ApiKeyCreate(BaseModel):
    """Create API key request."""

    name: str = Field(..., description="API key name", min_length=1, max_length=100)


class ApiKeyDelete(BaseModel):
    """Delete API key request."""

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
    """Create ShopRPA Agent credential request."""

    api_key: str = Field(..., description="API Key", min_length=1, max_length=100)
    api_secret: str = Field(..., description="API Secret", min_length=1, max_length=100)
    app_id: Optional[str] = Field(None, description="Application ID", min_length=1, max_length=100)
    name: str = Field(..., description="Credential name", min_length=1, max_length=100)


class ShoprpaAgentDelete(BaseModel):
    """Delete ShopRPA Agent credential request."""

    id: Union[int, str] = Field(..., description="ShopRPA Agent credential ID")


class ShoprpaAgentUpdate(BaseModel):
    """Update ShopRPA Agent credential request."""

    id: Union[int, str] = Field(..., description="ShopRPA Agent credential ID")
    name: Optional[str] = Field(None, description="Credential name", min_length=1, max_length=100)
    app_id: Optional[str] = Field(None, description="Application ID", min_length=1, max_length=100)
    api_key: Optional[str] = Field(None, description="API Key", min_length=1, max_length=100)
    api_secret: Optional[str] = Field(None, description="API Secret", min_length=1, max_length=100)
