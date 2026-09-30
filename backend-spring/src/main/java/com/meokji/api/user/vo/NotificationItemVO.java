package com.meokji.api.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 알림 한 건 (내 글에 달린 댓글 · 좋아요).
// id 는 "COMMENT-12", "LIKE-5" 처럼 종류를 붙인 글자다 (댓글과 좋아요의 번호가 겹칠 수 있어서).
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationItemVO {

    private String id;
    private String kind; // COMMENT 또는 LIKE
    private String icon;
    private String title;
    private String body;
    private String createdAt;
    private String link; // 누르면 갈 화면 주소
}
