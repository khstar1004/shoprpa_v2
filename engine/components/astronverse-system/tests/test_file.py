import os
from pathlib import Path
import shutil
import unittest
from uuid import uuid4
from unittest import TestCase, mock

from astronverse.system import *
from astronverse.system.file import File


def _make_temp_dir() -> str:
    temp_root = Path(__file__).resolve().parents[4] / "build" / "tmp" / "system-tests"
    temp_root.mkdir(parents=True, exist_ok=True)
    temp_dir = temp_root / f"file-{uuid4().hex}"
    temp_dir.mkdir()
    return str(temp_dir)


class TestFile(TestCase):
    def setUp(self):
        """시도전의준비"""
        self.temp_dir = _make_temp_dir()
        self.test_file_path = os.path.join(self.temp_dir, "test_file.txt")
        self.test_content = "예일개시도파일내용"

        # 생성시도파일
        with open(self.test_file_path, "w", encoding="utf-8") as f:
            f.write(self.test_content)

    def tearDown(self):
        """시도후의관리"""
        if os.path.exists(self.temp_dir):
            shutil.rmtree(self.temp_dir)

    def test_file_exist_exist_type(self):
        """시도파일저장된 조회 - 존재함유형"""
        result = File.file_exist(file_path=self.test_file_path, exist_type=ExistType.EXIST)
        self.assertTrue(result)

    def test_file_exist_not_exist_type(self):
        """시도파일저장된 조회 - 찾을 수 없습니다유형"""
        non_existent_file = os.path.join(self.temp_dir, "non_existent.txt")
        result = File.file_exist(file_path=non_existent_file, exist_type=ExistType.NOT_EXIST)
        self.assertTrue(result)

    def test_file_exist_invalid_type(self):
        """시도파일저장된 조회 - 없음유형"""
        with self.assertRaises(NotImplementedError):
            File.file_exist(file_path=self.test_file_path, exist_type="invalid_type")

    def test_file_create_success(self):
        """시도파일생성 - 성공"""
        new_file_name = "new_file.txt"
        result = File.file_create(
            dst_path=self.temp_dir,
            file_name=new_file_name,
            exist_options=OptionType.GENERATE,
        )
        expected_path = os.path.join(self.temp_dir, new_file_name)
        self.assertEqual(result, expected_path)
        self.assertTrue(os.path.exists(expected_path))

    def test_file_create_folder_not_exists(self):
        """시도파일생성 - 목록 폴더찾을 수 없습니다"""
        non_existent_dir = os.path.join(self.temp_dir, "non_existent")
        with self.assertRaises(BaseException):
            File.file_create(dst_path=non_existent_dir, file_name="test.txt")

    def test_file_create_overwrite_existing(self):
        """시도파일생성 - 덮어쓰기완료존재함파일"""
        # 생성일개파일
        existing_file = os.path.join(self.temp_dir, "existing.txt")
        with open(existing_file, "w", encoding="utf-8") as f:
            f.write("기존내용")

        result = File.file_create(
            dst_path=self.temp_dir,
            file_name="existing.txt",
            exist_options=OptionType.OVERWRITE,
        )
        self.assertEqual(result, existing_file)
        # 조회파일여부빈
        with open(existing_file, encoding="utf-8") as f:
            content = f.read()
        self.assertEqual(content, "")

    def test_file_delete_success(self):
        """시도파일삭제 - 성공"""
        result = File.file_delete(file_path=self.test_file_path, delete_options=DeleteType.DELETE)
        self.assertTrue(result)
        self.assertFalse(os.path.exists(self.test_file_path))

    def test_file_delete_file_not_exists(self):
        """시도파일삭제 - 파일찾을 수 없습니다"""
        non_existent_file = os.path.join(self.temp_dir, "non_existent.txt")
        with self.assertRaises(BaseException):
            File.file_delete(file_path=non_existent_file)

    @mock.patch("send2trash.send2trash")
    def test_file_delete_trash(self, mock_send2trash):
        """시도파일삭제 - 입력돌아가기"""
        result = File.file_delete(file_path=self.test_file_path, delete_options=DeleteType.TRASH)
        self.assertTrue(result)
        mock_send2trash.assert_called_once_with(self.test_file_path)

    def test_file_copy_success(self):
        """시도파일복사 - 성공"""
        target_dir = os.path.join(self.temp_dir, "target")
        os.makedirs(target_dir, exist_ok=True)

        result = File.file_copy(
            file_path=self.test_file_path,
            target_path=target_dir,
            state_type=StateType.ERROR,
            file_name="",
            copy_options=OptionType.GENERATE,
        )
        expected_path = os.path.join(target_dir, "test_file.txt")
        self.assertEqual(result, expected_path)
        self.assertTrue(os.path.exists(expected_path))

    def test_file_copy_source_not_exists(self):
        """시도파일복사 - 파일찾을 수 없습니다"""
        non_existent_file = os.path.join(self.temp_dir, "non_existent.txt")
        target_dir = os.path.join(self.temp_dir, "target")
        os.makedirs(target_dir, exist_ok=True)

        with self.assertRaises(BaseException):
            File.file_copy(file_path=non_existent_file, target_path=target_dir)

    def test_file_write_append(self):
        """시도파일입력 - 추가 입력방식"""
        content = "추가 입력의내용"
        result = File.file_write(
            file_path=self.test_file_path,
            file_option=StateType.ERROR,
            msg=content,
            write_type=WriteType.APPEND,
        )
        self.assertEqual(result, self.test_file_path)

        with open(self.test_file_path, encoding="utf-8") as f:
            final_content = f.read()
        self.assertEqual(final_content, self.test_content + content)

    def test_file_write_overwrite(self):
        """시도파일입력 - 덮어쓰기방식"""
        content = "덮어쓰기의내용"
        result = File.file_write(
            file_path=self.test_file_path,
            file_option=StateType.ERROR,
            msg=content,
            write_type=WriteType.OVERWRITE,
        )
        self.assertEqual(result, self.test_file_path)

        with open(self.test_file_path, encoding="utf-8") as f:
            final_content = f.read()
        self.assertEqual(final_content, content)

    def test_file_read_all(self):
        """시도파일가져오기 - 전체내용"""
        result = File.file_read(file_path=self.test_file_path, read_type=ReadType.ALL)
        self.assertEqual(result, self.test_content)

    def test_file_read_lines(self):
        """시도파일가져오기 - 행가져오기"""
        # 생성다중행파일
        multi_line_content = "일행\n이행\n삼행"
        multi_line_file = os.path.join(self.temp_dir, "multi_line.txt")
        with open(multi_line_file, "w", encoding="utf-8") as f:
            f.write(multi_line_content)

        result = File.file_read(file_path=multi_line_file, read_type=ReadType.List)
        expected_lines = ["일행", "이행", "삼행"]
        self.assertEqual(result, expected_lines)

    def test_file_move_success(self):
        """시도파일 - 성공"""
        target_dir = os.path.join(self.temp_dir, "target")
        os.makedirs(target_dir, exist_ok=True)

        result = File.file_move(
            file_path=self.test_file_path,
            target_folder=target_dir,
            state_type=StateType.ERROR,
            file_name="",
            exist_options=OptionType.GENERATE,
        )
        expected_path = os.path.join(target_dir, "test_file.txt")
        self.assertEqual(result, expected_path)
        self.assertTrue(os.path.exists(expected_path))
        self.assertFalse(os.path.exists(self.test_file_path))

    def test_file_rename_success(self):
        """시도파일이름 변경 - 성공"""
        new_name = "renamed_file"
        result = File.file_rename(
            file_path=self.test_file_path,
            new_name=new_name,
            exist_options=OptionType.GENERATE,
        )
        expected_path = os.path.join(self.temp_dir, "{}.txt".format(new_name))
        self.assertEqual(result, expected_path)
        self.assertTrue(os.path.exists(expected_path))
        self.assertFalse(os.path.exists(self.test_file_path))

    def test_file_search_fuzzy(self):
        """시도파일검색 - 검색"""
        # 생성다중개시도파일
        files = ["test1.txt", "test2.txt", "other.txt"]
        for file_name in files:
            file_path = os.path.join(self.temp_dir, file_name)
            with open(file_path, "w", encoding="utf-8") as f:
                f.write("content")

        result = File.file_search(folder_path=self.temp_dir, find_type=SearchType.FUZZY, search_pattern="test")
        self.assertGreaterEqual(len(result), 2)

    def test_file_search_exact(self):
        """시도파일검색 - 검색"""
        result = File.file_search(
            folder_path=self.temp_dir,
            find_type=SearchType.EXACT,
            search_pattern="test_file.txt",
        )
        self.assertEqual(len(result), 1)
        self.assertTrue(any("test_file.txt" in str(item) for item in result))

    def test_file_wait_status_created(self):
        """시도파일대기상태 - 생성상태"""
        # 생성일개새파일시도대기
        wait_file = os.path.join(self.temp_dir, "wait_file.txt")
        with open(wait_file, "w", encoding="utf-8") as f:
            f.write("content")

        result = File.file_wait_status(file_path=wait_file, status_type=StatusType.CREATED, wait_time=1)
        self.assertTrue(result)

    def test_file_info_all(self):
        """시도가져오기파일정보 - 전체정보"""
        result = File.file_info(file_path=self.test_file_path, info_type=InfoType.ALL)
        self.assertIsInstance(result, dict)
        self.assertIn("name", result)
        self.assertIn("size", result)
        self.assertIn("abs_path", result)

    def test_get_file_list(self):
        """시도가져오기파일목록"""
        # 생성다중개파일
        files = ["file1.txt", "file2.txt", "file3.txt"]
        for file_name in files:
            file_path = os.path.join(self.temp_dir, file_name)
            with open(file_path, "w", encoding="utf-8") as f:
                f.write("content")

        result = File.get_file_list(
            folder_path=self.temp_dir,
            traverse_subfolder=TraverseType.NO,
            output_type=OutputType.LIST,
        )
        self.assertIsInstance(result, list)
        self.assertEqual(len(result), 4)  # 패키지test_file.txt


if __name__ == "__main__":
    unittest.main()
