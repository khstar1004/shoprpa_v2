from no_config import Config


class AppSettings:
    """
    사용매칭
    """

    app_name: str = "rpa-browser-connector"


class HttpSettings:
    """
    http매칭
    """

    app_host: str = "0.0.0.0"
    app_port: int = 9082
    gateway_port: int = 13159


@Config(type=dict(app_settings=AppSettings, http_settings=HttpSettings))
class Config:
    """
    공유매칭
    """

    app_settings: AppSettings = AppSettings()
    http_settings: HttpSettings = HttpSettings()