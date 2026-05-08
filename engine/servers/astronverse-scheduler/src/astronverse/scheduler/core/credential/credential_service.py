"""
인증관리관리서비스

사용 keyring 라이브러리설치전체저장비밀번호인증
"""

import json
from typing import Optional

import keyring
import keyring.errors
from astronverse.scheduler.logger import logger

# keyring service names. Keep legacy spelling readable for existing installs.
SERVICE_NAME = "ShopRPA"
LEGACY_SERVICE_NAMES = ("Shoprpa",)

# 검색 key, 사용저장모든인증이름
INDEX_KEY = "__credential_index__"

# 빈비밀번호값, 사용분"빈비밀번호"및"찾을 수 없습니다"
EMPTY_PASSWORD_SENTINEL = "__RPA__Credential__EMPTY__PASSWORD__"


class CredentialService:
    """인증관리관리서비스"""

    # ---------- 내부모듈도구방법법 ----------

    @staticmethod
    def _encode_password(password: str) -> str:
        """확인저장까지 keyring 의비밀번호아니오비어 있습니다"""
        return EMPTY_PASSWORD_SENTINEL if password == "" else password

    @staticmethod
    def _decode_password(stored: Optional[str]) -> Optional[str]:
        """에서 keyring 가져오기출력후기존비밀번호"""
        if stored is None:
            return None
        if stored == EMPTY_PASSWORD_SENTINEL:
            return ""
        return stored

    @staticmethod
    def _get_index() -> list[str]:
        """가져오기 인증이름검색"""
        names: set[str] = set()
        try:
            for service_name in (SERVICE_NAME, *LEGACY_SERVICE_NAMES):
                raw = keyring.get_password(service_name, INDEX_KEY)
                if raw:
                    names.update(json.loads(raw))
            return sorted(names)
        except json.JSONDecodeError:
            return []
        except Exception as e:
            logger.exception(f"가져오기 인증검색실패: {e}")
            return []

    @staticmethod
    def _save_index(names: list[str]):
        """저장인증이름검색"""
        try:
            keyring.set_password(SERVICE_NAME, INDEX_KEY, json.dumps(sorted(set(names))))
        except Exception as e:
            logger.exception(f"저장인증검색실패: {e}")

    @staticmethod
    def _get_raw_credential(name: str) -> Optional[str]:
        """Read a credential from the current service name, then legacy names."""
        for service_name in (SERVICE_NAME, *LEGACY_SERVICE_NAMES):
            stored = keyring.get_password(service_name, name)
            if stored is not None:
                return stored
        return None

    @staticmethod
    def _cleanup_index():
        """
        관리 index 중완료삭제의인증
        에서가져오기유형시트리거, 아니오가능
        """
        names = CredentialService._get_index()
        valid = []

        for name in names:
            stored = CredentialService._get_raw_credential(name)
            if stored is not None:
                valid.append(name)

        if valid != names:
            CredentialService._save_index(valid)

    # ---------- 외부 API ----------

    @staticmethod
    def list_credentials() -> list[dict]:
        """
        가져오기모든인증이름목록(복사검색)

        Returns:
            인증이름목록, 예 [{"name": "admin_password"}, {"name": "db_connection"}]
        """
        try:
            CredentialService._cleanup_index()
            return [{"name": name} for name in CredentialService._get_index()]
        except Exception as e:
            logger.exception(f"가져오기 인증목록실패: {e}")
            return []

    @staticmethod
    def create_credential(name: str, password: str) -> bool:
        """
        생성인증

        Args:
            name: 인증이름
            password: 인증비밀번호(가능으로비어 있습니다문자열)

        Returns:
            여부생성성공

        Raises:
            ValueError: 결과가인증완료존재함
        """
        try:
            if CredentialService.exists(name):
                raise ValueError(f"인증 '{name}' 완료존재함")

            # 코드비밀번호(관리빈비밀번호)
            encoded = CredentialService._encode_password(password)
            keyring.set_password(SERVICE_NAME, name, encoded)

            # 업데이트검색
            names = CredentialService._get_index()
            names.append(name)
            CredentialService._save_index(names)

            logger.info(f"인증 '{name}' 생성성공")
            return True
        except ValueError:
            raise
        except Exception as e:
            logger.exception(f"생성인증실패: {e}")
            return False

    @staticmethod
    def delete_credential(name: str) -> bool:
        """
        삭제인증

        Args:
            name: 인증이름

        Returns:
            삭제 여부성공
        """
        try:
            # 삭제비밀번호(아니오저장된 아니오오류)
            for service_name in (SERVICE_NAME, *LEGACY_SERVICE_NAMES):
                try:
                    keyring.delete_password(service_name, name)
                except keyring.errors.PasswordDeleteError:
                    pass

            # 업데이트검색
            names = CredentialService._get_index()
            if name in names:
                names.remove(name)
                CredentialService._save_index(names)

            logger.info(f"인증 '{name}' 삭제성공")
            return True
        except Exception as e:
            logger.exception(f"삭제인증실패: {e}")
            return False

    @staticmethod
    def exists(name: str) -> bool:
        """
        조회인증여부존재함(으로 keyring 로, 아니오정보검색)

        Args:
            name: 인증이름

        Returns:
            인증여부존재함
        """
        try:
            stored = CredentialService._get_raw_credential(name)
            return stored is not None
        except Exception as e:
            logger.exception(f"조회인증여부존재함실패: {e}")
            return False

    @staticmethod
    def get_credential(name: str) -> str | None:
        """
        가져오기 인증비밀번호(내부모듈사용)

        Args:
            name: 인증이름

        Returns:
            인증비밀번호(찾을 수 없습니다반환 None, 존재함가능반환빈문자열)
        """
        try:
            stored = CredentialService._get_raw_credential(name)
            return CredentialService._decode_password(stored)
        except Exception as e:
            logger.exception(f"가져오기 인증실패: {e}")
            return None
