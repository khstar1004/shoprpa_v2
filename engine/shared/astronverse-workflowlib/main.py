import time
from typing import Any

from astronverse.actionlib.types import WinPick
from astronverse.input.gui import Gui
from astronverse.window.window import Window
from astronverse.workflowlib.helper import Helper


def main(*args, **kwargs) -> Any:
    h = Helper(**kwargs)
    logger = h.logger()
    params = h.params()

    # -------------시도일--통신의매개변수및반환값지정------------------

    g_a = params.get("a", 0)
    g_b = params.get("b", {})

    logger.info(g_a)
    logger.info(g_b)

    g_a = 12  # 아니요까지전역 변수
    g_b["x"] = 789  # 객체까지전역 변수

    logger.info(g_a)
    logger.info(g_b)

    # -------------시도이--단일의기존가능호출------------------

    Gui().mouse_move(position_x=10, position_y=10)
    time.sleep(3)
    Gui().mouse_move(position_x=20, position_y=20)

    # -------------시도삼--선택데이터의단일기존가능호출------------------

    pick = WinPick(h.element("1881993937947951104", []))
    Window().top(pick=pick)

    # 반환값지정
    return True