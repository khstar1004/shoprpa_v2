import ast
import copy
import os
import shutil
import threading
import time
from collections.abc import Callable
from typing import Optional

from astronverse.actionlib import AtomicFormType, AtomicFormTypeMeta, DynamicsItem
from astronverse.actionlib.atomic import atomicMg
from astronverse.ai import LLMModelTypes
from astronverse.ai.api.llm import DEFAULT_MODEL, chat_normal, chat_streamable
from astronverse.ai.error import *
from astronverse.ai.prompt.g_chat import prompt_generate_question
from astronverse.ai.utils.extract import FileExtractor
from astronverse.ai.utils.str import replace_keyword


def wait_with_timeout(check_done: Callable[[], bool], reset_timeout_on_activity: bool, wait_time: int):
    """
    대기대화상자결과, 지원사용자감지및시간 초과보관

    Args:
        check_done: 돌아가기조정데이터, 반환 True 테이블완료, False 테이블계속대기
        reset_timeout_on_activity: 여부에서감지까지사용자(마우스)시재시간 초과계획시기기
        wait_time: 시간 초과시간(초)
    """
    from pynput.mouse import Controller as MouseController

    mouse_controller: Optional[MouseController] = None
    if reset_timeout_on_activity:
        mouse_controller = MouseController()

    last_pos = None
    start = time.time()
    while not check_done():
        if reset_timeout_on_activity and mouse_controller:
            pos = mouse_controller.position
            if pos != last_pos:
                last_pos = pos
                start = time.time()
        if time.time() - start > wait_time:
            break
        time.sleep(0.1)


def require_chat_ws():
    ws = atomicMg.cfg().get("WS", None)
    if not ws:
        raise RuntimeError("AI 채팅 UI 연결이 없습니다. ShopRPA 실행 환경에서 다시 시도하세요.")
    return ws


