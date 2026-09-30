package com.meokji.api.notice.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * NOTICES 표 한 행. 공지사항.
 *
 * 파일에 두지 않고 표로 만든 이유는 운영하면서 계속 새로 쓰는 값이기
 * 때문이다. 파일에 두면 공지 하나 올릴 때마다 서버를 다시 배포해야 한다.
 *
 * pinnedYn 이 'Y'/'N' 이다. 응답에서는 boolean 으로 바꿔서 준다.
 */
@Data
public class NoticeVO {

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
}
