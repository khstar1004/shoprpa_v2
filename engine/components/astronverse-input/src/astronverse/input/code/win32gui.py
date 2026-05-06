"""win32gui 창닫기"""

from typing import Any

import win32com.client
import win32con
import win32gui
from astronverse.actionlib.types import WinPick
from astronverse.input.code import ControlInfo

WINDOW_SHADOW_OFFSET = 9  # 너비정도


def window_find(pick: WinPick) -> Any:
    """
    _find 조회 handle
    """
    wnd_name = pick.get("elementData", {}).get("path", [])[0].get("name", "")
    wnd_class_name = pick.get("elementData", {}).get("path", [])[0].get("cls", "")

    window_handle = win32gui.FindWindow(wnd_class_name, wnd_name)
    if not window_handle:
        window_handle = win32gui.FindWindowEx(None, None, None, wnd_name)
        if not window_handle:
            raise Exception("찾을 수 없는 목록 창{}".format(pick))
    return window_handle


def window_info(handler: Any) -> ControlInfo:
    """
    info 조회정보
    """
    window_rect = win32gui.GetWindowRect(handler)
    # 계획제거의창위치
    position = (
        window_rect[0] + WINDOW_SHADOW_OFFSET,
        window_rect[1] + WINDOW_SHADOW_OFFSET,
        window_rect[2] - WINDOW_SHADOW_OFFSET,
        window_rect[3] - WINDOW_SHADOW_OFFSET,
    )
    return ControlInfo(
        name=win32gui.GetWindowText(handler),
        classname=win32gui.GetWindowText(handler),
        position=position,
        client_position=win32gui.GetClientRect(handler),
        handler=handler,
    )


def window_top(handler):
    """창"""
    if win32gui.IsIconic(handler):
        win32gui.ShowWindow(handler, win32con.SW_NORMAL)
    else:
        # 결과합치기키보드파일
        shell = win32com.client.Dispatch("WScript.Shell")
        shell.SendKeys("%")
        win32gui.SetForegroundWindow(handler)