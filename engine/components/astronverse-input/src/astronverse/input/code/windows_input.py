"""
Windows Unicode 입력방법
해제완료관리, 입력인증, 오류 관리대기제목
"""

import ctypes
import time
from ctypes import wintypes
from typing import Union

# Windows API 일반량지정
INPUT_MOUSE = 0
INPUT_KEYBOARD = 1
INPUT_HARDWARE = 2

KEYEVENTF_EXTENDEDKEY = 0x0001
KEYEVENTF_KEYUP = 0x0002
KEYEVENTF_UNICODE = 0x0004
KEYEVENTF_SCANCODE = 0x0008

# 코드(사용)
VK_BACK = 0x08
VK_TAB = 0x09
VK_RETURN = 0x0D
VK_SHIFT = 0x10
VK_CONTROL = 0x11
VK_MENU = 0x12  # Alt
VK_ESCAPE = 0x1B
VK_SPACE = 0x20
VK_LEFT = 0x25
VK_UP = 0x26
VK_RIGHT = 0x27
VK_DOWN = 0x28
VK_DELETE = 0x2E


# 지정 Windows 결과
class MOUSEINPUT(ctypes.Structure):
    _fields_ = (
        ("dx", wintypes.LONG),
        ("dy", wintypes.LONG),
        ("mouseData", wintypes.DWORD),
        ("dwFlags", wintypes.DWORD),
        ("time", wintypes.DWORD),
        ("dwExtraInfo", ctypes.POINTER(wintypes.ULONG)),
    )


class KEYBDINPUT(ctypes.Structure):
    _fields_ = (
        ("wVk", wintypes.WORD),
        ("wScan", wintypes.WORD),
        ("dwFlags", wintypes.DWORD),
        ("time", wintypes.DWORD),
        ("dwExtraInfo", ctypes.POINTER(wintypes.ULONG)),
    )


class HARDWAREINPUT(ctypes.Structure):
    _fields_ = (("uMsg", wintypes.DWORD), ("wParamL", wintypes.WORD), ("wParamH", wintypes.WORD))


class _INPUTunion(ctypes.Union):
    _fields_ = (("mi", MOUSEINPUT), ("ki", KEYBDINPUT), ("hi", HARDWAREINPUT))


class INPUT(ctypes.Structure):
    _fields_ = (("type", wintypes.DWORD), ("union", _INPUTunion))


# 로드 user32.dll
user32 = ctypes.WinDLL("user32", use_last_error=True)


class InputError(Exception):
    """입력오류예외"""

    pass


