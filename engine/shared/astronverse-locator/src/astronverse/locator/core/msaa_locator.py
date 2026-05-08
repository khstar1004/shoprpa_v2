"""
MSAA위치 지정기기모듈

Microsoft Active Accessibility (MSAA)의요소위치 지정공가능.
"""

import ctypes
import ctypes.wintypes
from typing import Any, Optional, Union

import comtypes
import comtypes.automation
import comtypes.client
from astronverse.baseline.logger.logger import logger
from astronverse.locator import ILocator, Rect
from astronverse.locator.core.uia_locator import uia_factory

# 로드MSAA닫기라이브러리
comtypes.client.GetModule("oleacc.dll")
# MSAA 역할이름
ACC_ROLE_NAME_MAP = {
    1: "TitleBar",
    2: "MenuBar",
    3: "ScrollBar",
    4: "Grip",
    5: "Sound",
    6: "Cursor",
    7: "Caret",
    8: "Alert",
    9: "Window",
    10: "Client",
    11: "PopupMenu",
    12: "MenuItem",
    13: "Tooltip",
    14: "Application",
    15: "Document",
    16: "Pane",
    17: "Chart",
    18: "Dialog",
    19: "Border",
    20: "Grouping",
    21: "Separator",
    22: "ToolBar",
    23: "StatusBar",
    24: "Table",
    25: "ColumnHeader",
    26: "RowHeader",
    27: "Column",
    28: "Row",
    29: "Cell",
    30: "Link",
    31: "HelpBalloon",
    32: "Character",
    33: "List",
    34: "ListItem",
    35: "Outline",
    36: "OutlineItem",
    37: "PageTab",
    38: "PropertyPage",
    39: "Indicator",
    40: "Graphic",
    41: "Text",
    42: "EditableText",
    43: "PushButton",
    44: "CheckBox",
    45: "RadioButton",
    46: "ComboBox",
    47: "DropDown",
    48: "ProgressBar",
    49: "Dial",
    50: "HotKeyField",
    51: "Slider",
    52: "SpinBox",
    53: "Diagram",
    54: "Animation",
    55: "Equation",
    56: "DropDownButton",
    57: "MenuButton",
    58: "GridDropDownButton",
    59: "WhiteSpace",
    60: "PageTabList",
    61: "Clock",
    62: "SplitButton",
    63: "IPAddress",
}


class MSAALocator(ILocator):
    """
    MSAA위치 지정기기유형

    사용설치MSAA요소시스템일의위치 지정연결.
    """

    def __init__(self, ia_element):
        """MSAA위치 지정기기

        Args:
            ia_element: MSAA요소객체
        """
        self.__rect = ia_element.get_rect()
        self.ia_element = ia_element

    def rect(self) -> Optional[Rect]:
        logger.info(f"msaa가져오기까지의: {self.__rect.to_json()}")
        return self.__rect

    def control(self) -> Any:
        return self.ia_element


