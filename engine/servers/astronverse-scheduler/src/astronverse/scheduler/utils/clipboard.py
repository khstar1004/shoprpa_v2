import re
import subprocess
import sys
from enum import Enum

import pyperclip


class Base64CodeType(Enum):
    STRING = "string"
    PICTURE = "picture"


class Clipboard:
    @staticmethod
    def paste_str_clip() -> str:
        """
        가져오기잘라내기
        :return:
        """
        return pyperclip.paste()

    @staticmethod
    def paste_html_clip() -> str:
        if sys.platform != "win32":
            try:
                result = subprocess.run(
                    ["xclip", "-selection", "clipboard", "-o", "-t", "text/html"],
                    capture_output=True,
                    text=True,
                    encoding="utf-8",
                    errors="replace",
                    check=False,
                )
                return result.stdout or Clipboard.paste_str_clip()
            except FileNotFoundError:
                return Clipboard.paste_str_clip()

        import win32clipboard as cp

        html_data = ""
        cp.OpenClipboard()
        try:
            CF_HTML = cp.RegisterClipboardFormat("HTML Format")
            if cp.IsClipboardFormatAvailable(CF_HTML):
                html_data = cp.GetClipboardData(CF_HTML)
        except:
            pass
        finally:
            cp.CloseClipboard()

        if html_data:
            html_fragment = Clipboard._extract_html_fragment(html_data)
            if html_fragment:
                # 정상이면테이블방식방식, 매칭 src=" 및 " 의내용
                pattern = r'src="file:///(.*?\.(?:jpg|png|gif))"'
                # 사용 re.findall 조회모든매칭
                matches = re.findall(pattern, html_fragment)
                for match in matches:
                    base64_str = Clipboard._base64_encode(Base64CodeType.PICTURE, "", match)
                    html_fragment = html_fragment.replace(r"file:///" + match, base64_str)
                return html_fragment
            else:
                return html_data
        else:
            # HTML형식반환텍스트
            return Clipboard.paste_str_clip()

    @staticmethod
    def _extract_html_fragment(html_clipboard_data):
        if isinstance(html_clipboard_data, bytes):
            html_clipboard_str = html_clipboard_data.decode("utf-8", errors="replace")
        else:
            html_clipboard_str = str(html_clipboard_data)

        start_marker = "<!--StartFragment-->"
        end_marker = "<!--EndFragment-->"

        start_index = html_clipboard_str.find(start_marker)
        end_index = html_clipboard_str.find(end_marker)

        if start_index == -1 or end_index == -1:
            # 사용정상이면테이블방식행매칭
            # 일소의예방식
            match_start = re.search(r"StartHTML:(\d+)", html_clipboard_str)
            match_end = re.search(r"EndHTML:(\d+)", html_clipboard_str)
            if not match_start or not match_end:
                return html_clipboard_str
            match_start = match_start.group(1)
            match_end = match_end.group(1)
            return html_clipboard_str[int(match_start) : int(match_end)]

        start_offset = start_index + len(start_marker)
        end_offset = end_index
        # Extract the HTML fragment using the offsets
        html_fragment = html_clipboard_str[start_offset:end_offset]
        return html_fragment

    @staticmethod
    def _base64_encode(
        encode_type: Base64CodeType = Base64CodeType.STRING,
        string_data: str = "",
        file_path: str = "",
    ) -> str:
        import base64

        if file_path:
            with open(file_path, "rb") as file:
                input_content = file.read()
        else:
            input_content = string_data.encode("utf-8")
        base64_encoded = base64.b64encode(input_content)
        base64_encode_result = base64_encoded.decode("utf-8")
        if encode_type == Base64CodeType.PICTURE:
            base64_encode_result = "data:image/png;base64," + base64_encode_result
        return base64_encode_result
