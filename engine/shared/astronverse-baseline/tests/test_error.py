import unittest

from astronverse.baseline.error.error import BizCode, ErrorCode


class TestErrorCode(unittest.TestCase):
    def test_format_returns_independent_error_code(self):
        template = ErrorCode(BizCode.LocalErr, "failure: {}")

        first = template.format("first")
        second = template.format("second")

        self.assertEqual(template.message, "failure: {}")
        self.assertEqual(first.message, "failure: first")
        self.assertEqual(second.message, "failure: second")

    def test_format_preserves_http_code(self):
        template = ErrorCode(BizCode.LocalErr, "failure: {}", httpcode=503)

        result = template.format("unavailable")

        self.assertEqual(result.httpcode, 503)


if __name__ == "__main__":
    unittest.main()
