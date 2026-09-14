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

    # 인증번호를 다시 보내기까지 기다려야 하는 시간(초).
    #
    # 없으면 누르는 만큼 메일이 나간다. 남의 주소를 넣고 연타하면 그 사람
    # 받은편지함이 채워지고, 우리 쪽에서는 발송 한도가 깎인다
    # (개인 Gmail 은 하루 500통이다).
    #
    # 만료 시간(5분)보다 짧아야 한다. 길면 코드가 만료된 뒤에도 새로 못 받아
    # 아무것도 할 수 없는 구간이 생긴다.
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
    # mock   : API 키 없이 카탈로그에서 골라준다 (개발용)
    # gemini : Google Gemini
    # claude : Anthropic Claude
    AI_PROVIDER: str = "catalog"

    # Gemini (https://aistudio.google.com 에서 발급)
    GEMINI_API_KEY: str = ""

    # 쉼표로 여러 개를 적으면 앞에서부터 시도한다.
    # 무료 등급은 모델이 붐빌 때(503) 실패하는 일이 잦아서, 예비 모델을
    # 하나 적어두면 시연 중에 화면이 비는 일을 크게 줄일 수 있다.
    GEMINI_MODEL: str = "gemini-3.7-flash,gemini-3.5-flash"

    # Claude (https://console.anthropic.com 에서 발급)
    ANTHROPIC_API_KEY: str = ""
    AI_MODEL: str = "claude-opus-5"

    # ===== Weather =====
    WEATHER_PROVIDER: str = "mock"
    OPENWEATHER_API_KEY: str = ""

    # 위치 권한을 못 받았을 때(거부·미지원) 쓰는 기본 좌표 — 종로2가(탑골공원 인근).
    # 회원가입 지역 선택 기능이 생기기 전까지의 fallback이다.
    DEFAULT_WEATHER_LAT: float = 37.5711
    DEFAULT_WEATHER_LON: float = 126.9885

    # ===== 재료 가격 =====
    # kamis : 공공데이터포털 KAMIS 일별 도·소매 가격정보
    #
    # 네이버 쇼핑 검색 API 는 2026-07-31 종료되어 뺐다. 대체 API 는 없다.
    # 값을 지어내던 mock 프로바이더도 없앴다. 진짜 조사가격과 구분이
    # 안 되는 채로 합계까지 계산돼서, 표의 절반이 가짜였다.
    PRICE_PROVIDERS: str = "kamis"

    # 공공데이터포털 마이페이지의 일반 인증키 — Decoding 값을 넣는다.
    # Encoding 값을 넣으면 httpx 가 한 번 더 인코딩해서 인증 실패한다.
    # https://www.data.go.kr/data/15156057/openapi.do
    DATA_GO_KR_SERVICE_KEY: str = ""

    # ===== 후기 첨부파일 =====
    #
    # 파일 본체는 DB 에 넣지 않는다. 디스크에 저장하고 RECIPE_REVIEWS 의
    # FILE_* 컬럼에는 경로만 적는다 (db/01_schema.sql 주석 참고).
    # BLOB 으로 넣으면 목록 조회마다 사진이 딸려 나오고 백업이 그만큼 불어난다.

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
    def gemini_model_list(self) -> list[str]:
        """GEMINI_MODEL 을 리스트로 바꾼다. 적힌 순서가 곧 우선순위다."""
        return split_by_comma(self.GEMINI_MODEL)

    @property
    def price_provider_list(self) -> list[str]:
        """PRICE_PROVIDERS 를 리스트로 바꾼다. 적힌 순서가 곧 우선순위다."""
        return split_by_comma(self.PRICE_PROVIDERS)

    @property
    def upload_root(self) -> Path:
        """첨부파일 저장 폴더의 절대 경로.

        DB 의 FILE_PATH 는 "uploads/2026/09" 처럼 UPLOAD_DIR 을 포함한
        상대 경로다. 그래서 그 값을 이어붙일 기준은 upload_root 가 아니라
        BASE_DIR 이다 (file_service.absolute_path 참고).
        여기서 만든 절대 경로는 "폴더 밖으로 나가지 않았는지" 검사에 쓴다.
        """
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
