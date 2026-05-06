#!/usr/bin/env python3
"""
AI Service 시작항목본
지원통신경과매개변수항목지정항목매칭항목파일

사용방법법:
    python run.py          # 사용항목매칭항목
    python run.py dev      # 사용 .env.dev 매칭항목
    python run.py prod     # 사용 .env.prod 매칭항목
    python run.py test     # 사용 .env.test 매칭항목

항목변수:
    ENV=dev               # 통신경과항목변수항목지정항목
"""

import os
import sys

import uvicorn
from dotenv import load_dotenv


def load_environment_config(env_name=None):
    """항목근거항목이름로드항목의매칭항목파일"""
    base_dir = os.path.dirname(os.path.abspath(__file__))

    if env_name:
        env_file = f".env.{env_name}"
        env_path = os.path.join(base_dir, env_file)

        if os.path.exists(env_path):
            print(f"Loading environment from: {env_file}")
            load_dotenv(env_path, override=True)
        else:
            print(f"Warning: Environment file {env_file} not found, using default configuration")
            # 로드항목매칭항목
            load_dotenv(os.path.join(base_dir, ".env.default"))
            load_dotenv(os.path.join(base_dir, ".env"), override=True)
    else:
        print("Using default environment configuration")
        # 로드항목매칭항목
        load_dotenv(os.path.join(base_dir, ".env.default"))
        load_dotenv(os.path.join(base_dir, ".env"), override=True)


def main():
    # 가져오기항목매개변수, 항목에서항목변수가져오기, 항목에서명령항목행매개변수가져오기
    env_name = os.getenv("ENV") or (sys.argv[1] if len(sys.argv) > 1 else None)
    if not env_name:
        env_name = "local"

    # 로드항목매칭항목
    load_environment_config(env_name)

    print(f"Starting OpenAPI Service with environment: {env_name or 'default'}")

    # 시작항목사용
    uvicorn.run(
        "app.main:app",  # 에서Docker내용기기중, 에서/app디렉터리시작, 항목으로예app.main:app
        host="0.0.0.0",
        port=8020,
        proxy_headers=True,
        # workers=4,
        reload=True if env_name == "dev" else False,  # 열기발송항목사용항목재항목
    )


if __name__ == "__main__":
    main()