"""LLM API client helpers: streaming and normal chat plus prompt interface."""

import json
from typing import Any

import requests
import sseclient
from astronverse.actionlib.atomic import atomicMg
from astronverse.ai.error import *
from astronverse.baseline.logger.logger import logger

API_URL = "http://127.0.0.1:{}/api/rpa-ai-service/v1/chat/completions".format(
    atomicMg.cfg().get("GATEWAY_PORT") if atomicMg.cfg().get("GATEWAY_PORT") else "13159"
)
PROMPT_URL = "http://127.0.0.1:{}/api/rpa-ai-service/v1/chat/prompt".format(
    atomicMg.cfg().get("GATEWAY_PORT") if atomicMg.cfg().get("GATEWAY_PORT") else "13159"
)
DEFAULT_MODEL = "xopdeepseekv32"


def chat_streamable(messages: Any, model: str = DEFAULT_MODEL):
    """
    호출단말대유형

    :param
    messages: 제목
    model: 유형id

    - example
        inputs = [
            {"role": "assistant", "content": "요청 의"},
            {"role": "user", "content": "일"}
        ]

        outputs = {"content":"","reasoning_content":null}

    """
    chat_json = {"messages": messages, "model": model, "stream": True}

    response = requests.post(API_URL, json=chat_json)
    if response.status_code == 200:
        client = sseclient.SSEClient(response)  # type: ignore
        for event in client.events():
            if event.data and event.data != "[DONE]":
                response_json = json.loads(event.data)
                if response_json.get("choices"):
                    yield response_json["choices"][0]["delta"]["content"]
    else:
        raise BaseException(LLM_NO_RESPONSE_ERROR.format(response), "")


def chat_normal(user_input, system_input="", model=DEFAULT_MODEL):
    """생성요청 의 payload"""
    data = {
        "model": model,  # 선택대유형, 로유형식별자
        "messages": [
            {"role": "system", "content": system_input},
            {"role": "user", "content": user_input},
        ],
        "stream": False,
    }

    try:
        # 전송 API 요청 
        response = requests.post(API_URL, json=data)
        response.raise_for_status()  # 조회요청 여부성공

        # 반환유형완료의돌아가기복사
        response_json = response.json()

        # 내용 형식
        if "data" in response_json and "choices" in response_json["data"]:
            # 새형식
            return response_json["data"]["choices"][0]["message"]["content"]
        elif "choices" in response_json:
            # 기존형식
            return response_json["choices"][0]["message"]["content"]
        else:
            raise ValueError("지원하지 않는의형식")

    except requests.exceptions.RequestException as e:
        logger.info(f"요청 오류: {e}")
        return None
    except KeyError:
        logger.info("형식아니오정상")
        return None


def chat_prompt(prompt_type, params, model=DEFAULT_MODEL):
    """chat_prompt"""
    data = {
        "model": model,  # 선택대유형, 로유형식별자
        "prompt_type": prompt_type,
        "params": params,
    }

    try:
        # 전송 API 요청 
        response = requests.post(PROMPT_URL, json=data)
        response.raise_for_status()  # 조회요청 여부성공

        # 반환유형완료의돌아가기복사
        response_json = response.json()
        return response_json["data"]

    except requests.exceptions.RequestException as e:
        print(f"요청 오류: {e}")
        return None
    except KeyError:
        print("형식아니오정상")
        return None


if __name__ == "__main__":
    inputs = [
        {"role": "assistant", "content": "요청 의"},
        {"role": "user", "content": "일"},
    ]

    # inputs = [
    #     {'role': 'assistant', 'content': 'download from the releases pageadd -javaagent:/absolute/path/to/ja-netfilter.jar argument (Change to youractual path)add as an argument of the java command. eg:java -javaagent:/absolute/path/to/ja-netfilter.jar -jar executable_jar_file.jarsome apps support the JVM Options file, you can add as a line of theJVM Options file.WARNING: DO NOT put some unnecessary whitespace characters!or execute java -jar /path/to/ja-netfilter.jar to use attach mode.for Java 17 you have to add at least these JVM Options:--add-opens=java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED--add-opens=java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMEDedit your plugin conﬁg ﬁles: ${lower plugin name}.conf ﬁle in the config dir whereja-netfilter.jar is located.the config, logs and plugins directories can be speciﬁed through the javaagent args.eg: -javaagent:/path/to/ja-netfilter.jar=appName, your conﬁg, logs and pluginsdirectories will be config-appname, logs-appname and plugins-appname.if no javaagent args, they default to config, logs and plugins.this mechanism will avoid extraneous and bloated config, logs and plugins.run your java application and enjoy~ja-netﬁlter 2022.2.0A javaagent frameworkUsage\nConﬁg ﬁle format\n[ABC]# for the specified section name# for example[URL]EQUAL,https://someurl[DNS]EQUAL,somedomain# EQUAL       Use `equals` to compare# EQUAL_IC    Use `equals` to compare, ignore case# KEYWORD     Use `contains` to compare# KEYWORD_IC  Use `contains` to compare, ignore case# PREFIX      Use `startsWith` to compare# PREFIX_IC   Use `startsWith` to compare, ignore case# SUFFIX      Use `endsWith` to compare# SUFFIX_IC   Use `endsWith` to compare, ignore case# REGEXP      Use regular expressions to matchthe ja-netfilter will NOT output debugging information by defaultadd environment variable JANF_DEBUG=1 (log level) and start to enable itor add system property -Djanf.debug=1 (log level) to enable itlog level: NONE=0, DEBUG=1, INFO=2, WARN=3, ERROR=4the ja-netfilter will output debugging information to the console by defaultadd environment variable JANF_OUTPUT=value and start to change output mediumor add system property -Djanf.output=value to change output mediumoutput medium value: [NONE=0, CONSOLE=1, FILE=2, CONSOLE+FILE=3,WITH_PID=4]eg: console + file + pid file name = 1 + 2 + 4 = 7, so the -Djanf.output=7for developer:view the scaffold project written for the plugin systemcompile your plugin and publish itjust use your imagination~for user:download the jar ﬁle of the pluginput it in the subdirectory called plugins where the ja-netﬁlter.jar ﬁle is locatedDebug infoDebug outputPlugin system\nenjoy the new capabilities brought by the pluginif the ﬁle sufﬁx is .disabled.jar, the plugin will be disabled'},
    #     {'role': 'user', 'content': '예대유형'},
    #
    # ]

    s = []
    r = []
    for i in chat_streamable(inputs):
        s.append(i)
    print("".join(r))
    print("".join(s))