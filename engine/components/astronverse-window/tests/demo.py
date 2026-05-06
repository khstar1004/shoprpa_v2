import time

from astronverse.window.core import WindowExistType, WindowSizeType
from astronverse.window.window import Window


def demo():
    """기존가능"""
    print("🚀 열기  RPA Windows 요소가능...")
    print("사용자요청 열기일개  창")
    print("=" * 60)

    # 생성시도사용의WinPick객체
    test_pick = {"elementData": {"path": [{"name": " - 파일관리관리기기", "cls": "CabinetWClass"}]}}

    try:
        # exist방법법
        Window.exist(pick=test_pick, check_type=WindowExistType.EXIST, wait_time=0)
        time.sleep(1)
        # top방법법
        Window.top(pick=test_pick)
        time.sleep(1)
        # set_size방법법
        Window.set_size(pick=test_pick, size_type=WindowSizeType.MAX, width=0, height=0)
        time.sleep(1)
        Window.set_size(pick=test_pick, size_type=WindowSizeType.CUSTOM, width=200, height=200)
        time.sleep(1)
        Window.set_size(pick=test_pick, size_type=WindowSizeType.MIN, width=0, height=0)
        time.sleep(1)
        Window.set_size(pick=test_pick, size_type=WindowSizeType.CUSTOM, width=400, height=400)
        time.sleep(1)
        Window.set_size(pick=test_pick, size_type=WindowSizeType.MAX, width=0, height=0)
        time.sleep(1)
        Window.set_size(pick=test_pick, size_type=WindowSizeType.MIN, width=0, height=0)
        time.sleep(1)
        # close방법법
        Window.close(pick=test_pick)
        print("완료")
    except Exception as e:
        print(f"출력예외: {e}")


if __name__ == "__main__":
    demo()