from dataclasses import dataclass
from typing import Union

from astronverse.browser_bridge.config import Config


@dataclass
class ServiceContext:
    config: Union[type[Config], None]

    def set_config(self, conf: type[Config]):
        self.config = conf


def gen_svc() -> ServiceContext:
    """
    완료svc
    """
    # 공유서비스의 예데이터베이스연결, redis연결, 필요예로완료보관인증단일, 단일
    return ServiceContext(
        config=None,
    )


def get_svc() -> ServiceContext:
    """
    가져오기전체영역svc
    """
    return _svc


_svc = gen_svc()