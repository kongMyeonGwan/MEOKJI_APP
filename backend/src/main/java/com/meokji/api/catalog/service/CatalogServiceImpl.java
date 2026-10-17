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
 */
@Service
public class CatalogServiceImpl implements CatalogService {

    // 화면에 내려보낼 칸들. 음식 100종(foods)은 넣지 않는다 -
    // 화면이 목록 전체를 쓰지 않고, 48KB 를 매번 내려보낼 이유가 없다.
    private static final List<String> SECTIONS = List.of(
            "categories", "roulette", "games",
            "balance_questions", "search_suggestions", "budget_presets");

    @Autowired
    private FoodCatalog catalog;

    @Autowired
    private RegionCatalog regions;

    @Override
    public Map<String, JsonNode> getSections() {
        Map<String, JsonNode> body = new LinkedHashMap<>();

        for (String section : SECTIONS) {
            body.put(section, catalog.section(section));
        }

        return body;
    }

    @Override
    public List<Map<String, Object>> getRegions() {
        return regions.list();
    }
}