class UnicodeInput:
    """Windows Unicode 입력유형 - 수정버전"""

    def __init__(self, delay=0.01, max_retries=1):
        """
        입력기기

        Args:
            delay: 매개문자입력후의지연시간(초), 0.01초
            max_retries: 전송실패시의대재시도 데이터
        """
        self.delay = delay
        self.max_retries = max_retries

    def _send_unicode_char(self, char):
        """
        전송단일개 Unicode 문자(지요소관리)

        Args:
            char: 필요전송의문자
        """
        # 를문자변환로UTF-16LE코드
        try:
            utf16_bytes = char.encode("utf-16-le")
        except Exception as e:
            raise InputError(f"문자코드실패: {char}, 오류: {e}")

        # UTF-16LE 매2개문자테이블일개코드단일원
        # 본다중문서평면(BMP)의문자, 있음일개코드단일원
        # 평면의문자(예emoji), 있음개코드단일원(관리)
        all_inputs = []

        for i in range(0, len(utf16_bytes), 2):
            code_unit = int.from_bytes(utf16_bytes[i : i + 2], "little")

            # 아래파일
            input_down = INPUT()
            input_down.type = INPUT_KEYBOARD
            input_down.union.ki.wVk = 0
            input_down.union.ki.wScan = code_unit
            input_down.union.ki.dwFlags = KEYEVENTF_UNICODE
            input_down.union.ki.time = 0
            input_down.union.ki.dwExtraInfo = None
            all_inputs.append(input_down)

            # 파일
            input_up = INPUT()
            input_up.type = INPUT_KEYBOARD
            input_up.union.ki.wVk = 0
            input_up.union.ki.wScan = code_unit
            input_up.union.ki.dwFlags = KEYEVENTF_UNICODE | KEYEVENTF_KEYUP
            input_up.union.ki.time = 0
            input_up.union.ki.dwExtraInfo = None
            all_inputs.append(input_up)

        # 전송입력(재시도기기제어)
        for attempt in range(self.max_retries):
            try:
                array_type = INPUT * len(all_inputs)
                input_array = array_type(*all_inputs)
                result = user32.SendInput(len(all_inputs), input_array, ctypes.sizeof(INPUT))

                if result == len(all_inputs):
                    return  # 성공

                # 모듈분성공또는실패
                error_code = ctypes.get_last_error()
                if attempt < self.max_retries - 1:
                    time.sleep(0.01)  # 짧음지연후재시도
                    continue
                else:
                    raise InputError(
                        f"SendInput 실패: 전송 {len(all_inputs)} 개입력, "
                        f"전송 {result} 개, 오류코드: {error_code}"
                    )
            except Exception as e:
                if attempt < self.max_retries - 1:
                    time.sleep(0.01)
                    continue
                else:
                    raise InputError(f"전송문자 '{char}' 실패: {e}")

    def _send_special_key(self, vk_code):
        """
        전송(예돌아가기차, 격식대기)

        Args:
            vk_code: 코드
        """
        inputs = []

        # 아래파일
        input_down = INPUT()
        input_down.type = INPUT_KEYBOARD
        input_down.union.ki.wVk = vk_code
        input_down.union.ki.wScan = 0
        input_down.union.ki.dwFlags = 0
        input_down.union.ki.time = 0
        input_down.union.ki.dwExtraInfo = None
        inputs.append(input_down)

        # 파일
        input_up = INPUT()
        input_up.type = INPUT_KEYBOARD
        input_up.union.ki.wVk = vk_code
        input_up.union.ki.wScan = 0
        input_up.union.ki.dwFlags = KEYEVENTF_KEYUP
        input_up.union.ki.time = 0
        input_up.union.ki.dwExtraInfo = None
        inputs.append(input_up)

        # 전송입력(재시도기기제어)
        for attempt in range(self.max_retries):
            try:
                array_type = INPUT * len(inputs)
                input_array = array_type(*inputs)
                result = user32.SendInput(len(inputs), input_array, ctypes.sizeof(INPUT))

                if result == len(inputs):
                    return  # 성공

                error_code = ctypes.get_last_error()
                if attempt < self.max_retries - 1:
                    time.sleep(0.01)
                    continue
                else:
                    raise InputError(
                        f"SendInput 실패: 전송 {len(inputs)} 개입력, 전송 {result} 개, 오류코드: {error_code}"
                    )
            except Exception as e:
                if attempt < self.max_retries - 1:
                    time.sleep(0.01)
                    continue
                else:
                    raise InputError(f"전송실패 (VK={vk_code}): {e}")

    def type_text(self, text: Union[str, int, float]):
        """
        입력텍스트, 지원중영어, emoji대기모든Unicode문자

        Args:
            text: 입력할의텍스트문자열(변환로문자열)

        Raises:
            InputError: 입력실패시출력
            TypeError: 텍스트유형불가변환시출력
        """
        # 입력인증및유형변환
        if text is None:
            raise TypeError("텍스트할 수 없음로 None")

        if not isinstance(text, str):
            try:
                text = str(text)
            except Exception as e:
                raise TypeError(f"불가를 {type(text)} 변환로문자열: {e}")

        if len(text) == 0:
            return  # 빈문자열, 직선연결반환

        # 문자입력
        for i, char in enumerate(text):
            # 조회여부예문자
            if char == "\n":
                self._send_special_key(VK_RETURN)
            elif char == "\t":
                self._send_special_key(VK_TAB)
            elif char == "\b":
                self._send_special_key(VK_BACK)
            else:
                # 통신문자, 패키지중국어, 영어, 숫자, 기호, emoji대기
                self._send_unicode_char(char)

            # 지연
            if self.delay > 0:
                time.sleep(self.delay)

    def press_key(self, key_name: str):
        """
        아래

        Args:
            key_name: 이름, 지원 'enter', 'backspace', 'tab', 'esc', 'space',
                     'left', 'right', 'up', 'down', 'delete', 'shift', 'ctrl', 'alt'

        Raises:
            ValueError: 지원하지 않는 이름
        """
        key_map = {
            "enter": VK_RETURN,
            "backspace": VK_BACK,
            "tab": VK_TAB,
            "esc": VK_ESCAPE,
            "escape": VK_ESCAPE,
            "space": VK_SPACE,
            "left": VK_LEFT,
            "right": VK_RIGHT,
            "up": VK_UP,
            "down": VK_DOWN,
            "delete": VK_DELETE,
            "shift": VK_SHIFT,
            "ctrl": VK_CONTROL,
            "control": VK_CONTROL,
            "alt": VK_MENU,
        }

        if not isinstance(key_name, str):
            raise TypeError(f"이름예문자열, 현재유형: {type(key_name)}")

        key_name_lower = key_name.lower()
        if key_name_lower in key_map:
            self._send_special_key(key_map[key_name_lower])
            if self.delay > 0:
                time.sleep(self.delay)
        else:
            supported_keys = ", ".join(sorted(set(key_map.keys())))
            raise ValueError(f"지원하지 않는 이름입니다: '{key_name}'\n지원되는 이름: {supported_keys}")

    def press_keys(self, *key_names):
        """
        아래다중개

        Args:
            *key_names: 다중개이름

        Example:
            press_keys('ctrl', 'a')  # Ctrl+A
        """
        for key_name in key_names:
            self.press_key(key_name)


