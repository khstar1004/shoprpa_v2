"""code 닫기유형지정"""

from dataclasses import dataclass
from typing import Any


@dataclass
class ControlInfo:
    """창파일정보"""

    name: str
    classname: str
    position: Any
    handler: Any
    client_position: Any = None