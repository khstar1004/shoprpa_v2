"""String utility helpers for prompt keyword replacement and platform paths."""

import os
import sys


def replace_keyword(prompts: list, input_keys: list) -> list:
    """
    량닫기 
    :param input_keys: [{"keyword": "question", "text": "Your replacement keyword"}]
    :param prompts: Template will be replaced

    :return {"role": "System", "content": "제목예: {본월H6버전에서전체의판매상업라이브러리저장량예다중적음?}"}
    """
    for input_key in input_keys:
        for ind, prompt in enumerate(prompts):
            keyword = "{" + f"{input_key['keyword']}" + "}"
            if keyword in prompt["content"]:
                prompts[ind]["content"] = prompt["content"].replace(keyword, input_key["text"])
    return prompts


def platform_python_path(parent_dir: str):
    """Return platform specific python executable path under given parent directory."""
    if sys.platform == "win32":
        path = os.path.join(parent_dir, r"python.exe")
    else:
        path = os.path.join(parent_dir, "bin", "python3.7")
    return path