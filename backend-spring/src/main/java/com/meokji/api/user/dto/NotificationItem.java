package com.meokji.api.user.dto;

/**
 * 알림 한 건.
 *
 * <p><b>알림 표는 없다.</b> 내 글에 달린 댓글과 좋아요를 조회할 때 모아서 만든다.
 * 표를 함부로 늘리지 않는다는 원칙에 따른 것이고, 읽음 표시 같은 기능이 필요해지면
 * 그때 표를 만든다.
 *
 * <p>{@code id} 가 숫자가 아니라 {@code "COMMENT-12"} 같은 문자열인 이유도 여기
 * 있다 - 서로 다른 표의 id 를 한 목록에 섞기 때문에 종류를 붙여야 유일해진다.
 */
public record NotificationItem(
        String id,
        String kind,
        String icon,
        String title,
        String body,
        String createdAt,
        String link) {
}
