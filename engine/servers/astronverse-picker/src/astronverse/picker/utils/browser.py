import ctypes
import time
from ctypes import wintypes
from typing import Any, Optional
from urllib.parse import urljoin

import psutil
import requests
import uiautomation as auto

# --- Win32 API  (전체영역로드일) ---
user32 = ctypes.windll.user32

from astronverse.picker.error import *


class Browser:
    """브라우저유형"""

    @staticmethod
    def send_browser_rpc(req: dict, timeout: float = None, gateway_port=8003) -> Any:
        """전송브라우저RPC요청 ."""
        url = f"http://127.0.0.1:{gateway_port}"
        return requests.post(
            urljoin(
                url,
                "browser_connector",
            )
            + "/browser/transition",
            json=req,
            timeout=timeout,
        )

    @staticmethod
    def send_browser_extension(
        browser_type: str, data: Any, key: str, gate_way_port: int, data_path: str = "", timeout: float = None
    ):
        """전송브라우저요청 ."""
        max_retries = 3
        retry_interval = 0.5  # 매재시도 , 가능근거조정

        for attempt in range(1, max_retries + 1):
            response = Browser.send_browser_rpc(
                {
                    "browser_type": browser_type,
                    "data": data,
                    "key": key,
                    "data_path": data_path,
                },
                timeout,
                gateway_port=gate_way_port,
            )

            if response.status_code != 200:
                raise Exception("브라우저 확장연결기기통신통신출력오류, 다시 시도하세요")

            res_json = response.json()
            res_code = res_json.get("code")
            res_data = res_json.get("data") or {}
            res_msg = res_data.get("msg")

            # 결과가예1001, 이면다중재시도3
            if res_code == "1001":
                if attempt < max_retries:
                    time.sleep(retry_interval)
                    continue
                else:
                    raise Exception(f"[{browser_type}] 브라우저 확장출력오류, 확인하세요확장여부설치완료열기시작")

            # 확장반환오류
            if res_code != "0000":
                raise Exception(f"[{browser_type}] {res_msg}")

            if not res_data:
                return None

            data_code = res_data.get("code")
            data_msg = res_data.get("msg", "")

            # 지정오류코드및예외테이블
            error_map = {
                "5001": (BaseException, BROWSER_EXTENSION_ERROR_FORMAT, data_msg),
                "5002": (BaseException, WEB_GET_ElE_ERROR, data_msg),
                "5003": (BaseException, WEB_EXEC_ElE_ERROR, data_msg),
                "5004": (Exception, BROWSER_EXTENSION_ERROR_FORMAT, data_msg),
            }

            if data_code in error_map and key != "getElement":
                _, _, fallback_msg = error_map[data_code]
                raise Exception(fallback_msg)

            return res_data.get("data", "")

        # 관리위아니오까지
        return None


