import asyncio
import json
import threading
import time
from collections.abc import Callable
from enum import Enum
from typing import Any, Optional

from astronverse.picker import (
    RECORDING_BLACKLIST,
    DrawResult,
    MKSign,
    OperationResult,
    PickerType,
    Point,
    RecordAction,
    Rect,
)
from astronverse.picker.core.picker_core_win import PickerCore
from astronverse.picker.engines.uia_picker import UIAOperate
from astronverse.picker.logger import logger
from astronverse.picker.utils.process import find_real_application_process


class RecordingState(Enum):
    """기록제어상태"""

    IDLE = "idle"
    LISTENING = "listening"
    RECORDING = "recording"
    PAUSED = "paused"


class RecordPickerAdapter:
    """기록제어공가능의선택매칭기기"""

    def __init__(self, picker_core: PickerCore):
        self.picker_core = picker_core
        self.enable_blacklist = True

    def set_blacklist_enabled(self, enabled: bool):
        """여부사용이름단일공가능"""
        self.enable_blacklist = enabled

    def draw_for_record(self, svc, highlight_client, data: dict) -> DrawResult:
        """로기록제어방식지정제어의선택제어"""
        try:
            # 조회여부필요이름단일관리
            if self.enable_blacklist and self._should_use_blacklist(data):
                blacklist_result = self._handle_blacklist(highlight_client)
                if blacklist_result:
                    return blacklist_result

            # 호출선택공가능
            return self.picker_core.draw(svc, highlight_client, data)

        except Exception as e:
            logger.error(f"기록제어방식선택실패: {e}")
            return DrawResult(success=False, error_message=str(e))

    def _should_use_blacklist(self, data: dict) -> bool:
        """여부해당사용이름단일"""
        pick_type = data.get("pick_type")
        # 있음요소 선택유형필요이름단일관리
        return pick_type in [PickerType.ELEMENT, PickerType.SIMILAR, PickerType.BATCH]

    def _handle_blacklist(self, highlight_client) -> Optional[DrawResult]:
        """관리이름단일"""
        try:
            # 가져오기현재마우스위치의파일정보
            current_x, current_y = UIAOperate.get_cursor_pos()
            current_point = Point(current_x, current_y)
            start_control = UIAOperate.get_windows_by_point(current_point)
            if not start_control:
                logger.info(f"가져오기 위치에서uia-control출력오류{self.picker_core.last_point}")
                raise Exception("선택 변환기에서 요소를 가져오지 못했습니다. 선택 목록을 다시 여세요.")

            process_id = UIAOperate.get_process_id(start_control)
            process_info = find_real_application_process(process_id)
            process_name = process_info["name"]

            # 조회여부에서이름단일중
            if process_name in RECORDING_BLACKLIST:
                logger.debug("현재사용에서기록제어이름단일중, 사용위일의선택결과")
                return self._use_cached_result(highlight_client, process_name)
            return None  # 아니오에서이름단일중, 계속보통선택

        except Exception as e:
            logger.error(f"이름단일관리실패: {e}")
            return None

    def _use_cached_result(self, highlight_client, process_name: str) -> DrawResult:
        """사용저장의선택결과"""
        if self.picker_core.last_valid_rect:
            # 있음저장결과, 다시 제어
            logger.info(f"현재예{process_name} 저장결과예 {self.picker_core.last_valid_rect.to_json()}")
            highlight_client.draw_wnd(self.picker_core.last_valid_rect, msgs=self.picker_core.last_valid_tag)
            return DrawResult(
                success=True,
                rect=self.picker_core.last_valid_rect,
                app=process_name,
                domain=self.picker_core.last_valid_domain,
            )
        else:
            logger.info(f"저장결과예{process_name} {-1, -1, -1, -1}")
            # 있음저장결과, 반환위치기호
            placeholder_rect = Rect(-1, -1, -1, -1)
            return DrawResult(
                success=True,  # 예위치기호, 기록제어예보통
                rect=placeholder_rect,
                app=process_name,
                domain=None,  # 위치기호결과있음 domain 정보
            )


