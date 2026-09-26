package com.meokji.api.catalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.region.RegionCatalog;

import tools.jackson.databind.JsonNode;

/**
 * 화면이 쓰는 고정 목록.
 *
 * <p>표를 만들지 않고 파일에서 내려준다. 카테고리 · 룰렛 항목 · 게임 종류처럼
 * <b>사용자도 운영자도 만들지 않는 목록</b>이라, 표로 두면 배포마다 시드를 넣어야
 * 한다. 파이썬 쪽 {@code app/routers/catalog.py} 자리다.
 */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    /** catalog.json 에서 그대로 내려보낼 칸. 순서까지 파이썬 응답과 같게 둔다. */
    private static final List<String> SECTIONS = List.of(
            "categories", "roulette", "games",
            "balance_questions", "search_suggestions", "budget_presets");

    private final FoodCatalog catalog;
    private final RegionCatalog regions;

    public CatalogController(FoodCatalog catalog, RegionCatalog regions) {
        this.catalog = catalog;
        this.regions = regions;
    }

    /**
     * 대시보드가 처음 뜰 때 필요한 목록을 한 번에.
     *
     * <p>음식 100종({@code foods})은 넣지 않는다. 화면이 목록 전체를 쓰지 않고,
     * 48KB 를 매번 내려보낼 이유가 없다 - 필요한 음식은 추천이 골라서 준다.
     */
    @GetMapping
    public Map<String, JsonNode> read() {
        Map<String, JsonNode> body = new LinkedHashMap<>();

        for (String section : SECTIONS) {
            body.put(section, catalog.section(section));
        }

        return body;
    }

    /** 회원가입 · 정보수정의 주소 드롭다운이 쓰는 목록. */
    @GetMapping("/regions")
    public List<Map<String, Object>> regions() {
        return regions.list();
    }
}
