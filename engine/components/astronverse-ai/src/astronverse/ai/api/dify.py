"""Client wrapper for Dify API: file upload and workflow execution."""

import mimetypes
import os

import requests
from astronverse.baseline.logger.logger import logger

mimetypes.add_type("text/markdown", ".md")


class Dify:
    """Lightweight client for interacting with Dify platform APIs."""

    def __init__(self, api_key, app_url):
        self.api_key = api_key
        if not app_url.endswith("/"):
            app_url += "/"
        self.base_url = app_url
        self.headers = {
            "Authorization": f"Bearer {self.api_key}",
            # "Content-Type": "application/json"
        }

    def upload_file(self, file_path, user):
        upload_url = self.base_url + "files/upload"

        try:
            logger.info("파일 업로드 중...")
            mime_type, _ = mimetypes.guess_type(file_path)

            file_extension = os.path.splitext(file_path)[1].replace(".", "").upper()
            with open(file_path, "rb") as file:
                files = {
                    "file": (
                        os.path.basename(file_path),
                        file,
                        mime_type,
                    )  # 확인파일으로의MIME유형업로드
                }
                data = {"user": user, "type": file_extension}  # 파일유형로이름

                response = requests.post(upload_url, headers=self.headers, files=files, data=data)
                if response.status_code == 201:
                    logger.info("파일 업로드 성공")
                    return response.json().get("id")  # 가져오기업로드의파일 ID
                else:
                    logger.info("파일 업로드 실패, 상태 코드: %s", response.status_code)
                    return None
        except Exception as e:
            logger.info("파일 업로드 요청 오류: %s", e)
            return None

    def run_workflow(
        self,
        user: str,
        variable_name: str,
        file_flag: bool,
        variable_value,
        file_type: str,
        response_mode: str = "blocking",
    ) -> dict:
        """Run a workflow with given inputs and return execution result dict."""
        workflow_url = self.base_url + "workflows/run"

        if not variable_name:
            inputs = {}
        else:
            if file_flag:
                template = {
                    "transfer_method": "local_file",
                    "upload_file_id": variable_value,
                    "type": file_type,
                }
            else:
                template = variable_value
            inputs = {variable_name: template}

        data = {
            "inputs": inputs,
            "response_mode": response_mode,
            "user": user,
        }

        try:
            logger.info("워크플로 실행 중...")
            response = requests.post(workflow_url, headers=self.headers, json=data)
            if response.status_code == 200:
                logger.info("워크플로 실행 성공")
                return response.json()
            else:
                logger.info("워크플로 실행 실패, 상태 코드: %s", response.status_code)
                return {
                    "status": "error",
                    "message": f"워크플로 실행 실패, 상태 코드: {response.status_code}",
                }
        except Exception as e:
            logger.info("워크플로 실행 요청 오류: %s", e)
            return {"status": "error", "message": str(e)}

    def run_chatflow(
        self,
        user,
        query,
        variable_name,
        file_flag,
        variable_value,
        file_type,
        response_mode="blocking",
    ):
        chatflow_url = self.base_url + "chat-messages"

        if not variable_name:
            inputs = {}
        else:
            if file_flag:
                template = {
                    "transfer_method": "local_file",
                    "upload_file_id": variable_value,
                    "type": file_type,
                }
            else:
                template = variable_value
            inputs = {variable_name: template}

        data = {"inputs": inputs, "response_mode": response_mode, "user": user, "query": query, "conversation_id": ""}

        try:
            logger.info("채팅 플로 실행 중...")
            response = requests.post(chatflow_url, headers=self.headers, json=data)
            if response.status_code == 200:
                logger.info("채팅 플로 실행 성공")
                return response.json()
            else:
                logger.info("채팅 플로 실행 실패, 상태 코드: %s", response.status_code)
                return {
                    "status": "error",
                    "message": f"채팅 플로 실행 실패, 상태 코드: {response.status_code}",
                }
        except Exception as e:
            logger.info("채팅 플로 실행 요청 오류: %s", e)
            return {"status": "error", "message": str(e)}
