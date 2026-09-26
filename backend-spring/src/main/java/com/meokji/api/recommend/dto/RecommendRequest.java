package com.meokji.api.recommend.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 추천 요청. 전부 선택 항목이라 빈 본문({@code {}})으로도 부를 수 있다.
 *
 * <p>위경도를 받는 이유는 브라우저 위치 권한을 허용한 사람에게 지금 있는 곳의
 * 날씨를 보여주기 위해서다. 안 보내면 회원 주소의 좌표를 쓰고, 그것도 없으면
 * 서울시청 근처로 떨어뜨린다.
 */
public record RecommendRequest(

        /** 어디서 온 요청인지. 화면이 기록용으로 보낸다. */
        String source,

        @Min(1) @Max(12) Integer limit,

        @Size(max = 20) String category,

        @Min(0) @Max(1_000_000) Integer budgetMin,
        @Min(0) @Max(1_000_000) Integer budgetMax,

        @Size(max = 150) String query,

        /** 이미 보여준 메뉴. 다시 뽑지 않는다 ("다른 메뉴 보기"). */
        @Size(max = 30) List<String> exclude,

        Boolean useBargains,

        Boolean useWeather,
        @Min(-90) @Max(90) Double lat,
        @Min(-180) @Max(180) Double lon) {

    /** 한 번에 만들 후보 개수. 값이 없거나 이상하면 5개. */
    public int limitOrDefault() {
        if (limit == null || limit < 1) {
            return 5;
        }

        return Math.min(limit, 12);
    }

    public boolean weatherWanted() {
        return Boolean.TRUE.equals(useWeather);
    }

    public boolean bargainsWanted() {
        return Boolean.TRUE.equals(useBargains);
    }

    public List<String> excludeOrEmpty() {
        return exclude == null ? List.of() : exclude;
    }
}
