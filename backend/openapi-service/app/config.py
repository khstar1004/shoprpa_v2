from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    APP_NAME: str = "ShopRPA OpenAPI Service"
    API_VERSION: str = "1.0"
    DATABASE_URL: str
    DATABASE_USERNAME: str
    DATABASE_PASSWORD: str
    REDIS_URL: str
    INTERNAL_ADMIN_API_KEY: str
    REGISTER_BEARER_TOKEN: str
    ROBOT_SERVICE_BASE_URL: str = "http://robot-service:8040"

    LOG_LEVEL: str = "INFO"
    LOG_DIR: str = "/app/log"

    model_config = SettingsConfigDict(
        env_file=None,
        case_sensitive=False,
        extra="ignore",
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()
