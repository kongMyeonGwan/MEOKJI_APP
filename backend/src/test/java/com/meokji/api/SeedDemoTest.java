package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.meokji.api.util.IngredientMatcher;
import com.meokji.api.util.KamisClient;
import com.meokji.api.notice.vo.NoticeVO;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 데모 데이터 채우기 — 회원 · 공지 · 식사 기록 · 후기(첨부 포함).
 *
 * <p>{@code gradlew seedDemo} 가 이 클래스를 돌린다. 빈 DB 로는 화면을 볼 수가
 * 없다. 게시판도 공지도 비어 있고, 글을 쓰려면 식사를 먼저 확정해야 해서 손으로
 * 채우려면 한참 걸린다. 이 클래스가 그 한 바퀴를 대신 돌아준다.
 *
 * <pre>
 *   gradlew seedDemo
 * </pre>
 *
 * <h2>만드는 것</h2>
 *
 * <pre>
 *   1) 회원 2명    운영자(관리자) · 일반 회원
 *   2) 공지 4건    관리자가 쓴다. 공지에는 첨부파일이 없다
 *   3) 식사 기록 3건
 *   4) 후기 1건    첨부 없음
 *   5) 후기 1건    첨부 있음 (그림은 아래에서 직접 그린다)
 * </pre>
 *
 * <p><b>지금은 2 · 4 · 5 를 건너뛴다.</b> 게시판과 공지는 담당자가 채울 자리라
 * 서버가 501 을 돌려준다. 그 단계만 건너뛰고 나머지는 그대로 만든다 - 여기서
 * 멈추면 게시판과 상관없는 식사 기록까지 못 만들어진다. 구현이 끝나면 501 이
 * 사라져 조건이 저절로 풀리므로 이 파일은 고칠 필요가 없다.
 *
 * <h2>왜 DB 에 직접 INSERT 하지 않나</h2>
 *
 * <p>서버를 거쳐야 검증기 · FK · UNIQUE 가 실제로 돌아간다. INSERT 로 넣으면
 * 화면에서는 멀쩡해 보이는데 API 로는 만들 수 없는 데이터가 섞인다. 덤으로
 * "이 흐름이 지금도 도는가" 가 한 번 확인된다.
 *
 * <p>그래서 {@code MockMvc} 를 쓴다 -
 * 서버를 따로 띄우지 않으면서도 컨트롤러 · 검증기 · 매퍼를 다 지난다.
 *
 * <h2>왜 테스트 폴더에 있나</h2>
 *
 * <p>{@code @SpringBootTest} 가 필요해서다. {@code @Tag("seed")} 가 붙어 있어
 * {@code gradlew test} 에서는 <b>빠진다</b> - 평소 테스트를 돌릴 때마다 데모
 * 데이터를 새로 만들 이유가 없다.
 *
 * <p>여러 번 돌려도 안전하다. 이미 있는 것은 건너뛴다.
 */
