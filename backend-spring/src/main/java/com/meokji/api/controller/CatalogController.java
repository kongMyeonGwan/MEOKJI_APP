package com.meokji.api.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.util.FoodCatalog;
import com.meokji.api.util.RegionCatalog;

import tools.jackson.databind.JsonNode;

/** 화면이 쓰는 고정 목록. */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private static final List<String> SECTIONS = List.of(
            "categories", "roulette", "games",
            "balance_questions", "search_suggestions", "budget_presets");

    @Autowired
    private FoodCatalog catalog;
    @Autowired
    private RegionCatalog regions;

    /**
     * 대시보드가 처음 뜰 때 필요한 목록을 한 번에.
     *
     * 음식 100종(foods)은 넣지 않는다. 화면이 목록 전체를 쓰지 않고,
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
