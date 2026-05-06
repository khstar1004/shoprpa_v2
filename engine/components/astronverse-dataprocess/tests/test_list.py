import unittest

from astronverse.dataprocess import DeleteMethodType, InsertMethodType, ListType, SortMethodType
from astronverse.dataprocess.list import ListProcess


class TestListProcess(unittest.TestCase):
    """ListProcess유형의시도사용"""

    def setUp(self):
        """시도전의준비"""
        self.test_list = [1, 2, 3, 4, 5]
        self.test_list_str = ["apple", "banana", "cherry"]
        self.test_list_mixed = [1, "hello", 3.14, True]

    def test_create_new_list_empty(self):
        """시도생성빈목록"""
        result = ListProcess.create_new_list(list_type=ListType.EMPTY)
        self.assertEqual(result, [])

    def test_create_new_list_same_data(self):
        """시도생성데이터의목록"""
        result = ListProcess.create_new_list(list_type=ListType.SAME_DATA, size=5, value="test")
        self.assertEqual(result, ["test", "test", "test", "test", "test"])

    def test_create_new_list_user_defined(self):
        """시도생성사용자지정목록"""
        custom_list = [1, 2, 3, 4, 5]
        result = ListProcess.create_new_list(list_type=ListType.USER_DEFINED, value=custom_list)
        self.assertEqual(result, custom_list)

    def test_create_new_list_with_string_format(self):
        """시도사용문자열형식생성목록"""
        result = ListProcess.create_new_list(list_type=ListType.USER_DEFINED, value="[1, 2, 3, 4, 5]")
        self.assertEqual(result, [1, 2, 3, 4, 5])

    def test_clear_list(self):
        """시도빈목록"""
        test_list = [1, 2, 3, 4, 5]
        result = ListProcess.clear_list(list_data=test_list)
        self.assertEqual(result, [])
        self.assertEqual(len(test_list), 0)

    def test_insert_value_to_list_append(self):
        """시도에서목록 추가 입력요소"""
        test_list = [1, 2, 3]
        result = ListProcess.insert_value_to_list(list_data=test_list, value=4, insert_method=InsertMethodType.APPEND)
        self.assertEqual(result, [1, 2, 3, 4])

    def test_insert_value_to_list_index(self):
        """시도에서위치 지정삽입요소"""
        test_list = [1, 2, 3]
        result = ListProcess.insert_value_to_list(
            list_data=test_list,
            value=4,
            insert_method=InsertMethodType.INDEX,
            index="1",
        )
        self.assertEqual(result, [1, 4, 2, 3])

    def test_change_value_in_list(self):
        """시도수정목록중의요소"""
        test_list = [1, 2, 3, 4, 5]
        result = ListProcess.change_value_in_list(list_data=test_list, index="2", new_value=10)
        self.assertEqual(result[2], 10)

    def test_get_list_position(self):
        """시도가져오기요소에서목록중의위치"""
        test_list = ["apple", "banana", "cherry"]
        result = ListProcess.get_list_position(list_data=test_list, value="banana")
        self.assertEqual(result, 1)

    def test_get_list_position_not_found(self):
        """시도가져오기찾을 수 없습니다의요소위치"""
        test_list = ["apple", "banana", "cherry"]
        with self.assertRaises(ValueError):
            ListProcess.get_list_position(list_data=test_list, value="orange")

    def test_remove_value_from_list_by_index(self):
        """시도통신경과검색삭제요소"""
        test_list = [1, 2, 3, 4, 5]
        result = ListProcess.remove_value_from_list(list_data=test_list, del_mode=DeleteMethodType.INDEX, del_pos="2")
        self.assertEqual(result, [1, 2, 4, 5])

    def test_remove_value_from_list_by_index_multiple(self):
        """시도통신경과다중개검색삭제요소"""
        test_list = [1, 2, 3, 4, 5]
        result = ListProcess.remove_value_from_list(
            list_data=test_list, del_mode=DeleteMethodType.INDEX, del_pos="0,2,4"
        )
        self.assertEqual(result, [2, 4])

    def test_remove_value_from_list_by_value(self):
        """시도통신경과값삭제요소"""
        test_list = ["apple", "banana", "cherry"]
        result = ListProcess.remove_value_from_list(
            list_data=test_list, del_mode=DeleteMethodType.VALUE, del_value="banana"
        )
        self.assertEqual(result, ["apple", "cherry"])

    def test_remove_value_from_list_by_value_not_found(self):
        """시도삭제찾을 수 없습니다의값"""
        test_list = ["apple", "banana", "cherry"]
        with self.assertRaises(ValueError):
            ListProcess.remove_value_from_list(list_data=test_list, del_mode=DeleteMethodType.VALUE, del_value="orange")

    def test_sort_list_asc(self):
        """시도목록상승순서정렬"""
        test_list = [3, 1, 4, 1, 5, 9, 2, 6]
        result = ListProcess.sort_list(list_data=test_list, sort_method=SortMethodType.ASC)
        self.assertEqual(result, [1, 1, 2, 3, 4, 5, 6, 9])

    def test_sort_list_desc(self):
        """시도목록 순서정렬"""
        test_list = [3, 1, 4, 1, 5, 9, 2, 6]
        result = ListProcess.sort_list(list_data=test_list, sort_method=SortMethodType.DESC)
        self.assertEqual(result, [9, 6, 5, 4, 3, 2, 1, 1])

    def test_sort_list_mixed_types_error(self):
        """시도합치기유형목록정렬오류"""
        test_list = [1, "hello", 3.14, True]
        with self.assertRaises(ValueError):
            ListProcess.sort_list(list_data=test_list, sort_method=SortMethodType.ASC)

    def test_random_shuffle_list(self):
        """시도목록 기기정렬"""
        test_list = [1, 2, 3, 4, 5]
        original_list = test_list.copy()
        result = ListProcess.random_shuffle_list(list_data=test_list)
        # 조회요소여부(순서가능아니오)
        self.assertEqual(sorted(result), sorted(original_list))
        # 조회여부의발송완료기기정렬(관리위가능, 소)
        self.assertIs(result, test_list)  # 해당반환일개목록객체

    def test_filter_elements_from_list(self):
        """시도목록필터링"""
        list1 = [1, 2, 3, 4, 5]
        list2 = [2, 4]
        result = ListProcess.filter_elements_from_list(list_data_1=list1, list_data_2=list2)
        self.assertEqual(result, [1, 3, 5])

    def test_reverse_list(self):
        """시도목록반대변환"""
        test_list = [1, 2, 3, 4, 5]
        result = ListProcess.reverse_list(list_data=test_list)
        self.assertEqual(result, [5, 4, 3, 2, 1])
        self.assertIs(result, test_list)  # 해당반환일개목록객체

    def test_merge_list(self):
        """시도목록병합"""
        list1 = [1, 2, 3]
        list2 = [4, 5, 6]
        result = ListProcess.merge_list(list_data_1=list1, list_data_2=list2)
        self.assertEqual(result, [1, 2, 3, 4, 5, 6])

    def test_get_unique_list(self):
        """시도목록 재"""
        test_list = [1, 2, 2, 3, 3, 3, 4, 5, 5]
        result = ListProcess.get_unique_list(list_data=test_list)
        # 비고: set재후순서가능아니오, 으로필요정렬
        self.assertEqual(sorted(result), [1, 2, 3, 4, 5])

    def test_get_common_elements_from_list(self):
        """시도가져오기 개목록의공유요소"""
        list1 = [1, 2, 3, 4, 5]
        list2 = [3, 4, 5, 6, 7]
        result = ListProcess.get_common_elements_from_list(list_data_1=list1, list_data_2=list2)
        # 비고: set후순서가능아니오, 으로필요정렬
        self.assertEqual(sorted(result), [3, 4, 5])

    def test_get_value_from_list(self):
        """시도가져오기목록중의요소"""
        test_list = ["apple", "banana", "cherry", "date"]
        result = ListProcess.get_value_from_list(list_data=test_list, index="2")
        self.assertEqual(result, "cherry")

    def test_get_value_from_list_negative_index(self):
        """시도사용데이터검색가져오기요소"""
        test_list = ["apple", "banana", "cherry", "date"]
        result = ListProcess.get_value_from_list(list_data=test_list, index="-1")
        self.assertEqual(result, "date")

    def test_get_length_of_list(self):
        """시도가져오기목록길이정도"""
        test_list = [1, 2, 3, 4, 5]
        result = ListProcess.get_length_of_list(list_data=test_list)
        self.assertEqual(result, 5)

    def test_get_length_of_empty_list(self):
        """시도가져오기빈목록길이정도"""
        test_list = []
        result = ListProcess.get_length_of_list(list_data=test_list)
        self.assertEqual(result, 0)

    def test_list_legal_check_empty_list_error(self):
        """시도빈목록조회오류"""
        test_list = []
        with self.assertRaises(ValueError):
            ListProcess.change_value_in_list(list_data=test_list, index="0", new_value=10)

    def test_list_legal_check_index_out_of_range(self):
        """시도검색초과출력오류"""
        test_list = [1, 2, 3]
        with self.assertRaises(ValueError):
            ListProcess.change_value_in_list(list_data=test_list, index="10", new_value=10)

    def test_list_legal_check_invalid_index_format(self):
        """시도없음검색형식오류"""
        test_list = [1, 2, 3]
        with self.assertRaises(ValueError):
            ListProcess.change_value_in_list(list_data=test_list, index="abc", new_value=10)


if __name__ == "__main__":
    unittest.main()