import datetime
import json
import os
import shutil
import sys
import tempfile
import threading
import time
import traceback
import uuid
from enum import Enum
from typing import Union
from urllib.parse import quote

import requests
import websocket
from astronverse.scheduler.core.executor.virtual_desk import (
    WindowVirtualDeskSubprocessAdapter,
    virtual_desk,
)
from astronverse.scheduler.core.schduler.venv import create_project_venv
from astronverse.scheduler.core.terminal.terminal import Terminal
from astronverse.scheduler.logger import logger
from astronverse.scheduler.utils.notify_utils import NotifyUtils
from astronverse.scheduler.utils.subprocess import SubPopen
from astronverse.scheduler.utils.utils import (
    EmitType,
    check_port,
    emit_to_front,
    read_last_n_lines,
)


class ExecuteStatus(Enum):
    """
    봇실행상태
    """

    # 실행중
    EXECUTE = "robotExecute"
    # 성공
    SUCCESS = "robotSuccess"
    # 실행실패
    FAIL = "robotFail"
    # 가져오기 
    CANCEL = "robotCancel"


class TaskExecuteStatus(Enum):
    # 성공
    SUCCESS = "success"
    # 시작 실패
    START_ERROR = "start_error"  # 없음
    # 실행실패
    EXEC_ERROR = "exe_error"
    # 가져오기 
    CANCEL = "cancel"
    # 실행중
    EXECUTING = "executing"


class ProjectExecPosition(Enum):
    """
    지정에서개실행
    """

    # 목록 
    PROJECT_LIST = "PROJECT_LIST"
    # 
    EDIT_PAGE = "EDIT_PAGE"
    # 예약 작업시작
    CRONTAB = "CRONTAB"
    # 실행기기실행목록 
    EXECUTOR = "EXECUTOR"
    # 스케줄링방식
    DISPATCH = "DISPATCH"


def read_status(file) -> (ExecuteStatus, str):
    """
    에서로그파일중가져오기실행 결과
    """
    try:
        if os.path.exists(file):
            log_lines = read_last_n_lines(file, 5)
            for line in reversed(log_lines):
                line = line.strip()
                if line == "":
                    continue
                try:
                    result_json = json.loads(line)
                except Exception as e:
                    continue

                # 관리데이터
                result_json_data = result_json.get("data", {})
                if result_json_data.get("result", None) is None:
                    continue
                execute_status = ExecuteStatus(result_json_data.get("result"))

                execute_reason = result_json.get("data", {}).get("msg_str", "")
                execute_data = result_json.get("data", {}).get("data", "")
                if execute_status == execute_status.SUCCESS:
                    execute_reason = ""

                # 직선연결결과
                return execute_status, execute_reason, execute_data
    except Exception as e:
        logger.exception("read_exec_status error: {}".format(e))
    return ExecuteStatus.FAIL, "실행로그비어 있습니다", {}


