package com.meokji.api.notice.dao;

import org.apache.ibatis.annotations.Mapper;

<<<<<<< HEAD
/**
 * NOTICES 테이블 (공지사항). SQL 은 resources/mapper/NoticeMapper.xml 에 있다.
 *
 * TODO(공지 담당): 필요한 메서드를 더 만든다. (insert, update, delete, increaseView ...)
 * 메서드를 하나 만들면 NoticeMapper.xml 에도 id 가 같은 SQL 을 하나 만들어야 한다.
 *
 * <p>이름이 어긋나면 서버가 뜰 때 "찾을 수 없다" 로 막힌다. 실행 중에 터지는
 * 것이 아니라 시작 자체가 안 되므로, 빠뜨린 것은 바로 드러난다.
 *
 * <p>지금 있는 셋(count · findPage · findById)은 읽기뿐이다. 서비스가 아직
 * 501 을 던지고 있어서 쓰이지는 않지만, 목록과 상세에 무엇이 필요한지는
 * 정해 둔 것이다.
 */
@Mapper
public interface NoticeDAO {

    // 지운 것을 뺀 공지 개수
    //
    // 전체 페이지 수를 계산하려면 이 값이 필요하다 (common/Paging).
    // findPage 와 조건이 같아야 한다 - 한쪽만 use_yn 을 보면 숫자가 어긋난다
    int count();

    // 공지 한 페이지. 고정 공지가 위, 그 다음 최신순
    //   offset : 앞에서 건너뛸 개수,  size : 가져올 개수
    //
    // 정렬을 SQL 에서 하는 이유 — 자바에서 정렬하면 한 페이지 안에서만
    // 고정 공지가 올라간다. 2페이지에 있는 고정 공지는 2페이지에 남는다
    //
    // offset 을 자바에서 자르지 않고 SQL 에 넘기는 이유 — 공지가 500건이면
    // 20건을 보려고 500건을 서버 메모리에 올리게 된다
    List<NoticeVO> findPage(@Param("offset") int offset, @Param("size") int size);

    // 공지 한 건
    //
    // SQL 에 use_yn 조건을 넣지 않는다. "없는 번호" 와 "지운 공지" 를
    // 구별할 수 있게 두고, 판단은 서비스가 한다 (BanDAO.findById 주석과 같은 이유)
    NoticeVO findById(@Param("id") Long id);
=======
/** NOTICES 테이블 (공지사항). */
@Mapper
public interface NoticeDAO {
>>>>>>> 137ab2e5d83abad60912fb1f0b6b8a1315e728eb
}