class RecordManager:
    """기록제어관리관리기기 - 시스템일관리관리기록제어상태, 파일및돌아가기조정"""

    def __init__(self):
        self.svc = None
        self.highlight_client = None
        self.state = RecordingState.IDLE
        self.ws_connection = None

        # 기록제어사용의선택매칭기기
        self.record_adapter: Optional[RecordPickerAdapter] = None

        # 닫기
        self.drawing_thread: Optional[threading.Thread] = None
        self.stop_drawing = False

        # 파일닫기
        self.event_monitor_task = None

        # 오른쪽후의선택요소
        self.last_element = None

        # 현재의rect
        self.cur_rect = None
        self.cur_app = None
        self.cur_domain = None

        # 돌아가기조정데이터
        self.push_callbacks = {
            "on_f4": None,
            "on_esc": None,
            "on_hover": None,
            "on_mouse_out": None,
        }

        # 중지트리거선택
        self.hover_triggered = False  # 중지재복사트리거
        self.hover_threshold = 0.2  # 중지시간값(초)
        self.hover_start_time = None
        self.is_hover_paused = False  # 백엔드선택여부원인로프론트엔드hover일시중지
        self.last_hover_rect = None  # 위중지감지시의, 사용감지원변수

        # 이름단일열기닫기
        self.enable_record_blacklist = True

    def initialize(self, svc):
        """관리관리기기(에서svc생성후호출)"""
        self.svc = svc
        from astronverse.picker.core.highlight_client import highlight_client

        self.highlight_client = highlight_client
        while True:
            if not self.svc.event_core:
                logger.info("svc.event_core중....")
                time.sleep(0.1)
                continue
            if not self.svc.picker_core:
                logger.info("svc.picker_core중....")
                time.sleep(0.1)
                continue
            # 개컴포넌트완료완료, 출력
            break

        # 생성기록제어사용매칭기기
        if svc.picker_core and not self.record_adapter:
            self.record_adapter = RecordPickerAdapter(svc.picker_core)
            self.record_adapter.set_blacklist_enabled(self.enable_record_blacklist)

    def set_push_callbacks(
        self,
        on_f4: Callable = None,
        on_esc: Callable = None,
        on_hover: Callable = None,
        on_mouse_out: Callable = None,
    ):
        """돌아가기조정데이터"""
        self.push_callbacks["on_f4"] = on_f4
        self.push_callbacks["on_esc"] = on_esc
        self.push_callbacks["on_hover"] = on_hover
        self.push_callbacks["on_mouse_out"] = on_mouse_out
        logger.info("기록제어관리관리기기: 돌아가기조정데이터, f4...esc...on_hover")

    async def handle_record_action(self, action: RecordAction, ws, svc, input_data) -> dict[str, Any]:
        """관리기록제어"""
        self.initialize(svc)  # 확인svc예새의
        try:
            if action == RecordAction.LISTENING:
                return await self._handle_listening(ws)
            elif action == RecordAction.START:
                return await self._handle_start()
            elif action == RecordAction.PAUSE:
                return await self._handle_pause()
            elif action == RecordAction.HOVER_START:  # 닫기 
                return await self._handle_hover_start()
            elif action == RecordAction.HOVER_END:  # 열기시작
                return await self._handle_hover_end()
            elif action == RecordAction.AUTOMIC_END:
                return await self._handle_atomic_end(input_data)
            elif action == RecordAction.END:
                return await self._handle_end()
            else:
                return OperationResult.error(f"지원하지 않는의기록제어: {action}").to_dict()
        except Exception as e:
            import traceback

            error_traceback = traceback.format_exc()
            # 기록정보까지로그
            logger.error(f"관리기록제어실패: {e}\n정보:\n{error_traceback}")
            return OperationResult.error(str(e)).to_dict()

    async def _handle_listening(self, ws) -> dict[str, Any]:
        """관리"""
        if self.state != RecordingState.IDLE:
            return OperationResult.error(f"불가열기 , 현재상태: {self.state.value}").to_dict()

        is_start = self.svc.event_core.start(domain=MKSign.RECORD)
        if is_start:
            logger.info("기록제어열기시작성공")

        self.state = RecordingState.LISTENING
        self.ws_connection = ws
        self.highlight_client.start_wnd("record")

        # 시작파일
        self.event_monitor_task = asyncio.create_task(self._monitor_events())

        logger.info("기록제어관리관리기기: 열기 방식")
        return OperationResult.success().to_dict()

    async def _handle_start(self) -> dict[str, Any]:
        """관리열기 기록제어"""
        if self.state not in [RecordingState.LISTENING, RecordingState.PAUSED]:
            return OperationResult.error(f"불가열기 기록제어, 현재상태: {self.state.value}").to_dict()

        try:
            self.state = RecordingState.RECORDING
            self._start_continuous_drawing()
            logger.info("기록제어관리관리기기: 열기 기록제어")
            return OperationResult.success().to_dict()
        except Exception as e:
            logger.info(f' "error": f"불가열기 기록제어, 현재상태: {self.state.value} {e}"')
            return OperationResult.error("기록 제어를 시작하지 못했습니다.").to_dict()

    async def _handle_pause(self) -> dict[str, Any]:
        """관리일시중지기록제어"""
        if self.state != RecordingState.RECORDING:
            return OperationResult.error(f"불가일시중지기록제어, 현재상태: {self.state.value}").to_dict()

        self.state = RecordingState.PAUSED
        self._stop_continuous_drawing()

        logger.info("기록제어관리관리기기: 일시중지기록제어")
        return OperationResult.success().to_dict()

    async def _handle_hover_start(self) -> dict[str, Any]:
        """관리일시중지기록제어"""
        if self.state != RecordingState.RECORDING:
            return OperationResult.error(f"불가일시중지기록제어경과의선택, 현재상태: {self.state.value}").to_dict()

        self.is_hover_paused = True
        self.state = RecordingState.PAUSED
        self._stop_continuous_drawing()  # 닫기draw업데이트, 예아니오hide후일의rect

        logger.info("기록제어관리관리기기: 프론트엔드hover일시중지기록제어")
        return OperationResult.success().to_dict()

    async def _handle_hover_end(self) -> dict[str, Any]:
        """관리일시중지기록제어"""
        if self.state != RecordingState.PAUSED and not self.is_hover_paused:
            return OperationResult.error(f"불가열기 기록제어, 현재상태: {self.state.value}").to_dict()

        self.state = RecordingState.RECORDING
        self._start_continuous_drawing()
        self.is_hover_paused = False

        logger.info("기록제어관리관리기기: 계속기록제어")
        return OperationResult.success().to_dict()

    async def _handle_atomic_end(self, input_data) -> dict[str, Any]:
        """관리기존결과"""
        logger.info("입력_handle_atomic_end완료")
        was_recording = self.state == RecordingState.RECORDING

        # 시일시중지
        if was_recording:
            self._stop_continuous_drawing()
            # time.sleep(3)

        try:
            # 반환선택결과
            res = (
                self.last_element
            )  # self.svc.picker_core.element(self.svc, {"pick_type": PickerType.ELEMENT})#self.last_element#

            if isinstance(res, dict):
                res["picker_type"] = input_data.pick_type.name
                result = OperationResult.success(data=res).to_dict()
            else:
                result = OperationResult.error(res).to_dict()

            return result

        finally:
            # 복사기록제어상태
            if was_recording:
                self.state = RecordingState.RECORDING
                self._start_continuous_drawing()

    async def _handle_end(self) -> dict[str, Any]:
        """관리결과기록제어"""
        # 중지파일의callback
        if self.event_monitor_task:
            self.event_monitor_task.cancel()
            self.event_monitor_task = None

        # 중지
        self._stop_continuous_drawing()

        # 
        self.highlight_client.hide_wnd()

        # 중지
        self.svc.event_core.close()

        # 재상태
        self.state = RecordingState.IDLE
        self.ws_connection = None

        logger.info("기록제어관리관리기기: 결과기록제어")
        return OperationResult.success().to_dict()

    def _start_continuous_drawing(self):
        """시작"""
        if self.drawing_thread and self.drawing_thread.is_alive():
            return

        self.stop_drawing = False
        self.hover_start_time = None  # 닫기 후필요업데이트hover의데이터

        # 캐시 정리의결과, 확인에서현재마우스위치다시 열기 
        if self.record_adapter and self.record_adapter.picker_core:
            self.record_adapter.picker_core.last_valid_rect = None
            self.record_adapter.picker_core.last_valid_tag = ""
            logger.debug("완료관리저장, 를에서현재마우스위치다시 열기 ")

        self.drawing_thread = threading.Thread(target=self._continuous_drawing_loop, daemon=True)
        self.drawing_thread.start()
        logger.info("시작")

    def _stop_continuous_drawing(self):
        """중지"""
        self.stop_drawing = True
        if self.drawing_thread and self.drawing_thread.is_alive():
            self.drawing_thread.join(timeout=0.5)

    def _continuous_drawing_loop(self):
        """"""
        import pythoncom

        pythoncom.CoInitialize()
        self.highlight_client.start_wnd("record")
        while not self.stop_drawing and self.state == RecordingState.RECORDING:
            try:
                # 사용기록제어사용매칭기기행선택
                draw_data = {
                    "pick_type": PickerType.ELEMENT,
                }

                if self.record_adapter:
                    result: DrawResult = self.record_adapter.draw_for_record(self.svc, self.highlight_client, draw_data)
                else:
                    raise Exception("선택 변환기가 초기화되지 않았습니다.")

                # 업데이트현재상태
                if result.success and result.rect:
                    self.cur_rect = result.rect
                    self.cur_app = result.app
                    self.cur_domain = result.domain
                # logger.info(f'draw반환의result: success={result.success}, rect={result.rect.to_json() if result.rect else None}')
            except Exception as e:
                import traceback

                error_traceback = traceback.format_exc()
                # 기록정보까지로그
                logger.error(f"출력오류: {e}\n정보:\n{error_traceback}")
                # self.highlight_client.hide_wnd()
                # return
        if not self.is_hover_paused:
            self.highlight_client.hide_wnd()

    async def _monitor_events(self):
        """빠름및마우스파일"""
        logger.info("열기 파일")

        try:
            # time.sleep(1)
            await asyncio.sleep(1)
            while self.state != RecordingState.IDLE and self.ws_connection:
                # 조회F4(열기 기록제어)
                if self.svc.event_core.is_f4_pressed():
                    logger.info("감지까지F4")
                    self.svc.event_core.reset_f4_flag()

                    # 트리거돌아가기조정
                    if self.push_callbacks["on_f4"] and self.state in [
                        RecordingState.LISTENING,
                        RecordingState.PAUSED,
                    ]:
                        self.state = RecordingState.RECORDING
                        self._start_continuous_drawing()
                        await self.push_callbacks["on_f4"](self.ws_connection)

                # 조회ESC(일시중지기록제어)
                if self.svc.event_core.is_cancel():
                    logger.info("감지까지ESC")
                    if hasattr(self.svc.event_core, "reset_cancel_flag"):
                        self.svc.event_core.reset_cancel_flag()

                    # 트리거돌아가기조정
                    if self.push_callbacks["on_esc"] and self.state in [
                        RecordingState.RECORDING,
                        RecordingState.PAUSED,
                    ]:
                        self.state = RecordingState.PAUSED
                        # RecordingState.RECORDING필요직선연결호출_stop_continuous_drawing가능, 예증가추가완료hover_start, 가능아래esc의시draw완료닫기완료, 필요닫기일아래highlight_client가능
                        if self.is_hover_paused:
                            self.highlight_client.hide_wnd()
                        if not self.stop_drawing:
                            self._stop_continuous_drawing()
                        await self.push_callbacks["on_esc"](self.ws_connection)

                # 마우스중지감지
                if self.state == RecordingState.RECORDING:
                    await self._check_mouse_hover()

                await asyncio.sleep(0.05)  # 50ms조회일

        except asyncio.CancelledError:
            logger.info("파일가져오기 ")
        except Exception as e:
            logger.error(f"파일출력오류: {e}")
        finally:
            logger.info("파일결과")

    def _get_current_element_rect(self) -> str:
        """가져오기현재마우스위치의요소정보"""
        try:
            import win32api

            x, y = win32api.GetCursorPos()
            if not hasattr(self, "cur_rect") or self.cur_rect is None:
                raise ValueError("cur_rect 미완료")
            # 마우스여부에서내부
            if not (self.cur_rect.left <= x <= self.cur_rect.right and self.cur_rect.top <= y <= self.cur_rect.bottom):
                raise ValueError("마우스위치아니오에서현재요소내부")
            final_record_rect = {
                "left": self.cur_rect.left,
                "top": self.cur_rect.top,
                "right": self.cur_rect.right,
                "bottom": self.cur_rect.bottom,
                "mouse_x": x,
                "mouse_y": y,
                "domain": self.cur_domain,
            }

            return json.dumps(final_record_rect, ensure_ascii=False)

        except Exception as e:
            logger.error(f"가져오기요소정보실패: {e}")
            return "{}"

    def _is_rect_changed(self, current_rect, last_rect):
        """설치전체개여부아니오, 관리None의"""
        # 결과가중일개로None, 일개아니오로None, 이면로발송완료변수
        if (current_rect is None) != (last_rect is None):
            return True
        # 결과가로None, 이면있음변수
        if current_rect is None and last_rect is None:
            return False
        # 결과가아니오로None, 이면사용보통의
        return current_rect != last_rect

    async def _check_mouse_hover(self):
        """감지마우스중지 - 필요에서rect중계획시"""
        try:
            import win32api

            current_pos = win32api.GetCursorPos()
            current_time = time.time()

            # 조회현재여부발송변수
            current_rect = self.cur_rect
            if self._is_rect_changed(current_rect, self.last_hover_rect):
                # 변수시재중지상태
                if self.hover_start_time is not None:
                    logger.debug(
                        f"감지까지요소변수, 재중지상태.: {self.last_hover_rect}, 새: {current_rect}"
                    )
                self.last_hover_rect = current_rect
                self.hover_start_time = None
                self.hover_triggered = False

            # 조회마우스여부에서draw내부
            is_in_draw_area = (
                current_rect
                and current_rect.left <= current_pos[0] <= current_rect.right
                and current_rect.top <= current_pos[1] <= current_rect.bottom
            )
            rect_data = None
            if is_in_draw_area:
                # 마우스에서내부
                if self.hover_start_time is None:
                    # 이동, 열기 계획시
                    self.hover_start_time = current_time
                    self.hover_triggered = False
                    logger.debug("마우스이동draw, 열기 중지계획시")
                elif not self.hover_triggered:
                    # 에서내부, 조회중지시간
                    hover_duration = current_time - self.hover_start_time
                    if hover_duration >= self.hover_threshold:
                        logger.info(f"감지까지마우스중지{hover_duration:.1f}초, 트리거automic_start정보 ")
                        self.hover_triggered = True
                        rect_data = self._get_current_element_rect()

                        # 트리거돌아가기조정  비고이름단일중지다중적음초아니오해당전송데이터
                        if self.push_callbacks["on_hover"] and (
                            not self.enable_record_blacklist or self.cur_app not in RECORDING_BLACKLIST
                        ):
                            await self.push_callbacks["on_hover"](self.ws_connection, rect_data)
                        self.last_element = self.svc.picker_core.element(self.svc, {"pick_type": PickerType.ELEMENT})
            else:
                # 마우스열기 , 재모든상태
                if self.hover_start_time is not None:
                    logger.debug("마우스열기draw, 재중지상태")
                if self.hover_triggered and (
                    not self.enable_record_blacklist or self.cur_app not in RECORDING_BLACKLIST
                ):  # 필요현재의사용예아니오예shoprpa예의아니오필요알림
                    await self.push_callbacks["on_mouse_out"](self.ws_connection)
                    logger.debug(f"마우스중지후열기draw, 알림프론트엔드 현재예{self.cur_app}")
                self.hover_start_time = None
                self.hover_triggered = False

        except Exception as e:
            import traceback

            logger.error("정보:\n{}".format(traceback.format_exc()))
            logger.error(f"중지감지출력오류: {e} {traceback.extract_stack()}")

    def get_state(self) -> RecordingState:
        """가져오기현재상태"""
        return self.state

    def is_recording(self) -> bool:
        """여부정상에서기록제어"""
        return self.state == RecordingState.RECORDING

    def is_listening(self) -> bool:
        """여부에서상태"""
        return self.state == RecordingState.LISTENING


# 전체영역단일
record_manager = RecordManager()
