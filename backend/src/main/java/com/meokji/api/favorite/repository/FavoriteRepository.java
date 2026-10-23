package com.meokji.api.favorite.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.meokji.api.favorite.entity.FavoriteEntity;

/**
 * RECIPE_LIKES 에 JPA 로 보내는 것.
 *
 * 찜하기 · 해제는 JpaRepository 가 가진 save() 와 엔티티의 변경 감지로 끝난다.
 * 여기 적은 것은 그것만으로 안 되는 것들이다.
 */
public interface FavoriteRepository extends JpaRepository<FavoriteEntity, Long> {

    /**
     * 내 찜 목록. 최근 찜한 순.
     *
     * 화면은 이름으로 카탈로그에서 음식 정보를 찾아 쓰므로 이름만 있으면 된다.
     * 같은 초에 여러 개를 찜하면 created_at 이 같을 수 있어서 id 로 한 번 더
     * 가른다 — 안 그러면 새로 고칠 때마다 순서가 뒤바뀐다.
     */
    @Query("""
            SELECT f.foodName
              FROM FavoriteEntity f
             WHERE f.userId = :userId
               AND f.useYn = 'Y'
             ORDER BY f.createdAt DESC, f.id DESC
            """)
    List<String> findActiveNames(@Param("userId") Long userId);

    /**
     * 그 음식의 찜 행 하나. 해제한 행(use_yn='N')도 찾아야 되살릴 수 있다.
     *
     * 되살리기 규칙을 지키면 한 쌍당 행은 하나지만, 그 전에 쌓인 'N' 행이
     * 여럿일 수 있다. findByUserIdAndFoodName 으로 두면 그때
     * NonUniqueResultException 이 난다.
     */
    Optional<FavoriteEntity> findFirstByUserIdAndFoodNameOrderByIdDesc(Long userId, String foodName);

    /** 찜 개수. 화면의 뱃지에 쓴다 */
    int countByUserIdAndUseYn(Long userId, String useYn);

    /**
     * 찜 전체 비우기 (설정 화면).
     *
     * 행마다 읽어서 고치지 않고 UPDATE 한 방으로 끝낸다. 찜이 백 개여도
     * 쿼리는 하나다. 대신 이 UPDATE 는 영속성 컨텍스트를 거치지 않으므로,
     * 이미 읽어둔 엔티티가 있으면 옛 값을 들고 있게 된다 —
     * clearAutomatically 로 비워서 그 뒤에 읽으면 DB 에서 다시 가져오게 한다.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE FavoriteEntity f
               SET f.useYn = 'N'
             WHERE f.userId = :userId
               AND f.useYn = 'Y'
            """)
    int deactivateAll(@Param("userId") Long userId);
}
