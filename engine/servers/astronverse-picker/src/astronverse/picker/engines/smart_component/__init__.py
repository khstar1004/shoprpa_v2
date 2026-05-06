#!/usr/bin/env python
"""
의 MSAA 선택및검증모듈
합치기기존있음목록중의 MSAA 닫기공가능, 가능실행, 아니오목록중의파일
"""

import ctypes
import ctypes.wintypes
import traceback

import comtypes
import comtypes.automation
import comtypes.client
import uiautomation as auto
from astronverse.picker import IElement, PickerDomain, Point, Rect
from astronverse.picker.engines.uia_picker import UIAOperate
from astronverse.picker.logger import logger
from astronverse.picker.utils.cv import screenshot
from astronverse.picker.utils.process import get_process_name
from pywin.mfc.object import Object

# 로드 MSAA 닫기의 COM 유형라이브러리
try:
    comtypes.client.GetModule("oleacc.dll")
except Exception as e:
    logger.info(f"msaa로드예외 {e}")

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

# 가능유형
WIN32_CONTROL_TYPE = {
    "ListItem": "목록 ",
    "List": "목록",
    "Button": "버튼",
    "Text": "텍스트",
    "ToolBar": "도구",
    "MenuItem": "메뉴",
    "Window": "창",
    "PushButton": "버튼",
    "EditableText": "가능텍스트",
    "CheckBox": "복사선택",
    "RadioButton": "단일선택버튼",
    "ComboBox": "그룹합치기",
    "DropDown": "드롭다운",
    "ProgressBar": "정도",
    "Slider": "",
    "SpinBox": "숫자조정기기",
    "Dialog": "대화상자",
    "Pane": "",
    "Client": "사용자",
    "Application": "사용프로그램",
    "Document": "문서",
}


class MSAAElement(IElement):
    """
    MSAA 요소설치유형
    기존있음의 Element 유형,  MSAA 요소의본공가능
    """

    def __init__(self, iaElement=None, pid=None):
        self.__rect = None  # cache rect
        self.__tag = None
        self.ia_ele = iaElement
        self.pid = pid

    def tag(self) -> str:
        if self.__tag is None:
            tag = self.ia_ele.accRoleName()
            if WIN32_CONTROL_TYPE.get(tag):
                tag = WIN32_CONTROL_TYPE.get(tag)
            self.__tag = tag
        return self.__tag

    def path(self, svc=None, strategy_svc=None):
        self_img = screenshot(self.__rect) if self.__rect else None
        path_list = MSAAPickerUtil.get_element_path(self.ia_ele)
        res = {
            "version": "1",
            "type": PickerDomain.MSAA.value,
            "app": get_process_name(self.pid),
            "path": path_list,
            "img": {"self": self_img},
        }
        return res

    def rect(self) -> Rect:
        self.__rect = MSAAPickerUtil.get_rect(self.ia_ele)
        return self.__rect


