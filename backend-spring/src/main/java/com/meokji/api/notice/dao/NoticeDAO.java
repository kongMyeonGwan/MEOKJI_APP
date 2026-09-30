package com.meokji.api.notice.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.meokji.api.notice.vo.NoticeVO;

/**
 * NOTICES 테이블 (공지사항). SQL 은 resources/mapper/NoticeMapper.xml 에 있다.
 *
 * TODO(공지 담당): 필요한 메서드를 더 만든다. (insert, update, delete, increaseView ...)
 * 메서드를 하나 만들면 NoticeMapper.xml 에도 id 가 같은 SQL 을 하나 만들어야 한다.
 */
@Mapper
public interface NoticeDAO {

    // 지운 것을 뺀 공지 개수
    int count();

    // 공지 한 페이지. 고정 공지가 위, 그 다음 최신순
    //   offset : 앞에서 건너뛸 개수,  size : 가져올 개수
    List<NoticeVO> findPage(@Param("offset") int offset, @Param("size") int size);

    // 공지 한 건
    NoticeVO findById(@Param("id") Long id);
}
