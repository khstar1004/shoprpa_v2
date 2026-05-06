"""
Computer Use Agent - 사용대유형
의자동화 프로세스: 사용자 → 스크린샷 → 유형분 → 실행 → 직선까지작업완료
"""

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
from astronverse.cua.action_parser import (
    parse_action_to_structure_output,
    parsing_response_to_pyautogui_code,
)
from astronverse.cua.custom_action_screen import CustomActionScreen
from PIL import Image, ImageDraw

#  GUI 작업의안내
COMPUTER_USE_PROMPT = """You are a GUI agent. You are given a task and your action history, with screenshots. You need to perform the next action to complete the task.

## Output Format
```
Thought: ...
Action: ...
```

## Action Space
click(point='<point>x1 y1</point>')
left_double(point='<point>x1 y1</point>')
right_single(point='<point>x1 y1</point>')
drag(start_point='<point>x1 y1</point>', end_point='<point>x2 y2</point>')
hotkey(key='ctrl c') # Split keys with a space and use lowercase. Also, do not use more than 3 keys in one hotkey action.
type(content='xxx') # Use escape characters \\', \\\", and \\n in content part to ensure we can parse the content in normal python string format. If you want to submit your input, use \\n at the end of content. 
scroll(point='<point>x1 y1</point>', direction='down or up or right or left') # Show more information on the `direction` side.
wait() #Sleep for 5s and take a screenshot to check for any changes.
finished(content='xxx') # Use escape characters \\', \\", and \\n in content part to ensure we can parse the content in normal python string format.

## Note
- Use Chinese in `Thought` part.
- Write a small plan and finally summarize your next action (with its target element) in one sentence in `Thought` part.

## User Instruction
{instruction}
"""

API_URL = "http://127.0.0.1:{}/api/rpa-ai-service/cua/chat".format(
    atomicMg.cfg().get("GATEWAY_PORT") if atomicMg.cfg().get("GATEWAY_PORT") else "13159"
)


