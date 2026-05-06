from pydantic import BaseModel, Field


class UserRegisterRequest(BaseModel):
    """사용자회원가입요청 유형"""

    phone: str = Field(..., description="사용자휴대폰 번호", min_length=1, max_length=20)


class UserRegisterResponse(BaseModel):
    """사용자회원가입유형"""

    user_id: str = Field(..., description="사용자ID")
    api_key: str = Field(..., description="API Key")
    account: str = Field(..., description="계정")
    password: str = Field(..., description="비밀번호")
    url: str = Field(..., description="다운로드연결")


class UserAPIKeyResponse(BaseModel):
    """사용자회원가입유형"""

    user_id: str = Field(..., description="사용자ID")
    api_key: str = Field(..., description="API Key")