class BrowserControlFinder:
    """근거사용이름조회의UIA파일 (경과 Win32 API 정도버전)"""

    # 이름
    PROCESS_MAP = {
        "chrome": "chrome.exe",
        "edge": "msedge.exe",
        "iexplore": "iexplore.exe",
        "firefox": "firefox.exe",
        "360chromex": "360chromex.exe",
        "360se": "360se.exe",
        "360chrome": "360chrome.exe",
        "chromium": "chromium.exe",
    }

    # 브라우저창ClassName
    CLASS_NAME_MAP = {
        "chrome": "Chrome_WidgetWin_1",
        "edge": "Chrome_WidgetWin_1",
        "firefox": "MozillaWindowClass",
        "iexplore": "IEFrame",
        "360chromex": "Chrome_WidgetWin_1",
        "360se": "Chrome_WidgetWin_1",
        "360chrome": "Chrome_WidgetWin_1",
        "chromium": "Chrome_WidgetWin_1",
    }

    @staticmethod
    def _get_hwnds_by_pid(target_pid: int) -> list[int]:
        """
        []사용 Win32 API 빠름가져오기 지정 PID 의모든 *가능* 창
        """
        hwnds = []

        def callback(hwnd, _):
            # 1. 가져오기창의ID
            window_pid = ctypes.c_ulong()
            user32.GetWindowThreadProcessId(hwnd, ctypes.byref(window_pid))

            if window_pid.value == target_pid:
                # 2. [닫기 필터링]가져오기가능창.
                # 브라우저에서후생성대량할 수 없음창, 필터링가능대상승가능.
                if user32.IsWindowVisible(hwnd):
                    hwnds.append(hwnd)
            return True

        # 지정돌아가기조정데이터유형
        WNDENUMPROC = ctypes.WINFUNCTYPE(ctypes.c_bool, wintypes.HWND, wintypes.LPARAM)
        user32.EnumWindows(WNDENUMPROC(callback), 0)
        return hwnds

    @classmethod
    def _get_process_ids(cls, process_name: str) -> set[int]:
        """가져오기모든매칭의ID합치기"""
        pids = set()
        try:
            for proc in psutil.process_iter(["pid", "name"]):
                try:
                    if proc.info["name"] and proc.info["name"].lower() == process_name.lower():
                        pids.add(proc.info["pid"])
                except (psutil.NoSuchProcess, psutil.AccessDenied):
                    continue
        except Exception as e:
            print(f"가져오기 ID실패: {e}")
        return pids

    @classmethod
    def get_control_by_app_name(cls, app_name: str, window_title: str = None) -> Optional[auto.Control]:
        """
        가져오기 창파일 - 가능버전
        """
        app_key = app_name.lower()
        process_name = cls.PROCESS_MAP.get(app_key)
        expected_class_name = cls.CLASS_NAME_MAP.get(app_key)

        if not process_name:
            print(f"지원하지 않음의사용이름: {app_name}")
            return None

        target_pids = cls._get_process_ids(process_name)
        if not target_pids:
            return None

        matched_controls = []

        # --- 열기  ---
        for pid in target_pids:
            # 1. 가져오기  (건너뛰기 UIA )
            hwnds = cls._get_hwnds_by_pid(pid)

            for hwnd in hwnds:
                try:
                    # 2. 변환객체 (없음검색열기판매)
                    control = auto.ControlFromHandle(hwnd)

                    # 3. 조회 ClassName (본속성, 빠름)
                    if expected_class_name:
                        if control.ClassName != expected_class_name:
                            continue

                    # 4. 매칭제목
                    if window_title:
                        # 있음에서후일가져오기 Name, 원인로문자열느림
                        if window_title in control.Name:
                            return control
                    else:
                        # 저장준비선택
                        matched_controls.append(control)

                except Exception:
                    # 창가능에서닫기
                    continue

        # --- 후준비선택 ---
        # 결과가있음지정제목, 반환일개가능사용의창
        if matched_controls:
            for control in matched_controls:
                if control.IsEnabled:
                    return control
            return matched_controls[0]

        return None

    @staticmethod
    def _find_document_control_recursive(
        control: auto.Control, parent_name: str, depth: int = 0, max_depth: int = 20
    ) -> Optional[auto.Control]:
        """
        조회ControlType로Document의이름 (보관기존)
        """
        if depth > max_depth:
            return None

        try:
            if control.ControlType == auto.ControlType.DocumentControl:
                return control

            children = control.GetChildren()
            for child in children:
                try:
                    result = BrowserControlFinder._find_document_control_recursive(
                        child, parent_name, depth + 1, max_depth
                    )
                    if result:
                        return result
                except:
                    continue
        except Exception:
            pass
        return None

    @staticmethod
    def get_document_control(parent_control: auto.Control) -> Optional[auto.Control]:
        """가져오기 파일아래nameControlType로Document의 (보관기존)"""
        if not parent_control:
            return None

        try:
            parent_name = parent_control.Name
            if not parent_name:
                return None

            children = parent_control.GetChildren()
            for child in children:
                try:
                    result = BrowserControlFinder._find_document_control_recursive(child, parent_name)
                    if result:
                        return result
                except:
                    continue

        except Exception as e:
            print(f"조회Document파일실패: {e}")

        return None


# 사용예시
if __name__ == "__main__":
    print("정상에서조회창...")
    start = time.perf_counter()

    # 예시: 조회 Chrome 창 (가능으로열기 Chrome 시도)
    # 결과가시도제목매칭, 가능으로입력이개매개변수, 예: "정도"
    chrome_control = BrowserControlFinder.get_control_by_app_name("Chrome")

    end1 = time.perf_counter()
    print(f"조회창시: {(end1 - start) * 1000:.2f} ms")  # 해당예초단계

    if chrome_control:
        print(f"까지매칭창: {chrome_control.Name} | Handle: {hex(chrome_control.NativeWindowHandle)}")

        # 가져오기 Document  (모듈분예조회, 일시간)
        doc_start = time.perf_counter()
        doc_control = BrowserControlFinder.get_document_control(chrome_control)
        doc_end = time.perf_counter()

        if doc_control:
            print(f"까지Document파일: {doc_control.BoundingRectangle}")
        else:
            print("찾을 수 없는 Document파일")

        print(f"조회내부모듈Document시: {doc_end - doc_start:.4f} 초")

    else:
        print("찾을 수 없는 지정사용의창")