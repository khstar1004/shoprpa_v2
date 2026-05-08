import json

from mcp import types
from mcp.server.lowlevel import Server
from mcp.server.streamable_http_manager import StreamableHTTPSessionManager
from starlette.types import Receive, Scope, Send

from app.dependencies import extract_api_key_from_request
from app.logger import get_logger
from app.services.streamable_mcp import ToolsConfig

logger = get_logger(__name__)
app = Server("shoprpa-mcp")

global tools_config
tools_config = ToolsConfig()

session_manager = StreamableHTTPSessionManager(
    app=app,
    event_store=None,
    json_response=False,
    stateless=True,
)


@app.call_tool()
async def call_tool(name: str, arguments: dict) -> list[types.ContentBlock]:
    ctx = app.request_context

    api_key = extract_api_key_from_request(ctx)
    user_id = await tools_config.get_uid_from_raw_key(api_key)
    logger.info("[call_tool] authenticated=%s", bool(user_id))
    if not user_id:
        await ctx.session.send_log_message(
            level="error",
            data="No user found for supplied API key",
            logger="permission_check",
            related_request_id=ctx.request_id,
        )
        raise Exception("No user found for API key")

    result = await tools_config.execute_workflow_by_name(name, user_id, arguments)

    if result["success"]:
        await ctx.session.send_log_message(
            level="info",
            data=f"Started workflow execution: execution_id={result['execution_id']}, project_id={result['project_id']}",
            logger="workflow_execution",
            related_request_id=ctx.request_id,
        )

        message = result.get("message")
        if isinstance(message, dict):
            if message.get("code") == "0000":
                return [types.TextContent(type="text", text=json.dumps(message, indent=2, ensure_ascii=False))]
            raise Exception(f"Client execution failed: {message.get('msg') or message.get('message') or message}")

        return [types.TextContent(type="text", text=str(message or ""))]
    else:
        await ctx.session.send_log_message(
            level="warning",
            data=f"Failed to execute workflow: {result['error']}",
            logger="workflow_execution",
            related_request_id=ctx.request_id,
        )

        raise Exception(f"Server execution failed: {result['error']}")


@app.list_tools()
async def list_tools() -> list[types.Tool]:
    ctx = app.request_context

    api_key = extract_api_key_from_request(ctx)
    if not api_key:
        return []

    user_id = await tools_config.get_uid_from_raw_key(api_key)
    if not user_id:
        if hasattr(ctx, "session"):
            await ctx.session.send_log_message(
                level="warning",
                data="No user found for supplied API key",
                logger="permission_check",
                related_request_id=ctx.request_id,
            )
        return []

    allowed_tools = await tools_config.get_tools_for_user(user_id)

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
