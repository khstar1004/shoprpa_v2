import random
import time

import pyautogui
from astronverse.actionlib import AtomicFormType, AtomicFormTypeMeta, AtomicLevel, DynamicsItem
from astronverse.actionlib.atomic import atomicMg
from astronverse.actionlib.types import IMGPick
from astronverse.input import MoveType, Simulate_flag, Speed
from astronverse.input.code.clipboard import Clipboard
from astronverse.input.code.keyboard import Keyboard
from astronverse.input.code.mouse import Mouse
from astronverse.input.error import *
from astronverse.vision import *
from astronverse.vision.core import CvCore
from astronverse.vision.error import *

# 지정입력법의코드
ENGLISH = 0x0409  # 영어()
CHINESE = 0x0804  # 중국어(, 중)
speed_to_int = {Speed.SLOW: 2, Speed.NORMAL: 1, Speed.FAST: 0.5}


class CV:
    @staticmethod
    @atomicMg.atomic(
        "CV",
        inputList=[
            atomicMg.param(
                "input_data",
                formType=AtomicFormTypeMeta(AtomicFormType.PICK.value, params={"use": "CV"}),
                noInput=True,
            ),
            atomicMg.param("btn_type", required=False),
            atomicMg.param("btn_model", required=False),
            atomicMg.param("click_position", required=False),
            atomicMg.param(
                "specified_position",
                required=False,
                formType=AtomicFormTypeMeta(AtomicFormType.GRID.value),
                dynamics=[
                    DynamicsItem(
                        key="$this.specified_position.show",
                        expression="return $this.click_position.value == '{}'".format(PositionType.SPECIFIC.value),
                    )
                ],
            ),
            atomicMg.param(
                "horizontal_move",
                types="Int",
                dynamics=[
                    DynamicsItem(
                        key="$this.horizontal_move.show",
                        expression="return $this.click_position.value == '{}'".format(PositionType.SPECIFIC.value),
                    )
                ],
            ),
            atomicMg.param(
                "vertical_move",
                types="Int",
                dynamics=[
                    DynamicsItem(
                        key="$this.vertical_move.show",
                        expression="return $this.click_position.value == '{}'".format(PositionType.SPECIFIC.value),
                    )
                ],
            ),
            atomicMg.param(
                "wait_time",
                types="Int",
                level=AtomicLevel.ADVANCED.value,
                required=False,
            ),
            atomicMg.param(
                "match_similarity",
                formType=AtomicFormTypeMeta(AtomicFormType.SLIDER.value),
                required=False,
            ),
            atomicMg.param("move_type", level=AtomicLevel.ADVANCED.value, required=False),
            atomicMg.param(
                "move_speed",
                level=AtomicLevel.ADVANCED.value,
                dynamics=[
                    DynamicsItem(
                        key="$this.move_speed.show",
                        expression="return ['{}','{}'].includes($this.move_type.value)".format(
                            MoveType.LINEAR.value, MoveType.SIMULATION.value
                        ),
                    )
                ],
                required=False,
            ),
        ],
        outputList=[],
    )
    def cv_click(
        input_data: IMGPick,
        btn_type: BtnType = BtnType.LEFT,
        btn_model: BtnModel = BtnModel.CLICK,
        click_position: PositionType = PositionType.CENTER,
        specified_position=5,
        horizontal_move: int = 0,
        vertical_move: int = 0,
        match_similarity: float = 0.95,
        move_type: MoveType = MoveType.LINEAR,
        move_speed: Speed = Speed.NORMAL,
        wait_time: int = 10,
    ):
        """
        마우스이미지 클릭
        :param input_data: 이미지
        :param btn_type: 마우스
        :param btn_model: 클릭방식
        :param click_position: 클릭위치
        :param specified_position: 위치 지정
        :param horizontal_move: 가로 이동
        :param vertical_move: 세로 이동
        :param match_similarity: 매칭 정확도
        :param wait_time: 대기시간
        :return: 빈
        """
        start_time = time.time()
        while True:
            target_rect = CvCore.match_imgs(input_data=input_data, match_similarity=match_similarity)
            if target_rect is not None:
                try:
                    if click_position == PositionType.CENTER:
                        target_x = target_rect[0] + target_rect[2] // 2
                        target_y = target_rect[1] + target_rect[3] // 2
                    elif click_position == PositionType.RANDOM:
                        target_x = target_rect[0] + random.randint(0, target_rect[2])
                        target_y = target_rect[1] + random.randint(0, target_rect[3])
                    elif click_position == PositionType.SPECIFIC:
                        position = specified_position
                        if position is None:
                            raise BaseException(SPECIFIC_POSITION_ERROR, "지정되지 않았습니다클릭위치, 확인하세요매개변수")
                        # 위치 지정계획클릭위치
                        target_x, target_y = CvCore.get_region_position(
                            target_rect, position, horizontal_move, vertical_move
                        )
                    else:
                        raise NotImplementedError()

                    screen_weight, screen_height = Mouse.screen_size()
                    if target_x < 0 or target_x > screen_weight or target_y < 0 or target_y > screen_height:
                        raise BaseException(REGION_ERROR, "이미지 클릭 좌표가 화면 범위를 벗어났습니다.")

                    if move_type == MoveType.LINEAR:
                        Mouse.move(
                            target_x,
                            target_y,
                            duration=speed_to_int[move_speed],
                            tween=pyautogui.linear,
                        )
                    elif move_type == MoveType.SIMULATION:
                        Mouse.move_simulate(
                            target_x,
                            target_y,
                            duration=speed_to_int[move_speed],
                            tween=pyautogui.easeInOutQuad,  # type: ignore
                        )
                    elif move_type == MoveType.TELEPORTATION:
                        Mouse.move(target_x, target_y, duration=0)
                    else:
                        raise NotImplementedError()

                    if btn_model == BtnModel.CLICK:
                        Mouse.click(None, None, 1, 0, btn_type.value)
                    elif btn_model == BtnModel.DOUBLE_CLICK:
                        Mouse.click(None, None, 2, 0, btn_type.value)
                    else:
                        raise NotImplementedError()

                    return True
                except Exception as e:
                    raise BaseException(MOUSE_CLICK_ERROR, "마우스클릭실패")
            else:
                if time.time() - start_time > wait_time:
                    break
                else:
                    time.sleep(0.1)

        raise BaseException(CV_MATCH_ERROR, "시간 초과일치하지 않는까지목록 요소, 확인하세요현재또는낮음매칭 정확도재시도")

    @staticmethod
    @atomicMg.atomic(
        "CV",
        inputList=[
            atomicMg.param(
                "input_data",
                formType=AtomicFormTypeMeta(AtomicFormType.PICK.value, params={"use": "CV"}),
                noInput=True,
            ),
            atomicMg.param("click_position", required=False),
            atomicMg.param(
                "specified_position",
                formType=AtomicFormTypeMeta(AtomicFormType.GRID.value),
                required=False,
                dynamics=[
                    DynamicsItem(
                        key="$this.specified_position.show",
                        expression="return $this.click_position.value == '{}'".format(PositionType.SPECIFIC.value),
                    )
                ],
            ),
            atomicMg.param(
                "horizontal_move",
                types="Int",
                dynamics=[
                    DynamicsItem(
                        key="$this.horizontal_move.show",
                        expression="return $this.click_position.value == '{}'".format(PositionType.SPECIFIC.value),
                    )
                ],
            ),
            atomicMg.param(
                "vertical_move",
                types="Int",
                dynamics=[
                    DynamicsItem(
                        key="$this.vertical_move.show",
                        expression="return $this.click_position.value == '{}'".format(PositionType.SPECIFIC.value),
                    )
                ],
            ),
            atomicMg.param(
                "match_similarity",
                formType=AtomicFormTypeMeta(AtomicFormType.SLIDER.value),
                required=False,
            ),
            atomicMg.param(
                "wait_time",
                types="Int",
                level=AtomicLevel.ADVANCED.value,
                required=False,
            ),
            atomicMg.param("move_type", level=AtomicLevel.ADVANCED.value, required=False),
            atomicMg.param(
                "move_speed",
                level=AtomicLevel.ADVANCED.value,
                dynamics=[
                    DynamicsItem(
                        key="$this.move_speed.show",
                        expression="return ['{}','{}'].includes($this.move_type.value)".format(
                            MoveType.LINEAR.value, MoveType.SIMULATION.value
                        ),
                    )
                ],
                required=False,
            ),
        ],
        outputList=[],
    )
    def hover_image(
        input_data: IMGPick,
        click_position: PositionType = PositionType.CENTER,
        specified_position=5,
        horizontal_move: int = 0,
        vertical_move: int = 0,
        match_similarity: float = 0.95,
        move_type: MoveType = MoveType.LINEAR,
        move_speed: Speed = Speed.NORMAL,
        wait_time: int = 10,
    ):
        """
        이미지 위로 마우스 이동
        :param input_data: 이미지
        :param click_position: 클릭위치
        :param specified_position: 위치 지정
        :param horizontal_move: 가로 이동
        :param vertical_move: 세로 이동
        :param match_similarity: 매칭 정확도
        :param wait_time: 대기시간
        :return: 빈
        """
        start_time = time.time()
        while True:
            target_rect = CvCore.match_imgs(input_data, match_similarity)
            if target_rect is not None:
                try:
                    if click_position == PositionType.CENTER:
                        target_x = target_rect[0] + target_rect[2] // 2
                        target_y = target_rect[1] + target_rect[3] // 2
                    elif click_position == PositionType.RANDOM:
                        target_x = target_rect[0] + random.randint(0, target_rect[2])
                        target_y = target_rect[1] + random.randint(0, target_rect[3])
                    elif click_position == PositionType.SPECIFIC:
                        position = specified_position
                        if position is None:
                            raise BaseException(SPECIFIC_POSITION_ERROR, "지정되지 않았습니다클릭위치, 확인하세요매개변수")
                        # 위치 지정계획클릭위치
                        target_x, target_y = CvCore.get_region_position(
                            target_rect, position, horizontal_move, vertical_move
                        )
                    else:
                        raise NotImplementedError()

                    screen_weight, screen_height = Mouse.screen_size()
                    if target_x < 0 or target_x > screen_weight or target_y < 0 or target_y > screen_height:
                        raise BaseException(REGION_ERROR, "이미지 마우스 이동 좌표가 화면 범위를 벗어났습니다.")

                    if move_type == MoveType.LINEAR:
                        Mouse.move(
                            target_x,
                            target_y,
                            duration=speed_to_int[move_speed],
                            tween=pyautogui.linear,
                        )
                    elif move_type == MoveType.SIMULATION:
                        Mouse.move_simulate(
                            target_x,
                            target_y,
                            duration=speed_to_int[move_speed],
                            tween=pyautogui.easeInOutQuad,  # type: ignore
                        )
                    elif move_type == MoveType.TELEPORTATION:
                        Mouse.move(target_x, target_y, duration=0)
                    else:
                        raise NotImplementedError()

                    return True
                except Exception as e:
                    raise BaseException(MOUSE_HOVER_ERROR, "마우스중지실패")
            else:
                if time.time() - start_time > wait_time:
                    break
                else:
                    time.sleep(0.1)

        raise BaseException(CV_MATCH_ERROR, "시간 초과일치하지 않는까지목록 요소, 확인하세요현재또는낮음매칭 정확도재시도")

    @staticmethod
    @atomicMg.atomic(
        "CV",
        inputList=[
            atomicMg.param(
                "input_data",
                formType=AtomicFormTypeMeta(AtomicFormType.PICK.value, params={"use": "CV"}),
                noInput=True,
            ),
            atomicMg.param("exist_type", required=False),
            atomicMg.param(
                "match_similarity",
                formType=AtomicFormTypeMeta(AtomicFormType.SLIDER.value),
                required=False,
            ),
            atomicMg.param(
                "wait_time",
                types="Int",
                level=AtomicLevel.ADVANCED.value,
                required=False,
            ),
        ],
    )
    def is_image_exist(
        input_data: IMGPick,
        exist_type: ExistType = ExistType.EXIST,
        match_similarity: float = 0.95,
        wait_time: int = 10,
    ):
        """
        이미지여부존재함
        :param input_data: 이미지
        :param exist_type: 유형
        :param match_similarity: 매칭 정확도
        :param wait_time: 대기시간
        :return: 이미지여부존재함의결과
        """
        start_time = time.time()

        while True:
            target_rect = CvCore.match_imgs(input_data, match_similarity)

            if exist_type == ExistType.EXIST:
                if target_rect is not None:
                    return True
            elif exist_type == ExistType.NOT_EXIST:
                if target_rect is None:
                    return True
            else:
                raise NotImplementedError()

            if time.time() - start_time > wait_time:
                break

            time.sleep(0.5)

        return False

    # @staticmethod
    # @atomicMg.atomic("CV")
    # def is_image_exist_end():
    #     pass

    @staticmethod
    @atomicMg.atomic(
        "CV",
        inputList=[
            atomicMg.param(
                "input_data",
                formType=AtomicFormTypeMeta(AtomicFormType.PICK.value, params={"use": "CV"}),
                noInput=True,
            ),
            atomicMg.param("wait_type", required=False),
            atomicMg.param("wait_time", types="Int", required=False),
            atomicMg.param(
                "match_similarity",
                formType=AtomicFormTypeMeta(AtomicFormType.SLIDER.value),
                required=False,
            ),
        ],
        outputList=[atomicMg.param("image_wait_result", types="Bool")],
    )
    def wait_image(
        input_data: IMGPick,
        wait_type: WaitType = WaitType.APPEAR,
        wait_time: int = 10,
        match_similarity: float = 0.95,
    ):
        """
        이미지 대기출력또는실패
        :param input_data: 이미지
        :param wait_type: 대기유형
        :param wait_time: 시간 초과시간
        :param match_similarity: 매칭 정확도
        :return: 대기결과
        """
        start_time = time.time()

        if wait_type == WaitType.DISAPPEAR:
            target_rect = CvCore.match_imgs(input_data, match_similarity)
            if not target_rect:
                raise BaseException(TARGET_EXISTS_ERROR, "현재요소를 찾을 수 없습니다, 불가실패상태")

        while True:
            target_rect = CvCore.match_imgs(input_data, match_similarity)

            if wait_type == WaitType.APPEAR:
                if target_rect is not None:
                    return True
            elif wait_type == WaitType.DISAPPEAR:
                if target_rect is None:
                    return True
            else:
                raise NotImplementedError()

            if time.time() - start_time > wait_time:
                break

            time.sleep(0.5)

        return False

    @staticmethod
    @atomicMg.atomic(
        "CV",
        inputList=[
            atomicMg.param(
                "input_data",
                formType=AtomicFormTypeMeta(AtomicFormType.PICK.value, params={"use": "CV"}),
                noInput=True,
            ),
            atomicMg.param("input_type", required=False),
            atomicMg.param(
                "input_content",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.input_content.show",
                        expression="return $this.input_type.value == '{}'".format(InputType.TEXT.value),
                    )
                ],
            ),
            atomicMg.param(
                "simulate_flag",
                formType=AtomicFormTypeMeta(AtomicFormType.SWITCH.value),
                dynamics=[
                    DynamicsItem(
                        key="$this.simulate_flag.show",
                        expression="return $this.input_type.value == '{}'".format(InputType.TEXT.value),
                    )
                ],
                required=False,
            ),
            atomicMg.param(
                "interval",
                types="Float",
                required=False,
                dynamics=[
                    DynamicsItem(
                        key="$this.interval.show",
                        expression="return $this.input_type.value == '{}'".format(InputType.TEXT.value),
                    )
                ],
            ),
            atomicMg.param("wait_time", types="Int", required=False),
            atomicMg.param(
                "match_similarity",
                formType=AtomicFormTypeMeta(AtomicFormType.SLIDER.value),
                required=False,
            ),
        ],
        outputList=[],
    )
    def image_input(
        input_data: IMGPick,
        input_type: InputType = InputType.TEXT,
        input_content: str = "",
        simulate_flag: Simulate_flag = Simulate_flag.YES,
        interval: float = 0.1,
        match_similarity: float = 0.95,
        wait_time: int = 10,
    ):
        """
        이미지 입력란에 입력
        :param input_data: 이미지
        :param input_type: 입력유형
        :param input_content: 입력내용
        :param simulate_flag: 여부입력
        :param interval: 입력
        :param wait_time: 대기시간
        :param match_similarity: 매칭 정확도
        """
        start_time = time.time()
        while True:
            target_rect = CvCore.match_imgs(input_data, match_similarity)
            if target_rect is not None:
                try:
                    Mouse.click(
                        x=target_rect[0] + target_rect[2] / 2,
                        y=target_rect[1] + target_rect[3] / 2,
                    )
                    if input_type == InputType.TEXT:
                        message = str(input_content)
                        if simulate_flag == Simulate_flag.YES:
                            # Keyboard.change_language(ENGLISH)
                            for char in message:
                                random_num = random.uniform(0, interval)
                                Keyboard.write_unicode(char)
                                time.sleep(random_num)
                            # Keyboard.change_language(CHINESE)
                        elif simulate_flag == Simulate_flag.NO:
                            # Keyboard.change_language(ENGLISH)
                            for char in message:
                                Keyboard.write_unicode(char)
                                time.sleep(interval)
                            # Keyboard.change_language(CHINESE)
                        else:
                            raise NotImplementedError()
                    elif input_type == InputType.CLIP:
                        msg = Clipboard.paste()
                        if not msg:
                            raise BaseException(CLIP_PASTE_ERROR, "Clip is empty.")
                        else:
                            Keyboard.hotkey("ctrl", "v")
                            Clipboard.clear()
                    else:
                        raise NotImplementedError()

                    return True
                except Exception as e:
                    raise BaseException(CV_INPUT_ERROR, "입력실패, 확인하세요입력정보")
            else:
                if time.time() - start_time > wait_time:
                    break
                else:
                    time.sleep(0.5)

        raise BaseException(CV_MATCH_ERROR, "시간 초과일치하지 않는까지목록 요소, 확인하세요현재또는낮음매칭 정확도재시도")
