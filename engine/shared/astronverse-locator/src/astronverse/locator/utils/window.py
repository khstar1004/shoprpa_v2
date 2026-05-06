import ctypes

import pyautogui
import pygetwindow
import uiautomation as auto
import win32com
import win32com.client
import win32con
import win32gui
import win32process
from astronverse.baseline.logger.logger import logger
from astronverse.locator import PickerType, Rect
from astronverse.locator.utils.process import get_process_name
from pygetwindow._pygetwindow_win import Win32Window, isWindowVisible
from uiautomation import Control, ControlFromHandle


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

    import win32con

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
    1. 아니요허용로데이터[6,7](@ref)
    2. 너비정도아니요초과경과max_width(2000px, 4K의1/2너비정도)
    3. 높이정도아니요초과경과max_height(1200px, 4K의1/2높이정도)
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
    max_width = pyautogui.size().width
    max_height = pyautogui.size().height
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


def is_desktop_by_cls_and_name(cls_name: str, name: str) -> bool:
    """
    여부예창
    비고창시변수, cls및name변수
    """
    # 일반의창유형
    desktop_types = [
        # Windows 10/11 창
        ("WorkerW", ""),  # 창
        ("Progman", "Program Manager"),  # 창
        # Windows 7 창
        ("Shell_TrayWnd", "열기 "),  # 열기 메뉴
        ("Shell_TrayWnd", "작업"),  # 작업
        # 가능의창
        ("Shell_TrayWnd", ""),  # 빈이름의작업
        ("WorkerW", "Program Manager"),  # 버전의창
    ]

    return (cls_name, name) in desktop_types


def is_desktop_by_handle(handle, ctrl: Control) -> bool:
    """
    여부예창
    """
    if not ctrl or win32gui.GetParent(handle) != 0:
        return False
    return is_desktop_by_cls_and_name(ctrl.ClassName, ctrl.Name)


RPA_HIGHLIGHT_PROCESSES = list()
RPA_HIGHLIGHT_CHECKED = False


def is_rpa_highlight(ctrl: Control) -> bool:
    """control여부예높이창"""
    global RPA_HIGHLIGHT_CHECKED, RPA_HIGHLIGHT_PROCESSES
    if not RPA_HIGHLIGHT_CHECKED:
        RPA_HIGHLIGHT_CHECKED = True
        all_windows = pygetwindow.getWindowsWithTitle("")
        for window in all_windows:
            win_control = ControlFromHandle(window._hWnd)
            if getattr(win_control, "AutomationId", None) == "HighlightForm":
                RPA_HIGHLIGHT_PROCESSES.append(win_control.ProcessId)
    if ctrl.ProcessId in RPA_HIGHLIGHT_PROCESSES:
        return True
    return False


def get_pid_by_handle(handle: int):
    """
    에서창객체중가져오기프로그램pid
    """
    _, proc_pid = win32process.GetWindowThreadProcessId(handle)
    return proc_pid


def find_app_handles(app: str) -> list:
    """
    가져오기 지정app모든가능창, 필터링cmd명령
    """
    handles = []
    # 가져오기모든의창
    # logger.info(f"가져오기창목록: {app}")
    if app == "iexplore":
        ie_win = auto.WindowControl(searchDepth=1, ClassName="IEFrame")
        return [ie_win.NativeWindowHandle]
    for window in pygetwindow.getWindowsWithTitle(""):
        try:
            # logger.info(f"창제목: {window.title}")
            hwnd = window._hWnd
            pid = get_pid_by_handle(handle=hwnd)
            if not pid:
                continue

            app_name = get_process_name(pid)
            if app_name == "cmd":
                # 관리명령행창
                if app not in ["cmd", "conhost", "powershell", "bash"]:
                    continue
            elif app_name != app:
                # 통신창이름매칭
                continue

            if not isWindowVisible(hwnd):
                continue

            handles.append(hwnd)
        except Exception as e:
            logger.error("가져오기창실패: {}".format(e))
            continue
    return handles


DESKTOP_WINDOW_HANDLES = list()


