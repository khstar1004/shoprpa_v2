import os
from pathlib import Path
import shutil
import unittest
from uuid import uuid4
from unittest import TestCase

from astronverse.system import *
from astronverse.system.compress import Compress


def _make_temp_dir() -> str:
    temp_root = Path(__file__).resolve().parents[4] / "build" / "tmp" / "system-tests"
    temp_root.mkdir(parents=True, exist_ok=True)
    temp_dir = temp_root / f"compress-{uuid4().hex}"
    temp_dir.mkdir()
    return str(temp_dir)


class TestCompress(TestCase):
    def setUp(self):
        """시도전의준비"""
        self.temp_dir = _make_temp_dir()
        self.test_file_path = os.path.join(self.temp_dir, "test_file.txt")
        self.test_folder_path = os.path.join(self.temp_dir, "test_folder")
        self.compress_dir = os.path.join(self.temp_dir, "compress_output")

        # 생성시도파일및폴더
        with open(self.test_file_path, "w", encoding="utf-8") as f:
            f.write("시도파일내용")
        os.makedirs(self.test_folder_path, exist_ok=True)

        # 에서시도폴더중생성일파일
        for i in range(3):
            file_path = os.path.join(self.test_folder_path, f"file_{i}.txt")
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(f"파일 {i} 의내용")

        os.makedirs(self.compress_dir, exist_ok=True)

    def tearDown(self):
        """시도후의관리"""
        if os.path.exists(self.temp_dir):
            shutil.rmtree(self.temp_dir)

    def test_compress_file_success(self):
        """시도압축 - 단일개파일성공"""
        # 압축단일개파일
        result = Compress.compress(
            file_type=FileFolderType.FILE,
            file_path=self.test_file_path,
            compress_dir=self.compress_dir,
            state_type=StateType.ERROR,
        )

        # 인증압축파일존재함
        self.assertTrue(os.path.exists(result))
        self.assertTrue(result.endswith(".zip"))
        self.assertEqual(os.path.basename(result), "test_file.zip")

        # 인증압축파일내용
        self._verify_zip_content(result, ["test_file.txt"])

    def test_compress_folder_success(self):
        """시도압축 - 폴더성공"""
        # 압축폴더
        result = Compress.compress(
            file_type=FileFolderType.FOLDER,
            folder_path=self.test_folder_path,
            compress_dir=self.compress_dir,
            state_type=StateType.ERROR,
        )

        # 인증압축파일존재함
        self.assertTrue(os.path.exists(result))
        self.assertTrue(result.endswith(".zip"))
        self.assertEqual(os.path.basename(result), "test_folder.zip")

        # 인증압축파일내용
        expected_files = [
            "test_folder/file_0.txt",
            "test_folder/file_1.txt",
            "test_folder/file_2.txt",
        ]
        self._verify_zip_content(result, expected_files)

    def test_compress_both_success(self):
        """시도압축 - 파일및폴더성공"""
        # 압축파일및폴더
        result = Compress.compress(
            file_type=FileFolderType.BOTH,
            file_path=self.test_file_path,
            folder_path=self.test_folder_path,
            compress_dir=self.compress_dir,
            state_type=StateType.ERROR,
        )

        # 인증압축파일존재함
        self.assertTrue(os.path.exists(result))
        self.assertTrue(result.endswith(".zip"))

        # 인증압축파일내용
        expected_files = [
            "test_file.txt",
            "test_folder/file_0.txt",
            "test_folder/file_1.txt",
            "test_folder/file_2.txt",
        ]
        self._verify_zip_content(result, expected_files)

    def test_compress_file_empty_path(self):
        """시도압축 - 파일 경로비어 있습니다"""
        with self.assertRaises(ValueError):
            Compress.compress(
                file_type=FileFolderType.FILE,
                file_path="",
                compress_dir=self.compress_dir,
            )

    def test_compress_folder_empty_path(self):
        """시도압축 - 폴더 경로비어 있습니다"""
        with self.assertRaises(ValueError):
            Compress.compress(
                file_type=FileFolderType.FOLDER,
                folder_path="",
                compress_dir=self.compress_dir,
            )

    def test_compress_both_empty_paths(self):
        """시도압축 - 파일및폴더 경로 비어 있습니다"""
        with self.assertRaises(ValueError):
            Compress.compress(
                file_type=FileFolderType.BOTH,
                file_path="",
                folder_path="",
                compress_dir=self.compress_dir,
            )

    def test_compress_with_password(self):
        """시도압축 - 비밀번호"""
        password = "test_password"

        # 압축파일비밀번호
        result = Compress.compress(
            file_type=FileFolderType.FILE,
            file_path=self.test_file_path,
            compress_dir=self.compress_dir,
            state_type=StateType.ERROR,
            pwd=password,
        )

        # 인증압축파일존재함
        self.assertTrue(os.path.exists(result))
        self.assertTrue(result.endswith(".zip"))

        # 인증비밀번호의압축파일가능으로보통압축 해제
        extract_dir = os.path.join(self.temp_dir, "extract_test")
        os.makedirs(extract_dir, exist_ok=True)

        uncompress_result = Compress.uncompress(
            source_path=result,
            target_path=extract_dir,
            status_type=StateType.ERROR,
            pwd=password,
        )

        # 인증압축 해제결과
        self.assertTrue(os.path.exists(uncompress_result))
        extracted_file = os.path.join(extract_dir, "test_file.txt")
        self.assertTrue(os.path.exists(extracted_file))

    def test_compress_with_custom_name(self):
        """시도압축 - 지정압축패키지이름"""
        custom_name = "custom_archive"

        # 압축파일사용지정이름
        result = Compress.compress(
            file_type=FileFolderType.FILE,
            file_path=self.test_file_path,
            compress_dir=self.compress_dir,
            state_type=StateType.ERROR,
            compress_name=custom_name,
        )

        # 인증압축파일저장된 이름정상
        self.assertTrue(os.path.exists(result))
        self.assertEqual(os.path.basename(result), f"{custom_name}.zip")

    def test_compress_folder_not_exists_error(self):
        """시도압축 - 목록 폴더아니오저장된 로오류"""
        non_existent_dir = os.path.join(self.temp_dir, "non_existent")
        with self.assertRaises(BaseException):
            Compress.compress(
                file_type=FileFolderType.FILE,
                file_path=self.test_file_path,
                compress_dir=non_existent_dir,
                state_type=StateType.ERROR,
            )

    def test_compress_folder_not_exists_create(self):
        """시도압축 - 목록 폴더아니오저장된 생성"""
        new_dir = os.path.join(self.temp_dir, "new_compress_dir")

        # 압축파일까지찾을 수 없습니다의디렉터리, 로생성
        result = Compress.compress(
            file_type=FileFolderType.FILE,
            file_path=self.test_file_path,
            compress_dir=new_dir,
            state_type=StateType.CREATE,
        )

        # 인증압축파일저장된 디렉터리생성
        self.assertTrue(os.path.exists(result))
        self.assertTrue(os.path.exists(new_dir))

    def test_compress_invalid_save_type(self):
        """시도압축 - 지원하지 않는 저장유형"""
        with self.assertRaises(NotImplementedError):
            Compress.compress(
                file_type=FileFolderType.FILE,
                file_path=self.test_file_path,
                compress_dir=self.compress_dir,
                state_type=StateType.ERROR,
                save_type="invalid_type",
            )

    def test_uncompress_success(self):
        """시도압축 해제 - 성공"""
        # 생성일개의압축파일
        test_file = os.path.join(self.temp_dir, "test_content.txt")
        with open(test_file, "w", encoding="utf-8") as f:
            f.write("시도내용")

        # 사용압축방법법생성zip파일
        zip_path = Compress.compress(
            file_type=FileFolderType.FILE,
            file_path=test_file,
            compress_dir=self.temp_dir,
            state_type=StateType.ERROR,
        )

        # 압축 해제파일
        result = Compress.uncompress(
            source_path=zip_path,
            target_path=self.compress_dir,
            status_type=StateType.ERROR,
        )

        # 인증압축 해제결과
        self.assertTrue(os.path.exists(result))
        self.assertEqual(result, os.path.abspath(self.compress_dir))

        # 인증압축 해제후의파일내용
        extracted_file = os.path.join(self.compress_dir, "test_content.txt")
        self.assertTrue(os.path.exists(extracted_file))
        with open(extracted_file, encoding="utf-8") as f:
            content = f.read()
        self.assertEqual(content, "시도내용")

    def test_uncompress_source_not_exists(self):
        """시도압축 해제 - 파일찾을 수 없습니다"""
        non_existent_zip = os.path.join(self.temp_dir, "non_existent.zip")
        with self.assertRaises(BaseException):
            Compress.uncompress(
                source_path=non_existent_zip,
                target_path=self.compress_dir,
                status_type=StateType.ERROR,
            )

    def test_uncompress_with_password(self):
        """시도압축 해제 - 비밀번호"""
        password = "test_password"

        # 생성일개비밀번호의압축파일
        result = Compress.compress(
            file_type=FileFolderType.FILE,
            file_path=self.test_file_path,
            compress_dir=self.compress_dir,
            state_type=StateType.ERROR,
            pwd=password,
        )

        # 압축 해제비밀번호의파일
        extract_dir = os.path.join(self.temp_dir, "extract_with_pwd")
        os.makedirs(extract_dir, exist_ok=True)

        uncompress_result = Compress.uncompress(
            source_path=result,
            target_path=extract_dir,
            status_type=StateType.ERROR,
            pwd=password,
        )

        # 인증압축 해제결과
        self.assertTrue(os.path.exists(uncompress_result))
        extracted_file = os.path.join(extract_dir, "test_file.txt")
        self.assertTrue(os.path.exists(extracted_file))

    def test_uncompress_target_not_exists_create(self):
        """시도압축 해제 - 목록 폴더아니오저장된 생성"""
        # 생성일개압축파일
        result = Compress.compress(
            file_type=FileFolderType.FILE,
            file_path=self.test_file_path,
            compress_dir=self.compress_dir,
            state_type=StateType.ERROR,
        )

        # 압축 해제까지찾을 수 없습니다의디렉터리, 로생성
        new_dir = os.path.join(self.temp_dir, "new_extract_dir")
        uncompress_result = Compress.uncompress(source_path=result, target_path=new_dir, status_type=StateType.CREATE)

        # 인증압축 해제결과및디렉터리생성
        self.assertTrue(os.path.exists(uncompress_result))
        self.assertTrue(os.path.exists(new_dir))

    def test_uncompress_target_not_exists_error(self):
        """시도압축 해제 - 목록 폴더아니오저장된 로오류"""
        # 생성일개압축파일
        result = Compress.compress(
            file_type=FileFolderType.FILE,
            file_path=self.test_file_path,
            compress_dir=self.compress_dir,
            state_type=StateType.ERROR,
        )

        non_existent_dir = os.path.join(self.temp_dir, "non_existent")
        with self.assertRaises(BaseException):
            Compress.uncompress(
                source_path=result,
                target_path=non_existent_dir,
                status_type=StateType.ERROR,
            )

    def test_uncompress_invalid_save_type(self):
        """시도압축 해제 - 지원하지 않는 저장유형"""
        # 생성일개압축파일
        result = Compress.compress(
            file_type=FileFolderType.FILE,
            file_path=self.test_file_path,
            compress_dir=self.compress_dir,
            state_type=StateType.ERROR,
        )

        with self.assertRaises(NotImplementedError):
            Compress.uncompress(
                source_path=result,
                target_path=self.compress_dir,
                status_type=StateType.ERROR,
                save_type="invalid_type",
            )

    def test_compress_multiple_files(self):
        """시도압축 - 다중개파일"""
        # 생성다중개시도파일
        files = ["file1.txt", "file2.txt", "file3.txt"]
        file_paths = []
        for file_name in files:
            file_path = os.path.join(self.temp_dir, file_name)
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(f"content for {file_name}")
            file_paths.append(file_path)

        file_path_str = ",".join(file_paths)

        # 압축다중개파일
        result = Compress.compress(
            file_type=FileFolderType.FILE,
            file_path=file_path_str,
            compress_dir=self.compress_dir,
            state_type=StateType.ERROR,
        )

        # 인증압축파일존재함
        self.assertTrue(os.path.exists(result))
        self.assertTrue(result.endswith(".zip"))

        # 인증압축파일내용
        self._verify_zip_content(result, files)

    def test_compress_multiple_folders(self):
        """시도압축 - 다중개폴더"""
        # 생성다중개시도폴더
        folders = ["folder1", "folder2", "folder3"]
        folder_paths = []
        for folder_name in folders:
            folder_path = os.path.join(self.temp_dir, folder_name)
            os.makedirs(folder_path, exist_ok=True)

            # 에서매개폴더중생성일파일
            for i in range(2):
                file_path = os.path.join(folder_path, f"file_{i}.txt")
                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(f"content for {folder_name} file {i}")

            folder_paths.append(folder_path)

        folder_path_str = ",".join(folder_paths)

        # 압축다중개폴더
        result = Compress.compress(
            file_type=FileFolderType.FOLDER,
            folder_path=folder_path_str,
            compress_dir=self.compress_dir,
            state_type=StateType.ERROR,
        )

        # 인증압축파일존재함
        self.assertTrue(os.path.exists(result))
        self.assertTrue(result.endswith(".zip"))

    def test_compress_and_uncompress_cycle(self):
        """시도압축압축 해제 - 확인데이터"""
        original_content = "기존시도내용"

        # 생성시도파일
        test_file = os.path.join(self.temp_dir, "cycle_test.txt")
        with open(test_file, "w", encoding="utf-8") as f:
            f.write(original_content)

        # 압축파일
        compress_result = Compress.compress(
            file_type=FileFolderType.FILE,
            file_path=test_file,
            compress_dir=self.compress_dir,
            state_type=StateType.ERROR,
        )

        # 인증압축파일존재함
        self.assertTrue(os.path.exists(compress_result))

        # 압축 해제파일
        extract_dir = os.path.join(self.temp_dir, "cycle_extract")
        os.makedirs(extract_dir, exist_ok=True)

        uncompress_result = Compress.uncompress(
            source_path=compress_result,
            target_path=extract_dir,
            status_type=StateType.ERROR,
        )

        # 인증압축 해제결과
        self.assertTrue(os.path.exists(uncompress_result))

        # 인증압축 해제후의파일내용
        extracted_file = os.path.join(extract_dir, "cycle_test.txt")
        self.assertTrue(os.path.exists(extracted_file))

        with open(extracted_file, encoding="utf-8") as f:
            extracted_content = f.read()

        self.assertEqual(extracted_content, original_content)

    def test_compress_with_special_characters(self):
        """시도압축 - 패키지문자의파일이름"""
        special_file = os.path.join(self.temp_dir, "시도파일: 패키지중국어, 영어, 숫자123및기호!@#￥%.txt")
        with open(special_file, "w", encoding="utf-8") as f:
            f.write("문자파일내용")

        # 압축패키지문자의파일
        result = Compress.compress(
            file_type=FileFolderType.FILE,
            file_path=special_file,
            compress_dir=self.compress_dir,
            state_type=StateType.ERROR,
        )

        # 인증압축파일존재함
        self.assertTrue(os.path.exists(result))
        self.assertTrue(result.endswith(".zip"))

    def _verify_zip_content(self, zip_path, expected_files):
        """인증압축파일내용"""
        import zipfile

        with zipfile.ZipFile(zip_path, "r") as zip_file:
            file_list = zip_file.namelist()

            # 인증모든의파일에서압축패키지중
            for expected_file in expected_files:
                self.assertIn(expected_file, file_list)


if __name__ == "__main__":
    unittest.main()
