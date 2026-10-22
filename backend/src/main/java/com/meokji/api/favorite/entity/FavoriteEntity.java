package com.meokji.api.favorite.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Entity
@Table(name="RECIPE_LIKES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FavoriteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator ="recipe_likes_seq_generator")
    @SequenceGenerator(
            name="recipe_likes_seq_generator",  // Java 안에서 부르는 이름
            sequenceName = "RECIPE_LIKES_SEQ",  // Oracle 시퀀스 이름
            allocationSize = 1 )
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name="food_name", nullable = false)
    private String foodName;

    @Column(name="use_yn", nullable = false, length = 1)
    private String useYn;

    /**
     * 읽기만 한다. DB 기본갑시 넣는다
     */
    @Generated
    @Column(name="created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public static FavoriteEntity create(Long userId, String foodName){
        FavoriteEntity favorite = new FavoriteEntity();
        favorite.userId = userId;
        favorite.foodName = foodName;
        return favorite;
    }

}
