package com.meokji.api.service;

import com.meokji.api.common.Page;
import com.meokji.api.dto.notice.NoticeCreateRequest;
import com.meokji.api.dto.notice.NoticeResponse;
import com.meokji.api.dto.notice.NoticeUpdateRequest;
import com.meokji.api.dto.notice.NoticeViewResponse;
import com.meokji.api.vo.UserVO;

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
