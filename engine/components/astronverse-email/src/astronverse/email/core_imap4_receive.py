"""
 IMAP4 의메일수신및파싱도구유형.
"""

import base64
import email
import email.header
import email.utils
import imaplib
import re
from datetime import datetime
from imaplib import IMAP4_SSL

from astronverse.baseline.logger.logger import logger


def encode_imap_utf7(folder_name: str) -> bytes:
    """
    를메일함폴더이름코드로 IMAP 수정버전 UTF-7 형식(RFC 3501).
    - 통신가능인쇄 ASCII(제거 & 외부)기존보관
    - & 코드로 &-
    -  ASCII 문자(예중국어)코드로 &<base64(UTF-16BE)>-, 중 base64 중의 / 로 ,
    """
    res = bytearray()
    buf = []

    def flush_buf():
        if buf:
            encoded = base64.b64encode("".join(buf).encode("utf-16-be")).decode("ascii")
            encoded = encoded.rstrip("=").replace("/", ",")
            res.extend(("&" + encoded + "-").encode("ascii"))
            buf.clear()

    for char in folder_name:
        if char == "&":
            flush_buf()
            res.extend(b"&-")
        elif 0x20 <= ord(char) <= 0x7E:
            flush_buf()
            res.append(ord(char))
        else:
            buf.append(char)

    flush_buf()
    return bytes(res)


def decode_imap_utf7(encoded: str) -> str:
    """
    를 IMAP 수정버전 UTF-7 문자열해제코드로 Unicode(RFC 3501), 방법로그가능.
    """
    import re as _re

    result = []
    for part in _re.split(r"(&[^-]*-)", encoded):
        if part.startswith("&") and part.endswith("-"):
            inner = part[1:-1]
            if inner == "":
                result.append("&")
            else:
                b64 = inner.replace(",", "/")
                #  base64 padding
                b64 += "=" * (-len(b64) % 4)
                try:
                    result.append(base64.b64decode(b64).decode("utf-16-be"))
                except Exception:
                    result.append(part)
        else:
            result.append(part)
    return "".join(result)


def decode_folder_list(raw_list) -> list:
    """를 showFolders 반환의기존문자목록해제코드로가능문자열목록"""
    folders = []
    for item in raw_list or []:
        try:
            text = item.decode("ascii") if isinstance(item, bytes) else str(item)
            # 가져오기 내부의폴더이름, 예 () "/" "&XfJT0ZAB-"
            match = re.search(r'"([^"]*)"\s*$', text) or re.search(r'"([^"]+)"[^"]*$', text)
            if match:
                raw_name = match.group(1)
                decoded = decode_imap_utf7(raw_name)
                folders.append(f"{decoded!r}  (raw: {raw_name})")
            else:
                folders.append(text)
        except Exception as e:
            folders.append(repr(item))
    return folders


def decode_data(b, added_encode=None):
    """
    문자해제코드
    """

    def _decode(bs, encoding):
        """
        내부모듈해제코드데이터
        """
        try:
            return str(bs, encoding=encoding)
        except Exception as e:
            return None

    encodes = ["GB2312", "UTF-8", "GBK"]
    if added_encode:
        encodes = [added_encode] + encodes
    for encoding in encodes:
        str_data = _decode(b, encoding)
        if str_data is not None:
            return str_data
    return None


