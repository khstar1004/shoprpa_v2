import os
import sys

from astronverse.actionlib import AtomicFormType, AtomicFormTypeMeta, DynamicsItem
from astronverse.actionlib.atomic import atomicMg
from astronverse.system import *
from astronverse.system.core.clipboard_core import IClipBoardCore
from astronverse.system.error import *
from astronverse.system.utils import folder_is_exists

if sys.platform == "win32":
    from astronverse.system.core.clipboard_core_win import ClipBoardCore
else:
    from astronverse.system.core.clipboard_core_linux import ClipBoardCore

ClipBoardCore: IClipBoardCore = ClipBoardCore()


class Clipboard:
    @staticmethod
    @atomicMg.atomic(
        "System",
        inputList=[
            atomicMg.param("content_type"),
            atomicMg.param(
                "message",
                formType=AtomicFormTypeMeta(AtomicFormType.INPUT_VARIABLE_PYTHON.value),
                required=True,
                dynamics=[
                    DynamicsItem(
                        key="$this.message.show",
                        expression="return $this.content_type.value == '{}'".format(ContentType.MSG.value),
                    )
                ],
            ),
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "file"},
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.file_path.show",
                        expression="return $this.content_type.value == '{}'".format(ContentType.FILE.value),
                    )
                ],
            ),
            atomicMg.param(
                "folder_path",
                formType=AtomicFormTypeMeta(
                    AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "folder"},
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.folder_path.show",
                        expression="return $this.content_type.value == '{}'".format(ContentType.FOLDER.value),
                    )
                ],
            ),
        ],
    )
    def copy_clip(
        content_type: ContentType = ContentType.MSG,
        message: str = "",
        file_path: str = "",
        folder_path: str = "",
    ):
        """
        클립보드에 복사
        """
        if content_type == ContentType.MSG:
            if message == "":
                raise BaseException(MSG_EMPTY_FORMAT, "요청다시 입력대기복사내용")
            ClipBoardCore.copy_str_clip(message)
        elif content_type == ContentType.FILE:
            if not os.path.isfile(file_path):
                raise BaseException(
                    FILE_PATH_ERROR_FORMAT.format(file_path),
                    "지정파일찾을 수 없습니다, 확인하세요파일 경로!",
                )
            ClipBoardCore.copy_file_clip(file_path)
        elif content_type == ContentType.FOLDER:
            if not folder_is_exists(folder_path):
                raise BaseException(
                    FILE_PATH_ERROR_FORMAT.format(folder_path),
                    "지정폴더찾을 수 없습니다, 확인하세요파일 경로!",
                )
            ClipBoardCore.copy_file_clip(folder_path)
        else:
            raise NotImplementedError()

    @staticmethod
    @atomicMg.atomic("System")
    def clear_clip():
        """
        빈잘라내기
        """
        ClipBoardCore.clear_clip()

    @staticmethod
    @atomicMg.atomic(
        "System",
        inputList=[
            atomicMg.param("content_type"),
            atomicMg.param(
                "dst_path",
                formType=AtomicFormTypeMeta(
                    AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "folder"},
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.dst_path.show",
                        expression="return ['{}', '{}'].includes($this.content_type.value)".format(
                            ContentType.FILE.value, ContentType.FOLDER.value
                        ),
                    )
                ],
                required=True,
            ),
            atomicMg.param(
                "state_type",
                dynamics=[
                    DynamicsItem(
                        key="$this.state_type.show",
                        expression="return ['{}', '{}'].includes($this.content_type.value)".format(
                            ContentType.FOLDER.value, ContentType.FILE.value
                        ),
                    )
                ],
                required=False,
            ),
            atomicMg.param(
                "dst_file_name",
                formType=AtomicFormTypeMeta(AtomicFormType.INPUT_VARIABLE_PYTHON.value),
                dynamics=[
                    DynamicsItem(
                        key="$this.dst_file_name.show",
                        expression="return $this.content_type.value == '{}'".format(ContentType.FILE.value),
                    )
                ],
                required=False,
            ),
            atomicMg.param(
                "dst_folder_name",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.dst_folder_name.show",
                        expression="return $this.content_type.value == '{}'".format(ContentType.FOLDER.value),
                    )
                ],
                required=False,
            ),
        ],
        outputList=[
            atomicMg.param("output_content", types="Str"),
        ],
    )
    def paste_clip(
        content_type: ContentType = ContentType.MSG,
        dst_path: str = "",
        state_type: StateType = StateType.ERROR,
        dst_file_name: str = "",
        dst_folder_name: str = "",
    ):
        """
        가져오기잘라내기내용
        """
        if content_type == ContentType.MSG:
            output_content = ClipBoardCore.paste_str_clip()
        elif content_type == ContentType.FILE:
            src_path = ClipBoardCore.paste_file_clip()
            if not os.path.isfile(src_path):
                raise BaseException(
                    FILE_PATH_ERROR_FORMAT.format(src_path),
                    "잘라내기중찾을 수 없습니다대기가져오기파일, 확인하세요잘라내기내용 가져오기유형여부정상",
                )
            if not folder_is_exists(dst_path):
                if state_type == StateType.ERROR:
                    raise BaseException(
                        FOLDER_PATH_ERROR_FORMAT.format(dst_path),
                        "지정폴더 경로찾을 수 없습니다, 확인하세요경로정보",
                    )
                elif state_type == StateType.CREATE:
                    os.makedirs(dst_path, exist_ok=True)
                else:
                    raise NotImplementedError()

            base_name = os.path.basename(src_path)
            if not dst_file_name:
                dst_name = base_name
            else:
                if "." in base_name:
                    prefix = os.path.splitext(base_name)[1]
                    dst_name = "".join([dst_file_name, prefix])
                else:
                    raise BaseException(
                        FILE_TYPE_ERROR_FORMAT.format(base_name),
                        "파일이름실패, 확인하세요파일이름여부정상!",
                    )
            dst_path = os.path.join(dst_path, dst_name)
            if src_path != dst_path:
                import shutil

                shutil.copyfile(src_path, dst_path)
            output_content = dst_path

        elif content_type == ContentType.FOLDER:
            src_path = ClipBoardCore.paste_file_clip()
            if not folder_is_exists(src_path):
                raise BaseException(
                    FOLDER_PATH_ERROR_FORMAT.format(src_path),
                    "잘라내기중찾을 수 없습니다대기가져오기폴더, 확인하세요잘라내기내용 가져오기유형여부정상",
                )
            if not folder_is_exists(dst_path):
                if state_type == StateType.ERROR:
                    raise BaseException(
                        FOLDER_PATH_ERROR_FORMAT.format(dst_path),
                        "지정폴더 경로찾을 수 없습니다, 확인하세요경로정보",
                    )
                elif state_type == StateType.CREATE:
                    os.makedirs(dst_path, exist_ok=True)
                else:
                    raise NotImplementedError()
            if not dst_folder_name:
                path = src_path.rstrip(os.sep)
                dst_folder_name = os.path.basename(path)

            dst_path = os.path.join(dst_path, dst_folder_name)
            if src_path != dst_path:
                import shutil

                shutil.copytree(src_path, dst_path)
            output_content = dst_path
        elif content_type == ContentType.HTML:
            output_content = ClipBoardCore.paste_html_clip()
        else:
            raise NotImplementedError()
        return output_content