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
    """Workflow create/update payload."""

    project_id: Union[str, int] = Field(..., description="Project ID")
    name: str = Field("Default_Workflow", description="Workflow name", min_length=1, max_length=100)
    english_name: Optional[str] = Field(None, description="English workflow name", max_length=100)
    description: Optional[str] = Field(None, description="Workflow description", max_length=500)
    version: int = Field(1, description="Workflow version")
    status: int = Field(1, description="Workflow status")
    parameters: Optional[str] = Field(None, description="Workflow parameters as a JSON string")
    example_project_id: Optional[str] = Field(None, description="Source example project ID used for execution")


class WorkflowResponse(WorkflowBase):
    """Workflow response."""

    created_at: datetime = Field(..., description="Created at")
    updated_at: Optional[datetime] = Field(None, description="Updated at")

    model_config = {"from_attributes": True}


class WorkflowListResponse(BaseModel):
    """Workflow list response."""

    data: list[WorkflowResponse] = Field(..., description="Workflow list")


class ExecutionCreate(BaseModel):
    """Workflow execution request."""

    project_id: str = Field(..., description="Project ID")
    params: Optional[dict[str, Any]] = Field(None, description="Execution parameters")
    exec_position: str = Field("EXECUTOR", description="Execution position")
    recording_config: Optional[str] = Field(None, description="Recording configuration")
    version: Optional[int] = Field(None, description="Workflow version")

    phone_number: Optional[str] = Field(None, description="Phone number for ShopRPA Agent copy execution")


class ExecutionResponse(BaseModel):
    """Execution response."""

    id: str = Field(..., description="Execution ID")
    project_id: str = Field(..., description="Project ID")
    status: str = Field(..., description="Execution status")
    parameters: Optional[dict[str, Any]] = Field(None, description="Execution parameters")
    result: Optional[dict[str, Any]] = Field(None, description="Execution result")
    error: Optional[str] = Field(None, description="Error details")
    exec_position: str = Field(..., description="Execution position")
    version: Optional[int] = Field(None, description="Workflow version")
    start_time: datetime = Field(..., description="Start time")
    end_time: Optional[datetime] = Field(None, description="End time")

    model_config = {"from_attributes": True}


class WorkflowCopyRequest(BaseModel):
    """Workflow copy request."""

    project_id: Union[str, int] = Field(..., description="Project ID")
    version: int = Field(..., description="Workflow version")
    phone_number: str = Field(..., description="Target phone number")
