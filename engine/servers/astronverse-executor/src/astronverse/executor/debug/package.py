import os
import sys
from urllib.parse import urlparse

from astronverse.actionlib import ReportTip
from astronverse.executor.error import *
from astronverse.executor.logger import logger
from astronverse.executor.utils.utils import exec_run
from importlib_metadata import version as check_version


def compare_versions(v1, v2):
    for i in range(max(len(v1), len(v2))):
        num1 = v1[i] if i < len(v1) else 0
        num2 = v2[i] if i < len(v2) else 0
        if num1 > num2:
            return 1
        elif num1 < num2:
            return -1
    return 0


def find_version(lib, ver, ver_strict) -> bool:
    try:
        v0 = [int(x) for x in check_version(lib).split(".")]
        if ver:
            if not ver_strict:
                # 너비방식
                v1 = [[int(x) for x in ver.split(".")][0]]
                if compare_versions(v0, v1) >= 0 and compare_versions([v1[0] + 1], v0) > 0:
                    return True
            else:
                # 격식방식
                if compare_versions(v0, [int(x) for x in ver.split(".")]) == 0:
                    return True
        else:
            # 있음버전, 직선연결반환
            return True
    except Exception as e:
        # 있음library
        pass
    return False


class Package:
    def __init__(self, svc):
        self.svc = svc
        self.library_cache = {}

    def download(self, library: str, version: str, mirror: str = "", version_strict: bool = False, error_try=True):
        # 1. 빠름결과
        if not library:
            return

        # 2. cache
        if library in self.library_cache:
            return
        self.library_cache[library] = True

        # 3. 버전조회
        if find_version(library, version, version_strict):
            return

        # 4. 
        self.svc.report.info(ReportTip(msg_str=MSG_DOWNLOAD_FORMAT.format(library)))

        pip_cache_dir = os.path.join(self.svc.conf.package_cache_dir)
        if not os.path.exists(pip_cache_dir):
            os.makedirs(pip_cache_dir)

        # 6. 다운로드

        # 6.1 다운로드의이름
        if version:
            if version_strict:
                cmd_name = "{}=={}".format(library, version)
            else:
                v1 = [[int(x) for x in version.split(".")][0]]
                v2 = [v1[0] + 1]
                cmd_name = "{}>={},<{}".format(library, ".".join(str(x) for x in v1), ".".join(str(x) for x in v2))
        else:
            cmd_name = library

        # 6.2 다운로드의mirror
        if mirror:
            mirror = ["--index-url", mirror, "--trusted-host", urlparse(mirror).hostname]

        # 6.3 다운로드의명령
        pip_download = [
            sys.executable,
            "-m",
            "pip",
            "download",
            cmd_name,
            "-d",
            pip_cache_dir,
            *mirror,
            "--disable-pip-version-check",
            "--no-cache",
        ]
        pip_install_1 = [
            sys.executable,
            "-m",
            "pip",
            "install",
            cmd_name,
            "--no-index",
            "--find-links={}".format(pip_cache_dir),
            "--no-warn-script-location",
            "--disable-pip-version-check",
        ]
        pip_install_2 = [
            sys.executable,
            "-m",
            "pip",
            "install",
            cmd_name,
            "--find-links={}".format(pip_cache_dir),
            *mirror,
            "--no-warn-script-location",
            "--disable-pip-version-check",
        ]

        try:
            exec_run(pip_download, False, 600)
        except Exception as e:
            logger.warning("pip_download error: {}".format(e))
            pass

        def __install_pip(cmd):
            try:
                exec_run(cmd, False, 600)
            except Exception as e:
                # 3. 조회일버전조회
                if find_version(library, version, version_strict):
                    return
                logger.error("__install_pip error:{}".format(e))
                raise e

        try:
            __install_pip(pip_install_1)
        except Exception as e:
            if error_try:
                __install_pip(pip_install_2)
            else:
                raise e

        self.svc.report.info(ReportTip(msg_str=MSG_DOWNLOAD_SUCCESS_FORMAT.format(library)))