import base64
import os
import time
from astronverse.actionlib import AtomicFormType, AtomicFormTypeMeta, AtomicLevel, DynamicsItem
from astronverse.actionlib.atomic import atomicMg
from astronverse.actionlib.types import PATH, WebPick
from astronverse.baseline.logger.logger import logger
from astronverse.browser import *
from astronverse.browser.error import *
from astronverse.browser.browser import Browser
from astronverse.browser.utils.table_filter import (
    DataFilter,
    page_values_merge,
    table_df_to_out,
    table_json_merge_values,
)
from astronverse.browser.core.core_win import BrowserCore
from astronverse.locator import smooth_move
from astronverse.locator.locator import locator


def get_browser_instance():
    return get_default_browser()


def get_default_browser(raw_browser_type: str = None):
    """가져오기가능사용의브라우저."""
    control = None
    browser_type = CommonForBrowserType.BTChrome
    for bt in ALL_BROWSER_TYPES:
        if raw_browser_type and bt.value != raw_browser_type:
            continue
        control = BrowserCore.get_browser_control(bt.value)
        if control:
            browser_type = bt
            break
    if not control:
        return None
    browser = Browser()
    browser.browser_control = control
    browser.browser_type = browser_type
    return browser


def check_element(browser_obj: Browser, element_data: WebPick, element_timeout: int = 10):
    """감지browser_obj,  element_data"""
    if element_data:
        if element_data.get("elementData", {}).get("app", "") == "iexplore":
            raise Exception(
                "선택원유형필요브라우저유형보관일!현재의브라우저로!{}".format(browser_obj.browser_type.value)
            )

    if not browser_obj:
        browser_obj = get_default_browser()

    if element_data:
        res = BrowserElement.wait_element(
            browser_obj=browser_obj,
            element_data=element_data,
            ele_status=WaitElementForStatusFlag.ElementExists,
            element_timeout=element_timeout,
        )
        if not res:
            reason = browser_obj.send_browser_extension(
                browser_type=browser_obj.browser_type.value,
                key="checkElement",
                data=element_data["elementData"]["path"],
            )
            msg = ""
            if isinstance(reason, dict):
                msg = reason.get("msg", "")
            raise BaseException(WEB_GET_ELE_ERROR.format(msg), "브라우저요소찾을 수 없는 !")
    return browser_obj


