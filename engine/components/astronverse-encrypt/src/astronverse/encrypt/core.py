"""암호화데이터합치기.

패키지 MD5, SHA, AES 명칭추가복호화으로 Base64 코드/해제코드대기가능.
"""

import base64
import hashlib
from collections.abc import Callable
from pathlib import Path

from astronverse.encrypt import (
    Base64CodeType,
    EncryptCaseType,
    MD5bitsType,
    SHAType,
)
from Cryptodome.Cipher import AES


class EncryptCore:  # pylint: disable=too-few-public-methods
    """암호화도구유형.모든방법법로데이터방식, 아니오상태."""

    # ---- Hash 시스템열 ----
    @staticmethod
    def md5_encrypt(
        source_str: str,
        md5_method: MD5bitsType = MD5bitsType.MD5_32,
        case_method: EncryptCaseType = EncryptCaseType.LOWER,
    ) -> str:
        """문자열행 MD5 필요.

        매개변수:
            source_str: 대기관리문자열.
            md5_method: 16 또는 32 위치출력.
            case_method: 출력크기제어.
        """
        md5_obj = hashlib.md5()
        md5_obj.update(source_str.encode("utf8"))

        if md5_method == MD5bitsType.MD5_32:
            digest = md5_obj.hexdigest()
        else:  # MD5_16
            digest = md5_obj.hexdigest()[8:-8]

        if case_method == EncryptCaseType.UPPER:
            return digest.upper()
        return digest.lower()

    @staticmethod
    def sha_encrypt(
        source_str: str,
        sha_method: SHAType = SHAType.SHA1,
        case_method: EncryptCaseType = EncryptCaseType.LOWER,
    ) -> str:
        """문자열행 SHA (* 및 SHA3 *) 필요.

        사용딕셔너리적음분가능의지정되지 않았습니다변수."""
        sha_map: dict[SHAType, Callable[[], hashlib._Hash]] = {
            SHAType.SHA1: hashlib.sha1,
            SHAType.SHA224: hashlib.sha224,
            SHAType.SHA256: hashlib.sha256,
            SHAType.SHA384: hashlib.sha384,
            SHAType.SHA512: hashlib.sha512,
            SHAType.SHA3_224: hashlib.sha3_224,
            SHAType.SHA3_256: hashlib.sha3_256,
            SHAType.SHA3_384: hashlib.sha3_384,
            SHAType.SHA3_512: hashlib.sha3_512,
        }
        sha_func = sha_map[sha_method]
        sha_obj = sha_func()
        sha_obj.update(source_str.encode("utf8"))
        digest = sha_obj.hexdigest()
        if case_method == EncryptCaseType.UPPER:
            return digest.upper()
        return digest.lower()

    # ---- AES 명칭암호화 ----
    @staticmethod
    def symmetric_encrypt(source_str: str, password: str = "") -> str:
        """사용 AES-CBC 방식명칭암호화.

        키및 IV 직선연결 password(까지 16 의데이터)."""

        def pad16(value: str) -> bytes:
            while len(value.encode("utf-8")) % 16 != 0:
                value += "\0"
            return value.encode("utf-8")

        if not source_str:
            raise ValueError("암호화객체비워 둘 수 없습니다!")
        if not isinstance(source_str, str):  # 방식
            raise ValueError("요청 문자열유형객체!")

        password = str(password)
        iv = password
        aes = AES.new(pad16(password), AES.MODE_CBC, pad16(iv))
        encrypt_aes = aes.encrypt(pad16(source_str))
        return str(base64.b64encode(encrypt_aes), encoding="utf-8")

    @staticmethod
    def symmetric_decrypt(source_str: str, password: str = "") -> str:
        """AES-CBC 복호화.입력필요로 Base64 코드후의비밀문서."""

        def pad16(value: str) -> bytes:
            while len(value.encode("utf-8")) % 16 != 0:
                value += "\0"
            return value.encode("utf-8")

        if not source_str:
            raise ValueError("복호화객체비워 둘 수 없습니다!")
        if not isinstance(source_str, str):
            raise ValueError("요청 문자열유형객체!")

        password = str(password)
        iv = password
        aes = AES.new(pad16(password), AES.MODE_CBC, pad16(iv))
        base64_decrypted = base64.decodebytes(source_str.encode("utf-8"))
        return str(aes.decrypt(base64_decrypted), encoding="utf-8").replace("\0", "")

    # ---- Base64 ----
    @staticmethod
    def base64_encode(
        encode_type: Base64CodeType = Base64CodeType.STRING,
        string_data: str = "",
        file_path: str = "",
    ) -> str:
        """Base64 코드.가능관리문자열또는파일.

         encode_type == PICTURE 시, 증가추가 data URI 전."""
        if encode_type == Base64CodeType.PICTURE:
            input_content = Path(file_path).read_bytes()
        else:
            input_content = string_data.encode("utf-8")
        base64_encoded = base64.b64encode(input_content)
        result = base64_encoded.decode("utf-8")
        if encode_type == Base64CodeType.PICTURE:
            return f"data:image/png;base64,{result}"
        return result

    @staticmethod
    def base64_decode(
        decode_type: Base64CodeType = Base64CodeType.STRING,
        string_data: str = "",
        file_path: str = "",
    ) -> str:
        """Base64 해제코드.

         decode_type == PICTURE  file_path 시, 를해제코드후의이미지입력파일반환파일 경로.
        아니오이면반환해제코드후의문자열."""

        def pad4(value: str) -> str:
            return value + "=" * ((4 - len(value) % 4) % 4)

        if decode_type == Base64CodeType.STRING:
            decoded = base64.b64decode(pad4(string_data))
            return str(decoded, "utf-8")

        if file_path:
            Path(file_path).write_bytes(base64.b64decode(string_data.replace("data:image/png;base64,", "")))
        return file_path
