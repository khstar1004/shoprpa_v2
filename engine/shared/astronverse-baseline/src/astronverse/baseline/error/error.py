from dataclasses import dataclass
from enum import Enum


class BizCode(Enum):
    LocalOK = "0000"
    LocalErr = "1001"


@dataclass
class ErrorCode:
    code: BizCode  # Business code
    message: str
    httpcode: int = 200

    def format(self, *args, **kwargs):
        return ErrorCode(self.code, self.message.format(*args, **kwargs), self.httpcode)


class BaseException(Exception):
    def __init__(self, code: ErrorCode, message: str):
        self.code = code
        self.message = message
        super().__init__(self.code.message)
