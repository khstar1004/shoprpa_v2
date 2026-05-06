import argparse
import threading

from astronverse.picker.logger import logger
from astronverse.picker.server.ws_server import WsServer
from astronverse.picker.svc import ServiceContext


def start():
    parser = argparse.ArgumentParser(description=f"{'executor'} service")

    parser.add_argument("--port", help="서비스http단말", type=int, default=8101)
    parser.add_argument("--route_port", help="브라우저통신중파일단말", type=int, default=13159)
    parser.add_argument("--highlight_socket_port", help="높이프로그램단말", type=int, default=11001)

    args = parser.parse_args()

    # 
    logger.debug(f"ws start {args}")
    service_context = ServiceContext(
        port=args.port,
        highlight_socket_port=args.highlight_socket_port,
        route_port=args.route_port,
    )

    # 시작ws서비스
    ws = WsServer(svc=service_context, port=args.port)
    thread_ws = threading.Thread(target=ws.server, args=(), daemon=True)
    thread_ws.start()

    # 시작선택서비스
    from astronverse.picker.server.picker_server import PickerServer

    threading.Thread(target=service_context.load_modules, args=(), daemon=True).start()

    service_context.pick_server = PickerServer(service_context)
    service_context.pick_server.server()