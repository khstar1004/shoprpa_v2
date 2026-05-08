import ftplib
import os.path

from astronverse.actionlib import AtomicFormType, AtomicFormTypeMeta, DynamicsItem
from astronverse.actionlib.atomic import atomicMg
from astronverse.network import FileExistenceType, FileType, ListType, StateType
from astronverse.network.core_ftp import FtpCore
from astronverse.network.error import *
from astronverse.network.utils import (
    file_is_exist,
    folder_is_exist,
    generate_local_name,
    get_exist_files,
    get_file_list,
)


class FTP:
    @staticmethod
    @atomicMg.atomic(
        "Network",
        inputList=[
            atomicMg.param("host", types="Str", required=True),
            atomicMg.param("port", types="Int", required=True),
            atomicMg.param("name", types="Str", required=False),
            atomicMg.param("password", types="Str", required=False),
        ],
        outputList=[atomicMg.param("ftp_instance", types="Str")],
    )
    def ftp_create(host: str, port: int, name: str, password: str):
        ftp_instance = FtpCore.create_ftp()
        try:
            FtpCore.ftp_connection(ftp_instance, host, port)
        except Exception as e:
            raise BaseException(FTP_CONNECTION_FORMAT.format(host, port), "연결까지FTP서비스서버실패")

        if name and password:
            try:
                FtpCore.ftp_login(ftp_instance, name, password)
            except Exception as e:
                raise BaseException(FTP_LOGIN_FORMAT.format(name, password), "로그인까지FTP서비스서버실패")

        return ftp_instance

    @staticmethod
    @atomicMg.atomic(
        "Network",
        outputList=[
            atomicMg.param("close_ftp", types="Bool"),
        ],
    )
    def ftp_close(ftp_instance: ftplib.FTP):
        try:
            FtpCore.close_ftp(ftp_instance)
            return True
        except Exception as e:
            raise BaseException(FTP_CLOSE_FORMAT.format(e), "FTP연결닫기실패")

    @staticmethod
    @atomicMg.atomic(
        "Network",
        outputList=[
            atomicMg.param("get_work_dir", types="Str"),
        ],
    )
    def get_work_dir(ftp_instance: ftplib.FTP):
        try:
            get_work_dir = FtpCore.get_working_dir(ftp_instance)
        except Exception as e:
            raise BaseException(FTP_STATUS_FORMAT.format(e), "{e}")
        return get_work_dir

    @staticmethod
    @atomicMg.atomic(
        "Network",
        outputList=[
            atomicMg.param("change_work_dir", types="Str"),
        ],
    )
    def change_working_dir(ftp_instance: ftplib.FTP, new_work_dir: str):
        try:
            FtpCore.change_working_dir(ftp_instance, new_work_dir)
            change_work_dir = FtpCore.get_working_dir(ftp_instance)
            return change_work_dir
        except Exception as e:
            raise BaseException(FTP_STATUS_FORMAT.format(e), "{e}")

    @staticmethod
    @atomicMg.atomic(
        "Network",
        inputList=[
            atomicMg.param("ftp_instance", types="Str"),
            atomicMg.param("folder_name", types="Str"),
            atomicMg.param(
                "exist_type",
                formType=AtomicFormTypeMeta(type=AtomicFormType.SELECT.value),
                required=False,
            ),
        ],
        outputList=[
            atomicMg.param("new_folder", types="Str"),
        ],
    )
    def create_folder(
        ftp_instance: ftplib.FTP,
        folder_name: str,
        exist_type: FileExistenceType = FileExistenceType.RENAME,
    ):
        """
        생성폴더
        :param ftp_instance: FTP연결객체
        :param folder_name: 폴더이름
        :param exist_type: 폴더존재함시
        :return: 새생성폴더 경로
        """
        get_list = FtpCore.get_nlst(ftp_instance)
        if folder_name in get_list:
            if exist_type == FileExistenceType.RENAME:
                folder_name = FtpCore.generate_name(ftp_instance, folder_name)
            elif exist_type == FileExistenceType.CANCEL:
                return FtpCore.get_path(ftp_instance, folder_name)
            elif exist_type == FileExistenceType.OVERWRITE:
                FtpCore.ftp_delete_dir(ftp_instance, folder_name)
            else:
                raise NotImplementedError

        try:
            new_folder = FtpCore.create_dir(ftp_instance, folder_name)
        except Exception as e:
            raise BaseException(FTP_CREATE_FORMAT.format(e), "폴더생성실패: {}".format(e))

        return new_folder

    @staticmethod
    @atomicMg.atomic(
        "Network",
        inputList=[
            atomicMg.param("file_type", required=False),
        ],
        outputList=[
            atomicMg.param("get_ftp_list", types="Dict"),
        ],
    )
    def get_ftp_list(ftp_instance: ftplib.FTP, file_type: ListType = ListType.FILE):
        """
        가져오기 디렉터리아래파일/폴더
        :param ftp_instance: FTP연결객체
        :param file_type: 가져오기유형
        :return: 가져오기내용
        """
        file_structure = {"files": [], "folders": []}
        try:
            FtpCore.get_working_dir(ftp_instance)
        except Exception as e:
            raise BaseException(FTP_STATUS_FORMAT.format(e), "{e}")

        try:
            list_file = FtpCore.get_nlst(ftp_instance)
            for item in list_file:
                if FtpCore.is_dir(ftp_instance, item):
                    file_structure["folders"].append(item)
                else:
                    file_structure["files"].append(item)

            if file_type == ListType.FILE:
                return file_structure["files"]
            elif file_type == ListType.FOLDER:
                return file_structure["folders"]
            elif file_type == ListType.ALL:
                return file_structure
            else:
                raise NotImplementedError()
        except Exception as e:
            raise BaseException(FTP_STATUS_FORMAT.format(e), "{e}")

    @staticmethod
    @atomicMg.atomic(
        "Network",
        inputList=[
            atomicMg.param("ftp_instance", types="Str", required=True),
            atomicMg.param("file_type", required=False),
            atomicMg.param(
                "cur_file_name",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.cur_file_name.show",
                        expression="return $this.file_type.value == '{}'".format(FileType.FILE.value),
                    )
                ],
                required=True,
            ),
            atomicMg.param(
                "new_file_name",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.new_file_name.show",
                        expression="return $this.file_type.value == '{}'".format(FileType.FILE.value),
                    )
                ],
                required=True,
            ),
            atomicMg.param(
                "cur_folder_name",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.cur_folder_name.show",
                        expression="return $this.file_type.value == '{}'".format(FileType.FOLDER.value),
                    )
                ],
                required=True,
            ),
            atomicMg.param(
                "new_folder_name",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.new_folder_name.show",
                        expression="return $this.file_type.value == '{}'".format(FileType.FOLDER.value),
                    )
                ],
                required=True,
            ),
            atomicMg.param(
                "exist_type",
                formType=AtomicFormTypeMeta(type=AtomicFormType.SELECT.value),
                required=False,
            ),
        ],
        outputList=[
            atomicMg.param("rename_ftp_path", types="Str"),
        ],
    )
    def ftp_rename(
        ftp_instance: ftplib.FTP,
        file_type: FileType = FileType.FILE,
        cur_file_name: str = "",
        new_file_name: str = "",
        cur_folder_name: str = "",
        new_folder_name: str = "",
        exist_type: FileExistenceType = FileExistenceType.RENAME,
    ):
        """
        FTP서비스서버위의파일/폴더이름 변경
        :param ftp_instance: 연결의FTP서비스서버
        :param file_type: 이름 변경유형 파일/폴더
        :param cur_file_name:  기존파일이름
        :param new_file_name:  새파일이름(아니오필요이름)
        :param cur_folder_name:  기존폴더이름
        :param new_folder_name:  새폴더이름
        :param exist_type:  파일존재함시
        :return: 이름 변경후파일 경로
        """

        exist_file = FtpCore.get_nlst(ftp_instance)

        if file_type == FileType.FILE:
            if cur_file_name not in exist_file:
                raise BaseException(FILE_EXIST_FORMAT.format(cur_file_name), "이름을 변경할 파일을 찾을 수 없습니다")

            file_ext = os.path.splitext(cur_file_name)[1]
            if not file_ext:
                raise BaseException(
                    FILE_NAME_FORMAT.format(cur_file_name),
                    "파일 이름이 올바른지 입력 내용을 확인하세요",
                )

            new_file_name = new_file_name + file_ext
            if new_file_name in exist_file:
                if exist_type == FileExistenceType.CANCEL:
                    return FtpCore.get_path(ftp_instance, cur_file_name)
                elif exist_type == FileExistenceType.OVERWRITE:
                    FtpCore.ftp_delete_file(ftp_instance, new_file_name)
                elif exist_type == FileExistenceType.RENAME:
                    new_file_name = FtpCore.generate_name(ftp_instance, new_file_name)
                else:
                    raise NotImplementedError()
            try:
                FtpCore.ftp_rename(ftp_instance, cur_file_name, new_file_name)
                return FtpCore.get_path(ftp_instance, new_file_name)
            except Exception as e:
                raise BaseException(FTP_RENAME_FORMAT.format(e), "FTP 파일 이름 변경에 실패했습니다")

        elif file_type == FileType.FOLDER:
            if cur_folder_name not in exist_file:
                raise BaseException(FOLDER_EXIST_FORMAT.format(cur_folder_name), "이름을 변경할 폴더를 찾을 수 없습니다")

            if not FtpCore.is_dir(ftp_instance, cur_folder_name):
                raise BaseException(
                    FILE_NAME_FORMAT.format(cur_folder_name),
                    "이름 변경 대상이 폴더인지 입력 정보를 확인하세요",
                )

            if new_folder_name in exist_file:
                if exist_type == FileExistenceType.CANCEL:
                    return FtpCore.get_path(ftp_instance, cur_folder_name)
                elif exist_type == FileExistenceType.OVERWRITE:
                    FtpCore.ftp_delete_dir(ftp_instance, new_folder_name)
                elif exist_type == FileExistenceType.RENAME:
                    new_folder_name = FtpCore.generate_name(ftp_instance, new_folder_name)
                else:
                    raise NotImplementedError()
            try:
                FtpCore.ftp_rename(ftp_instance, cur_file_name, new_folder_name)
                return FtpCore.get_path(ftp_instance, new_folder_name)
            except Exception as e:
                raise BaseException(FTP_RENAME_FORMAT.format(e), "FTP 폴더 이름 변경에 실패했습니다")

    @staticmethod
    @atomicMg.atomic(
        "Network",
        inputList=[
            atomicMg.param(
                "file_path",
                formType=AtomicFormTypeMeta(
                    AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "files"},
                ),
                dynamics=[
                    DynamicsItem(
                        key="$this.file_path.show",
                        expression="return $this.file_type.value == '{}'".format(FileType.FILE.value),
                    )
                ],
                required=True,
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
                        expression="return $this.file_type.value == '{}'".format(FileType.FOLDER.value),
                    )
                ],
                required=True,
            ),
            atomicMg.param("file_type", required=False),
            atomicMg.param("ftp_pwd", types="Str", required=False),
            atomicMg.param(
                "exist_type",
                formType=AtomicFormTypeMeta(type=AtomicFormType.SELECT.value),
                required=False,
            ),
        ],
        outputList=[
            atomicMg.param("upload_ftp_list", types="List"),
        ],
    )
    def ftp_upload(
        ftp_instance: ftplib.FTP,
        file_type: FileType = FileType.FILE,
        ftp_pwd: str = "",
        file_path: str = "",
        folder_path: str = "",
        exist_type: FileExistenceType = FileExistenceType.RENAME,
    ):
        if ftp_pwd:
            if not FtpCore.is_dir(ftp_instance, ftp_pwd):
                if not FtpCore.create_dir(ftp_instance, ftp_pwd):
                    raise BaseException(
                        FTP_CREATE_FORMAT.format(ftp_pwd),
                        "지정한 디렉터리 생성에 실패했습니다: {}. FTP 연결 또는 디렉터리 이름을 확인하세요".format(ftp_pwd),
                    )
            FtpCore.change_working_dir(ftp_instance, ftp_pwd)

        dst_list = FtpCore.get_nlst(ftp_instance)
        upload_ftp_list = []

        if file_type == FileType.FILE:
            file_list = get_file_list(file_path)
            for file in file_list:
                if not file_is_exist(file):
                    raise BaseException(FILE_EXIST_FORMAT.format(file), "업로드할 파일을 찾을 수 없거나 형식이 올바르지 않습니다")

                file_name = os.path.basename(file)

                if file_name in dst_list:
                    if exist_type == FileExistenceType.CANCEL:
                        return FtpCore.get_path(ftp_instance, file_name)
                    elif exist_type == FileExistenceType.OVERWRITE:
                        FtpCore.ftp_delete_file(ftp_instance, file_name)
                    elif exist_type == FileExistenceType.RENAME:
                        file_name = FtpCore.generate_name(ftp_instance, file_name)
                    else:
                        raise NotImplementedError()
                try:
                    dst_path = FtpCore.ftp_upload_file(ftp_instance, file, file_name)
                except Exception as e:
                    raise BaseException(FTP_UPLOAD_FORMAT.format(file), "파일 업로드에 실패했습니다. FTP 연결을 확인하세요")
                upload_ftp_list.append(dst_path)

        elif file_type == FileType.FOLDER:
            folder_list = get_file_list(folder_path)
            for folder in folder_list:
                if not folder_is_exist(folder):
                    raise BaseException(FOLDER_EXIST_FORMAT.format(folder), "업로드할 폴더를 찾을 수 없습니다")

                folder_name = os.path.basename(folder)
                if folder_name in dst_list:
                    if exist_type == FileExistenceType.CANCEL:
                        return FtpCore.get_path(ftp_instance, folder_name)
                    elif exist_type == FileExistenceType.OVERWRITE:
                        FtpCore.ftp_delete_dir(ftp_instance, folder_name)
                    elif exist_type == FileExistenceType.RENAME:
                        folder_name = FtpCore.generate_name(ftp_instance, folder_name)
                    else:
                        raise NotImplementedError()
                try:
                    dst_path = FtpCore.ftp_upload_dir(ftp_instance, folder, folder_name)
                except Exception as e:
                    raise BaseException(FTP_UPLOAD_FORMAT.format(folder), "폴더 업로드에 실패했습니다. FTP 연결을 확인하세요")
                upload_ftp_list.append(dst_path)
        else:
            raise NotImplementedError()

        return upload_ftp_list

    @staticmethod
    @atomicMg.atomic(
        "Network",
        inputList=[
            atomicMg.param("file_type", required=False),
            atomicMg.param(
                "download_file_name",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.download_file_name.show",
                        expression="return $this.file_type.value == '{}'".format(FileType.FILE.value),
                    )
                ],
                required=True,
            ),
            atomicMg.param(
                "download_folder_name",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.download_folder_name.show",
                        expression="return $this.file_type.value == '{}'".format(FileType.FOLDER.value),
                    )
                ],
            ),
            atomicMg.param(
                "dst_path",
                formType=AtomicFormTypeMeta(
                    AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "folder"},
                ),
                required=True,
            ),
            atomicMg.param("state_type", required=False),
            atomicMg.param(
                "exist_type",
                formType=AtomicFormTypeMeta(type=AtomicFormType.SELECT.value),
                required=False,
            ),
        ],
        outputList=[
            atomicMg.param("download_ftp_path", types="List"),
        ],
    )
    def ftp_download(
        ftp_instance: ftplib.FTP,
        file_type: FileType = FileType.FILE,
        download_file_name: str = "",
        download_folder_name: str = "",
        dst_path: str = "",
        state_type: StateType = StateType.CREATE,
        exist_type: FileExistenceType = FileExistenceType.RENAME,
    ):
        """
        에서지정FTP서비스서버위다운로드파일/폴더
        :param ftp_instance: FTP연결객체
        :param file_type: 다운로드객체  파일/폴더
        :param download_file_name: 다운로드파일이름,다중개파일이름사용,열기
        :param download_folder_name:  다운로드폴더이름,다중개폴더이름사용,열기
        :param dst_path:    본디렉터리
        :param state_type:  디렉터리찾을 수 없습니다시   생성/안내오류
        :param exist_type:   파일/폴더존재함시   덮어쓰기/이름 변경/건너뛰기
        :return: 다운로드후파일/폴더 경로목록
        """
        if not folder_is_exist(dst_path):
            if state_type == StateType.CREATE:
                os.mkdir(dst_path)
            elif state_type == StateType.ERROR:
                raise BaseException(
                    FOLDER_EXIST_FORMAT.format(dst_path),
                    "지정목록경로찾을 수 없습니다, 확인하세요경로정보",
                )
        ftp_list = FtpCore.get_nlst(ftp_instance)
        local_exist_list = get_exist_files(dst_path)
        work_dir = FtpCore.get_working_dir(ftp_instance)
        download_ftp_path = []

        if file_type == FileType.FILE:
            download_file_list = get_file_list(download_file_name)
            for file in download_file_list:
                file_name = file
                if file not in ftp_list:
                    raise BaseException(
                        FILE_EXIST_FORMAT.format(file),
                        "현재디렉터리중아니오저장된 지정다운로드파일: {}, 확인하세요다운로드이름".format(file),
                    )
                if file in local_exist_list:
                    if exist_type == FileExistenceType.CANCEL:
                        download_ftp_path.append(os.path.join(dst_path, file))
                        continue
                    elif exist_type == FileExistenceType.OVERWRITE:
                        os.remove(os.path.join(dst_path, file))
                    elif exist_type == FileExistenceType.RENAME:
                        file_name = generate_local_name(local_exist_list, file)
                    else:
                        raise NotImplementedError()
                try:
                    download_file = FtpCore.ftp_download_file(
                        ftp_instance,
                        os.path.join(work_dir, file),
                        os.path.join(dst_path, file_name),
                    )
                except Exception as e:
                    raise BaseException(FTP_DOWNLOAD_FORMAT.format(file), "파일 다운로드에 실패했습니다. FTP 연결을 확인하세요")

                download_ftp_path.append(download_file)

        elif file_type == FileType.FOLDER:
            download_folder_list = get_file_list(download_folder_name)
            for folder in download_folder_list:
                if folder not in ftp_list:
                    raise BaseException(
                        FOLDER_EXIST_FORMAT.format(folder),
                        "현재 디렉터리에 다운로드할 폴더가 없습니다: {}. 다운로드 이름을 확인하세요".format(folder),
                    )
                folder_new = folder
                if folder in local_exist_list:
                    if exist_type == FileExistenceType.CANCEL:
                        download_ftp_path.append(os.path.join(dst_path, folder))
                        continue
                    elif exist_type == FileExistenceType.OVERWRITE:
                        os.rmdir(os.path.join(dst_path, folder))
                    elif exist_type == FileExistenceType.RENAME:
                        folder_new = generate_local_name(local_exist_list, folder)
                    else:
                        raise NotImplementedError()

                try:
                    download_folder = FtpCore.ftp_download_dir(
                        ftp_instance,
                        os.path.join(work_dir, folder),
                        os.path.join(dst_path, folder_new),
                    )
                except Exception as e:
                    raise BaseException(
                        FTP_DOWNLOAD_FORMAT.format(folder),
                        "폴더 다운로드에 실패했습니다. FTP 연결을 확인하세요",
                    )

                download_ftp_path.append(download_folder)

        else:
            raise NotImplementedError()

        return download_ftp_path

    @staticmethod
    @atomicMg.atomic(
        "Network",
        inputList=[
            atomicMg.param("file_type", required=False),
            atomicMg.param(
                "delete_file_name",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.delete_file_name.show",
                        expression="return $this.file_type.value == '{}'".format(FileType.FILE.value),
                    )
                ],
                required=True,
            ),
            atomicMg.param(
                "delete_folder_name",
                types="Str",
                dynamics=[
                    DynamicsItem(
                        key="$this.delete_folder_name.show",
                        expression="return $this.file_type.value == '{}'".format(FileType.FOLDER.value),
                    )
                ],
                required=True,
            ),
        ],
        outputList=[
            atomicMg.param("delete_ftp_result", types="Bool"),
        ],
    )
    def ftp_delete(
        ftp_instance: ftplib.FTP,
        file_type: FileType = FileType.FILE,
        delete_file_name: str = "",
        delete_folder_name: str = "",
    ):
        exist_list = FtpCore.get_nlst(ftp_instance)
        try:
            if file_type == FileType.FILE:
                delete_file_list = get_file_list(delete_file_name)
                for item in delete_file_list:
                    if item not in exist_list:
                        raise BaseException(
                            FTP_DELETE_FORMAT.format(delete_file_name),
                            "현재 디렉터리에서 삭제할 파일을 찾을 수 없습니다. 파일 이름을 확인하세요",
                        )
                    FtpCore.ftp_delete_file(ftp_instance, item)
                return True
            elif file_type == FileType.FOLDER:
                delete_folder_list = get_file_list(delete_folder_name)
                for item in delete_folder_list:
                    if item not in exist_list:
                        raise BaseException(
                            FTP_DELETE_FORMAT.format(delete_folder_name),
                            "현재 디렉터리에서 삭제할 폴더를 찾을 수 없습니다. 삭제할 이름을 확인하세요",
                        )
                    FtpCore.ftp_delete_dir(ftp_instance, item)
                return True
            else:
                raise NotImplementedError()
        except Exception as e:
            raise BaseException(FTP_DELETE_FORMAT.format(e), "파일 또는 폴더 삭제 여부를 확인하세요")
