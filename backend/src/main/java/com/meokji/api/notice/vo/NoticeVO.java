package com.meokji.api.notice.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meokji.api.util.Times;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * NOTICES 표 한 행. 공지사항.
 *
 * 파일에 두지 않고 표로 만든 이유는 운영하면서 계속 새로 쓰는 값이기
 * 때문이다. 파일에 두면 공지 하나 올릴 때마다 서버를 다시 배포해야 한다.
 * (카테고리 · 룰렛처럼 안 바뀌는 목록은 반대로 파일에 둔다 — catalog 기능)
 *
 * 이 클래스 하나가 세 가지 일을 한다.
 *   1. DB 한 행을 담는다          (MyBatis 가 pinnedYn 같은 칸을 채운다)
 *   2. 관리자가 보낸 값을 받는다   (검증이 그래서 붙어 있다)
 *   3. 화면으로 나갈 모양이 된다   (@JsonIgnore 가 붙은 칸은 안 나간다)
 *
 * 나가는 모양: { "id", "title", "content", "view_count", "pinned", "created_at" }
 *
 * <p>이 VO 에만 있는 사정 — 'Y'/'N' 글자와 true/false 를 같이 다뤄야 한다.
 * DB 컬럼은 CHAR(1) 이고 화면은 참/거짓으로 쓴다. 그래서 칸이 둘
 * (pinnedYn · pinned)이고 아래 getPinned 가 그 사이를 잇는다.
 */
@Data
public class NoticeVO {

    /** 시퀀스로 받는 번호. 상세 · 수정 · 삭제 주소가 이 값을 쓴다 */
    private Long id;

    /** 작성자(관리자). 화면에 이름을 보여주지는 않는다 */
    // 공지는 "운영자가 쓴 글" 이라 누가 썼는지가 중요하지 않다.
    // 그래도 표에는 남긴다 - 문제가 생겼을 때 누가 올렸는지는 알아야 한다
    @JsonIgnore
    private Long userId;

    @NotBlank(message = "제목을 입력해주세요")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다")
    private String title;

    /** VARCHAR2(4000). 길이 단위가 BYTE 라 한글로는 약 1333자다 */
    // @Size 를 걸지 않은 이유 — 글자 수로 막으면 한글과 영어의 한계가
    // 달라진다. 1333 으로 막으면 영어 공지가 짧게 끊기고, 4000 으로 막으면
    // 한글 공지가 DB 에서 걸려 500 이 난다. 길이 처리는 구현할 때 정할 부분이다
    @NotBlank(message = "내용을 입력해주세요")
    private String content;

    /**
     * 조회수. 올리는 것은 POST /{id}/view 뿐이다.
     *
     * READ_ONLY 라 화면이 보낸 view_count 는 안 들어오고, 응답으로는 나간다.
     * @JsonIgnore 를 붙이면 목록·상세에서도 사라져서 화면이 조회수를 못 쓴다.
     */
    // Integer 인 이유 — 아직 아무도 안 본 공지도 DDL 의 DEFAULT 0 으로
    // 채워지지만, MyBatis 가 NULL 을 int 에 넣으려 하면 에러가 난다
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer viewCount;

    /** DB 에 들어가는 값. 'Y' = 상단 고정. 화면과는 pinned 로 주고받는다. */
    @JsonIgnore
    private String pinnedYn;

    /** 화면이 보낸 값. 안 보내면 null (고정 안 함) */
    // Boolean 인 이유 — "안 보냄(null)" 을 구별해야 한다. boolean 이면
    // 안 보낸 것이 false 가 되어, 고정된 공지를 수정할 때 pinned 를
    // 빼고 보내면 고정이 풀린다
    private Boolean pinned;

    /** 'N' = 지운 공지. 목록 · 상세에서 빠진다 */
    @JsonIgnore
    private String useYn;

    @JsonIgnore
    private LocalDateTime createdAt;

    /** 고친 시각. 화면에 내보내지 않는다 - 공지는 "올린 날" 만 보여준다 */
    @JsonIgnore
    private LocalDateTime updatedAt;

    /**
     * 화면이 읽는 pinned.
     *
     * DB 에서 읽어온 행이면 pinnedYn('Y'/'N')을 뒤집어 주고,
     * 화면이 보낸 요청이면 받은 값을 그대로 준다.
     */
    // 한 VO 가 요청과 응답을 겸하면서 생긴 자리다. 요청으로 들어올 때는
    // pinned 만 차 있고(pinnedYn 은 null), DB 에서 읽을 때는 pinnedYn 만
    // 차 있다. 어느 쪽이든 쓸 수 있게 둘을 보고 가린다.
    //
    // @JsonProperty 가 없는 이유 — 메서드 이름이 getPinned 라서
    // Jackson 이 그대로 pinned 칸으로 알아본다. 이름이 맞으면 안 적는다
    public Boolean getPinned() {
        if (pinnedYn != null) {
            return "Y".equals(pinnedYn);
        }
        return pinned;
    }

    /** 화면이 읽는 created_at. 예) "2026-09-11T21:29:42Z" */
    // 이름을 손으로 적는 이유 — snake_case 설정은 메서드 이름에서
    // created_at_text 를 만든다. 뒤에 붙은 Text 까지 이름에 들어간다
    @JsonProperty("created_at")
    public String getCreatedAtText() {
        return Times.toText(createdAt);
    }
}
