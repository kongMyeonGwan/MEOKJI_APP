package com.meokji.api.free.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meokji.api.free.entity.FreePostEntity;

/**
 * FREE_POSTS 에 JPA 로 보내는 것. 글쓰기 · 수정 · 삭제만 여기를 지난다.
 *
 * JpaRepository 를 물려받으면 save() · findById() 같은 것이 이미 들어 있어서
 * 따로 적을 것이 없다. SQL 도 안 쓴다 — 메서드 이름을 보고 JPA 가 만든다.
 *
 * 목록 · 검색 · 페이징은 여기 두지 않는다. 그 SQL 은 JOIN 과 COUNT 와
 * DBMS_LOB.SUBSTR 이 섞여 있어서 메서드 이름으로 만들 수 있는 모양이 아니다.
 * FreePostDAO 와 FreePostMapper.xml 에 그대로 있다.
 */
public interface FreePostRepository extends JpaRepository<FreePostEntity, Long> {

    /**
     * 살아있는 글 하나. 지운 글(use_yn='N')은 안 나온다
     *
     * 이름이 곧 조건이다 — findBy + Id + And + UseYn 을 보고 JPA 가
     * WHERE id = ? AND use_yn = ? 를 만든다. 그래서 이름을 잘못 적으면
     * 서버가 뜰 때 "그런 칸이 없다" 로 막힌다.
     *
     * Optional 로 받는 이유 — JPA 가 정한 모양이다. 못 찾았을 때 null 이
     * 아니라 "빈 Optional" 이 와서, 받는 쪽이 null 확인을 빠뜨릴 수 없다.
     * (프로젝트의 다른 자리에서는 Optional 을 쓰지 않는다 — MyBatis 는
     *  못 찾으면 그냥 null 을 준다)
     *
     * useYn 을 인자로 받는 모양인 이유 — 메서드 이름으로 값을 고정할 수
     * 없다. 부르는 쪽이 늘 "Y" 를 넘긴다.
     */
    Optional<FreePostEntity> findByIdAndUseYn(Long id, String useYn);
}
