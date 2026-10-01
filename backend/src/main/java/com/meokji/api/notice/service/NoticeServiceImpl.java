package com.meokji.api.notice.service;

import org.springframework.stereotype.Service;

import com.meokji.api.common.ApiException;
import com.meokji.api.common.Page;
import com.meokji.api.notice.vo.NoticeVO;
import com.meokji.api.notice.vo.NoticeViewVO;
import com.meokji.api.user.vo.UserVO;

/**
 * 공지사항.
 *
 * TODO(공지 담당): 아래 메서드들을 채운다. 지금은 전부 501(미구현) 에러를 던진다.
 *   1. dao/NoticeDAO.java 에 메서드를 만든다      (예: List<NoticeVO> findPage(...))
 *   2. resources/mapper/NoticeMapper.xml 에 SQL 을 쓴다
 *   3. 여기서 @Autowired private NoticeDAO noticeDAO; 로 받아서 부른다
 *   4. NoticeVO → NoticeResponse 로 옮겨 담아 돌려준다
 * 비슷한 예시: BanServiceImpl (목록 · 추가 · 삭제), HistoryServiceImpl (페이지 목록)
 */
@Service
public class NoticeServiceImpl implements NoticeService {

    private static final String TODO = "아직 만들지 않은 기능입니다. (공지사항 담당자 작업 예정)";

    @Override
    public Page<NoticeVO> getList(int page, int size) {
        // 예시)
        // int total = noticeDAO.count();
        // List<NoticeVO> items = new ArrayList<>();
        // for (NoticeVO notice : noticeDAO.findPage((page - 1) * size, size)) {
        //     items.add(notice);
        // }
        // int totalPages = 0;
        // if (size > 0) {
        //     totalPages = (total + size - 1) / size;
        // }
        //
        // Page<NoticeVO> result = new Page<>();
        // result.setItems(items);
        // result.setTotal(total);
        // result.setPage(page);
        // result.setSize(size);
        // result.setTotalPages(totalPages);
        // return result;
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public NoticeViewVO increaseView(Long noticeId) {
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public NoticeVO add(UserVO admin, NoticeVO request) {
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public NoticeVO update(Long noticeId, NoticeVO request) {
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public void delete(Long noticeId) {
        throw ApiException.notImplemented(TODO);
    }
}
