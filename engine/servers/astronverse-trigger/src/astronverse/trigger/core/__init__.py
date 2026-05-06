import argparse

from astronverse.trigger.core.config import config
from astronverse.trigger.core.logger import logger
from astronverse.trigger.server.server import app

if __name__ == "__main__":
    # 가져오기매칭
    parser = argparse.ArgumentParser(description="{} service".format("executor"))
    parser.add_argument("--port", default="8087", help="[시스템매칭]본단말", required=False)
    parser.add_argument("--gateway_port", default="13159", help="[시스템매칭]네트워크닫기단말", required=False)
    parser.add_argument("--terminal_mode", default="n", help="[시스템매칭]여부로스케줄링방식", required=False)
    parser.add_argument("--terminal_id", help="[시스템매칭]스케줄링방식단말ID", required=True)
    args = parser.parse_args()

    logger.info("start trigger {}".format(args))

    config.PORT = args.port
    config.GATEWAY_PORT = args.gateway_port
    config.TERMINAL_MODE = True if args.terminal_mode == "y" else False
    config.TERMINAL_ID = args.terminal_id

    # 시작서비스
    import uvicorn

    uvicorn.run(app, host="127.0.0.1", port=int(args.port))