"""이메일 설정 점검 스크립트.

.env의 EMAIL_PROVIDER 설정대로 실제 발송을 한 번 시도해본다.
비밀번호 재설정 흐름 전체를 돌리지 않고도 SMTP 계정 문제를 바로 확인할 수 있다.

사용법 (backend 디렉터리에서):
    .venv\\Scripts\\activate
    python -m scripts.check_email                    # SMTP_USER 본인에게 발송
    python -m scripts.check_email you@example.com    # 특정 주소로 발송
"""

import sys

from app.core.config import settings
from app.integrations.email import get_email_provider


def main() -> int:
    print("=" * 56)
    print(f"  EMAIL_PROVIDER : {settings.EMAIL_PROVIDER}")

    if settings.EMAIL_PROVIDER.lower() == "smtp":
        print(f"  SMTP_HOST      : {settings.SMTP_HOST}")
        print(f"  SMTP_PORT      : {settings.SMTP_PORT}"
              f"  ({'SSL' if settings.SMTP_PORT == 465 else 'STARTTLS'})")
        print(f"  SMTP_USER      : {settings.SMTP_USER}")
        print(f"  SMTP_PASSWORD  : {'*' * 8 if settings.SMTP_PASSWORD else '(비어있음)'}")
        print(f"  SMTP_FROM      : {settings.SMTP_FROM or '(SMTP_USER 사용)'}")
    print("=" * 56)

    to = sys.argv[1] if len(sys.argv) > 1 else settings.SMTP_USER
    if not to:
        print("\n받는 사람을 알 수 없습니다. 인자로 이메일을 넘기거나 SMTP_USER를 채우세요.")
        return 1

    try:
        provider = get_email_provider()
    except RuntimeError as exc:
        print(f"\n[설정 오류] {exc}")
        return 1

    print(f"\n{to} 로 테스트 발송합니다...\n")
    provider.send_verification_code(to=to, code="123456", expire_minutes=5)

    print(
        "\n호출이 끝났습니다.\n"
        "  - 위에 '[SMTP 발송 실패]'가 찍혔다면 그 메시지가 원인입니다.\n"
        "  - 아무 에러도 없으면 메일함(스팸함 포함)을 확인하세요."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