class Executor:
    """실행기기 """

    def __init__(
        self,
        project_id: str = "",  # id
        project_name: str = "",  # 이름
        exec_id: str = "",  # 실행id
        exec_port: int = 0,  # 실행단말
        ins: Union[SubPopen, WindowVirtualDeskSubprocessAdapter] = None,  # 실행기기
        recording_path: str = "",  # 기록제어로그경로
        exec_position: ProjectExecPosition = ProjectExecPosition.EDIT_PAGE,  # 실행위치
        task_id: str = "",  # 예약 작업id
        task_exec_id: str = "",  # 예약 작업실행id
        open_virtual_desk: bool = False,
        version: str = "",  # 버전
        run_param: str = "",  # 실행매개변수
    ):
        # 매칭데이터
        self.project_id = project_id
        self.project_name = project_name
        self.exec_id = exec_id
        self.exec_port = exec_port
        self.__ins__ = ins
        self.recording_path = recording_path
        self.exec_position = exec_position
        self.task_id = task_id
        self.task_exec_id = task_exec_id
        self.open_virtual_desk = open_virtual_desk
        self.version = version
        self.run_param = run_param
        # 여부필요전송로그파일
        self.is_send_log_event = True

        # -프로세스상태
        self.open_async = False  # 여부열기시작돌아가기
        self.kill_time = 0  # 강함시간 0 아니오강함 >0 강함 <0 완료강함
        self.report_log_time = 0  # 위 0 위 > 0 위중 <0 위결과
        self.run_param_file = None  # run_param시파일 경로

        # -실행결과
        self.execute_status = ExecuteStatus.EXECUTE  # 실행상태
        self.execute_reason = None  # 실행원인
        self.execute_data = None  # 실행반환데이터
        self.execute_video_path = None  # 실행경로
        self.execute_data_table_path = None  # 데이터테이블경로

    @property
    def ins(self):
        return self.__ins__

    @ins.setter
    def ins(self, value):
        self.__ins__ = value

    def run(self):
        """시작"""
        if self.open_virtual_desk and sys.platform != "win32":
            self.__ins__.run(env=virtual_desk.env)
        else:
            self.__ins__.run()

    def wait_start(self, time_out=5, interval=0.1) -> bool:
        """대기정상시작"""
        if isinstance(self.__ins__, SubPopen):
            if not self.__ins__:
                return False

            for i in range(int(time_out / interval)):
                if not check_port(port=self.exec_port):
                    return True
                time.sleep(interval)
            return False
        elif isinstance(self.__ins__, WindowVirtualDeskSubprocessAdapter):
            return True
        else:
            raise NotImplementedError()

    def kill(self):
        """강함행닫기 """
        if self.__ins__:
            if self.__ins__.is_alive():
                self.__ins__.kill()
        self.kill_time = -1  # [강함제어닫기결과]

    def close(self):
        """닫기 """
        # 완료결과
        if not self.__ins__.is_alive():
            return

        # 결과가있음닫기,및닫기, 강함제어닫기
        self.kill_time = time.time() + 3  # [강함제어닫기]
        # 및닫기
        if not check_port(port=self.exec_port):
            ws = websocket.create_connection(f"ws://127.0.0.1:{self.exec_port}/?tag=scheduler")
            closed_event = {
                "event_id": self.exec_id,
                "event_time": int(time.time()),
                "channel": "flow",
                "key": "close",
                "data": {},
            }
            ws.send(json.dumps(closed_event))
            time.sleep(0.1)
            ws.close()


