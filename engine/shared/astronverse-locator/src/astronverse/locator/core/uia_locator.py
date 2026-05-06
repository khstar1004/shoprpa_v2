import dataclasses
import time
from copy import deepcopy
from typing import Any, Optional, Union

import pyautogui
from astronverse.baseline.logger.logger import logger
from astronverse.locator import ILocator, PickerType, Rect
from astronverse.locator.utils.window import (
    find_window_by_enum_list,
    find_window_handles_list,
    is_desktop_by_handle,
    show_desktop_rect,
    top_window,
    validate_window_rect,
)
from uiautomation import Control, ControlFromHandle


class UIALocator(ILocator):
    def __init__(self, control: Control):
        self.__control = control
        self.__rect = None

    def rect(self) -> Optional[Rect]:
        if self.__rect is None:
            rect = self.__control.BoundingRectangle
            logger.info(f"검증결과의rect {rect.left} {rect.top} {rect.right} {rect.bottom}")
            is_valid_rect = validate_window_rect(rect.left, rect.top, rect.right, rect.bottom)
            # logger.info(f'UIALocator rect  {is_valid_rect}')
            if not is_valid_rect:
                rect.left = 1 if rect.left < 0 else rect.left
                rect.top = 1 if rect.top < 0 else rect.top
                rect.right = pyautogui.size().width - 1 if rect.right > pyautogui.size().width else rect.right
                rect.bottom = pyautogui.size().height - 1 if rect.bottom > pyautogui.size().height else rect.bottom
            self.__rect = Rect(rect.left, rect.top, rect.right, rect.bottom)
        logger.info(f"검증결과의rect {self.__rect.to_json()}")
        return self.__rect

    def control(self) -> Any:
        return self.__control


@dataclasses.dataclass
class UIANode:
    """개예프론트엔드PATH수정후의값, 필요및UIAEle"""

    tag_name: str = None  # 태그이름
    checked: bool = False  # 여부선택중
    disable_keys: list[str] = None  # 사용 안 함의key
    cls: str = None  # class name
    index: int = None  # 검색
    name: str = None
    value: str = None


class UIAEle:
    """개예UIA의값, 필요및프론트엔드PATH, 기록결과"""

    def __init__(self, control: Control, index: int = None, index_match_sort: str = ""):
        # 위예control, 및index계획출력의데이터
        self.__control = control
        self.__rect = None
        self.__index = index
        self.__cls = None
        self.__name = None
        self.__tag_name = None
        self.__value = None

        # : 개예UIANode의매칭데이터, index의매칭아니요예강함매칭
        self.index_parent_match_sort: str = ""
        self.index_match_sort: str = ""

    @property
    def rect(self):
        if self.__rect is None:
            bounding_rectangle = self.__control.BoundingRectangle
            self.__rect = Rect(
                bounding_rectangle.left,
                bounding_rectangle.top,
                bounding_rectangle.right,
                bounding_rectangle.bottom,
            )
        return self.__rect

    @property
    def tag_name(self):
        if self.__tag_name is None:
            self.__tag_name = self.__control.ControlTypeName
        return self.__tag_name

    @property
    def index(self):
        if self.__index is None:
            self.__index = 0
            pre = self.__control.GetPreviousSiblingControl()
            while pre:
                self.__index += 1
                pre = pre.GetPreviousSiblingControl()
        return self.__index

    @property
    def cls(self):
        if self.__cls is None:
            self.__cls = self.__control.ClassName
        return self.__cls

    @property
    def name(self):
        if self.__name is None:
            self.__name = self.__control.Name
        return self.__name

    @property
    def value(self):
        if self.__value is None:
            try:
                value = self.__control.GetValuePattern().Value
            except Exception:
                value = None
            self.__value = value
        return self.__value

    @property
    def control(self):
        return self.__control


