"""암호화닫기 열기 유형.

일반사용암호화/코드시필요의선택, 외부모듈호출시스템일사용, 법문자열.
"""

from enum import Enum

__all__ = [
    "Base64CodeType",
    "EncryptCaseType",
    "MD5bitsType",
    "SHAType",
]


class MD5bitsType(Enum):
    """MD5 출력위치데이터.

    - MD5_32:  32 위치십육제어테이블
    - MD5_16: 시스템(가져오기 32 위치중 16 위치)테이블
    """

    MD5_32 = "32"
    MD5_16 = "16"


class EncryptCaseType(Enum):
    """암호화출력크기제어."""

    LOWER = "lower"
    UPPER = "upper"


class SHAType(Enum):
    """SHA 시스템열법유형.패키지 SHA1 / SHA2 / SHA3 일반변수."""

    SHA1 = "sha1"
    SHA224 = "sha224"
    SHA256 = "sha256"
    SHA384 = "sha384"
    SHA512 = "sha512"
    SHA3_224 = "sha3_224"
    SHA3_256 = "sha3_256"
    SHA3_384 = "sha3_384"
    SHA3_512 = "sha3_512"


class Base64CodeType(Enum):
    """Base64 입력/출력데이터 유형."""

    STRING = "string"
    PICTURE = "picture"