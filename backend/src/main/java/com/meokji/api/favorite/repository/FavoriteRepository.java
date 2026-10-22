package com.meokji.api.favorite.repository;

import com.meokji.api.favorite.dto.FavoriteListResponse;
import com.meokji.api.favorite.entity.FavoriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<FavoriteEntity, Long> {

    /** 내 찜 목록. 최신순. 응답 모양으로 바로 받는다 */
    @Query("""
            SELECT new com.meokji.api.favorite.dto.FavoriteListResponse(f.id, f.foodName, f.createdAt)
              FROM FavoriteEntity f
             WHERE f.userId = :userId
               AND f.useYn = 'Y'
             ORDER BY f.createdAt DESC
            """)
    List<FavoriteListResponse> findList(@Param("userId") Long userId);
}
