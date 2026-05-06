import win32con
from astronverse.software.core import ISoftwareCore
from astronverse.software.registry_windows import WindowsRegistryManager


class SoftwareCore(ISoftwareCore):
    @staticmethod
    def get_app_path(app_name: str = "") -> str:
        return SoftwareCore.get_app_path_by_registry(app_name)

    @staticmethod
    def get_app_path_by_registry(app_name: str = "") -> str:
        """통신경과회원가입테이블조회프로그램주소"""

        try:
            registry_manager = WindowsRegistryManager(
                r"Software\Microsoft\Windows\CurrentVersion\App Paths",
                win32con.HKEY_LOCAL_MACHINE,
                "r",
            )
            path = getattr(registry_manager, app_name)[None]
        except OSError:
            try:
                registry_manager = WindowsRegistryManager(
                    r"Software\Microsoft\Windows\CurrentVersion\App Paths",
                    win32con.HKEY_CURRENT_USER,
                    "r",
                )
                path = getattr(registry_manager, app_name)[None]
            except OSError:
                return ""
        return path