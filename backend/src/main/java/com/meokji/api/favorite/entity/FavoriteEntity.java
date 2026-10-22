package com.meokji.api.favorite.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * RECIPE_LIKES 표 한 행 — 찜 하나. JPA 가 영속성을 관리하는 대상이다.
 *
 * 표 이름은 예전 그대로 RECIPE_LIKES 다. 후기 글 좋아요(REVIEW_LIKES)와 다른
 * 기능이라 자바 쪽은 favorite 으로 부르고, 이름이 갈리는 자리는 여기 하나다.
 *
 * 회원을 @ManyToOne 이 아니라 userId 숫자로 둔다
 *
 *   USERS 는 MyBatis 가 관리하는 표라 User 엔티티가 없다. FreePostEntity 와
 *   같은 이유이고 같은 방식이다.
 *
 * 해제해도 행을 지우지 않는다 (use_yn='N')
 *
 *   uq_recipe_likes 가 함수 기반 UNIQUE 라 use_yn='Y' 인 행만 중복을 막는다.
 *   그래서 껐던 행을 놔둔 채 같은 음식을 다시 찜하면 INSERT 가 막히지 않고
 *   통과해서, 죽은 행이 계속 쌓인다. 켤 때는 새로 넣지 말고 그 행을
 *   되살려야 한다 — turnOn() 이 그 일을 한다.
 *
 * use_yn 을 create() 에서 직접 넣는 이유
 *
 *   표에 DEFAULT 'Y' 가 있지만 그 기본값은 INSERT 문에서 그 칸을 아예 뺐을
 *   때만 먹는다. JPA 는 insertable 인 칸을 전부 적어 보내므로, 여기서 안
 *   넣으면 use_yn = NULL 이 실려 ORA-01400 이 난다.
 */
@Entity
@Table(name = "recipe_likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FavoriteEntity {

    /** RECIPE_LIKES_SEQ 시퀀스로 채운다 (db/13_recipe_likes_to_sequence.sql) */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "recipe_likes_seq_generator")
    @SequenceGenerator(
            name = "recipe_likes_seq_generator", // Java 안에서 부르는 이름
            sequenceName = "RECIPE_LIKES_SEQ",   // Oracle 시퀀스 이름
            allocationSize = 1)
    private Long id;

    /** 찜한 사람. 한 번 정해지면 안 바뀐다 */
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    /**
     * 음식 이름. 추천 결과를 저장하지 않으므로 음식 번호가 없다.
     * 켜고 끄는 것만 바뀌므로 이 칸도 한 번 정해지면 안 바뀐다.
     */
    @Column(name = "food_name", nullable = false, updatable = false)
    private String foodName;

    /** 'Y' = 찜한 상태, 'N' = 해제한 상태 */
    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn;

    /**
     * 읽기만 한다. DB 기본값 SYS_EXTRACT_UTC(SYSTIMESTAMP) 가 채운다.
     *
     * @Generated 를 붙였으므로 INSERT 직후 하이버네이트가 DB 가 넣은 값을 다시
     * 읽어 온다. 롬복의 @Generated 와 이름이 같으니 import 를 보고 고른다 —
     * 롬복 쪽을 쓰면 하이버네이트가 못 보고 이 칸이 null 로 남는다.
     */
    @Generated
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 처음 찜하는 음식. id 와 찜한 시각은 DB 가 채운다 */
    public static FavoriteEntity create(Long userId, String foodName) {
        FavoriteEntity favorite = new FavoriteEntity();
        favorite.userId = userId;
        favorite.foodName = foodName;
        favorite.useYn = "Y";
        return favorite;
    }

    /**
     * 찜하기. 해제했던 행을 되살릴 때도 이걸 쓴다.
     *
     * save() 를 다시 부를 필요가 없다. 트랜잭션이 끝날 때 JPA 가 처음 읽어둔
     * 값과 견주어 달라진 칸을 찾아 스스로 UPDATE 를 보낸다(변경 감지).
     */
    public void turnOn() {
        this.useYn = "Y";
    }

    /** 해제. 행은 남기고 use_yn 만 바꾼다 */
    public void turnOff() {
        this.useYn = "N";
    }

    /** 지금 찜한 상태인지 */
    public boolean isActive() {
        return "Y".equals(useYn);
    }
}
