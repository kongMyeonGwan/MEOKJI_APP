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

    /** 살아있는 글 하나. 지운 글(use_yn='N')은 안 나온다 */
    Optional<FreePostEntity> findByIdAndUseYn(Long id, String useYn);
}
