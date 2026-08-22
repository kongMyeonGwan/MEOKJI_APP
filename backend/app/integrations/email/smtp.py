"""실제 이메일 발송 Provider (SMTP).

네이버 / Gmail 등 표준 SMTP 서버를 그대로 쓴다. 별도 라이브러리 없이
파이썬 표준 라이브러리(smtplib)만 사용한다.

포트에 따라 접속 방식이 다르다.
    587  STARTTLS  평문으로 연결한 뒤 TLS로 승격 (권장)
    465  SSL       처음부터 TLS로 연결
잘못 조합하면 응답 없이 멈춰 있다가 타임아웃이 나므로 포트로 자동 판별한다.
"""

import smtplib
import ssl
from email.message import EmailMessage

# 메일 서버가 느리거나 방화벽에 막혔을 때 요청이 무한정 매달리지 않게 한다.
# 이 값을 넘기면 발송을 포기하고 에러를 로그로 남긴다.
SMTP_TIMEOUT_SECONDS = 10

SSL_PORT = 465


class SmtpEmailProvider:
    def __init__(
        self,
        host: str,
        port: int,
        user: str,
        password: str,
        sender: str,
    ) -> None:
        # 설정 누락은 여기서 즉시 터뜨린다.
        # 발송 시점까지 미루면 "메일이 안 온다"는 증상으로만 나타나 원인을 찾기 어렵다.
        missing = [
            name
            for name, value in (
                ("SMTP_HOST", host),
                ("SMTP_USER", user),
                ("SMTP_PASSWORD", password),
            )
            if not value
        ]
        if missing:
            raise RuntimeError(
                f"EMAIL_PROVIDER=smtp 인데 {', '.join(missing)} 이(가) 비어 있습니다. "
                "backend/.env 를 확인하세요."
            )

        self.host = host
        self.port = port
        self.user = user
        self.password = password
        # SMTP_FROM을 안 적었으면 로그인 계정을 보내는 사람으로 쓴다.
        # 대부분의 메일 서버는 계정과 다른 발신자를 거부한다.
        self.sender = sender or user

    def send_verification_code(self, to: str, code: str, expire_minutes: int) -> None:
        message = EmailMessage()
        message["Subject"] = "[뭐먹지?] 비밀번호 재설정 인증번호"
        message["From"] = self.sender
        message["To"] = to
        message.set_content(
            "안녕하세요, 뭐먹지? 입니다.\n\n"
            f"비밀번호 재설정 인증번호는 [ {code} ] 입니다.\n"
            f"{expire_minutes}분 안에 입력해주세요.\n\n"
            "본인이 요청한 것이 아니라면 이 메일을 무시하셔도 됩니다.\n"
            "누군가 회원님의 이메일 주소를 잘못 입력했을 수 있습니다.\n"
        )

        try:
            self._send(message)
        except Exception as exc:
            # 발송 실패를 예외로 올리지 않는다.
            # 500이 나가면 "이 이메일은 가입되어 있다"는 사실이 응답 차이로 새어나간다.
            # 대신 서버 로그에 원인을 남겨 개발자가 볼 수 있게 한다.
            print(
                "\n[SMTP 발송 실패] "
                f"{type(exc).__name__}: {exc}\n"
                f"  host={self.host} port={self.port} user={self.user}\n",
                flush=True,
            )

    def _send(self, message: EmailMessage) -> None:
        context = ssl.create_default_context()

        if self.port == SSL_PORT:
            with smtplib.SMTP_SSL(
                self.host, self.port, timeout=SMTP_TIMEOUT_SECONDS, context=context
            ) as server:
                server.login(self.user, self.password)
                server.send_message(message)
            return

        with smtplib.SMTP(self.host, self.port, timeout=SMTP_TIMEOUT_SECONDS) as server:
            server.starttls(context=context)
            server.login(self.user, self.password)
            server.send_message(message)
