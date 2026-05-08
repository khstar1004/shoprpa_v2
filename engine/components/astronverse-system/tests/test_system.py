import os
from pathlib import Path
import shutil
import unittest
from uuid import uuid4
from unittest import TestCase

import pyautogui

from astronverse.system import *
from astronverse.system.system import System


_SCREEN_CAPTURE_AVAILABLE: bool | None = None


def _make_temp_dir() -> str:
    temp_root = Path(__file__).resolve().parents[4] / "build" / "tmp" / "system-tests"
    temp_root.mkdir(parents=True, exist_ok=True)
    temp_dir = temp_root / f"system-{uuid4().hex}"
    temp_dir.mkdir()
    return str(temp_dir)


def _screen_capture_available() -> bool:
    global _SCREEN_CAPTURE_AVAILABLE
    if _SCREEN_CAPTURE_AVAILABLE is None:
        try:
            image = pyautogui.screenshot(region=(0, 0, 1, 1))
            image.close()
            _SCREEN_CAPTURE_AVAILABLE = True
        except Exception:
            _SCREEN_CAPTURE_AVAILABLE = False
    return _SCREEN_CAPTURE_AVAILABLE


class TestSystem(TestCase):
    def setUp(self):
        """시도전의준비"""
        self.temp_dir = _make_temp_dir()
        self.test_png_name = "test_screenshot.png"

    def tearDown(self):
        """시도후의관리"""
        if os.path.exists(self.temp_dir):
            shutil.rmtree(self.temp_dir)

    def require_screen_capture(self):
        if not _screen_capture_available():
            self.skipTest("Screen capture is unavailable in this session")

    def test_screen_shot_full_screen_success(self):
        """시도화면스크린샷 - 전체스크린샷성공"""
        self.require_screen_capture()
        # 실행전체스크린샷
        result = System.screen_shot(
            png_path=self.temp_dir,
            state_type=StateType.ERROR,
            png_name=self.test_png_name,
            screen_type=ScreenType.FULL,
        )

        # 인증스크린샷파일존재함
        self.assertTrue(os.path.exists(result))
        self.assertEqual(os.path.basename(result), self.test_png_name)
        self.assertTrue(result.endswith(".png"))

    def test_screen_shot_region_success(self):
        """시도화면스크린샷 - 스크린샷성공"""
        self.require_screen_capture()
        # 실행스크린샷(사용소의초과출력화면)
        result = System.screen_shot(
            png_path=self.temp_dir,
            state_type=StateType.ERROR,
            png_name=self.test_png_name,
            screen_type=ScreenType.REGION,
            top_left_x=100,
            top_left_y=100,
            bottom_right_x=300,
            bottom_right_y=200,
        )

        # 인증스크린샷파일존재함
        self.assertTrue(os.path.exists(result))
        self.assertEqual(os.path.basename(result), self.test_png_name)
        self.assertTrue(result.endswith(".png"))

    def test_screen_shot_region_invalid_coordinates(self):
        """시도화면스크린샷 - 없음"""
        # 사용지원하지 않는 (데이터)
        with self.assertRaises(ValueError):
            System.screen_shot(
                png_path=self.temp_dir,
                state_type=StateType.ERROR,
                png_name=self.test_png_name,
                screen_type=ScreenType.REGION,
                top_left_x=-1,
                top_left_y=100,
                bottom_right_x=500,
                bottom_right_y=400,
            )

    def test_screen_shot_folder_not_exists_error(self):
        """시도화면스크린샷 - 폴더아니오저장된 로오류"""
        non_existent_dir = os.path.join(self.temp_dir, "non_existent")

        with self.assertRaises(BaseException):
            System.screen_shot(
                png_path=non_existent_dir,
                state_type=StateType.ERROR,
                png_name=self.test_png_name,
            )

    def test_screen_shot_folder_not_exists_create(self):
        """시도화면스크린샷 - 폴더아니오저장된 생성"""
        self.require_screen_capture()
        new_dir = os.path.join(self.temp_dir, "new_dir")

        # 실행스크린샷까지찾을 수 없습니다의디렉터리, 로생성
        result = System.screen_shot(png_path=new_dir, state_type=StateType.CREATE, png_name=self.test_png_name)

        # 인증스크린샷파일저장된 디렉터리생성
        self.assertTrue(os.path.exists(result))
        self.assertTrue(os.path.exists(new_dir))
        self.assertEqual(os.path.basename(result), self.test_png_name)

    def test_screen_shot_auto_extension(self):
        """시도화면스크린샷 - 추가이름"""
        self.require_screen_capture()
        name_without_ext = "test_screenshot"
        expected_name = name_without_ext + ".png"

        # 실행스크린샷, 파일이름아니오패키지이름
        result = System.screen_shot(
            png_path=self.temp_dir,
            state_type=StateType.ERROR,
            png_name=name_without_ext,
        )

        # 인증추가완료.png이름
        self.assertTrue(os.path.exists(result))
        self.assertEqual(os.path.basename(result), expected_name)

    def test_screen_shot_with_jpg_extension(self):
        """시도화면스크린샷 - 사용jpg이름"""
        self.require_screen_capture()
        jpg_name = "test_screenshot.jpg"

        # 실행스크린샷, 사용jpg이름
        result = System.screen_shot(png_path=self.temp_dir, state_type=StateType.ERROR, png_name=jpg_name)

        # 인증파일저장된 이름정상
        self.assertTrue(os.path.exists(result))
        self.assertEqual(os.path.basename(result), jpg_name)
        self.assertTrue(result.endswith(".jpg"))

    def test_screen_shot_multiple_screenshots(self):
        """시도화면스크린샷 - 다중스크린샷"""
        self.require_screen_capture()
        # 실행다중스크린샷
        screenshots = []
        for i in range(3):
            screenshot_name = f"screenshot_{i}.png"
            result = System.screen_shot(
                png_path=self.temp_dir,
                state_type=StateType.ERROR,
                png_name=screenshot_name,
            )
            screenshots.append(result)

        # 인증모든스크린샷파일존재함
        for screenshot in screenshots:
            self.assertTrue(os.path.exists(screenshot))
            self.assertTrue(screenshot.endswith(".png"))

    def test_screen_shot_file_size_verification(self):
        """시도화면스크린샷 - 인증파일크기"""
        self.require_screen_capture()
        # 실행스크린샷
        result = System.screen_shot(
            png_path=self.temp_dir,
            state_type=StateType.ERROR,
            png_name=self.test_png_name,
        )

        # 인증스크린샷파일저장된 크기대0
        self.assertTrue(os.path.exists(result))
        file_size = os.path.getsize(result)
        self.assertGreater(file_size, 0)

    def test_screen_shot_different_regions(self):
        """시도화면스크린샷 - 아니오"""
        self.require_screen_capture()
        regions = [(50, 50, 150, 100), (200, 100, 400, 200), (100, 200, 300, 300)]

        for i, (x1, y1, x2, y2) in enumerate(regions):
            screenshot_name = f"region_screenshot_{i}.png"
            try:
                result = System.screen_shot(
                    png_path=self.temp_dir,
                    state_type=StateType.ERROR,
                    png_name=screenshot_name,
                    screen_type=ScreenType.REGION,
                    top_left_x=x1,
                    top_left_y=y1,
                    bottom_right_x=x2,
                    bottom_right_y=y2,
                )

                # 인증스크린샷파일존재함
                self.assertTrue(os.path.exists(result))
                self.assertEqual(os.path.basename(result), screenshot_name)
            except ValueError:
                # 결과가초과출력화면, 예의
                pass


if __name__ == "__main__":
    unittest.main()
