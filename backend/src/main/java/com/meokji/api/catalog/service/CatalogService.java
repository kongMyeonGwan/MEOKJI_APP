package com.meokji.api.catalog.service;

import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;

/**
 * 화면이 쓰는 고정 목록. 실제 코드는 CatalogServiceImpl 에 있다.
 *
 * <p>인터페이스를 따로 두는 이유 — 컨트롤러는 "무엇을 부를 수 있는지" 만 알면 되고,
 * 그게 JSON 파일에서 오는지 DB 에서 오는지는 몰라도 된다. 이 기능은 지금
 * JSON 파일을 읽지만, 나중에 표로 옮겨도 컨트롤러는 한 글자도 안 고친다.
 *
 * <p>프로젝트의 모든 기능이 이 모양(인터페이스 + Impl)으로 맞춰져 있어서,
 * 메서드가 두 개뿐인 이 기능도 같은 모양을 지킨다. 기능마다 구조가 다르면
 * 코드를 찾을 때 "이 기능은 어떻게 돼 있었지" 를 매번 확인해야 한다.
 */
public interface CatalogService {

    /**
     * 대시보드가 처음 뜰 때 필요한 목록을 한 번에
     *
     * @return 칸 이름 -> 그 칸의 JSON. 예) "categories" -> [{key, label, art}, ...]
     */
    Map<String, JsonNode> getSections();

    /**
     * 주소 드롭다운이 쓰는 시·도 · 시·군·구 목록
     *
     * @return [{ "name": 시·도 이름, "sigungu": [시·군·구 이름...] }, ...]
     */
    List<Map<String, Object>> getRegions();
}
