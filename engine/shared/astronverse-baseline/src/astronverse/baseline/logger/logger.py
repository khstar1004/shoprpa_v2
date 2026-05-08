import os
import sys
import time

from loguru import logger as log


class Logger:
    """Logger"""

    def __init__(self):
        self.logger = log

    def init(self, name: str = ""):
        if not name:
            return
        self.logger.remove()
        log_path = os.path.join(os.getcwd(), "logs")
        os.makedirs(log_path, exist_ok=True)

        # Time-rotated log file
        log_path = os.path.abspath(os.path.join(log_path, "{}-{}.log".format(name, time.strftime("%Y-%m-%d"))))
        try:
            self.logger.add(
                log_path, rotation="50MB", retention="7 days", encoding="utf-8", enqueue=True, compression="zip"
            )
        except (OSError, PermissionError):
            try:
                self.logger.add(
                    log_path, rotation="50MB", retention="7 days", encoding="utf-8", enqueue=False, compression="zip"
                )
            except Exception:
                self.logger.add(sys.stderr, enqueue=False)

    def get_log(self):
        return self.logger


base_logger = Logger()
base_logger.init()
logger = base_logger.get_log()
