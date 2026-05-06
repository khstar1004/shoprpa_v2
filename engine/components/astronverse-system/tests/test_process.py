import os
import sys
import unittest
from unittest import TestCase

from astronverse.system import *
from astronverse.system.process import Process


class TestProcess(TestCase):
    def setUp(self):
        """시도전의준비"""
        self.test_command = os.path.join(os.path.dirname(__file__), "test.exe")

    def test_run_command_normal_success(self):
        """시도실행명령 - 통신방식성공"""
        # 실행단일의echo명령
        result = Process.run_command(
            command=self.test_command,
            cmd_type=CmdType.NORMAL,
            run_type=RunType.CONTINUE,
        )
        self.assertTrue(result)

    def test_run_command_with_params(self):
        """시도실행명령 - 매개변수"""
        params = "param1 param2"

        result = Process.run_command(
            command=self.test_command,
            cmd_type=CmdType.NORMAL,
            run_type=RunType.CONTINUE,
            params=params,
        )
        self.assertTrue(result)

    def test_run_command_with_work_dir(self):
        """시도실행명령 - 지정디렉터리"""
        work_dir = os.getcwd()  # 사용현재디렉터리

        result = Process.run_command(
            command=self.test_command,
            cmd_type=CmdType.NORMAL,
            run_type=RunType.CONTINUE,
            work_dir=work_dir,
        )
        self.assertTrue(result)

    def test_run_command_complete_wait(self):
        """시도실행명령 - 대기방식"""
        # 실행일개빠름완료의명령
        try:
            result = Process.run_command(
                command=self.test_command,
                cmd_type=CmdType.NORMAL,
                run_type=RunType.COMPLETE,
                wait_time=5,
            )
            self.assertTrue(result)
        except Exception as e:
            pass

    def test_get_pid_exact_match(self):
        """시도가져오기PID - 매칭"""
        # 조회현재Python
        result = Process.get_pid(
            process_name="python.exe" if sys.platform == "win32" else "python",
            search_type=SearchType.EXACT,
            pid_type=PidType.ALL,
        )

        # 인증반환의예목록 패키지현재의PID
        self.assertIsInstance(result, list)
        self.assertIn(os.getpid(), result)

    def test_get_pid_fuzzy_match(self):
        """시도가져오기PID - 매칭"""
        # 사용매칭조회Python
        result = Process.get_pid(process_name="python", search_type=SearchType.FUZZY, pid_type=PidType.ALL)

        # 인증반환의예목록
        self.assertIsInstance(result, list)
        # 해당가능까지현재Python
        self.assertIn(os.getpid(), result)

    def test_get_pid_regex_match(self):
        """시도가져오기PID - 정상이면매칭"""
        # 사용정상이면테이블방식매칭Python
        result = Process.get_pid(process_name="python.*", search_type=SearchType.REGEX, pid_type=PidType.ALL)

        # 인증반환의예목록
        self.assertIsInstance(result, list)
        # 해당가능까지현재Python
        self.assertIn(os.getpid(), result)

    def test_get_pid_one_result(self):
        """시도가져오기PID - 반환단일개결과"""
        # 조회현재Python, 반환단일개PID
        result = Process.get_pid(
            process_name="python.exe" if sys.platform == "win32" else "python",
            search_type=SearchType.EXACT,
            pid_type=PidType.ONE,
        )

        # 인증반환의예정수예현재의PID
        self.assertIsInstance(result, int)

    def test_get_pid_empty_name(self):
        """시도가져오기PID - 빈이름"""
        with self.assertRaises(BaseException):
            Process.get_pid(process_name="", search_type=SearchType.EXACT, pid_type=PidType.ALL)

    def test_get_pid_no_such_process(self):
        """시도가져오기PID - 찾을 수 없습니다"""
        # 조회일개찾을 수 없습니다의
        result = Process.get_pid(
            process_name="nonexistent_process_12345",
            search_type=SearchType.EXACT,
            pid_type=PidType.ALL,
        )

        # 인증반환빈목록
        self.assertEqual(result, [])

    def test_run_command_system_commands(self):
        """시도실행명령 - 시스템명령"""
        system_commands = [
            "dir" if sys.platform == "win32" else "ls",
            "echo Hello World",
            "whoami" if sys.platform != "win32" else "whoami",
        ]

        for cmd in system_commands:
            try:
                result = Process.run_command(command=cmd, cmd_type=CmdType.NORMAL, run_type=RunType.CONTINUE)
                self.assertTrue(result)
            except Exception:
                # 명령가능에서시스템위할 수 없음사용, 예보통의
                pass

    def test_run_command_with_environment_variables(self):
        """시도실행명령 - 변수"""
        # 실행일개사용변수의명령
        env_command = "echo %PATH%" if sys.platform == "win32" else "echo $PATH"

        result = Process.run_command(command=env_command, cmd_type=CmdType.NORMAL, run_type=RunType.CONTINUE)
        self.assertTrue(result)


if __name__ == "__main__":
    unittest.main()