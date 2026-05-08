from typing import Optional

from pydantic import BaseModel, Field


class JFBYMGeneralRequestBody(BaseModel):
    image: str = Field(
        ...,
        description="Base64-encoded CAPTCHA image",
        examples=["iVBORw0KGgoAAAANSUhEUgAA..."],
    )
    type: str = Field(..., description="CAPTCHA type", examples=["..."])
    direction: Optional[str] = Field("bottom", description="Image direction", examples=["bottom"])


class JFBYMGeneralResponseInnerData(BaseModel):
    code: int
    data: str
    time: float
    unique_code: str


class JFBYMGeneralResponseBody(BaseModel):
    code: int = Field(
        ...,
        description="Status code. 10000 means success.",
        examples=[10000],
    )
    msg: str = Field(
        ...,
        description="Response message",
        examples=["success"],
    )
    data: Optional[JFBYMGeneralResponseInnerData] = Field(
        None,
        description="CAPTCHA result data",
        examples=[
            {
                "code": 0,
                "data": "5298",
                "time": 0.03829169273376465,
                "unique_code": "56a74a3b9b9b796b3ec554832c1cccbb",
            },
        ],
    )
