import subprocess
import sys
from astronverse.baseline.logger.logger import logger
from astronverse.browser import BROWSER_REGISTER_NAME
from astronverse.browser.error import *


class BrowserLauncher:
    """브라우저시작도구유형, 유형 webbrowser 의공가능, 아니오 webbrowser 모듈"""

    @staticmethod
    def open(path: str, url: str = "", open_args: str = "") -> bool:
        """브라우저 열기"""

        cmd_parts = [f'"{path}"']
        if open_args:
            cmd_parts.append(open_args)
        if url:
            cmd_parts.append(f'"{url}"')
        cmdline = " ".join(cmd_parts)

        logger.info(f"시작브라우저명령: {cmdline}")

        try:
            if sys.platform[:3] == "win":
                p = subprocess.Popen(cmdline)
            else:
                p = subprocess.Popen(cmdline, close_fds=True, start_new_session=True)
            return p.poll() is None
        except Exception as e:
            raise BaseException(BROWSER_OPEN_ERROR, "브라우저열기실패{}".format(e))