package com.meokji.api.vo;

import java.time.LocalDateTime;

/**
 * NOTICES 표 한 행. 공지사항.
 *
 * <p>파일에 두지 않고 표로 만든 이유는 <b>운영하면서 계속 새로 쓰는 값</b>이기
 * 때문이다. 파일에 두면 공지 하나 올릴 때마다 서버를 다시 배포해야 한다.
 *
 * <p>{@code pinnedYn} 이 'Y'/'N' 이다. 응답에서는 boolean 으로 바꿔서 준다.
 */
public class Notice {

    private Long id;

    /** 작성자(관리자). 화면에 이름을 보여주지는 않는다 */
    private Long userId;

    private String title;

    /** VARCHAR2(4000). 길이 단위가 BYTE 라 한글로는 약 1333자다 */
    private String content;

    private Integer viewCount;

    /** 'Y' = 상단 고정. 목록에서 먼저 나온다 */
    private String pinnedYn;

    private String useYn;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public String getPinnedYn() {
        return pinnedYn;
    }

    public void setPinnedYn(String pinnedYn) {
        this.pinnedYn = pinnedYn;
    }

    public String getUseYn() {
        return useYn;
    }

    public void setUseYn(String useYn) {
        this.useYn = useYn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
