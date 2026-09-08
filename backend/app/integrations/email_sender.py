"""이메일 발송.

.env 의 EMAIL_PROVIDER 값에 따라 두 가지로 동작한다.

    mock   실제로 보내지 않고 서버 콘솔에 인증번호를 출력한다 (개발용)
    smtp   네이버 · Gmail 같은 진짜 메일 서버로 보낸다

바깥에서는 send_verification_code() 하나만 쓰면 된다.

파일 이름이 email.py 가 아니라 email_sender.py 인 이유
------------------------------------------------------
파이썬 표준 라이브러리에 이미 email 이라는 이름이 있다.
같은 이름을 쓰면 아래 `from email.message import EmailMessage` 가
어느 쪽을 가리키는지 헷갈리므로 이름을 다르게 지었다.
"""

import smtplib
import ssl
from email.message import EmailMessage

from app.core.config import settings

# 메일 서버가 느리거나 방화벽에 막혔을 때 무한정 기다리지 않게 한다.
SMTP_TIMEOUT_SECONDS = 10

# 포트에 따라 접속 방식이 다르다.
#   587  STARTTLS  평문으로 연결한 뒤 TLS 로 올린다 (대부분 이 쪽)
#   465  SSL       처음부터 TLS 로 연결한다
# 잘못 맞추면 에러도 없이 멈춰 있다가 타임아웃이 나므로 포트로 자동 판별한다.
SSL_PORT = 465


def send_verification_code(to: str, code: str, expire_minutes: int) -> None:
    """비밀번호 재설정 인증번호를 보낸다.

    발송에 실패해도 예외를 던지지 않는다.
    메일 서버가 죽었을 때만 500 이 나가면, 응답이 다르다는 것만으로
    "이 이메일은 가입되어 있다"는 사실이 밖으로 새기 때문이다.
    대신 서버 콘솔에 원인을 남겨서 개발자가 볼 수 있게 한다.
    """
    if settings.EMAIL_PROVIDER.lower() == "smtp":
        send_by_smtp(to, code, expire_minutes)
    else:
        print_to_console(to, code, expire_minutes)


def print_to_console(to: str, code: str, expire_minutes: int) -> None:
    """개발용. 메일 대신 서버 콘솔에 인증번호를 찍는다.

    SMTP 계정 없이도 비밀번호 재설정 화면을 처음부터 끝까지 눌러볼 수 있다.

    logging 이 아니라 print 를 쓰는 이유: uvicorn 기본 설정에서는 앱 로거가
    화면에 안 나올 때가 있어서, 인증번호를 못 찾고 헤매게 된다.
    """
    print(
        "\n"
        "===================== [MOCK EMAIL] =====================\n"
        f" 받는 사람 : {to}\n"
        f" 인증번호  : {code}\n"
        f" 유효시간  : {expire_minutes}분\n"
        "  (실제로 발송되지 않았습니다. EMAIL_PROVIDER=mock)\n"
        "========================================================\n",
        flush=True,
    )


def send_by_smtp(to: str, code: str, expire_minutes: int) -> None:
    """진짜 메일 서버로 보낸다."""
    # .env 가 덜 채워졌으면 여기서 알려준다.
    # 이걸 안 하면 "메일이 안 온다"는 증상만 보이고 원인을 찾기 어렵다.
    missing = []
    if not settings.SMTP_HOST:
        missing.append("SMTP_HOST")
    if not settings.SMTP_USER:
        missing.append("SMTP_USER")
    if not settings.SMTP_PASSWORD:
        missing.append("SMTP_PASSWORD")

    if missing:
        print(
            f"\n[SMTP 설정 오류] {', '.join(missing)} 이(가) 비어 있습니다. "
            "backend/.env 를 확인하세요.\n",
            flush=True,
        )
        return

    # SMTP_FROM 을 안 적었으면 로그인 계정을 보내는 사람으로 쓴다.
    # 대부분의 메일 서버는 로그인 계정과 다른 발신자를 거부한다.
    sender = settings.SMTP_FROM or settings.SMTP_USER

    message = EmailMessage()
    message["Subject"] = "[뭐먹지?] 비밀번호 재설정 인증번호"
    message["From"] = sender
    message["To"] = to
    message.set_content(
        "안녕하세요, 뭐먹지? 입니다.\n\n"
        f"비밀번호 재설정 인증번호는 [ {code} ] 입니다.\n"
        f"{expire_minutes}분 안에 입력해주세요.\n\n"
        "본인이 요청한 것이 아니라면 이 메일을 무시하셔도 됩니다.\n"
        "누군가 회원님의 이메일 주소를 잘못 입력했을 수 있습니다.\n"
    )

    try:
        connect_and_send(message)
    except Exception as exc:
        # 어떤 에러든 여기서 멈춘다. 위 docstring 참고.
        print(
            "\n[SMTP 발송 실패] "
            f"{type(exc).__name__}: {exc}\n"
            f"  host={settings.SMTP_HOST} port={settings.SMTP_PORT} "
            f"user={settings.SMTP_USER}\n",
            flush=True,
        )


def connect_and_send(message: EmailMessage) -> None:
    """메일 서버에 접속해서 실제로 보낸다. 실패하면 예외가 난다."""
    context = ssl.create_default_context()

    if settings.SMTP_PORT == SSL_PORT:
        # 465 : 처음부터 TLS
        with smtplib.SMTP_SSL(
            settings.SMTP_HOST,
            settings.SMTP_PORT,
            timeout=SMTP_TIMEOUT_SECONDS,
            context=context,
        ) as server:
            server.login(settings.SMTP_USER, settings.SMTP_PASSWORD)
            server.send_message(message)
        return

    # 587 등 : 평문으로 붙은 뒤 starttls() 로 올린다
    with smtplib.SMTP(
        settings.SMTP_HOST, settings.SMTP_PORT, timeout=SMTP_TIMEOUT_SECONDS
    ) as server:
        server.starttls(context=context)
        server.login(settings.SMTP_USER, settings.SMTP_PASSWORD)
        server.send_message(message)
