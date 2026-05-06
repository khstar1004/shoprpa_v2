from enum import Enum
from typing import Any, Optional

from pydantic import BaseModel, Field


class ResCode(Enum):
    ERR = "5001"
    SUCCESS = "0000"


class StandardResponse(BaseModel):
    """유형"""

    code: ResCode = Field(ResCode.SUCCESS, description="코드")
    msg: str = Field("", description="메시지")
    data: Optional[Any] = Field(None, description="데이터")