package com.meokji.api.free.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.meokji.api.free.vo.FreePostListVO;
import com.meokji.api.free.vo.FreePostVO;

/**
 * FREE_POSTS 표에 보내는 SQL. 실제 SQL 은 resources/mapper/FreePostMapper.xml 에 있다.
 *
 * 메서드를 하나 만들면 XML 에도 id 가 같은 SQL 을 하나 만들어야 한다.
 * 이름이 다르면 서버가 뜰 때가 아니라 그 메서드를 처음 부를 때 터진다.
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

    /** 목록 한 페이지. 최신순. 본문은 앞 200자만 잘라서 가져온다 */
    List<FreePostListVO> findPage(
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size);

    /** 글 한 건 (지운 글도 나온다. use_yn 확인은 부르는 쪽이 한다) */
    FreePostVO findById(@Param("id") Long id);

    /** 글쓰기. id · 날짜는 DB 가 채운다 */
    void insert(FreePostVO post);

    /** 제목 · 내용 수정. updated_at 도 같이 올린다 */
    void update(FreePostVO post);

    /** 삭제. 행을 지우지 않고 use_yn='N' */
    void deactivate(@Param("id") Long id);

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
