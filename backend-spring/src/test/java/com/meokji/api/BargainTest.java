package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.meokji.api.dto.recommend.BargainItem;
import com.meokji.api.service.BargainService;
import com.meokji.api.util.IngredientMatcher;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 떨이 재료 (use_bargains).
 *
 * <h2>값이 실제로 나오는지는 확인하지 않는다</h2>
 *
 * <p>내린 재료가 <b>하루에 하나도 없을 수 있다.</b> 설이나 장마처럼 값이 한 방향으로
 * 움직이는 주간이면 5% 넘게 내린 품목이 0건인 날이 실제로 있다. 그걸 단언하면
 * 테스트가 바깥 시세에 따라 빨강과 초록을 오간다.
 *
 * <p>그래서 여기서는 <b>모양</b>만 본다 - 켜면 칸이 있는지, 끄면 비어 있는지,
 * 훑을 대상이 파이썬과 같은 101개인지. 값이 맞는지는 KAMIS 응답을 다루는
 * {@code PriceTest} 와 {@code RecipeFixtureTest} 가 이미 대조한다.
 */
@SpringBootTest
@DisplayName("떨이 재료")
class BargainTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private IngredientMatcher matcher;

    @Autowired
    private BargainService bargains;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private JsonNode recommend(String json) throws Exception {
        MvcResult result = mvc.perform(post("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        return MAPPER.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("훑을 품목은 116개가 아니라 101개다 (코드가 겹치는 이름을 줄인다)")
    void uniqueItemsMatchPython() {
        assertThat(matcher.itemCount())
                .as("kamis-items.json 의 품목 수").isEqualTo(116);

        // 파이썬 bargain_service.unique_items() 와 같은 숫자여야 한다. 같은 코드를
        // 가리키는 이름이 여럿이라("대파"·"쪽파") 이름마다 물어보면 같은 응답을
        // 두 번 받는다.
        assertThat(matcher.uniqueItemNames())
                .as("코드가 겹치는 이름을 줄인 뒤의 품목 수").hasSize(101);
    }

    @Test
    @DisplayName("current() 는 기다리지 않고 바로 돌려준다")
    void currentDoesNotBlock() {
        // 파이썬과 달라진 곳이다. 훑기는 1분 가까이 걸리므로 부르는 쪽을 묶어두지
        // 않는다 - 가진 것을 주고 훑기는 뒤에서 돈다. 그래서 첫 호출이 빈 목록을
        // 돌려주는 것은 정상이다.
        long started = System.currentTimeMillis();

        List<BargainItem> rows = bargains.current();

        assertThat(System.currentTimeMillis() - started)
                .as("바깥 API 를 기다리면 안 된다").isLessThan(3_000);

        assertThat(rows).as("null 이 아니라 빈 목록이어야 한다").isNotNull();

        // 내린 것만 담으므로 값이 있다면 전부 음수이고 5% 이상이어야 한다.
        for (BargainItem row : rows) {
            assertThat(row.getPercent()).isLessThanOrEqualTo(-5.0);
            assertThat(row.getName()).isNotBlank();
            assertThat(row.getPrice()).isPositive();
        }

        assertThat(rows.size()).as("화면에 넘기는 기본 개수").isLessThanOrEqualTo(8);
    }

    @Test
    @DisplayName("use_bargains 를 끄면 bargains 가 빈 배열로 나간다")
    void offGivesEmptyArray() throws Exception {
        JsonNode body = recommend("{\"limit\":4}");

        JsonNode rows = body.get("bargains");

        // 칸 자체는 늘 있어야 한다. 파이썬(Pydantic)이 default_factory=list 로
        // 늘 내보내고 있어서, 빼버리면 화면이 "없음" 과 "빈 목록" 을 구분 못 한다.
        assertThat(rows).as("bargains 칸은 늘 있어야 한다").isNotNull();
        assertThat(rows.isArray()).isTrue();
        assertThat(rows).isEmpty();
    }

    @Test
    @DisplayName("use_bargains 를 켜도 추천 응답 모양은 그대로다")
    void onKeepsResponseShape() throws Exception {
        JsonNode body = recommend("{\"limit\":4,\"use_bargains\":true}");

        assertThat(body.get("bargains").isArray()).isTrue();

        // 떨이 재료는 고른 메뉴에 영향을 주지 않는다. STEP 43 에서 AI 호출을
        // 걷어내며 그 길이 끊겼고, 파이썬도 지금 같은 상태다.
        assertThat(body.get("items")).isNotEmpty();
        assertThat(body.get("condition").asString()).isNotBlank();

        for (JsonNode row : body.get("bargains")) {
            // 화면(BargainPick.jsx)이 읽는 칸이 다 있는지 본다.
            assertThat(row.has("name")).isTrue();
            assertThat(row.has("percent")).isTrue();
            assertThat(row.has("price")).isTrue();
            assertThat(row.has("unit")).as("단위 없이 가격만 주면 비교가 안 된다").isTrue();
            assertThat(row.has("surveyed_at")).isTrue();
        }
    }
}
