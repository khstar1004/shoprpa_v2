import json
import unittest
from unittest.mock import Mock, patch

from astronverse.dataprocess import *
from astronverse.dataprocess.data import DataProcess


class TestDataProcess(unittest.TestCase):
    """데이터 처리기기시도유형"""

    def setUp(self):
        """시도전의준비"""
        self.test_int_value = 123
        self.test_float_value = 123.45
        self.test_str_value = "test string"
        self.test_bool_value = True
        self.test_list_value = [1, 2, 3]
        self.test_dict_value = {"key": "value"}
        self.test_tuple_value = (1, 2, 3)
        self.test_json_value = '{"name": "test", "age": 25}'

    def test_set_variable_value_int(self):
        """시도정수유형변수"""
        result = DataProcess.set_variable_value(value=self.test_int_value, variable_type=VariableType.INT)
        self.assertEqual(result, 123)
        self.assertIsInstance(result, int)

    def test_set_variable_value_float(self):
        """시도데이터유형변수"""
        result = DataProcess.set_variable_value(value=self.test_float_value, variable_type=VariableType.FLOAT)
        self.assertEqual(result, 123.45)
        self.assertIsInstance(result, float)

    def test_set_variable_value_str(self):
        """시도문자열유형변수"""
        result = DataProcess.set_variable_value(value=self.test_str_value, variable_type=VariableType.STR)
        self.assertEqual(result, "test string")
        self.assertIsInstance(result, str)

    def test_set_variable_value_bool_true(self):
        """시도유형변수 - True"""
        result = DataProcess.set_variable_value(value="True", variable_type=VariableType.BOOL)
        self.assertEqual(result, True)
        self.assertIsInstance(result, bool)

    def test_set_variable_value_bool_false(self):
        """시도유형변수 - False"""
        result = DataProcess.set_variable_value(value="False", variable_type=VariableType.BOOL)
        self.assertEqual(result, False)
        self.assertIsInstance(result, bool)

    def test_set_variable_value_bool_numeric_true(self):
        """시도유형변수 - 숫자1"""
        result = DataProcess.set_variable_value(value="1", variable_type=VariableType.BOOL)
        self.assertEqual(result, True)
        self.assertIsInstance(result, bool)

    def test_set_variable_value_bool_numeric_false(self):
        """시도유형변수 - 숫자0"""
        result = DataProcess.set_variable_value(value="0", variable_type=VariableType.BOOL)
        self.assertEqual(result, False)
        self.assertIsInstance(result, bool)

    def test_set_variable_value_list(self):
        """시도목록유형변수"""
        list_str = str(self.test_list_value)
        result = DataProcess.set_variable_value(value=list_str, variable_type=VariableType.LIST)
        self.assertEqual(result, [1, 2, 3])
        self.assertIsInstance(result, list)

    def test_set_variable_value_dict(self):
        """시도딕셔너리유형변수"""
        dict_str = str(self.test_dict_value)
        result = DataProcess.set_variable_value(value=dict_str, variable_type=VariableType.DICT)
        self.assertEqual(result, {"key": "value"})
        self.assertIsInstance(result, dict)

    def test_set_variable_value_tuple(self):
        """시도원그룹유형변수"""
        tuple_str = str(self.test_tuple_value)
        result = DataProcess.set_variable_value(value=tuple_str, variable_type=VariableType.TUPLE)
        self.assertEqual(result, (1, 2, 3))
        self.assertIsInstance(result, tuple)

    def test_set_variable_value_json(self):
        """시도JSON유형변수"""
        result = DataProcess.set_variable_value(value=self.test_json_value, variable_type=VariableType.JSON)
        expected = json.loads(self.test_json_value)
        self.assertEqual(result, expected)
        self.assertIsInstance(result, dict)

    def test_set_variable_value_other(self):
        """시도유형변수"""
        result = DataProcess.set_variable_value(value=self.test_str_value, variable_type=VariableType.OTHER)
        self.assertEqual(result, self.test_str_value)

    def test_set_variable_value_invalid_int(self):
        """시도지원하지 않는 정수변환"""
        with self.assertRaises(Exception):
            DataProcess.set_variable_value(value="invalid_int", variable_type=VariableType.INT)

    def test_set_variable_value_invalid_float(self):
        """시도지원하지 않는 데이터변환"""
        with self.assertRaises(Exception):
            DataProcess.set_variable_value(value="invalid_float", variable_type=VariableType.FLOAT)

    def test_set_variable_value_invalid_list(self):
        """시도지원하지 않는 목록변환"""
        with self.assertRaises(Exception):
            DataProcess.set_variable_value(value="invalid_list", variable_type=VariableType.LIST)

    def test_set_variable_value_invalid_json(self):
        """시도지원하지 않는 JSON변환"""
        with self.assertRaises(Exception):
            DataProcess.set_variable_value(value="invalid_json", variable_type=VariableType.JSON)

    def test_get_shared_variable_empty_list(self):
        """시도가져오기공유 변수 - 빈목록"""
        shared_variable = {"subVarList": []}
        result = DataProcess.get_shared_variable(shared_variable=shared_variable)
        self.assertIsNone(result)

    def test_get_shared_variable_no_sub_var_list(self):
        """시도가져오기공유 변수 - 없음subVarList"""
        shared_variable = {}
        result = DataProcess.get_shared_variable(shared_variable=shared_variable)
        self.assertIsNone(result)

    def test_get_shared_variable_normal_variables(self):
        """시도가져오기공유 변수 - 통신변수"""
        shared_variable = {
            "subVarList": [
                {
                    "varName": "test_var1",
                    "varValue": "test_value1",
                    "encrypt": False,
                    "key": None,
                },
                {
                    "varName": "test_var2",
                    "varValue": 123,
                    "encrypt": False,
                    "key": None,
                },
            ]
        }
        result = DataProcess.get_shared_variable(shared_variable=shared_variable)
        expected = {"test_var1": "test_value1", "test_var2": 123}
        self.assertEqual(result, expected)

    @patch("astronverse.dataprocess.data.Ciphertext")
    def test_get_shared_variable_encrypted_variables(self, mock_ciphertext):
        """시도가져오기공유 변수 - 암호화변수"""
        # Ciphertext유형
        mock_cipher = Mock()
        mock_ciphertext.return_value = mock_cipher

        shared_variable = {
            "subVarList": [
                {
                    "varName": "encrypted_var",
                    "varValue": "encrypted_value",
                    "encrypt": True,
                    "key": "test_key",
                }
            ]
        }
        result = DataProcess.get_shared_variable(shared_variable=shared_variable)

        # 인증Ciphertext정상생성및매칭
        mock_ciphertext.assert_called_once_with("encrypted_value")
        mock_cipher.set_key.assert_called_once_with("test_key")

        # 인증결과패키지Ciphertext객체
        self.assertIn("encrypted_var", result)
        self.assertEqual(result["encrypted_var"], mock_cipher)

    def test_get_shared_variable_mixed_variables(self):
        """시도가져오기공유 변수 - 합치기변수(통신+암호화)"""
        with patch("astronverse.dataprocess.data.Ciphertext") as mock_ciphertext:
            mock_cipher = Mock()
            mock_ciphertext.return_value = mock_cipher

            shared_variable = {
                "subVarList": [
                    {
                        "varName": "normal_var",
                        "varValue": "normal_value",
                        "encrypt": False,
                        "key": None,
                    },
                    {
                        "varName": "encrypted_var",
                        "varValue": "encrypted_value",
                        "encrypt": True,
                        "key": "test_key",
                    },
                ]
            }
            result = DataProcess.get_shared_variable(shared_variable=shared_variable)

            # 인증통신변수
            self.assertEqual(result["normal_var"], "normal_value")

            # 인증암호화변수
            self.assertEqual(result["encrypted_var"], mock_cipher)
            mock_cipher.set_key.assert_called_once_with("test_key")

    def test_set_variable_value_edge_cases(self):
        """시도가장자리"""
        # 시도빈문자열
        result = DataProcess.set_variable_value(value="", variable_type=VariableType.STR)
        self.assertEqual(result, "")

        # 시도영값
        result = DataProcess.set_variable_value(value=0, variable_type=VariableType.INT)
        self.assertEqual(result, 0)

        # 시도데이터
        result = DataProcess.set_variable_value(value=-123, variable_type=VariableType.INT)
        self.assertEqual(result, -123)

    def test_set_variable_value_boolean_edge_cases(self):
        """시도유형의가장자리"""
        # 시도True값
        true_values = ["True", "true", "1", True, 1]
        for value in true_values:
            result = DataProcess.set_variable_value(value=value, variable_type=VariableType.BOOL)
            self.assertEqual(result, True)

        # 시도False값
        false_values = ["False", "false", "0", False, 0]
        for value in false_values:
            result = DataProcess.set_variable_value(value=value, variable_type=VariableType.BOOL)
            self.assertEqual(result, False)

        # 시도값변환로
        result = DataProcess.set_variable_value(value="hello", variable_type=VariableType.BOOL)
        self.assertEqual(result, True)  # 빈문자열변환로True


if __name__ == "__main__":
    unittest.main()