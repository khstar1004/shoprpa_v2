from typing import Any, Optional

import pyautogui
import uiautomation as auto
from astronverse.picker import APP, IElement, PickerDomain, PickerType, Point, Rect, BROWSER_UIA_POINT_CLASS
from astronverse.picker.logger import logger
from astronverse.picker.utils.cv import screenshot
from astronverse.picker.utils.process import get_process_name
from astronverse.picker.utils.window import validate_window_rect

element_aliases = {
    "AppBarControl": "사용프로그램",
    "ButtonControl": "버튼",
    "CalendarControl": "일정보",
    "CheckBoxControl": "복사선택",
    "ComboBoxControl": "그룹합치기",
    "CustomControl": "지정",
    "DataGridControl": "데이터네트워크격식",
    "DataItemControl": "데이터",
    "DocumentControl": "문서",
    "EditControl": "",
    "GroupControl": "분그룹",
    "HeaderControl": "제목",
    "HeaderItemControl": "제목",
    "HyperlinkControl": "초과연결",
    "ImageControl": "이미지",
    "ListControl": "목록",
    "ListItemControl": "목록 ",
    "MenuBarControl": "메뉴",
    "MenuControl": "메뉴",
    "MenuItemControl": "메뉴",
    "PaneControl": "격식",
    "ProgressBarControl": "정도",
    "RadioButtonControl": "단일선택버튼",
    "ScrollBarControl": "",
    "SemanticZoomControl": "",
    "SeparatorControl": "분기호",
    "SliderControl": "",
    "SpinnerControl": "조정기기",
    "SplitButtonControl": "분할버튼",
    "StatusBarControl": "상태",
    "TabControl": "선택",
    "TabItemControl": "선택",
    "TableControl": "테이블",
    "TextControl": "텍스트",
    "ThumbControl": "",
    "TitleBarControl": "제목",
    "ToolBarControl": "도구",
    "ToolTipControl": "도구안내",
    "TreeControl": "결과",
    "TreeItemControl": "",
    "WindowControl": "창",
}


