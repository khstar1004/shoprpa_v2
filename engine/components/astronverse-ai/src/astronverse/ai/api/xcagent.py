"""Client for xcAgent flow execution (Xingchen API streaming wrapper)."""

import http.client
import json
import os

import requests
from astronverse.baseline.logger.logger import logger


class xcAgent:  # pylint: disable=invalid-name
    """Minimal client for Xingchen flow execution.

    NOTE: Class name kept for backward compatibility; consider renaming to `XcAgent`.
    """

    def __init__(self, api_key: str, api_secret: str):
        """Store credentials and prepare headers."""
        self.headers = {
            "Content-Type": "application/json",
            "Accept": "text/event-stream",
            "Authorization": "Bearer {}:{}".format(api_key, api_secret),
        }
        self.upload_headers = {
            "Authorization": "Bearer {}:{}".format(api_key, api_secret),
            # 추가개출력오류 "Content-Type": "multipart/form-data"
        }

    def run_flow(self, flow_id, content, is_stream, file_flag, variable_name, variable_value, file_path):
        if not variable_name:
            parameters = {"AGENT_USER_INPUT": content}
        else:
            if file_flag:
                # 업로드파일
                response = self.upload_file(file_path)
                file_url = response.json()["data"]["url"]
                parameters = {"AGENT_USER_INPUT": content, variable_name: file_url}
            else:
                parameters = {"AGENT_USER_INPUT": content, variable_name: variable_value}
        logger.info("parameters: {}".format(parameters))

        data = {
            "flow_id": flow_id,
            "parameters": parameters,
            "stream": is_stream,
        }
        payload = json.dumps(data)

        conn = http.client.HTTPSConnection("xingchen-api.xf-yun.com", timeout=600)
        conn.request(
            "POST",
            "/workflow/v1/chat/completions",
            payload,
            self.headers,
            encode_chunked=True,
        )
        res = conn.getresponse()

        data = res.readline().decode("utf-8")
        response_json = json.loads(data)
        logger.info("response_json: {}".format(response_json))
        return response_json["choices"][0]["delta"]["content"]

    @staticmethod
    def get_content_type(file_path):
        """가져오기파일의Content-Type"""
        import mimetypes

        local_mime_types = {
            # 이미지
            ".png": "image/png",
            ".jpg": "image/jpeg",
            ".jpeg": "image/jpeg",
            ".gif": "image/gif",
            ".webp": "image/webp",
            ".bmp": "image/bmp",
            ".svg": "image/svg+xml",
            # 문서
            ".pdf": "application/pdf",
            ".doc": "application/msword",
            ".docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ".ppt": "application/vnd.ms-powerpoint",
            ".pptx": "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            # Excel
            ".xls": "application/vnd.ms-excel",
            ".xlsx": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            ".csv": "text/csv",
            # 텍스트
            ".txt": "text/plain",
            ".json": "application/json",
            ".xml": "application/xml",
            ".html": "text/html",
            ".css": "text/css",
            ".js": "application/javascript",
            # 압축파일
            ".zip": "application/zip",
            ".rar": "application/x-rar-compressed",
            ".7z": "application/x-7z-compressed",
            # 
            ".mp3": "audio/mpeg",
            ".mp4": "video/mp4",
            ".avi": "video/x-msvideo",
            ".wav": "audio/wav",
        }

        _, ext = os.path.splitext(str(file_path))
        extension = ext.lower()

        # 시도에서지정가져오기
        if extension in local_mime_types:
            return local_mime_types[extension]

        # 시도사용시스템의mimetypes모듈
        mime_type, _ = mimetypes.guess_type(str(file_path))
        if mime_type:
            return mime_type

        # 반환이제어
        return "application/octet-stream"

    def upload_file(self, file_path):
        # 조회파일여부존재함
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"파일찾을 수 없습니다: {file_path}")

        file_name = os.path.basename(file_path)

        # 가져오기Content-Type
        content_type = self.get_content_type(file_path)
        logger.info(f"파일유형: {content_type}")

        with open(file_path, "rb") as f:
            # 생성files매개변수
            files = {"file": (file_name, f, content_type)}

            try:
                response = requests.post(
                    "https://xingchen-api.xf-yun.com/workflow/v1/upload_file",
                    files=files,
                    headers=self.upload_headers,
                    timeout=300,  # 5분시간 초과, 대파일
                )

                response.raise_for_status()
                logger.info(f"업로드성공: {response.json()}")
                return response

            except requests.exceptions.RequestException as e:
                logger.info(f"업로드실패: {e}")
                raise


if __name__ == "__main__":
    api_key = "20xxxxxxxxxxxx0083741"
    api_secret = "ZjcxxxxxxxxxxxUyNjI2"
    agent = xcAgent(api_key, api_secret)
    inputs = [
        {"key": "AGENT_USER_INPUT", "value": "value1", "type": "string"},
        {"key": "nihao", "value": r"C:\Users\xxxx\Downloads\중목록실행제목.pdf", "type": "file"},
    ]
    agent.run_astron_flow("735xxxxxxx170", False, inputs)