import time

from astronverse.scheduler.apis.response import ResCode, res_msg
from astronverse.scheduler.core.svc import Svc, get_svc
from fastapi import APIRouter, Depends

router = APIRouter()


@router.post("/start")
def picker_start(svc: Svc = Depends(get_svc)):
    """
    시작선택
    :return:
    """
    if svc.picker.app_picker.is_alive():
        return res_msg(msg="시작성공", data=None)

    svc.picker.highlighter.run()
    svc.picker.app_picker.run()
    svc.picker.vision_picker.run()
    svc.picker.set_start(True)

    tag = True
    for _ in range(10):
        time.sleep(0.5)
        if not svc.picker.app_picker.is_alive():
            tag = True
        else:
            tag = False
            break
    if tag:
        return res_msg(code=ResCode.ERR, msg="시작 실패", data=None)
    return res_msg(msg="시작성공", data=None)


@router.post("/stop")
def picker_stop(svc: Svc = Depends(get_svc)):
    """
    닫기선택
    :return:
    """
    try:
        svc.picker.set_start(False)
        svc.picker.vision_picker.kill()
        svc.picker.app_picker.kill()
        svc.picker.highlighter.kill()
    except Exception as e:
        pass
    return res_msg(msg="중지성공", data=None)