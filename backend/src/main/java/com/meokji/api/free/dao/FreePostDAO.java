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
    // keyword 가 null 이면 XML 의 if 가 조건을 빼서 전체를 센다
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

    /**
     * 조회수 +1
     *
     * SQL 안에서 올린다 (view_count = view_count + 1). 읽어서 +1 해
     * UPDATE 하면, 두 사람이 동시에 열었을 때 둘 다 같은 값을 읽어
     * 조회수가 하나만 오른다.
     */
    void increaseView(@Param("id") Long id);

    /**
     * 조회수 읽기 (올린 뒤 값을 돌려주기 위해)
     *
     * increaseView 가 숫자를 돌려주지 않아서 따로 읽는다. UPDATE 는
     * "몇 행이 바뀌었는지" 만 알려주고 바뀐 값은 알려주지 않는다.
     *
     * 글 전체(findById)를 다시 읽지 않는 이유 — 조회수 하나만 필요하다.
     * 본문 CLOB 과 닉네임 JOIN 까지 끌어올 이유가 없다.
     */
    int findViewCount(@Param("id") Long id);

    /**
     * 첨부파일 정보 저장. file_yn 과 FILE_* 을 한 번에 바꾼다.
     *
     * 따로 바꾸면 ck_free_posts_file_set 제약에 걸린다 — 그 제약이
     * "file_yn='Y' 인데 경로가 비어 있는 행" 을 막고 있다.
     */
    void setFile(FreePostVO post);

    /**
     * 첨부파일 떼기. file_yn='N' 으로 바꾸고 FILE_* 을 전부 비운다
     *
     * 디스크의 파일을 지우는 일은 여기서 하지 않는다. 서비스가
     * FileStore 로 따로 지운다 - DB 와 디스크는 같은 트랜잭션에
     * 묶이지 않으므로 순서를 서비스가 정해야 한다.
     */
    void clearFile(@Param("id") Long id);
}