class MSAAElement:
    """MSAA요소패키지설치유형"""

    def __init__(self, IAccessible, iObjectId):
        if not isinstance(iObjectId, int):
            raise TypeError("iObjectId예정수유형")
        self.IAccessible = IAccessible
        self.iObjectId = iObjectId
        self.dictCache = {}

    @property
    def name(self):
        return self.get_name()

    def get_acc_role(self):
        """가져오기요소역할"""
        obj_child_id = comtypes.automation.VARIANT()
        obj_child_id.vt = comtypes.automation.VT_I4
        obj_child_id.value = self.iObjectId
        obj_role = comtypes.automation.VARIANT()
        obj_role.vt = comtypes.automation.VT_BSTR
        self.IAccessible._IAccessible__com__get_accRole(obj_child_id, obj_role)
        return obj_role.value

    def get_acc_role_name(self):
        """가져오기역할이름"""
        try:
            role_id = self.get_acc_role()
            return ACC_ROLE_NAME_MAP.get(role_id)
        except Exception as e:
            logger.info("MSAA role 이름 조회 중 예외: %s", e)
            return None

    def get_type(self):
        """가져오기 파일유형"""
        # logger.info(f'가져오기 파일유형 start ')
        role_name = self.get_acc_role_name()
        if not role_name:
            role_name = "MSAA"
        # logger.info(f'가져오기 파일유형 end {role_name}')
        return role_name

    def get_name(self):
        """가져오기원이름"""
        try:
            obj_child_id = comtypes.automation.VARIANT()
            obj_child_id.vt = comtypes.automation.VT_I4
            obj_child_id.value = self.iObjectId
            obj_name = comtypes.automation.BSTR()
            self.IAccessible._IAccessible__com__get_accName(obj_child_id, ctypes.byref(obj_name))
            return obj_name.value or ""
        except:
            return ""

    def get_value(self):
        """가져오기요소값"""
        try:
            obj_child_id = comtypes.automation.VARIANT()
            obj_child_id.vt = comtypes.automation.VT_I4
            obj_child_id.value = self.iObjectId
            obj_bstr_value = comtypes.automation.BSTR()
            self.IAccessible._IAccessible__com__get_accValue(obj_child_id, ctypes.byref(obj_bstr_value))
            return obj_bstr_value.value or ""
        except:
            return ""

    def get_role(self):
        """가져오기요소역할"""
        try:
            obj_child_id = comtypes.automation.VARIANT()
            obj_child_id.vt = comtypes.automation.VT_I4
            obj_child_id.value = self.iObjectId
            obj_role = comtypes.automation.VARIANT()
            obj_role.vt = comtypes.automation.VT_BSTR
            self.IAccessible._IAccessible__com__get_accRole(obj_child_id, obj_role)
            return obj_role.value
        except:
            return None

    def get_location(self):
        """가져오기요소위치 (left, top, width, height)"""
        try:
            obj_child_id = comtypes.automation.VARIANT()
            obj_child_id.vt = comtypes.automation.VT_I4
            obj_child_id.value = self.iObjectId
            obj_left, obj_top, obj_width, obj_height = (
                ctypes.c_long(),
                ctypes.c_long(),
                ctypes.c_long(),
                ctypes.c_long(),
            )
            self.IAccessible._IAccessible__com_accLocation(
                ctypes.byref(obj_left),
                ctypes.byref(obj_top),
                ctypes.byref(obj_width),
                ctypes.byref(obj_height),
                obj_child_id,
            )
            return (obj_left.value, obj_top.value, obj_width.value, obj_height.value)
        except:
            return (0, 0, 0, 0)

    def get_rect(self):
        """가져오기 파일"""
        try:
            bound = self.get_location()
        except Exception as e:
            bound = [0, 0, 0, 0]
        right = bound[0] + bound[2]
        bottom = bound[1] + bound[3]
        rect = Rect(bound[0], bound[1], right, bottom)
        return rect

    def get_parent(self):
        """가져오기 요소"""
        try:
            objParent = self.IAccessible.accParent
            if objParent is not None:
                return MSAAElement(objParent, 0)
            return None
        except:
            return None

    def get_children(self):
        """가져오기 원목록"""
        try:
            if self.iObjectId > 0:
                return []

            child_count = self.IAccessible.accChildCount
            if child_count == 0:
                return []

            objAccChildArray = (comtypes.automation.VARIANT * child_count)()
            objAccChildCount = ctypes.c_long()
            ctypes.oledll.oleacc.AccessibleChildren(
                self.IAccessible,
                0,
                child_count,
                objAccChildArray,
                ctypes.byref(objAccChildCount),
            )

            children = []
            for i in range(objAccChildCount.value):
                objAccChild = objAccChildArray[i]
                if objAccChild.vt == comtypes.automation.VT_DISPATCH:
                    child = MSAAElement(
                        objAccChild.value.QueryInterface(comtypes.gen.Accessibility.IAccessible),
                        0,
                    )
                    children.append(child)
                else:
                    child = MSAAElement(self.IAccessible, objAccChild.value)
                    children.append(child)
            return children
        except:
            return []

    def get_index(self):
        """가져오기요소에서요소중의검색"""
        try:
            parent = self.get_parent()
            if not parent:
                logger.info(f"요소 {self.get_name()} 있음요소")
                return 0

            logger.info(f"계획요소 {self.get_name()} 의검색, 요소: {parent.get_name()}")

            # 가져오기현재요소의정보사용
            current_name = self.get_name()
            current_role = self.get_role()
            current_location = self.get_location()
            current_type = self.get_type()

            # 가져오기모든요소
            siblings = parent.get_children()

            # 요소, 까지현재요소의위치
            for index, sibling in enumerate(siblings):
                sibling_name = sibling.get_name()
                sibling_role = sibling.get_role()
                sibling_location = sibling.get_location()
                sibling_type = sibling.get_type()

                # 사용다중파일행
                is_match = False

                # 방법법1: 결과가위치정보가능사용, 사용위치()
                if current_location and sibling_location:
                    if current_location == sibling_location:
                        is_match = True
                        logger.info(f"통신경과위치매칭까지요소, 검색: {index}")

                # 방법법2: 결과가위치할 수 없음사용또는아니요매칭, 사용역할+이름+유형그룹합치기
                if not is_match:
                    if current_name == sibling_name and current_role == sibling_role and current_type == sibling_type:
                        # 일검증인증: 조회여부예일개객체
                        if sibling.IAccessible == self.IAccessible and sibling.iObjectId == self.iObjectId:
                            is_match = True
                            logger.info(f"통신경과역할+이름+유형+객체주소매칭까지요소, 검색: {index}")

                # 방법법3: 결과가이름비어 있음, 사용객체주소및ID
                if not is_match and current_name == "":
                    if sibling.IAccessible == self.IAccessible and sibling.iObjectId == self.iObjectId:
                        is_match = True
                        logger.info(f"통신경과객체주소매칭까지요소, 검색: {index}")

                if is_match:
                    return index

            logger.info("매칭 요소를 찾지 못했습니다. 확인한 형제 요소 수: %s", len(siblings))
            return 0

        except Exception as e:
            logger.info(f"가져오기MSAA요소검색시출력오류: {str(e)}")
            return 0


