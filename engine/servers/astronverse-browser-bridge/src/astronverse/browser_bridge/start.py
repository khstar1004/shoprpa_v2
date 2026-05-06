import argparse
import os

import uvicorn
from astronverse.browser_bridge.apis import context, route
from astronverse.browser_bridge.config import Config as conf
from astronverse.browser_bridge.logger import logger  # 정렬일
from fastapi import FastAPI
from no_config import Config

# 0. app, 
app = FastAPI()
route.handler(app)


def start():
    # 1. 매칭
    parser = argparse.ArgumentParser(description="{} service".format("rpa-browser-connector"))
    parser.add_argument("--f", default="service.yaml", help="매칭파일")
    parser.add_argument("--port", default="19082", help="본단말")
    parser.add_argument("--gateway_port", default="", help="네트워크닫기단말", required=False)
    args = parser.parse_args()
    if os.path.exists(args.f):
        Config.init(args.f)  # 사용예conf값, 통신경과비고해제완료
    if args.port:
        conf.http_settings.app_port = int(args.port)
    if args.gateway_port:
        conf.http_settings.gateway_port = int(args.gateway_port)

    # 2. 위아래문서
    svc = context.get_svc()
    svc.set_config(conf)

    # 3. FastApi시작
    logger.info("{} service[:{}] start".format(conf.app_settings.app_name, conf.http_settings.app_port))
    logger.info("swagger urL:{}".format("/docs"))
    uvicorn.run(
        app="astronverse.browser_bridge.start:app", host=conf.http_settings.app_host, port=conf.http_settings.app_port
    )