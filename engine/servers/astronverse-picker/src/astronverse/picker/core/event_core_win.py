import threading
import time

import pythoncom
import pyWinhook as pyWinhook
from astronverse.picker import IEventCore, MKSign
from astronverse.picker.logger import logger
from pyWinhook import KeyboardEvent


class EventCore(IEventCore):
    """사용자키보드마우스파일"""

    def __init__(self):
        self.__hook_manager = None
        self.__closed = True
        self.__control_down = False
        # 아래개클릭후 있음일지정의대기가능
        self.__esc = False
        self.__control_left_down = False
        self.__init = False
        # 추가의로그위치
        self.__f4_pressed = False  # F4아래로그
        # 시작의위사용
        self.domain = None

    def __mouse_left_down__(self, event):
        if self.__control_down:
            self.__control_left_down = True
            return False
        return True

    def __key_pressed__(self, event: KeyboardEvent):
        if event.Key == "Lcontrol":
            self.__control_down = True
        elif event.Key == "F4":
            self.__f4_pressed = True
        return True

    def __key_released__(self, event: KeyboardEvent):
        if event.Key == "Escape":
            self.__esc = True
        if event.Key == "Lcontrol":
            self.__control_down = False
        return True

    def __un_hook__(self):
        if self.__hook_manager is None:
            return
        self.__hook_manager.UnhookMouse()
        self.__hook_manager.UnhookKeyboard()
        self.__hook_manager = None

        logger.info("EventCore __un_hook__")

    def __hook__(self):
        logger.info("EventCore __hook__ start")
        self.__hook_manager = pyWinhook.HookManager()
        self.__hook_manager.MouseLeftDown = self.__mouse_left_down__
        self.__hook_manager.KeyDown = self.__key_pressed__
        self.__hook_manager.KeyUp = self.__key_released__
        self.__hook_manager.HookMouse()
        self.__hook_manager.HookKeyboard()
        self.__init = True
        pythoncom.PumpMessages()
        logger.info("EventCore __hook__ end")

    def is_cancel(self):
        return self.__esc

    def is_focus(self):
        return self.__control_left_down

    def is_f4_pressed(self):
        """조회F4여부아래"""
        return self.__f4_pressed

    def reset_f4_flag(self):
        """재F4로그위치"""
        self.__f4_pressed = False

    def reset_cancel_flag(self):
        """재ESC가져오기 로그위치"""
        self.__esc = False

    def start(self, domain=MKSign.PICKER):
        if not self.__closed:
            return False

        logger.info("EventCore start")
        self.__init = False

        # 시작마우스및키보드hook
        threading.Thread(target=self.__hook__, args=(), daemon=True).start()
        self.__control_down = False
        self.__control_left_down = False
        self.__esc = False
        self.__f4_pressed = False
        self.__closed = False

        while not self.__init:
            time.sleep(0.01)
        self.domain = domain
        return True

    def close(self):
        if self.__closed:
            return False

        logger.info("EventCore close")
        self.__un_hook__()
        self.__control_down = False
        self.__control_left_down = False
        self.__esc = False
        self.__f4_pressed = False
        self.__closed = True
        self.domain = None
        return True