@SpringBootTest
@Tag("seed")
@DisplayName("데모 데이터 채우기")
class SeedDemoTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    /**
     * {@code db/02_seed.sql} · {@code docs/DB_SETUP.md} 와 같은 개발용 비밀번호다.
     *
     * <p>이 값이 저장소에 적혀 있어도 되는 이유는 {@link #localOnly()} 가 로컬 밖에서는
     * 아예 실행을 막기 때문이다. 그 가드가 없으면 이 줄은 그냥 공개된 비밀번호다.
     * 다른 값을 쓰고 싶으면 환경변수 {@code SEED_PASSWORD} 로 덮어쓴다.
     */
    private static final String PASSWORD =
            System.getenv("SEED_PASSWORD") == null || System.getenv("SEED_PASSWORD").isBlank()
                    ? "meokji1234!"
                    : System.getenv("SEED_PASSWORD");

    /**
     * {@code db/05_seed_notices.sql} 이 공지 작성자로 찾는 바로 그 이메일이다.
     *
     * <p>{@code .local} 같은 주소는 쓸 수 없다. 가입 API 의 이메일 검증이 예약
     * 도메인이라며 422 로 막아서 그 주소로는 계정이 아예 만들어지지 않는다.
     */
    private static final Map<String, String> ADMIN = Map.of(
            "email", "admin@meokji.com",
            "nickname", "뭐먹지운영팀",
            "address_sido", "서울특별시",
            "address_sigungu", "강남구");

    /**
     * 후기는 일반 회원이 쓰는 글이다. 한 계정으로 다 하면 "운영팀이 자기 후기를
     * 올린" 데이터가 되어 화면이 실제와 달라진다.
     */
    private static final Map<String, String> MEMBER = Map.of(
            "email", "minji@example.com",
            "nickname", "민지의부엌",
            "gender", "FEMALE",
            "address_sido", "서울특별시",
            "address_sigungu", "마포구");

    private record NoticeVO(String title, String content, boolean pinned) {
    }

    private static final List<NoticeVO> NOTICES = List.of(
            new NoticeVO("커뮤니티 이용 규칙 안내", """
                    서로 존중하는 말투로 소통해주세요.
                    레시피는 자세히 공유해주실수록 좋아요.
                    링크 및 홍보성 글은 사전 안내 없이 삭제될 수 있습니다.
                    신고는 관리자 확인 후 조치됩니다.""", true),

            new NoticeVO("9월 15일(월) 새벽 서버 점검 안내 (02:00~04:00)", """
                    추천 기능 개선을 위한 서버 점검이 예정되어 있습니다.
                    점검 시간 동안에는 로그인과 레시피 추천을 이용하실 수 없습니다.
                    작성 중인 글은 미리 저장해두시길 권해드립니다.""", true),

            new NoticeVO("못 먹는 재료 등록 기능이 추가되었습니다", """
                    마이페이지에서 알레르기 재료와 싫어하는 재료를 등록할 수 있습니다.
                    알레르기로 등록한 재료는 추천에서 완전히 제외되고,
                    싫어하는 재료는 가능한 한 대체재료로 바꿔 제안합니다.""", false),

            new NoticeVO("가격 정보는 참고용입니다", """
                    재료 가격은 KAMIS 소매 조사가격을 기준으로 보여드립니다.
                    실제 결제 금액은 구입처와 판매자 사정에 따라 달라질 수 있습니다.""", false));

    /**
     * 확정할 메뉴 셋.
     *
     * <p><b>{@code recipes.json} 에 있는 이름이어야 한다.</b> 없는 메뉴를 적으면
     * {@code GET /api/recipes} 가 404 로 끝난다. 예전 시드에는 여기에 "크림 파스타" 가
     * 적혀 있는데 STEP 41 에서 레시피 100종을 다시 정의할 때 빠진 메뉴라, 빈 DB 에
     * 돌리면 그 단계에서 죽는다 - 기존 DB 에는 옛날에 만들어진 행이 남아 있어
     * "이미 있음" 으로 건너뛰는 바람에 드러나지 않았다.
     */
    private static final List<String> FOODS = List.of("김치찌개", "된장찌개", "비빔국수");

    private record Post(String food, String title, String content, boolean attach) {
    }

    private static final List<Post> POSTS = List.of(
            new Post("김치찌개", "묵은지 있으면 무조건 이거 끓이세요", """
                    냉장고에 반년 묵은 김치가 굴러다녀서 끓여봤는데 결과가 좋았습니다.

                    돼지고기를 먼저 기름 나올 때까지 볶고 김치를 넣어 같이 볶다가 물을 부으면
                    국물 색이 확실히 진해집니다. 처음부터 다 넣고 끓이면 이 맛이 안 나요.

                    두부는 마지막 5분에 넣는 게 좋습니다. 오래 끓이면 부서집니다.
                    재료비는 8천원이 안 나왔는데 두 끼를 먹었습니다.""", false),

            new Post("비빔국수", "국수 안 붇게 만드는 법 (사진 있음)", """
                    비빔국수는 맛보다 타이밍이 어렵습니다. 몇 번 말아먹고 찾은 방법입니다.

                    삶은 면은 찬물에 헹구는 걸로 끝내지 말고 손으로 치대듯 빨아야 합니다.
                    겉의 전분이 남아 있으면 양념이 안 붙고 금방 퍼집니다.

                    양념은 면을 삶기 전에 미리 섞어두세요. 면이 익은 뒤에 양념을 만들면
                    그 1분 사이에 면이 불어버립니다.
                    얼음 두 개를 올린 게 아래 사진입니다.""", true));

    /** 자유게시판 글. 식사 기록이 없어도 쓸 수 있어서 후기보다 넣기가 간단하다 */
    private record FreePost(String title, String content, List<String> comments) {
    }

    private static final List<FreePost> FREE_POSTS = List.of(
            new FreePost("혼밥하기 괜찮은 식당 공유해요", """
                    회사 근처에 혼자 가도 눈치 안 보이는 곳이 생각보다 많더라고요.
                    바 자리가 있는 집은 거의 괜찮았습니다.

                    국밥집은 애초에 혼자 오는 사람이 많아서 제일 편하고,
                    요즘은 1인 화로를 두는 고깃집도 늘었어요. 다들 자주 가는 곳 있으면 알려주세요.
                    """,
                    List.of("저도 국밥집 자주 가요. 주인분이 아예 신경을 안 쓰셔서 편합니다",
                            "회전초밥도 괜찮습니다. 혼자 앉는 자리가 기본이라서요")),

            new FreePost("냉장고에 두부만 세 모 남았습니다", """
                    유통기한이 모레인데 뭘 해야 할까요.
                    부침은 어제 했고 찌개는 좀 질렸어요. 두부로 할 수 있는 거 아무거나 받습니다.
                    """,
                    List.of("으깨서 두부면 비빔국수 해보세요. 생각보다 괜찮습니다",
                            "두부조림 추천합니다. 간장 양념에 조리면 밑반찬으로 며칠 갑니다")),

            new FreePost("자취 2년 만에 산 물건 중 제일 잘 산 것", """
                    전기 그릴이요. 설거지가 프라이팬보다 훨씬 편하고 기름도 덜 튑니다.
                    고기 말고 야채 구울 때도 쓸 만해요.
                    """,
                    List.of("기름받이 닦는 게 일이긴 한데 그래도 프라이팬보단 낫죠")),

            new FreePost("다들 밥은 몇 인분씩 해두시나요", """
                    한 번에 많이 해서 냉동했다가 먹으려는데 얼마나 해둬야 할지 모르겠어요.
                    2인분씩 소분하면 금방 떨어지고, 많이 하면 맛이 떨어지는 느낌입니다.
                    """,
                    List.of()));

    private static final String ATTACH_NAME = "비빔국수.png";

    @Autowired
    private WebApplicationContext context;

    @org.springframework.beans.factory.annotation.Value("${app.env}")
    private String appEnv;

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 가격 조회를 막는다 (KAMIS 를 부르지 않게 바꿔 끼운다).
     *
     * <p>인증키가 비어 있으면 {@code GET /api/recipes} 가 재료마다 KAMIS 를 부르다
     * 503 으로 끝난다. 키가 있어도 재료 아홉 개를 바깥에 물어보느라 느려진다.
     * <b>시세는 DB 에 저장되지도 않는 값</b>이라 데모 데이터와 아무 상관이 없다.
     */
    @MockitoBean
    private KamisClient kamis;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();

        BDDMockito.given(kamis.fetch(BDDMockito.any(IngredientMatcher.Codes.class)))
                .willReturn(KamisClient.Quote.empty());
    }

    // ------------------------------------------------------------------ 도우미

    /**
     * 로컬이 아니면 아무것도 하지 않고 멈춘다.
     *
     * <p>이 시드는 회원을 만들고 그중 하나를 <b>관리자로 올린다</b>. 비밀번호는
     * 저장소에 적혀 있는 개발용 값이라, 운영이나 공용 DB 에서 돌면 누구나 README 만
     * 읽고 관리자로 로그인할 수 있게 된다.
     *
     * <p>DB 를 보고 판단하지 않고 {@code app.env} 로 판단한다. 로컬 DB 와 운영 DB 가
     * 겉보기에 다르지 않을 수 있고, "비어 있으면 로컬이겠지" 같은 추측은 한 번
     * 틀리면 돌이킬 수 없다.
     */
    private void localOnly() {
        if (!"local".equals(appEnv)) {
            throw new IllegalStateException(
                    "app.env 가 '" + appEnv + "' 입니다. 이 시드는 로컬에서만 돌릴 수 있습니다.\n"
                    + "  저장소에 적힌 개발용 비밀번호로 관리자 계정을 만들기 때문입니다.\n"
                    + "  정말 필요하면 APP_ENV 를 local 로 두고 다시 실행하세요.");
        }
    }

    private static void step(String number, String title) {
        System.out.printf("%n[%s] %s%n", number, title);
    }

    private static void ok(String message) {
        System.out.println("  OK  " + message);
    }

    private static void skip(String message) {
        System.out.println("  --  " + message);
    }

    /**
     * 아직 서버에 없는 기능인지 본다.
     *
     * <p>게시판(posts)과 공지(notices)는 담당자가 채울 자리라 컨트롤러가 501 을
     * 돌려준다. 그때는 실패로 끝내지 않고 그 단계만 건너뛴다.
     *
     * <p>상태 코드로 판단한다. "기능이 없다" 는 뜻을 가진 코드가 501 이라서,
     * 구현이 끝나면 조건이 저절로 풀린다. 나중에 손으로 지울 일이 없다.
     */
    private static boolean notReady(MvcResult result) {
        return result.getResponse().getStatus() == 501;
    }

    private static JsonNode body(MvcResult result) {
        try {
            String raw = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
            return raw.isBlank() ? MAPPER.createObjectNode() : MAPPER.readTree(raw);
        } catch (Exception exc) {
            throw new IllegalStateException("응답을 읽지 못했습니다.", exc);
        }
    }

    /**
     * GET 한 번.
     *
     * <p>경로에 값이 들어가면 {@code uriVars} 로 넘긴다 ("/api/recipes/{name}", "김치찌개").
     * <b>직접 퍼센트 인코딩해서 붙이면 안 된다</b> - MockMvc 는 그걸 다시 풀어주지
     * 않아서 {@code @PathVariable} 에 {@code %ED%81%AC...} 가 그대로 들어간다.
     */
    private MvcResult getJson(String path, String token, Object... uriVars) throws Exception {
        var request = get(path, uriVars);
        if (token != null) {
            request = request.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(request).andReturn();
    }

    private MvcResult postJson(String path, String token, Object payload) throws Exception {
        var request = post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(MAPPER.writeValueAsString(payload));

        if (token != null) {
            request = request.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(request).andReturn();
    }

    /**
     * 가입하고 로그인해서 토큰을 돌려준다.
     *
     * <p>이미 있는 계정이면 409 가 오는데, 그때는 로그인만 한다. 시드를 다시
     * 돌렸다고 실패로 끝낼 이유가 없다.
     */
    private String signUp(Map<String, String> profile) throws Exception {
        Map<String, String> payload = new LinkedHashMap<>(profile);
        payload.put("password", PASSWORD);

        MvcResult registered = postJson("/api/auth/register", null, payload);
        int status = registered.getResponse().getStatus();

        if (status == 201) {
            ok("가입 " + profile.get("email") + " (" + profile.get("nickname") + ")");
        } else if (status == 409) {
            skip("이미 있는 계정 " + profile.get("email") + " - 로그인만 한다");
        } else {
            throw new IllegalStateException(
                    "가입 실패 " + status + ": " + body(registered));
        }

        MvcResult logged = postJson("/api/auth/login", null,
                Map.of("email", profile.get("email"), "password", PASSWORD));

        assertThat(logged.getResponse().getStatus())
                .as("로그인 실패: %s", body(logged)).isEqualTo(200);

        return body(logged).get("access_token").asString();
    }

    /**
     * 관리자로 올린다. <b>DB 를 직접 고치는 유일한 대목이다.</b>
     *
     * <p>아무나 자기를 관리자로 올릴 수 있으면 등급이 의미가 없어서, 관리자를
     * 만드는 화면도 API 도 일부러 두지 않았다({@code db/06_add_user_level.sql}).
     * 매퍼에 메서드를 만들지 않고 여기서 SQL 을 직접 쓰는 것도 같은 이유다 -
     * 운영 코드에 "관리자로 승격" 이라는 길을 내지 않으려는 것이다.
     */
    private void promote(String email) {
        jdbc.update("UPDATE users SET user_level = 1 WHERE email = ?", email);
    }

    /**
     * 접시에 담긴 국수 그림 한 장을 PNG 로 만든다.
     *
     * <p>이미지 파일을 저장소에 같이 두지 않고 여기서 그린다. 첨부 기능을 확인하는
     * 데 필요한 건 "진짜 열리는 이미지" 하나뿐인데, 그걸 위해 바이너리를 커밋하면
     * diff 에도 안 잡히는 파일이 하나 늘어난다.
     *
     * <p>PNG 인코딩은 자바 표준 {@code ImageIO} 에 맡긴다 - 라이브러리를 늘릴 필요가 없다.
     */
    private static byte[] drawNoodlePng() {
        int width = 640;
        int height = 420;

        double centerX = width / 2.0;
        double centerY = height / 2.0 + 10;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // 세로로 눕힌 타원이 되게 y 쪽을 눌러준다.
                double dx = x - centerX;
                double dy = (y - centerY) * 1.35;
                double distance = Math.hypot(dx, dy);

                int red;
                int green;
                int blue;

                if (distance < 96) {
                    // 면 가닥. 각도에 따라 밝기를 흔들어 감아 올린 결을 만든다.
                    // 색은 고추장 양념 쪽으로 — 초록 · 파랑을 더 내린다.
                    double wave = Math.sin(Math.atan2(dy, dx) * 9 + distance * 0.16);
                    int tone = 236 + (int) (14 * wave) - (distance > 88 ? 22 : 0);
                    red = Math.min(255, tone);
                    green = Math.max(0, tone - 110);
                    blue = Math.max(0, tone - 150);
                } else if (distance < 150) {
                    red = 252; green = 250; blue = 246;       // 접시
                } else if (distance < 158) {
                    red = 226; green = 216; blue = 202;       // 접시 테두리
                } else {
                    // 배경. 위에서 아래로 따뜻하게 어두워진다.
                    double t = y / (double) height;
                    red = 250 - (int) (28 * t);
                    green = 243 - (int) (34 * t);
                    blue = 232 - (int) (40 * t);
                }

                image.setRGB(x, y, (clamp(red) << 16) | (clamp(green) << 8) | clamp(blue));
            }
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (Exception exc) {
            throw new IllegalStateException("첨부용 그림을 만들지 못했습니다.", exc);
        }
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    // -------------------------------------------------------------------- 본문

    @Test
    @DisplayName("회원 · 공지 · 식사 기록 · 후기를 만든다")
    void seed() throws Exception {
        localOnly();

        step("1", "회원 생성");
        signUp(ADMIN);
        promote(ADMIN.get("email"));
        ok(ADMIN.get("email") + " 를 관리자(user_level=1)로 올림");

        String member = signUp(MEMBER);

        // 등급을 바꿨으니 토큰을 다시 받는다. 토큰 안에 등급이 들어 있는 건
        // 아니지만(요청마다 DB 를 본다), is_admin 이 실린 user 는 로그인 응답에
        // 담겨오므로 화면이 쓰려면 새로 받아야 한다.
        String admin = signUp(ADMIN);
        ok("is_admin = " + body(getJson("/api/auth/me", admin)).get("is_admin").asBoolean());

        step("2", "공지사항 작성 (관리자 · 첨부 없음)");
        seedNotices(admin);

        step("3", "히스토리에 식사 기록 넣기 (회원)");
        Map<String, JsonNode> histories = seedHistories(member);

        step("4·5", "레시피 후기 작성 (첨부 없는 글 · 첨부 있는 글)");
        seedPosts(member, histories);

        step("6", "자유게시판 글 · 댓글 작성 (회원)");
        seedFreePosts(member);

        report(member);
    }

    /**
     * 2) 공지. 관리자만 쓸 수 있다.
     *
     * <p>공지에는 첨부파일이 없다. NOTICES 표에 파일 컬럼 자체가 없고 붙일 API 도
     * 없다 - 첨부는 후기(RECIPE_REVIEWS)에만 있다.
     */
    private void seedNotices(String admin) throws Exception {
        // 목록 API 는 배열이 아니라 {items, total, ...} 로 온다 (STEP 31).
        // 시드가 넣는 공지는 몇 건뿐이라 1페이지만 봐도 충분하다.
        MvcResult listed = getJson("/api/notices", null);

        if (notReady(listed)) {
            skip("공지사항이 아직 서버에 없다(501). 담당자가 구현하면 이 단계가 다시 돈다.");
            return;
        }

        Set<String> existing = new LinkedHashSet<>();
        for (JsonNode row : body(listed).path("items")) {
            existing.add(row.path("title").asString(""));
        }

        for (NoticeVO notice : NOTICES) {
            if (existing.contains(notice.title())) {
                skip("이미 있음 - " + notice.title());
                continue;
            }

            MvcResult created = postJson("/api/notices", admin, Map.of(
                    "title", notice.title(),
                    "content", notice.content(),
                    "pinned", notice.pinned()));

            assertThat(created.getResponse().getStatus())
                    .as("공지 실패: %s", body(created)).isEqualTo(201);

            JsonNode saved = body(created);
            ok((saved.path("pinned").asBoolean() ? "고정 " : "") + saved.path("title").asString());
        }
    }

    /**
     * 3) 식사 기록. 화면과 같은 순서로 간다.
     *
     * <p>레시피를 먼저 받아서({@code GET /api/recipes}) 그 값을 그대로 확정한다
     * ({@code POST /api/histories}). 확정한 순간의 레시피가 스냅샷으로 남는다.
     */
    private Map<String, JsonNode> seedHistories(String member) throws Exception {
        Map<String, JsonNode> histories = new LinkedHashMap<>();

        for (JsonNode row : body(getJson("/api/histories", member)).path("items")) {
            histories.put(row.path("food_name").asString(""), row);
        }

        for (String food : FOODS) {
            if (histories.containsKey(food)) {
                skip("이미 있음 - " + food);
                continue;
            }

            MvcResult fetched = getJson("/api/recipes/{name}", member, food);

            assertThat(fetched.getResponse().getStatus())
                    .as("레시피 실패: %s", body(fetched)).isEqualTo(200);

            JsonNode recipe = body(fetched);

            ObjectNode payload = MAPPER.createObjectNode();
            payload.put("food_name", recipe.path("food_name").asString());
            payload.put("recipe_name", recipe.path("recipe_name").asString());
            payload.set("recipe_json", recipe.path("recipe_json"));
            payload.set("ingredients_json", recipe.path("ingredients_json"));
            payload.set("substitutes_json", recipe.path("substitutes_json"));
            payload.put("estimated_cost", recipe.path("estimated_cost").asInt());

            MvcResult created = postJson("/api/histories", member, payload);

            assertThat(created.getResponse().getStatus())
                    .as("확정 실패: %s", body(created)).isEqualTo(201);

            JsonNode saved = body(created);
            histories.put(food, saved);

            ok(String.format("%s - %s (%,d원)",
                    saved.path("food_name").asString(),
                    saved.path("recipe_name").asString(),
                    saved.path("estimated_cost").asInt()));
        }

        return histories;
    }

    /**
     * 6) 자유게시판. 글을 쓰는 조건이 없어서 식사 기록 없이 바로 쓴다.
     *
     * <p>여러 번 돌려도 글이 쌓이지 않게 제목으로 먼저 찾아본다. 후기는
     * "한 기록에 한 글"(uq_reviews_meal)이라 DB 가 막아주지만, 자유게시판은
     * 같은 제목으로 몇 개든 쓸 수 있어서 여기서 직접 봐야 한다.
     */
    private void seedFreePosts(String member) throws Exception {
        MvcResult listed = getJson("/api/free-posts?size=50", null);

        if (notReady(listed)) {
            skip("자유게시판이 아직 서버에 없다(501).");
            return;
        }

        Set<String> already = new HashSet<>();
        for (JsonNode row : body(listed).path("items")) {
            already.add(row.path("title").asString());
        }

        for (FreePost item : FREE_POSTS) {
            if (already.contains(item.title())) {
                skip("이미 있는 글 - " + item.title());
                continue;
            }

            MvcResult created = postJson("/api/free-posts", member, Map.of(
                    "title", item.title(),
                    "content", item.content()));

            assertThat(created.getResponse().getStatus())
                    .as("자유게시판 글쓰기 - %s", item.title())
                    .isEqualTo(201);

            long postId = body(created).path("id").asLong();
            ok("#" + postId + " " + item.title());

            for (String comment : item.comments()) {
                MvcResult added = postJson(
                        "/api/free-posts/" + postId + "/comments", member, Map.of("content", comment));

                assertThat(added.getResponse().getStatus()).as("댓글").isEqualTo(201);
                ok("  댓글: " + comment);
            }
        }
    }

    /** 4·5) 후기. 하나는 첨부 없이, 하나는 첨부를 붙여서. */
    private void seedPosts(String member, Map<String, JsonNode> histories) throws Exception {
        if (notReady(getJson("/api/reviews", null))) {
            skip("게시판이 아직 서버에 없다(501). 담당자가 구현하면 이 단계가 다시 돈다.");
            skip("식사 기록은 이미 만들어져 있으니, 구현 뒤에 다시 돌리면 후기가 붙는다.");
            return;
        }

        for (Post item : POSTS) {
            JsonNode history = histories.get(item.food());

            if (history == null) {
                skip("식사 기록이 없어 건너뜀 - " + item.food());
                continue;
            }

            // 한 기록에는 후기를 한 번만 쓸 수 있다(uq_reviews_meal).
            // 이미 썼으면 recipe_review_id 가 채워져 있으니 그 글을 이어서 쓴다.
            JsonNode shared = history.path("recipe_review_id");
            Long reviewId = shared.isNull() || shared.isMissingNode() ? null : shared.asLong();

            if (reviewId != null) {
                skip("이미 공유한 기록 - #" + reviewId + " " + item.title());
            } else {
                MvcResult created = postJson("/api/reviews", member, Map.of(
                        "meal_history_id", history.path("id").asLong(),
                        "title", item.title(),
                        "content", item.content()));

                assertThat(created.getResponse().getStatus())
                        .as("글쓰기 실패: %s", body(created)).isEqualTo(201);

                reviewId = body(created).path("id").asLong();
                ok("#" + reviewId + " " + item.title());
            }

            if (!item.attach()) {
                skip("   첨부 없음");
                continue;
            }

            // 이미 붙어 있으면 다시 올리지 않는다. 다시 올려도 교체라 문제는 없지만,
            // 돌릴 때마다 같은 그림이 디스크에 하나씩 쌓인다.
            JsonNode detail = body(getJson("/api/reviews/" + reviewId, null));
            if (!detail.path("file").isNull() && !detail.path("file").isMissingNode()) {
                skip("   첨부 이미 있음");
                continue;
            }

            // 글쓰기(JSON)와 파일 업로드는 API 가 따로다.
            // 업로드가 실패해도 남는 건 '첨부 없는 멀쩡한 글' 이고, 첨부는 원래
            // 선택 사항이라 그게 정상 상태다.
            MockMultipartFile file = new MockMultipartFile(
                    "file", ATTACH_NAME, "image/png", drawNoodlePng());

            MvcResult attached = mvc.perform(multipart("/api/reviews/" + reviewId + "/file")
                            .file(file)
                            .header("Authorization", "Bearer " + member))
                    .andReturn();

            assertThat(attached.getResponse().getStatus())
                    .as("첨부 실패: %s", body(attached)).isEqualTo(200);

            JsonNode saved = body(attached).path("file");
            ok("   첨부 " + saved.path("name").asString()
                    + " (" + saved.path("size_text").asString() + ")"
                    + " -> " + saved.path("url").asString());
        }
    }

    /** 무엇이 만들어졌는지 한눈에 보여준다. */
    private void report(String member) throws Exception {
        System.out.println("\n[결과]");

        MvcResult notices = getJson("/api/notices", null);
        MvcResult posts = getJson("/api/reviews", null);

        if (notReady(notices)) {
            System.out.println("  공지      아직 서버에 없음 (501)");
        } else {
            List<JsonNode> rows = new ArrayList<>();
            body(notices).path("items").forEach(rows::add);
            long pinned = rows.stream().filter(r -> r.path("pinned").asBoolean()).count();
            System.out.printf("  공지      %d 건 (고정 %d)%n", rows.size(), pinned);
        }

        if (notReady(posts)) {
            System.out.println("  게시글    아직 서버에 없음 (501)");
        } else {
            JsonNode page = body(posts);
            long withFile = 0;
            for (JsonNode row : page.path("items")) {
                if (row.path("has_file").asBoolean()) {
                    withFile++;
                }
            }
            System.out.printf("  게시글    %d 건 (첨부 %d)%n", page.path("total").asInt(), withFile);
        }

        JsonNode stats = body(getJson("/api/histories/stats", member));
        System.out.printf("  식사 기록 %d 건 · 누적 %,d원%n",
                stats.path("count").asInt(), stats.path("total_cost").asInt());

        System.out.printf("%n  로그인    %s (관리자) / %s%n",
                ADMIN.get("email"), MEMBER.get("email"));
    }
}
