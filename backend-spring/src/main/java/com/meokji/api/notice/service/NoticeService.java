package com.meokji.api.notice.service;

import com.meokji.api.common.Page;
import com.meokji.api.notice.dto.NoticeCreateRequest;
import com.meokji.api.notice.dto.NoticeResponse;
import com.meokji.api.notice.dto.NoticeUpdateRequest;
import com.meokji.api.notice.dto.NoticeViewResponse;
import com.meokji.api.user.vo.UserVO;

/** 공지사항. 실제 코드는 NoticeServiceImpl 에 있다. (TODO: 공지 담당) */
public interface NoticeService {

    // 목록 한 페이지 (고정 공지가 위, 그 다음 최신순)
    Page<NoticeResponse> getList(int page, int size);

    // 조회수 +1
    NoticeViewResponse increaseView(Long noticeId);

    // 작성
    NoticeResponse add(UserVO admin, NoticeCreateRequest request);

    // 수정
    NoticeResponse update(Long noticeId, NoticeUpdateRequest request);

    // 삭제 (use_yn 을 'N' 으로)
    void delete(Long noticeId);
}
