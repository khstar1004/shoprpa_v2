"""
제어기기닫기공가능모듈.
"""

import json
import subprocess
import time

from pynput.mouse import Controller


class DialogController:
    """제어기기, 설치마우스및닫기 ."""

    mouse_controller = Controller()

    @staticmethod
    def get_current_mouse_position():
        """가져오기현재마우스위치."""
        current_position = DialogController.mouse_controller.position
        return current_position

    @staticmethod
    def execute_subprocess(args):
        """
        실행파싱출력중의JSON데이터.

        Args:
            args (list): 매개변수목록.

        Returns:
            dict: 파싱까지의JSON데이터(예있음).
        """
        with subprocess.Popen(
            args,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            encoding="utf-8",
        ) as process:
            output_data = {}
            while True:
                output = process.stdout.readline()
                if output == "" and process.poll() is not None:
                    break
                if not output:
                    continue
                output_line = output.strip()
                try:
                    output_data = json.loads(output_line)
                    print(f"output_data: {output_data}")
                except (json.JSONDecodeError, ValueError):
                    # JSON파싱오류, 계속관리아래일행
                    pass
            try:
                time.sleep(1)
                process.kill()
            except (OSError, ProcessLookupError):
                # 가능완료결과, 오류
                pass
            return output_data

    @staticmethod
    def read_process_output(process, process_output_list):
        """
        가져오기 출력추가 입력까지목록.

        Args:
            process: 객체.
            process_output_list (list): 사용저장출력의목록.
        """
        for line in iter(process.stdout.readline, ""):
            process_output_list.append(line)
        process.stdout.close()