from typing import Any
from urllib.parse import urljoin

import requests
from astronverse.actionlib.atomic import atomicMg
from astronverse.actionlib.error import *
from astronverse.actionlib.types import typesMg
from astronverse.browser import CommonForBrowserType
from astronverse.browser.error import *


class Browser:
    """브라우저유형, 브라우저의본방법법."""

    def __init__(self):
        self.browser_type: CommonForBrowserType = CommonForBrowserType.BTChrome
        self.browser_abs_path: str = ""
        self.browser_control = None

    @typesMg.shortcut(group_key="Browser", res_type="Str")
    def get_url(self) -> str:
        """가져오기현재웹 페이지URL."""
        return self.send_browser_extension(browser_type=self.browser_type.value, key="getUrl")

    @typesMg.shortcut(group_key="Browser", res_type="Str")
    def get_title(self) -> str:
        """가져오기현재웹 페이지제목."""
        return self.send_browser_extension(browser_type=self.browser_type.value, key="getTitle")

    @typesMg.shortcut(group_key="Browser", res_type="Int")
    def get_tabid(self) -> int:
        """가져오기현재태그ID."""
        data = self.send_browser_extension(browser_type=self.browser_type.value, key="getTabId")
        return data if isinstance(data, int) else -1

    @classmethod
    def __validate__(cls, name: str, value):
        """인증브라우저객체."""
        if isinstance(value, Browser):
            return value
        return None

    @staticmethod
    def send_browser_rpc(req: dict, timeout: float = 0.0) -> Any:
        """전송브라우저RPC요청 ."""
        gateway_port = atomicMg.cfg().get("GATEWAY_PORT")
        if not gateway_port:
            gateway_port = "13159"
        url = f"http://127.0.0.1:{gateway_port}"
        res = requests.post(
            urljoin(
                url,
                "browser_connector",
            )
            + "/browser/transition",
            json=req,
            timeout=timeout,
        )
        return res

    def send_browser_extension(
        self,
        browser_type: str,
        key: str,
        data: Any = None,
        data_path: str = "",
        timeout: float = None,
    ):
        """전송브라우저요청 ."""
        if not data:
            data = {}
        if browser_type == CommonForBrowserType.BTChrome.value:
            browser_type = CommonForBrowserType.BTChromium.value

        res = self.send_browser_rpc(
            {
                "browser_type": browser_type,
                "data": data,
                "key": key,
                "data_path": data_path,
            },
            timeout,
        )

        if res.status_code != 200:
            raise BaseException(BROWSER_EXTENSION_INSTALL_ERROR, "브라우저 확장 통신 오류입니다. 다시 시도하세요.")
        res_data = res.json()
        if not res_data.get("data"):
            raise BaseException(BROWSER_EXTENSION_INSTALL_ERROR, "브라우저 확장이 연결되지 않았습니다")
        if res_data.get("data").get("code") == "5001":
            raise BaseException(
                BROWSER_EXTENSION_ERROR_FORMAT.format(res_data.get("data").get("msg")), res_data.get("data").get("msg")
            )
        if res_data.get("data").get("code") == "5002":
            raise BaseException(WEB_GET_ELE_ERROR.format(res_data.get("data").get("msg")), "웹 페이지요소찾을 수 없는 ")
        if res_data.get("data").get("code") == "5003":
            raise BaseException(
                WEB_EXEC_ELE_ERROR.format(res_data.get("data").get("msg")), res_data.get("data").get("msg")
            )
        if res_data.get("data").get("code") == "5004":
            raise BaseException(
                BROWSER_EXTENSION_ERROR_FORMAT.format(res_data.get("data").get("msg")), res_data.get("data").get("msg")
            )
        return res_data.get("data").get("data")