class UIAFactory:
    """UIA"""

    @classmethod
    def find(cls, ele: dict, picker_type: str, **kwargs) -> Union[list[UIALocator], UIALocator, None]:
        if picker_type == PickerType.SIMILAR.value:
            return cls.__find_similar__(ele, picker_type, **kwargs)
        else:
            return cls.__find_one__(ele, picker_type, **kwargs)

    @classmethod
    def __get_child_walk_control__(cls, control: Control):
        child = control.GetFirstChildControl()
        index = 0
        while child:
            uia_ele = UIAEle(control=child, index=index)
            yield uia_ele
            index += 1
            child = child.GetNextSiblingControl()

    @classmethod
    def __compare_node_and_uia_ele__(cls, uia_ele: UIAEle, node: UIANode, keys: list[str]) -> bool:
        # 있음선택중
        if not node.checked:
            return True

        for key in keys:
            if key in node.disable_keys:
                continue
            v1 = getattr(node, key, None)
            v2 = getattr(uia_ele, key, None)
            if v1 is not None:
                v1 = str(v1)
            if v2 is not None:
                v2 = str(v2)
            if not v1 and not v2:
                continue
            if v1 != v2:
                return False
        return True

    @classmethod
    def __show_desktop_ele__(cls, root_handle, root_ctrl, rect):
        # 결과가예요소, 를의창소
        if not root_handle or not root_ctrl:
            return
        if is_desktop_by_handle(root_handle, root_ctrl):
            show_desktop_rect(rect, desktop_handle=root_handle)
            time.sleep(0.2)

    @classmethod
    def _format_node_info(cls, node_or_obj) -> str:
        """형식정보로단일행문자열"""
        attrs = []
        for key in ["tag_name", "name", "cls", "value"]:
            value = getattr(node_or_obj, key, None)
            if value:  # 있음값의속성
                attrs.append(f"{key}={value}")
        return ", ".join(attrs)

    @classmethod
    def __find_similar__(cls, ele: dict, picker_type: str, **kwarg) -> Union[list[UIALocator], None]:
        path_list = ele.get("path", [])
        if not path_list:
            return None

        # 1. 까지경로
        parent_path = [v for v in path_list if v.get("similar_parent", False)]
        parent_ele = deepcopy(ele)
        parent_ele["path"] = parent_path
        parent_locator = cls.__find_one__(parent_ele, picker_type=picker_type, **kwarg)
        if not parent_locator:
            raise Exception("요소불가까지")
        assert isinstance(parent_locator.control(), Control)

        # 2. 요소
        res = []
        node_list = [
            UIANode(
                tag_name=path.get("tag_name", None),
                checked=path.get("checked", None),
                disable_keys=path.get("disable_keys", []),
                cls=path.get("cls", None),
                index=path.get("index", None),
                name=path.get("name", None),
                value=path.get("value", None),
            )
            for path in path_list
            if not path.get("similar_parent", None)
        ]

        for root_ctrl in cls.__get_child_walk_control__(parent_locator.control()):
            # 일요소여부기호합치기
            root_ele = UIAEle(control=root_ctrl.control, index=0, index_match_sort="1")
            is_ok = cls.__compare_node_and_uia_ele__(root_ele, node_list[0], ["tag_name", "name", "cls", "value"])
            if not is_ok:
                continue

            if len(node_list) == 1:
                # 결과가있음일직선연결결과
                res.append(UIALocator(control=root_ctrl.control))
                continue
            else:
                # 결과가있음다중필요아래, 까지일개의값
                search_list = [UIAEle(control=root_ctrl.control, index=0, index_match_sort="1")]
                i = 0
                for i, node in enumerate(node_list[1:]):
                    # i 테이블

                    # 4.1 조회의
                    child_list = []
                    for search in search_list:
                        for uia_ele in cls.__get_child_walk_control__(search.control):
                            uia_ele.index_parent_match_sort = search.index_match_sort
                            child_list.append(uia_ele)

                    # 4.2 프론트엔드의node, 필터링아니요기호합치기필요의, 강함매칭
                    child_list = [
                        item
                        for item in child_list
                        if cls.__compare_node_and_uia_ele__(item, node, ["tag_name", "name", "cls", "value"])
                    ]

                    # 4.3 프론트엔드의node, 관리index, 약함매칭
                    for item in child_list:
                        index_match = cls.__compare_node_and_uia_ele__(item, node, ["index"])
                        item.index_match_sort = "{}{}".format(item.index_parent_match_sort, "1" if index_match else "0")

                    # 4.3 일아래, 직선까지있음사람작업기호합치기, 또는단계결과
                    search_list = child_list
                    if not search_list:
                        break

                if not search_list or i != (len(node_list) - 2):
                    continue
                search_list.sort(key=lambda s: -int(s.index_match_sort))
                match = search_list[0]
                res.append(UIALocator(control=match.control))
        return res

    @classmethod
    def __find_one__(cls, ele: dict, picker_type: str, **kwargs) -> Union[UIALocator, None]:
        """
        사용목록 의방식조회창, 까지요소시중지
        사용 find_window_by_enum_list 및 find_window_handles_list 가져오기 목록
        """
        app_name = ele.get("app", "")
        path_list = ele.get("path", [])
        if not path_list:
            return None

        # 1. 관리프론트엔드path
        node_list = [
            UIANode(
                tag_name=path.get("tag_name", None),
                checked=path.get("checked", None),
                disable_keys=path.get("disable_keys", []),
                cls=path.get("cls", None),
                index=path.get("index", None),
                name=path.get("name", None),
                value=path.get("value", None),
            )
            for path in path_list
        ]

        first_cls = node_list[0].cls if "cls" not in node_list[0].disable_keys else None
        first_name = node_list[0].name if "name" not in node_list[0].disable_keys else None
        first_app_name = app_name if app_name not in node_list[0].disable_keys else None

        # 2. 가져오기모든가능의창
        root_handles = []

        # 시도사용 find_window_handles_list 가져오기 목록
        try:
            handles_list = find_window_handles_list(
                first_cls, first_name, app_name=first_app_name, picker_type=picker_type
            )
            if handles_list:
                root_handles.extend(handles_list)
        except Exception as e:
            logger.debug(f"find_window_handles_list 호출실패: {e}")
        if len(root_handles) == 0:
            # 시도사용 find_window_by_enum_list 가져오기 목록
            try:
                enum_handles = find_window_by_enum_list(
                    first_cls,
                    first_name,
                    app_name=first_app_name,
                    picker_type=picker_type,
                )
                if enum_handles:
                    root_handles.extend(enum_handles)
            except Exception as e:
                logger.debug(f"find_window_by_enum_list 호출실패: {e}")

        # 재관리
        root_handles = list(set(root_handles))

        if not root_handles:
            raise Exception("요소불가까지")

        logger.info(f"까지 {len(root_handles)} 개창, 열기 조회")

        # 3. 모든, 시도까지요소
        for idx, root_handle in enumerate(root_handles):
            try:
                logger.debug(f"정상에서시도 {idx + 1} 개: {root_handle}")
                root_ctrl = ControlFromHandle(handle=root_handle)
                top_window(handle=root_handle, ctrl=root_ctrl)  # 창

                # 4. 결과가서비스유형 WINDOW, 직선연결결과
                if picker_type == PickerType.WINDOW.value:
                    logger.info(f"까지WINDOW유형요소, 사용: {root_handle}")
                    return UIALocator(control=root_ctrl)

                # 5. index의일일조회
                search_list = [UIAEle(control=root_ctrl, index=0, index_match_sort="1")]
                i = 0
                element_found = True  # 여부까지요소

                for i, node in enumerate(node_list[1:]):
                    # 5.1 조회의
                    child_list = []
                    tag_list = []
                    for search in search_list:
                        for uia_ele in cls.__get_child_walk_control__(search.control):
                            uia_ele.index_parent_match_sort = search.index_match_sort
                            child_list.append(uia_ele)
                            tag_list.append(uia_ele.tag_name)

                    # logger.debug(f"선택: {cls._format_node_info(node)}")
                    # for idx_child, ni in enumerate(child_list):
                    #     logger.debug(f"  {idx_child}: {cls._format_node_info(ni)}")

                    # 5.2 프론트엔드의node, 필터링아니요기호합치기필요의, 강함매칭
                    befor_cmp_child = child_list
                    child_list = [
                        item
                        for item in child_list
                        if cls.__compare_node_and_uia_ele__(item, node, ["tag_name", "name", "cls", "value"])
                    ]
                    # if len(child_list) > 0:
                    #     logger.info(f'선택예{child_list[0].tag_name}')

                    # 5.3 프론트엔드의node, 관리index, 약함매칭
                    for item in child_list:
                        index_match = cls.__compare_node_and_uia_ele__(item, node, ["index"])
                        item.index_match_sort = "{}{}".format(item.index_parent_match_sort, "1" if index_match else "0")

                    # 5.4 일아래, 직선까지있음까지작업기호합치기, 또는단계결과
                    search_list = child_list
                    if not search_list:
                        logger.debug(f"선택후child_list비어 있음 현재단계예{i} 선택taglist예 {tag_list}")
                        logger.debug(f"선택전선택({len(befor_cmp_child)}개):")
                        for idx_child, ni in enumerate(befor_cmp_child):
                            logger.debug(f"  {idx_child}: {cls._format_node_info(ni)}")
                        logger.debug(f"선택: {cls._format_node_info(node)}")
                        element_found = False
                        break

                # 6. 조회여부성공까지요소
                if element_found and search_list and i == (len(node_list) - 2):
                    # 7. 관리index
                    search_list.sort(key=lambda s: -int(s.index_match_sort))
                    match = search_list[0]

                    # 8. 후관리
                    # 요소, 의
                    cls.__show_desktop_ele__(root_handle, root_ctrl, match.rect)
                    res = UIALocator(control=match.control)
                    logger.info(f"성공까지요소, 사용: {root_handle}, 검증결과의rect {res.rect().to_json()}")
                    return res
                else:
                    logger.debug(f" {root_handle} 찾을 수 없는 매칭요소, 계속시도아래일개")

            except Exception as e:
                # 결과가현재관리실패, 계속시도아래일개
                logger.debug(f"관리 {root_handle} 시출력오류: {e}")
                continue

        # 결과가모든불가까지요소, 출력예외
        logger.error(f"완료 {len(root_handles)} 개, 찾을 수 없는 매칭요소")
        raise Exception("요소불가까지")

    @classmethod
    def __find_partial_match__(cls, ele: dict, picker_type: str, **kwargs) -> Union[UIALocator, None]:
        """
        근거경로조회요소, 결과가경로 있음전체매칭, 반환후매칭의요소아니요예오류
        """
        logger.info(f"UIAFactory __find_partial_match__ 열기 조회요소 {ele}")
        app_name = ele.get("app", "")
        path_list = ele.get("path", [])
        if not path_list:
            return None

        # 1. 관리프론트엔드path
        node_list = [
            UIANode(
                tag_name=path.get("tag_name", None),
                checked=path.get("checked", None),
                disable_keys=path.get("disable_keys", []),
                cls=path.get("cls", None),
                index=path.get("index", None),
                name=path.get("name", None),
                value=path.get("value", None),
            )
            for path in path_list
        ]

        first_cls = node_list[0].cls if node_list[0].cls not in node_list[0].disable_keys else None
        first_name = node_list[0].name if node_list[0].name not in node_list[0].disable_keys else None
        first_app_name = app_name if app_name not in node_list[0].disable_keys else None

        # 2. 가져오기모든가능의창
        root_handles = []

        # 시도사용 find_window_handles_list 가져오기 목록
        try:
            handles_list = find_window_handles_list(first_cls, first_name, app_name=first_app_name)
            if handles_list:
                root_handles.extend(handles_list)
        except Exception as e:
            logger.debug(f"find_window_handles_list 호출실패: {e}")
        if len(root_handles) == 0:
            # 시도사용 find_window_by_enum_list 가져오기 목록
            try:
                enum_handles = find_window_by_enum_list(first_cls, first_name, app_name=first_app_name)
                if enum_handles:
                    root_handles.extend(enum_handles)
            except Exception as e:
                logger.debug(f"find_window_by_enum_list 호출실패: {e}")

        # 재관리
        root_handles = list(set(root_handles))

        if not root_handles:
            raise Exception("요소불가까지")

        logger.info(f"까지 {len(root_handles)} 개창, 열기 조회")

        # 3. 모든, 시도까지요소
        best_match = None
        best_match_depth = -1

        for idx, root_handle in enumerate(root_handles):
            try:
                logger.debug(f"정상에서시도 {idx + 1} 개: {root_handle}")
                root_ctrl = ControlFromHandle(handle=root_handle)
                top_window(handle=root_handle, ctrl=root_ctrl)  # 창

                # 5. index의일일조회
                search_list = [UIAEle(control=root_ctrl, index=0, index_match_sort="1")]
                # 요소완료매칭완료일개, 으로정도로1
                current_depth = 1
                last_valid_match = search_list[0]  # 요소로매칭

                for i, node in enumerate(node_list[1:]):
                    # 5.1 조회의
                    child_list = []
                    tag_list = []
                    for search in search_list:
                        for uia_ele in cls.__get_child_walk_control__(search.control):
                            uia_ele.index_parent_match_sort = search.index_match_sort
                            child_list.append(uia_ele)
                            tag_list.append(uia_ele.tag_name)

                    # 5.2 프론트엔드의node, 필터링아니요기호합치기필요의, 강함매칭
                    befor_cmp_child = child_list
                    child_list = [
                        item
                        for item in child_list
                        if cls.__compare_node_and_uia_ele__(item, node, ["tag_name", "name", "cls", "value"])
                    ]

                    # 5.3 프론트엔드의node, 관리index, 약함매칭
                    for item in child_list:
                        index_match = cls.__compare_node_and_uia_ele__(item, node, ["index"])
                        item.index_match_sort = "{}{}".format(item.index_parent_match_sort, "1" if index_match else "0")

                    # 5.4 결과가까지완료매칭의요소, 업데이트search_list및현재매칭정도
                    if child_list:
                        search_list = child_list
                        current_depth = i + 2  # i예에서0열기 의, 추가위요소의1, 으로예i+2
                        # 저장현재단계의매칭
                        search_list.sort(key=lambda s: -int(s.index_match_sort))
                        last_valid_match = search_list[0]
                    else:
                        # 현재단계있음매칭, 중지검색
                        logger.debug(f"선택후child_list비어 있음 현재단계예{i} 선택taglist예 {tag_list}")
                        logger.debug(f"선택전선택({len(befor_cmp_child)}개):")
                        for idx_child, ni in enumerate(befor_cmp_child):
                            logger.debug(f"  {idx_child}: {cls._format_node_info(ni)}")
                        logger.debug(f"선택: {cls._format_node_info(node)}")
                        break

                # 6. 여부까지완료변경의매칭
                if current_depth > best_match_depth:
                    best_match_depth = current_depth
                    if current_depth == len(node_list):
                        # 전체매칭, 직선연결반환
                        cls.__show_desktop_ele__(root_handle, root_ctrl, last_valid_match.rect)
                        res = UIALocator(control=last_valid_match.control)
                        logger.info(f"전체매칭성공, 사용: {root_handle}, 검증결과의rect {res.rect().to_json()}")
                        return res
                    else:
                        # 모듈분매칭, 저장매칭
                        best_match = (root_handle, root_ctrl, last_valid_match)
                        logger.debug(f" {root_handle} 모듈분매칭, 정도: {current_depth}")

            except Exception as e:
                # 결과가현재관리실패, 계속시도아래일개
                logger.debug(f"관리 {root_handle} 시출력오류: {e}")
                continue

        # 7. 반환매칭결과
        if best_match:
            root_handle, root_ctrl, match_ele = best_match
            cls.__show_desktop_ele__(root_handle, root_ctrl, match_ele.rect)
            res = UIALocator(control=match_ele.control)
            logger.info(
                f"모듈분매칭성공, 사용: {root_handle}, 매칭정도: {best_match_depth}, 검증결과의rect {res.rect().to_json()}"
            )
            return res
        else:
            logger.error(f"완료 {len(root_handles)} 개, 찾을 수 없는 작업매칭요소")
            raise Exception("요소불가까지")


uia_factory = UIAFactory()