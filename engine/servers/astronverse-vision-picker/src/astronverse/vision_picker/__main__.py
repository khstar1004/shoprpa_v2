import argparse
import asyncio
import json
import sys
import time

import pyautogui
import websockets
from astronverse.vision_picker.core import PickType, Status
from astronverse.vision_picker.core.core import IPickCore
from astronverse.vision_picker.core.message import PickerInputData, PickerResponse, PickerResponseItem, PickerSign
from astronverse.vision_picker.core.picker import CVPicker, Socket
from astronverse.vision_picker.logger import logger


async def handler(websocket):
    try:
        picker = CVPicker(status=Status.INIT, picktype=PickType.TARGET)
        # 관리프론트엔드 Websocket 전송경과의메시지
        async for message in websocket:
            data = json.loads(message)
            input_data = PickerInputData(**data)
            logger.info(
                f"remote_addr:{Config.REMOTE_ADDR}, port:{Config.VISION_PICKER_PORT}, input_data.sign:{input_data.pick_type}"
            )

            # 여부까지열기 선택정보 
            if input_data.pick_sign == PickerSign.START:
                logger.info("START CV PICKER")
                # 선택
                picker.set(status=Status.INIT, picktype=PickType.TARGET)
                status, msg = picker.run()

                # 근거선택반환의아니오상태반환의
                if status == Status.OVER:
                    if msg is not None:
                        await websocket.send(PickerResponse(err_msg="", data=msg).model_dump_json())
                    else:
                        await websocket.send(
                            PickerResponse(
                                err_msg="선택실패, 선택하지 못한 요소", data="", key=PickerResponseItem.ERROR
                            ).model_dump_json()
                        )
                    continue
                elif status == Status.TIMEOUT:
                    await websocket.send(
                        PickerResponse(err_msg="선택시간 초과", data="", key=PickerResponseItem.ERROR).model_dump_json()
                    )
                    continue
                elif status == Status.CANCEL:
                    await websocket.send(
                        PickerResponse(err_msg="선택가져오기 ", data="", key=PickerResponseItem.CANCEL).model_dump_json()
                    )
                    continue
                else:
                    raise NotImplementedError()
            # 여부까지검증정보 
            elif input_data.pick_sign == PickerSign.VALIDATE:
                with Socket() as hl:
                    data = json.loads(input_data.data)
                    logger.info("검증data ", data)
                    logger.info("이동검증")
                    # 실행검증프로그램, 가져오기목록 요소위치
                    match_rect = IPickCore.match_imgs(data=data, remote_addr=Config.REMOTE_ADDR)
                    logger.info(f"매칭목록 : {match_rect}")
                    if match_rect:
                        # 높이전송정보, 행높이
                        logger.info(f"목록 요소검증성공, 요소: {match_rect}")
                        hl.send_rect(operation="validate", rect=match_rect)
                        time.sleep(3)
                        await websocket.send(PickerResponse(err_msg="", data="검증성공").model_dump_json())
                    else:
                        # 전송검증하지 못한 목록 요소
                        logger.info("목록 요소검증실패")
                        await websocket.send(
                            PickerResponse(
                                err_msg="검증하지 못한 목록 요소, 확인하세요페이지요소또는낮음검증정도재시도",
                                data="",
                                key=PickerResponseItem.ERROR,
                            ).model_dump_json()
                        )

                    continue

            # 여부까지재의정보 
            elif input_data.pick_sign == PickerSign.DESIGNATE:
                with Socket() as hl:
                    data = json.loads(input_data.data)
                    # 행목록 요소검증, 현재여부존재함목록 요소
                    match_rect = IPickCore.match_imgs(data=data, remote_addr=Config.REMOTE_ADDR)
                    desktop_img = pyautogui.screenshot()
                    if match_rect:
                        # 높이전송designate정보목록 요소, 행식별자
                        hl.send_rect(operation="start", status="designate", rect=match_rect)
                        # 열기 선택이미지
                        logger.info("요소검증성공, 열기 선택")
                        picker.set(status=Status.INIT, picktype=PickType.ANCHOR, anchor_pick_img=desktop_img)
                        status, anchor_msg = picker.run()
                    else:
                        # 감지하지 못한 목록 요소, 오류반환
                        logger.info("요소검증실패, 현재없음목록 요소")
                        await websocket.send(
                            PickerResponse(
                                err_msg="현재감지하지 못한 목록 요소, 불가선택", data="", key=PickerResponseItem.ERROR
                            ).model_dump_json()
                        )
                        continue

                    # 근거선택반환의아니오상태반환의
                    if status == Status.OVER:
                        if anchor_msg is not None:
                            await websocket.send(PickerResponse(err_msg="", data=anchor_msg).model_dump_json())
                        else:
                            await websocket.send(
                                PickerResponse(
                                    err_msg="선택실패, 선택하지 못한 요소", data="", key=PickerResponseItem.ERROR
                                ).model_dump_json()
                            )
                    elif status == Status.TIMEOUT:
                        logger.info("선택시간 초과")
                        await websocket.send(
                            PickerResponse(err_msg="선택시간 초과", data="", key=PickerResponseItem.ERROR).model_dump_json()
                        )
                        continue
                    elif status == Status.CANCEL:
                        logger.info("선택가져오기 ")
                        await websocket.send(
                            PickerResponse(err_msg="선택가져오기 ", data="", key=PickerResponseItem.CANCEL).model_dump_json()
                        )
                    else:
                        raise NotImplementedError()
                    continue

    except websockets.ConnectionClosed as e:
        logger.info(f"Connection closed: {e}")
    except Exception as e:
        logger.info(f"프로그램실행오류: {e}")
    finally:
        logger.info("Client disconnected")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--schema", type=str, default="vision_picker")
    parser.add_argument("--vision_picker_port", type=int, default=8108)
    parser.add_argument("--highlight_socket_port", type=int, default=11001)
    parser.add_argument("--remote_addr", type=str)
    try:
        args = parser.parse_args()
        # 보통의서비스
        logger.info(
            f"Starting application with schema: {args.schema}, "
            f"vision_picker_port: {args.vision_picker_port}, highlight_socket_port: {args.highlight_socket_port}"
        )
    except SystemExit as e:
        # argparse 통신경과호출 sys.exit() 관리아니오정상의매개변수, 출력 SystemExit 예외
        logger.error("Error parsing arguments", exc_info=True)
        sys.exit(e.code)

    schema = args.schema
    vision_picker_port = args.vision_picker_port
    highlight_socket_port = args.highlight_socket_port
    remote_addr = args.remote_addr

    from astronverse.vision_picker.config import Config

    Config.VISION_PICKER_PORT = vision_picker_port
    Config.HIGHLIGHT_SOCKET_PORT = highlight_socket_port
    Config.REMOTE_ADDR = remote_addr

    if schema == "vision_picker":
        try:
            # 및실행websocket서비스서버의데이터
            async def main():
                # 에서지정의기기및단말위시작Websocket서비스서버
                start_serve = websockets.serve(
                    handler, "localhost", Config.VISION_PICKER_PORT, max_size=10 * 1024 * 1024
                )
                # 대기서비스서버시작완료
                await start_serve
                try:
                    await asyncio.Future()  # run forever
                except asyncio.CancelledError:
                    logger.info("Server stopped")

            asyncio.run(main())
        except Exception as e:
            logger.exception(e)
            sys.exit(-1)