"""Prompt templates for document theme expansion and sentence operations."""

PROMPT_THEME_EXTEND = """
    근거지정제목, 완료일문서, 필요해당문서기호합치기통신사용방식및문자데이터필요.

    1. 해당유형제목의, 의.
    2. 제목완료의내용 변경있음정도및가격값.
    3. 완료문자데이터에서1000문자으로위.

    theme: {theme}
"""


PROMPT_SENTENCE_EXTEND = """
    예일개
, 지정를, 내용.

    1. 해당유형제목의, 의.
    2. 제목완료의내용 변경있음정도및가격값.

    paragraph:{paragraph}
"""


PROMPT_SENTENCE_REDUCE = """
    예일개, 지정를, 가져오기 , 닫기 의정보및필요.

    paragraph:{paragraph}
"""