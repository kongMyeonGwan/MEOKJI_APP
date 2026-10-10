package com.meokji.api.catalog.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meokji.api.util.FoodCatalog;
import com.meokji.api.util.RegionCatalog;

import tools.jackson.databind.JsonNode;

/**
 * 고정 목록은 표가 아니라 파일(resources/data)에서 온다.
 * 그래서 이 기능에는 DAO 와 VO 가 없다.
 *
 * <p>이 클래스가 하는 일은 "어느 칸을 내려보낼지" 를 고르는 것뿐이다. 파일을
 * 읽어 메모리에 들고 있는 일은 FoodCatalog · RegionCatalog 가 서버 시작 때
 * 이미 해 두었다. 여기서 매번 파일을 다시 읽지는 않는다.
 */
@Service
public class CatalogServiceImpl implements CatalogService {

    // 화면에 내려보낼 칸들. 음식 100종(foods)은 넣지 않는다 -
    // 화면이 목록 전체를 쓰지 않고, 48KB 를 매번 내려보낼 이유가 없다.
    //
    // 각 칸이 무엇이고 어느 화면이 쓰는지
    //   categories        : 홈 카테고리 칩        {key, label, art}        11개
    //   roulette          : 돌림판 조각           {label, color}            8개
    //   games             : 게임 카드 목록        {key, to, emoji, title, desc}  3개
    //   balance_questions : 취향 밸런스 문항      {id, question, options[]} 5개
    //   search_suggestions: 검색창 예시 검색어    문장 배열                 8개
    //   budget_presets    : 예산 버튼             {key, label, max}         4개
    //
    // catalog.json 에는 있지만 여기 없는 칸이 둘 더 있다
    //   foods             : 음식 100종. 추천이 서버 안에서만 쓴다 (48KB)
    //   hot_weather_picks : 더운 날 가점용 이름. 점수 계산에만 쓰는 내부 값
    //
    // 목록(List)으로 적고 for 로 도는 이유 — 칸을 하나 더 내려보내려면 이름만
    // 한 줄 더 적으면 된다. 메서드에 body.put 을 여섯 줄 늘어놓으면 칸을
    // 추가할 때 코드를 고쳐야 하고, 이름을 잘못 적어도 컴파일은 된다.
    private static final List<String> SECTIONS = List.of(
            "categories", "roulette", "games",
            "balance_questions", "search_suggestions", "budget_presets");

    @Autowired
    private FoodCatalog catalog;

    @Autowired
    private RegionCatalog regions;

    /**
     * SECTIONS 에 적힌 칸을 catalog.json 에서 꺼내 한 덩어리로 묶는다.
     *
     * LinkedHashMap 을 쓰는 이유 — 보통 HashMap 은 순서를 안 지켜서 응답의
     * 칸 순서가 뜰 때마다 바뀐다. 동작에 지장은 없지만, 브라우저 개발자도구로
     * 응답을 볼 때마다 순서가 달라서 눈으로 비교하기 어렵다.
     */
    @Override
    public Map<String, JsonNode> getSections() {
        Map<String, JsonNode> body = new LinkedHashMap<>();

        for (String section : SECTIONS) {
            // section(name) 은 안에서 root.path(name) 을 쓴다. 없는 칸 이름을
            // 적어도 null 이 아니라 "빈 노드" 가 와서 터지지 않는다. 대신
            // 이름을 잘못 적으면 조용히 빈 값이 나가므로, 이름이 맞는지는
            // DataFileTest 가 확인한다.
            body.put(section, catalog.section(section));
        }

        return body;
    }

    /** 주소 드롭다운 목록. 좌표는 빼고 이름만 들어 있다 (RegionCatalog.list 참고). */
    @Override
    public List<Map<String, Object>> getRegions() {
        return regions.list();
    }
}
