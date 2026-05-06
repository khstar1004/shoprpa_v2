"""기존의암호화가능설치.

패키지설치 `EncryptCore` 의로기존(atomicMg)가능호출방식, 
 MD5 / SHA / AES  Base64 해제코드기존가능.
"""

import os

from astronverse.actionlib import (
    AtomicFormType,
    AtomicFormTypeMeta,
    AtomicLevel,
    DynamicsItem,
)
from astronverse.actionlib.atomic import atomicMg
from astronverse.actionlib.utils import FileExistenceType, handle_existence
from astronverse.encrypt import Base64CodeType, EncryptCaseType, MD5bitsType, SHAType
from astronverse.encrypt.core import EncryptCore


class Encrypt:  # pylint: disable=too-few-public-methods
    """외부의암호화기존합치기.

    통신경과 atomic 설치기기를패키지설치성공프로세스가능정렬의기존.모든방법법로
    없음상태방법법, 시도및복사사용.
    """

    @staticmethod
    @atomicMg.atomic("Encrypt", outputList=[atomicMg.param("md5_encrypted_result", types="Str")])
    def md5_encrypt(
        source_str: str,
        md5_method: MD5bitsType = MD5bitsType.MD5_32,
        case_method: EncryptCaseType = EncryptCaseType.LOWER,
    ) -> str:
        """MD5 필요.

        매개변수:
            source_str: 대기암호화문자열.
            md5_method: 32 / 16 위치출력.
            case_method: 크기제어.
        반환: 계획후의 MD5 문자열(또는빈문자열 source_str 비어 있습니다).
        """
        if source_str:
            md5_encrypted_result = EncryptCore.md5_encrypt(source_str, md5_method, case_method)
            return md5_encrypted_result
        return ""

    @staticmethod
    @atomicMg.atomic("Encrypt", outputList=[atomicMg.param("sha_encrypted_result", types="Str")])
    def sha_encrypt(
        source_str: str,
        sha_method: SHAType = SHAType.SHA1,
        case_method: EncryptCaseType = EncryptCaseType.LOWER,
    ) -> str:
        """SHA / SHA3 필요.

        매개변수:
            source_str: 대기관리문자열
            sha_method: 법유형 (SHA1 / SHA2 / SHA3*)
            case_method: 크기제어
        반환: 결과문자열(또는빈문자열).
        """
        if source_str:
            sha_encrypted_result = EncryptCore.sha_encrypt(source_str, sha_method, case_method)
            return sha_encrypted_result
        return ""

    @staticmethod
    @atomicMg.atomic(
        "Encrypt",
        outputList=[atomicMg.param("symmetric_encrypted_result", types="Str")],
    )
    def symmetric_encrypt(source_str: str, password: str = "") -> str:
        """AES 명칭암호화(CBC)."""
        if source_str:
            symmetric_encrypted_result = EncryptCore.symmetric_encrypt(source_str, password)
            return symmetric_encrypted_result
        return ""

    @staticmethod
    @atomicMg.atomic(
        "Encrypt",
        outputList=[atomicMg.param("symmetric_decrypted_result", types="Str")],
    )
    def symmetric_decrypt(source_str: str, password: str = "") -> str:
        """AES 명칭복호화(CBC)."""
        if source_str:
            symmetric_decrypted_result = EncryptCore.symmetric_decrypt(source_str, password)
            return symmetric_decrypted_result
        return ""

    # --------------Base64-----------------
    @staticmethod
    @atomicMg.atomic(
        "Encrypt",
        inputList=[
            atomicMg.param(
                "string_data",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.string_data.show",
                        expression=(f"return $this.encode_type.value == '{Base64CodeType.STRING.value}'"),
                    )
                ],
            ),
            atomicMg.param(
                "file_path",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.file_path.show",
                        expression=(f"return $this.encode_type.value == '{Base64CodeType.PICTURE.value}'"),
                    )
                ],
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "file"},
                ),
            ),
        ],
        outputList=[atomicMg.param("encoded_string", types="Str")],
    )
    def base64_encoding(
        encode_type: Base64CodeType = Base64CodeType.STRING,
        string_data: str = "",
        file_path: str = "",
    ):
        """Base64 코드기존가능패키지설치.

        - STRING: 직선연결 `string_data` 코드
        - PICTURE: 가져오기 `file_path` 파일코드로 data URI
        """
        if encode_type == Base64CodeType.PICTURE and not os.path.exists(file_path):
            raise ValueError("이미지파일찾을 수 없습니다!")
        encoded_string = EncryptCore.base64_encode(encode_type, string_data, file_path)
        return encoded_string

    @staticmethod
    @atomicMg.atomic(
        "Encrypt",
        inputList=[
            atomicMg.param(
                "file_path",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.file_path.show",
                        expression=(f"return $this.decode_type.value == '{Base64CodeType.PICTURE.value}'"),
                    )
                ],
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "folder"},
                ),
            ),
            atomicMg.param(
                "file_name",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.file_name.show",
                        expression=(f"return $this.decode_type.value == '{Base64CodeType.PICTURE.value}'"),
                    )
                ],
            ),
            atomicMg.param(
                "exist_handle_type",
                level=AtomicLevel.ADVANCED,
                dynamics=[
                    DynamicsItem(
                        key="$this.exist_handle_type.show",
                        expression=(f"return $this.decode_type.value == '{Base64CodeType.PICTURE.value}'"),
                    )
                ],
            ),
        ],
        outputList=[atomicMg.param("decoded_string", types="Str")],
    )
    def base64_decoding(
        decode_type: Base64CodeType = Base64CodeType.STRING,
        string_data: str = "",
        file_path: str = "",
        file_name: str = "",
        exist_handle_type: FileExistenceType = FileExistenceType.RENAME,
    ):
        """Base64 해제코드기존가능패키지설치.

        - STRING: 반환해제코드후의문자열
        - PICTURE: 입력파일(관리이름)후반환종료파일 경로
        """
        new_file_path: str = (
            os.path.join(file_path, f"{file_name}.png") if decode_type == Base64CodeType.PICTURE else ""
        )
        if new_file_path:
            # handle_existence 가능반환 None, 사용짧음경로보관기존값
            new_file_path = handle_existence(new_file_path, exist_handle_type) or new_file_path
        decoded_string = EncryptCore.base64_decode(
            decode_type,
            string_data,
            new_file_path,
        )
        return decoded_string