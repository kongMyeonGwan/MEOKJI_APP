"""VERIFICATION_CODES 테이블 모델 (이메일 OTP)."""

from datetime import datetime

from sqlalchemy import ForeignKey, Integer, String, text
from sqlalchemy.orm import Mapped, mapped_column

from app.database.database import Base
from app.models.types import UtcTimestamp

# DDL의 ck_vc_purpose CHECK 제약과 값이 일치해야 한다.
# DDL 은 EMAIL_VERIFY · ACCOUNT_UNLOCK 도 허용하지만 아직 안 쓴다.
PURPOSE_PASSWORD_RESET = "PASSWORD_RESET"


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
