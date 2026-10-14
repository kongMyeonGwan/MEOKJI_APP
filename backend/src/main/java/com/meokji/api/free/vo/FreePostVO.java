package com.meokji.api.free.vo;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meokji.api.util.Times;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * 자유게시판 글. FREE_POSTS 표 한 행이면서, 요청과 응답을 같이 겸한다.
 *
 * 목록 · 상세 · 요청을 VO 세 개로 나눴다가 하나로 합쳤다. 나눠서 얻는 것이
 * 이 게시판 규모에서는 크지 않았고, 서비스에 옮겨 담는 코드만 늘었다.
 * 합칠 수 있었던 이유는 다음 세 가지다.
 *
 *   1. 칸이 있다고 다 채우는 것이 아니다. 목록은 FreePostRepository 의
 *      findPage 가 고른 칸만 채우고 content 는 거기서 200자로 자른다.
 *      comments · file 처럼 남는 칸은 그냥 null 로 둔다.
 *   2. 표에 쓰는 것은 FreePostEntity 다. 표에 없는 칸(comments ·
 *      isMine · file)이 여기 있어도 INSERT · UPDATE 와 상관없다.
 *   3. 받을 때와 보낼 때가 다른 칸은 READ_ONLY 로 한쪽만 연다.
 *      FreePostCommentVO 가 먼저 이렇게 쓰고 있었다.
 *
 * 칸을 막는 방법이 두 가지다
 *
 *   @JsonIgnore   양쪽 다 막는다. 화면이 보내서도 안 되고 봐서도 안 되는 칸 —
 *                 글쓴이 id, 파일의 서버 경로 · UUID 이름, use_yn.
 *   READ_ONLY     받을 때만 막는다. 화면은 봐야 하지만 정할 수는 없는 칸 —
 *                 닉네임, 조회수, 댓글 수, 첨부 정보, 내 글 여부, 댓글.
 *
 * 막는 것이 마지막 방어선은 아니다. 글쓴이는 FreePostServiceImpl 이 토큰에서
 * 꺼내 덮어쓰고, 조회수는 SQL 이 view_count + 1 로만 올린다. 화면이 무엇을
 * 보내든 서버가 그 값을 SQL 에 쓰지 않는 것이 먼저다. (docs/BOARD_SPEC.md 3번)
 *
 * 날짜는 LocalDateTime 으로 받아 두고 나갈 때만 문자열로 바꾼다.
 * 아래 getCreatedAtText() 가 그 일을 한다.
 */
@Data
public class FreePostVO {

    private Long id;

    /** 글쓴이. 요청 본문이 아니라 토큰에서 꺼낸다 */
    @JsonIgnore
    private Long userId;

    @NotBlank(message = "제목을 입력해주세요")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다")
    private String title;

    /** 목록에서는 앞 200자만 담긴다 (findPage 가 잘라서 가져온다) */
    @NotBlank(message = "내용을 입력해주세요")
    @Size(max = 4000, message = "내용은 4000자 이하여야 합니다")
    private String content;

    // ---------- 첨부파일. 글 하나당 하나다 ----------
    //
    // 표에는 FILE_* 컬럼이 여섯 개지만 여기 남는 것은 file_yn 하나다.
    // 나머지 다섯은 FreeFileVO 가 들고 있다 (아래 file 칸). 같은 값을
    // 두 군데 두지 않으려는 것이다.
    //
    // file_yn 만 남긴 이유: 목록은 첨부 정보를 안 읽어온다. 글마다
    // 500자짜리 경로까지 가져올 일이 없어서 CHAR(1) 하나만 보고
    // 제목 옆 클립 표시를 붙인다. 그때 file 은 null 이다.

    /** 'Y' = 첨부 있음 */
    @JsonIgnore
    private String fileYn;

    // ---------- 나머지 ----------

    /** 조회수. 올리는 것은 POST /{id}/view 뿐이다 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer viewCount;

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

    /** 살아있는 댓글 수. 목록은 COUNT 로 세고, 상세는 담은 댓글 수를 넣는다 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer commentCount;

    /**
     * 첨부 사진. 없거나 목록이면 null.
     *
     * 표의 FILE_* 다섯 칸과 크기 표시 · 주소를 서비스가
     * FreePostEntity 에서 옮겨 담는다.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private FreeFileVO file;

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
    private List<FreePostCommentVO> comments;

    /**
     * 첨부가 붙어 있는지. 목록은 이 값으로 제목 옆에 클립 표시를 붙인다.
     *
     * file_yn 자체는 서버 사정이라 내보내지 않고, 이 메서드만 has_file 로 나간다.
     */
    @JsonProperty("has_file")
    public boolean getHasFile() {
        return "Y".equals(fileYn);
    }

    /** 화면이 읽는 created_at. 예) "2026-10-04T12:30:00Z" */
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
