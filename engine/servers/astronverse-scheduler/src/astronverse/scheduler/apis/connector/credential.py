"""
인증관리관리 API 경로 
"""

from astronverse.scheduler.apis.response import ResCode, res_msg
from astronverse.scheduler.core.credential import CredentialService
from astronverse.scheduler.logger import logger
from fastapi import APIRouter, Query
from pydantic import BaseModel

router = APIRouter()


class CreateCredentialRequest(BaseModel):
    """생성인증요청 """

    name: str
    password: str


class DeleteCredentialRequest(BaseModel):
    """삭제인증요청 """

    name: str


@router.get("/list")
def credential_list():
    """
    가져오기 인증목록
    """
    try:
        credentials = CredentialService.list_credentials()
        return res_msg(code=ResCode.SUCCESS, msg="success", data=credentials)
    except Exception as e:
        logger.exception(f"가져오기 인증목록실패: {e}")
        return res_msg(code=ResCode.ERR, msg=str(e), data=None)


@router.post("/create")
def credential_create(req: CreateCredentialRequest):
    """
    생성인증
    """
    try:
        success = CredentialService.create_credential(req.name, req.password)
        if success:
            return res_msg(code=ResCode.SUCCESS, msg="success", data=None)
        else:
            return res_msg(code=ResCode.ERR, msg="생성인증실패", data=None)
    except Exception as e:
        logger.exception(f"생성인증실패: {e}")
        return res_msg(code=ResCode.ERR, msg=str(e), data=None)


@router.post("/delete")
def credential_delete(req: DeleteCredentialRequest):
    """
    삭제인증
    """
    try:
        success = CredentialService.delete_credential(req.name)
        if success:
            return res_msg(code=ResCode.SUCCESS, msg="success", data=None)
        else:
            return res_msg(code=ResCode.ERR, msg="삭제인증실패", data=None)
    except Exception as e:
        logger.exception(f"삭제인증실패: {e}")
        return res_msg(code=ResCode.ERR, msg=str(e), data=None)


@router.get("/exists")
def credential_exists(name: str = Query(..., description="인증이름")):
    """
    조회인증여부존재함
    """
    try:
        exists = CredentialService.exists(name)
        return res_msg(code=ResCode.SUCCESS, msg="success", data={"exists": exists})
    except Exception as e:
        logger.exception(f"조회인증여부존재함실패: {e}")
        return res_msg(code=ResCode.ERR, msg=str(e), data=None)