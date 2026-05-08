import os
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.config import get_settings
from app.dependencies import get_ws_service
from app.internal import admin
from app.logger import get_logger
from app.middlewares.tracing import RequestTracingMiddleware
from app.redis import close_redis_pool, init_redis_pool
from app.routers import api_keys, executions, healthcheck, user, websocket, workflows
from app.routers.streamable_mcp import (
    handle_streamable_http,
    session_manager,
    tools_config,
)

logger = get_logger(__name__)
settings = get_settings()


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    """Context manager for application lifespan."""
    # Initialize connections
    # await create_db_and_tables()

    await init_redis_pool()

    worker_id = os.getpid()
    await get_ws_service()
    logger.info("WsManagerService singleton initialized for worker %s", worker_id)

    async with session_manager.run():
        logger.info("Application started with StreamableHTTP session manager!")
        try:
            yield
        finally:
            logger.info("Application shutting down...")

            await tools_config.cleanup_connections()
            logger.info("Tools config connections cleaned up")

            await close_redis_pool()
            logger.info("Worker %s shutting down", worker_id)


app = FastAPI(title=settings.APP_NAME, version=settings.API_VERSION, lifespan=lifespan)

app.include_router(admin.router, prefix="/admin", tags=["admin"])

app.include_router(workflows.router)
app.include_router(executions.router)
app.include_router(api_keys.router)
app.include_router(healthcheck.router)
app.include_router(user.router)
app.include_router(websocket.router)
app.mount("/mcp", handle_streamable_http)

app.add_middleware(RequestTracingMiddleware)


@app.get("/")
async def root():
    logger.info("Root endpoint accessed!")

    return {"service": settings.APP_NAME, "status": "ok"}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8020,
        proxy_headers=True,
    )
