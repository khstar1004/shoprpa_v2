import argparse
import time
import traceback
from pathlib import Path
import uvicorn
from astronverse.scheduler.logger import logger
from astronverse.baseline.config.config import load_config
from astronverse.scheduler.apis import route
from astronverse.scheduler.config import Config
from astronverse.scheduler.core.schduler.init import linux_env_check, win_env_check
from astronverse.scheduler.core.server import ServerManager
from astronverse.scheduler.core.servers.async_server import (
    CheckPickProcessAliveServer,
    CheckStartPidExitsServer,
    RpaSchedulerAsyncServer,
    TerminalAsyncServer,
)
from astronverse.scheduler.core.servers.core_server import (
    RpaBrowserConnectorServer,
    RpaRouteServer,
)
from astronverse.scheduler.core.setup.setup import Process
from astronverse.scheduler.core.svc import get_svc
from astronverse.scheduler.utils.utils import check_port
from fastapi import FastAPI

# 0. app, 
app = FastAPI()
route.handler(app)


def start(args):
    try:
        # 2. 가져오기매칭, 파싱까지위아래문서
        conf_path = Path(args.conf.strip('"').replace("\\\\", "\\")).resolve()
        conf_data = load_config(conf_path)

        Config.conf_file = conf_path
        Config.remote_addr = conf_data.get("remote_addr")
        svc = get_svc()
        svc.set_config(Config)

        # 3. 감지
        Process.kill_all_zombie()
        win_env_check(svc)
        linux_env_check()

        # 4. 서비스회원가입및시작
        server_mg = ServerManager(svc)
        server_mg.register(RpaRouteServer(svc))
        server_mg.register(RpaBrowserConnectorServer(svc))
        server_mg.register(RpaSchedulerAsyncServer(svc))
        server_mg.register(TerminalAsyncServer(svc))
        server_mg.register(CheckPickProcessAliveServer(svc))
        server_mg.register(CheckStartPidExitsServer(svc))
        server_mg.register(svc.trigger_server)
        if svc.vnc_server:
            server_mg.register(svc.vnc_server)
        server_mg.run()

        # 5. 대기본네트워크닫기로드완료, 회원가입서비스
        while check_port(port=svc.rpa_route_port):  # 대기본경로 로드완료
            time.sleep(0.1)
        svc.route_server_is_start = True
        svc.register_server()

        # 6. 프론트엔드전송완료완료메시지, 까지완료 startup 방법법

        # 7. 시작서비스
        uvicorn.run(
            app="astronverse.scheduler.start:app",
            host="0.0.0.0",
            port=svc.scheduler_port,
            workers=1,
        )
    except Exception as e:
        logger.error("astronverse.scheduler error: {} traceback: {}".format(e, traceback.format_exc()))