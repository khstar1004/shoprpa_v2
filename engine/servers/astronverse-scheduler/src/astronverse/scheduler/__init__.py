from enum import Enum


class ComponentType(Enum):
    """
    컴포넌트유형
    """

    # 경로 
    ROUTE = 0
    # 선택
    PICKER = 1
    # 실행기기
    EXECUTOR = 2
    # 브라우저통신중파일
    BROWSER_CONNECTOR = 3
    # 스케줄링기기
    SCHEDULER = 4
    # CV선택
    CV_PICKER = 5
    # 트리거기기
    TRIGGER = 6


class ServerLevel(Enum):
    CORE = 1  #  예에서시작전시작
    NORMAL = 2  # 통신 가능으로에서시작후시작