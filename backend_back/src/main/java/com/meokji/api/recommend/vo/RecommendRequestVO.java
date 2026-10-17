package com.meokji.api.recommend.vo;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import lombok.Data;

// 음식 추천 요청. 조건은 전부 선택이라 안 보내도 된다.
// 모양: { "limit": 5, "category": "한식", "budget_max": 10000, "use_weather": true, ... }
//
// 칸이 전부 감싼 타입(Integer · Boolean · Double)인 이유 — "안 보냄" 을
// 구별해야 한다. int 면 안 보낸 것이 0 이 되어 "예산 0원" 이 되고,
// boolean 이면 안 보낸 것이 false 가 되어 "날씨 안 봄" 이 된다.
//
// @NotNull 이 하나도 없다. 대시보드가 처음 뜰 때 빈 객체({})로 부르고,
// 서버가 기본값(5개)으로 추천한다. 조건을 꼭 받으면 첫 화면이 안 뜬다.
//
// 요청 전용이다. 응답은 RecommendVO 가 맡는다
@Data
public class RecommendRequestVO {

    // 어디서 온 요청인지 (화면이 기록용으로 보낸다)
    //
    // 추천 결과에는 영향을 주지 않는다. "홈에서 눌렀는지, 검색에서
    // 눌렀는지" 를 나중에 들여다볼 수 있게 받아두는 칸이다
    private String source;

    // 몇 개 추천받을지 (안 보내면 5개)
    //
    // @Max(12) 로 막는 이유 — 카탈로그에 음식이 100개뿐이고, 조건을 걸면
    // 후보가 더 줄어든다. 50개를 달라고 해도 채울 수 없다. 화면도
    // 카드를 12개까지만 그린다
    @Min(1)
    @Max(12)
    private Integer limit;

    // 한식, 중식 ... (catalog.json 의 categories 에 있는 label)
    //
    // key("KR")가 아니라 label("한식")을 받는다. 음식 쪽 데이터가
    // label 로 적혀 있어서, 비교하려면 같은 글자여야 한다
    @Size(max = 20)
    private String category;

    // 예산 범위(원). 재료비 기준이고, 둘 중 하나만 보내도 된다.
    //
    // 100만원까지 받는 이유 — 한 끼 재료비가 그렇게 될 수는 없지만,
    // 화면의 슬라이더가 잘못 계산해 큰 값을 보내도 서버가 터지지 않게 둔다
    @Min(0)
    @Max(1000000)
    private Integer budgetMin;

    @Min(0)
    @Max(1000000)
    private Integer budgetMax;

    // 검색어 ("8천원 이하 파스타")
    //
    // 문장을 그대로 받는다. 서버가 그 안에서 금액 · 분류 · 키워드를
    // 읽어낸다 - 화면이 쪼개 보내면 쪼개는 규칙이 화면에 들어가고,
    // 규칙을 고칠 때 화면과 서버를 같이 고쳐야 한다
    @Size(max = 150)
    private String query;

    // 이미 보여준 메뉴 (다시 안 뽑는다)
    //
    // "다른 메뉴 추천" 을 누를 때 화면이 지금까지 본 이름을 실어 보낸다.
    // 서버가 기억하지 않는 이유 — 기억하려면 사람마다 "최근에 본 목록" 을
    // 들고 있어야 하고, 로그인 안 한 사람은 가릴 방법이 없다
    //
    // @Size(max = 30) 은 목록의 길이를 막는다. 글자 수가 아니다.
    // 30개를 빼면 후보가 거의 안 남으므로 그 선에서 끊는다
    @Size(max = 30)
    private List<String> exclude;

    // true 면 "요즘 싼 재료" 도 같이 준다
    //
    // 기본으로 안 주는 이유 — 바깥 API(KAMIS)를 여러 번 불러야 해서 느리다.
    // 대시보드의 「지금 싼 재료로」 카드만 이것을 켜서 부른다
    private Boolean useBargains;

    // true 면 날씨도 같이 보고 추천한다
    //
    // 켜면 추천 점수가 달라진다(더운 날엔 냉면 쪽에 가점). 응답의
    // weather 칸도 이때만 채워진다
    private Boolean useWeather;

    // 내 위치. 날씨를 부를 때 쓴다.
    //
    // 저장된 주소보다 이 값을 먼저 쓴다 - 지금 여행 중이면 집 주소의
    // 날씨는 맞지 않는다. 브라우저가 위치를 알려주면 화면이 실어 보낸다
    //
    // @Min/@Max 범위는 위도 · 경도가 가질 수 있는 값의 한계다.
    // 벗어난 값을 바깥 API 에 보내면 그쪽에서 에러가 나고,
    // 그 에러는 우리 화면에 "날씨를 못 가져왔다" 로만 보인다
    @Min(-90)
    @Max(90)
    private Double lat;

    @Min(-180)
    @Max(180)
    private Double lon;
}
