import asyncio
import fnmatch
import os
import time
from pathlib import Path
from typing import Union

from astronverse.trigger.core.logger import logger
from watchfiles import Change, awatch

create_flag = "create"
delete_flag = "delete"
modified_flag = "update"
rename_flag = "rename"


class FileTask:
    def __init__(
        self,
        directory: str = ".",
        relative_sub_path: bool = False,
        events: list[str] = None,
        files_or_type: Union[list[str], None] = None,
        **kwargs,
    ):
        """
        생성파일조회의유형

        directory: `str`, 목록 폴더
        relative_sub_path: `bool`, 여부패키지경로
        events: `List[str]`, 파일유형, 요소지원['create', 'delete', 'update', 'rename']
        files_or_type: `Union[List[str], None]`, 단일개파일통신또는매칭기호

        Kwargs: 해당매개변수사용생성작업의매개변수상태

        """
        self.directory = directory
        self.relative_sub_path = relative_sub_path
        self.events = events
        self.files_or_type = files_or_type or []

    def _match_file(self, filepath: str) -> bool:
        filename = os.path.basename(filepath)
        for pattern in self.files_or_type:
            if filename == pattern or fnmatch.fnmatch(filename, pattern):
                return True
        return False

    async def callback(self, q: asyncio.Queue, run_event: asyncio.Event):
        """
        조회돌아가기조정
        """
        async for changes in awatch(
            self.directory, recursive=self.relative_sub_path, debounce=500
        ):  # debounce500ms여부가능일
            if run_event.is_set():
                continue

            deleted_paths = set()
            added_paths = set()
            modified_paths = set()
            final_modified = set()
            renamed_pairs = []
            current_time = time.time()

            for change_type, path in changes:
                logger.info(f"[AsyncFileTask callback]파일변수: {change_type} {path}")
                path_obj = Path(path)
                if path_obj.is_dir():  # 디렉터리의파일(예디렉터리MODIFIED)
                    continue

                if change_type == Change.deleted:  # delete파일, 이면에서의합치기추가
                    deleted_paths.add(path)
                elif change_type == Change.added:  # added파일, 이면에서의합치기추가
                    added_paths.add(path)
                elif change_type == Change.modified:  # modified파일, 이면에서의합치기추가
                    if path_obj.exists():  # 인증경로있음(삭제됨파일파일)
                        modified_paths.add((path, current_time))

            # 매칭이름 변경파일(일DELETED+ADDED매칭)
            for deleted_path in list(deleted_paths):
                for added_path in list(added_paths):
                    if (
                        Path(deleted_path).parent == Path(added_path).parent
                        and Path(deleted_path).name != Path(added_path).name
                    ):
                        renamed_pairs.append((deleted_path, added_path))
                        deleted_paths.remove(deleted_path)
                        added_paths.remove(added_path)
                        break

            # 관리파일(필터링높이MODIFIED)
            for path, timestamp in modified_paths:
                if current_time - timestamp < 0.5:
                    final_modified.add(path)

            # 의선택파일
            if (
                (create_flag in self.events and added_paths)
                or (delete_flag in self.events and deleted_paths)
                or (rename_flag in self.events and renamed_pairs)
                or (modified_flag in self.events and final_modified)
            ):
                await q.put(True)
                continue