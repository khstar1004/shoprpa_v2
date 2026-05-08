import os
from pathlib import Path
import shutil
import unittest
from uuid import uuid4
from unittest import TestCase, mock

from astronverse.system import *
from astronverse.system.folder import Folder


def _make_temp_dir() -> str:
    temp_root = Path(__file__).resolve().parents[4] / "build" / "tmp" / "system-tests"
    temp_root.mkdir(parents=True, exist_ok=True)
    temp_dir = temp_root / f"folder-{uuid4().hex}"
    temp_dir.mkdir()
    return str(temp_dir)


class TestFolder(TestCase):
    def setUp(self):
        """시도전의준비"""
        self.temp_dir = _make_temp_dir()
        self.test_folder_path = os.path.join(self.temp_dir, "test_folder")
        self.test_content = "시도파일내용"

        # 생성시도폴더
        os.makedirs(self.test_folder_path, exist_ok=True)

        # 에서시도폴더중생성일파일
        for i in range(3):
            file_path = os.path.join(self.test_folder_path, f"file_{i}.txt")
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(f"파일 {i} 의내용")

    def tearDown(self):
        """시도후의관리"""
        if os.path.exists(self.temp_dir):
            shutil.rmtree(self.temp_dir)

    def test_folder_exist_exist_type(self):
        """시도폴더저장된 조회 - 존재함유형"""
        result = Folder.folder_exist(folder_path=self.test_folder_path, exist_type=ExistType.EXIST)
        self.assertTrue(result)

    def test_folder_exist_not_exist_type(self):
        """시도폴더저장된 조회 - 찾을 수 없습니다유형"""
        non_existent_folder = os.path.join(self.temp_dir, "non_existent")
        result = Folder.folder_exist(folder_path=non_existent_folder, exist_type=ExistType.NOT_EXIST)
        self.assertTrue(result)

    def test_folder_exist_invalid_type(self):
        """시도폴더저장된 조회 - 없음유형"""
        with self.assertRaises(NotImplementedError):
            Folder.folder_exist(folder_path=self.test_folder_path, exist_type="invalid_type")

    @mock.patch("astronverse.system.folder.open_folder")
    def test_folder_open_success(self, mock_open_folder):
        """시도열기폴더 - 성공"""
        Folder.folder_open(folder_path=self.test_folder_path)
        mock_open_folder.assert_called_once_with(self.test_folder_path)

    def test_folder_open_not_exists(self):
        """시도열기폴더 - 폴더찾을 수 없습니다"""
        non_existent_folder = os.path.join(self.temp_dir, "non_existent")
        with self.assertRaises(BaseException):
            Folder.folder_open(folder_path=non_existent_folder)

    def test_folder_create_success(self):
        """시도폴더생성 - 성공"""
        new_folder_name = "new_folder"
        result = Folder.folder_create(
            target_path=self.temp_dir,
            folder_name=new_folder_name,
            exist_options=OptionType.GENERATE,
        )
        expected_path = os.path.join(self.temp_dir, new_folder_name)
        self.assertEqual(result, expected_path)
        self.assertTrue(os.path.exists(expected_path))

    def test_folder_create_target_not_exists(self):
        """시도폴더생성 - 목록경로찾을 수 없습니다"""
        non_existent_dir = os.path.join(self.temp_dir, "non_existent")
        with self.assertRaises(BaseException):
            Folder.folder_create(target_path=non_existent_dir, folder_name="test_folder")

    def test_folder_create_overwrite_existing(self):
        """시도폴더생성 - 덮어쓰기완료존재함폴더"""
        # 생성일개폴더
        existing_folder = os.path.join(self.temp_dir, "existing")
        os.makedirs(existing_folder, exist_ok=True)

        # 에서폴더중생성일파일
        with open(os.path.join(existing_folder, "test.txt"), "w", encoding="utf-8") as f:
            f.write("기존내용")

        result = Folder.folder_create(
            target_path=self.temp_dir,
            folder_name="existing",
            exist_options=OptionType.OVERWRITE,
        )
        self.assertEqual(result, existing_folder)
        # 조회폴더여부빈
        self.assertFalse(os.path.exists(os.path.join(existing_folder, "test.txt")))

    def test_folder_create_skip_existing(self):
        """시도폴더생성 - 건너뛰기완료존재함폴더"""
        existing_folder = os.path.join(self.temp_dir, "existing")
        os.makedirs(existing_folder, exist_ok=True)

        result = Folder.folder_create(
            target_path=self.temp_dir,
            folder_name="existing",
            exist_options=OptionType.SKIP,
        )
        self.assertEqual(result, existing_folder)

    def test_folder_delete_success(self):
        """시도폴더삭제 - 성공"""
        result = Folder.folder_delete(folder_path=self.test_folder_path, delete_options=DeleteType.DELETE)
        self.assertTrue(result)
        self.assertFalse(os.path.exists(self.test_folder_path))

    def test_folder_delete_folder_not_exists(self):
        """시도폴더삭제 - 폴더찾을 수 없습니다"""
        non_existent_folder = os.path.join(self.temp_dir, "non_existent")
        with self.assertRaises(BaseException):
            Folder.folder_delete(folder_path=non_existent_folder)

    @mock.patch("send2trash.send2trash")
    def test_folder_delete_trash(self, mock_send2trash):
        """시도폴더삭제 - 입력돌아가기"""
        result = Folder.folder_delete(folder_path=self.test_folder_path, delete_options=DeleteType.TRASH)
        self.assertTrue(result)
        mock_send2trash.assert_called_once_with(self.test_folder_path)

    def test_folder_copy_success(self):
        """시도폴더복사 - 성공"""
        target_dir = os.path.join(self.temp_dir, "target")
        os.makedirs(target_dir, exist_ok=True)

        result = Folder.folder_copy(
            source_path=self.test_folder_path,
            target_path=target_dir,
            state_type=StateType.ERROR,
            folder_name="",
            exist_options=OptionType.GENERATE,
        )
        expected_path = os.path.join(target_dir, "test_folder")
        self.assertEqual(result, expected_path)
        self.assertTrue(os.path.exists(expected_path))

    def test_folder_copy_source_not_exists(self):
        """시도폴더복사 - 폴더찾을 수 없습니다"""
        non_existent_folder = os.path.join(self.temp_dir, "non_existent")
        target_dir = os.path.join(self.temp_dir, "target")
        os.makedirs(target_dir, exist_ok=True)

        with self.assertRaises(BaseException):
            Folder.folder_copy(source_path=non_existent_folder, target_path=target_dir)

    def test_folder_copy_with_custom_name(self):
        """시도폴더복사 - 지정이름"""
        target_dir = os.path.join(self.temp_dir, "target")
        os.makedirs(target_dir, exist_ok=True)
        custom_name = "custom_folder"

        result = Folder.folder_copy(
            source_path=self.test_folder_path,
            target_path=target_dir,
            state_type=StateType.ERROR,
            folder_name=custom_name,
            exist_options=OptionType.GENERATE,
        )
        expected_path = os.path.join(target_dir, custom_name)
        self.assertEqual(result, expected_path)
        self.assertTrue(os.path.exists(expected_path))

    def test_folder_move_success(self):
        """시도폴더 - 성공"""
        target_dir = os.path.join(self.temp_dir, "target")
        os.makedirs(target_dir, exist_ok=True)

        result = Folder.folder_move(
            folder_path=self.test_folder_path,
            target_folder=target_dir,
            state_type=StateType.ERROR,
            folder_name="",
            exist_options=OptionType.GENERATE,
        )
        expected_path = os.path.join(target_dir, "test_folder")
        self.assertEqual(result, expected_path)
        self.assertTrue(os.path.exists(expected_path))
        self.assertFalse(os.path.exists(self.test_folder_path))

    def test_folder_move_with_custom_name(self):
        """시도폴더 - 지정이름"""
        target_dir = os.path.join(self.temp_dir, "target")
        os.makedirs(target_dir, exist_ok=True)
        custom_name = "moved_folder"

        result = Folder.folder_move(
            folder_path=self.test_folder_path,
            target_folder=target_dir,
            state_type=StateType.ERROR,
            folder_name=custom_name,
            exist_options=OptionType.GENERATE,
        )
        expected_path = os.path.join(target_dir, custom_name)
        self.assertEqual(result, expected_path)
        self.assertTrue(os.path.exists(expected_path))
        self.assertFalse(os.path.exists(self.test_folder_path))

    def test_folder_rename_success(self):
        """시도폴더이름 변경 - 성공"""
        new_name = "renamed_folder"
        result = Folder.folder_rename(
            folder_path=self.test_folder_path,
            new_name=new_name,
            exist_options=OptionType.GENERATE,
        )
        expected_path = os.path.join(self.temp_dir, new_name)
        self.assertEqual(result, expected_path)
        self.assertTrue(os.path.exists(expected_path))
        self.assertFalse(os.path.exists(self.test_folder_path))

    def test_folder_rename_with_custom_name(self):
        """시도폴더이름 변경 - 지정이름"""
        new_name = "custom_renamed_folder"
        result = Folder.folder_rename(
            folder_path=self.test_folder_path,
            new_name=new_name,
            exist_options=OptionType.GENERATE,
        )
        expected_path = os.path.join(self.temp_dir, new_name)
        self.assertEqual(result, expected_path)
        self.assertTrue(os.path.exists(expected_path))

    def test_folder_clear_success(self):
        """시도폴더빈 - 성공"""
        # 에서시도폴더중생성일파일및폴더
        sub_folder = os.path.join(self.test_folder_path, "sub_folder")
        os.makedirs(sub_folder, exist_ok=True)

        for i in range(3):
            file_path = os.path.join(self.test_folder_path, f"clear_file_{i}.txt")
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(f"파일 {i} 의내용")

        result = Folder.folder_clear(folder_path=self.test_folder_path)
        self.assertTrue(result)

        # 조회폴더여부빈
        files = os.listdir(self.test_folder_path)
        self.assertEqual(len(files), 0)

    def test_folder_clear_empty_folder(self):
        """시도폴더빈 - 빈폴더"""
        empty_folder = os.path.join(self.temp_dir, "empty_folder")
        os.makedirs(empty_folder, exist_ok=True)

        result = Folder.folder_clear(folder_path=empty_folder)
        self.assertTrue(result)

    def test_get_folder_list_basic(self):
        """시도가져오기폴더목록 - 본공가능"""
        # 생성다중개폴더
        sub_folders = ["sub1", "sub2", "sub3"]
        for folder_name in sub_folders:
            folder_path = os.path.join(self.test_folder_path, folder_name)
            os.makedirs(folder_path, exist_ok=True)

        result = Folder.get_folder_list(
            folder_path=self.test_folder_path,
            traverse_subfolder=TraverseType.NO,
            output_type=OutputType.LIST,
        )
        self.assertIsInstance(result, list)
        self.assertEqual(len(result), 3)  # 3개폴더

    def test_get_folder_list_with_traverse(self):
        """시도가져오기폴더목록 - 폴더"""
        # 생성폴더결과
        sub_folder = os.path.join(self.test_folder_path, "sub_folder")
        os.makedirs(sub_folder, exist_ok=True)

        nested_folder = os.path.join(sub_folder, "nested_folder")
        os.makedirs(nested_folder, exist_ok=True)

        result = Folder.get_folder_list(
            folder_path=self.test_folder_path,
            traverse_subfolder=TraverseType.YES,
            output_type=OutputType.LIST,
        )
        self.assertIsInstance(result, list)
        self.assertGreater(len(result), 1)  # 해당패키지폴더

    def test_get_folder_list_excel_output(self):
        """시도가져오기폴더목록 - Excel출력"""
        # 생성시도폴더
        test_folders = ["folder1", "folder2", "folder3"]
        for folder_name in test_folders:
            folder_path = os.path.join(self.test_folder_path, folder_name)
            os.makedirs(folder_path, exist_ok=True)

        excel_path = os.path.join(self.temp_dir, "excel_output")
        os.makedirs(excel_path, exist_ok=True)

        result = Folder.get_folder_list(
            folder_path=self.test_folder_path,
            traverse_subfolder=TraverseType.NO,
            output_type=OutputType.EXCEL,
            excel_path=excel_path,
            state_type=StateType.ERROR,
            excel_name="folders.xlsx",
        )
        self.assertIsInstance(result, list)

    def test_get_folder_list_with_sorting(self):
        """시도가져오기폴더목록 - 정렬"""
        # 생성시도폴더
        test_folders = ["folder1", "folder2", "folder3"]
        for folder_name in test_folders:
            folder_path = os.path.join(self.test_folder_path, folder_name)
            os.makedirs(folder_path, exist_ok=True)

        result = Folder.get_folder_list(
            folder_path=self.test_folder_path,
            traverse_subfolder=TraverseType.NO,
            output_type=OutputType.LIST,
            sort_method=SortMethod.NONE,
            sort_type=SortType.ASCENDING,
        )
        self.assertIsInstance(result, list)

    def test_folder_create_generate_option(self):
        """시도폴더생성 - 완료선택"""
        # 생성일개폴더
        existing_folder = os.path.join(self.temp_dir, "existing")
        os.makedirs(existing_folder, exist_ok=True)

        result = Folder.folder_create(
            target_path=self.temp_dir,
            folder_name="existing",
            exist_options=OptionType.GENERATE,
        )
        # 해당완료일개새의폴더이름
        self.assertNotEqual(result, existing_folder)
        self.assertTrue(os.path.exists(result))

    def test_folder_copy_overwrite_option(self):
        """시도폴더복사 - 덮어쓰기선택"""
        target_dir = os.path.join(self.temp_dir, "target")
        os.makedirs(target_dir, exist_ok=True)

        # 생성일개목록 폴더
        existing_target = os.path.join(target_dir, "test_folder")
        os.makedirs(existing_target, exist_ok=True)

        result = Folder.folder_copy(
            source_path=self.test_folder_path,
            target_path=target_dir,
            state_type=StateType.ERROR,
            folder_name="",
            exist_options=OptionType.OVERWRITE,
        )
        self.assertEqual(result, existing_target)

    def test_folder_copy_skip_option(self):
        """시도폴더복사 - 건너뛰기선택"""
        target_dir = os.path.join(self.temp_dir, "target")
        os.makedirs(target_dir, exist_ok=True)

        # 생성일개목록 폴더
        existing_target = os.path.join(target_dir, "test_folder")
        os.makedirs(existing_target, exist_ok=True)

        result = Folder.folder_copy(
            source_path=self.test_folder_path,
            target_path=target_dir,
            state_type=StateType.ERROR,
            folder_name="",
            exist_options=OptionType.SKIP,
        )
        self.assertEqual(result, existing_target)

    def test_folder_move_overwrite_option(self):
        """시도폴더 - 덮어쓰기선택"""
        target_dir = os.path.join(self.temp_dir, "target")
        os.makedirs(target_dir, exist_ok=True)

        # 생성일개목록 폴더
        existing_target = os.path.join(target_dir, "test_folder")
        os.makedirs(existing_target, exist_ok=True)

        result = Folder.folder_move(
            folder_path=self.test_folder_path,
            target_folder=target_dir,
            state_type=StateType.ERROR,
            folder_name="",
            exist_options=OptionType.OVERWRITE,
        )
        self.assertEqual(result, existing_target)

    def test_folder_rename_overwrite_option(self):
        """시도폴더이름 변경 - 덮어쓰기선택"""
        # 생성일개이름폴더
        existing_folder = os.path.join(self.temp_dir, "renamed_folder")
        os.makedirs(existing_folder, exist_ok=True)

        result = Folder.folder_rename(
            folder_path=self.test_folder_path,
            new_name="renamed_folder",
            exist_options=OptionType.OVERWRITE,
        )
        self.assertEqual(result, existing_folder)

    def test_folder_rename_skip_option(self):
        """시도폴더이름 변경 - 건너뛰기선택"""
        # 생성일개이름폴더
        existing_folder = os.path.join(self.temp_dir, "renamed_folder")
        os.makedirs(existing_folder, exist_ok=True)

        result = Folder.folder_rename(
            folder_path=self.test_folder_path,
            new_name="renamed_folder",
            exist_options=OptionType.SKIP,
        )
        self.assertEqual(result, existing_folder)

    def test_folder_with_special_characters(self):
        """시도폴더 - 패키지문자의폴더이름"""
        special_folder_name = "시도폴더_패키지중국어, 영어, 숫자123및기호!@#￥%"
        special_folder_path = os.path.join(self.temp_dir, special_folder_name)
        os.makedirs(special_folder_path, exist_ok=True)

        # 시도저장된 조회
        result = Folder.folder_exist(folder_path=special_folder_path, exist_type=ExistType.EXIST)
        self.assertTrue(result)

        # 시도이름 변경
        new_name = "이름 변경후의폴더"
        result = Folder.folder_rename(
            folder_path=special_folder_path,
            new_name=new_name,
            exist_options=OptionType.GENERATE,
        )
        expected_path = os.path.join(self.temp_dir, new_name)
        self.assertEqual(result, expected_path)

    def test_folder_with_spaces_in_name(self):
        """시도폴더 - 패키지공백의폴더이름"""
        folder_with_spaces = os.path.join(self.temp_dir, "folder with spaces")
        os.makedirs(folder_with_spaces, exist_ok=True)

        # 시도저장된 조회
        result = Folder.folder_exist(folder_path=folder_with_spaces, exist_type=ExistType.EXIST)
        self.assertTrue(result)

        # 시도이름 변경
        new_name = "folder without spaces"
        result = Folder.folder_rename(
            folder_path=folder_with_spaces,
            new_name=new_name,
            exist_options=OptionType.GENERATE,
        )
        expected_path = os.path.join(self.temp_dir, new_name)
        self.assertEqual(result, expected_path)


if __name__ == "__main__":
    unittest.main()
