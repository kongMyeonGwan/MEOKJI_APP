"""이메일 설정 점검 스크립트.

.env 의 EMAIL_PROVIDER 설정대로 실제 발송을 한 번 시도해본다.
비밀번호 재설정 흐름을 처음부터 다 돌리지 않고도 SMTP 계정 문제를 바로 확인할 수 있다.

사용법 (backend 디렉터리에서):
    .venv\Scripts\activate
    python -m scripts.check_email                    # SMTP_USER 본인에게 발송
    python -m scripts.check_email you@example.com    # 특정 주소로 발송
"""

import sys

from app.core.config import settings
from app.integrations.email_sender import send_verification_code


def print_settings() -> None:
    """지금 .env 에 어떤 값이 들어있는지 보여준다."""
    print("=" * 56)
    print(f"  EMAIL_PROVIDER : {settings.EMAIL_PROVIDER}")

    if settings.EMAIL_PROVIDER.lower() == "smtp":
        # 포트에 따라 연결 방식이 다르다 (email_sender.py 참고)
        if settings.SMTP_PORT == 465:
            connect_type = "SSL"
        else:
            connect_type = "STARTTLS"

        # 비밀번호는 화면에 그대로 찍지 않는다
        if settings.SMTP_PASSWORD:
            masked_password = "*" * 8
        else:
            masked_password = "(비어있음)"

        print(f"  SMTP_HOST      : {settings.SMTP_HOST}")
        print(f"  SMTP_PORT      : {settings.SMTP_PORT}  ({connect_type})")
        print(f"  SMTP_USER      : {settings.SMTP_USER}")
        print(f"  SMTP_PASSWORD  : {masked_password}")
        print(f"  SMTP_FROM      : {settings.SMTP_FROM or '(SMTP_USER 사용)'}")

    print("=" * 56)


def main() -> int:
    print_settings()

    # 인자로 이메일을 주면 그 주소로, 안 주면 내 계정으로 보낸다
    if len(sys.argv) > 1:
        to = sys.argv[1]
    else:
        to = settings.SMTP_USER

    if not to:
        print("\n받는 사람을 알 수 없습니다. 인자로 이메일을 넘기거나 SMTP_USER를 채우세요.")
        return 1

    print(f"\n{to} 로 테스트 발송합니다...\n")
    send_verification_code(to=to, code="123456", expire_minutes=5)

    print(
        "\n호출이 끝났습니다.\n"
        "  - 위에 '[SMTP 발송 실패]' 나 '[SMTP 설정 오류]' 가 찍혔다면 그게 원인입니다.\n"
        "  - 아무 에러도 없으면 메일함(스팸함 포함)을 확인하세요."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
