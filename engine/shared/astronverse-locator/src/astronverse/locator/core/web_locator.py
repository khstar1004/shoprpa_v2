from typing import Any, Optional, Union

import requests
import uiautomation as auto
from astronverse.baseline.logger.logger import logger
from astronverse.locator import (
    LIKE_CHROME_BROWSER_TYPES,
    BrowserType,
    ILocator,
    Rect,
    BROWSER_UIA_POINT_CLASS,
    BROWSER_UIA_WINDOW_CLASS,
)
from astronverse.locator.utils.window import top_browser


class WEBLocator(ILocator):
    def __init__(self, rect=None, rects=None):
        self.__rect = rect
        self.__rects = rects

    def rect(self) -> Optional[Rect]:
        if self.__rects is not None and len(self.__rects) > 0:
            return self.__rects
        return self.__rect

    def control(self) -> Any:
        return None


class WebFactory:
    """Web"""

    @classmethod
    def find(cls, ele: dict, picker_type: str, **kwargs) -> Union[WEBLocator, None]:
        cur_target_app = kwargs.get("cur_target_app")
        app = ele.get("app", "")
        if cur_target_app:
            app = cur_target_app
        if app not in LIKE_CHROME_BROWSER_TYPES:
            # 직선연결결과
            return None
        # 가져오기외부모듈매칭
        scroll_into_view = kwargs.get("scroll_into_view", True)
        scroll_into_center = kwargs.get("scroll_into_center", True)

        menu_height, menu_left = cls.__get_web_top__(ele, app=app)

        # 통신경과확장가져오기요소위치정보
        rect_res = cls.__get_rect_from_browser_plugin__(
            ele, app=app, scroll_into_view=scroll_into_view, scroll_into_center=scroll_into_center
        )
        if not rect_res:
            return None
        rect = Rect(
            int(rect_res[0]["x"] + menu_left),
            int(rect_res[0]["y"] + menu_height),
            int(rect_res[0]["right"] + menu_left),
            int(rect_res[0]["bottom"] + menu_height),
        )
        rects = []
        if len(rect_res) > 1:
            for s_rect in rect_res:
                rects.append(
                    Rect(
                        int(s_rect["x"] + menu_left),
                        int(s_rect["y"] + menu_height),
                        int(s_rect["right"] + menu_left),
                        int(s_rect["bottom"] + menu_height),
                    )
                )
        return WEBLocator(rect=rect, rects=rects)

    @classmethod
    def __get_rect_from_browser_plugin__(cls, element: dict, app: str, scroll_into_view=True, scroll_into_center=True):
        """통신경과브라우저 확장가져오기rect"""
        url = "http://127.0.0.1:9082/browser/transition"
        browser_type = app
        path_data = element.get("path", {})
        try:
            # 결과가필요까지이미지중
            if scroll_into_view:
                path_data = {**path_data, "atomConfig": {"scrollIntoCenter": scroll_into_center}}
                requests.post(
                    url, json={"browser_type": browser_type, "data": path_data, "key": "scrollIntoView"}, timeout=10
                )

            # 조회요소
            response = requests.post(
                url, json={"browser_type": browser_type, "data": path_data, "key": "checkElement"}, timeout=10
            )

            if response.status_code != 200:
                raise Exception("브라우저 확장 통신 오류입니다. 브라우저와 확장 서비스를 재시작한 뒤 다시 시도하세요.")

            logger.info(f"브라우저 확장반환결과: {response.text}")
            res_json = response.json()

            if not res_json or res_json.get("code", "") != "0000":  # 통신오류
                raise Exception("브라우저 확장 통신에 실패했습니다. 확장이 설치되어 있고 실행 중인지 확인하세요.")
            elif res_json.get("code", "") == "0000":
                data = res_json.get("data", {})
                if data.get("code", "") != "0000":  # 원오류
                    raise Exception(data.get("msg", "브라우저 확장에서 요소 정보를 가져오지 못했습니다."))
                web_info = data.get("data", {})
                return web_info["rect"]

        except requests.exceptions.ConnectionError:
            raise Exception("브라우저 확장 서비스에 연결할 수 없습니다. 확장 상태를 확인하세요.")
        except requests.exceptions.Timeout:
            raise Exception("브라우저 확장 응답 시간이 초과되었습니다. 확장 설치 및 실행 상태를 확인하세요.")
        except Exception as e:
            raise Exception(f"요소 정보를 가져오지 못했습니다: {e}")

    @classmethod
    def __get_web_top__(cls, element: dict, app: str) -> tuple[int, int]:
        """브라우저오른쪽위역할위치"""
        app_name = app
        cfg = BROWSER_UIA_WINDOW_CLASS.get(app_name)
        if not cfg:
            return 0, 0
        point_cfg = BROWSER_UIA_POINT_CLASS.get(app_name)
        if not point_cfg:
            return 0, 0

        class_name, patterns, match_type = cfg
        tag_value, tag = point_cfg

        # 조회창
        root_control = auto.GetRootControl()
        base_ctrl = None
        for control, depth in auto.WalkControl(root_control, includeTop=True, maxDepth=1):
            if control.ClassName != class_name:
                continue
            if not patterns:
                base_ctrl = control
                break
            text = control.Name.split("-")[-1].strip() if match_type == "last_in" else control.Name
            if any(p.lower() in text.lower() for p in patterns):
                base_ctrl = control
                break

        if base_ctrl is None:
            raise Exception(f"{app_name} 브라우저 창을 찾을 수 없습니다. 브라우저가 실행 중인지 확인하세요.")

        # 창
        try:
            top_browser(handle=base_ctrl.NativeWindowHandle, ctrl=base_ctrl)
        except Exception as e:
            pass

        # 가져오기위치
        for control, depth in auto.WalkControl(base_ctrl, includeTop=True, maxDepth=12):
            if tag == "ClassName":
                tag_match = control.ClassName
            elif tag == "AutomationId":
                tag_match = control.AutomationId
            else:
                tag_match = ""
            if tag_match == tag_value:
                bounding_rect = control.BoundingRectangle
                top = bounding_rect.top
                left = bounding_rect.left
                return top, left
        return 0, 0


web_factory = WebFactory()
