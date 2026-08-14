"""애플리케이션 설정.

.env 파일의 값을 읽어 타입 검증된 Settings 객체로 제공한다.
Credential을 코드에 직접 쓰지 않기 위한 단일 진입점이다.
"""

from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict
from sqlalchemy.engine import URL

# app/core/config.py -> app/core -> app -> backend
BASE_DIR = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=BASE_DIR / ".env",
        env_file_encoding="utf-8",
        extra="ignore",  # .env에 미정의 키가 있어도 무시
    )

    # ===== App =====
    APP_NAME: str = "뭐먹지? API"
    APP_VERSION: str = "0.1.0"
    APP_ENV: str = "local"
    DEBUG: bool = True

    # ===== Database (Oracle 21c XE) =====
    # 네트워크가 바뀌면 .env의 DB_HOST 한 줄만 수정하면 된다.
    DB_HOST: str
    DB_PORT: int = 1521
    DB_SERVICE: str = "XEPDB1"
    DB_USER: str
    DB_PASSWORD: str
    DB_ECHO: bool = False
    DB_POOL_SIZE: int = 5
    DB_MAX_OVERFLOW: int = 5

    # ===== JWT =====
    JWT_SECRET_KEY: str
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRE_MINUTES: int = 30

    # ===== 계정 잠금 / OTP =====
    MAX_LOGIN_FAIL_COUNT: int = 20
    OTP_EXPIRE_MINUTES: int = 5
    OTP_MAX_ATTEMPT: int = 5

    # ===== CORS =====
    CORS_ORIGINS: str = "http://localhost:5173"

    # ===== Email =====
    EMAIL_PROVIDER: str = "mock"
    SMTP_HOST: str = ""
    SMTP_PORT: int = 587
    SMTP_USER: str = ""
    SMTP_PASSWORD: str = ""
    SMTP_FROM: str = ""

    # ===== AI =====
    AI_PROVIDER: str = "claude"
    ANTHROPIC_API_KEY: str = ""
    AI_MODEL: str = "claude-opus-5"

    # ===== Weather =====
    WEATHER_PROVIDER: str = "mock"
    OPENWEATHER_API_KEY: str = ""

    # ===== Shopping =====
    PRICE_PROVIDERS: str = "mock"
    NAVER_CLIENT_ID: str = ""
    NAVER_CLIENT_SECRET: str = ""

    @property
    def database_url(self) -> URL:
        """Oracle 접속 URL.

        문자열을 직접 조립하지 않고 URL.create()를 쓴다.
        비밀번호에 @ : / 같은 문자가 있어도 SQLAlchemy가 이스케이프해준다.
        XEPDB1은 SID가 아니라 서비스명이므로 service_name 쿼리 파라미터로 전달한다.
        """
        return URL.create(
            "oracle+oracledb",
            username=self.DB_USER,
            password=self.DB_PASSWORD,
            host=self.DB_HOST,
            port=self.DB_PORT,
            query={"service_name": self.DB_SERVICE},
        )

    @property
    def cors_origin_list(self) -> list[str]:
        """쉼표로 구분된 CORS_ORIGINS를 리스트로 변환."""
        return [origin.strip() for origin in self.CORS_ORIGINS.split(",") if origin.strip()]

    @property
    def price_provider_list(self) -> list[str]:
        """쉼표로 구분된 PRICE_PROVIDERS를 리스트로 변환."""
        return [p.strip() for p in self.PRICE_PROVIDERS.split(",") if p.strip()]


@lru_cache
def get_settings() -> Settings:
    """설정을 한 번만 읽어 캐시한다."""
    return Settings()


settings = get_settings()