class ExecutorManager:
    """실행기기관리관리"""

    def __init__(self, svc):
        # 실행열
        self.svc = svc
        self.thread_lock = threading.Lock()
        self.report_log_lock = threading.Lock()
        # 정상에서실행큐
        self.executor_list = {}

        # 일시스템계획데이터
        self.curr_task_name = ""
        self.curr_project_name = ""
        self.curr_log_name = ""
        self.curr_task_id = ""

        # 예외작업관리
        threading.Thread(target=self.async_call, daemon=True).start()

    def create(
        self,
        project_id: str = "",  # id
        project_name: str = "",  # 이름
        process_id: str = "",  # 프로세스id
        line: int = 1,  # 시도행
        end_line: int = 0,  # 시도행
        debug: str = None,  # debug방식
        exec_position: ProjectExecPosition = ProjectExecPosition.EDIT_PAGE,  # 실행위치
        recording_config: dict = None,  # 기록제어기기매칭
        hide_log_window: bool = False,  # 여부로그
        task_id: str = "",  # 예약 작업id
        task_name: str = "",  # 예약 작업이름
        task_exec_id: str = "",  # 예약 작업실행id
        run_param: str = "",  # 실행매개변수
        open_virtual_desk: bool = False,  # 
        version: str = "",  # 버전
        is_send_log_event: bool = True,  # 여부필요전송로그파일
        is_custom_component: bool = False,  # 여부예지정컴포넌트
    ):
        """시작일개"""
        executor = Executor()
        executor.project_id = project_id
        executor.project_name = project_name
        executor.exec_position = exec_position
        executor.task_id = task_id
        executor.task_exec_id = task_exec_id
        executor.open_virtual_desk = open_virtual_desk
        executor.version = version
        executor.run_param = run_param
        executor.is_send_log_event = is_send_log_event

        # 1. 로그위
        if exec_position in [
            ProjectExecPosition.DISPATCH,
            ProjectExecPosition.CRONTAB,
            ProjectExecPosition.EXECUTOR,
        ]:
            executor.exec_id = self.get_execute_id(
                project_id,
                exec_position,
                Terminal.get_terminal_id(),
                task_exec_id,
                executor.version,
                executor.run_param,
            )
            if not executor.exec_id:
                raise Exception(r"서버연결예외, 실행실패")
        if not executor.exec_id:
            executor.exec_id = str(uuid.uuid1())

        # 2. 조회여부사용
        if self.status():
            raise Exception("완료있음실행, 시작 실패...")

        # 2.1 시스템계획데이터
        self.curr_task_name = task_name
        self.curr_task_id = task_id
        self.curr_project_name = project_name
        self.curr_log_name = os.path.join(r"logs", "report", executor.project_id, "{}.txt".format(executor.exec_id))

        # 3. 가져오기단말
        executor.exec_port = self.svc.get_validate_port(None)

        # 4. 생성
        exec_python = create_project_venv(self.svc, project_id)

        if open_virtual_desk and sys.platform == "win32":
            ins = WindowVirtualDeskSubprocessAdapter(self.svc, exec_python=exec_python)
        else:
            ins = SubPopen(name="executor", cmd=[exec_python, "-m", "astronverse.executor"])

        ins.set_param("port", executor.exec_port)
        ins.set_param("gateway_port", self.svc.rpa_route_port)
        ins.set_param("project_id", executor.project_id)
        ins.set_param("mode", exec_position.value)
        ins.set_param("exec_id", executor.exec_id)
        if run_param:
            try:
                # 에서 temp 디렉터리아래생성시파일
                temp_dir = os.path.join(os.getcwd(), "logs", "param")
                if os.path.exists(temp_dir):
                    if os.listdir(temp_dir):
                        shutil.rmtree(temp_dir)
                else:
                    os.makedirs(temp_dir)
                random_filename = f"run_param_{uuid.uuid4().hex}.tmp"
                temp_file_path = os.path.join(temp_dir, random_filename)

                # 파싱 run_param 문자열로 JSON 객체, 후입력파일
                try:
                    run_param_obj = json.loads(run_param)
                    with open(temp_file_path, "w", encoding="utf-8") as f:
                        json.dump(run_param_obj, f, ensure_ascii=False)
                except (json.JSONDecodeError, TypeError):
                    with open(temp_file_path, "w", encoding="utf-8") as f:
                        f.write(run_param)

                executor.run_param_file = temp_file_path
                ins.set_param("run_param", quote(temp_file_path))
            except Exception:
                raise Exception("매개변수 실패...")
        if process_id:
            ins.set_param("process_id", process_id)
        if line:
            ins.set_param("line", line)
        if end_line:
            ins.set_param("end_line", end_line)
        if debug:
            ins.set_param("debug", debug)
        if is_custom_component:
            ins.set_param("is_custom_component", "y")
        if project_name:
            ins.set_param("project_name", quote(project_name))
        if version:
            ins.set_param("version", int(version))
        if self.svc.config and self.svc.config.conf_file:
            resource_dir = os.path.dirname(self.svc.config.conf_file)
            ins.set_param("resource_dir", quote(resource_dir))

        wait_web_ws = "y"
        wait_tip_ws = "y"
        if hide_log_window:
            wait_tip_ws = "n"
        if exec_position in [
            ProjectExecPosition.PROJECT_LIST,
            ProjectExecPosition.DISPATCH,
            ProjectExecPosition.CRONTAB,
            ProjectExecPosition.EXECUTOR,
        ]:
            wait_web_ws = "n"
        ins.set_param("wait_web_ws", wait_web_ws)
        ins.set_param("wait_tip_ws", wait_tip_ws)

        executor.recording_path = ""
        if recording_config and exec_position in [
            ProjectExecPosition.CRONTAB,
            ProjectExecPosition.DISPATCH,
            ProjectExecPosition.EXECUTOR,
        ]:
            try:
                if recording_config.get("enable", False):
                    ins.set_param(
                        "recording_config",
                        quote(json.dumps(recording_config, ensure_ascii=True)),
                    )
                    executor.recording_path = recording_config.get("filePath", "./logs/report")
            except Exception as e:
                pass

        executor.ins = ins

        # 6. 시작실행
        if open_virtual_desk:
            # 열기시작완료
            virtual_desk.start(self.svc)

        try:
            executor.run()
        except Exception as e:
            logger.error("ExecutorManager error: {}".format(e))
            return None
        with self.thread_lock:
            self.executor_list[executor.exec_id] = executor

        # 7. 조회여부시작완료
        if executor.wait_start(time_out=20):
            return executor
        else:
            executor.execute_status = ExecuteStatus.FAIL
            executor.execute_reason = "시작 실패"
            executor.execute_data = {}
            self.close(executor)
            return None

    def async_call(self):
        """예외작업: 돌아가기, 강함, 위대기예외작업"""
        while True:
            time.sleep(0.1)
            try:
                if len(self.executor_list) == 0:
                    time.sleep(3)  # 실행기기적음실행3s, 지연길이작업변경다중시간
                    continue

                # 매.1초조회일여부, 필요돌아가기
                for key in list(self.executor_list.keys()):
                    executor = self.executor_list[key]

                    # 작업1: 감지의여부닫기(가능예가능예예외닫기또는사용자닫기), 필요돌아가기, 아니오재복사
                    try:
                        if not executor.open_async and not executor.ins.is_alive():
                            # 시작돌아가기
                            logger.info("step1: {}".format(executor.exec_id))
                            executor.open_async = True
                            continue
                    except Exception as e:
                        logger.errr("step1 error: {}".format(e))
                        continue

                    # 작업2: 강함, 결과가돌아가기, 완료강함시간[대0], 까지완료강함시간돌아가기[강함결과후kill_time완료-1]
                    try:
                        if executor.open_async and 0 < executor.kill_time <= time.time():
                            logger.info("step2: {} {}".format(executor.exec_id, executor.kill_time))
                            executor.kill()
                    except Exception as e:
                        logger.errr("step2 error: {}".format(e))
                        continue

                    # 작업3: 로그위, 결과가돌아가기, 있음강함시간, 또는완료강함완료, 모든후, 위상태, 위상태 위 0 위 > 0 위중 <0 위결과
                    try:
                        if executor.open_async and executor.kill_time <= 0 and executor.report_log_time == 0:
                            logger.info("step3: {}".format(executor.exec_id))
                            self.report_app_log(executor)
                    except Exception as e:
                        logger.errr("step3 error: {} ".format(e))
                        continue

                    # 작업4: 전체완료, 위결과후, 삭제실행기기
                    try:
                        if executor.open_async and executor.kill_time <= 0 and executor.report_log_time < 0:
                            logger.info("step4: {}".format(executor.exec_id))
                            try:
                                if executor.open_virtual_desk:
                                    virtual_desk.stop()
                            except Exception as e:
                                pass
                            if executor.run_param_file and os.path.exists(executor.run_param_file):
                                try:
                                    os.remove(executor.run_param_file)
                                except Exception:
                                    pass
                            del self.executor_list[executor.exec_id]
                    except Exception as e:
                        logger.info("step4 error: {}".format(e))
                        continue
            except Exception as e:
                logger.error("async_call error: {} {}".format(e, traceback.format_exc()))
                pass

    def close(self, executor: Executor):
        """사용자결과, 아니오패키지닫기"""
        if executor.exec_id not in self.executor_list:
            return
        try:
            executor.close()  # 사용자결과
            executor.open_async = True  # 닫기상태
        except Exception as e:
            logger.exception("close error: {}".format(e))

    def close_by_project(self, project_id: int):
        """사용자결과, 아니오패키지닫기"""
        if len(self.executor_list) > 0:
            for _, v in self.executor_list.items():
                if v.project_id == project_id:
                    self.close(v)
                    return True
        return False

    def close_all(self):
        """사용자결과, 아니오패키지닫기"""
        for _, v in self.executor_list.items():
            self.close(v)
        return True

    def status(self) -> bool:
        """여부존재함정상에서실행의, 있음반환True"""

        with self.thread_lock:
            if len(self.executor_list) == 0:
                return False
        return True

    def task_trigger_status(self):
        """알림트리거"""

        emit_to_front(EmitType.TERMINAL_STATUS, msg={"type": "busy" if self.status() else "idle"})

    def get_execute_id(
        self,
        project_id: str,
        exec_position: ProjectExecPosition,
        terminalId="",
        task_exec_id="",
        version="",
        paramJson="",
    ):
        """서버가져오기 실행ID, 사용로그위"""
        api = "/api/robot/robot-record/save-result"
        try:
            data = {
                "robotId": project_id,
                "taskExecuteId": task_exec_id,
                "terminalId": terminalId,
                "result": ExecuteStatus.EXECUTE.value,
                "isDispatch": self.svc.terminal_mod,
                "paramJson": paramJson,
            }
            if exec_position.value:
                data["mode"] = exec_position.value
            if version:
                data["robotVersion"] = int(version)
            if self.svc.terminal_mod:
                data["dispatchTaskExecuteId"] = task_exec_id
            response = requests.post(
                url="http://127.0.0.1:{}{}".format(self.svc.rpa_route_port, api),
                json=data,
                timeout=10,
            )
            status_code = response.status_code
            text = response.text
            if status_code != 200:
                raise Exception("get error status_code: {}".format(status_code))
            logger.info("report data: {}, response: {} {}".format(data, status_code, text))
            return json.loads(text.strip())["data"]
        except Exception as e:
            logger.exception("[APP] request api: {} error: {}".format(api, e))

    def report_app_log(self, executor: Executor):
        """로그위"""
        if executor.report_log_time != 0:
            return
        executor.report_log_time = time.time()

        try:
            # 1. 안내프론트엔드닫기
            if executor.is_send_log_event:
                emit_to_front(EmitType.EXECUTOR_END)

            # 2. 로그데이터
            # 2.1 데이터테이블경로 
            src_data_table_path = os.path.join(
                self.svc.config.venv_base_dir, executor.project_id, "astron", "data_table.xlsx"
            )
            if os.path.exists(src_data_table_path):
                data_table_path = os.path.join(
                    r"logs",
                    "report",
                    executor.project_id,
                    "{}.xlsx".format(executor.exec_id),
                )
                shutil.copy2(src_data_table_path, data_table_path)
            else:
                data_table_path = ""
            executor.execute_data_table_path = data_table_path

            # 2.2 경로 
            video_path = os.path.join(
                executor.recording_path,
                executor.project_id,
                "{}.mp4".format(executor.exec_id),
            )
            if not os.path.exists(video_path):
                video_path = ""
            executor.execute_video_path = video_path

            # 3. 로그
            log_file = os.path.join(
                r"logs",
                "report",
                executor.project_id,
                "{}.txt".format(executor.exec_id),
            )
            log_content = ""
            if os.path.exists(log_file):
                # 3.1 로그파일존재함
                # 3.2 전송프론트엔드
                if executor.is_send_log_event:
                    emit_to_front(
                        EmitType.LOG_REPORT,
                        msg={
                            "exec_id": executor.exec_id,
                            "exec_position": executor.exec_position.name,
                            "log_path": log_file,
                            "data_table_path": data_table_path,
                        },
                    )

                # 3.3 가져오기로그
                log_path_size = os.path.getsize(log_file)
                if log_path_size < 10 * 1024 * 1024:
                    # 소10M의가져오기
                    with open(log_file, encoding="utf-8") as f:
                        log_content = f.readlines()
                    log_content = [json.loads(item.strip()) for item in log_content]
                    log_content = json.dumps(log_content)
                else:
                    logger.warning(f"{log_file} size is {log_path_size / (10 * 1024 * 1024)}, will ignore report.")

                # 3.4 상태
                execute_status, execute_reason, execute_data = read_status(log_file)
                executor.execute_status = execute_status
                executor.execute_reason = execute_reason
                executor.execute_data = execute_data

            # 4. 로그위
            if executor.exec_position in [
                ProjectExecPosition.CRONTAB,
                ProjectExecPosition.DISPATCH,
                ProjectExecPosition.EXECUTOR,
            ]:
                # 전송알림
                if executor.execute_status == ExecuteStatus.FAIL:
                    NotifyUtils(self.svc).send(
                        "{} ID: {}".format(executor.project_name, executor.project_id),
                        datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                    )

                # 로그위
                data = {
                    "robotId": executor.project_id,
                    "executeId": executor.exec_id or "",
                    "taskExecuteId": executor.task_exec_id,
                    "result": executor.execute_status.value,
                    "errorReason": executor.execute_reason,
                    "executeLog": log_content,
                    "terminalId": Terminal.get_terminal_id(),
                    "videoLocalPath": video_path,
                    "dataTablePath": data_table_path,
                    "isDispatch": self.svc.terminal_mod,
                    "paramJson": executor.run_param,
                }
                if executor.exec_position.value:
                    data["mode"] = executor.exec_position.value
                if executor.version:
                    data["robotVersion"] = int(executor.version)
                if self.svc.terminal_mod:
                    data["dispatchTaskExecuteId"] = executor.task_exec_id
                response = requests.post(
                    url="http://127.0.0.1:{}/api/robot/robot-record/save-result".format(self.svc.rpa_route_port),
                    json=data,
                    timeout=10,
                )
                status_code = response.status_code
                text = response.text
                logger.info("report log data: {}, response: {} {}".format(data, status_code, text))
        except Exception as e:
            logger.exception("report_app_log error: {}".format(e))
        finally:
            executor.report_log_time = -1