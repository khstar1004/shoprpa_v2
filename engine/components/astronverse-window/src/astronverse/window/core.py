from abc import ABC, abstractmethod
from typing import Any

from astronverse.actionlib.types import WinPick
from astronverse.window import ControlInfo, WindowSizeType


class IWindowsCore(ABC):
    @staticmethod
    @abstractmethod
    def find(pick: WinPick) -> Any:
        pass

    @staticmethod
    @abstractmethod
    def top(handler: Any):
        pass

    @staticmethod
    @abstractmethod
    def info(handler: Any) -> ControlInfo:
        """창정보"""
        pass

    @staticmethod
    @abstractmethod
    def close(handler: Any):
        """창 닫기"""
        pass

    @staticmethod
    @abstractmethod
    def size(
        handler: Any,
        size_type: WindowSizeType = WindowSizeType.MAX,
        width: int = 0,
        height: int = 0,
    ):
        """"""
        pass

    @staticmethod
    @abstractmethod
    def toControl(handler: Any) -> Any:
        """변환성공Control"""
        pass


class IUITreeCore(ABC):
    @staticmethod
    @abstractmethod
    def GetRootControl() -> Any:
        """가져오기 Control"""
        pass

    @staticmethod
    @abstractmethod
    def WalkControl(control: Any, includeTop: bool = False, maxDepth: int = 0):
        """완료기기, Control"""
        pass

    @staticmethod
    @abstractmethod
    def toHandler(control) -> Any:
        """toHandler 변환성공HWN"""
        pass

    @staticmethod
    @abstractmethod
    def setAction(control) -> bool:
        pass