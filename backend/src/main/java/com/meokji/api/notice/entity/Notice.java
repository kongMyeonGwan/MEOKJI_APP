package com.meokji.api.notice.entity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.hibernate.annotations.Generated;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 글쓴이(관리자). 한 번 정해지면 안 바뀐다 */
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, length = 600)
    private String content;

    /** 읽기만 한다. 올리는 것은 Repository 의 increaseView 다 */
    @Generated
    @Column(name = "view_count", insertable = false, updatable = false)
    private Integer viewCount;

    /** 'Y' = 목록 맨 위에 고정 */
    @Column(name = "pinned_yn", nullable = false, length = 1)
    private String pinnedYn;

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn;

    /** 읽기만 한다. DB 기본값이 넣는다 */
    @Generated
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 새 공지. id · 조회수 · 작성 시각은 DB 가 채운다 */
    public static Notice create(Long userId, String title, String content, boolean pinned) {
        Notice notice = new Notice();
        notice.userId = userId;
        notice.title = title;
        notice.content = content;
        notice.pinnedYn = pinned ? "Y" : "N";
        notice.useYn = "Y";
        notice.updatedAt = nowUtc();
        return notice;
    }

    /**
     * 제목 · 내용 · 고정 여부 고치기.
     */
    public void change(String title, String content, boolean pinned) {
        this.title = title;
        this.content = content;
        this.pinnedYn = pinned ? "Y" : "N";
        this.updatedAt = nowUtc();
    }

    /**
     * 삭제. 행을 지우지 않고 use_yn='N' 으로 바꾼다.
     *
     * delete() 를 쓰면 행이 사라져서 나중에 이 글을 가리키는 것이 있을 때
     * 가리킬 곳을 잃는다.
     */
    public void deactivate() {
        this.useYn = "N";
        this.updatedAt = nowUtc();
    }

    /** 상단 고정인지 */
    public boolean isPinned() {
        return "Y".equals(pinnedYn);
    }

    /** DB 가 UTC 로 적으므로 여기서 넣는 값도 UTC 여야 한다 */
    private static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
