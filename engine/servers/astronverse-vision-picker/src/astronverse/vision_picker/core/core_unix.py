import subprocess

from astronverse.vision_picker.core.core import IPickCore, IRectHandler
from pynput.mouse import Controller


class RectHandler(IRectHandler):
    @staticmethod
    def get_foreground_window_rect():
        try:
            # 가져오기현재창의 ID
            window_id = subprocess.check_output(
                ["xdotool", "getactivewindow"], encoding="utf-8", errors="replace"
            ).strip()

            # 가져오기창의제목
            window_name = subprocess.check_output(
                ["xdotool", "getwindowname", window_id], encoding="utf-8", errors="replace"
            ).strip()

            # 가져오기창의정보
            window_geometry = subprocess.check_output(
                ["xdotool", "getwindowgeometry", "--shell", window_id], encoding="utf-8", errors="replace"
            )
            geometry = {}
            for line in window_geometry.splitlines():
                key, value = line.split("=")
                geometry[key] = int(value)

            rect = (geometry["X"], geometry["Y"], geometry["WIDTH"], geometry["HEIGHT"])

            return window_id.decode("utf-8"), window_name, rect
        except subprocess.CalledProcessError:
            return None, None, None


class PickCore(IPickCore):
    mouse = Controller()

    @staticmethod
    def get_mouse_position():
        position = PickCore.mouse.position
        return position