def show_desktop_rect(rect: Rect, desktop_handle=None):
    all_windows = pygetwindow.getWindowsWithTitle("")
    for window in all_windows:
        win_control = ControlFromHandle(window._hWnd)

        # rpa높이창, 
        if is_rpa_highlight(win_control):
            continue

        # 완료소완료, 
        if window.isMinimized:
            continue

        # 창할 수 없음소, 소가져오기 
        if (desktop_handle and window._hWnd == desktop_handle) or window._hWnd in DESKTOP_WINDOW_HANDLES:
            continue

        # 창요소, 소창
        win_rect = Rect(window.left, window.top, window.right, window.bottom)
        if win_rect.overlaps(rect):
            window.minimize()


def find_window(cls_name: str, name: str, app_name: str = None) -> int:
    global DESKTOP_WINDOW_HANDLES
    is_desktop_win = is_desktop_by_cls_and_name(cls_name, name)

    # 통신경과app_name(이름)가져오기모든의창, 필터링cls_name및name
    match_list = list()
    for handle in find_app_handles(app_name):
        handler_ctrl = ControlFromHandle(handle)
        handler_name = handler_ctrl.Name
        handler_class_name = handler_ctrl.ClassName

        # 사용여부예요소필터링(창의,할 수 없음직선연결통신경과cls필터링)
        match_desktop_win = is_desktop_by_cls_and_name(handler_class_name, handler_name)
        if match_desktop_win and handle not in DESKTOP_WINDOW_HANDLES:
            # 기록모든의의handle
            DESKTOP_WINDOW_HANDLES.append(handle)
        if is_desktop_win and match_desktop_win:
            match_list.append(
                (
                    handle,
                    handler_name,
                    handler_class_name,
                    win32gui.GetParent(handle) == 0,
                    cls_name == handler_class_name,
                )
            )
            continue

        # 사용cls필터링
        if handler_class_name != cls_name:
            continue

        # 사용name필터링
        if handler_name != name:
            continue
        match_list.append(
            (
                handle,
                handler_name,
                handler_class_name,
                win32gui.GetParent(handle) == 0,
                True,
            )
        )

    # 조회win32gui.GetParent(handle) == 0, 결과가비어 있음제거개선택
    match_list_lv2 = [item for item in match_list if item[3]]
    logger.info(f"조회win32gui.GetParent(handle) == 0, 결과가비어 있음제거개선택: {match_list_lv2}")
    if not match_list_lv2:
        match_list_lv2 = match_list

    # 선택창cls일의handle, 결과가있음다중개창의
    if is_desktop_win and len(match_list_lv2) > 1:
        match_list_lv2 = [item for item in match_list_lv2 if item[4]]
        logger.info(f"선택창cls일의handle: {match_list_lv2}")
        if match_list_lv2:
            return match_list_lv2[0][0]

    # 선택창name길이의handle
    if match_list_lv2:
        match_list_lv2.sort(key=lambda item: len(item[1]))
        logger.info(f"선택창name길이의handle: {match_list_lv2}")
        return match_list_lv2[0][0]

    # 결과가있음, 이면반환0
    return 0


