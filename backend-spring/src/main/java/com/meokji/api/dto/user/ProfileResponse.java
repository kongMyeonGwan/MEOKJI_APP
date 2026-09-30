package com.meokji.api.dto.user;

import java.math.BigDecimal;

import com.meokji.api.util.Times;
import com.meokji.api.vo.UserVO;

import lombok.Data;

// 마이페이지의 내 정보 응답.
// streakDays(연속 기록일), cookCount(요리 횟수)는 DB 컬럼이 아니라 조회할 때 계산한다.
@Data
public class ProfileResponse {

    private Long id;
    private String email;
    private String nickname;
    private String createdAt;
    private String gender;
    private String addressSido;
    private String addressSigungu;
    private BigDecimal latitude; // 위도
    private BigDecimal longitude; // 경도
    private int streakDays;
    private int cookCount;

    // UserVO + 계산한 값 → 응답 모양으로 옮겨 담는다
    public static ProfileResponse of(UserVO user, int streakDays, int cookCount) {
        ProfileResponse response = new ProfileResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setNickname(user.getNickname());
        response.setCreatedAt(Times.toText(user.getCreatedAt()));
        response.setGender(user.getGender());
        response.setAddressSido(user.getAddressSido());
        response.setAddressSigungu(user.getAddressSigungu());
        response.setLatitude(user.getLatitude());
        response.setLongitude(user.getLongitude());
        response.setStreakDays(streakDays);
        response.setCookCount(cookCount);
        return response;
    }
}
