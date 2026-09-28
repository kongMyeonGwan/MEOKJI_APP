package com.meokji.api.recommend;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meokji.api.ban.BanMapper;
import com.meokji.api.catalog.FoodScorer;
import com.meokji.api.common.Numbers;
import com.meokji.api.price.BargainService;
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

    @Autowired
    private FoodScorer scorer;
    @Autowired
    private WeatherService weatherService;
    @Autowired
    private BanMapper bans;
    @Autowired
    private BargainService bargainService;


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

        // 요즘 값이 내린 재료. 대시보드의 「지금 싼 재료로」 카드만 켜서 보낸다.
        //
        // 고른 메뉴에는 영향을 주지 않는다. 원래는 이 목록을 AI 프롬프트에 넣어
        // "이 재료를 쓰는 메뉴" 를 고르게 했는데, STEP 43 에서 AI 호출을 걷어내며
        // 그 길이 끊겼다. 파이썬도 지금 같은 상태다 - catalog_recommend_foods 가
        // context["bargains"] 를 읽지 않는다. 화면은 이 값으로 칩만 그린다.
        List<BargainItem> bargains =
                request.bargainsWanted() ? bargainService.current() : List.of();

        return new RecommendResponse(
                describe(request, weather, bargains, allergies.size() + dislikes.size()),
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
            RecommendRequest request,
            WeatherResponse weather,
            List<BargainItem> bargains,
            int banCount) {

        List<String> parts = new ArrayList<>();

        if (weather != null) {
            parts.add(weather.city() + " · " + weather.conditionText() + " "
                    + weather.temp() + "°");
        }

        if (!bargains.isEmpty()) {
            // "시세 대파 12%↓ 외 3건" - 무슨 근거로 골랐는지 한눈에 보이게 한다.
            BargainItem first = bargains.get(0);

            // Math.round 가 아니라 Numbers.round 다. 파이썬의 f"{...:.0f}" 는
            // 짝수로 맞춤이라 12.5 가 12 가 된다 - Math.round 는 13 이다.
            String text = first.name() + " "
                    + Numbers.round(Math.abs(first.percent())) + "%↓";

            if (bargains.size() > 1) {
                text += " 외 " + (bargains.size() - 1) + "건";
            }

            parts.add("시세 " + text);
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