def find_window_handles_list(cls_name: str, name: str, app_name: str = None, picker_type=None) -> list[int]:
    """
    가져오기 지정창의handle목록, 패키지cls전체일의handle및창name길이일의handle

    :param cls_name: 창유형이름
    :param name: 창이름
    :param app_name: 사용프로그램이름
    :return: handle목록, 패키지cls전체일의handle및창name길이일의handle
    """
    if picker_type == PickerType.WINDOW.value:
        return [find_window(cls_name, name, app_name)]
    global DESKTOP_WINDOW_HANDLES
    is_desktop_win = is_desktop_by_cls_and_name(cls_name, name)

    # 통신경과app_name(이름)가져오기모든의창, 필터링cls_name및name
    match_list = list()
    for handle in find_app_handles(app_name):
        handler_ctrl = ControlFromHandle(handle)
        handler_name = handler_ctrl.Name
        handler_class_name = handler_ctrl.ClassName

        # 사용여부예요소필터링(창의,할 수 없음직선연결통신경과cls필터링)
        match_desktop_win = is_desktop_by_cls_and_name(handler_class_name, handler_name)
        if match_desktop_win and handle not in DESKTOP_WINDOW_HANDLES:
            # 기록모든의의handle
            DESKTOP_WINDOW_HANDLES.append(handle)
        if is_desktop_win and match_desktop_win:
            match_list.append(
                (
                    handle,
                    handler_name,
                    handler_class_name,
                    win32gui.GetParent(handle) == 0,
                    cls_name == handler_class_name,
                )
            )
            continue

        # 사용cls필터링
        if handler_class_name != cls_name:
            continue

        # 사용name필터링
        if handler_name != name:
            continue
        match_list.append(
            (
                handle,
                handler_name,
                handler_class_name,
                win32gui.GetParent(handle) == 0,
                True,
            )
        )

    # 조회win32gui.GetParent(handle) == 0, 결과가비어 있음제거개선택
    match_list_lv2 = [item for item in match_list if item[3]]
    logger.info(f"조회win32gui.GetParent(handle) == 0, 결과가비어 있음제거개선택: {match_list_lv2}")
    if not match_list_lv2:
        match_list_lv2 = match_list

    result_handles = []

    # 가져오기cls전체일의handle
    if is_desktop_win and len(match_list_lv2) > 1:
        cls_match_list = [item for item in match_list_lv2 if item[4]]
        logger.info(f"cls전체일의handle: {cls_match_list}")
        if cls_match_list:
            result_handles.append(cls_match_list[0][0])

    # 가져오기창name길이일의handle
    if match_list_lv2:
        # name길이정도정렬, 가져오기 길이의
        match_list_lv2.sort(key=lambda item: len(item[1]), reverse=True)
        logger.info(f"창name길이일의handle: {match_list_lv2}")

        # 가져오기 일개매칭의name로
        target_name = match_list_lv2[0][1]
        logger.info(f"목록 창이름: {target_name}")

        # match_list_lv2, 가져오기 match_list_lv2[0][1]이름의모든handle
        for item in match_list_lv2:
            handle = item[0]
            handler_name = item[1]

            # 결과가창이름및목록이름, handle아니요재복사, 이면추가까지결과중
            if handler_name == target_name and handle not in result_handles:
                result_handles.append(handle)
                logger.info(f"추가이름handle: {handle}, 창이름: {handler_name}")

    # 선택창

    return result_handles


