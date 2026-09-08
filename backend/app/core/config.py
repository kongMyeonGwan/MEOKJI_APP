"""애플리케이션 설정.

backend/.env 파일의 값을 읽어서 settings 라는 객체 하나에 담아둔다.
다른 파일들은 이렇게 쓴다.

    from app.core.config import settings
    print(settings.DB_HOST)

비밀번호나 API 키를 코드에 직접 쓰지 않으려고 만든 통로다.
.env 는 git 에 올리지 않으므로(.gitignore) 키가 GitHub 에 새지 않는다.

BaseSettings 를 상속하면 아래 적어둔 이름과 같은 항목을 .env 에서 자동으로 찾아
넣어주고, 타입까지 맞춰준다. DB_PORT: int 라고 적어두면 .env 의 "1521"(문자열)이
1521(정수)로 바뀌어 들어온다. 값이 없고 기본값도 없으면 서버가 뜰 때 바로 에러가 난다.
"""

from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict
from sqlalchemy.engine import URL

# 이 파일(app/core/config.py) 기준으로 두 단계 위가 backend 폴더다.
#   app/core/config.py -> app/core -> app -> backend
# .env 를 절대 경로로 찾아야, 어느 폴더에서 서버를 켜도 같은 파일을 읽는다.
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

    # 위치 권한을 못 받았을 때(거부·미지원) 쓰는 기본 좌표 — 종로2가(탑골공원 인근).
    # 회원가입 지역 선택 기능이 생기기 전까지의 fallback이다.
    DEFAULT_WEATHER_LAT: float = 37.5711
    DEFAULT_WEATHER_LON: float = 126.9885

    # ===== Shopping =====
    PRICE_PROVIDERS: str = "mock"
    NAVER_CLIENT_ID: str = ""
    NAVER_CLIENT_SECRET: str = ""

    # 아래 셋은 @property 다. 괄호 없이 settings.database_url 처럼 쓰면
    # 그때마다 함수가 실행되어 계산된 값을 돌려준다.

    @property
    def database_url(self) -> URL:
        """Oracle 접속 주소를 만든다.

        문자열을 f-string 으로 직접 이어붙이지 않고 URL.create() 를 쓴다.
        비밀번호에 @ : / 같은 글자가 들어있어도 SQLAlchemy 가 알아서 처리해준다.
        직접 이어붙이면 그런 비밀번호에서 주소가 깨진다.

        XEPDB1 은 SID 가 아니라 서비스명이라서 service_name 으로 넘겨야 한다.
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
        """CORS_ORIGINS 를 리스트로 바꾼다.

        .env 에는 한 줄로 적혀 있다.
            CORS_ORIGINS=http://localhost:5173,http://localhost:3000
        이걸 ["http://localhost:5173", "http://localhost:3000"] 으로 만든다.
        """
        return split_by_comma(self.CORS_ORIGINS)

    @property
    def price_provider_list(self) -> list[str]:
        """PRICE_PROVIDERS 를 리스트로 바꾼다. 적힌 순서가 곧 우선순위다."""
        return split_by_comma(self.PRICE_PROVIDERS)


def split_by_comma(text: str) -> list[str]:
    """쉼표로 나누고, 앞뒤 공백을 떼고, 빈 칸은 버린다.

    "a, b ,, c" -> ["a", "b", "c"]
    """
    result = []
    for piece in text.split(","):
        piece = piece.strip()
        if piece:
            result.append(piece)
    return result


# 서버가 켜질 때 딱 한 번 .env 를 읽어서 만들어 둔다.
# 다른 파일에서는 이 settings 를 그대로 가져다 쓰면 된다.
settings = Settings()
