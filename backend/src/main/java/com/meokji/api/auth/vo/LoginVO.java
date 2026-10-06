package com.meokji.api.auth.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 로그인 요청. 모양: { "email": "...", "password": "...", "keep": true }
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요")
    private String password;

    /**
     * 화면의 "로그인 상태 유지" 체크 여부.
     *
     * Refresh Token 쿠키를 얼마나 남길지가 이 값으로 갈린다.
     *
     * <pre>
     *   true  : 14일짜리 쿠키. 브라우저를 껐다 켜도 남아서 자동으로 로그인된다.
     *   false : 수명을 안 적은 쿠키. 브라우저를 닫으면 사라진다 (공용 PC 용).
     * </pre>
     *
     * 안 보내면 false 다. 빠뜨렸을 때 덜 오래 남는 쪽이 안전해서 그렇게 뒀다.
     * (Refresh Token 자체의 유효기간은 어느 쪽이든 14일이다. 쿠키가 먼저
     *  사라지면 그 토큰을 꺼내 쓸 길이 없어질 뿐이다)
     *
     * boolean 이 아니라 Boolean 인 이유
     *   이 칸을 안 보내는 요청이 있다(예전 화면, 테스트). 그때 Jackson 이 null 을
     *   넣으려 하는데 boolean 은 null 을 못 받아서 요청 전체가 400 으로 튕긴다.
     *   Boolean 으로 두면 null 이 들어오고, 아래 isKeep() 이 false 로 읽어준다.
     */
    private Boolean keep;

    /** 안 보냈으면(null) 체크 안 한 것으로 본다. */
    public boolean isKeep() {
        return keep != null && keep;
    }
}
