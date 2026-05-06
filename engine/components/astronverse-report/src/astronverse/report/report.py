"""
설치실행경과중의사용자단계로그출력.일개기존가능 `Report.print`, 
근거 `ReportLevelType` 분발송까지 info / warning / error 통신.
"""

from collections.abc import Callable
from typing import Any  # PEP 585: use built-in generics for dict

from astronverse.actionlib import AtomicFormType, AtomicFormTypeMeta, ReportType, ReportUser
from astronverse.actionlib.atomic import atomicMg
from astronverse.actionlib.report import report
from astronverse.report import ReportLevelType

__all__ = ["Report"]


class Report:  # pylint: disable=too-few-public-methods
    """Report 기존가능내용기기.

    목록전일개방법법 `print`, 사용시스템일알림모듈전송일사용자단계로그.
    """

    @staticmethod
    @atomicMg.atomic(
        "Report",
        inputList=[
            atomicMg.param(
                key="report_type",
                formType=AtomicFormTypeMeta(type=AtomicFormType.SELECT.value),
            ),
        ],
    )
    def print(  # pylint: disable=redefined-builtin
        report_type: ReportLevelType = ReportLevelType.INFO,
        msg: Any = "",
        **kwargs: Any,
    ) -> None:
        """출력일로그.

        매개변수:
            report_type: 로그단계, 로 INFO.
            msg: 작업가능변환로문자열의메시지객체.
            **kwargs: 실행 프레임워크비고입력의위아래문서필드, 예 `__line__`, `__process_name__` 대기.
        """
        msg = str(msg)
        info = kwargs.get("__info__", [])
        line = info[0]
        process_id = info[1]

        user_obj = ReportUser(
            log_type=ReportType.User,
            process_id=process_id,
            line=line,
            msg_str=msg,
        )
        dispatcher: dict[ReportLevelType, Callable[[ReportUser], Any]] = {
            ReportLevelType.INFO: report.info,
            ReportLevelType.WARNING: report.warning,
            ReportLevelType.ERROR: report.error,
        }
        dispatch = dispatcher.get(report_type, report.info)
        dispatch(user_obj)
        # no explicit return needed (implicit None)