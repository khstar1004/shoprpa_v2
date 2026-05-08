import re
from datetime import datetime

import pandas as pd
from astronverse.picker.logger import logger


def parse_datetime(date_string):
    chinese_to_number = {
        "일": "1",
        "일": "1",
        "이": "2",
        "이": "2",
        "삼": "3",
        "삼": "3",
        "사": "4",
        "사": "4",
        "오": "5",
        "오": "5",
        "육": "6",
        "육": "6",
        "칠": "7",
        "칠": "7",
        "팔": "8",
        "팔": "8",
        "구": "9",
        "구": "9",
        "영": "0",
        "십": "10",
        "십": "10",
        "십일": "11",
        "십이": "12",
        "십삼": "13",
        "십사": "14",
        "십오": "15",
        "십육": "16",
        "십칠": "17",
        "십팔": "18",
        "십구": "19",
        "이십": "20",
        "이십일": "21",
        "일": "21",
        "이십이": "22",
        "이": "22",
        "이십삼": "23",
        "삼": "23",
        "이십사": "24",
        "사": "24",
        "이십오": "25",
        "오": "25",
        "이십육": "26",
        "육": "26",
        "이십칠": "27",
        "칠": "27",
        "이십팔": "28",
        "팔": "28",
        "이십구": "29",
        "구": "29",
        "삼십": "30",
        "삼십일": "31",
    }

    # 중국어날짜형식의정상이면테이블방식
    chinese_date_formats = {
        r"^(\d{4})-(\d{1,2})-(\d{1,2})\s(\d{1,2}):(\d{1,2}):(\d{1,2})$": "%Y-%m-%d %H:%M:%S",
        r"^(\d{4})/(\d{1,2})/(\d{1,2})\s(\d{1,2}):(\d{1,2}):(\d{1,2})$": "%Y/%m/%d %H:%M:%S",
        r"^(\d{4})\.(\d{1,2})\.(\d{1,2})\s(\d{1,2}):(\d{1,2}):(\d{1,2})$": "%Y.%m.%d %H:%M:%S",
        r"^(\d{4})년(\d{1,2})월(\d{1,2})일\s(\d{1,2}):(\d{1,2}):(\d{1,2})$": "%Y년%m월%d일 %H:%M:%S",
        r"^(\d{4})-(\d{1,2})-(\d{1,2})T(\d{1,2}):(\d{1,2}):(\d{1,2})$": "%Y-%m-%dT%H:%M:%S",
        r"^(\d{4})년(\d{1,2})월(\d{1,2})일\s(\d{1,2})시(\d{1,2})분(\d{1,2})초$": "%Y년%m월%d일 %H시%M분%S초",
        r"^(\d{4})-(\d{1,2})-(\d{1,2})\s(\d{1,2}):(\d{1,2})$": "%Y-%m-%d %H:%M",
        r"^(\d{4})/(\d{1,2})/(\d{1,2})\s(\d{1,2}):(\d{1,2})$": "%Y/%m/%d %H:%M",
        r"^(\d{4})\.(\d{1,2})\.(\d{1,2})\s(\d{1,2}):(\d{1,2})$": "%Y.%m.%d %H:%M",
        r"^(\d{4})년(\d{1,2})월(\d{1,2})일\s(\d{1,2}):(\d{1,2})$": "%Y년%m월%d일 %H:%M",
        r"^(\d{4})년(\d{1,2})월(\d{1,2})일\s(\d{1,2})시(\d{1,2})분$": "%Y년%m월%d일 %H시%M분",
        r"^(\d{1,2})-(\d{1,2})\s(\d{1,2}):(\d{1,2}):(\d{1,2})$": "%m-%d %H:%M:%S",
        r"^(\d{1,2})/(\d{1,2})\s(\d{1,2}):(\d{1,2}):(\d{1,2})$": "%m/%d %H:%M:%S",
        r"^(\d{1,2})\.(\d{1,2})\s(\d{1,2}):(\d{1,2}):(\d{1,2})$": "%m.%d %H:%M:%S",
        r"^(\d{1,2})월(\d{1,2})일\s(\d{1,2}):(\d{1,2}):(\d{1,2})$": "%m월%d일 %H:%M:%S",
        r"^(\d{1,2})월(\d{1,2})일\s(\d{1,2}):(\d{1,2})$": "%m월%d일 %H:%M",
        r"^(\d{1,2})월(\d{1,2})일\s(\d{1,2})시(\d{1,2})분(\d{1,2})초$": "%m월%d일 %H시%M분%S초",
        r"^(\d{4})-(\d{1,2})-(\d{1,2})$": "%Y-%m-%d",
        r"^(\d{4})/(\d{1,2})/(\d{1,2})$": "%Y/%m/%d",
        r"^(\d{4})\.(\d{1,2})\.(\d{1,2})$": "%Y.%m.%d",
        r"^(\d{4})년(\d{1,2})월(\d{1,2})일$": "%Y년%m월%d일",
        r"^(\d{4})-(\d{1,2})$": "%Y-%m",
        r"^(\d{4})/(\d{1,2})$": "%Y/%m",
        r"^(\d{4})\.(\d{1,2})$": "%Y.%m",
        r"^(\d{4})년(\d{1,2})월$": "%Y년%m월",
        r"^(\d{1,2})-(\d{1,2})$": "%m-%d",
        r"^(\d{1,2})/(\d{1,2})$": "%m/%d",
        r"^(\d{1,2})\.(\d{1,2})$": "%m.%d",
        r"^(\d{1,2})월(\d{1,2})일$": "%m월%d일",
        r"^(\d{1,2}):(\d{1,2}):(\d{1,2})$": "%H:%M:%S",
        r"^(\d{1,2})시(\d{1,2})분(\d{1,2})초$": "%H시%M분%S초",
        r"^(\d{1,2}):(\d{1,2})$": "%H:%M",
        r"^(\d{1,2})시(\d{1,2})분$": "%H시%M분",
    }

    if not date_string:
        return ""
    date_format = None
    for pattern, format_str in chinese_date_formats.items():
        match = re.match(pattern, date_string)
        if match:
            for chinese, number in chinese_to_number.items():
                date_string = date_string.replace(chinese, number)
            date_format = datetime.strptime(date_string, format_str)

    if not date_format:
        date_format = date_string
    return date_format


