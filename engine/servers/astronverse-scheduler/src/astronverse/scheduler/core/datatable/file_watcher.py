import asyncio
import os
import time
from collections.abc import AsyncGenerator, Callable
from typing import Optional

from astronverse.scheduler.logger import logger
from watchdog.events import FileSystemEvent, FileSystemEventHandler
from watchdog.observers import Observer


class ExcelFileHandler(FileSystemEventHandler):
    """Excel 파일변수변경관리기기"""

    def __init__(
        self,
        target_file: str,
        on_modified: Optional[Callable[[str], None]] = None,
        on_deleted: Optional[Callable[[str], None]] = None,
        debounce_seconds: float = 0.5,
    ):
        """
        파일변수변경관리기기

        Args:
            target_file: 필요의목록파일 경로
            on_modified: 파일수정시의돌아가기조정데이터
            on_deleted: 파일삭제시의돌아가기조정데이터
            debounce_seconds: 시간(초), 짧음시간내부재복사트리거
        """
        super().__init__()
        # Windows 경로아니오분크기, 시스템일변환로소
        self.target_file = os.path.normpath(target_file).lower()
        self._on_modified_callback = on_modified
        self._on_deleted_callback = on_deleted
        self.debounce_seconds = debounce_seconds

        # 제어
        self._last_modified_time = 0
        self._ignore_until = 0  # 사용입력가져오기 의변수변경

    def pause_watching(self, duration: float = 1.0):
        """
        일시중지일시간, 사용입력트리거의변수변경

        Args:
            duration: 일시중지시간(초)
        """
        self._ignore_until = time.time() + duration

    def _should_process(self, event_path: str) -> bool:
        """조회여부해당관리파일"""
        # Windows 경로아니오분크기, 시스템일변환로소
        normalized_path = os.path.normpath(event_path).lower()

        # 조회여부예목록파일
        if normalized_path != self.target_file:
            return False

        # 조회여부에서
        if time.time() < self._ignore_until:
            logger.debug(f"Ignoring event during pause period: {event_path}")
            return False

        return True

    def _debounce_check(self) -> bool:
        """조회"""
        current_time = time.time()
        if current_time - self._last_modified_time < self.debounce_seconds:
            return False
        self._last_modified_time = current_time
        return True

    def on_modified(self, event: FileSystemEvent):
        """파일수정파일"""
        if event.is_directory:
            return

        if not self._should_process(event.src_path):
            return

        if not self._debounce_check():
            return

        logger.info(f"File modified: {event.src_path}")
        if self._on_modified_callback:
            self._on_modified_callback(event.src_path)

    def on_deleted(self, event: FileSystemEvent):
        """파일삭제파일"""
        if event.is_directory:
            return

        if not self._should_process(event.src_path):
            return

        logger.info(f"File deleted: {event.src_path}")
        if self._on_deleted_callback:
            self._on_deleted_callback(event.src_path)

    def on_created(self, event: FileSystemEvent):
        """파일생성파일 - Excel 저장시가능삭제생성"""
        if event.is_directory:
            return

        if not self._should_process(event.src_path):
            return

        if not self._debounce_check():
            return

        logger.info(f"File created (treated as modified): {event.src_path}")
        if self._on_modified_callback:
            self._on_modified_callback(event.src_path)

    def on_moved(self, event: FileSystemEvent):
        """파일/이름 변경파일 - Excel 저장시가능사용시파일이름 변경"""
        if event.is_directory:
            return

        # 조회목록경로여부예의파일
        dest_path = getattr(event, "dest_path", None)
        if dest_path and os.path.normpath(dest_path).lower() == self.target_file:
            if not self._debounce_check():
                return

            logger.info(f"File moved to target (treated as modified): {dest_path}")
            if self._on_modified_callback:
                self._on_modified_callback(dest_path)


class FileWatcher:
    """파일기기"""

    def __init__(self):
        """파일기기"""
        self._observers: dict[str, Observer] = {}
        self._handlers: dict[str, ExcelFileHandler] = {}

    def start_watching(
        self,
        file_path: str,
        on_modified: Optional[Callable[[str], None]] = None,
        on_deleted: Optional[Callable[[str], None]] = None,
    ) -> bool:
        """
        열기 지정파일

        Args:
            file_path: 필요의파일 경로
            on_modified: 파일수정시의돌아가기조정데이터
            on_deleted: 파일삭제시의돌아가기조정데이터

        Returns:
            여부성공열기 
        """
        file_path = os.path.normpath(file_path)

        # 결과가완료에서, 중지
        if file_path in self._observers:
            self._stop_watching_internal(file_path)

        # 가져오기파일에서디렉터리
        watch_dir = os.path.dirname(file_path)
        if not watch_dir:
            watch_dir = "."

        if not os.path.exists(watch_dir):
            logger.error(f"Watch directory does not exist: {watch_dir}")
            return False

        # 생성파일관리기기
        handler = ExcelFileHandler(
            target_file=file_path,
            on_modified=on_modified,
            on_deleted=on_deleted,
        )

        # 생성시작
        observer = Observer()
        observer.schedule(handler, watch_dir, recursive=False)
        observer.start()

        self._observers[file_path] = observer
        self._handlers[file_path] = handler

        logger.info(f"Started watching file: {file_path}")
        return True

    def stop_watching(self, file_path: str) -> bool:
        """
        중지지정파일

        Args:
            file_path: 파일 경로

        Returns:
            여부성공중지
        """
        file_path = os.path.normpath(file_path)
        return self._stop_watching_internal(file_path)

    def _stop_watching_internal(self, file_path: str) -> bool:
        """내부모듈방법법: 중지"""
        if file_path not in self._observers:
            return False

        observer = self._observers.pop(file_path)
        self._handlers.pop(file_path, None)

        observer.stop()
        observer.join(timeout=2)

        logger.info(f"Stopped watching file: {file_path}")
        return True

    def pause_watching(self, file_path: str, duration: float = 1.0):
        """
        일시중지지정파일일시간

        Args:
            file_path: 파일 경로
            duration: 일시중지시간(초)
        """
        file_path = os.path.normpath(file_path)

        handler = self._handlers.get(file_path)
        if handler:
            handler.pause_watching(duration)

    def is_watching(self, file_path: str) -> bool:
        """
        조회여부정상에서지정파일

        Args:
            file_path: 파일 경로

        Returns:
            여부정상에서
        """
        file_path = os.path.normpath(file_path)
        return file_path in self._observers

    def stop_all(self):
        """중지모든"""
        for file_path in list(self._observers.keys()):
            self._stop_watching_internal(file_path)

        logger.info("Stopped all file watchers")


