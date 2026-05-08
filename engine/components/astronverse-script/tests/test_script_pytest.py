#!/usr/bin/env python3

import unittest

from astronverse.script.script import Script


class MockEnv:
    def __init__(self, params):
        self.params = params

    def to_dict(self):
        return self.params


class TestScriptModulePytestScenarios(unittest.TestCase):
    def test_module_with_main_function(self):
        result = Script.module(
            content="""
def main(param1, param2):
    return {"result": f"{param1}_{param2}", "status": "success"}
""",
            __env__=MockEnv({"param1": "value1", "param2": "value2"}),
        )

        self.assertEqual(result, {"result": "value1_value2", "status": "success"})

    def test_module_without_main_function(self):
        result = Script.module(
            content="""
x = 10
y = 20
""",
            __env__=MockEnv({"param1": "value1", "param2": "value2"}),
        )

        self.assertIsNone(result)

    def test_module_with_mathematical_operations(self):
        result = Script.module(
            content="""
def main(a, b):
    return {"sum": a + b, "product": a * b, "difference": a - b, "quotient": a / b}
""",
            __env__=MockEnv({"a": 10, "b": 3}),
        )

        self.assertEqual(result, {"sum": 13, "product": 30, "difference": 7, "quotient": 10 / 3})

    def test_module_with_list_operations(self):
        result = Script.module(
            content="""
def main(items):
    return {"result": [1] + items[1:]}
""",
            __env__=MockEnv({"items": [1, 1, 3, 4, 5]}),
        )

        self.assertEqual(result, {"result": [1, 1, 3, 4, 5]})

    def test_module_with_string_operations(self):
        result = Script.module(
            content="""
def main(text):
    return {"result": text.upper()}
""",
            __env__=MockEnv({"text": "hello world"}),
        )

        self.assertEqual(result, {"result": "HELLO WORLD"})

    def test_module_with_conditional_logic(self):
        result = Script.module(
            content="""
def main(score):
    if score >= 90:
        grade = "A"
    elif score >= 80:
        grade = "B"
    else:
        grade = "C"
    return {"score": score, "grade": grade}
""",
            __env__=MockEnv({"score": 85}),
        )

        self.assertEqual(result, {"score": 85, "grade": "B"})

    def test_module_with_real_world_scenario(self):
        result = Script.module(
            content="""
def main(values):
    filtered = [value for value in values if value >= 15]
    return {
        "original_count": len(values),
        "filtered_count": len(filtered),
        "average": sum(filtered) / len(filtered),
        "max": max(filtered),
        "min": min(filtered),
    }
""",
            __env__=MockEnv({"values": [5, 10, 15, 25, 30, 40, 45]}),
        )

        self.assertEqual(
            result,
            {
                "original_count": 7,
                "filtered_count": 5,
                "average": 31.0,
                "max": 45,
                "min": 15,
            },
        )


class TestScriptModuleParameterFormat(unittest.TestCase):
    def test_atomic_parameter_format(self):
        result = Script.module(
            content="""
def main(a, b):
    return {"sum": a + b, "product": a * b}
""",
            __env__=MockEnv({"a": 5, "b": 3}),
        )

        self.assertEqual(result, {"sum": 8, "product": 15})


class TestScriptModuleEdgeCases(unittest.TestCase):
    def test_module_with_none_values(self):
        result = Script.module(
            content="""
def main(value1, value2):
    return {
        "value1_is_none": value1 is None,
        "value2_is_none": value2 is None,
        "combined": f"{value1}_{value2}" if value1 and value2 else "empty"
    }
""",
            __env__=MockEnv({"value1": None, "value2": "test"}),
        )

        self.assertEqual(
            result,
            {
                "value1_is_none": True,
                "value2_is_none": False,
                "combined": "empty",
            },
        )


if __name__ == "__main__":
    unittest.main()
