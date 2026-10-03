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

    /** 글 하나의 댓글 전부. 오래된 순 (대화 순서로 읽힌다) */
    List<FreePostCommentVO> findByPost(@Param("postId") Long postId);

    /** 댓글 한 건 (지운 댓글도 나온다) */
    FreePostCommentVO findById(@Param("id") Long id);

    /** 댓글 쓰기 */
    void insert(FreePostCommentVO comment);

    /** 댓글 내용 수정 */
    void update(FreePostCommentVO comment);

    /** 댓글 삭제. 행을 지우지 않고 use_yn='N' */
    void deactivate(@Param("id") Long id);
}
