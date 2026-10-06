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
 *
 * 이 클래스 하나가 세 가지 일을 한다.
 *   1. DB 한 행을 담는다          (MyBatis 가 pinnedYn 같은 칸을 채운다)
 *   2. 관리자가 보낸 값을 받는다   (검증이 그래서 붙어 있다)
 *   3. 화면으로 나갈 모양이 된다   (@JsonIgnore 가 붙은 칸은 안 나간다)
 *
 * 나가는 모양: { "id", "title", "content", "view_count", "pinned", "created_at" }
 */
@Data
public class NoticeVO {

    private Long id;

    /** 작성자(관리자). 화면에 이름을 보여주지는 않는다 */
    @JsonIgnore
    private Long userId;

    @NotBlank(message = "제목을 입력해주세요")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다")
    private String title;

    /** VARCHAR2(4000). 길이 단위가 BYTE 라 한글로는 약 1333자다 */
    @NotBlank(message = "내용을 입력해주세요")
    private String content;

    /**
     * 조회수. 올리는 것은 POST /{id}/view 뿐이다.
     *
     * READ_ONLY 라 화면이 보낸 view_count 는 안 들어오고, 응답으로는 나간다.
     * @JsonIgnore 를 붙이면 목록·상세에서도 사라져서 화면이 조회수를 못 쓴다.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer viewCount;

    /** DB 에 들어가는 값. 'Y' = 상단 고정. 화면과는 pinned 로 주고받는다. */
    @JsonIgnore
    private String pinnedYn;

    /** 화면이 보낸 값. 안 보내면 null (고정 안 함) */
    private Boolean pinned;

    @JsonIgnore
    private String useYn;

    @JsonIgnore
    private LocalDateTime createdAt;

    @JsonIgnore
    private LocalDateTime updatedAt;

    /**
     * 화면이 읽는 pinned.
     *
     * DB 에서 읽어온 행이면 pinnedYn('Y'/'N')을 뒤집어 주고,
     * 화면이 보낸 요청이면 받은 값을 그대로 준다.
     */
    public Boolean getPinned() {
        if (pinnedYn != null) {
            return "Y".equals(pinnedYn);
        }
        return pinned;
    }

    /** 화면이 읽는 created_at. 예) "2026-09-11T21:29:42Z" */
    @JsonProperty("created_at")
    public String getCreatedAtText() {
        return Times.toText(createdAt);
    }
}
