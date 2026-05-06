import sys

from astronverse.executor.utils.utils import platform_python_venv_run_dir


class Config:
    # 단말
    port: int = 0

    # 본네트워크닫기단말
    gateway_port: int = 0

    # 실행id
    exec_id: str = ""

    # id
    project_id: str = ""

    # DB에 저장된 기본 메인 프로세스 이름
    main_process_name: str = "\uD56D\uBAA9프로세스"

    # 코드(4개공백)
    indentation: str = " " * 4

    # 목록완료디렉터리경로
    gen_core_path: str = "{}/astron/".format(platform_python_venv_run_dir(sys.executable))

    # 컴포넌트완료디렉터리
    gen_component_path: str = "{}/astron_extension/".format(platform_python_venv_run_dir(sys.executable))

    # 입력본파일이름
    main_file_name: str = "main.py"

    # 로그저장위치
    log_path: str = "./logs/"

    # package cache
    package_cache_dir: str = "./pip_cache/"

    # resource dir
    resource_dir: str = "./"

    # 열기시작ws로그통신
    open_log_ws: bool = True

    # 여부대기프론트엔드ws연결
    wait_web_ws: bool = True

    # 여부열기시작대기오른쪽아래역할ws연결
    wait_tip_ws: bool = False

    # 여부예 debug 방식
    debug_mode: bool = False

    # 여부예 debug 방식
    is_custom_component: bool = False
