import asyncio
import json
import time
import uuid
from enum import Enum
from typing import Any, Optional

import websockets
from astronverse.picker import OperationResult, PickerSign, PickerType, RecordAction, SmartComponentAction, SVCSign
from astronverse.picker.logger import logger
from astronverse.picker.utils.browser import Browser
from pydantic import BaseModel


class PickerRequire(BaseModel):
    """선택기기요청 매개변수 유형"""

    pick_sign: PickerSign = PickerSign.START
    pick_type: PickerType = PickerType.ELEMENT
    record_action: Optional[RecordAction] = None  # 에서RECORD시사용
    smart_component_action: Optional[SmartComponentAction] = None  # 에서pick_sign예SMART_COMPONENT시사용
    data: str = None
    pick_mode: str = None
    ext_data: dict = {}


class PushAcknowledgment(BaseModel):
    """메시지유형 - 문사용메시지"""

    message_type: str = "ack"  # 식별자예메시지
    reply_to: str  # 돌아가기복사의메시지ID
    status: str = "success"
    data: str = ""
    err_msg: str = ""


class MessageType(Enum):
    """메시지유형: 분및"""

    RESPONSE = "response"  # 요청 의
    PUSH = "push"  # 


class ResponseKey(Enum):
    """메시지의key값"""

    SUCCESS = "success"
    ERROR = "error"
    CANCEL = "cancel"
    PING = "ping"


class PushKey(Enum):
    """메시지의key값"""

    RECORD_START = "record_start"
    RECORD_PAUSE = "record_pause"
    RECORD_AUTOMIC_CHOICE = "record_automic_start"
    RECORD_AUTOMIC_DRAW_END = "record_automic_draw_end"


class PickerMessage(BaseModel):
    err_msg: str = ""
    data: str = ""
    key: str
    message_type: Optional[str] = None
    message_id: Optional[str] = None  # 메시지일ID
    reply_to: Optional[str] = None  # 돌아가기복사개메시지의ID

    @classmethod
    def create_response(cls, key: ResponseKey, data: str = "", err_msg: str = ""):
        """생성메시지"""
        return cls(key=key.value, data=data, err_msg=err_msg)

    @classmethod
    def create_push(cls, key: PushKey, data: str = "", err_msg: str = ""):
        """생성메시지(ID)"""
        return cls(
            key=key.value,
            data=data,
            err_msg=err_msg,
            message_type="push",
            message_id=str(uuid.uuid4()),  # 완료일ID
        )