def type_text(text: Union[str, int, float], delay=0.01):
    """
    데이터: 입력텍스트

    Args:
        text: 입력할의텍스트
        delay: 매개문자의지연(초)

    Example:
        type_text("abAB123")
        type_text("Hello!😀", delay=0.02)
    """
    inputter = UnicodeInput(delay=delay)
    inputter.type_text(text)


# 시도코드
if __name__ == "__main__":
    print("=" * 60)
    print("Windows Unicode 입력시도 - 수정버전")
    print("=" * 60)
    print("\n5초후열기 시도...")
    print("요청를에서작업가능입력텍스트의방법(예본, 브라우저입력란)\n")
    time.sleep(5)

    # 생성입력기기
    inputter = UnicodeInput(delay=0.05)

    try:
        print("✓ 시도0: 공백")
        inputter.type_text("")
        time.sleep(1)
        # 시도1: 본합치기입력
        print("✓ 시도1: 합치기중영어숫자")
        inputter.type_text("abAB123")
        time.sleep(1)

        # 시도2: 변경복사의텍스트
        print("✓ 시도2: 복사텍스트")
        inputter.type_text("Hello!Test시도123")
        time.sleep(1)

        # 시도3: Emoji및기호(관리시도)
        print("✓ 시도3: Emoji및기호")
        inputter.type_text("😀🎉💻🚀 테이블기호시도")
        time.sleep(1)

        # 시도4: 기호
        print("✓ 시도4: 기호")
        inputter.type_text("중국어: , .!?;: ''()[]")
        time.sleep(1)

        # 시도5: 사용데이터
        print("✓ 시도5: 데이터")
        type_text("예데이터시도abcABC123", delay=0.03)

        # 시도6: 숫자변환
        print("✓ 시도6: 숫자및유형변환")
        inputter.type_text(123456)
        inputter.type_text(" | ")
        inputter.type_text(3.14159)

        print("\n" + "=" * 60)
        print("✓ 모든시도완료!")
        print("=" * 60)

    except InputError as e:
        print(f"\n❌ 입력오류: {e}")
    except Exception as e:
        print(f"\n❌ 지원하지 않는오류: {e}")
        import traceback

        traceback.print_exc()
