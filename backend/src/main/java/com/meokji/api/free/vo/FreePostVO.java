package com.meokji.api.free.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.meokji.api.util.Times;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * FREE_POSTS 표 한 행. 자유게시판 글.
 *
 * 이 클래스가 두 가지 일을 한다.
 *   1. DB 한 행을 담는다        (MyBatis 가 fileYn 같은 칸을 채운다)
 *   2. 화면이 보낸 값을 받는다  (검증이 그래서 붙어 있다)
 *
 * 화면으로 나갈 때는 이 클래스를 그대로 쓰지 않는다. 목록은 FreePostListVO,
 * 상세는 FreePostDetailVO 다. 표 한 행에는 화면이 알 필요 없는 칸(파일 경로 ·
 * 글쓴이 id)이 섞여 있고, 반대로 화면에 필요한 칸(닉네임 · 댓글 수)은 없다.
 *
 * 받을 때 막아야 하는 칸
 *   userId · viewCount · useYn · 날짜 · FILE_* 에 @JsonIgnore 를 붙였다.
 *   안 붙이면 화면이 { "title": "...", "view_count": 99999 } 처럼 보내서
 *   조회수를 마음대로 올릴 수 있다. 글쓴이도 토큰에서 꺼내야 위조가 안 된다.
 *   (docs/BOARD_SPEC.md 3번)
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

    @NotBlank(message = "내용을 입력해주세요")
    @Size(max = 4000, message = "내용은 4000자 이하여야 합니다")
    private String content;

    // ---------- 첨부파일. 글 하나당 하나다 ----------

    /** 'Y' = 첨부 있음 */
    @JsonIgnore
    private String fileYn;

    /** 서버에 저장한 이름 (UUID). 올린 이름을 그대로 쓰면 서로 덮어쓴다 */
    @JsonIgnore
    private String fileName;

    /** 사용자가 올린 원래 이름. 화면 표시 · 내려받기용 */
    @JsonIgnore
    private String fileOrgName;

    /** 저장 폴더. 'free/2026/10' 처럼 파일명은 빼고 적는다 */
    @JsonIgnore
    private String filePath;

    /** 확장자 (점 없이 'jpg') */
    @JsonIgnore
    private String fileExt;

    /** 바이트 */
    @JsonIgnore
    private Long fileSize;

    // ---------- 나머지 ----------

    @JsonIgnore
    private Integer viewCount;

    @JsonIgnore
    private String useYn;

    @JsonIgnore
    private LocalDateTime createdAt;

    @JsonIgnore
    private LocalDateTime updatedAt;

    // ---------- 표에 없는 칸. 조회할 때 JOIN 으로 채운다 ----------

    /** 글쓴이 닉네임 (USERS 와 JOIN) */
    @JsonIgnore
    private String nickname;

    /** 살아있는 댓글 수 (COUNT) */
    @JsonIgnore
    private Integer commentCount;

    /** 첨부가 붙어 있는지 */
    public boolean hasFile() {
        return "Y".equals(fileYn);
    }

    /** 지워지지 않은 글인지. 이름을 isAlive 로 하면 Jackson 이 "alive" 칸으로 내보낸다 */
    public boolean alive() {
        return "Y".equals(useYn);
    }

    /** 작성 시각을 응답용 문자열로. 예) "2026-10-04T12:30:00Z" */
    public String createdAtText() {
        return Times.toText(createdAt);
    }
}
