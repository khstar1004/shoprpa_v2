import traceback
from _ctypes import COMError
from typing import TYPE_CHECKING, Optional

from astronverse.picker import APP, MSAA_APPLICATIONS, IElement
from astronverse.picker.engines.uia_picker import UIAOperate
from astronverse.picker.logger import logger

if TYPE_CHECKING:
    from astronverse.picker.strategy.types import Strategy, StrategySvc
    from astronverse.picker.svc import ServiceContext


def auto_default_strategy(
    service: "ServiceContext", strategy: "Strategy", strategy_svc: "StrategySvc"
) -> Optional[IElement]:
    """선택"""

    # 지연가져오기 데이터
    from astronverse.picker.strategy.msaa_strategy import msaa_default_strategy
    from astronverse.picker.strategy.uia_strategy import uia_default_strategy
    from astronverse.picker.strategy.web_strategy import web_default_strategy

    try:
        from astronverse.picker.strategy.jab_strategy import jab_default_strategy
        from astronverse.picker.strategy.sap_default_strategy import sap_default_strategy
        from astronverse.picker.strategy.web_ie_strategy import web_ie_default_strategy
    except Exception as e:
        logger.info(f"선택모듈가져오기예외{e}")

    # 2. 가져오기가능의요소
    preliminary_element = None
    chrome_like_apps = [
        APP.Chrome,
        APP.Firefox,
        APP.Chrome360X,
        APP.Chrome360se,
        APP.Chrome360,
        APP.Edge,
        APP.IE,
        APP.Chromium,
    ]
    if strategy_svc.app in chrome_like_apps:
        # 1. 결과가예브라우저사용브라우저가져오기
        try:
            try:
                web_control_result = UIAOperate().get_web_control(
                    strategy_svc.start_control,
                    strategy_svc.app,
                    strategy_svc.last_point,
                )
                is_document, menu_top, menu_left, hwnd = web_control_result
            except Exception as e:
                logger.error("정보:\n{}".format(traceback.format_exc()))
                return None

            if is_document:
                if strategy_svc.app == APP.IE:
                    try:
                        preliminary_element = web_ie_default_strategy(
                            service, strategy, strategy_svc, (is_document, menu_top, menu_left, hwnd)
                        )
                    except Exception as e:
                        logger.error(f"auto_default_strategy web_ie_picker error: {e} {traceback.extract_stack()}")
                        preliminary_element = None
                else:
                    web_cache = (is_document, menu_top, menu_left, hwnd)
                    preliminary_element = web_default_strategy(service, strategy_svc, web_cache)
                # web요소직선연결반환, 아니오
                return preliminary_element
        except COMError as e:
            # 모든 COM 호출오류
            logger.warning(f" COMError: {e}")
            logger.debug("COMError 정보:\n{}".format(traceback.format_exc()))
            return None
        except Exception as e:
            logger.error("정보:\n%s", traceback.format_exc())
            logger.error(f"auto_default_strategy web error: {e} {traceback.extract_stack()}")
            raise e
    elif strategy_svc.app.value in MSAA_APPLICATIONS:
        preliminary_element = msaa_default_strategy(strategy_svc)
    elif strategy_svc.app == APP.SAP:
        # 2. 여부예sap
        try:
            preliminary_element = sap_default_strategy(service, strategy, strategy_svc)
        except Exception as e:
            logger.error(f"auto_default_strategy sap_picker error: {e} {traceback.extract_stack()}")
    else:
        # 3. 결과가아니오예브라우저사용JAB
        try:
            preliminary_element = jab_default_strategy(service, strategy, strategy_svc)
        except Exception as e:
            logger.error(f"auto_default_strategy jab_picker error: {e} {traceback.extract_stack()}")

    # 3. 사용uia
    uia_element = None
    try:
        uia_element = uia_default_strategy(strategy_svc)
    except Exception as e:
        logger.error("정보:\n%s", traceback.format_exc())
        logger.error(f"auto_default_strategy uia_picker error: {e} {traceback.extract_stack()}")

    # 4. 결과선택가져오기
    if uia_element is None and preliminary_element is not None:
        return preliminary_element
    if preliminary_element is None and uia_element is not None:
        return uia_element
    if uia_element is None and preliminary_element is None:
        return None
    # 사용소의, 결과가사용preliminary_element
    logger.info(
        "pk: uia %s preliminary %s",
        uia_element.rect().area(),
        preliminary_element.rect().area(),
    )
    if preliminary_element.rect().area() <= uia_element.rect().area():
        return preliminary_element
    else:
        return uia_element