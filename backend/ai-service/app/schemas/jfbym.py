from typing import Optional

from pydantic import BaseModel, Field


class JFBYMGeneralRequestBody(BaseModel):
    image: str = Field(
        ...,
        description="필요이미지의base64문자열",
        examples=["iVBORw0KGgoAAAANSUhEUgAA..."],
    )
    type: str = Field(..., description="인증코드유형", examples=["..."])
    direction: Optional[str] = Field("bottom", description="이미지방법", examples=["bottom"])


class JFBYMGeneralResponseInnerData(BaseModel):
    code: int
    data: str
    time: float
    unique_code: str


class JFBYMGeneralResponseBody(BaseModel):
    code: int = Field(
        ...,
        description="상태값, 10000테이블완료, 테이블실패",
        examples=[10000],
    )
    msg: str = Field(
        ...,
        description="요청 설명",
        examples=["완료"],
    )
    data: JFBYMGeneralResponseInnerData = Field(
        None,
        description="열기코드데이터",
        examples=[
            {
                "code": 0,
                "data": "5298",
                "time": 0.03829169273376465,
                "unique_code": "56a74a3b9b9b796b3ec554832c1cccbb",
            },
        ],
    )