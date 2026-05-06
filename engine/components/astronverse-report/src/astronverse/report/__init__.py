"""Report 컴포넌트유형및.

 `ReportLevelType` 외부모듈선택로그단계사용.
"""

from enum import Enum

__all__ = ["ReportLevelType"]


class ReportLevelType(Enum):
    """사용자로그단계.

    값사용소문자열으로시스템일아래(예순서열/프론트엔드)출력.
    """

    INFO = "info"
    WARNING = "warning"
    ERROR = "error"