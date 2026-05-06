#!/usr/bin/env python3

from pprint import pprint

from astronverse.workflowlib.params import ComplexParamParser


def test_complex_param_parser():
    """시도복사매개변수파싱기기"""
    source_dict = {
        "python_expr": {"rpa": "special", "value": [{"type": "python", "data": "len(user_list)"}]},
        "flow_var": {"rpa": "special", "value": [{"type": "var", "data": "current_user"}]},
        "global_var": {"rpa": "special", "value": [{"type": "g_var", "data": "api_base_url"}]},
        "mixed": {"rpa": "special", "value": [
            {"type": "var", "data": "prefix"},
            {"type": "str", "data": "_"},
            {"type": "g_var", "data": "suffix"}
        ]},

        "nested": {
            "deep": [
                {"rpa": "special", "value": [{"type": "var", "data": "deep_var"}]},
                {"rpa": "special", "value": [{"type": "other", "data": "deep_var"}]},
                {"rpa": "special", "value": [{"type": "str", "data": "deep_var"}]},
            ]
        }
    }

    # 런타임변수
    user_list = ["a", "b"]
    current_user = "A()"
    prefix = "order"
    deep_var = "nested_value"
    gv = {
        "api_base_url": "https://api.example.com",
        "suffix": "_end"
    }

    _processor = ComplexParamParser()
    template = _processor.parse_params(source_dict)

    # 의변수위아래문서
    ctx = {
        'prefix': "order2",  # 덮어쓰기기존의값
    }
    result = _processor.evaluate_params(template, ctx)
    print("시도결과:")
    pprint(result)


if __name__ == "__main__":
    test_complex_param_parser()