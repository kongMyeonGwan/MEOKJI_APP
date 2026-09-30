package com.meokji.api.recommend.service;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meokji.api.ban.dao.BanDAO;
import com.meokji.api.recommend.dto.BargainItem;
import com.meokji.api.recommend.dto.FoodItem;
import com.meokji.api.recommend.dto.RecommendRequest;
import com.meokji.api.recommend.dto.RecommendResponse;
import com.meokji.api.recommend.dto.WeatherResponse;
import com.meokji.api.util.FoodScorer;
import com.meokji.api.util.Numbers;
import com.meokji.api.user.vo.UserVO;

/**
 * 음식 추천.
 * 음식 목록(catalog.json, 약 100종)에 조건별로 점수를 매겨서 높은 순으로 고른다. (FoodScorer)
 */
@Service
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private FoodScorer foodScorer;

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private BargainService bargainService;

    @Autowired
    private BanDAO banDAO;

    @Override
    public RecommendResponse recommend(UserVO loginUser, RecommendRequest request) {
        // 1. 로그인했으면 못 먹는 재료를 가져온다
        List<String> allergies = new ArrayList<>();
        List<String> dislikes = new ArrayList<>();
        if (loginUser != null) {
            allergies = banDAO.findActiveNames(loginUser.getId(), "ALLERGY");
            dislikes = banDAO.findActiveNames(loginUser.getId(), "DISLIKE");
        }

        // 2. 날씨를 보고 추천해달라고 했으면 날씨를 가져온다
        WeatherResponse weather = null;
        if (Boolean.TRUE.equals(request.getUseWeather())) {
            weather = getWeather(loginUser, request);
        }

        // 3. 기본값 채우기
        int limit = 5;
        if (request.getLimit() != null) {
            limit = request.getLimit();
        }
        List<String> exclude = new ArrayList<>();
        if (request.getExclude() != null) {
            exclude = request.getExclude();
        }

        // 4. 점수를 매겨서 고른다
        List<FoodItem> items = foodScorer.recommend(limit, request.getCategory(),
                request.getBudgetMin(), request.getBudgetMax(), request.getQuery(),
                weather, allergies, dislikes, exclude);

        // 5. 하나도 없으면 예산을 3천원 넓혀서 한 번 더 찾는다
        if (items.isEmpty() && request.getBudgetMax() != null) {
            items = foodScorer.recommend(limit, request.getCategory(),
                    request.getBudgetMin(), request.getBudgetMax() + 3000, request.getQuery(),
                    weather, allergies, dislikes, exclude);
        }

        // 6. "요즘 싼 재료" 를 달라고 했으면 같이 준다 (첫 화면 카드용)
        List<BargainItem> bargains = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getUseBargains())) {
            bargains = bargainService.current();
        }

        RecommendResponse response = new RecommendResponse();
        response.setCondition(makeCondition(request, weather, bargains, allergies.size() + dislikes.size()));
        response.setItems(items);
        response.setWeather(weather);
        response.setBargains(bargains);
        return response;
    }

    // 어느 위치의 날씨를 볼지 정한다.
    //   1순위: 브라우저가 보낸 내 위치  2순위: 회원가입 때 고른 주소  3순위: 서울(기본값)
    private WeatherResponse getWeather(UserVO loginUser, RecommendRequest request) {
        if (request.getLat() != null && request.getLon() != null) {
            return weatherService.current(request.getLat(), request.getLon());
        }
        if (loginUser != null && loginUser.getLatitude() != null && loginUser.getLongitude() != null) {
            return weatherService.current(loginUser.getLatitude().doubleValue(), loginUser.getLongitude().doubleValue());
        }
        return weatherService.current(null, null); // null 이면 기본 위치(서울)
    }

    // 무슨 조건으로 골랐는지 한 줄로 적는다. 화면이 목록 위에 보여준다.
    //   예) "서울 · 맑음 28° · 한식 · 1만원 이하 · 못 먹는 재료 2건 제외"
    private String makeCondition(RecommendRequest request, WeatherResponse weather,
            List<BargainItem> bargains, int banCount) {
        List<String> parts = new ArrayList<>();

        if (weather != null) {
            parts.add(weather.getCity() + " · " + weather.getConditionText() + " " + weather.getTemp() + "°");
        }

        if (!bargains.isEmpty()) {
            // 예) "시세 대파 12%↓ 외 3건"
            BargainItem first = bargains.get(0);
            String text = "시세 " + first.getName() + " " + Numbers.round(Math.abs(first.getPercent())) + "%↓";
            if (bargains.size() > 1) {
                text = text + " 외 " + (bargains.size() - 1) + "건";
            }
            parts.add(text);
        }

        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            parts.add(request.getCategory());
        }

        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            parts.add("\"" + request.getQuery() + "\"");
        }

        String budget = makeBudgetText(request.getBudgetMin(), request.getBudgetMax());
        if (!budget.isEmpty()) {
            parts.add(budget);
        }

        if (banCount > 0) {
            parts.add("못 먹는 재료 " + banCount + "건 제외");
        }

        if (parts.isEmpty()) {
            return "조건 없음";
        }
        return String.join(" · ", parts);
    }

    // 예산 문구. 예) "5,000원 ~ 10,000원", "10,000원 이하"
    private String makeBudgetText(Integer min, Integer max) {
        NumberFormat won = NumberFormat.getIntegerInstance(Locale.KOREA); // 1000 → "1,000"

        if (min == null && max == null) {
            return "";
        }
        if (min == null) {
            return won.format(max) + "원 이하";
        }
        if (max == null) {
            return won.format(min) + "원 이상";
        }
        return won.format(min) + "원 ~ " + won.format(max) + "원";
    }
}
