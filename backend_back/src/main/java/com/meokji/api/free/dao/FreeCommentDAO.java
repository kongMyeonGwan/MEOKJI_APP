package com.meokji.api.free.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.meokji.api.free.vo.FreePostCommentVO;

/**
 * 자유게시판 댓글 SQL. 실제 SQL 은 resources/mapper/FreeCommentMapper.xml 에 있다.
 *
 * 표는 POST_COMMENTS 다. 레시피리뷰 댓글과 한 표를 같이 쓰고 BOARD_TYPE 으로
 * 가른다 ('F' = 자유게시판). 'F' 는 매퍼 XML 에 박아두었으므로 이 인터페이스를
 * 쓰는 쪽은 신경 쓸 것이 없다 — 여기 메서드는 전부 자유게시판 댓글만 본다.
 *
 * 글(FreePostDAO)과 표가 다르니 DAO 도 나눴다. 한 DAO 에 두 표의 SQL 을
 * 섞으면 어느 표를 건드리는 메서드인지 이름만 보고 알기 어려워진다.
 */
@Mapper
public interface FreeCommentDAO {

    /**
     * 글 하나의 댓글 전부. 오래된 순 (대화 순서로 읽힌다)
     *
     * 페이징이 없다. 댓글은 많아도 수십 개라 한 번에 다 보내고, 화면도
     * 글 아래에 전부 펼쳐 보여준다.
     *
     * 지운 댓글(use_yn='N')은 빠진다. 닉네임은 USERS 를 JOIN 해서 같이 온다.
     */
    List<FreePostCommentVO> findByPost(@Param("postId") Long postId);

    /**
     * 댓글 한 건 (지운 댓글도 나온다)
     *
     * 지운 것까지 가져오는 이유 — 수정 · 삭제할 때 "이미 지운 댓글" 과
     * "없는 번호" 를 구별해야 한다. SQL 에 use_yn 조건을 넣으면 둘 다
     * null 로 돌아와 구별할 수 없다. 판단은 서비스가 한다.
     */
    FreePostCommentVO findById(@Param("id") Long id);

    /**
     * 댓글 쓰기
     *
     * VO 통째로 받는다. 넣는 칸이 셋(postId · userId · content)이고
     * 넣고 나면 comment.id 에 시퀀스가 준 번호가 채워진다(XML 의 selectKey).
     * board_type='F' 는 XML 이 박아 넣는다.
     */
    void insert(FreePostCommentVO comment);

    /**
     * 댓글 내용 수정
     *
     * 고치는 칸은 content 와 updated_at 뿐이다. 쓴 사람 · 글 번호는
     * 바뀔 수 없는 값이라 XML 의 SET 에 들어 있지 않다.
     */
    void update(FreePostCommentVO comment);

    /**
     * 댓글 삭제. 행을 지우지 않고 use_yn='N'
     *
     * 행째로 지우지 않는 이유 — 글 · 후기 · 재료 등 이 프로젝트의 모든
     * 삭제가 같은 방식이다. 어디는 지우고 어디는 끄면 "이 표는 어느
     * 쪽이었지" 를 매번 확인해야 한다.
     */
    void deactivate(@Param("id") Long id);
}