def find_window_by_enum(cls: str, name: str, app_name: str = None) -> int:
    """
    통신경과창 classname 및 name속성가져오기 창, 반환결과가예0이면창아니요저장에서
    및find_window의예사용EnumWindows모든창, 가능까지변경다중창
    :param cls: 파일className
    :param name: 파일name
    :param app_name: 프로그램이름문자
    :return:
    """

    def get_all_windows_by_enum():
        """통신경과가져오기모든창"""
        handles = []

        def enum_win(hwnd, result):
            # 통신경과방식가져오기현재창여부예창
            if not isWindowVisible(hwnd):
                return
            handles.append(hwnd)

        win32gui.EnumWindows(enum_win, handles)
        return handles

    # 가져오기 의창목록
    enum_handles = get_all_windows_by_enum()
    logger.info(f"의창목록: {enum_handles}")
    # 결과가지정완료app_name, 추가위find_app_handles의결과, 확인아니요
    if app_name:
        try:
            app_handles = find_app_handles(app_name)
            # 병합목록, 재
            all_handles = list(set(enum_handles + app_handles))
            logger.info(f"병합후의창목록: {all_handles}")
        except Exception:
            all_handles = enum_handles
    else:
        all_handles = enum_handles

    # 복사사용find_window의매칭
    global DESKTOP_WINDOW_HANDLES
    is_desktop_win = is_desktop_by_cls_and_name(cls, name)

    match_list = []
    for handle in all_handles:
        handler_ctrl = ControlFromHandle(handle)
        if not handler_ctrl:
            continue

        handler_name = handler_ctrl.Name or ""
        handler_class_name = handler_ctrl.ClassName or ""

        # 사용여부예요소필터링(창의,할 수 없음직선연결통신경과cls필터링)
        match_desktop_win = is_desktop_by_cls_and_name(handler_class_name, handler_name)
        if match_desktop_win and handle not in DESKTOP_WINDOW_HANDLES:
            DESKTOP_WINDOW_HANDLES.append(handle)
        if is_desktop_win and match_desktop_win:
            match_list.append(
                (
                    handle,
                    handler_name,
                    handler_class_name,
                    win32gui.GetParent(handle) == 0,
                    cls == handler_class_name,
                )
            )
            continue

        # 사용cls필터링
        if handler_class_name != cls:
            continue

        # 사용name필터링 (지요소매칭)
        if handler_name != name:
            continue
        match_list.append(
            (
                handle,
                handler_name,
                handler_class_name,
                win32gui.GetParent(handle) == 0,
                True,
            )
        )

    # 복사사용find_window의단계선택
    match_list_lv2 = [item for item in match_list if item[3]]
    logger.info(f"시도일열기 의: {match_list_lv2}")
    if not match_list_lv2:
        match_list_lv2 = match_list

    if is_desktop_win and len(match_list_lv2) > 1:
        match_list_lv2 = [item for item in match_list_lv2 if item[4]]
        if match_list_lv2:
            return match_list_lv2[0][0]

    if match_list_lv2:
        match_list_lv2.sort(key=lambda item: len(item[1]), reverse=True)
        logger.info(f"선택창cls일의handle: {match_list_lv2}")
        return match_list_lv2[0][0]

    return 0


def find_window_by_enum_list(cls: str, name: str, app_name: str = None, picker_type=None):
    """
    통신경과창 classname 및 name속성가져오기 창, 반환결과가예0이면창아니요저장에서
    및find_window의예사용EnumWindows모든창, 가능까지변경다중창
    """
    if picker_type == PickerType.WINDOW.value:
        return [find_window_by_enum(cls, name, app_name)]

    def get_all_windows_by_enum():
        """통신경과가져오기모든창"""
        handles = []

        def enum_win(hwnd, result):
            # 통신경과방식가져오기현재창여부예창
            if not isWindowVisible(hwnd):
                return
            handles.append(hwnd)

        win32gui.EnumWindows(enum_win, handles)
        return handles

    # 가져오기 의창목록
    enum_handles = get_all_windows_by_enum()
    logger.info(f"의창목록: {enum_handles}")
    # 결과가지정완료app_name, 추가위find_app_handles의결과, 확인아니요
    if app_name:
        try:
            app_handles = find_app_handles(app_name)
            # 병합목록, 재
            all_handles = list(set(enum_handles + app_handles))
            logger.info(f"병합후의창목록: {all_handles}")
        except Exception:
            all_handles = enum_handles
    else:
        all_handles = enum_handles

    # 복사사용find_window의매칭
    global DESKTOP_WINDOW_HANDLES
    is_desktop_win = is_desktop_by_cls_and_name(cls, name)

    match_list = []
    for handle in all_handles:
        handler_ctrl = ControlFromHandle(handle)
        if not handler_ctrl:
            continue

        handler_name = handler_ctrl.Name or ""
        handler_class_name = handler_ctrl.ClassName or ""

        # 사용여부예요소필터링(창의,할 수 없음직선연결통신경과cls필터링)
        match_desktop_win = is_desktop_by_cls_and_name(handler_class_name, handler_name)
        if match_desktop_win and handle not in DESKTOP_WINDOW_HANDLES:
            DESKTOP_WINDOW_HANDLES.append(handle)
        if is_desktop_win and match_desktop_win:
            match_list.append(
                (
                    handle,
                    handler_name,
                    handler_class_name,
                    win32gui.GetParent(handle) == 0,
                    cls == handler_class_name,
                )
            )
            continue

        # 사용cls필터링
        if handler_class_name != cls:
            continue

        # 사용name필터링 (지요소매칭)
        if handler_name != name:
            continue
        match_list.append(
            (
                handle,
                handler_name,
                handler_class_name,
                win32gui.GetParent(handle) == 0,
                True,
            )
        )

    # 복사사용find_window의단계선택
    match_list_lv2 = [item for item in match_list if item[3]]
    if not match_list_lv2:
        match_list_lv2 = match_list
    result_handles = []

    # 가져오기cls전체일의handle
    if is_desktop_win and len(match_list_lv2) > 1:
        cls_match_list = [item for item in match_list_lv2 if item[4]]
        logger.info(f"cls전체일의handle: {cls_match_list}")
        if cls_match_list:
            result_handles.append(cls_match_list[0][0])

    # 가져오기창name길이일의handle
    if match_list_lv2:
        # name길이정도정렬, 가져오기 길이의
        match_list_lv2.sort(key=lambda item: len(item[1]), reverse=True)
        logger.info(f"창name길이일의handle: {match_list_lv2}")

        # 가져오기 일개매칭의name로
        target_name = match_list_lv2[0][1]
        logger.info(f"목록 창이름: {target_name}")

        # match_list_lv2, 가져오기 match_list_lv2[0][1]이름의모든handle
        for item in match_list_lv2:
            handle = item[0]
            handler_name = item[1]

            # 결과가창이름및목록이름, handle아니요재복사, 이면추가까지결과중
            if handler_name == target_name and handle not in result_handles:
                result_handles.append(handle)
                logger.info(f"추가이름handle: {handle}, 창이름: {handler_name}")

    return result_handles


