package com.meokji.api.service;

import org.springframework.stereotype.Service;

import com.meokji.api.common.Page;
import com.meokji.api.dto.notice.NoticeResponse;
import com.meokji.api.security.LoginUser;
import com.meokji.api.vo.Notice;

/**
 * 공지사항 로직 — <b>아직 만들지 않았다. 담당자가 채운다.</b>
 *
 * <h2>여기에 만들 메서드</h2>
 *
 * <pre>
 *   Page&lt;NoticeResponse&gt; list(page, size)        목록 (고정 공지가 위, 그 다음 최신순)
 *   int                  increaseView(noticeId)  조회수 +1
 *   NoticeResponse       create(admin, request)  작성 (관리자만)
 *   NoticeResponse       update(noticeId, request)
 *   void                 delete(noticeId)        use_yn='N'
 * </pre>
 *
 * <h2>쓸 수 있는 것</h2>
 *
 * <pre>
 *   표      notices  (db/01_schema.sql)
 *   도메인   {@link Notice}
 *   DTO     com.meokji.api.notice.dto
 *   페이징   {@link com.meokji.api.common.Page}
 * </pre>
 *
 * <h2>관리자 판단은 컨트롤러가 한다</h2>
 *
 * <p>{@code @LoginUser(admin = true)} 가 {@code users.user_level = 1} 을 확인한다.
 * 여기서 다시 볼 필요가 없다.
 *
 * <h2>주의</h2>
 *
 * <ul>
 *   <li>목록 정렬이 <b>고정 공지 먼저, 그 안에서 최신순</b>이다.
 *       {@code ORDER BY pinned_yn DESC, created_at DESC} — 'Y' > 'N' 이라 DESC 다.
 *   <li>고정 공지는 1페이지에만 보인다. 2페이지에도 끌고 오면 같은 공지가 두 번
 *       나온다.
 *   <li>본문 상한이 글자 수가 아니라 <b>4000바이트</b>다. 컬럼이
 *       {@code VARCHAR2(4000)} 이고 이 DB 의 길이 단위가 BYTE 다. 글자 수로 검사하면
 *       통과했는데 INSERT 에서 잘린다.
 *   <li>작성자를 본문으로 받지 않는다. 토큰의 주인이 작성자다.
 * </ul>
 */
@Service
public class NoticeService {

    // 담당자가 채운다.
}