class AsyncFileWatcher:
    """예외파일기기, 사용 SSE 방식"""

    def __init__(self, file_path: str, debounce_delay: float = 0.5):
        """
        예외파일기기

        Args:
            file_path: 필요의파일 경로
            debounce_delay: 지연시간(초), 파일수정완료후대기시간트리거파일
        """
        self.file_path = os.path.normpath(file_path)
        self._queue: asyncio.Queue = asyncio.Queue()
        self._observer: Optional[Observer] = None
        self._handler: Optional[ExcelFileHandler] = None
        self._running = False
        self._ignore_until = 0
        self._debounce_delay = debounce_delay
        self._pending_task: Optional[asyncio.Task] = None
        self._event_loop: Optional[asyncio.AbstractEventLoop] = None

    def pause_watching(self, duration: float = 1.0):
        """
        일시중지일시간

        Args:
            duration: 일시중지시간(초)
        """
        self._ignore_until = time.time() + duration
        if self._handler:
            self._handler.pause_watching(duration)

    async def start(self) -> AsyncGenerator[dict]:
        """
        시작예외완료파일

        Yields:
            파일변수변경파일딕셔너리
        """
        if self._running:
            return

        self._running = True
        self._event_loop = asyncio.get_event_loop()

        # 지연트리거작업
        async def _delayed_trigger(event_data: dict):
            """지연트리거파일, 대기시간후입력큐"""
            try:
                await asyncio.sleep(self._debounce_delay)
                # 조회여부에서실행상태
                if self._running:
                    try:
                        self._queue.put_nowait(event_data)
                    except asyncio.QueueFull:
                        logger.warning("Event queue is full, dropping event")
            except asyncio.CancelledError:
                # 작업가져오기 예보통의, 아니오필요기록
                pass

        # 생성돌아가기조정데이터
        def on_modified(path: str):
            """파일수정돌아가기조정 - 사용지연트리거기기제어"""
            if time.time() < self._ignore_until:
                return

            # 가져오기 전의지연작업
            if self._pending_task and not self._pending_task.done():
                self._pending_task.cancel()

            # 생성새의지연작업
            event_data = {"type": "file_changed", "path": path}
            self._pending_task = self._event_loop.create_task(_delayed_trigger(event_data))

        def on_deleted(path: str):
            """파일삭제돌아가기조정 - 트리거, 아니오필요지연"""
            try:
                self._queue.put_nowait({"type": "file_deleted", "path": path})
            except asyncio.QueueFull:
                logger.warning("Event queue is full, dropping delete event")

        # 가져오기파일에서디렉터리
        watch_dir = os.path.dirname(self.file_path)
        if not watch_dir:
            watch_dir = "."

        # 생성파일관리기기
        # 비고: ExcelFileHandler 의시간로 0.1 초, 필요사용적음파일
        # 정상의지연트리거 AsyncFileWatcher 의지연기기제어관리
        self._handler = ExcelFileHandler(
            target_file=self.file_path,
            on_modified=on_modified,
            on_deleted=on_deleted,
            debounce_seconds=0.1,  # 짧음시간, 필터링후일개파일
        )

        # 생성시작
        self._observer = Observer()
        self._observer.schedule(self._handler, watch_dir, recursive=False)
        self._observer.start()

        logger.info(f"AsyncFileWatcher started for: {self.file_path}")

        try:
            while self._running:
                try:
                    # 대기파일, 시간 초과으로지정조회실행상태
                    event = await asyncio.wait_for(self._queue.get(), timeout=1.0)
                    yield event
                except TimeoutError:
                    # 전송보관연결
                    yield {"type": "heartbeat"}
                except Exception as e:
                    logger.error(f"Error in AsyncFileWatcher: {e}")
                    break
        finally:
            self.stop()

    def stop(self):
        """중지"""
        self._running = False

        # 가져오기 대기관리의지연작업
        if self._pending_task and not self._pending_task.done():
            self._pending_task.cancel()

        if self._observer:
            self._observer.stop()
            self._observer.join(timeout=2)
            self._observer = None

        self._handler = None
        self._event_loop = None
        logger.info(f"AsyncFileWatcher stopped for: {self.file_path}")


# 전체영역파일기기
_file_watcher: Optional[FileWatcher] = None


def get_file_watcher() -> FileWatcher:
    """가져오기전체영역파일기기"""
    global _file_watcher
    if _file_watcher is None:
        _file_watcher = FileWatcher()
    return _file_watcher