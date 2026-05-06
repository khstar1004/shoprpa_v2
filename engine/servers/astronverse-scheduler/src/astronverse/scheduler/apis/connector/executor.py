import json
import time
from typing import Optional, Union

from astronverse.scheduler.apis.connector.terminal import Terminal
from astronverse.scheduler.apis.response import ResCode, exec_res_msg, res_msg
from astronverse.scheduler.core.executor.executor import (
    ExecuteStatus,
    ProjectExecPosition,
    TaskExecuteStatus,
)
from astronverse.scheduler.core.svc import Svc, get_svc
from astronverse.scheduler.logger import logger
from astronverse.scheduler.utils.utils import EmitType, emit_to_front, get_settings
from fastapi import APIRouter, Depends
from pydantic import BaseModel

router = APIRouter()


class ExecutorProject(BaseModel):
    project_id: str  # id
    project_name: str = ""  # 이름
    process_id: str = ""  # 프로세스id
    line: int = 0  # 시도행
    end_line: int = 0  # 시도행
    jwt: str = ""  # jwt[없음]
    debug: str = "n"  # debug방식
    exec_position: ProjectExecPosition = ProjectExecPosition.EDIT_PAGE  # 실행위치
    recording_config: str = ""  # 기록제어기기매칭
    hide_log_window: bool = False  # 여부로그
    run_param: str = ""  # 실행기기매개변수
    open_virtual_desk: bool = False  # 
    version: Union[int, str] = ""  # 봇버전
    is_custom_component: bool = False  # 여부예지정컴포넌트


class StopTask(BaseModel):
    task_id: Optional[str] = None


class RobotInfo(BaseModel):
    robotId: str
    robotName: str
    paramJson: str = ""
    version: str = ""
    sort: int = 1


class TaskInfo(BaseModel):
    trigger_id: str  # 작업id
    task_type: str = ""  # schedule manual hotKey files mail
    trigger_name: str  # 작업이름
    exceptional: str
    timeout: int = 0
    callback_project_ids: list[RobotInfo] = []
    mode: ProjectExecPosition = ProjectExecPosition.EDIT_PAGE
    retry_num: int = 0
    open_virtual_desk: bool = False  # 


def report_task_log(svc, status: TaskExecuteStatus, task_id: str = None, task_execute_id: str = None):
    """로그위: 예약 작업상태위, 분및통신로그위"""

    import requests

    if svc.terminal_mod:
        data = {
            "dispatchTaskId": task_id,
            "result": status.value,
            "isDispatch": True,
            "terminalId": Terminal.get_terminal_id(),
        }
        if task_execute_id:
            data["dispatchTaskExecuteId"] = task_execute_id
    else:
        data = {
            "taskId": task_id,
            "result": status.value,
            "isDispatch": False,
        }
        if task_execute_id:
            data["taskExecuteId"] = task_execute_id

    response = requests.post(
        headers={"Content-Type": "application/json"},
        url="http://127.0.0.1:{}/api/robot/task-execute/status".format(svc.rpa_route_port),
        data=json.dumps(data),
        timeout=3,
    )
    status_code = response.status_code
    text = response.text
    logger.info("report log request: {}".format(json.dumps(data)))
    logger.info("report log result: {}, response: {} {}".format(task_id, status_code, text))
    json_data = json.loads(text.strip())
    return json_data["data"]


