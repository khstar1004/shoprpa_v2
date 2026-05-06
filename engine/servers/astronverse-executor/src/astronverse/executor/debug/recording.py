import os
import subprocess
import sys
import threading
import time
from datetime import datetime, timedelta

from astronverse.executor.logger import logger


def folder_empty(folder_path) -> bool:
    contents = os.listdir(folder_path)
    if len(contents) == 0:
        return True
    else:
        return False


class RecordingTool:
    def __init__(self, svc):
        self.thread = None
        self.svc = svc
        self.config = {
            "open": False,
            "cut_time": 30,  # 후시간: 0테이블아니오
            "scene": "always",  # 실행:fail/always
            "file_path": "./logs/report",
            "file_clear_time": 7,  # 관리기록제어7
        }
        self.start_time = 0
        self.end_time = 0
        self.local_raw_file = None
        self.local_file = None

        # 통신닫기
        self.event = threading.Event()
        self.exec_success = False
        self.exec_res = None

    def init(self, project_id, exec_id, config=None):
        if config:
            self.config = config

        local_file_path = os.path.join(os.getcwd(), self.config.get("file_path"), project_id)
        if not os.path.exists(local_file_path):
            os.makedirs(local_file_path)
        self.local_raw_file = os.path.join(local_file_path, "{}_raw.mp4".format(exec_id))
        self.local_file = os.path.join(local_file_path, "{}.mp4".format(exec_id))

        return self

    def __tool__(self):
        try:
            if not self.config.get("open"):
                return
            url = os.path.join(os.path.abspath(self.svc.conf.resource_dir), "ffmpeg.exe")
            if not os.path.exists(url):
                return

            self.start_time = int(time.time())
            if sys.platform == "win32":
                exec_args_1 = [
                    url,
                    "-thread_queue_size",
                    "16",
                    "-f",
                    "gdigrab",
                    "-rtbufsize",
                    "500M",
                    "-framerate",
                    "3",
                    "-i",
                    "desktop",
                    "-crf",
                    "23",
                    "-pix_fmt",
                    "yuv420p",
                    "-vf",
                    "scale=iw*75/100:ih*75/100,pad=ceil(iw/2)*2:ceil(ih/2)*2",
                    "{}".format(self.local_raw_file),
                    "-y",
                ]
            else:
                exec_args_1 = [
                    url,
                    "-thread_queue_size",
                    "16",
                    "-f",
                    "x11grab",
                    "-rtbufsize",
                    "500M",
                    "-framerate",
                    "3",
                    "-i",
                    ":0.0",
                    "-crf",
                    "23",
                    "-vf",
                    "scale=iw*75/100:ih*75/100,pad=ceil(iw/2)*2:ceil(ih/2)*2",
                    "{}".format(self.local_raw_file),
                    "-y",
                ]

            # 1. 시작기록제어
            proc_1 = subprocess.Popen(
                exec_args_1,
                stdin=subprocess.PIPE,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
            )

            # 2. 까지결과정보 
            self.event.wait()

            # 3. 닫기기록제어
            try:
                proc_1.stdin.write(b"q")
                proc_1.stdin.close()
            except BrokenPipeError:
                pass

            try:
                proc_1.wait(timeout=30)
            except subprocess.TimeoutExpired:
                proc_1.kill()
                proc_1.wait()

            self.end_time = int(time.time())

            # 4. 여부저장
            if self.exec_success:
                if self.config.get("scene") == "always":
                    is_save = True
                else:
                    is_save = False
            else:
                is_save = True
            if not is_save:
                # 아니오보관
                __path = "{}".format(self.local_raw_file)
                os.remove(__path)
                # 결과가삭제후파일의폴더예빈, 폴더삭제
                __path_dir = os.path.dirname(__path)
                if folder_empty(__path_dir):
                    os.rmdir("{}".format(__path_dir))
            elif int(self.config.get("cut_time")) <= 0:
                # 아니오잘라내기
                os.rename("{}".format(self.local_raw_file), self.local_file)
            else:
                # 잘라내기mp4, [수정: 량전후+5s]
                dt = 5
                ss = self.end_time - self.start_time - int(self.config.get("cut_time")) - dt
                if ss <= 0:
                    os.rename("{}".format(self.local_raw_file), self.local_file)
                else:
                    exec_args_2 = [
                        url,
                        "-ss",
                        "{}".format(ss),
                        "-t",
                        "{}".format(int(self.config.get("cut_time")) + dt + dt),
                        "-i",
                        self.local_raw_file,
                        self.local_file,
                        "-y",
                    ]
                    proc_2 = subprocess.Popen(
                        exec_args_2,
                        stdin=subprocess.DEVNULL,
                        stdout=subprocess.DEVNULL,
                        stderr=subprocess.DEVNULL,
                    )
                    proc_2.wait(timeout=120)
                    if proc_2.returncode != 0:
                        logger.warning("ffmpeg error, return code = {}".format(proc_2.returncode))
                    os.remove("{}".format(self.local_raw_file))
        except Exception as e:
            self.exec_res = False
            raise e
        finally:
            self.exec_res = True

    def __clear__(self):
        # 1.디렉터리여부존재함
        # 2.디렉터리저장된 디렉터리, 패키지디렉터리아래의모든완료.mp4결과의파일
        # 3..mp4파일의수정시간여부대7, 
        # 4.삭제대7의데이터

        logger.info("clear mp4:{}")

        file_clear_time = int(self.config.get("file_clear_time", 0))
        if not file_clear_time:
            return
        seven_days_ago = datetime.now() - timedelta(days=file_clear_time)

        for root, dirs, files in os.walk(self.config.get("file_path")):
            for file in files:
                if file.endswith(".mp4"):
                    file_path = os.path.join(root, file)
                    logger.info("walk mp4:{}".format(file_path))
                    file_mod_time = datetime.fromtimestamp(os.path.getmtime(file_path))
                    # 결과가파일수정시간초과경과7, 이면삭제해당파일
                    if file_mod_time < seven_days_ago:
                        os.remove(file_path)
                        logger.info("clear mp4:{}".format(file_path))
                        # 결과가삭제후파일의폴더예빈, 폴더삭제
                        __path_dir = os.path.dirname(file_path)
                        if folder_empty(__path_dir):
                            os.rmdir("{}".format(__path_dir))

    def start(self):
        if self.config.get("open"):
            self.thread = threading.Thread(target=self.__tool__, daemon=True)
            self.thread.start()
        else:
            # 아니오사용close결과
            self.exec_res = True
        if int(self.config.get("file_clear_time", 0)):
            # 결과가필요관리시작관리프로그램
            thread_2 = threading.Thread(target=self.__clear__, daemon=True)
            thread_2.start()

    def close(self, is_success: bool):
        # self.event 있음트리거, exec_res있음반환값[있음반환값테이블전결과완료]
        if not self.event.is_set() and self.exec_res is None:
            self.exec_success = is_success
            now = int(time.time())
            if now - self.start_time < 3:
                time.sleep(3 - now + self.start_time)
            self.event.set()
            while self.exec_res is None:
                time.sleep(0.1)
            self.exec_res = None