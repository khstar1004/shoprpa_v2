from astronverse.browser_bridge.apis import ws_route
from astronverse.browser_bridge.apis.browser.v1 import browser
from astronverse.browser_bridge.apis.context import get_svc
from astronverse.browser_bridge.apis.response import http_base_exception, http_exception
from astronverse.browser_bridge.error import *
from fastapi import Depends, FastAPI
from fastapi.middleware.cors import CORSMiddleware


def handler(app: FastAPI):
    # 추가전체영역오류 관리
    app.add_exception_handler(BaseException, http_base_exception)
    app.add_exception_handler(Exception, http_exception)

    # 추가전체영역중파일
    app.add_middleware(
        CORSMiddleware, allow_origins=["*"], allow_credentials=True, allow_methods=["*"], allow_headers=["*"]
    )

    # 지정websocket경로 
    app.include_router(ws_route.router, prefix="/ws", tags=["ws"], dependencies=[Depends(get_svc)])

    # 지정http경로 
    app.include_router(browser.router, prefix="/browser", tags=["browser"], dependencies=[Depends(get_svc)])