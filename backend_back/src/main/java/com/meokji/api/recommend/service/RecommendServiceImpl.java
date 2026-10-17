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
 *
 * <p>이 클래스가 하는 일은 "재료 모으기" 와 "결과 포장" 이다. 점수 계산 자체는
 * FoodScorer 가 한다 — 그 계산이 길어서 util 로 뺐다.
 *
 * <p>모아야 하는 재료가 셋이다
 *   못 먹는 재료 : DB (사람마다 다르다)
 *   날씨         : 바깥 API (요청에서 켰을 때만)
 *   싼 재료      : 바깥 API (요청에서 켰을 때만)
 *
 * <p>&#64;Transactional 이 없다. 읽기만 하고, 바깥 API 를 부르는 일이 들어
 * 있어서 트랜잭션을 열어두면 응답을 기다리는 동안 DB 연결을 붙잡는다.
 *
 * <p>추천 결과를 저장하지 않는다. 사용자가 "이걸로 먹을래요" 를 눌렀을 때만
 * history 기능이 그 한 건을 저장한다 - 보여준 것을 다 남기면 쓸 일도 없는
 * 행이 끝없이 쌓인다.
 */
@Service
public class RecommendServiceImpl implements RecommendService {

    // 점수 계산. 조건에 맞는 후보를 골라 높은 순으로 준다
    @Autowired
    private FoodScorer foodScorer;

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private BargainService bargainService;

    // 못 먹는 재료. 이 기능이 DB 를 보는 유일한 곳이다.
    // BanService 가 아니라 DAO 를 바로 쓰는 이유는 필요한 것이 이름 목록뿐이라서다
    @Autowired
    private BanDAO banDAO;

