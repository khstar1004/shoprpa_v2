import unittest

from astronverse.dataprocess import *
from astronverse.dataprocess.error import *
from astronverse.dataprocess.math import MathProcess


class TestMathProcess(unittest.TestCase):
    """데이터관리모듈시도유형"""

    def setUp(self):
        """시도전의준비"""
        self.test_number = 10
        self.test_float = 10.567
        self.test_negative = -15
        self.test_string_number = "25"
        self.test_string_float = "25.5"

    def test_generate_random_number_integer_single(self):
        """시도완료단일개정수기기데이터"""
        result = MathProcess.generate_random_number(number_type=NumberType.INTEGER, size=1, start=1, end=101)
        self.assertIsInstance(result, int)
        self.assertGreaterEqual(result, 1)
        self.assertLess(result, 101)

    def test_generate_random_number_integer_multiple(self):
        """시도완료다중개정수기기데이터"""
        result = MathProcess.generate_random_number(number_type=NumberType.INTEGER, size=5, start=1, end=101)
        self.assertIsInstance(result, list)
        self.assertEqual(len(result), 5)
        for num in result:
            self.assertIsInstance(num, int)
            self.assertGreaterEqual(num, 1)
            self.assertLess(num, 101)

    def test_generate_random_number_float_single(self):
        """시도완료단일개데이터기기데이터"""
        result = MathProcess.generate_random_number(number_type=NumberType.FLOAT, size=1, start=1.0, end=10.0)
        self.assertIsInstance(result, float)
        self.assertGreaterEqual(result, 1.0)
        self.assertLess(result, 10.0)

    def test_generate_random_number_float_multiple(self):
        """시도완료다중개데이터기기데이터"""
        result = MathProcess.generate_random_number(number_type=NumberType.FLOAT, size=3, start=1.0, end=10.0)
        self.assertIsInstance(result, list)
        self.assertEqual(len(result), 3)
        for num in result:
            self.assertIsInstance(num, float)
            self.assertGreaterEqual(num, 1.0)
            self.assertLess(num, 10.0)

    def test_generate_random_number_invalid_range(self):
        """시도없음매개변수"""
        with self.assertRaises(BaseException):
            MathProcess.generate_random_number(number_type=NumberType.INTEGER, size=1, start=100, end=50)

    def test_get_rounding_number_integer(self):
        """시도정수사오입력"""
        result = MathProcess.get_rounding_number(number=10, precision=0)
        self.assertEqual(result, 10)

    def test_get_rounding_number_float_round_up(self):
        """시도데이터사오입력위"""
        result = MathProcess.get_rounding_number(number=10.567, precision=2)
        self.assertEqual(result, 10.57)

    def test_get_rounding_number_float_round_down(self):
        """시도데이터사오입력아래"""
        result = MathProcess.get_rounding_number(number=10.564, precision=2)
        self.assertEqual(result, 10.56)

    def test_get_rounding_number_negative_precision(self):
        """시도데이터정도사오입력"""
        result = MathProcess.get_rounding_number(number=1234.567, precision=-2)
        self.assertEqual(result, 1200)

    def test_get_rounding_number_zero_precision(self):
        """시도영정도사오입력"""
        result = MathProcess.get_rounding_number(number=10.567, precision=0)
        self.assertEqual(result, 11)

    def test_self_calculation_number_add(self):
        """시도증가"""
        result = MathProcess.self_calculation_number(number=self.test_number, add_sub=AddSubType.ADD, add_sub_number=5)
        self.assertEqual(result, 15)

    def test_self_calculation_number_sub(self):
        """시도"""
        result = MathProcess.self_calculation_number(number=self.test_number, add_sub=AddSubType.SUB, add_sub_number=3)
        self.assertEqual(result, 7)

    def test_self_calculation_number_add_default(self):
        """시도증가매개변수"""
        result = MathProcess.self_calculation_number(number=self.test_number, add_sub=AddSubType.ADD)
        self.assertEqual(result, 11)

    def test_self_calculation_number_sub_default(self):
        """시도매개변수"""
        result = MathProcess.self_calculation_number(number=self.test_number, add_sub=AddSubType.SUB)
        self.assertEqual(result, 9)

    def test_get_absolute_number_positive_int(self):
        """시도정상정수의값"""
        result = MathProcess.get_absolute_number(raw_number=25)
        self.assertEqual(result, 25)

    def test_get_absolute_number_negative_int(self):
        """시도정수의값"""
        result = MathProcess.get_absolute_number(raw_number=self.test_negative)
        self.assertEqual(result, 15)

    def test_get_absolute_number_positive_float(self):
        """시도정상데이터의값"""
        result = MathProcess.get_absolute_number(raw_number=25.5)
        self.assertEqual(result, 25.5)

    def test_get_absolute_number_negative_float(self):
        """시도데이터의값"""
        result = MathProcess.get_absolute_number(raw_number=-25.5)
        self.assertEqual(result, 25.5)

    def test_get_absolute_number_string_int(self):
        """시도문자열정수의값"""
        result = MathProcess.get_absolute_number(raw_number=self.test_string_number)
        self.assertEqual(result, 25)

    def test_get_absolute_number_string_float(self):
        """시도문자열데이터의값"""
        result = MathProcess.get_absolute_number(raw_number=self.test_string_float)
        self.assertEqual(result, 25.5)

    def test_get_absolute_number_string_negative(self):
        """시도문자열데이터의값"""
        result = MathProcess.get_absolute_number(raw_number="-25")
        self.assertEqual(result, 25)

    def test_get_absolute_number_invalid_string(self):
        """시도없음문자열형식"""
        with self.assertRaises(BaseException):
            MathProcess.get_absolute_number(raw_number="invalid")

    def test_calculate_expression_add(self):
        """시도추가법테이블방식계획"""
        result = MathProcess.calculate_expression(left="10", operator=MathOperatorType.ADD, right="5")
        self.assertEqual(result, 15)

    def test_calculate_expression_subtract(self):
        """시도법테이블방식계획"""
        result = MathProcess.calculate_expression(left="20", operator=MathOperatorType.SUB, right="8")
        self.assertEqual(result, 12)

    def test_calculate_expression_multiply(self):
        """시도법테이블방식계획"""
        result = MathProcess.calculate_expression(left="6", operator=MathOperatorType.MUL, right="7")
        self.assertEqual(result, 42)

    def test_calculate_expression_divide(self):
        """시도제거법테이블방식계획"""
        result = MathProcess.calculate_expression(left="15", operator=MathOperatorType.DIV, right="3")
        self.assertEqual(result, 5.0)

    def test_calculate_expression_with_round(self):
        """시도사오입력의테이블방식계획"""
        result = MathProcess.calculate_expression(
            left="10",
            operator=MathOperatorType.DIV,
            right="3",
            handle_method=MathRoundType.ROUND,
            precision=2,
        )
        self.assertIsInstance(result, float)
        self.assertAlmostEqual(result, 3.33, places=2)

    def test_calculate_expression_with_floor(self):
        """시도아래가져오기 의테이블방식계획"""
        result = MathProcess.calculate_expression(
            left="10",
            operator=MathOperatorType.DIV,
            right="3",
            handle_method=MathRoundType.FLOOR,
        )
        self.assertEqual(result, 3)

    def test_calculate_expression_with_ceil(self):
        """시도위가져오기 의테이블방식계획"""
        result = MathProcess.calculate_expression(
            left="10",
            operator=MathOperatorType.DIV,
            right="3",
            handle_method=MathRoundType.CEIL,
        )
        self.assertEqual(result, 4)

    def test_calculate_expression_with_none(self):
        """시도아니오관리의테이블방식계획"""
        result = MathProcess.calculate_expression(
            left="10",
            operator=MathOperatorType.DIV,
            right="3",
            handle_method=MathRoundType.NONE,
        )
        self.assertEqual(result, 10 / 3)

    def test_calculate_expression_invalid_expression(self):
        """시도없음테이블방식"""
        with self.assertRaises(BaseException):
            MathProcess.calculate_expression(left="10", operator=MathOperatorType.DIV, right="0")

    def test_calculate_expression_invalid_syntax(self):
        """시도문법 오류의테이블방식"""
        with self.assertRaises(BaseException):
            MathProcess.calculate_expression(left="invalid", operator=MathOperatorType.ADD, right="5")

    def test_calculate_expression_float_numbers(self):
        """시도데이터테이블방식계획"""
        result = MathProcess.calculate_expression(left="10.5", operator=MathOperatorType.ADD, right="5.3")
        self.assertEqual(result, 15.8)

    def test_calculate_expression_mixed_types(self):
        """시도합치기유형테이블방식계획"""
        result = MathProcess.calculate_expression(left="10", operator=MathOperatorType.MUL, right="2.5")
        self.assertEqual(result, 25.0)

    def test_calculate_expression_default_parameters(self):
        """시도매개변수테이블방식계획"""
        result = MathProcess.calculate_expression()
        self.assertEqual(result, 0)  # 빈문자열추가결과로0

    def test_calculate_expression_complex_expression(self):
        """시도복사테이블방식계획"""
        result = MathProcess.calculate_expression(
            left="100",
            operator=MathOperatorType.DIV,
            right="7",
            handle_method=MathRoundType.ROUND,
            precision=3,
        )
        self.assertIsInstance(result, float)
        self.assertAlmostEqual(result, 14.286, places=3)


if __name__ == "__main__":
    unittest.main()