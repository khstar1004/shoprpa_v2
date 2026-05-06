import time
import winreg
from typing import Optional

import win32com
import win32com.client
from astronverse.actionlib.logger import logger
from astronverse.excel import ApplicationType
from astronverse.excel.excel_obj import ExcelObj


def get_default_excel_application():
    """
    가져오기시스템의Excel사용유형

    통신경과조회Windows회원가입테이블지정의Excel사용프로그램예Microsoft Excel예WPS.

    Returns:
        ApplicationType: 의Excel사용유형, 결과가불가지정이면반환ApplicationType.EXCEL
    """
    try:
        key = winreg.OpenKey(winreg.HKEY_CLASSES_ROOT, r"Excel.Sheet.12\shell\open\command")
        default_value, _ = winreg.QueryValueEx(key, None)
        winreg.CloseKey(key)

        if "et.exe" in default_value.lower():
            return ApplicationType.WPS
        elif "excel.exe" in default_value.lower():
            return ApplicationType.EXCEL
    except FileNotFoundError:
        return ApplicationType.EXCEL


def _create_app(params: str, retry: int = 0, retry_delay: float = 0.5):
    """
    생성Excel사용객체, 선택 가능재시도기기제어
    """
    max_attempts = retry + 1  # 시도데이터 = 재시도 데이터 + 1시도

    for attempt in range(max_attempts):
        try:
            # 사용cache, 가능변경
            return win32com.client.gencache.EnsureDispatch(params)
        except Exception as e:
            # 단계방법: 사용Dispatch
            try:
                return win32com.client.Dispatch(params)
            except Exception as e:
                if attempt < max_attempts - 1:
                    logger.warning(
                        f"생성Excel사용실패 (시도 {attempt + 1}/{max_attempts}): {params}, "
                        f"오류: {e}, {retry_delay:.2f}초후재시도..."
                    )
                    time.sleep(retry_delay)
                else:
                    logger.error(f"생성Excel사용실패: {params}, 오류: {e}, 완료까지대재시도 데이터")
    return None


def _get_app(params: str, retry: int = 0, retry_delay: float = 0.5):
    """
    가져오기완료존재함의Excel사용객체, 선택 가능재시도기기제어
    """
    max_attempts = retry + 1  # 시도데이터 = 재시도 데이터 + 1시도

    for attempt in range(max_attempts):
        try:
            return win32com.client.GetObject(Class=params)
        except Exception as e:
            if attempt < max_attempts - 1:
                logger.warning(
                    f"가져오기Excel사용실패 (시도 {attempt + 1}/{max_attempts}): {params}, "
                    f"오류: {e}, {retry_delay:.2f}초후재시도..."
                )
                time.sleep(retry_delay)
            else:
                logger.error(f"가져오기Excel사용실패: {params}, 오류: {e}, 완료까지대재시도 데이터")
    return None


def _get_key(default_application: ApplicationType = ApplicationType.DEFAULT):
    """가져오기Excel사용의회원가입테이블값"""

    if default_application == ApplicationType.DEFAULT:
        default_application = get_default_excel_application()
        if default_application == ApplicationType.WPS:
            keys = ["Ket.Application", "et.Application", "Excel.Application"]
        elif default_application == ApplicationType.EXCEL:
            keys = ["Excel.Application", "Ket.Application", "et.Application"]
        else:
            keys = ["Ket.Application", "et.Application", "Excel.Application"]
    elif default_application == ApplicationType.WPS:
        keys = ["Ket.Application", "et.Application"]
    elif default_application == ApplicationType.EXCEL:
        keys = ["Excel.Application"]
    else:
        keys = ["Ket.Application", "et.Application", "Excel.Application"]
    return keys


