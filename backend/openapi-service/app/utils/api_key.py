import secrets
import string

import bcrypt

from app.logger import get_logger

logger = get_logger(__name__)


class APIKeyUtils:
    @staticmethod
    def generate_api_key(length=32):
        alphabet = string.ascii_letters + string.digits
        return "".join(secrets.choice(alphabet) for _ in range(length))

    @staticmethod
    def hash_api_key(api_key):
        salt = bcrypt.gensalt()
        hashed = bcrypt.hashpw(api_key.encode("utf-8"), salt)
        return hashed.decode("utf-8")

    @staticmethod
    def verify_api_key(input_key, hashed_key):
        if not input_key or not hashed_key:
            return False
        try:
            return bcrypt.checkpw(input_key.encode("utf-8"), hashed_key.encode("utf-8"))
        except ValueError:
            logger.warning("Stored API key hash is invalid")
            return False
