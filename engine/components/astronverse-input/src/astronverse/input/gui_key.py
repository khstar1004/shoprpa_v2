import random
import subprocess
import time

from astronverse.actionlib import AtomicFormType, AtomicFormTypeMeta, AtomicLevel, DynamicsItem
from astronverse.actionlib.atomic import atomicMg
from astronverse.input import KeyboardType, KeyModel, Simulate_flag
from astronverse.input.code.clipboard import Clipboard
from astronverse.input.code.keyboard import Keyboard
from astronverse.input.error import *

# 지정입력법의코드
ENGLISH = 0x0409  # 영어()
CHINESE = 0x0804  # 중국어(, 중)


class GuiKeyBoard:
    @staticmethod
    @atomicMg.atomic(
        "Gui",
        inputList=[
            atomicMg.param(
                "message",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON.value,
                    params={"size": "middle"},
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.message.show",
                        expression="return ['{}', '{}', '{}'].includes($this.keyboard_type.value)".format(
                            KeyboardType.NORMAL.value,
                            KeyboardType.DRIVER.value,
                            KeyboardType.GBLID.value,
                        ),
                    )
                ],
            ),
            atomicMg.param(
                "simulate_flag",
                dynamics=[
                    DynamicsItem(
                        key="$this.simulate_flag.show",
                        expression="return $this.keyboard_type.value == '{}'".format(KeyboardType.NORMAL.value),
                    )
                ],
            ),
            atomicMg.param(
                "interval",
                level=AtomicLevel.ADVANCED.value,
                dynamics=[
                    DynamicsItem(
                        key="$this.interval.show",
                        expression="return ['{}', '{}'].includes($this.keyboard_type.value)".format(
                            KeyboardType.NORMAL.value, KeyboardType.DRIVER.value
                        ),
                    )
                ],
            ),
        ],
    )
    def keyboard(
        keyboard_type: KeyboardType = KeyboardType.NORMAL,
        message: str = "",
        simulate_flag: Simulate_flag = Simulate_flag.NO,
        interval: float = 0.1,
    ):
        """
        키보드입력
        """
        if keyboard_type == KeyboardType.NORMAL:
            message = str(message)
            if simulate_flag == Simulate_flag.YES:
                # Keyboard.change_language(ENGLISH)
                for char in message:
                    random_num = random.uniform(0, interval)
                    Keyboard.write_unicode(char)
                    time.sleep(random_num)
                # Keyboard.change_language(CHINESE)
            elif simulate_flag == Simulate_flag.NO:
                # Keyboard.change_language(ENGLISH)
                for char in message:
                    Keyboard.write_unicode(char)
                    time.sleep(interval)
                # Keyboard.change_language(CHINESE)
            else:
                raise NotImplementedError()
        elif keyboard_type == KeyboardType.CLIP:
            msg = Clipboard.paste()
            if not msg:
                raise BaseException(CLIP_PASTE_ERROR, "Clip is empty.")
            else:
                Keyboard.hotkey("ctrl", "v")
                Clipboard.clear()
        elif keyboard_type == KeyboardType.DRIVER:
            if message == "":
                raise BaseException(KEYBOARD_MSG_ERROR, "입력내용비어 있습니다, 확인하세요입력내용")

            file_path = Keyboard.get_drive_path()
            cmd = [file_path, message, f"{interval}"]
            try:
                subprocess.run(
                    cmd, check=True, stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
                )
            except subprocess.CalledProcessError as e:
                raise BaseException(DRIVE_INPUT_ERROR, "키보드드라이버입력있음관리자 권한")
        elif keyboard_type == KeyboardType.GBLID:
            from astronverse.input.code import ghostbox as gb

            device = gb.opendevicebyid(0x5188, 0x1801)
            is_connected = gb.isconnected()
            if not device or not is_connected:
                raise BaseException(GHOST_DRIVE_ERROR, "드라이버를 찾을 수 없거나 연결되지 않았습니다. 드라이버 연결을 확인하세요")
            if message == "":
                raise BaseException(KEYBOARD_MSG_ERROR, "입력내용비어 있습니다, 확인하세요입력내용")
            try:
                Keyboard.change_language(ENGLISH)
                gb.inputstring(message)
                Keyboard.change_language(CHINESE)
            except Exception as e:
                raise BaseException(DRIVE_INPUT_ERROR, "키보드드라이버입력오류")
            finally:
                gb.closedevice()

        else:
            raise NotImplementedError()

    @staticmethod
    @atomicMg.atomic(
        "Gui",
        inputList=[
            atomicMg.param(
                "keys_str",
                types="Str",
                formType=AtomicFormTypeMeta(type=AtomicFormType.KEYBOARD.value),
            ),
        ],
    )
    def key_input(keys_str: str = "", key_model: KeyModel = KeyModel.CLICK):
        try:
            keys_str = keys_str.replace("ArrowLeft", "left")
            keys_str = keys_str.replace("ArrowRight", "right")
            keys_str = keys_str.replace("ArrowUp", "up")
            keys_str = keys_str.replace("ArrowDown", "down")
            key_list = []
            for k in keys_str.split("+"):
                key_list.append(k.strip())
            if key_model == KeyModel.CLICK:
                Keyboard.hotkey(*key_list)
            elif key_model == KeyModel.DOWN:
                for key in key_list:
                    Keyboard.key_down(key)
            elif key_model == KeyModel.UP:
                for key in key_list:
                    Keyboard.key_up(key)
            else:
                raise NotImplementedError()
        except Exception as e:
            raise BaseException(KEY_INPUT_ERROR, "키보드입력오류")