@router.post("/run_list")
def executor_run_list(task_info: TaskInfo, svc: Svc = Depends(get_svc)):
    """
    실행및시작일그룹(예약 작업), 
    """
    if svc.executor_mg.status():
        return res_msg(code=ResCode.ERR, msg="완료있음에서실행, 불가시작")
    svc.terminal_task_stop = False
    settings = get_settings()
    task_executor_id = ""
    try:
        emit_to_front(EmitType.EDIT_SHOW_HIDE, msg={"type": "hide"})

        task_executor_id = report_task_log(svc, TaskExecuteStatus.EXECUTING, task_info.trigger_id)
        if not task_executor_id:
            raise Exception("서비스로그위예외")

        end_time = 0
        if task_info.timeout > 0:
            end_time = time.time() + (task_info.timeout * 60)

        temp_terminal_mod = svc.terminal_mod

        # 매개봇
        is_cancel = False
        for r in sorted(task_info.callback_project_ids, key=lambda x: x.sort):
            is_break = False
            for t in range(task_info.retry_num + 1):
                executor = svc.executor_mg.create(
                    task_id=task_info.trigger_id,
                    task_name=task_info.trigger_name,
                    task_exec_id=task_executor_id,
                    project_id=r.robotId,
                    project_name=r.robotName,
                    exec_position=task_info.mode,
                    recording_config=settings.get("videoForm", None),
                    hide_log_window=settings.get("commonSetting", {}).get("hideLogWindow", False),
                    run_param=r.paramJson,
                    open_virtual_desk=settings.get("open_virtual_desk", False) or task_info.open_virtual_desk,
                    version=r.version,
                    is_send_log_event=False,
                )
                if svc.terminal_mod:
                    svc.executor_mg.task_trigger_status()

                # 조회여부실행 결과
                while svc.executor_mg.status():
                    time.sleep(1)
                    if 0 < end_time < time.time():
                        svc.executor_mg.close(executor)
                        raise Exception("시작 실패: 실행시간 초과")

                # 조회전체영역상태
                if temp_terminal_mod != svc.terminal_mod:
                    # 상태완료, 직선연결출력작업
                    is_cancel = True
                    is_break = True
                    break

                if svc.terminal_task_stop:
                    svc.terminal_task_stop = False
                    is_cancel = True
                    is_break = True
                    break

                # 감지상태
                if executor is not None:
                    execute_status = executor.execute_status
                    execute_reason = executor.execute_reason
                    execute_data = executor.execute_data
                else:
                    execute_status = ExecuteStatus.FAIL
                    execute_reason = "시작 실패"
                    execute_data = {}

                if execute_status == ExecuteStatus.SUCCESS:
                    # 성공
                    break
                else:
                    # 실패
                    if task_info.exceptional == "jump":
                        break
                    elif task_info.exceptional == "retry_stop":
                        if t == task_info.retry_num - 1:
                            raise Exception("시작 실패: {}".format(execute_reason))
                    elif task_info.exceptional == "retry_jump":
                        if t == task_info.retry_num - 1:
                            break
                    else:
                        # stop
                        raise Exception("시작 실패: {}".format(execute_reason))

            if is_break:
                break
        # 실행성공
        if task_info.task_type in ["manual", "hotKey"]:
            emit_to_front(EmitType.EXECUTOR_END)
        if task_executor_id:
            if is_cancel:
                report_task_log(
                    svc,
                    TaskExecuteStatus.CANCEL,
                    task_info.trigger_id,
                    task_executor_id,
                )
            else:
                report_task_log(
                    svc,
                    TaskExecuteStatus.SUCCESS,
                    task_info.trigger_id,
                    task_executor_id,
                )
        if svc.terminal_mod:
            svc.executor_mg.task_trigger_status()
        return res_msg(code=ResCode.SUCCESS, msg="실행성공", data={})
    except Exception as e:
        # 실행실패
        if task_info.task_type in ["manual", "hotKey"]:
            emit_to_front(EmitType.EXECUTOR_END)
        if task_executor_id:
            report_task_log(
                svc,
                TaskExecuteStatus.EXEC_ERROR,
                task_info.trigger_id,
                task_executor_id,
            )
        if svc.terminal_mod:
            svc.executor_mg.task_trigger_status()
        return res_msg(code=ResCode.SUCCESS, msg=str(e), data={})