class Application:
    @staticmethod
    def init_app(
        default_application: ApplicationType = ApplicationType.DEFAULT,
        visible_flag: bool = None,
        retry: int = 0,
        retry_delay: float = 0.5,
        prefer_existing: bool = True,
    ) -> object:
        """ Excel 사용"""

        application = None
        keys = _get_key(default_application=default_application)

        if prefer_existing:
            # 결과가 prefer_existing=True, 시도가져오기완료존재함의
            for key in keys:
                application = _get_app(key, retry=retry, retry_delay=retry_delay)
                if application:
                    break
        if not application:
            # 결과가 prefer_existing=False 또는 GetObject 실패, 이면생성새
            for key in keys:
                application = _create_app(key, retry=retry, retry_delay=retry_delay)
                if application:
                    break

        if not application:
            try:
                win32com.client.gencache.Rebuild()
                win32com.client.gencache.EnsureModule("{00020813-0000-0000-C000-000000000046}", 0, 8, 7)
            except Exception:
                raise Exception("실패, 요청 시도삭제 %LOCALAPPDATA%\\Temp\\gen_py 디렉터리실행!")

            # 재생성저장후, 시도(가져오기완료존재함의)
            if prefer_existing:
                # 결과가 prefer_existing=True, 시도가져오기완료존재함의
                for key in keys:
                    application = _get_app(key, retry=retry, retry_delay=retry_delay)
                    if application:
                        break
            if not application:
                # 결과가 prefer_existing=False 또는 GetObject 실패, 이면생성새
                for key in keys:
                    application = _create_app(key, retry=retry, retry_delay=retry_delay)
                    if application:
                        break

        # 예외오류
        if not application:
            raise Exception("감지하지 못한 Excel또는WPS사용")

        try:
            if visible_flag is not None:
                application.Visible = visible_flag
            # 종료사용 안 함경고, 팝업중자동화 프로세스
            # 가능으로관리파일덮어쓰기, 삭제테이블대기, 높이본지정
            application.DisplayAlerts = False
        except Exception as e:
            raise Exception("Excel/WPS사용프로그램실패, 가능정상에서사용, 불가속성")
        return application

    @staticmethod
    def quit_app(default_application: ApplicationType = ApplicationType.DEFAULT, save_changes: bool = False):
        """출력 Excel 사용"""

        keys = _get_key(default_application=default_application)

        for key in keys:
            application = _get_app(key, retry=0, retry_delay=0)
            if not application:
                continue
            try:
                workbooks_count = application.Workbooks.Count
                for i in range(workbooks_count, 0, -1):
                    workbook = application.Workbooks(i)
                    workbook.Close(SaveChanges=save_changes)
                application.Quit()
            except Exception as e:
                logger.warning("닫기예외 {}".format(e))
                pass

    @staticmethod
    def create_workbook(application, file_path: str = "", password: str = "") -> ExcelObj:
        """생성새"""
        workbook = application.Workbooks.Add()
        if file_path:
            workbook.SaveAs(Filename=file_path, ReadOnlyRecommended=False, ConflictResolution=2, Password=password)
        return ExcelObj(obj=workbook, path=file_path or "")

    @staticmethod
    def open_workbook(application, file_path: str, password: str = "", update_links: bool = True) -> ExcelObj:
        """열기 """

        workbook = application.Workbooks.Open(
            Filename=file_path, UpdateLinks=update_links, Password=password, ReadOnly=False, Format=None
        )
        return ExcelObj(obj=workbook, path=file_path or "")

    @staticmethod
    def get_existing_workbook(application, match_name: str) -> Optional[ExcelObj]:
        """가져오기완료열기의"""
        workbooks_count = application.Workbooks.Count
        for i in range(workbooks_count, 0, -1):
            workbook = application.Workbooks(i)
            if match_name in workbook.Name:
                return ExcelObj(obj=workbook)
        return None

    @staticmethod
    def save_workbook(excel_obj: ExcelObj, file_path: str = "", password: str = ""):
        workbook = excel_obj.obj
        if file_path:
            workbook.SaveAs(Filename=file_path, ReadOnlyRecommended=False, ConflictResolution=2, Password=password)
        else:
            workbook.Save()

    @staticmethod
    def close_workbook(excel_obj: ExcelObj, save_changes: bool = True):
        workbook = excel_obj.obj
        workbook.Close(SaveChanges=save_changes)