class DataFilter:
    def __init__(self, data_json):
        self.data_json = data_json
        self.produceType = data_json.get("produceType")
        self.data_values = data_json.get("values")
        self.value_types = list(map(lambda x: x.get("value_type"), self.data_values))
        self.data_list = list(map(lambda x: x["value"], self.data_values))
        self.cell_filterConfig_list = list(map(lambda x: x.get("colFilterConfig"), self.data_values))
        self.filterConfig_list = list(map(lambda x: x.get("filterConfig"), self.data_values))
        self.dataProcessConfig_list = list(map(lambda x: x.get("colDataProcessConfig"), self.data_values))
        self.hightLightIndex_list = []
        self.dataProcess_state = 0
        self.data_table = self.get_table()
        self.filter_mian = self.data_filter_main()

    def get_table(self):
        """
        를데이터변환로 DataFrame
        data_json: 가져오기의데이터
        return:
        """
        if self.produceType == "similar":
            max_length = max(len(sublist) for sublist in self.data_list)
            for one_list in self.data_list:
                while len(one_list) < max_length:
                    one_list.append({"attrs": {}, "text": ""})
            for i, item_list in enumerate(self.data_list):
                for item in item_list:
                    item["text"] = (
                        re.sub(
                            r"[\n\t]|^\s+|\s+$|\xa0",
                            "",
                            item["attrs"].get(self.value_types[i]),
                        )
                        if item["attrs"].get(self.value_types[i])
                        else ""
                    )
            text_values = list(map(lambda x: [item["text"] for item in x], self.data_list))
        elif self.produceType == "table":
            for i, item_list in enumerate(self.data_list):
                self.data_list[i] = [re.sub(r"[\n\t]|^\s+|\s+$|\xa0", "", item) for item in item_list]
            text_values = self.data_list

        else:
            text_values = []
        data_table = pd.DataFrame(text_values).T
        data_table.reset_index(inplace=True)
        self.hightLightIndex_list = [list(data_table["index"]) for _ in range(len(text_values))]

        return data_table

    def cell_filter(self):
        """
        셀필터링
        단일열, 선택후, 아니오수정변수열
        """
        for index in range(len(self.cell_filterConfig_list)):
            if self.cell_filterConfig_list[index]:
                filter_condition = ""
                filter_index = 0
                for condition_one in self.cell_filterConfig_list[index]:
                    filterAssociation = condition_one.get("filterAssociation")
                    logical = condition_one.get("logical")
                    parameter = condition_one.get("parameter")
                    parameter = re.sub(r"[\n\t]|^\s+|\s+$|\xa0", "", parameter)
                    logic_op_map = {"and": "&", "or": "|"}
                    filter_logic_str = self.filter_logic_calc(index, logical, parameter)
                    filter_one_str = f"({filter_logic_str})"
                    if filter_index > 0:
                        filter_one_str = f" {logic_op_map[filterAssociation]} {filter_one_str}"
                    filter_condition += filter_one_str
                    filter_index += 1
                try:
                    filter_df = self.data_table[eval(filter_condition)]
                    self.hightLightIndex_list[index] = list(filter_df["index"])
                    filter_result = list(filter_df[index])
                    self.data_table[index] = ""
                    self.data_table[index].update(filter_result)
                except Exception as e:
                    logger.error(f"cell_filter: {str(e)}")
                    raise ValueError(f"지원하지 않는 셀 필터 조건입니다: {str(e)}")

    def table_filter(self):
        """
        테이블필터링
        테이블, 선택후, 보관기호합치기파일의행데이터
        """
        for index in range(len(self.filterConfig_list)):
            if self.filterConfig_list[index]:
                filter_condition = ""
                filter_index = 0
                for condition_one in self.filterConfig_list[index]:
                    filterAssociation = condition_one.get("filterAssociation")
                    logical = condition_one.get("logical")
                    parameter = condition_one.get("parameter")
                    parameter = re.sub(r"[\n\t]|^\s+|\s+$|\xa0", "", parameter)
                    logic_op_map = {"and": "&", "or": "|"}
                    filter_logic_str = self.filter_logic_calc(index, logical, parameter)
                    filter_one_str = f"({filter_logic_str})"
                    if filter_index > 0:
                        filter_one_str = f" {logic_op_map[filterAssociation]} {filter_one_str}"
                    filter_condition += filter_one_str
                    filter_index += 1
                try:
                    self.data_table = self.data_table[eval(filter_condition)]
                except Exception as e:
                    logger.error(f"table_filter: {str(e)}")
                    raise ValueError(f"지원하지 않는 테이블 필터 조건입니다: {str(e)}")
        for index in range(len(self.hightLightIndex_list)):
            self.hightLightIndex_list[index] = [
                self.hightLightIndex_list[index][i] for i in list(self.data_table["index"])
            ]

    def filter_logic_calc(self, index, logical, parameter):
        """
        logic_op: ['==', '!=', '>', '<', '>=', '<=', 'isnull', 'notnull', 'enumerate',
                  'startswith', 'endswith', 'contains', 'not_startswith', 'not_endswith', 'not_contains',
                  'time_befor', 'time_after', 'time_between', 'regular']
        """
        filter_logic_str = None
        filter_col_str = f"self.data_table[{index}]"
        if logical in ["==", "!=", ">", "<", ">=", "<="]:
            if logical in ["==", "!="]:
                if parameter.isdigit():
                    self.data_table[index] = pd.to_numeric(self.data_table[index], errors="coerce")
                    parameter = f"{parameter}"
                else:
                    parameter = f'"{parameter}"'
            else:
                self.data_table[index] = pd.to_numeric(self.data_table[index], errors="coerce")
            filter_logic_str = f"{filter_col_str}{logical}{parameter}"
        elif logical in ["startswith", "endswith", "contains"]:
            if logical == "contains":
                parameter = re.escape(parameter)
            filter_logic_str = f'{filter_col_str}.astype(str).str.{logical}("{parameter}")'
        elif logical in ["not_startswith", "not_endswith", "not_contains"]:
            if logical == "not_contains":
                parameter = re.escape(parameter)
            logical = logical.split("_")[1]
            filter_logic_str = f'~{filter_col_str}.astype(str).str.{logical}("{parameter}")'
        elif logical in ["isnull", "notnull"]:
            # filter_logic_str = f'{filter_col_str}.astype(str).{logical}()'
            if logical == "isnull":
                filter_logic_str = f'{filter_col_str}.astype(str)==""'
            else:
                filter_logic_str = f'{filter_col_str}.astype(str)!=""'
        elif logical in ["time_befor", "time_after", "time_between"]:
            self.data_table[index] = self.data_table[index].astype(str)
            if logical == "time_befor":
                filter_logic_str = f"{filter_col_str}<'{parameter}'"
            elif logical == "time_after":
                filter_logic_str = f"{filter_col_str}>'{parameter}'"
            elif logical == "time_between":
                if isinstance(parameter, str):
                    parameter = eval(parameter)
                if isinstance(parameter, list):
                    filter_logic_str = f"({filter_col_str} >= '{parameter[0]}')&({filter_col_str} <= '{parameter[1]}')"
                else:
                    raise ValueError("시간 범위 필터 매개변수가 올바르지 않습니다")
        elif logical == "regular":
            filter_logic_str = f'{filter_col_str}.astype(str).str.contains(r"{parameter}", regex=True)'
        elif logical == "enumerate":
            if isinstance(parameter, str):
                parameter = eval(parameter)
            if isinstance(parameter, list):
                filter_logic_str = f"{filter_col_str}.isin({parameter})"
            else:
                raise ValueError("열거 필터 매개변수가 올바르지 않습니다")
        else:
            filter_logic_str = None

        return filter_logic_str

    def ExtractNum(self, index, parameters):
        # 가져오기숫자
        self.data_table[index] = list(
            self.data_table[index].astype(str).apply(lambda x: "".join(re.findall(r"\d+", x)))
        )

    def trim(self, index, parameters):
        # 제거공백
        self.data_table[index] = list(
            self.data_table[index]
            .astype(str)
            .str.replace(r"^\s+|\s+?$", "", regex=True)
            .str.replace(r"[\t\n]+", "", regex=True)
        )

    def replace(self, index, parameters):
        # 문자
        for parameter in parameters:
            text = re.escape(parameter.get("text"))
            replaceText = parameter.get("replaceText")
            self.data_table[index] = list(self.data_table[index].astype(str).str.replace(text, replaceText))

    def prefix(self, index, parameters):
        # 추가전
        val = parameters[0].get("val")
        self.data_table[index] = list(val + self.data_table[index].astype(str))

    def suffix(self, index, parameters):
        # 추가후
        val = parameters[0].get("val")
        self.data_table[index] = list(self.data_table[index].astype(str) + val)

    def formatTime(self, index, parameters):
        # 형식시간
        val = parameters[0].get("val")
        if val != "":
            self.data_table[index] = self.data_table[index].apply(parse_datetime)
            self.data_table[index] = pd.to_datetime(self.data_table[index].astype(str), errors="coerce")
            self.data_table[index] = self.data_table[index].dt.strftime(val.encode("unicode-escape").decode())
            self.data_table[index] = (
                self.data_table[index].fillna("").apply(lambda x: x.encode().decode("unicode-escape"))
            )

    def regular(self, index, parameters):
        # 정상이면선택
        val = parameters[0].get("val")
        # self.data_table[index] = self.data_table[index].astype(str).str.extractall(f'({val})')
        extracted = (
            self.data_table[index]
            .astype(str)
            .str.extractall(f"({val})")
            .groupby(level=0)[0]
            .apply(" ".join)
            .reset_index()
        )
        self.data_table[index] = extracted[0]

    def dataProcess(self):
        """
        데이터 처리: 가져오기숫자, 제거공백, 문자, 추가전, 추가후, 형식시간, 정상이면선택
        """
        # 열
        self.dataProcess_state = 1
        process_order = [
            "Trim",
            "FormatTime",
            "Regular",
            "Replace",
            "ExtractNum",
            "Prefix",
            "Suffix",
        ]
        for index in range(len(self.dataProcessConfig_list)):
            for process_type in process_order:
                process_type_result = list(
                    filter(
                        lambda x: x["processType"] == process_type and x["isEnable"],
                        self.dataProcessConfig_list[index],
                    )
                )
                if process_type_result != []:
                    process_type = process_type_result[0]["processType"]
                    parameters = process_type_result[0]["parameters"]
                    if process_type in [
                        "Replace",
                        "Prefix",
                        "Suffix",
                        "FormatTime",
                        "Regular",
                    ]:
                        if not parameters:
                            raise ValueError(f"{index + 1}열 데이터 처리 매개변수가 부족합니다")
                    try:
                        if process_type == "Trim":
                            self.trim(index, parameters)
                        elif process_type == "FormatTime":
                            self.formatTime(index, parameters)
                        elif process_type == "Regular":
                            self.regular(index, parameters)
                        elif process_type == "Replace":
                            self.replace(index, parameters)
                        elif process_type == "ExtractNum":
                            self.ExtractNum(index, parameters)
                        elif process_type == "Prefix":
                            self.prefix(index, parameters)
                        elif process_type == "Suffix":
                            self.suffix(index, parameters)
                    except Exception as e:
                        raise ValueError(f"{process_type} 매개변수가 올바르지 않습니다: {e}")

    def data_filter_main(self):
        if any(self.cell_filterConfig_list):
            # 셀필터링
            self.cell_filter()
        if any(self.dataProcessConfig_list):
            # 데이터 처리
            self.dataProcess()
        if any(self.filterConfig_list):
            # 데이터선택
            self.table_filter()
        if (
            (any(self.cell_filterConfig_list) == False)
            and (any(self.filterConfig_list) == False)
            and (any(self.dataProcessConfig_list) == False)
        ):
            for i in range(len(self.data_values)):
                self.data_json["values"][i]["filterConfig"] = []
                self.data_json["values"][i]["cellFilterConfig"] = []
                self.data_json["values"][i]["dataProcessConfig"] = []

        self.data_table.fillna("")

        return self.data_table

    def get_filtered_data(self):
        """
        가져오기 선택필터링후데이터
        """

        for list_index in range(len(self.hightLightIndex_list)):
            self.data_json["values"][list_index].update(
                {
                    "value": [
                        self.data_values[list_index].get("value")[i] for i in self.hightLightIndex_list[list_index]
                    ],
                    # "highlightRows": self.hightLightIndex_list[list_index],
                }
            )

        if self.dataProcess_state == 1:
            data_list_new = list(map(lambda x: x["value"], self.data_json["values"]))
            for row in range(len(data_list_new)):
                for col in range(len(data_list_new[row])):
                    data_table_text = self.data_table[row].tolist()[col]
                    data_table_text = "" if pd.isnull(data_table_text) else data_table_text
                    if self.produceType == "similar":
                        self.data_values[row]["value"][col].update({"text": data_table_text})
                    else:
                        self.data_values[row]["value"][col] = data_table_text

        return self.data_json

    def get_hightLightIndex(self):
        """
        가져오기높이의검색
        """
        return self.hightLightIndex_list


