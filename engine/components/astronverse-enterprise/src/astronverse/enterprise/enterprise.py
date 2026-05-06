"""Enterprise module"""

import base64
import json
import os
import urllib.parse
from json import JSONDecodeError
from pathlib import Path
from typing import Optional

import requests
from astronverse.actionlib import AtomicFormType, AtomicFormTypeMeta
from astronverse.actionlib.atomic import atomicMg
from astronverse.actionlib.types import PATH, Ciphertext
from astronverse.baseline.logger.logger import logger
from astronverse.enterprise.error import *

cache_remote_var_key: str = ""
cache_remote_var: dict = {}


def http(shot_url: str, params: Optional[dict], data: Optional[dict], meta: str = "post"):
    """post 요청 """
    gateway_port = atomicMg.cfg().get("GATEWAY_PORT") if atomicMg.cfg().get("GATEWAY_PORT") else "13159"
    logger.debug("요청 열기  {}:{}:{}".format(shot_url, params, data))
    if meta == "post":
        response = requests.post("http://127.0.0.1:{}{}".format(gateway_port, shot_url), json=data, params=params)
    else:
        response = requests.get("http://127.0.0.1:{}{}".format(gateway_port, shot_url), params=params)
    if response.status_code != 200:
        raise BaseException(
            SERVER_ERROR_FORMAT.format(response.status_code), "서버 오류{}".format(response.status_code)
        )

    try:
        json_data = response.json()
    except JSONDecodeError:
        base64_encoded_data = base64.b64encode(response.content).decode("utf-8")
        return base64_encoded_data
    logger.debug("요청 결과 {}:{}".format(shot_url, json_data))
    if json_data.get("code") != "0000" and json_data.get("code") != "000000":
        msg = json_data.get("message", "")
        raise BaseException(SERVER_ERROR_FORMAT.format(msg), "서버 오류{}".format(json_data))
    return json_data.get("data", {})


def get_remote_var_key() -> str:
    global cache_remote_var_key
    if cache_remote_var_key:
        return cache_remote_var_key

    res = http("/api/robot/robot-shared-var/shared-var-key", None, None, "get")
    cache_remote_var_key = res.get("key", "")
    return cache_remote_var_key


def get_remote_var_value(key: str) -> dict:
    global cache_remote_var

    if key in cache_remote_var:
        return cache_remote_var[key]

    res = http("/api/robot/robot-shared-var/get-batch-shared-var", None, {"ids": [key]}, "post")
    if res:
        cache_remote_var[key] = res[0]
    else:
        cache_remote_var[key] = None
    return cache_remote_var[key]