class UIAElement(IElement):
    def __init__(self, control: auto.Control):
        self.control = control
        self.__index = None  # cache index
        self.__rect = None  # cache rect
        self.__tag = None

    def rect(self) -> Rect:
        if self.__rect is None:
            rect = self.control.BoundingRectangle
            self.__rect = Rect(rect.left, rect.top, rect.right, rect.bottom)
            valid_res = True
            is_program_manager = self.control.ClassName == "Progman" and self.control.Name == "Program Manager"
            if is_program_manager:
                valid_res = validate_window_rect(rect.left, rect.top, rect.right, rect.bottom)
            # logger.info(f'UIALocator rect  {valid_res}')
            if not valid_res:
                self.__rect.left = 1
                self.__rect.top = 1
                self.__rect.right = pyautogui.size().width - 1
                self.__rect.bottom = pyautogui.size().height - 1
        return self.__rect

    def index(self) -> int:
        if self.__index is None:
            self.__index = 0
            pre = self.control.GetPreviousSiblingControl()
            while pre:
                self.__index += 1
                pre = pre.GetPreviousSiblingControl()
        return self.__index

    def tag(self) -> str:
        if self.__tag is None:
            tag = self.control.ControlTypeName
            if element_aliases.get(tag):
                tag = element_aliases.get(tag)
            self.__tag = tag
        return self.__tag

    def _is_same_control(self, control1, control2) -> bool:
        """
        개 control 여부예일개요소
        사용 RuntimeId, 단계사용방법법
        """
        try:
            # 방법법1: RuntimeId (가능, UIA 보관인증일)
            if hasattr(control1, "GetRuntimeId") and hasattr(control2, "GetRuntimeId"):
                rid1 = control1.GetRuntimeId()
                rid2 = control2.GetRuntimeId()
                if rid1 and rid2:
                    return rid1 == rid2
        except Exception:
            pass

        try:
            # 방법법2: NativeWindowHandle (선택)
            h1 = getattr(control1, "NativeWindowHandle", None)
            h2 = getattr(control2, "NativeWindowHandle", None)
            if h1 and h2 and h1 != 0 and h2 != 0:
                return h1 == h2
        except Exception:
            pass

        try:
            # 방법법3: 속성그룹합치기 ()
            rect1 = control1.BoundingRectangle
            rect2 = control2.BoundingRectangle
            return (
                control1.ControlTypeName == control2.ControlTypeName
                and control1.ClassName == control2.ClassName
                and control1.Name == control2.Name
                and rect1.left == rect2.left
                and rect1.top == rect2.top
                and rect1.right == rect2.right
                and rect1.bottom == rect2.bottom
            )
        except Exception:
            return False

    def _has_same_type_sibling(self, parent_control, current_control, tag_name) -> bool:
        try:
            children = parent_control.GetChildren()

            for child in children:
                # 건너뛰기
                if self._is_same_control(child, current_control):
                    continue

                # 건너뛰기
                try:
                    if UIAOperate._is_desktop_element(child):
                        continue
                except:
                    pass

                # 까지유형
                if child.ControlTypeName == tag_name:
                    return True

            return False
        except Exception as e:
            logger.warning(f"빠름조회유형실패: {e}")
            return True  # 보관

    def _get_siblings_by_tag(self, parent_control, current_control, tag_name) -> list:
        try:
            children = parent_control.GetChildren()

            sibling_list = []
            for i, child in enumerate(children):
                # 건너뛰기
                if self._is_same_control(child, current_control):
                    continue

                # 건너뛰기
                try:
                    if UIAOperate._is_desktop_element(child):
                        continue
                except:
                    pass

                # 유형의
                if child.ControlTypeName != tag_name:
                    continue

                try:
                    sibling_value = child.GetValuePattern().Value
                except Exception:
                    sibling_value = None

                sibling_attrs = {
                    "cls": child.ClassName,
                    "name": child.Name,
                    "tag_name": child.ControlTypeName,
                    "value": sibling_value,
                    "index": i,
                }
                sibling_list.append(sibling_attrs)

            return sibling_list
        except Exception as e:
            logger.warning(f"가져오기 유형실패: {e}")
            return []

    def _get_empty_attrs(self, current_attrs: dict) -> list:
        """
        가져오기빈값속성목록

        반환: 빈값속성이름목록
        """
        priority_attrs = ["tag_name", "name", "cls", "value", "index"]
        empty_attrs = []
        for attr in priority_attrs:
            attr_value = current_attrs.get(attr)
            # 빈문자열, None, 빈문자열비어 있습니다값
            if attr_value is None or str(attr_value).strip() == "":
                empty_attrs.append(attr)
        return empty_attrs

    def _calculate_disable_keys_without_siblings(self, current_attrs: dict) -> list:
        """
        계획있음시의 disable_keys(선택 tag_name)

        반환: 아니오필요선택의속성이름목록
        """
        priority_attrs = ["tag_name", "name", "cls", "value", "index"]
        empty_attrs = self._get_empty_attrs(current_attrs)

        # tag_name 결과가빈, 선택;아니오이면출력예외
        if "tag_name" not in empty_attrs:
            # tag_name 빈, 전체사용 안 함
            disable_keys = [attr for attr in priority_attrs if attr != "tag_name"]
            return disable_keys
        else:
            # tag_name 비어 있습니다(아니오해당출력)
            raise Exception("tag_name 비어 있습니다, 불가일요소")

    def _calculate_disable_keys_progressive(
        self, current_attrs: dict, parent_control, current_control, is_root_level: bool = False
    ) -> list:
        priority_attrs = ["tag_name", "cls", "name", "value", "index"]

        empty_attrs = self._get_empty_attrs(current_attrs)
        available_attrs = [attr for attr in priority_attrs if attr not in empty_attrs]

        logger.info("========== 열기 계획 disable_keys ==========")
        logger.info(f"현재요소: {current_attrs}")
        logger.info(f"empty_attrs: {empty_attrs}")
        logger.info(f"available_attrs: {available_attrs}")
        logger.info(f"is_root_level: {is_root_level}")
        logger.info(f"parent_control is None: {parent_control is None}")

        if not available_attrs:
            logger.info("있음가능사용속성, 반환전체사용 안 함")
            return priority_attrs.copy()

        if not parent_control:
            if is_root_level:
                logger.info(f"없음, 사용 안 함빈값속성: {empty_attrs}")
                current_attrs.pop("index")
                return []  # empty_attrs.copy()
            else:
                result = self._calculate_disable_keys_without_siblings(current_attrs)
                logger.info(f"없음, 반환: {result}")
                return result

        tag_name = current_attrs.get("tag_name")
        has_same_type = self._has_same_type_sibling(parent_control, current_control, tag_name)
        logger.info(f"has_same_type: {has_same_type}")

        if not has_same_type:
            disable_keys = empty_attrs.copy()
            disable_keys.extend([attr for attr in priority_attrs if attr != "tag_name" and attr not in empty_attrs])
            logger.info(f"있음유형, 필요 tag_name, disable_keys: {disable_keys}")
            return disable_keys

        sibling_list = self._get_siblings_by_tag(parent_control, current_control, tag_name)
        logger.info(f"가져오기까지 {len(sibling_list)} 개유형")

        if not sibling_list:
            disable_keys = empty_attrs.copy()
            disable_keys.extend([attr for attr in priority_attrs if attr != "tag_name" and attr not in empty_attrs])
            logger.info(f"목록비어 있습니다, 필요 tag_name, disable_keys: {disable_keys}")
            return disable_keys

        for i in range(len(available_attrs)):
            check_attrs = available_attrs[: i + 1]
            logger.info(f"{i + 1}시도: {check_attrs}")

            has_conflict = False
            for sibling in sibling_list:
                all_match = True
                for attr in check_attrs:
                    current_value = str(current_attrs.get(attr, "")).strip()
                    sibling_value = str(sibling.get(attr, "")).strip()
                    if current_value != sibling_value:
                        all_match = False
                        break

                if all_match:
                    logger.info(f"  발송: name={sibling.get('name')}, cls={sibling.get('cls')}")
                    has_conflict = True
                    break

            if not has_conflict:
                disable_keys = empty_attrs.copy()
                disable_keys.extend(available_attrs[i + 1 :])
                logger.info(f"  까지소속성: {check_attrs}")
                logger.info(f"  종료 disable_keys: {disable_keys}")
                return disable_keys

        logger.info(f"모든속성필요, 사용 안 함빈값: {empty_attrs}")
        return empty_attrs

    def path(self, svc=None, strategy_svc=None) -> dict:
        curr_ele = self
        path_list = []
        parent_rects = []
        while True:
            # 추가원정보까지경로목록
            try:
                value = curr_ele.control.GetValuePattern().Value
            except Exception:
                value = None

            # 현재요소의모든속성(추가빈속성)
            current_attrs = {}

            # tag_name
            tag_name = curr_ele.control.ControlTypeName
            if tag_name and str(tag_name).strip():
                current_attrs["tag_name"] = tag_name

            # cls
            cls_name = curr_ele.control.ClassName
            if cls_name and str(cls_name).strip():
                current_attrs["cls"] = cls_name

            # name
            name = curr_ele.control.Name
            current_attrs["name"] = name

            # value
            if value is not None and str(value).strip():
                current_attrs["value"] = value

            # index 예추가
            current_attrs["index"] = curr_ele.index()

            # 가져오기 요소
            parent_control = curr_ele.control.GetParentControl()

            # 여부예(있음요소또는까지단계)
            is_root_level = False
            if not parent_control:
                # 있음요소, 예의
                is_root_level = True
                parent = None
            else:
                parent = UIAElement(control=parent_control)
                parent_rects.append(parent.rect())

                # 조회parent여부까지단계
                if UIAOperate._is_desktop_element(parent.control):
                    is_root_level = True

            # === 사용방식계획 disable_keys ===
            disable_keys = self._calculate_disable_keys_progressive(
                current_attrs, parent_control if not is_root_level else None, curr_ele.control, is_root_level
            )

            current_attrs["checked"] = True
            current_attrs["disable_keys"] = disable_keys
            path_list.append(current_attrs)

            # 결과가예, 결과
            if is_root_level:
                break

            curr_ele = parent

        # 생성반환결과
        path_list.reverse()
        res = {
            "version": "1",
            "type": PickerDomain.UIA.value,
            "app": get_process_name(self.control.ProcessId),
            "path": path_list,
            "img": {
                "self": screenshot(self.rect()),
            },
        }
        pick_type = strategy_svc.data.get("pick_type")
        if pick_type == PickerType.SIMILAR:
            from astronverse.locator.locator import LocatorManager

            similar_path = UIAPicker.get_similar_path(strategy_svc, res)
            if similar_path is None:
                raise Exception("아니오까지요소")
            res["path"] = similar_path
            res["img"]["self"] = strategy_svc.data.get("data", {}).get("img", {}).get("self", "")
            res["picker_type"] = PickerType.SIMILAR.value  # 개필요에서전, 가능locator사용
            similar_list = LocatorManager().locator(res, timeout=10)
            if isinstance(similar_list, list):
                similar_count = len(similar_list)
                if similar_count == 0:
                    raise Exception("아니오까지요소")
            else:
                raise Exception("아니오까지요소")
            res["similar_count"] = similar_count
        return res


