import json
import os.path
import platform
import random
import socket
import string
import sys

import psutil
import requests
from astronverse.scheduler.logger import logger


def generate_password(length=8):
    """완료지정길이정도의기기비밀번호(숫자+영어문자)"""
    chars = string.ascii_letters + string.digits
    return "".join(random.choice(chars) for _ in range(length))


class Terminal:
    @staticmethod
    def register(svc):
        api = "/api/robot/terminal/register"
        try:
            if not terminal_id:
                logger.error("terminal_id 비어 있습니다")
                return
            data = {
                "terminalId": terminal_id,  # 단말일식별자, 예준비mac주소
                "name": Terminal.get_device_name(),  # 단말이름
                "account": Terminal.get_account(),  # 준비계정
                "os": Terminal.get_os_info(),  # 운영체제
                "osPwd": terminal_pwd,
                "port": svc.rpa_route_port,
                "ip": ",".join(ips),  # IP주소
                "status": "busy"
                if svc.executor_mg.status()
                else "free",  # 현재상태, 사용계획종료상태, 있음상태, 실행중busy, 빈free
                "cpu": int(Terminal.get_cpu_percent()),  # CPU사용(분)
                "memory": int(Terminal.get_memory_percent()),  # 메모리사용(분)
                "disk": int(Terminal.get_disk_percent()),  # 하드사용(분)
                "isDispatch": 1 if svc.terminal_mod else 0,  # 여부스케줄링방식 (0: 아니오, 1: 예)
                "monitorUrl": "/terminal/ping",  # URL
            }
            logger.info("Terminal register data: {}".format(data))
            response = requests.post(
                url="http://127.0.0.1:{}{}".format(svc.rpa_route_port, api),
                json=data,
                timeout=10,
            )
            status_code = response.status_code
            text = response.text
            if status_code != 200:
                raise Exception("get error status_code: {}".format(status_code))
            return json.loads(text.strip())["data"]
        except Exception as e:
            logger.exception("[APP] request api: {} error: {}".format(api, e))

    @staticmethod
    def upload(svc):
        api = "/api/robot/terminal/beat"
        try:
            if not terminal_id:
                logger.error("terminal_id 비어 있습니다")
                return
            data = {
                "terminalId": terminal_id,  # 단말일식별자, 예준비mac주소
                "status": "busy"
                if svc.executor_mg.status()
                else "free",  # 현재상태, 사용계획종료상태, 있음상태, 실행중busy, 빈free
                "isDispatch": 1 if svc.terminal_mod else 0,  # 여부스케줄링방식 (0: 아니오, 1: 예)
                "cpu": int(Terminal.get_cpu_percent()),  # CPU사용(분)
                "memory": int(Terminal.get_memory_percent()),  # 메모리사용(분)
                "disk": int(Terminal.get_disk_percent()),  # 하드사용(분)
            }
            logger.info("Terminal upload data: {}".format(data))
            response = requests.post(
                url="http://127.0.0.1:{}{}".format(svc.rpa_route_port, api),
                json=data,
                timeout=10,
            )
            status_code = response.status_code
            text = response.text
            if status_code != 200:
                raise Exception("get error status_code: {}".format(status_code))
            res = json.loads(text.strip())
            if "data" not in res:
                # 있음로그인, 반환None
                return None
            return json.loads(text.strip())["data"]
        except Exception as e:
            logger.exception("[APP] request api: {} error: {}".format(api, e))

    @staticmethod
    def get_terminal_id():
        if len(ip_address) > 0:
            return ip_address[0].get("mac", "")
        return ""

    @staticmethod
    def get_ip_address() -> list:
        """가져오기본기기돌아가기IPv4주소, WLAN, 있음, 가져오기 일개"""

        def net_priority(net):
            # 정렬이면
            iface = net["interface"].lower()
            if any(k in iface for k in ["wlan", "wi-fi", "wifi"]):
                return 0
            if any(k in iface for k in ["eth", "en", "으로네트워크"]):
                return 1
            return 2

        try:
            active_net = []
            addrs = psutil.net_if_addrs()
            stats = psutil.net_if_stats()
            for iface, addresses in addrs.items():
                if not stats[iface].isup:
                    # 건너뛰기사용할 수 없습니다의연결
                    continue

                # Linux아래필터링변경다중연결
                if iface.startswith(
                    (
                        "lo",
                        "virbr",
                        "docker",
                        "veth",
                        "br-",
                        "vmnet",
                        "vboxnet",
                        "tun",
                        # "tap",
                    )
                ):
                    # 건너뛰기본돌아가기및연결, 예Docker, VMware, VirtualBox대기
                    continue

                mac = None
                ipv4 = None
                for addr in addresses:
                    if addr.family == psutil.AF_LINK:
                        mac = addr.address.replace("-", ":").upper()
                for addr in addresses:
                    if addr.family == socket.AF_INET and not addr.address.startswith("127."):
                        ipv4 = addr.address
                if mac and ipv4:
                    active_net.append({"interface": iface, "mac": mac, "ipv4": ipv4})
            return sorted(active_net, key=net_priority)
        except Exception as e:
            logger.error("가져오기IP주소실패: {}".format(e))
            return []

    @staticmethod
    def get_disk_percent() -> float:
        """
        가져오기설치디렉터리의기호, 계획사용
        """
        try:
            abs_path = os.path.abspath(__file__)
            if sys.platform == "win32":
                drive = os.path.splitdrive(abs_path)[0]
                logger.info("disk percent: {}".format(drive))
                return psutil.disk_usage(drive).percent
            else:
                root_path = "/"
                return psutil.disk_usage(root_path).percent
        except Exception as e:
            logger.error("가져오기디스크사용실패: {}".format(e))
            return 0.0

    @staticmethod
    def get_cpu_percent() -> float:
        """
        가져오기CPU사용
        """
        try:
            ls = psutil.cpu_percent(interval=1, percpu=True)
            return sum(ls) / len(ls)
        except Exception as e:
            logger.error("가져오기CPU사용실패: {}".format(e))
            return 0.0

    @staticmethod
    def get_memory_percent() -> float:
        """
        가져오기메모리사용
        """
        try:
            return psutil.virtual_memory().percent
        except Exception as e:
            logger.error("가져오기메모리사용실패: {}".format(e))
            return 0.0

    @staticmethod
    def get_device_name() -> str:
        """가져오기 준비전체이름(아니오패키지, 유형hostname)"""
        try:
            return socket.gethostname()
        except Exception as e:
            logger.error("가져오기 준비이름실패: {}".format(e))
            return ""

    @staticmethod
    def get_account() -> str:
        """가져오기현재로그인계정(내용Windows/Linux/macOS)"""
        try:
            # 시도os.getlogin()
            return os.getlogin()
        except Exception as e:
            logger.error("가져오기사용자계정실패: {}".format(e))
            return ""

    @staticmethod
    def get_os_info() -> str:
        """가져오기운영체제정보"""
        try:
            system = platform.system()
            if system == "Windows":
                version = platform.version()
                if version >= "10.0.22000":
                    return "Windows 11"
                else:
                    return f"Windows {platform.release()}"
            else:
                return f"{system} {platform.release()}"
        except Exception as e:
            logger.error("가져오기운영체제정보실패: {}".format(e))
            return ""


ip_address = Terminal.get_ip_address()
ips = [v.get("ipv4") for v in ip_address]
terminal_id = Terminal.get_terminal_id()
terminal_pwd = generate_password(8)