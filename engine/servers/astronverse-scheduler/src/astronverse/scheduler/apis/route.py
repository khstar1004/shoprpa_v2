from astronverse.scheduler.apis.connector import credential, datatable, executor, picker, terminal, tools, ws
from astronverse.scheduler.core.lsp.routes import router as lsp_router
from astronverse.scheduler.core.svc import get_svc
from fastapi import Depends, FastAPI
from fastapi.middleware.cors import CORSMiddleware


def handler(app: FastAPI):
    # 추가전체영역중파일
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # 지정tools경로 
    app.include_router(tools.router, prefix="", tags=["tools"], dependencies=[Depends(get_svc)])

    # 지정단말
    app.include_router(
        terminal.router,
        prefix="/terminal",
        tags=["terminal"],
        dependencies=[Depends(get_svc)],
    )

    # 지정실행기기경로 
    app.include_router(
        executor.router,
        prefix="/executor",
        tags=["executor"],
        dependencies=[Depends(get_svc)],
    )

    # 지정선택경로 
    app.include_router(
        picker.router,
        prefix="/picker",
        tags=["picker"],
        dependencies=[Depends(get_svc)],
    )

    # 지정전체영역ws
    app.include_router(ws.router, prefix="/ws", tags=["ws"], dependencies=[Depends(get_svc)])

    # 지정lsp경로 
    app.include_router(lsp_router, prefix="/lsp", tags=["lsp"], dependencies=[Depends(get_svc)])

    # 지정데이터테이블경로 
    app.include_router(
        datatable.router,
        prefix="/datatable",
        tags=["datatable"],
        dependencies=[Depends(get_svc)],
    )

    # 지정인증관리관리경로 
    app.include_router(
        credential.router,
        prefix="/credential",
        tags=["credential"],
        dependencies=[Depends(get_svc)],
    )