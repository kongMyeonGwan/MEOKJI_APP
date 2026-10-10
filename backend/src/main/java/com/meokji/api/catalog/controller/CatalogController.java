package com.meokji.api.catalog.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.catalog.service.CatalogService;

import tools.jackson.databind.JsonNode;

/**
 * 화면이 쓰는 고정 목록.
 *
 * <p>읽기만 하는 기능이라 GET 두 개가 전부다. 로그인 없이도 부를 수 있다 -
 * 로그인 화면에도 게임 카드가 보이고, 회원가입 화면에서 주소를 골라야 하므로
 * 토큰이 없는 상태에서 먼저 필요하다.
 *
 * <p>왜 이 기능만 DAO 와 VO 가 없나
 *
 * <p>카테고리 · 룰렛 조각 · 게임 카드 같은 목록은 사용자도 운영자도 고치지 않는다.
 * 표로 만들면 INSERT 문을 적어 넣고, 바꿀 때마다 운영 DB 에 UPDATE 를 쳐야 한다.
 * 그래서 resources/data/catalog.json · regions.json 에 두고 서버가 뜰 때 한 번
 * 읽어 메모리에 들고 있는다. 고칠 일이 있으면 JSON 을 고쳐 다시 배포한다.
 *
 * <p>응답이 VO 가 아니라 Map / JsonNode 인 것도 같은 이유다. 칸 생김새가 목록마다
 * 다른데(룰렛은 label + color, 예산은 key + label + max) VO 를 여섯 개 만들면
 * JSON 을 고칠 때마다 VO 도 같이 고쳐야 한다. 서버는 내용을 들여다보지 않고
 * 그대로 내려보내기만 하므로 읽은 모양을 유지한다.
 */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    @Autowired
    private CatalogService catalogService;

    /**
     * 대시보드가 처음 뜰 때 필요한 목록을 한 번에.
     *
     * GET /api/catalog
     *
     * 응답 모양 — 칸 이름이 그대로 열쇠가 된다
     *   { "categories": [...], "roulette": [...], "games": [...],
     *     "balance_questions": [...], "search_suggestions": [...],
     *     "budget_presets": [...] }
     *
     * 목록마다 주소를 따로 두지 않고 한 번에 묶어 보내는 이유 — 홈 화면 하나가
     * categories · games · search_suggestions · budget_presets 를 같이 쓴다.
     * 주소를 나누면 화면이 뜰 때 요청이 네 번 나가고, 그만큼 늦게 그려진다.
     *
     * 어떤 칸이 들어가는지는 CatalogServiceImpl 의 SECTIONS 가 정한다.
     */
    @GetMapping
    public Map<String, JsonNode> read() {
        return catalogService.getSections();
    }

    /**
     * 회원가입 · 정보수정의 주소 드롭다운이 쓰는 목록.
     *
     * GET /api/catalog/regions
     *
     * 응답 모양: [{ "name": "서울특별시", "sigungu": ["강남구", "강동구", ...] }, ...]
     *
     * 위 read 에 같이 넣지 않은 이유는 양이다. 시·군·구까지 다 들어가서
     * 25KB 쯤 되는데, 주소를 고르는 화면은 가입과 정보수정 둘뿐이다.
     * 홈 화면이 뜰 때마다 같이 내려보낼 이유가 없다.
     *
     * 좌표(lat · lon)는 뺀다. 화면은 이름만 보여주면 되고, 좌표는 가입할 때
     * 서버가 RegionCatalog.findCoords 로 직접 찾아 넣는다.
     */
    @GetMapping("/regions")
    public List<Map<String, Object>> regions() {
        return catalogService.getRegions();
    }
}
