import traceback
from _ctypes import COMError
from typing import TYPE_CHECKING, Optional

from astronverse.picker import APP, IElement
from astronverse.picker.engines.uia_picker import UIAOperate
from astronverse.picker.logger import logger

if TYPE_CHECKING:
    from astronverse.picker.strategy.types import Strategy, StrategySvc
    from astronverse.picker.svc import ServiceContext


def auto_default_strategy_web(
    service: "ServiceContext", strategy: "Strategy", strategy_svc: "StrategySvc"
) -> Optional[IElement]:
    """선택"""

    # 지연가져오기 데이터
    from astronverse.picker.strategy.web_strategy import web_default_strategy

    try:
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
    return None