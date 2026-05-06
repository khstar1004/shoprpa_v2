"""email기존가능"""

import copy

from astronverse.actionlib import AtomicFormType, AtomicFormTypeMeta, AtomicLevel, DynamicsItem
from astronverse.actionlib.atomic import atomicMg
from astronverse.baseline.logger.logger import logger
from astronverse.email import EmailSeenType, EmailServerType


class Email:
    """email기존가능"""

    @staticmethod
    @atomicMg.atomic(
        "Email",
        inputList=[
            atomicMg.param("cc", required=False),
            atomicMg.param(
                "content",
                required=False,
                formType=AtomicFormTypeMeta(type=AtomicFormType.CONTENTPASTE.value),
            ),
            atomicMg.param(
                "attachment_path",
                required=False,
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "files"},
                ),
            ),
            atomicMg.param("bcc", required=False),
            atomicMg.param(
                "replace_table",
                required=False,
                formType=AtomicFormTypeMeta(type=AtomicFormType.MODALBUTTON.value),
                need_parse="json_str",
            ),
            atomicMg.param("send_name", required=False),
            atomicMg.param(
                "other_mail_server",
                dynamics=[
                    DynamicsItem(
                        key="$this.other_mail_server.show",
                        expression="return $this.mail_server.value == '{}'".format(EmailServerType.OTHER.value),
                    )
                ],
                required=False,
            ),
            atomicMg.param("mail_port", required=False),
        ],
        outputList=[],
    )
    def send_email(
        receiver: str = "",
        cc: str = "",
        subject: str = "",
        is_html: bool = False,
        content: str = "",
        attachment_path: str = "",
        mail_server: EmailServerType = EmailServerType.QQ,
        other_mail_server: str = "",
        mail_port: int = 465,
        use_ssl: bool = True,
        sender_mail: str = "",
        send_name: str = "",
        password: str = "",
        bcc: str = "",
        replace_table: str = "",
    ):
        """메일전송기존가능"""
        mail_server_dict = {
            EmailServerType.QQ: "smtp.qq.com",
            EmailServerType.NETEASE_163: "smtp.163.com",
            EmailServerType.NETEASE_126: "smtp.126.com",
            EmailServerType.SHOPRPA: "mail.shoprpa.com",
            EmailServerType.OTHER: other_mail_server,
        }
        from astronverse.email.core_smtp_send import EmailSmtpSend

        core = EmailSmtpSend()
        core.login(
            server=mail_server_dict.get(mail_server),
            port=mail_port,
            user=sender_mail,
            password=password,
            use_ssl=use_ssl,
        )

        # 
        content = copy.deepcopy(content)
        try:
            if replace_table and isinstance(replace_table, list):
                for replace in replace_table:
                    content = content.replace(str(replace.get("origintext")), str(replace.get("replacetext")))  # type: ignore
        except Exception:
            pass
        core.send(
            user=sender_mail,
            user_name=send_name,
            receiver=receiver,
            cc=cc,
            bcc=bcc,
            content=content,
            content_is_html=is_html,
            subject=subject,
            attachment_path=attachment_path,
        )

    @staticmethod
    @atomicMg.atomic(
        "Email",
        inputList=[
            atomicMg.param(
                key="custom_mail_server",
                level=AtomicLevel.NORMAL,
                dynamics=[
                    DynamicsItem(
                        key="$this.custom_mail_server.show",
                        expression="return $this.mail_server.value == '{}'".format(EmailServerType.OTHER.value),
                    )
                ],
                types="Str",
            ),
            atomicMg.param(
                key="custom_mail_port",
                level=AtomicLevel.NORMAL,
                dynamics=[
                    DynamicsItem(
                        key="$this.custom_mail_port.show",
                        expression="return $this.mail_server.value == '{}'".format(EmailServerType.OTHER.value),
                    )
                ],
                types="Str",
            ),
            atomicMg.param("user_mail", level=AtomicLevel.NORMAL, types="Str", required=True),
            atomicMg.param("user_password", level=AtomicLevel.NORMAL, types="Str", required=True),
            atomicMg.param(
                "unseen_flag",
                formType=AtomicFormTypeMeta(type=AtomicFormType.CHECKBOX.value),
                level=AtomicLevel.NORMAL,
                required=True,
            ),
            atomicMg.param(
                "save_attachment_flag",
                formType=AtomicFormTypeMeta(type=AtomicFormType.CHECKBOX.value),
                level=AtomicLevel.NORMAL,
                required=True,
            ),
            atomicMg.param(
                key="save_attachment_path",
                level=AtomicLevel.NORMAL,
                dynamics=[
                    DynamicsItem(
                        key="$this.save_attachment_path.show",
                        expression="return $this.save_attachment_flag.value == true",
                    )
                ],
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "folder"},
                ),
                types="Str",
            ),
            atomicMg.param("folder_name", types="Str", required=True),
            atomicMg.param(
                "mask_as_read_flag",
                formType=AtomicFormTypeMeta(type=AtomicFormType.CHECKBOX.value),
                required=True,
            ),
            atomicMg.param("sender_text", required=False, types="Str"),
            atomicMg.param("receiver_text", required=False, types="Str"),
            atomicMg.param("theme_text", required=False, types="Str"),
            atomicMg.param("content_text", required=False, types="Str"),
        ],
        outputList=[atomicMg.param("mail_list", types="Dict")],
    )
    def receive_email(
        mail_server: EmailServerType = EmailServerType.QQ,
        custom_mail_server: str = "",
        custom_mail_port: int = 993,
        user_mail: str = "",
        user_password: str = "",
        max_return_num: int = 5,
        unseen_flag: bool = False,
        save_attachment_flag: bool = False,
        save_attachment_path: str = "",
        folder_name: str = "INBOX",
        mask_as_read_flag: bool = False,
        sender_text: str = "",
        receiver_text: str = "",
        theme_text: str = "",
        content_text: str = "",
    ):
        """
        메일수신기존가능
        user_mail: `str`, 사용자주소정보
        user_password: `str`, 사용자비밀번호정보
        custom_mail_server: `str`, 사용자 입력삼방법상업주소
        custom_mail_port: `int`, 사용자 입력단말
        folder_name: `str`, 메일디렉터리
        mail_server: `EmailServerType`, 의상업서비스서버유형

        max_return_num: `int`, 메일대반환수
        sender_text: `str`, 통신경과전송패키지닫기 문자행필터링
        receiver_text: `str`, 통신경과수신패키지닫기 문자행필터링
        theme_text: `str`, 통신경과제목패키지닫기 문자행필터링
        content_text: `str`, 통신경과내용패키지닫기 문자행필터링

        save_attachment_flag: `bool`, 여부저장파일
        save_attachment_path: `str`, 파일저장경로
        unseen_flag: `str`, 여부미완료메일
        mask_as_read_flag: `bool`, 여부를경과의메일로완료
        """
        # 매개변수
        if max_return_num == 0:
            return []

        mail_server_dict = {
            EmailServerType.QQ.value: "imap.qq.com",
            EmailServerType.NETEASE_163.value: "imap.163.com",
            EmailServerType.NETEASE_126.value: "imap.126.com",
            EmailServerType.SHOPRPA.value: "mail.shoprpa.com",
            EmailServerType.OTHER.value: custom_mail_server,
        }
        from astronverse.email.core_imap4_receive import EmailImap4Receive

        core = EmailImap4Receive()
        core.login(
            server=mail_server_dict.get(mail_server.value),
            port=custom_mail_port,
            user=user_mail,
            password=user_password,
            # use_ssl=True,
        )
        core.select(selector=folder_name)

        searched_mail_ids = core.search(
            "utf-8",
            EmailSeenType.ALL.value if not unseen_flag else EmailSeenType.UNSEEN.value,
        )[1][0].split(b" ")

        matched_mails = []
        for num in reversed(searched_mail_ids):
            if not num or num == b"":
                continue
            try:
                logger.info(f"mail id:{str(num)}")
                mail_info = core.get_entire_mail_info(num)
            except Exception as e:
                logger.info(f"im error: {str(e)}")
                continue
            # id여부기호합치기조회필요
            if (
                (not (sender_text or receiver_text or theme_text or content_text))
                or sender_text
                and mail_info["from"]
                and sender_text in " ".join([item for item in mail_info["from"] if item])
                or receiver_text
                and mail_info["to"]
                and receiver_text in " ".join([item for item in mail_info["to"] if item])
                or theme_text
                and mail_info["subject"]
                and theme_text in mail_info["subject"]
                or content_text
                and mail_info["body"]
                and content_text in mail_info["body"]
            ):
                logger.info(f"im append: {str(num)}")
                matched_mails.append((num, mail_info))
            else:
                logger.info(f"im continue: {str(num)}")
                continue

            # 가득대반환파일가능결과
        # Use parsed mail time for ordering instead of relying on provider-specific SEARCH order.
        matched_mails.sort(key=lambda item: item[1].get("time") or "", reverse=True)
        matched_mails = matched_mails[:max_return_num]
        logger.info(f"mail id:{[mail_id for mail_id, _ in matched_mails]}")

        # 가져오기메일정보, 저장파일
        return_mail_res = []
        from pathlib import Path

        for mail_id, mail_info in matched_mails:
            # 여부필요저장파일
            if save_attachment_flag:
                for attachment in mail_info["attachments"]:
                    if not attachment:
                        continue
                    file_path = Path(save_attachment_path) / attachment["name"]
                    file_path.write_bytes(attachment["data"])
            # 여부필요비고로완료
            if mask_as_read_flag:
                core.mask_as_read(mail_id)
            mail_info["attachments"] = [
                attachment["name"] for attachment in mail_info["attachments"] if attachment and attachment["name"]
            ]
            return_mail_res.append(mail_info)
        return return_mail_res