class PickerRequestHandler:
    """선택요청 관리기기 - 모든서비스관리"""

    def __init__(self, svc):
        self.svc = svc

    async def handle_request(self, ws, input_data: PickerRequire) -> bool:
        """관리선택요청 , 반환여부필요닫기연결"""
        logger.info("[RequestHandler] 관리요청 : {}".format(input_data))

        if input_data.pick_sign == PickerSign.RECORD:
            await self._handle_record_request(ws, input_data)
            if input_data.record_action == RecordAction.END:
                return True  # 기록제어end요청 아니오닫기연결
            else:
                return False  # 기록제어end요청 아니오닫기연결
        elif input_data.pick_sign == PickerSign.SMART_COMPONENT:
            await self._handle_smart_component_request(ws, input_data)
            return False
        else:
            await self._handle_picker_request(ws, input_data)
            return True  # 요청 필요닫기연결

    async def _handle_smart_component_request(self, ws, input_data: PickerRequire):
        """관리통신선택요청 """
        if input_data.smart_component_action == SmartComponentAction.START:
            result = await self._handle_smart_component_start(input_data)
        elif input_data.smart_component_action in [SmartComponentAction.NEXT, SmartComponentAction.PREVIOUS]:
            result = await self._handle_smart_component_next_previous(input_data)
        elif input_data.smart_component_action in [SmartComponentAction.CANCEL, SmartComponentAction.END]:
            result = await self._handle_smart_component_end(input_data)
        else:
            result = OperationResult.error("smart_component_start있음").to_dict()

        await self._send_response(ws, result)

    async def _handle_smart_component_start(self, input_data: PickerRequire) -> dict[str, Any]:
        """관리선택열기 """
        try:
            from astronverse.picker.core.highlight_client import highlight_client

            highlight_client.start_wnd("normal")

            self.svc.tag(SVCSign.SMARTCOMPONENT)
            # 전송선택열기 정보 
            res = await self.svc.send_sign(PickerSign.START, input_data.model_dump())

            # high_light.hide_wnd()
            if res == "cancel":
                return OperationResult.cancel().to_dict()
            elif isinstance(res, dict):
                res["picker_type"] = input_data.pick_type.name
                # 선택성공후, 덮어쓰기창중지의, 직선까지원저장
                from astronverse.picker.core.block_overlay import block_overlay

                block_overlay.show()
                return OperationResult.success(data=res).to_dict()
            else:
                return OperationResult.error(res).to_dict()

        except Exception as e:
            logger.error(f"가능컴포넌트열기 관리실패: {e}")
            return OperationResult.error(str(e)).to_dict()

    async def _handle_smart_component_next_previous(self, input_data: PickerRequire) -> dict[str, Any]:
        """관리선택열기 """
        try:
            # 전송선택열기 정보 
            res = await self.svc.send_sign(PickerSign.SMART_COMPONENT, input_data.model_dump())
            # high_light.hide_wnd()

            if isinstance(res, dict):
                res["picker_type"] = input_data.pick_type.name
                return OperationResult.success(data=res).to_dict()
            else:
                return OperationResult.error(res).to_dict()

        except Exception as e:
            logger.error(f"가능컴포넌트선택관리실패: {e}")
            return OperationResult.error(str(e)).to_dict()

    async def _handle_smart_component_end(self, input_data: PickerRequire) -> dict[str, Any]:
        """관리가능컴포넌트선택결과(저장/가져오기 )"""
        try:
            from astronverse.picker.core.block_overlay import block_overlay
            from astronverse.picker.core.highlight_client import highlight_client

            # 덮어쓰기창, 복사모든의
            block_overlay.hide()
            highlight_client.hide_wnd()
            return OperationResult.success(data="").to_dict()
        except Exception as e:
            logger.error(f"가능컴포넌트선택관리실패: {e}")
            return OperationResult.error(str(e)).to_dict()

    async def _handle_record_request(self, ws, input_data: PickerRequire):
        """관리기록제어요청 """
        from astronverse.picker.core.recorder_core_win import record_manager

        # 기록제어관리관리기기관리
        result = await record_manager.handle_record_action(input_data.record_action, ws, self.svc, input_data)
        # 전송
        await self._send_response(ws, result)

    async def _handle_picker_request(self, ws, input_data: PickerRequire):
        """관리통신선택요청 """
        if input_data.pick_sign == PickerSign.START:
            result = await self._handle_pick_start(input_data)
        elif input_data.pick_sign == PickerSign.STOP:
            result = await self._handle_pick_stop(input_data)
        elif input_data.pick_sign == PickerSign.VALIDATE:
            result = await self._handle_pick_validate(input_data)
        elif input_data.pick_sign == PickerSign.HIGHLIGHT:
            result = await self._handle_pick_highlight(input_data)
        elif input_data.pick_sign == PickerSign.GAIN:
            result = await self._handle_pick_gain(input_data)
        else:
            result = OperationResult.error("pick_sign있음").to_dict()

        await self._send_response(ws, result)

    async def _handle_pick_start(self, input_data: PickerRequire) -> dict[str, Any]:
        """관리선택열기 """
        try:
            from astronverse.picker.core.highlight_client import highlight_client

            with highlight_client:
                highlight_client.start_wnd("normal")

                # 관리선택데이터
                if input_data.pick_type in [PickerType.SIMILAR, PickerType.BATCH]:
                    input_data.data = self._process_element_data(input_data)
                    if input_data.pick_mode:
                        input_data.data["pick_mode"] = input_data.pick_mode

                # 전송선택열기 정보 
                self.svc.tag(SVCSign.PICKER)
                res = await self.svc.send_sign(PickerSign.START, input_data.model_dump())
                highlight_client.hide_wnd()

                if res == "cancel":
                    return OperationResult.cancel().to_dict()
                elif isinstance(res, dict):
                    res["picker_type"] = input_data.pick_type.name
                    return OperationResult.success(data=res).to_dict()
                else:
                    return OperationResult.error(res).to_dict()

        except Exception as e:
            logger.error(f"선택열기 관리실패: {e}")
            return OperationResult.error(str(e)).to_dict()

    async def _handle_pick_stop(self, input_data: PickerRequire) -> dict[str, Any]:
        """관리선택중지"""
        try:
            await self.svc.send_sign(PickerSign.STOP, input_data.model_dump())
            return OperationResult.success().to_dict()
        except Exception as e:
            return OperationResult.error(str(e)).to_dict()

    async def _handle_pick_validate(self, input_data: PickerRequire) -> dict[str, Any]:
        """관리선택검증"""
        try:
            from astronverse.locator.locator import LocatorManager
            from astronverse.picker.core.highlight_client import highlight_client

            with highlight_client:
                highlight_client.start_wnd("validate")
                input_data.data = self._process_element_data(input_data)

                res = LocatorManager().locator(input_data.data)
                if isinstance(res, list):
                    rects = [item.rect() for item in res]
                else:
                    rects = res.rect()

                highlight_client.draw_wnd(rects, "", "validate")

                time.sleep(3)

                return OperationResult.success(data="검증성공").to_dict()

        except Exception as e:
            return OperationResult.error(str(e)).to_dict()

    async def _handle_pick_highlight(self, input_data: PickerRequire) -> dict[str, Any]:
        """관리선택높이"""
        try:
            from astronverse.locator.locator import LocatorManager

            input_data.data = self._process_element_data(input_data)
            data = (
                LocatorManager.parse_element_json(input_data.data)
                if isinstance(input_data.data, str)
                else input_data.data
            )

            Browser.send_browser_extension(
                browser_type=data.get("app"),
                data=data.get("path"),
                key="highLightColumn",
                gate_way_port=self.svc.route_port,
            )

            return OperationResult.success(data="높이완료").to_dict()

        except Exception as e:
            return OperationResult.error(str(e)).to_dict()

    async def _handle_pick_gain(self, input_data: PickerRequire) -> dict[str, Any]:
        """관리선택가져오기데이터"""
        try:
            from astronverse.locator.locator import LocatorManager
            from astronverse.picker.utils.table_filter import (
                DataFilter,
                table_json_merge_values,
            )

            input_data.data = self._process_element_data(input_data)
            data = (
                LocatorManager.parse_element_json(input_data.data)
                if isinstance(input_data.data, str)
                else input_data.data
            )

            web_info = Browser.send_browser_extension(
                browser_type=data.get("app"),
                data=data.get("path"),
                key="getBatchData",
                gate_way_port=self.svc.route_port,
            )
            values = web_info["values"]
            batch_element = data.get("path")
            batch_element = table_json_merge_values(batch_element, values)
            locate_data = DataFilter(data_json=batch_element).get_filtered_data()

            return OperationResult.success(data=locate_data).to_dict()

        except Exception as e:
            return OperationResult.error(str(e)).to_dict()

    def _process_element_data(self, input_data: PickerRequire):
        """관리원데이터"""
        from astronverse.locator.locator import LocatorManager
        from astronverse.picker.utils.params import complex_param_parser

        global_data = input_data.ext_data.get("global", [])
        data = (
            LocatorManager.parse_element_json(input_data.data) if isinstance(input_data.data, str) else input_data.data
        )
        return complex_param_parser(complex_param=data, global_data=global_data)

    async def _send_response(self, ws, result: dict[str, Any]):
        """전송메시지"""
        if result.get("success"):
            data = result.get("data", "")
            if isinstance(data, dict):
                data = json.dumps(data, ensure_ascii=False)
            elif not isinstance(data, str):
                data = str(data)
            await ws.send(PickerMessage.create_response(ResponseKey.SUCCESS, data=data).model_dump_json())

        else:
            if result.get("cancel"):
                await ws.send(PickerMessage.create_response(ResponseKey.CANCEL).model_dump_json())
            error_msg = result.get("error", "지원하지 않는오류")
            await ws.send(PickerMessage.create_response(ResponseKey.ERROR, err_msg=error_msg).model_dump_json())