def table_json_merge_values(data_json, values):
    """
    병합가져오기의 table_list 까지 data_json 중그룹설치새의가져오기데이터
    @:param data_json: 가져오기객체
    @:param values: 가져오기데이터
    """
    logger.info(f"table_data_merge_values data_json: {data_json}")
    logger.info(f"table_data_merge_values values: {values}")
    if data_json["values"] is None or len(data_json["values"]) == 0 or values is None or len(values) == 0:
        data_json["values"] = values
        return data_json
    for index, item in enumerate(data_json["values"]):
        item["value"] = values[index]["value"]
    return data_json


def table_df_to_out(data_json):
    """
    를 data_json 변환성공 table 데이터사용출력
    @:param data_json: 가져오기객체
    """
    produce_type = data_json["produceType"]
    table_head = [item["title"] for item in data_json["values"]]  # 테이블
    if produce_type == "table":
        # values: [{"value": ["A", "B", "C"]}, {"value": ["D", "E", "F"]}]
        rows = list(zip(*[item["value"] for item in data_json["values"]]))  # 데이터행
    else:
        # values: [{ `value`: [{"text" : `xxx`, "attrs": {}}] }], 행가져오기출력text
        rows_temp = list(zip(*[item["value"] for item in data_json["values"]]))  # 데이터행
        rows = [[val_item["text"] for val_item in row] for row in rows_temp]

    df = pd.DataFrame(rows, columns=table_head)
    return df


