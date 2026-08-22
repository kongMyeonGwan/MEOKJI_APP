"""이메일 발송 인터페이스.

서비스 계층은 이 인터페이스만 알고, 실제로 Mock인지 SMTP인지는 모른다.
Provider를 갈아끼워도 비즈니스 로직은 그대로다.
"""

from typing import Protocol


class EmailProvider(Protocol):
    """이메일 발송자가 갖춰야 할 모양."""

    def send_verification_code(self, to: str, code: str, expire_minutes: int) -> None:
        """인증번호를 보낸다.

        실패해도 예외를 던지지 않는 것을 원칙으로 한다.
        메일 서버가 죽었다고 해서 "이 이메일이 가입되어 있는가"라는 정보가
        응답 차이로 새어나가면 안 되기 때문이다.
        """
        ...
