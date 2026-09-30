package com.meokji.api.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 목록 한 줄. 본문은 미리보기용으로 잘라서 내려준다.
 *
 * hasFile 만 준다. 목록에서 파일 정보까지 필요하지 않고, 화면은 이
 * 값으로 제목 옆에 클립 표시를 붙인다.
 *
 * 좋아요 · 댓글 수를 컬럼으로 두지 않고 COUNT() 로 센다. 비정규화
 * 컬럼은 동기화 버그 위험이 성능 이득보다 크다는 판단이다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewListItem {

    private Long id;

    private String title;

    private String content;

    private String foodName;

    private String nickname;

    private int viewCount;

    private int likeCount;

    private int commentCount;

    private String createdAt;

    private boolean hasFile;
}
