package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 자유게시판 — 글 · 댓글 · 사진의 한살이를 처음부터 끝까지 돌려본다.
 *
 * <p>정답지(fixtures)와 대조하지 않는 이유: 자유게시판은 글을 쓸 때마다 id 와
 * 시각이 달라진다. 고정된 답을 둘 수 없으니 <b>규칙</b>을 본다 — 쓴 글이 그대로
 * 읽히는지, 남의 글을 못 고치는지, 지운 글이 404 가 되는지.
 *
 * <p>테스트가 만든 글은 끝에서 지운다. 데모 계정을 그대로 쓰기 때문에
 * 남겨두면 목록 건수가 돌릴 때마다 늘어난다.
 */
@SpringBootTest
@DisplayName("자유게시판")
class FreePostTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final String MEMBER_EMAIL = "minji@example.com";
    private static final String MEMBER_PASSWORD = "meokji1234!";

    private static final String ADMIN_EMAIL = "admin@meokji.com";
    private static final String ADMIN_PASSWORD = "meokji1234!";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;
    private String auth;

    /** 이 테스트가 만든 글. 끝나면 전부 지운다 */
    private final List<Long> made = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        // .apply(springSecurity()) 는 보안 필터를 붙인다. 이 줄이 없으면 MockMvc 가
        // Controller 를 바로 불러서 JwtAuthenticationFilter 를 건너뛴다. 그러면
        // @AuthenticationPrincipal 이 늘 null 이고 로그인이 필요한 주소도 통과해서,
        // 테스트는 통과하는데 실제 서버와는 다른 길을 재는 꼴이 된다.
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        auth = login(MEMBER_EMAIL, MEMBER_PASSWORD);
    }

    /**
     * 테스트가 만든 글을 치운다.
     *
     * <p>본문 끝에서 지우지 않고 여기서 지우는 이유: 중간에서 assert 가 깨지면
     * 그 뒤 줄이 실행되지 않아 글이 DB 에 남는다. 데모 계정을 그대로 쓰기 때문에
     * 남은 글은 다음 실행의 목록 건수를 어긋나게 한다.
     */
    @AfterEach
    void tearDown() throws Exception {
        for (Long postId : made) {
            mvc.perform(delete("/api/free-posts/" + postId).header("Authorization", auth)).andReturn();
        }
        made.clear();
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn();
        return "Bearer " + body(result).get("access_token").asString();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return MAPPER.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /** 글 하나 써두고 id 를 돌려준다 */
    private long writePost(String title, String content) throws Exception {
        MvcResult result = mvc.perform(post("/api/free-posts")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"content\":\"" + content + "\"}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).as("글쓰기").isEqualTo(201);

        long postId = body(result).get("id").asLong();
        made.add(postId); // tearDown 이 치운다
        return postId;
    }

    @Test
    @DisplayName("쓰고 읽고 고치고 지우는 한살이가 그대로 돈다")
    void postLifecycle() throws Exception {
        long postId = writePost("테스트 글입니다", "본문입니다.\\n둘째 줄입니다.");

        // 1. 상세로 읽으면 쓴 그대로다. 내 글이라 is_mine 이 true
        MvcResult detail = mvc.perform(get("/api/free-posts/" + postId).header("Authorization", auth))
                .andReturn();
        assertThat(detail.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(detail).get("title").asString()).isEqualTo("테스트 글입니다");
        assertThat(body(detail).get("is_mine").asBoolean()).isTrue();
        assertThat(body(detail).get("file").isNull()).as("첨부는 아직 없다").isTrue();
        assertThat(body(detail).get("comments").size()).isEqualTo(0);

        // 2. 로그인 없이도 보인다. 다만 내 글로 보이지는 않는다
        MvcResult guest = mvc.perform(get("/api/free-posts/" + postId)).andReturn();
        assertThat(guest.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(guest).get("is_mine").asBoolean()).as("비로그인은 남의 글").isFalse();

        // 3. 목록 첫 페이지에 방금 쓴 글이 맨 앞에 온다 (최신순)
        MvcResult list = mvc.perform(get("/api/free-posts?page=1&size=20")).andReturn();
        assertThat(list.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(list).get("items").get(0).get("id").asLong()).isEqualTo(postId);
        assertThat(body(list).get("items").get(0).get("comment_count").asInt()).isEqualTo(0);
        assertThat(body(list).get("total_pages").asInt()).isGreaterThanOrEqualTo(1);

        // 4. 수정하면 바뀐 값이 읽힌다
        MvcResult updated = mvc.perform(put("/api/free-posts/" + postId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"고친 제목\",\"content\":\"고친 본문\"}"))
                .andReturn();
        assertThat(updated.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(updated).get("title").asString()).isEqualTo("고친 제목");

        // 5. 조회수는 /view 를 부를 때만 오른다
        MvcResult viewed = mvc.perform(post("/api/free-posts/" + postId + "/view")).andReturn();
        assertThat(viewed.getResponse().getContentAsString()).isEqualTo("1");

        // 6. 지우면 상세가 404 다
        MvcResult removed = mvc.perform(delete("/api/free-posts/" + postId).header("Authorization", auth))
                .andReturn();
        assertThat(removed.getResponse().getStatus()).isEqualTo(204);

        MvcResult gone = mvc.perform(get("/api/free-posts/" + postId)).andReturn();
        assertThat(gone.getResponse().getStatus()).as("지운 글은 404").isEqualTo(404);
    }

    @Test
    @DisplayName("댓글을 달고 고치고 지운다. 댓글 수도 같이 움직인다")
    void commentLifecycle() throws Exception {
        long postId = writePost("댓글 테스트", "댓글을 달아보자");

        // 1. 댓글 쓰기
        MvcResult added = mvc.perform(post("/api/free-posts/" + postId + "/comments")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"첫 댓글\"}"))
                .andReturn();
        assertThat(added.getResponse().getStatus()).isEqualTo(201);
        assertThat(body(added).get("content").asString()).isEqualTo("첫 댓글");
        assertThat(body(added).get("is_mine").asBoolean()).isTrue();
        assertThat(body(added).get("nickname").isNull()).as("닉네임이 같이 온다").isFalse();

        long commentId = body(added).get("id").asLong();

        // 2. 상세에 댓글이 같이 실려 온다
        MvcResult detail = mvc.perform(get("/api/free-posts/" + postId).header("Authorization", auth))
                .andReturn();
        assertThat(body(detail).get("comments").size()).isEqualTo(1);

        // 3. 목록의 댓글 수도 1 이다
        MvcResult list = mvc.perform(get("/api/free-posts?page=1&size=20")).andReturn();
        assertThat(body(list).get("items").get(0).get("comment_count").asInt()).isEqualTo(1);

        // 4. 댓글 수정
        MvcResult edited = mvc.perform(put("/api/free-posts/" + postId + "/comments/" + commentId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"고친 댓글\"}"))
                .andReturn();
        assertThat(edited.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(edited).get("content").asString()).isEqualTo("고친 댓글");

        // 5. 빈 댓글은 막힌다 (@Valid)
        MvcResult blank = mvc.perform(post("/api/free-posts/" + postId + "/comments")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andReturn();
        // 422 다. @Valid 검사 실패는 ApiExceptionHandler 가 422 로 보낸다 (400 이 아니다)
        assertThat(blank.getResponse().getStatus()).as("빈 댓글은 422").isEqualTo(422);

        // 6. 댓글 삭제 → 상세에서도 목록 댓글 수에서도 빠진다
        MvcResult removed = mvc.perform(
                        delete("/api/free-posts/" + postId + "/comments/" + commentId)
                                .header("Authorization", auth))
                .andReturn();
        assertThat(removed.getResponse().getStatus()).isEqualTo(204);

        MvcResult after = mvc.perform(get("/api/free-posts/" + postId)).andReturn();
        assertThat(body(after).get("comments").size()).isEqualTo(0);

    }

    @Test
    @DisplayName("남의 글 · 남의 댓글은 고치거나 지울 수 없다 (403)")
    void onlyOwnerCanEdit() throws Exception {
        long postId = writePost("주인만 고친다", "남이 못 고쳐야 한다");

        MvcResult comment = mvc.perform(post("/api/free-posts/" + postId + "/comments")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"내 댓글\"}"))
                .andReturn();
        long commentId = body(comment).get("id").asLong();

        String otherAuth = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        MvcResult updateTry = mvc.perform(put("/api/free-posts/" + postId)
                        .header("Authorization", otherAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"남이 고침\",\"content\":\"고쳐졌나\"}"))
                .andReturn();
        assertThat(updateTry.getResponse().getStatus()).as("남의 글 수정").isEqualTo(403);

        MvcResult deleteTry = mvc.perform(delete("/api/free-posts/" + postId)
                        .header("Authorization", otherAuth))
                .andReturn();
        assertThat(deleteTry.getResponse().getStatus()).as("남의 글 삭제").isEqualTo(403);

        MvcResult commentTry = mvc.perform(
                        delete("/api/free-posts/" + postId + "/comments/" + commentId)
                                .header("Authorization", otherAuth))
                .andReturn();
        assertThat(commentTry.getResponse().getStatus()).as("남의 댓글 삭제").isEqualTo(403);

        // 로그인하지 않으면 401
        MvcResult guestTry = mvc.perform(delete("/api/free-posts/" + postId)).andReturn();
        assertThat(guestTry.getResponse().getStatus()).as("비로그인 삭제").isEqualTo(401);

    }

    @Test
    @DisplayName("사진을 붙이고 받아보고 뗀다. 못 받는 파일은 400")
    void fileLifecycle() throws Exception {
        long postId = writePost("사진 테스트", "사진을 붙여보자");

        // 1. png 한 장 붙이기
        MockMultipartFile png = new MockMultipartFile(
                "file", "내 사진.png", "image/png", new byte[] { 1, 2, 3, 4, 5 });

        MvcResult attached = mvc.perform(multipart("/api/free-posts/" + postId + "/file")
                        .file(png)
                        .header("Authorization", auth))
                .andReturn();
        assertThat(attached.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(attached).get("name").asString()).isEqualTo("내 사진.png");
        assertThat(body(attached).get("ext").asString()).isEqualTo("png");
        assertThat(body(attached).get("url").asString())
                .isEqualTo("/api/free-posts/" + postId + "/file");
        assertThat(body(attached).get("size_text").asString()).isEqualTo("5 B");

        // 서버 사정은 안 나간다. 저장 폴더와 UUID 이름이 보이면 남의 파일 주소를 짐작할 수 있다
        assertThat(body(attached).has("path")).as("저장 폴더").isFalse();
        assertThat(body(attached).has("saved_name")).as("UUID 이름").isFalse();

        // 2. 목록의 has_file 이 켜진다
        MvcResult list = mvc.perform(get("/api/free-posts?page=1&size=20")).andReturn();
        assertThat(body(list).get("items").get(0).get("has_file").asBoolean()).isTrue();

        // 3. 받아보면 올린 바이트 그대로다
        MvcResult downloaded = mvc.perform(get("/api/free-posts/" + postId + "/file")).andReturn();
        assertThat(downloaded.getResponse().getStatus()).isEqualTo(200);
        assertThat(downloaded.getResponse().getContentAsByteArray()).isEqualTo(new byte[] { 1, 2, 3, 4, 5 });
        assertThat(downloaded.getResponse().getContentType()).isEqualTo("image/png");

        // 4. 받을 수 없는 확장자는 400
        MockMultipartFile exe = new MockMultipartFile(
                "file", "나쁜파일.exe", "application/octet-stream", new byte[] { 9 });
        MvcResult badExt = mvc.perform(multipart("/api/free-posts/" + postId + "/file")
                        .file(exe)
                        .header("Authorization", auth))
                .andReturn();
        assertThat(badExt.getResponse().getStatus()).as("exe 는 못 올린다").isEqualTo(400);

        // 5. 떼면 상세의 file 이 null 이 된다
        MvcResult detached = mvc.perform(delete("/api/free-posts/" + postId + "/file")
                        .header("Authorization", auth))
                .andReturn();
        assertThat(detached.getResponse().getStatus()).isEqualTo(204);

        MvcResult detail = mvc.perform(get("/api/free-posts/" + postId)).andReturn();
        assertThat(body(detail).get("file").isNull()).isTrue();

    }

    @Test
    @DisplayName("검색은 제목과 본문에서 찾는다")
    void searchFindsTitleAndContent() throws Exception {
        long postId = writePost("김치찌개 끓이는 법", "묵은지가 있어야 맛있다");

        MvcResult byTitle = mvc.perform(get("/api/free-posts?keyword=김치찌개")).andReturn();
        assertThat(body(byTitle).get("total").asInt()).isGreaterThanOrEqualTo(1);

        MvcResult byContent = mvc.perform(get("/api/free-posts?keyword=묵은지")).andReturn();
        assertThat(body(byContent).get("total").asInt()).isGreaterThanOrEqualTo(1);

        MvcResult nothing = mvc.perform(get("/api/free-posts?keyword=없는말없는말")).andReturn();
        assertThat(body(nothing).get("total").asInt()).isEqualTo(0);
        assertThat(body(nothing).get("items").size()).isEqualTo(0);

    }
}
