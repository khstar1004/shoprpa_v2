import ast
from enum import Enum
from typing import Any

import requests
import sseclient


class InputType(Enum):
    FILE = "file"
    TEXT = "text"


CONTRACT_COMMON_PROMPT = """
로일이름합치기파일파싱, 의작업예에서사용자의**합치기파일내용**중, 일가져오기사용자 지정 **가져오기필요**, 필요출력결과.요청 격식**작업필요**및**출력형식**.

=====================
### **가져오기필요**: 

요청에서합치기파일중가져오기으로아래모든필요의정보: 

{factors}

=====================
### **작업필요**: 

1. *매칭*: 매개필요, 요청에서합치기파일내용중행일매칭, 직선연결가져오기기존문서정보;
2. *격식*: 모든필요정보 직선연결합치기파일기존문서, 요청 행작업, 결과또는관리;
3. *재복사출력*: 결과가필요에서합치기파일중다중재복사출력내용, 가져오기일가능;
4. *다중매칭*: 결과가필요정보패키지합치기파일중아니오위치의다중내용, 요청를아니오위치의내용 연결성공일개문자열, 사용중국어 `, `행분;
5. *없음매칭시*: 필요에서합치기파일중찾을 수 없는 정보, 요청를해당필요의값비어 있습니다문자열 `""`;
6. *반환결과*: 요청 추가작업해제문서문자, 또는없음닫기내용.

=====================
### **합치기파일내용**: 

으로아래예사용자의합치기파일내용, 요청 **작업필요**출력가져오기결과: 

{parsed_content}

=====================
### **출력형식**: 

요청 격식으로아래 JSON 형식반환결과, 확인필드이름및필요일일, 패키지가져오기의내용: 

{
  "필요1이름": ["필요1기존문서정보1", "필요1기존문서정보2"],
  "필요2이름": "필요2기존문서정보",
  "필요3이름": "",
  ...
}

"""

CONTRACT_FACTOR_DICT = {
    "합치기이름": "반대합치기내용및의제목또는이름",
    "합치기번호": "합치기의일코드",
    "합치기주문날짜": "방법정상방식합치기의날짜",
    "합치기열기 날짜": "합치기정상방식또는시작 행의날짜",
    "합치기결과날짜": "합치기있음결과또는일행결과의후날짜",
    "합치기의": "합치기객체의이름, 예물품, 서비스, 물품대기",
    "의수": "수단일위치, 필요계획량방식",
    "단일가격": "단일위치상품/서비스의가격",
    "": "사용합치기물품또는서비스의, 예증가값",
    "금액": "근거계획출력의금액",
    "합치기금액": "가격, 필요여부",
    "방식": "결과방식, 합치기중의채널방식",
    "방법": "합치기발송방법/필요방법/, 구매, , 발송패키지또는실행대기작업의일방법, 가능으로관리해제로발송패키지사람/사람/실행방법",
    "방법": "합치기수신방법/서비스/방법, , , 계획, 관리, 실행입출력또는서비스의일방법, 가능으로관리해제로패키지사람/계획사람/관리사람/사람/실행방법",
    "방법열기사용자행": "합치기발송방법(방법)의열기사용자행이름",
    "방법행계정": "합치기발송방법(방법)사용의행계정",
    "방법열기사용자행": "합치기수신방법(방법)의열기사용자행이름",
    "방법행계정": "합치기수신방법(방법)사용의행계정",
}


def extract_pdf(path: str) -> str:
    """
    가져오기 pdf 텍스트내용

    :param path:
    :return:
    """
    import pypdf

    pdf_reader = pypdf.PdfReader(path)
    return "\n".join([pdf_reader.pages[page_num].extract_text() for page_num in range(len(pdf_reader.pages))])


def extract_docx(path: str) -> str:
    """
    가져오기 docx 텍스트내용

    :param path:
    :return:
    """
    try:
        from docx import Document as d_doc
    except Exception:
        raise Exception(
            "Docx package depend on Python 3.x version,"
            "Which compatible with most of OS and file generate by support Microsoft Office 2007"
            "if import error, please execute `pip install python-docx`"
        )
    document = d_doc(path)
    return "\n\n".join([para.text for para in document.paragraphs])


def chat_sse(inputs: Any, route_port: int):
    url = "http://127.0.0.1:{}/api/rpaai/chat".format(route_port)
    response = requests.post(url, json=inputs, stream=True)
    if response.status_code == 200:
        client = sseclient.SSEClient(response)
        for event in client.events():
            if event:
                yield event.data
    else:
        pass


def get_factors(
    contract_type: InputType = InputType.TEXT,
    contract_path: str = "",
    contract_content: str = "",
    custom_factors: str = "",
    contract_validate: str = "",
    route_port: int = 13159,
):
    if contract_type == InputType.FILE:
        file_extension = contract_path.split(".")[-1]
        if file_extension == "pdf":
            contract_content = extract_pdf(contract_path)
        elif file_extension in ["docx", "doc"]:
            contract_content = extract_docx(contract_path)
        elif file_extension == "txt":
            contract_content = open(contract_path).read()
        else:
            raise ValueError("지원하지 않음의파일유형: " + file_extension)
    try:
        custom_factors = ast.literal_eval(custom_factors)
    except:
        raise ValueError("custom_factors 형식오류, 확인하세요")
    preset_factors = custom_factors.get("preset", [])
    custom_factors = custom_factors.get("custom", [])

    factors = []
    for factor in preset_factors:
        factors.append({"필요이름": factor, "필요설명": CONTRACT_FACTOR_DICT[factor]})
    for factor in custom_factors:
        factors.append(
            {
                "필요이름": factor["name"],
                "필요설명": factor.get("desc", ""),
                "필요": factor.get("example", ""),
            }
        )

    system_input = "예일이름합치기파일파싱, 길이에서사용자의**합치기파일내용**중, 일가져오기사용자 지정 **가져오기필요**, 필요출력결과.격식**작업필요**및**출력형식**."
    user_input = CONTRACT_COMMON_PROMPT.replace("{factors}", str(factors)).replace("{parsed_content}", contract_content)

    inputs = [
        {"role": "user", "content": user_input},
        {"role": "system", "content": system_input},
    ]
    s = []
    for i in chat_sse(inputs, route_port):
        content = i.split("<$start>")[1].split("<$end>")[0]
        if content == "start" or content == "end":
            continue
        s.append(content)
    reply = "".join(s)

    return reply