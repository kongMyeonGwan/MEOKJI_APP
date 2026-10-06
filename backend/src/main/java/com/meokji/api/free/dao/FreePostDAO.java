package com.meokji.api.free.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.meokji.api.free.vo.FreePostVO;

/**
 * FREE_POSTS 표에 보내는 SQL. 실제 SQL 은 resources/mapper/FreePostMapper.xml 에 있다.
 *
 * 메서드를 하나 만들면 XML 에도 id 가 같은 SQL 을 하나 만들어야 한다.
 * 이름이 다르면 서버가 뜰 때가 아니라 그 메서드를 처음 부를 때 터진다.
 *
 * 글쓰기 · 글 수정 · 글 삭제는 여기 없다. JPA 가 맡는다
 * (free/repository/FreePostRepository + free/entity/FreePostEntity).
 * 한 표에 글을 쓰는 길을 둘로 두면, 어느 쪽으로 들어간 값인지 모르게 된다.
 *
 * 여기 남은 것은 JPA 로 옮기지 않기로 한 것들이다 - 목록 · 검색 · 페이징 ·
 * 조회수 · 첨부. JOIN 과 COUNT 와 DBMS_LOB.SUBSTR 이 섞여 있어서
 * 메서드 이름만으로 만들 수 있는 모양이 아니다.
 */
@Mapper
public interface FreePostDAO {

    /**
     * 조건에 맞는 전체 건수.
     *
     * 목록과 같은 조건을 봐야 한다. 한쪽에만 검색어를 붙이면
     * "3페이지가 있다는데 열면 비어 있는" 일이 생긴다.
     */
    int count(@Param("keyword") String keyword);

    /**
     * 목록 한 페이지. 최신순. 본문은 앞 200자만 잘라서 가져온다.
     *
     * 상세와 같은 FreePostVO 를 쓰되 고르는 칸이 다르다 — 댓글 · 첨부는
     * 안 담는다. 목록에 없어도 되는 값이라 XML 의 SELECT 에서 뺐다.
     */
    List<FreePostVO> findPage(
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size);

    /**
     * 글 한 건 (지운 글도 나온다. use_yn 확인은 부르는 쪽이 한다).
     *
     * JPA 에도 같은 행을 읽는 findByIdAndUseYn 이 있지만 쓰임이 다르다.
     * 이쪽은 닉네임을 JOIN 해서 화면으로 나갈 모양을 만들고,
     * 저쪽은 JPA 가 들고 있다가 고친 것을 알아채는 대상이다.
     */
    FreePostVO findById(@Param("id") Long id);

    /** 조회수 +1 */
    void increaseView(@Param("id") Long id);

    /** 조회수 읽기 (올린 뒤 값을 돌려주기 위해) */
    int findViewCount(@Param("id") Long id);

    /**
     * 첨부파일 정보 저장. file_yn 과 FILE_* 을 한 번에 바꾼다.
     *
     * 따로 바꾸면 ck_free_posts_file_set 제약에 걸린다 — 그 제약이
     * "file_yn='Y' 인데 경로가 비어 있는 행" 을 막고 있다.
     */
    void setFile(FreePostVO post);

    /** 첨부파일 떼기. file_yn='N' 으로 바꾸고 FILE_* 을 전부 비운다 */
    void clearFile(@Param("id") Long id);
}
