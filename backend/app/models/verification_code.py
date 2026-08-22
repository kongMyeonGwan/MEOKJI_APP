"""VERIFICATION_CODES 테이블 모델 (이메일 OTP).

스키마의 단일 소스는 db/01_schema.sql 이다.
"""

from datetime import datetime

from sqlalchemy import ForeignKey, Integer, String, text
from sqlalchemy.orm import Mapped, mapped_column

from app.database.database import Base
from app.models.types import UtcTimestamp

# DDL의 ck_vc_purpose CHECK 제약과 값이 일치해야 한다.
PURPOSE_PASSWORD_RESET = "PASSWORD_RESET"
PURPOSE_EMAIL_VERIFY = "EMAIL_VERIFY"
PURPOSE_ACCOUNT_UNLOCK = "ACCOUNT_UNLOCK"


class VerificationCode(Base):
    __tablename__ = "verification_codes"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)

    user_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )

    # 6자리 원문은 이메일로만 나가고 DB에는 bcrypt 해시만 남긴다.
    # DB가 유출돼도 코드를 그대로 읽어 계정을 탈취할 수 없게 하기 위함이다.
    code_hash: Mapped[str] = mapped_column(String(255), nullable=False)

    purpose: Mapped[str] = mapped_column(String(30), nullable=False)

    expires_at: Mapped[datetime] = mapped_column(UtcTimestamp(), nullable=False)

    # 무차별 대입 방지. OTP_MAX_ATTEMPT(기본 5)에 도달하면 이 코드는 폐기한다.
    attempt_count: Mapped[int] = mapped_column(
        Integer, nullable=False, server_default="0"
    )

    # 인증에 성공한 시각. NULL이면 아직 검증 전이다.
    # verify와 change가 별도 엔드포인트라서, change 단계에서 "검증된 요청인지"를
    # 판별할 근거가 필요하다. 이 컬럼이 그 역할을 한다.
    used_at: Mapped[datetime | None] = mapped_column(UtcTimestamp(), nullable=True)

    created_at: Mapped[datetime] = mapped_column(
        UtcTimestamp(),
        nullable=False,
        server_default=text("SYS_EXTRACT_UTC(SYSTIMESTAMP)"),
    )

    def __repr__(self) -> str:
        return (
            f"<VerificationCode id={self.id} user_id={self.user_id} "
            f"purpose={self.purpose!r} used={self.used_at is not None}>"
        )
