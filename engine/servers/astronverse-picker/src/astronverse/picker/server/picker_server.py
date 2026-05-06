import time
import traceback

from astronverse.picker import DrawResult, PickerSign, SVCSign
from astronverse.picker.core.highlight_client import highlight_client
from astronverse.picker.logger import logger


class PickerServer:
    def __init__(self, service_context):
        self.service_context = service_context
        self.start_time = None  # 사용시스템계획여부
        self.end_time = None  # 사용시스템계획여부

    def server(self):
        """
        관리ws_server발송경과의메시지, 문자열, 파일, 있음선택 pick_core
        후보통아래필요하지 않습니다수정해당모듈분코드
        """
        while True:
            # 대기로드완료
            if not self.service_context.event_core:
                time.sleep(0.1)
                continue
            if not self.service_context.picker_core:
                time.sleep(0.1)
                continue

            # 외부모듈메시지관리
            sign = self.service_context.sign()
            if PickerSign.STOP.value in sign:
                try:
                    # 출력

                    # 1.
                    highlight_client.hide_wnd()
                    time.sleep(0.1)  # 대기정상

                    # 반환데이터
                    result = None
                except Exception as e:
                    logger.error("pick error: {} {}".format(e, traceback.format_exc()))
                    result = "{}".format(e)
                finally:
                    # 2.출력파일
                    self.service_context.event_core.close()

                # 3.STOP메시지,프론트엔드
                del sign[PickerSign.STOP.value]
                result_sign = "{}_RES".format(PickerSign.STOP.value)
                sign[result_sign] = result

                logger.info("선택결과, 외부모듈출력")
            elif PickerSign.START.value in sign:
                try:
                    # 시작파일
                    is_start = self.service_context.event_core.start()
                    if is_start:
                        logger.info("선택열기 ")
                    event_core = self.service_context.event_core
                    if event_core.is_cancel() or event_core.is_focus():
                        # 출력

                        try:
                            # 1.
                            # highlight_client.hide_wnd()
                            # time.sleep(0.1)  # 대기정상
                            if event_core.is_cancel() or (
                                event_core.is_focus() and self.service_context.event_tag == SVCSign.PICKER
                            ):
                                highlight_client.hide_wnd()
                                time.sleep(0.1)

                            # 반환데이터
                            if self.service_context.event_core.is_focus():
                                picker_data = sign[PickerSign.START.value]
                                result = self.service_context.picker_core.element(self.service_context, picker_data)
                            else:
                                result = "cancel"
                        except Exception as e:
                            logger.error("pick error: %s %s", e, traceback.format_exc())
                            result = "{}".format(e)
                        finally:
                            # 2.출력파일
                            self.service_context.event_core.close()

                        # 3.Cancel또는focus메시지,프론트엔드
                        del sign[PickerSign.START.value]
                        result_sign = "{}_RES".format(PickerSign.START.value)
                        sign[result_sign] = result

                        logger.info("선택결과, 출력")
                    else:
                        # 이미지
                        self.start_time = time.time()
                        draw_result: DrawResult = self.service_context.picker_core.draw(
                            self.service_context,
                            highlight_client,
                            sign[PickerSign.START.value],
                        )
                        self.end_time = time.time()

                        # 조회이미지결과
                        if not draw_result.success and draw_result.error_message:
                            logger.warning(f"선택이미지실패: {draw_result.error_message}")
                            # 기록경고계속
                            try:
                                # 1. 
                                highlight_client.hide_wnd()
                                time.sleep(0.1)

                                # 2. 준비예외정보
                                res = "{}".format(draw_result.error_message)
                            except Exception as cleanup_error:
                                logger.error("관리시출력오류: {}".format(cleanup_error))
                                res = "{}".format(draw_result.error_message)
                            finally:
                                # 3. 출력파일
                                self.service_context.event_core.close()

                            # 4. 정보 ,  ws_server 가능까지예외
                            del sign[PickerSign.START.value]
                            res_sign = "{}_RES".format(PickerSign.START.value)
                            sign[res_sign] = res

                            logger.info("선택원인예외결과")

                except Exception as e:
                    logger.error("pick error: {} {}".format(e, traceback.format_exc()))
            elif PickerSign.SMART_COMPONENT.value in sign:
                logger.info("가능컴포넌트위아래선택열기 ")
                res = self.service_context.picker_core.call_pluguin(
                    self.service_context, highlight_client, sign[PickerSign.SMART_COMPONENT.value]
                )
                del sign[PickerSign.SMART_COMPONENT.value]
                res_sign = "{}_RES".format(PickerSign.SMART_COMPONENT.value)
                sign[res_sign] = res
            else:
                # 3 
                time.sleep(0.1)