@router.post("/run_sync")
def executor_run_sync(param: ExecutorProject, svc: Svc = Depends(get_svc)):
    """
    실행및시작일개(스케줄링), , 가져오기반환값
    """

    if svc.executor_mg.status():
        return res_msg(code=ResCode.ERR, msg="완료있음에서실행, 불가시작")

    recording_config = {}
    try:
        if param.recording_config:
            recording_config = json.loads(param.recording_config)
    except Exception as e:
        # 기록제어공가능아니오실행기기
        pass

    executor = svc.executor_mg.create(
        project_id=param.project_id,
        project_name=param.project_name,
        process_id=param.process_id,
        line=param.line,
        end_line=param.end_line,
        debug=param.debug,
        exec_position=param.exec_position,
        recording_config=recording_config,
        hide_log_window=param.hide_log_window,
        run_param=param.run_param,
        open_virtual_desk=param.open_virtual_desk,
        is_send_log_event=False,
        version=param.version,
        is_custom_component=param.is_custom_component,
    )
    # 조회여부실행 결과
    while svc.executor_mg.status():
        time.sleep(1)
    # 감지상태
    if executor is not None:
        execute_status = executor.execute_status
        execute_reason = executor.execute_reason
        execute_data = executor.execute_data
    else:
        execute_status = ExecuteStatus.FAIL
        execute_reason = "시작 실패"
        execute_data = {}
    video_path = executor.execute_video_path if executor else ""
    if execute_status == ExecuteStatus.SUCCESS:
        return exec_res_msg(code=ResCode.SUCCESS, msg="실행성공", data=execute_data, video_path=video_path)
    else:
        return exec_res_msg(code=ResCode.ERR, msg=execute_reason, video_path=video_path)


@router.post("/run")
def executor_run(param: ExecutorProject, svc: Svc = Depends(get_svc)):
    """
    실행및시작일개(본실행), 예외
    """
    # 
    if not param.project_id:
        return res_msg(code=ResCode.ERR, msg="id비어 있습니다", data=None)
    if svc.executor_mg.status():
        return res_msg(code=ResCode.ERR, msg="완료있음에서실행, 불가시작")

    recording_config = {}
    try:
        if param.recording_config:
            recording_config = json.loads(param.recording_config)
    except Exception as e:
        # 기록제어공가능아니오실행기기
        pass

    executor = svc.executor_mg.create(
        project_id=param.project_id,
        project_name=param.project_name,
        process_id=param.process_id,
        line=param.line,
        end_line=param.end_line,
        debug=param.debug,
        exec_position=param.exec_position,
        recording_config=recording_config,
        hide_log_window=param.hide_log_window,
        run_param=param.run_param,
        open_virtual_desk=param.open_virtual_desk,
        is_send_log_event=True,
        version=param.version,
        is_custom_component=param.is_custom_component,
    )
    if executor is not None:
        return res_msg(msg="시작성공", data={"addr": "ws://127.0.0.1:{}/".format(executor.exec_port)})
    else:
        return res_msg(code=ResCode.ERR, msg="시작 실패")


@router.post("/status")
def executor_status(svc: Svc = Depends(get_svc)):
    """
    가져오기실행기기상태
    """
    status = svc.executor_mg.status()
    return res_msg(msg="ok", data={"running": status})


@router.post("/stop")
def executor_stop(exe_pro: ExecutorProject, svc: Svc = Depends(get_svc)):
    """
    강함제어닫기일개
    """
    project_id = exe_pro.project_id
    if not project_id:
        return res_msg(code=ResCode.ERR, msg="id비어 있습니다", data=None)
    svc.executor_mg.close_by_project(project_id=project_id)
    return res_msg(msg="중지성공", data=None)


@router.post("/stop_current")
def executor_stop_current(svc: Svc = Depends(get_svc)):
    if svc.executor_mg:
        svc.terminal_task_stop = True
        svc.executor_mg.close_all()
    return res_msg(msg="중지성공", data=None)


@router.post("/stop_list")
def executor_stop_list(stop_info: StopTask, svc: Svc = Depends(get_svc)):
    if svc.executor_mg:
        if (stop_info.task_id and svc.executor_mg.curr_task_id == stop_info.task_id) or (not stop_info.task_id):
            svc.terminal_task_stop = True
            svc.executor_mg.close_all()  # 닫기정상에서행의작업
    return res_msg(msg="중지성공", data=None)