"""
전체덮어쓰기창모듈

에서가능컴포넌트선택완료후, 생성일개전체, , 의덮어쓰기창, 
ShopRPA 클라이언트 창 외부의 마우스 입력을 차단합니다.
직선까지원저장완료후판매덮어쓰기창.

방법: Win32 Layered Window + SetWindowRgn 
- 생성전체 TOPMOST  Layered Window(alpha=1, 할 수 없음)
- SetWindowRgn으로 "전체 화면 - ShopRPA 창" 영역을 구성합니다.
- ShopRPA 창은 클릭 가능하게 유지합니다.
- 있음덮어쓰기창, 클릭
- 예약기기지정새로고침으로창/

디버그지원:
- Ctrl+Shift+F12 전체영역: 강함제어닫기덮어쓰기창
"""

import ctypes
import threading
import traceback

import win32api
import win32con
import win32gui
import win32process

from astronverse.picker.logger import logger

# Win32 일반량
LWA_ALPHA = 0x02
WS_EX_NOACTIVATE = 0x08000000
WM_HOTKEY = 0x0312
RGN_DIFF = 4  # win32con.RGN_DIFF 가능찾을 수 없습니다, 직선연결지정

# GDI32 데이터
_gdi32 = ctypes.windll.gdi32
_user32 = ctypes.windll.user32


def _create_rect_rgn(left: int, top: int, right: int, bottom: int) -> int:
    """호출 GDI32 CreateRectRgn 생성"""
    return _gdi32.CreateRectRgn(left, top, right, bottom)


def _combine_rgn(dest, src1, src2, mode: int) -> int:
    """호출 GDI32 CombineRgn 병합"""
    return _gdi32.CombineRgn(dest, src1, src2, mode)


def _delete_object(obj) -> bool:
    """호출 GDI32 DeleteObject 삭제 GDI 객체"""
    return _gdi32.DeleteObject(obj)


