from typing import Optional

from pydantic import BaseModel, Field

DEFAULT_MODEL = "maas/deepseek-v3.2"


class ChatCompletionParam(BaseModel):
    model: str = Field(DEFAULT_MODEL, examples=[DEFAULT_MODEL])
    stream: bool = Field(False, examples=[True])
    temperature: Optional[float] = Field(None, ge=0.0, le=2.0, examples=[0.7])
    max_tokens: Optional[int] = Field(None, examples=[4096])
    messages: Optional[list[dict]] = Field(
        None,
        examples=[
            {
                "role": "system",
                "content": "You are a helpful assistant.",
            },
            {
                "role": "user",
                "content": "What is the capital of France?",
            },
        ],
    )


class ChatPromptParam(BaseModel):
    model: str = Field(DEFAULT_MODEL, examples=[DEFAULT_MODEL])
    stream: bool = Field(False, examples=[True])
    prompt_type: str = Field(
        ...,
        examples=["translate", "code_review", "document_summary", "sql_generator", "business_analysis", "email_writer"],
        description="prompt유형",
    )
    params: Optional[dict] = Field(
        None,
        examples=[
            {"name": "사용자관리관리시스템"},  # translate
            {"language": "python", "code": "def hello(): print('world')"},  # code_review
            {"doc_type": "문서", "content": "예일개닫기 API의문서..."},  # document_summary
            {
                "db_type": "MySQL",
                "table_info": "users테이블패키지id,name,email필드",
                "requirement": "조회모든사용자",
            },  # sql_generator
            {"topic": "판매증가길이", "data": "Q1판매100, Q2판매120", "perspective": "마켓분"},  # business_analysis
            {"email_type": "메일", "recipient": "", "content": "목록 정도", "tone": "정상방식"},  # email_writer
        ],
        description="prompt매개변수",
    )