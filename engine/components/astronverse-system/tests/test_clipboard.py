import os
import shutil
import tempfile
import unittest
from unittest import TestCase

from astronverse.system import *
from astronverse.system.clipboard import Clipboard


class TestClipboard(TestCase):
    def setUp(self):
        """시도전의준비"""
        self.temp_dir = tempfile.mkdtemp()
        self.test_file_path = os.path.join(self.temp_dir, "test_file.txt")
        self.test_folder_path = os.path.join(self.temp_dir, "test_folder")

        # 생성시도파일및폴더
        with open(self.test_file_path, "w", encoding="utf-8") as f:
            f.write("시도파일내용")
        os.makedirs(self.test_folder_path, exist_ok=True)

        # 빈, 확인시도
        Clipboard.clear_clip()

    def tearDown(self):
        """시도후의관리"""
        if os.path.exists(self.temp_dir):
            shutil.rmtree(self.temp_dir)

    def test_copy_clip_message_success(self):
        """시도복사까지 - 텍스트메시지성공"""
        test_message = "예일개시도메시지"

        # 복사메시지까지
        Clipboard.copy_clip(content_type=ContentType.MSG, message=test_message)

        # 인증내용
        result = Clipboard.paste_clip(content_type=ContentType.MSG)
        self.assertEqual(result, test_message)

    def test_copy_clip_message_empty(self):
        """시도복사까지 - 빈메시지"""
        with self.assertRaises(BaseException):
            Clipboard.copy_clip(content_type=ContentType.MSG, message="")

    def test_copy_clip_file_success(self):
        """시도복사까지 - 파일성공"""
        # 복사파일까지
        Clipboard.copy_clip(content_type=ContentType.FILE, file_path=self.test_file_path)

        # 인증중의파일 경로
        result = Clipboard.paste_clip(
            content_type=ContentType.FILE,
            dst_path=self.temp_dir,
            state_type=StateType.ERROR,
        )
        self.assertTrue(os.path.exists(result))
        self.assertEqual(os.path.basename(result), "test_file.txt")

    def test_copy_clip_file_not_exists(self):
        """시도복사까지 - 파일찾을 수 없습니다"""
        non_existent_file = os.path.join(self.temp_dir, "non_existent.txt")
        with self.assertRaises(BaseException):
            Clipboard.copy_clip(content_type=ContentType.FILE, file_path=non_existent_file)

    def test_copy_clip_folder_success(self):
        """시도복사까지 - 폴더성공"""
        # 복사폴더까지
        Clipboard.copy_clip(content_type=ContentType.FOLDER, folder_path=self.test_folder_path)

        # 인증중의폴더 경로
        result = Clipboard.paste_clip(
            content_type=ContentType.FOLDER,
            dst_path=self.temp_dir,
            state_type=StateType.ERROR,
        )
        self.assertTrue(os.path.exists(result))
        self.assertEqual(os.path.basename(result), "test_folder")

    def test_copy_clip_folder_not_exists(self):
        """시도복사까지 - 폴더찾을 수 없습니다"""
        non_existent_folder = os.path.join(self.temp_dir, "non_existent_folder")
        with self.assertRaises(BaseException):
            Clipboard.copy_clip(content_type=ContentType.FOLDER, folder_path=non_existent_folder)

    def test_copy_clip_invalid_content_type(self):
        """시도복사까지 - 없음내용유형"""
        with self.assertRaises(NotImplementedError):
            Clipboard.copy_clip(content_type="invalid_type")

    def test_clear_clip_success(self):
        """시도빈 - 성공"""
        # 복사일내용까지
        test_message = "시도메시지"
        Clipboard.copy_clip(content_type=ContentType.MSG, message=test_message)

        # 인증있음내용
        result = Clipboard.paste_clip(content_type=ContentType.MSG)
        self.assertEqual(result, test_message)

        # 빈
        Clipboard.clear_clip()

        # 인증완료빈
        result = Clipboard.paste_clip(content_type=ContentType.MSG)
        self.assertEqual(result, "")

    def test_paste_clip_message_success(self):
        """시도에서붙여넣기 - 텍스트메시지성공"""
        expected_content = "붙여넣기의텍스트내용"

        # 복사내용까지
        Clipboard.copy_clip(content_type=ContentType.MSG, message=expected_content)

        # 에서붙여넣기
        result = Clipboard.paste_clip(content_type=ContentType.MSG)
        self.assertEqual(result, expected_content)

    def test_paste_clip_file_success(self):
        """시도에서붙여넣기 - 파일성공"""
        # 복사파일까지
        Clipboard.copy_clip(content_type=ContentType.FILE, file_path=self.test_file_path)

        # 에서붙여넣기파일
        result = Clipboard.paste_clip(
            content_type=ContentType.FILE,
            dst_path=self.temp_dir,
            state_type=StateType.ERROR,
        )
        self.assertTrue(os.path.exists(result))
        self.assertEqual(os.path.basename(result), "test_file.txt")

    def test_paste_clip_file_with_custom_name(self):
        """시도에서붙여넣기 - 파일지정이름"""
        # 복사파일까지
        Clipboard.copy_clip(content_type=ContentType.FILE, file_path=self.test_file_path)

        # 에서붙여넣기파일, 사용지정이름
        result = Clipboard.paste_clip(
            content_type=ContentType.FILE,
            dst_path=self.temp_dir,
            state_type=StateType.ERROR,
            dst_file_name="custom_name",
        )
        self.assertTrue(os.path.exists(result))
        self.assertEqual(os.path.basename(result), "custom_name.txt")

    def test_paste_clip_folder_success(self):
        """시도에서붙여넣기 - 폴더성공"""
        # 복사폴더까지
        Clipboard.copy_clip(content_type=ContentType.FOLDER, folder_path=self.test_folder_path)

        # 에서붙여넣기폴더
        result = Clipboard.paste_clip(
            content_type=ContentType.FOLDER,
            dst_path=self.temp_dir,
            state_type=StateType.ERROR,
        )
        self.assertTrue(os.path.exists(result))
        self.assertEqual(os.path.basename(result), "test_folder")

    def test_paste_clip_folder_with_custom_name(self):
        """시도에서붙여넣기 - 폴더지정이름"""
        # 복사폴더까지
        Clipboard.copy_clip(content_type=ContentType.FOLDER, folder_path=self.test_folder_path)

        # 에서붙여넣기폴더, 사용지정이름
        result = Clipboard.paste_clip(
            content_type=ContentType.FOLDER,
            dst_path=self.temp_dir,
            state_type=StateType.ERROR,
            dst_folder_name="custom_folder_name",
        )
        self.assertTrue(os.path.exists(result))
        self.assertEqual(os.path.basename(result), "custom_folder_name")

    def test_paste_clip_folder_not_exists_error(self):
        """시도에서붙여넣기 - 목록 폴더아니오저장된 로오류"""
        # 복사파일까지
        Clipboard.copy_clip(content_type=ContentType.FILE, file_path=self.test_file_path)

        non_existent_dir = os.path.join(self.temp_dir, "non_existent")
        with self.assertRaises(BaseException):
            Clipboard.paste_clip(
                content_type=ContentType.FILE,
                dst_path=non_existent_dir,
                state_type=StateType.ERROR,
            )

    def test_paste_clip_folder_not_exists_create(self):
        """시도에서붙여넣기 - 목록 폴더아니오저장된 생성"""
        # 복사파일까지
        Clipboard.copy_clip(content_type=ContentType.FILE, file_path=self.test_file_path)

        new_dir = os.path.join(self.temp_dir, "new_dir")
        result = Clipboard.paste_clip(content_type=ContentType.FILE, dst_path=new_dir, state_type=StateType.CREATE)
        self.assertTrue(os.path.exists(result))
        self.assertTrue(os.path.exists(new_dir))

    def test_paste_clip_invalid_content_type(self):
        """시도에서붙여넣기 - 없음내용유형"""
        with self.assertRaises(NotImplementedError):
            Clipboard.paste_clip(content_type="invalid_type")

    def test_copy_clip_message_with_special_characters(self):
        """시도복사까지 - 패키지문자의메시지"""
        special_message = "시도메시지: 패키지중국어, 영어, 숫자123및기호!@#￥%"

        # 복사문자메시지까지
        Clipboard.copy_clip(content_type=ContentType.MSG, message=special_message)

        # 인증내용
        result = Clipboard.paste_clip(content_type=ContentType.MSG)
        self.assertEqual(result, special_message)

    def test_copy_clip_multiple_files(self):
        """시도복사까지 - 다중개파일"""
        # 생성다중개시도파일
        files = ["file1.txt", "file2.txt", "file3.txt"]
        for file_name in files:
            file_path = os.path.join(self.temp_dir, file_name)
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(f"content for {file_name}")

        # 시도복사일개파일
        first_file = os.path.join(self.temp_dir, "file1.txt")
        Clipboard.copy_clip(content_type=ContentType.FILE, file_path=first_file)

        # 인증중의파일
        result = Clipboard.paste_clip(
            content_type=ContentType.FILE,
            dst_path=self.temp_dir,
            state_type=StateType.ERROR,
        )
        self.assertTrue(os.path.exists(result))
        self.assertEqual(os.path.basename(result), "file1.txt")

    def test_paste_clip_empty_content(self):
        """시도에서붙여넣기 - 빈내용"""
        # 빈
        Clipboard.clear_clip()

        # 인증비어 있습니다
        result = Clipboard.paste_clip(content_type=ContentType.MSG)
        self.assertEqual(result, "")

    def test_copy_clip_file_with_spaces_in_path(self):
        """시도복사까지 - 경로패키지공백의파일"""
        file_with_spaces = os.path.join(self.temp_dir, "file with spaces.txt")
        with open(file_with_spaces, "w", encoding="utf-8") as f:
            f.write("content")

        # 복사패키지공백의파일까지
        Clipboard.copy_clip(content_type=ContentType.FILE, file_path=file_with_spaces)

        # 인증중의파일
        result = Clipboard.paste_clip(
            content_type=ContentType.FILE,
            dst_path=self.temp_dir,
            state_type=StateType.ERROR,
        )
        self.assertTrue(os.path.exists(result))
        self.assertEqual(os.path.basename(result), "file with spaces.txt")

    def test_copy_clip_folder_with_spaces_in_path(self):
        """시도복사까지 - 경로패키지공백의폴더"""
        folder_with_spaces = os.path.join(self.temp_dir, "folder with spaces")
        os.makedirs(folder_with_spaces, exist_ok=True)

        # 복사패키지공백의폴더까지
        Clipboard.copy_clip(content_type=ContentType.FOLDER, folder_path=folder_with_spaces)

        # 인증중의폴더
        result = Clipboard.paste_clip(
            content_type=ContentType.FOLDER,
            dst_path=self.temp_dir,
            state_type=StateType.ERROR,
        )
        self.assertTrue(os.path.exists(result))
        self.assertEqual(os.path.basename(result), "folder with spaces")

    def test_copy_and_paste_cycle(self):
        """시도복사붙여넣기 - 확인데이터"""
        original_message = "기존시도메시지"

        # 복사메시지
        Clipboard.copy_clip(content_type=ContentType.MSG, message=original_message)

        # 붙여넣기인증
        pasted_message = Clipboard.paste_clip(content_type=ContentType.MSG)
        self.assertEqual(pasted_message, original_message)

        # 복사붙여넣기, 확인공가능보통
        Clipboard.copy_clip(content_type=ContentType.MSG, message=pasted_message)
        final_message = Clipboard.paste_clip(content_type=ContentType.MSG)
        self.assertEqual(final_message, original_message)

    def test_file_content_integrity(self):
        """시도파일내용 """
        original_content = "예기존파일내용"

        # 생성시도파일
        test_file = os.path.join(self.temp_dir, "content_test.txt")
        with open(test_file, "w", encoding="utf-8") as f:
            f.write(original_content)

        # 복사파일까지
        Clipboard.copy_clip(content_type=ContentType.FILE, file_path=test_file)

        # 붙여넣기파일
        pasted_file = Clipboard.paste_clip(
            content_type=ContentType.FILE,
            dst_path=self.temp_dir,
            state_type=StateType.ERROR,
        )

        # 인증파일내용
        with open(pasted_file, encoding="utf-8") as f:
            pasted_content = f.read()

        self.assertEqual(pasted_content, original_content)


if __name__ == "__main__":
    unittest.main()