import unittest

from astronverse.dataprocess import *
from astronverse.dataprocess.string import StringProcess


class TestStringProcess(unittest.TestCase):
    """문자열관리모듈시도유형"""

    def setUp(self):
        """시도전의준비"""
        self.test_text = "Hello World 123 test@example.com 13812345678"
        self.test_email = "user@example.com"
        self.test_phone = "13812345678"
        self.test_url = "https://www.example.com/path?param=value"
        self.test_id = "110101199001011234"

    def test_extract_content_from_string_digit(self):
        """시도가져오기숫자내용"""
        result = StringProcess.extract_content_from_string(
            text=self.test_text, extract_type=ExtractType.DIGIT, first_flag=True
        )
        self.assertEqual(result, "123")

    def test_extract_content_from_string_email(self):
        """시도가져오기메일함주소"""
        result = StringProcess.extract_content_from_string(
            text=self.test_text, extract_type=ExtractType.EMAIL, first_flag=True
        )
        self.assertEqual(result, "test@example.com")

    def test_extract_content_from_string_phone_number(self):
        """시도가져오기휴대폰 번호"""
        result = StringProcess.extract_content_from_string(
            text=self.test_text, extract_type=ExtractType.PHONE_NUMBER, first_flag=True
        )
        self.assertEqual(result, "13812345678")

    def test_extract_content_from_string_url(self):
        """시도가져오기URL"""
        result = StringProcess.extract_content_from_string(
            text=f"Visit {self.test_url} for more info",
            extract_type=ExtractType.URL,
            first_flag=True,
        )
        self.assertEqual(result, self.test_url)

    def test_extract_content_from_string_id_number(self):
        """시도가져오기 인증코드"""
        result = StringProcess.extract_content_from_string(
            text=f"ID: {self.test_id}",
            extract_type=ExtractType.ID_NUMBER,
            first_flag=True,
        )
        self.assertEqual(result, self.test_id)

    def test_extract_content_from_string_regex(self):
        """시도사용정상이면테이블방식가져오기내용"""
        result = StringProcess.extract_content_from_string(
            text=self.test_text,
            extract_type=ExtractType.REGEX,
            regex_formula=r"\b\w{4}\b",
            first_flag=True,
        )
        self.assertEqual(result, "test")

    def test_extract_content_from_string_all_matches(self):
        """시도가져오기모든매칭"""
        result = StringProcess.extract_content_from_string(
            text="123 456 789", extract_type=ExtractType.DIGIT, first_flag=False
        )
        self.assertEqual(result, ["123", "456", "789"])

    def test_replace_content_in_string_string(self):
        """시도문자열"""
        result = StringProcess.replace_content_in_string(
            text="Hello World",
            replace_type=ReplaceType.STRING,
            replaced_string="World",
            new_value="Python",
            first_flag=True,
        )
        self.assertEqual(result, "Hello Python")

    def test_replace_content_in_string_regex(self):
        """시도정상이면테이블방식"""
        result = StringProcess.replace_content_in_string(
            text="Hello123World456",
            replace_type=ReplaceType.REGEX,
            regex_formula=r"\d+",
            new_value="",
            first_flag=True,
        )
        self.assertEqual(result, "HelloWorld456")

    def test_replace_content_in_string_digit(self):
        """시도숫자"""
        result = StringProcess.replace_content_in_string(
            text="Price: 100, Quantity: 50",
            replace_type=ReplaceType.DIGIT,
            new_value="XXX",
            first_flag=True,
        )
        self.assertEqual(result, "Price: XXX, Quantity: 50")

    def test_replace_content_in_string_email(self):
        """시도메일함"""
        result = StringProcess.replace_content_in_string(
            text=f"Contact: {self.test_email}",
            replace_type=ReplaceType.EMAIL,
            new_value="[EMAIL]",
            first_flag=True,
        )
        self.assertEqual(result, "Contact: [EMAIL]")

    def test_replace_content_in_string_phone_number(self):
        """시도휴대폰 번호"""
        result = StringProcess.replace_content_in_string(
            text=f"Phone: {self.test_phone}",
            replace_type=ReplaceType.PHONE_NUMBER,
            new_value="[PHONE]",
            first_flag=True,
        )
        self.assertEqual(result, "Phone: [PHONE]")

    def test_replace_content_in_string_ignore_case(self):
        """시도크기"""
        result = StringProcess.replace_content_in_string(
            text="Hello HELLO hello",
            replace_type=ReplaceType.STRING,
            replaced_string="hello",
            new_value="Hi",
            first_flag=False,
            ignore_case_flag=True,
        )
        self.assertEqual(result, "Hi Hi Hi")

    def test_merge_list_to_string(self):
        """시도목록병합로문자열"""
        result = StringProcess.merge_list_to_string(list_data=["Hello", "World", "Python"], separator=" ")
        self.assertEqual(result, "Hello World Python")

    def test_merge_list_to_string_empty_separator(self):
        """시도빈분기호병합"""
        result = StringProcess.merge_list_to_string(list_data=["Hello", "World"], separator="")
        self.assertEqual(result, "HelloWorld")

    def test_merge_list_to_string_with_numbers(self):
        """시도숫자목록병합"""
        result = StringProcess.merge_list_to_string(list_data=[1, 2, 3, 4, 5], separator="-")
        self.assertEqual(result, "1-2-3-4-5")

    def test_split_string_to_list(self):
        """시도문자열분로목록"""
        result = StringProcess.split_string_to_list(string_data="Hello,World,Python", separator=",")
        self.assertEqual(result, ["Hello", "World", "Python"])

    def test_split_string_to_list_space_separator(self):
        """시도공백분기호분"""
        result = StringProcess.split_string_to_list(string_data="Hello World Python", separator=" ")
        self.assertEqual(result, ["Hello", "World", "Python"])

    def test_split_string_to_list_empty_separator(self):
        """시도빈분기호분"""
        result = StringProcess.split_string_to_list(string_data="Hello", separator="")
        self.assertEqual(result, ["H", "e", "l", "l", "o"])

    def test_concatenate_string_none(self):
        """시도없음분기호문자열연결"""
        result = StringProcess.concatenate_string(
            string_data_1="Hello",
            string_data_2="World",
            concat_type=ConcatStringType.NONE,
        )
        self.assertEqual(result, "HelloWorld")

    def test_concatenate_string_space(self):
        """시도공백분기호연결"""
        result = StringProcess.concatenate_string(
            string_data_1="Hello",
            string_data_2="World",
            concat_type=ConcatStringType.SPACE,
        )
        self.assertEqual(result, "Hello World")

    def test_concatenate_string_hyphen(self):
        """시도문자분기호연결"""
        result = StringProcess.concatenate_string(
            string_data_1="Hello",
            string_data_2="World",
            concat_type=ConcatStringType.HYPHEN,
        )
        self.assertEqual(result, "Hello-World")

    def test_concatenate_string_underline(self):
        """시도아래계획분기호연결"""
        result = StringProcess.concatenate_string(
            string_data_1="Hello",
            string_data_2="World",
            concat_type=ConcatStringType.UNDERLINE,
        )
        self.assertEqual(result, "Hello_World")

    def test_concatenate_string_linebreak(self):
        """시도행기호분기호연결"""
        result = StringProcess.concatenate_string(
            string_data_1="Hello",
            string_data_2="World",
            concat_type=ConcatStringType.LINEBREAK,
        )
        self.assertEqual(result, "Hello\nWorld")

    def test_concatenate_string_other(self):
        """시도지정분기호연결"""
        result = StringProcess.concatenate_string(
            string_data_1="Hello",
            string_data_2="World",
            concat_type=ConcatStringType.OTHER,
            separator="***",
        )
        self.assertEqual(result, "Hello***World")

    def test_fill_string_to_length_right(self):
        """시도오른쪽단말문자열"""
        result = StringProcess.fill_string_to_length(
            string_data="Hello",
            add_str="*",
            total_length="10",
            fill_type=FillStringType.RIGHT,
        )
        self.assertEqual(result, "Hello*****")

    def test_fill_string_to_length_left(self):
        """시도왼쪽단말문자열"""
        result = StringProcess.fill_string_to_length(
            string_data="Hello",
            add_str="*",
            total_length="10",
            fill_type=FillStringType.LEFT,
        )
        self.assertEqual(result, "*****Hello")

    def test_fill_string_to_length_exact_length(self):
        """시도문자열길이정도정상대기목록 길이정도"""
        result = StringProcess.fill_string_to_length(
            string_data="Hello",
            add_str="*",
            total_length="5",
            fill_type=FillStringType.RIGHT,
        )
        self.assertEqual(result, "Hello")

    def test_fill_string_to_length_short_length(self):
        """시도문자열길이정도대목록 길이정도"""
        result = StringProcess.fill_string_to_length(
            string_data="Hello",
            add_str="*",
            total_length="3",
            fill_type=FillStringType.RIGHT,
        )
        self.assertEqual(result, "Hello")

    def test_fill_string_to_length_invalid_input(self):
        """시도없음입력"""
        with self.assertRaises(ValueError):
            StringProcess.fill_string_to_length(string_data="", add_str="*", total_length="10")

        with self.assertRaises(ValueError):
            StringProcess.fill_string_to_length(string_data="Hello", add_str="", total_length="10")

        with self.assertRaises(ValueError):
            StringProcess.fill_string_to_length(string_data="Hello", add_str="*", total_length="-1")

    def test_strip_string_both(self):
        """시도단말제거공백"""
        result = StringProcess.strip_string(string_data="  Hello World  ", strip_method=StripStringType.BOTH)
        self.assertEqual(result, "Hello World")

    def test_strip_string_left(self):
        """시도왼쪽단말제거공백"""
        result = StringProcess.strip_string(string_data="  Hello World  ", strip_method=StripStringType.LEFT)
        self.assertEqual(result, "Hello World  ")

    def test_strip_string_right(self):
        """시도오른쪽단말제거공백"""
        result = StringProcess.strip_string(string_data="  Hello World  ", strip_method=StripStringType.RIGHT)
        self.assertEqual(result, "  Hello World")

    def test_strip_string_empty(self):
        """시도빈문자열"""
        result = StringProcess.strip_string(string_data="", strip_method=StripStringType.BOTH)
        self.assertEqual(result, "")

    def test_cut_string_to_length_first(self):
        """시도에서일개문자열기 가져오기"""
        result = StringProcess.cut_string_to_length(string_data="Hello World", length=5, cut_type=CutStringType.FIRST)
        self.assertEqual(result, "Hello")

    def test_cut_string_to_length_index(self):
        """시도에서위치 지정열기 가져오기"""
        result = StringProcess.cut_string_to_length(
            string_data="Hello World", length=5, cut_type=CutStringType.INDEX, index=6
        )
        self.assertEqual(result, "World")

    def test_cut_string_to_length_string(self):
        """시도에서지정문자열열기 가져오기"""
        result = StringProcess.cut_string_to_length(
            string_data="Hello World Python",
            length=5,
            cut_type=CutStringType.STRING,
            find_str="World",
        )
        self.assertEqual(result, "World")

    def test_cut_string_to_length_invalid_input(self):
        """시도없음입력"""
        with self.assertRaises(ValueError):
            StringProcess.cut_string_to_length(string_data="", length=5)

        with self.assertRaises(ValueError):
            StringProcess.cut_string_to_length(string_data="Hello", length=-1)

        with self.assertRaises(ValueError):
            StringProcess.cut_string_to_length(
                string_data="Hello World",
                length=5,
                cut_type=CutStringType.STRING,
                find_str="NotFound",
            )

    def test_change_case_of_string_upper(self):
        """시도변환로대"""
        result = StringProcess.change_case_of_string(string_data="Hello World", case_type=CaseChangeType.UPPER)
        self.assertEqual(result, "HELLO WORLD")

    def test_change_case_of_string_lower(self):
        """시도변환로소"""
        result = StringProcess.change_case_of_string(string_data="Hello World", case_type=CaseChangeType.LOWER)
        self.assertEqual(result, "hello world")

    def test_change_case_of_string_caps(self):
        """시도문자대"""
        result = StringProcess.change_case_of_string(string_data="hello world", case_type=CaseChangeType.CAPS)
        self.assertEqual(result, "Hello world")

    def test_change_case_of_string_empty(self):
        """시도빈문자열"""
        result = StringProcess.change_case_of_string(string_data="", case_type=CaseChangeType.UPPER)
        self.assertEqual(result, "")

    def test_get_string_length(self):
        """시도가져오기문자열길이정도"""
        result = StringProcess.get_string_length(string_data="Hello World")
        self.assertEqual(result, 11)

    def test_get_string_length_empty(self):
        """시도빈문자열길이정도"""
        result = StringProcess.get_string_length(string_data="")
        self.assertEqual(result, 0)

    def test_invalid_regex_error(self):
        """시도없음정상이면테이블방식오류"""
        with self.assertRaises(Exception):
            StringProcess.extract_content_from_string(
                text="Hello World",
                extract_type=ExtractType.REGEX,
                regex_formula="[invalid",
                first_flag=True,
            )

        with self.assertRaises(Exception):
            StringProcess.replace_content_in_string(
                text="Hello World",
                replace_type=ReplaceType.REGEX,
                regex_formula="[invalid",
                new_value="replacement",
                first_flag=True,
            )


if __name__ == "__main__":
    unittest.main()