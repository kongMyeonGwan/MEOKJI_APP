"""개발용 가짜 이메일 Provider.

실제로 보내지 않고 서버 콘솔에 출력한다.
SMTP 계정 없이도 비밀번호 재설정 흐름 전체를 테스트할 수 있다.

logging 대신 print를 쓰는 이유: uvicorn의 기본 로깅 설정에서는 앱 로거가
출력되지 않는 경우가 있어, 개발자가 코드를 못 보고 헤매게 된다.
"""


class MockEmailProvider:
    def send_verification_code(self, to: str, code: str, expire_minutes: int) -> None:
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
