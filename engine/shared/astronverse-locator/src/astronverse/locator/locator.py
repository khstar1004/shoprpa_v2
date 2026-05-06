"""
위치 지정기기관리관리모듈

시스템일의요소위치 지정관리관리공가능, 지원다중위치 지정기기유형.
"""

import json
import re
import traceback
from typing import Union

from astronverse.baseline.logger.logger import logger
from astronverse.locator import ILocator, PickerDomain


def uia_factory_callback():
    """가져오기UIA위치 지정기기의돌아가기조정데이터"""
    from astronverse.locator.core.uia_locator import (
        uia_factory,
    )

    return uia_factory.find


def web_factory_callback():
    """가져오기Web위치 지정기기의돌아가기조정데이터"""
    from astronverse.locator.core.web_locator import (
        web_factory,
    )

    return web_factory.find


def msaa_factory_callback():
    """가져오기MSAA위치 지정기기의돌아가기조정데이터"""
    from astronverse.locator.core.msaa_locator import (
        msaa_factory,
    )

    return msaa_factory.find


def web_ie_factory_callback():
    try:
        from astronverse.locator.core.web_ie_locator import web_ie_factory

        return web_ie_factory.find
    except Exception as e:
        logger.info(f" 가져오기ie모듈출력제목 {e}")
        from astronverse.locator.core.web_locator import (
            web_factory,
        )

        return web_factory.find


def jab_factory_callback():
    try:
        from astronverse.locator.core.jab_locator import jab_factory

        return jab_factory.find
    except Exception as e:
        logger.info(f" 가져오기jab모듈출력제목 {e}")
        from astronverse.locator.core.uia_locator import (
            uia_factory,
        )

        return uia_factory.find


def sap_factory_callback():
    try:
        from astronverse.locator.core.sap_locator import sap_factory

        return sap_factory.find
    except Exception as e:
        logger.info(f" 가져오기sap모듈출력제목 {e}")
        from astronverse.locator.core.uia_locator import (
            uia_factory,
        )

        return uia_factory.find


class LocatorManager:
    """관리관리기기"""

    def __init__(self):
        self.locator_handler = {
            PickerDomain.UIA.value: [uia_factory_callback],
            PickerDomain.WEB.value: [web_factory_callback, web_ie_factory_callback],
            PickerDomain.MSAA.value: [msaa_factory_callback],
            PickerDomain.JAB.value: [jab_factory_callback],
            PickerDomain.SAP.value: [sap_factory_callback],
        }

    @staticmethod
    def parse_element_json(element_string):
        """
        사용정상이면매칭출력의img이미지필터링

        Args:
            element_string: 원문자열

        Returns:
            파싱후의요소딕셔너리
        """
        try:
            img_match = re.search(r'(,"img".*})}$', element_string)
            if img_match:
                dictionary_string = element_string[0 : img_match.regs[1][0]] + "}"
                image_dictionary_string = img_match.group(1)[7:]
                dictionary_json = json.loads(dictionary_string)
                image_dictionary = json.loads(image_dictionary_string)
                dictionary_json["img"] = image_dictionary
                return dictionary_json
        except Exception:
            pass
        return json.loads(element_string)

    def locator(self, element: Union[str, dict], **kwargs) -> Union[list[ILocator], ILocator, None]:
        """
        위치 지정요소

        Args:
            element: 원정보, 가능으로예문자열또는딕셔너리
            **kwargs: 금액외부매개변수

        Returns:
            위치 지정기기객체또는위치 지정기기목록
        """
        # 가져오기element
        if isinstance(element, str):
            element = self.parse_element_json(element)

        # 외부모듈매칭
        # timeout = kwargs.get('timeout', 10)  시간 초과시간(초)
        # use_cache = kwargs.get('use_cache', 10) 여부사용저장
        # search_depth = kwargs.get('search_depth', 10) 검색정도
        # scroll_into_view = kwargs.get('scroll_into_view', 10) 여부까지가능

        # 요소공유매칭
        locator_type = element.get("type", PickerDomain.UIA.value)
        picker_type = element.get("picker_type", "")
        last_error = None
        for callback in self.locator_handler[locator_type]:
            try:
                callback_func = callback()
                if callback_func is None:
                    continue
                result = callback_func(ele=element, picker_type=picker_type, **kwargs)
                if result is not None:
                    return result
            except Exception as exception:
                last_error = exception
                logger.error(f"Strategy run error: {exception} {traceback.format_exc()}")
        if last_error:
            raise last_error
        return None


locator = LocatorManager()