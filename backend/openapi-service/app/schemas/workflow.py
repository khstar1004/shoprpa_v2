from datetime import datetime
from enum import Enum
from typing import Any, Optional, Union

from pydantic import BaseModel, Field


class WorkflowStatus(int, Enum):
    ACTIVE = 1
    INACTIVE = 0


class ExecutionStatus(str, Enum):
    PENDING = "PENDING"
    RUNNING = "RUNNING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"


class WorkflowBase(BaseModel):
    """워크플로본데이터유형"""

    project_id: Union[str, int] = Field(..., description="목록ID")
    name: str = Field("Default_Workflow", description="워크플로이름", min_length=1, max_length=100)
    english_name: Optional[str] = Field(None, description="워크플로영어이름", max_length=100)
    description: Optional[str] = Field(None, description="워크플로설명", max_length=500)
    version: int = Field(1, description="워크플로버전")
    status: int = Field(0, description="워크플로상태")
    parameters: Optional[str] = Field(None, description="워크플로매개변수(JSON형식)")
    example_project_id: Optional[str] = Field(None, description="예시사용자계정아래의project_id, 사용실행시")


class WorkflowResponse(WorkflowBase):
    """워크플로유형"""

    created_at: datetime = Field(..., description="생성 시간")
    updated_at: Optional[datetime] = Field(None, description="수정 시간")

    model_config = {"from_attributes": True}


class WorkflowListResponse(BaseModel):
    """워크플로목록 유형"""

    data: list[WorkflowResponse] = Field(..., description="워크플로목록")


class ExecutionCreate(BaseModel):
    """생성워크플로실행기록요청 유형"""

    project_id: str = Field(..., description="목록ID")
    params: Optional[dict[str, Any]] = Field(None, description="실행매개변수")
    exec_position: str = Field("EXECUTOR", description="실행위치")
    recording_config: Optional[str] = Field(None, description="기록제어매칭")
    version: Optional[int] = Field(None, description="워크플로버전")

    # 2026-01-12 추가휴대폰 번호매개변수, 사용ShoprpaAgent의복사호출
    phone_number: Optional[str] = Field(None, description="휴대폰 번호")


class ExecutionResponse(BaseModel):
    """실행기록유형"""

    id: str = Field(..., description="실행기록ID")
    project_id: str = Field(..., description="목록ID")
    status: str = Field(..., description="실행상태")
    parameters: Optional[dict[str, Any]] = Field(None, description="실행매개변수")
    result: Optional[dict[str, Any]] = Field(None, description="실행 결과")
    error: Optional[str] = Field(None, description="오류정보")
    exec_position: str = Field(..., description="실행위치")
    version: Optional[int] = Field(None, description="워크플로버전")
    start_time: datetime = Field(..., description="시작 시간")
    end_time: Optional[datetime] = Field(None, description="종료 시간")

    model_config = {"from_attributes": True}


class WorkflowCopyRequest(BaseModel):
    """복사워크플로요청 유형"""

    project_id: Union[str, int] = Field(..., description="목록ID")
    version: int = Field(..., description="워크플로버전")
    phone_number: str = Field(..., description="목록 휴대폰 번호")