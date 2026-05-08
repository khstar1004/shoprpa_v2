import ftplib
import os


class FtpCore:
    @staticmethod
    def create_ftp():
        """
        생성FTP
        """
        ftp_instance = ftplib.FTP()
        ftp_instance.encoding = "gbk"
        return ftp_instance

    @staticmethod
    def ftp_connection(ftp_instance: ftplib.FTP, host: str, port: int):
        """
        연결FTP
        """
        return ftp_instance.connect(host, port)

    @staticmethod
    def ftp_login(ftp_instance: ftplib.FTP, user: str, password: str):
        """
        ftp
        """
        return ftp_instance.login(user, password)

    @staticmethod
    def close_ftp(ftp_instance: ftplib.FTP):
        """
        닫기ftp
        """
        return ftp_instance.close()

    @staticmethod
    def get_working_dir(ftp_instance: ftplib.FTP):
        """
        가져오기현재디렉터리
        """
        return ftp_instance.pwd()

    @staticmethod
    def change_working_dir(ftp_instance: ftplib.FTP, working_dir: str):
        """
        디렉터리
        """
        return ftp_instance.cwd(working_dir)

    @staticmethod
    def get_list(ftp_instance: ftplib.FTP):
        """
        가져오기현재디렉터리아래의전체파일폴더
        """
        try:
            raw_data = []

            def callback(line):
                raw_data.append(line)

            ftp_instance.retrlines("LIST", callback)
        except ftplib.error_perm as e:
            raise ValueError(f"권한 오류 또는 지원하지 않는 FTP 명령입니다: {e}")
        except Exception as e:
            raise ValueError("FTP 목록 조회 오류: {}".format(e))

        return raw_data

    @staticmethod
    def get_nlst(ftp_instance: ftplib.FTP):
        """
        가져오기현재디렉터리아래의전체파일폴더
        """
        return ftp_instance.nlst()

    @staticmethod
    def ftp_rename(ftp_instance: ftplib.FTP, old_name: str, new_name: str):
        """
        이름 변경지정파일/폴더
        """
        return ftp_instance.rename(old_name, new_name)

    @staticmethod
    def ftp_upload_file(ftp_instance: ftplib.FTP, src_path: str, file_name: str):
        """
        FTP지정디렉터리업로드파일
        """
        bufsize = 1024
        fp = open(src_path, "rb")
        ftp_instance.storbinary("STOR " + file_name, fp, bufsize)
        ftp_instance.set_debuglevel(0)
        fp.close()
        return FtpCore.get_path(ftp_instance, file_name)

    @staticmethod
    def ftp_upload_dir(ftp_instance: ftplib.FTP, src: str, folder_name: str):
        """
        FTP지정디렉터리업로드폴더
        """
        if not os.path.isdir(src):
            raise ValueError("{} 경로는 디렉터리가 아닙니다".format(src))

        if not FtpCore.is_dir(ftp_instance, folder_name):
            res = FtpCore.create_dir(ftp_instance, folder_name)
            if not res:
                raise ValueError("디렉터리 생성에 실패했습니다. FTP 연결을 확인하세요.")

        ftp_instance.cwd(folder_name)

        upload_name_list = os.listdir(src)
        for name in upload_name_list:
            local_path = os.path.join(src, name)
            if os.path.isdir(local_path):
                FtpCore.ftp_upload_dir(ftp_instance, local_path, name)
            else:
                FtpCore.ftp_upload_file(ftp_instance, local_path, name)

        ftp_instance.cwd("..")
        return FtpCore.get_path(ftp_instance, folder_name)

    @staticmethod
    def ftp_delete_file(ftp_instance: ftplib.FTP, file_name: str):
        """
        삭제파일
        """
        return ftp_instance.delete(file_name)

    @staticmethod
    def ftp_delete_dir(ftp_instance: ftplib.FTP, dir_name: str):
        """
        삭제폴더
        """
        ftp_instance.cwd(dir_name)
        name_list = ftp_instance.nlst()
        for name in name_list:
            if FtpCore.is_dir(ftp_instance, name):
                FtpCore.ftp_delete_dir(ftp_instance, name)
            else:
                FtpCore.ftp_delete_file(ftp_instance, name)
        ftp_instance.cwd("..")
        ftp_instance.rmd(dir_name)

    @staticmethod
    def ftp_download_file(ftp_instance: ftplib.FTP, remote_path, local_path: str):
        bufsize = 1024
        fp = open(local_path, "wb")
        ftp_instance.retrbinary(f"RETR {remote_path}", fp.write, bufsize)
        ftp_instance.set_debuglevel(0)
        fp.close()
        return local_path

    @staticmethod
    def ftp_download_dir(ftp_instance: ftplib.FTP, remote_path, local_path: str):
        if not os.path.isdir(local_path):
            os.makedirs(local_path)
        ftp_instance.cwd(remote_path)
        name_list = ftp_instance.nlst()
        for name in name_list:
            local_item_path = os.path.join(local_path, name)
            if not FtpCore.is_dir(ftp_instance, name):
                FtpCore.ftp_download_file(ftp_instance, os.path.join(remote_path, name), local_item_path)
            else:
                FtpCore.ftp_download_dir(ftp_instance, os.path.join(remote_path, name), local_item_path)
        ftp_instance.cwd("..")
        return local_path

    @staticmethod
    def get_path(ftp_instance: ftplib.FTP, name: str):
        """
        가져오기현재디렉터리아래의파일/폴더 경로
        """
        pwd = ftp_instance.pwd()
        return os.path.join(pwd, name)

    @staticmethod
    def generate_name(ftp_instance, rename: str):
        """
        로재이름파일/폴더완료본
        """
        base, extension = os.path.splitext(rename)
        counter = 1
        new_name = f"{base}({counter}){extension}"
        while new_name in FtpCore.get_nlst(ftp_instance):
            counter += 1
            new_name = f"{base}({counter}){extension}"
        return new_name

    @staticmethod
    def is_dir(ftp_instance: ftplib.FTP, dir_name: str):
        """
        FTP서비스서버중의지정경로여부존재함
        """
        current_dir = ftp_instance.pwd()
        try:
            ftp_instance.cwd(dir_name)
            ftp_instance.cwd(current_dir)
            return True
        except ftplib.error_perm as e:
            return False

    @staticmethod
    def create_dir(ftp_instance: ftplib.FTP, dir_name: str):
        """
        에서FTP현재디렉터리아래새생성폴더
        """
        try:
            ftp_instance.mkd(dir_name)
            return FtpCore.get_path(ftp_instance, dir_name)
        except ftplib.error_perm as e:
            raise ValueError(e)
