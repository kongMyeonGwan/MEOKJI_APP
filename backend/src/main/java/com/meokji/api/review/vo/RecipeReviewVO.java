package com.meokji.api.review.vo;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meokji.api.util.Times;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

import tools.jackson.databind.JsonNode;

/**
 * 레시피 후기(게시글). RECIPE_REVIEWS 표 한 행이면서, 요청과 응답을 같이 겸한다.
 *
 * mealHistoryId 가 NOT NULL 이다. 후기는 자기 식사 기록에서만 쓸 수
 * 있다는 규칙을 FK 로 강제한다. 같은 기록으로 두 번 쓰는 것은
 * uq_reviews_meal 이 막는다.
 *
 * 첨부파일은 컬럼 여섯 개로 들고 있다. 파일 본체는 디스크(UPLOAD_DIR)에
 * 두고 DB 에는 경로만 적는다. 글 하나당 사진 하나라서 표를 나누지 않았다.
 *
 * 목록 · 상세 · 요청을 VO 세 개로 나눴다가 하나로 합쳤다. 자유게시판
 * (free/vo/FreePostVO)이 먼저 그렇게 바꿨고 같은 이유다.
 *
 *   1. 칸이 있다고 SQL 이 다 쓰는 것이 아니다. 목록 SQL 은 목록에 쓸 칸만
 *      고르고 본문은 거기서 잘라 온다. 안 고른 칸(comments · file ·
 *      recipeJson)은 null 로 남는다. 인기 목록(GET /api/reviews/popular)도
 *      같은 표를 좋아요순으로 뽑는 것이라 이 VO 를 그대로 쓴다.
 *   2. INSERT · UPDATE 가 컬럼을 하나씩 적는다. 표에 없는 칸이 여기 있어도
 *      SQL 은 모른다.
 *   3. 받을 때와 보낼 때가 다른 칸은 READ_ONLY 로 한쪽만 연다.
 *
 * 칸을 막는 방법이 두 가지다
 *
 *   @JsonIgnore   양쪽 다 막는다. 화면이 보내서도 안 되고 봐서도 안 되는 칸 —
 *                 글쓴이 id, 파일의 서버 경로 · UUID 이름, use_yn.
 *   READ_ONLY     받을 때만 막는다. 화면은 봐야 하지만 정할 수는 없는 칸 —
 *                 닉네임, 조회수, 좋아요 · 댓글 수, 레시피 스냅샷, 첨부 정보.
 *
 * 화면이 정해도 되는 칸은 셋뿐이다 — mealHistoryId · title · content.
 * 그래서 검증도 그 셋에만 붙어 있다.
 *
 * 막는 것이 마지막 방어선은 아니다. 글쓴이는 서비스가 토큰에서 꺼내 덮어쓰고,
 * 조회수는 SQL 이 view_count + 1 로만 올린다. 화면이 무엇을 보내든 서버가
 * 그 값을 SQL 에 쓰지 않는 것이 먼저다. (docs/BOARD_SPEC.md 3번)
 */
@Data
public class RecipeReviewVO {

    private Long id;

    /** 글쓴이. 요청 본문이 아니라 토큰에서 꺼낸다 */
    @JsonIgnore
    private Long userId;

    /**
     * 어떤 식사의 후기인지. NOT NULL — 자기 기록만 공유할 수 있다.
     *
     * 이 칸만은 화면이 고른다. 대신 서비스가 그 기록이 정말 로그인한 사람의
     * 것인지 확인해야 한다 — 번호만 바꿔 보내면 남의 기록이 공유된다.
     */
    @NotNull(message = "어떤 식사 기록을 공유할지 골라주세요")
    @Min(value = 1, message = "식사 기록 번호가 올바르지 않습니다")
    private Long mealHistoryId;

    @NotBlank(message = "제목을 입력해주세요")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다")
    private String title;

    /** 목록에서는 미리보기로 잘라서 담는다 (자르는 일은 SQL 이 한다) */
    @NotBlank(message = "내용을 입력해주세요")
    private String content;

    // ---------- 첨부파일. 글 하나당 하나다 ----------
    //
    // 표에는 FILE_* 컬럼이 여섯 개지만 여기 남는 것은 file_yn 하나다.
    // 나머지 다섯은 ReviewFileVO 가 들고 있다 (아래 file 칸). 같은 값을
    // 두 군데 두지 않으려는 것이다.
    //
    // file_yn 만 남긴 이유: 목록은 첨부 정보를 안 읽어온다. 글마다
    // 500자짜리 경로까지 가져올 일이 없어서 CHAR(1) 하나만 보고
    // 제목 옆 클립 표시를 붙인다. 그때 file 은 null 이다.

