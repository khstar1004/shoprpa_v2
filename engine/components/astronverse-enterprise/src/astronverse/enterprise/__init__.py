"""Enterprise module initialization"""

from enum import Enum

__all__ = ["Enterprise", "ReportLevelType"]


class ReportLevelType(Enum):
    """Report level types"""

    INFO = "info"
    WARNING = "warning"
    ERROR = "error"


from astronverse.enterprise.enterprise import Enterprise  # noqa: E402
