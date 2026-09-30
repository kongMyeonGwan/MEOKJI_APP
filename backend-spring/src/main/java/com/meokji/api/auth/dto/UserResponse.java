package com.meokji.api.auth.dto;

import com.meokji.api.util.Times;
import com.meokji.api.user.vo.UserVO;

import lombok.Data;

// 회원 정보 응답. (비밀번호는 절대 담지 않는다)
// 자바는 isAdmin 처럼 카멜로 쓰고, JSON 으로 나갈 때 is_admin 처럼 바뀐다 (application.yml 설정).
@Data
public class UserResponse {

    private Long id;
    private String email;
    private String nickname;
    private String createdAt; // 가입일 (예: "2026-09-11T21:29:42Z")
    private String gender;
    private String addressSido;
    private String addressSigungu;
    private Boolean isAdmin; // 관리자인지

    // UserVO(DB 한 행) → 응답 모양으로 옮겨 담는다
    public static UserResponse from(UserVO user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setNickname(user.getNickname());
        response.setCreatedAt(Times.toText(user.getCreatedAt()));
        response.setGender(user.getGender());
        response.setAddressSido(user.getAddressSido());
        response.setAddressSigungu(user.getAddressSigungu());
        response.setIsAdmin(user.isAdmin());
        return response;
    }
}