class UIAPicker:
    """UIA선택, uiautomation의설치, 

    UIAOperate필요예Operate필요예전관리으로의행관리,
    UIAPicker변경추가선택본의가능, 를모든의변환성공매칭, 사용
    """

    __uia_control_cache__: Optional[UIAElement] = None

    @classmethod
    def _initialize_control(cls, control: auto.Control):
        try:
            # fix: 복사있음시예아니오아래의, 가져올 의다시 아래행
            # 중 50026, 50030 분테이블 GroupControl, DocumentControl
            first_child = control.GetFirstChildControl()
            # logger.info(f'시도 __control_init__ {first_child}  {control.ControlType}')
            if not first_child and control.ControlType in [50026, 50030, 50033]:
                while control:
                    control = control.GetParentControl()
        except Exception as e:
            logger.info(f"uia예외 {e}")

    @classmethod
    def _search_elements_recursively(
        cls,
        res_list: list[UIAElement],
        control: auto.Control,
        point: Point,
        ignore_parent_zero=False,
        deep=1,
    ):
        """정도"""

        if deep == 1 and UIAOperate._is_desktop_element(control):
            # 경과다중의
            return

        for child in control.GetChildren():
            rect = child.BoundingRectangle

            # 결과가에서개내부추가
            contains = Rect.check_point_containment(rect.left, rect.top, rect.right, rect.bottom, point)
            if contains:
                res_list.append(UIAElement(control=child))

            # 결과가parent의, 가능으로.아니오이면필요패키지에서내부가능
            if contains:
                cls._search_elements_recursively(res_list, child, point, ignore_parent_zero, deep + 1)
            else:
                parent_zero = rect.left == 0 and rect.top == 0 and rect.right == 0 and rect.bottom == 0
                if parent_zero and ignore_parent_zero:
                    cls._search_elements_recursively(res_list, child, point, ignore_parent_zero, deep + 1)

    @classmethod
    def get_similar_path(cls, strategy_svc, curr_path):
        """사용자지정개요소"""

        old_ele = strategy_svc.data["data"]
        new_ele = curr_path

        # 필터링
        if old_ele.get("app", "") != new_ele.get("app", ""):
            return None
        if old_ele.get("type", "") != "uia" or new_ele.get("type", "") != "uia":
            return None
        path1 = old_ele.get("path", [])
        path2 = new_ele.get("path", [])
        if not path1 or not path2 or len(path1) != len(path2):
            return None

        # 
        match_similar = False
        is_first = True
        for i, v in enumerate(path1):
            if i == 0:
                attrs = ["tag_name", "cls", "name", "value"]
                for attr in attrs:
                    self_attr = path1[i].get(attr, None)
                    other_attr = path2[i].get(attr, None)
                    if self_attr and other_attr and self_attr != other_attr:
                        return None
                path1[i]["similar_parent"] = True
            else:
                is_eq = True
                attrs = ["tag_name", "cls", "name", "value", "index"]
                for attr in attrs:
                    self_attr = path1[i].get(attr, None)
                    other_attr = path2[i].get(attr, None)
                    if self_attr is not None and other_attr is not None and self_attr != other_attr:
                        is_eq = False
                        break
                if is_eq and not match_similar:
                    path1[i]["similar_parent"] = True  # similar_parent 개값예식별자의단계
                else:
                    match_similar = True
                    if is_first:
                        # 일요소 tag_name 분
                        is_first = False
                        path1[i]["disable_keys"] = ["cls", "name", "value", "index"]
                    else:
                        # 후의제거 name 분
                        path1[i]["disable_keys"] = ["name", "value"]
        if not match_similar:
            return None
        return path1

    @classmethod
    def get_element(cls, root: UIAElement, point: Point, **kwargs) -> UIAElement:
        # 가져오기매칭
        used_cache = kwargs.get("used_cache", False)
        root_need_init = kwargs.get("root_need_init", True)
        ignore_parent_zero = kwargs.get("ignore_parent_zero", True)

        # 가져오기위일개저장
        if used_cache:
            try:
                # 결과가에서위일개저장의직선연결반환
                res = cls.__uia_control_cache__
                if res and res.rect().contains(point):
                    return res
            except Exception as e:
                cls.__uia_control_cache__ = None

        # 여부열기시작root조회
        if root_need_init:
            cls._initialize_control(root.control)

        # uia행정도
        ele_list = [root]
        cls._search_elements_recursively(ele_list, root.control, point, ignore_parent_zero=ignore_parent_zero)

        # 가져오기 소의위치반환
        ele = min(ele_list, key=lambda x: x.rect().area())
        return ele


