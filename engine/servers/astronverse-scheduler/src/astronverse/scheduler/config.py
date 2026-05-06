import sys

from astronverse.scheduler.utils.platform_utils import platform_python_run_dir


class Config:
    """
    스케줄링기기공가능
    """

    # 사용이름
    app_name: str = "scheduler"
    # 주소
    remote_addr: str = None
    # config파일
    conf_file: str = None
    # python실행디렉터리
    python_run_dir = platform_python_run_dir(sys.executable)
    # core_python
    python_core = sys.executable
    # base_python
    python_base = sys.executable
    # dir
    venv_base_dir = "venvs"