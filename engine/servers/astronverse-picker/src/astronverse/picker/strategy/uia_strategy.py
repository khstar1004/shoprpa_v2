from astronverse.picker import IElement
from astronverse.picker.engines.uia_picker import UIAElement, uia_picker
from astronverse.picker.strategy.types import StrategySvc


def uia_default_strategy(strategy_svc: StrategySvc) -> IElement:
    """
    
    strategy_svc 위아래문서
    """

    ele = uia_picker.get_element(
        root=UIAElement(control=strategy_svc.start_control),
        point=strategy_svc.last_point,
        # 아래예매칭
        used_cache=False,
        root_need_init=True,
        ignore_parent_zero=True,
    )
    return ele