    /**
     * 조건에 맞는 음식을 골라 돌려준다.
     *
     * 하는 일이 순서대로 여섯이다. 번호를 매겨 두었다.
     */
    @Override
    public RecommendVO recommend(UserVO loginUser, RecommendRequestVO request) {
        // 1. 로그인했으면 못 먹는 재료를 가져온다
        //
        // 빈 목록으로 시작한다. 비로그인이면 그대로 넘어가서 FoodScorer 가
        // "뺄 재료가 없다" 로 받는다. null 을 넘기면 저쪽에서 또 확인해야 한다
        List<String> allergies = new ArrayList<>();
        List<String> dislikes = new ArrayList<>();
        if (loginUser != null) {
            // 둘을 따로 읽는다. 알레르기는 대체재료로도 넣으면 안 되고,
            // 싫어함은 점수를 깎는 정도로 다룬다 - 취급이 달라서다
            allergies = banDAO.findActiveNames(loginUser.getId(), "ALLERGY");
            dislikes = banDAO.findActiveNames(loginUser.getId(), "DISLIKE");
        }

        // 2. 날씨를 보고 추천해달라고 했으면 날씨를 가져온다
        //
        // Boolean.TRUE.equals(...) 로 쓰는 이유 — 칸이 Boolean 이라 null 일
        // 수 있다. request.getUseWeather() 를 그냥 if 에 넣으면 null 일 때
        // NullPointerException 이 난다
        WeatherVO weather = null;
        if (Boolean.TRUE.equals(request.getUseWeather())) {
            weather = getWeather(loginUser, request);
        }

        // 3. 기본값 채우기
        //
        // 컨트롤러의 @Valid 는 "보낸 값이 올바른지" 만 본다. 안 보낸 값에
        // 기본값을 넣는 일은 서비스가 한다 - 그 값이 업무 규칙이라서다
        int limit = 5;
        if (request.getLimit() != null) {
            limit = request.getLimit();
        }
        List<String> exclude = new ArrayList<>();
        if (request.getExclude() != null) {
            exclude = request.getExclude();
        }

        // 4. 점수를 매겨서 고른다
        //
        // 인자가 아홉 개라 길다. VO 하나로 묶어 넘길 수도 있지만, 그러면
        // FoodScorer 가 recommend 기능의 요청 VO 를 알아야 한다 - 찜 목록도
        // FoodScorer 를 쓰므로 그쪽에 묶이지 않게 값으로 넘긴다
        List<FoodVO> items = foodScorer.recommend(limit, request.getCategory(),
                request.getBudgetMin(), request.getBudgetMax(), request.getQuery(),
                weather, allergies, dislikes, exclude);

        // 5. 하나도 없으면 예산을 3천원 넓혀서 한 번 더 찾는다
        //
        // 예산을 좁게 잡으면 후보가 통째로 사라진다. 그때 빈 목록을 주면
        // 사용자는 "추천이 안 되는 앱" 으로 받아들인다. 조금 넘는 메뉴라도
        // 보여주는 쪽이 낫다고 봤다.
        //
        // 예산을 안 보냈으면 다시 찾지 않는다 - 예산 때문에 빈 것이 아니라
        // 분류나 검색어 때문이라서, 예산을 넓혀도 결과가 같다
        if (items.isEmpty() && request.getBudgetMax() != null) {
            items = foodScorer.recommend(limit, request.getCategory(),
                    request.getBudgetMin(), request.getBudgetMax() + 3000, request.getQuery(),
                    weather, allergies, dislikes, exclude);
        }

        // 6. "요즘 싼 재료" 를 달라고 했으면 같이 준다 (첫 화면 카드용)
        //
        // 기본으로 안 주는 이유 — 바깥 API 를 여러 번 불러야 해서 느리다.
        // 추천만 받을 때 그 시간을 같이 기다릴 이유가 없다
        List<BargainVO> bargains = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getUseBargains())) {
            bargains = bargainService.current();
        }

        RecommendVO response = new RecommendVO();
        // 무슨 조건으로 골랐는지 한 줄로 적어 같이 보낸다. 안 보내면
        // 사용자는 왜 이 메뉴가 나왔는지 알 수 없다
        response.setCondition(makeCondition(request, weather, bargains, allergies.size() + dislikes.size()));
        response.setItems(items);
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
    //
    // 세 단계로 떨어지게 둔 이유 — 어느 단계에서도 "날씨를 못 가져왔다" 로
    // 끝나지 않게 하려는 것이다. 날씨가 없으면 추천 점수도 못 매긴다.
    private WeatherVO getWeather(UserVO loginUser, RecommendRequestVO request) {
        // 좌표가 BigDecimal 이라 doubleValue() 로 바꿔 넘긴다.
        // DB 에는 자릿수를 지키려고 BigDecimal 로 담지만, 날씨 API 는
        // 소수를 받으므로 여기서는 double 로 충분하다
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
    //
    // 화면이 아니라 서버가 만드는 이유 — 조건을 실제로 적용한 쪽이 서버다.
    // 예산을 3천원 넓혀 다시 찾은 경우처럼 화면이 모르는 일도 있고,
    // 화면마다 문구를 짜면 같은 조건에 다른 말이 나온다.
    //
    // 목록에 담고 마지막에 " · " 로 잇는 모양인 이유 — 조건이 있을 때만
    // 넣으므로, 글자를 이어붙이면 "· · 한식" 처럼 구분자가 남는다.
    private String makeCondition(RecommendRequestVO request, WeatherVO weather,
            List<BargainVO> bargains, int banCount) {
        List<String> parts = new ArrayList<>();

        if (weather != null) {
            parts.add(weather.getCity() + " · " + weather.getConditionText() + " " + weather.getTemp() + "°");
        }

        if (!bargains.isEmpty()) {
            // 예) "시세 대파 12%↓ 외 3건"
            //
            // 가장 많이 내린 것 하나만 이름으로 적는다. 여덟 개를 다 적으면
            // 이 줄이 화면을 넘어간다. 목록이 많이 내린 순으로 와서 첫 번째가 그것이다
            BargainVO first = bargains.get(0);
            // percent 는 음수다(내린 것만 담는다). 절댓값에 ↓ 를 붙인다
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
            // 따옴표를 붙여 검색어임을 드러낸다. 안 붙이면 분류 이름과
            // 섞여 "한식 · 파스타" 처럼 둘 다 분류로 읽힌다
            parts.add("\"" + request.getQuery() + "\"");
        }

        String budget = makeBudgetText(request.getBudgetMin(), request.getBudgetMax());
        if (!budget.isEmpty()) {
            parts.add(budget);
        }

        // 몇 건을 뺐는지만 알려준다. 재료 이름을 적지 않는 이유 —
        // 알레르기는 민감한 정보라 화면에 띄워 둘 값이 아니고,
        // 사용자는 자기가 등록한 것을 이미 안다
        if (banCount > 0) {
            parts.add("못 먹는 재료 " + banCount + "건 제외");
        }

        // 조건을 하나도 안 보낸 경우(대시보드 첫 화면). 빈 글자를 주면
        // 화면에 빈 줄이 생기므로 말로 적는다
        if (parts.isEmpty()) {
            return "조건 없음";
        }
        return String.join(" · ", parts);
    }

    // 예산 문구. 예) "5,000원 ~ 10,000원", "10,000원 이하"
    //
    // 네 갈래다 — 둘 다 없음 · 최대만 · 최소만 · 둘 다.
    // 화면에서 만들지 않는 이유는 위 makeCondition 과 같다
    private String makeBudgetText(Integer min, Integer max) {
        NumberFormat won = NumberFormat.getIntegerInstance(Locale.KOREA); // 1000 → "1,000"
        // 쉼표를 직접 끼워 넣지 않는 이유 — 자리 수를 세어 넣는 코드를
        // 짜면 틀릴 자리가 생긴다. 이미 있는 것을 쓴다

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