def table_values_to_table_dict(values, produce_type):
    """
    를 values 변환성공 table dict
    """
    produce_type = produce_type
    logger.info(f"table_values_to_table_dict: {values}")
    table_head = [item["title"] for item in values]
    table_data = {}
    max_length = max(len(item["value"]) for item in values)

    if produce_type == "table":
        table_data = {item["title"]: item["value"] for item in values}
    else:
        for item in values:
            val_items = [val_item["text"] for val_item in item["value"]]
            logger.info(f"val_items: {val_items}")
            if len(val_items) < max_length:
                val_items += [""] * (max_length - len(val_items))
            table_data[item["title"]] = val_items
            logger.info(f"table_data: {table_data}")
    table_df = pd.DataFrame(table_data, columns=table_head)
    return table_df.to_dict("list")


def values_to_row_list(values, produce_type):
    """
    를 values 변환성공행이배열
    @:param values: 가져오기배열, 으로열로단일원
    @:param produce_type: 가져오기유형
    """
    produce_type = produce_type
    logger.info(f"values_to_row_list: {values}")
    table_head = [item["title"] for item in values]
    table_data = {}
    max_length = max(len(item["value"]) for item in values)

    if produce_type == "table":
        table_data = {item["title"]: item["value"] for item in values}
    else:
        for item in values:
            val_items = [val_item for val_item in item["value"]]
            logger.info(f"val_items: {val_items}")
            if len(val_items) < max_length:
                val_items += [{"text": "", "attrs": {}}] * (max_length - len(val_items))
            table_data[item["title"]] = val_items
            logger.info(f"table_data: {table_data}")
    table_df = pd.DataFrame(table_data, columns=table_head)
    return table_df.values.tolist()


def page_values_merge(preValues: list, values: list, produce_type: str):
    """
    를 values 변환성공행이배열
    @:param values: 가져오기배열, 으로열로단일원
    @:param produce_type: 가져오기유형
    """
    logger.info(f"page_values_merge: {values}")
    # 1, values 의매일내부value 의길이정도
    max_length = max(len(item["value"]) for item in values)
    for item in values:
        if len(item["value"]) < max_length:
            if produce_type == "table":
                item["value"] += [""] * (max_length - len(item["value"]))
            else:
                item["value"] += [{"text": "", "attrs": {}}] * (max_length - len(item["value"]))

    logger.info(f"page_values_merge1: {values}")
    # 2, 를preValues 의value 및 values 의value 병합
    if len(preValues) == 0:
        return values
    else:
        for i in range(len(preValues)):
            preValues[i]["value"] += values[i]["value"]
        return preValues
