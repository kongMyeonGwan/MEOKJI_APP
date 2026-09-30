package com.meokji.api.notice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 공지 한 건.
 *
 * pinned 는 DB 의 pinned_yn('Y'/'N')을 화면이 쓰기 쉬운
 * boolean 으로 바꿔서 준다. 목록은 고정 공지가 먼저, 그 안에서는 최신순이다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticeResponse {

    private Long id;

    private String title;

    private String content;

    private int viewCount;

    private boolean pinned;

    private String createdAt;
}
