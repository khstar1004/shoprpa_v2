import base64
import re
import subprocess

import pyperclip
from astronverse.baseline.logger.logger import logger
from astronverse.system.core.clipboard_core import IClipBoardCore
from astronverse.system.error import *


class ClipBoardCore(IClipBoardCore):
    @staticmethod
    def copy_str_clip(data: str = ""):
        return pyperclip.copy(data)

    @staticmethod
    def paste_str_clip() -> str:
        return pyperclip.paste()

    @staticmethod
    def clear_clip():
        return pyperclip.copy("")

    @staticmethod
    def copy_file_clip(file_path: str = ""):
        pyperclip.copy(file_path)
        subprocess.run(
            ["powershell", "-command", f'set-Clipboard -Path "{file_path}"'],
            stdin=subprocess.DEVNULL,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )

    @staticmethod
    def paste_file_clip() -> str:
        import win32clipboard as cp

        cp.OpenClipboard()
        try:
            file_list = cp.GetClipboardData(cp.CF_HDROP)
        except TypeError as e:
            raise BaseException(
                CONTENT_TYPE_ERROR_FORMAT,
                "잘라내기중내용로텍스트내용, 확인하세요잘라내기내용 가져오기유형여부정상",
            )
        file_path = file_list[0]
        try:
            cp.CloseClipboard()
        except Exception as e:
            raise e
        return file_path

    @staticmethod
    def __extract_html_fragment__(html_clipboard_data):
        html_clipboard_str = html_clipboard_data.decode("utf-8")

        start_marker = "<!--StartFragment-->"
        end_marker = "<!--EndFragment-->"

        start_index = html_clipboard_str.find(start_marker)
        end_index = html_clipboard_str.find(end_marker)

        if start_index == -1 or end_index == -1:
            # 사용정상이면테이블방식행매칭
            # 일소의예방식
            match_start = re.search(r"StartHTML:(\d+)\r", html_clipboard_str).group(1)
            match_end = re.search(r"EndHTML:(\d+)\r", html_clipboard_str).group(1)
            return html_clipboard_str[int(match_start) : int(match_end)]

        start_offset = start_index + len(start_marker)
        end_offset = end_index
        # Extract the HTML fragment using the offsets
        html_fragment = html_clipboard_str[start_offset:end_offset]
        return html_fragment

    @staticmethod
    def paste_html_clip() -> str:
        import win32clipboard as cp

        html_data = ""
        cp.OpenClipboard()
        try:
            html = cp.RegisterClipboardFormat("HTML Format")
            if cp.IsClipboardFormatAvailable(html):
                html_data = cp.GetClipboardData(html)
        except Exception as e:
            logger.debug("No HTML content found in clipboard.")
        finally:
            cp.CloseClipboard()

        if html_data:
            html_fragment = ClipBoardCoreWin.__extract_html_fragment__(html_data)
            if html_fragment:
                # 정상이면테이블방식방식, 매칭 src=" 및 " 의내용
                pattern = r'src="file:///(.*?\.(?:jpg|png|gif))"'
                # 사용 re.findall 조회모든매칭
                matches = re.findall(pattern, html_fragment)
                for match in matches:
                    with open(match, "rb") as file:
                        input_content = file.read()
                    base64_encoded = base64.b64encode(input_content)
                    base64_encode_result = base64_encoded.decode("utf-8")
                    base64_encode_result = "data:image/png;base64," + base64_encode_result
                    html_fragment = html_fragment.replace(r"file:///" + match, base64_encode_result)
                return html_fragment
            else:
                logger.debug("Failed to extract HTML fragment.")
                return html_data
        else:
            # HTML형식반환텍스트
            return ClipBoardCore.paste_str_clip()