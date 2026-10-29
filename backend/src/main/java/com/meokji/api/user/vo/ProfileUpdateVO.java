package com.meokji.api.user.vo;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 내 정보 수정 요청 (PATCH). 보내지 않은 칸(null)은 바꾸지 않는다.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateVO {

    @Size(min = 1, max = 50, message = "닉네임은 50자 이하여야 합니다")
    private String nickname;

//    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "성별 값이 올바르지 않습니다")
    @Pattern(regexp = "(MALE|FEMALE|OTHER)?", message = "성별 값이 올바르지 않습니다")
    private String gender;

    @Size(max = 30)
    private String addressSido;

    @Size(max = 30)
    private String addressSigungu;

    @Size(max = 30)
    private String addressDong;
}
