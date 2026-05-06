"""
메일닫기의유형지정.
"""

from enum import Enum


class EmailServerType(Enum):
    """메일 서비스 서버유형."""

    OTHER = "other"
    NETEASE_126 = "126"
    NETEASE_163 = "163"
    QQ = "qq"
    SHOPRPA = "shoprpa"


class EmailReceiverType(Enum):
    """메일수신유형."""

    POP3 = "pop3"
    IMAP = "imap"


class EmailSeenType(Enum):
    """메일완료상태유형."""

    ALL = "ALL"
    UNSEEN = "Unseen"