class IAElement(Object):
    """
    MSAA 요소설치유형
    기존있음의 Element 유형,  MSAA 요소의본공가능
    """

    def __init__(self, IAccessible, iObjectId):
        if not isinstance(iObjectId, int):
            error_msg = "MSAAElement(IAccessible,iObjectId) second argument type must be int"
            raise TypeError(error_msg)
        self.IAccessible = IAccessible
        self.iObjectId = iObjectId
        self.dictCache = {}
        self.__rect = None  # cache rect
        self.__tag = None

    def accChildCount(self):
        """가져오기 원수"""
        if self.iObjectId == 0:
            return self.IAccessible.accChildCount
        else:
            return 0

    def accRole(self):
        """가져오기요소역할"""
        objChildId = comtypes.automation.VARIANT()
        objChildId.vt = comtypes.automation.VT_I4
        objChildId.value = self.iObjectId
        objRole = comtypes.automation.VARIANT()
        objRole.vt = comtypes.automation.VT_BSTR
        self.IAccessible._IAccessible__com__get_accRole(objChildId, objRole)
        return objRole.value

    def accName(self, objValue=None):
        """가져오기원이름"""
        objChildId = comtypes.automation.VARIANT()
        objChildId.vt = comtypes.automation.VT_I4
        objChildId.value = self.iObjectId
        if objValue is None:
            objName = comtypes.automation.BSTR()
            self.IAccessible._IAccessible__com__get_accName(objChildId, ctypes.byref(objName))
            return objName.value
        else:
            self.IAccessible._IAccessible__com__set_accName(objChildId, objValue)

    def accLocation(self):
        """가져오기요소위치, 반환 (left, top, width, height)"""
        objChildId = comtypes.automation.VARIANT()
        objChildId.vt = comtypes.automation.VT_I4
        objChildId.value = self.iObjectId
        objL, objT = ctypes.c_long(), ctypes.c_long()
        objW, objH = ctypes.c_long(), ctypes.c_long()
        self.IAccessible._IAccessible__com_accLocation(
            ctypes.byref(objL),
            ctypes.byref(objT),
            ctypes.byref(objW),
            ctypes.byref(objH),
            objChildId,
        )
        return (objL.value, objT.value, objW.value, objH.value)

    def accValue(self, objValue=None):
        """가져오기요소값"""
        objChildId = comtypes.automation.VARIANT()
        objChildId.vt = comtypes.automation.VT_I4
        objChildId.value = self.iObjectId
        objBSTRValue = comtypes.automation.BSTR()
        if objValue is None:
            self.IAccessible._IAccessible__com__get_accValue(objChildId, ctypes.byref(objBSTRValue))
            return objBSTRValue.value
        else:
            objBSTRValue.value = objValue
            self.IAccessible._IAccessible__com__set_accValue(objChildId, objValue)
            return objBSTRValue.value

    def accDefaultAction(self):
        """가져오기요소이름"""
        objChildId = comtypes.automation.VARIANT()
        objChildId.vt = comtypes.automation.VT_I4
        objChildId.value = self.iObjectId
        objDefaultAction = comtypes.automation.BSTR()
        self.IAccessible._IAccessible__com__get_accDefaultAction(objChildId, ctypes.byref(objDefaultAction))
        return objDefaultAction.value

    def accDescription(self):
        """가져오기요소설명"""
        objChildId = comtypes.automation.VARIANT()
        objChildId.vt = comtypes.automation.VT_I4
        objChildId.value = self.iObjectId
        objDescription = comtypes.automation.BSTR()
        self.IAccessible._IAccessible__com__get_accDescription(objChildId, ctypes.byref(objDescription))
        return objDescription.value

    def accState(self):
        """가져오기요소상태"""
        objChildId = comtypes.automation.VARIANT()
        objChildId.vt = comtypes.automation.VT_I4
        objChildId.value = self.iObjectId
        objState = comtypes.automation.VARIANT()
        self.IAccessible._IAccessible__com__get_accState(objChildId, ctypes.byref(objState))
        return objState.value

    def accParent(self):
        """가져오기 요소"""
        objParent = self.IAccessible.accParent
        if objParent is not None:
            return IAElement(objParent, 0)

    def accDoDefaultAction(self):
        """실행요소"""
        objChildId = comtypes.automation.VARIANT()
        objChildId.vt = comtypes.automation.VT_I4
        objChildId.value = self.iObjectId
        self.IAccessible._IAccessible__com_accDoDefaultAction(objChildId)

    def accRoleName(self):
        """가져오기역할이름"""
        try:
            iRole = self.accRole()
            return ACC_ROLE_NAME_MAP.get(iRole)
        except:
            return None

    def accHwnd(self):
        """가져오기요소"""
        hwnd = ctypes.c_long()
        ctypes.oledll.oleacc.WindowFromAccessibleObject(self.IAccessible, ctypes.byref(hwnd))
        return hwnd.value

    def __iter__(self):
        """모든요소"""
        if self.iObjectId > 0:
            return
        objAccChildArray = (comtypes.automation.VARIANT * self.IAccessible.accChildCount)()
        objAccChildCount = ctypes.c_long()
        ctypes.oledll.oleacc.AccessibleChildren(
            self.IAccessible,
            0,
            self.IAccessible.accChildCount,
            objAccChildArray,
            ctypes.byref(objAccChildCount),
        )
        for i in range(objAccChildCount.value):
            objAccChild = objAccChildArray[i]
            if objAccChild.vt == comtypes.automation.VT_DISPATCH:
                accessible_obj = objAccChild.value.QueryInterface(comtypes.gen.Accessibility.IAccessible)
                yield IAElement(accessible_obj, 0)
            else:
                yield IAElement(self.IAccessible, objAccChild.value)

    def __str__(self):
        """형식원정보"""
        iRole = self.accRole()
        role_name = ACC_ROLE_NAME_MAP.get(iRole, "Unkown")
        child_count = self.IAccessible.accChildCount
        return "[%s(0x%X)|%r|ChildCount:%d]" % (
            role_name,
            iRole,
            self.accName(),
            child_count,
        )

    def match_by_rect(self, x, y):
        """근거매칭요소"""
        bMatched = True
        try:
            rect = self.accLocation()
            if rect[2] <= 0 or rect[3] <= 0:
                bMatched = False
            if (rect[0] > x) or ((rect[0] + rect[2]) < x) or (rect[1] > y) or ((rect[1] + rect[3]) < y):
                bMatched = False
        except Exception as ex:
            logger.info(f"통신경과match_by_rect매칭출력오류{ex}")
            bMatched = False
        return bMatched

    def finditer_by_rect(self, x, y):
        """근거조회요소"""
        parentElement = self
        location = (0, 0, 0, 0)
        while parentElement.accLocation() != location:
            location = parentElement.accLocation()
            for child in list(parentElement):
                if child.match_by_rect(x, y):
                    parentElement = child
                    break
        return parentElement


