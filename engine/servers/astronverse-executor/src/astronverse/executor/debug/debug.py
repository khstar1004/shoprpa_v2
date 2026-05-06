import copy
import traceback

from astronverse.actionlib import ReportCode, ReportCodeStatus, ReportFlow, ReportFlowStatus, ReportType
from astronverse.actionlib.error import *
from astronverse.executor import ExecuteStatus
from astronverse.executor.debug.bdb import CustomBdb
from astronverse.executor.error import *
from astronverse.executor.utils.utils import str_to_list_if_possible


class Debug:
    def __init__(self, svc, args):
        self.svc = svc
        self.bdb = CustomBdb(
            project_dir=svc.conf.gen_core_path,
            ext_dir=svc.conf.gen_component_path,
            notify=self.notify,
            err_handler=python_base_error,
        )
        self.svc.main_process_id = args.process_id
        self.file_to_process = {}
        self.process = {}
        for i, v in self.svc.ast_globals.process_info.items():
            # 가져오기 프로세스id
            self.file_to_process[v.process_file_name] = v.process_id
            if not self.svc.main_process_id and v.process_name == svc.conf.main_process_name:
                self.svc.main_process_id = v.process_id

            # 가져오기 프로세스의행
            if v.process_id == self.svc.main_process_id:
                try:
                    self.svc.main_process_start_line = v.process_meta[0][0]
                except Exception as e:
                    self.svc.main_process_start_line = 1

            # 가져오기프로세스정보
            process_meta = {}
            if v.process_meta:
                for m in v.process_meta:
                    process_meta[m[0]] = m
            new_v = copy.copy(v)
            new_v.process_meta = process_meta
            self.process[v.process_id] = new_v

        # 가져오기meta데이터
        self.atomic_params_meta = {}
        for i, v in self.svc.ast_globals.atomic_info.items():
            self.atomic_params_meta[v.key] = v.params_name

    def find_log_position(self):
        file, line = self.bdb.find_nearest_caller()
        process_id = ""
        if file in self.file_to_process:
            process_id = self.file_to_process[file]
        return process_id, line

    def notify(self, typ, **kw):
        """인쇄"""

        file = kw.get("file")
        process_id = ""
        if file in self.file_to_process:
            process_id = self.file_to_process[file]

        line = kw.get("line")

        if typ == "breakpoint" or typ == "step":
            self.svc.report.info(
                ReportCode(
                    log_type=ReportType.Code,
                    process_id=process_id,
                    line=line,
                    msg_str=MSG_DEBUG_INSTRUCTION_START_FORMAT.format("{process}", line, "{atomic}"),
                    status=ReportCodeStatus.DEBUG_START,
                    debug_data={"is_break": True, "data": kw.get("merged_vars")},
                )
            )
        else:
            exc = kw.get("exc")
            exc_msg = kw.get("exc_msg")
            if isinstance(exc, ParamException):
                if process_id in self.process and line in self.process[process_id].process_meta:
                    meta = self.process[process_id].process_meta[line]
                    key = meta[3]
                    if key in self.atomic_params_meta:
                        for k, v in self.atomic_params_meta[key].items():
                            exc_msg = exc_msg.replace(k, "{}({})".format(k, v))
            self.svc.report.error(
                ReportCode(
                    log_type=ReportType.Code,
                    process_id=process_id,
                    line=line,
                    status=ReportCodeStatus.ERROR,
                    msg_str="{}".format(exc_msg),
                    error_traceback=traceback.format_exc(),
                )
            )

    def start(self, params: list) -> dict:
        """실행코드"""

        # 환경 준비, 다운로드
        if self.svc.ast_globals.project_info.requirement:
            for k, v in self.svc.ast_globals.project_info.requirement.items():
                self.svc.package.download(
                    library=v.get("package_name"),
                    version=v.get("package_version", ""),
                    mirror=v.get("package_mirror", ""),
                )
        if self.svc.ast_globals.component_info:
            for c_id, c in self.svc.ast_globals.component_info.items():
                for k, v in c.requirement.items():
                    self.svc.package.download(
                        library=v.get("package_name"),
                        version=v.get("package_version", ""),
                        mirror=v.get("package_mirror", ""),
                    )

        # 
        if self.svc.debug_model:
            # 결과가열기시작완료debug,필요추가일개일개로
            self.set_breakpoint(self.svc.main_process_id, self.svc.main_process_start_line)

        for k, v in self.svc.ast_globals.process_info.items():
            for b in v.breakpoint:
                self.set_breakpoint(v.process_id, b)

        shared = {"_args": params}
        self.bdb.cmd_start(g_v=shared)
        return shared.get("_args", {})

    def cmd_continue(self):
        """계속실행"""
        return self.bdb.cmd_continue()

    def cmd_next(self):
        """단일실행"""
        return self.bdb.cmd_next()

    def cmd_force_stop(self):
        """강함제어중실행"""
        return self.bdb.cmd_force_stop()

    def set_breakpoint(self, filename, flow_line: int):
        """ - 지원다중파일"""
        if self.svc.debug_model:
            info = self.svc.get_process_info(filename)
            if info:
                filename = info.process_file_name
            return self.bdb.set_breakpoint(filename=filename, flow_line=flow_line)

    def clear_breakpoint(self, filename: str, flow_line: int):
        """지우기 - 지원다중파일"""
        if self.svc.debug_model:
            info = self.svc.get_process_info(filename)
            if info:
                filename = info.process_file_name
            return self.bdb.clear_breakpoint(filename=filename, flow_line=flow_line)