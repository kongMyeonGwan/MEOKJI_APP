package com.meokji.api.vo;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 알림 조회 결과 한 줄. (테이블이 아니라 여러 테이블을 JOIN 한 결과를 담는다)
 * 내 글에 달린 댓글 · 좋아요를 가져올 때 쓴다. (UserMapper.xml 의 findCommentNotifications)
 */
@Data
public class NotificationVO {

    private Long id; // 댓글 번호 또는 좋아요 번호
    private Long reviewId; // 어느 글에 달렸는지
    private String reviewTitle; // 그 글의 제목
    private String nickname; // 댓글 · 좋아요를 남긴 사람
    private String content; // 댓글 내용 (좋아요는 null)
    private LocalDateTime createdAt;
}
