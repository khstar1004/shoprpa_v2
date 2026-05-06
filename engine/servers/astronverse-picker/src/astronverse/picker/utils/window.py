import ctypes

import pyautogui
import win32con
import win32gui
import win32print
from astronverse.picker.logger import logger
from win32api import GetSystemMetrics


def window_size():
    return pyautogui.size()


def get_screen_scale_rate_new():
    """
    근거dpi, 가져오기화면의
    :return:
    """
    import ctypes

    user32 = ctypes.windll.user32
    ctypes.windll.shcore.SetProcessDpiAwareness(2)
    sys_dpi = user32.GetDpiForSystem()
    return round(sys_dpi / 96, 2)


def get_screen_scale_rate_runtime():
    """
    시근거dpi가져오기까지의
    :return:
    """
    from ctypes import Structure, c_long, c_uint, pointer, windll

    try:

        class RECT(Structure):
            _fields_ = [
                ("left", c_long),
                ("top", c_long),
                ("right", c_long),
                ("bottom", c_long),
            ]

        rect = RECT()
        user32 = windll.user32
        rectp = pointer(rect)
        hmonitor = user32.MonitorFromRect(rectp, win32con.MONITOR_DEFAULTTOPRIMARY)
        dpix = c_uint()
        dpiy = c_uint()
        p_dpix = pointer(dpix)
        p_dpiy = pointer(dpiy)
        res = windll.shcore.GetDpiForMonitor(hmonitor, 0, p_dpix, p_dpiy)
        if res != 0:
            return get_screen_scale_rate_new()
        return round(p_dpix.contents.value / 96, 2)
    except Exception as e:
        logger.info(f"가져오기 출력완료예외{e}")
        return get_screen_scale_rate_new()


def get_system_display_size() -> tuple[int, int]:
    user32 = ctypes.windll.user32
    width = user32.GetSystemMetrics(0)
    height = user32.GetSystemMetrics(1)
    return width, height


def validate_ui_element_rect(left, top, right, bottom, max_width=2000, max_height=1200):
    """
    검증요소의합치기관리(시스템기존에서왼쪽위역할)
    추가검증이면: 
    1. 아니오허용로데이터[6,7](@ref)
    2. 너비정도아니오초과경과max_width(2000px, 4K의1/2너비정도)
    3. 높이정도아니오초과경과max_height(1200px, 4K의1/2높이정도)
    """
    try:
        # 유형검증(지원int/float유형)
        if not all(isinstance(v, (int, float)) for v in [left, top, right, bottom]):
            return False

        # 있음검증
        if any(v < 0 for v in [left, top, right, bottom]):
            return False

        # 위치검증
        if left >= right or top >= bottom:
            return False

        # 계획
        width = right - left
        height = bottom - top

        # 검증(영요소)
        if width <= 0 or height <= 0:
            return False

        # 대검증(일반요소[6,8](@ref))
        if width > max_width or height > max_height:
            return False

        return True
    except TypeError:
        # 관리데이터값유형입력(예문자열)
        return False


def validate_window_rect(left, top, right, bottom):
    max_width = window_size().width
    max_height = window_size().height
    try:
        # 유형검증(지원int/float유형)
        if not all(isinstance(v, (int, float)) for v in [left, top, right, bottom]):
            return False

        # 있음검증
        if any(v < 0 for v in [left, top, right, bottom]):
            return False

        # 위치검증
        if left >= right or top >= bottom:
            return False

        # 계획
        width = right - left
        height = bottom - top

        # 검증(영요소)
        if width <= 0 or height <= 0:
            return False

        # 대검증(일반요소[6,8](@ref))
        if width > max_width or height > max_height:
            return False

        return True
    except TypeError:
        # 관리데이터값유형입력(예문자열)
        return False


def get_screen_scale():
    def get_real_resolution():
        """가져오기 의분"""
        hDC = win32gui.GetDC(0)
        # 가로분
        w = win32print.GetDeviceCaps(hDC, win32con.DESKTOPHORZRES)
        # 세로분
        h = win32print.GetDeviceCaps(hDC, win32con.DESKTOPVERTRES)
        return w, h

    def get_screen_size():
        """가져오기 후의분"""
        w = GetSystemMetrics(0)
        h = GetSystemMetrics(1)
        return w, h

    real_resolution = get_real_resolution()
    screen_size = get_screen_size()

    screen_scale_rate = round(real_resolution[0] / screen_size[0], 2)
    screen_scale_rate2 = get_screen_scale_rate_runtime()
    return max(screen_scale_rate, screen_scale_rate2)