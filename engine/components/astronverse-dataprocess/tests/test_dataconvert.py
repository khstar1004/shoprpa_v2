import unittest

from astronverse.dataprocess import *
from astronverse.dataprocess.dataconvert import DataConvertProcess


class TestDataConvertProcess(unittest.TestCase):
    """데이터변환관리모듈시도유형"""

    def setUp(self):
        """시도전의준비"""
        self.test_dict = {"name": "삼", "age": 25, "city": ""}
        self.test_list = [1, 2, 3, "시도"]
        self.test_str_json = '{"name": "사", "age": 30, "city": "위"}'
        self.test_str_list = "[1, 2, 3, '시도']"
        self.test_str_dict = "{'name': '오', 'age': 35, 'city': ''}"
        self.test_str_tuple = "(1, 2, 3, '시도')"

    def test_json_convertor_json_to_str(self):
        """시도JSON변환문자열"""
        result = DataConvertProcess.json_convertor(input_data=self.test_dict, convert_type=JSONConvertType.JSON_TO_STR)
        expected = '{"name": "삼", "age": 25, "city": ""}'
        self.assertEqual(result, expected)

    def test_json_convertor_str_to_json(self):
        """시도문자열변환JSON"""
        result = DataConvertProcess.json_convertor(
            input_data=self.test_str_json, convert_type=JSONConvertType.STR_TO_JSON
        )
        expected = {"name": "사", "age": 30, "city": "위"}
        self.assertEqual(result, expected)

    def test_json_convertor_default_type(self):
        """시도JSON변환유형(JSON_TO_STR)"""
        result = DataConvertProcess.json_convertor(input_data=self.test_list)
        expected = '[1, 2, 3, "시도"]'
        self.assertEqual(result, expected)

    def test_json_convertor_with_list(self):
        """시도목록변환JSON문자열"""
        result = DataConvertProcess.json_convertor(input_data=self.test_list, convert_type=JSONConvertType.JSON_TO_STR)
        expected = '[1, 2, 3, "시도"]'
        self.assertEqual(result, expected)

    def test_other_to_str_with_dict(self):
        """시도딕셔너리변환문자열"""
        result = DataConvertProcess.other_to_str(input_data=self.test_dict)
        expected = "{'name': '삼', 'age': 25, 'city': ''}"
        self.assertEqual(result, expected)

    def test_other_to_str_with_list(self):
        """시도목록변환문자열"""
        result = DataConvertProcess.other_to_str(input_data=self.test_list)
        expected = "[1, 2, 3, '시도']"
        self.assertEqual(result, expected)

    def test_other_to_str_with_int(self):
        """시도정수변환문자열"""
        result = DataConvertProcess.other_to_str(input_data=123)
        self.assertEqual(result, "123")

    def test_other_to_str_with_float(self):
        """시도데이터변환문자열"""
        result = DataConvertProcess.other_to_str(input_data=123.45)
        self.assertEqual(result, "123.45")

    def test_other_to_str_with_bool(self):
        """시도불리언변환문자열"""
        result = DataConvertProcess.other_to_str(input_data=True)
        self.assertEqual(result, "True")

    def test_str_to_other_str_to_int(self):
        """시도문자열변환정수"""
        result = DataConvertProcess.str_to_other(input_data="123", convert_type=StringConvertType.STR_TO_INT)
        self.assertEqual(result, 123)

    def test_str_to_other_str_to_int_with_float_string(self):
        """시도소데이터의문자열변환정수(가져오기정수모듈분)"""
        result = DataConvertProcess.str_to_other(input_data="123.45", convert_type=StringConvertType.STR_TO_INT)
        self.assertEqual(result, 123)

    def test_str_to_other_str_to_float(self):
        """시도문자열변환데이터"""
        result = DataConvertProcess.str_to_other(input_data="123.45", convert_type=StringConvertType.STR_TO_FLOAT)
        self.assertEqual(result, 123.45)

    def test_str_to_other_str_to_bool_true_1(self):
        """시도문자열'1'변환불리언"""
        result = DataConvertProcess.str_to_other(input_data="1", convert_type=StringConvertType.STR_TO_BOOL)
        self.assertEqual(result, True)

    def test_str_to_other_str_to_bool_true_string(self):
        """시도문자열'True'변환불리언"""
        result = DataConvertProcess.str_to_other(input_data="True", convert_type=StringConvertType.STR_TO_BOOL)
        self.assertEqual(result, True)

    def test_str_to_other_str_to_bool_false_0(self):
        """시도문자열'0'변환불리언"""
        result = DataConvertProcess.str_to_other(input_data="0", convert_type=StringConvertType.STR_TO_BOOL)
        self.assertEqual(result, False)

    def test_str_to_other_str_to_bool_false_string(self):
        """시도문자열'False'변환불리언"""
        result = DataConvertProcess.str_to_other(input_data="False", convert_type=StringConvertType.STR_TO_BOOL)
        self.assertEqual(result, False)

    def test_str_to_other_str_to_bool_other_string(self):
        """시도문자열변환불리언"""
        result = DataConvertProcess.str_to_other(input_data="hello", convert_type=StringConvertType.STR_TO_BOOL)
        self.assertEqual(result, True)  # 빈문자열변환로True

    def test_str_to_other_str_to_list(self):
        """시도문자열변환목록"""
        result = DataConvertProcess.str_to_other(
            input_data=self.test_str_list, convert_type=StringConvertType.STR_TO_LIST
        )
        expected = [1, 2, 3, "시도"]
        self.assertEqual(result, expected)

    def test_str_to_other_str_to_dict(self):
        """시도문자열변환딕셔너리"""
        result = DataConvertProcess.str_to_other(
            input_data=self.test_str_dict, convert_type=StringConvertType.STR_TO_DICT
        )
        expected = {"name": "오", "age": 35, "city": ""}
        self.assertEqual(result, expected)

    def test_str_to_other_str_to_tuple(self):
        """시도문자열변환원그룹"""
        result = DataConvertProcess.str_to_other(
            input_data=self.test_str_tuple, convert_type=StringConvertType.STR_TO_TUPLE
        )
        expected = (1, 2, 3, "시도")
        self.assertEqual(result, expected)

    def test_str_to_other_default_type(self):
        """시도문자열변환유형(STR_TO_INT)"""
        result = DataConvertProcess.str_to_other(input_data="123")
        self.assertEqual(result, 123)

    def test_str_to_other_invalid_int(self):
        """시도지원하지 않는 문자열변환정수(해당출력예외)"""
        with self.assertRaises(Exception):
            DataConvertProcess.str_to_other(input_data="abc", convert_type=StringConvertType.STR_TO_INT)

    def test_str_to_other_invalid_float(self):
        """시도지원하지 않는 문자열변환데이터(해당출력예외)"""
        with self.assertRaises(Exception):
            DataConvertProcess.str_to_other(input_data="abc", convert_type=StringConvertType.STR_TO_FLOAT)

    def test_str_to_other_invalid_list(self):
        """시도지원하지 않는 문자열변환목록(해당출력예외)"""
        with self.assertRaises(Exception):
            DataConvertProcess.str_to_other(input_data="invalid_list", convert_type=StringConvertType.STR_TO_LIST)

    def test_str_to_other_invalid_dict(self):
        """시도지원하지 않는 문자열변환딕셔너리(해당출력예외)"""
        with self.assertRaises(Exception):
            DataConvertProcess.str_to_other(input_data="invalid_dict", convert_type=StringConvertType.STR_TO_DICT)

    def test_str_to_other_invalid_tuple(self):
        """시도지원하지 않는 문자열변환원그룹(해당출력예외)"""
        with self.assertRaises(Exception):
            DataConvertProcess.str_to_other(input_data="invalid_tuple", convert_type=StringConvertType.STR_TO_TUPLE)

    def test_edge_cases_empty_string_to_int(self):
        """시도가장자리: 빈문자열변환정수"""
        with self.assertRaises(Exception):
            DataConvertProcess.str_to_other(input_data="", convert_type=StringConvertType.STR_TO_INT)

    def test_edge_cases_empty_string_to_float(self):
        """시도가장자리: 빈문자열변환데이터"""
        with self.assertRaises(Exception):
            DataConvertProcess.str_to_other(input_data="", convert_type=StringConvertType.STR_TO_FLOAT)

    def test_edge_cases_empty_string_to_bool(self):
        """시도가장자리: 빈문자열변환불리언"""
        result = DataConvertProcess.str_to_other(input_data="", convert_type=StringConvertType.STR_TO_BOOL)
        self.assertEqual(result, False)

    def test_edge_cases_complex_json(self):
        """시도복사JSON결과변환"""
        complex_data = {
            "users": [
                {"name": "삼", "age": 25, "skills": ["Python", "Java"]},
                {"name": "사", "age": 30, "skills": ["C++", "Go"]},
            ],
            "metadata": {"total": 2, "active": True, "tags": ["열기발송", "시도"]},
        }

        # JSON변환문자열
        json_str = DataConvertProcess.json_convertor(input_data=complex_data, convert_type=JSONConvertType.JSON_TO_STR)

        # 문자열변환JSON
        result = DataConvertProcess.json_convertor(input_data=json_str, convert_type=JSONConvertType.STR_TO_JSON)

        self.assertEqual(result, complex_data)

    def test_edge_cases_nested_structures(self):
        """시도결과변환"""
        nested_str = "{'outer': {'inner': [1, 2, {'deep': 'value'}]}}"

        result = DataConvertProcess.str_to_other(input_data=nested_str, convert_type=StringConvertType.STR_TO_DICT)

        expected = {"outer": {"inner": [1, 2, {"deep": "value"}]}}
        self.assertEqual(result, expected)


if __name__ == "__main__":
    unittest.main()