class PushManager:
    """관리관리기기 - 관리메시지의전송및"""

    def __init__(self):
        self.pending_pushes = {}  # 저장대기의메시지

    async def send_push_message(self, ws, push_key: PushKey, data: str = "") -> str:
        """전송메시지기록"""
        push_msg = PickerMessage.create_push(push_key, data=data)

        # 기록대기의
        self.pending_pushes[push_msg.message_id] = {
            "type": push_key.value,
            "timestamp": time.time(),
            "data": push_msg.data,
        }

        await ws.send(push_msg.model_dump_json())
        logger.info(f"메시지: {push_key.value}, ID: {push_msg.message_id}")

        return push_msg.message_id

    async def handle_acknowledgment(self, ack_data: PushAcknowledgment) -> bool:
        """관리"""
        reply_to_id = ack_data.reply_to
        status = ack_data.status
        data = ack_data.data

        logger.info(f"[PushManager] 까지: reply_to={reply_to_id}, status={status}")

        if reply_to_id in self.pending_pushes:
            push_info = self.pending_pushes[reply_to_id]
            logger.info(f"프론트엔드 {push_info['type']}: {data}")
            del self.pending_pushes[reply_to_id]
            return True
        else:
            logger.warning(f"까지지원하지 않는ID의: {reply_to_id}")
            return False


