package com.meokji.api.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 알림 한 건 (내 글에 달린 댓글 · 좋아요).
// id 는 "COMMENT-12", "LIKE-5" 처럼 종류를 붙인 글자다 (댓글과 좋아요의 번호가 겹칠 수 있어서).
//
// 응답 전용이다. 표 한 행이 아니고, 아래 NotificationVO(여러 표를 JOIN 한
// 결과)를 화면이 쓸 모양으로 바꾼 것이다.
//
// 왜 둘로 나뉘어 있나 — NotificationVO 는 DB 에서 온 모양(댓글 번호 ·
// 글 제목 · 시각)이고, 이쪽은 화면에 그릴 모양(아이콘 · 제목 · 본문 ·
// 누르면 갈 주소)이다. 바꾸는 일은 UserServiceImpl 이 한다.
//
// 알림 표를 따로 두지 않은 이유 — 댓글과 좋아요에 이미 "언제 · 누가 ·
// 어느 글에" 가 다 들어 있다. 알림 표를 만들면 같은 사실이 두 군데
// 쌓이고, 한쪽만 지우는 일이 생긴다. 대신 "읽음 표시" 를 할 수 없다.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationItemVO {

    // "COMMENT-12" 처럼 종류와 번호를 이은 글자.
    //
    // 숫자가 아니라 글자인 이유 — 댓글 12번과 좋아요 12번이 같이 있을 수
    // 있다. 번호만 쓰면 화면의 목록에서 두 줄이 같은 열쇠를 갖는다
    private String id;

    private String kind; // COMMENT 또는 LIKE

    // 화면에 띄울 이모지. 서버가 정하는 이유 — 종류가 늘어날 때
    // 화면을 안 고치고 서버만 고치면 된다
    private String icon;

    /** 한 줄 요약. 예) "홍길동님이 댓글을 남겼어요" */
    private String title;

    /** 그 아래 작은 글씨. 댓글이면 내용, 좋아요면 글 제목 */
    private String body;

    /** 언제. 글자로 나간다 (예: "2026-10-04T12:30:00Z") */
    private String createdAt;

    private String link; // 누르면 갈 화면 주소
    // 서버가 만들어 보낸다. 화면이 kind 를 보고 조립하면,
    // 주소 규칙이 화면과 서버 두 군데에 생긴다
}