class BlockOverlay:
    """
    ShopRPA 창 외부의 마우스 입력을 차단하는 전체 화면 오버레이입니다.

    사용방식::

        from astronverse.picker.core.block_overlay import block_overlay

        block_overlay.show()   # 덮어쓰기창, 열기 
        block_overlay.hide()   # 덮어쓰기창, 중지

    디버그: Ctrl+Shift+F12 가능강함제어닫기덮어쓰기창
    """

    _CLASS_NAME = "ShopRPABlockOverlay"
    _RPA_WINDOW_KEYWORDS = ("ShopRPA", "Shoprpa")

    # 새로고침(초)
    _REGION_UPDATE_MS = 300

    # 디버그 & 예약기기 ID
    _HOTKEY_ID = 9901
    _TIMER_ID = 9901

    def __init__(self):
        self._hwnd: int | None = None
        self._thread: threading.Thread | None = None
        self._lock = threading.Lock()
        self._class_registered = False

        # 화면(사용변환)
        self._vx = 0
        self._vy = 0

        # 검색로그(제어로그단계)
        self._first_search = True

    def show(self):
        """덮어쓰기창, 열기 모든 ShopRPA 의마우스"""
        with self._lock:
            if self._hwnd is not None:
                logger.info("[BlockOverlay] 덮어쓰기창완료존재함, 건너뛰기생성")
                return

        self._first_search = True
        self._thread = threading.Thread(
            target=self._window_thread,
            daemon=True,
            name="BlockOverlayThread",
        )
        self._thread.start()
        logger.info("[BlockOverlay] 덮어쓰기창완료시작")

    def hide(self):
        """판매덮어쓰기창, 복사모든의마우스"""
        with self._lock:
            hwnd = self._hwnd

        if not hwnd:
            return

        try:
            win32gui.PostMessage(hwnd, win32con.WM_CLOSE, 0, 0)
        except Exception as e:
            logger.error(f"[BlockOverlay] 전송닫기메시지실패: {e}")

        if self._thread:
            self._thread.join(timeout=3)
            self._thread = None

        logger.info("[BlockOverlay] 덮어쓰기창완료닫기")

    @property
    def is_active(self) -> bool:
        """덮어쓰기창여부상태"""
        return self._hwnd is not None

    # ───────────── 창 ─────────────

    def _window_thread(self):
        """에서중생성덮어쓰기창실행메시지"""
        try:
            self._ensure_class_registered()
            self._create_overlay_window()

            # 메시지(직선까지 PostQuitMessage)
            win32gui.PumpMessages()
        except Exception as e:
            logger.error(f"[BlockOverlay] 창예외: {e}\n{traceback.format_exc()}")
        finally:
            with self._lock:
                self._hwnd = None
            logger.info("[BlockOverlay] 창완료출력")

    def _ensure_class_registered(self):
        """회원가입창유형(대기)"""
        if self._class_registered:
            return

        wc = win32gui.WNDCLASS()
        wc.lpfnWndProc = self._wnd_proc
        wc.lpszClassName = self._CLASS_NAME
        wc.hCursor = win32gui.LoadCursor(0, win32con.IDC_ARROW)
        wc.hbrBackground = 0

        try:
            win32gui.RegisterClass(wc)
            self._class_registered = True
        except Exception as e:
            # ERROR_CLASS_ALREADY_EXISTS (1410)
            if getattr(e, "winerror", 0) == 1410:
                self._class_registered = True
            else:
                raise

    def _create_overlay_window(self):
        """생성전체덮어쓰기창회원가입/예약기기"""
        # 화면(다중기기전체덮어쓰기)
        self._vx = win32api.GetSystemMetrics(win32con.SM_XVIRTUALSCREEN)
        self._vy = win32api.GetSystemMetrics(win32con.SM_YVIRTUALSCREEN)
        vw = win32api.GetSystemMetrics(win32con.SM_CXVIRTUALSCREEN)
        vh = win32api.GetSystemMetrics(win32con.SM_CYVIRTUALSCREEN)

        ex_style = (
            win32con.WS_EX_LAYERED  # 분창(지요소)
            | win32con.WS_EX_TOPMOST  # 종료
            | win32con.WS_EX_TOOLWINDOW  # 아니오에서작업
            | WS_EX_NOACTIVATE  # 아니오
        )

        hwnd = win32gui.CreateWindowEx(
            ex_style,
            self._CLASS_NAME,
            "",  # 없음제목
            win32con.WS_POPUP,  # 없음가장자리출력창
            self._vx,
            self._vy,
            vw,
            vh,
            0,
            0,
            0,
            None,
        )

        if not hwnd:
            raise RuntimeError("CreateWindowEx로 오버레이 창을 생성하지 못했습니다")

        # 정도 1/255 ≈ 0.4%, 할 수 없음
        _user32.SetLayeredWindowAttributes(hwnd, 0, 1, LWA_ALPHA)

        # 아니오
        win32gui.ShowWindow(hwnd, win32con.SW_SHOWNOACTIVATE)
        win32gui.UpdateWindow(hwnd)

        with self._lock:
            self._hwnd = hwnd

        # ── 회원가입디버그: Ctrl+Shift+F12──
        MOD_CONTROL = 0x0002
        MOD_SHIFT = 0x0004
        VK_F12 = 0x7B
        hotkey_ok = _user32.RegisterHotKey(hwnd, self._HOTKEY_ID, MOD_CONTROL | MOD_SHIFT, VK_F12)
        if hotkey_ok:
            logger.info("[BlockOverlay] 디버그 Ctrl+Shift+F12 완료회원가입(가능강함제어닫기덮어쓰기창)")
        else:
            logger.warning("[BlockOverlay] 디버그회원가입실패(가능완료사용)")

        # ── 예약기기, 지정새로고침 ──
        _user32.SetTimer(hwnd, self._TIMER_ID, self._REGION_UPDATE_MS, None)

        # ── 업데이트 ──
        self._update_window_region(hwnd)

        logger.info(f"[BlockOverlay] 덮어쓰기창완료생성 hwnd={hwnd}, 화면=({self._vx}, {self._vy}, {vw}x{vh})")

    # ───────────── 창경과 ─────────────

    def _wnd_proc(self, hwnd, msg, wparam, lparam):
        """창경과 - 관리, 예약기기, 닫기 파일"""
        try:
            if msg == WM_HOTKEY and wparam == self._HOTKEY_ID:
                logger.info("[BlockOverlay] 까지디버그 Ctrl+Shift+F12, 강함제어닫기덮어쓰기창")
                win32gui.PostMessage(hwnd, win32con.WM_CLOSE, 0, 0)
                return 0

            elif msg == win32con.WM_TIMER and wparam == self._TIMER_ID:
                self._update_window_region(hwnd)
                return 0

            elif msg == win32con.WM_CLOSE:
                # 관리예약기기및
                _user32.KillTimer(hwnd, self._TIMER_ID)
                _user32.UnregisterHotKey(hwnd, self._HOTKEY_ID)
                win32gui.DestroyWindow(hwnd)
                return 0

            elif msg == win32con.WM_DESTROY:
                win32gui.PostQuitMessage(0)
                return 0

        except Exception as e:
            logger.error(f"[BlockOverlay] WndProc 예외: {e}")

        return win32gui.DefWindowProc(hwnd, msg, wparam, lparam)

    # ───────────── 업데이트() ─────────────

    def _update_window_region(self, hwnd):
        """
        업데이트창: 전체 - RPA 창.

        를 RPA 창에서의에서덮어쓰기창위"", 
        해당물품관리위찾을 수 없습니다덮어쓰기창, 마우스클릭직선연결까지아래방법의 RPA 클라이언트.

        사용 ctypes 직선연결호출 GDI32 의 CreateRectRgn / CombineRgn / DeleteObject, 
        원인로 pywin32 의 win32gui 모듈아니오일지정내보내기 GDI 데이터.
        """
        try:
            vw = win32api.GetSystemMetrics(win32con.SM_CXVIRTUALSCREEN)
            vh = win32api.GetSystemMetrics(win32con.SM_CYVIRTUALSCREEN)

            # 생성전체(창, 에서 0,0 열기 )
            full_rgn = _create_rect_rgn(0, 0, vw, vh)
            if not full_rgn:
                logger.error("[BlockOverlay] CreateRectRgn(전체) 반환 NULL")
                return

            # 조회 RPA 창에서위
            rpa_rects = self._find_rpa_window_rects()

            for rect in rpa_rects:
                # 화면 → 창(창위치)
                rx1 = rect[0] - self._vx
                ry1 = rect[1] - self._vy
                rx2 = rect[2] - self._vx
                ry2 = rect[3] - self._vy

                rpa_rgn = _create_rect_rgn(rx1, ry1, rx2, ry2)
                if rpa_rgn:
                    _combine_rgn(full_rgn, full_rgn, rpa_rgn, RGN_DIFF)
                    _delete_object(rpa_rgn)

            # 사용(시스템연결관리 full_rgn 의명령, 아니오필요 DeleteObject)
            _user32.SetWindowRgn(hwnd, full_rgn, True)

        except Exception as e:
            logger.error(f"[BlockOverlay] 업데이트창실패: {e}\n{traceback.format_exc()}")

    # ───────────── RPA 창조회 ─────────────

    def _find_rpa_window_rects(self) -> list[tuple]:
        """
        조회모든 ShopRPA 창 닫기의화면.

        :
        1. 모든가능창
        2. 까지제목패키지 ShopRPA 의창, 기록 ID
        3. 의모든가능창(팝업/대화상자)
        """
        window_list: list[tuple[int, int, str]] = []  # (hwnd, pid, title)

        def _enum_callback(hwnd, _):
            if not win32gui.IsWindowVisible(hwnd):
                return True
            # 정렬제거
            if hwnd == self._hwnd:
                return True
            try:
                _, pid = win32process.GetWindowThreadProcessId(hwnd)
                title = win32gui.GetWindowText(hwnd)
                window_list.append((hwnd, pid, title))
            except Exception:
                pass
            return True

        try:
            win32gui.EnumWindows(_enum_callback, None)
        except Exception as e:
            logger.error(f"[BlockOverlay] 창실패: {e}")
            return []

        # ── 1:  RPA  ID ──
        rpa_pids: set[int] = set()
        for _, pid, title in window_list:
            if any(keyword in title for keyword in self._RPA_WINDOW_KEYWORDS):
                rpa_pids.add(pid)

        # ── 검색또는찾을 수 없는 시출력로그 ──
        if self._first_search or not rpa_pids:
            self._first_search = False
            if rpa_pids:
                matched = [
                    (t, p) for _, p, t in window_list if any(keyword in t for keyword in self._RPA_WINDOW_KEYWORDS)
                ]
                for title, pid in matched:
                    logger.info(f"[BlockOverlay] 매칭까지RPA창: title='{title}', pid={pid}")
            else:
                logger.warning(
                    f"[BlockOverlay] ShopRPA 창을 찾을 수 없습니다. 후보 창 수: {len(window_list)}"
                )
                # 출력있음제목의창디버그
                titled = [(t, p) for _, p, t in window_list if t.strip()]
                for title, pid in titled[:15]:
                    logger.warning(f"[BlockOverlay]   가능창: title='{title}', pid={pid}")

        if not rpa_pids:
            return []

        # ── 2:  RPA 의모든가능창 ──
        rects: list[tuple] = []
        for hwnd, pid, _ in window_list:
            if pid in rpa_pids:
                try:
                    rect = win32gui.GetWindowRect(hwnd)
                    rects.append(rect)
                except Exception:
                    pass

        return rects


# 모듈단계단일
block_overlay = BlockOverlay()
