package com.meokji.api.recommend;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.meokji.api.ban.BanMapper;
import com.meokji.api.catalog.FoodScorer;
import com.meokji.api.recommend.dto.BargainItem;
import com.meokji.api.recommend.dto.FoodItem;
import com.meokji.api.recommend.dto.RecommendRequest;
import com.meokji.api.recommend.dto.RecommendResponse;
import com.meokji.api.user.User;
import com.meokji.api.weather.WeatherService;
import com.meokji.api.weather.dto.WeatherResponse;

/**
 * 음식 추천.
 *
 * <p>파이썬 쪽 {@code app/services/ai_service.py} 의 추천 부분이다. AI 는 부르지
 * 않는다 - 카탈로그 100종에 점수를 매겨 고른다({@link FoodScorer}).
 */
@Service
public class RecommendService {

    /**
     * 후보가 0건일 때 예산을 넓혀보는 폭(원).
     *
     * <p>"8천원 이하" 로 검색했는데 아무것도 없으면 화면이 통째로 빈다. 조금만
     * 넓히면 나올 때가 많아서 한 번 더 물어본다 - 그 사실은 condition 문구에
     * 원래 예산으로 적혀 있어서 사용자가 알아볼 수 있다.
     */
    private static final int BUDGET_RELAX = 3_000;

    private static final NumberFormat WON = NumberFormat.getIntegerInstance(Locale.KOREA);

    private final FoodScorer scorer;
    private final WeatherService weatherService;
    private final BanMapper bans;

    public RecommendService(
            FoodScorer scorer,
            WeatherService weatherService,
            BanMapper bans) {
        this.scorer = scorer;
        this.weatherService = weatherService;
        this.bans = bans;
    }

    /** 조건에 맞는 후보를 만들어 돌려준다. 로그인하지 않았으면 Ban 이 비어 있다. */
    public RecommendResponse recommend(User me, RecommendRequest request) {
        List<String> allergies = banNames(me, "ALLERGY");
        List<String> dislikes = banNames(me, "DISLIKE");

        WeatherResponse weather = null;
        if (request.weatherWanted()) {
            Double[] coords = pickCoords(request, me);
            weather = weatherService.current(coords[0], coords[1]);
        }

        List<FoodItem> items = scorer.recommend(
                request.limitOrDefault(),
                request.category(),
                request.budgetMin(),
                request.budgetMax(),
                request.query(),
                weather,
                allergies,
                dislikes,
                request.excludeOrEmpty());

        // 후보가 하나도 없으면 예산을 조금 넓혀서 한 번만 더 본다.
        if (items.isEmpty() && request.budgetMax() != null) {
            items = scorer.recommend(
                    request.limitOrDefault(),
                    request.category(),
                    request.budgetMin(),
                    request.budgetMax() + BUDGET_RELAX,
                    request.query(),
                    weather,
                    allergies,
                    dislikes,
                    request.excludeOrEmpty());
        }

        // 떨이 재료는 아직 옮기지 않았다. KAMIS 품목 116개를 훑어 일주일 전과
        // 비교하는 일이라 시세 조회를 옮긴 뒤에 붙인다. use_bargains 를 켜도
        // 빈 목록이 온다 - 화면은 빈 목록을 이미 견딘다.
        List<BargainItem> bargains = List.of();

        return new RecommendResponse(
                describe(request, weather, allergies.size() + dislikes.size()),
                items,
                weather,
                bargains);
    }

    /** 로그인 사용자의 Ban 재료. 비로그인이면 빈 목록. */
    private List<String> banNames(User me, String banType) {
        if (me == null) {
            return List.of();
        }

        return bans.findActiveNames(me.getId(), banType);
    }

    /**
     * 어느 좌표로 날씨를 볼지 정한다.
     *
     * <p>요청에 실려 온 좌표(브라우저 위치 권한)가 가장 우선이다. 없으면 회원이
     * 가입할 때 고른 주소의 좌표를 쓰고, 그것도 없으면 {@code null} 을 돌려
     * {@link WeatherService} 가 기본 자리를 쓰게 한다.
     */
    private static Double[] pickCoords(RecommendRequest request, User me) {
        if (request.lat() != null && request.lon() != null) {
            return new Double[] {request.lat(), request.lon()};
        }

        if (me != null && me.getLatitude() != null && me.getLongitude() != null) {
            // DB 는 NUMBER(9,6) 이라 BigDecimal 로 온다.
            return new Double[] {
                    me.getLatitude().doubleValue(), me.getLongitude().doubleValue(),
            };
        }

        // 둘 다 null 이면 WeatherService 가 기본 자리(서울시청 근처)를 쓴다.
        return new Double[] {null, null};
    }

    /**
     * 무슨 조건으로 골랐는지 한 줄로 적는다.
     *
     * <p>화면이 목록 위에 그대로 띄운다. 이게 없으면 추천이 아무렇게나 고른 것처럼
     * 읽힌다 - 특히 날씨를 반영했을 때 "왜 국물만 나오지" 가 된다.
     */
    private static String describe(
            RecommendRequest request, WeatherResponse weather, int banCount) {

        List<String> parts = new ArrayList<>();

        if (weather != null) {
            parts.add(weather.city() + " · " + weather.conditionText() + " "
                    + weather.temp() + "°");
        }

        if (request.category() != null && !request.category().isBlank()) {
            parts.add(request.category());
        }

        if (request.query() != null && !request.query().isBlank()) {
            parts.add("\"" + request.query() + "\"");
        }

        String budget = describeBudget(request.budgetMin(), request.budgetMax());
        if (!budget.isEmpty()) {
            parts.add(budget);
        }

        if (banCount > 0) {
            parts.add("못 먹는 재료 " + banCount + "건 제외");
        }

        return parts.isEmpty() ? "조건 없음" : String.join(" · ", parts);
    }

    /** 예산 조건을 사람이 읽는 문구로. */
    private static String describeBudget(Integer low, Integer high) {
        if (low == null && high == null) {
            return "";
        }

        if (low == null) {
            return WON.format(high) + "원 이하";
        }

        if (high == null) {
            return WON.format(low) + "원 이상";
        }

        return WON.format(low) + "원 ~ " + WON.format(high) + "원";
    }
}
