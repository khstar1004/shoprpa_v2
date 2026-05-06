import json

from mcp import types
from mcp.server.lowlevel import Server
from mcp.server.streamable_http_manager import StreamableHTTPSessionManager
from starlette.types import Receive, Scope, Send

from app.logger import get_logger

logger = get_logger(__name__)
from app.dependencies import extract_api_key_from_request
from app.services.streamable_mcp import ToolsConfig

app = Server("shoprpa-mcp")

global tools_config
tools_config = ToolsConfig()

# 생성 session_manager 
session_manager = StreamableHTTPSessionManager(
    app=app,
    event_store=None,
    json_response=False,
    stateless=True,
)


@app.call_tool()
async def call_tool(name: str, arguments: dict) -> list[types.ContentBlock]:
    ctx = app.request_context

    # 에서URL매개변수가져오기API_KEY
    api_key = extract_api_key_from_request(ctx)
    user_id = await tools_config.get_uid_from_raw_key(api_key)
    logger.info(f"[call_tool] user_id: {user_id}")
    if not user_id:
        await ctx.session.send_log_message(
            level="error",
            data=f"No user found for API key: {api_key}",
            logger="permission_check",
            related_request_id=ctx.request_id,
        )
        raise Exception("찾을 수 없는 사용자: No user found for API key")

    # 사용 ToolsConfig 실행워크플로
    result = await tools_config.execute_workflow_by_name(name, user_id, arguments)

    if result["success"]:
        # 기록성공실행
        await ctx.session.send_log_message(
            level="info",
            data=f"Started workflow execution: execution_id={result['execution_id']}, project_id={result['project_id']}",
            logger="workflow_execution",
            related_request_id=ctx.request_id,
        )

        if result["message"]["code"] == "0000":
            return [types.TextContent(type="text", text=json.dumps(result["message"], indent=2, ensure_ascii=False))]
        else:
            raise Exception(f"클라이언트실행실패: {result['message']['msg']}")
    else:
        # 기록실패정보
        await ctx.session.send_log_message(
            level="warning",
            data=f"Failed to execute workflow: {result['error']}",
            logger="workflow_execution",
            related_request_id=ctx.request_id,
        )

        raise Exception(f"서버실행실패: {result['error']}")


@app.list_tools()
async def list_tools() -> list[types.Tool]:
    # 가져오기요청 위아래문서정보
    ctx = app.request_context

    # 에서URL매개변수가져오기API_KEY
    api_key = extract_api_key_from_request(ctx)
    if not api_key:
        return []

    user_id = await tools_config.get_uid_from_raw_key(api_key)
    if not user_id:
        # 기록권한조회실패
        if hasattr(ctx, "session"):
            await ctx.session.send_log_message(
                level="warning",
                data=f"No user found for API key: {api_key}",
                logger="permission_check",
                related_request_id=ctx.request_id,
            )
        return []

    # 가져오기사용자가능사용의도구
    allowed_tools = await tools_config.get_tools_for_user(user_id)

    # 기록권한조회성공
    if hasattr(ctx, "session"):
        await ctx.session.send_log_message(
            level="info",
            data=f"User access: user_id={user_id}, allowed_tools={len(allowed_tools)}",
            logger="permission_check",
            related_request_id=ctx.request_id,
        )

    return allowed_tools


async def handle_streamable_http(scope: Scope, receive: Receive, send: Send) -> None:
    await session_manager.handle_request(scope, receive, send)