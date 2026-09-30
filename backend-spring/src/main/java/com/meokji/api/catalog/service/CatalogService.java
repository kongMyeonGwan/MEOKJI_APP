package com.meokji.api.catalog.service;

import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;

/** 화면이 쓰는 고정 목록. 실제 코드는 CatalogServiceImpl 에 있다. */
public interface CatalogService {

    /** 대시보드가 처음 뜰 때 필요한 목록을 한 번에 */
    Map<String, JsonNode> getSections();

    /** 주소 드롭다운이 쓰는 시·도 · 시·군·구 목록 */
    List<Map<String, Object>> getRegions();
}
