package com.meokji.api.notice.service;

import com.meokji.api.common.Page;
import com.meokji.api.notice.vo.NoticeVO;
import com.meokji.api.notice.vo.NoticeViewVO;
import com.meokji.api.user.vo.UserVO;

/** 공지사항. 실제 코드는 NoticeServiceImpl 에 있다. (TODO: 공지 담당) */
public interface NoticeService {

    // 목록 한 페이지 (고정 공지가 위, 그 다음 최신순)
    Page<NoticeVO> getList(int page, int size);

    // 조회수 +1
    NoticeViewVO increaseView(Long noticeId);

    // 작성
    NoticeVO add(UserVO admin, NoticeVO request);

    // 수정
    NoticeVO update(Long noticeId, NoticeVO request);

    // 삭제 (use_yn 을 'N' 으로)
    void delete(Long noticeId);
}
