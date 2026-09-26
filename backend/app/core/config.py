"""애플리케이션 설정."""

from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict
from sqlalchemy.engine import URL

# 이 파일(app/core/config.py) 기준으로 두 단계 위가 backend 폴더다.
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

    # 인증번호를 다시 보내기까지 기다려야 하는 시간(초).
    OTP_RESEND_COOLDOWN_SECONDS: int = 60

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
    # 쓸 수 있는 값은 catalog(=mock) 하나다.
    # 제미나이 · 클로드는 쓰지 않기로 했다(2026-09-26).
    AI_PROVIDER: str = "catalog"

    # # Gemini (https://aistudio.google.com 에서 발급)
    # GEMINI_API_KEY: str = ""

    # # 쉼표로 여러 개를 적으면 앞에서부터 시도한다.
    # GEMINI_MODEL: str = "gemini-3.7-flash,gemini-3.5-flash"

    # # Claude (https://console.anthropic.com 에서 발급)
    # ANTHROPIC_API_KEY: str = ""
    # AI_MODEL: str = "claude-opus-5"

    # ===== Weather =====
    WEATHER_PROVIDER: str = "mock"
    OPENWEATHER_API_KEY: str = ""

    # 위치 권한을 못 받았을 때(거부·미지원) 쓰는 기본 좌표 — 종로2가(탑골공원 인근).
    # 회원가입 지역 선택 기능이 생기기 전까지의 fallback이다.
    DEFAULT_WEATHER_LAT: float = 37.5711
    DEFAULT_WEATHER_LON: float = 126.9885

    # ===== 재료 가격 =====
    PRICE_PROVIDERS: str = "kamis"

    # 공공데이터포털 마이페이지의 일반 인증키 — Decoding 값을 넣는다.
    DATA_GO_KR_SERVICE_KEY: str = ""

    # ===== 후기 첨부파일 =====

    # 저장 폴더. 상대 경로면 backend 폴더 기준이다.
    # DB 의 FILE_PATH 에도 이 이름이 앞에 붙어 들어간다 ("uploads/2026/09").
    UPLOAD_DIR: str = "uploads"

    # 한 파일의 최대 크기(MB). 후기 사진 한 장이라 크게 잡을 이유가 없다.
    UPLOAD_MAX_MB: int = 5

    # 받아줄 확장자. 실행 파일이 올라오는 것을 막는 게 목적이라
    # "허용한 것만 받는다"로 적는다. 금지 목록은 빠뜨린 게 곧 구멍이 된다.
    UPLOAD_ALLOWED_EXTS: str = "jpg,jpeg,png,gif,webp"

    # 아래 셋은 @property 다. 괄호 없이 settings.database_url 처럼 쓰면
    # 그때마다 함수가 실행되어 계산된 값을 돌려준다.

    @property
    def database_url(self) -> URL:
        """Oracle 접속 주소를 만든다."""
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
        """CORS_ORIGINS 를 리스트로 바꾼다."""
        return split_by_comma(self.CORS_ORIGINS)

    # @property
    # def gemini_model_list(self) -> list[str]:
        # """GEMINI_MODEL 을 리스트로 바꾼다. 적힌 순서가 곧 우선순위다."""
        # return split_by_comma(self.GEMINI_MODEL)

    @property
    def price_provider_list(self) -> list[str]:
        """PRICE_PROVIDERS 를 리스트로 바꾼다. 적힌 순서가 곧 우선순위다."""
        return split_by_comma(self.PRICE_PROVIDERS)

    @property
    def upload_root(self) -> Path:
        """첨부파일 저장 폴더의 절대 경로."""
        folder = Path(self.UPLOAD_DIR)

        return folder if folder.is_absolute() else BASE_DIR / folder

    @property
    def upload_max_bytes(self) -> int:
        """UPLOAD_MAX_MB 를 바이트로. 검사는 바이트 단위로 한다."""
        return self.UPLOAD_MAX_MB * 1024 * 1024

    @property
    def upload_allowed_ext_list(self) -> list[str]:
        """허용 확장자 목록. 점 없이 소문자로 맞춰둔다 ('jpg')."""
        return [ext.lower().lstrip(".") for ext in split_by_comma(self.UPLOAD_ALLOWED_EXTS)]


def split_by_comma(text: str) -> list[str]:
    """쉼표로 나누고, 앞뒤 공백을 떼고, 빈 칸은 버린다."""
    result = []
    for piece in text.split(","):
        piece = piece.strip()
        if piece:
            result.append(piece)
    return result


# 서버가 켜질 때 딱 한 번 .env 를 읽어서 만들어 둔다.
# 다른 파일에서는 이 settings 를 그대로 가져다 쓰면 된다.
settings = Settings()
