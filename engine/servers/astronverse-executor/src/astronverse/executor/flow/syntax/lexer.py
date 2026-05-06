from typing import Optional

from astronverse.executor.error import *
from astronverse.executor.flow.syntax import Token
from astronverse.executor.flow.syntax.token import TokenType


class Lexer:
    """법분, 필요예를flow변환성공token, 필터링flow사용의정보"""

    def __init__(self, flow_list: list):
        self.flow_list: list = flow_list  # list dict
        self.position: int = 0
        self.read_position: int = 0
        self.flow: dict = {}

        self.read_flow()  # 

    @staticmethod
    def flow_to_token(flow_json: dict) -> Optional[Token]:
        """를flow변환성공token"""

        token_type = flow_json.get("key", "")

        if not token_type:
            raise BaseException(MISSING_REQUIRED_KEY_ERROR_FORMAT.format(flow_json), f"missing key {flow_json}")
        if token_type in [TokenType.Group.value, TokenType.GroupEnd.value]:
            return
        return Token(type=token_type, value=flow_json)

    def read_flow(self):
        """법분"""

        if self.read_position >= len(self.flow_list):
            self.flow = None
        else:
            self.flow = self.flow_list[self.read_position]
        self.position = self.read_position
        self.read_position += 1

    def next_token(self) -> Token:
        """법분nex_token"""

        while True:
            if self.flow is None:
                token = Token(TokenType.EOF.value)
            else:
                token = self.flow_to_token(self.flow)
            self.read_flow()
            if token is not None:  # 결과가token로None(건너뛰기의), 계속가져오기아래일개
                return token