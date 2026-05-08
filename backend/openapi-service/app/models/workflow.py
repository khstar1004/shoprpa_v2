import json

from sqlalchemy import Column, DateTime, Integer, String, Text, func

from app.database import Base


class Workflow(Base):
    """Stored workflow definition."""

    __tablename__ = "openai_workflows"

    project_id = Column(String(100), primary_key=True, index=True)
    name = Column(String(100), index=True, nullable=False)
    english_name = Column(String(100), nullable=True)
    description = Column(String(500), nullable=True)
    version = Column(Integer, nullable=False, default=1)
    status = Column(Integer, default=1, nullable=False)
    parameters = Column(Text, nullable=True)
    user_id = Column(String(50), nullable=False, index=True)
    example_project_id = Column(String(100), nullable=True)
    created_at = Column(DateTime, default=func.now(), nullable=False)
    updated_at = Column(DateTime, default=func.now(), onupdate=func.now(), nullable=False)

    def to_dict(self):
        """Convert the workflow to a JSON-serializable dict."""
        parameters_dict = None
        if self.parameters:
            try:
                parameters_dict = json.loads(self.parameters)
            except (json.JSONDecodeError, TypeError):
                parameters_dict = None

        workflow_dict = {
            "project_id": self.project_id,
            "name": self.name,
            "english_name": self.english_name,
            "description": self.description,
            "version": self.version,
            "status": self.status,
            "parameters": parameters_dict,
            "user_id": self.user_id,
            "example_project_id": self.example_project_id,
            "created_at": self.created_at,
            "updated_at": self.updated_at,
        }
        return workflow_dict

    def get_parameters_as_dict(self):
        """Return parameters parsed as a dict."""
        if self.parameters:
            try:
                return json.loads(self.parameters)
            except (json.JSONDecodeError, TypeError):
                return {}
        return {}

    def set_parameters_from_dict(self, params_dict):
        """Store parameters from a dict."""
        if params_dict:
            self.parameters = json.dumps(params_dict, ensure_ascii=False)
        else:
            self.parameters = None


class Execution(Base):
    """Stored workflow execution record."""

    __tablename__ = "openai_executions"

    id = Column(String(36), primary_key=True, index=True)
    project_id = Column(String(100), nullable=False, index=True)
    status = Column(String(20), default="PENDING", nullable=False)
    parameters = Column(Text, nullable=True)
    result = Column(Text, nullable=True)
    error = Column(Text, nullable=True)
    user_id = Column(String(50), nullable=False, index=True)
    exec_position = Column(String(50), default="EXECUTOR", nullable=False)
    recording_config = Column(Text, nullable=True)
    version = Column(Integer, nullable=True)
    start_time = Column(DateTime, default=func.now(), nullable=False)
    end_time = Column(DateTime, nullable=True)

    def to_dict(self):
        """Convert the execution to a JSON-serializable dict."""
        parameters_dict = None
        if self.parameters:
            try:
                parameters_dict = json.loads(self.parameters)
            except (json.JSONDecodeError, TypeError):
                parameters_dict = None

        result_dict = None
        if self.result:
            try:
                result_dict = json.loads(self.result)
            except (json.JSONDecodeError, TypeError):
                result_dict = None

        execution_dict = {
            "id": self.id,
            "project_id": self.project_id,
            "status": self.status,
            "parameters": parameters_dict,
            "result": result_dict,
            "error": self.error,
            "exec_position": self.exec_position,
            "version": self.version,
            "user_id": self.user_id,
            "start_time": self.start_time,
            "end_time": self.end_time,
        }
        return execution_dict

    def get_parameters_as_dict(self):
        """Return parameters parsed as a dict."""
        if self.parameters:
            try:
                return json.loads(self.parameters)
            except (json.JSONDecodeError, TypeError):
                return {}
        return {}

    def get_result_as_dict(self):
        """Return result parsed as a dict."""
        if self.result:
            try:
                return json.loads(self.result)
            except (json.JSONDecodeError, TypeError):
                return {}
        return {}

    def set_parameters_from_dict(self, params_dict):
        """Store parameters from a dict."""
        if params_dict:
            self.parameters = json.dumps(params_dict, ensure_ascii=False)
        else:
            self.parameters = None

    def set_result_from_dict(self, result_dict):
        """Store result from a dict."""
        if result_dict:
            self.result = json.dumps(result_dict, ensure_ascii=False)
        else:
            self.result = None
