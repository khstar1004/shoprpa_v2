import tempfile
import unittest
from pathlib import Path
from shutil import rmtree
from unittest.mock import patch

from astronverse.enterprise import Enterprise
from astronverse.enterprise import enterprise as enterprise_module
from astronverse.enterprise.error import BaseException


class FakeDownloadResponse:
    status_code = 200
    text = ""
    headers = {
        "Content-Type": "application/octet-stream",
        "content-disposition": 'attachment; filename="report.txt"',
    }

    def iter_content(self, chunk_size=8192):
        yield b"hello "
        yield b"world"


class TestEnterprise(unittest.TestCase):
    def setUp(self):
        self.tmp_root = Path.cwd() / "build" / "tmp" / "enterprise-tests"
        self.tmp_root.mkdir(parents=True, exist_ok=True)

    def tearDown(self):
        rmtree(self.tmp_root, ignore_errors=True)

    def test_upload_to_sharefolder_rejects_missing_file(self):
        with self.assertRaises(BaseException):
            Enterprise.upload_to_sharefolder(str(Path(tempfile.gettempdir()) / "shoprpa-missing-file.txt"))

    def test_download_from_sharefolder_writes_stream_to_save_folder(self):
        save_dir = self.tmp_root / "download"
        save_dir.mkdir(parents=True, exist_ok=True)
        with patch.object(enterprise_module.requests, "get", return_value=FakeDownloadResponse()) as mock_get:
            result = Enterprise.download_from_sharefolder(file_path=12345, save_folder=str(save_dir))

        saved_path = Path(result)
        self.assertEqual(saved_path.name, "report.txt")
        self.assertEqual(saved_path.read_bytes(), b"hello world")
        mock_get.assert_called_once()

    def test_download_from_sharefolder_requires_absolute_save_folder(self):
        with self.assertRaises(BaseException):
            Enterprise.download_from_sharefolder(file_path=12345, save_folder="relative-folder")

    def test_get_shared_variable_returns_plain_values(self):
        shared_data = {
            "subVarList": [
                {"varName": "account", "varValue": "sales", "encrypt": False},
                {"varName": "limit", "varValue": "100", "encrypt": False},
            ]
        }
        with patch.object(enterprise_module, "get_remote_var_key", return_value="unused"):
            with patch.object(enterprise_module, "get_remote_var_value", return_value=shared_data):
                result = Enterprise.get_shared_variable("shared-data")

        self.assertEqual(result, {"account": "sales", "limit": "100"})

    def test_get_shared_variable_returns_none_for_missing_value(self):
        with patch.object(enterprise_module, "get_remote_var_key", return_value="unused"):
            with patch.object(enterprise_module, "get_remote_var_value", return_value=None):
                result = Enterprise.get_shared_variable("missing")

        self.assertIsNone(result)

    def test_error_code_format_does_not_mutate_template(self):
        first = enterprise_module.FILE_DOWNLOAD_FAILED_FORMAT.format("first")
        second = enterprise_module.FILE_DOWNLOAD_FAILED_FORMAT.format("second")

        self.assertIn("first", first.message)
        self.assertIn("second", second.message)
        self.assertNotIn("first", second.message)


if __name__ == "__main__":
    unittest.main()