class MSAAValidator:
    """MSAA요소검증기기"""

    @staticmethod
    def _get_msaa_ele_from_hwnd(hwnd, dwObjectID=0):
        """
        통신경과창가져오기MSAA객체

        Args:
            hwnd: 창
            dwObjectID: 객체식별자기호, 로OBJID_CLIENT(0)
        """
        # 지정일반사용의객체ID
        OBJID_CLIENT = 0
        OBJID_WINDOW = -1

        # 결과가지정완료지정의객체ID, 시도해당ID
        if dwObjectID != 0:
            try:
                IAccessible = ctypes.POINTER(comtypes.gen.Accessibility.IAccessible)()
                ctypes.oledll.oleacc.AccessibleObjectFromWindow(
                    hwnd,
                    dwObjectID,
                    ctypes.byref(comtypes.gen.Accessibility.IAccessible._iid_),
                    ctypes.byref(IAccessible),
                )
                logger.info(f"성공통신경과객체ID {dwObjectID} 가져오기MSAA객체")
                return IAccessible
            except Exception as e:
                logger.info(f"통신경과객체ID {dwObjectID} 가져오기MSAA객체실패: {e}")

        # 시도아니요의객체ID
        object_ids = [OBJID_CLIENT, OBJID_WINDOW]

        for obj_id in object_ids:
            try:
                IAccessible = ctypes.POINTER(comtypes.gen.Accessibility.IAccessible)()
                ctypes.oledll.oleacc.AccessibleObjectFromWindow(
                    hwnd,
                    obj_id,
                    ctypes.byref(comtypes.gen.Accessibility.IAccessible._iid_),
                    ctypes.byref(IAccessible),
                )
                logger.info(f"성공통신경과객체ID {obj_id} 가져오기MSAA객체")
                return IAccessible
            except Exception as e:
                logger.info(f"통신경과객체ID {obj_id} 가져오기MSAA객체실패: {e}")
                continue

        logger.info("불가통신경과작업객체ID가져오기MSAA객체")
        return None

    @staticmethod
    def _find_matches_in_parent(parent_element, target_desc, use_recursive=False):
        """에서요소중조회매칭목록 설명의요소"""
        matches = []

        try:
            target_name = target_desc.get("name")
            target_value = target_desc.get("value")
            target_index = target_desc.get("index", 0)
            target_tag = target_desc.get("tag_name")

            logger.info(
                f"정상에서조회: tag_name={target_tag}, name={target_name}, value={target_value}, index={target_index}, use_recursive={use_recursive}"
            )

            # 가져오기모든요소
            children = parent_element.get_children()
            logger.info(f"요소있음 {len(children)} 개요소")

            for i, c in enumerate(children):
                child_type = c.get_type()
                child_name = c.get_name()
                child_value = c.get_value()
                logger.info(f'요소[{i}]: type={child_type}, name="{child_name}", value="{child_value}"')

            # 선택매칭유형의요소
            candidates = [c for c in children if c.get_type() == target_tag]
            logger.info(f"매칭유형 {target_tag} 의선택원데이터량: {len(candidates)}")

            # 결과가있음직선연결매칭의요소, 허용검색, 이면시도검색
            if not candidates and use_recursive:
                logger.info("있음까지직선연결매칭의요소, 시도검색...")
                for child in children:
                    recursive_matches = MSAAValidator._find_matches_in_parent(child, target_desc, use_recursive=True)
                    if recursive_matches:
                        logger.info("검색까지매칭요소")
                        return recursive_matches

            # 일필터링매칭이름및값의요소
            filtered_candidates = []
            for candidate in candidates:
                name_match = not target_name or candidate.get_name() == target_name
                value_match = not target_value or candidate.get_value() == target_value
                index_match = not target_index or candidate.get_index() == target_index

                logger.info(f"선택요소매칭조회: name_match={name_match}, value_match={value_match}")

                if name_match and value_match and index_match:
                    filtered_candidates.append(candidate)

            logger.info(f"종료필터링후의선택원데이터량: {len(filtered_candidates)}")

            # 근거검색선택요소
            if filtered_candidates:
                matches.append(filtered_candidates[0])
                logger.info("선택일개요소")

        except Exception as e:
            logger.info(f"에서요소중조회매칭시출력오류: {str(e)}")

        return matches

    @staticmethod
    def find_element_by_msaa_path(path_info, start_element):
        """
        근거경로정보조회요소
        반환 (까지의원목록, 오류정보)
        """
        # logger.info(
        #     f'msaa 열기 의원정보 {start_element.get_type()} {start_element.get_name()}  {start_element.get_value()}')

        try:
            if not path_info:
                return [], "경로정보비어 있음"

            current_elements = [start_element]
            path_start_index = 0

            # 단계조회요소
            for depth in range(path_start_index, len(path_info)):
                target_desc = path_info[depth]
                if not current_elements:
                    return [], f"에서{depth}단계조회실패"

                next_elements = []

                # 에서일조회시사용검색
                use_recursive = depth == 0
                logger.info(f"{depth + 1}단계조회, use_recursive={use_recursive}")

                for parent_elem in current_elements:
                    matches = MSAAValidator._find_matches_in_parent(
                        parent_elem, target_desc, use_recursive=use_recursive
                    )
                    next_elements.extend(matches)

                if not next_elements:
                    return (
                        [],
                        f"{depth + 1}단계에서 매칭 요소를 찾지 못했습니다: {target_desc['tag_name']} '{target_desc.get('name', '')}'",
                    )

                current_elements = next_elements
                logger.info(f"{depth + 1}단계조회완료, 까지 {len(current_elements)} 개매칭요소")

            return current_elements

        except Exception as e:
            logger.info(f"경로조회예외: {str(e)}")
            return []

    @staticmethod
    def find_element_by_uia_path(ele, picker_type):
        """근거uia원경로정보조회사용, 반환후일개요소의정보"""
        hwnd = 0
        try:
            uia_ele = uia_factory.__find_partial_match__(ele, picker_type)
            hwnd = uia_ele.control().NativeWindowHandle
        except Exception as e:
            logger.info(f"에서요소중조회매칭시출력오류: {str(e)}")
        return hwnd

    @staticmethod
    def validate(ele: dict, picker_type: str):
        """
        통신경과경로정보검증요소
        반환 (여부있음, 메시지, 까지의원목록)
        """
        try:
            path = ele["path"]
            logger.info(f"경로: {path}")

            uia_path = []
            msaa_path = []
            last_control_index = -1

            for i, item in enumerate(path):
                tag_name = item.get("tag_name", "")
                # logger.info(f'경로 [{i}]: {item}')
                if tag_name.endswith("Control"):
                    last_control_index = i

            logger.info(f"후일개Control검색: {last_control_index}")

            if last_control_index == -1:
                logger.info("있음까지Control요소, 사용UIA경로")
                return uia_factory.__find_one__(ele, picker_type)

            uia_path = path[: last_control_index + 1]
            msaa_path = path[last_control_index + 1 :]

            logger.info(f"UIA경로: {uia_path}")
            logger.info(f"MSAA경로: {msaa_path}")

            hwnd = MSAAValidator.find_element_by_uia_path(ele, picker_type)
            logger.info(f"가져오기까지의창: {hwnd}")

            if hwnd == 0:
                logger.info("불가가져오기있음의창")
                return False, "불가가져오기있음의창", []

            ia_start_ele = MSAAValidator._get_msaa_ele_from_hwnd(hwnd)
            logger.info(f"가져오기까지의IAccessible객체: {ia_start_ele}")

            if ia_start_ele is None:
                logger.info("불가가져오기IAccessible객체")
                return False, "불가가져오기IAccessible객체", []

            msaa_start_ele = MSAAElement(ia_start_ele, 0)
            logger.info(f'생성의MSAA요소: name="{msaa_start_ele.get_name()}", type="{msaa_start_ele.get_type()}"')

            elements = MSAAValidator.find_element_by_msaa_path(msaa_path, msaa_start_ele)
            logger.info(f"MSAA경로조회결과: {elements}")

            if elements and len(elements) > 0:
                logger.info("성공까지MSAA요소")
                return MSAALocator(elements[0])
            else:
                logger.info("MSAA 요소를 찾을 수 없습니다")
                return False, "MSAA 요소를 찾을 수 없습니다", []

        except Exception as e:
            logger.info(f"MSAA경로검증예외: {str(e)}")
            return False, f"경로검증예외: {str(e)}", []


class MSAAFactory:
    """MSAA"""

    @classmethod
    def find(cls, ele: dict, picker_type: str, **kwargs) -> Union[MSAALocator, None]:
        return MSAAValidator.validate(ele, picker_type)


msaa_factory = MSAAFactory()
