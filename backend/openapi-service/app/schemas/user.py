from pydantic import BaseModel, Field


class UserRegisterRequest(BaseModel):
    """Request body for user registration."""

    phone: str = Field(..., description="User phone number", min_length=1, max_length=20)


class UserRegisterResponse(BaseModel):
    """Registration response with the generated API credentials."""

    user_id: str = Field(..., description="User ID")
    api_key: str = Field(..., description="API Key")
    account: str = Field(..., description="Account")
    password: str = Field(..., description="Password")
    url: str = Field(..., description="Download URL")


class UserAPIKeyResponse(BaseModel):
    """Response containing an existing user API key."""

    user_id: str = Field(..., description="User ID")
    api_key: str = Field(..., description="API Key")
