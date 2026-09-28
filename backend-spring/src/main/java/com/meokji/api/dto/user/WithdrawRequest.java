package com.meokji.api.dto.user;

import jakarta.validation.constraints.NotBlank;

/**
 * 회원 탈퇴.
 *
 * <p>비밀번호를 한 번 더 받는다. 토큰만으로 지울 수 있게 두면 남의 기기에서
 * 열린 화면으로 탈퇴가 된다.
 */
public record WithdrawRequest(

        @NotBlank(message = "비밀번호를 입력해주세요")
        String password) {
}
