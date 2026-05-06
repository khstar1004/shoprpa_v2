import json

from sqlalchemy import Column, DateTime, Integer, String, Text, func

from app.database import Base


class Workflow(Base):
    """워크플로데이터베이스유형"""

    __tablename__ = "openai_workflows"

    project_id = Column(String(100), primary_key=True, index=True)  # 목록ID로기본 키
    name = Column(String(100), index=True, nullable=False)
    english_name = Column(String(100), nullable=True)  # 후의영어이름
    description = Column(String(500), nullable=True)
    version = Column(Integer, nullable=False, default=1)
    status = Column(Integer, default=1, nullable=False)
    parameters = Column(Text, nullable=True)  # 저장JSON문자열형식의매개변수
    user_id = Column(String(50), nullable=False, index=True)
    example_project_id = Column(String(100), nullable=True)  # 예시사용자계정아래의project_id, 사용실행시
    created_at = Column(DateTime, default=func.now(), nullable=False)
    updated_at = Column(DateTime, default=func.now(), onupdate=func.now(), nullable=False)

    def to_dict(self):
        """를Workflow객체변환로가능순서열의딕셔너리"""
        # 관리parameters필드의반대순서열
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
        """를parameters필드에서JSON문자열변환로딕셔너리"""
        if self.parameters:
            try:
                return json.loads(self.parameters)
            except (json.JSONDecodeError, TypeError):
                return {}
        return {}

    def set_parameters_from_dict(self, params_dict):
        """를딕셔너리변환로JSON문자열저장까지parameters필드"""
        if params_dict:
            self.parameters = json.dumps(params_dict, ensure_ascii=False)
        else:
            self.parameters = None


class Execution(Base):
    """워크플로실행기록데이터베이스유형"""

    __tablename__ = "openai_executions"

    id = Column(String(36), primary_key=True, index=True)  # UUID형식
    project_id = Column(String(100), nullable=False, index=True)
    status = Column(String(20), default="PENDING", nullable=False)
    parameters = Column(Text, nullable=True)
    result = Column(Text, nullable=True)
    error = Column(Text, nullable=True)
    user_id = Column(String(50), nullable=False, index=True)
    exec_position = Column(String(50), default="EXECUTOR", nullable=False)  # 실행위치
    recording_config = Column(Text, nullable=True)  # 기록제어매칭
    version = Column(Integer, nullable=True)  # 워크플로버전
    start_time = Column(DateTime, default=func.now(), nullable=False)
    end_time = Column(DateTime, nullable=True)

    def to_dict(self):
        """를Execution객체변환로가능순서열의딕셔너리"""
        # 관리parameters및result필드의반대순서열
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
        """를parameters필드에서JSON문자열변환로딕셔너리"""
        if self.parameters:
            try:
                return json.loads(self.parameters)
            except (json.JSONDecodeError, TypeError):
                return {}
        return {}

    def get_result_as_dict(self):
        """를result필드에서JSON문자열변환로딕셔너리"""
        if self.result:
            try:
                return json.loads(self.result)
            except (json.JSONDecodeError, TypeError):
                return {}
        return {}

    def set_parameters_from_dict(self, params_dict):
        """를딕셔너리변환로JSON문자열저장까지parameters필드"""
        if params_dict:
            self.parameters = json.dumps(params_dict, ensure_ascii=False)
        else:
            self.parameters = None

    def set_result_from_dict(self, result_dict):
        """를딕셔너리변환로JSON문자열저장까지result필드"""
        if result_dict:
            self.result = json.dumps(result_dict, ensure_ascii=False)
        else:
            self.result = None