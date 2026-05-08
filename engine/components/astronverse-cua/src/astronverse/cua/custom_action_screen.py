import base64
import json
import tempfile
import time
from datetime import datetime
from pathlib import Path
from typing import Optional

import pyautogui
import pyperclip
import requests
from astronverse.actionlib.atomic import atomicMg
from astronverse.baseline.logger.logger import logger

#  GUI 작업의안내
COMPUTER_USE_PROMPT = """You are a GUI automation agent. You receive a task, screenshots, and action history. Choose the next single action required to complete the task.

# RPA client popup
The desktop RPA client may show a task-control popup near the bottom-right corner. It can display task status, pause/stop/log controls, and internal runtime details. Do not close or interact with this popup unless the user explicitly asks you to close popups or windows.

When the task is to close a popup, close a window, or dismiss a notice:
- Close only visible web-page or application popups that are relevant to the user request.
- Do not close the RPA task-control popup unless it is clearly the target.
- If no closeable popup is present, finish with a clear result such as {"type":"finished","thought":"No closeable popup is visible.","param":{}}.

# Workflow
Analyze the screenshot and history, then return exactly one JSON action.
1. thought: Summarize the next action in no more than 50 words.
2. type: One of input, click, drag, wait, data, finished, error, hotkey, hover, scroll, screenshot.
3. param: Parameters required by the selected action.

# Supported actions
- input: {"value": string, "point": [number, number]}
- click: {"point": [number, number], "button": "left"|"right", "clicks": number, "type": "click"|"down"|"up"}
- drag: {"start_point": [number, number], "end_point": [number, number]}
- wait: {"time_ms": number}
- data: {"data": boolean|string|object|array|number}
- finished: {}
- error: {"reason": string}
- hotkey: {"value": string}
- hover: {"point": [number, number]}
- scroll: {"point": [number, number], "delta_x": number, "delta_y": number}
- screenshot: {}

Return only valid JSON. Do not include markdown or extra text.
"""

API_URL = "http://127.0.0.1:{}/api/rpa-ai-service/cua/chat".format(
    atomicMg.cfg().get("GATEWAY_PORT") if atomicMg.cfg().get("GATEWAY_PORT") else "13159"
)


