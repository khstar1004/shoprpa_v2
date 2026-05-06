from astronverse.actionlib import AtomicFormType, AtomicFormTypeMeta, DynamicsItem
from astronverse.actionlib.atomic import atomicMg
from astronverse.actionlib.types import PATH
from astronverse.openapi import utils
from astronverse.openapi.core_shoprpa import OpenapiShoprpa
from astronverse.openapi.error import *


class OpenApi:
    @staticmethod
    @atomicMg.atomic(
        "OpenApi",
        inputList=[
            atomicMg.param(
                "src_file",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={
                        "file_type": "file",
                        "filters": [".jpeg", ".jpg", ".png", ".gif", ".bmp"],
                    },
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.src_file.show",
                        expression="return $this.is_multi.value == false",
                    )
                ],
            ),
            atomicMg.param(
                "src_dir",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"file_type": "folder"},
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.src_dir.show",
                        expression="return $this.is_multi.value == true",
                    )
                ],
            ),
            atomicMg.param(
                "dst_file",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"file_type": "folder"},
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.dst_file.show",
                        expression="return $this.is_save.value == true",
                    )
                ],
            ),
        ],
        outputList=[atomicMg.param("id_card", types="List")],
    )
    def id_card(
        is_multi: bool = True,
        src_file: PATH = "",
        src_dir: PATH = "",
        is_save: bool = True,
        dst_file: PATH = "",
        dst_file_name: str = "id_card",
    ) -> list:
        header_dict = {
            "Address": "주소",
            "Birthday": "출력날짜",
            "Gender": "",
            "ID": "코드",
            "Issuing-authority": "발송기기닫기",
            "Name": "이름",
            "Nation": "",
            "Validity-period": "있음제한",
        }

        src_file = src_file if not is_multi else src_dir
        files = utils.generate_src_files(src_file)
        if len(files) == 0:
            raise BaseException(IMAGE_EMPTY, "이미지경로찾을 수 없습니다또는형식오류")

        res = OpenapiShoprpa.template_ocr(
            header_dict,
            files,
            "id_card",
            "https://api.xf-yun.com/v1/private/s5ccecfce",
            "s5ccecfce",
        )
        if is_save:
            utils.write_to_excel(dst_file, dst_file_name, header_dict, res)
        return res

    @staticmethod
    @atomicMg.atomic(
        "OpenApi",
        inputList=[
            atomicMg.param(
                "src_file",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={
                        "file_type": "file",
                        "filters": [".jpeg", ".jpg", ".png", ".gif", ".bmp"],
                    },
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.src_file.show",
                        expression="return $this.is_multi.value == false",
                    )
                ],
            ),
            atomicMg.param(
                "src_dir",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"file_type": "folder"},
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.src_dir.show",
                        expression="return $this.is_multi.value == true",
                    )
                ],
            ),
            atomicMg.param(
                "dst_file",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"file_type": "folder"},
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.dst_file.show",
                        expression="return $this.is_save.value == true",
                    )
                ],
            ),
        ],
        outputList=[atomicMg.param("business_license", types="List")],
    )
    def business_license(
        is_multi: bool = True,
        src_file: PATH = "",
        src_dir: PATH = "",
        is_save: bool = True,
        dst_file: PATH = "",
        dst_file_name: str = "business_license",
    ):
        header_dict = {
            "Business-scope": "운영",
            "Code": "시스템일정보사용코드",
            "Company-name": "이름",
            "Corporate-residence": "",
            "Date": "성공날짜",
            "Formation": "유형",
            "Operating-period": "운영제한",
            "Paid-in-capital": "본",
            "Registered-capital": "회원가입본",
            "Registration-number": "회원가입번호",
            "Type": "유형",
            "owner-name": "법지정테이블사람",
        }

        src_file = src_file if not is_multi else src_dir
        files = utils.generate_src_files(src_file)
        if len(files) == 0:
            raise BaseException(IMAGE_EMPTY, "이미지경로찾을 수 없습니다또는형식오류")

        res = OpenapiShoprpa.template_ocr(
            header_dict,
            files,
            "bus_license",
            "https://api.xf-yun.com/v1/private/sff4ea3cf",
            "sff4ea3cf",
        )
        if is_save:
            utils.write_to_excel(dst_file, dst_file_name, header_dict, res)
        return res

    @staticmethod
    @atomicMg.atomic(
        "OpenApi",
        inputList=[
            atomicMg.param(
                "src_file",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={
                        "file_type": "file",
                        "filters": [".jpeg", ".jpg", ".png", ".gif", ".bmp"],
                        "defaultPath": "미완료명령이름.xls",
                    },
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.src_file.show",
                        expression="return $this.is_multi.value == false",
                    )
                ],
            ),
            atomicMg.param(
                "src_dir",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"file_type": "folder"},
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.src_dir.show",
                        expression="return $this.is_multi.value == true",
                    )
                ],
            ),
            atomicMg.param(
                "dst_file",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"file_type": "folder"},
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.dst_file.show",
                        expression="return $this.is_save.value == true",
                    )
                ],
            ),
        ],
        outputList=[atomicMg.param("vat_invoice", types="List")],
    )
    def vat_invoice(
        is_multi: bool = True,
        src_file: PATH = "",
        src_dir: PATH = "",
        is_save: bool = True,
        dst_file: PATH = "",
        dst_file_name: str = "vat_invoice",
    ) -> list:
        header_dict = {
            "check-code": "인증 코드",
            "cryptographic-area": "비밀번호",
            "unit-price": "단일가격",
            "vat-invoice-daima": "발송코드",
            "vat-invoice-daima-right-side": "발송코드-오른쪽",
            "vat-invoice-goods-list": "물품또는서비스, 서비스이름",
            "vat-invoice-haoma": "발송코드",
            "vat-invoice-haoma-right-side": "발송코드-오른쪽",
            "vat-invoice-issue-date": "열기 날짜",
            "vat-invoice-payer-addr-tell": "구매방법주소",
            "vat-invoice-payer-bank-account": "구매방법열기사용자행계정",
            "vat-invoice-payer-name": "구매방법이름",
            "vat-invoice-price-list": "금액",
            "vat-invoice-rate-payer-id": "구매방법사람",
            "vat-invoice-seller-addr-tell": "판매방법주소, ",
            "vat-invoice-seller-bank-account": "판매방법열기사용자행계정",
            "vat-invoice-seller-id": "판매방법",
            "vat-invoice-seller-name": "판매방법이름",
            "vat-invoice-tax-list": "금액",
            "vat-invoice-tax-rate-list": "",
            "vat-invoice-tax-total": "금액합치기계획",
            "vat-invoice-total": "금액합치기계획",
            "vat-invoice-total-cover-tax": "가격합치기계획",
            "vat-invoice-total-cover-tax-digits": "가격합치기계획소",
        }

        src_file = src_file if not is_multi else src_dir
        files = utils.generate_src_files(src_file)
        if len(files) == 0:
            raise BaseException(IMAGE_EMPTY, "이미지경로찾을 수 없습니다또는형식오류")

        res = OpenapiShoprpa.template_ocr(
            header_dict,
            files,
            "vat_invoice",
            "https://api.xf-yun.com/v1/private/s824758f1",
            "s824758f1",
        )
        if is_save:
            utils.write_to_excel(dst_file, dst_file_name, header_dict, res)
        return res

    @staticmethod
    @atomicMg.atomic(
        "OpenApi",
        inputList=[
            atomicMg.param(
                "src_file",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={
                        "file_type": "file",
                        "filters": [".jpeg", ".jpg", ".png", ".gif", ".bmp"],
                    },
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.src_file.show",
                        expression="return $this.is_multi.value == false",
                    )
                ],
            ),
            atomicMg.param(
                "src_dir",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"file_type": "folder"},
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.src_dir.show",
                        expression="return $this.is_multi.value == true",
                    )
                ],
            ),
            atomicMg.param(
                "dst_file",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"file_type": "folder"},
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.dst_file.show",
                        expression="return $this.is_save.value == true",
                    )
                ],
            ),
        ],
        outputList=[atomicMg.param("train_ticket", types="List")],
    )
    def train_ticket(
        is_multi: bool = True,
        src_file: PATH = "",
        src_dir: PATH = "",
        is_save: bool = True,
        dst_file: PATH = "",
        dst_file_name: str = "train_ticket",
    ) -> list:
        header_dict = {
            "Number1": "",
            "Ticket-check": "",
            "Station-From": "",
            "Number2-Train": "차",
            "Station-To": "목록의",
            "Date": "발송차날짜",
            "Time": "열기차시간",
            "Seat": "위치",
            "Number3-Amount": "가격",
            "Seat-type": "위치유형",
            "Number4-Identity": "인증코드",
            "Name": "차사람이름",
            "Number5": "코드숫자",
        }

        src_file = src_file if not is_multi else src_dir
        files = utils.generate_src_files(src_file)
        if len(files) == 0:
            raise BaseException(IMAGE_EMPTY, "이미지경로찾을 수 없습니다또는형식오류")

        res = OpenapiShoprpa.template_ocr(
            header_dict,
            files,
            "train_ticket",
            "https://api.xf-yun.com/v1/private/s19cfe728",
            "s19cfe728",
        )
        if is_save:
            utils.write_to_excel(dst_file, dst_file_name, header_dict, res)
        return res

    @staticmethod
    @atomicMg.atomic(
        "OpenApi",
        inputList=[
            atomicMg.param(
                "src_file",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={
                        "file_type": "file",
                        "filters": [".jpeg", ".jpg", ".png", ".gif", ".bmp"],
                    },
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.src_file.show",
                        expression="return $this.is_multi.value == false",
                    )
                ],
            ),
            atomicMg.param(
                "src_dir",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"file_type": "folder"},
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.src_dir.show",
                        expression="return $this.is_multi.value == true",
                    )
                ],
            ),
            atomicMg.param(
                "dst_file",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"file_type": "folder"},
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.dst_file.show",
                        expression="return $this.is_save.value == true",
                    )
                ],
            ),
        ],
        outputList=[atomicMg.param("taxi_ticket", types="List")],
    )
    def taxi_ticket(
        is_multi: bool = True,
        src_file: PATH = "",
        src_dir: PATH = "",
        is_save: bool = True,
        dst_file: PATH = "",
        dst_file_name: str = "taxi_ticket",
    ) -> list:
        header_dict = {
            "Plate-number": "차브랜드",
            "Date": "날짜",
            "Time": "위아래차시간",
            "Number3_price": "단일가격",
            "Number4_mileage": "",
            "Number5_amount": "금액",
            "Number2_code": "코드",
            "Number1_code": "코드",
            "unknown_type": "지원하지 않는유형",
        }

        src_file = src_file if not is_multi else src_dir
        files = utils.generate_src_files(src_file)
        if len(files) == 0:
            raise BaseException(IMAGE_EMPTY, "이미지경로찾을 수 없습니다또는형식오류")

        res = OpenapiShoprpa.template_ocr(
            header_dict,
            files,
            "taxi_ticket",
            "https://api.xf-yun.com/v1/private/sb6db0171",
            "sb6db0171",
        )
        if is_save:
            utils.write_to_excel(dst_file, dst_file_name, header_dict, res)
        return res

    @staticmethod
    @atomicMg.atomic(
        "OpenApi",
        inputList=[
            atomicMg.param(
                "src_file",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={
                        "file_type": "file",
                        "filters": [".jpeg", ".jpg", ".png", ".gif", ".bmp"],
                    },
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.src_file.show",
                        expression="return $this.is_multi.value == false",
                    )
                ],
            ),
            atomicMg.param(
                "src_dir",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={
                        "file_type": "folder",  # ["file", "files", "folder"] 중의일개
                    },
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.src_dir.show",
                        expression="return $this.is_multi.value == true",
                    )
                ],
            ),
            atomicMg.param(
                "dst_file",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={
                        "file_type": "folder",  # ["file", "files", "folder"] 중의일개
                    },
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.dst_file.show",
                        expression="return $this.is_save.value == true",
                    )
                ],
            ),
        ],
        outputList=[atomicMg.param("common_ocr", types="List")],
    )
    def common_ocr(
        is_multi: bool = False,
        src_file: PATH = "",
        src_dir: PATH = "",
        is_save: bool = True,
        dst_file: PATH = "",
        dst_file_name: str = "common_ocr",
    ) -> list:
        header_dict = {
            "Context": "이미지문서결과",
        }

        src_file = src_file if not is_multi else src_dir
        files = utils.generate_src_files(src_file)
        if len(files) == 0:
            raise BaseException(IMAGE_EMPTY, "이미지경로찾을 수 없습니다또는형식오류")

        res = OpenapiShoprpa.common_ocr(header_dict, files)
        if is_save:
            utils.write_to_excel(dst_file, dst_file_name, header_dict, res)
        return res