class MSAAPickerUtil:
    """
    MSAA 선택기기유형
     MSAA 요소의선택및검증공가능
    """

    @staticmethod
    def point(x, y):
        """근거가져오기 MSAA 요소"""
        objPoint = ctypes.wintypes.POINT()
        objPoint.x = x
        objPoint.y = y
        IAccessible = ctypes.POINTER(comtypes.gen.Accessibility.IAccessible)()
        objChildId = comtypes.automation.VARIANT()
        ctypes.oledll.oleacc.AccessibleObjectFromPoint(objPoint, ctypes.byref(IAccessible), ctypes.byref(objChildId))
        return IAElement(IAccessible, objChildId.value or 0)

    @staticmethod
    def window(objHandle):
        """근거창가져오기 MSAA 요소"""
        if objHandle in (0, None):
            objElement = MSAAPickerUtil.window(ctypes.windll.user32.GetDesktopWindow())
        elif isinstance(objHandle, str):
            objHandle = str(objHandle)
            iHwnd = ctypes.windll.user32.FindWindowW(objHandle, None) or ctypes.windll.user32.FindWindowW(
                None, objHandle
            )
            assert iHwnd > 0, "Cannot FindWindow %r" % objHandle
            objElement = MSAAPickerUtil.window(iHwnd)
        elif isinstance(objHandle, int):
            iHwnd = objHandle
            IAccessible = ctypes.POINTER(comtypes.gen.Accessibility.IAccessible)()
            ctypes.oledll.oleacc.AccessibleObjectFromWindow(
                iHwnd,
                0,
                ctypes.byref(comtypes.gen.Accessibility.IAccessible._iid_),
                ctypes.byref(IAccessible),
            )
            objElement = IAElement(IAccessible, 0)
        else:
            raise TypeError("window argument objHandle must be a int/str/unicode, not %r" % objHandle)
        return objElement

    @staticmethod
    def get_name(ctrl):
        """가져오기 파일이름"""
        try:
            acc_name = ctrl.accName()
            if not acc_name:
                return ""
            return acc_name
        except:
            return ""

    @staticmethod
    def get_type(ctrl):
        """가져오기 파일유형"""
        role_name = ctrl.accRoleName()
        if not role_name:
            role_name = "MSAA"
        return role_name

    @staticmethod
    def get_value(ctrl):
        """가져오기 파일값"""
        try:
            acc_value = ctrl.accValue()
            if not acc_value:
                return ""
            return acc_value
        except:
            return ""

    @staticmethod
    def get_rect(ctrl):
        """가져오기 파일"""
        try:
            bound = ctrl.accLocation()
        except:
            bound = [0, 0, 0, 0]
        right = bound[0] + bound[2]
        bottom = bound[1] + bound[3]
        rect = Rect(bound[0], bound[1], right, bottom)
        return rect

    @staticmethod
    def get_hwnd(ctrl):
        """가져오기 파일"""
        return ctrl.accHwnd()

    @staticmethod
    def get_parent(ctrl):
        """가져오기 파일"""
        return ctrl.accParent()

    @staticmethod
    def has_children(ctrl):
        """여부있음파일"""
        children_len = ctrl.accChildCount()
        return children_len > 0

    @staticmethod
    def get_children(ctrl):
        """가져오기 파일"""
        for child in list(ctrl):
            yield child

    @staticmethod
    def get_control_by_point(x, y):
        """근거가져오기 파일"""
        try:
            # 시도직선연결가져오기클릭위치의요소
            control = MSAAPickerUtil.point(x, y)
            # 통신경과의요소
            control = control.finditer_by_rect(x, y)
            return control
        except Exception as e:
            logger.info("가져오기 파일시출력오류: {}".format(str(e)))
            return None

    @staticmethod
    def get_msaa_element_index(element):
        """계획 MSAA 요소에서요소중의검색"""
        try:
            parent = element.accParent()
            if not parent:
                logger.info(f"디버그: 요소 {element.accName()} 있음요소")
                return 0

            # logger.info(f"디버그: 계획요소 {element.accName()} 의검색, 요소: {parent.accName()}")

            # 가져오기현재요소의정보사용
            current_name = element.accName() or ""
            current_role = element.accRole() if hasattr(element, "accRole") else None
            current_location = None
            try:
                current_location = element.accLocation()
            except:
                pass

            index = 0
            sibling_count = 0
            # 사용있음의방법법요소
            for sibling in parent:
                sibling_count += 1
                sibling_name = sibling.accName() or ""
                sibling_role = sibling.accRole() if hasattr(sibling, "accRole") else None
                sibling_location = None
                try:
                    sibling_location = sibling.accLocation()
                except:
                    pass

                # logger.info(f"디버그: 요소 {sibling_count}: {sibling_name}, 현재요소: {current_name} {current_location}")

                # 사용다중파일행
                is_match = False

                # 방법법1: 결과가위치정보가능사용, 사용위치()
                if current_location and sibling_location:
                    if current_location == sibling_location:
                        is_match = True
                        # logger.info(f"디버그: 통신경과위치매칭까지요소, 검색: {index}")

                # 방법법2: 결과가위치할 수 없음사용, 사용역할+이름+객체주소그룹합치기
                elif not is_match:
                    if (
                        current_name == sibling_name and current_role == sibling_role and current_name != ""
                    ):  # 빈이름의오류매칭
                        # 일인증: 조회여부예일개객체
                        if sibling.IAccessible == element.IAccessible and sibling.iObjectId == element.iObjectId:
                            is_match = True
                            # logger.info(f"디버그: 통신경과역할+이름+객체주소매칭까지요소, 검색: {index}")

                # 방법법3: 결과가이름비어 있습니다, 사용객체주소및ID
                elif not is_match and current_name == "":
                    if sibling.IAccessible == element.IAccessible and sibling.iObjectId == element.iObjectId:
                        is_match = True
                        # logger.info(f"디버그: 통신경과객체주소매칭까지요소, 검색: {index}")

                if is_match:
                    return index

                index += 1

            # logger.info(f"디버그: 찾을 수 없는 매칭요소, 공유조회완료 {sibling_count} 개요소")
            return 0
        except Exception as e:
            logger.info("가져오기MSAA요소검색시출력오류: {}".format(str(e)))
            logger.info("예외정보:")
            logger.info(traceback.format_exc())
            return 0

    @staticmethod
    def get_element_info(ctrl):
        """가져오기요소의정보"""
        if not ctrl:
            return None

        try:
            info = {
                "name": MSAAPickerUtil.get_name(ctrl),
                "type": MSAAPickerUtil.get_type(ctrl),
                "value": MSAAPickerUtil.get_value(ctrl),
                "index": MSAAPickerUtil.get_msaa_element_index(ctrl),
            }
            return info
        except Exception as e:
            logger.info("가져오기원정보시출력오류: {}".format(str(e)))
            logger.info("예외정보:")
            logger.info(traceback.format_exc())
            return None

    @staticmethod
    def get_element_path(ctrl):
        """가져오기요소의경로정보"""

        def index_of_control(control) -> int:
            index = 0
            pre = control.GetPreviousSiblingControl()
            while pre:
                index += 1
                pre = pre.GetPreviousSiblingControl()
            return index

        path = []
        current = ctrl
        ancestor = None
        try:
            while current:
                info = {
                    "name": MSAAPickerUtil.get_name(current),
                    "tag_name": MSAAPickerUtil.get_type(current),
                    "value": MSAAPickerUtil.get_value(current),
                    "index": MSAAPickerUtil.get_msaa_element_index(current),
                    "checked": True,
                }
                parent = MSAAPickerUtil.get_parent(current)
                if not parent or MSAAPickerUtil.get_type(current) == "Window":
                    ancestor = current
                    break
                if MSAAPickerUtil.get_type(current) == "Document" and MSAAPickerUtil.get_type(parent) == "Window":
                    ancestor = current
                    path.append(info)
                    break
                path.append(info)
                current = parent
                # 중지없음제한
                if len(path) > 40:
                    logger.info(path)
                    logger.info("현재선택정도초과경과20, 저장된 ")
                    break

        except Exception as e:
            logger.info("가져오기원경로시출력오류: {}".format(str(e)))

        # 이: 사용UIA창단계정보
        if ancestor:
            try:
                # 가져오기MSAA파일의
                bottom_hwnd = MSAAPickerUtil.get_hwnd(ancestor)
                if bottom_hwnd:
                    # 변환로UIA파일
                    uia_ele = auto.ControlFromHandle(bottom_hwnd)
                    if uia_ele:
                        # 사용UIA위가져오기창단계
                        uia_current = uia_ele
                        uia_path = []

                        while uia_current:
                            try:
                                value = uia_current.GetValuePattern().Value
                            except Exception:
                                value = None
                            uia_info = {
                                "cls": uia_current.ClassName,
                                "name": uia_current.Name,
                                "tag_name": uia_current.ControlTypeName,
                                "index": index_of_control(uia_current),
                                "value": value,
                                "checked": True,
                            }
                            uia_path.append(uia_info)
                            uia_parent = uia_current.GetParentControl()

                            # 조회여부까지또는
                            if not uia_parent:
                                break
                            # 조회parent여부까지단계
                            if UIAOperate._is_desktop_element(uia_parent):
                                break

                            uia_current = uia_parent

                            # 중지없음제한
                            if len(uia_path) > 20:
                                logger.info(uia_path)
                                logger.info("UIA창단계정도초과경과20, 저장된 ")
                                break

                        # 를UIA가져오기의창단계추가까지경로전
                        # logger.info(f'uia_path: {uia_path}')
                        # logger.info(f'path: {path}')
                        uia_path.reverse()
                        path.reverse()
                        path = uia_path + path

            except Exception as e:
                logger.info("사용UIA창단계시출력오류: {}".format(str(e)))
                logger.info("예외정보:")
                logger.info(traceback.format_exc())

        return path


class MSAAPicker:
    @classmethod
    def get_similar_path(cls, strategy_svc, curr_path):
        """사용자지정개요소"""
        raise Exception("msaa지원하지 않음요소")

    @classmethod
    def get_element(cls, point: Point, pid, **kwargs):
        # 근거선택요소
        element = MSAAPickerUtil.get_control_by_point(point.x, point.y)
        if element:
            return MSAAElement(iaElement=element, pid=pid)
        else:
            logger.info("msaa미완료선택 가능까지요소")