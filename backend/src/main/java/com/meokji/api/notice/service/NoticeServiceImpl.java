package com.meokji.api.notice.service;

import org.springframework.stereotype.Service;

import com.meokji.api.common.ApiException;
import com.meokji.api.common.Page;
import com.meokji.api.notice.vo.NoticeVO;
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
 *
 * <p>왜 빈 메서드가 아니라 501 을 던지나
 *
 * <p>아무것도 안 하고 null 이나 빈 목록을 돌려주면, 화면에서는 "공지가 없는
 * 상태" 로 보인다. 아직 안 만들었다는 사실이 드러나지 않아서, 다 만들었다고
 * 착각한 채로 넘어갈 수 있다. 501 이면 부르는 즉시 안 만들어진 것이 보인다.
 *
 * <p>쓰기 쪽(add · update · delete)에 {@code @Transactional} 이 없는 것도
 * 아직 DB 를 건드리지 않기 때문이다. 채울 때 같이 붙여야 한다 — 다른
 * 서비스들은 쓰기 메서드마다 붙어 있다.
 *
 * <p>구현할 때 참고할 점
 *   - 목록의 offset · total_pages 는 직접 계산하지 않는다 (common/Paging)
 *   - 상세는 use_yn='N' 도 404 로 본다. 지운 공지가 주소로는 보이면 안 된다
 *   - 조회수는 SQL 안에서 올린다 (view_count = view_count + 1).
 *     읽어서 +1 해 UPDATE 하면 동시에 본 사람 수만큼 빠진다
 */
@Service
public class NoticeServiceImpl implements NoticeService {

    // 여섯 메서드가 같은 문장을 쓴다. 각자 적으면 한쪽만 고치게 된다
    private static final String TODO = "아직 만들지 않은 기능입니다. (공지사항 담당자 작업 예정)";

    @Override
    public Page<NoticeVO> getList(int page, int size) {
        // 예시) offset 과 total_pages 는 직접 계산하지 않는다. common/Paging.java 가 한다.
        // Paging paging = new Paging(page, size);
        // paging.setTotal(noticeDAO.count());
        //
        // List<NoticeVO> items = new ArrayList<>();
        // for (NoticeVO notice : noticeDAO.findPage(paging.getOffset(), paging.getSize())) {
        //     items.add(notice);
        // }
        //
        // Page<NoticeVO> result = new Page<>();
        // result.fill(paging, items);
        // return result;
        //
        // count 와 findPage 는 둘 다 use_yn='Y' 만 봐야 한다. 한쪽만 걸면
        // "3페이지가 있다는데 열면 비어 있는" 일이 생긴다
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public NoticeVO getDetail(Long noticeId) {
        // 예시) 없거나 use_yn='N' 이면 404 다. 조회수는 여기서 올리지 않는다 (increaseView 가 한다)
        // NoticeVO notice = noticeDAO.findById(noticeId);
        // if (notice == null || !"Y".equals(notice.getUseYn())) {
        //     throw ApiException.notFound("공지를 찾을 수 없습니다.");
        // }
        // return notice;
        //
        // findById 의 SQL 에 use_yn 조건을 넣지 않고 자바에서 보는 이유는
        // BanDAO.findById 주석과 같다 - "없는 번호" 와 "지운 글" 을 구별할 수 있게 둔다
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public int increaseView(Long noticeId) {
        // 채울 때 — UPDATE 로 올리고 그 뒤의 값을 SELECT 해서 돌려준다.
        // 올리기 전 값에 +1 해서 돌려주면 동시에 본 사람 수만큼 어긋난다
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public NoticeVO add(UserVO admin, NoticeVO request) {
        // 채울 때 — 받은 request 를 그대로 insert 하지 말고 새 VO 에 옮겨 담는다.
        // 화면이 id · viewCount · useYn 을 섞어 보내도 들어가지 않게 하려는 것이다.
        // userId 는 request 가 아니라 admin.getId() 를 쓴다
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public NoticeVO update(Long noticeId, NoticeVO request) {
        // 채울 때 — 고치기 전에 그 공지가 있는지 먼저 본다. 없는 번호에
        // UPDATE 를 보내면 0건이 바뀌고 에러도 안 나서, 화면은 성공으로 받는다
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public void delete(Long noticeId) {
        // 채울 때 — DELETE 가 아니라 use_yn='N' 으로 바꾼다 (다른 기능과 같은 방식)
        throw ApiException.notImplemented(TODO);
    }
}
