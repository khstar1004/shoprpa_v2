import ast
import smtplib
import time
from email.header import Header
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.utils import formataddr, parseaddr

import requests
from astronverse.scheduler.logger import logger
from astronverse.scheduler.utils.utils import get_settings


class NotifyConfig:
    EMAIL_SERVER = "mail.shoprpa.com"
    EMAIL_PORT = 465
    EMAIL_ADDRESS = ""
    EMAIL_PASSWORD = ""


class NotifyUtils:
    def __init__(self, svc):
        self.svc = svc
        self.settings = get_settings()
        self.email_setting = self.settings.get("msgNotifyForm", {}).get("email", {})
        self.text_setting = self.settings.get("msgNotifyForm", {}).get("phone_msg", {})
        self.email_msg = None
        self.mail_handler = None

    @staticmethod
    def handle_email_address(email_str):
        # 파싱가능존재함의목록유형입력
        list_result = []
        if isinstance(email_str, list):
            list_result = [list(filter(lambda a: a, email.replace(";", ";").split(";"))) for email in email_str]
        elif isinstance(email_str, str):
            email_str = email_str.replace(";", ";")
            try:
                email_str = ast.literal_eval(email_str)
                list_result = [list(filter(lambda a: a, email.split(";"))) for email in email_str]
            except Exception as e:
                list_result = [list(filter(lambda a: a, email_str.split(";")))]
                email_str = [email_str]
        return email_str, list_result

    @staticmethod
    def format_addr(s):
        name, addr = parseaddr(s)
        return formataddr((Header(name, "utf-8").encode(), addr))

    def login_send(self):
        if self.email_setting.get("is_default", True):
            mail_server = NotifyConfig.EMAIL_SERVER
            mail_port = NotifyConfig.EMAIL_PORT
            sender_mail = NotifyConfig.EMAIL_ADDRESS
            password = NotifyConfig.EMAIL_PASSWORD
            use_ssl = True
        else:
            mail_server = self.email_setting["mail_server"]
            mail_port = self.email_setting["mail_port"]
            sender_mail = self.email_setting["sender_mail"]
            password = self.email_setting["password"]
            use_ssl = self.email_setting["use_ssl"]
        self.email_msg = MIMEMultipart()
        if use_ssl:
            self.mail_handler = smtplib.SMTP_SSL(mail_server, int(mail_port), timeout=20)
        else:
            self.mail_handler = smtplib.SMTP(mail_server, int(mail_port), timeout=20)
        try:
            self.mail_handler.login(sender_mail, password)
        except smtplib.SMTPException as e:
            logger.debug(e)
            raise Exception("전송예외메일실패!")

    def send(self, robot_name, run_time):
        if self.email_setting.get("is_enable", False):
            content = "[ShoprpaShoprpaRPA]실행의봇 {}{}실행실패, 요청 시조회".format(robot_name, run_time)
            self.login_send()
            self.send_email(content)
        if self.text_setting.get("is_enable", False):
            self.send_text(robot_name, run_time)

    def send_email(self, content: str = "봇실행출력오류, 확인하세요!"):
        if self.email_setting.get("is_default", True):
            sender_mail = NotifyConfig.EMAIL_ADDRESS
        else:
            sender_mail = self.email_setting["sender_mail"]

        receiver = self.email_setting["receiver"]
        cc = self.email_setting["cc"]

        receiver_list, receiver_split_list = self.handle_email_address(receiver)
        cc_list, cc_split_list = self.handle_email_address(cc)

        for i in range(len(receiver_list)):
            self.email_msg = MIMEMultipart()
            self.email_msg["From"] = self.format_addr(f"Shoprpa <{sender_mail}>")
            self.email_msg["To"] = Header(receiver_list[i])
            self.email_msg["Cc"] = cc_list[i]
            self.email_msg["date"] = time.strftime("%a, %d %b %Y %H:%M:%S %z")
            self.email_msg["Subject"] = "RPA봇실행예외알림"
            self.email_msg.attach(MIMEText(content, "plain", "utf-8"))

            # 열기 전송
            try:
                self.mail_handler.sendmail(
                    sender_mail,
                    receiver_split_list[i] + cc_split_list[i],
                    self.email_msg.as_string(),
                )
            except smtplib.SMTPException as e:
                raise Exception("전송예외메일전송실패!")

    def send_text(self, robot_name, run_time):
        try:
            params = {
                "phone": self.text_setting["receiver"],
                "robotName": robot_name,
                "errorTime": run_time,
            }
            response = requests.get(
                "http://127.0.0.1:{}/api/uac/sms/sendForRobotError".format(self.svc.rpa_route_port),
                params=params,
            )
            status_code = response.status_code
            text = response.text
            if status_code != 200:
                logger.debug(f"전송짧음정보연결호출실패!{text}")
                raise Exception(f"전송짧음정보연결호출실패!{text}")
        except Exception as e:
            logger.debug(f"전송짧음정보연결호출실패!{e}")
            raise Exception("전송짧음정보연결호출실패!")
