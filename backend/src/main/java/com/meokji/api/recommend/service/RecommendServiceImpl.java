package com.meokji.api.recommend.service;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meokji.api.ban.dao.BanDAO;
import com.meokji.api.recommend.vo.BargainVO;
import com.meokji.api.recommend.vo.FoodVO;
import com.meokji.api.recommend.vo.RecommendRequestVO;
import com.meokji.api.recommend.vo.RecommendVO;
import com.meokji.api.recommend.vo.WeatherVO;
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
    public RecommendVO recommend(UserVO loginUser, RecommendRequestVO request) {
        // 1. 로그인했으면 못 먹는 재료를 가져온다
        List<String> allergies = new ArrayList<>();
        List<String> dislikes = new ArrayList<>();
        if (loginUser != null) {
            allergies = banDAO.findActiveNames(loginUser.getId(), "ALLERGY");
            dislikes = banDAO.findActiveNames(loginUser.getId(), "DISLIKE");
        }

        // 2. 날씨를 보고 추천해달라고 했으면 날씨를 가져온다
        WeatherVO weather = null;
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

        // 4. "요즘 싼 재료" 를 달라고 했으면 메뉴보다 먼저 가져온다.
        //    화면 위쪽에 칩으로 보여주기도 하고, 아래 메뉴를 고르는 기준으로도 쓴다
        List<BargainVO> bargains = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getUseBargains())) {
            bargains = bargainService.current();
        }

        // 칩으로 보여주는 그 재료 이름만 넘긴다. 화면에 안 보이는 재료로 고르면
        //  "왜 이 메뉴가 나왔지" 가 되기 때문이다
        List<String> bargainNames = new ArrayList<>();
        for (BargainVO row : bargains) {
            bargainNames.add(row.getName());
        }

        // 5. 점수를 매겨서 고른다.
        //    "지금 싼 재료로" 카드는 날씨도 검색어도 안 보내서 점수가 모두 0 이 된다.
        //    그래서 정렬의 두 번째 기준인 '싼 순' 이 실제 순서를 정한다
        List<FoodVO> ranked = foodScorer.rank(request.getCategory(),
                request.getBudgetMin(), request.getBudgetMax(), request.getQuery(),
                weather, allergies, dislikes, exclude, bargainNames);

        // 6. 하나도 없으면 예산을 3천원 넓혀서 한 번 더 찾는다
        if (ranked.isEmpty() && request.getBudgetMax() != null) {
            ranked = foodScorer.rank(request.getCategory(),
                    request.getBudgetMin(), request.getBudgetMax() + 3000, request.getQuery(),
                    weather, allergies, dislikes, exclude, bargainNames);
        }

        // 7. 순위에서 이번 페이지만 자른다. page 를 안 보내면 1페이지(앞에서 limit 개)
        int page = 1;
        if (request.getPage() != null) {
            page = request.getPage();
        }
        int from = Math.min((page - 1) * limit, ranked.size());
        int to = Math.min(from + limit, ranked.size());

        RecommendVO response = new RecommendVO();
        response.setCondition(makeCondition(request, weather, bargains, allergies.size() + dislikes.size()));
        response.setItems(new ArrayList<>(ranked.subList(from, to)));
        response.setPage(page);
        response.setTotal(ranked.size());
        response.setTotalPages((ranked.size() + limit - 1) / limit);
        response.setWeather(weather);
        response.setBargains(bargains);
        return response;
    }

    // 어느 위치의 날씨를 볼지 정한다.
    //   1순위: 회원이 고른 주소  2순위: 브라우저가 보낸 내 위치  3순위: 서울(기본값)
    //
    // 주소를 먼저 보는 이유는, 그게 사용자가 직접 고른 값이기 때문이다. 브라우저 위치는
    // 지금 PC 가 놓인 자리일 뿐이라 - 제주로 설정해둔 사람이 서울에서 켜면 서울 날씨가
    // 떴다. 주소를 안 골랐거나 좌표를 못 찾은 주소면(LATITUDE 가 비어 있다) 위치를 쓴다.
    private WeatherVO getWeather(UserVO loginUser, RecommendRequestVO request) {
        if (loginUser != null && loginUser.getLatitude() != null && loginUser.getLongitude() != null) {
            return weatherService.current(loginUser.getLatitude().doubleValue(), loginUser.getLongitude().doubleValue());
        }
        if (request.getLat() != null && request.getLon() != null) {
            return weatherService.current(request.getLat(), request.getLon());
        }
        return weatherService.current(null, null); // null 이면 기본 위치(서울)
    }

    // 무슨 조건으로 골랐는지 한 줄로 적는다. 화면이 목록 위에 보여준다.
    //   예) "서울 · 맑음 28° · 한식 · 1만원 이하 · 못 먹는 재료 2건 제외"
    private String makeCondition(RecommendRequestVO request, WeatherVO weather,
            List<BargainVO> bargains, int banCount) {
        List<String> parts = new ArrayList<>();

        if (weather != null) {
            parts.add(weather.getCity() + " · " + weather.getConditionText() + " " + weather.getTemp() + "°");
        }

        if (!bargains.isEmpty()) {
            // 예) "시세 대파 12%↓ 외 3건"
            BargainVO first = bargains.get(0);
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
