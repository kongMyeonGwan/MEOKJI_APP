package com.meokji.api.recommend.vo;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import lombok.Data;

// 음식 추천 요청. 조건은 전부 선택이라 안 보내도 된다.
// 모양: { "limit": 5, "category": "한식", "budget_max": 10000, "use_weather": true, ... }
@Data
public class RecommendRequestVO {

    private String source; // 어디서 온 요청인지 (화면이 기록용으로 보낸다)

    @Min(1)
    @Max(12)
    private Integer limit; // 몇 개 추천받을지 (안 보내면 5개). 페이지를 쓰면 한 페이지 크기

    @Min(1)
    @Max(1000)
    private Integer page; // 몇 번째 페이지 (안 보내면 1페이지 = 앞에서 limit 개)

    @Size(max = 20)
    private String category; // 한식, 중식 ...

    @Min(0)
    @Max(1000000)
    private Integer budgetMin; // 예산 최소

    @Min(0)
    @Max(1000000)
    private Integer budgetMax; // 예산 최대

    @Size(max = 150)
    private String query; // 검색어 ("8천원 이하 파스타")

    @Size(max = 30)
    private List<String> exclude; // 이미 보여준 메뉴 (다시 안 뽑는다)

    private Boolean useBargains; // true 면 "요즘 싼 재료" 도 같이 준다

    private Boolean useWeather; // true 면 날씨도 같이 보고 추천한다

    @Min(-90)
    @Max(90)
    private Double lat; // 내 위치 위도

    @Min(-180)
    @Max(180)
    private Double lon; // 내 위치 경도
}