class BrowserElement:
    """브라우저요소유형, 웹 페이지요소의방법법."""

    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        outputList=[
            atomicMg.param("wait_element", types="Bool"),
        ],
    )
    def wait_element(
        browser_obj: Browser,
        element_data: WebPick,
        ele_status: WaitElementForStatusFlag = WaitElementForStatusFlag.ElementExists,
        element_timeout: int = 10,
    ) -> bool:
        """대기원출력또는실패."""
        if element_data:
            if element_data.get("elementData", {}).get("app", "") == "iexplore":
                raise Exception(
                    "선택원유형필요브라우저유형보관일!현재의브라우저로!{}".format(
                        browser_obj.browser_type.value
                    )
                )

        if not browser_obj:
            browser_obj = get_default_browser()

        timeout = element_timeout
        if timeout < 0:
            raise BaseException(PARAMETER_INVALID_FORMAT.format(timeout), f"대기시간할 수 없음소0!{timeout}")
        while timeout >= 0:
            start = time.time()
            # 가져오기상태
            try:
                element_exist = browser_obj.send_browser_extension(
                    browser_type=browser_obj.browser_type.value,
                    key="elementIsReady",
                    data=element_data["elementData"]["path"],
                )
            except Exception:
                element_exist = None

            # 여부전결과
            if (
                ele_status == WaitElementForStatusFlag.ElementExists
                and element_exist
                or ele_status == WaitElementForStatusFlag.ElementDisappears
                and not element_exist
            ):
                return True
            else:
                # 재시도
                time.sleep(0.3)
                use = time.time() - start
                timeout -= use
        return False

    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        inputList=[
            atomicMg.param("simulate_flag", required=False),
            atomicMg.param(
                "assistive_key",
                dynamics=[
                    DynamicsItem(
                        key="$this.assistive_key.show",
                        expression="return $this.simulate_flag.value == true",
                    )
                ],
            ),
            atomicMg.param(
                "button_type",
                formType=AtomicFormTypeMeta(type=AtomicFormType.RADIO.value),
            ),
            atomicMg.param(
                "scroll_into_center",
                level=AtomicLevel.ADVANCED,
                dynamics=[
                    DynamicsItem(
                        key="$this.scroll_into_center.show",
                        expression="return $this.simulate_flag.value == true",
                    )
                ],
                required=False,
            ),
        ],
    )
    def click(
        browser_obj: Browser = None,
        element_data: WebPick = None,
        simulate_flag: bool = False,
        assistive_key: ButtonForAssistiveKeyFlag = ButtonForAssistiveKeyFlag.Nothing,
        button_type: ButtonForClickTypeFlag = ButtonForClickTypeFlag.Left,
        element_timeout: int = 10,
        scroll_into_center: bool = True,
    ):
        """클릭"""
        try:
            browser_obj = check_element(browser_obj, element_data, element_timeout)

            if assistive_key != ButtonForAssistiveKeyFlag.Nothing:
                from astronverse.input.code.keyboard import Keyboard

                Keyboard.key_down(assistive_key.value)

            if not simulate_flag:
                # js클릭
                browser_obj.send_browser_extension(
                    browser_type=browser_obj.browser_type.value,
                    key="clickElement",
                    data={
                        **element_data["elementData"]["path"],  # 해제패키지내부모듈딕셔너리의내용
                        "atomConfig": {"buttonType": button_type.value},
                    },
                )
            else:
                # 위치 지정
                element = locator.locator(
                    element_data.get("elementData"),
                    cur_target_app=browser_obj.browser_type.value,
                    scroll_into_center=scroll_into_center,
                )
                if isinstance(element.rect(), list):
                    raise Exception("브라우저 요소 위치를 확인할 수 없습니다.")

                # 클릭
                center = element.point()
                smooth_move(center.x, center.y, duration=0.5)
                from astronverse.input.code.mouse import Mouse

                Mouse.click(
                    x=center.x,
                    y=center.y,
                    clicks=2 if button_type == ButtonForClickTypeFlag.Double else 1,
                    button=("left" if button_type != ButtonForClickTypeFlag.Right else "right"),
                )
        finally:
            if assistive_key != ButtonForAssistiveKeyFlag.Nothing:
                from astronverse.input.code.keyboard import Keyboard

                Keyboard.key_up(assistive_key.value)

    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        inputList=[
            atomicMg.param("simulate_flag", required=False),
            atomicMg.param(
                "focus_time",
                required=False,
                dynamics=[
                    DynamicsItem(
                        key="$this.focus_time.show",
                        expression="return $this.simulate_flag.value == true",
                    )
                ],
            ),
            atomicMg.param(
                "fill_type",
                formType=AtomicFormTypeMeta(type=AtomicFormType.RADIO.value),
            ),
            atomicMg.param(
                "write_gap_time",
                required=False,
                dynamics=[
                    DynamicsItem(
                        key="$this.write_gap_time.show",
                        expression="return $this.simulate_flag.value == true",
                    )
                ],
            ),
            atomicMg.param(
                "fill_input",
                dynamics=[
                    DynamicsItem(
                        key="$this.fill_input.show",
                        expression=f"return $this.fill_type.value == '{FillInputForFillTypeFlag.Text.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "fill_input_credential",
                formType=AtomicFormTypeMeta(type=AtomicFormType.SELECT.value, params={"filters": ["credential"]}),
                dynamics=[
                    DynamicsItem(
                        key="$this.fill_input_credential.show",
                        expression=f"return $this.fill_type.value == '{FillInputForFillTypeFlag.Credential.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "input_type",
                formType=AtomicFormTypeMeta(type=AtomicFormType.RADIO.value),
            ),
            atomicMg.param(
                "scroll_into_center",
                level=AtomicLevel.ADVANCED,
                dynamics=[
                    DynamicsItem(
                        key="$this.scroll_into_center.show",
                        expression="return $this.simulate_flag.value == true",
                    )
                ],
                required=False,
            ),
        ],
        outputList=[
            atomicMg.param("form_input", types="Str"),
        ],
    )
    def input(
        browser_obj: Browser = None,
        element_data: WebPick = None,
        simulate_flag: bool = False,
        fill_type: FillInputForFillTypeFlag = FillInputForFillTypeFlag.Text,
        fill_input: str = "",
        fill_input_credential: str = "",
        element_timeout: int = 10,
        focus_time: float = 1000,
        write_gap_time: float = 0,
        input_type: FillInputForInputTypeFlag = FillInputForInputTypeFlag.Overwrite,
        scroll_into_center: bool = True,
    ):
        """
        브라우저 입력란에 값을 입력합니다.
        """
        browser_obj = check_element(browser_obj, element_data, element_timeout)

        if fill_type == FillInputForFillTypeFlag.Text:
            text = fill_input
        elif fill_type == FillInputForFillTypeFlag.Clipboard:
            from astronverse.input.code.clipboard import Clipboard

            text = Clipboard.paste()
        elif fill_type == FillInputForFillTypeFlag.Credential:
            from astronverse.actionlib.utils import Credential

            text = Credential.get_credential(fill_input_credential)
        else:
            text = ""

        # js입력
        if not simulate_flag:
            # 가져오기기존데이터
            if input_type == FillInputForInputTypeFlag.Append:
                origin_text = BrowserElement.element_text(
                    browser_obj=browser_obj,
                    element_data=element_data,
                    element_timeout=10,
                )
                if origin_text:
                    text = str(origin_text) + str(text)

            # 입력
            browser_obj.send_browser_extension(
                browser_type=browser_obj.browser_type.value,
                key="inputElement",
                data={
                    **element_data["elementData"]["path"],  # 해제패키지내부모듈딕셔너리의내용
                    "atomConfig": {"inputText": text},
                },
            )
        else:
            from astronverse.input.code.keyboard import Keyboard
            from astronverse.input.code.mouse import Mouse

            # 매개변수인증
            if focus_time < 0:
                raise BaseException(FOCUS_TIMEOUT_MUST_BE_POSITIVE, "포커스 대기시간은 0 이상이어야 합니다.")
            if write_gap_time < 0:
                raise BaseException(KEY_PRESS_INTERVAL_MUST_BE_NON_NEGATIVE, "입력 간격은 0 이상이어야 합니다.")

            # 위치 지정
            element = locator.locator(
                element_data.get("elementData"),
                cur_target_app=browser_obj.browser_type.value,
                scroll_into_center=scroll_into_center,
            )
            if isinstance(element.rect(), list):
                raise Exception("브라우저 요소 위치를 확인할 수 없습니다.")

            # 클릭
            center = element.point()
            smooth_move(center.x, center.y, duration=0.4)
            Mouse.click(x=center.x, y=center.y)

            # 빈입력
            if input_type == FillInputForInputTypeFlag.Overwrite:
                Keyboard.hotkey("ctrl", "a")
                time.sleep(0.5)
                Keyboard.press("delete")
                time.sleep(0.5)

            # 시간
            time.sleep(focus_time / 1000)

            # 유형
            if fill_type == FillInputForFillTypeFlag.Text:
                for item in text:
                    Keyboard.write_unicode(item)
                    # 입력시간
                    time.sleep(write_gap_time if write_gap_time > 0 else 0.03)
            elif fill_type == FillInputForFillTypeFlag.Clipboard:
                from astronverse.input.code.clipboard import Clipboard

                Clipboard.copy(data=text)
                Keyboard.hotkey("ctrl", "v")

        return text

    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        inputList=[
            atomicMg.param("scroll_into_center", level=AtomicLevel.ADVANCED, required=False),
        ],
        outputList=[],
    )
    def hover_over(
        browser_obj: Browser = None,
        element_data: WebPick = None,
        element_timeout: int = 10,
        scroll_into_center: bool = True,
    ):
        """
        마우스중지에서요소위(web)
        """
        browser_obj = check_element(browser_obj, element_data, element_timeout)
        element = locator.locator(
            element_data.get("elementData"),
            cur_target_app=browser_obj.browser_type.value,
            scroll_into_center=scroll_into_center,
        )
        if isinstance(element.rect(), list):
            raise Exception("브라우저 요소 위치를 확인할 수 없습니다.")
        element.move()

    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        inputList=[
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"file_type": "folder"},
                ),
            ),
        ],
        outputList=[
            atomicMg.param("xpath_shot", types="Str"),
        ],
    )
    def screenshot(
        browser_obj: Browser = None,
        element_data: WebPick = None,
        file_path: PATH = None,
        image_name: str = "",
        element_timeout: int = 10,
    ):
        """스크린샷"""
        browser_obj = check_element(browser_obj, element_data, element_timeout)
        data = browser_obj.send_browser_extension(
            browser_type=browser_obj.browser_type.value,
            key="elementShot",
            data={
                **element_data["elementData"]["path"],  # 해제패키지내부모듈딕셔너리의내용
            },
            timeout=30,
        )
        if data:
            data = data.replace("data:image/jpeg;base64,", "")
        else:
            raise Exception("브라우저 확장에서 스크린샷 데이터를 반환하지 않았습니다.")

        # 출력
        if not image_name.endswith((".png", ".jpg", ".jpeg")):
            image_name += ".jpg"
        path = os.path.join(file_path, image_name)
        with open(path, "wb") as f:
            f.write(base64.b64decode(data))
        return path

    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        inputList=[
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"file_type": "folder"},
                ),
            ),
        ],
        outputList=[
            atomicMg.param("position_shot", types="Str"),
        ],
    )
    def position_screenshot(
        browser_obj: Browser = None,
        element_data: WebPick = None,
        file_path: PATH = None,
        image_name: str = "",
        element_timeout: int = 10,
    ):
        """요소위치스크린샷"""
        browser_obj = check_element(browser_obj, element_data, element_timeout)
        element = locator.locator(element_data.get("elementData"), cur_target_app=browser_obj.browser_type.value)
        if isinstance(element.rect(), list):
            raise Exception("브라우저 요소 위치를 확인할 수 없습니다.")
        rect = element.rect()

        if not image_name.endswith((".png", ".jpg", ".jpeg")):
            image_name += ".jpg"
        path = os.path.join(file_path, image_name)
        from astronverse.input.code.screenshot import Screenshot

        Screenshot.screenshot(region=(rect.left, rect.top, rect.width(), rect.height()), file_path=path)
        return path

    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        inputList=[
            atomicMg.param(
                "scrollbar_type",
                formType=AtomicFormTypeMeta(type=AtomicFormType.RADIO.value),
            ),
            atomicMg.param(
                "element_data",
                dynamics=[
                    DynamicsItem(
                        key="$this.element_data.show",
                        expression=f"return $this.scrollbar_type.value == '{ScrollbarType.CustomEle.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "element_timeout",
                dynamics=[
                    DynamicsItem(
                        key="$this.element_timeout.show",
                        expression=f"return $this.scrollbar_type.value == '{ScrollbarType.CustomEle.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "x_scroll_type",
                dynamics=[
                    DynamicsItem(
                        key="$this.x_scroll_type.show",
                        expression=f"return $this.scroll_direction.value == '{ScrollDirection.Horizontal.value}'",
                    )
                ],
                formType=AtomicFormTypeMeta(type=AtomicFormType.RADIO.value),
            ),
            atomicMg.param(
                "y_scroll_type",
                dynamics=[
                    DynamicsItem(
                        key="$this.y_scroll_type.show",
                        expression=f"return $this.scroll_direction.value == '{ScrollDirection.Vertical.value}'",
                    )
                ],
                formType=AtomicFormTypeMeta(type=AtomicFormType.RADIO.value),
            ),
            atomicMg.param(
                "x_custom_scroll_dis",
                dynamics=[
                    DynamicsItem(
                        key="$this.x_custom_scroll_dis.show",
                        expression=f"return $this.scroll_direction.value == '{ScrollDirection.Horizontal.value}' && $this.x_scroll_type.value == '{ScrollbarForXScrollTypeFlag.Defined.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "y_custom_scroll_dis",
                dynamics=[
                    DynamicsItem(
                        key="$this.y_custom_scroll_dis.show",
                        expression=f"return $this.scroll_direction.value == '{ScrollDirection.Vertical.value}' && $this.y_scroll_type.value == '{ScrollbarForYScrollTypeFlag.Defined.value}'",
                    )
                ],
            ),
        ],
    )
    def scroll(
        browser_obj: Browser = None,
        scrollbar_type: ScrollbarType = ScrollbarType.Window,
        element_data: WebPick = None,
        scroll_direction: ScrollDirection = ScrollDirection.Horizontal,
        x_scroll_type: ScrollbarForXScrollTypeFlag = ScrollbarForXScrollTypeFlag.Left,
        x_custom_scroll_dis: int = 0,
        y_scroll_type: ScrollbarForYScrollTypeFlag = ScrollbarForYScrollTypeFlag.Top,
        y_custom_scroll_dis: int = 0,
        element_timeout: int = 10,
    ):
        """."""
        browser_obj = check_element(browser_obj, element_data, element_timeout)

        scroll_distance_x = ""
        scroll_distance_y = ""
        scroll_to = "top"
        scroll_axis = "y"
        if scroll_direction == ScrollDirection.Vertical:
            if y_scroll_type == ScrollbarForYScrollTypeFlag.Defined:
                scroll_distance_y = y_custom_scroll_dis
                scroll_to = "custom"
            elif y_scroll_type == ScrollbarForYScrollTypeFlag.Bottom:
                scroll_distance_y = 99999
                scroll_to = "bottom"
        else:
            scroll_to = "left"
            scroll_axis = "x"
            if x_scroll_type == ScrollbarForXScrollTypeFlag.Defined:
                scroll_distance_x = x_custom_scroll_dis
                scroll_to = "custom"
            elif x_scroll_type == ScrollbarForXScrollTypeFlag.Right:
                scroll_distance_x = 99999
                scroll_to = "right"

        if scrollbar_type == ScrollbarType.Window:
            browser_obj.send_browser_extension(
                browser_type=browser_obj.browser_type.value,
                key="scrollWindow",
                data={
                    "atomConfig": {
                        "scrollX": scroll_distance_x,
                        "scrollY": scroll_distance_y,
                        "scrollBehavior": "auto",
                        "scrollTo": scroll_to,
                        "scrollAxis": scroll_axis,
                    }
                },
            )
        else:
            # 지정
            browser_obj.send_browser_extension(
                browser_type=browser_obj.browser_type.value,
                key="scrollWindow",
                data={
                    **element_data["elementData"]["path"],  # 해제패키지내부모듈딕셔너리의내용
                    "atomConfig": {
                        "scrollX": scroll_distance_x,
                        "scrollY": scroll_distance_y,
                        "scrollBehavior": "auto",
                        "scrollTo": scroll_to,
                        "scrollAxis": scroll_axis,
                    },
                },
            )

    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        inputList=[
            atomicMg.param("scroll_into_center", level=AtomicLevel.ADVANCED, required=False),
        ],
        outputList=[],
    )
    def scroll_into_view(
        browser_obj: Browser = None,
        element_data: WebPick = None,
        element_timeout: int = 10,
        scroll_into_center: bool = True,
    ):
        """까지요소가능위치."""
        browser_obj = check_element(browser_obj, element_data, element_timeout)
        browser_obj.send_browser_extension(
            browser_type=browser_obj.browser_type.value,
            key="scrollIntoView",
            data={**element_data["elementData"]["path"], "atomConfig": {"scrollIntoCenter": scroll_into_center}},
        )

    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        inputList=[
            atomicMg.param(
                "attribute_name",
                dynamics=[
                    DynamicsItem(
                        key="$this.attribute_name.show",
                        expression=f"return $this.get_type.value == '{ElementGetAttributeHasSelfTypeFlag.GetAttribute.value}'",
                    )
                ],
            ),
        ],
        outputList=[
            atomicMg.param("get_similar_ele", types="List"),
        ],
    )
    def similar(
        browser_obj: Browser,
        element_data: WebPick,
        get_type: ElementGetAttributeHasSelfTypeFlag = ElementGetAttributeHasSelfTypeFlag.GetElement,
        attribute_name: str = "",
        element_timeout: int = 10,
    ) -> list:
        """
        가져오기 원목록
        """
        browser_obj = check_element(browser_obj, element_data, element_timeout)
        # 가져오기 요소의정보
        data = browser_obj.send_browser_extension(
            browser_type=browser_obj.browser_type.value,
            key="elementFromSelect",
            data=element_data["elementData"]["path"],
        )
        # 요소, 를결과입력list
        if get_type == ElementGetAttributeHasSelfTypeFlag.GetElement:
            res_list = []
            for di in data:
                res_list.append(
                    {
                        "elementData": {
                            "version": element_data["elementData"]["version"],
                            "type": element_data["elementData"]["type"],
                            "app": element_data["elementData"]["app"],
                            "picker_type": "ELEMENT",
                            "path": di,
                        }
                    }
                )
            return res_list

        res_list = []
        for di in data:
            data = browser_obj.send_browser_extension(
                browser_type=browser_obj.browser_type.value,
                key="getElementAttrs",
                data={
                    **di,  # 해제패키지내부모듈딕셔너리의내용
                    "atomConfig": {
                        "operation": str(list(ElementGetAttributeHasSelfTypeFlag).index(get_type) - 1),
                        "attrName": attribute_name,
                    },
                },
            )
            res_list.append(data)
        return res_list

    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        noAdvanced=True,
        inputList=[
            atomicMg.param(
                "attribute_name",
                dynamics=[
                    DynamicsItem(
                        key="$this.attribute_name.show",
                        expression=f"return $this.get_type.value == '{ElementGetAttributeHasSelfTypeFlag.GetAttribute.value}'",
                    )
                ],
            ),
        ],
        outputList=[
            atomicMg.param("index", types="Int"),
            atomicMg.param("item", types="Any"),
        ],
    )
    def loop_similar(
        browser_obj: Browser,
        element_data: WebPick,
        get_type: ElementGetAttributeHasSelfTypeFlag = ElementGetAttributeHasSelfTypeFlag.GetElement,
        start: int = 0,
        end: int = -1,
        attribute_name: str = "",
        element_timeout: int = 10,
    ):
        """요소"""
        browser_obj = check_element(browser_obj, element_data, element_timeout)

        def get_iterator():
            count = 0
            batch_size = 20
            while True:
                data = browser_obj.send_browser_extension(
                    browser_type=browser_obj.browser_type.value,
                    key="getSimilarIterator",
                    data={
                        **element_data["elementData"]["path"],  # 해제패키지내부모듈딕셔너리의내용
                        "index": count,
                        "count": batch_size,
                    },
                )
                if not data or len(data) <= 0:
                    break
                for di in data:
                    if count < start:
                        count += 1
                        continue
                    if 0 < end <= count:
                        return
                    count += 1
                    if get_type == ElementGetAttributeHasSelfTypeFlag.GetElement:
                        di_wrapper = {
                            "elementData": {
                                "version": element_data["elementData"]["version"],
                                "type": element_data["elementData"]["type"],
                                "app": element_data["elementData"]["app"],
                                "picker_type": "ELEMENT",
                                "path": di,
                            }
                        }
                        yield count, di_wrapper
                    else:
                        res = browser_obj.send_browser_extension(
                            browser_type=browser_obj.browser_type.value,
                            key="getElementAttrs",
                            data={
                                **di,  # 해제패키지내부모듈딕셔너리의내용
                                "atomConfig": {
                                    "operation": str(list(ElementGetAttributeHasSelfTypeFlag).index(get_type) - 1),
                                    "attrName": attribute_name,
                                },
                            },
                        )
                        yield count, res
                similar_count = data[0]["similarCount"]
                if similar_count <= count:
                    break

        return get_iterator()

    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        outputList=[
            atomicMg.param("data_pick", types="Str"),
        ],
    )
    def element_text(browser_obj: Browser, element_data: WebPick, element_timeout: int = 10) -> str:
        """
        가져오기원텍스트내용
        """

        browser_obj = check_element(browser_obj, element_data, element_timeout)
        res = browser_obj.send_browser_extension(
            browser_type=browser_obj.browser_type.value,
            key="getElementText",
            data={
                **element_data["elementData"]["path"],  # 해제패키지내부모듈딕셔너리의내용
            },
        )
        return res

    @staticmethod
    @atomicMg.atomic("BrowserElement")
    def slider_hover(
        browser_obj: Browser = None,
        element_slider: WebPick = None,
        element_progress: WebPick = None,
        percent_value: float = 0.0,
        drag_direction: ElementDragDirectionTypeFlag = ElementDragDirectionTypeFlag.Left,
        drag_type: ElementDragTypeFlag = ElementDragTypeFlag.Start,
        duration: float = 0.25,
    ):
        """"""
        from astronverse.input.code.mouse import Mouse

        def html_drag(start_pos, end_pos, duration):
            """
            지정으로끝 행
            """
            smooth_move(start_pos[0], start_pos[1], duration=0.05)
            Mouse.down(x=start_pos[0], y=start_pos[1], button="left")
            logger.info(f"slider_hover start {start_pos}  end {end_pos}")
            smooth_move(end_pos[0], end_pos[1], duration=duration)
            time.sleep(0.05)
            Mouse.up(x=end_pos[0], y=end_pos[1], button="left")  # 에서결과위치마우스버튼
            time.sleep(0.05)

        percent_value = max(0.0, min(1.0, percent_value / 100))
        # 위치 지정및요소
        # (필요의요소)
        element = locator.locator(element_slider.get("elementData"), cur_target_app=browser_obj.browser_type.value)
        if isinstance(element.rect(), list):
            raise Exception("슬라이더 요소 위치를 확인할 수 없습니다.")
        slider_center = element.point()

        # (가능의)
        element = locator.locator(
            element_progress.get("elementData"), cur_target_app=browser_obj.browser_type.value, scroll_into_view=False
        )
        if isinstance(element.rect(), list):
            raise Exception("진행 바 요소 위치를 확인할 수 없습니다.")
        progress_rect = element.rect()

        # 계획의및위치
        progress_left = progress_rect.left
        progress_top = progress_rect.top
        progress_width = progress_rect.right - progress_rect.left
        progress_height = progress_rect.bottom - progress_rect.top

        # 기록위치
        start_pos = [slider_center.x, slider_center.y]
        logger.info(f"중: {start_pos}")
        logger.info(f"위치및: [{progress_left}, {progress_top}, {progress_width}, {progress_height}]")

        # 근거방법예가로예세로
        is_horizontal = drag_direction in [
            ElementDragDirectionTypeFlag.Left,
            ElementDragDirectionTypeFlag.Right,
        ]
        logger.info(f"방법: {'가로' if is_horizontal else '세로'}, 방법: {drag_direction.value}")

        # 계획종료위치
        if is_horizontal:
            # 가로
            if drag_type == ElementDragTypeFlag.Start:  # 에서방식
                if drag_direction == ElementDragDirectionTypeFlag.Right:
                    # 오른쪽, 직선연결사용분
                    target_x = progress_left + (progress_width * percent_value)
                else:  # 왼쪽
                    # 사용반대분(1-percent_value)
                    target_x = progress_left + (progress_width * (1 - percent_value))
                end_pos = [int(target_x), int(slider_center.y)]
            else:  # 현재위치방식
                # 계획거리
                move_distance = progress_width * percent_value
                if drag_direction == ElementDragDirectionTypeFlag.Left:
                    move_distance = -move_distance
                # 계획목록 위치
                target_x = slider_center.x + move_distance
                # 확인아니오초과출력
                target_x = max(progress_left, min(progress_left + progress_width, target_x))
                end_pos = [int(target_x), int(slider_center.y)]
        else:
            # 세로
            if drag_type == ElementDragTypeFlag.Start:  # 에서방식
                if drag_direction == ElementDragDirectionTypeFlag.Down:
                    # 아래, 직선연결사용분
                    target_y = progress_top + (progress_height * percent_value)
                else:  # 위
                    # 사용반대분(1-percent_value)
                    target_y = progress_top + (progress_height * (1 - percent_value))
                end_pos = [int(slider_center.x), int(target_y)]
            else:  # 현재위치방식
                # 계획거리
                move_distance = progress_height * percent_value
                if drag_direction == ElementDragDirectionTypeFlag.Up:
                    move_distance = -move_distance
                # 계획목록 위치
                target_y = slider_center.y + move_distance
                # 확인아니오초과출력
                target_y = max(progress_top, min(progress_top + progress_height, target_y))
                end_pos = [int(slider_center.x), int(target_y)]

        logger.info(f"계획의종료위치: {end_pos}")
        html_drag(start_pos, end_pos, duration)

    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        inputList=[atomicMg.param("current_content", required=False)],
        outputList=[
            atomicMg.param("get_selected", types="List"),
        ],
    )
    def get_select(
        browser_obj: Browser = None,
        element_data: WebPick = None,
        current_content: bool = True,
        element_timeout: int = 10,
    ):
        """가져오기드롭다운선택중값."""

        browser_obj = check_element(browser_obj, element_data, element_timeout)
        data = browser_obj.send_browser_extension(
            browser_type=browser_obj.browser_type.value,
            key="getElementSelected",
            data={
                **element_data["elementData"]["path"],  # 해제패키지내부모듈딕셔너리의내용
                "atomConfig": {
                    "option": "selected" if current_content else "_",
                },
            },
        )
        return data

    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        outputList=[
            atomicMg.param("get_checkbox_checked", types="Str"),
        ],
    )
    def get_checked(
        browser_obj: Browser = None,
        element_data: WebPick = None,
        element_timeout: int = 10,
    ):
        """가져오기복사선택선택중상태."""
        browser_obj = check_element(browser_obj, element_data, element_timeout)
        data = browser_obj.send_browser_extension(
            browser_type=browser_obj.browser_type.value,
            key="getElementChecked",
            data={
                **element_data["elementData"]["path"],  # 해제패키지내부모듈딕셔너리의내용
            },
        )
        return data

    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        inputList=[
            atomicMg.param(
                "value",
                required=False,
                dynamics=[
                    DynamicsItem(
                        key="$this.value.show",
                        expression=f"return ['{SelectionPartner.Contains.value}', '{SelectionPartner.Equal.value}'].includes($this.pattern.value)",
                    )
                ],
            ),
            atomicMg.param(
                "solution",
                required=False,
                dynamics=[
                    DynamicsItem(
                        key="$this.solution.show",
                        expression=f"return $this.pattern.value == '{SelectionPartner.Index.value}'",
                    )
                ],
            ),
        ],
    )
    def set_select(
        browser_obj: Browser = None,
        element_data: WebPick = None,
        pattern: SelectionPartner = SelectionPartner.Contains,
        value: str = "",
        solution: int = 0,
        element_timeout: int = 10,
    ):
        """드롭다운선택중값."""
        browser_obj = check_element(browser_obj, element_data, element_timeout)
        browser_obj.send_browser_extension(
            browser_type=browser_obj.browser_type.value,
            key="setElementSelected",
            data={
                **element_data["elementData"]["path"],  # 해제패키지내부모듈딕셔너리의내용
                "atomConfig": {
                    "value": value,
                    "pattern": pattern.value,
                    "indexValue": solution,
                },
            },
        )

    @staticmethod
    @atomicMg.atomic("BrowserElement")
    def set_checked(
        browser_obj: Browser = None,
        element_data: WebPick = None,
        checked_type: ElementCheckedTypeFlag = ElementCheckedTypeFlag.Checked,
        element_timeout: int = 10,
    ):
        """복사선택선택중상태."""
        browser_obj = check_element(browser_obj, element_data, element_timeout)
        browser_obj.send_browser_extension(
            browser_type=browser_obj.browser_type.value,
            key="setElementChecked",
            data={
                **element_data["elementData"]["path"],  # 해제패키지내부모듈딕셔너리의내용
                "atomConfig": {
                    "checked": checked_type == ElementCheckedTypeFlag.Checked,
                    "reverse": checked_type == ElementCheckedTypeFlag.Reversed,
                },
            },
        )

    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        inputList=[
            atomicMg.param(
                "get_type",
                dynamics=[
                    DynamicsItem(
                        key="$this.get_type.show",
                        expression=f"return $this.operation_type.value == '{ElementAttributeOpTypeFlag.Get.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "attribute_name",
                dynamics=[
                    DynamicsItem(
                        key="$this.attribute_name.show",
                        expression="return ['getAttribute','getStyle'].includes($this.get_type.value) || ['set', 'del'].includes($this.operation_type.value)",
                    )
                ],
            ),
            atomicMg.param(
                "position",
                dynamics=[
                    DynamicsItem(
                        key="$this.position.show",
                        expression=f"return $this.get_type.value == '{ElementGetAttributeTypeFlag.GetPosition.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "attribute_value",
                dynamics=[
                    DynamicsItem(
                        key="$this.attribute_value.show",
                        expression=f"return $this.operation_type.value == '{ElementAttributeOpTypeFlag.Set.value}'",
                    )
                ],
            ),
        ],
        outputList=[
            atomicMg.param(
                "get_ele_attr",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.get_ele_attr.show",
                        expression=f"return $this.get_type.value == '{ElementGetAttributeTypeFlag.GetAttribute.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "get_ele_value",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.get_ele_value.show",
                        expression=f"return $this.get_type.value == '{ElementGetAttributeTypeFlag.GetValue.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "get_ele_html",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.get_ele_html.show",
                        expression=f"return $this.get_type.value == '{ElementGetAttributeTypeFlag.GetHtml.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "get_ele_link",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.get_ele_link.show",
                        expression=f"return $this.get_type.value == '{ElementGetAttributeTypeFlag.GetLink.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "get_ele_text",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.get_ele_text.show",
                        expression=f"return $this.get_type.value == '{ElementGetAttributeTypeFlag.GetText.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "get_ele_position",
                types="List",
                dynamics=[
                    DynamicsItem(
                        key="$this.get_ele_position.show",
                        expression=f"return $this.get_type.value == '{ElementGetAttributeTypeFlag.GetPosition.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "get_ele_selected",
                types="Bool",
                dynamics=[
                    DynamicsItem(
                        key="$this.get_ele_selected.show",
                        expression=f"return $this.get_type.value == '{ElementGetAttributeTypeFlag.GetSelection.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "get_ele_style",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.get_ele_style.show",
                        expression="return $this.get_type.value == '{}'".format(
                            ElementGetAttributeTypeFlag.GetStyle.value
                        ),
                    )
                ],
            ),
        ],
    )
    def element_operation(
        browser_obj: Browser = None,
        element_data: WebPick = None,
        operation_type: ElementAttributeOpTypeFlag = ElementAttributeOpTypeFlag.Get,
        get_type: ElementGetAttributeTypeFlag = ElementGetAttributeTypeFlag.GetText,
        attribute_name: str = "",
        position: RelativePosition = RelativePosition.ScreenLeft,
        attribute_value: str = "",
        element_timeout: int = 10,
    ):
        """삼개대유형예선택, 매개변수예선택출력후출력"""
        browser_obj = check_element(browser_obj, element_data, element_timeout)
        if operation_type == ElementAttributeOpTypeFlag.Del:
            browser_obj.send_browser_extension(
                browser_type=browser_obj.browser_type.value,
                key="removeElementAttr",
                data={
                    **element_data["elementData"]["path"],  # 해제패키지내부모듈딕셔너리의내용
                    "atomConfig": {
                        "attrName": attribute_name,
                    },
                },
            )
        elif operation_type == ElementAttributeOpTypeFlag.Get:
            data = browser_obj.send_browser_extension(
                browser_type=browser_obj.browser_type.value,
                key="getElementAttrs",
                data={
                    **element_data["elementData"]["path"],  # 해제패키지내부모듈딕셔너리의내용
                    "atomConfig": {
                        "attrName": attribute_name,
                        "operation": str(list(ElementGetAttributeTypeFlag).index(get_type)),
                    },
                },
            )

            logger.info(f"가져오기요소속성: {data}")
            if get_type == ElementGetAttributeTypeFlag.GetPosition:
                if position == RelativePosition.ScreenLeft:
                    top, left = BrowserCore.get_browser_point(browser_obj.browser_type.value)
                    return [
                        data["x"] + left,
                        data["y"] + top,
                        data["right"] + left,
                        data["bottom"] + top,
                    ]
                # 반환의예목록
                return [data["x"], data["y"], data["right"], data["bottom"]]
            else:
                return data

        elif operation_type == ElementAttributeOpTypeFlag.Set:
            browser_obj.send_browser_extension(
                browser_type=browser_obj.browser_type.value,
                key="setElementAttr",
                data={
                    **element_data["elementData"]["path"],  # 해제패키지내부모듈딕셔너리의내용
                    "atomConfig": {
                        "attrName": attribute_name,
                        "attrValue": attribute_value,
                    },
                },
            )

    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        inputList=[
            atomicMg.param(
                "excel_path",
                dynamics=[
                    DynamicsItem(
                        key="$this.excel_path.show",
                        expression="return $this.to_excel.value == true",
                    )
                ],
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"file_type": "file"},
                ),
            ),
        ],
        outputList=[
            atomicMg.param("table_pick", types="List"),
        ],
    )
    def get_table(
        browser_obj: Browser,
        element_data: WebPick,
        to_excel: bool = False,  # 여부내보내기excel
        excel_path: str = "",  # excel경로
        element_timeout: int = 10,
    ):
        """
        가져오기테이블내용
        """
        browser_obj = check_element(browser_obj, element_data, element_timeout)
        table_data = browser_obj.send_browser_extension(
            browser_type=browser_obj.browser_type.value,
            key="getTableData",
            data={
                **element_data["elementData"]["path"],  # 해제패키지내부모듈딕셔너리의내용
            },
        )

        # table_data 존재함 tbody
        if "tbody" in table_data:
            import pandas as pd

            df = pd.DataFrame(table_data["tbody"])
            table_body = df.values.tolist()
            # 추가테이블
            table_head = table_data["thead"]
            df.columns = table_data["thead"]
            table_list = table_body
            if to_excel:
                # 조회 excel_path 여부로 .xlsx 파일
                if excel_path and not excel_path.endswith(".xlsx"):
                    raise Exception(f"{excel_path}테이블파일 경로오류, 지원 .xlsx 파일")
                if excel_path is None:
                    excel_path = f"{element_data['elementData']['name']}.xlsx"
                df.to_excel(excel_path, index=False)
            return [table_head] + table_list
        else:
            raise Exception(table_data["msg"])

    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        inputList=[
            atomicMg.param(
                "batch_data",
                formType=AtomicFormTypeMeta(type=AtomicFormType.PICK.value, params={"use": "BATCH"}),
            ),
            # multi_page  로 True 시, page_element_data, page_count, page_interval매개변수 존재함
            atomicMg.param("multi_page", required=False),
            atomicMg.param(
                "page_count",
                dynamics=[
                    DynamicsItem(
                        key="$this.page_count.show",
                        expression="return $this.multi_page.value == true",
                    )
                ],
            ),
            atomicMg.param(
                "page_interval",
                dynamics=[
                    DynamicsItem(
                        key="$this.page_interval.show",
                        expression="return $this.multi_page.value == true",
                    )
                ],
            ),
            atomicMg.param(
                "element_data",
                dynamics=[
                    DynamicsItem(
                        key="$this.element_data.show",
                        expression="return $this.multi_page.value == true",
                    )
                ],
            ),
            atomicMg.param(
                "simulate_flag",
                required=False,
                dynamics=[
                    DynamicsItem(
                        key="$this.simulate_flag.show",
                        expression="return $this.multi_page.value == true",
                    )
                ],
            ),
            atomicMg.param(
                "button_type",
                formType=AtomicFormTypeMeta(type=AtomicFormType.RADIO.value),
            ),
            # to_excel 로 True 시, excel_path매개변수 존재함
            atomicMg.param(
                "excel_path",
                dynamics=[
                    DynamicsItem(
                        key="$this.excel_path.show",
                        expression="return $this.to_excel.value == true",
                    )
                ],
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={
                        "file_type": "file",
                        "filters": [".xlsx"],  # 에서file_type로file있음 식별자필요.txt의후파일
                        "defaultPath": "default.xlsx",  # 이름 사용 file
                    },
                ),
            ),
            atomicMg.param(
                "output_type",
                formType=AtomicFormTypeMeta(type=AtomicFormType.RADIO.value),
            ),
            # 여부출력테이블
            atomicMg.param("output_head", required=False),
            atomicMg.param(
                "scroll_into_center",
                level=AtomicLevel.ADVANCED,
                dynamics=[
                    DynamicsItem(
                        key="$this.scroll_into_center.show",
                        expression="return $this.simulate_flag.value == true",
                    )
                ],
                required=False,
            ),
        ],
        outputList=[
            atomicMg.param("table_pick", types="List"),
            atomicMg.param(
                "table_path",
                dynamics=[
                    DynamicsItem(
                        key="$this.table_path.show",
                        expression="return $this.to_excel.value == true",
                    )
                ],
                types="Str",
            ),
        ],
    )
    def data_batch(
        browser_obj: Browser = None,  # 브라우저객체
        batch_data: WebPick = None,  # 량가져오기객체
        multi_page: bool = False,  # 여부
        page_count: int = 1,  # 데이터
        page_interval: int = 1,  # 
        element_data: WebPick = None,  # 버튼요소
        simulate_flag: bool = False,  # 버튼요소사람클릭
        button_type: ButtonForClickTypeFlag = ButtonForClickTypeFlag.Left,
        to_excel: bool = False,  # 여부내보내기excel
        excel_path: str = "",  # excel경로
        element_timeout: int = 10,
        output_type: TablePickType = TablePickType.Row,
        output_head: bool = True,  # 여부출력테이블
        output_filter_empty_col: bool = False,  # 여부필터링빈열
        is_save_to_data_table: bool = False,  # 여부저장까지데이터테이블
        scroll_into_center: bool = True,
    ):
        """데이터가져오기(web)"""
        browser_obj = check_element(browser_obj, element_data, element_timeout)

        table_list = []
        batch_element = batch_data.get("elementData")  # 가져오기객체
        table_element = batch_element["path"]  # 원정보
        produce_type = table_element["produceType"]  # 가져오기유형,  produceType: table/similar
        for i in range(1, page_count + 1):
            # 가져오기테이블내용, 이배열
            if produce_type == "table":
                # 대기요소
                wait = BrowserElement.wait_element(
                    browser_obj=browser_obj,
                    element_data=batch_data,
                    ele_status=WaitElementForStatusFlag.ElementExists,
                    element_timeout=int(element_timeout),
                )
                if not wait:
                    raise BaseException(WEB_GET_ELE_ERROR.format("확인하세요가져오기요소"), "브라우저요소찾을 수 없는 !")
                # 전송확장
                response = browser_obj.send_browser_extension(
                    browser_type=browser_obj.browser_type.value,
                    key="tableDataBatch",
                    data=table_element,
                )
                # 인쇄 response
                # logger.info(f'table_element response: {response}')
                table_values = response["values"]
                table_list = page_values_merge(table_list, table_values, produce_type)
            else:
                # 요소객체
                similar_element = batch_element["path"]
                wait = BrowserElement.wait_element(
                    browser_obj=browser_obj,
                    element_data={
                        "elementData": {
                            "version": batch_element["version"],
                            "type": batch_element["type"],
                            "app": batch_element["app"],
                            "picker_type": "ELEMENT",
                            "path": similar_element,
                        }
                    },
                    ele_status=WaitElementForStatusFlag.ElementExists,
                    element_timeout=int(element_timeout),
                )
                if not wait:
                    raise BaseException(WEB_GET_ELE_ERROR.format("확인하세요가져오기요소"), "브라우저요소찾을 수 없는 !")
                response = browser_obj.send_browser_extension(
                    browser_type=browser_obj.browser_type.value,
                    key="simalarListBatch",
                    data=similar_element,
                )
                # 인쇄 response
                # logger.info(f'similar_element response: {response}')
                similar_values = response["values"]
                table_list = page_values_merge(table_list, similar_values, produce_type)

            # 여부
            if page_count > 1 and multi_page:
                # logger.info(f': {i} ')
                # 클릭버튼요소,호출 위의click 방법법
                try:
                    BrowserElement.click(
                        browser_obj=browser_obj,
                        element_data=element_data,
                        simulate_flag=simulate_flag,
                        assistive_key=ButtonForAssistiveKeyFlag.Nothing,
                        button_type=button_type,
                        element_timeout=element_timeout,
                        scroll_into_center=scroll_into_center,
                    )
                except Exception:
                    pass
                # 대기 page_interval 초
                time.sleep(page_interval)

        # logger.info(f'테이블데이터: {table_list}')
        # 병합가져오기까지의데이터 까지 table_element의 values 중
        data_formated = table_json_merge_values(data_json=table_element, values=table_list)
        # logger.info(f'테이블데이터형식: {data_formated}')

        # 데이터 처리
        data_filtered = DataFilter(data_json=data_formated).get_filtered_data()
        # logger.info(f'테이블데이터필터링: {data_filtered}')

        # 까지필터링후의 table_df
        table_df_out = table_df_to_out(data_json=data_filtered)

        # 여부필터링빈열
        if output_filter_empty_col:
            table_df_out = table_df_out.dropna(axis=1, how="all")

        output_data = []
        if output_head:
            output_data = [table_df_out.columns.tolist()] + table_df_out.values.tolist()
        else:
            output_data = table_df_out.values.tolist()

        if is_save_to_data_table:
            # 저장까지데이터테이블
            from astronverse.datatable import WriteMode, WriteType
            from astronverse.datatable.datatable import DataTable

            DataTable.write_data(
                write_type=WriteType.AREA,
                start_row=1,
                start_col="A",
                data=output_data,
                write_mode=WriteMode.OVERWRITE,
            )

        if to_excel:
            # 를table_list 변환로excel
            # 조회 excel_path 여부로 .xlsx 파일
            if excel_path and not excel_path.endswith(".xlsx"):
                raise Exception(f"{excel_path}테이블파일 경로오류, 지원 .xlsx 파일")
            if excel_path is None:
                excel_path = f"{table_element['name']}.xlsx"
            table_df_out.to_excel(excel_path, index=False, header=output_head)
            # logger.info(f'테이블데이터완료저장까지 {excel_path}')
            table_path = excel_path
            if output_type == TablePickType.Row:
                return output_data, table_path
            return table_df_out.to_dict(orient="list"), table_path

        # 반환테이블데이터 행/열 반환
        if output_type == TablePickType.Row:
            return output_data
        else:
            return table_df_out.to_dict(orient="list")

    # 근거xpath/cssSelector 완료요소객체
    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        inputList=[atomicMg.param("locate_type", formType=AtomicFormTypeMeta(AtomicFormType.SELECT.value))],
        outputList=[atomicMg.param("element_obj", types="Any")],
    )
    def create_element(
        browser_obj: Browser = None,  # 브라우저객체
        locate_type: LocateType = LocateType.Xpath,  # 위치 지정방식 xpath / cssSelector / text
        locate_value: str = "",
        return_type: ElementCreateReturnType = ElementCreateReturnType.LIST,
    ):
        """
        근거xpath또는cssSelector완료요소객체
        """
        browser_obj = check_element(browser_obj, None, 20)

        # 검증locate_value
        if not locate_value:
            raise BaseException(CODE_EMPTY, "위치 지정값비워 둘 수 없습니다")

        # 전송확장
        response = browser_obj.send_browser_extension(
            browser_type=browser_obj.browser_type.value,
            key="generateElement",
            data={"type": locate_type.value, "value": locate_value, "returnType": return_type.value},
        )

        #  response 여부예목록, 목록, 추가요소속성
        if isinstance(response, list):
            element_obj = []
            for item in response:
                element_obj.append(
                    {
                        "elementData": {
                            "app": browser_obj.browser_type.value,
                            "type": "web",
                            "picker_type": "ELEMENT",
                            "version": "1",
                            "path": item,
                        }
                    }
                )
        else:
            element_obj = {
                "elementData": {
                    "app": browser_obj.browser_type.value,
                    "type": "web",
                    "picker_type": "ELEMENT",
                    "version": "1",
                    "path": response,
                }
            }
        return element_obj

    # 근거요소객체까지닫기 요소
    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        inputList=[
            atomicMg.param(
                "relative_type",
                formType=AtomicFormTypeMeta(AtomicFormType.SELECT.value),
            ),
            atomicMg.param(
                "child_element_type",
                formType=AtomicFormTypeMeta(AtomicFormType.SELECT.value),
                dynamics=[
                    DynamicsItem(
                        key="$this.child_element_type.show",
                        expression=f"return $this.relative_type.value == '{RelativeType.Child.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "sibling_element_type",
                formType=AtomicFormTypeMeta(AtomicFormType.SELECT.value),
                dynamics=[
                    DynamicsItem(
                        key="$this.sibling_element_type.show",
                        expression=f"return $this.relative_type.value == '{RelativeType.Sibling.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "child_element_xpath",
                dynamics=[
                    DynamicsItem(
                        key="$this.child_element_xpath.show",
                        expression=f"return $this.child_element_type.value == '{ChildElementType.Xpath.value}' && $this.relative_type.value == '{RelativeType.Child.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "child_element_index",
                dynamics=[
                    DynamicsItem(
                        key="$this.child_element_index.show",
                        expression=f"return $this.child_element_type.value == '{ChildElementType.Index.value}' && $this.relative_type.value == '{RelativeType.Child.value}'",
                    )
                ],
            ),
            atomicMg.param(
                "is_multiple",
                dynamics=[
                    DynamicsItem(
                        key="$this.is_multiple.show",
                        expression=f"return $this.child_element_type.value == '{ChildElementType.Xpath.value}' && $this.relative_type.value == '{RelativeType.Child.value}'",
                    )
                ],
            ),
        ],
        outputList=[atomicMg.param("element_obj", types="Any")],
    )
    def get_relative_element(
        browser_obj: Browser = None,
        element_data: WebPick = None,
        relative_type: RelativeType = RelativeType.Child,
        child_element_type: ChildElementType = ChildElementType.All,
        child_element_xpath: str = "",
        child_element_index: int = 0,
        sibling_element_type: SiblingElementType = SiblingElementType.All,
        element_timeout: int = 10,
        is_multiple: bool = False,
    ):
        """
        근거요소객체까지닫기 요소
        """
        browser_obj = check_element(browser_obj, element_data, element_timeout)

        element_get_type = ""
        if relative_type == RelativeType.Child:
            element_get_type = child_element_type.value
        if relative_type == RelativeType.Sibling:
            element_get_type = sibling_element_type.value

        response = browser_obj.send_browser_extension(
            browser_type=browser_obj.browser_type.value,
            key="getRelativeElement",
            data={
                **element_data["elementData"]["path"],  # 해제패키지내부모듈딕셔너리의내용
                "relativeOptions": {
                    "relativeType": relative_type.value,
                    "index": child_element_index,
                    "xpath": child_element_xpath,
                    "elementGetType": element_get_type,
                    "multiple": is_multiple,
                },
            },
        )
        # response 여부예목록
        if isinstance(response, list):
            element_obj = []
            for item in response:
                element_obj.append(
                    {
                        "elementData": {
                            "app": element_data["elementData"]["app"],
                            "version": element_data["elementData"]["version"],
                            "type": element_data["elementData"]["type"],
                            "picker_type": "ELEMENT",
                            "path": item,
                        }
                    }
                )
        else:
            element_obj = {
                "elementData": {
                    "app": element_data["elementData"]["app"],
                    "version": element_data["elementData"]["version"],
                    "type": element_data["elementData"]["type"],
                    "picker_type": "ELEMENT",
                    "path": response,
                }
            }
        return element_obj

    @staticmethod
    @atomicMg.atomic(
        "BrowserElement",
        outputList=[
            atomicMg.param("element_exist", types="Bool"),
        ],
    )
    def element_exist(
        browser_obj: Browser,
        element_data: WebPick,
    ) -> bool:
        """조회요소여부존재함."""
        try:
            browser_obj = check_element(browser_obj, element_data, 20)
            element_exist = browser_obj.send_browser_extension(
                browser_type=browser_obj.browser_type.value,
                key="elementIsRender",
                data=element_data["elementData"]["path"],
            )
        except Exception:
            element_exist = False
        return element_exist
