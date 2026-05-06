import base64
import io

import pyautogui
from astronverse.scheduler.apis.response import ResCode, res_msg
from astronverse.scheduler.core.svc import Svc, get_svc
from astronverse.scheduler.core.terminal.terminal import Terminal
from fastapi import APIRouter, Depends
from pydantic import BaseModel

router = APIRouter()


class TerminalStartReq(BaseModel):
    start_watch: bool = False


@router.post("/start")
def terminal_start(req: TerminalStartReq, svc: Svc = Depends(get_svc)):
    svc.terminal_mod = True
    svc.start_watch = req.start_watch
    Terminal.register(svc)  # 강함행회원가입일아래
    Terminal.upload(svc)  # 강함행업데이트일아래
    if svc.executor_mg:
        svc.executor_mg.close_all()  # 닫기정상에서행의작업
    svc.trigger_server.update_config(svc.terminal_mod)  # 트리거기기방식
    return res_msg(msg="시작성공", data=None)


@router.post("/end")
def terminal_end(svc: Svc = Depends(get_svc)):
    svc.terminal_mod = False
    svc.start_watch = False
    Terminal.upload(svc)  # 강함행업데이트일아래
    if svc.vnc_server:
        svc.vnc_server.close()  # 강함제어닫기아니오필요의서비스
    if svc.executor_mg:
        svc.executor_mg.close_all()  # 닫기정상에서행의작업
    svc.trigger_server.update_config(svc.terminal_mod)  # 트리거기기방식
    return res_msg(msg="결과완료", data=None)


@router.get("/ping")
def terminal_ping(svc: Svc = Depends(get_svc)):
    """
    사용pyautogui가져오기현재화면의스크린샷반환base64코드
    """
    try:
        if not svc.terminal_mod:
            return res_msg(
                code=ResCode.SUCCESS,
                msg="pong",
                data={
                    "width": 0,  # 화면
                    "height": 0,  # 화면높이
                    "terminal_mod": svc.terminal_mod,  # 방식
                    "start_watch": svc.start_watch,  # 여부열기시작
                    "vnc_port": "",  # 단말
                    "curr_status": False,  # 현재상태
                    "curr_task_name": "",  # 있음curr_status로true있음
                    "curr_project_name": "",
                    "curr_log_name": "",
                    "base64": "",  # 이미지
                },
            )

        # 가져오기화면스크린샷
        screenshot = pyautogui.screenshot()

        # 를이미지변환로문자
        img_buffer = io.BytesIO()
        screenshot.save(img_buffer, format="PNG")
        img_buffer.seek(0)

        # 변환로base64코드
        img_base64 = base64.b64encode(img_buffer.getvalue()).decode("utf-8")
        curr_status = svc.executor_mg.status()
        return res_msg(
            code=ResCode.SUCCESS,
            msg="pong",
            data={
                "width": screenshot.width,  # 화면
                "height": screenshot.height,  # 화면높이
                "terminal_mod": svc.terminal_mod,  # 방식
                "start_watch": svc.start_watch,  # 여부열기시작
                "vnc_port": svc.vnc_server.vnc_ws_port if svc.vnc_server else "",  # 단말
                "curr_status": svc.executor_mg.status(),  # 현재상태
                "curr_task_name": svc.executor_mg.curr_task_name if curr_status else "",  # 있음curr_status로true있음
                "curr_project_name": svc.executor_mg.curr_project_name if curr_status else "",
                "curr_log_name": svc.executor_mg.curr_log_name if curr_status else "",
                "base64": f"data:image/png;base64,{img_base64}",  # 이미지
            },
        )
    except Exception as e:
        return res_msg(code=ResCode.ERR, msg=f"가져오기실패: {str(e)}", data=None)


@router.get("/terminal_id")
def terminal_id():
    return res_msg(msg="가져오기성공", data={"terminal_id": Terminal.get_terminal_id()})