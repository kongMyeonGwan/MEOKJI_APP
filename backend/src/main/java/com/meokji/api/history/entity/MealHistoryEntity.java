package com.meokji.api.history.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * MEAL_HISTORIES 표 한 행 — 후기(JPA) 쪽에서 읽기만 할 때 쓴다.
 *
 * 같은 표에 MealHistoryVO(MyBatis)가 이미 있는데 이 클래스가 따로 있는 이유는
 * 두 모듈이 쓰는 기술이 달라서다. 히스토리 화면은 MyBatis 로 읽고 쓰지만,
 * 후기(review) 쪽은 JPA 라 ReviewRepository 의 JOIN 결과를 받을 그릇이 필요하다.
 *
 * 인터페이스 프로젝션으로는 받을 수 없다. RECIPE_JSON 같은 CLOB 칸을
 * String 으로 바꿔주지 못해서 "Cannot project ... Clob to java.lang.String" 이 난다.
 * 엔티티로 받으면 @Lob 이 붙은 칸을 Hibernate 가 풀어서 String 으로 채워준다.
 *
 * 쓰기는 하지 않는다. INSERT · UPDATE 는 지금처럼 MyBatis(HistoryDAO)가 맡는다.
 */
@Entity
@Table(name = "meal_histories")
@Getter
public class MealHistoryEntity {

    @Id
    private Long id;

    @Column(name = "food_name")
    private String foodName;

    @Column(name = "recipe_name")
    private String recipeName;

    /** {cook_time_minutes, servings, difficulty, steps[]} — 칸은 CLOB 이다 */
    @Lob
    @Column(name = "recipe_json")
    private String recipeJson;

    /** [{name, quantity, unit}, ...] */
    @Lob
    @Column(name = "ingredients_json")
    private String ingredientsJson;

    /** [{original, substitute, reason}, ...] 없으면 null */
    @Lob
    @Column(name = "substitutes_json")
    private String substitutesJson;

    @Column(name = "estimated_cost")
    private Integer estimatedCost;
}
