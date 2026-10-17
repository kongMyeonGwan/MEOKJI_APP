package com.meokji.api.notice.service;

<<<<<<< HEAD
import com.meokji.api.common.Page;
import com.meokji.api.notice.vo.NoticeVO;
import com.meokji.api.user.vo.UserVO;

/**
 * 공지사항. 실제 코드는 NoticeServiceImpl 에 있다. (TODO: 공지 담당)
 *
 * <p>인터페이스는 완성돼 있고 구현만 비어 있다. 메서드 이름 · 인자 · 돌려주는
 * 모양이 여기서 정해지므로, 컨트롤러는 이미 이대로 부르고 있다.
 * 구현을 채울 때 이 파일은 안 고쳐도 된다.
 *
 * <p>목록 · 상세 · 조회수에는 UserVO 가 없다. 누구나 보는 글이라 "누가 보는지"
 * 가 판단에 들어가지 않는다. add 만 UserVO 를 받는데, 작성자를 남겨야 해서다.
 * update · delete 는 작성자를 바꾸지 않으므로 받지 않는다.
 */
public interface NoticeService {

    /**
     * 목록 한 페이지 (고정 공지가 위, 그 다음 최신순)
     *
     * 고정 공지를 위로 올리는 정렬은 SQL 이 한다(NoticeMapper.xml).
     * 자바에서 정렬하면 한 페이지 안에서만 올라가므로, 2페이지에 있는
     * 고정 공지는 여전히 2페이지에 남는다.
     */
    Page<NoticeVO> getList(int page, int size);

    /** 공지 하나 (상세 화면). 없거나 지워진 공지면 404 를 던진다 */
    NoticeVO getDetail(Long noticeId);

    /**
     * 조회수 +1. 올린 뒤의 값을 돌려준다 (자유게시판 · 후기와 같은 모양)
     *
     * 세 기능의 모양을 맞춘 이유 — 화면에서 조회수를 올리는 코드가 같아진다.
     * 한 곳만 "올리고 아무것도 안 주는" 모양이면 그 화면만 다르게 짜야 한다.
     */
    int increaseView(Long noticeId);

    /**
     * 작성
     *
     * @param admin 작성자로 남길 관리자. 컨트롤러가 getAdminUser 로 확인한 값이다
     */
    NoticeVO add(UserVO admin, NoticeVO request);

    /** 수정 */
    NoticeVO update(Long noticeId, NoticeVO request);

    /** 삭제 (use_yn 을 'N' 으로) */
    // 행을 지우지 않는 이유 — 지운 공지를 가리키는 자리는 없지만,
    // 다른 기능과 삭제 방식을 맞춰 두면 "어디는 지우고 어디는 끄는지" 를
    // 기억하지 않아도 된다
    void delete(Long noticeId);
=======
/** 공지사항. 실제 코드는 NoticeServiceImpl 에 있다. */
public interface NoticeService {
>>>>>>> 137ab2e5d83abad60912fb1f0b6b8a1315e728eb
}
