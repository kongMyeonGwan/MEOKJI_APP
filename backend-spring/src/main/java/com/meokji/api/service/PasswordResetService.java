package com.meokji.api.service;

/**
 * 비밀번호 재설정 (이메일 인증번호). 실제 코드는 PasswordResetServiceImpl 에 있다.
 *
 *   1단계 send   : 6자리 인증번호를 만들어 메일로 보낸다
 *   2단계 verify : 받은 인증번호가 맞는지 확인한다
 *   3단계 change : 새 비밀번호로 바꾸고, 잠긴 계정도 같이 푼다
 */
public interface PasswordResetService {

    void send(String email);

    void verify(String email, String code);

    void change(String email, String code, String newPassword);
}