class Enterprise:
    """Enterprise class"""

    @staticmethod
    @atomicMg.atomic(
        "Enterprise",
        inputList=[
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "file"},
                ),
            ),
        ],
        outputList=[atomicMg.param("upload_result", types="Str")],
    )
    def upload_to_sharefolder(file_path: PATH = ""):
        """Upload file to shared folder"""
        upload_url = "http://127.0.0.1:{}/api/resource/file/shared-file-upload".format(
            atomicMg.cfg().get("GATEWAY_PORT") if atomicMg.cfg().get("GATEWAY_PORT") else "13159"
        )
        update_info_url = "http://127.0.0.1:{}/api/robot/robot-shared-file/addSharedFileInfo".format(
            atomicMg.cfg().get("GATEWAY_PORT") if atomicMg.cfg().get("GATEWAY_PORT") else "13159"
        )
        # 조회파일여부존재함
        if not (os.path.exists(file_path) and os.path.isfile(file_path)):
            return BaseException(PATH_INVALID_FORMAT.format(file_path), "요청다시 입력정상의파일 경로")

        try:
            # 준비파일업로드
            with open(file_path, "rb") as file:
                files = {
                    "file": (
                        os.path.basename(file_path),
                        file,
                        "application/octet-stream",
                    )
                }
                data = {"fileId": "", "tags": ""}
                # 전송POST요청 
                response = requests.post(upload_url, files=files, data=data, timeout=30)
                if response.status_code == 200:
                    logger.info(f"요청 반환값: {response.text}")
                    inner_data = json.loads(response.text)
                    if inner_data.get("code") in ["999999", "500000"]:
                        raise BaseException(
                            FILE_UPLOAD_FAILED_FORMAT.format(response.text),
                            "가능사용완료지원하지 않음의이름!",
                        )
                    info_data = {
                        "fileId": inner_data.get("data").get("fileid"),
                        "fileType": inner_data.get("data").get("type"),
                        "fileName": inner_data.get("data").get("fileName"),
                        "tags": [],
                    }
                    info_response = requests.post(update_info_url, json=info_data, timeout=30)
                    if info_response.status_code == 200:
                        logger.info(info_response.text)
                        if info_response.json().get("code") != "000000":
                            raise BaseException(
                                FILE_UPLOAD_FAILED_FORMAT.format(info_response.json().get("message")),
                                "파일완료존재함또는업데이트파일정보실패!",
                            )
                        return "업로드성공"
                    else:
                        logger.info(
                            f"업로드성공, 업데이트파일정보실패, 상태코드: {info_response.status_code}, : {info_response.text}"
                        )
                        raise BaseException(
                            FILE_UPLOAD_FAILED_FORMAT.format(info_response.text),
                            "확인하세요업데이트파일정보연결!",
                        )
                else:
                    logger.info(f"업로드실패, 상태코드: {response.status_code}, : {response.text}")
                    raise BaseException(
                        FILE_UPLOAD_FAILED_FORMAT.format(response.text),
                        "확인하세요업로드연결!",
                    )
        except Exception as e:
            logger.error(f"업로드경과중발송오류: {str(e)}")
            raise BaseException(FILE_UPLOAD_FAILED_FORMAT.format(e), "")

    @staticmethod
    @atomicMg.atomic(
        "Enterprise",
        inputList=[
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(type=AtomicFormType.REMOTEFOLDERS.value),
            ),
            atomicMg.param(
                "save_folder",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "folder"},
                ),
            ),
        ],
        outputList=[atomicMg.param("download_result", types="Str")],
    )
    def download_from_sharefolder(file_path: int, save_folder: PATH = ""):
        """Download file from shared folder"""
        download_url = "http://127.0.0.1:{}/api/resource/file/download".format(
            atomicMg.cfg().get("GATEWAY_PORT") if atomicMg.cfg().get("GATEWAY_PORT") else "13159"
        )
        # 조회 save_folder 경로여부예경로
        if not Path(save_folder).is_absolute():
            raise Exception(f"폴더 경로오류: {save_folder} 아니오예경로")
        # 조회저장폴더여부존재함, 결과가찾을 수 없습니다이면생성
        if not os.path.exists(save_folder):
            os.makedirs(save_folder)

        # 조회저장경로여부로디렉터리
        if not os.path.isdir(save_folder):
            raise Exception(f"폴더 경로오류: {save_folder} 아니오예폴더 경로")

        try:
            params = {"fileId": file_path}
            response = requests.get(download_url, params=params, timeout=30, stream=True)

            # 조회상태
            if response.status_code != 200:
                logger.error(f"다운로드실패, 상태코드: {response.status_code}, : {response.text}")
                raise BaseException(FILE_DOWNLOAD_FAILED_FORMAT.format(response.text), "확인하세요다운로드연결!")

            content_type = response.headers.get("Content-Type", "").lower()
            if "application/json" in content_type:
                error = response.json()
                if not error.get("success"):
                    raise BaseException(
                        FILE_DOWNLOAD_FAILED_FORMAT.format(error.get("message", "")), "확인하세요다운로드연결!"
                    )
            elif "application/octet-stream" in content_type:
                # 에서중가져오기파일이름, 결과가있음이면사용이름
                content_disposition = response.headers.get("content-disposition", "")
                if "filename=" in content_disposition:
                    filename = content_disposition.split("filename=")[1].strip('"')
                    # 파일이름행URL해제코드, 해제중국어파일이름제목
                    try:
                        filename = urllib.parse.unquote(filename)
                    except Exception as e:
                        logger.info(f"해제코드실패: {e}")
                        pass  # 결과가해제코드실패, 사용기존파일이름
                else:
                    filename = f"downloaded_file_{file_path}"

                # 생성의저장경로
                save_path = os.path.join(save_folder, filename)
                # 파일완료존재함, 이름 변경파일
                if os.path.exists(save_path):
                    base, ext = os.path.splitext(filename)
                    count = 1
                    while os.path.exists(save_path):
                        new_filename = f"{base}({count}){ext}"
                        save_path = os.path.join(save_folder, new_filename)
                        count += 1
                # 저장파일
                with open(save_path, "wb") as file:
                    for chunk in response.iter_content(chunk_size=8192):
                        if chunk:
                            file.write(chunk)

                logger.info(f"다운로드성공: 파일완료저장까지 {save_path}")
                return save_path
            else:
                raise NotImplementedError()
        except Exception as e:
            logger.error(f"다운로드경과중발송오류: {str(e)}")
            raise BaseException(FILE_UPLOAD_FAILED_FORMAT.format(e), "")

    # 가져오기 변수
    @staticmethod
    @atomicMg.atomic(
        "Enterprise",
        inputList=[
            atomicMg.param(
                "shared_variable",
                types="Str",
                formType=AtomicFormTypeMeta(type=AtomicFormType.REMOTEPARAMS.value),
            ),
        ],
        outputList=[
            atomicMg.param("variable_data", types="Dict"),
        ],
    )
    def get_shared_variable(shared_variable: str):
        """
        Get shared variable from remote
        """
        key = get_remote_var_key()
        value = get_remote_var_value(shared_variable)

        sub_var_list = value.get("subVarList", [])
        if not sub_var_list:
            return None
        res = {}
        for sub_var in sub_var_list:
            if sub_var["encrypt"]:
                c = Ciphertext(sub_var.get("varValue"))
                c.set_key(key)
                res[sub_var.get("varName")] = c.decrypt()
            else:
                res[sub_var.get("varName")] = sub_var.get("varValue")
        return res