class WsServer:
    """WebSocket서비스서버 - 연결관리관리및메시지경로 """

    def __init__(self, svc, port: int):
        self.svc = svc
        self.port = port

        # 서비스관리기기
        self.request_handler = PickerRequestHandler(svc)
        self.push_manager = PushManager()

        # 기록제어파일돌아가기조정
        self._setup_record_callbacks()

    def _setup_record_callbacks(self):
        """기록제어파일돌아가기조정"""
        from astronverse.picker.core.recorder_core_win import record_manager

        record_manager.set_push_callbacks(
            on_f4=self._on_f4_pressed,
            on_esc=self._on_esc_pressed,
            on_hover=self._on_mouse_hover,
            on_mouse_out=self._on_mouse_out,
        )

    async def _on_f4_pressed(self, ws_connection):
        """기록제어 F4돌아가기조정"""
        await self.push_manager.send_push_message(ws_connection, PushKey.RECORD_START)

    async def _on_esc_pressed(self, ws_connection):
        """기록제어 ESC돌아가기조정"""
        await self.push_manager.send_push_message(ws_connection, PushKey.RECORD_PAUSE)

    async def _on_mouse_hover(self, ws_connection, rect_data):
        """기록제어 마우스중지돌아가기조정"""
        await self.push_manager.send_push_message(
            ws_connection,
            PushKey.RECORD_AUTOMIC_CHOICE,  # 복사사용있음의정보유형
            data=rect_data,
        )

    async def _on_mouse_out(self, ws_connection):
        """기록제어 마우스출력중지요소돌아가기조정"""
        await self.push_manager.send_push_message(ws_connection, PushKey.RECORD_AUTOMIC_DRAW_END)

    async def websocket_endpoint(self, ws):
        """WebSocket단말 - 메시지경로 """
        async for message in ws:
            try:
                data = json.loads(message)

                # 1. 조회여부예메시지
                if data.get("message_type") == "ack":
                    ack_data = PushAcknowledgment(**data)
                    await self.push_manager.handle_acknowledgment(ack_data)
                    continue

                # 2. 조회여부예선택요청 
                if data.get("pick_sign"):
                    input_data = PickerRequire(**data)
                    should_close = await self.request_handler.handle_request(ws, input_data)
                    if should_close:
                        await ws.close()
                    continue

                # 3. 지원하지 않는메시지형식
                logger.warning(f"지원하지 않는의메시지형식: {data}")
                await ws.send(
                    PickerMessage.create_response(ResponseKey.ERROR, err_msg="지원하지 않는의메시지형식").model_dump_json()
                )

            except Exception as e:
                import traceback

                logger.error("WebSocket메시지관리오류: {} stack: {}".format(e, traceback.format_exc()))
                try:
                    await ws.send(PickerMessage.create_response(ResponseKey.ERROR, err_msg=str(e)).model_dump_json())
                except:
                    pass  # 연결가능완료열기

    def server(self) -> None:
        """시작WebSocket서비스서버"""
        import pythoncom

        pythoncom.CoInitialize()

        async def start_server():
            """예외시작WebSocket서비스서버"""
            server = await websockets.serve(
                self.websocket_endpoint,
                "127.0.0.1",
                self.port,
                max_size=10 * 1024 * 1024,
            )
            await server.wait_closed()

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            loop.run_until_complete(start_server())
        except KeyboardInterrupt:
            logger.info("picker ws연결중")
        finally:
            loop.close()