def top_window(handle: int, ctrl: Control):
    # 빠름결과:창아니요필요
    if is_desktop_by_handle(handle, ctrl):
        return

    # 빠름결과:IE필요추가
    if ctrl and ctrl.ClassName == "IEFrame":
        ct = None
        root_control = auto.GetRootControl()
        for control, _ in auto.WalkControl(root_control, includeTop=True, maxDepth=1):
            if control.ClassName == "IEFrame":
                ct = control
                break
        if ct:
            ct.SetActive()
        return

    # 복사및창
    try:
        cur_window = Win32Window(handle)
        if cur_window.isMinimized:
            cur_window.restore()
            cur_window.activate()
    except Exception as e:
        pass

    # 
    if win32gui.IsIconic(handle):
        win32gui.ShowWindow(handle, win32con.SW_NORMAL)
    else:
        if ctrl.ClassName == "SAP_FRONTEND_SESSION":
            return
        # 결과합치기키보드파일
        shell = win32com.client.Dispatch("WScript.Shell")
        shell.SendKeys("%")
        win32gui.SetForegroundWindow(handle)


def top_browser(handle: int, ctrl: Control):
    # 빠름결과:창아니요필요
    if is_desktop_by_handle(handle, ctrl):
        return

    # 빠름결과:IE필요추가
    if ctrl and ctrl.ClassName == "IEFrame":
        ct = None
        root_control = auto.GetRootControl()
        for control, _ in auto.WalkControl(root_control, includeTop=True, maxDepth=1):
            if control.ClassName == "IEFrame":
                ct = control
                break
        if ct:
            ct.SetActive()
        return

    # 복사및창
    try:
        cur_window = Win32Window(handle)
        if cur_window.isMinimized:
            cur_window.restore()
            cur_window.activate()
    except Exception as e:
        pass

    # 
    if win32gui.IsIconic(handle):
        win32gui.ShowWindow(handle, win32con.SW_NORMAL)
    else:
        win32gui.SetWindowPos(
            handle,
            win32con.HWND_TOPMOST,
            0,
            0,
            0,
            0,
            win32con.SWP_NOMOVE | win32con.SWP_NOSIZE,
        )
        win32gui.SetWindowPos(
            handle,
            win32con.HWND_NOTOPMOST,
            0,
            0,
            0,
            0,
            win32con.SWP_NOMOVE | win32con.SWP_NOSIZE,
        )