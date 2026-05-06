import unittest

from astronverse.dataprocess import NoKeyOptionType
from astronverse.dataprocess.dict import DictProcess


class TestDictProcess(unittest.TestCase):
    """DictProcess유형의시도사용"""

    def setUp(self):
        """시도전의준비"""
        self.test_dict = {"name": "삼", "age": 25, "city": ""}
        self.test_dict_empty = {}
        self.test_dict_nested = {
            "user": {"name": "사", "age": 30},
            "settings": {"theme": "dark", "language": "zh"},
        }

    def test_create_new_dict_empty(self):
        """시도생성빈딕셔너리"""
        result = DictProcess.create_new_dict(dict_data={})
        self.assertEqual(result, {})

    def test_create_new_dict_with_data(self):
        """시도생성패키지데이터의딕셔너리"""
        test_data = {"key1": "value1", "key2": "value2"}
        result = DictProcess.create_new_dict(dict_data=test_data)
        self.assertEqual(result, test_data)

    def test_set_value_to_dict_new_key(self):
        """시도딕셔너리삽입새값"""
        test_dict = {"name": "삼", "age": 25}
        result = DictProcess.set_value_to_dict(dict_data=test_dict, dict_key="city", value="위")
        expected = {"name": "삼", "age": 25, "city": "위"}
        self.assertEqual(result, expected)

    def test_set_value_to_dict_existing_key(self):
        """시도업데이트딕셔너리중완료존재함의"""
        test_dict = {"name": "삼", "age": 25}
        result = DictProcess.set_value_to_dict(dict_data=test_dict, dict_key="age", value=30)
        expected = {"name": "삼", "age": 30}
        self.assertEqual(result, expected)

    def test_set_value_to_dict_empty_dict(self):
        """시도빈딕셔너리삽입값"""
        test_dict = {}
        result = DictProcess.set_value_to_dict(dict_data=test_dict, dict_key="first_key", value="first_value")
        expected = {"first_key": "first_value"}
        self.assertEqual(result, expected)

    def test_set_value_to_dict_complex_value(self):
        """시도삽입복사유형의값"""
        test_dict = {"simple": "value"}
        complex_value = {"nested": {"key": "value"}, "list": [1, 2, 3]}
        result = DictProcess.set_value_to_dict(dict_data=test_dict, dict_key="complex", value=complex_value)
        expected = {"simple": "value", "complex": complex_value}
        self.assertEqual(result, expected)

    def test_delete_value_from_dict_existing_key(self):
        """시도삭제딕셔너리중존재함의"""
        test_dict = {"name": "삼", "age": 25, "city": ""}
        result = DictProcess.delete_value_from_dict(dict_data=test_dict, dict_key="age")
        expected = {"name": "삼", "city": ""}
        self.assertEqual(result, expected)

    def test_delete_value_from_dict_nonexistent_key(self):
        """시도삭제딕셔너리중찾을 수 없습니다의"""
        test_dict = {"name": "삼", "age": 25}
        result = DictProcess.delete_value_from_dict(dict_data=test_dict, dict_key="nonexistent")
        # 삭제찾을 수 없습니다의해당아니오오류, 반환기존딕셔너리
        self.assertEqual(result, test_dict)

    def test_delete_value_from_dict_empty_dict(self):
        """시도에서빈딕셔너리삭제"""
        test_dict = {}
        result = DictProcess.delete_value_from_dict(dict_data=test_dict, dict_key="any_key")
        self.assertEqual(result, {})

    def test_get_value_from_dict_existing_key(self):
        """시도가져오기딕셔너리중존재함의의값"""
        test_dict = {"name": "삼", "age": 25, "city": ""}
        result = DictProcess.get_value_from_dict(dict_data=test_dict, dict_key="name")
        self.assertEqual(result, "삼")

    def test_get_value_from_dict_nonexistent_key_raise_error(self):
        """시도가져오기찾을 수 없습니다의시출력예외"""
        test_dict = {"name": "삼", "age": 25}
        with self.assertRaises(ValueError):
            DictProcess.get_value_from_dict(
                dict_data=test_dict,
                dict_key="nonexistent",
                fail_option=NoKeyOptionType.RAISE_ERROR,
            )

    def test_get_value_from_dict_nonexistent_key_return_default(self):
        """시도가져오기찾을 수 없습니다의시반환값"""
        test_dict = {"name": "삼", "age": 25}
        result = DictProcess.get_value_from_dict(
            dict_data=test_dict,
            dict_key="nonexistent",
            fail_option=NoKeyOptionType.RETURN_DEFAULT,
            default_value="값",
        )
        self.assertEqual(result, "값")

    def test_get_value_from_dict_nonexistent_key_return_default_empty_string(self):
        """시도가져오기찾을 수 없습니다의시반환빈문자열값"""
        test_dict = {"name": "삼", "age": 25}
        result = DictProcess.get_value_from_dict(
            dict_data=test_dict,
            dict_key="nonexistent",
            fail_option=NoKeyOptionType.RETURN_DEFAULT,
            default_value="",
        )
        self.assertEqual(result, "")

    def test_get_value_from_dict_nonexistent_key_return_default_none(self):
        """시도가져오기찾을 수 없습니다의시반환None값"""
        test_dict = {"name": "삼", "age": 25}
        result = DictProcess.get_value_from_dict(
            dict_data=test_dict,
            dict_key="nonexistent",
            fail_option=NoKeyOptionType.RETURN_DEFAULT,
            default_value=None,
        )
        self.assertEqual(result, "")

    def test_get_value_from_dict_complex_value(self):
        """시도가져오기복사유형의값"""
        complex_value = {"nested": {"key": "value"}, "list": [1, 2, 3]}
        test_dict = {"simple": "value", "complex": complex_value}
        result = DictProcess.get_value_from_dict(dict_data=test_dict, dict_key="complex")
        self.assertEqual(result, complex_value)

    def test_get_keys_from_dict(self):
        """시도가져오기딕셔너리의모든"""
        test_dict = {"name": "삼", "age": 25, "city": ""}
        result = DictProcess.get_keys_from_dict(dict_data=test_dict)
        expected = ["name", "age", "city"]
        # 딕셔너리의순서가능아니오지정, 조회의합치기여부
        self.assertEqual(set(result), set(expected))

    def test_get_keys_from_dict_empty(self):
        """시도가져오기빈딕셔너리의"""
        test_dict = {}
        result = DictProcess.get_keys_from_dict(dict_data=test_dict)
        self.assertEqual(result, [])

    def test_get_keys_from_dict_nested(self):
        """시도가져오기 딕셔너리의"""
        test_dict = {
            "user": {"name": "사", "age": 30},
            "settings": {"theme": "dark", "language": "zh"},
        }
        result = DictProcess.get_keys_from_dict(dict_data=test_dict)
        expected = ["user", "settings"]
        self.assertEqual(set(result), set(expected))

    def test_get_values_from_dict(self):
        """시도가져오기딕셔너리의모든값"""
        test_dict = {"name": "삼", "age": 25, "city": ""}
        result = DictProcess.get_values_from_dict(dict_data=test_dict)
        expected = ["삼", 25, ""]
        # 딕셔너리값의순서가능아니오지정, 조회값의합치기여부
        self.assertEqual(set(result), set(expected))

    def test_get_values_from_dict_empty(self):
        """시도가져오기빈딕셔너리의값"""
        test_dict = {}
        result = DictProcess.get_values_from_dict(dict_data=test_dict)
        self.assertEqual(result, [])

    def test_get_values_from_dict_nested(self):
        """시도가져오기 딕셔너리의값"""
        nested_dict1 = {"name": "사", "age": 30}
        nested_dict2 = {"theme": "dark", "language": "zh"}
        test_dict = {"user": nested_dict1, "settings": nested_dict2}
        result = DictProcess.get_values_from_dict(dict_data=test_dict)
        expected = [nested_dict1, nested_dict2]
        # 딕셔너리값의순서가능아니오지정, 조회값의합치기여부
        self.assertEqual(set(str(v) for v in result), set(str(v) for v in expected))

    def test_get_values_from_dict_mixed_types(self):
        """시도가져오기패키지아니오유형값의딕셔너리"""
        test_dict = {
            "string": "hello",
            "number": 42,
            "boolean": True,
            "list": [1, 2, 3],
        }
        result = DictProcess.get_values_from_dict(dict_data=test_dict)
        expected = ["hello", 42, True, [1, 2, 3]]
        # 딕셔너리값의순서가능아니오지정, 조회값의합치기여부
        self.assertEqual(set(str(v) for v in result), set(str(v) for v in expected))

    def test_dict_operations_chain(self):
        """시도딕셔너리의방식호출"""
        # 생성딕셔너리
        test_dict = {}

        # 삽입다중개값
        test_dict = DictProcess.set_value_to_dict(dict_data=test_dict, dict_key="name", value="오")
        test_dict = DictProcess.set_value_to_dict(dict_data=test_dict, dict_key="age", value=28)
        test_dict = DictProcess.set_value_to_dict(dict_data=test_dict, dict_key="city", value="")

        # 인증삽입결과
        self.assertEqual(test_dict, {"name": "오", "age": 28, "city": ""})

        # 가져오기값
        name = DictProcess.get_value_from_dict(dict_data=test_dict, dict_key="name")
        self.assertEqual(name, "오")

        # 삭제일개
        test_dict = DictProcess.delete_value_from_dict(dict_data=test_dict, dict_key="age")
        self.assertEqual(test_dict, {"name": "오", "city": ""})

        # 가져오기모든
        keys = DictProcess.get_keys_from_dict(dict_data=test_dict)
        self.assertEqual(set(keys), {"name", "city"})

        # 가져오기모든값
        values = DictProcess.get_values_from_dict(dict_data=test_dict)
        self.assertEqual(set(values), {"오", ""})


if __name__ == "__main__":
    unittest.main()