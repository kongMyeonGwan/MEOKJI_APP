package com.meokji.api.user.vo;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 알림 조회 결과 한 줄. (테이블이 아니라 여러 테이블을 JOIN 한 결과를 담는다)
 * 내 글에 달린 댓글 · 좋아요를 가져올 때 쓴다. (UserMapper.xml 의 findCommentNotifications)
 *
 * <p>화면으로 나가지 않는다. 서비스가 이것을 받아 NotificationItemVO(화면에
 * 그릴 모양)로 바꾼다. 그래서 @JsonIgnore 도, 검증도 없다.
 *
 * <p>댓글과 좋아요가 같은 VO 를 쓴다. 칸이 거의 같고(누가 · 어느 글에 ·
 * 언제), 다른 것은 content 하나뿐이다 - 좋아요에는 내용이 없다.
 * 둘로 나누면 서비스가 두 모양을 각각 다뤄야 한다.
 */
@Data
public class NotificationVO {

    private Long id; // 댓글 번호 또는 좋아요 번호
    // 둘의 번호가 겹칠 수 있다. 그래서 NotificationItemVO 로 바꿀 때
    // "COMMENT-" · "LIKE-" 를 앞에 붙인다

    private Long reviewId; // 어느 글에 달렸는지
    // 누르면 갈 주소(link)를 만드는 데 쓴다

    private String reviewTitle; // 그 글의 제목
    // 글 제목을 같이 가져오는 이유 — 알림에 "어느 글에 달렸는지" 가
    // 안 보이면 무슨 알림인지 알 수 없다. 번호만으로는 모른다

    private String nickname; // 댓글 · 좋아요를 남긴 사람

    private String content; // 댓글 내용 (좋아요는 null)
    // 좋아요에는 남길 말이 없다. null 인 것이 정상이라
    // 서비스가 그때 글 제목을 본문 자리에 넣는다

    private LocalDateTime createdAt;
    // UTC 다. 화면에 나갈 때 Times.toText 로 글자가 된다
}