class CustomActionScreen:
    """계획기기사용관리유형 - 사용대유형"""

    def __init__(
        self,
        max_steps: int = 20,
        temperature: float = 0.0,
    ):
        """
        Agent

        Args:
            max_steps: 대실행데이터
            temperature: 유형정도매개변수
        """

        self.max_steps = max_steps
        self.temperature = temperature

        # 스크린샷디렉터리
        self.screenshot_dir = Path(tempfile.mkdtemp(prefix="cua_agent_"))
        self.screenshot_dir.mkdir(parents=True, exist_ok=True)

        # 기록
        self.screenshots: list[str] = []
        # 저장: (assistant, (base64_image, image_format) 또는 None)
        self.conversation_history: list[tuple[str, Optional[tuple[str, str]]]] = []
        self.pending_response: Optional[str] = None  # 대기저장의(대기다음 단계의스크린샷)
        self.instruction: Optional[str] = None  # 저장사용자

        # 화면저장, 매열기Image파일
        self.screen_width = None
        self.screen_height = None
        self.max_history_rounds = 3

        logger.info(f"[] 스크린샷저장디렉터리: {self.screenshot_dir}")

    def take_screenshot(self) -> tuple[str, str]:
        """
        가져오기현재화면, 에서스크린샷위위일클릭의위치(결과가있음)

        Returns:
            Tuple[스크린샷파일 경로, Base64코드의이미지]
        """
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        screenshot_path = self.screenshot_dir / f"screenshot_{timestamp}.png"

        # 사용pyautogui스크린샷
        screenshot = pyautogui.screenshot()
        screenshot.save(str(screenshot_path))

        # 일스크린샷시저장화면, 후매열기Image파일
        if self.screen_width is None or self.screen_height is None:
            self.screen_width, self.screen_height = screenshot.size
            logger.info(f"[] 저장화면: {self.screen_width}x{self.screen_height}")

        # 코드로Base64(사용후의이미지)
        with open(str(screenshot_path), "rb") as f:
            base64_image = base64.b64encode(f.read()).decode("utf-8")

        return str(screenshot_path), base64_image

    def build_messages(self, instruction: str, screenshot_path: str, base64_image: str) -> list[dict]:
        """
        생성전송유형의메시지(패키지)

        Args:
            instruction: 사용자
            screenshot_path: 스크린샷경로
            base64_image: Base64코드의이미지
        """
        # 저장(사용후)
        if not self.instruction:
            self.instruction = instruction

        # 가져오기이미지형식
        image_format = Path(screenshot_path).suffix[1:] or "png"

        # 생성시스템안내
        messages = [{"role": "system", "content": COMPUTER_USE_PROMPT}]

        # 추가기록, 보관새의 max_history_rounds 
        # 계획필요보관의기록수
        start_index = max(0, len(self.conversation_history) - self.max_history_rounds)
        recent_history = self.conversation_history[start_index:]

        # 추가메시지, 확인메시지순서정상아니오재복사
        for i, (assistant_response, screenshot_data) in enumerate(recent_history):
            # 결과가아니오예후일기록, 추가사용자메시지(패키지스크린샷)및
            if i < len(recent_history) - 1 and screenshot_data:
                screenshot_base64, screenshot_format = screenshot_data
                messages.append(
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "image_url",
                                "image_url": {"url": f"data:image/{screenshot_format};base64,{screenshot_base64}"},
                            },
                            {"type": "text", "text": self.instruction},
                        ],
                    }
                )
                messages.append({"role": "assistant", "content": assistant_response})
            # 결과가예후일기록, 추가, 메시지재복사
            elif i == len(recent_history) - 1:
                messages.append({"role": "assistant", "content": assistant_response})

        # 추가현재사용자메시지(패키지스크린샷및)
        messages.append(
            {
                "role": "user",
                "content": [
                    {"type": "image_url", "image_url": {"url": f"data:image/{image_format};base64,{base64_image}"}},
                    {"type": "text", "text": self.instruction},
                ],
            }
        )

        return messages

    def inference(self, messages: list[dict] = None) -> str:
        """
        호출유형행관리

        Args:
            messages: 메시지목록

        Returns:
            유형텍스트
        """

        try:
            # 전송 API
            request_body = {"messages": messages}
            response = requests.post(API_URL, json=request_body)
            response.raise_for_status()  # 조회요청 여부성공

            # 반환유형완료의돌아가기복사
            response_json = response.json()

            # 내용 형식
            if "data" in response_json and "choices" in response_json["data"]:
                # 새형식
                return response_json["data"]["choices"][0]["message"]["content"]
            elif "choices" in response_json:
                # 기존형식
                return response_json["choices"][0]["message"]["content"]
            else:
                raise ValueError("지원하지 않는의형식")

        except requests.exceptions.RequestException as e:
            logger.info(f"요청 오류: {e}")
            return None
        except KeyError:
            logger.info("형식아니오정상")
            return None

    def execute_action(self, action_response, image_height, image_width) -> tuple:
        """
        실행, 저장클릭사용아래일스크린샷의

        Args:
            action: 딕셔너리또는목록
            image_height: 스크린샷높이정도
            image_width: 스크린샷너비정도

        Returns:
            여부실행성공(작업완료)
        """
        try:
            # 관리JSON형식
            cleaned_str = action_response.strip()
            cleaned_str = cleaned_str.replace("```json", "").replace("```JSON", "")
            cleaned_str = cleaned_str.replace("```", "").strip()

            # 파싱JSON
            actions = json.loads(cleaned_str)
            if not isinstance(actions, list):
                actions = [actions]

            thought, param = "", ""

            # 실행매개
            for action in actions:
                action_type = action.get("type")
                thought = action.get("thought")
                param = action.get("param", {})

                logger.debug("Executing CUA action type=%s thought=%s", action_type, thought)

                # 근거유형실행아니오의
                if action_type == "click":
                    # 관리클릭
                    point = param.get("point", [0, 0])
                    button = param.get("button", "left")
                    clicks = param.get("clicks", 1)
                    click_type = param.get("type", "click")

                    # 확인예정수
                    x, y = int(point[0] / 1000 * image_width), int(point[1] / 1000 * image_height)

                    if click_type == "down":
                        # 아래마우스
                        pyautogui.mouseDown(x, y, duration=0.5, button=button)
                    elif click_type == "up":
                        # 마우스
                        pyautogui.mouseUp(x, y, duration=0.5, button=button)
                    elif clicks == 2:
                        # 더블클릭
                        pyautogui.doubleClick(x, y, duration=0.5, button=button)
                    else:
                        # 단일
                        pyautogui.click(x, y, duration=0.5, clicks=clicks, button=button)

                elif action_type == "input":
                    # 관리입력
                    value = param.get("value", "")
                    point = param.get("point", [0, 0])

                    # 확인예정수
                    x, y = int(point[0] / 1000 * image_width), int(point[1] / 1000 * image_height)

                    # 클릭입력란위치
                    pyautogui.click(x, y, duration=0.5)
                    time.sleep(0.2)

                    pyperclip.copy(value)
                    pyautogui.hotkey("ctrl", "v")

                elif action_type == "drag":
                    # 관리
                    start_point = param.get("start_point", [0, 0])
                    end_point = param.get("end_point", [0, 0])

                    # 확인예정수
                    start_x, start_y = (
                        int(start_point[0] / 1000 * image_width),
                        int(start_point[1] / 1000 * image_height),
                    )
                    end_x, end_y = int(end_point[0] / 1000 * image_width), int(end_point[1] / 1000 * image_height)

                    pyautogui.moveTo(start_x, start_y, duration=0.5)
                    pyautogui.dragTo(end_x, end_y, duration=0.5)

                elif action_type == "wait":
                    # 관리대기
                    time_ms = param.get("time_ms", 1000)
                    time.sleep(time_ms / 1000.0)

                elif action_type == "hotkey":
                    # 관리
                    hotkey_value = param.get("value", "")

                    # 파싱그룹합치기
                    # 지원형식: "^{a}" (Ctrl+A), "^+{s}" (Ctrl+Shift+S), "{ENTER}" (Enter)대기

                    # 관리형식의
                    if "{" in hotkey_value and "}" in hotkey_value:
                        # 가져오기 값
                        key = hotkey_value.split("{")[-1].split("}")[0]

                        # 가져오기 
                        modifiers = []
                        if "^" in hotkey_value:
                            modifiers.append("ctrl")
                        if "!" in hotkey_value:
                            modifiers.append("alt")
                        if "+" in hotkey_value:
                            modifiers.append("shift")

                        if modifiers:
                            # 그룹합치기
                            modifiers.append(key)
                            pyautogui.hotkey(*modifiers)
                        else:
                            # 단일개
                            pyautogui.press(key)
                    else:
                        # 통신그룹합치기, 예: ctrl+c
                        keys = hotkey_value.split("+")
                        pyautogui.hotkey(*keys)

                elif action_type == "hover":
                    # 관리중지
                    point = param.get("point", [0, 0])
                    x, y = int(point[0] / 1000 * image_width), int(point[1] / 1000 * image_height)
                    pyautogui.moveTo(x, y, duration=0.5)

                elif action_type == "scroll":
                    # 관리
                    direction = param.get("direction", "down")
                    wheel_times = param.get("wheel_times", 1)
                    point = param.get("point", None)

                    # 계획량
                    scroll_amount = wheel_times * 100 if direction == "up" else -wheel_times * 100

                    if point:
                        x, y = int(point[0] / 1000 * image_width), int(point[1] / 1000 * image_height)
                        pyautogui.scroll(scroll_amount, x, y)
                    else:
                        pyautogui.scroll(scroll_amount)

                elif action_type == "data":
                    # 관리데이터반환
                    data = param.get("data", None)
                    logger.debug("CUA action returned data of type %s", type(data).__name__)
                    return True, thought, param
                    # 데이터 유형아니오필요관리, 필요기록

                elif action_type == "finished":
                    # 관리완료
                    logger.debug("CUA action sequence finished")
                    return True, thought, param

                elif action_type == "error":
                    # 관리오류 
                    reason = param.get("reason", "지원하지 않는오류")
                    logger.warning("CUA action reported an error: %s", reason)
                    # 근거 prompt 중의설명, 까지 error 시해당종료작업
                    return True, thought, param

                else:
                    logger.warning("Unsupported CUA action type: %s", action_type)

                # 매개후대기일소시간
                time.sleep(0.5)

            return False, thought, param  # 완료되지 않은, 계속

        except Exception as e:
            logger.exception("Failed to execute CUA action")
            return False, "", e

    def run(self, instruction: str) -> dict:
        """
        실행작업
        Args:
            instruction: 사용자

        Returns:
            실행 결과딕셔너리
        """
        logger.info(f"{'=' * 60}")
        logger.info(f"[작업열기 ] {instruction}")
        logger.info(f"{'=' * 60}\n")

        step = 0
        thought = ""
        param = ""
        start_time = time.time()

        # PyAutoGUI설치전체
        current_failsafe = pyautogui.FAILSAFE
        current_pause = pyautogui.PAUSE
        pyautogui.FAILSAFE = False  # 마우스까지왼쪽위역할트리거예외중지
        pyautogui.PAUSE = 0.5  # 매개일시중지0.5초

        try:
            while step < self.max_steps:
                step += 1
                logger.info(f"[ {step}/{self.max_steps}]")
                logger.info("-" * 60)

                # 1. 스크린샷(실행후의새상태)
                screenshot_path, base64_image = self.take_screenshot()
                self.screenshots.append(screenshot_path)

                # 결과가있음대기저장의, 에서저장(원인로있음완료새의스크린샷)
                if self.pending_response:
                    image_format = Path(screenshot_path).suffix[1:] or "png"
                    self.conversation_history.append(
                        (
                            self.pending_response,  # 위일의
                            (base64_image, image_format),  # 현재의스크린샷(실행후의새상태)
                        )
                    )
                    self.pending_response = None

                # 2. 생성메시지호출유형
                logger.info("유형분중...")
                messages = self.build_messages(instruction, screenshot_path, base64_image)

                response = self.inference(messages)
                logger.info(response)

                # 저장, 대기다음 단계의스크린샷일저장까지
                self.pending_response = response

                # 직선연결사용저장의화면, 매열기Image파일
                image_width, image_height = self.screen_width, self.screen_height

                # 3. 실행
                logger.info("실행...")

                is_finished, thought, param = self.execute_action(response, image_height, image_width)

                if is_finished:
                    logger.info("=" * 60)
                    logger.info("[작업성공완료]")
                    logger.info(f"데이터: {step}")
                    logger.info(f"시: {time.time() - start_time:.2f}초")
                    logger.info("=" * 60)
                    return {
                        "success": True,
                        "steps": step,
                        "duration": time.time() - start_time,
                        "screenshots": self.screenshot_dir,
                        "thought": thought,
                        "data": param,
                    }

                # 대기
                logger.info("대기...")
                time.sleep(1)
            # 까지대데이터
            logger.info("=" * 60)
            logger.info("[작업완료되지 않은] 완료까지대데이터제한")
            logger.info(f"데이터: {step}")
            logger.info(f"시: {time.time() - start_time:.2f}초")
            logger.info("=" * 60)
            return {
                "success": False,
                "steps": step,
                "duration": time.time() - start_time,
                "screenshots": self.screenshot_dir,
                "error": "까지대데이터제한",
                "thought": thought,
                "data": param,
            }
        except KeyboardInterrupt:
            logger.info("\n\n[작업중] 사용자중지")
            return {
                "success": False,
                "steps": step,
                "duration": time.time() - start_time,
                "screenshots": self.screenshot_dir,
                "error": "사용자중",
                "thought": thought,
                "data": param,
            }
        except Exception as e:
            logger.info(f"\n\n[작업 실패] 오류: {e}")
            return {
                "success": False,
                "steps": step,
                "duration": time.time() - start_time,
                "screenshots": self.screenshot_dir,
                "error": str(e),
                "thought": thought,
                "data": param,
            }
        finally:
            # PyAutoGUI설치전체
            pyautogui.FAILSAFE = current_failsafe  # 마우스까지왼쪽위역할트리거예외중지
            pyautogui.PAUSE = current_pause  # 매개일시중지0.5초
