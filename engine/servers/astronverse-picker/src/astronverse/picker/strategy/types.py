"""닫기유형지정모듈 - 패키지데이터 유형지정"""

import dataclasses
import platform
import sys
from typing import Any

from astronverse.picker import APP, PickerDomain, Point


@dataclasses.dataclass
class StrategyEnv:
    """유형"""

    os_name: str = platform.system()
    os_version: str = platform.version()
    os_arch: str = platform.machine()
    # 변경추가의win버전
    win_version: str = sys.getwindowsversion() if platform.system() == "Windows" else ""


@dataclasses.dataclass
class StrategySvc:
    """위아래문서"""

    # 내부모듈
    app: APP = None
    process_id: int = ""
    last_point: Point = None
    start_control: Any = None

    # 외부모듈입력
    data: dict = None

    # 관리
    domain: PickerDomain = PickerDomain.UIA