import time

from astronverse.actionlib.types import WinPick
from astronverse.actionlib.utils import FileExistenceType
from astronverse.winelement import MouseClickButton, MouseClickType
from astronverse.winelement.core_win import WinEleCore
from astronverse.winelement.winele import WinEle


def demo():
    """기존가능"""
    print("🚀 열기  RPA Winele 요소가능...")
    print("=" * 60)

    # 생성시도사용의WinPick객체
    test_pick_data = {
        "elementData": {
            "version": "1",
            "type": "uia",
            "app": "explorer",
            "path": [
                {
                    "cls": "Progman",
                    "name": "Program Manager",
                    "tag_name": "PaneControl",
                    "index": 13,
                    "value": None,
                },
                {
                    "cls": "SHELLDLL_DefView",
                    "name": "",
                    "tag_name": "PaneControl",
                    "index": 0,
                    "value": None,
                },
                {
                    "cls": "SysListView32",
                    "name": "",
                    "tag_name": "ListControl",
                    "index": 0,
                    "value": None,
                },
                {
                    "cls": "",
                    "name": "",
                    "tag_name": "ListItemControl",
                    "index": 0,
                    "value": None,
                },
            ],
            "img": {"self": ""},
            "picker_type": "ELEMENT",
        }
    }

    test_pick = WinPick(test_pick_data)

    print("📋 내용:")
    print("1. 원조회 (find)")
    print("2. 요소클릭 (click_element)")
    print("3. 요소중지 (hover_element)")
    print("4. 요소스크린샷 (screenshot_element)")
    print("5. 가능시도")
    print("=" * 60)

    try:
        # 1. 원조회
        print("\n🔍 1. 원조회")
        print("-" * 30)

        start_time = time.time()
        locator = WinEleCore.find(pick=test_pick, wait_time=5.0)
        find_time = time.time() - start_time

        if locator:
            point = locator.point()
            print("✅ 원조회완료!")
            print(f"   실행시간: {find_time:.3f}초")
            print(f"   요소위치: ({point.x}, {point.y})")

            # 가져오기요소정보
            rect = locator.rect()
            print(f"   요소크기: {rect.width()} x {rect.height()}")
        else:
            print("❌ 원조회실패")
            return

        # 2. 요소클릭
        print("\n🖱️ 2. 요소클릭")
        print("-" * 30)

        # 왼쪽 버튼단일
        print("   실행왼쪽 버튼단일...")
        start_time = time.time()
        WinEle.click_element(
            pick=test_pick,
            click_button=MouseClickButton.LEFT,
            click_type=MouseClickType.CLICK,
            wait_time=5.0,
        )
        click_time = time.time() - start_time
        print(f"✅ 왼쪽 버튼단일완료 - 시: {click_time:.3f}초")

        # 3. 요소중지
        print("\n🖱️ 3. 요소중지")
        print("-" * 30)

        start_time = time.time()
        WinEle.hover_element(pick=test_pick, wait_time=5.0)
        hover_time = time.time() - start_time
        print(f"✅ 요소중지완료 - 시: {hover_time:.3f}초")

        # 4. 요소스크린샷
        print("\n📸 4. 요소스크린샷")
        print("-" * 30)

        start_time = time.time()
        WinEle.screenshot_element(
            pick=test_pick,
            file_path="./",
            file_name="demo_screenshot",
            exist_type=FileExistenceType.OVERWRITE,
        )
        screenshot_time = time.time() - start_time
        print(f"✅ 요소스크린샷완료 - 시: {screenshot_time:.3f}초")
        print("   스크린샷저장경로: ./demo_screenshot.png")

        # 5. 가능시도
        print("\n⚡ 5. 가능시도")
        print("-" * 30)

        test_iterations = 3
        execution_times = []

        print(f"   실행 {test_iterations} 원조회가능시도...")

        for i in range(test_iterations):
            start_time = time.time()
            try:
                locator = WinEleCore.find(pick=test_pick, wait_time=3.0)
                point = locator.point()
                execution_time = time.time() - start_time
                execution_times.append(execution_time)
                print(f"    {i + 1} : {execution_time:.3f}초")
            except Exception as e:
                execution_time = time.time() - start_time
                print(f"    {i + 1} : {execution_time:.3f}초 - 실패: {str(e)}")

        if execution_times:
            avg_time = sum(execution_times) / len(execution_times)
            min_time = min(execution_times)
            max_time = max(execution_times)

            print("\n📊 가능시도결과:")
            print(f"   평면실행시간: {avg_time:.3f}초")
            print(f"   빠름실행시간: {min_time:.3f}초")
            print(f"   느림실행시간: {max_time:.3f}초")
            print(f"   실행데이터: {len(execution_times)}")

            if avg_time < 3.0:
                print("✅ 가능테이블")
            else:
                print("⚠️ 가능필요")

        print("\n🎉 모든완료!")
        print("=" * 60)

    except Exception as e:
        print(f"❌ 경과중출력예외: {e}")
        print("   가능예목록 요소를 찾을 수 없습니다또는시스템제목")
        print("   요청확인위있음''요소, 또는수정시도요소")


if __name__ == "__main__":
    demo()