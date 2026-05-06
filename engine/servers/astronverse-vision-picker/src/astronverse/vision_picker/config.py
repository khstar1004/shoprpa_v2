import logging


class Config:
    # 단말
    VISION_PICKER_PORT = None
    # 서비스시작단말열기 단말
    LOCAL_PORT_START = 32000
    # 본로그파일
    LOG_BASE_DIR = "logs"
    # log로그대기단계
    LOG_LEVEL = logging.DEBUG
    # 높이프로그램단말
    HIGHLIGHT_SOCKET_PORT = 11001
    REMOTE_ADDR = None