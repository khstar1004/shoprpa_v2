import pyperclip


class Clipboard:
    """목록전문자열있음"""

    @staticmethod
    def copy(data: str = ""):
        """
        잘라내기
        :param data:
        :return:
        """
        return pyperclip.copy(data)

    @staticmethod
    def paste() -> str:
        """
        가져오기잘라내기
        :return:
        """
        return pyperclip.paste()

    @staticmethod
    def clear():
        """
        빈잘라내기
        :return:
        """
        return pyperclip.copy("")