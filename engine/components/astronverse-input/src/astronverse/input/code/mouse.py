import random
import time

import pyautogui
from astronverse.input import Speed

speed_to_int = {Speed.SLOW: 0.5, Speed.NORMAL: 1, Speed.FAST: 2}


class Mouse:
    def __int__(self):
        pyautogui.FAILSAFE = False

    @staticmethod
    def calculate_movement_duration(start_x: int, start_y: int, end_x: int, end_y: int, speed: Speed) -> float:
        """
        근거거리및정도계획필요시간.
        """
        distance = ((end_x - start_x) ** 2 + (end_y - start_y) ** 2) ** 0.5
        base_speed = 1000
        speed_multiplier = speed_to_int[speed]
        duration = distance / (base_speed * speed_multiplier)
        return max(0.1, duration)

    @staticmethod
    def position() -> tuple:
        """
        마우스위치
        """

        point = pyautogui.position()
        return point.x, point.y

    @staticmethod
    def move(x=None, y=None, duration: float = 0.0, tween=pyautogui.linear) -> None:
        """
        마우스
        """
        return pyautogui.moveTo(x=x, y=y, duration=duration, tween=tween)

    @staticmethod
    def move_simulate(x=None, y=None, duration: float = 0.0, tween=pyautogui.linear) -> None:
        """
        마우스사람방식
        """
        start_x, start_y = Mouse.position()
        # 계획거리
        distance = ((x - start_x) ** 2 + (y - start_y) ** 2) ** 0.5

        # 근거거리조정데이터, 적음데이터으로높이정도
        if distance < 300:
            steps = 1
        elif distance < 800:
            steps = 2
        else:
            steps = 3

        # 계획매의시간
        interval = duration / steps

        # 완료기기의매개변수, 매의아니오
        ease_param = random.uniform(1.5, 2.5)  # 소매개변수 , 변경평면

        # 추가기기의지연, 사람유형반대시간
        time.sleep(random.uniform(0.02, 0.05))  # 적음지연시간

        # 분
        for i in range(steps):
            t = i / steps
            # 사용수정의데이터, 변경
            ease_t = t**ease_param / (t**ease_param + (1 - t) ** ease_param)

            # 계획현재의목록 위치
            new_x = start_x + (x - start_x) * ease_t
            new_y = start_y + (y - start_y) * ease_t

            # 추가소의기기, 모듈, 소
            if i < steps - 1:  # 후일아니오추가
                new_x += random.uniform(-1, 1)
                new_y += random.uniform(-1, 1)

            # 사용pyautogui의moveTo데이터, 적음호출데이터
            pyautogui.moveTo(new_x, new_y, duration=interval, tween=pyautogui.easeInOutQuad)  # type: ignore

        # 확인종료위치
        Mouse.move(x=x, y=y)

    @staticmethod
    def click(
        x=None,
        y=None,
        clicks=1,
        interval=0.0,
        button=pyautogui.PRIMARY,
        duration=0.0,
        tween=pyautogui.linear,
    ) -> None:
        """
        마우스클릭
        """
        return pyautogui.click(
            x=x,
            y=y,
            clicks=clicks,
            interval=interval,
            button=button,
            duration=duration,
            tween=tween,
        )

    @staticmethod
    def down(x=None, y=None, button=pyautogui.PRIMARY, duration=0.0, tween=pyautogui.linear):
        """
        마우스
        """
        return pyautogui.mouseDown(x=x, y=y, button=button, duration=duration, tween=tween)

    @staticmethod
    def up(x=None, y=None, button=pyautogui.PRIMARY, duration=0.0, tween=pyautogui.linear):
        """
        마우스
        """
        return pyautogui.mouseUp(x=x, y=y, button=button, duration=duration, tween=tween)

    @staticmethod
    def scroll(clicks, x=None, y=None):
        """
        마우스
        """
        return pyautogui.scroll(clicks=clicks, x=x, y=y)

    @staticmethod
    def screen_size() -> tuple:
        """
        가져오기화면크기
        """
        return pyautogui.size()