class EmailImap4Receive:
    """
     IMAP4 의메일수신및파싱도구유형.
    로그인, 메일검색, 파싱, 완료대기공가능.
    """

    def __init__(self):
        self.mail_handler: IMAP4_SSL

    def login(self, server, port: int, user, password):
        """로그인메일함서비스서버"""
        self.mail_handler = imaplib.IMAP4_SSL(server, port)
        self.mail_handler.login(user, password)
        self.__build_header__(user)

    def __build_header__(self, user):
        """
        생성네트워크클라이언트id
        """
        imaplib.Commands["ID"] = ("AUTH",)
        args = (
            "name",
            user.split("@")[0],
            "contact",
            user,
            "version",
            "1.0.0",
            "vendor",
            "myclient",
        )
        self.mail_handler._simple_command("ID", '("' + '" "'.join(args) + '")')

    def showFolders(self):
        """
        반환모든폴더

        """
        return self.mail_handler.list()

    def select(self, selector):
        """
        선택파일함(예`INBOX`, 결과가아니오알림가능으로호출showFolders).
         selector 패키지 ASCII 문자(예중국어), 코드로 IMAP 수정버전 UTF-7.
        """
        status, folder_data = self.showFolders()
        logger.info(f"available folders:\n" + "\n".join(decode_folder_list(folder_data)))
        if isinstance(selector, str):
            encoded_selector = encode_imap_utf7(selector)
            logger.info(f"selecting folder: {selector!r} -> encoded: {encoded_selector}")
        else:
            encoded_selector = selector
        result = self.mail_handler.select(encoded_selector)
        logger.info(f"select result: {result}")
        return result

    def search(self, charset="utf-8", *criteria):
        """
        조회메일 검색메일(매개RFC문서http://tools.ietf.org/html/rfc3501#page-49)

        :param charset:
        :param criteria:
            1. support `FROM`, `TO`, `BODY`, `SUBJECT` to filter by keyword
            2. support `Unseen`, `All`, `Seen`, `(SINCE "01-Jan-2017")` to filter by mail's state
        :return:
        """
        try:
            return self.mail_handler.search(charset, *criteria)
        except Exception:
            self.select("INBOX")
            return self.mail_handler.search(charset, *criteria)

    def __get_email_format__(self, num):
        """
        으로RFC822형식반환메일의email객체

        :param num:
        :return: msg
        """
        data = self.mail_handler.fetch(num, "RFC822")
        if data[0] == "OK" and data[1] and data[1][0] and len(data[1][0]) > 1:
            decoded = decode_data(data[1][0][1])
            if decoded is not None:
                return email.message_from_string(decoded)
            else:
                return "decode error"
        else:
            return "fetch error"

    @staticmethod
    def __parse_attachment__(message_part):
        content_disposition = message_part.get("Content-Disposition", None)
        if not content_disposition:
            return None

        dispositions = content_disposition.strip().split(";")
        if not bool(content_disposition and dispositions[0].lower() == "attachment"):
            return None

        attachment = {}
        file_data = message_part.get_payload(decode=True)
        attachment["content_type"] = message_part.get_content_type()
        attachment["size"] = len(file_data) if file_data else None
        de_name = email.header.decode_header(message_part.get_filename())[0]
        name = de_name[0]
        if de_name[1] is not None:
            name = de_name[0].decode(de_name[1])
        attachment["name"] = name
        attachment["data"] = file_data
        return attachment

    @staticmethod
    def __get_sender_info__(msg):
        """반환전송의정보——원그룹(메일명칭, 메일주소)"""
        name = email.utils.parseaddr(msg["from"])[0]
        de_name = email.header.decode_header(name)[0]
        if de_name[1] is not None:
            name = decode_data(de_name[0], de_name[1])
        address = email.utils.parseaddr(msg["from"])[1]
        return name, address

    @staticmethod
    def __get_receiver_info__(msg):
        """반환연결의정보——원그룹(메일명칭, 메일주소)"""
        name = email.utils.parseaddr(msg["to"])[0]
        de_name = email.header.decode_header(name)[0]
        if de_name[1] is not None:
            name = decode_data(de_name[0], de_name[1])
        address = email.utils.parseaddr(msg["to"])[1]
        return name, address

    @staticmethod
    def __get_subject_content__(msg):
        """반환메일의제목(매개변수msg예email객체, 가능호출getEmailFormat가져오기 )"""
        try:
            de_content = email.header.decode_header(msg["subject"])[0]
        except Exception:
            return msg["subject"]
        if de_content[1] is not None:
            return decode_data(de_content[0], de_content[1])
        return de_content[0]

    @staticmethod
    def __get_email_time__(msg):
        date_tuple = email.utils.parsedate_tz(msg["Date"])
        if date_tuple:
            local_date = datetime.fromtimestamp(email.utils.mktime_tz(date_tuple))
            formatted_time = local_date.strftime("%Y-%m-%d %H:%M:%S")
        else:
            formatted_time = None
        return formatted_time

    def get_entire_mail_info(self, num):
        """
        반환메일의파싱후정보모듈분
        반환목록패키지(제목, 텍스트정상문서모듈분, html의정상문서모듈분, 발송파일사람원그룹, 파일사람원그룹, 파일목록)
        """
        msg = self.__get_email_format__(num)
        attachments = []
        body = None
        html = None

        for part in msg.walk():  # type: ignore
            if part.get_content_type() == "text/plain":
                if body is None:
                    body = b""
                payload = part.get_payload(decode=True)
                if isinstance(payload, bytes):
                    body += payload
            elif part.get_content_type() == "text/html":
                if html is None:
                    html = b""
                payload = part.get_payload(decode=True)
                if isinstance(payload, bytes):
                    html += payload
            else:
                attachment = self.__parse_attachment__(part)
                attachments.append(attachment)
        return {
            "from": self.__get_sender_info__(msg),
            "to": self.__get_receiver_info__(msg),
            "subject": self.__get_subject_content__(msg),
            "body": decode_data(body),
            "html": decode_data(html),
            "time": self.__get_email_time__(msg),
            "attachments": attachments,
        }

    def mask_as_read(self, num):
        """필요비고로완료"""
        self.mail_handler.store(num, "+FLAGS", "\\Seen")