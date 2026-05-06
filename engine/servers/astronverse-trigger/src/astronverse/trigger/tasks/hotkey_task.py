import asyncio

from astronverse.trigger.core.logger import logger
from keyboard import add_hotkey, remove_hotkey


class HotKeyTask:
    def __init__(self, shortcuts: list = None, **kwargs):
        """
        생성의유형

        shortcuts: `List`, 빠름그룹합치기

        Kwargs: 해당매개변수사용생성작업의매개변수상태
        """
        self.shortcuts = shortcuts
        self._h_handle = None  # 사용제어파일의객체

    async def callback(self, q: asyncio.Queue, run_event: asyncio.Event):
        async def handle_hotkey():
            logger.debug("handle_hotkey: enqueue True to asyncio.Queue")
            await q.put(True)
            try:
                logger.debug(f"handle_hotkey: queue size after put -> {q.qsize()}")
            except Exception:
                pass

        def on_hotkey_press(loop):
            """loop필요사용현재파일, 에서일시간아래행작업실행"""
            try:
                is_set = run_event.is_set()
            except Exception as e:
                logger.exception(f"on_hotkey_press: failed to read run_event.is_set(): {e}")
                is_set = False
            logger.debug(f"on_hotkey_press: hotkey '{hotkey_expression}' pressed, run_event.is_set={is_set}")
            if is_set:
                logger.info("on_hotkey_press: run_event is set; ignore hotkey")
                return
            loop.call_soon_threadsafe(asyncio.create_task, handle_hotkey())
            logger.debug("on_hotkey_press: scheduled handle_hotkey on event loop")

        loop = asyncio.get_running_loop()
        hotkey_expression = "+".join(self.shortcuts)
        logger.info(f"Registering hotkey '{hotkey_expression}' via keyboard.add_hotkey")
        self._h_handle = add_hotkey(hotkey_expression, on_hotkey_press, args=(loop,))

    def force_end_callback(self):
        """해당방법법행작업돌아가기"""
        logger.info("force_end_callback: removing hotkey listener")
        remove_hotkey(self._h_handle)