package com.meokji.api.catalog.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meokji.api.common.ApiException;
import com.meokji.api.util.FoodCatalog;
import com.meokji.api.util.RegionCatalog;

import tools.jackson.databind.JsonNode;

/**
 * 고정 목록은 표가 아니라 파일(resources/data)에서 온다.
 * 그래서 이 기능에는 DAO 와 VO 가 없다.
 *
 * 여기만 응답이 VO 가 아니라 Map 이다. Jackson 의 이름 바꾸기는 VO 의 필드에만
 * 걸리고 Map 의 키에는 안 걸리므로, 아래 이름이 그대로 JSON 키가 된다.
 * 그래서 catalog.json 의 키 자체를 카멜로 적어 두었다.
 */
@Service
public class CatalogServiceImpl implements CatalogService {

    // 화면에 내려보낼 칸들. 음식 100종(foods)은 넣지 않는다 -
    // 화면이 목록 전체를 쓰지 않고, 48KB 를 매번 내려보낼 이유가 없다.
    private static final List<String> SECTIONS = List.of(
            "categories", "roulette", "games",
            "balanceQuestions", "searchSuggestions", "budgetPresets");

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

    @Override
    public List<String> getDongs(String sido, String sigungu) {
        return regions.listDong(sido, sigungu);
    }

    @Override
    public Map<String, String> getNearestRegion(double lat, double lon) {
        Map<String, String> found = regions.findNearest(lat, lon);

        if (found == null) {
            throw ApiException.notFound("가까운 지역을 찾지 못했습니다.");
        }

        return found;
    }
}
