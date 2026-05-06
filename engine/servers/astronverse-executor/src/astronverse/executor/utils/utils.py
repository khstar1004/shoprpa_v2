import ast
import os
import subprocess
import sys

import psutil
from astronverse.executor.logger import logger


def platform_python_venv_run_dir(dir: str):
    if sys.platform == "win32":
        path = os.path.dirname(os.path.dirname(os.path.dirname(dir)))
    else:
        path = os.path.dirname(os.path.dirname(os.path.dirname(dir)))
    return path


def kill_proc_tree(pid, including_parent=True, exclude_pids: list = None):
    """
    지정PID의모든.
    """
    try:
        # 가져오기 지정PID의
        proc = psutil.Process(pid)
    except psutil.NoSuchProcess:
        return  # 결과가찾을 수 없습니다, 이면출력데이터

    try:
        children = proc.children(recursive=True)
        for child in children:
            # 호출으로의
            kill_proc_tree(child, including_parent=True)
    except Exception as e:
        pass

    if including_parent:
        try:
            if exclude_pids:
                if proc.pid in exclude_pids:
                    return

            # 시작실행디렉터리아래의
            proc_cwd = proc.exe()
            if "shoprpa" not in proc_cwd:
                return

            # 시도
            proc.kill()
            proc.wait(5)  # 대기결과, 중지
        except psutil.NoSuchProcess:
            pass


def str_to_list_if_possible(s):
    if not isinstance(s, str):
        return s  # 결과가아니오예문자열, 직선연결반환

    s = s.strip()  # 제거빈
    if not (s.startswith("[") and s.endswith("]")):
        return s  # 아니오예목록문자열, 직선연결반환

    try:
        # 설치전체를문자열파싱로 Python 문자량(지원목록, 딕셔너리, 원그룹, 숫자, 문자열대기)
        result = ast.literal_eval(s)
        if isinstance(result, list):
            return result
        else:
            return s  # 예 [] 방식, 파싱후아니오예 list(예가능예 tuple)
    except (ValueError, SyntaxError):
        return s  # 파싱실패, 설명아니오예있음의목록문자열


def exec_run(exec_args: list, ignore_error: bool = False, timeout=-1):
    """시작관리오류로그"""

    logger.debug("준비실행명령: %s", exec_args)
    proc = subprocess.Popen(
        exec_args,
        stdin=subprocess.DEVNULL,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        env={**os.environ, "no_proxy": "*"},
        creationflags=subprocess.CREATE_NO_WINDOW if sys.platform == "win32" else 0,
    )
    try:
        proc.wait(timeout=timeout if timeout > 0 else None)
    except subprocess.TimeoutExpired:
        proc.kill()
        proc.wait()
        raise TimeoutError("error: timeout") from None

    if proc.returncode != 0 and not ignore_error:
        raise BaseException(f"error: return code({proc.returncode})")