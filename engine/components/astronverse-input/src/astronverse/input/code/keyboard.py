import os
import subprocess
import sys

import pyautogui
from astronverse.baseline.logger.logger import logger
from pynput.keyboard import Controller

language_map = {0x0409: "xkb:us::eng", 0x0804: "zh_CN"}  # 영어  # 중국어


class Keyboard:
    def __init__(self):
        pyautogui.FAILSAFE = False

    @staticmethod
    def change_language(language: int):
        if sys.platform == "win32":
            import win32api
            import win32gui
            from win32con import WM_INPUTLANGCHANGEREQUEST

            hwnd = win32gui.GetForegroundWindow()
            im_list = win32api.GetKeyboardLayoutList()
            im_list = list(map(hex, im_list))
            win32api.SendMessage(hwnd, WM_INPUTLANGCHANGEREQUEST, 0, language)
        else:
            try:
                # 조회현재입력법상태
                result = subprocess.run(
                    ["fcitx-remote"],
                    timeout=5,
                    capture_output=True,
                    text=True,
                    check=False,
                    encoding="utf-8",
                    errors="replace",
                )
                if result.returncode != 0:
                    logger.info("불가조회fcitx상태, 가능fcitx미완료실행")
                    return

                current_status = int(result.stdout.strip())
                logger.info(f"현재입력법상태: {current_status}")

                # 근거language매개변수 지정상태
                if language == 0x0409:  # 영어 - 상태로1(미완료)
                    expected_status = 1
                elif language == 0x0804:  # 중국어 - 상태로2()
                    expected_status = 2
                else:
                    logger.info(f"지원하지 않음의코드: {hex(language)}")
                    return

                # 여부필요
                if current_status != expected_status:
                    logger.info(f"입력할법: 에서상태{current_status}까지상태{expected_status}")
                    # 실행명령
                    subprocess.run(
                        ["fcitx-remote", "-t"],
                        timeout=5,
                        check=False,
                        stdin=subprocess.DEVNULL,
                        stdout=subprocess.DEVNULL,
                        stderr=subprocess.DEVNULL,
                    )
            except Exception as e:
                (logger.info(f"입력법시발송오류: {e}"))

    @staticmethod
    def write_char(char: str):
        """
        키보드문자
        keyboard.type()에서입력법영어상태아래가능시입력중영어문자
        """
        keyboard = Controller()
        return keyboard.type(char)

    @staticmethod
    def write_unicode(text: str, delay: float = 0):
        """
        사용 Windows API 입력 Unicode 텍스트
        지원중영어, emoji대기모든Unicode문자, 아니오입력법상태

        Args:
            text: 입력할의텍스트
            delay: 매개문자의지연(초), 0.01초

        Example:
            Keyboard.write_unicode("Hello!😀")
        """
        if sys.platform == "win32":
            from astronverse.input.code.windows_input import type_text

            return type_text(text, delay=delay)
        else:
            # Linux/Mac 돌아가기까지 pynput
            keyboard = Controller()
            return keyboard.type(text)

    @staticmethod
    def press(keys, presses: int = 1, interval: float = 0.0):
        """
        
        eg1: pyautogui.press(['left', 'left', 'left'])
        eg2: pyautogui.press('left')
        :param keys: 가능으로예배열 https://pyautogui.readthedocs.io/en/latest/keyboard.html#keyboard-keys
        """
        return pyautogui.press(keys=keys, presses=presses, interval=interval)

    @staticmethod
    def hotkey(*args, **kwargs):
        """
        
        eg: pyautogui.hotkey('ctrl', 'shift', 'esc')
        """
        return pyautogui.hotkey(*args, **kwargs)

    @staticmethod
    def key_down(key):
        """
        
        """
        return pyautogui.keyDown(key=key)

    @staticmethod
    def key_up(key):
        """
        
        :param key:  https://pyautogui.readthedocs.io/en/latest/keyboard.html#keyboard-keys
        :return:
        """
        return pyautogui.keyUp(key=key)

    @staticmethod
    def get_drive_path():
        script_dir = os.path.dirname(os.path.abspath(__file__))
        parent_dir = os.path.dirname(script_dir)
        relative_dir = os.path.join("VK", "bin", "Debug", "VK.exe")
        drive_path = os.path.join(parent_dir, relative_dir)
        return drive_path