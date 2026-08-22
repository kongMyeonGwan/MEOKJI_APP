"""이메일 Provider 선택.

.env의 EMAIL_PROVIDER 값에 따라 구현체를 고른다.
"""

from functools import lru_cache

from app.core.config import settings
from app.integrations.email.base import EmailProvider
from app.integrations.email.mock import MockEmailProvider
from app.integrations.email.smtp import SmtpEmailProvider

__all__ = ["EmailProvider", "get_email_provider"]


@lru_cache
def get_email_provider() -> EmailProvider:
    """설정에 맞는 Provider를 돌려준다 (한 번만 만들어 재사용)."""
    provider = settings.EMAIL_PROVIDER.lower()

    if provider == "mock":
        return MockEmailProvider()

    if provider == "smtp":
        return SmtpEmailProvider(
            host=settings.SMTP_HOST,
            port=settings.SMTP_PORT,
            user=settings.SMTP_USER,
            password=settings.SMTP_PASSWORD,
            sender=settings.SMTP_FROM,
        )

    # 조용히 mock으로 떨어뜨리면 운영에서 메일이 안 나가는데
    # 아무도 모르는 상황이 되므로 에러를 낸다.
    raise RuntimeError(
        f"지원하지 않는 EMAIL_PROVIDER: {settings.EMAIL_PROVIDER!r}. "
        "사용 가능한 값: mock, smtp"
    )
