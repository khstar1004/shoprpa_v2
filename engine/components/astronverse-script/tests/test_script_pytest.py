#!/usr/bin/env python3
"""
사용pytest의시도파일
예사용fixture적음mock의사용
"""

from astronverse.script.script import Script


class TestScriptModulePytest:
    """사용pytest시도Script.module기존가능"""

    def test_module_with_main_function(self, basic_env, sample_script_content):
        """시도패키지main데이터의본실행"""
        result = Script.module(content=sample_script_content, __env__=basic_env)

        expected = {"result": "value1_value2", "status": "success"}
        assert result == expected

    def test_module_without_main_function(self, basic_env, no_main_script_content):
        """시도아니오패키지main데이터의본실행"""
        result = Script.module(content=no_main_script_content, __env__=basic_env)

        assert result is None

    def test_module_with_mathematical_operations(self, math_env, math_script_content):
        """시도데이터실행본"""
        result = Script.module(content=math_script_content, __env__=math_env)

        expected = {"sum": 13, "product": 30, "difference": 7, "quotient": 10 / 3}
        assert result == expected

    def test_module_with_list_operations(self, list_env, list_script_content):
        """시도목록 본"""
        result = Script.module(content=list_script_content, __env__=list_env)

        expected = {"result": [1, 1, 3, 4, 5]}
        assert result == expected

    def test_module_with_string_operations(self, string_env, string_script_content):
        """시도문자열본"""
        result = Script.module(content=string_script_content, __env__=string_env)

        expected = {"result": "HELLO WORLD"}
        assert result == expected

    def test_module_with_conditional_logic(self, grade_env, grade_script_content):
        """시도파일본"""
        result = Script.module(content=grade_script_content, __env__=grade_env)

        expected = {"score": 85, "grade": "B"}
        assert result == expected

    def test_module_with_real_world_scenario(self, data_processing_env, data_processing_script_content):
        """시도"""
        result = Script.module(content=data_processing_script_content, __env__=data_processing_env)

        expected = {
            "original_count": 7,
            "filtered_count": 4,
            "average": 27.5,
            "max": 40,
            "min": 15,
        }
        assert result == expected


class TestScriptModuleParameterFormat:
    """시도기존가능매개변수형식의내용 """

    def test_atomic_parameter_format(self):
        """시도기존가능매개변수형식(a=a,b=b)의내용 """
        test_content = """
def main(a, b):
    return {"sum": a + b, "product": a * b}
"""

        env = type("MockEnv", (), {"to_dict": lambda: {"a": 5, "b": 3}})()

        result = Script.module(content=test_content, __env__=env)

        expected = {"sum": 8, "product": 15}
        assert result == expected


class TestScriptModuleEdgeCases:
    """시도가장자리"""

    def test_module_with_none_values(self):
        """시도패키지None값의"""
        test_content = """
def main(value1, value2):
    return {
        "value1_is_none": value1 is None,
        "value2_is_none": value2 is None,
        "combined": f"{value1}_{value2}" if value1 and value2 else "empty"
    }
"""

        env = type("MockEnv", (), {"to_dict": lambda: {"value1": None, "value2": "test"}})()

        result = Script.module(content=test_content, __env__=env)

        expected = {
            "value1_is_none": True,
            "value2_is_none": False,
            "combined": "empty",
        }
        assert result == expected