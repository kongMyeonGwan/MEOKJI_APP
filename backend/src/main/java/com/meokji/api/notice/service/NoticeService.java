package com.meokji.api.notice.service;

import com.meokji.api.common.Page;
import com.meokji.api.notice.vo.NoticeVO;
import com.meokji.api.user.vo.UserVO;

/** 공지사항. 실제 코드는 NoticeServiceImpl 에 있다. (TODO: 공지 담당) */
public interface NoticeService {

    // 목록 한 페이지 (고정 공지가 위, 그 다음 최신순)
    Page<NoticeVO> getList(int page, int size);

    // 공지 하나 (상세 화면). 없거나 지워진 공지면 404 를 던진다
    NoticeVO getDetail(Long noticeId);

    // 조회수 +1. 올린 뒤의 값을 돌려준다 (자유게시판 · 후기와 같은 모양)
    int increaseView(Long noticeId);

    // 작성
    NoticeVO add(UserVO admin, NoticeVO request);

    // 수정
    NoticeVO update(Long noticeId, NoticeVO request);

    // 삭제 (use_yn 을 'N' 으로)
    void delete(Long noticeId);
}
