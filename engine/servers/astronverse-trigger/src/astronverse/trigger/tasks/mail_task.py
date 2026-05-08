import email
import imaplib
import poplib
from datetime import datetime

from apscheduler.triggers.interval import IntervalTrigger
from astronverse.trigger.core.logger import logger

global_mail_ids = {}

CONDITION_OR = "or"
CONDITION_AND = "and"
CONDITION_ALL = "all"


def decode_data(bytes, added_encode=None):
    """
    문자해제코드
    :param bytes:
    :return:
    """

    def _decode(bytes, encoding):
        try:
            return str(bytes, encoding=encoding)
        except Exception as e:
            return None

    encodes = ["GB2312", "UTF-8", "GBK"]
    if added_encode:
        encodes = [added_encode] + encodes
    for encoding in encodes:
        str_data = _decode(bytes, encoding)
        if str_data is not None:
            return str_data
    return None


class MailTask:
    def __init__(
        self,
        task_id: str,
        end_time: str = None,
        sender_text: str = None,
        receiver_text: str = None,
        theme_text: str = None,
        content_text: str = None,
        interval_time: int = 1,
        condition: str = "or",
        attachment: bool = False,
        **kwargs,
    ):
        """
        생성메일함조회의유형

        task_id: `str`, 작업id
        end_time: `str`, 여부사용종료 시간
        sender_text: `str`, 통신경과전송패키지닫기 문자행필터링
        receiver_text: `str`, 통신경과수신패키지닫기 문자행필터링
        theme_text: `str`, 통신경과제목패키지닫기 문자행필터링
        content_text: `str`, 통신경과내용패키지닫기 문자행필터링
        interval_time: `int`, 조회메일의시간
        condition: `str`, 메일함필터링파일의그룹합치기
        attachment: `bool`, 메일함필터링여부있음파일
        Kwargs: 해당매개변수사용생성작업의매개변수상태
            - mail_flag: 메일 서비스 유형. `163`, `126`, `qq`, `shoprpa`, `advance`를 지원합니다.
            - custom_mail_server: 사용자지정메일함서비스주소
            - custom_mail_port: 사용자지정메일함단말
            - user_mail: 사용자메일함계정
            - user_authorization: 사용자메일함권한 부여코드

        """

        self.sender_text: str = sender_text
        self.receiver_text: str = receiver_text
        self.theme_text: str = theme_text
        self.content_text: str = content_text
        self.task_id: str = task_id
        self._end_time: str = end_time
        self.interval_time: int = interval_time
        self.condition = condition
        self.attachment = attachment

        self.mail_flag: str = kwargs.get("mail_flag")
        self.custom_mail_server: str = kwargs.get("custom_mail_server")
        self.custom_mail_port: str = kwargs.get("custom_mail_port", "993")
        self.custom_mail_protocol: str = kwargs.get("custom_mail_protocol", "IMAP")
        self.custom_mail_ssl: bool = kwargs.get("custom_mail_ssl", True)
        self.user_mail: str = kwargs.get("user_mail")
        self.user_authorization: str = kwargs.get("user_authorization")

        self.mail_server_dict = {
            # IMAP SSL
            "qq": ["imap.qq.com", 993, "IMAP", True],
            "163": ["imap.163.com", 993, "IMAP", True],
            "126": ["imap.126.com", 993, "IMAP", True],
            "shoprpa": ["mail.shoprpa.com", 993, "IMAP", True],
            # 지정매칭
            "advance": [
                self.custom_mail_server,
                self.custom_mail_port,
                self.custom_mail_protocol,
                self.custom_mail_ssl,
            ],
        }

    def connect(self):
        """
        방법법, 외부모듈행메일함연결가능사용감지
        :param host:
        :param port:
        :param user_mail:
        :param user_authorization:
        :return:
        """
        try:
            used_mail_server, used_mail_port, used_mail_protocol, used_mail_ssl = self.mail_server_dict[self.mail_flag]

            # 근거및SSL생성의클라이언트
            if used_mail_protocol == "IMAP":
                if used_mail_ssl:
                    client = imaplib.IMAP4_SSL(host=used_mail_server, port=int(used_mail_port))
                else:
                    client = imaplib.IMAP4(host=used_mail_server, port=int(used_mail_port))

                client.login(self.user_mail, self.user_authorization)
                client.logout()  # 로그아웃
                return True
            elif used_mail_protocol == "POP3":
                if used_mail_ssl:
                    client = poplib.POP3_SSL(host=used_mail_server, port=int(used_mail_port))
                else:
                    client = poplib.POP3(host=used_mail_server, port=int(used_mail_port))

                client.user(self.user_mail)
                client.pass_(self.user_authorization)
                client.quit()  # 출력
                return True
            else:
                raise ValueError(f"지원하지 않는 메일 프로토콜입니다: {used_mail_protocol}")
        except Exception as e:
            return False

    async def aconnect(self):
        """
        예외방법법, 연결메일함클라이언트반환
        :return:
        """
        try:
            used_mail_server, used_mail_port, used_mail_protocol, used_mail_ssl = self.mail_server_dict[self.mail_flag]

            # 시스템일사용방식, 에서예외데이터중실행
            if used_mail_protocol == "IMAP":
                if used_mail_ssl:
                    client = imaplib.IMAP4_SSL(host=used_mail_server, port=int(used_mail_port))
                else:
                    client = imaplib.IMAP4(host=used_mail_server, port=int(used_mail_port))

                # 로그인
                client.login(self.user_mail, self.user_authorization)
                imaplib.Commands["ID"] = "AUTH"
                args = (
                    "name",
                    self.user_mail.split("@")[0],
                    "contact",
                    self.user_mail,
                    "version",
                    "1.0.0",
                    "vendor",
                    "myclient",
                )
                client._simple_command("ID", '("' + '" "'.join(args) + '")')
                logger.info("[AsyncMailTask callback]IMAP로그인성공")

                return client
            elif used_mail_protocol == "POP3":
                if used_mail_ssl:
                    client = poplib.POP3_SSL(host=used_mail_server, port=int(used_mail_port))
                else:
                    client = poplib.POP3(host=used_mail_server, port=int(used_mail_port))

                client.user(self.user_mail)
                client.pass_(self.user_authorization)
                logger.info("[AsyncMailTask callback]POP3로그인성공")

                return client
            else:
                raise ValueError(f"지원하지 않는 메일 프로토콜입니다: {used_mail_protocol}")

        except Exception as e:
            logger.info(f"[AsyncMailTask callback]연결메일함시오류: {str(e)}")
            import traceback

            logger.info(f"[AsyncMailTask callback]오류정보: {traceback.format_exc()}")
            return None

    async def search_all(self, client, condition: str = "ALL"):
        """검색메일"""
        # 근거유형관리
        if self.custom_mail_protocol == "IMAP":
            # IMAP - 필요선택메일함, 검색
            try:
                # 선택INBOX
                status, data = client.select("INBOX")
                if status != "OK":
                    logger.error(f"[AsyncMailTask callback]선택INBOX실패: {status} - {data}")
                    return False

                # 후검색메일
                status, data = client.search(None, condition)
                if status != "OK":
                    logger.error(f"[AsyncMailTask callback]검색메일실패: {status} - {data}")
                    return False
                return data
            except Exception as e:
                logger.error(f"[AsyncMailTask callback]IMAP검색메일예외: {str(e)}")
                return False
        elif self.custom_mail_protocol == "POP3":
            # POP3 - 가져오기모든메일수
            try:
                num_messages = len(client.list()[1])
                # 반환메일ID목록(POP3사용숫자ID)
                return [str(i + 1).encode() for i in range(num_messages)]
            except Exception as e:
                logger.info(f"[AsyncMailTask callback]POP3가져오기메일목록실패: {str(e)}")
                return False
        else:
            logger.error(f"[AsyncMailTask callback]지원하지 않는 메일 프로토콜입니다: {self.custom_mail_protocol}")
            return False

    async def callback(self) -> bool:
        """
        조회돌아가기조정

        :return
            `bool`, 식별자여부스케줄링성공
        """

        # 메일함연결
        logger.info("[AsyncMailTask callback]준비열기 연결메일함...")
        try:
            client = await self.aconnect()
            if not client:
                logger.error("[AsyncMailTask callback]메일함Client연결실패")
                return False
        except Exception as e:
            logger.error(f"[AsyncMailTask callback]연결메일함예외: {str(e)}")
            return False

        # 조회현재메일의모든메일(search_all선택INBOX)
        try:
            data = await self.search_all(client)
            if not data:
                logger.info("[AsyncMailTask callback]가져오기메일목록실패")
                return False
        except Exception as e:
            logger.error(f"[AsyncMailTask callback]검색메일예외: {str(e)}")
            return False

        # 관리메일ID목록
        if self.custom_mail_protocol == "IMAP":
            # if len(data) > 1 and data[1] == b'SEARCH completed':
            #     # IMAP반환형식: (b'1 2 3 4 5...', b'SEARCH completed')
            #     email_ids = data[0].split()
            # else:
            email_ids = data[0].split()
        elif self.custom_mail_protocol == "POP3":
            # POP3반환의예목록
            email_ids = data

        cache_ids = global_mail_ids.get(self.task_id, [])
        logger.info(f"[AsyncMailTask callback]가져오기메일현재메일성공: {len(email_ids)} 메일")

        # 가능관리대량메일: 조회추가의메일
        if not cache_ids:
            cache_ids = email_ids
            global_mail_ids[self.task_id] = cache_ids
            return False  # 실행아니오트리거, 생성

        # 메일순서열여부추가
        if len(cache_ids) >= len(email_ids):
            # 메일수있음증가추가, 업데이트저장로새의메일ID
            global_mail_ids[self.task_id] = email_ids
            return False

        # 계획추가의메일수
        distance = len(email_ids) - len(cache_ids)
        if distance > 0:
            # 조회추가의메일
            updated_ids = email_ids[-distance:]
            logger.info(f"[AsyncMailTask callback]발송{distance}새메일, 열기 조회")
        else:
            # 메일수있음변수, 가능메일ID있음변수(메일삭제대기)
            global_mail_ids[self.task_id] = email_ids
            return False
        # 결과가있음필터링파일, 이면
        logger.info("[AsyncMailTask callback]준비시작 행새메일...")
        processed_count = 0
        for email_id in updated_ids:
            try:
                # 근거유형가져오기메일내용
                if self.custom_mail_protocol == "IMAP":
                    # IMAP
                    status, data = client.fetch(email_id.decode(), "RFC822")
                elif self.custom_mail_protocol == "POP3":
                    # POP3
                    email_id_str = email_id.decode() if isinstance(email_id, bytes) else str(email_id)
                    data = client.retr(int(email_id_str))
                    # POP3반환의예원그룹 (response, lines, octets)
                    data = [data[1]]  # 변환로및IMAP의형식
                else:
                    logger.error(f"[AsyncMailTask callback]지원하지 않는 메일 프로토콜입니다: {self.custom_mail_protocol}")
                    continue

                mail_info = self._extract_info(data, self.custom_mail_protocol)
                processed_count += 1
                logger.info(f"[AsyncMailTask callback]완료가져오기메일정보 , 준비열기 : {mail_info}...")

                if self._check_mail_conditions(mail_info):
                    logger.info(f"[AsyncMailTask callback]이동파일{self.condition}")
                    global_mail_ids[self.task_id] = email_ids
                    return True

            except Exception as e:
                logger.error(f"[AsyncMailTask callback]가져오기메일실패: {str(e)}")
                continue

        logger.info(f"[AsyncMailTask callback]관리완료 {processed_count} 새메일, 있음기호합치기의메일정보, 직선연결반환")
        global_mail_ids[self.task_id] = email_ids
        return False

    def _check_mail_conditions(self, mail_info):
        """
        조회메일여부기호합치기필터링파일

        Args:
            mail_info: 메일정보딕셔너리

        Returns:
            bool: 여부기호합치기파일
        """
        # 모든빈파일의매칭결과
        conditions = []

        if self.sender_text:  # 있음빈파일매개및
            sender_match = self._check_sender(mail_info)
            conditions.append(("sender", sender_match))

        if self.receiver_text:
            receiver_match = self._check_receiver(mail_info)
            conditions.append(("receiver", receiver_match))

        if self.theme_text:
            subject_match = self._check_subject(mail_info)
            conditions.append(("subject", subject_match))

        if self.content_text:
            content_match = self._check_content(mail_info)
            conditions.append(("content", content_match))

        if self.attachment is not None:  # 파일파일관리, None테이블아니오제한
            attachment_match = self._check_attachment(mail_info)
            conditions.append(("attachment", attachment_match))

        # 기록매칭결과
        condition_logs = [f"{name}: {match}" for name, match in conditions]
        logger.info(f"[AsyncMailTask callback]파일매칭결과: {', '.join(condition_logs)}")

        # 결과가있음작업파일, 반환True
        if not conditions:
            return True

        # 근거파일유형행
        if self.condition == CONDITION_OR:
            # OR파일: 작업일파일가득가능
            return any(match for _, match in conditions)
        elif self.condition == CONDITION_AND:
            # AND파일: 모든파일가득
            return all(match for _, match in conditions)
        elif self.condition == CONDITION_ALL:
            # ALL파일: 없음파일매칭
            return True
        else:
            return False

    def _check_sender(self, mail_info):
        """조회발송파일사람여부매칭"""
        if not mail_info.get("from"):
            return False
        sender_text = " ".join([item for item in mail_info["from"] if item])
        return self.sender_text in sender_text

    def _check_receiver(self, mail_info):
        """조회파일사람여부매칭"""
        if not mail_info.get("to"):
            return False
        receiver_text = " ".join([item for item in mail_info["to"] if item])
        return self.receiver_text in receiver_text

    def _check_subject(self, mail_info):
        """조회제목여부매칭"""
        if not mail_info.get("subject"):
            return False
        return self.theme_text in mail_info["subject"]

    def _check_content(self, mail_info):
        """조회내용여부매칭"""
        if not mail_info.get("body"):
            return False
        return self.content_text in mail_info["body"]

    def _check_attachment(self, mail_info):
        """조회파일파일여부매칭"""
        return self.attachment == mail_info.get("has_attachment", False)

    @staticmethod
    def _extract_info(data, mail_type="IMAP"):
        """
        반환메일의파싱후정보모듈분
        반환목록패키지(제목, 텍스트정상문서모듈분, html의정상문서모듈분, 발송파일사람원그룹, 파일사람원그룹, 파일목록)

        """

        def get_sender_info(msg):
            name = email.utils.parseaddr(msg["from"])[0]
            deName = email.header.decode_header(name)[0]
            if deName[1] != None:
                name = decode_data(deName[0], deName[1])
                # name = deName[0].decode(deName[1])
            address = email.utils.parseaddr(msg["from"])[1]
            return (name, address)

        def get_receiver_info(msg):
            name = email.utils.parseaddr(msg["to"])[0]
            deName = email.header.decode_header(name)[0]
            if deName[1] != None:
                name = decode_data(deName[0], deName[1])
                # name = deName[0].decode(deName[1])
            address = email.utils.parseaddr(msg["to"])[1]
            return (name, address)

        def get_subject_content(msg):
            try:
                deContent = email.header.decode_header(msg["subject"])[0]
            except:
                return msg["subject"]

            if deContent[1] != None:
                return decode_data(deContent[0], deContent[1])
            return deContent[0]

        def get_mail_time(msg):
            date_tuple = email.utils.parsedate_tz(msg["Date"])
            if date_tuple:
                local_date = datetime.fromtimestamp(email.utils.mktime_tz(date_tuple))
                formatted_time = local_date.strftime("%Y-%m-%d %H:%M:%S")
            else:
                formatted_time = None
            return formatted_time

        body = None
        html = None
        has_attachment = False

        # 관리아니오의데이터형식
        if mail_type == "IMAP":
            if isinstance(data, list) and len(data) > 0:
                if isinstance(data[0], tuple):
                    # IMAP형식: data[0] = (b'1 (RFC822 {1234}', b'...메일내용...')
                    msg = email.message_from_string(decode_data(data[0][1]))
                elif isinstance(data[1], (bytes, bytearray)) and len(data) > 1:
                    # IMAP예외fetch형식: data[1] = bytearray(b'...메일내용...')
                    msg = email.message_from_string(decode_data(data[1]))
        if mail_type == "POP3":
            # POP3형식: data[0] = [b'...메일내용행1...', b'...메일내용행2...', ...]
            # 필요를다중행병합성공일개문자열
            email_content = b"\n".join(data[0])
            msg = email.message_from_string(decode_data(email_content))

        logger.info(f"[AsyncMailTask callback]메일정보: {msg}")

        for part in msg.walk():
            if part.get_content_type() == "text/plain":
                if body is None:
                    body = b""
                body += part.get_payload(decode=True)
            elif part.get_content_type() == "text/html":
                if html is None:
                    html = b""
                html += part.get_payload(decode=True)

            if not has_attachment:
                name = part.get_filename()
                if name:
                    has_attachment = True

        return {
            "from": get_sender_info(msg),  # 전송사람
            "to": get_receiver_info(msg),  # 수신사람
            "subject": get_subject_content(msg),  # 제목
            "body": decode_data(body),  # 문서문자내용
            "html": decode_data(html),  # (정상문서)html정보
            "time": get_mail_time(msg),  # 전송시간
            "has_attachment": has_attachment,  # 여부패키지파일
        }

    def to_trigger(self):
        """가져오기해당유형작업의트리거기기유형"""
        return IntervalTrigger(end_date=self._end_time, **{"minutes": self.interval_time})
