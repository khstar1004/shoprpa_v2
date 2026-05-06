import unittest

from astronverse.script.script import Script


class TestScriptModule(unittest.TestCase):
    """시도Script.module기존가능"""

    def setUp(self):
        """시도전의준비"""

        # 생성의객체, to_dict방법법
        class MockEnv:
            def __init__(self, params):
                self.params = params

            def to_dict(self):
                return self.params

        self.env = MockEnv({"param1": "value1", "param2": "value2"})

    def test_module_with_main_function(self):
        """시도패키지main데이터의본실행"""
        # 준비시도데이터
        test_content = """
def main(param1, param2):
    return {"result": f"{param1}_{param2}", "status": "success"}
"""

        # 호출기존가능, 사용a=a,b=b의방식
        result = Script.module(content=test_content, __env__=self.env)

        # 인증결과
        expected = {"result": "value1_value2", "status": "success"}
        self.assertEqual(result, expected)

    def test_module_without_main_function(self):
        """시도아니오패키지main데이터의본실행"""
        # 준비시도데이터
        test_content = """
# 있음main데이터의본
x = 10
y = 20
"""

        # 호출기존가능, 사용a=a,b=b의방식
        result = Script.module(content=test_content, __env__=self.env)

        # 인증결과(있음main데이터시해당반환None)
        self.assertIsNone(result)

    def test_module_with_complex_script(self):
        """시도복사본의실행"""
        # 준비시도데이터
        test_content = """
import json

def main(param1, param2):
    # 복사의서비스
    data = {
        "input": {"param1": param1, "param2": param2},
        "processed": True,
        "timestamp": "2024-01-01"
    }
    
    # 사용logger기록로그
    logger.info("관리완료")
    
    return data
"""

        # 호출기존가능, 사용a=a,b=b의방식
        result = Script.module(content=test_content, __env__=self.env)

        # 인증결과
        expected = {
            "input": {"param1": "value1", "param2": "value2"},
            "processed": True,
            "timestamp": "2024-01-01",
        }
        self.assertEqual(result, expected)

    def test_module_with_error_handling(self):
        """시도본실행오류 관리"""
        # 준비시도데이터(패키지오류의본)
        test_content = """
def main(param1, param2):
    # 제어일개오류
    undefined_variable + 1
    return "should not reach here"
"""

        # 인증예외정상출력
        with self.assertRaises(NameError):
            Script.module(content=test_content, __env__=self.env)

    def test_module_with_empty_content(self):
        """시도빈내용의"""
        # 호출기존가능, 사용a=a,b=b의방식
        result = Script.module(content="", __env__=self.env)

        # 인증결과
        self.assertIsNone(result)


if __name__ == "__main__":
    unittest.main()