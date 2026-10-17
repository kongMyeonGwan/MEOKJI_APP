package com.meokji.api.user.vo;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 회원 탈퇴 요청. 비밀번호를 한 번 더 받는다
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawVO {

    @NotBlank(message = "비밀번호를 입력해주세요")
    private String password;
}
