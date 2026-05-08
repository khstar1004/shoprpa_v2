import importlib
import sys
import unittest
from types import ModuleType, SimpleNamespace


def _install_logger_stub():
    baseline_module = ModuleType("astronverse.baseline")
    baseline_logger_module = ModuleType("astronverse.baseline.logger")
    logger_module = ModuleType("astronverse.baseline.logger.logger")
    logger_module.logger = SimpleNamespace(info=lambda *args, **kwargs: None)

    sys.modules["astronverse.baseline"] = baseline_module
    sys.modules["astronverse.baseline.logger"] = baseline_logger_module
    sys.modules["astronverse.baseline.logger.logger"] = logger_module


_install_logger_stub()
core_imap4_receive = importlib.import_module("astronverse.email.core_imap4_receive")


class TestCoreImap4Receive(unittest.TestCase):
    def test_encode_imap_utf7_for_chinese_folder_name(self):
        self.assertEqual(core_imap4_receive.encode_imap_utf7("\u5de5\u4f5c"), b"&XeVPXA-")

    def test_decode_folder_list_shows_decoded_folder_name(self):
        folders = core_imap4_receive.decode_folder_list([b'() "/" "&XeVPXA-"'])

        self.assertEqual(folders, ["'\u5de5\u4f5c'  (raw: &XeVPXA-)"])


if __name__ == "__main__":
    unittest.main()