class ChatAI:
    """Chat interaction utilities: single turn, multi-turn, and knowledge-based chat."""

    @staticmethod
    @atomicMg.atomic(
        "ChatAI",
        inputList=[
            atomicMg.param(
                "custom_model",
                dynamics=[
                    DynamicsItem(
                        key="$this.custom_model.show",
                        expression="return $this.model.value == '{}'".format(LLMModelTypes.CUSTOM_MODEL.value),
                    )
                ],
            )
        ],
        outputList=[atomicMg.param("single_chat_res", types="Str")],
    )
    def single_turn_chat(query: str, model: LLMModelTypes = LLMModelTypes.DEEPSEEK_V3_2, custom_model: str = "") -> str:
        """
        단일방법법
        Args:
            - query(str): 사용자제목
        Return:
            `str`, 대유형완료의
        """
        if model == LLMModelTypes.CUSTOM_MODEL and custom_model:
            model = custom_model
        else:
            model = model.value
        return chat_normal(user_input=query, system_input="", model=model)

    @staticmethod
    @atomicMg.atomic(
        "ChatAI",
        inputList=[
            atomicMg.param(
                "custom_model",
                dynamics=[
                    DynamicsItem(
                        key="$this.custom_model.show",
                        expression="return $this.model.value == '{}'".format(LLMModelTypes.CUSTOM_MODEL.value),
                    )
                ],
            )
        ],
        outputList=[atomicMg.param("chat_res", types="Dict")],
    )
    def chat(
        is_save: bool,
        title: str,
        max_turns: int,
        model: LLMModelTypes = LLMModelTypes.DEEPSEEK_V3_2,
        custom_model: str = "",
    ) -> dict:
        """
        다중방법법
        Args:
            - is_save(bool): 사용여부필요저장후의내보내기
            - title(title): 제목이름
            - max_turns(int): 대의데이터
        Return:
            `dict`, 선택내보내기의기록
        """

        if model == LLMModelTypes.CUSTOM_MODEL and custom_model:
            model = custom_model
        else:
            model = model.value

        done = threading.Event()
        res = {}
        res_e = None

        def callback_func(watch_msg, e: Exception = None):
            nonlocal done, res, res_e
            if watch_msg:
                res = watch_msg.data
            if e:
                res_e = e
            done.set()

        ws = require_chat_ws()
        params = {
            "max_turns": str(max_turns),
            "is_save": str(int(is_save)),
            "title": title,
            "model": model,
        }
        ws.send_reply({"data": {"name": "multichat", "params": params, "height": 600}}, 600, callback_func)

        done.wait()
        if res_e:
            raise Exception(res_e)

        return res

    @staticmethod
    def _extract_file_content(file_path: str) -> str:
        """가져오기파일내용"""
        _, extension = os.path.splitext(file_path)

        if "pdf" in extension.lower():
            return FileExtractor.extract_pdf(file_path)
        elif "docx" in extension.lower():
            return FileExtractor.extract_docx(file_path)
        else:
            raise NotImplementedError(f"Not support file type: {extension}")

    @staticmethod
    def _generate_questions(file_content: str) -> list:
        """완료제목목록"""
        inputs = replace_keyword(
            prompts=copy.deepcopy(prompt_generate_question),
            input_keys=[{"keyword": "text", "text": file_content[:18000]}],
        )
        content, _ = ChatAI.streamable_response(inputs)
        s_content = "".join(content).replace(", ", ",")

        try:
            output = ast.literal_eval(s_content)
        except (ValueError, SyntaxError):
            output = [
                "텍스트의제목예?",
                "텍스트중까지완료닫기 정보?",
                "텍스트까지완료의결과?",
            ]
        return output

    @staticmethod
    def _setup_file_cache(file_path: str) -> str:
        """파일저장"""
        word_dir = os.path.join("cache", "chatData")
        cache_file = os.path.join(word_dir, os.path.basename(file_path))

        if not os.path.exists(word_dir):
            os.makedirs(word_dir)
        if os.path.exists(cache_file):
            os.remove(cache_file)
        shutil.copy2(file_path, cache_file)

        return cache_file

    @staticmethod
    @atomicMg.atomic(
        "ChatAI",
        inputList=[
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "file"},
                ),
            ),
        ],
        outputList=[atomicMg.param("knowledge_chat_res", types="Dict")],
    )
    def knowledge_chat(
        file_path: str,
        is_save: bool = False,
        max_turns: int = 20,
    ):
        """
        알림라이브러리
        Args:
            - file_path(str): 파일 경로
            - is_save(bool): 사용여부필요저장후의내보내기
            - max_turns(int): 대의데이터

        Return:
            `dict`, 선택내보내기의기록
        """
        ws = require_chat_ws()

        # 가져오기파일내용
        file_content = ChatAI._extract_file_content(file_path)

        # 완료제목
        output = ChatAI._generate_questions(file_content)

        # 파일저장
        dest_file = ChatAI._setup_file_cache(file_path)

        # 창
        done = threading.Event()
        res = {}
        res_e = None

        def callback_func(watch_msg, e: Exception = None):
            nonlocal done, res, res_e
            if watch_msg:
                res = watch_msg.data
            if e:
                res_e = e
            done.set()

        try:
            params = {
                "max_turns": str(max_turns),
                "is_save": str(int(is_save)),
                "questions": "$-$".join(output),
                "file_path": file_path,
            }
            ws.send_reply(
                {"data": {"name": "multichat", "params": params, "content": file_content[:5000], "height": 700}},
                600,
                callback_func,
            )

            done.wait()
        finally:
            # 임시 채팅 파일은 UI 오류가 나도 남기지 않습니다.
            if os.path.exists(dest_file):
                os.remove(dest_file)

        if res_e:
            raise Exception(res_e)
        return res

    @staticmethod
    def streamable_response(inputs: list, model: str = DEFAULT_MODEL):
        """Stream model responses accumulating content and reasoning lists.

        Args:
            inputs (list): chat message list [{'role': str, 'content': str}, ...]
            model(str): default model name
        Returns:
            tuple[list[str], list[str]]: (content tokens, reasoning tokens)
        """
        content: list[str] = []
        reason: list[str] = []
        for item in chat_streamable(inputs, model):
            # if item.get("content"):
            #     content.append(item.get("content"))
            # if item.get("reasoning_content"):
            #     reason.append(item.get("reasoning_content"))
            content.append(item)
        return content, reason
