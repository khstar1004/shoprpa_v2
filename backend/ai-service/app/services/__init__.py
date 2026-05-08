from typing import Optional

from pydantic import BaseModel, Field


class OCRGeneralRequestBody(BaseModel):
    encoding: str = Field(
        "jpg",
        description="이미지코드, jpg형식(값)/jpeg형식/png형식/bmp형식",
        examples=["jpg"],
    )
    status: int = Field(3, description="업로드데이터상태, 선택 가능값: 3(일)", examples=[3])
    image: str = Field(
        ...,
        description="이미지데이터, 필요보관인증이미지파일크기base64코드후아니오초과경과4MB",
        examples=["iVBORw0KGgoAAAANSUhEUgAA..."],
    )


class OCRGeneralResponseInnerHeader(BaseModel):
    code: int = Field(..., description="반환코드, 0테이블완료, 0테이블실패", examples=[0])
    message: str = Field(..., description="반환정보", examples=["success"])
    sid: str = Field(..., description="일식별자", examples=["ase000fa8ab@hu196fbeb910905c4882"])


class OCRGeneralResponseInnerResult(BaseModel):
    compress: str = Field(..., description="텍스트압축형식", examples=["raw"])
    encoding: str = Field(..., description="텍스트코드형식", examples=["utf8"])
    format: str = Field(..., description="텍스트형식", examples=["json"])
    text: str = Field(
        ...,
        description="반환의텍스트데이터,  필요행base64해제코드",
        examples=["fQogXQp9Cg=="],
    )


class OCRGeneralResponseInnerPayload(BaseModel):
    result: OCRGeneralResponseInnerResult


class OCRGeneralResponseBody(BaseModel):
    header: OCRGeneralResponseInnerHeader
    payload: Optional[OCRGeneralResponseInnerPayload]