class ComputerUseAgent:
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
        self.action_history: list[dict] = []
        self.screenshots: list[str] = []
        # 저장: (assistant, (base64_image, image_format) 또는 None)
        self.conversation_history: list[tuple[str, Optional[tuple[str, str]]]] = []
        self.pending_response: Optional[str] = None  # 대기저장의(대기다음 단계의스크린샷)
        self.instruction: Optional[str] = None  # 저장사용자

        # 저장위일클릭의, 사용에서아래일스크린샷위
        self.last_click_coords: Optional[tuple[int, int]] = None

        # 화면저장, 매열기Image파일
        self.screen_width = None
        self.screen_height = None
        self.max_screenshots = 5

        # 없음action계획데이터기기, 사용있음action의데이터
        self.consecutive_no_action = 0

        # action
        self.last_action = None  # 저장위일의action
        self.consecutive_same_action = 1  # action의데이터

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

        # 결과가있음위일클릭, 에서스크린샷위
        final_screenshot_path = str(screenshot_path)
        if self.last_click_coords:
            x, y = self.last_click_coords
            final_screenshot_path = self.mark_click_on_image(str(screenshot_path), x, y)

        # 코드로Base64(사용후의이미지)
        with open(final_screenshot_path, "rb") as f:
            base64_image = base64.b64encode(f.read()).decode("utf-8")

        return final_screenshot_path, base64_image

    @staticmethod
    def mark_click_on_image(image_path: str, x: int, y: int, radius: int = 20) -> str:
        """
        에서스크린샷위제어색상, 테이블위일클릭의위치

        Args:
            image_path: 스크린샷파일 경로
            x: 클릭의X(화면)
            y: 클릭의Y(화면)
            radius: 경로(이미지)

        Returns:
            후의이미지경로
        """
        try:
            # 열기이미지
            img = Image.open(image_path)
            draw = ImageDraw.Draw(img)

            # 제어색상
            draw.ellipse([x - radius, y - radius, x + radius, y + radius], fill="red", outline="white", width=3)

            # 제어외부
            draw.ellipse([x - radius - 10, y - radius - 10, x + radius + 10, y + radius + 10], outline="red", width=3)

            # 제어십문자
            line_length = radius + 15
            draw.line([x - line_length, y, x + line_length, y], fill="red", width=2)
            draw.line([x, y - line_length, x, y + line_length], fill="red", width=2)

            # 저장후의이미지
            marked_path = str(Path(image_path).parent / f"marked_{Path(image_path).name}")
            img.save(marked_path)

            return marked_path
        except Exception as e:
            return image_path  # 결과가출력오류, 반환기존경로

    @staticmethod
    def extract_click_coordinates(action: dict, image_height: int, image_width: int) -> Optional[tuple[int, int]]:
        """
        에서중가져오기클릭

        Args:
            action: 딕셔너리
            image_height: 스크린샷높이정도
            image_width: 스크린샷너비정도

        Returns:
            화면 (x, y), 결과가아니오예클릭이면반환None
        """
        action_type = action.get("action_type", "")
        action_inputs = action.get("action_inputs", {})

        # 조회여부예클릭유형
        click_actions = ["click", "left_single", "left_double", "right_single"]
        if action_type not in click_actions:
            return None

        # 가져오기 
        start_box = action_inputs.get("start_box")
        if not start_box:
            return None

        try:
            # 파싱()
            start_box = eval(str(start_box))
            if len(start_box) == 4:
                x1, y1, x2, y2 = start_box
            elif len(start_box) == 2:
                x1, y1 = start_box
                x2 = x1
                y2 = y1
            else:
                return None

            # 변환로화면
            x = round(float((x1 + x2) / 2) * image_width)
            y = round(float((y1 + y2) / 2) * image_height)

            return (x, y)
        except Exception as e:
            logger.info(f"[경고] 불가파싱: {e}")
            return None

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
        system_prompt = COMPUTER_USE_PROMPT.format(instruction=self.instruction)

        messages: list[dict] = []

        # 일: 있음system_prompt및일스크린샷
        if not self.conversation_history:
            messages = [
                {"role": "user", "content": system_prompt},
                {
                    "role": "user",
                    "content": [
                        {"type": "image_url", "image_url": {"url": f"data:image/{image_format};base64,{base64_image}"}}
                    ],
                },
            ]
        else:
            # 후: 형식추가
            messages.append({"role": "user", "content": system_prompt})

            # 추가: assistant + 의스크린샷()
            for assistant_response, screenshot_info in self.conversation_history:
                # 추가assistant의
                messages.append({"role": "assistant", "content": assistant_response})

                # 결과가스크린샷정보존재함, 추가의스크린샷(user메시지)
                if screenshot_info is not None:
                    msg_image, msg_format = screenshot_info
                    messages.append(
                        {
                            "role": "user",
                            "content": [
                                {
                                    "type": "image_url",
                                    "image_url": {"url": f"data:image/{msg_format};base64,{msg_image}"},
                                }
                            ],
                        }
                    )

            # 추가현재스크린샷
            messages.append(
                {
                    "role": "user",
                    "content": [
                        {"type": "image_url", "image_url": {"url": f"data:image/{image_format};base64,{base64_image}"}}
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
            # 전송 API 요청 
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

    def limit_screenshots_in_history(self) -> None:
        """
        제한중의스크린샷수, 다중보관max_screenshots-1개스크린샷
        """
        screenshots_count = sum(1 for _, screenshot_info in self.conversation_history if screenshot_info is not None)

        max_screenshots_in_history = self.max_screenshots - 1
        if screenshots_count >= max_screenshots_in_history:
            for i, (assistant_response, screenshot_info) in enumerate(self.conversation_history):
                if screenshot_info is not None:
                    self.conversation_history[i] = (assistant_response, None)
                    break

    def execute_action(self, action, image_height, image_width) -> bool:
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
            # 관리action가능예목록의
            action_to_process = action
            if isinstance(action, list) and len(action) > 0:
                action_to_process = action[0]  # 가져오기 일개action사용가져오기 
            elif isinstance(action, list) and len(action) == 0:
                return False

            # 에서실행전가져오기클릭(결과가예클릭)
            if isinstance(action_to_process, dict):
                click_coords = self.extract_click_coordinates(action_to_process, image_height, image_width)
                if click_coords:
                    self.last_click_coords = click_coords

            py_code = parsing_response_to_pyautogui_code(action, image_height, image_width)

            if py_code == "DONE":
                return True

            # 생성실행
            exec_globals = {"pyautogui": pyautogui, "time": time, "pyperclip": pyperclip}

            # 실행코드
            exec(py_code, exec_globals)

            # 대기완료
            time.sleep(0.5)

            return False  # 완료되지 않은, 계속

        except Exception as e:
            logger.info(f"[오류] 실행시출력오류: {e}")
            import traceback

            traceback.logger.info_exc()
            return False

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
        action_step = 0
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
                    # 에서추가새전, 관리의스크린샷으로제한수
                    self.limit_screenshots_in_history()

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

                # 3. 파싱
                # 직선연결사용저장의화면, 매열기Image파일
                image_width, image_height = self.screen_width, self.screen_height
                action = parse_action_to_structure_output(
                    response, 1000, image_height, image_width, model_type="doubao"
                )
                if not action:
                    # 업데이트없음action계획데이터기기
                    self.consecutive_no_action += 1

                    # 결과가없음action, 빈, 보관열기 의system_prompt
                    if self.consecutive_no_action >= 3:
                        self.conversation_history.clear()
                        self.pending_response = None
                        self.consecutive_no_action = 0

                    continue
                else:
                    # 있음있음, 재없음action계획데이터기기
                    self.consecutive_no_action = 0
                    action_step += 1

                    # 조회action
                    current_action_key = (action[0].get("action_type"), action[0].get("action_inputs"))
                    if current_action_key == self.last_action:
                        self.consecutive_same_action += 1

                        if self.consecutive_same_action >= 3:
                            self.conversation_history.clear()
                            self.pending_response = None
                            self.consecutive_same_action = 1
                            self.consecutive_no_action = 0
                            self.last_action = None
                            continue
                    else:
                        self.consecutive_same_action = 1
                        self.last_action = current_action_key

                # 4. 실행
                logger.info("실행...")

                is_finished = self.execute_action(action, image_height, image_width)

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
            }
        except KeyboardInterrupt:
            logger.info("\n\n[작업중] 사용자중지")
            return {
                "success": False,
                "steps": step,
                "duration": time.time() - start_time,
                "screenshots": self.screenshot_dir,
                "error": "사용자중",
            }
        except Exception as e:
            logger.info(f"\n\n[작업실패] 발송오류: {e}")
            return {
                "success": False,
                "steps": step,
                "duration": time.time() - start_time,
                "screenshots": self.screenshot_dir,
                "error": str(e),
            }
        finally:
            # PyAutoGUI설치전체
            pyautogui.FAILSAFE = current_failsafe  # 마우스까지왼쪽위역할트리거예외중지
            pyautogui.PAUSE = current_pause  # 매개일시중지0.5초


class ComputerUse:
    """Computer Use Agent패키지설치유형, 사용기존가능회원가입"""

    @staticmethod
    @atomicMg.atomic(
        "ComputerUse",
        inputList=[
            atomicMg.param("instruction", types="Str"),
            atomicMg.param("max_steps", types="Int", required=False),
            atomicMg.param("temperature", types="Float", required=False),
        ],
        outputList=[
            atomicMg.param("computer_use_res", types="Dict"),
        ],
    )
    def run(
        instruction: str,
        max_steps: int = 20,
        temperature: float = 0.0,
    ):
        """
        실행계획기기사용관리작업

        Args:
            instruction: 사용자
            max_steps: 대실행데이터
            temperature: 유형정도매개변수

        Returns:
            실행 결과, 패키지success, steps, action_steps, duration, screenshots, error대기필드
        """

        agent = ComputerUseAgent(
            max_steps=max_steps,
            temperature=temperature,
        )
        result = agent.run(instruction)

        # 반환결과, 확인모든출력매개변수 있음값
        return {
            "success": result.get("success", False),
            "steps": result.get("steps", 0),
            "duration": result.get("duration", 0.0),
            "screenshots": result.get("screenshots", []),
            "error": result.get("error", ""),
        }

    @staticmethod
    @atomicMg.atomic(
        "ComputerUse",
        inputList=[
            atomicMg.param("instruction", types="Str"),
            atomicMg.param("max_steps", types="Int", required=False),
            atomicMg.param("temperature", types="Float", required=False),
        ],
        outputList=[
            atomicMg.param("computer_use_res", types="Dict"),
        ],
    )
    def custom_action_screen(
        instruction: str,
        max_steps: int = 20,
        temperature: float = 0.0,
    ):
        """
        지정AI화면

        Args:
            instruction: 사용자
            max_steps: 대실행데이터
            temperature: 유형정도매개변수

        Returns:
            실행 결과, 패키지success, steps, action_steps, duration, screenshots, error대기필드
        """

        agent = CustomActionScreen(
            max_steps=max_steps,
            temperature=temperature,
        )
        result = agent.run(instruction)

        # 반환결과, 확인모든출력매개변수 있음값
        return {
            "success": result.get("success", False),
            "steps": result.get("steps", 0),
            "duration": result.get("duration", 0.0),
            "screenshots": result.get("screenshots", []),
            "error": result.get("error", ""),
            "thought": result.get("thought", ""),
            "data": result.get("data", ""),
        }

    @staticmethod
    @atomicMg.atomic(
        "ComputerUse",
        inputList=[
            atomicMg.param("instruction", types="Str", default="에서화면중가져오기데이터, 반환 JSON 형식."),
            atomicMg.param("max_steps", types="Int", required=False),
            atomicMg.param("temperature", types="Float", required=False),
        ],
        outputList=[
            atomicMg.param("computer_use_res", types="Dict"),
        ],
    )
    def extract_data(
        instruction: str,
        max_steps: int = 1,
        temperature: float = 0.0,
    ):
        """
        가져오기화면데이터

        Args:
            instruction: 사용자
            max_steps: 대실행데이터
            temperature: 유형정도매개변수

        Returns:
            실행 결과, 패키지success, steps, action_steps, duration, screenshots, error대기필드
        """

        agent = CustomActionScreen(
            max_steps=max_steps,
            temperature=temperature,
        )
        result = agent.run(instruction)

        # 반환결과, 확인모든출력매개변수 있음값
        return {
            "success": result.get("success", False),
            "steps": result.get("steps", 0),
            "duration": result.get("duration", 0.0),
            "screenshots": result.get("screenshots", []),
            "error": result.get("error", ""),
            "thought": result.get("thought", ""),
            "data": result.get("data", ""),
        }

    @staticmethod
    @atomicMg.atomic(
        "ComputerUse",
        inputList=[
            atomicMg.param("instruction", types="Str", default="를 [데이터내용] 까지화면중의테이블단일.데이터내용: "),
            atomicMg.param("max_steps", types="Int", required=False),
            atomicMg.param("temperature", types="Float", required=False),
        ],
        outputList=[
            atomicMg.param("computer_use_res", types="Dict"),
        ],
    )
    def fill_form(
        instruction: str,
        max_steps: int = 20,
        temperature: float = 0.0,
    ):
        """
        테이블단일

        Args:
            instruction: 사용자
            max_steps: 대실행데이터
            temperature: 유형정도매개변수

        Returns:
            실행 결과, 패키지success, steps, action_steps, duration, screenshots, error대기필드
        """

        agent = CustomActionScreen(
            max_steps=max_steps,
            temperature=temperature,
        )
        result = agent.run(instruction)

        # 반환결과, 확인모든출력매개변수 있음값
        return {
            "success": result.get("success", False),
            "steps": result.get("steps", 0),
            "duration": result.get("duration", 0.0),
            "screenshots": result.get("screenshots", []),
            "error": result.get("error", ""),
            "thought": result.get("thought", ""),
            "data": result.get("data", ""),
        }

    @staticmethod
    @atomicMg.atomic(
        "ComputerUse",
        inputList=[
            atomicMg.param("instruction", types="Str", default="관리닫기화면중의인증 코드."),
            atomicMg.param("max_steps", types="Int", required=False),
            atomicMg.param("temperature", types="Float", required=False),
        ],
        outputList=[
            atomicMg.param("computer_use_res", types="Dict"),
        ],
    )
    def process_captcha(
        instruction: str,
        max_steps: int = 20,
        temperature: float = 0.0,
    ):
        """
        관리인증 코드

        Args:
            instruction: 사용자
            max_steps: 대실행데이터
            temperature: 유형정도매개변수

        Returns:
            실행 결과, 패키지success, steps, action_steps, duration, screenshots, error대기필드
        """

        agent = CustomActionScreen(
            max_steps=max_steps,
            temperature=temperature,
        )
        result = agent.run(instruction)

        # 반환결과, 확인모든출력매개변수 있음값
        return {
            "success": result.get("success", False),
            "steps": result.get("steps", 0),
            "duration": result.get("duration", 0.0),
            "screenshots": result.get("screenshots", []),
            "error": result.get("error", ""),
            "thought": result.get("thought", ""),
            "data": result.get("data", ""),
        }