class UIAOperate:
    """UIA도구유형, uiautomation의설치"""

    @classmethod
    def _is_desktop_element(cls, control: auto.Control) -> bool:
        """여부예요소객체"""

        if control.ClassName == "#32769":
            return True
        if control.NativeWindowHandle == 0x10010:
            return True
        if control.Name == " 1":
            return True
        return False

    @classmethod
    def _is_window_element(cls, control: auto.Control) -> bool:
        """여부창객체"""

        parent = control.GetParentControl()
        if not parent:
            return False
        # 결과가의단계예예창
        return cls._is_desktop_element(parent)

    @classmethod
    def get_cursor_pos(cls) -> tuple[int, int]:
        return auto.GetCursorPos()

    @classmethod
    def get_windows_by_point(cls, point: Point) -> Optional[auto.Control]:
        """
        통신경과point가져오기창

        	    ControlFromPoint	            ControlFromPoint2
        사용	필요직선연결파일(예버튼, 입력란)	필요창단계(예소, 닫기)
        """
        try:
            return auto.ControlFromPoint2(point.x, point.y)
        except Exception as e:
            return auto.ControlFromPoint(point.x, point.y)

    @classmethod
    def get_process_id(cls, control: auto.Control) -> int:
        """가져오기 이름"""

        return control.ProcessId

    @classmethod
    def get_app_windows(cls, control: Optional[auto.Control]) -> Optional[auto.Control]:
        """ele의창"""

        if not control:
            return None
        parent = control.GetParentControl()
        if not parent:
            return control
        if cls._is_window_element(control):
            return control
        return cls.get_app_windows(parent)

    @classmethod
    def is_control_value_diff_from_web_inject(cls, control: auto.Control):
        try:
            url_win11 = control.GetLegacyIAccessiblePattern().Value  # win11

            target_ctl = control.GetFirstChildControl()  # win10

            url = target_ctl.GetLegacyIAccessiblePattern().Value or url_win11
            # logger.debug(f'가져오기까지의 URL: {url}')

            if not isinstance(url, str) or not url:
                return True  # 아니오예문자열또는비어 있습니다 web

            web_matches_schema = {"http", "https", "file", "ftp", "devtools"}

            #  URL 여부으로 schema 중작업일개열기 
            res = any(url.lower().startswith(f"{schema}://") for schema in web_matches_schema)
            # logger.info(f'명령중web {res}')
            return res

        except Exception as e:
            logger.info(f"출력예외완료: {e}")
            return True

    @classmethod
    def get_web_control(
        cls,
        control: auto.Control,
        app: APP = None,
        point=None,
    ) -> tuple[bool, int, int, Any]:
        x = point.x
        y = point.y
        point_cfg = BROWSER_UIA_POINT_CLASS.get(app.value)
        if not point_cfg:
            return False, 0, 0, 0
        tag_value, tag = point_cfg
        while control:
            if app in [APP.Firefox]:
                # Firefox: 아래조회, 필요가장자리
                for child, _ in auto.WalkControl(control, includeTop=True, maxDepth=10):
                    if child.AutomationId == tag_value:
                        bound = child.BoundingRectangle
                        if bound.left <= x <= bound.right and bound.top <= y <= bound.bottom:
                            return True, bound.top, bound.left, child.NativeWindowHandle
                        else:
                            return False, 0, 0, 0
            else:
                # 브라우저: 위단계조회현재파일의 ClassName
                if tag == "ClassName":
                    tag_match = control.ClassName
                elif tag == "AutomationId":
                    tag_match = control.AutomationId
                else:
                    tag_match = ""
                if tag_match == tag_value:
                    bound = control.BoundingRectangle
                    return True, bound.top, bound.left, control.NativeWindowHandle
            control = control.GetParentControl()
        return False, 0, 0, None


uia_picker = UIAPicker()