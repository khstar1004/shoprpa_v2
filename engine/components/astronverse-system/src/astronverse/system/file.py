import re
import time

from astronverse.actionlib import AtomicFormType, AtomicFormTypeMeta, AtomicLevel, DynamicsItem
from astronverse.actionlib.atomic import atomicMg
from astronverse.system import *
from astronverse.system.error import *
from astronverse.system.utils import *


class File:
    @staticmethod
    @atomicMg.atomic(
        "File",
        inputList=[
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "file"},
                ),
                required=True,
            ),
            atomicMg.param("exist_type", required=False),
        ],
    )
    def file_exist(file_path: str = "", exist_type: ExistType = ExistType.EXIST) -> bool:
        """
        파일여부존재함
        """

        if exist_type == ExistType.EXIST:
            return os.path.isfile(file_path)
        elif exist_type == ExistType.NOT_EXIST:
            return not os.path.isfile(file_path)
        else:
            raise NotImplementedError()

    @staticmethod
    @atomicMg.atomic(
        "File",
        inputList=[
            atomicMg.param(
                "dst_path",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "folder"},
                ),
                required=True,
            ),
            atomicMg.param(
                "file_name",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON.value,
                    params={"size": "middle"},
                ),
                required=True,
            ),
            atomicMg.param(
                "exist_options",
                formType=AtomicFormTypeMeta(type=AtomicFormType.SELECT.value),
                required=False,
            ),
        ],
        outputList=[
            atomicMg.param(
                "new_file_path",
                types="Str",
                formType=AtomicFormTypeMeta(type=AtomicFormType.RESULT.value),
            ),
        ],
    )
    def file_create(
        dst_path: str = "",
        file_name: str = "",
        exist_options: OptionType = OptionType.GENERATE,
    ) -> str:
        """
        새생성파일, 지정목록경로및파일이름, 반환생성의파일 경로
        """
        if not folder_is_exists(dst_path):
            raise BaseException(
                FOLDER_PATH_ERROR_FORMAT.format(dst_path),
                "의디렉터리경로찾을 수 없습니다, 확인하세요목록경로!",
            )
        file_path = os.path.join(dst_path, file_name)
        if os.path.isfile(file_path):
            if exist_options == OptionType.OVERWRITE:
                os.remove(file_path)
            elif exist_options == OptionType.SKIP:
                return file_path
            elif exist_options == OptionType.GENERATE:
                file_path = generate_copy(dst_path, file_name)
            else:
                raise NotImplementedError()
        with open(file_path, "w", encoding="utf-8") as file:
            pass
        return file_path

    @staticmethod
    @atomicMg.atomic(
        "File",
        inputList=[
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "file"},
                ),
            ),
            atomicMg.param("delete_options", required=False),
        ],
        outputList=[
            atomicMg.param("delete_file_result", types="Str"),
        ],
    )
    def file_delete(file_path: str = "", delete_options: DeleteType = DeleteType.DELETE):
        """
        삭제지정파일
        """
        if not os.path.isfile(file_path):
            raise BaseException(FILE_PATH_ERROR_FORMAT.format(file_path), "파일찾을 수 없습니다, 확인하세요파일 경로!")
        if delete_options == DeleteType.DELETE:
            try:
                os.remove(file_path)
            except PermissionError as e:
                raise BaseException(
                    PermissionError_FORMAT.format(file_path),
                    "파일사용, 요청닫기파일후재시도!",
                )
        elif delete_options == DeleteType.TRASH:
            try:
                from send2trash import send2trash

                send2trash(file_path)
            except OSError as e:
                raise BaseException(
                    PermissionError_FORMAT.format(file_path),
                    "파일사용, 요청닫기파일후재시도!",
                )
        else:
            raise NotImplementedError()
        return True

    @staticmethod
    @atomicMg.atomic(
        "File",
        inputList=[
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "file"},
                ),
            ),
            atomicMg.param(
                "target_path",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "folder"},
                ),
            ),
            atomicMg.param(
                "file_name",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON.value,
                    params={"size": "middle"},
                ),
                required=False,
            ),
            atomicMg.param(
                "copy_options",
                formType=AtomicFormTypeMeta(type=AtomicFormType.SELECT.value),
                required=False,
            ),
        ],
        outputList=[
            atomicMg.param("copy_file_path", types="Str"),
        ],
    )
    def file_copy(
        file_path: str = "",
        target_path: str = "",
        state_type: StateType = StateType.ERROR,
        file_name: str = "",
        copy_options: OptionType = OptionType.GENERATE,
    ) -> str:
        """
        복사파일까지지정디렉터리
        """
        if not os.path.isfile(file_path):
            raise BaseException(FILE_PATH_ERROR_FORMAT.format(file_path), "파일찾을 수 없습니다, 확인하세요파일 경로!")
        if not folder_is_exists(target_path):
            if state_type == StateType.ERROR:
                raise BaseException(
                    FOLDER_PATH_ERROR_FORMAT.format(target_path),
                    "지정디렉터리있음오류, 확인하세요경로정보!",
                )
            elif state_type == StateType.CREATE:
                os.makedirs(target_path, exist_ok=True)
            else:
                raise NotImplementedError()

        base_name = os.path.basename(file_path)

        if not file_name:
            file_name = base_name
        else:
            if "." in base_name:
                prefix = os.path.splitext(file_path)[1]
                file_name = "".join([file_name, prefix])
            else:
                raise BaseException(
                    FILE_TYPE_ERROR_FORMAT.format(base_name),
                    "파일이름실패, 확인하세요파일이름여부정상!",
                )

        file_copy_path = os.path.join(target_path, file_name)

        if os.path.isfile(file_copy_path):
            if copy_options == OptionType.OVERWRITE:
                os.remove(file_copy_path)
            elif copy_options == OptionType.SKIP:
                return file_copy_path
            elif copy_options == OptionType.GENERATE:
                file_copy_path = generate_copy(target_path, file_name)
            else:
                raise NotImplementedError()

        if file_path != file_copy_path:
            import shutil

            shutil.copyfile(file_path, file_copy_path)
        return file_copy_path

    @staticmethod
    @atomicMg.atomic(
        "File",
        inputList=[
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "file"},
                ),
            ),
            atomicMg.param(
                "msg",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON.value,
                    params={"size": "middle"},
                ),
            ),
            atomicMg.param("file_option", required=False),
            atomicMg.param("write_type", required=False),
            atomicMg.param("encode_type", required=False),
        ],
        outputList=[
            atomicMg.param("write_file_path", types="Str"),
        ],
    )
    def file_write(
        file_path: str = "",
        file_option: StateType = StateType.ERROR,
        msg: str = "",
        write_type: WriteType = WriteType.APPEND,
        encode_type: EncodeType = EncodeType.DEFAULT,
    ) -> str:
        """
        지정파일중입력내용
        """
        SUPPORT_FORMAT = [".txt", ".docx", ".md", ".py", ".json", ".csv", ".html"]
        if not os.path.isfile(file_path):
            if file_option == StateType.CREATE:
                with open(file_path, "w", encoding="utf-8") as file:
                    pass
            elif file_option == StateType.ERROR:
                raise BaseException(
                    FILE_PATH_ERROR_FORMAT.format(file_path),
                    "목록파일찾을 수 없습니다, 확인하세요파일 경로!",
                )
            else:
                raise NotImplementedError()

        file_ext = os.path.splitext(file_path)[1]
        if file_ext not in SUPPORT_FORMAT:
            raise BaseException(
                READ_TYPE_ERROR_FORMAT.format(file_path, SUPPORT_FORMAT),
                "현재파일형식지원하지 않음내용 가져오기, 요청다시 선택",
            )

        if encode_type == EncodeType.DEFAULT:
            encoding = get_file_encoding_type(file_path)
        elif encode_type in [
            EncodeType.ANSI,
            EncodeType.UTF8,
            EncodeType.UTF16,
            EncodeType.UTF_16_BE,
            EncodeType.GB2312,
            EncodeType.GBK,
            EncodeType.GB18030,
        ]:
            encoding = encode_type.value
        else:
            raise NotImplementedError()

        if write_type == WriteType.OVERWRITE:
            with open(file_path, "w", encoding=encoding) as file:
                file.write(msg)

        elif write_type == WriteType.APPEND:
            with open(file_path, "a", encoding=encoding) as file:
                file.write(msg)
        else:
            raise NotImplementedError()
        return file_path

    @staticmethod
    @atomicMg.atomic(
        "File",
        inputList=[
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "file"},
                ),
            ),
        ],
        outputList=[
            atomicMg.param("file_encoding_type", types="Str"),
        ],
    )
    def get_file_encoding_type(file_path: str = "") -> str:
        """
        가져오기파일코드유형
        :param file_path: 파일 경로
        :return: file_encoding_type: 코드유형
        """
        if not os.path.isfile(file_path):
            raise BaseException(FILE_PATH_ERROR_FORMAT.format(file_path), "파일찾을 수 없습니다, 확인하세요경로정보!")
        file_encoding_type = get_file_encoding_type(file_path)
        return file_encoding_type

    @staticmethod
    @atomicMg.atomic(
        "File",
        inputList=[
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "file"},
                ),
            ),
            atomicMg.param(
                "read_type",
                formType=AtomicFormTypeMeta(type=AtomicFormType.SELECT.value),
                required=False,
            ),
            atomicMg.param("encode_type", required=False),
        ],
        outputList=[
            atomicMg.param("read_file_content", types="Any"),
        ],
    )
    def file_read(
        file_path: str = "",
        read_type: ReadType = ReadType.ALL,
        encode_type: EncodeType = EncodeType.DEFAULT,
    ):
        """
        가져오기파일내용
        """
        SUPPORT_FORMAT = [".txt", ".docx", ".md", ".py", ".json", ".csv"]
        if not os.path.isfile(file_path):
            raise BaseException(FILE_PATH_ERROR_FORMAT.format(file_path), "파일찾을 수 없습니다, 확인하세요경로정보!")

        file_ext = os.path.splitext(file_path)[1]
        if not file_ext or file_ext not in SUPPORT_FORMAT:
            raise BaseException(
                READ_TYPE_ERROR_FORMAT.format(file_path, SUPPORT_FORMAT),
                "현재파일형식지원하지 않음내용 가져오기, 요청다시 선택",
            )
        if encode_type == EncodeType.DEFAULT:
            encoding = get_file_encoding_type(file_path)
        elif encode_type in [
            EncodeType.ANSI,
            EncodeType.UTF8,
            EncodeType.UTF16,
            EncodeType.UTF_16_BE,
            EncodeType.GB2312,
            EncodeType.GBK,
            EncodeType.GB18030,
        ]:
            encoding = encode_type.value
        else:
            raise NotImplementedError()

        try:
            if read_type == ReadType.ALL:
                with open(file_path, encoding=encoding) as f:
                    read_file_content = f.read()
            elif read_type == ReadType.List:
                with open(file_path, encoding=encoding) as f:
                    read_file_content = f.readlines()
                    read_file_content = [line.rstrip("\r\n") for line in read_file_content]
            elif read_type == ReadType.BYTE:
                with open(file_path, "rb") as f:
                    read_file_content = f.read()
            else:
                raise NotImplementedError()
        except UnicodeError as e:
            encode_type_file = get_file_encoding_type(file_path)
            raise BaseException(
                ENCODE_TYPE_ERROR_FORMAT.format(encode_type_file, encode_type),
                "지정의코드유형출력오류, 확인하세요코드유형",
            )

        return read_file_content

    @staticmethod
    @atomicMg.atomic(
        "File",
        inputList=[
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "file"},
                ),
            ),
            atomicMg.param(
                "target_folder",
                formType=AtomicFormTypeMeta(
                    AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "folder"},
                ),
            ),
            atomicMg.param(
                "file_name",
                formType=AtomicFormTypeMeta(AtomicFormType.INPUT_VARIABLE_PYTHON.value),
                required=False,
            ),
            atomicMg.param(
                "exist_options",
                formType=AtomicFormTypeMeta(type=AtomicFormType.SELECT.value),
                required=False,
            ),
        ],
        outputList=[
            atomicMg.param("move_file_path", types="Str"),
        ],
    )
    def file_move(
        file_path: str = "",
        target_folder: str = "",
        state_type: StateType = StateType.ERROR,
        file_name: str = "",
        exist_options: OptionType = OptionType.GENERATE,
    ) -> str:
        """
        파일까지목록 폴더.
        """
        if not os.path.isfile(file_path):
            raise BaseException(FILE_PATH_ERROR_FORMAT.format(file_path), "파일찾을 수 없습니다, 확인하세요파일 경로")
        if not folder_is_exists(target_folder):
            if state_type == StateType.CREATE:
                os.makedirs(target_folder, exist_ok=True)
            elif state_type == StateType.ERROR:
                raise BaseException(
                    FOLDER_PATH_ERROR_FORMAT.format(target_folder),
                    "폴더찾을 수 없습니다, 확인하세요폴더 경로",
                )
            else:
                raise NotImplementedError()

        pre_file_name = os.path.basename(file_path)

        if not file_name:
            file_name = pre_file_name
        else:
            if "." in pre_file_name:
                prefix = os.path.splitext(file_path)[1]
                file_name = "".join([file_name, prefix])
            else:
                raise BaseException(
                    FILE_TYPE_ERROR_FORMAT.format(pre_file_name),
                    "파일이름실패, 확인하세요파일이름여부정상!",
                )

        target_path = os.path.join(target_folder, file_name)

        if os.path.isfile(target_path):
            if exist_options == OptionType.SKIP:
                return target_path
            elif exist_options == OptionType.GENERATE:
                target_path = generate_copy(target_folder, file_name)
            elif exist_options == OptionType.OVERWRITE:
                pass
            else:
                raise NotImplementedError()

        import shutil

        shutil.move(file_path, target_path)
        return target_path

    @staticmethod
    @atomicMg.atomic(
        "File",
        inputList=[
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "file"},
                ),
            ),
            atomicMg.param(
                "new_name",
                formType=AtomicFormTypeMeta(AtomicFormType.INPUT_VARIABLE_PYTHON.value),
                required=True,
            ),
            atomicMg.param(
                "exist_options",
                formType=AtomicFormTypeMeta(type=AtomicFormType.SELECT.value),
                required=False,
            ),
        ],
        outputList=[
            atomicMg.param("rename_file_path", types="Str"),
        ],
    )
    def file_rename(
        file_path: str = "",
        new_name: str = "",
        exist_options: OptionType = OptionType.GENERATE,
    ) -> str:
        """
        파일이름 변경
        :param file_path: 이름 변경파일 경로
        :param new_name: 이름 변경이름,  아니오추가파일이름
        :param exist_options: 파일존재함시 덮어쓰기/건너뛰기/생성본
        :return: 이름 변경후파일 경로
        """
        if not os.path.isfile(file_path):
            raise BaseException(FILE_PATH_ERROR_FORMAT.format(file_path), "파일찾을 수 없습니다, 확인하세요파일 경로")

        if new_name == get_file_name_only(file_path):
            raise BaseException(
                RENAME_ERROR_FORMAT.format(new_name),
                "이름 변경이름및기존이름일, 확인하세요입력내용",
            )

        file_ext = os.path.splitext(file_path)[1]
        file_dir = os.path.dirname(file_path)
        new_name = new_name + file_ext
        rename_file_path = os.path.join(file_dir, new_name)

        if os.path.isfile(rename_file_path):
            if exist_options == OptionType.SKIP:
                return file_path
            elif exist_options == OptionType.GENERATE:
                rename_file_path = generate_copy(file_dir, new_name)
            elif exist_options == OptionType.OVERWRITE:
                os.remove(rename_file_path)
            else:
                raise NotImplementedError()

        os.rename(file_path, rename_file_path)
        return rename_file_path

    @staticmethod
    @atomicMg.atomic(
        "File",
        inputList=[
            atomicMg.param(
                "folder_path",
                formType=AtomicFormTypeMeta(
                    AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "folder"},
                ),
            ),
            atomicMg.param("find_type", required=True),
            atomicMg.param(
                "search_pattern",
                formType=AtomicFormTypeMeta(AtomicFormType.INPUT_VARIABLE_PYTHON.value),
                required=True,
            ),
            atomicMg.param("traverse_subfolder", level=AtomicLevel.ADVANCED.value, required=False),
        ],
        outputList=[
            atomicMg.param("find_file_result", types="List"),
        ],
    )
    def file_search(
        folder_path: str = "",
        find_type: SearchType = SearchType.FUZZY,
        search_pattern: str = "",
        traverse_subfolder: TraverseType = TraverseType.NO,
    ) -> list:
        """
        조회파일
        """
        if not folder_is_exists(folder_path):
            raise BaseException(
                FOLDER_PATH_ERROR_FORMAT.format(folder_path),
                "지정폴더디렉터리찾을 수 없습니다, 확인하세요경로정보!",
            )
        if not search_pattern:
            raise BaseException(
                MSG_EMPTY_FORMAT.format(search_pattern),
                "대기조회파일이름비어 있습니다, 확인하세요입력내용",
            )

        find_file_result = []

        for root, dirs, files in os.walk(folder_path, topdown=True):
            for file in files:
                if find_type == SearchType.EXACT and file == search_pattern:
                    find_file_result.append(os.path.join(root, file))
                if (
                    find_type == SearchType.FUZZY
                    and search_pattern in file
                    or find_type == SearchType.REGEX
                    and re.search(search_pattern, file)
                ):
                    find_file_result.append(os.path.join(root, file))

            if traverse_subfolder == TraverseType.YES:
                continue
            elif traverse_subfolder == TraverseType.NO:
                break
            else:
                raise NotImplementedError()

        return find_file_result

    @staticmethod
    @atomicMg.atomic(
        "File",
        inputList=[
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "file"},
                ),
            ),
            atomicMg.param("status_type", required=False),
            atomicMg.param("wait_time", types="Int", required=False),
        ],
        outputList=[
            atomicMg.param("wait_file_result", types="Str"),
        ],
    )
    def file_wait_status(
        file_path: str = "",
        status_type: StatusType = StatusType.CREATED,
        wait_time: int = 10,
    ) -> bool:
        """
        대기파일생성/삭제
        """
        start_time = time.time()
        file_status = os.path.isfile(file_path)
        if status_type == StatusType.DELETED and not file_status:
            raise BaseException(
                FILE_PATH_ERROR_FORMAT.format(file_path),
                "파일찾을 수 없습니다불가삭제, 확인하세요경로정보",
            )

        while time.time() - start_time <= wait_time:
            file_status = os.path.isfile(file_path)
            if (status_type == StatusType.CREATED and file_status) or (
                status_type == StatusType.DELETED and not file_status
            ):
                return True
            time.sleep(1)

        return False

    @staticmethod
    @atomicMg.atomic(
        "File",
        inputList=[
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"file_type": "file"},
                ),
            ),
            atomicMg.param("info_type", required=False),
        ],
        outputList=[
            atomicMg.param("file_info", types="Dict"),
        ],
    )
    def file_info(file_path: str = "", info_type: InfoType = InfoType.ALL) -> dict:
        """
        가져오기파일정보
        """
        if not os.path.isfile(file_path):
            raise BaseException(FILE_PATH_ERROR_FORMAT.format(file_path), "파일찾을 수 없습니다, 확인하세요경로정보")

        abs_path = os.path.abspath(file_path)
        file_info = {
            "abs_path": abs_path,
            "root": os.path.splitdrive(abs_path)[0],
            "directory": os.path.dirname(abs_path),
            "name_ext": os.path.basename(file_path),
            "name": get_file_name_only(abs_path),
            "extension": os.path.splitext(file_path)[1],
            "size": os.path.getsize(abs_path),
            "c_time": convert_time_format(os.path.getctime(abs_path)),
            "m_time": convert_time_format(os.path.getmtime(abs_path)),
        }

        if info_type == InfoType.ALL:
            return file_info
        elif str(info_type.value) in file_info:
            return file_info[info_type.value]
        else:
            raise NotImplementedError()

    @staticmethod
    @atomicMg.atomic(
        "File",
        inputList=[
            atomicMg.param(
                "folder_path",
                formType=AtomicFormTypeMeta(
                    AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "folder"},
                ),
            ),
            atomicMg.param(
                "excel_path",
                formType=AtomicFormTypeMeta(
                    AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "folder"},
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.excel_path.show",
                        expression="return $this.output_type.value == '{}'".format(OutputType.EXCEL.value),
                    )
                ],
                required=True,
            ),
            atomicMg.param(
                "state_type",
                required=False,
                dynamics=[
                    DynamicsItem(
                        key="$this.state_type.show",
                        expression="return $this.output_type.value == '{}'".format(OutputType.EXCEL.value),
                    )
                ],
            ),
            atomicMg.param(
                "excel_name",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.excel_name.show",
                        expression="return $this.output_type.value == '{}'".format(OutputType.EXCEL.value),
                    )
                ],
            ),
            atomicMg.param("output_type", required=False),
            atomicMg.param("sort_method", level=AtomicLevel.ADVANCED.value, required=False),
            atomicMg.param(
                "sort_type",
                dynamics=[
                    DynamicsItem(
                        key="$this.sort_type.show",
                        expression="return ['{}', '{}'].includes($this.sort_method.value)".format(
                            SortMethod.CTIME.value, SortMethod.MTIME.value
                        ),
                    )
                ],
                level=AtomicLevel.ADVANCED.value,
                required=False,
            ),
        ],
        outputList=[
            atomicMg.param("file_list", types="List"),
        ],
    )
    def get_file_list(
        folder_path: str = "",
        traverse_subfolder: TraverseType = TraverseType.NO,
        tempfile_include: bool = True,
        output_type: OutputType = OutputType.LIST,
        excel_path: str = "",
        state_type: StateType = StateType.ERROR,
        excel_name: str = "file_list.xlsx",
        sort_method: SortMethod = SortMethod.NONE,
        sort_type: SortType = SortType.ASCENDING,
    ) -> list:
        """
        가져오기파일목록

        """
        if not folder_is_exists(folder_path):
            raise BaseException(
                FOLDER_PATH_ERROR_FORMAT.format(folder_path),
                "폴더찾을 수 없습니다, 확인하세요경로정보",
            )

        # 가져오기파일목록(제거파일)
        file_list = []
        if traverse_subfolder == TraverseType.YES:
            for root, _, files in os.walk(folder_path, topdown=True):
                file_list.extend(os.path.join(root, file) for file in files if not file.startswith("."))
        elif traverse_subfolder == TraverseType.NO:
            file_list = [
                os.path.join(folder_path, file)
                for file in os.listdir(folder_path)
                if (os.path.isfile(os.path.join(folder_path, file))) and not file.startswith(".")
            ]
        else:
            raise NotImplementedError()

        # 필터링시파일
        if not tempfile_include:
            temp_file_patterns = [r"^~", r"^\.~\$", r".*\.tmp$"]
            filtered_file_list = []
            for file in file_list:
                if not any(re.search(pattern, os.path.basename(file)) for pattern in temp_file_patterns):
                    filtered_file_list.append(file)
            file_list = filtered_file_list

        # 정렬
        if sort_method == SortMethod.NONE:
            pass
        elif sort_method == SortMethod.CTIME:
            file_list = sorted(file_list, key=lambda x: os.path.getctime(x))
            file_list = file_list[::-1] if sort_type == SortType.DESCENDING else file_list
        elif sort_method == SortMethod.MTIME:
            file_list = sorted(file_list, key=lambda x: os.path.getmtime(x))
            file_list = file_list[::-1] if sort_type == SortType.DESCENDING else file_list
        else:
            raise NotImplementedError()

        # 출력
        if output_type == OutputType.EXCEL:
            if not folder_is_exists(excel_path):
                if state_type == StateType.ERROR:
                    raise BaseException(
                        FILE_PATH_ERROR_FORMAT.format(excel_path),
                        "지정excel저장경로찾을 수 없습니다, 확인하세요경로정보",
                    )
                elif state_type == StateType.CREATE:
                    os.makedirs(excel_path, exist_ok=True)
                else:
                    raise NotImplementedError()

            if not excel_name:
                excel_name = "file_list.xlsx"
            if not os.path.splitext(excel_name)[1] == ".xlsx":
                if os.path.splitext(excel_name)[1]:
                    excel_name = get_file_name_only(excel_name)
                excel_name += ".xlsx"

            excel_path = os.path.join(excel_path, excel_name)
            list_to_excel(path_list=file_list, excel_path=excel_path)
            return file_list
        elif output_type == OutputType.LIST:
            return file_list
        else:
            raise NotImplementedError()