    /** 'Y' = 첨부파일이 있다 */
    @JsonIgnore
    private String fileYn;

    // ---------- 나머지 ----------

    /** 조회수. 올리는 것은 POST /{id}/view 뿐이다 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer viewCount;

    /** 'N' = 사용자가 지운 글. 조회에는 use_yn='Y' 를 붙여야 한다 */
    @JsonIgnore
    private String useYn;

    @JsonIgnore
    private LocalDateTime createdAt;

    @JsonIgnore
    private LocalDateTime updatedAt;

    // ---------- 표에 없는 칸. 조회할 때 JOIN 하거나 서비스가 채운다 ----------

    /** 글쓴이 닉네임 (USERS 와 JOIN) */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String nickname;

    /**
     * 좋아요 수. REVIEW_LIKES 를 COUNT 로 센다.
     *
     * 컬럼으로 들고 있으면 좋아요를 취소했을 때 같이 줄이는 것을 잊어
     * 조용히 어긋난다. 댓글 수도 같은 이유다.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer likeCount;

    /** 살아있는 댓글 수. POST_COMMENTS 에서 BOARD_TYPE='R' 만 센다 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer commentCount;

    // 아래 셋은 글쓴이의 식사 기록(MEAL_HISTORIES)에 남은 그때의 값이다.
    // 후기 표에는 없으니 JOIN 해서 가져온다.

    /** 무슨 음식인지 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String foodName;

    /** 레시피 이름. 없으면 화면이 foodName 을 대신 쓴다 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String recipeName;

    /** 예상 재료비(원). 인기 목록이 쓴다 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer estimatedCost;

    /**
     * 레시피 스냅샷. 상세에서만 채운다.
     *
     * 후기를 읽은 사람이 바로 따라 만들 수 있어야 해서, 글쓴이의 식사 기록에
     * 저장된 그때의 레시피를 붙여 보낸다. 지금 레시피를 다시 부르지 않는 이유는
     * 원본이 바뀌면 글과 레시피가 어긋나기 때문이다.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private JsonNode recipeJson;

    /** 재료 스냅샷. 상세에서만 채운다 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private JsonNode ingredientsJson;

    /**
     * 첨부 사진. 없거나 목록이면 null.
     *
     * 표의 FILE_* 다섯 칸이 여기 담긴다 — 매퍼의 association 이 채우고,
     * 크기 표시와 주소는 서비스가 마저 넣는다.
     * (본보기: resources/mapper/FreePostMapper.xml 의 postWithFile)
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private ReviewFileVO file;

    /** 지금 보는 사람이 좋아요를 눌렀는지. 비로그인이면 false */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean liked;

    /**
     * 로그인한 사람이 쓴 글인지. 상세에서만 채운다.
     *
     * boolean 이 아니라 Boolean 인 이유: boolean isMine 이면 롬복이 isMine() 을
     * 만들고, Jackson 이 그 is 를 접두어로 보고 떼어내 "mine" 으로 내보낸다.
     * 화면은 is_mine 을 읽으므로 값이 통째로 사라진다.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean isMine;

    /** 댓글. 상세에서만 채운다. 글을 열면 댓글도 늘 같이 보이기 때문이다 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<ReviewCommentVO> comments;

    /**
     * 첨부가 붙어 있는지. 목록은 이 값으로 제목 옆에 클립 표시를 붙인다.
     *
     * file_yn 자체는 서버 사정이라 내보내지 않고, 이 메서드만 has_file 로 나간다.
     */
    @JsonProperty("has_file")
    public boolean getHasFile() {
        return "Y".equals(fileYn);
    }

    /** 지워지지 않은 글인지. @JsonProperty 가 없으니 응답에는 나가지 않는다 */
    public boolean alive() {
        return "Y".equals(useYn);
    }

    /** 화면이 읽는 created_at. 예) "2026-09-11T21:29:42Z" */
    @JsonProperty("created_at")
    public String getCreatedAtText() {
        return Times.toText(createdAt);
    }

    /** 화면이 읽는 updated_at */
    @JsonProperty("updated_at")
    public String getUpdatedAtText() {
        return Times.toText(updatedAt);
    }
}
